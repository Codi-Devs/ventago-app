package com.teco.ventago.features.product.ui.item.add.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.core.camera.SharedImage
import com.teco.ventago.design_system.organism.LoadingState
import com.teco.ventago.features.product.domain.ProductService
import com.teco.ventago.features.product.domain.model.AdditionalInfoCatalog
import com.teco.ventago.features.product.domain.model.AdditionalInfoKey
import com.teco.ventago.features.product.domain.model.AdditionalValueType
import com.teco.ventago.features.product.domain.model.GoodsFamily
import com.teco.ventago.features.product.domain.model.GoodsSegment
import com.teco.ventago.features.product.domain.model.UomRegistry
import com.teco.ventago.features.settings.ui.settings.viewmodel.SettingsState
import com.teco.ventago.features.settings.ui.settings.viewmodel.SettingsStateUiEvent
import com.teco.ventago.utils.uploadImageToBunnyCdn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ventago.composeapp.generated.resources.Res

abstract class ItemViewModel(private val productService: ProductService) :
    BaseViewModel<ItemState, ItemStateUiEvent>(ItemState()) {

    fun onNameChange(name: String) {
        // Limit to 100 characters
        val limitedName = name.take(100)
        updateState {
            copy(
                wrongName = false,
                name = limitedName
            )
        }
    }

    fun onPriceChange(price: Long) {
        updateState {
            copy(
                wrongPrice = false,
                price = price
            )
        }
    }

    fun onCostChange(cost: Long) {
        updateState {
            copy(
                cost = cost
            )
        }
    }

    fun onBarcodeChange(barcode: String) {
        updateState {
            copy(
                barcode = barcode
            )
        }
    }

    fun onSkuChange(sku: String) {
        updateState {
            copy(
                sku = sku
            )
        }
    }

    fun onTaxPercentChange(taxPercent: Int) {
        updateState {
            copy(
                taxPercent = taxPercent
            )
        }
    }

    fun onProductTypeChange(productTypeId: Int) {
        updateState {
            copy(
                productTypeId = productTypeId
            )
        }
    }

    fun onDescriptionChange(description: String) {
        updateState {
            copy(
                description = description,
            )
        }
    }

    fun onActiveChange(active: Boolean) {
        updateState {
            copy(
                active = active,
            )
        }
    }

    abstract fun saveItem(image: SharedImage?)

    fun showScanner(showScanner: Boolean) {
        updateState { copy(showScanner = showScanner) }
    }

    fun uomOptions(): List<String> {
        // Show all codes in a readable way (e.g., "und - Unidad", "kg - Kilogramo")
        return UomRegistry.all().map { "${it.code} - ${it.nameEs}" }
    }

    fun selectedUomIndex(): Int {
        // Find the position of the currently selected code
        val currentCode = uiState.value.unitMeasureCode.ifBlank { "und" } // default
        return UomRegistry.all().indexOfFirst { it.code == currentCode }.takeIf { it >= 0 } ?: 0
    }

    fun onUomSelected(code: String) {
        updateState { copy(unitMeasureCode = code) }
    }

    fun onIscRateChange(input: String) {
        updateState {
            copy(iscRate = sanitizeDecimalInput(input))
        }
    }

    fun onOtiTypeSelected(index: Int) {
        updateState { copy(selectedOtiIndex = index) }
    }

    fun onOtiRateChanged(value: String) {
        // Sanitize: replace comma with dot, ensure only one decimal point
        val replaced = value.replace(',', '.')
        val sanitized = buildString {
            var dotSeen = false
            for (char in replaced) {
                when {
                    char.isDigit() -> append(char)
                    char == '.' && !dotSeen -> {
                        append(char)
                        dotSeen = true
                    }
                }
            }
        }
        updateState { copy(otiRateInput = sanitized) }
    }

    fun onAddOtiTax() {
        val idx = uiState.value.selectedOtiIndex
        val rate = uiState.value.otiRateInput
        if (idx < 0 || rate.isBlank()) return

        val otiNames = listOf(
            "SUME911",
            "Portabilidad Numérica",
            "Seguro 5%",
            "ATTT Seguro Autos 1%",
            "Tasa Salida Aeropuerto FZ",
            "Cargo Incentivo F3",
            "Cargo Seguridad AH",
            "Otros Cargos XT",
            "Combustible YQ",
            "FECI",
            "Intereses"
        )

        val otiCodes = listOf(
            "01","02","03","04","05","06","07","08","09","10","11"
        )

        val newTax = OTITaxUI(
            code = otiCodes[idx],
            name = otiNames[idx],
            rate = rate
        )

        updateState {
            copy(
                otiTaxes = otiTaxes + newTax,
                otiRateInput = "",
                selectedOtiIndex = -1
            )
        }
    }

    fun onRemoveOtiTax(index: Int) {
        updateState {
            copy(otiTaxes = otiTaxes.toMutableList().apply { removeAt(index) })
        }
    }

    private fun sanitizeDecimalInput(raw: String): String {
        val filtered = buildString {
            var dotSeen = false
            for (ch in raw.trim()) {
                when {
                    ch.isDigit() -> append(ch)
                    ch == '.' && !dotSeen -> { append('.'); dotSeen = true }
                }
            }
        }
        if (filtered.isEmpty()) return ""
        if (filtered == ".") return "0."
        // trim leading zeros but keep "0", "0.xxx"
        val parts = filtered.split('.', limit = 2)
        val intPart = parts[0].trimStart('0').ifEmpty { "0" }
        return if (parts.size == 2) "$intPart.${parts[1]}" else intPart
    }

    fun onAdditionalKeySelected(idx: Int) {
        val safeIdx = idx.coerceIn(0, AdditionalInfoCatalog.lastIndex)
        val vt = AdditionalInfoCatalog[safeIdx].valueType
        val defaultValue = when (vt) {
            AdditionalValueType.STRING -> ""
            AdditionalValueType.NUMBER -> ""
            AdditionalValueType.DATE   -> "" // e.g., prefill with today in "AAAA-MM-DD" if you want
        }
        updateState {
            copy(additionalSelectedKeyIndex = safeIdx, additionalInputValue = defaultValue)
        }
    }

    fun onAdditionalValueChanged(v: String) {
        val vt = AdditionalInfoCatalog[uiState.value.additionalSelectedKeyIndex].valueType
        val sanitized = when (vt) {
            AdditionalValueType.STRING -> v
            AdditionalValueType.NUMBER -> sanitizeDecimalInput(v)
            AdditionalValueType.DATE   -> v.trim() // optionally enforce YYYY-MM-DD with a validator
        }
        updateState { copy(additionalInputValue = sanitized) }
    }

    fun onAddAdditionalInfo() {
        val current = uiState.value
        val key = AdditionalInfoCatalog[current.additionalSelectedKeyIndex]
        val value = current.additionalInputValue.trim()

        // basic validation per type
        if (value.isEmpty()) return

        if (key.valueType == AdditionalValueType.DATE && !value.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) {
            // TODO: expose validation error to UI if needed
            return
        }

        // avoid duplicates (replace existing with same key)
        val withoutSame = current.additionalInfo.filterNot { it.keyName == key.keyName }
        val newList = withoutSame + AdditionalEntryUI(keyName = key.keyName, title = key.title, rawValue = value)

        updateState {
            copy(
                additionalInfo = newList,
                // reset input after add
                additionalInputValue = ""
            )
        }
    }

    fun onRemoveAdditionalInfo(idx: Int) {
        val list = uiState.value.additionalInfo.toMutableList()
        if (idx in list.indices) {
            list.removeAt(idx)
            updateState { copy(additionalInfo = list) }
        }
    }

    fun additionalInfoOptions(): List<String> {
        // What the user sees in the selector
        return AdditionalInfoCatalog.map { it.title }
    }

    /** Parallel list of expected value types (useful for rendering appropriate input). */
    fun additionalInfoValueTypes(): List<AdditionalValueType> {
        return AdditionalInfoCatalog.map { it.valueType }
    }

    fun currentAdditionalSelected(): AdditionalInfoKey =
        AdditionalInfoCatalog[uiState.value.additionalSelectedKeyIndex.coerceIn(0, AdditionalInfoCatalog.lastIndex)]


    fun setEditMode(mode: ItemEditMode) {
        updateState { copy(editMode = mode) }
    }

    fun toggleMode() {
        updateState { copy(editMode = if (editMode == ItemEditMode.BASIC) ItemEditMode.ADVANCED else ItemEditMode.BASIC) }
    }

    fun onOpenGoodsDialog() {
        updateState { copy(showGoodsDialog = true) }
    }

    fun onCloseGoodsDialog() {
        updateState { copy(showGoodsDialog = false) }
    }

    fun onSelectSegment(index: Int) {
        updateState { copy(selectedSegmentIndex = index, selectedFamilyIndex = -1) }
    }

    fun onSelectFamily(index: Int) {
        updateState { copy(selectedFamilyIndex = index) }
    }

    fun onConfirmGoodsSelection() {
        val state = uiState.value
        val seg = state.goodsSegments.getOrNull(state.selectedSegmentIndex)
        val fam = seg?.families?.getOrNull(state.selectedFamilyIndex)

        if (seg != null && fam != null) {
            val goodsCode = fam.code
            val goodsTitle = "${seg.description} - ${fam.description}"

            // Add automatically to additional info
            addOrReplaceAdditionalInfo(
                key = AdditionalInfoKey.PANAMA_GOODS_SERVICES_CODE,
                value = goodsCode,
                title = goodsTitle
            )
            addOrReplaceAdditionalInfo(
                key = AdditionalInfoKey.PANAMA_GOODS_SERVICES_UNIT_CODE,
                value = uiState.value.unitMeasureCode,
                title = "Unidad: ${uiState.value.unitMeasureCode}"
            )
        }

        updateState { copy(showGoodsDialog = false) }
    }

    private fun addOrReplaceAdditionalInfo(key: AdditionalInfoKey, value: String, title: String) {
        val current = uiState.value.additionalInfo.toMutableList()
        val idx = current.indexOfFirst { it.keyName == key.keyName }
        val entry = AdditionalEntryUI(keyName = key.keyName, title = title, rawValue = value)
        if (idx >= 0) current[idx] = entry else current.add(entry)
        updateState { copy(additionalInfo = current) }
    }

    fun loadGoodsCsv() {
        viewModelScope.launch(Dispatchers.IO) {

            val bytes = Res.readBytes("files/cat_panama_goods_and_services.csv")
            val content = bytes.decodeToString()
            val lines = content.lines().filter { it.isNotBlank() }.drop(1) // skip header

            val segmentMap = mutableMapOf<String, MutableList<GoodsFamily>>()
            val segmentNames = mutableMapOf<String, String>()

            println("ASDASD: Loading goods csv. lines: ${lines.size}")

            for (line in lines) {
                val parts = line.split(";").map { it.trim() }
                if (parts.size >= 3) {
                    val segmentCode = parts[1]
                    val segmentDesc = parts[2].replace("\"", "")
                    val familyCode = parts[3]
                    val familyDesc = parts.getOrNull(4).orEmpty().replace("\"", "")

                    segmentMap.getOrPut(segmentCode) { mutableListOf() }
                        .add(GoodsFamily(familyCode, familyDesc))
                    segmentNames[segmentCode] = segmentDesc
                }
            }
            println("ASDASD: Loading goods csv. segments: ${segmentMap.size}")


            val segments = segmentMap.map { (code, fams) ->
                GoodsSegment(code, segmentNames[code] ?: code, fams)
            }.sortedBy { it.code }

            withContext(Dispatchers.Main) {
                println("ASDASD: Segments: ${segments.size}")
                updateState { copy(goodsSegments = segments) }
            }
        }
    }

    fun setPersonalizedProductMode(isPersonalized: Boolean) {
        updateState { copy(isPersonalizedProduct = isPersonalized) }
    }

    fun onSaveProductChange(saveProduct: Boolean) {
        updateState { copy(saveProduct = saveProduct) }
    }

}