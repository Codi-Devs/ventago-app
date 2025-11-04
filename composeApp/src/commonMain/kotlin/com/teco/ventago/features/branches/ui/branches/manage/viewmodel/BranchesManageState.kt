package com.teco.ventago.features.branches.ui.branches.manage.viewmodel

import com.teco.ventago.core.LoadableState
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.features.branches.domain.model.Branch

data class BranchesManageState(
    val branches: List<Branch> = emptyList(),
    override val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
): LoadableState<BranchesManageState> {
    override fun withLoading(state: LoadingBottomSheetState): BranchesManageState {
        return copy(loadingBottomSheet = state)
    }
}

sealed class BranchesManageStateUiEvent {
    data object GoBack: BranchesManageStateUiEvent()

}