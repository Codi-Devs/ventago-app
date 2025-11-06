package com.teco.ventago.features.product.ui.item.add.viewmodel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.camera.SharedImage
import com.teco.ventago.features.orders.domain.models.requests.NameValue
import com.teco.ventago.features.product.domain.ProductService
import com.teco.ventago.features.product.domain.model.AdditionalInfoCatalog
import com.teco.ventago.features.product.domain.model.AdditionalInfoKey
import com.teco.ventago.features.product.domain.model.AdditionalValueType
import com.teco.ventago.features.product.domain.model.GoodsFamily
import com.teco.ventago.features.product.domain.model.GoodsSegment
import com.teco.ventago.features.product.domain.model.Item
import com.teco.ventago.features.product.domain.model.OTITax
import com.teco.ventago.features.product.domain.model.ProductType
import com.teco.ventago.features.product.domain.model.UomRegistry
import com.teco.ventago.utils.randomUUID
import com.teco.ventago.utils.uploadImageToBunnyCdn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.jetbrains.compose.resources.ExperimentalResourceApi
import ventago.composeapp.generated.resources.Res
import kotlin.ranges.coerceIn

class AddItemViewModel(
    private val productService: ProductService
) : ItemViewModel(productService) {

    init {
        loadGoodsCsv()
        if ((productService.state.value?.categories?.size ?: 0) > 0) {
            // Check if there's a selected category from navigation
            val selectedCategory = productService.selectedCategoryId?.let { categoryId ->
                productService.state.value?.categories?.find { it.id == categoryId }
            } ?: productService.state.value?.categories?.get(0)
            
            selectedCategory?.let { category ->
                updateState { copy(selectedCategory = category) }
            }
        }
    }

    fun categories() = productService.state.value?.categories ?: listOf()

    fun selectedCategoryIndex() = uiState.value.selectedCategory?.let { category ->
        productService.state.value?.categories?.indexOfFirst { it.id == category.id } ?: 0
    } ?: 0

    fun onCategoryChanged(index: Int) {
        productService.state.value?.categories?.get(index)?.let { category ->
            updateState { copy(selectedCategory = category) }
        }
    }

    private fun resetView() {
        updateState {
            copy(
                name = "",
                description = "",
                price = 0,
                cost = 0,
                active = true,
                imgUrl = null,
                selectedCategory = productService.state.value?.categories?.get(0),
                wrongName = false,
                wrongPrice = false,
                barcode = "",
                sku = "",
            )
        }
    }

    override fun saveItem(image: SharedImage?) {
        if (uiState.value.name.isBlank()) {
            updateState { copy(wrongName = true) }
            return
        }

        if (uiState.value.name.length > 100) {
            updateState { copy(wrongName = true) }
            return
        }

        if (uiState.value.price <= 0) {
            updateState { copy(wrongPrice = true) }
            return
        }


//        AnalyticsHelper.logEvent("item_created", AnalyticsHelper.getAnalyticsBundle().apply {
//            putString("item_name", state.name.value)
//        })

        val state = uiState.value
        
        // Check if user wants to save a personalized product but there are no active categories
        if (state.isPersonalizedProduct && state.saveProduct) {
            if (!hasActiveCategories()) {
                // No active categories - show alert (covers both no categories and no active categories)
                updateState { copy(showNoCategoryAlert = true) }
                return
            }
        }
        
        // Check if this is a personalized product and should not be saved
        if (state.isPersonalizedProduct && !state.saveProduct) {
            // Create personalized product with id -1 (not saved to DB)
            viewModelScope.launch {
                showLoading()
                val price: Double = state.price / 100.0
                val cost: Double = state.cost / 100.0
                var imageUrl = ""
                if (image != null) {
                    val imageData = withContext(Dispatchers.Default) {
                        image.toByteArray()
                    }
                    if (imageData != null) {
                        withContext(Dispatchers.IO) {
                            imageUrl = uploadImageToBunnyCdn(
                                imageData = imageData,
                                "item_${randomUUID()}.jpg",
                                productService.state.value?.id.toString()
                            ) ?: ""
                        }
                    }
                }

                // ✅ Build OTI taxes list from UI state
                val otiTaxes = state.otiTaxes.mapNotNull {
                    val rate = it.rate.toDoubleOrNull()?.div(100) ?: 0.0
                    if (rate > 0) OTITax(id = it.code, rate = rate) else null
                }

                val personalizedItem = Item(
                    -1, // Special ID for personalized products
                    state.barcode,
                    state.sku,
                    state.name,
                    state.description,
                    imageUrl,
                    price,
                    cost,
                    state.active,
                    0, // order doesn't matter for personalized products
                    state.taxPercent,
                    ProductType.fromId(state.productTypeId),
                    state.unitMeasureCode,
                    state.iscRate?.toDoubleOrNull() ?: 0.0,
                    otiTaxes = otiTaxes,
                    state.isPharma,
                    state.additionalInfo.toJsonObject()
                )
                
                showSuccess()
                delay(600)
                emitEvent(ItemStateUiEvent.ReturnPersonalizedProduct(personalizedItem))
            }
            return
        }

        uiState.value.selectedCategory?.let { category ->
            viewModelScope.launch {
                showLoading()
                val price: Double = uiState.value.price / 100.0
                val cost: Double = uiState.value.cost / 100.0
                var imageUrl = ""
                if (image != null) {
                    val imageData = withContext(Dispatchers.Default) {
                        image.toByteArray()
                    }
                    if (imageData != null) {
                        withContext(Dispatchers.IO) {
                            imageUrl = uploadImageToBunnyCdn(
                                imageData = imageData,
                                "item_${randomUUID()}.jpg",
                                productService.state.value?.id.toString()
                            ) ?: ""
                        }
                    }
                }

                val state = uiState.value

                // ✅ Build OTI taxes list from UI state
                val otiTaxes = state.otiTaxes.mapNotNull {
                    val rate = it.rate.toDoubleOrNull()?.div(100) ?: 0.0
                    if (rate > 0) OTITax(id = it.code, rate = rate) else null
                }

                val newItem = Item(
                    -1,
                    state.barcode,
                    state.sku,
                    state.name,
                    state.description,
                    imageUrl,
                    price,
                    cost,
                    state.active,
                    category.getLastItemOrder() + 1,
                    state.taxPercent,
                    ProductType.fromId(state.productTypeId),
                    state.unitMeasureCode,
                    state.iscRate?.toDoubleOrNull() ?: 0.0,
                    otiTaxes = otiTaxes,
                    state.isPharma,
                    state.additionalInfo.toJsonObject()
                )
                try {
                    val savedItem = productService.addItem(newItem, category.id)
                    if (savedItem.itemId > 0) {
                        showSuccess()
                        delay(600)
                        if (state.isPersonalizedProduct) {
                            // If it was a personalized product but user chose to save it, go back
                            emitEvent(ItemStateUiEvent.ReturnPersonalizedProduct(savedItem))
                        } else {
                            resetView()
                            emitEvent(ItemStateUiEvent.GoBack)
                        }
                    } else {
                        // TODO Add logs
                        showError()
                    }
                } catch (e: Exception) {
                    // TODO Add logs
                    showError()
                }
            }
        } ?: run {
            viewModelScope.launch {
                emitEvent(ItemStateUiEvent.GoBack)
            }
        }

    }

    fun hideNoCategoryAlert() {
        updateState { copy(showNoCategoryAlert = false) }
    }
    
    fun hasActiveCategories(): Boolean {
        val categories = productService.state.value?.categories ?: emptyList()
        return categories.any { it.active }
    }
    
    fun hasAnyCategories(): Boolean {
        val categories = productService.state.value?.categories ?: emptyList()
        return categories.isNotEmpty()
    }

}