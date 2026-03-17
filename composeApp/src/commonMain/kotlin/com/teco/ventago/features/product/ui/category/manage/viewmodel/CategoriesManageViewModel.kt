package com.teco.ventago.features.product.ui.category.manage.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.authz.ActionKey
import com.teco.ventago.core.authz.AuthzEvaluator
import com.teco.ventago.design_system.organism.LoadingState
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.product.domain.ProductService
import com.teco.ventago.features.product.domain.model.Category
import com.teco.ventago.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString

class CategoriesManageViewModel(
    private val authService: IAuthService,
    private val businessService: BusinessService,
    private val productService: ProductService,
): ViewModel() {

    val state = CategoriesManageState()

    init {
        viewModelScope.launch {
            authService.getUser().collect { user ->
                state.canManageCategories.value = AuthzEvaluator.canAction(
                    ActionKey.PRODUCTS_MANAGE_CATEGORIES,
                    user,
                    emptySet()
                )
            }
        }
        viewModelScope.launch {
            productService.getMenu().onEach {
                it?.let {
                    for (cat in it.categories) {
                        println("ASDASD: items: ${json.encodeToString(cat.items)}")
                    }

                    state.categories.value = it.categories
                }
            }.launchIn(this)
        }
    }

    fun loadingDone() {
        state.loadingState.value = state.loadingState.value.copy(state = LoadingState.HIDDEN)
    }

    fun selectCategory(category: Category) {
        productService.selectedCategoryId = category.id
    }

    fun reorderCategories() {
        if (!state.canManageCategories.value) return
        state.showLoading("Reordenando categorías")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = productService.changeCategoryOrder(state.categories.value)
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

    fun removeCategory(categoryId: Int) {
        if (!state.canManageCategories.value) return
        if (categoryId <= 0) return
        state.showLoading("Eliminando categoría")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = productService.removeCategory(categoryId)
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

    fun activateCategory(categoryId: Int, active: Boolean) {
        if (!state.canManageCategories.value) return
        val message = if (active) "Activando categoría" else "Desactivando categoría"
        state.showLoading(message)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = productService.setCategoryActive(categoryId, active)
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
