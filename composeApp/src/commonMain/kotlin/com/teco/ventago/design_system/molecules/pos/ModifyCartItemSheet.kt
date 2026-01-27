package com.teco.ventago.design_system.molecules.pos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
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
import com.teco.ventago.design_system.theme.titleLarge
import com.teco.ventago.design_system.theme.titleMedium
import com.teco.ventago.features.pos.domain.models.CartLine
import com.teco.ventago.features.pos.domain.models.Discount
import com.teco.ventago.utils.formatNumberToMoney
import com.teco.ventago.utils.toLongCents
import kotlin.math.max

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
        newUnitPriceCents: Long,    // BEFORE discount
        qty: Int,
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
    var qty by remember { mutableStateOf(kotlin.math.max(1, itemToModify.quantity)) }
    var qtyText by rememberSaveable { mutableStateOf(kotlin.math.max(1, itemToModify.quantity).toString()) }
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
    val lineTotalCents = discountedUnitCents * qty

    ModalBottomSheet(
        containerColor = cardContainerColor(),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
        ) {
            // Header with close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(itemToModify.name, style = titleLarge())
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Cerrar",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Divider()

            // Scrollable content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {

                // Unit price (raw cents digits)
                DMMoneyOutlinedTextField(
                    text = unitPriceRaw,
                    label = "Precio unitario ($currencySymbol)",
                    onChange = { unitPriceRaw = it.filter(Char::isDigit) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    imeAction = ImeAction.Done,
                    maxLines = 1,
                    leadingIcon = null
                )

                // Quantity
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Cantidad", style = labelMedium(), modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = {
                            val newQty = kotlin.math.max(1, qty - 1)
                            qty = newQty
                            qtyText = newQty.toString()
                        }) {
                            Icon(Icons.Rounded.Remove, contentDescription = "Disminuir")
                        }
                        DMOutlinedTextField(
                            text = qtyText,
                            label = "",
                            onChange = {
                                qtyText = it.filter(Char::isDigit).take(6)
                                qty = qtyText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                            },
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                            modifier = Modifier.width(80.dp),
                            maxLines = 1
                        )
                        IconButton(onClick = {
                            qty += 1
                            qtyText = qty.toString()
                        }) {
                            Icon(Icons.Rounded.Add, contentDescription = "Incrementar")
                        }
                    }
                }

                // Discount selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        modifier = Modifier.weight(1f),
                        selected = mode == DiscountMode.NONE,
                        onClick = { mode = DiscountMode.NONE },
                        label = {
                            Text(
                                "Sin desc.",
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    )
                    FilterChip(
                        modifier = Modifier.weight(1f),
                        selected = mode == DiscountMode.PERCENT,
                        onClick = { mode = DiscountMode.PERCENT },
                        label = {
                            Text(
                                "%",
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    )
                    FilterChip(
                        modifier = Modifier.weight(1f),
                        selected = mode == DiscountMode.FIXED,
                        onClick = { mode = DiscountMode.FIXED },
                        label = {
                            Text(
                                "Fijo",
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    )
                }

                when (mode) {
                    DiscountMode.PERCENT -> {
                        DMOutlinedTextField(
                            text = percentText,
                            label = "Descuento (%)",
                            onChange = { percentText = it.filter(Char::isDigit).take(3) },
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            maxLines = 1
                        )
                    }

                    DiscountMode.FIXED -> {
                        DMMoneyOutlinedTextField(
                            text = fixedRaw,
                            label = "Descuento fijo ($currencySymbol)",
                            onChange = { fixedRaw = it.filter(Char::isDigit) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            imeAction = ImeAction.Done,
                            maxLines = 1,
                            leadingIcon = null
                        )
                    }

                    else -> Unit
                }

                // =========================
                // Collapsible: Extra fields
                // =========================
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
                    globalInsurance = invoiceHasGlobalInsurance
                )

                // Preview
                Text(
                    "Resumen",
                    style = labelMediumBold(),
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
                Text(
                    text = "$currencySymbol${formatNumberToMoney((discountedUnitCents / 100.0).toString())} × $qty",
                    style = bodyMedium()
                )
                Text(
                    text = "Total: $currencySymbol${formatNumberToMoney((lineTotalCents / 100.0).toString())}",
                    style = headlineMediumBold(color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(Modifier.width(8.dp))
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

                            onApply(
                                unitPriceCents,
                                qty,
                                mode,
                                discountValue,
                                shippingCents,
                                insuranceCents,
                                batchNumOut,
                                batchQtyOut
                            )
                            onDismiss()
                        }
                    ) { Text("Aplicar") }
                }
            }
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
    globalInsurance: Boolean
) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 12.dp)
            .animateContentSize()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Información adicional", style = MaterialTheme.typography.titleMedium)
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = null,
                modifier = Modifier.rotate(rotation)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        onChange = onBatchNumber,
                        maxLines = 1,
                        imeAction = ImeAction.Next
                    )
                    DMOutlinedTextField(
                        text = batchQtyText,
                        label = "Cantidad del lote",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
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