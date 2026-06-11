package com.teco.ventago.features.home.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.core.authz.ActionKey
import com.teco.ventago.core.authz.AuthzEvaluator
import com.teco.ventago.core.authz.RouteKey
import com.teco.ventago.core.beta.BetaFeature
import com.teco.ventago.core.beta.BetaService
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.financialProfile.domain.FinancialProfileService
import com.teco.ventago.features.home.domain.HomeSummaryService
import com.teco.ventago.features.home.domain.model.HomeSalesChartMapper
import com.teco.ventago.features.home.domain.model.HomeSalesRange
import com.teco.ventago.features.notifications.domain.INotificationsService
import com.teco.ventago.features.product.domain.ProductService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class HomeViewModel(
    private val authService: IAuthService,
    private val businessService: BusinessService,
    private val productService: ProductService,
    private val financialProfileService: FinancialProfileService,
    private val homeSummaryService: HomeSummaryService,
    private val betaService: BetaService,
    private val notificationsService: INotificationsService,
    private val ioDispatcher: CoroutineDispatcher,
) : BaseViewModel<HomeState, HomeStateUiEvent>(HomeState()) {

    private var summaryBusinessId: Int? = null

    init {
        if (isEmptyData()) {
            updateState { copy(isLoadingData = true) }
        }

        viewModelScope.launch {
            authService.getUser()
                .combine(betaService.features()) { user, betaResponse ->
                    val betaSnapshot = betaResponse?.features.orEmpty()
                        .mapNotNull(BetaFeature::fromKey)
                        .toSet()
                    AuthzUiState(
                        canCreateOrderEntry = AuthzEvaluator.canAction(ActionKey.ORDERS_OPEN_CREATE, user, betaSnapshot),
                        hasQuotesAccess = AuthzEvaluator.canAction(ActionKey.QUOTES_CREATE, user, betaSnapshot),
                        canCreateExpense = AuthzEvaluator.canAction(ActionKey.EXPENSES_CREATE, user, betaSnapshot),
                        canAccessCustomers = AuthzEvaluator.canRoute(RouteKey.CUSTOMERS_LIST, user, betaSnapshot),
                        canAccessExpenses = AuthzEvaluator.canRoute(RouteKey.EXPENSES_LIST, user, betaSnapshot),
                        canAccessReports = AuthzEvaluator.canRoute(RouteKey.REPORTS_PAGE, user, betaSnapshot),
                        showSupportCard = user?.isOwnerMain == true,
                        showFolioPurchase = user?.isOwnerMain == true
                    )
                }
                .onEach { authz ->
                    updateState {
                        copy(
                            canCreateOrderEntry = authz.canCreateOrderEntry,
                            hasQuotesAccess = authz.hasQuotesAccess,
                            canCreateExpense = authz.canCreateExpense,
                            canAccessCustomers = authz.canAccessCustomers,
                            canAccessExpenses = authz.canAccessExpenses,
                            canAccessReports = authz.canAccessReports,
                            showSupportCard = authz.showSupportCard,
                            showFolioPurchase = authz.showFolioPurchase
                        )
                    }
                }
                .launchIn(this)
            betaService.getFeatures()

            businessService.getBusiness().combine(productService.state) { business, menu ->
                Pair(business, menu)
            }.onEach { newState ->
                val business = newState.first
                val products = newState.second
                if (business != null && products != null) {
                    updateState {
                        copy(
                            business = business,
                            products = products
                        )
                    }
                    if (uiState.value.isLoadingData) {
                        delay(200)
                        updateState { copy(isLoadingData = false) }
                    }

                    val businessId = business.businessId
                    val activeBusinessChanged = summaryBusinessId != businessId
                    if (activeBusinessChanged) {
                        summaryBusinessId = businessId
                        updateState { copy(isSummaryLoading = true, summaryError = null) }
                        val summaryResult = runCatching {
                            withContext(ioDispatcher) {
                                homeSummaryService.setBusiness(businessId, refresh = true)
                            }
                        }
                        summaryResult.onFailure { error ->
                            updateState {
                                copy(
                                    isSummaryLoading = false,
                                    summaryError = error.message ?: "Error loading summary"
                                )
                            }
                        }
                    }

                    val financialProfileBusinessId = financialProfileService.observe().value?.businessId
                    if (activeBusinessChanged || financialProfileBusinessId != businessId) {
                        runCatching {
                            withContext(ioDispatcher) {
                                financialProfileService.setBusiness(businessId, refresh = true)
                            }
                        }
                    }
                }
            }.launchIn(this)

            financialProfileService.observe().onEach { profile ->
                profile?.let {
                    val invoicingPlanState = it.invoiceSummary?.let { invoiceSummary ->
                        InvoicingPlanState(
                            availableDtes = invoiceSummary.planAvailableDte,
                            totalDtes = invoiceSummary.planTotalDte,
                            activationDate = invoiceSummary.aggregateActivationDate(),
                            expirationDate = invoiceSummary.aggregateExpiryDate()
                        )
                    }

                    updateState {
                        copy(
                            invoicingEnabled = it.invoicingActive,
                            invoicingPlanState = invoicingPlanState
                        )
                    }
                }
            }.launchIn(this)

            homeSummaryService.observe().onEach { summary ->
                val range = uiState.value.selectedRange
                val chart = HomeSalesChartMapper.buildChart(summary, range)
                updateState {
                    copy(
                        homeSummary = summary,
                        salesChart = chart,
                        selectedSalesIndex = defaultSelectedIndex(range, chart),
                        isSummaryLoading = false,
                        summaryError = if (summary != null) null else summaryError
                    )
                }
            }.launchIn(this)

            notificationsService.observeUnreadCount().onEach { unread ->
                updateState { copy(unreadCount = unread) }
            }.launchIn(this)
        }
    }

    private data class AuthzUiState(
        val canCreateOrderEntry: Boolean,
        val hasQuotesAccess: Boolean,
        val canCreateExpense: Boolean,
        val canAccessCustomers: Boolean,
        val canAccessExpenses: Boolean,
        val canAccessReports: Boolean,
        val showSupportCard: Boolean,
        val showFolioPurchase: Boolean,
    )

    private fun isEmptyData(): Boolean {
        return uiState.value.business == null || uiState.value.products == null
    }

    fun setSalesRange(range: HomeSalesRange) {
        if (uiState.value.selectedRange == range) {
            return
        }

        val chart = HomeSalesChartMapper.buildChart(uiState.value.homeSummary, range)
        updateState {
            copy(
                selectedRange = range,
                salesChart = chart,
                selectedSalesIndex = defaultSelectedIndex(range, chart)
            )
        }
    }

    fun setSelectedSalesIndex(index: Int) {
        val chart = uiState.value.salesChart
        if (chart.isEmpty()) return
        updateState { copy(selectedSalesIndex = index.coerceIn(0, chart.lastIndex)) }
    }

    private fun defaultSelectedIndex(range: HomeSalesRange, chart: List<Pair<String, Double>>): Int {
        if (chart.isEmpty()) return 0
        return when (range) {
            HomeSalesRange.YEAR -> {
                val currentMonthIndex = Clock.System.now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
                    .monthNumber - 1
                currentMonthIndex.coerceIn(0, chart.lastIndex)
            }

            else -> chart.lastIndex
        }
    }

    fun onHomeVisible() {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                runCatching { notificationsService.refreshUnreadCount() }
            }
        }
    }
}
