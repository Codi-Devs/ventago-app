package com.teco.ventago.features.branches.ui.billing_point.add.viewmodel

import com.teco.ventago.core.LoadableState
import com.teco.ventago.core.location.PanamaLocations
import com.teco.ventago.design_system.organism.LoadingBottomSheetState

data class AddBillingPointState(
    val name: String = "",
    val buttonEnabled: Boolean = false,
    override val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
) : LoadableState<AddBillingPointState> {
    override fun withLoading(state: LoadingBottomSheetState): AddBillingPointState {
        return copy(loadingBottomSheet = state)
    }
}

sealed class AddBillingPointStateUiEvent {
    data object GoBack: AddBillingPointStateUiEvent()

}