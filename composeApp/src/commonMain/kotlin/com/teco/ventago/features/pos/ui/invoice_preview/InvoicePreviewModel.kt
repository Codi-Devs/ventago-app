package com.teco.ventago.features.pos.ui.invoice_preview

import com.teco.ventago.features.branches.domain.model.Branch
import com.teco.ventago.features.business.domain.model.Business
import com.teco.ventago.features.customers.domain.models.CustomerTaxRetentionCatalog
import com.teco.ventago.features.pos.domain.models.CartLine
import com.teco.ventago.features.pos.ui.viewmodel.CartCalc
import com.teco.ventago.features.pos.ui.viewmodel.InstallmentUI
import com.teco.ventago.features.pos.ui.viewmodel.PosState
import com.teco.ventago.features.product.domain.model.Item
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt
import kotlin.math.roundToLong

data class InvoicePreview(
    val issuer: InvoicePreviewIssuer,
    val branch: InvoicePreviewBranch?,
    val receptor: InvoicePreviewReceptor,
    val meta: InvoicePreviewMeta,
    val items: List<InvoicePreviewItem>,
    val itbmsBreakdown: List<InvoicePreviewTaxBreakdown>,
    val payments: List<InvoicePreviewPayment>,
    val retention: InvoicePreviewRetention?,
    val totals: InvoicePreviewTotals,
    val bottomNote: InvoicePreviewBottomNote?,
)

data class InvoicePreviewIssuer(
    val name: String,
    val ruc: String,
    val address: String,
    val logoUrl: String,
)

data class InvoicePreviewBranch(
    val code: String,
    val name: String,
    val tradeName: String?,
    val addressLine: String,
    val logoUrl: String?,
    val billingPoint: String,
)

data class InvoicePreviewReceptor(
    val type: String,
    val name: String,
    val identification: String,
)

data class InvoicePreviewMeta(
    val invoiceTypeTitle: String,
    val number: String,
    val issuedAt: String,
    val billingPoint: String,
    val consultationUrl: String,
    val cufe: String,
)

data class InvoicePreviewItem(
    val number: Int,
    val description: String,
    val detail: String,
    val quantity: String,
    val unit: String,
    val unitPriceCents: Long,
    val discountPerUnitCents: Long,
    val itbmsAmountCents: Long,
    val totalCents: Long,
    val taxableBaseCents: Long,
    val taxRatePercent: Int,
)

data class InvoicePreviewTaxBreakdown(
    val baseCents: Long,
    val rateLabel: String,
    val taxCents: Long,
)

data class InvoicePreviewPayment(
    val label: String,
    val amountCents: Long,
)

data class InvoicePreviewRetention(
    val label: String,
    val amountCents: Long,
)

data class InvoicePreviewTotals(
    val netTotalCents: Long,
    val exemptItbmsCents: Long,
    val taxableItbmsCents: Long,
    val itbmsCents: Long,
    val iscCents: Long,
    val otiCents: Long,
    val totalTaxCents: Long,
    val discountCents: Long,
    val freightCents: Long,
    val insuranceCents: Long,
    val otherChargesCents: Long,
    val totalCents: Long,
)

data class InvoicePreviewBottomNote(
    val title: String,
    val body: String,
)

object InvoicePreviewBuilder {
    private const val CONSULTATION_URL = "https://dgi-fep.mef.gob.pa/Consultas/FacturasPorCUFE"

    fun build(
        state: PosState,
        business: Business?,
        issuedAt: LocalDateTime = Clock.System.now()
            .toLocalDateTime(TimeZone.of("America/Panama")),
    ): InvoicePreview {
        val previewItems = buildItems(state)
        val summary = CartCalc.summarize(state)
        val globalChargeParts = globalChargeParts(state)
        val selectedBranch = state.branches.getOrNull(state.selectedBranchIndex)
        val selectedBillingPoint = state.billingPoints
            .getOrNull(state.selectedBillingPointIndex)
            ?.billingPoint
            .orEmpty()
        val totals = InvoicePreviewTotals(
            netTotalCents = summary.subtotal,
            exemptItbmsCents = previewItems
                .filter { state.taxExempt || it.taxRatePercent == 0 }
                .sumOf { it.taxableBaseCents },
            taxableItbmsCents = previewItems
                .filter { !state.taxExempt && it.taxRatePercent > 0 }
                .sumOf { it.taxableBaseCents },
            itbmsCents = summary.itbms,
            iscCents = summary.isc,
            otiCents = summary.oti,
            totalTaxCents = summary.tax,
            discountCents = summary.globalDiscount,
            freightCents = globalChargeParts.freightCents,
            insuranceCents = globalChargeParts.insuranceCents,
            otherChargesCents = globalChargeParts.otherChargesCents,
            totalCents = summary.totalBeforeTip,
        )

        return InvoicePreview(
            issuer = InvoicePreviewIssuer(
                name = business?.name.orEmpty(),
                ruc = business?.ruc.orEmpty(),
                address = business?.address?.placeAddress
                    ?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                    ?: "DIRECCIÓN DEL NEGOCIO",
                logoUrl = resolveDocumentLogo(selectedBranch, business),
            ),
            branch = selectedBranch?.let { branch ->
                InvoicePreviewBranch(
                    code = branch.branchCode,
                    name = branch.name,
                    tradeName = branch.tradeName,
                    addressLine = branch.addressLine,
                    logoUrl = branch.logoUrl,
                    billingPoint = selectedBillingPoint,
                )
            },
            receptor = buildReceptor(state),
            meta = InvoicePreviewMeta(
                invoiceTypeTitle = invoiceTypeTitle(state.selectedDocType),
                number = "VISTA PREVIA",
                issuedAt = formatDateTime(issuedAt),
                billingPoint = selectedBillingPoint,
                consultationUrl = CONSULTATION_URL,
                cufe = "Pendiente - se genera al emitir la factura",
            ),
            items = previewItems,
            itbmsBreakdown = buildItbmsBreakdown(previewItems),
            payments = buildPayments(state),
            retention = buildRetention(state, totals.itbmsCents),
            totals = totals,
            bottomNote = buildBottomNote(state),
        )
    }

    private fun buildBottomNote(state: PosState): InvoicePreviewBottomNote? {
        val settings = state.bottomNoteSettings ?: return null
        if (state.includeBottomNote != true) return null
        val title = settings.title.trim()
        val body = htmlToPreviewText(settings.body)
        if (title.isBlank() || body.isBlank()) return null
        return InvoicePreviewBottomNote(title = title, body = body)
    }

    private fun buildReceptor(state: PosState): InvoicePreviewReceptor {
        return if (state.finalCustomer == false) {
            val customer = state.customer
            InvoicePreviewReceptor(
                type = "Contribuyente",
                name = customer?.name ?: "",
                identification = customer?.ruc?.takeIf { it.isNotBlank() } ?: "",
            )
        } else {
            InvoicePreviewReceptor(
                type = "Consumidor Final",
                name = state.finalName?.takeIf { it.isNotBlank() } ?: "CONSUMIDOR FINAL",
                identification = state.finalIdNumber?.takeIf { it.isNotBlank() } ?: "CF",
            )
        }
    }

    private fun buildItems(state: PosState): List<InvoicePreviewItem> {
        val itemsById = state.items.associateBy { it.itemId }
        return state.cart.mapIndexedNotNull { index, line ->
            val product = itemForLine(line, state, itemsById) ?: return@mapIndexedNotNull null
            val taxRatePercent = if (state.taxExempt) 0 else line.tax?.rateBps?.let { it / 100 } ?: product.taxPercent ?: 0
            InvoicePreviewItem(
                number = index + 1,
                description = line.name,
                detail = product.description.ifBlank { line.notes.orEmpty() },
                quantity = formatQuantity(line.quantity),
                unit = unitLabel(product.unitMeasureCode),
                unitPriceCents = line.unitPrice(),
                discountPerUnitCents = line.discountPerUnit(),
                itbmsAmountCents = line.taxTotal(state.taxExempt),
                totalCents = line.total(state.taxExempt),
                taxableBaseCents = line.lineSubtotal(),
                taxRatePercent = taxRatePercent,
            )
        }
    }

    private fun itemForLine(
        line: CartLine,
        state: PosState,
        itemsById: Map<Int, Item>,
    ): Item? {
        return if (line.itemId < 0) {
            state.personalizedItems[line.lineId]
        } else {
            itemsById[line.itemId]
        }
    }

    private fun buildItbmsBreakdown(items: List<InvoicePreviewItem>): List<InvoicePreviewTaxBreakdown> {
        return items
            .groupBy { it.taxRatePercent }
            .entries
            .sortedBy { it.key }
            .map { (rate, groupItems) ->
                InvoicePreviewTaxBreakdown(
                    baseCents = groupItems.sumOf { it.taxableBaseCents },
                    rateLabel = if (rate == 0) "Exento" else "$rate%",
                    taxCents = groupItems.sumOf { it.itbmsAmountCents },
                )
            }
    }

    private fun buildPayments(state: PosState): List<InvoicePreviewPayment> {
        val manualPayments = state.charged
            .entries
            .sortedBy { it.key }
            .map { (code, amount) ->
                InvoicePreviewPayment(
                    label = paymentMethodLabel(code),
                    amountCents = amount,
                )
            }
        val installments = state.installments.mapIndexed { index, installment ->
            InvoicePreviewPayment(
                label = installmentLabel(index, installment),
                amountCents = installment.amountCents,
            )
        }
        return manualPayments + installments
    }

    private fun buildRetention(state: PosState, itbmsCents: Long): InvoicePreviewRetention? {
        if (itbmsCents <= 0L) return null
        val option = CustomerTaxRetentionCatalog.options.getOrNull(state.retentionCodeIndex)
            ?: return null
        val normalizedCode = CustomerTaxRetentionCatalog.normalizeCode(option.code)
        if (normalizedCode.isEmpty()) return null
        val rate = when {
            option.defaultRate != null -> option.defaultRate.toDouble()
            normalizedCode == "8" -> state.retentionAmount.toDoubleOrNull()
            else -> null
        } ?: return null
        if (rate <= 0.0) return null
        val amountCents = (itbmsCents * rate / 100.0).roundToLong()
        if (amountCents <= 0L) return null
        return InvoicePreviewRetention(
            label = option.label,
            amountCents = amountCents,
        )
    }

    private data class GlobalChargeParts(
        val freightCents: Long,
        val insuranceCents: Long,
        val otherChargesCents: Long,
    )

    private fun globalChargeParts(state: PosState): GlobalChargeParts {
        val hasItemFreight = state.cart.any { (it.shippingCents ?: 0L) > 0L }
        val hasItemInsurance = state.cart.any { (it.insuranceCents ?: 0L) > 0L }
        return GlobalChargeParts(
            freightCents = if (hasItemFreight) 0L else state.globalShippingCents ?: 0L,
            insuranceCents = if (hasItemInsurance) 0L else state.globalInsuranceCents ?: 0L,
            otherChargesCents = state.globalOtherChargesCents ?: 0L,
        )
    }

    private fun invoiceTypeTitle(type: String): String {
        return when (type) {
            "01" -> "Factura de Operación Interna"
            "02" -> "Factura de Importación"
            "03" -> "Factura de Exportación"
            "04" -> "Nota de Crédito"
            "05" -> "Nota de Débito"
            "06" -> "Nota de Crédito Genérica"
            "08" -> "Factura de Zona Franca"
            "09" -> "Reembolso"
            "10" -> "Factura de Operación Extranjera"
            else -> "Factura de Operación Interna"
        }
    }

    private fun unitLabel(code: String): String {
        return when (code.uppercase()) {
            "UND" -> "und"
            "SER" -> "ser"
            "KG" -> "kg"
            "LB" -> "lb"
            "M" -> "m"
            "CM" -> "cm"
            "L" -> "lt"
            "GAL" -> "gal"
            "HRS" -> "hrs"
            "DIA" -> "dia"
            "MES" -> "mes"
            else -> code.ifBlank { "und" }
        }
    }

    private fun paymentMethodLabel(code: Int): String {
        return when (code) {
            2 -> "Efectivo"
            3 -> "Tarjeta credito"
            4 -> "Tarjeta debito"
            5 -> "Tarjeta fidelizacion"
            6 -> "Vale"
            7 -> "Tarjeta de regalo"
            8 -> "Transferencia bancaria"
            9 -> "Cheque"
            10 -> "Punto Pago"
            99 -> "Otro"
            else -> code.toString()
        }
    }

    private fun installmentLabel(index: Int, installment: InstallmentUI): String {
        return if (installment.dueDateIso.isBlank()) {
            "Credito/Plazo ${index + 1}"
        } else {
            "Credito/Plazo ${index + 1} - ${installment.dueDateIso}"
        }
    }

    private fun formatDateTime(value: LocalDateTime): String {
        return "${value.dayOfMonth.toString().padStart(2, '0')}-" +
            "${value.monthNumber.toString().padStart(2, '0')}-${value.year} " +
            "${value.hour.toString().padStart(2, '0')}:${value.minute.toString().padStart(2, '0')}"
    }

    private fun formatQuantity(value: Double): String {
        val rounded = (value * 100.0).roundToInt() / 100.0
        return rounded.toString().let { raw ->
            val parts = raw.split(".")
            val decimals = parts.getOrNull(1).orEmpty().padEnd(2, '0').take(2)
            "${parts.first()}.$decimals"
        }
    }

    private fun htmlToPreviewText(value: String): String {
        return value
            .replace(Regex("<\\s*br\\s*/?\\s*>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</\\s*(p|div|h[1-6]|blockquote|li|ol|ul)\\s*>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<\\s*li\\b[^>]*>", RegexOption.IGNORE_CASE), "- ")
            .replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }
}

fun resolveDocumentLogo(branch: Branch?, business: Business?): String {
    return branch?.logoUrl?.takeIf { it.isValidLogoValue() }
        ?: business?.logo?.takeIf { it.isValidLogoValue() }
        ?: ""
}

private fun String.isValidLogoValue(): Boolean {
    return isNotBlank() && !equals("null", ignoreCase = true)
}
