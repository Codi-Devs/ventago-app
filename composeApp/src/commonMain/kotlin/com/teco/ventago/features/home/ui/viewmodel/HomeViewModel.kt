package com.teco.ventago.features.home.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.core.beta.BetaFeature
import com.teco.ventago.core.beta.BetaService
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.financialProfile.domain.FinancialProfileService
import com.teco.ventago.features.home.domain.HomeSummaryService
import com.teco.ventago.features.home.domain.model.HomeSalesChartMapper
import com.teco.ventago.features.home.domain.model.HomeSalesRange
import com.teco.ventago.features.product.domain.ProductService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class HomeViewModel(
    private val businessService: BusinessService,
    private val productService: ProductService,
    private val financialProfileService: FinancialProfileService,
    private val homeSummaryService: HomeSummaryService,
    private val betaService: BetaService,
) : BaseViewModel<HomeState, HomeStateUiEvent>(HomeState()) {

    private var summaryBusinessId: Int? = null

    init {
        if (isEmptyData()) {
            updateState { copy(isLoadingData = true) }
        }

        viewModelScope.launch {
            betaService.accessFlow(BetaFeature.QUOTES)
                .onEach { hasAccess -> updateState { copy(hasQuotesAccess = hasAccess) } }
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

                    if (summaryBusinessId != business.businessId) {
                        summaryBusinessId = business.businessId
                        updateState { copy(isSummaryLoading = true, summaryError = null) }
                        runCatching {
                            homeSummaryService.setBusiness(business.businessId, refresh = true)
                        }.onFailure { error ->
                            updateState {
                                copy(
                                    isSummaryLoading = false,
                                    summaryError = error.message ?: "Error loading summary"
                                )
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
                            activationDate = invoiceSummary.planStartDate,
                            expirationDate = invoiceSummary.planExpiryDate
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
                val chart = HomeSalesChartMapper.buildChart(summary, uiState.value.selectedRange)
                updateState {
                    copy(
                        homeSummary = summary,
                        salesChart = chart,
                        selectedSalesIndex = if (chart.isNotEmpty()) chart.lastIndex else 0,
                        isSummaryLoading = false,
                        summaryError = if (summary != null) null else summaryError
                    )
                }
            }.launchIn(this)
        }
    }

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
                selectedSalesIndex = if (chart.isNotEmpty()) chart.lastIndex else 0
            )
        }
    }

    fun setSelectedSalesIndex(index: Int) {
        val chart = uiState.value.salesChart
        if (chart.isEmpty()) return
        updateState { copy(selectedSalesIndex = index.coerceIn(0, chart.lastIndex)) }
    }
}
