package com.teco.ventago.features.product.ui.category.edit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teco.ventago.design_system.organism.LoadingState
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.product.domain.ProductService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class EditCategoryViewModel(
    private val authService: IAuthService,
    private val businessService: BusinessService,
    private val productService: ProductService,
) : ViewModel() {

    val state = EditCategoryState()
    private val authJob: Job

    init {
        val selectedCategoryId = productService.selectedCategoryId
        if (selectedCategoryId == null) {
            state.goBack.value = true
        }

        authJob = viewModelScope.launch {
            authService.getUser().cancellable().collect {
                state.isPremium.value = it?.premium ?: false
            }
        }

        viewModelScope.launch {
            productService.getMenu().onEach { menu ->
                menu?.let { _ ->
                    menu.categories.firstOrNull{ it.id == selectedCategoryId }?.let {
                        state.selectedCategory.value = it
                        state.items.value = it.items
                    } ?: run {
                        // TODO Add logs
                        state.goBack.value = true
                    }
                } ?: run {
                    // TODO Add logs
                    state.goBack.value = true
                }
            }.launchIn(this)

            businessService.getBusiness().onEach { business ->
                business?.let {
                    state.currency.value = it.currency.currencyCode
                }
            }.launchIn(this)
        }
    }

    override fun onCleared() {
        try {
            authJob.cancel()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        super.onCleared()
    }

    fun selectItem(itemId: Int) {
        productService.selectedItemId = itemId
    }

    fun navigateUp() {
        productService.selectedCategoryId = null
    }

    fun loadingDone() {
        state.loadingState.value = state.loadingState.value.copy(state = LoadingState.HIDDEN)
    }

    fun filterItems(query: String) {
        state.query.value = query
        productService.state.value?.let { products ->
            val category = products.categories.firstOrNull { it.id == state.selectedCategory.value?.id }
            category?.let { cat ->
                if (query.isBlank()) {
                    state.items.value = cat.items
                } else {
                    state.items.value = cat.items.filter {
                        it.name.contains(query, true) || it.description.contains(
                            query,
                            true
                        )
                    }
                }

            }
        }
    }

    fun removeItem(itemId: Int) {
        if (itemId <= 0) return
        state.showLoading("Eliminando elemento")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = productService.removeItem(itemId)
                if (res) {
                    state.showSuccess()
                } else {
                    state.showError()
                }
            } catch (e: Exception) {
                // TODO Add logs
                state.showError()
            }
        }
    }

    fun activateItem(itemId: Int, active: Boolean) {
        if (itemId <= 0) return
        val item = state.items.value.firstOrNull { it.itemId == itemId }
        item?.let {
            val itemCopy = it.copy(active = active)
            val message = if (active) "Activando elemento" else "Desactivando elemento"
            val categoryId = state.selectedCategory.value?.id
            if (categoryId == null) {
                viewModelScope.launch(Dispatchers.Main) {
                    state.showError()
                }
                return
            }
            state.showLoading(message)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val res = productService.editItem(itemCopy, categoryId)
                    if (res) {
                        state.showSuccess()
                    } else {
                        state.showError()
                    }
                } catch (e: Exception) {
                    // TODO Add logs
                    state.showError()
                }
            }
        }
    }
}