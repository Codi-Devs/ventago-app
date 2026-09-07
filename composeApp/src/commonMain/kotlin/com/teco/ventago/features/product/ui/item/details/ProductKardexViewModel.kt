package com.teco.ventago.features.product.ui.item.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.inventory.domain.InventoryKardexSupport
import com.teco.ventago.features.inventory.domain.ProductInventorySupport
import com.teco.ventago.features.product.domain.ProductService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductKardexViewModel(
    private val businessService: BusinessService,
    private val productService: ProductService,
    private val productInventorySupport: ProductInventorySupport,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProductKardexUiState())
    val uiState: StateFlow<ProductKardexUiState> = _uiState.asStateFlow()

    init {
        loadInitial()
    }

    fun loadInitial(force: Boolean = false) {
        val itemId = productService.selectedItemId ?: return
        val businessId = businessService.business.value?.businessId ?: return
        val itemName = resolveItemName(itemId)

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(loading = true, itemName = itemName, errorMessage = null)
            val result = productInventorySupport.loadKardex(
                businessId = businessId,
                itemId = itemId,
                force = force,
            )
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    rows = result.rows,
                    displayRows = result.displayRows,
                    page = 0,
                    hasMoreOnServer = result.hasMoreOnServer,
                    nextCursor = result.nextBeforeMovementId,
                )
            }
        }
    }

    fun loadMoreFromServer() {
        val itemId = productService.selectedItemId ?: return
        val businessId = businessService.business.value?.businessId ?: return
        val state = _uiState.value
        if (!state.hasMoreOnServer || state.nextCursor == null || state.loadingMore) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = state.copy(loadingMore = true)
            val result = productInventorySupport.loadKardex(
                businessId = businessId,
                itemId = itemId,
                existingRows = state.rows,
                nextCursor = state.nextCursor,
            )
            val mergedDisplay = InventoryKardexSupport.collapseKardexRows(result.rows)
            val newPageCount = InventoryKardexSupport.pageCount(mergedDisplay.size)
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    loadingMore = false,
                    rows = result.rows,
                    displayRows = mergedDisplay,
                    page = (newPageCount - 1).coerceAtLeast(0),
                    hasMoreOnServer = result.hasMoreOnServer,
                    nextCursor = result.nextBeforeMovementId,
                )
            }
        }
    }

    fun goToPage(delta: Int) {
        val state = _uiState.value
        val lastPage = state.pageCount - 1
        val target = (state.page + delta).coerceIn(0, lastPage)
        if (target == state.page) {
            if (delta > 0 && state.page == lastPage && state.hasMoreOnServer) {
                loadMoreFromServer()
            }
            return
        }
        _uiState.value = state.copy(page = target)
    }

    private fun resolveItemName(itemId: Int): String {
        val categoryId = productService.selectedCategoryId ?: return "Producto"
        val menu = productService.state.value ?: return "Producto"
        val item = menu.categories.firstOrNull { it.id == categoryId }
            ?.items?.firstOrNull { it.itemId == itemId }
        return item?.name ?: "Producto"
    }
}
