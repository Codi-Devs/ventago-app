package com.teco.ventago.design_system.molecules.pos

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.teco.ventago.design_system.textfields.DMMoneyOutlinedTextField
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.headlineMediumBold
import com.teco.ventago.design_system.theme.labelMediumBold
import com.teco.ventago.design_system.theme.titleLarge
import com.teco.ventago.features.pos.domain.models.Money
import com.teco.ventago.utils.formatNumberToMoney

@Stable
enum class GlobalDiscountMode { NONE, PERCENT, FIXED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalInvoiceSheet(
    currencySymbol: String,
    subtotalCents: Money,                // current invoice subtotal (before global discount/charges)
    // exclusivity flags coming from cart state:
    anyItemHasShipping: Boolean,        // disables global shipping if true
    anyItemHasInsurance: Boolean,       // disables global insurance if true

    // initial values
    initialMode: GlobalDiscountMode = GlobalDiscountMode.NONE,
    initialPercent: Int = 0,            // 0..100, only used if mode == PERCENT
    initialFixedCents: Money = 0L,       // only used if mode == FIXED
    initialGlobalShippingCents: Money? = null,
    initialGlobalInsuranceCents: Money? = null,
    initialGlobalOtherCents: Money? = null,

    onDismiss: () -> Unit,
    onApply: (
        mode: GlobalDiscountMode,
        discountValue: Money,            // PERCENT: 0..100, FIXED: cents, NONE: 0
        globalShippingCents: Money?,     // null if disabled/blank/0
        globalInsuranceCents: Money?,    // null if disabled/blank/0
        globalOtherCents: Money?         // null if blank/0
    ) -> Unit
) {
    // money helpers (keep raw digit strings = cents)
    fun centsToRaw(c: Long) = c.coerceAtLeast(0).toString()
    fun rawToCents(raw: String) = raw.filter(Char::isDigit).toLongOrNull() ?: 0L
    
    // Strip leading zeros from money input, but keep at least 2 digits for decimal values
    fun sanitizeMoneyInput(raw: String): String {
        val digits = raw.filter(Char::isDigit)
        if (digits.isEmpty()) return ""
        // If it's 2 digits or less, keep as is (for values like 0.59)
        if (digits.length <= 2) return digits
        // Otherwise, strip leading zeros
        return digits.trimStart('0').ifEmpty { "0" }
    }

    var mode by rememberSaveable { mutableStateOf(initialMode) }
    var percentText by rememberSaveable { mutableStateOf(initialPercent.coerceIn(0, 100).toString()) }
    var fixedRaw by rememberSaveable { mutableStateOf(centsToRaw(initialFixedCents)) }

    var shippingRaw by rememberSaveable { mutableStateOf(centsToRaw(initialGlobalShippingCents ?: 0L)) }
    var insuranceRaw by rememberSaveable { mutableStateOf(centsToRaw(initialGlobalInsuranceCents ?: 0L)) }
    var otherRaw by rememberSaveable { mutableStateOf(centsToRaw(initialGlobalOtherCents ?: 0L)) }

    val discountValuePct = remember(percentText) { percentText.filter(Char::isDigit).toIntOrNull()?.coerceIn(0, 100) ?: 0 }
    val discountFixed = remember(fixedRaw) { rawToCents(fixedRaw) }

    val globalShipping = if (anyItemHasShipping) 0L else rawToCents(shippingRaw)
    val globalInsurance = if (anyItemHasInsurance) 0L else rawToCents(insuranceRaw)
    val globalOther = rawToCents(otherRaw)

    // Derived totals (simple preview)
    val discountCents = when (mode) {
        GlobalDiscountMode.NONE -> 0L
        GlobalDiscountMode.PERCENT -> (subtotalCents * discountValuePct) / 100
        GlobalDiscountMode.FIXED -> discountFixed.coerceAtMost(subtotalCents)
    }
    val chargesCents = globalShipping + globalInsurance + globalOther
    val previewTotal = (subtotalCents - discountCents + chargesCents).coerceAtLeast(0L)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = cardContainerColor(),
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
                Text(
                    "Ajustes globales de la factura",
                    style = titleLarge()
                )
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
                // -------- Global Discount --------
                Text("Descuento global", style = labelMediumBold())
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        modifier = Modifier.weight(1f),
                        selected = mode == GlobalDiscountMode.NONE,
                        onClick = { mode = GlobalDiscountMode.NONE },
                        label = { Text("Sin desc.", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                    )
                    FilterChip(
                        modifier = Modifier.weight(1f),
                        selected = mode == GlobalDiscountMode.PERCENT,
                        onClick = { mode = GlobalDiscountMode.PERCENT },
                        label = { Text("%", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                    )
                    FilterChip(
                        modifier = Modifier.weight(1f),
                        selected = mode == GlobalDiscountMode.FIXED,
                        onClick = { mode = GlobalDiscountMode.FIXED },
                        label = { Text("Fijo", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                    )
                }

                when (mode) {
                    GlobalDiscountMode.PERCENT -> {
                        DMOutlinedTextField(
                            text = percentText,
                            label = "Descuento (%)",
                            onChange = { percentText = it.filter(Char::isDigit).take(3) },
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 16.dp),
                            maxLines = 1
                        )
                    }
                    GlobalDiscountMode.FIXED -> {
                        DMMoneyOutlinedTextField(
                            text = fixedRaw,
                            label = "Descuento fijo ($currencySymbol)",
                            onChange = { fixedRaw = sanitizeMoneyInput(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 16.dp),
                            imeAction = ImeAction.Done,
                            maxLines = 1,
                            leadingIcon = null
                        )
                    }
                    else -> Spacer(Modifier.height(8.dp))
                }

                // -------- Global Charges --------
                Text("Cargos globales", style = labelMediumBold())
                Spacer(Modifier.height(8.dp))

                // Shipping (Acarreo) — disabled if any item uses shipping
                DMMoneyOutlinedTextField(
                    text = shippingRaw,
                    label = "Acarreo global ($currencySymbol)",
                    onChange = { shippingRaw = sanitizeMoneyInput(it) },
                    enabled = !anyItemHasShipping,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    imeAction = ImeAction.Next,
                    maxLines = 1,
                    leadingIcon = null,
                    supportingText = if (anyItemHasShipping) "Deshabilitado: hay ítems con Acarreo individual." else ""
                )

                // Insurance — disabled if any item uses insurance
                DMMoneyOutlinedTextField(
                    text = insuranceRaw,
                    label = "Seguro global ($currencySymbol)",
                    onChange = { insuranceRaw = sanitizeMoneyInput(it) },
                    enabled = !anyItemHasInsurance,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    imeAction = ImeAction.Next,
                    maxLines = 1,
                    leadingIcon = null,
                    supportingText = if (anyItemHasInsurance) "Deshabilitado: hay ítems con Seguro individual." else ""
                )

                // Other charges (always allowed)
                DMMoneyOutlinedTextField(
                    text = otherRaw,
                    label = "Otros cargos ($currencySymbol)",
                    onChange = { otherRaw = sanitizeMoneyInput(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    imeAction = ImeAction.Done,
                    maxLines = 1,
                    leadingIcon = null
                )

                // -------- Preview --------
                Divider()
                Spacer(Modifier.height(8.dp))
                Text("Resumen", style = labelMediumBold())
                Text("Subtotal: $currencySymbol${formatNumberToMoney((subtotalCents / 100.0).toString())}", style = bodyMedium())
                if (discountCents > 0) {
                    val dLabel = when (mode) {
                        GlobalDiscountMode.PERCENT -> "Descuento (${discountValuePct}%)"
                        GlobalDiscountMode.FIXED   -> "Descuento fijo"
                        else -> "Descuento"
                    }
                    Text("$dLabel: -$currencySymbol${formatNumberToMoney((discountCents / 100.0).toString())}", style = bodyMedium())
                }
                if (globalShipping > 0) {
                    Text("Acarreo: +$currencySymbol${formatNumberToMoney((globalShipping / 100.0).toString())}", style = bodyMedium())
                }
                if (globalInsurance > 0) {
                    Text("Seguro: +$currencySymbol${formatNumberToMoney((globalInsurance / 100.0).toString())}", style = bodyMedium())
                }
                if (globalOther > 0) {
                    Text("Otros cargos: +$currencySymbol${formatNumberToMoney((globalOther / 100.0).toString())}", style = bodyMedium())
                }
                Text(
                    "Total estimado: $currencySymbol${formatNumberToMoney((previewTotal / 100.0).toString())}",
                    style = headlineMediumBold(color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
                )

                Spacer(Modifier.height(16.dp))
                
                // Fixed bottom buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val outDiscount = when (mode) {
                                GlobalDiscountMode.NONE -> 0L
                                GlobalDiscountMode.PERCENT -> discountValuePct.toLong()
                                GlobalDiscountMode.FIXED -> discountFixed
                            }
                            val outShipping = if (anyItemHasShipping) null else rawToCents(shippingRaw).takeIf { it > 0 }
                            val outInsurance = if (anyItemHasInsurance) null else rawToCents(insuranceRaw).takeIf { it > 0 }
                            val outOther = rawToCents(otherRaw).takeIf { it > 0 }

                            onApply(
                                mode,
                                outDiscount,
                                outShipping,
                                outInsurance,
                                outOther
                            )
                            onDismiss()
                        }
                    ) { Text("Aplicar") }
                }
            }
        }
    }
}