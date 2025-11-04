package com.teco.ventago.features.branches.ui.billing_point.edit.viewmodel

import com.teco.ventago.core.LoadableState
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.features.branches.domain.model.FiscalBillingPoint

data class EditBillingPointState(
    val billingPoint: FiscalBillingPoint? = null,
    val name: String = "",
    val buttonEnabled: Boolean = false,
    override val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
) : LoadableState<EditBillingPointState> {
    override fun withLoading(state: LoadingBottomSheetState): EditBillingPointState {
        return copy(loadingBottomSheet = state)
    }
}

sealed class EditBillingPointStateUiEvent {
    data object GoBack: EditBillingPointStateUiEvent()

}