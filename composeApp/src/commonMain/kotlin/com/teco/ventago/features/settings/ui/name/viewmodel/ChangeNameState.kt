package com.teco.ventago.features.settings.ui.name.viewmodel

import androidx.compose.runtime.mutableStateOf
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.design_system.organism.LoadingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChangeNameState {
    val isError = mutableStateOf(false)
    val name = mutableStateOf("")
    val loadingState = mutableStateOf(LoadingBottomSheetState())

    fun showLoading(title: String) {
        loadingState.value = loadingState.value.copy(state = LoadingState.LOADING, title = title)
    }

    suspend fun showSuccess() {
        withContext(Dispatchers.Main) {
            loadingState.value = loadingState.value.copy(state = LoadingState.SUCCESS)
        }
    }

    suspend fun showError() {
        withContext(Dispatchers.Main) {
            loadingState.value = loadingState.value.copy(state = LoadingState.ERROR)
        }
    }
}