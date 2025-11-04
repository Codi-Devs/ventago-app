package com.teco.ventago.features.product.ui.category.manage.viewmodel

import androidx.compose.runtime.mutableStateOf
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.design_system.organism.LoadingState
import com.teco.ventago.features.product.domain.model.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CategoriesManageState {
    val categories = mutableStateOf(listOf<Category>())

    val loadingState = mutableStateOf(LoadingBottomSheetState())

    val selectedCategory = mutableStateOf<Category?>(null)

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