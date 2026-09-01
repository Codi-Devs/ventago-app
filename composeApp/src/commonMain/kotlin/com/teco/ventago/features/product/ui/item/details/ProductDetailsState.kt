package com.teco.ventago.features.product.ui.item.details

import com.teco.ventago.features.inventory.domain.ProductInventoryDetails
import com.teco.ventago.features.product.domain.model.Item

data class ProductDetailsUiState(
    val loading: Boolean = true,
    val item: Item? = null,
    val categoryName: String = "",
    val currency: String = "USD",
    val canEdit: Boolean = false,
    val inventoryLoading: Boolean = false,
    val inventory: ProductInventoryDetails = ProductInventoryDetails(),
    val errorMessage: String? = null,
)
