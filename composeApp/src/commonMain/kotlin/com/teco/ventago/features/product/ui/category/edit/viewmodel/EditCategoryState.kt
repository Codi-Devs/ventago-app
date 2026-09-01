package com.teco.ventago.features.product.ui.category.edit.viewmodel

import androidx.compose.runtime.mutableStateOf
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.design_system.organism.LoadingState
import com.teco.ventago.features.product.domain.model.Category
import com.teco.ventago.features.product.domain.model.Item

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EditCategoryState {
    val items = mutableStateOf(listOf<Item>())
    val query = mutableStateOf("")
    val loadingState = mutableStateOf(LoadingBottomSheetState())
    val selectedCategory = mutableStateOf<Category?>(null)
    val currency = mutableStateOf("USD")

    val goBack = mutableStateOf(false)

    val isPremium = mutableStateOf(false)
    val canManageCategories = mutableStateOf(false)
    val inventoryStockByItemId = mutableStateOf(emptyMap<Int, String>())
    val inventoryCostByItemId = mutableStateOf(emptyMap<Int, String>())

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
