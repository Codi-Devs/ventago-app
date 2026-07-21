package com.teco.ventago.features.pos.ui.viewmodel

import com.teco.ventago.core.LoadableState
import com.teco.ventago.design_system.molecules.pos.GlobalDiscountMode
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.features.branches.domain.model.Branch
import com.teco.ventago.features.branches.domain.model.FiscalBillingPoint
import com.teco.ventago.features.customers.ui.form.viewmodel.CustomerCountries
import com.teco.ventago.features.customers.ui.form.viewmodel.CustomerCountryOption
import com.teco.ventago.features.customers.domain.models.CustomerAddress
import com.teco.ventago.features.customers.domain.models.Customer
import com.teco.ventago.features.customers.domain.models.CustomerListItem
import com.teco.ventago.features.invoicing.domain.PostCreateInvoiceWarningState
import com.teco.ventago.features.invoicing.domain.models.BottomNoteSettings
import com.teco.ventago.features.invoicing.domain.models.InvoiceStatus
import com.teco.ventago.features.orders.domain.models.responses.OnsitePaymentDto
import com.teco.ventago.features.orders.domain.models.responses.YappyOnsitePendingTransactionDto

import com.teco.ventago.features.pos.domain.models.CartLine
import com.teco.ventago.features.pos.domain.models.Discount
import com.teco.ventago.features.pos.domain.models.Money
import com.teco.ventago.features.pos.domain.models.Tax
import com.teco.ventago.features.product.domain.model.Item
import com.teco.ventago.features.payments.domain.models.YappyOnsiteDevice
import com.teco.ventago.features.payments.domain.models.YappyOnsiteTransactionPayload
import com.teco.ventago.features.quotes.domain.models.QuoteSettings
import com.teco.ventago.utils.toDecimalString
import com.teco.ventago.utils.ViewState
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlin.math.roundToLong

enum class PaymentFlowMode { MANUAL_OR_INSTALLMENTS, PAYMENT_LINK, YAPPY_ONSITE, DRAFT }
enum class PendingPaymentIntentMethod { PAYMENT_LINK, YAPPY_ONSITE }
enum class PendingPaymentChangeExitAction { POS_START, HOME }
enum class ProductViewMode { LIST, GRID }

fun PendingPaymentIntentMethod.hiddenPaymentFlowMode(): PaymentFlowMode {
    return when (this) {
        PendingPaymentIntentMethod.PAYMENT_LINK -> PaymentFlowMode.PAYMENT_LINK
        PendingPaymentIntentMethod.YAPPY_ONSITE -> PaymentFlowMode.YAPPY_ONSITE
    }
}

fun shouldShowPendingPaymentChangeOption(
    mode: PaymentFlowMode,
    sourceMethod: PendingPaymentIntentMethod?,
    normallyVisible: Boolean,
): Boolean {
    if (!normallyVisible) return false
    if (mode == PaymentFlowMode.DRAFT) return sourceMethod == null
    return sourceMethod?.hiddenPaymentFlowMode() != mode
}

private fun currentPanamaDateIso(): String {
    val date = Clock.System.now().toLocalDateTime(TimeZone.of("America/Panama")).date
    return "${date.year.toString().padStart(4, '0')}-${date.monthNumber.toString().padStart(2, '0')}-${date.dayOfMonth.toString().padStart(2, '0')}"
}

data class PosCartCustomerDisplay(
    val name: String,
    val email: String? = null,
    val identificationLabel: String? = null,
    val identificationValue: String? = null,
    val customerTypeLabel: String? = null,
    val isRegisteredCustomer: Boolean,
    val invoiceCustomer: Int? = null,
)

data class PosProductCategoryFilter(
    val id: Int,
    val label: String
)

data class InstallmentUI(
    val amountCents: Long = 0L,
    val dueDateIso: String = "" // "YYYY-MM-DD"
)

enum class OrderCreationStep { CUSTOMER, PRODUCTS, CART, PAYMENT }

internal data class PosDocumentTypeOption(
    val code: String,
    val label: String,
) {
    override fun toString(): String = "$code - $label"
}

internal object PosDocumentTypeOptions {
    val selectable: List<PosDocumentTypeOption> = listOf(
        PosDocumentTypeOption("01", "Factura de Operación Interna"),
        PosDocumentTypeOption("02", "Factura de Importación"),
        PosDocumentTypeOption("03", "Factura de Exportación"),
        PosDocumentTypeOption("06", "Nota de Crédito Genérica"),
        PosDocumentTypeOption("08", "Factura de Zona Franca"),
        PosDocumentTypeOption("09", "Reembolso"),
        PosDocumentTypeOption("10", "Factura de Operación Extranjera")
    )

    fun indexOf(code: String): Int = selectable.indexOfFirst { it.code == code }

    fun codeAt(index: Int): String = selectable.getOrNull(index)?.code ?: "01"

    fun sanitizedSelection(code: String, index: Int): Pair<Int, String> {
        val resolvedIndex = indexOf(code)
        return if (resolvedIndex >= 0) {
            resolvedIndex to code
        } else {
            0 to "01"
        }
    }
}

@Serializable
data class OrderCreationCheckpoint(
    val version: Int = 1,
    val businessId: Int,
    val savedAtEpochSeconds: Long,
    val currentStep: OrderCreationStep,
    val data: OrderCreationCheckpointData,
)

@Serializable
data class OrderCreationCheckpointData(
    val branchCode: String? = null,
    val billingPoint: String? = null,
    val selectedDocTypeIndex: Int = 0,
    val selectedDocType: String = "01",
    val selectedOperationNatureIndex: Int = 0,
    val selectedOperationNature: String = "01",
    val invoiceIssueDateIso: String = currentPanamaDateIso(),
    val customer: CustomerListItem? = null,
    val finalCustomer: Boolean? = null,
    val finalName: String? = null,
    val finalEmail: String? = null,
    val finalPhone: String? = null,
    val finalIdTypeIndex: Int = 0,
    val finalIdType: String = "cedula",
    val finalIdNumber: String? = null,
    val finalCustomerCountryCode: String? = null,
    val cart: List<OrderCreationCartLineCheckpoint> = emptyList(),
    val productSnapshots: Map<String, Item> = emptyMap(),
    val taxExempt: Boolean = false,
    val globalDiscountMode: GlobalDiscountMode = GlobalDiscountMode.NONE,
    val globalDiscountPercent: Int = 0,
    val globalDiscountFixedCents: Long = 0L,
    val globalShippingCents: Long? = null,
    val globalInsuranceCents: Long? = null,
    val globalOtherChargesCents: Long? = null,
    val referencedNoteCUFE: String = "",
    val referencedCreatedAt: String = "",
    val includeBottomNote: Boolean? = null,
    val logisticsInfo: String = "",
    val logisticsVehiclePlate: String = "",
    val logisticsCarrierLegalName: String = "",
    val logisticsCarrierRuc: String = "",
    val logisticsCarrierDv: String = "",
    val logisticsCarrierTaxpayerTypeIndex: Int = 0,
    val logisticsBoxesQty: String = "",
    val logisticsTotalWeightLb: String = "",
    val deliveryReceiverLegalName: String = "",
    val deliveryReceiverRuc: String = "",
    val deliveryReceiverDv: String = "",
    val deliveryReceiverTaxpayerTypeIndex: Int = 0,
    val deliveryContactPhone: String = "",
    val deliveryAltContactPhone: String = "",
    val deliveryProvinceIndex: Int = 0,
    val deliveryDistrictIndex: Int = 0,
    val deliveryCorregIndex: Int = 0,
    val selectedCustomerAddressId: Long? = null,
    val retentionCodeIndex: Int = 0,
    val retentionAmount: String = "",
    val exportIncoterm: String = "",
    val exportCurrency: String = "PAB",
    val exportPortOfLoading: String = "",
)

@Serializable
data class OrderCreationCartLineCheckpoint(
    val lineId: String,
    val itemId: Int,
    val name: String,
    val baseUnitPrice: Long,
    val quantity: Double = 1.0,
    val overrideUnitPrice: Long? = null,
    val discount: OrderCreationDiscountCheckpoint? = null,
    val tax: OrderCreationTaxCheckpoint? = null,
    val notes: String? = null,
    val shippingCents: Long? = null,
    val insuranceCents: Long? = null,
    val pharmaBatchNumber: String? = null,
    val pharmaBatchQty: Int? = null,
    val costCents: Long? = null,
) {
    fun toCartLine(): CartLine = CartLine(
        lineId = lineId,
        itemId = itemId,
        name = name,
        baseUnitPrice = baseUnitPrice,
        quantity = quantity,
        overrideUnitPrice = overrideUnitPrice,
        discount = discount?.toDiscount(),
        tax = tax?.toTax(),
        notes = notes,
        shippingCents = shippingCents,
        insuranceCents = insuranceCents,
        pharmaBatchNumber = pharmaBatchNumber,
        pharmaBatchQty = pharmaBatchQty,
        costCents = costCents,
    )

    companion object {
        fun from(line: CartLine): OrderCreationCartLineCheckpoint =
            OrderCreationCartLineCheckpoint(
                lineId = line.lineId,
                itemId = line.itemId,
                name = line.name,
                baseUnitPrice = line.baseUnitPrice,
                quantity = line.quantity,
                overrideUnitPrice = line.overrideUnitPrice,
                discount = OrderCreationDiscountCheckpoint.from(line.discount),
                tax = line.tax?.let(OrderCreationTaxCheckpoint::from),
                notes = line.notes,
                shippingCents = line.shippingCents,
                insuranceCents = line.insuranceCents,
                pharmaBatchNumber = line.pharmaBatchNumber,
                pharmaBatchQty = line.pharmaBatchQty,
                costCents = line.costCents,
            )
    }
}

@Serializable
data class OrderCreationDiscountCheckpoint(
    val mode: String,
    val value: Long,
) {
    fun toDiscount(): Discount? = when (mode) {
        MODE_AMOUNT -> Discount.Amount(value)
        MODE_PERCENT -> Discount.Percent(value.toInt())
        else -> null
    }

    companion object {
        private const val MODE_AMOUNT = "AMOUNT"
        private const val MODE_PERCENT = "PERCENT"

        fun from(discount: Discount?): OrderCreationDiscountCheckpoint? = when (discount) {
            is Discount.Amount -> OrderCreationDiscountCheckpoint(MODE_AMOUNT, discount.value)
            is Discount.Percent -> OrderCreationDiscountCheckpoint(MODE_PERCENT, discount.bps.toLong())
            null -> null
        }
    }
}

@Serializable
data class OrderCreationTaxCheckpoint(
    val id: Int,
    val name: String,
    val rateBps: Int,
) {
    fun toTax(): Tax = Tax(id = id, name = name, rateBps = rateBps)

    companion object {
        fun from(tax: Tax): OrderCreationTaxCheckpoint =
            OrderCreationTaxCheckpoint(id = tax.id, name = tax.name, rateBps = tax.rateBps)
    }
}

fun OrderCreationCheckpointData.hasMeaningfulUserData(): Boolean {
    val hasFinalCustomerInfo = finalCustomer != null ||
        !finalName.isNullOrBlank() ||
        !finalEmail.isNullOrBlank() ||
        !finalPhone.isNullOrBlank() ||
        !finalIdNumber.isNullOrBlank() ||
        !finalCustomerCountryCode.isNullOrBlank()

    val hasGlobalAdjustments = globalDiscountMode != GlobalDiscountMode.NONE ||
        globalDiscountPercent > 0 ||
        globalDiscountFixedCents > 0L ||
        (globalShippingCents ?: 0L) > 0L ||
        (globalInsuranceCents ?: 0L) > 0L ||
        (globalOtherChargesCents ?: 0L) > 0L

    val hasAdditionalInvoiceInfo = logisticsInfo.isNotBlank() ||
        logisticsVehiclePlate.isNotBlank() ||
        logisticsCarrierLegalName.isNotBlank() ||
        logisticsCarrierRuc.isNotBlank() ||
        logisticsCarrierDv.isNotBlank() ||
        logisticsBoxesQty.isNotBlank() ||
        logisticsTotalWeightLb.isNotBlank() ||
        deliveryReceiverLegalName.isNotBlank() ||
        deliveryReceiverRuc.isNotBlank() ||
        deliveryReceiverDv.isNotBlank() ||
        deliveryContactPhone.isNotBlank() ||
        deliveryAltContactPhone.isNotBlank() ||
        selectedCustomerAddressId != null ||
        exportIncoterm.isNotBlank() ||
        exportCurrency != "PAB" ||
        exportPortOfLoading.isNotBlank()

    val hasRetention = retentionCodeIndex != 0 || retentionAmount.isNotBlank()

    return customer != null ||
        hasFinalCustomerInfo ||
        referencedNoteCUFE.isNotBlank() ||
        cart.isNotEmpty() ||
        hasGlobalAdjustments ||
        taxExempt ||
        hasAdditionalInvoiceInfo ||
        hasRetention
}

data class PosState(
    val items: List<Item> = listOf(),
    val visibleItems: List<Item> = listOf(),
    val availableProductCategories: List<PosProductCategoryFilter> = emptyList(),
    val selectedProductCategoryId: Int? = null,
    val productViewMode: ProductViewMode = ProductViewMode.LIST,
    val itemCategoryById: Map<Int, Int> = mapOf(),
    val cart: List<CartLine> = listOf(),
    val personalizedItems: Map<String, Item> = mapOf(), // Stores personalized products keyed by lineId (since all have itemId = -1)
    val productAddedSnackbarToken: Long = 0L,
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
    val selectedCustomerFeCustomerType: String? = null,
    val customerQuery: String = "",
    val paymentsConfigured: Boolean = false,
    val paymentsOnboardingCompleted: Boolean = false,
    val paymentProfileResolved: Boolean = false,
    val paymentLinkConfigured: Boolean = false,
    val yappyOnsiteConfigured: Boolean = false,
    val yappyOnsiteDevices: List<YappyOnsiteDevice> = emptyList(),
    val yappyOnsiteDevicesResolved: Boolean = false,
    val invoicingEnabled: Boolean = false,
    val autoInvoiceOnPaymentSuccess: Boolean = false,
    val canCreateInvoice: Boolean = false,
    val canCreateDraft: Boolean = false,
    val canCreatePaymentLink: Boolean = false,
    val canConfigurePayments: Boolean = false,
    val canConfigureYappyOnsite: Boolean = false,
    val canCreateYappyOnsiteQr: Boolean = false,
    val canCreateQuote: Boolean = false,
    val canUpdateQuote: Boolean = false,
    val canUseCustomProduct: Boolean = false,
    val canEditProduct: Boolean = false,

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
    val invoiceIssueDateIso: String = currentPanamaDateIso(),

    // === Customer ===
    val finalCustomer: Boolean? = null, // null = not selected, false = registered customer, true = consumidor final

    // Fields for final consumer (not saved in DB)
    val finalName: String? = null,
    val finalEmail: String? = null,
    val finalEmailError: String? = null,
    val finalPhone: String? = null,
    val finalIdTypeIndex: Int = 0,
    val finalIdType: String = "cedula",
    val finalIdNumber: String? = null,
    val finalIdNumberError: String? = null,
    val finalCustomerCountryCode: String? = null,
    val finalCustomerCountryOptions: List<CustomerCountryOption> = CustomerCountries.options,


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
    val maxCreditNoteAmountCents: Long? = null,
    val sourceOrderNumber: String? = null,
    val originalInvoiceNumber: String = "",
    val originalInvoiceNumberError: String? = null,
    val originalInvoiceEmissionDateIso: String = "",
    val originalInvoiceEmissionDateError: String? = null,

    // === Additional Info Sheet ===
    val showAdditionalSheet: Boolean = false,
    val expandExportSection: Boolean = false,
    val bottomNoteSettings: BottomNoteSettings? = null,
    val bottomNoteRefreshFailed: Boolean = false,
    val includeBottomNote: Boolean? = null,

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
    val showPaymentLinkNewBadge: Boolean = false,
    val otherPaymentDescription: String = "",
    val wantPaymentLink: Boolean = false,
    val installments: List<InstallmentUI> = emptyList(),
    val showTipsSheet: Boolean = false,

    // ==== Final State ====
    val invoiceStatus: InvoiceStatus = InvoiceStatus.NONE,
    val pdfDocument: String = "", // base64
    val paymentLink: String = "",
    val createdOrderId: Int? = null,
    val paymentLinkPolling: Boolean = false,
    val paymentLinkPaymentDetected: Boolean = false,
    val paymentLinkInvoicePrintAttemptedOrderId: Int? = null,
    val paymentLinkManualPanelVisible: Boolean = false,
    val paymentLinkManualErrorMessage: String? = null,
    val pendingPaymentChangeSourceMethod: PendingPaymentIntentMethod? = null,
    val pendingPaymentChangeSourceReleased: Boolean = false,
    val pendingPaymentChangeOpen: Boolean = false,
    val pendingPaymentChangeCompleted: Boolean = false,
    val pendingPaymentChangeCancelDialogVisible: Boolean = false,
    val pendingPaymentChangeCancelErrorMessage: String? = null,
    val pendingPaymentChangeExitAction: PendingPaymentChangeExitAction? = null,
    val onsitePayment: OnsitePaymentDto? = null,
    val yappyOnsiteTransaction: YappyOnsiteTransactionPayload? = null,
    val yappyOnsitePolling: Boolean = false,
    val yappyOnsitePollingSuppressed: Boolean = false,
    val yappyOnsiteInvoiceProcessingTimedOut: Boolean = false,
    val yappyOnsitePrintAttemptedTransactionId: String? = null,
    val yappyOnsiteExitCancelDialogVisible: Boolean = false,
    val showYappyOnsitePendingConflictDialog: Boolean = false,
    val yappyOnsitePendingConflict: YappyOnsitePendingTransactionDto? = null,
    val orderNumber: String = "",
    val postCreateInvoiceWarning: PostCreateInvoiceWarningState = PostCreateInvoiceWarningState(),

    val orderCreationFailed: Boolean = false,
    val showOrderRestoreDialog: Boolean = false,

    override val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
) : LoadableState<PosState> {
    override fun withLoading(state: LoadingBottomSheetState): PosState {
        return copy(loadingBottomSheet = state)
    }
}

enum class FlowMode {
    SALE, QUOTE
}

internal object PosNoteValidators {
    const val ORIGINAL_INVOICE_NUMBER_REQUIRED_MESSAGE = "Ingresa el número de la factura original."
    const val ORIGINAL_INVOICE_NUMBER_MAX_LENGTH_MESSAGE = "El número de la factura original no puede superar 22 caracteres."
    const val ORIGINAL_INVOICE_DATE_REQUIRED_MESSAGE = "Selecciona la fecha de emisión de la factura original."

    fun validateCreditNoteAmountLimit(
        selectedDocType: String,
        referencedNoteCUFE: String,
        maxCreditNoteAmountCents: Long?,
        requestedCreditNoteCents: Long,
        sourceOrderNumber: String?
    ): String? {
        val isReferencedCreditNote = referencedNoteCUFE.isNotBlank() &&
            selectedDocType in setOf("04", "06")
        if (!isReferencedCreditNote) return null

        val maxAmount = maxCreditNoteAmountCents ?: return null
        if (requestedCreditNoteCents <= maxAmount) return null

        val sourceLabel = sourceOrderNumber
            ?.takeIf { it.isNotBlank() }
            ?.let { " del pedido $it" }
            .orEmpty()
        return "El monto de la nota de crédito$sourceLabel no puede exceder el saldo disponible de $${maxAmount.toDecimalString()}."
    }

    fun validateOriginalInvoiceNumber(selectedDocType: String, value: String): String? {
        if (selectedDocType != "06") return null
        val normalized = value.trim()
        return when {
            normalized.isBlank() -> ORIGINAL_INVOICE_NUMBER_REQUIRED_MESSAGE
            normalized.length > 22 -> ORIGINAL_INVOICE_NUMBER_MAX_LENGTH_MESSAGE
            else -> null
        }
    }

    fun validateOriginalInvoiceEmissionDate(selectedDocType: String, value: String): String? {
        if (selectedDocType != "06") return null
        return if (value.isBlank()) ORIGINAL_INVOICE_DATE_REQUIRED_MESSAGE else null
    }

    fun validateGenericCreditNoteReference(selectedDocType: String, number: String, emissionDateIso: String): List<String> {
        return listOfNotNull(
            validateOriginalInvoiceNumber(selectedDocType, number),
            validateOriginalInvoiceEmissionDate(selectedDocType, emissionDateIso)
        )
    }
}

fun PosState.cartCustomerDisplay(): PosCartCustomerDisplay? {
    customer?.let { savedCustomer ->
        return PosCartCustomerDisplay(
            name = savedCustomer.name,
            email = savedCustomer.email?.trim()?.takeIf { it.isNotEmpty() },
            identificationLabel = savedCustomer.ruc?.trim()?.takeIf { it.isNotEmpty() }?.let { "Ruc" },
            identificationValue = savedCustomer.ruc?.trim()?.takeIf { it.isNotEmpty() },
            isRegisteredCustomer = true,
            invoiceCustomer = savedCustomer.invoiceCustomer,
        )
    }

    if (finalCustomer != true) return null

    val normalizedName = finalName?.trim()?.takeIf { it.isNotEmpty() }
    val normalizedEmail = finalEmail?.trim()?.takeIf { it.isNotEmpty() }
    val normalizedIdNumber = finalIdNumber?.trim()?.takeIf { it.isNotEmpty() }
    val hasTypedIdentity = normalizedName != null || normalizedEmail != null || normalizedIdNumber != null
    if (!hasTypedIdentity) return null

    return PosCartCustomerDisplay(
        name = normalizedName ?: "Consumidor final",
        email = normalizedEmail,
        identificationLabel = normalizedIdNumber?.let { finalCustomerIdentificationLabel(finalIdType) },
        identificationValue = normalizedIdNumber,
        customerTypeLabel = "Consumidor final",
        isRegisteredCustomer = false,
    )
}

internal fun finalCustomerIdentificationLabel(finalIdType: String): String {
    return when (finalIdType) {
        "cedula" -> "Cédula"
        "passport" -> "Pasaporte"
        "foreing_taxid" -> "Identificación extranjera"
        else -> "Identificación"
    }
}

sealed class PosStateUiEvent {
    data object OrderCreated : PosStateUiEvent()
    data object OrderUpdated : PosStateUiEvent()
    data object ShowPremiumScreen : PosStateUiEvent()
    data object ShowPaymentMethodsScreen : PosStateUiEvent()
}

fun List<CartLine>.totalItems(): Int {
    return this.size
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
