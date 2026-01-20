package com.teco.ventago.features.home.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.financialProfile.domain.FinancialProfileService
import com.teco.ventago.features.financialProfile.domain.model.BusinessFinancialProfile
import com.teco.ventago.features.home.domain.HistoricSalesService
import com.teco.ventago.features.payments.domain.PaymentService
import com.teco.ventago.features.product.domain.ProductService
import com.teco.ventago.core.beta.BetaService
import com.teco.ventago.core.beta.BetaFeature
import com.teco.ventago.viewModels
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class HomeViewModel(
    private val businessService: BusinessService,
    private val productService: ProductService,
    private val financialProfileService: FinancialProfileService,
    private val authService: IAuthService,
    private val salesService: HistoricSalesService,
    private val betaService: BetaService,
): BaseViewModel<HomeState, HomeStateUiEvent>(HomeState()) {

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
                println("ASDASD: hasQuotes: ${business != null && products != null}")
                if (business != null && products != null) {
                    updateState {
                        copy(
                            business = business,
                            products = products
                        )
                    }
                    delay(200)
                    updateState { copy(isLoadingData = false) }
                    salesService.initialize(business.businessId)
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

            salesService.getSales().onEach { newSales ->
                val nextIndex = if (!newSales.isNullOrEmpty()) {
                    newSales.lastIndex
                } else {
                    uiState.value.selectedSalesIndex
                }
                updateState { copy(sales = newSales, selectedSalesIndex = nextIndex) }
            }.launchIn(this)
        }
    }



    private fun isEmptyData(): Boolean {
        return uiState.value.business == null || uiState.value.products == null
    }

    fun setSelectedSalesIndex(index: Int) {
        updateState { copy(selectedSalesIndex = index) }
    }

}
