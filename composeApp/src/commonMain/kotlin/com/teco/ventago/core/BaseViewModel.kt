package com.teco.ventago.core

import androidx.lifecycle.ViewModel
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.design_system.organism.LoadingState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


abstract class BaseViewModel<UiState, UiEvent>(
    initialState: UiState
) : ViewModel() where UiState : LoadableState<UiState>,
                      UiState : Any,
                      UiEvent : Any  {

    protected val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    protected val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    protected fun updateState(reducer: UiState.() -> UiState) {
        _uiState.update { it.reducer() }
    }

    protected suspend fun emitEvent(event: UiEvent) {
        _events.emit(event)
    }

    fun showLoading() {
        updateState {
            withLoading(LoadingBottomSheetState(LoadingState.LOADING, ""))
        }
    }

    fun showSuccess() {
        updateState {
            withLoading(LoadingBottomSheetState(LoadingState.SUCCESS, ""))
        }
    }

    fun showError() {
        updateState {
            withLoading(LoadingBottomSheetState(LoadingState.ERROR, ""))
        }
    }

    fun hideLoading() {
        updateState {
            withLoading(LoadingBottomSheetState(LoadingState.HIDDEN))
        }
    }
}

interface LoadableState<T : LoadableState<T>> {
    val loadingBottomSheet: LoadingBottomSheetState
    fun withLoading(state: LoadingBottomSheetState): T
}