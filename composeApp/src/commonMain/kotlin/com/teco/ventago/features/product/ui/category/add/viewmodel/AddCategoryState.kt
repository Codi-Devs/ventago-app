package com.teco.ventago.features.product.ui.category.add.viewmodel

import androidx.compose.runtime.mutableStateOf
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.design_system.organism.LoadingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AddCategoryState {
    val name = mutableStateOf("")
    val description = mutableStateOf("")
    val showHelpDialog = mutableStateOf(false)
    val wrongName = mutableStateOf(false)
    val loadingState = mutableStateOf(LoadingBottomSheetState())

    val nonVipLimitReached = mutableStateOf(false)


    val goBack = mutableStateOf(false)

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