package com.teco.ventago.features.product.ui.item.edit

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.camera.SharedImage
import com.teco.ventago.features.product.domain.ProductService
import com.teco.ventago.features.product.domain.model.AdditionalInfoCatalog
import com.teco.ventago.features.product.domain.model.AdditionalInfoKey
import com.teco.ventago.features.product.domain.model.AdditionalValueType
import com.teco.ventago.features.product.domain.model.OTITax
import com.teco.ventago.features.product.domain.model.ProductType
import com.teco.ventago.features.product.domain.model.UomRegistry
import com.teco.ventago.features.product.domain.model.otiNameFromCode
import com.teco.ventago.features.product.ui.item.add.viewmodel.AdditionalEntryUI
import com.teco.ventago.features.product.ui.item.add.viewmodel.ItemEditMode
import com.teco.ventago.features.product.ui.item.add.viewmodel.ItemStateUiEvent
import com.teco.ventago.features.product.ui.item.add.viewmodel.ItemViewModel
import com.teco.ventago.features.product.ui.item.add.viewmodel.OTITaxUI
import com.teco.ventago.features.product.ui.item.add.viewmodel.toJsonObject
import com.teco.ventago.utils.randomUUID
import com.teco.ventago.utils.uploadImageToBunnyCdn
import com.teco.ventago.utils.formatTwoDecimals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive

class EditItemViewModel (
    private val productService: ProductService,
) : ItemViewModel(productService) {

    private val itemId: Int?

    init {
        loadGoodsCsv()
        val selectedCategoryId = productService.selectedCategoryId
        itemId = productService.selectedItemId
        if (selectedCategoryId == null || itemId == null) {
            viewModelScope.launch {
                emitEvent(ItemStateUiEvent.GoBack)
            }
        }

        viewModelScope.launch {
            productService.getMenu().onEach { menu ->
                menu?.let { _ ->
                    menu.categories.firstOrNull{ it.id == selectedCategoryId }?.let {
                        updateState { copy(selectedCategory = it) }
                        it.items.firstOrNull{ item -> item.itemId == itemId}?.let { item ->
                            val mappedAdditional = item.additionalInfo?.entries?.mapNotNull { (k, v) ->
                                val key = AdditionalInfoKey.fromKey(k) ?: return@mapNotNull null
                                val valueStr = when (v) {
                                    is JsonPrimitive -> v.content
                                    else -> v.toString()
                                }
                                AdditionalEntryUI(keyName = key.keyName, title = key.title, rawValue = valueStr)
                            } ?: emptyList()


                            // ✅ Map OTI taxes list from backend -> UI
                            val mappedOtiTaxes = item.otiTaxes?.map { oti ->
                                OTITaxUI(
                                    code = oti.id,
                                    name = otiNameFromCode(oti.id),
                                    rate = (oti.rate * 100).formatTwoDecimals() // convert decimal fraction to percentage string, rounded to 2 decimals
                                )
                            } ?: emptyList()

                            updateState {
                                copy(
                                    selectedItem = item,
                                    name = item.name,
                                    description = item.description,
                                    imgUrl = item.getImgUrl(),
                                    price = (item.price * 100).toLong(),
                                    cost = (item.cost?.times(100))?.toLong() ?: 0L,
                                    taxPercent = item.taxPercent?: 0,
                                    barcode = item.barcode?:"",
                                    sku = item.sku?:"",
                                    active = item.active,
                                    productTypeId = item.productType.typeId,
                                    unitMeasureCode = item.unitMeasureCode,
                                    iscRate = item.iscRate?.toString(),     // shown as text in the optional field
                                    otiTaxes = mappedOtiTaxes,
//                                    isPharma = item.isPharma,
                                    additionalInfo = mappedAdditional,      // List<AdditionalEntryUI>
                                    additionalSelectedKeyIndex = 0,                 // default selection for the picker
                                    additionalInputValue = ""                    // empty input until user edits
                                )
                            }

                            if (item.unitMeasureCode != "und" ||
                                item.iscRate != null ||
                                !(item.otiTaxes.isNullOrEmpty()) ||
                                !item.additionalInfo.isNullOrEmpty()
                            ) {
                                updateState { copy(editMode = ItemEditMode.ADVANCED) }
                            }
                        }?: run {
                            // TODO Add logs
                            emitEvent(ItemStateUiEvent.GoBack)
                        }
                    } ?: run {
                        // TODO Add logs
                        emitEvent(ItemStateUiEvent.GoBack)
                    }
                } ?: run {
                    // TODO Add logs
                    emitEvent(ItemStateUiEvent.GoBack)
                }
            }.launchIn(this)
        }
    }

    fun navigateUp() {
        productService.selectedItemId = null
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

//        AnalyticsHelper.logEvent("item_modified", AnalyticsHelper.getAnalyticsBundle().apply {
//            putString("item_name", state.name.value)
//        })

        uiState.value.selectedItem?.let { item ->
            viewModelScope.launch {
                showLoading()
                val price: Double = uiState.value.price / 100.0
                val cost: Double = uiState.value.cost / 100.0
                var imageUrl = uiState.value.imgUrl ?: ""
                if (image != null) {
                    val imageData = withContext(Dispatchers.Default) {
                        image.toByteArray()
                    }
                    if (imageData != null) {
                        withContext(Dispatchers.IO)  {
                            imageUrl = uploadImageToBunnyCdn(
                                imageData = imageData,
                                "item_${randomUUID()}.jpg",
                                productService.state.value?.id.toString()
                            ) ?: uiState.value.imgUrl ?: ""
                        }
                    }
                }


                val state = uiState.value

                // ✅ Build OTI taxes list from UI state
                val otiTaxes = state.otiTaxes.mapNotNull {
                    val rate = it.rate.toDoubleOrNull()?.div(100) ?: 0.0
                    if (rate > 0) OTITax(id = it.code, rate = rate) else null
                }
                try {
                    val categoryId = state.selectedCategory?.id
                        ?: productService.selectedCategoryId
                        ?: return@launch
                    
                    val res = productService.editItem(item.copy(
                        barcode = uiState.value.barcode,
                        sku = uiState.value.sku,
                        name = uiState.value.name,
                        description = uiState.value.description,
                        img = imageUrl,
                        price = price,
                        cost = cost,
                        active = uiState.value.active,
                        taxPercent = uiState.value.taxPercent,
                        productType = ProductType.fromId(uiState.value.productTypeId),
                        otiTaxes = otiTaxes,
                        isPharma = state.isPharma,
                        additionalInfo = state.additionalInfo.toJsonObject(),
                        unitMeasureCode = state.unitMeasureCode,
                        iscRate = state.iscRate?.toDoubleOrNull() ?: 0.0,
                    ), categoryId)

                    if (res) {
                        showSuccess()
                        delay(600)
                        emitEvent(ItemStateUiEvent.GoBack)
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

}