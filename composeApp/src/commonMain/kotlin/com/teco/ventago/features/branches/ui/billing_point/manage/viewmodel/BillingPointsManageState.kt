package com.teco.ventago.features.branches.ui.billing_point.manage.viewmodel

import com.teco.ventago.core.LoadableState
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.features.branches.domain.model.FiscalBillingPoint

data class BillingPointsManageState(
    val billingPoints: List<FiscalBillingPoint> = emptyList(),
    val selectedBranchCode: String? = null,
    override val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
): LoadableState<BillingPointsManageState> {
    override fun withLoading(state: LoadingBottomSheetState): BillingPointsManageState {
        return copy(loadingBottomSheet = state)
    }
}

sealed class BillingPointsManageStateUiEvent {
    data object GoBack: BillingPointsManageStateUiEvent()

}