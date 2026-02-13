package com.teco.ventago.features.pos.ui.viewmodel

import com.teco.ventago.core.LoadableState
import com.teco.ventago.design_system.molecules.pos.GlobalDiscountMode
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.features.branches.domain.model.Branch
import com.teco.ventago.features.branches.domain.model.FiscalBillingPoint
import com.teco.ventago.features.customers.domain.models.CustomerAddress
import com.teco.ventago.features.customers.domain.models.Customer
import com.teco.ventago.features.customers.domain.models.CustomerListItem
import com.teco.ventago.features.invoicing.domain.models.InvoiceStatus

import com.teco.ventago.features.pos.domain.models.CartLine
import com.teco.ventago.features.pos.domain.models.Money
import com.teco.ventago.features.product.domain.model.Item
import com.teco.ventago.features.quotes.domain.models.QuoteSettings
import com.teco.ventago.utils.ViewState
import kotlin.math.roundToLong

enum class PaymentFlowMode { MANUAL_OR_INSTALLMENTS, PAYMENT_LINK }

data class InstallmentUI(
    val amountCents: Long = 0L,
    val dueDateIso: String = "" // "YYYY-MM-DD"
)

data class PosState(
    val items: List<Item> = listOf(),
    val cart: List<CartLine> = listOf(),
    val personalizedItems: Map<String, Item> = mapOf(), // Stores personalized products keyed by lineId (since all have itemId = -1)
    val taxExempt: Boolean = false,
    val currency: String = "USD",
    val currencySymbol: String = "$",
    val query: String = "",
    val tipAmount: Long = 0,
    val tipIsPercentage: Boolean = true,
    val charged: Map<Int, Long> = mapOf(),
    val freeTrialAvailable: Boolean = false,
    // TODO Oscar check if needed
    val creatingOrderState: ViewState = ViewState(),
    val customer: CustomerListItem? = null,
    val customerQuery: String = "",
    val paymentsConfigured: Boolean = false,
    val invoicingEnabled: Boolean = false,

    val branches: List<Branch> = listOf(),
    val selectedBranchIndex: Int = 0,

    val billingPoints: List<FiscalBillingPoint> = emptyList(),
    val selectedBillingPointIndex: Int = 0,

    // === Document Type ===
    val selectedDocTypeIndex: Int = 0,
    val selectedDocType: String = "01", // Default: Factura de Operación Interna
    val enabledSelectionDocType: Boolean = true,

    // === Operation Nature ===
    val selectedOperationNatureIndex: Int = 0,
    val selectedOperationNature: String = "01", // Default: Venta
    val enabledOperationNature: Boolean = true,

    // === Customer ===
    val finalCustomer: Boolean? = null, // null = not selected, false = registered customer, true = consumidor final

    // Fields for final consumer (not saved in DB)
    val finalName: String? = null,
    val finalEmail: String? = null,
    val finalPhone: String? = null,
    val finalIdTypeIndex: Int = 0,
    val finalIdType: String = "cedula",
    val finalIdNumber: String? = null,
    val finalPassportCountry: String? = null,


    // === Global discounts / charges ===
    val globalDiscountMode: GlobalDiscountMode = GlobalDiscountMode.NONE,
    val globalDiscountPercent: Int = 0,            // 0..100, used when mode = PERCENT
    val globalDiscountFixedCents: Long = 0L,       // used when mode = FIXED

    val globalShippingCents: Long? = null,         // mutually exclusive with any per-item shipping
    val globalInsuranceCents: Long? = null,        // mutually exclusive with any per-item insurance
    val globalOtherChargesCents: Long? = null,     // always allowed

    // Reference Note CUFE
    val referencedNoteCUFE: String = "",
    val referencedCreatedAt: String = "",

    // === Additional Info Sheet ===
    val showAdditionalSheet: Boolean = false,
    val expandExportSection: Boolean = false,

    // Logistics
    val logisticsInfo: String = "",
    val logisticsVehiclePlate: String = "",
    val logisticsCarrierLegalName: String = "",
    val logisticsCarrierRuc: String = "",
    val logisticsCarrierDv: String = "",
    val logisticsCarrierTaxpayerTypeIndex: Int = 0, // 0: Natural, 1: Jurídico
    val logisticsBoxesQty: String = "",
    val logisticsTotalWeightLb: String = "",

    // Delivery Location
    val deliveryReceiverLegalName: String = "",
    val deliveryReceiverRuc: String = "",
    val deliveryReceiverDv: String = "",
    val deliveryReceiverTaxpayerTypeIndex: Int = 0, // 0: Natural, 1: Jurídico
    val deliveryContactPhone: String = "",
    val deliveryAltContactPhone: String = "",
    val deliveryProvinceIndex: Int = 0,
    val deliveryDistrictIndex: Int = 0,
    val deliveryCorregIndex: Int = 0,
    val customerAddresses: List<CustomerAddress> = emptyList(),
    val selectedCustomerAddressId: Long? = null,
    val customerAddressesLoading: Boolean = false,

    // === Quotes ===
    val flowMode: FlowMode = FlowMode.SALE,
    val quoteId: Long? = null,
    val quoteStyle: String = "style1",
    val quoteStyleWasEdited: Boolean = false,
    val quoteExpiryDate: String? = null,
    val quoteAdditionalInfo: String? = null,
    val quotesSettings: QuoteSettings? = null,
    val lastQuoteId: Long? = null,
    val lastQuoteNumber: String? = null,

    // Retention
    val retentionCodeIndex: Int = 0,      // index in retention list (0 = none)
    val retentionAmount: String = "",     // retention rate (%) for code 8

    // Exportation (only visible if selectedOperationNature == "02")
    val exportIncoterm: String = "",
    val exportCurrency: String = "PAB",
    val exportPortOfLoading: String = "",

    // === Payments view ===
    val paymentFlowMode: PaymentFlowMode = PaymentFlowMode.MANUAL_OR_INSTALLMENTS,
    val otherPaymentDescription: String = "",
    val wantPaymentLink: Boolean = false,
    val installments: List<InstallmentUI> = emptyList(),
    val showTipsSheet: Boolean = false,

    // ==== Final State ====
    val invoiceStatus: InvoiceStatus = InvoiceStatus.NONE,
    val pdfDocument: String = "", // base64
    val paymentLink: String = "",
    val orderNumber: String = "",

    val orderCreationFailed: Boolean = false,

    override val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
) : LoadableState<PosState> {
    override fun withLoading(state: LoadingBottomSheetState): PosState {
        return copy(loadingBottomSheet = state)
    }
}

enum class FlowMode {
    SALE, QUOTE
}

sealed class PosStateUiEvent {
    data object OrderCreated : PosStateUiEvent()
    data object OrderUpdated : PosStateUiEvent()
    data object ShowPremiumScreen : PosStateUiEvent()
    data object ShowPaymentMethodsScreen : PosStateUiEvent()
}

fun List<CartLine>.totalItems(): Int {
    return this.sumOf { it.quantity }
}

object CartCalc {
    data class Summary(
        val subtotal: Money,
        val lineDiscounts: Money,
        val perLineCharges: Money,      // sum of per-item shipping+insurance
        val globalDiscount: Money,
        val globalCharges: Money,

        val itbms: Money,
        val isc: Money,
        val oti: Money,
        val tax: Money,

        val totalBeforeTip: Money,
        val tip: Money,
        val grandTotal: Money
    )

    fun summarize(state: PosState): Summary {
        val lines = state.cart

        // Subtotal BEFORE discounts: sum of (unitPrice × quantity) for all items
        val subtotal = lines.sumOf { it.lineSubtotalBeforeDiscount() }
        // Line discounts: sum of (discountPerUnit × quantity) for all items
        val lineDiscounts = lines.sumOf { it.discountAmount() }
        val perLineCharges = lines.sumOf { it.lineCharges() }

        val itemsById = state.items.associateBy { it.itemId }
        
        // Helper to get item for a cart line (handles both saved and personalized items)
        fun getItemForLine(line: CartLine): Item? {
            return if (line.itemId < 0) {
                // Personalized item: lookup by lineId
                state.personalizedItems[line.lineId]
            } else {
                // Saved item: lookup by itemId
                itemsById[line.itemId]
            }
        }

        // Base for all taxes: subtotal AFTER per-unit discounts (does NOT include charges)
        // This is the discounted unit price × quantity for each line
        fun baseForTaxes(line: CartLine): Long = line.lineSubtotal().coerceAtLeast(0L)

        // ITBMS: calculated from CartLine.tax field (separate tax, shown separately)
        val itbms = lines.sumOf { it.taxTotal(state.taxExempt) }

        // ISC: calculated from item.iscRate (percentage, e.g., 7.0 for 7%)
        // Shown separately as sum of all ISC taxes only
        val isc = lines.sumOf { line ->
            val item = getItemForLine(line)
            val ratePct = item?.iscRate ?: 0.0
            if (ratePct <= 0.0 || state.taxExempt) 0L
            else ((baseForTaxes(line) * ratePct) / 100.0).roundToLong()
        }

        // OTI: calculated from item.otiTaxes (rate stored as decimal, e.g., 0.07 for 7%)
        // Shown separately as sum of all OTI taxes only
        val oti = lines.sumOf { line ->
            val item = getItemForLine(line)
            val otiList = item?.otiTaxes.orEmpty()
            if (otiList.isEmpty() || state.taxExempt) return@sumOf 0L

            val base = baseForTaxes(line)
            otiList.sumOf { oti ->
                // Backend stores rate as decimal (e.g., 0.07 for 7%), use directly
                ((base * oti.rate) / 1.0).roundToLong()
            }
        }


        val tax = itbms + isc + oti

        // Global discount base = items subtotal BEFORE discounts - line discounts + per-line charges
        // This represents the total after item-level discounts but before global discount
        val subtotalAfterItemDiscounts = (subtotal - lineDiscounts).coerceAtLeast(0L)
        val baseForGlobalDiscount = (subtotalAfterItemDiscounts + perLineCharges).coerceAtLeast(0L)
        val globalDiscount = when (state.globalDiscountMode) {
            GlobalDiscountMode.NONE    -> 0L
            GlobalDiscountMode.PERCENT -> (baseForGlobalDiscount * state.globalDiscountPercent.coerceIn(0, 100)) / 100L
            GlobalDiscountMode.FIXED   -> state.globalDiscountFixedCents.coerceAtMost(baseForGlobalDiscount)
        }

        // Exclusivity for global shipping/insurance
        val anyItemHasShipping = lines.any { (it.shippingCents ?: 0L) > 0L }
        val anyItemHasInsurance = lines.any { (it.insuranceCents ?: 0L) > 0L }

        val globalShipping = if (anyItemHasShipping) 0L else (state.globalShippingCents ?: 0L)
        val globalInsurance = if (anyItemHasInsurance) 0L else (state.globalInsuranceCents ?: 0L)
        val globalOther = state.globalOtherChargesCents ?: 0L
        val globalCharges = globalShipping + globalInsurance + globalOther

        // Base before tip: subtotal after item discounts + per-line charges - global discount + taxes + global charges
        val base = (subtotalAfterItemDiscounts + perLineCharges - globalDiscount + tax + globalCharges).coerceAtLeast(0L)

        val tip = if (state.tipIsPercentage) base * state.tipAmount / 10_000L else state.tipAmount
        val grand = (base + tip).coerceAtLeast(0L)

        return Summary(
            subtotal = subtotal,
            lineDiscounts = lineDiscounts,
            perLineCharges = perLineCharges,
            globalDiscount = globalDiscount,
            globalCharges = globalCharges,
            itbms = itbms,
            isc = isc,
            oti = oti,
            tax = tax,
            totalBeforeTip = base,
            tip = tip,
            grandTotal = grand
        )
    }
}
