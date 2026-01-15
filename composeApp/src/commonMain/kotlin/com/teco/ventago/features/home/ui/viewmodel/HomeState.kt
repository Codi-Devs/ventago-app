package com.teco.ventago.features.home.ui.viewmodel

import com.teco.ventago.core.LoadableState
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.features.business.domain.model.Business
import com.teco.ventago.features.product.domain.model.Products


data class HomeState(
    val business: Business? = null,
    val products: Products? = null,
    val isLoadingData: Boolean = true,

    val selectedSalesIndex: Int = 5,
    val sales: List<Pair<String, Double>>? = null,
    val unreadCount: Int = 0,

    val invoicingEnabled: Boolean = false,
    val invoicingPlanState: InvoicingPlanState? = null,
    val hasQuotesAccess: Boolean = false,

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
