package com.teco.ventago.features.branches.ui.branches.manage.viewmodel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.features.branches.domain.BranchService
import com.teco.ventago.features.business.domain.BusinessService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class BranchesManageViewModel(
    branchService: BranchService
) : BaseViewModel<BranchesManageState, BranchesManageStateUiEvent>(BranchesManageState()) {

    init {
        branchService.observe()
            .onStart {
                updateState {
                    copy(
                        branches = emptyList()
                    )
                }
            }
            .onEach { branches ->
                updateState {
                    copy(
                        branches =  branches
                    )
                }
            }.catch {
                // TODO Add logs
            }.launchIn(viewModelScope)
    }
}