package com.teco.ventago.features.product.ui.item.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.authz.ActionKey
import com.teco.ventago.core.authz.AuthzEvaluator
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.inventory.domain.ProductInventorySupport
import com.teco.ventago.features.product.domain.ProductService
import com.teco.ventago.features.product.domain.model.ProductType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductDetailsViewModel(
    private val authService: IAuthService,
    private val businessService: BusinessService,
    private val productService: ProductService,
    private val productInventorySupport: ProductInventorySupport,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProductDetailsUiState())
    val uiState: StateFlow<ProductDetailsUiState> = _uiState.asStateFlow()

    private var loadedItemId: Int? = null

    init {
        authService.getUser().onEach { user ->
            _uiState.value = _uiState.value.copy(
                canEdit = AuthzEvaluator.canAction(ActionKey.PRODUCTS_UPDATE, user, emptySet()),
            )
        }.launchIn(viewModelScope)

        businessService.getBusiness().onEach { business ->
            business?.let {
                _uiState.value = _uiState.value.copy(currency = it.currency.currencyCode)
            }
        }.launchIn(viewModelScope)

        loadFromSelection()
    }

    fun loadFromSelection(force: Boolean = false) {
        val categoryId = productService.selectedCategoryId
        val itemId = productService.selectedItemId
        if (categoryId == null || itemId == null) {
            _uiState.value = _uiState.value.copy(loading = false, errorMessage = "Producto no encontrado.")
            return
        }
        if (!force && loadedItemId == itemId && _uiState.value.item != null) return
        loadedItemId = itemId

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, errorMessage = null)
            val menu = productService.state.value
            val category = menu?.categories?.firstOrNull { it.id == categoryId }
            val item = category?.items?.firstOrNull { it.itemId == itemId }
            if (item == null) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    errorMessage = "Producto no encontrado.",
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(
                loading = false,
                item = item,
                categoryName = category.name,
            )
            if (item.productType == ProductType.GOOD) {
                refreshInventory(item.itemId, force)
            }
        }
    }

    private fun refreshInventory(itemId: Int, force: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val businessId = businessService.business.value?.businessId ?: return@launch
            _uiState.value = _uiState.value.copy(inventoryLoading = true)
            val inventory = productInventorySupport.loadDetails(businessId, itemId, force)
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    inventoryLoading = false,
                    inventory = inventory,
                )
            }
        }
    }

    fun navigateUp() {
        productService.selectedItemId = null
    }
}
