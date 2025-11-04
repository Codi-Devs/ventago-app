package com.teco.ventago.features.settings.ui.logo.viewmodel

import androidx.compose.runtime.mutableStateOf
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.design_system.organism.LoadingState
import com.teco.ventago.utils.ViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChangeImageState {
    val imgUrl = mutableStateOf<String?>(null)
    val uploadingImage = ViewState()
    val loadingState = mutableStateOf(LoadingBottomSheetState())
    val enableButton = mutableStateOf(false)
    val pendingChanges = mutableStateOf(false)


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