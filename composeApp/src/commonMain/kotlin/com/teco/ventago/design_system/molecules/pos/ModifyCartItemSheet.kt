package com.teco.ventago.design_system.molecules.pos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.textfields.DMMoneyOutlinedTextField
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.headlineMediumBold
import com.teco.ventago.design_system.theme.labelMedium
import com.teco.ventago.design_system.theme.labelMediumBold
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.design_system.theme.titleLarge
import com.teco.ventago.design_system.theme.titleMedium
import com.teco.ventago.features.pos.domain.models.CartLine
import com.teco.ventago.features.pos.domain.models.Discount
import com.teco.ventago.design_system.theme.Online
import com.teco.ventago.design_system.theme.RedLight
import com.teco.ventago.design_system.theme.WarningAmber
import com.teco.ventago.utils.formatNumberToMoney
import com.teco.ventago.utils.multiplyCentsByQuantity
import com.teco.ventago.utils.normalizeQuantity
import com.teco.ventago.utils.sanitizeQuantityInput
import com.teco.ventago.utils.toNormalizedQuantityOrNull
import com.teco.ventago.utils.toQuantityUiString
import kotlin.math.roundToInt

@Stable
enum class DiscountMode { NONE, PERCENT, FIXED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModifyCartItemSheet(
    itemToModify: CartLine,
    currencySymbol: String,
    invoiceHasGlobalShipping: Boolean,   // NEW: disables per-item shipping when true
    invoiceHasGlobalInsurance: Boolean,  // NEW: disables per-item insurance when true
    onDismiss: () -> Unit,
    onApply: (
        productName: String,
        newUnitPriceCents: Long,    // BEFORE discount
        qty: Double,
        discountMode: DiscountMode,
        discountValue: Long,        // PERCENT: 0..100, FIXED: cents, NONE: 0
        itemShippingCents: Long?,   // NEW: null if not set / disabled
        itemInsuranceCents: Long?,  // NEW: null if not set / disabled
        pharmaBatchNumber: String?, // NEW: only if isPharma
        pharmaBatchQty: Int?        // NEW: only if isPharma
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ------------ money helpers (store raw digits = cents) ------------
    fun centsToRawText(cents: Long): String = cents.coerceAtLeast(0).toString()
    fun rawTextToCents(raw: String): Long = raw.filter(Char::isDigit).toLongOrNull() ?: 0L

    // --- initial values from line ---
    var productName by rememberSaveable(itemToModify.lineId) { mutableStateOf(itemToModify.name) }
    var qty by remember { mutableStateOf(normalizeQuantity(itemToModify.quantity, minValue = 0.0001)) }
    var qtyText by rememberSaveable { mutableStateOf(normalizeQuantity(itemToModify.quantity, minValue = 0.0001).toQuantityUiString()) }
    val initialUnitPriceCents = itemToModify.overrideUnitPrice ?: itemToModify.baseUnitPrice
    var unitPriceRaw by rememberSaveable { mutableStateOf(centsToRawText(initialUnitPriceCents)) }
    var fixedRaw by rememberSaveable { mutableStateOf("") }

    var mode by rememberSaveable { mutableStateOf(DiscountMode.NONE) }
    var percentText by rememberSaveable { mutableStateOf("") } // "0".."100"

    // Prefill discount UI from current line
    LaunchedEffect(itemToModify.discount) {
        when (val d = itemToModify.discount) {
            is Discount.Percent -> {
                mode = DiscountMode.PERCENT
                percentText = d.bps.toString()
                fixedRaw = ""
            }

            is Discount.Amount -> {
                mode = DiscountMode.FIXED
                fixedRaw = centsToRawText(d.value)
                percentText = ""
            }

            null -> {
                mode = DiscountMode.NONE
                fixedRaw = ""
                percentText = ""
            }
        }
    }

    // --- Additional (per-item) fields -------------------------------
    var extraOpen by rememberSaveable { mutableStateOf(false) }

    // Per-item Shipping (Acarreo)
    val itemShippingPreset = itemToModify.shippingCents ?: 0L
    var itemShippingRaw by rememberSaveable { mutableStateOf(centsToRawText(itemShippingPreset)) }
    val itemShippingEnabled = !invoiceHasGlobalShipping

    // Per-item Insurance
    val itemInsurancePreset = itemToModify.insuranceCents ?: 0L
    var itemInsuranceRaw by rememberSaveable { mutableStateOf(centsToRawText(itemInsurancePreset)) }
    val itemInsuranceEnabled = !invoiceHasGlobalInsurance

    // Sync shipping and insurance values when item changes
    LaunchedEffect(itemToModify.lineId) {
        itemShippingRaw = centsToRawText(itemToModify.shippingCents ?: 0L)
        itemInsuranceRaw = centsToRawText(itemToModify.insuranceCents ?: 0L)
    }

    // Pharma
//    val isPharma = itemToModify.isPharma == true
    val isPharma = true
    var batchNumber by rememberSaveable { mutableStateOf("") }
//    var batchNumber by rememberSaveable { mutableStateOf(itemToModify.pharmaBatchNumber.orEmpty()) }
    var batchQtyText by rememberSaveable { mutableStateOf("") }
//    var batchQtyText by rememberSaveable { mutableStateOf(itemToModify.pharmaBatchQty?.toString().orEmpty()) }

    // Parsed values
    val unitPriceCents = remember(unitPriceRaw) { rawTextToCents(unitPriceRaw) }
    val fixedCents = remember(fixedRaw) { rawTextToCents(fixedRaw) }

    // Margin calculation: only for registered products (itemId > 0) with cost > 0
    val showMargin = itemToModify.itemId > 0
            && itemToModify.costCents != null
            && itemToModify.costCents > 0L
    val marginPercent = remember(unitPriceCents, itemToModify.costCents) {
        val price = unitPriceCents.toDouble()
        val cost = (itemToModify.costCents ?: 0L).toDouble()
        if (price > 0.0) ((price - cost) / price) * 100.0 else 0.0
    }
    val marginColor = when {
        unitPriceCents == 0L -> MaterialTheme.colorScheme.onSurfaceVariant
        marginPercent >= 30 -> Online
        marginPercent >= 15 -> WarningAmber
        else -> RedLight
    }
    val percent = remember(percentText) {
        percentText.filter(Char::isDigit).toIntOrNull()?.coerceIn(0, 100) ?: 0
    }

    // Discounted unit price (never below 0) - using per-unit discount calculation
    // For percentage: discountPerUnit = unitPrice × (percent / 100), rounded to 2 decimals
    // For fixed: discountPerUnit = min(fixedCents, unitPrice)
    val discountedUnitCents = remember(unitPriceCents, mode, percent, fixedCents) {
        val discountPerUnit = when (mode) {
            DiscountMode.NONE -> 0L
            DiscountMode.PERCENT -> {
                // Percentage discount per unit: unitPrice × (percent / 100)
                (unitPriceCents * percent) / 100L
            }
            DiscountMode.FIXED -> {
                // Fixed discount per unit: cap at unit price
                fixedCents.coerceAtMost(unitPriceCents).coerceAtLeast(0L)
            }
        }
        // Discounted unit price = unitPrice - discountPerUnit
        (unitPriceCents - discountPerUnit).coerceAtLeast(0L)
    }
    // Line total = discounted unit price × quantity
    val lineTotalCents = multiplyCentsByQuantity(discountedUnitCents, qty)
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val secondary = MaterialTheme.colorScheme.secondary
    val inputBounds = remember { mutableStateMapOf<String, Rect>() }
    var sheetBounds by remember { mutableStateOf<Rect?>(null) }
    var contentBounds by remember { mutableStateOf<Rect?>(null) }

    fun Modifier.trackInputBounds(key: String): Modifier = onGloballyPositioned {
        inputBounds[key] = it.boundsInRoot()
    }

    fun Modifier.clearKeyboardOnOutsideTap(containerBounds: Rect?): Modifier = pointerInput(
        containerBounds,
        inputBounds.size
    ) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val bounds = containerBounds ?: return@awaitEachGesture
            val rootTapPosition = Offset(
                x = bounds.left + down.position.x,
                y = bounds.top + down.position.y
            )
            val tappedInput = inputBounds.values.any { it.contains(rootTapPosition) }
            val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
            if (up != null && !tappedInput) {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
        }
    }

    ModalBottomSheet(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { sheetBounds = it.boundsInRoot() }
                .background(MaterialTheme.colorScheme.surface)
                .clearKeyboardOnOutsideTap(sheetBounds)
                .imePadding()
                .navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(width = 72.dp, height = 5.dp)
                        .background(secondary.copy(alpha = 0.18f), RoundedCornerShape(50))
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        productName.ifBlank { itemToModify.name },
                        style = titleLarge(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Editar producto",
                        style = titleMedium().copy(color = secondary.copy(alpha = 0.78f)),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Surface(
                    onClick = onDismiss,
                    shape = CircleShape,
                    color = secondary.copy(alpha = 0.08f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Cerrar",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onGloballyPositioned { contentBounds = it.boundsInRoot() }
                    .clearKeyboardOnOutsideTap(contentBounds)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                EditSheetSectionCard(
                    icon = Icons.Rounded.Edit,
                    title = "Nombre del producto"
                ) {
                    DMOutlinedTextField(
                        text = productName,
                        label = "Nombre",
                        onChange = { productName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .trackInputBounds("name"),
                        imeAction = ImeAction.Done,
                        maxLines = 1
                    )
                    if (productName.trim() != itemToModify.name.trim()) {
                        Text(
                            text = "Se aplicará como producto personalizado.",
                            style = labelMedium(),
                            color = secondary,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }

                EditSheetSectionCard(
                    icon = Icons.Rounded.AttachMoney,
                    title = "Precio unitario ($currencySymbol)"
                ) {
                    DMMoneyOutlinedTextField(
                        text = unitPriceRaw,
                        label = "",
                        onChange = { unitPriceRaw = it.filter(Char::isDigit) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .trackInputBounds("unitPrice"),
                        imeAction = ImeAction.Done,
                        maxLines = 1,
                        leadingIcon = null
                    )
                    if (showMargin) {
                        val costDisplay = formatNumberToMoney((itemToModify.costCents!! / 100.0).toString())
                        Text(
                            text = "Margen: ${marginPercent.roundToInt()}% (costo: $currencySymbol$costDisplay)",
                            style = labelMedium(),
                            color = marginColor,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }

                EditSheetSectionCard(
                    title = "Cantidad",
                    iconContent = {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = secondary
                        )
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        QuantityButton(onClick = {
                            val newQty = normalizeQuantity(qty - 1.0, minValue = 0.0001)
                            qty = newQty
                            qtyText = newQty.toQuantityUiString()
                        }) {
                            Icon(Icons.Rounded.Remove, contentDescription = "Disminuir")
                        }
                        OutlinedTextField(
                            value = qtyText,
                            onValueChange = {
                                val sanitized = sanitizeQuantityInput(it)
                                qtyText = sanitized
                                qty = sanitized.toNormalizedQuantityOrNull(minValue = 0.0001) ?: qty
                            },
                            modifier = Modifier
                                .width(128.dp)
                                .padding(horizontal = 8.dp)
                                .trackInputBounds("quantity"),
                            textStyle = TextStyle(
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = secondary,
                                cursorColor = secondary
                            )
                        )
                        QuantityButton(onClick = {
                            val newQty = normalizeQuantity(qty + 1.0, minValue = 0.0001)
                            qty = newQty
                            qtyText = newQty.toQuantityUiString()
                        }) {
                            Icon(Icons.Rounded.Add, contentDescription = "Incrementar")
                        }
                    }
                }

                EditSheetSectionCard(
                    title = "Tipo de descuento",
                    subtitle = "Descuento por ítem",
                    iconContent = {
                        Text(
                            text = "%",
                            style = titleMediumBold(color = secondary)
                        )
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DiscountChip(
                            selected = mode == DiscountMode.NONE,
                            label = "Sin desc.",
                            onClick = { mode = DiscountMode.NONE },
                            modifier = Modifier.weight(1f)
                        )
                        DiscountChip(
                            selected = mode == DiscountMode.PERCENT,
                            label = "%",
                            onClick = { mode = DiscountMode.PERCENT },
                            modifier = Modifier.weight(1f)
                        )
                        DiscountChip(
                            selected = mode == DiscountMode.FIXED,
                            label = "Fijo",
                            onClick = { mode = DiscountMode.FIXED },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    when (mode) {
                        DiscountMode.PERCENT -> {
                            DMOutlinedTextField(
                                text = percentText,
                                label = "Descuento por ítem (%)",
                                onChange = { percentText = it.filter(Char::isDigit).take(3) },
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                                    .trackInputBounds("discountPercent"),
                                maxLines = 1
                            )
                        }

                        DiscountMode.FIXED -> {
                            DMMoneyOutlinedTextField(
                                text = fixedRaw,
                                label = "Descuento por ítem ($currencySymbol)",
                                onChange = { fixedRaw = it.filter(Char::isDigit) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                                    .trackInputBounds("discountFixed"),
                                imeAction = ImeAction.Done,
                                maxLines = 1,
                                leadingIcon = null
                            )
                        }

                        else -> Unit
                    }
                }

                AdditionalItemInfoCard(
                    expanded = extraOpen,
                    onToggle = { extraOpen = !extraOpen },
                    currencySymbol = currencySymbol,
                    // Shipping
                    shippingEnabled = itemShippingEnabled,
                    shippingRaw = itemShippingRaw,
                    onShippingRaw = { itemShippingRaw = it.filter(Char::isDigit) },
                    // Insurance
                    insuranceEnabled = itemInsuranceEnabled,
                    insuranceRaw = itemInsuranceRaw,
                    onInsuranceRaw = { itemInsuranceRaw = it.filter(Char::isDigit) },
                    // Pharma
                    isPharma = isPharma,
                    batchNumber = batchNumber,
                    onBatchNumber = { batchNumber = it },
                    batchQtyText = batchQtyText,
                    onBatchQtyText = { batchQtyText = it.filter(Char::isDigit) },
                    globalShipping = invoiceHasGlobalShipping,
                    globalInsurance = invoiceHasGlobalInsurance,
                    trackInputModifier = { key, modifier -> modifier.trackInputBounds(key) }
                )

                SummaryCard(
                    currencySymbol = currencySymbol,
                    discountedUnitCents = discountedUnitCents,
                    quantity = qty,
                    lineTotalCents = lineTotalCents
                )

                Spacer(Modifier.height(8.dp))
            }

            Surface(
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar", color = secondary)
                    }
                    Button(
                        onClick = {
                            val discountValue = when (mode) {
                                DiscountMode.NONE -> 0L
                                DiscountMode.PERCENT -> percent.toLong()
                                DiscountMode.FIXED -> fixedCents
                            }

                            // Respect global/disabled: if disabled or zero/blank -> null
                            val shippingCents =
                                if (itemShippingEnabled) rawTextToCents(itemShippingRaw).takeIf { it > 0 } else null
                            val insuranceCents =
                                if (itemInsuranceEnabled) rawTextToCents(itemInsuranceRaw).takeIf { it > 0 } else null

                            val batchQty = batchQtyText.toIntOrNull()
                            val batchNumOut =
                                if (isPharma && batchNumber.isNotBlank()) batchNumber else null
                            val batchQtyOut = if (isPharma) batchQty else null
                            val quantityOut = qtyText.toNormalizedQuantityOrNull(minValue = 0.0001) ?: qty

                            onApply(
                                productName.trim().ifBlank { itemToModify.name },
                                unitPriceCents,
                                quantityOut,
                                mode,
                                discountValue,
                                shippingCents,
                                insuranceCents,
                                batchNumOut,
                                batchQtyOut
                            )
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.7f)
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = secondary)
                    ) { Text("Aplicar") }
                }
            }
        }
    }
}

@Composable
private fun EditSheetSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconContent: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)),
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EditSheetIconBadge(icon = icon, content = iconContent)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = titleMedium().copy(color = MaterialTheme.colorScheme.onSurface))
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            subtitle,
                            style = labelMedium(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun EditSheetIconBadge(
    icon: ImageVector?,
    content: @Composable (() -> Unit)?
) {
    val secondary = MaterialTheme.colorScheme.secondary
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = secondary.copy(alpha = 0.08f),
        modifier = Modifier.size(38.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            when {
                content != null -> content()
                icon != null -> Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = secondary
                )
            }
        }
    }
}

@Composable
private fun QuantityButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
        modifier = Modifier.size(48.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            content()
        }
    }
}

@Composable
private fun DiscountChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val secondary = MaterialTheme.colorScheme.secondary
    FilterChip(
        modifier = modifier.height(46.dp),
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                label,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = secondary.copy(alpha = 0.22f),
            selectedBorderColor = secondary
        ),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = secondary,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
            containerColor = Color.Transparent,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
private fun SummaryCard(
    currencySymbol: String,
    discountedUnitCents: Long,
    quantity: Double,
    lineTotalCents: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Resumen",
                style = labelMediumBold().copy(color = MaterialTheme.colorScheme.secondary)
            )
            Text(
                text = "$currencySymbol${formatNumberToMoney((discountedUnitCents / 100.0).toString())} x ${quantity.toQuantityUiString()}",
                style = bodyMedium(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            Divider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
            Text(
                text = "Total",
                style = titleMediumBold(color = MaterialTheme.colorScheme.onSurface)
            )
            Text(
                text = "$currencySymbol${formatNumberToMoney((lineTotalCents / 100.0).toString())}",
                style = headlineMediumBold(color = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun AdditionalItemInfoCard(
    expanded: Boolean,
    onToggle: () -> Unit,
    currencySymbol: String,
    // Shipping
    shippingEnabled: Boolean,
    shippingRaw: String,
    onShippingRaw: (String) -> Unit,
    // Insurance
    insuranceEnabled: Boolean,
    insuranceRaw: String,
    onInsuranceRaw: (String) -> Unit,
    // Pharma
    isPharma: Boolean,
    batchNumber: String,
    onBatchNumber: (String) -> Unit,
    batchQtyText: String,
    onBatchQtyText: (String) -> Unit,
    // Global flags (for helper notes)
    globalShipping: Boolean,
    globalInsurance: Boolean,
    trackInputModifier: (String, Modifier) -> Modifier = { _, modifier -> modifier }
) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                EditSheetIconBadge(
                    icon = Icons.Rounded.Description,
                    content = null
                )
                Spacer(Modifier.width(12.dp))
                Text("Información adicional", style = titleMedium())
            }
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = null,
                modifier = Modifier.rotate(rotation),
                tint = MaterialTheme.colorScheme.secondary
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                // Per-item Shipping
                DMMoneyOutlinedTextField(
                    text = shippingRaw,
                    label = "Acarreo del ítem ($currencySymbol)",
                    onChange = onShippingRaw,
                    enabled = shippingEnabled,
                    modifier = trackInputModifier(
                        "shipping",
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ),
                    imeAction = ImeAction.Next,
                    maxLines = 1,
                    leadingIcon = null,
                    supportingText = when {
                        !shippingEnabled && globalShipping ->
                            "Deshabilitado: hay un Acarreo global en la factura."
                        else -> ""
                    }
                )

                // Per-item Insurance
                DMMoneyOutlinedTextField(
                    text = insuranceRaw,
                    label = "Seguro del ítem ($currencySymbol)",
                    onChange = onInsuranceRaw,
                    enabled = insuranceEnabled,
                    modifier = trackInputModifier(
                        "insurance",
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ),
                    imeAction = ImeAction.Next,
                    maxLines = 1,
                    leadingIcon = null,
                    supportingText = when {
                        !insuranceEnabled && globalInsurance ->
                            "Deshabilitado: hay un Seguro global en la factura."
                        else -> ""
                    }
                )

                // Pharma
                if (isPharma) {
                    Spacer(Modifier.height(8.dp))
                    Text("Datos farmacéuticos", style = MaterialTheme.typography.titleSmall)
                    DMOutlinedTextField(
                        text = batchNumber,
                        label = "Lote (batch number)",
                        modifier = trackInputModifier(
                            "batchNumber",
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ),
                        onChange = onBatchNumber,
                        maxLines = 1,
                        imeAction = ImeAction.Next
                    )
                    DMOutlinedTextField(
                        text = batchQtyText,
                        label = "Cantidad del lote",
                        modifier = trackInputModifier(
                            "batchQty",
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ),
                        onChange = onBatchQtyText,
                        maxLines = 1,
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    )
                }
            }
        }
    }
}
