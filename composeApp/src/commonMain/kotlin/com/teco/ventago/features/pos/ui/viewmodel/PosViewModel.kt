package com.teco.ventago.features.pos.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.core.PdfSharer
import com.teco.ventago.design_system.molecules.pos.DiscountMode
import com.teco.ventago.design_system.molecules.pos.GlobalDiscountMode
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.branches.domain.BranchService
import com.teco.ventago.features.branches.domain.model.Branch as BranchModel
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.business.domain.model.Business
import com.teco.ventago.features.customers.domain.models.CustomerListItem
import com.teco.ventago.features.financialProfile.domain.FinancialProfileService
import com.teco.ventago.features.invoicing.domain.models.InvoiceStatus
import com.teco.ventago.features.orders.domain.models.CustomerSnapshot
import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.orders.domain.models.requests.Branch
import com.teco.ventago.features.orders.domain.models.requests.Charge
import com.teco.ventago.features.orders.domain.models.requests.CommercialAddenda
import com.teco.ventago.features.orders.domain.models.requests.CreateOrderPayment
import com.teco.ventago.features.orders.domain.models.requests.CreateOrderRequest
import com.teco.ventago.features.orders.domain.models.requests.CreateOrderTotals
import com.teco.ventago.features.orders.domain.models.requests.DeliveryLocation
import com.teco.ventago.features.orders.domain.models.requests.Exportation
import com.teco.ventago.features.orders.domain.models.requests.FinalCustomerInfo
import com.teco.ventago.features.orders.domain.models.requests.Invoice
import com.teco.ventago.features.orders.domain.models.requests.InvoiceCharge
import com.teco.ventago.features.orders.domain.models.requests.InvoiceDiscount
import com.teco.ventago.features.orders.domain.models.requests.ItemTotals
import com.teco.ventago.features.orders.domain.models.requests.Logistics
import com.teco.ventago.features.orders.domain.models.requests.NameValue
import com.teco.ventago.features.orders.domain.models.requests.OrderItem
import com.teco.ventago.features.orders.domain.models.requests.OrderItemDiscount
import com.teco.ventago.features.orders.domain.models.requests.OrderItemTax
import com.teco.ventago.features.orders.domain.models.requests.OrderReference
import com.teco.ventago.features.orders.domain.models.requests.PaymentLinksBlock
import com.teco.ventago.features.orders.domain.models.requests.PharmaSale
import com.teco.ventago.features.orders.domain.models.requests.ReferenceNumber
import com.teco.ventago.features.orders.domain.models.requests.References
import com.teco.ventago.features.orders.domain.models.requests.Retentions
import com.teco.ventago.features.orders.domain.models.requests.ThirdParty
import com.teco.ventago.features.pos.domain.PosService
import com.teco.ventago.features.pos.domain.models.CartLine
import com.teco.ventago.features.pos.domain.models.Discount
import com.teco.ventago.features.pos.domain.models.Money
import com.teco.ventago.features.pos.domain.models.Tax
import com.teco.ventago.features.product.domain.ProductService
import com.teco.ventago.features.product.domain.model.Item
import com.teco.ventago.features.product.domain.model.AdditionalInfoKey
import com.teco.ventago.features.quotes.domain.QuoteRequestBuilder
import com.teco.ventago.features.quotes.domain.QuotesService
import com.teco.ventago.features.quotes.domain.models.Quote
import com.teco.ventago.features.quotes.domain.models.QuoteSettings
import com.teco.ventago.features.quotes.domain.models.QuoteLine
import com.teco.ventago.features.quotes.domain.models.requests.GetQuoteRequest
import com.teco.ventago.features.quotes.domain.models.requests.SendQuoteEmailRequest
import com.teco.ventago.json
import com.teco.ventago.navigation.PosNoteRoute
import com.teco.ventago.utils.dbFormat
import com.teco.ventago.utils.randomUUID
import com.teco.ventago.utils.toDecimalString
import com.teco.ventago.utils.toLongCents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.encodeToString
import kotlin.String
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.roundToLong
import kotlin.math.roundToInt
import kotlin.time.Clock.System.now
import kotlin.time.ExperimentalTime


class PosViewModel(
    private val authService: IAuthService,
    private val businessService: BusinessService,
    private val branchService: BranchService,
    private val productService: ProductService,
    private val posService: PosService,
    private val financialProfileService: FinancialProfileService,
    private val quotesService: QuotesService,
    private val pdfSharer: PdfSharer,
) : BaseViewModel<PosState, PosStateUiEvent>(PosState()) {
    private companion object {
        const val DEFAULT_QUOTE_BRANCH_CODE = "0000"
    }

    var business: Business? = null
    var items: List<Item> = emptyList()

    private var order: Order? = null
    private var pendingQuoteBranchCode: String? = null

    init {
        viewModelScope.launch {

            productService.observe().onEach { products ->
                products?.let {
                    items = productService.getAllActiveItems(it)
                    updateItemList(items)
                }
            }.launchIn(this)

            branchService.observe().onEach { branches ->
                if (branches.isEmpty()) return@onEach
                val currentState = uiState.value
                val safeCurrentIndex = currentState.selectedBranchIndex
                    .takeIf { it in branches.indices } ?: 0
                val defaultIndex = branches.indexOfFirst { it.branchCode == DEFAULT_QUOTE_BRANCH_CODE }
                val pendingCode = pendingQuoteBranchCode
                val pendingIndex = pendingCode?.let { code ->
                    branches.indexOfFirst { it.branchCode == code }
                } ?: -1
                val resolvedIndex = when {
                    pendingIndex >= 0 -> pendingIndex
                    currentState.flowMode == FlowMode.QUOTE && currentState.branches.isEmpty() ->
                        if (defaultIndex >= 0) defaultIndex else safeCurrentIndex
                    else -> safeCurrentIndex
                }
                val resetBillingPoint = currentState.branches.isEmpty() || resolvedIndex != currentState.selectedBranchIndex
                updateBranchSelection(resolvedIndex, branches, resetBillingPoint)
                if (pendingIndex >= 0 || pendingCode != null) {
                    pendingQuoteBranchCode = null
                }
            }.launchIn(this)

            financialProfileService.observe().onEach { profile ->
                profile?.let {
                    updateState {
                        copy(
                            paymentsConfigured = financialProfileService.paymentsConfigured(),
                            invoicingEnabled = it.invoicingActive
                        )
                    }
                }
            }.launchIn(this)

            businessService.getBusiness().onEach { businessData ->
                businessData?.let {
                    updateState {
                        copy(
                            currency = it.currency.currencyCode,
                            currencySymbol = it.currency.symbol
                        )
                    }
                    business = it
                }
            }.launchIn(this)
        }
    }


    @OptIn(ExperimentalTime::class)
    fun startNoteFromInvoice(
        args: PosNoteRoute,
    ) {
        updateState {
            copy(
                selectedDocTypeIndex = when (args.op) {
                    "04" -> 3 // Nota de Crédito Referente a FE
                    "05" -> 4 // Nota de Débito Referente a FE
                    else -> 0
                },
                selectedDocType = args.op,
                enabledSelectionDocType = false,
                referencedNoteCUFE = args.cufe,
                referencedCreatedAt = args.createdAt
            )
        }

        if (args.customerId == null) {
            updateState { copy(finalCustomer = true) }
        } else {
            updateState {
                copy(
                    finalCustomer = false,
                    customer = CustomerListItem(
                        id = args.customerId.toLong(),
                        name = args.customerName!!,
                        email = args.customerEmail,
                        ruc = args.customerRuc,
                        status = args.customerStatus,
                        invoiceCustomer = args.customerInvoiceID ?: -1,
                        updatedAt = now().epochSeconds,
                    )
                )
            }
        }
    }

    fun updateItemList(itemsToUpdate: List<Item>) = updateState {
        val newItems = itemsToUpdate.map { it }
        copy(items = newItems) // new list + changed item instance
    }

    fun onSearchChange(query: String) {
        updateState { copy(query = query) }
        if (query.isEmpty()) {
            updateState { copy(items = this@PosViewModel.items) }
            return
        }
        updateItemList(uiState.value.items.filter { item ->
            item.name.contains(query, ignoreCase = true)
                    || item.description.contains(query, ignoreCase = true)
        })
    }

    fun selectCustomer(customer: CustomerListItem?) {
        updateState { copy(customer = customer) }
    }

    fun governmentWarningInvalidProducts(): List<String> {
        if (!isGovernmentLikeCustomer()) {
            return emptyList()
        }
        return validateGovernmentInvoiceProducts()
    }

    private fun isGovernmentLikeCustomer(): Boolean {
        val state = uiState.value
        if (state.finalCustomer != false) {
            return false
        }
        val ruc = state.customer?.ruc?.uppercase()?.trim().orEmpty()
        return ruc.isNotEmpty() && ruc.contains("NT")
    }

    private fun validateGovernmentInvoiceProducts(): List<String> {
        val state = uiState.value
        val invalidProducts = state.cart.mapNotNull { line ->
            val product = resolveProductForLine(state, line)
            val info = product?.additionalInfo
            val goodsCode = info.valueOrBlank(AdditionalInfoKey.PANAMA_GOODS_SERVICES_CODE.keyName)
            val unitCode = info.valueOrBlank(AdditionalInfoKey.PANAMA_GOODS_SERVICES_UNIT_CODE.keyName)
            if (goodsCode.isBlank() || unitCode.isBlank()) {
                line.name.ifBlank { "Producto ${line.itemId}" }
            } else {
                null
            }
        }
        return invalidProducts.distinct()
    }

    private fun resolveProductForLine(state: PosState, line: CartLine): Item? {
        return if (line.itemId < 0) {
            state.personalizedItems[line.lineId]
        } else {
            items.firstOrNull { it.itemId == line.itemId }
                ?: state.items.firstOrNull { it.itemId == line.itemId }
        }
    }

    private fun JsonObject?.valueOrBlank(key: String): String {
        return this?.get(key)?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
    }

    fun hasExportationData(): Boolean {
        return uiState.value.exportIncoterm.isNotBlank() &&
                uiState.value.exportCurrency.isNotBlank() &&
                uiState.value.exportPortOfLoading.isNotBlank()
    }

    fun openAdditionalSheetExpandExport() {
        updateState {
            copy(showAdditionalSheet = true, expandExportSection = true)
        }
    }

    @OptIn(ExperimentalTime::class)
    fun addItemToCart(
        item: Item,
        customUnitPrice: Money? = null,   // null -> use base price
        deltaQty: Int = 1,
        tax: Tax? = null
    ) = updateState {
        val priceCents = customUnitPrice
        val baseCents = item.price.toLongCents()

        // For personalized items (itemId < 0), always create a new line since each is unique
        // For saved items, try to merge with existing line if same item, price, tax, and no discount
        val isPersonalized = item.itemId < 0
        val idx = if (!isPersonalized) {
            cart.indexOfFirst { line ->
                line.itemId == item.itemId &&
                        (line.overrideUnitPrice == priceCents) &&
                        (line.tax?.id == tax?.id) &&
                        line.discount == null
            }
        } else {
            -1 // Never merge personalized items
        }

        if (idx >= 0) {
            // Merge: bump quantity (and drop the line if it goes to 0)
            val cur = cart[idx]
            val newQty = (cur.quantity + deltaQty).coerceAtLeast(0)
            val newCart = cart.toMutableList()
            if (newQty == 0) {
                newCart.removeAt(idx)
                copy(cart = newCart)
            } else {
                newCart[idx] = cur.copy(quantity = newQty)
                copy(cart = newCart)
            }
        } else {
            // New line (different variant: price/tax/discount, or personalized item)
            val lineId = "${item.itemId}-${randomUUID()}"
            val newLine = CartLine(
                lineId = lineId,
                itemId = item.itemId,
                name = item.name,
                baseUnitPrice = baseCents,
                overrideUnitPrice = customUnitPrice, // keep Money? so totals can choose override if present
                quantity = deltaQty.coerceAtLeast(1),
                tax = tax,
                discount = null,
            )
            // Store personalized items (itemId < 0) keyed by lineId for later use in order creation
            val newPersonalizedItems = if (isPersonalized) {
                personalizedItems + (lineId to item)
            } else {
                personalizedItems
            }
            copy(cart = cart + newLine, personalizedItems = newPersonalizedItems)
        }
    }

    fun updateCartLine(
        lineId: String,
        unitPriceCents: Long,         // manual unit price before discount (cents)
        quantity: Int,
        discountMode: DiscountMode,
        discountValue: Long,
        itemShippingCents: Long?,
        itemInsuranceCents: Long?,
        pharmaBatchNumber: String?,
        pharmaBatchQty: Int?
    ) {
        setLineQty(lineId, quantity)
        setLineOverridePrice(lineId, unitPriceCents)
        when (discountMode) {
            DiscountMode.NONE -> setLineDiscount(lineId, null)
            DiscountMode.PERCENT -> setLineDiscount(
                lineId,
                Discount.Percent(discountValue.toInt().coerceIn(0, 100) * 100)
            )

            DiscountMode.FIXED -> setLineDiscount(
                lineId,
                Discount.Amount(discountValue.coerceAtLeast(0L))
            )
        }
        setLineShipping(lineId, itemShippingCents)
        setLineInsurance(lineId, itemInsuranceCents)
        setLinePharma(lineId, pharmaBatchNumber, pharmaBatchQty)
    }

    fun setLineQty(lineId: String, qty: Int) = updateState {
        copy(cart = cart.map { if (it.lineId == lineId) it.copy(quantity = qty.coerceAtLeast(1)) else it })
    }

    fun setLineOverridePrice(lineId: String, price: Money?) = updateState {
        copy(cart = cart.map { if (it.lineId == lineId) it.copy(overrideUnitPrice = price) else it })
    }

    fun setLineDiscount(lineId: String, discount: Discount?) = updateState {
        copy(cart = cart.map { if (it.lineId == lineId) it.copy(discount = discount) else it })
    }

    fun toggleTaxExempt(enabled: Boolean) = updateState { copy(taxExempt = enabled) }

    fun removeLine(lineId: String) =
        updateState { 
            val lineToRemove = cart.firstOrNull { it.lineId == lineId }
            copy(
                cart = cart.filterNot { it.lineId == lineId },
                // Remove personalized item if this line was a personalized product
                personalizedItems = if (lineToRemove?.itemId != null && lineToRemove.itemId < 0) {
                    personalizedItems - lineId
                } else {
                    personalizedItems
                }
            )
        }

    fun clearCart() = updateState { copy(cart = emptyList(), personalizedItems = mapOf()) }


    fun getChange(): Long {
        val charged = uiState.value.charged.values.sum()
        return if (charged < getTotalAmount()) {
            0
        } else {
            charged - getTotalAmount()
        }
    }


    fun resetForNewSale() {
        updateState {
            copy(
                cart = emptyList(),
                personalizedItems = mapOf(),
                taxExempt = false,
                query = "",
                tipAmount = 0L,
                tipIsPercentage = true,
                charged = mapOf(),
                freeTrialAvailable = false,
                customer = null,
                customerQuery = "",
                selectedBranchIndex = 0,
                selectedBillingPointIndex = 0,
                selectedDocTypeIndex = 0,
                selectedDocType = "01",
                finalCustomer = null,
                finalName = null,
                finalEmail = null,
                finalPhone = null,
                finalIdTypeIndex = 0,
                finalIdType = "cedula",
                finalIdNumber = null,
                finalPassportCountry = null,

                globalDiscountMode = GlobalDiscountMode.NONE,
                globalDiscountPercent = 0,
                globalDiscountFixedCents = 0L,
                globalShippingCents = 0L,
                globalInsuranceCents = 0L,
                globalOtherChargesCents = 0L,
                referencedNoteCUFE = "",
                referencedCreatedAt = "",

                logisticsInfo = "",
                logisticsVehiclePlate = "",
                logisticsCarrierLegalName = "",
                logisticsCarrierRuc = "",
                logisticsCarrierDv = "",
                logisticsCarrierTaxpayerTypeIndex = 0, // 0: Natural, 1: Jurídico
                logisticsBoxesQty = "",
                logisticsTotalWeightLb = "",

                deliveryReceiverLegalName = "",
                deliveryReceiverRuc = "",
                deliveryReceiverDv = "",
                deliveryReceiverTaxpayerTypeIndex = 0, // 0: Natural, 1: Jurídico
                deliveryContactPhone = "",
                deliveryAltContactPhone = "",
                deliveryProvinceIndex = 0,
                deliveryDistrictIndex = 0,
                deliveryCorregIndex = 0,

                // Retention
                retentionCodeIndex = 0,      // index in retention list
                retentionAmount = "",     // required only if "Otros (8)"

                // Exportation (only visible if selectedOperationNature == "02")
                exportIncoterm = "",
                exportCurrency = "PAB",
                exportPortOfLoading = "",

                // === Payments view ===
                paymentFlowMode = PaymentFlowMode.MANUAL_OR_INSTALLMENTS,
                otherPaymentDescription = "",
                wantPaymentLink = false,
                installments = emptyList(),
                showTipsSheet = false,

                // ==== Final State ====
                invoiceStatus = InvoiceStatus.NONE,
                pdfDocument = "", // base64
                paymentLink = "",
                orderNumber = "",

                )
        }
    }


    fun createOrder(createPaymentLink: Boolean = false, saveAsDraft: Boolean) {
        showLoading()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val request = createOrderRequest(createPaymentLink, saveAsDraft)
                    println("ASDASD: ${json.encodeToString(request)}")
                  val response = posService.createOrder(business!!.businessId, request)
                  val hasValidOrderNumber = response.orderNumber.isNotBlank()
                  var invoiceStatusFromResponse = InvoiceStatus.fromId(response.invoiceStatus)
                  
                  // If creating a confirmed order (not draft, not payment link) and invoice status is NONE,
                  // treat it as invoice generation failure
                  if (!createPaymentLink && !saveAsDraft && invoiceStatusFromResponse == InvoiceStatus.NONE) {
                      invoiceStatusFromResponse = InvoiceStatus.FAILED
                  }
                  
                  updateState {
                      copy(
                          invoiceStatus = invoiceStatusFromResponse,
                          pdfDocument = response.invoiceFiles?.pdf ?: "",
                          paymentLink = response.links?.firstOrNull { link -> link.action == "payer_action" }?.url
                              ?: "",
                          orderNumber = response.orderNumber,
                          orderCreationFailed = !hasValidOrderNumber
                      )
                  }
                  
                  withContext(Dispatchers.Main) {
                      if (!hasValidOrderNumber) {
                          showError()
                      } else {
                        showSuccess()
                      }
                  }
                } catch (e: Exception) {
                    println("Error creating order: ${e.message}")
                    updateState { copy(orderCreationFailed = true) }
                    withContext(Dispatchers.Main) {
                        showError()
                    }
                }
            }

        }
    }

    @OptIn(ExperimentalTime::class)
    fun createOrderRequest(createPaymentLink: Boolean, saveAsDraft: Boolean): CreateOrderRequest {
        val state = uiState.value
        val isFinalCustomer = requireNotNull(state.finalCustomer) { "Customer type not selected" }

        val operationDestination = if (state.selectedDocType == "03" || state.selectedDocType == "10") "2" else "1"


        val panamaZone = TimeZone.of("America/Panama")
        val localDateTime = Clock.System.now().toLocalDateTime(panamaZone)

        val invoice = Invoice(
            type = state.selectedDocType,
            deliveryDate = null,
            operationNature = state.selectedOperationNature,
            operationDestination = operationDestination,
            issuerFeAdditionalInfo = "",
            issuedDatetime = localDateTime.toString() // 2025-11-08T09:23:00
        )

        val branch = Branch(
            code = state.branches[state.selectedBranchIndex].branchCode,
            billingPoint = state.billingPoints[state.selectedBillingPointIndex].billingPoint
        )


        val customerId = if (isFinalCustomer) {
            null
        } else {
            state.customer?.id
        }

        var finalCustomerInfo: FinalCustomerInfo? = null
        if (isFinalCustomer && (state.finalName != null || state.finalIdNumber != null || state.finalEmail != null)) {
            var idType: String? = null
            if (state.finalIdNumber != null) {
                idType = state.finalIdType
            }
            finalCustomerInfo = FinalCustomerInfo(
                name = state.finalName,
                email = state.finalEmail,
                identificationType = idType,
                identificationNumber = state.finalIdNumber,
                countryCode = state.finalPassportCountry
            )
        }

        val thirdParty: List<ThirdParty> = emptyList()

        val orderItems = mutableListOf<OrderItem>()
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
        
        for (item in state.cart) {
            val product = getItemForLine(item) ?: continue

            // Item Discounts - send PER UNIT discount amount (not total)
            val orderItemDiscounts = mutableListOf<OrderItemDiscount>()
            val discountPerUnit = item.discountPerUnit()
            if (discountPerUnit > 0L) {
                orderItemDiscounts.add(
                    OrderItemDiscount(
                        amount = discountPerUnit.toDecimalString()
                    )
                )
            }

            // Item taxes
            val itemTaxes = mutableListOf<OrderItemTax>()
            // If tax exempt, override all ITBMS taxes to code 00, rate 0, amount 0
            if (state.taxExempt) {
                itemTaxes.add(
                    OrderItemTax(
                        code = "00",
                        type = "ITBMS",
                        description = "ITBMS",
                        rate = "0.00",
                        amount = "0.00"
                    )
                )
            } else {
                when (product.taxPercent) {
                    7 -> {
                        itemTaxes.add(
                            OrderItemTax(
                                code = "01",
                                type = "ITBMS",
                                description = "ITBMS",
                                rate = "0.07",
                                amount = item.taxTotal(state.taxExempt).toDecimalString()
                            )
                        )
                    }

                    10 -> {
                        itemTaxes.add(
                            OrderItemTax(
                                code = "02",
                                type = "ITBMS",
                                description = "ITBMS",
                                rate = "0.10",
                                amount = item.taxTotal(state.taxExempt).toDecimalString()
                            )
                        )
                    }

                    15 -> {
                        itemTaxes.add(
                            OrderItemTax(
                                code = "03",
                                type = "ITBMS",
                                description = "ITBMS",
                                rate = "0.15",
                                amount = item.taxTotal(state.taxExempt).toDecimalString()
                            )
                        )
                    }

                    else -> {
                        itemTaxes.add(
                            OrderItemTax(
                                code = "00",
                                type = "ITBMS",
                                description = "ITBMS",
                                rate = "0.00",
                                amount = "0.00"
                            )
                        )
                    }
                }
            }

            product.iscRate?.let {
                if (product.iscRate > 0.0 && !state.taxExempt) {
                    // Base for ISC is the subtotal after per-unit discounts
                    val base = item.lineSubtotal().coerceAtLeast(0L)
                    val iscAmount = ((base * product.iscRate) / 100.0).roundToLong()
                    itemTaxes.add(
                        OrderItemTax(
                            code = "",
                            type = "ISC",
                            description = "ISC",
                            rate = product.iscRate.toString(),
                            amount = iscAmount.toDecimalString()
                        )
                    )
                }
            }

            if (!state.taxExempt) {
                val otiList = product.otiTaxes.orEmpty()
                // Base for OTI is the subtotal after per-unit discounts
                val base = item.lineSubtotal().coerceAtLeast(0L)

                otiList.forEach { oti ->
                    val otiAmount = ((base * oti.rate) / 1.0).roundToLong() // backend already stores rate as decimal (e.g. 0.07)
                    itemTaxes.add(
                        OrderItemTax(
                            code = oti.id,             // ✅ code = OTIxx (e.g., OTI01, OTI02)
                            type = "OTI",
                            description = "OTI",
                            rate = oti.rate.toString(),
                            amount = otiAmount.toDecimalString()
                        )
                    )
                }
            }


            val additionalCharges = mutableListOf<Charge>()
            if ((item.shippingCents ?: 0L) > 0L) {
                additionalCharges.add(
                    Charge(
                        description = "FACTURA",
                        amount = (item.shippingCents ?: 0L).toDecimalString()
                    )
                )
            }
            if ((item.insuranceCents ?: 0L) > 0L) {
                additionalCharges.add(
                    Charge(
                        description = "SEGURO",
                        amount = (item.insuranceCents ?: 0L).toDecimalString()
                    )
                )
            }

            var pharmaSale: PharmaSale? = null
            if (product.isPharma) {
                pharmaSale = PharmaSale(
                    pharmaBatchNumber = item.pharmaBatchNumber ?: "",
                    pharmaBatchQuantity = item.pharmaBatchQty ?: 0,
                )
            }

            val additionalInfo = mutableListOf<NameValue>()
            product.additionalInfo?.let { infoJson ->
                val infoMap = infoJson.toMap() // You need to implement this extension function
                for ((key, value) in infoMap) {
                    additionalInfo.add(NameValue(name = key, value = value))
                }
            }


            val orderItem = OrderItem(
                itemId = item.itemId.toLong(),
                code = product.barcode ?: "0001",
                name = item.name,
                unitMeasure = product.unitMeasureCode,
                quantity = item.quantity,
                baseUnitPrice = item.baseUnitPrice.toDecimalString(),
                overrideUnitPrice = item.overrideUnitPrice?.toDecimalString(),
                orderItemDiscounts = orderItemDiscounts,
                orderItemTaxes = itemTaxes,
                additionalCharges = additionalCharges,
                totals = ItemTotals(
                    beforeDiscounts = item.lineSubtotalBeforeDiscount().toDecimalString(),
                    afterDiscounts = item.lineSubtotal().toDecimalString(),
                    beforeTaxes = item.lineSubtotal().toDecimalString(),
                    taxes = item.taxTotal(state.taxExempt).toDecimalString(),
                    afterTaxes = (item.total(state.taxExempt) - item.lineCharges()).toDecimalString(),
                    total = item.total(state.taxExempt).toDecimalString(),
                ),
                pharmaSale = pharmaSale,
                vehicleSale = null, // TODO hardcoded to null by now
                additionalInfo = additionalInfo,
                productType = product.productType.code
            )
            orderItems.add(orderItem)
        }

        val cartSummary = CartCalc.summarize(state)
        val charges = mutableListOf<InvoiceCharge>()
        state.globalShippingCents?.let {
            if (state.globalShippingCents > 0L && !state.cart.any {
                    (it.shippingCents ?: 0L) > 0L
                }) {
                charges.add(
                    InvoiceCharge(
                        type = "FACTURA",
                        amount = state.globalShippingCents.toDecimalString()
                    )
                )
            }
        }
        state.globalInsuranceCents?.let {
            if (state.globalInsuranceCents > 0L && !state.cart.any {
                    (it.insuranceCents ?: 0L) > 0L
                }) {
                charges.add(
                    InvoiceCharge(
                        type = "SEGURO",
                        amount = state.globalInsuranceCents.toDecimalString()
                    )
                )
            }
        }
        state.globalOtherChargesCents?.let {
            if (state.globalOtherChargesCents > 0L) {
                charges.add(
                    InvoiceCharge(
                        type = "OTROS_GASTOS",
                        amount = state.globalOtherChargesCents.toDecimalString()
                    )
                )
            }
        }

        val globalDiscounts = mutableListOf<InvoiceDiscount>()
        if (cartSummary.globalDiscount > 0L) {
            globalDiscounts.add(
                InvoiceDiscount(
                    description = "Decuento faactura",
                    amount = cartSummary.globalDiscount.toDecimalString()
                )
            )
        }

        val totals = CreateOrderTotals(
            quantityItems = state.cart.sumOf { it.quantity },
            charges = charges,
            discounts = globalDiscounts,
            subtotal = cartSummary.subtotal.toDecimalString(),
            totalBeforeDiscounts = cartSummary.subtotal.toDecimalString(),
            totalAfterDiscounts = (cartSummary.subtotal - cartSummary.lineDiscounts - cartSummary.globalDiscount).toDecimalString(),
            totalBeforeTaxes = (cartSummary.subtotal - cartSummary.lineDiscounts - cartSummary.globalDiscount).toDecimalString(),
            totalAfterTaxes = cartSummary.totalBeforeTip.toDecimalString(),
            totalTaxes = cartSummary.tax.toDecimalString(),
            invoiceTotal = cartSummary.totalBeforeTip.toDecimalString(),
        )

        var links: PaymentLinksBlock? = null
        val payments = mutableListOf<CreateOrderPayment>()
        if (createPaymentLink && !saveAsDraft) {
            links = PaymentLinksBlock(
                create = true,
                expireInMinutes = 140,
                note = "", // No note handled bu now
                method = "LINK" // For now just LINK. In future YAPPY QR
            )
        } else if (!saveAsDraft) {
            for ((key, value) in state.charged) {
                // For type 99 (OTHER), use custom description and ensure it's bigger than 10 characters
                val description = if (key == 99) {
                    val customDesc = state.otherPaymentDescription.ifBlank { 
                        manualMethodOptions()[key].second 
                    }
                    // Add meaningful text instead of padding with spaces (server trims spaces)
                    if (customDesc.length <= 10) {
                        "Otro: $customDesc"
                    } else {
                        customDesc
                    }
                } else {
                    manualMethodOptions()[key].second
                }
                
                payments.add(
                    CreateOrderPayment(
                        type = key,
                        description = description,
                        amount = value.toDecimalString(),
                        dueDate = null
                    )
                )
            }
            for (installment in state.installments) {
                // Convert "YYYY-MM-DD" to ISO 8601 format with time and timezone: "YYYY-MM-DDTHH:mm:ss-00:00"
                val dueDateIso = if (installment.dueDateIso.isNotBlank()) {
                    convertDateToIso8601(installment.dueDateIso)
                } else {
                    null
                }
                
                // Ensure installment description is bigger than 15 characters
                val baseDescription = "Cuota ${state.installments.indexOf(installment) + 1} de ${state.installments.size}"
                val description = if (baseDescription.length <= 15) {
                    // Add meaningful text instead of padding with spaces (server trims spaces)
                    "Pago en ${baseDescription.lowercase()}"
                } else {
                    baseDescription
                }
                
                payments.add(
                    CreateOrderPayment(
                        type = 11,
                        description = description,
                        amount = installment.amountCents.toDecimalString(),
                        dueDate = dueDateIso
                    )
                )
            }
        }

        var references: List<References>? = null
        if (state.referencedNoteCUFE.isNotEmpty()) {
            references = listOf(
                References(
                    legalName = "", // Empty backend fills this with business name
                    issueDatetime = state.referencedCreatedAt,
                    referenceNumber = ReferenceNumber(
                        type = "cufe", // For now only cufe references allowed
                        number = state.referencedNoteCUFE
                    )
                )
            )
        }

        val orderReference: OrderReference? = null // TODO add references if needed

        var retentions: Retentions? = null
        val retentionOption = retentionOptionsList.getOrNull(state.retentionCodeIndex)
        val retentionCode = retentionOption?.code.orEmpty()
        val retentionRate = when {
            retentionCode.isEmpty() -> ""
            retentionOption?.defaultRate != null -> retentionOption.defaultRate
            retentionCode == "8" -> state.retentionAmount
            else -> ""
        }
        if (retentionCode.isNotEmpty() && retentionRate.isNotBlank()) {
            retentions = Retentions(
                code = retentionCode,
                rate = retentionRate
            )
        }

        var exportation: Exportation? = null
        if (state.selectedDocType == "03" || state.selectedDocType == "10") { // Exportation type
            exportation = Exportation(
                incoterm = state.exportIncoterm,
                currency = state.exportCurrency,
                portOfLoading = state.exportPortOfLoading
            )
        }


        var logistics: Logistics? = null
        if (state.logisticsInfo.isNotEmpty()) {
            logistics = Logistics(
                totalPackagesNumber = state.logisticsBoxesQty.toIntOrNull() ?: 1,
                totalCargoWeight = state.logisticsTotalWeightLb.toDoubleOrNull() ?: 0.0,
                totalWeightUnit = "4",
                cargoVehicleLicense = state.logisticsVehiclePlate,
                carrierLegalName = state.logisticsCarrierLegalName,
                carrierTaxpayerType = if (state.logisticsCarrierTaxpayerTypeIndex == 0) "01" else "02",
                carrierTaxId = state.logisticsCarrierRuc,
                carrierTaxDv = state.logisticsCarrierDv,
                issuerLogisticsAdditionalInfo = state.logisticsInfo
            )
        }

        val deliveryLocation: DeliveryLocation? = if (state.deliveryReceiverRuc.isNotEmpty()) {
            DeliveryLocation(
                receiverLegalName = state.deliveryReceiverLegalName,
                receiverTaxpayerType = if (state.deliveryReceiverTaxpayerTypeIndex == 0) "01" else "02",
                receiverTaxId = state.deliveryReceiverRuc,
                receiverTaxDv = state.deliveryReceiverDv,
                contactPhone = state.deliveryContactPhone,
                alternateContactPhone = state.deliveryAltContactPhone,
                locationCode = "8-8-8", // TODO fix hardcoded code
            )
        } else {
            null
        }

        val commercialAddenda: CommercialAddenda? = null // TODO add commercial addenda if needed

        val formats = listOf("PDF", "XML")

        return CreateOrderRequest(
            invoice = invoice,
            branch = branch,
            customerId = customerId,
            finalCustomer = isFinalCustomer,
            finalCustomerInfo = finalCustomerInfo,
            thirdParties = thirdParty,
            orderItems = orderItems,
            totals = totals,
            payments = payments,
            references = references,
            orderReference = orderReference,
            retentions = retentions,
            exportation = exportation,
            logistics = logistics,
            deliveryLocation = deliveryLocation,
            commercialAddenda = commercialAddenda,
            links = links,
            formats = formats,
            saveAs = if (saveAsDraft) "draft" else "confirmed"
        )
    }

    fun onBranchSelected(index: Int) {
        updateBranchSelection(index)
    }

    fun onBillingPointSelected(index: Int) {
        updateState { copy(selectedBillingPointIndex = index) }
    }

    private fun updateBranchSelection(
        index: Int,
        branches: List<BranchModel> = uiState.value.branches,
        resetBillingPoint: Boolean = true
    ) {
        if (branches.isEmpty()) return
        val safeIndex = index.coerceIn(0, branches.lastIndex)
        val selectedBranch = branches[safeIndex]
        val billingPoints = selectedBranch.fiscalBillingPoints
        val billingPointIndex = if (resetBillingPoint) {
            0
        } else {
            val currentIndex = uiState.value.selectedBillingPointIndex
            if (billingPoints.isEmpty()) 0 else currentIndex.coerceIn(0, billingPoints.lastIndex)
        }
        updateState {
            copy(
                branches = branches,
                selectedBranchIndex = safeIndex,
                billingPoints = billingPoints,
                selectedBillingPointIndex = billingPointIndex
            )
        }
    }

    private fun applyQuoteDefaultBranch() {
        val branches = uiState.value.branches
        if (branches.isEmpty()) return
        val defaultIndex = branches.indexOfFirst { it.branchCode == DEFAULT_QUOTE_BRANCH_CODE }
        val resolvedIndex = if (defaultIndex >= 0) defaultIndex else 0
        updateBranchSelection(resolvedIndex, branches)
    }

    // TODO Convert to proper enum
    fun docTypeOptions(): List<String> = listOf(
        "01 - Factura de Operación Interna",
        "02 - Factura de Importación",
        "03 - Factura de Exportación",
        "04 - Nota de Crédito Referente a FE",
        "05 - Nota de Débito Referente a FE",
        "06 - Nota de Crédito Genérica",
        "07 - Nota de Débito Genérica",
        "08 - Factura de Zona Franca",
        "09 - Reembolso",
        "10 - Factura de Operación Extranjera"
    )

    fun onDocTypeSelected(index: Int) {
        val key = docTypeOptions()[index].take(2) // "01", "02", ...
        updateState { copy(selectedDocTypeIndex = index, selectedDocType = key) }
        if (key == "03") {
            onOperationNatureSelected(1)
            updateState {
                copy(
                    enabledOperationNature = false
                )
            }
        } else if (key == "10") {
            updateState {
                copy(
                    exportIncoterm = "CPT",
                    exportCurrency = "USD",
                    exportPortOfLoading = "No aplica"
                )
            }
        } else {
            updateState {
                copy(
                    enabledOperationNature = true
                )
            }
        }
    }

    // TODO Convert to proper enum
    // === Operation Nature ===
    fun operationNatureOptions(): List<String> = listOf(
        "01 - Venta",
        "02 - Exportación",
        "03 - Reexportación",
        "04 - Venta con fuente extranjera",
        "05 - Servicio de fuente extranjera",
        "10 - Transferencia",
        "11 - Devolución",
        "12 - Consignación",
        "13 - Remesa",
        "14 - Entrega gratuita",
        "20 - Compra",
        "21 - Importación"
    )

    fun onOperationNatureSelected(index: Int) {
        val key = operationNatureOptions()[index].take(2)
        updateState {
            copy(selectedOperationNatureIndex = index, selectedOperationNature = key)
        }
    }

    // === Customer ===
    fun onPickCustomerClick() {
        // navigate to customer picker or open dialog
    }

    fun setFlowMode(mode: FlowMode, quoteId: Long? = null) {
        updateState {
            copy(
                flowMode = mode,
                quoteId = quoteId,
                quoteStyleWasEdited = if (mode == FlowMode.QUOTE && quoteId == null) false else quoteStyleWasEdited
            )
        }
        if (mode == FlowMode.QUOTE) {
            observeQuoteSettings()
            applyQuoteDefaultBranch()
            if (quoteId != null) {
                loadQuoteForEdit(quoteId)
            }
        }
    }

    fun startSaleFromQuote(quoteId: Long) {
        viewModelScope.launch {
            try {
                val quote = withContext(Dispatchers.IO) {
                    quotesService.getQuote(GetQuoteRequest(quoteId = quoteId))
                }
                applyQuoteForSale(quote)
            } catch (e: Exception) {
                // Keep current state if we can't prefill.
            }
        }
    }

    fun setQuoteMeta(style: String? = null, expiryDate: String? = null, additionalInfo: String? = null) {
        updateState {
            copy(
                quoteStyle = style ?: quoteStyle,
                quoteStyleWasEdited = quoteStyleWasEdited || style != null,
                quoteExpiryDate = expiryDate ?: quoteExpiryDate,
                quoteAdditionalInfo = additionalInfo ?: quoteAdditionalInfo
            )
        }
    }

    private fun loadQuoteForEdit(quoteId: Long) {
        viewModelScope.launch {
            try {
                val quote = withContext(Dispatchers.IO) {
                    quotesService.getQuote(GetQuoteRequest(quoteId = quoteId))
                }
                applyQuoteForEdit(quote)
            } catch (e: Exception) {
                // Keep current state if we can't prefill.
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun applyQuoteForEdit(quote: Quote) {
        val current = uiState.value
        val availableItems = if (items.isNotEmpty()) items else current.items
        val cartLines = buildCartLinesFromQuote(quote, availableItems)
        val finalInfo = quote.finalCustomerInfo
        val hasCustomerId = quote.customerId != null
        val isFinalCustomer = !hasCustomerId
        val customerName = quote.customerName.orEmpty()
        val customerEmail = quote.customerEmail
        val customerPhone = quote.customerPhone
        val customerRuc = quote.customerRuc

        fun String?.nullIfBlank(): String? = this?.trim()?.takeIf { it.isNotBlank() }
        val finalName = finalInfo?.name.nullIfBlank() ?: customerName.nullIfBlank()
        val finalEmail = finalInfo?.email.nullIfBlank() ?: customerEmail.nullIfBlank()
        val finalPhone = finalInfo?.phone.nullIfBlank() ?: customerPhone.nullIfBlank()
        val finalIdType = finalInfo?.idType
        val finalIdNumber = finalInfo?.idNumber
        val finalCountry = finalInfo?.country
        val (finalIdTypeIndex, resolvedFinalIdType) = resolveFinalIdType(finalIdType)

        val hasTaxes = cartLines.any { line -> (line.tax?.rateBps ?: 0) > 0 }

        updateState {
            copy(
                cart = cartLines,
                personalizedItems = emptyMap(),
                taxExempt = !hasTaxes,
                quoteId = quote.id ?: current.quoteId,
                quoteStyle = quote.quoteStyle ?: current.quoteStyle,
                quoteExpiryDate = quote.expiryDate,
                quoteAdditionalInfo = quote.additionalInfo,
                finalCustomer = isFinalCustomer,
                customer = if (isFinalCustomer || customerName.isBlank()) {
                    null
                } else {
                    CustomerListItem(
                        id = quote.customerId,
                        name = customerName,
                        email = customerEmail,
                        ruc = customerRuc,
                        status = 1,
                        invoiceCustomer = 0,
                        updatedAt = now().epochSeconds
                    )
                },
                finalName = if (isFinalCustomer) finalName else null,
                finalEmail = if (isFinalCustomer) finalEmail else null,
                finalPhone = if (isFinalCustomer) finalPhone else null,
                finalIdTypeIndex = if (isFinalCustomer) finalIdTypeIndex else 0,
                finalIdType = if (isFinalCustomer) resolvedFinalIdType else current.finalIdType,
                finalIdNumber = if (isFinalCustomer) finalIdNumber else null,
                finalPassportCountry = if (isFinalCustomer) finalCountry else null,
                globalDiscountMode = GlobalDiscountMode.NONE,
                globalDiscountPercent = 0,
                globalDiscountFixedCents = 0L,
                globalShippingCents = 0L,
                globalInsuranceCents = 0L,
                globalOtherChargesCents = 0L
            )
        }

        val branchCode = quote.branchCode
        if (!branchCode.isNullOrBlank()) {
            val branches = uiState.value.branches
            val branchIndex = branches.indexOfFirst { it.branchCode == branchCode }
            if (branchIndex >= 0) {
                updateBranchSelection(branchIndex, branches)
            } else {
                pendingQuoteBranchCode = branchCode
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun applyQuoteForSale(quote: Quote) {
        val current = uiState.value
        val availableItems = if (items.isNotEmpty()) items else current.items
        val cartLines = buildCartLinesFromQuote(quote, availableItems)
        val finalInfo = quote.finalCustomerInfo
        val hasCustomerId = quote.customerId != null
        val isFinalCustomer = !hasCustomerId
        val customerName = quote.customerName.orEmpty()
        val customerEmail = quote.customerEmail
        val customerPhone = quote.customerPhone
        val customerRuc = quote.customerRuc

        fun String?.nullIfBlank(): String? = this?.trim()?.takeIf { it.isNotBlank() }
        val finalName = finalInfo?.name.nullIfBlank() ?: customerName.nullIfBlank()
        val finalEmail = finalInfo?.email.nullIfBlank() ?: customerEmail.nullIfBlank()
        val finalPhone = finalInfo?.phone.nullIfBlank() ?: customerPhone.nullIfBlank()
        val finalIdType = finalInfo?.idType
        val finalIdNumber = finalInfo?.idNumber
        val finalCountry = finalInfo?.country
        val (finalIdTypeIndex, resolvedFinalIdType) = resolveFinalIdType(finalIdType)

        val hasTaxes = cartLines.any { line -> (line.tax?.rateBps ?: 0) > 0 }

        updateState {
            copy(
                flowMode = FlowMode.SALE,
                quoteId = null,
                cart = cartLines,
                personalizedItems = emptyMap(),
                taxExempt = !hasTaxes,
                finalCustomer = isFinalCustomer,
                customer = if (isFinalCustomer || quote.customerId == null || customerName.isBlank()) {
                    null
                } else {
                    CustomerListItem(
                        id = quote.customerId,
                        name = customerName,
                        email = customerEmail,
                        ruc = customerRuc,
                        status = 1,
                        invoiceCustomer = 0,
                        updatedAt = now().epochSeconds
                    )
                },
                finalName = if (isFinalCustomer) finalName else null,
                finalEmail = if (isFinalCustomer) finalEmail else null,
                finalPhone = if (isFinalCustomer) finalPhone else null,
                finalIdTypeIndex = if (isFinalCustomer) finalIdTypeIndex else 0,
                finalIdType = if (isFinalCustomer) resolvedFinalIdType else current.finalIdType,
                finalIdNumber = if (isFinalCustomer) finalIdNumber else null,
                finalPassportCountry = if (isFinalCustomer) finalCountry else null,
                globalDiscountMode = GlobalDiscountMode.NONE,
                globalDiscountPercent = 0,
                globalDiscountFixedCents = 0L,
                globalShippingCents = 0L,
                globalInsuranceCents = 0L,
                globalOtherChargesCents = 0L
            )
        }

        val branchCode = quote.branchCode
        if (!branchCode.isNullOrBlank()) {
            val branches = uiState.value.branches
            val branchIndex = branches.indexOfFirst { it.branchCode == branchCode }
            if (branchIndex >= 0) {
                updateBranchSelection(branchIndex, branches)
            }
        }
    }

    private fun resolveFinalIdType(value: String?): Pair<Int, String> {
        val keys = finalIdTypeKeys()
        val resolvedIndex = value?.let { keys.indexOf(it) } ?: -1
        val safeIndex = if (resolvedIndex >= 0) resolvedIndex else 0
        return safeIndex to keys[safeIndex]
    }

    private fun buildCartLinesFromQuote(quote: Quote, products: List<Item>): List<CartLine> {
        return quote.lines.orEmpty().map { line ->
            val itemId = (line.itemId ?: 0L).toInt()
            val product = products.firstOrNull { it.itemId == itemId }
            val quantity = line.quantity?.toInt()?.coerceAtLeast(1) ?: 1
            val unitPriceCents = line.unitPrice?.toLongCents() ?: 0L
            val baseUnitPrice = product?.price?.toLongCents() ?: unitPriceCents
            val overrideUnitPrice = if (product != null && unitPriceCents > 0 && unitPriceCents != baseUnitPrice) {
                unitPriceCents
            } else {
                null
            }
            val discount = buildDiscountFromQuote(line)
            val tax = buildTaxFromQuote(line, product)

            CartLine(
                lineId = "${itemId}-${randomUUID()}",
                itemId = itemId,
                name = line.itemName ?: product?.name ?: "Item",
                baseUnitPrice = baseUnitPrice,
                overrideUnitPrice = overrideUnitPrice,
                quantity = quantity,
                discount = discount,
                tax = tax
            )
        }
    }

    private fun buildDiscountFromQuote(line: QuoteLine): Discount? {
        val value = line.discountValue ?: return null
        if (value <= 0.0) return null
        return when (line.discountMode) {
            1 -> {
                val percent = if (value <= 1.0) value * 100.0 else value
                Discount.Percent((percent * 100).roundToInt().coerceAtLeast(0))
            }
            2 -> Discount.Amount(value.toLongCents())
            else -> Discount.Amount(value.toLongCents())
        }
    }

    private fun buildTaxFromQuote(line: QuoteLine, product: Item?): Tax? {
        val productTax = product?.taxPercent?.takeIf { it > 0 }
        val taxPercent = productTax?.toDouble() ?: parseTaxPercent(line.taxRate) ?: return null
        val rateBps = (taxPercent * 100).roundToInt()
        if (rateBps <= 0) return null
        return Tax(
            id = rateBps,
            name = line.taxName ?: "ITBMS",
            rateBps = rateBps
        )
    }

    private fun parseTaxPercent(rate: String?): Double? {
        val raw = rate?.trim()?.toDoubleOrNull() ?: return null
        if (raw <= 0.0) return null
        return if (raw <= 1.0) raw * 100.0 else raw
    }

    fun buildQuoteRequest(): com.teco.ventago.features.quotes.domain.models.requests.CreateQuoteRequest {
        val state = uiState.value
        return QuoteRequestBuilder.build(
            state = state,
            quoteStyle = state.quoteStyle,
            expiryDate = state.quoteExpiryDate,
            additionalInfo = state.quoteAdditionalInfo,
            quoteId = state.quoteId
        )
    }

    suspend fun createQuote(): Quote = withContext(Dispatchers.IO) {
        val request = buildQuoteRequest().copy(quoteId = null)
        val quote = quotesService.createQuote(request)
        updateState { copy(lastQuoteId = quote.id, lastQuoteNumber = quote.displayNumberOrQuoteNumber) }
        quote
    }

    suspend fun updateQuote(): Quote = withContext(Dispatchers.IO) {
        val state = uiState.value
        val createReq = buildQuoteRequest().copy(quoteId = state.quoteId ?: 0)
        val updateReq = com.teco.ventago.features.quotes.domain.models.requests.UpdateQuoteRequest(
            customerId = createReq.customerId,
            finalCustomer = createReq.finalCustomer,
            finalCustomerInfo = createReq.finalCustomerInfo,
            quoteStyle = createReq.quoteStyle,
            expiryDate = createReq.expiryDate,
            items = createReq.items,
            totals = createReq.totals,
            includePaymentButton = createReq.includePaymentButton,
            additionalInfo = createReq.additionalInfo,
            quoteId = createReq.quoteId ?: 0
        )
        val quote = quotesService.updateQuote(updateReq)
        updateState { copy(lastQuoteId = quote.id, lastQuoteNumber = quote.displayNumberOrQuoteNumber) }
        quote
    }

    suspend fun openQuotePdf(quoteId: Long?) {
        if (quoteId == null) return
        val pdfB64 = withContext(Dispatchers.IO) { quotesService.getQuotePdf(quoteId) }
        val pdfBytes = Base64.decode(pdfB64)
        val filename = "quote_${quoteId}.pdf"
        withContext(Dispatchers.Main) {
            pdfSharer.openPdf(filename, pdfBytes)
        }
    }

    suspend fun sendQuoteEmail(quoteId: Long?, recipientEmail: String): Boolean {
        if (quoteId == null) return false
        return withContext(Dispatchers.IO) {
            quotesService.sendQuoteEmail(
                SendQuoteEmailRequest(quoteId = quoteId, recipientEmail = recipientEmail)
            )
        }
    }

    fun onFinalCustomerToggle(isFinal: Boolean) {
        updateState {
            copy(finalCustomer = isFinal, customer = if (isFinal) null else customer)
        }
    }

    fun onFinalNameChanged(v: String) = updateState { copy(finalName = v) }
    fun onFinalEmailChanged(v: String) = updateState { copy(finalEmail = v) }
    fun onFinalPhoneChanged(v: String) = updateState { copy(finalPhone = v) }

    // TODO Convert to proper enum
    fun finalIdTypeDisplayNames(): List<String> = listOf(
        "Cédula",
        "Pasaporte",
        "Identificación Extranjera"
    )
    
    private fun finalIdTypeKeys(): List<String> = listOf("cedula", "passport", "foreing_taxid")
    
    fun onFinalIdTypeSelected(idx: Int) {
        val types = finalIdTypeKeys()
        if (idx in types.indices) {
            updateState { copy(finalIdTypeIndex = idx, finalIdType = types[idx]) }
        }
    }

    fun onFinalIdNumberChanged(v: String) = updateState { copy(finalIdNumber = v) }
    fun onFinalPassportCountryChanged(v: String) = updateState { copy(finalPassportCountry = v) }


    // ---------- Per-line extras ----------
    fun setLineShipping(lineId: String, cents: Long?) = updateState {
        copy(cart = cart.map {
            if (it.lineId == lineId) it.copy(
                shippingCents = cents?.coerceAtLeast(
                    0L
                )
            ) else it
        })
    }

    fun setLineInsurance(lineId: String, cents: Long?) = updateState {
        copy(cart = cart.map {
            if (it.lineId == lineId) it.copy(
                insuranceCents = cents?.coerceAtLeast(
                    0L
                )
            ) else it
        })
    }

    fun setLinePharma(lineId: String, batchNumber: String?, batchQty: Int?) = updateState {
        copy(cart = cart.map {
            if (it.lineId == lineId) it.copy(
                pharmaBatchNumber = batchNumber?.takeIf { it.isNotBlank() },
                pharmaBatchQty = batchQty?.coerceAtLeast(0)
            ) else it
        })
    }

    // ---------- Global discount / charges ----------
    fun applyGlobalSettings(
        mode: GlobalDiscountMode,
        discountValue: Long,           // if PERCENT: 0..100; if FIXED: cents; NONE: 0
        shippingCents: Long?,          // nullable; ignored if any item has shipping
        insuranceCents: Long?,         // nullable; ignored if any item has insurance
        otherCents: Long?
    ) = updateState {
        val anyItemHasShipping = cart.any { (it.shippingCents ?: 0L) > 0L }
        val anyItemHasInsurance = cart.any { (it.insuranceCents ?: 0L) > 0L }

        when (mode) {
            GlobalDiscountMode.NONE -> copy(
                globalDiscountMode = mode,
                globalDiscountPercent = 0,
                globalDiscountFixedCents = 0L,
                globalShippingCents = if (anyItemHasShipping) null else shippingCents?.coerceAtLeast(
                    0L
                ),
                globalInsuranceCents = if (anyItemHasInsurance) null else insuranceCents?.coerceAtLeast(
                    0L
                ),
                globalOtherChargesCents = otherCents?.coerceAtLeast(0L)
            )

            GlobalDiscountMode.PERCENT -> copy(
                globalDiscountMode = mode,
                globalDiscountPercent = discountValue.toInt().coerceIn(0, 100),
                globalDiscountFixedCents = 0L,
                globalShippingCents = if (anyItemHasShipping) null else shippingCents?.coerceAtLeast(
                    0L
                ),
                globalInsuranceCents = if (anyItemHasInsurance) null else insuranceCents?.coerceAtLeast(
                    0L
                ),
                globalOtherChargesCents = otherCents?.coerceAtLeast(0L)
            )

            GlobalDiscountMode.FIXED -> copy(
                globalDiscountMode = mode,
                globalDiscountPercent = 0,
                globalDiscountFixedCents = discountValue.coerceAtLeast(0L),
                globalShippingCents = if (anyItemHasShipping) null else shippingCents?.coerceAtLeast(
                    0L
                ),
                globalInsuranceCents = if (anyItemHasInsurance) null else insuranceCents?.coerceAtLeast(
                    0L
                ),
                globalOtherChargesCents = otherCents?.coerceAtLeast(0L)
            )
        }
    }

    // Convenience for UI (to disable global fields)
    fun anyItemHasShipping(): Boolean = uiState.value.cart.any { (it.shippingCents ?: 0L) > 0L }
    fun anyItemHasInsurance(): Boolean = uiState.value.cart.any { (it.insuranceCents ?: 0L) > 0L }


    fun onTipAmountChange(value: Long) {
        updateState { copy(tipAmount = value.coerceAtLeast(0L)) }
    }

    fun getCartTotal(): Money = CartCalc.summarize(uiState.value).subtotal
    fun getDiscountedTotal(): Money = CartCalc.summarize(uiState.value).totalBeforeTip
    fun getSubtotalAmount(): Money = CartCalc.summarize(uiState.value).subtotal
    fun getTotalAmount(): Money = CartCalc.summarize(uiState.value).grandTotal
    fun getTipsTotal(): Money = CartCalc.summarize(uiState.value).tip
    fun getLinesDiscountTotal(): Money = CartCalc.summarize(uiState.value).lineDiscounts
    fun getGlobalDiscountTotal(): Money = CartCalc.summarize(uiState.value).globalDiscount
    fun getDiscountTotal(): Money = getLinesDiscountTotal() + getGlobalDiscountTotal()

    fun getItemsShippingTotal(): Money {
        val state = uiState.value
        val lines = state.cart
        val linesShipping = lines.sumOf { it.shippingCents ?: 0L }
        return linesShipping
    }

    fun getItemsInsuranceTotal(): Money {
        val state = uiState.value
        val lines = state.cart
        val linesInsurance = lines.sumOf { it.insuranceCents ?: 0L }
        return linesInsurance
    }

    fun getGlobalOtherCharges(): Money = uiState.value.globalOtherChargesCents ?: 0L

    fun getTaxes(): Money = CartCalc.summarize(uiState.value).tax

    fun getISC(): Money = CartCalc.summarize(uiState.value).isc
    fun getOTI(): Money = CartCalc.summarize(uiState.value).oti
    fun getITBMS(): Money = CartCalc.summarize(uiState.value).itbms

    /* ---------- Sheet visibility ---------- */
    fun openAdditionalSheet() = updateState { copy(showAdditionalSheet = true) }
    fun closeAdditionalSheet() = updateState { copy(showAdditionalSheet = false) }

    /* ---------- Options providers ---------- */
    fun taxpayerTypeOptions(): List<String> = listOf("01 - Natural", "02 - Jurídico")
    private data class RetentionOption(
        val code: String,
        val label: String,
        val defaultRate: String?
    )

    private val retentionOptionsList = listOf(
        RetentionOption("", "Sin retención", null),
        RetentionOption("1", "Estado (Profesional) 100%", "100"),
        RetentionOption("2", "Estado (Bienes/Servicios) 50%", "50"),
        RetentionOption("3", "No domiciliado exterior 100%", "100"),
        RetentionOption("4", "Compra Bienes/Servicios 50%", "50"),
        RetentionOption("7", "Comercio afiliado TCTD 50%", "50"),
        RetentionOption("8", "Otros (disminución)", null)
    )

    fun retentionOptions(): List<Pair<String, String>> =
        retentionOptionsList.map { it.code to it.label }

    fun retentionLabels(): List<String> = retentionOptionsList.map { it.label }
    fun retentionCodeAt(index: Int): String = retentionOptionsList.getOrNull(index)?.code ?: ""

    // 🗺️ Replace these with your real data sources
    fun provinceOptions(): List<String> = listOf("Panamá", "Panamá Oeste", "Colón")
    fun districtOptions(provIdx: Int): List<String> =
        when (provIdx) {
            0 -> listOf("Panamá", "San Miguelito", "La Chorrera (NO, es Panamá Oeste)")
            1 -> listOf("Arraiján", "La Chorrera")
            2 -> listOf("Colón", "Portobelo")
            else -> emptyList()
        }

    fun corregOptions(provIdx: Int, distIdx: Int): List<String> =
        when (provIdx to distIdx) {
            (0 to 0) -> listOf("Bella Vista", "Betania", "El Cangrejo")
            (0 to 1) -> listOf("Amelia Denis", "Arnulfo Arias")
            (1 to 0) -> listOf("Burunga", "Vista Alegre")
            else -> listOf("Centro", "Norte", "Sur")
        }

    /* ---------- Logistics setters ---------- */
    fun onLogInfo(v: String) = updateState { copy(logisticsInfo = v) }
    fun onLogPlate(v: String) = updateState { copy(logisticsVehiclePlate = v) }
    fun onLogCarrierName(v: String) = updateState { copy(logisticsCarrierLegalName = v) }
    fun onLogCarrierRuc(v: String) = updateState { copy(logisticsCarrierRuc = v) }
    fun onLogCarrierDv(v: String) = updateState { copy(logisticsCarrierDv = v) }
    fun onLogCarrierType(idx: Int) = updateState { copy(logisticsCarrierTaxpayerTypeIndex = idx) }
    fun onLogBoxes(v: String) = updateState { copy(logisticsBoxesQty = v.filter { it.isDigit() }) }
    fun onLogWeightLb(v: String) = updateState {
        copy(logisticsTotalWeightLb = v.filter { it.isDigit() || it == '.' }.take(10))
    }

    /* ---------- Delivery setters ---------- */
    fun onDelName(v: String) = updateState { copy(deliveryReceiverLegalName = v) }
    fun onDelRuc(v: String) = updateState { copy(deliveryReceiverRuc = v) }
    fun onDelDv(v: String) = updateState { copy(deliveryReceiverDv = v) }
    fun onDelType(idx: Int) = updateState { copy(deliveryReceiverTaxpayerTypeIndex = idx) }
    fun onDelPhone(v: String) = updateState { copy(deliveryContactPhone = v) }
    fun onDelAltPhone(v: String) = updateState { copy(deliveryAltContactPhone = v) }

    fun onProvinceSelected(idx: Int) = updateState {
        copy(
            deliveryProvinceIndex = idx,
            deliveryDistrictIndex = 0,
            deliveryCorregIndex = 0
        )
    }

    fun onDistrictSelected(idx: Int) = updateState {
        copy(deliveryDistrictIndex = idx, deliveryCorregIndex = 0)
    }

    fun onCorregSelected(idx: Int) = updateState { copy(deliveryCorregIndex = idx) }

    /* ---------- Retention setters ---------- */
    fun onRetentionSelected(idx: Int) = updateState {
        val option = retentionOptionsList.getOrNull(idx) ?: retentionOptionsList.first()
        val nextAmount = when {
            option.code.isEmpty() -> ""
            option.defaultRate != null -> option.defaultRate
            option.code == "8" && retentionCodeAt(retentionCodeIndex) == "8" -> retentionAmount
            else -> ""
        }
        copy(retentionCodeIndex = idx, retentionAmount = nextAmount)
    }
    fun onRetentionAmount(v: String) = updateState {
        // Only allow digits (no decimals), filter and limit to 3 digits (0-100)
        val filtered = v.filter { it.isDigit() }.take(3)
        // Validate that the value is between 0-100
        val validated = if (filtered.isEmpty()) {
            ""
        } else {
            val numValue = filtered.toIntOrNull() ?: 0
            if (numValue > 100) "100" else filtered
        }
        copy(retentionAmount = validated)
    }

    fun selectedRetentionRequiresAmount(): Boolean =
        retentionCodeAt(uiState.value.retentionCodeIndex) == "8"

    /* ---------- Exportation setters ---------- */
    fun onIncoterm(v: String) = updateState { copy(exportIncoterm = v.uppercase()) }
    fun onExportCurrency(v: String) = updateState { copy(exportCurrency = v.uppercase()) }
    fun onPortOfLoading(v: String) = updateState { copy(exportPortOfLoading = v) }


    // ---------- Tips sheet ----------
    // ---------- Totals helpers ----------
    /** Legal invoice total (goes to PAC): NO tips. */
    fun legalInvoiceTotal(): Long = CartCalc.summarize(uiState.value).totalBeforeTip

    /** Tips (from state.tipAmount/tipIsPercentage computed over legal base) */
    fun tipsTotal(): Long = CartCalc.summarize(uiState.value).tip

    /** Amount to charge = legal + tips */
    fun amountToCharge(): Long = (legalInvoiceTotal() + tipsTotal()).coerceAtLeast(0L)

    // ---------- Payment flow ----------
    fun setPaymentFlow(mode: PaymentFlowMode) = updateState {
        // If switching to PAYMENT_LINK, clear installments and manual charged amounts
        if (mode == PaymentFlowMode.PAYMENT_LINK) {
            copy(
                paymentFlowMode = mode,
                charged = emptyMap(),
                installments = emptyList()
            )
        } else {
            copy(paymentFlowMode = mode)
        }
    }

    // ---------- Manual payments ----------
    /** Label shown to users for DGI codes. */
    fun manualMethodOptions(): List<Pair<Int, String>> = listOf(
        1 to "Crédito (cuentas por cobrar)",
        2 to "Efectivo",
        3 to "Tarjeta crédito",
        4 to "Tarjeta débito",
        5 to "Tarjeta fidelización",
        6 to "Vale",
        7 to "Tarjeta de regalo",
        8 to "Transferencia bancaria",
        9 to "Cheque",
        10 to "Punto Pago",
        99 to "Otro (especificar)"
    )

    fun toggleManualMethod(code: Int, enabled: Boolean) = updateState {
        if (!enabled) {
            copy(charged = charged - code)
        } else {
            if (!charged.containsKey(code)) copy(charged = charged + (code to 0L)) else this
        }
    }

    fun setManualAmount(code: Int, cents: Long) {
        val totalToCharge = amountToCharge()     // legal + tips
        val charged = uiState.value.charged.toMutableMap()
        val remaining = totalToCharge - charged.filterKeys { it != code }.values.sum()

        var finalAmount = cents

        // If NOT cash, cap at remaining
        if (code != 2) { // 02 = Cash
            finalAmount = minOf(remaining, cents)
        }

        charged[code] = finalAmount.coerceAtLeast(0L)
        updateState { copy(charged = charged) }
    }


    /**
     * Calculate the total change (cash overpayment)
     */
    fun calculateChange(): Long {
        val total = amountToCharge()
        val chargedTotal = uiState.value.charged.values.sum()
        val cash = uiState.value.charged[2] ?: 0L
        val nonCashTotal = chargedTotal - cash
        val effectiveDue = (total - nonCashTotal).coerceAtLeast(0L)

        return if (cash > effectiveDue) cash - effectiveDue else 0L
    }

    fun setOtherDescription(text: String) = updateState { copy(otherPaymentDescription = text) }

    fun manualPaidSum(): Long = uiState.value.charged.values.sum()

    // ---------- Installments ----------
    fun addInstallment() = updateState {
        if (paymentFlowMode == PaymentFlowMode.PAYMENT_LINK) this
        else {
            // Calculate default due date: 30 days from now
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val defaultDueDate = today.plus(30, DateTimeUnit.DAY)
            val defaultDueDateIso = "${defaultDueDate.year}-${defaultDueDate.monthNumber.toString().padStart(2, '0')}-${defaultDueDate.dayOfMonth.toString().padStart(2, '0')}"
            
            copy(installments = installments + InstallmentUI(dueDateIso = defaultDueDateIso))
        }
    }

    fun removeInstallment(index: Int) = updateState {
        if (index !in installments.indices) this
        else copy(installments = installments.toMutableList().also { it.removeAt(index) })
    }

    fun setInstallmentAmount(index: Int, cents: Long) {
        val totalToCharge = amountToCharge() // legal + tips
        val state = uiState.value
        
        // Calculate remaining amount: total - manual payments - other installments (excluding current one)
        val manualPaid = state.charged.values.sum()
        val otherInstallments = state.installments.filterIndexed { i, _ -> i != index }
            .sumOf { it.amountCents }
        val remaining = totalToCharge - manualPaid - otherInstallments
        
        // Cap the installment amount at the remaining total
        val finalAmount = cents.coerceIn(0L, remaining.coerceAtLeast(0L))
        
        updateState {
            if (index !in installments.indices) this
            else copy(installments = installments.toMutableList().also {
                it[index] = it[index].copy(amountCents = finalAmount)
            })
        }
    }

    fun setInstallmentDueDate(index: Int, iso: String) = updateState {
        if (index !in installments.indices) this
        else copy(installments = installments.toMutableList().also {
            it[index] = it[index].copy(dueDateIso = iso)
        })
    }

    fun installmentsSum(): Long = uiState.value.installments.sumOf { it.amountCents }

    // ---------- Tips ----------
    fun openTipsSheet() = updateState { copy(showTipsSheet = true) }
    fun closeTipsSheet() = updateState { copy(showTipsSheet = false) }
    fun setTipIsPercent(isPercent: Boolean) = updateState { copy(tipIsPercentage = isPercent) }
    fun setTipAmountRaw(majorText: String) {
        // Accept digits only -> cents
        val cents = majorText.filter { it.isDigit() }.toLongOrNull() ?: 0L
        updateState { copy(tipAmount = cents) }
    }

    fun setTipPercentRaw(percentText: String) {
        val p = percentText.filter(Char::isDigit).toIntOrNull()?.coerceIn(0, 100) ?: 0
        // We store tipAmount as either "cents (fixed)" OR "basis points of base?" — your existing code uses:
        // tipIsPercentage + tipAmount but interprets as percent over base (0..100) or as cents.
        // Keeping your prior logic: if percentage, uiState.tipAmount holds the percent (0..100) *NOT* bps.
        updateState { copy(tipAmount = p.toLong()) }
    }

    // Helpers for UI binding
    fun tipPercent(): Int =
        if (uiState.value.tipIsPercentage) uiState.value.tipAmount.toInt() else 0

    fun tipFixedCents(): Long = if (!uiState.value.tipIsPercentage) uiState.value.tipAmount else 0L

    // ---------- Validation helpers for button enablement ----------
    fun paymentLinkEnabled(): Boolean =
        uiState.value.paymentFlowMode == PaymentFlowMode.PAYMENT_LINK

    fun manualOrInstallmentsEnabled(): Boolean =
        uiState.value.paymentFlowMode == PaymentFlowMode.MANUAL_OR_INSTALLMENTS

    /** Remaining (legal+tips) minus manual-minus-installments. */
    fun remainingToAllocate(): Long {
        val total = amountToCharge() // includes tips
        val allocated = manualPaidSum() + installmentsSum()
        return (total - allocated).coerceAtLeast(0L)
    }

    /**
     * Converts a date string from "YYYY-MM-DD" format to ISO 8601 format with time and timezone.
     * Example: "2025-12-06" -> "2025-12-06T00:00:00-00:00"
     */
    private fun convertDateToIso8601(dateString: String): String? {
        return try {
            if (dateString.isBlank()) return null
            
            // Parse "YYYY-MM-DD" to LocalDate
            val parts = dateString.split("-")
            if (parts.size != 3) return null
            
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val day = parts[2].toInt()
            
            val localDate = LocalDate(year, month, day)
            
            // Convert to LocalDateTime at midnight (00:00:00)
            val localDateTime = LocalDateTime(
                date = localDate,
                time = LocalTime(0, 0, 0)
            )
            
            // Format as ISO 8601 with timezone offset: "YYYY-MM-DDTHH:mm:ss-00:00"
            val yearStr = localDateTime.year.toString()
            val monthStr = localDateTime.monthNumber.toString().padStart(2, '0')
            val dayStr = localDateTime.dayOfMonth.toString().padStart(2, '0')
            val hourStr = localDateTime.hour.toString().padStart(2, '0')
            val minuteStr = localDateTime.minute.toString().padStart(2, '0')
            val secondStr = localDateTime.second.toString().padStart(2, '0')
            
            "$yearStr-$monthStr-${dayStr}T$hourStr:$minuteStr:$secondStr-00:00"
        } catch (e: Exception) {
            null
        }
    }

    fun openPdfDocument() {
        val state = uiState.value
        if (state.pdfDocument.isBlank()) return
        val pdfB64 = state.pdfDocument

        @OptIn(ExperimentalEncodingApi::class)
        val pdfBytes = Base64.decode(pdfB64)
        val filename = "${state.orderNumber}.pdf"
        pdfSharer.openPdf(filename, pdfBytes)
    }

    private fun observeQuoteSettings() {
        viewModelScope.launch {
            quotesService.quoteSettings().collect { settings ->
                if (settings != null) {
                    applyQuoteSettings(settings)
                }
            }
        }
    }

    private fun applyQuoteSettings(settings: QuoteSettings) {
        updateState {
            copy(
                quoteAdditionalInfo = if (quoteId == null) {
                    settings.defaultAdditionalInfo
                } else {
                    quoteAdditionalInfo
                },
                quoteStyle = if (quoteId == null && !quoteStyleWasEdited) {
                    settings.defaultQuoteStyle
                } else {
                    quoteStyle
                },
                quotesSettings = settings
            )
        }
    }

    fun saveQuoteAdditionalInfoAsDefault() {
        viewModelScope.launch {
            showLoading()
            try {
                val currentInfo = _uiState.value.quoteAdditionalInfo.orEmpty()
                val settings = _uiState.value.quotesSettings
                val payload = settings?.copy(defaultAdditionalInfo = currentInfo)
                    ?: QuoteSettings(
                        defaultQuoteStyle = "style1",
                        defaultAdditionalInfo = currentInfo,
                        defaultIncludePaymentButton = false
                    )
                withContext(Dispatchers.IO) {
                    quotesService.updateQuoteSettings(payload)
                }
            } finally {
                hideLoading()
            }
        }
    }
}
