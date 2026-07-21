package com.teco.ventago.features.home.ui.viewmodel

import com.teco.ventago.core.LoadableState
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.features.business.domain.model.Business
import com.teco.ventago.features.financialProfile.domain.model.FeeBillingSummary
import com.teco.ventago.features.home.domain.model.HomeSalesRange
import com.teco.ventago.features.home.domain.model.HomeSummary
import com.teco.ventago.features.product.domain.model.Products


data class HomeState(
    val business: Business? = null,
    val products: Products? = null,
    val isLoadingData: Boolean = true,

    val selectedSalesIndex: Int = 0,
    val selectedRange: HomeSalesRange = HomeSalesRange.D7,
    val salesChart: List<Pair<String, Double>> = emptyList(),
    val homeSummary: HomeSummary? = null,
    val isSummaryLoading: Boolean = false,
    val summaryError: String? = null,
    val unreadCount: Int = 0,

    val invoicingEnabled: Boolean = false,
    val invoicingPlanState: InvoicingPlanState? = null,
    val feeBillingSummary: FeeBillingSummary = FeeBillingSummary(),
    val paymentProfileResolved: Boolean = false,
    val hasConfiguredPaymentMethods: Boolean = false,
    val hasQuotesAccess: Boolean = false,
    val canCreateOrderEntry: Boolean = false,
    val canCreateExpense: Boolean = false,
    val canAccessCustomers: Boolean = false,
    val canAccessExpenses: Boolean = false,
    val canAccessReports: Boolean = false,
    val showSupportCard: Boolean = false,
    val showFolioPurchase: Boolean = false,

    override val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
): LoadableState<HomeState> {
    override fun withLoading(state: LoadingBottomSheetState): HomeState {
        return copy(loadingBottomSheet = state)
    }
}

data class InvoicingPlanState(
    val availableDtes: Int = 0,
    val totalDtes: Int = 0,
    val activationDate: String = "",
    val expirationDate: String = ""
)

sealed class HomeStateUiEvent {
    data object HomeLoadingFailed : HomeStateUiEvent()
}
