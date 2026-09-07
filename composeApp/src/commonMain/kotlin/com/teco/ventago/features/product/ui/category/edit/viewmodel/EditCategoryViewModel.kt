package com.teco.ventago.features.product.ui.category.edit.viewmodel

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
import com.teco.ventago.features.product.domain.model.Item
import com.teco.ventago.utils.formatNumberToMoney
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditCategoryViewModel(
    private val authService: IAuthService,
    private val businessService: BusinessService,
    private val productService: ProductService,
    private val inventoryAvailabilityStore: InventoryAvailabilityStore,
) : ViewModel() {

    val state = EditCategoryState()
    private val authJob: Job
    private var inventoryJob: Job? = null

    init {
        val selectedCategoryId = productService.selectedCategoryId
        if (selectedCategoryId == null) {
            state.goBack.value = true
        }

        authJob = viewModelScope.launch {
            authService.getUser().cancellable().collect {
                state.isPremium.value = it?.premium ?: false
                state.canManageCategories.value = AuthzEvaluator.canAction(
                    ActionKey.PRODUCTS_MANAGE_CATEGORIES,
                    it,
                    emptySet()
                )
            }
        }

        inventoryAvailabilityStore.snapshot
            .onEach { snap ->
                if (state.items.value.isNotEmpty()) {
                    applyInventoryLabels(state.items.value, snap)
                }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            productService.getMenu().onEach { menu ->
                menu?.let { _ ->
                    menu.categories.firstOrNull{ it.id == selectedCategoryId }?.let {
                        state.selectedCategory.value = it
                        state.items.value = it.items
                        syncInventoryLabels()
                        refreshInventoryLabels(it.items)
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
            inventoryJob?.cancel()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        super.onCleared()
    }

    fun onScreenVisible() {
        syncInventoryLabels()
        refreshInventoryLabels(state.items.value)
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
                syncInventoryLabels()
                refreshInventoryLabels(state.items.value)
            }
        }
    }

    fun removeItem(itemId: Int) {
        if (!state.canManageCategories.value) return
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
        if (!state.canManageCategories.value) return
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

    private fun refreshInventoryLabels(items: List<Item>) {
        inventoryJob?.cancel()
        inventoryJob = viewModelScope.launch(Dispatchers.IO) {
            val businessId = businessService.business.value?.businessId ?: return@launch
            inventoryAvailabilityStore.refreshForCatalog(businessId, items.map { it.itemId })
            withContext(Dispatchers.Main) {
                syncInventoryLabels()
            }
        }
    }

    private fun syncInventoryLabels() {
        val items = state.items.value
        if (items.isEmpty()) return
        applyInventoryLabels(items, inventoryAvailabilityStore.snapshot.value)
    }

    private fun applyInventoryLabels(items: List<Item>, snap: InventoryAvailabilitySnapshot) {
        if (!snap.enabled && snap.byItemId.isEmpty() && snap.fetchedAtEpochMs == 0L) return
        val stock = mutableMapOf<Int, String>()
        val cost = mutableMapOf<Int, String>()
        items.forEach { item ->
            snap.catalogStockLabel(item.itemId)?.let { stock[item.itemId] = it }
            val row = snap.byItemId[item.itemId] ?: return@forEach
            if (!row.tracked) return@forEach
            val avg = row.movingAverageUnitCost?.trim().orEmpty()
            val amount = avg.ifEmpty { item.cost?.toString().orEmpty() }
            if (amount.isNotEmpty()) {
                val suffix = if (avg.isNotEmpty()) "Inv." else "Cat."
                cost[item.itemId] = "${formatNumberToMoney(amount)} $suffix"
            }
        }
        state.inventoryStockByItemId.value = stock
        state.inventoryCostByItemId.value = cost
    }
}
