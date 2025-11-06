package com.teco.ventago.features.product.ui.item.add.viewmodel

import com.teco.ventago.core.LoadableState
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.features.product.domain.model.Category
import com.teco.ventago.features.product.domain.model.GoodsSegment
import com.teco.ventago.features.product.domain.model.Item
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

enum class ItemEditMode { BASIC, ADVANCED }
data class AdditionalEntryUI(
    val keyName: String,   // e.g., "fabrication_date"
    val title: String,     // human label, e.g., "Fecha de fabricación"
    val rawValue: String,  // what the user typed (stringified)
) {
    val displayValue: String get() = rawValue
}

fun List<AdditionalEntryUI>.toJsonObject(): JsonObject {
    val map = this.associate { it.keyName to JsonPrimitive(it.rawValue) }
    return JsonObject(map)
}

data class OTITaxUI(
    val code: String,
    val name: String,
    val rate: String
)

data class ItemState(
    val imgUrl: String? = null,
    val name: String = "",
    val description: String = "",
    val price: Long = 0,
    val cost: Long = 0,
    val taxPercent: Int = 0,
    val productTypeId: Int = 1,

    val barcode: String = "",
    val sku: String = "",
    val active: Boolean = true,
    val selectedCategory: Category? = null,
    val selectedItem: Item? = null,

    val wrongName: Boolean = false,
    val wrongPrice: Boolean = false,

    val showScanner: Boolean = false,
    val showPermissionRationalDialog: Boolean = false,

    // For the UOM dropdown
    val unitMeasureCode: String = "und",     // e.g., "und"
    val iscRate: String? = null,        // nullable numeric string
    val isPharma: Boolean = false,

    val otiTaxes: List<OTITaxUI> = emptyList(),
    val selectedOtiIndex: Int = -1,
    val otiRateInput: String = "",

// For the "Información adicional" editor
    val additionalInfo: List<AdditionalEntryUI> = emptyList<AdditionalEntryUI>(), // rendered list
    val additionalSelectedKeyIndex: Int = -1,   // picker position
    val additionalInputValue: String = "",      // raw text input

    val editMode: ItemEditMode = ItemEditMode.BASIC,


    val goodsSegments: List<GoodsSegment> = emptyList(),
    val selectedSegmentIndex: Int = -1,
    val selectedFamilyIndex: Int = -1,
    val showGoodsDialog: Boolean = false,

    // Unit measure dialog
    val showUnitMeasureDialog: Boolean = false,
    val selectedUnitMeasureIndex: Int = -1,

    // Personalized product mode
    val isPersonalizedProduct: Boolean = false,
    val saveProduct: Boolean = true,
    val showNoCategoryAlert: Boolean = false,

    override val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
) : LoadableState<ItemState> {
    override fun withLoading(state: LoadingBottomSheetState): ItemState {
        return copy(loadingBottomSheet = state)
    }
}

sealed class ItemStateUiEvent {
    data object GoBack : ItemStateUiEvent()
    data class ReturnPersonalizedProduct(val item: Item) : ItemStateUiEvent()

}