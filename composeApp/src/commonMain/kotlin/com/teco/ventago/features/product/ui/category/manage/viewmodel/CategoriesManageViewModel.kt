package com.teco.ventago.features.product.ui.category.manage.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.authz.ActionKey
import com.teco.ventago.core.authz.AuthzEvaluator
import com.teco.ventago.design_system.organism.LoadingState
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.inventory.domain.InventoryAvailabilitySnapshot
import com.teco.ventago.features.inventory.domain.InventoryAvailabilityStore
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
    private val inventoryAvailabilityStore: InventoryAvailabilityStore,
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
        inventoryAvailabilityStore.snapshot.onEach { snap ->
            applyPositiveStock(state.categories.value, snap)
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            productService.getMenu().onEach {
                it?.let { menu ->
                    for (cat in menu.categories) {
                        println("ASDASD: items: ${json.encodeToString(cat.items)}")
                    }

                    state.categories.value = menu.categories
                    applyPositiveStock(menu.categories, inventoryAvailabilityStore.snapshot.value)
                    refreshInventory(menu.categories)
                }
            }.launchIn(this)
        }
    }

    private fun applyPositiveStock(
        categories: List<Category>,
        snap: InventoryAvailabilitySnapshot,
    ) {
        state.positiveStockByItemId.value = categories
            .flatMap { it.items }
            .mapNotNull { item ->
                snap.positiveAvailableStockLabel(item.itemId)?.let { item.itemId to it }
            }
            .toMap()
    }

    private fun refreshInventory(categories: List<Category>) {
        viewModelScope.launch(Dispatchers.IO) {
            val businessId = businessService.business.value?.businessId ?: return@launch
            val itemIds = categories.flatMap { it.items }.map { it.itemId }
            inventoryAvailabilityStore.refreshForCatalog(businessId, itemIds)
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
        val category = state.categories.value.firstOrNull { it.id == categoryId }
        if (category?.items?.any { state.positiveStockByItemId.value.containsKey(it.itemId) } == true) return
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
