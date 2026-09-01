package com.teco.ventago.features.pos.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.core.LocalStorage
import com.teco.ventago.core.PdfSharer
import com.teco.ventago.core.SnackbarService
import com.teco.ventago.core.firebase.AnalyticsService
import com.teco.ventago.core.authz.ActionKey
import com.teco.ventago.core.authz.AuthzEvaluator
import com.teco.ventago.core.authz.ScopeKey
import com.teco.ventago.core.beta.BetaFeature
import com.teco.ventago.core.beta.BetaService
import com.teco.ventago.design_system.molecules.pos.DiscountMode
import com.teco.ventago.design_system.molecules.pos.GlobalDiscountMode
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.branches.domain.BranchService
import com.teco.ventago.features.branches.domain.model.Branch as BranchModel
import com.teco.ventago.features.branches.domain.model.FiscalBillingPoint
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.business.domain.model.Business
import com.teco.ventago.features.customers.domain.CustomerService
import com.teco.ventago.features.customers.domain.models.CustomerDetails
import com.teco.ventago.features.customers.domain.models.CustomerListItem
import com.teco.ventago.features.customers.domain.models.CustomerTaxRetentionCatalog
import com.teco.ventago.features.financialProfile.domain.FinancialProfileService
import com.teco.ventago.features.invoicing.domain.InvoicingSettingsService
import com.teco.ventago.features.invoicing.domain.PostCreateInvoiceWarningState
import com.teco.ventago.features.invoicing.domain.resolvePostCreateInvoiceWarning
import com.teco.ventago.features.invoicing.domain.models.InvoiceStatus
import com.teco.ventago.features.orders.domain.OrderService
import com.teco.ventago.features.orders.domain.OrderPaymentSubmission
import com.teco.ventago.features.orders.domain.models.CustomerSnapshot
import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.orders.domain.models.PaymentStatus
import com.teco.ventago.features.orders.domain.models.requests.Branch
import com.teco.ventago.features.orders.domain.models.requests.Charge
import com.teco.ventago.features.orders.domain.models.requests.CommercialAddenda
import com.teco.ventago.features.orders.domain.models.requests.AdditionalAddress
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
import com.teco.ventago.features.orders.domain.models.requests.PendingIntentReleaseResponse
import com.teco.ventago.features.orders.domain.models.requests.RegisterManualPaymentsDataResponse
import com.teco.ventago.features.orders.domain.models.requests.ThirdParty
import com.teco.ventago.features.orders.domain.models.responses.CreateOrderLinkDto
import com.teco.ventago.features.orders.domain.models.responses.OnsitePaymentDto
import com.teco.ventago.features.orders.domain.models.responses.YAPPY_ONSITE_PENDING_TRANSACTION_EXISTS
import com.teco.ventago.features.orders.domain.models.responses.YappyOnsitePendingTransactionDto
import com.teco.ventago.features.orders.domain.models.responses.YappyOnsitePendingTransactionExistsException
import com.teco.ventago.features.payments.domain.PaymentService
import com.teco.ventago.features.payments.domain.models.YappyOnsiteDevice
import com.teco.ventago.features.pos.domain.PosService
import com.teco.ventago.features.printers.domain.PrinterService
import com.teco.ventago.features.printers.domain.model.PrintContext
import com.teco.ventago.features.printers.domain.model.TicketDocumentPayload
import com.teco.ventago.features.pos.domain.models.CartLine
import com.teco.ventago.features.pos.domain.models.Discount
import com.teco.ventago.features.pos.domain.models.Money
import com.teco.ventago.features.pos.domain.models.Tax
import com.teco.ventago.features.pos.provisioning.domain.PosDeviceProvisioningService
import com.teco.ventago.features.product.domain.ProductService
import com.teco.ventago.features.inventory.domain.InventoryAvailabilityStore
import com.teco.ventago.features.inventory.domain.InventoryKardexSupport
import com.teco.ventago.features.inventory.domain.InventorySaleErrorMapper
import com.teco.ventago.features.product.domain.model.Item
import com.teco.ventago.features.product.domain.model.ProductType
import com.teco.ventago.features.product.domain.model.AdditionalInfoKey
import com.teco.ventago.features.product.domain.model.Products
import com.teco.ventago.features.quotes.domain.QuoteRequestBuilder
import com.teco.ventago.features.quotes.domain.QuotesService
import com.teco.ventago.features.quotes.domain.models.Quote
import com.teco.ventago.features.quotes.domain.models.QuoteSettings
import com.teco.ventago.features.quotes.domain.models.QuoteLine
import com.teco.ventago.features.quotes.domain.models.requests.GetQuoteRequest
import com.teco.ventago.features.quotes.domain.models.requests.SendQuoteEmailRequest
import com.teco.ventago.json
import com.teco.ventago.navigation.PosNoteRoute
import com.teco.ventago.features.orders.domain.models.OrderLineDto
import kotlinx.serialization.json.Json as KotlinJson
import com.teco.ventago.utils.dbFormat
import com.teco.ventago.utils.emailRegex
import com.teco.ventago.utils.isValidPanamaCedula
import com.teco.ventago.utils.normalizePanamaCedula
import com.teco.ventago.utils.normalizeQuantity
import com.teco.ventago.utils.randomUUID
import com.teco.ventago.utils.toDecimalString
import com.teco.ventago.utils.toLongCents
import com.teco.ventago.utils.toQuantityRequestString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.String
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.roundToLong
import kotlin.math.roundToInt
import kotlin.time.Clock.System.now
import kotlin.time.ExperimentalTime

internal const val INVALID_FINAL_CUSTOMER_CEDULA_MESSAGE =
    "Cedula invalida. Revise el formato (ej: 1-1234-12345, 8-88-8456, PE-123-12345, E-1234-12345, N-12345-1234, 1AV1234-12345, 1PI-1234-1234)."
internal const val INVALID_FINAL_CUSTOMER_EMAIL_MESSAGE = "Correo inválido"

private fun CreateOrderLinkDto.resolvedUrl(): String = url.ifBlank { link.orEmpty() }

class PosViewModel(
    private val authService: IAuthService,
    private val businessService: BusinessService,
    private val branchService: BranchService,
    private val customerService: CustomerService,
    private val productService: ProductService,
    private val posService: PosService,
    private val orderService: OrderService,
    private val paymentService: PaymentService,
    private val financialProfileService: FinancialProfileService,
    private val quotesService: QuotesService,
    private val invoicingSettingsService: InvoicingSettingsService,
    private val pdfSharer: PdfSharer,
    private val localStorage: LocalStorage,
    private val betaService: BetaService,
    private val printerService: PrinterService,
    private val snackbarService: SnackbarService,
    private val analyticsService: AnalyticsService,
    private val appScope: CoroutineScope,
    private val posProvisioningService: PosDeviceProvisioningService,
    private val inventoryAvailabilityStore: InventoryAvailabilityStore,
) : BaseViewModel<PosState, PosStateUiEvent>(PosState()) {
    private companion object {
        const val DEFAULT_QUOTE_BRANCH_CODE = "0000"
        const val PRODUCT_VIEW_MODE_KEY_PREFIX = "pos.product_view_mode"
        const val BRANCH_BILLING_POINT_KEY_PREFIX = "pos.last_branch_billing_point"
        const val PAYMENT_LINK_BADGE_SEEN_KEY_PREFIX = "pos.payment_link_tab_seen"
        const val ORDER_CREATION_CHECKPOINT_KEY_PREFIX = "pos.order_creation_checkpoint"
        const val YAPPY_ONSITE_DEVICES_CACHE_KEY_PREFIX = "pos.yappy_onsite_devices"
        const val PAYMENT_CONFIG_REFRESH_THROTTLE_SECONDS = 60L
        const val PAYMENT_LINK_POLL_MS = 4_000L
        const val GOVERNMENT_FE_CUSTOMER_TYPE = "03"
    }

    var business: Business? = null
    var items: List<Item> = emptyList()

    private var order: Order? = null
    private var pendingQuoteBranchCode: String? = null
    private var pendingOrderCreationCheckpoint: OrderCreationCheckpoint? = null
    private var orderCheckpointPromptChecked = false
    private var suppressOrderCheckpointWrites = false
    private var orderCheckpointDisabledForCurrentFlow = false
    private var customerAddressesJob: Job? = null
    private var paymentConfigRefreshJob: Job? = null
    private var yappyOnsiteDevicesJob: Job? = null
    private var paymentLinkPollingJob: Job? = null
    private var yappyOnsitePollingJob: Job? = null
    private var inventoryRefreshJob: Job? = null
    private var lastPaymentConfigRefreshAtEpochSeconds: Long = 0L
    private data class PersistedBranchBillingPoint(
        val branchCode: String,
        val billingPoint: String
    )

    private data class PosAuthzState(
        val canCreateInvoice: Boolean,
        val canCreateDraft: Boolean,
        val canCreatePaymentLink: Boolean,
        val canUseManualPaymentMethods: Boolean,
        val canConfigurePayments: Boolean,
        val canConfigureYappyOnsite: Boolean,
        val canCreateYappyOnsiteQr: Boolean,
        val canCreateQuote: Boolean,
        val canUpdateQuote: Boolean,
        val canUseCustomProduct: Boolean,
        val canEditProduct: Boolean,
    )

    init {
        inventoryAvailabilityStore.snapshot
            .onEach { snap -> applyInventorySnapshot(snap) }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            authService.getUser()
                .combine(betaService.features()) { user, betaResponse ->
                    val betaSnapshot = betaResponse?.features.orEmpty()
                        .mapNotNull(BetaFeature::fromKey)
                        .toSet()
                    val yappyOnsiteSetupScopes = setOf(
                        ScopeKey.INVOICE_YAPPY_ONSITE,
                        ScopeKey.PAYMENTS_CONFIGURE,
                        ScopeKey.PAYMENTS_VIEW
                    )
                    val canConfigurePayments = AuthzEvaluator.canAction(ActionKey.PAYMENTS_CONFIGURE, user, betaSnapshot)
                    PosAuthzState(
                        canCreateInvoice = AuthzEvaluator.canAction(ActionKey.ORDERS_CREATE, user, betaSnapshot),
                        canCreateDraft = AuthzEvaluator.canAction(ActionKey.ORDERS_CREATE_DRAFT, user, betaSnapshot),
                        canCreatePaymentLink = AuthzEvaluator.canAction(ActionKey.ORDERS_PAYMENT_LINK, user, betaSnapshot),
                        canUseManualPaymentMethods = AuthzEvaluator.canAction(ActionKey.ORDERS_MANUAL_PAYMENT, user, betaSnapshot),
                        canConfigurePayments = canConfigurePayments,
                        canConfigureYappyOnsite = canConfigurePayments && (
                            user?.isOwnerMain == true || user?.scopes?.containsAll(yappyOnsiteSetupScopes) == true
                        ),
                        canCreateYappyOnsiteQr = AuthzEvaluator.canAction(ActionKey.ORDERS_YAPPY_ONSITE, user, betaSnapshot),
                        canCreateQuote = AuthzEvaluator.canAction(ActionKey.QUOTES_CREATE, user, betaSnapshot),
                        canUpdateQuote = AuthzEvaluator.canAction(ActionKey.QUOTES_UPDATE, user, betaSnapshot),
                        canUseCustomProduct = AuthzEvaluator.canAction(ActionKey.ORDERS_CUSTOM_PRODUCT, user, betaSnapshot),
                        canEditProduct = AuthzEvaluator.canAction(ActionKey.ORDERS_EDIT_PRODUCT, user, betaSnapshot),
                    )
                }
                .onEach { authz ->
                    updateState {
                        copy(
                            canCreateInvoice = authz.canCreateInvoice,
                            canCreateDraft = authz.canCreateDraft,
                            canCreatePaymentLink = authz.canCreatePaymentLink,
                            canUseManualPaymentMethods = authz.canUseManualPaymentMethods,
                            canConfigurePayments = authz.canConfigurePayments,
                            canConfigureYappyOnsite = authz.canConfigureYappyOnsite,
                            canCreateYappyOnsiteQr = authz.canCreateYappyOnsiteQr,
                            canCreateQuote = authz.canCreateQuote,
                            canUpdateQuote = authz.canUpdateQuote,
                            canUseCustomProduct = authz.canUseCustomProduct,
                            canEditProduct = authz.canEditProduct
                        )
                    }
                }
                .launchIn(this)
            posProvisioningService.observe().onEach { provisioning ->
                val authz = currentPosAuthzState()
                updateState {
                    copy(
                        posProvisioningActive = provisioning.required && provisioning.isProvisioned,
                        canCreateInvoice = authz.canCreateInvoice,
                        canCreateDraft = authz.canCreateDraft,
                        canCreatePaymentLink = authz.canCreatePaymentLink,
                        canUseManualPaymentMethods = authz.canUseManualPaymentMethods,
                        canConfigurePayments = authz.canConfigurePayments,
                        canConfigureYappyOnsite = authz.canConfigureYappyOnsite,
                        canCreateYappyOnsiteQr = authz.canCreateYappyOnsiteQr,
                        canCreateQuote = authz.canCreateQuote,
                        canUpdateQuote = authz.canUpdateQuote,
                        canUseCustomProduct = authz.canUseCustomProduct,
                        canEditProduct = authz.canEditProduct
                    )
                }
                applyProvisionedBranchBillingPointSelectionIfPossible()
            }.launchIn(this)
            betaService.getFeatures()

            productService.observe().onEach { products ->
                products?.let {
                    refreshProductsCatalog(it)
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
                val persistedSelection = if (
                    currentState.flowMode == FlowMode.SALE &&
                    currentState.branches.isEmpty()
                ) {
                    resolveProvisionedBranchBillingPointSelection(branches)
                        ?: resolvePersistedBranchBillingPointSelection(branches)
                } else {
                    null
                }
                val resolvedIndex = when {
                    pendingIndex >= 0 -> pendingIndex
                    persistedSelection != null -> persistedSelection.first
                    currentState.flowMode == FlowMode.QUOTE && currentState.branches.isEmpty() ->
                        if (defaultIndex >= 0) defaultIndex else safeCurrentIndex
                    else -> safeCurrentIndex
                }
                val resetBillingPoint = currentState.branches.isEmpty() || resolvedIndex != currentState.selectedBranchIndex
                updateBranchSelection(
                    index = resolvedIndex,
                    branches = branches,
                    resetBillingPoint = resetBillingPoint,
                    forcedBillingPointIndex = persistedSelection?.second
                )
                if (pendingIndex >= 0 || pendingCode != null) {
                    pendingQuoteBranchCode = null
                }
            }.launchIn(this)

            financialProfileService.observe().onEach { profile ->
                profile?.let {
                    val onsite = it.paymentSummary.paymentMethods.yappy.onsite
                    val onsiteConfigured = onsite.configured && onsite.enabled
                    updateState {
                        copy(
                            paymentsConfigured = financialProfileService.paymentsConfigured(),
                            paymentsOnboardingCompleted = financialProfileService.paymentsOnboardingCompleted(),
                            paymentProfileResolved = true,
                            paymentLinkConfigured = financialProfileService.paymentLinkMethodsConfigured(),
                            yappyOnsiteConfigured = onsiteConfigured,
                            yappyOnsiteDevices = if (onsiteConfigured) yappyOnsiteDevices else emptyList(),
                            yappyOnsiteDevicesResolved = if (onsiteConfigured) {
                                onsiteConfigured && yappyOnsiteDevicesResolved
                            } else {
                                true
                            },
                            invoicingEnabled = it.invoicingActive,
                            autoInvoiceOnPaymentSuccess = it.paymentSummary.autoInvoiceOnPaymentSuccess
                        )
                    }
                    if (onsiteConfigured) {
                        loadYappyOnsiteDevices(forceRefresh = true)
                    }
                    if (it.invoicingActive) {
                        refreshBottomNoteSettingsInBackground()
                    }
                }
            }.launchIn(this)

            businessService.getBusiness().onEach { businessData ->
                businessData?.let {
                    business = it
                    val persistedViewMode = readPersistedProductViewMode(it.businessId)
                    updateState {
                        copy(
                            currency = it.currency.currencyCode,
                            currencySymbol = it.currency.symbol,
                            productViewMode = persistedViewMode
                        )
                    }
                    refreshPaymentLinkBadge(it.businessId)
                    invoicingSettingsService.loadCachedBottomNoteSettingsForCurrentBusiness()
                    refreshBottomNoteSettingsInBackground()
                    applyProvisionedBranchBillingPointSelectionIfPossible()
                    applyPersistedBranchBillingPointSelectionIfPossible()
                    fetchCustomerAddresses()
                    warmYappyOnsiteAvailabilityForNewOrder()
                }
            }.launchIn(this)

            invoicingSettingsService.bottomNoteSettings().onEach { bottomNoteState ->
                val settings = bottomNoteState.settings
                updateState {
                    val shouldShow = settings != null &&
                        !bottomNoteState.refreshFailed &&
                        settings.title.isNotBlank() &&
                        settings.body.isNotBlank()
                    copy(
                        bottomNoteSettings = settings,
                        bottomNoteRefreshFailed = bottomNoteState.refreshFailed,
                        includeBottomNote = if (shouldShow) {
                            includeBottomNote ?: settings.includeOnInvoice
                        } else {
                            null
                        },
                    )
                }
            }.launchIn(this)
        }
    }

    private fun betaSnapshot(): Set<BetaFeature> {
        return betaService.features().value?.features.orEmpty()
            .mapNotNull(BetaFeature::fromKey)
            .toSet()
    }

    private fun canAction(actionKey: ActionKey): Boolean {
        return AuthzEvaluator.canAction(actionKey, authService.getUserSync(), betaSnapshot())
    }

    private fun currentPosAuthzState(): PosAuthzState {
        val user = authService.getUserSync()
        val betaSnapshot = betaSnapshot()
        val canConfigurePayments = AuthzEvaluator.canAction(ActionKey.PAYMENTS_CONFIGURE, user, betaSnapshot)
        val yappyOnsiteSetupScopes = setOf(
            ScopeKey.INVOICE_YAPPY_ONSITE,
            ScopeKey.PAYMENTS_CONFIGURE,
            ScopeKey.PAYMENTS_VIEW
        )
        return PosAuthzState(
            canCreateInvoice = AuthzEvaluator.canAction(ActionKey.ORDERS_CREATE, user, betaSnapshot),
            canCreateDraft = AuthzEvaluator.canAction(ActionKey.ORDERS_CREATE_DRAFT, user, betaSnapshot),
            canCreatePaymentLink = AuthzEvaluator.canAction(ActionKey.ORDERS_PAYMENT_LINK, user, betaSnapshot),
            canUseManualPaymentMethods = AuthzEvaluator.canAction(ActionKey.ORDERS_MANUAL_PAYMENT, user, betaSnapshot),
            canConfigurePayments = canConfigurePayments,
            canConfigureYappyOnsite = canConfigurePayments && (
                user?.isOwnerMain == true || user?.scopes?.containsAll(yappyOnsiteSetupScopes) == true
            ),
            canCreateYappyOnsiteQr = AuthzEvaluator.canAction(ActionKey.ORDERS_YAPPY_ONSITE, user, betaSnapshot),
            canCreateQuote = AuthzEvaluator.canAction(ActionKey.QUOTES_CREATE, user, betaSnapshot),
            canUpdateQuote = AuthzEvaluator.canAction(ActionKey.QUOTES_UPDATE, user, betaSnapshot),
            canUseCustomProduct = AuthzEvaluator.canAction(ActionKey.ORDERS_CUSTOM_PRODUCT, user, betaSnapshot),
            canEditProduct = AuthzEvaluator.canAction(ActionKey.ORDERS_EDIT_PRODUCT, user, betaSnapshot),
        )
    }

    private fun canEditProduct(): Boolean {
        return canAction(ActionKey.ORDERS_EDIT_PRODUCT)
    }


    @OptIn(ExperimentalTime::class)
    fun startNoteFromInvoice(
        args: PosNoteRoute,
    ) {
        disableOrderCreationCheckpointForCurrentFlow(clearExisting = true)
        updateState {
            copy(
                selectedDocTypeIndex = when (args.op) {
                    "04", "05" -> 0 // Preset-only note types are hidden from the normal dropdown.
                    else -> 0
                },
                selectedDocType = args.op,
                enabledSelectionDocType = false,
                referencedNoteCUFE = args.cufe,
                referencedCreatedAt = args.createdAt,
                originalInvoiceNumber = "",
                originalInvoiceNumberError = null,
                originalInvoiceEmissionDateIso = "",
                originalInvoiceEmissionDateError = null,
                // Force manual payment for credit/debit notes (no payment links or drafts)
                paymentFlowMode = PaymentFlowMode.MANUAL_OR_INSTALLMENTS,
                maxCreditNoteAmountCents = args.maxCreditNoteAmountCents,
                sourceOrderNumber = args.sourceOrderNumber
            )
        }

        if (args.customerId == null) {
            updateState {
                copy(
                    finalCustomer = true,
                    selectedCustomerFeCustomerType = null,
                    customerAddresses = emptyList(),
                    selectedCustomerAddressId = null,
                    customerAddressesLoading = false
                )
            }
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
                    ),
                    selectedCustomerFeCustomerType = null,
                )
            }
            hydrateSelectedCustomerTaxSettings(
                customerId = args.customerId.toLong(),
                applyDefaultsOnlyWhenMissing = true,
            )
            fetchCustomerAddresses()
        }

        // Load cart from order lines if available
        args.orderLinesJson?.let { orderLinesJson ->
            try {
                val orderLines = KotlinJson.decodeFromString<List<OrderLineDto>>(orderLinesJson)
                loadCartFromOrderLines(orderLines)
            } catch (e: Exception) {
                println("Error loading order lines into cart: ${e.message}")
            }
        }
    }

    private fun loadCartFromOrderLines(orderLines: List<OrderLineDto>) {
        // Clear existing cart first
        updateState { copy(cart = emptyList(), personalizedItems = mapOf()) }

        var personalizedIdCounter = -1

        orderLines.forEach { orderLine ->
            // Try to find existing product in catalog
            val existingItem = items.firstOrNull { it.itemId == orderLine.itemId }

            if (existingItem != null) {
                // Product exists in catalog - add it with the order line's data
                val unitPrice = orderLine.overrideUnitPrice?.toDoubleOrNull()?.toLongCents()
                    ?: orderLine.baseUnitPrice.toDoubleOrNull()?.toLongCents()
                    ?: existingItem.price.toLongCents()

                // Create tax from order line
                val tax = createTaxFromOrderLine(orderLine)

                // Create discount from order line
                val discount = createDiscountFromOrderLine(orderLine)

                val lineId = "${existingItem.itemId}-${randomUUID()}"
                val newLine = CartLine(
                    lineId = lineId,
                    itemId = existingItem.itemId,
                    name = orderLine.itemName,
                    baseUnitPrice = orderLine.baseUnitPrice.toDoubleOrNull()?.toLongCents()
                        ?: existingItem.price.toLongCents(),
                    overrideUnitPrice = if (orderLine.overrideUnitPrice != null) {
                        orderLine.overrideUnitPrice.toDoubleOrNull()?.toLongCents()
                    } else null,
                    quantity = normalizeQuantity(orderLine.quantity, minValue = 0.0001),
                    tax = tax,
                    discount = discount,
                    costCents = existingItem.cost?.toLongCents(),
                )

                updateState { copy(cart = cart + newLine) }
            } else {
                // Product doesn't exist - create personalized item
                val personalizedItemId = personalizedIdCounter--
                val lineId = "${personalizedItemId}-${randomUUID()}"

                // Parse tax percent from tax rate (e.g., "0.07" -> 7)
                val taxPercent = orderLine.taxRate.toDoubleOrNull()?.let { rate ->
                    (rate * 100).toInt()
                } ?: 0

                // Create personalized Item
                val personalizedItem = Item(
                    itemId = personalizedItemId,
                    barcode = null,
                    sku = null,
                    name = orderLine.itemName,
                    description = orderLine.itemName,
                    img = "",
                    price = orderLine.baseUnitPrice.toDoubleOrNull() ?: 0.0,
                    cost = null,
                    active = true,
                    order = 0,
                    taxPercent = taxPercent,
                    productType = ProductType.GOOD,
                    unitMeasureCode = "und",
                    iscRate = null,
                    otiTaxes = null,
                    isPharma = false,
                    additionalInfo = null
                )

                // Create tax from order line
                val tax = createTaxFromOrderLine(orderLine)

                // Create discount from order line
                val discount = createDiscountFromOrderLine(orderLine)

                val newLine = CartLine(
                    lineId = lineId,
                    itemId = personalizedItemId,
                    name = orderLine.itemName,
                    baseUnitPrice = orderLine.baseUnitPrice.toDoubleOrNull()?.toLongCents() ?: 0L,
                    overrideUnitPrice = if (orderLine.overrideUnitPrice != null) {
                        orderLine.overrideUnitPrice.toDoubleOrNull()?.toLongCents()
                    } else null,
                    quantity = normalizeQuantity(orderLine.quantity, minValue = 0.0001),
                    tax = tax,
                    discount = discount,
                )

                updateState {
                    copy(
                        cart = cart + newLine,
                        personalizedItems = personalizedItems + (lineId to personalizedItem)
                    )
                }
            }
        }
    }

    private fun createTaxFromOrderLine(orderLine: OrderLineDto): Tax? {
        val taxRate = orderLine.taxRate.toDoubleOrNull() ?: return null
        if (taxRate <= 0.0) return null

        // Convert rate to percentage (e.g., 0.07 -> 7.0)
        val taxPercent = taxRate * 100.0
        val rateBps = (taxPercent * 100).roundToInt()

        return Tax(
            id = rateBps,
            name = orderLine.taxName,
            rateBps = rateBps
        )
    }

    private fun createDiscountFromOrderLine(orderLine: OrderLineDto): Discount? {
        val discountValue = orderLine.discountValue.toDoubleOrNull() ?: return null
        if (discountValue <= 0.0) return null

        return when (orderLine.discountMode) {
            1 -> {
                // Percentage discount
                val percent = if (discountValue <= 1.0) discountValue * 100.0 else discountValue
                Discount.Percent((percent * 100).roundToInt().coerceAtLeast(0))
            }
            2 -> {
                // Fixed amount discount
                Discount.Amount(discountValue.toLongCents())
            }
            else -> null
        }
    }

    private fun refreshProductsCatalog(products: Products) {
        val activeItems = productService.getAllActiveItems(products)
        val categoriesWithActiveItems = products.categories
            .asSequence()
            .filter { it.active }
            .map { category ->
                category to category.items.filter { item -> item.active }
            }
            .filter { (_, categoryItems) -> categoryItems.isNotEmpty() }
            .toList()

        val categoryByItemId = mutableMapOf<Int, Int>()
        categoriesWithActiveItems.forEach { (category, categoryItems) ->
            categoryItems.forEach { item ->
                if (!categoryByItemId.containsKey(item.itemId)) {
                    categoryByItemId[item.itemId] = category.id
                }
            }
        }

        val availableCategories = categoriesWithActiveItems.map { (category, _) ->
            PosProductCategoryFilter(id = category.id, label = category.name)
        }

        items = activeItems
        updateState {
            val selectedCategoryId = selectedProductCategoryId
                ?.takeIf { categoryId -> availableCategories.any { it.id == categoryId } }
            copy(
                items = activeItems,
                itemCategoryById = categoryByItemId,
                availableProductCategories = availableCategories,
                selectedProductCategoryId = selectedCategoryId
            )
        }
        applyProductFilters()
    }

    private fun applyProductFilters() {
        val state = uiState.value
        val query = state.query.trim()
        val selectedCategoryId = state.selectedProductCategoryId

        val filteredItems = state.items.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.name.contains(query, ignoreCase = true) ||
                item.description.contains(query, ignoreCase = true) ||
                item.barcode?.contains(query, ignoreCase = true) == true ||
                item.sku?.contains(query, ignoreCase = true) == true
            val matchesCategory = selectedCategoryId == null ||
                state.itemCategoryById[item.itemId] == selectedCategoryId
            matchesQuery && matchesCategory
        }

        updateState { copy(visibleItems = filteredItems) }
        refreshInventoryAvailability()
    }

    private fun refreshInventoryAvailability() {
        val state = uiState.value
        val businessId = business?.businessId ?: return
        val branchCode = state.branches.getOrNull(state.selectedBranchIndex)?.branchCode.orEmpty()
        val billingPoint = state.billingPoints.getOrNull(state.selectedBillingPointIndex)?.billingPoint.orEmpty()
        val itemIds = (state.visibleItems.map { it.itemId } + state.cart.map { it.itemId }).distinct()
        inventoryRefreshJob?.cancel()
        inventoryRefreshJob = viewModelScope.launch(Dispatchers.IO) {
            inventoryAvailabilityStore.refresh(businessId, branchCode, billingPoint, itemIds)
        }
    }

    private fun applyInventorySnapshot(snap: com.teco.ventago.features.inventory.domain.InventoryAvailabilitySnapshot) {
        if (!snap.enabled && snap.byItemId.isEmpty() && snap.fetchedAtEpochMs == 0L) return
        updateState {
            copy(
                inventoryEnabled = snap.enabled,
                inventoryLocationId = snap.locationId,
                inventoryFreshnessLabel = snap.freshnessLabel(),
                inventoryAvailableByItemId = snap.byItemId.keys.mapNotNull { itemId ->
                    snap.catalogStockLabel(itemId)?.let { itemId to it }
                }.toMap(),
            )
        }
    }

    fun onSearchChange(query: String) {
        updateState { copy(query = query) }
        applyProductFilters()
    }

    fun onProductCategorySelected(categoryId: Int?) {
        updateState { copy(selectedProductCategoryId = categoryId) }
        applyProductFilters()
    }

    fun setProductViewMode(mode: ProductViewMode) {
        updateState { copy(productViewMode = mode) }
        persistProductViewMode(mode)
    }

    private fun readPersistedProductViewMode(businessId: Int): ProductViewMode {
        val saved = localStorage.string(productViewModeKey(businessId))
        return runCatching { ProductViewMode.valueOf(saved.orEmpty()) }
            .getOrDefault(ProductViewMode.LIST)
    }

    private fun persistProductViewMode(mode: ProductViewMode) {
        val businessId = business?.businessId ?: return
        localStorage.set(productViewModeKey(businessId), mode.name)
    }

    private fun productViewModeKey(businessId: Int): String {
        return "$PRODUCT_VIEW_MODE_KEY_PREFIX.$businessId"
    }

    fun selectCustomer(customer: CustomerListItem?) {
        updateState {
            copy(
                customer = customer,
                selectedCustomerFeCustomerType = null,
                customerAddresses = emptyList(),
                selectedCustomerAddressId = null,
                customerAddressesLoading = false
            )
        }
        when {
            customer != null -> {
                applySelectedCustomerTaxDefaults(customer)
                hydrateSelectedCustomerTaxSettings(
                    customerId = customer.id,
                    applyDefaultsOnlyWhenMissing = false,
                )
            }
            uiState.value.finalCustomer == false -> clearSelectedCustomerTaxDefaults()
        }
        fetchCustomerAddresses()
        saveOrderCreationCheckpoint(OrderCreationStep.CUSTOMER)
    }

    private fun fetchCustomerAddresses() {
        val state = uiState.value
        val selectedCustomer = state.customer
        val shouldLoad = state.finalCustomer == false &&
            selectedCustomer != null &&
            selectedCustomer.id > 0

        if (!shouldLoad) {
            customerAddressesJob?.cancel()
            updateState {
                copy(
                    customerAddresses = emptyList(),
                    selectedCustomerAddressId = null,
                    customerAddressesLoading = false
                )
            }
            return
        }

        val businessId = business?.businessId ?: return
        val customerId = selectedCustomer.id.toInt()

        customerAddressesJob?.cancel()
        customerAddressesJob = viewModelScope.launch {
            updateState {
                copy(
                    customerAddressesLoading = true,
                    customerAddresses = emptyList(),
                    selectedCustomerAddressId = null
                )
            }

            try {
                val addresses = withContext(Dispatchers.IO) {
                    customerService.listCustomerAddresses(
                        businessId = businessId,
                        invoiceCustomerId = customerId
                    )
                }

                val selectedAddressId = addresses.firstOrNull { it.isDefault }?.id
                    ?: addresses.firstOrNull()?.id

                updateState {
                    copy(
                        customerAddresses = addresses,
                        selectedCustomerAddressId = selectedAddressId,
                        customerAddressesLoading = false
                    )
                }
            } catch (e: Exception) {
                println("Error fetching customer addresses: ${e.message}")
                updateState {
                    copy(
                        customerAddresses = emptyList(),
                        selectedCustomerAddressId = null,
                        customerAddressesLoading = false
                    )
                }
            }
        }
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
        return state.selectedCustomerFeCustomerType == GOVERNMENT_FE_CUSTOMER_TYPE
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
    ) {
        if (item.itemId < 0 && !canAction(ActionKey.ORDERS_CUSTOM_PRODUCT)) {
            showError()
            return
        }
        if (deltaQty > 0 && inventoryAvailabilityStore.shouldBlockUnstocked(item.itemId, item.itemId < 0)) {
            viewModelScope.launch {
                snackbarService.show(InventorySaleErrorMapper.ZERO_STOCK_CART_MESSAGE)
            }
            return
        }
        updateState {
        val priceCents = customUnitPrice
        val baseCents = item.price.toLongCents()
        val shouldShowProductAddedSnackbar = deltaQty > 0

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
            val newQty = cur.quantity + deltaQty.toDouble()
            val newCart = cart.toMutableList()
            if (newQty <= 0.0) {
                newCart.removeAt(idx)
                copy(cart = newCart)
            } else {
                newCart[idx] = cur.copy(quantity = normalizeQuantity(newQty, minValue = 0.0001))
                copy(
                    cart = newCart,
                    productAddedSnackbarToken = if (shouldShowProductAddedSnackbar) {
                        productAddedSnackbarToken + 1
                    } else {
                        productAddedSnackbarToken
                    }
                )
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
                quantity = normalizeQuantity(deltaQty.toDouble().coerceAtLeast(1.0), minValue = 0.0001),
                tax = tax,
                discount = null,
                costCents = item.cost?.toLongCents(),
                locationId = uiState.value.inventoryLocationId,
            )
            // Store personalized items (itemId < 0) keyed by lineId for later use in order creation
            val newPersonalizedItems = if (isPersonalized) {
                personalizedItems + (lineId to item)
            } else {
                personalizedItems
            }
            copy(
                cart = cart + newLine,
                personalizedItems = newPersonalizedItems,
                productAddedSnackbarToken = if (shouldShowProductAddedSnackbar) {
                    productAddedSnackbarToken + 1
                } else {
                    productAddedSnackbarToken
                }
            )
        }
    }
        saveOrderCreationCheckpoint(OrderCreationStep.PRODUCTS)
    }

    fun updateCartLine(
        lineId: String,
        productName: String,
        unitPriceCents: Long,         // manual unit price before discount (cents)
        quantity: Double,
        discountMode: DiscountMode,
        discountValue: Long,
        itemShippingCents: Long?,
        itemInsuranceCents: Long?,
        pharmaBatchNumber: String?,
        pharmaBatchQty: Int?
    ) {
        if (!canEditProduct()) {
            showError()
            return
        }
        setLineProductName(lineId, productName)
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
        saveOrderCreationCheckpoint(OrderCreationStep.CART)
    }

    private fun setLineProductName(lineId: String, productName: String) = updateState {
        val line = cart.firstOrNull { it.lineId == lineId } ?: return@updateState this
        val cleanName = productName.trim().ifBlank { line.name }
        val renamed = cleanName != line.name
        val shouldPersonalize = renamed || line.itemId < 0

        if (!shouldPersonalize) {
            return@updateState this
        }

        val sourceProduct = resolveProductForLine(this, line)
        val personalizedItemId = if (line.itemId < 0) line.itemId else -1
        val personalizedItem = sourceProduct?.copy(
            itemId = personalizedItemId,
            name = cleanName,
            description = sourceProduct.description.ifBlank { cleanName },
            price = line.unitPrice() / 100.0,
            cost = null
        ) ?: Item(
            itemId = personalizedItemId,
            barcode = null,
            sku = null,
            name = cleanName,
            description = cleanName,
            img = "",
            price = line.unitPrice() / 100.0,
            cost = null,
            active = true,
            order = 0,
            taxPercent = line.tax?.rateBps?.let { it / 100 },
            productType = ProductType.GOOD,
            unitMeasureCode = "und",
            iscRate = null,
            otiTaxes = null,
            isPharma = false,
            additionalInfo = null
        )

        copy(
            cart = cart.map {
                if (it.lineId == lineId) {
                    it.copy(itemId = personalizedItemId, name = cleanName, costCents = null)
                } else {
                    it
                }
            },
            personalizedItems = personalizedItems + (lineId to personalizedItem)
        )
    }

    fun setLineQty(lineId: String, qty: Double) = updateState {
        copy(
            cart = cart.map {
                if (it.lineId == lineId) it.copy(quantity = normalizeQuantity(qty, minValue = 0.0001)) else it
            }
        )
    }

    fun setLineOverridePrice(lineId: String, price: Money?) = updateState {
        if (!canEditProduct()) return@updateState this
        copy(cart = cart.map { if (it.lineId == lineId) it.copy(overrideUnitPrice = price) else it })
    }

    fun setLineDiscount(lineId: String, discount: Discount?) = updateState {
        if (!canEditProduct()) return@updateState this
        copy(cart = cart.map { if (it.lineId == lineId) it.copy(discount = discount) else it })
    }

    fun toggleTaxExempt(enabled: Boolean) {
        updateState { copy(taxExempt = enabled) }
        saveOrderCreationCheckpoint(OrderCreationStep.CART)
    }

    fun removeLine(lineId: String) {
        updateState { 
            copy(
                cart = cart.filterNot { it.lineId == lineId },
                personalizedItems = personalizedItems - lineId
            )
        }
        saveOrderCreationCheckpoint(OrderCreationStep.CART)
    }

    fun clearCart() {
        updateState { copy(cart = emptyList(), personalizedItems = mapOf()) }
        saveOrderCreationCheckpoint(OrderCreationStep.CART)
    }


    fun getChange(): Long {
        val charged = uiState.value.charged.values.sum()
        return if (charged < getTotalAmount()) {
            0
        } else {
            charged - getTotalAmount()
        }
    }


    fun resetForNewSale() {
        val currentState = uiState.value
        val (nextBranchIndex, nextBillingPointIndex) = resolveSelectionForNewSaleReset(currentState)
        val nextBillingPoints = currentState.branches
            .getOrNull(nextBranchIndex)
            ?.fiscalBillingPoints
            .orEmpty()
        customerAddressesJob?.cancel()
        paymentLinkPollingJob?.cancel()
        paymentLinkPollingJob = null
        yappyOnsitePollingJob?.cancel()
        yappyOnsitePollingJob = null
        pendingOrderCreationCheckpoint = null
        orderCheckpointPromptChecked = false
        orderCheckpointDisabledForCurrentFlow = false
        updateState {
            copy(
                cart = emptyList(),
                personalizedItems = mapOf(),
                taxExempt = false,
                query = "",
                visibleItems = items,
                selectedProductCategoryId = null,
                tipAmount = 0L,
                tipIsPercentage = true,
                charged = mapOf(),
                freeTrialAvailable = false,
                customer = null,
                selectedCustomerFeCustomerType = null,
                customerQuery = "",
                selectedBranchIndex = nextBranchIndex,
                billingPoints = nextBillingPoints,
                selectedBillingPointIndex = nextBillingPointIndex,
                selectedDocTypeIndex = 0,
                selectedDocType = "01",
                enabledSelectionDocType = true,
                selectedOperationNatureIndex = 0,
                selectedOperationNature = "01",
                enabledOperationNature = true,
                invoiceIssueDateIso = currentPanamaDateIsoString(),
                finalCustomer = null,
                finalName = null,
                finalEmail = null,
                finalPhone = null,
                finalIdTypeIndex = 0,
                finalIdType = "cedula",
                finalIdNumber = null,
                finalIdNumberError = null,
                finalCustomerCountryCode = null,

                globalDiscountMode = GlobalDiscountMode.NONE,
                globalDiscountPercent = 0,
                globalDiscountFixedCents = 0L,
                globalShippingCents = 0L,
                globalInsuranceCents = 0L,
                globalOtherChargesCents = 0L,
                referencedNoteCUFE = "",
                referencedCreatedAt = "",
                maxCreditNoteAmountCents = null,
                sourceOrderNumber = null,
                originalInvoiceNumber = "",
                originalInvoiceNumberError = null,
                originalInvoiceEmissionDateIso = "",
                originalInvoiceEmissionDateError = null,
                includeBottomNote = if (
                    !currentState.bottomNoteRefreshFailed &&
                    currentState.bottomNoteSettings != null &&
                    currentState.bottomNoteSettings.title.isNotBlank() &&
                    currentState.bottomNoteSettings.body.isNotBlank()
                ) {
                    currentState.bottomNoteSettings.includeOnInvoice
                } else {
                    null
                },

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
                customerAddresses = emptyList(),
                selectedCustomerAddressId = null,
                customerAddressesLoading = false,

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
                createdOrderId = null,
                paymentLinkPolling = false,
                paymentLinkPaymentDetected = false,
                paymentLinkInvoicePrintAttemptedOrderId = null,
                paymentLinkManualPanelVisible = false,
                paymentLinkManualErrorMessage = null,
                pendingPaymentChangeSourceMethod = null,
                pendingPaymentChangeSourceReleased = false,
                pendingPaymentChangeOpen = false,
                pendingPaymentChangeCompleted = false,
                pendingPaymentChangeCancelDialogVisible = false,
                pendingPaymentChangeCancelErrorMessage = null,
                pendingPaymentChangeExitAction = null,
                onsitePayment = null,
                yappyOnsiteTransaction = null,
                yappyOnsitePolling = false,
                yappyOnsitePollingSuppressed = false,
                yappyOnsiteInvoiceProcessingTimedOut = false,
                yappyOnsitePrintAttemptedTransactionId = null,
                yappyOnsiteExitCancelDialogVisible = false,
                successReprintInFlight = false,
                orderNumber = "",
                postCreateInvoiceWarning = com.teco.ventago.features.invoicing.domain.PostCreateInvoiceWarningState(),
                orderCreationFailed = false,
                showOrderRestoreDialog = false,

                )
        }
        persistCurrentBranchBillingPointSelection()
        warmYappyOnsiteAvailabilityForNewOrder()
    }

    private fun resolveSelectionForNewSaleReset(state: PosState): Pair<Int, Int> {
        val branches = state.branches
        if (branches.isEmpty()) return 0 to 0

        resolveProvisionedBranchBillingPointSelection(branches)?.let { return it }
        resolvePersistedBranchBillingPointSelection(branches)?.let { return it }

        val safeBranchIndex = state.selectedBranchIndex.coerceIn(0, branches.lastIndex)
        val billingPoints = branches[safeBranchIndex].fiscalBillingPoints
        val safeBillingPointIndex = if (billingPoints.isEmpty()) {
            0
        } else {
            state.selectedBillingPointIndex.coerceIn(0, billingPoints.lastIndex)
        }
        return safeBranchIndex to safeBillingPointIndex
    }

    fun checkOrderCreationCheckpointForRestore() {
        if (orderCheckpointPromptChecked) return
        orderCheckpointPromptChecked = true

        val businessId = business?.businessId ?: return
        val state = uiState.value
        if (!canUseOrderCreationCheckpoint(state)) return
        if (state.toOrderCreationCheckpointData().hasMeaningfulUserData()) {
            pendingOrderCreationCheckpoint = null
            updateState { copy(showOrderRestoreDialog = false) }
            return
        }

        val raw = localStorage.string(orderCreationCheckpointKey(businessId))
        if (raw.isNullOrBlank()) return

        val checkpoint = runCatching {
            json.decodeFromString<OrderCreationCheckpoint>(raw)
        }.getOrNull()

        if (checkpoint == null || checkpoint.businessId != businessId || checkpoint.version != 1) {
            clearOrderCreationCheckpoint()
            return
        }

        if (!checkpoint.currentStep.isRecoverableOrderCreationStep()) {
            clearOrderCreationCheckpoint()
            return
        }

        if (!checkpoint.data.hasMeaningfulUserData()) {
            clearOrderCreationCheckpoint()
            return
        }

        pendingOrderCreationCheckpoint = checkpoint
        updateState { copy(showOrderRestoreDialog = true) }
    }

    fun restorePendingOrderCreationCheckpoint(): OrderCreationStep? {
        val checkpoint = pendingOrderCreationCheckpoint ?: return null
        val data = checkpoint.data
        val current = uiState.value
        val (branchIndex, billingPoints, billingPointIndex) = resolveCheckpointBranchSelection(data, current)
        val (docTypeIndex, docType) = PosDocumentTypeOptions.sanitizedSelection(
            code = data.selectedDocType,
            index = data.selectedDocTypeIndex
        )

        suppressOrderCheckpointWrites = true
        updateState {
            copy(
                selectedBranchIndex = branchIndex,
                billingPoints = billingPoints,
                selectedBillingPointIndex = billingPointIndex,
                selectedDocTypeIndex = docTypeIndex,
                selectedDocType = docType,
                enabledSelectionDocType = true,
                selectedOperationNatureIndex = data.selectedOperationNatureIndex,
                selectedOperationNature = data.selectedOperationNature,
                enabledOperationNature = true,
                invoiceIssueDateIso = data.invoiceIssueDateIso,
                customer = data.customer,
                selectedCustomerFeCustomerType = null,
                finalCustomer = data.finalCustomer,
                finalName = data.finalName,
                finalEmail = data.finalEmail?.trim()?.takeIf { it.isNotEmpty() },
                finalEmailError = validateFinalCustomerEmail(data.finalEmail),
                finalPhone = data.finalPhone,
                finalIdTypeIndex = data.finalIdTypeIndex,
                finalIdType = data.finalIdType,
                finalIdNumber = normalizeFinalCustomerIdentificationNumber(data.finalIdType, data.finalIdNumber),
                finalIdNumberError = validateFinalCustomerIdentification(data.finalIdType, data.finalIdNumber),
                finalCustomerCountryCode = data.finalCustomerCountryCode,
                cart = data.cart.map { it.toCartLine() },
                personalizedItems = data.productSnapshots,
                taxExempt = data.taxExempt,
                globalDiscountMode = data.globalDiscountMode,
                globalDiscountPercent = data.globalDiscountPercent,
                globalDiscountFixedCents = data.globalDiscountFixedCents,
                globalShippingCents = data.globalShippingCents,
                globalInsuranceCents = data.globalInsuranceCents,
                globalOtherChargesCents = data.globalOtherChargesCents,
                referencedNoteCUFE = "",
                referencedCreatedAt = "",
                maxCreditNoteAmountCents = null,
                sourceOrderNumber = null,
                originalInvoiceNumber = "",
                originalInvoiceNumberError = null,
                originalInvoiceEmissionDateIso = "",
                originalInvoiceEmissionDateError = null,
                includeBottomNote = data.includeBottomNote,
                logisticsInfo = data.logisticsInfo,
                logisticsVehiclePlate = data.logisticsVehiclePlate,
                logisticsCarrierLegalName = data.logisticsCarrierLegalName,
                logisticsCarrierRuc = data.logisticsCarrierRuc,
                logisticsCarrierDv = data.logisticsCarrierDv,
                logisticsCarrierTaxpayerTypeIndex = data.logisticsCarrierTaxpayerTypeIndex,
                logisticsBoxesQty = data.logisticsBoxesQty,
                logisticsTotalWeightLb = data.logisticsTotalWeightLb,
                deliveryReceiverLegalName = data.deliveryReceiverLegalName,
                deliveryReceiverRuc = data.deliveryReceiverRuc,
                deliveryReceiverDv = data.deliveryReceiverDv,
                deliveryReceiverTaxpayerTypeIndex = data.deliveryReceiverTaxpayerTypeIndex,
                deliveryContactPhone = data.deliveryContactPhone,
                deliveryAltContactPhone = data.deliveryAltContactPhone,
                deliveryProvinceIndex = data.deliveryProvinceIndex,
                deliveryDistrictIndex = data.deliveryDistrictIndex,
                deliveryCorregIndex = data.deliveryCorregIndex,
                customerAddresses = emptyList(),
                selectedCustomerAddressId = data.selectedCustomerAddressId,
                customerAddressesLoading = false,
                retentionCodeIndex = data.retentionCodeIndex,
                retentionAmount = data.retentionAmount,
                exportIncoterm = data.exportIncoterm,
                exportCurrency = data.exportCurrency,
                exportPortOfLoading = data.exportPortOfLoading,
                flowMode = FlowMode.SALE,
                quoteId = null,
                paymentFlowMode = PaymentFlowMode.MANUAL_OR_INSTALLMENTS,
                tipAmount = 0L,
                tipIsPercentage = true,
                charged = emptyMap(),
                otherPaymentDescription = "",
                wantPaymentLink = false,
                installments = emptyList(),
                invoiceStatus = InvoiceStatus.NONE,
                pdfDocument = "",
                paymentLink = "",
                createdOrderId = null,
                paymentLinkPolling = false,
                paymentLinkPaymentDetected = false,
                paymentLinkInvoicePrintAttemptedOrderId = null,
                paymentLinkManualPanelVisible = false,
                paymentLinkManualErrorMessage = null,
                pendingPaymentChangeSourceMethod = null,
                pendingPaymentChangeSourceReleased = false,
                pendingPaymentChangeOpen = false,
                pendingPaymentChangeCompleted = false,
                pendingPaymentChangeCancelDialogVisible = false,
                pendingPaymentChangeCancelErrorMessage = null,
                pendingPaymentChangeExitAction = null,
                onsitePayment = null,
                yappyOnsiteTransaction = null,
                yappyOnsitePolling = false,
                yappyOnsitePollingSuppressed = false,
                yappyOnsiteInvoiceProcessingTimedOut = false,
                yappyOnsitePrintAttemptedTransactionId = null,
                yappyOnsiteExitCancelDialogVisible = false,
                successReprintInFlight = false,
                orderNumber = "",
                orderCreationFailed = false,
                showOrderRestoreDialog = false,
                postCreateInvoiceWarning = com.teco.ventago.features.invoicing.domain.PostCreateInvoiceWarningState(),
            )
        }
        suppressOrderCheckpointWrites = false

        pendingOrderCreationCheckpoint = null
        data.customer?.id?.let {
            hydrateSelectedCustomerTaxSettings(
                customerId = it,
                applyDefaultsOnlyWhenMissing = true,
            )
        }
        fetchCustomerAddresses()
        saveOrderCreationCheckpoint(checkpoint.currentStep)
        return checkpoint.currentStep
    }

    fun discardPendingOrderCreationCheckpoint() {
        pendingOrderCreationCheckpoint = null
        clearOrderCreationCheckpoint()
        resetForNewSale()
    }

    fun clearOrderCreationCheckpoint() {
        val businessId = business?.businessId ?: return
        localStorage.deleteObject(orderCreationCheckpointKey(businessId))
    }

    fun saveOrderCreationCheckpoint(step: OrderCreationStep) {
        if (suppressOrderCheckpointWrites) return
        if (!step.isRecoverableOrderCreationStep()) return
        val businessId = business?.businessId ?: return
        val state = uiState.value
        if (!canUseOrderCreationCheckpoint(state)) return

        val data = state.toOrderCreationCheckpointData()
        if (!data.hasMeaningfulUserData()) {
            clearOrderCreationCheckpoint()
            return
        }

        val checkpoint = OrderCreationCheckpoint(
            businessId = businessId,
            savedAtEpochSeconds = Clock.System.now().epochSeconds,
            currentStep = step,
            data = data,
        )
        localStorage.set(orderCreationCheckpointKey(businessId), json.encodeToString(checkpoint))
    }

    fun disableOrderCreationCheckpointForCurrentFlow(clearExisting: Boolean = true) {
        orderCheckpointDisabledForCurrentFlow = true
        pendingOrderCreationCheckpoint = null
        updateState { copy(showOrderRestoreDialog = false) }
        if (clearExisting) {
            clearOrderCreationCheckpoint()
        }
    }

    private fun canUseOrderCreationCheckpoint(state: PosState = uiState.value): Boolean {
        if (orderCheckpointDisabledForCurrentFlow) return false
        if (state.flowMode != FlowMode.SALE) return false
        if (state.selectedDocType in noteDocumentTypes()) return false
        if (state.referencedNoteCUFE.isNotBlank()) return false
        return true
    }

    private fun OrderCreationStep.isRecoverableOrderCreationStep(): Boolean {
        return this != OrderCreationStep.CUSTOMER
    }

    private fun noteDocumentTypes(): Set<String> = setOf("04", "05", "06")

    private fun orderCreationCheckpointKey(businessId: Int): String {
        return "$ORDER_CREATION_CHECKPOINT_KEY_PREFIX:$businessId"
    }

    private fun PosState.toOrderCreationCheckpointData(): OrderCreationCheckpointData {
        val branchCode = branches.getOrNull(selectedBranchIndex)?.branchCode
        val billingPoint = billingPoints.getOrNull(selectedBillingPointIndex)?.billingPoint
        val snapshots = cart.mapNotNull { line ->
            resolveProductForLine(this, line)?.let { product -> line.lineId to product }
        }.toMap()

        return OrderCreationCheckpointData(
            branchCode = branchCode,
            billingPoint = billingPoint,
            selectedDocTypeIndex = selectedDocTypeIndex,
            selectedDocType = selectedDocType,
            selectedOperationNatureIndex = selectedOperationNatureIndex,
            selectedOperationNature = selectedOperationNature,
            invoiceIssueDateIso = invoiceIssueDateIso,
            customer = customer,
            finalCustomer = finalCustomer,
            finalName = finalName,
            finalEmail = finalEmail,
            finalPhone = finalPhone,
            finalIdTypeIndex = finalIdTypeIndex,
            finalIdType = finalIdType,
            finalIdNumber = finalIdNumber,
            finalCustomerCountryCode = finalCustomerCountryCode,
            cart = cart.map(OrderCreationCartLineCheckpoint::from),
            productSnapshots = snapshots,
            taxExempt = taxExempt,
            globalDiscountMode = globalDiscountMode,
            globalDiscountPercent = globalDiscountPercent,
            globalDiscountFixedCents = globalDiscountFixedCents,
            globalShippingCents = globalShippingCents,
            globalInsuranceCents = globalInsuranceCents,
            globalOtherChargesCents = globalOtherChargesCents,
            referencedNoteCUFE = referencedNoteCUFE,
            referencedCreatedAt = referencedCreatedAt,
            includeBottomNote = includeBottomNote,
            logisticsInfo = logisticsInfo,
            logisticsVehiclePlate = logisticsVehiclePlate,
            logisticsCarrierLegalName = logisticsCarrierLegalName,
            logisticsCarrierRuc = logisticsCarrierRuc,
            logisticsCarrierDv = logisticsCarrierDv,
            logisticsCarrierTaxpayerTypeIndex = logisticsCarrierTaxpayerTypeIndex,
            logisticsBoxesQty = logisticsBoxesQty,
            logisticsTotalWeightLb = logisticsTotalWeightLb,
            deliveryReceiverLegalName = deliveryReceiverLegalName,
            deliveryReceiverRuc = deliveryReceiverRuc,
            deliveryReceiverDv = deliveryReceiverDv,
            deliveryReceiverTaxpayerTypeIndex = deliveryReceiverTaxpayerTypeIndex,
            deliveryContactPhone = deliveryContactPhone,
            deliveryAltContactPhone = deliveryAltContactPhone,
            deliveryProvinceIndex = deliveryProvinceIndex,
            deliveryDistrictIndex = deliveryDistrictIndex,
            deliveryCorregIndex = deliveryCorregIndex,
            selectedCustomerAddressId = selectedCustomerAddressId,
            retentionCodeIndex = retentionCodeIndex,
            retentionAmount = retentionAmount,
            exportIncoterm = exportIncoterm,
            exportCurrency = exportCurrency,
            exportPortOfLoading = exportPortOfLoading,
        )
    }

    private fun resolveCheckpointBranchSelection(
        data: OrderCreationCheckpointData,
        current: PosState,
    ): Triple<Int, List<FiscalBillingPoint>, Int> {
        val branches = current.branches
        if (branches.isEmpty()) return Triple(0, emptyList(), 0)
        val branchIndex = data.branchCode
            ?.let { code -> branches.indexOfFirst { it.branchCode == code } }
            ?.takeIf { it >= 0 }
            ?: current.selectedBranchIndex.coerceIn(0, branches.lastIndex)
        val billingPoints = branches[branchIndex].fiscalBillingPoints
        val billingPointIndex = data.billingPoint
            ?.let { point -> billingPoints.indexOfFirst { it.billingPoint == point } }
            ?.takeIf { it >= 0 }
            ?: if (billingPoints.isEmpty()) 0 else current.selectedBillingPointIndex.coerceIn(0, billingPoints.lastIndex)
        return Triple(branchIndex, billingPoints, billingPointIndex)
    }


    fun createOrder(
        createPaymentLink: Boolean = false,
        createYappyOnsite: Boolean = false,
        saveAsDraft: Boolean
    ) {
        if (!validateFinalCustomerSelection()) {
            return
        }
        updateState {
            copy(
                orderCreationFailed = false,
                orderCreationErrorMessage = "",
                postCreateInvoiceWarning = com.teco.ventago.features.invoicing.domain.PostCreateInvoiceWarningState(),
            )
        }
        if (saveAsDraft) {
            analyticsService.logOrderCreationPaymentOptionSelected(mode = "DRAFT")
        }
        val hasPaymentLinkAccess = canAction(ActionKey.ORDERS_PAYMENT_LINK)
        val hasYappyOnsiteAccess = canAction(ActionKey.ORDERS_YAPPY_ONSITE)
        val hasManualPaymentAccess = canAction(ActionKey.ORDERS_MANUAL_PAYMENT)
        val effectiveCreatePaymentLink = createPaymentLink && hasPaymentLinkAccess
        val effectiveCreateYappyOnsite = createYappyOnsite && hasYappyOnsiteAccess && selectedBillingPointHasYappyOnsiteDevice()
        val isManualPayment = !saveAsDraft && !effectiveCreatePaymentLink && !effectiveCreateYappyOnsite
        if (createPaymentLink && !hasPaymentLinkAccess) {
            updateState { copy(paymentFlowMode = PaymentFlowMode.MANUAL_OR_INSTALLMENTS) }
            viewModelScope.launch {
                snackbarService.show("No tienes permisos para crear enlaces de pago. Se aplicará cobro manual.")
            }
        }
        if (createYappyOnsite && !hasYappyOnsiteAccess) {
            updateState { copy(paymentFlowMode = PaymentFlowMode.MANUAL_OR_INSTALLMENTS) }
            viewModelScope.launch {
                snackbarService.show("No tienes permisos para generar QR de Yappy en caja.")
            }
            return
        }
        if (createYappyOnsite && !effectiveCreateYappyOnsite) {
            viewModelScope.launch {
                snackbarService.show("Yappy en caja no está configurado para esta sucursal y punto de facturación.")
            }
            return
        }
        if (isManualPayment && !hasManualPaymentAccess) {
            showError()
            viewModelScope.launch {
                snackbarService.show("No tienes permisos para registrar cobros manuales.")
            }
            return
        }
        val allowed = if (saveAsDraft) {
            canAction(ActionKey.ORDERS_CREATE_DRAFT)
        } else {
            canAction(ActionKey.ORDERS_CREATE)
        }
        if (!allowed) {
            showError()
            return
        }
        val stateBeforeSubmit = uiState.value
        if (!saveAsDraft && !effectiveCreatePaymentLink && !effectiveCreateYappyOnsite && stateBeforeSubmit.installments.any { it.dueDateIso.isBlank() }) {
            viewModelScope.launch {
                snackbarService.show("Selecciona fecha de vencimiento para cada pago a crédito.")
            }
            return
        }
        if (!saveAsDraft && !validateCreditNoteAmountLimit(stateBeforeSubmit)) {
            return
        }
        if (!saveAsDraft && !validateGenericCreditNoteReference(stateBeforeSubmit)) {
            return
        }
        showLoading()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val request = createOrderRequest(effectiveCreatePaymentLink, saveAsDraft, effectiveCreateYappyOnsite)
                  val response = posService.createOrder(business!!.businessId, request)
                  val hasValidOrderNumber = response.orderNumber.isNotBlank()
                  val invoiceStatusFromResponse = InvoiceStatus.fromId(response.invoiceStatus)
                  val postCreateInvoiceWarning = resolvePostCreateInvoiceWarning(
                          invoiceStatus = response.invoiceStatus,
                      invoiceWarningCode = response.invoiceWarningCode,
                      invoiceWarningMessage = response.invoiceWarningMessage,
                      isImmediateInvoiceCreate = !effectiveCreatePaymentLink && !saveAsDraft
                  )
                  
                  updateState {
                      copy(
                          invoiceStatus = invoiceStatusFromResponse,
                          pdfDocument = response.invoiceFiles?.pdf ?: "",
                          paymentLink = response.links?.firstOrNull { link -> link.action == "payer_action" }?.resolvedUrl()
                              ?: response.links?.firstOrNull { it.resolvedUrl().isNotBlank() }?.resolvedUrl()
                              ?: "",
                          createdOrderId = response.id,
                          paymentLinkPolling = false,
                          paymentLinkPaymentDetected = false,
                          paymentLinkInvoicePrintAttemptedOrderId = null,
                          paymentLinkManualPanelVisible = false,
                          paymentLinkManualErrorMessage = null,
                          pendingPaymentChangeSourceMethod = null,
                          pendingPaymentChangeSourceReleased = false,
                          pendingPaymentChangeOpen = false,
                          pendingPaymentChangeCompleted = false,
                          pendingPaymentChangeCancelDialogVisible = false,
                          pendingPaymentChangeCancelErrorMessage = null,
                          pendingPaymentChangeExitAction = null,
                          onsitePayment = response.onsitePayment,
                          yappyOnsiteTransaction = null,
                          yappyOnsitePolling = false,
                          yappyOnsitePollingSuppressed = false,
                          yappyOnsiteInvoiceProcessingTimedOut = false,
                          yappyOnsitePrintAttemptedTransactionId = null,
                          yappyOnsiteExitCancelDialogVisible = false,
                          successReprintInFlight = false,
                          orderNumber = response.orderNumber,
                          postCreateInvoiceWarning = postCreateInvoiceWarning,
                          orderCreationFailed = !hasValidOrderNumber
                      )
                  }

                  if (hasValidOrderNumber && !effectiveCreatePaymentLink && !effectiveCreateYappyOnsite && !saveAsDraft) {
                      val printer = printerService.resolveActivePrinter(
                          branchCode = request.branch.code,
                          billingPointCode = request.branch.billingPoint
                      )
                      if (printer != null) {
                          val ticketPayload = response.invoiceFiles?.ticket
                          if (ticketPayload != null) {
                              runCatching {
                                  printerService.printTicketPayload(
                                      printerConfig = printer,
                                      ticketPayload = ticketPayload,
                                      context = PrintContext(
                                          source = "pos_order_create",
                                          orderId = response.id
                                      )
                                  )
                              }.onFailure {
                                  loggerPrintFailure(response.orderNumber, it)
                                  withContext(Dispatchers.Main) {
                                      snackbarService.show("La venta se guardó, pero la impresión del ticket falló.")
                                  }
                              }
                          } else {
                              withContext(Dispatchers.Main) {
                                  snackbarService.show("La venta se guardó, pero el ticket no estuvo disponible para imprimir.")
                              }
                          }
                      }
                  }
                  
                  withContext(Dispatchers.Main) {
                      if (!hasValidOrderNumber) {
                          showError()
                      } else {
                        clearOrderCreationCheckpoint()
                        showSuccess()
                      }
                  }
                } catch (e: Exception) {
                    println("Error creating order: ${e.message}")
                    val yappyPendingConflict = yappyOnsitePendingConflict(e)
                    if (effectiveCreateYappyOnsite && yappyPendingConflict != null) {
                        updateState {
                            copy(
                                orderCreationFailed = false,
                                showYappyOnsitePendingConflictDialog = true,
                                yappyOnsitePendingConflict = yappyPendingConflict,
                            )
                        }
                        withContext(Dispatchers.Main) {
                            hideLoading()
                        }
                        return@withContext
                    }
                    updateState {
                        copy(
                            orderCreationFailed = true,
                            orderCreationErrorMessage = InventorySaleErrorMapper.messageFor(
                                e,
                                "No se pudo crear el pedido. Por favor, intenta nuevamente o contacta al soporte.",
                            ),
                        )
                    }
                    withContext(Dispatchers.Main) {
                        showError()
                    }
                }
            }

        }
    }

    private fun yappyOnsitePendingConflict(error: Throwable): YappyOnsitePendingTransactionDto? {
        if (error is YappyOnsitePendingTransactionExistsException) {
            return error.pendingTransaction
        }
        val raw = error.message.orEmpty()
        return if (raw.contains(YAPPY_ONSITE_PENDING_TRANSACTION_EXISTS, ignoreCase = true)) {
            YappyOnsitePendingTransactionDto()
        } else {
            null
        }
    }

    fun keepYappyOnsitePendingConflictActive() {
        updateState {
            copy(
                showYappyOnsitePendingConflictDialog = false,
                yappyOnsitePendingConflict = null,
                paymentFlowMode = PaymentFlowMode.MANUAL_OR_INSTALLMENTS,
            )
        }
    }

    fun cancelPendingYappyOnsiteAndRetry() {
        val businessId = business?.businessId ?: -1
        val state = uiState.value
        val branchCode = state.yappyOnsitePendingConflict?.branchCode?.takeIf { it.isNotBlank() }
            ?: state.branches.getOrNull(state.selectedBranchIndex)?.branchCode.orEmpty()
        val billingPoint = state.yappyOnsitePendingConflict?.billingPoint?.takeIf { it.isNotBlank() }
            ?: state.billingPoints.getOrNull(state.selectedBillingPointIndex)?.billingPoint.orEmpty()

        if (businessId <= 0 || branchCode.isBlank() || billingPoint.isBlank()) {
            updateState {
                copy(
                    showYappyOnsitePendingConflictDialog = false,
                    yappyOnsitePendingConflict = null,
                )
            }
            viewModelScope.launch { snackbarService.show("No se pudo identificar la caja para cancelar el QR pendiente.") }
            return
        }

        updateState {
            copy(
                showYappyOnsitePendingConflictDialog = false,
                yappyOnsitePendingConflict = null,
            )
        }
        showLoading()
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    paymentService.cancelPendingYappyOnsiteTransaction(
                        businessId = businessId,
                        branchCode = branchCode,
                        billingPoint = billingPoint,
                        reason = "cashier_cancelled_pending_qr",
                    )
                }
            }
            result.onSuccess { response ->
                if (response.cancelled) {
                    createOrder(createYappyOnsite = true, saveAsDraft = false)
                } else {
                    showError()
                    snackbarService.show("No fue posible cancelar el QR pendiente.")
                }
            }.onFailure { error ->
                println("Error cancelling pending Yappy onsite transaction: ${error.message}")
                showError()
                snackbarService.show("No fue posible cancelar el QR pendiente.")
            }
        }
    }

    private fun validateCreditNoteAmountLimit(state: PosState): Boolean {
        val errorMessage = PosNoteValidators.validateCreditNoteAmountLimit(
            selectedDocType = state.selectedDocType,
            referencedNoteCUFE = state.referencedNoteCUFE,
            maxCreditNoteAmountCents = state.maxCreditNoteAmountCents,
            requestedCreditNoteCents = legalInvoiceTotal(),
            sourceOrderNumber = state.sourceOrderNumber
        ) ?: return true

        viewModelScope.launch { snackbarService.show(errorMessage) }
        return false
    }

    private fun validateGenericCreditNoteReference(state: PosState): Boolean {
        val numberError = PosNoteValidators.validateOriginalInvoiceNumber(
            selectedDocType = state.selectedDocType,
            value = state.originalInvoiceNumber
        )
        val dateError = PosNoteValidators.validateOriginalInvoiceEmissionDate(
            selectedDocType = state.selectedDocType,
            value = state.originalInvoiceEmissionDateIso
        )
        if (numberError == null && dateError == null) return true

        updateState {
            copy(
                originalInvoiceNumberError = numberError,
                originalInvoiceEmissionDateError = dateError
            )
        }
        val message = numberError ?: dateError ?: "Completa la referencia de la factura original."
        viewModelScope.launch { snackbarService.show(message) }
        return false
    }

    @OptIn(ExperimentalTime::class)
    fun createOrderRequest(
        createPaymentLink: Boolean,
        saveAsDraft: Boolean,
        createYappyOnsite: Boolean = false,
    ): CreateOrderRequest {
        val state = uiState.value
        val isFinalCustomer = requireNotNull(state.finalCustomer) { "Customer type not selected" }

        val operationDestination = if (state.selectedDocType == "03" || state.selectedDocType == "10") "2" else "1"


        val invoice = Invoice(
            type = state.selectedDocType,
            deliveryDate = null,
            operationNature = state.selectedOperationNature,
            operationDestination = operationDestination,
            issuerFeAdditionalInfo = "",
            issuedDatetime = resolveInvoiceIssuedDatetime(state.invoiceIssueDateIso)
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
        val finalCustomerCountryCode = if (finalIdTypeRequiresCountry(state.finalIdType)) {
            state.finalCustomerCountryCode
                ?.takeIf { code -> state.finalCustomerCountryOptions.any { it.code == code } }
                ?: "CO"
        } else {
            null
        }

        if (
            isFinalCustomer &&
            (
                state.finalName != null ||
                    state.finalIdNumber != null ||
                    state.finalEmail != null ||
                    finalCustomerCountryCode != null
                )
        ) {
            var idType: String? = null
            if (state.finalIdNumber != null) {
                idType = state.finalIdType
            }
            finalCustomerInfo = FinalCustomerInfo(
                name = state.finalName,
                email = state.finalEmail,
                identificationType = idType,
                identificationNumber = state.finalIdNumber,
                countryCode = finalCustomerCountryCode
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
                // Saved item: lookup by itemId, then fall back to restored snapshot.
                itemsById[line.itemId] ?: state.personalizedItems[line.lineId]
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
                quantity = item.quantity.toQuantityRequestString(),
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
                productType = product.productType.code,
                locationId = item.locationId?.toLong()
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
            quantityItems = state.cart.size,
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
        if ((createPaymentLink || createYappyOnsite) && !saveAsDraft) {
            links = PaymentLinksBlock(
                create = true,
                expireInMinutes = if (createYappyOnsite) 5 else 140,
                note = if (createYappyOnsite) yappyOnsitePaymentDescription() else "",
                method = if (createYappyOnsite) "YAPPY_ONSITE" else "LINK"
            )
        } else if (!saveAsDraft) {
            for ((key, value) in state.charged) {
                val methodLabel = manualMethodOptions().firstOrNull { it.first == key }?.second ?: "Método $key"
                // For type 99 (OTHER), use custom description and ensure it's bigger than 10 characters
                val description = if (key == 99) {
                    val customDesc = state.otherPaymentDescription.ifBlank { 
                        methodLabel
                    }
                    // Add meaningful text instead of padding with spaces (server trims spaces)
                    if (customDesc.length <= 10) {
                        "Otro: $customDesc"
                    } else {
                        customDesc
                    }
                } else {
                    methodLabel
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
            // Convert UTC timestamp to Panama timezone
            val panamaIssueDatetime = convertUtcToPanamaTimezone(state.referencedCreatedAt)
            references = listOf(
                References(
                    legalName = "", // Empty backend fills this with business name
                    issueDatetime = panamaIssueDatetime,
                    referenceNumber = ReferenceNumber(
                        type = "cufe", // For now only cufe references allowed
                        number = state.referencedNoteCUFE
                    )
                )
            )
        } else if (state.selectedDocType == "06") {
            val originalInvoiceNumber = state.originalInvoiceNumber.trim()
            val originalInvoiceDate = state.originalInvoiceEmissionDateIso
            if (originalInvoiceNumber.isNotBlank() && originalInvoiceDate.isNotBlank()) {
                references = listOf(
                    References(
                        legalName = "",
                        issueDatetime = "${originalInvoiceDate}T00:00:00",
                        referenceNumber = ReferenceNumber(
                            type = "paper",
                            number = originalInvoiceNumber
                        )
                    )
                )
            }
        }

        val orderReference: OrderReference? = null // TODO add references if needed

        var retentions: Retentions? = null
        val retentionOption = retentionOptionsList.getOrNull(state.retentionCodeIndex)
        val retentionCode = retentionOption?.code.orEmpty()
        val retentionRate = when {
            retentionCode.isEmpty() -> ""
            retentionOption?.defaultRate != null -> retentionOption.defaultRate.toString()
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

        val selectedAddressLocationCode = state.customerAddresses
            .firstOrNull { it.id == state.selectedCustomerAddressId }
            ?.locationCode
            ?.takeIf { it.isNotBlank() }

        val hasDeliveryData = state.deliveryReceiverRuc.isNotEmpty() || selectedAddressLocationCode != null

        val deliveryLocation: DeliveryLocation? = if (hasDeliveryData) {
            DeliveryLocation(
                receiverLegalName = state.deliveryReceiverLegalName,
                receiverTaxpayerType = if (state.deliveryReceiverTaxpayerTypeIndex == 0) "01" else "02",
                receiverTaxId = state.deliveryReceiverRuc,
                receiverTaxDv = state.deliveryReceiverDv,
                contactPhone = state.deliveryContactPhone,
                alternateContactPhone = state.deliveryAltContactPhone,
                locationCode = selectedAddressLocationCode ?: "8-8-8",
            )
        } else {
            null
        }

        val selectedCustomerAddress = state.customerAddresses
            .firstOrNull { it.id == state.selectedCustomerAddressId }

        val additionalAddress = selectedCustomerAddress
            ?.takeIf { !it.isDefault }
            ?.let { address ->
                val locationCode = address.locationCode?.takeIf { it.isNotBlank() } ?: return@let null
                AdditionalAddress(
                    addressLine = address.addressLine,
                    locationCode = locationCode,
                    email = address.email?.trim()?.takeIf { it.isNotBlank() }
                )
            }

        val commercialAddenda: CommercialAddenda? = null // TODO add commercial addenda if needed

        val formats = buildList {
            add("PDF")
            add("XML")
            if (printerService.shouldRequestTicket(branch.code, branch.billingPoint)) {
                add("TICKET")
            }
        }

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
            additionalAddress = additionalAddress,
            commercialAddenda = commercialAddenda,
            links = links,
            paymentFlowType = if (createYappyOnsite) "in_place" else null,
            formats = formats,
            includeBottomNote = if (
                state.bottomNoteRefreshFailed ||
                state.bottomNoteSettings == null ||
                state.bottomNoteSettings.title.isBlank() ||
                state.bottomNoteSettings.body.isBlank()
            ) {
                null
            } else {
                state.includeBottomNote
            },
            saveAs = if (saveAsDraft) "draft" else "confirmed"
        )
    }

    private fun yappyOnsitePaymentDescription(): String {
        return business?.name?.trim()?.takeIf { it.isNotBlank() } ?: "Pago Yappy"
    }

    fun startPaymentLinkStatusPolling() {
        val initialState = uiState.value
        val businessId = business?.businessId ?: return
        val orderId = initialState.createdOrderId ?: return
        if (initialState.paymentFlowMode != PaymentFlowMode.PAYMENT_LINK) return
        if (initialState.paymentLink.isBlank()) return
        if (paymentLinkPollingJob?.isActive == true) return

        paymentLinkPollingJob = viewModelScope.launch {
            updateState { copy(paymentLinkPolling = true) }
            try {
                while (true) {
                    val freshOrder = try {
                        withContext(Dispatchers.IO) {
                            orderService.refreshOrder(businessId = businessId, orderId = orderId)
                        }
                    } catch (error: Throwable) {
                        if (error is CancellationException) throw error
                        delay(PAYMENT_LINK_POLL_MS)
                        continue
                    }

                    val invoiceStatus = InvoiceStatus.fromId(freshOrder.invoiceStatus ?: InvoiceStatus.NONE.id)
                    val paymentDetected = uiState.value.paymentLinkPaymentDetected ||
                        freshOrder.paymentStatus == PaymentStatus.PAID.id ||
                        invoiceStatus == InvoiceStatus.ISSUED

                    updateState {
                        copy(
                            paymentLinkPaymentDetected = paymentDetected,
                            invoiceStatus = invoiceStatus,
                            orderNumber = freshOrder.internalNumber.ifBlank { orderNumber },
                        )
                    }

                    if (paymentDetected && !uiState.value.autoInvoiceOnPaymentSuccess) {
                        return@launch
                    }

                    when (invoiceStatus) {
                        InvoiceStatus.ISSUED -> {
                            handlePaymentLinkInvoiceIssued(
                                businessId = businessId,
                                freshOrder = freshOrder,
                            )
                            return@launch
                        }

                        InvoiceStatus.FAILED -> {
                            snackbarService.show("El pago fue recibido, pero la factura requiere atención.")
                            return@launch
                        }

                        else -> delay(PAYMENT_LINK_POLL_MS)
                    }
                }
            } finally {
                updateState { copy(paymentLinkPolling = false) }
                paymentLinkPollingJob = null
            }
        }
    }

    fun stopPaymentLinkStatusPolling() {
        paymentLinkPollingJob?.cancel()
        paymentLinkPollingJob = null
        updateState { copy(paymentLinkPolling = false) }
    }

    fun canSwitchPaymentLinkToManual(): Boolean {
        val state = uiState.value
        return state.canUseManualPaymentMethods &&
            state.createdOrderId != null &&
            state.paymentLink.isNotBlank() &&
            !state.paymentLinkPaymentDetected &&
            state.invoiceStatus != InvoiceStatus.ISSUED &&
            state.invoiceStatus != InvoiceStatus.FAILED
    }

    fun canSwitchPaymentLinkToYappyOnsite(): Boolean {
        val state = uiState.value
        return canSwitchPaymentLinkToManual() &&
            state.canCreateYappyOnsiteQr &&
            selectedBillingPointHasYappyOnsiteDevice()
    }

    fun canOpenPendingPaymentMethodChange(sourceMethod: PendingPaymentIntentMethod): Boolean {
        val state = uiState.value
        return when (sourceMethod) {
            PendingPaymentIntentMethod.PAYMENT_LINK -> canSwitchPaymentLinkToManual()
            PendingPaymentIntentMethod.YAPPY_ONSITE -> state.canUseManualPaymentMethods &&
                hasActiveYappyOnsitePendingIntent(state)
        }
    }

    fun openPendingPaymentMethodChange(sourceMethod: PendingPaymentIntentMethod) {
        if (!canOpenPendingPaymentMethodChange(sourceMethod)) return
        updateState {
            copy(
                pendingPaymentChangeSourceMethod = sourceMethod,
                pendingPaymentChangeSourceReleased = false,
                pendingPaymentChangeOpen = true,
                pendingPaymentChangeCompleted = false,
                pendingPaymentChangeCancelDialogVisible = false,
                pendingPaymentChangeCancelErrorMessage = null,
                pendingPaymentChangeExitAction = null,
                paymentFlowMode = PaymentFlowMode.MANUAL_OR_INSTALLMENTS,
                charged = emptyMap(),
                installments = emptyList(),
                otherPaymentDescription = "",
                paymentLinkManualErrorMessage = null,
            )
        }
    }

    fun confirmOpenYappyOnsitePaymentMethodChange() {
        val state = uiState.value
        val businessId = business?.businessId ?: return
        val orderId = state.createdOrderId ?: state.onsitePayment?.orderId ?: return
        if (!canOpenPendingPaymentMethodChange(PendingPaymentIntentMethod.YAPPY_ONSITE)) return

        showLoading()
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val released = releasePendingPaymentChangeIntent(
                        businessId = businessId,
                        orderId = orderId,
                        sourceMethod = PendingPaymentIntentMethod.YAPPY_ONSITE,
                        targetMethod = PaymentFlowMode.MANUAL_OR_INSTALLMENTS,
                    )
                    if (!released.released || !released.allowsNextAction("manual_payment_allowed")) {
                        error("No se pudo liberar el QR de Yappy pendiente.")
                    }
                }
            }.onSuccess {
                updateState {
                    copy(
                        createdOrderId = createdOrderId ?: orderId,
                        pendingPaymentChangeSourceMethod = PendingPaymentIntentMethod.YAPPY_ONSITE,
                        pendingPaymentChangeSourceReleased = true,
                        pendingPaymentChangeOpen = true,
                        pendingPaymentChangeCompleted = false,
                        pendingPaymentChangeCancelDialogVisible = false,
                        pendingPaymentChangeCancelErrorMessage = null,
                        pendingPaymentChangeExitAction = null,
                        paymentFlowMode = PaymentFlowMode.MANUAL_OR_INSTALLMENTS,
                        paymentLinkManualErrorMessage = null,
                        charged = emptyMap(),
                        installments = emptyList(),
                        otherPaymentDescription = "",
                        onsitePayment = null,
                        yappyOnsiteTransaction = null,
                        yappyOnsitePolling = false,
                        yappyOnsitePollingSuppressed = true,
                    )
                }
                showSuccess()
            }.onFailure { error ->
                val message = paymentLinkSwitchErrorMessage(error)
                snackbarService.show(message)
                showError()
            }
        }
    }

    fun confirmOpenPaymentLinkPaymentMethodChange() {
        val state = uiState.value
        val businessId = business?.businessId ?: return
        val orderId = state.createdOrderId ?: return
        if (!canOpenPendingPaymentMethodChange(PendingPaymentIntentMethod.PAYMENT_LINK)) return

        showLoading()
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val released = releasePendingPaymentChangeIntent(
                        businessId = businessId,
                        orderId = orderId,
                        sourceMethod = PendingPaymentIntentMethod.PAYMENT_LINK,
                        targetMethod = PaymentFlowMode.MANUAL_OR_INSTALLMENTS,
                    )
                    if (!released.released || !released.allowsNextAction("manual_payment_allowed")) {
                        error("No se pudo liberar el link de pago pendiente.")
                    }
                }
            }.onSuccess {
                updateState {
                    copy(
                        pendingPaymentChangeSourceMethod = PendingPaymentIntentMethod.PAYMENT_LINK,
                        pendingPaymentChangeSourceReleased = true,
                        pendingPaymentChangeOpen = true,
                        pendingPaymentChangeCompleted = false,
                        pendingPaymentChangeCancelDialogVisible = false,
                        pendingPaymentChangeCancelErrorMessage = null,
                        pendingPaymentChangeExitAction = null,
                        paymentFlowMode = PaymentFlowMode.MANUAL_OR_INSTALLMENTS,
                        paymentLinkPolling = false,
                        paymentLinkManualPanelVisible = false,
                        paymentLinkManualErrorMessage = null,
                        charged = emptyMap(),
                        installments = emptyList(),
                        otherPaymentDescription = "",
                    )
                }
                showSuccess()
            }.onFailure { error ->
                val message = paymentLinkSwitchErrorMessage(error)
                snackbarService.show(message)
                showError()
            }
        }
    }

    fun requestPendingPaymentChangeExit(action: PendingPaymentChangeExitAction): Boolean {
        val state = uiState.value
        if (!state.pendingPaymentChangeOpen || state.pendingPaymentChangeCompleted) return false
        updateState {
            copy(
                pendingPaymentChangeCancelDialogVisible = true,
                pendingPaymentChangeCancelErrorMessage = null,
                pendingPaymentChangeExitAction = action,
            )
        }
        return true
    }

    fun dismissPendingPaymentChangeCancelDialog() {
        updateState {
            copy(
                pendingPaymentChangeCancelDialogVisible = false,
                pendingPaymentChangeCancelErrorMessage = null,
                pendingPaymentChangeExitAction = null,
            )
        }
    }

    fun confirmPendingPaymentChangeOrderCancellation(onCancelled: (PendingPaymentChangeExitAction) -> Unit) {
        val state = uiState.value
        val businessId = business?.businessId ?: return
        val orderId = state.createdOrderId ?: state.onsitePayment?.orderId ?: return
        val exitAction = state.pendingPaymentChangeExitAction ?: PendingPaymentChangeExitAction.POS_START

        showLoading()
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    orderService.cancelOrder(
                        businessId = businessId,
                        orderID = orderId,
                        reason = "customer_abandoned_payment_method_change",
                        userName = authService.getUserSync()?.name ?: "App",
                    )
                }
            }.onSuccess { cancelled ->
                if (cancelled) {
                    updateState {
                        copy(
                            pendingPaymentChangeCancelDialogVisible = false,
                            pendingPaymentChangeCancelErrorMessage = null,
                            pendingPaymentChangeExitAction = null,
                            pendingPaymentChangeSourceReleased = false,
                            pendingPaymentChangeOpen = false,
                            pendingPaymentChangeCompleted = true,
                        )
                    }
                    hideLoading()
                    onCancelled(exitAction)
                } else {
                    updateState {
                        copy(pendingPaymentChangeCancelErrorMessage = "No se pudo cancelar la orden.")
                    }
                    showError()
                }
            }.onFailure { error ->
                val message = paymentLinkSwitchErrorMessage(error)
                updateState { copy(pendingPaymentChangeCancelErrorMessage = message) }
                snackbarService.show(message)
                showError()
            }
        }
    }

    fun confirmPendingPaymentChangeManual(onCompleted: () -> Unit = {}) {
        val state = uiState.value
        if (!state.canUseManualPaymentMethods) return
        val businessId = business?.businessId ?: return
        val orderId = state.createdOrderId ?: state.onsitePayment?.orderId ?: return
        val sourceMethod = state.pendingPaymentChangeSourceMethod ?: return
        val validation = validateManualReplacementPayments(state)
        if (validation != null) {
            updateState { copy(paymentLinkManualErrorMessage = validation) }
            return
        }
        val payments = manualReplacementPaymentSubmissions(state)
        if (payments.isEmpty()) {
            updateState { copy(paymentLinkManualErrorMessage = "Agrega al menos un método de pago.") }
            return
        }

        showLoading()
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val released = releasePendingPaymentChangeIntentIfNeeded(
                        state = state,
                        businessId = businessId,
                        orderId = orderId,
                        sourceMethod = sourceMethod,
                        targetMethod = PaymentFlowMode.MANUAL_OR_INSTALLMENTS,
                        requiredNextAction = "manual_payment_allowed",
                    )
                    if (!released) {
                        error("No se pudo liberar el cobro pendiente.")
                    }
                    val response = orderService.registerOrderPayments(
                        businessId = businessId,
                        orderId = orderId,
                        payments = payments,
                    )
                    val freshOrder = orderService.refreshOrder(businessId = businessId, orderId = orderId)
                    response to freshOrder
                }
                val (response, freshOrder) = result
                completeManualPaymentMethodChange(response, freshOrder)
                showSuccess()
                onCompleted()
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                updateState { copy(paymentLinkManualErrorMessage = paymentLinkSwitchErrorMessage(error)) }
                snackbarService.show(paymentLinkSwitchErrorMessage(error))
                showError()
            }
        }
    }

    fun confirmPendingPaymentChangePaymentLink(onCreated: () -> Unit = {}) {
        val state = uiState.value
        val businessId = business?.businessId ?: return
        val orderId = state.createdOrderId ?: state.onsitePayment?.orderId ?: return
        val sourceMethod = state.pendingPaymentChangeSourceMethod ?: return
        if (sourceMethod == PendingPaymentIntentMethod.PAYMENT_LINK) return
        val amount = amountToCharge().takeIf { it > 0L }?.toDecimalString() ?: return

        showLoading()
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val released = releasePendingPaymentChangeIntentIfNeeded(
                        state = state,
                        businessId = businessId,
                        orderId = orderId,
                        sourceMethod = sourceMethod,
                        targetMethod = PaymentFlowMode.PAYMENT_LINK,
                        requiredNextAction = "payment_link_allowed",
                    )
                    if (!released) {
                        error("No se pudo liberar el cobro pendiente.")
                    }
                    orderService.createReplacementPaymentLink(
                        businessId = businessId,
                        orderId = orderId,
                        amount = amount,
                    )
                }
            }.onSuccess { replacement ->
                val link = replacement.resolvedPaymentLinkUrl()
                updateState {
                    copy(
                        paymentFlowMode = PaymentFlowMode.PAYMENT_LINK,
                        paymentLink = link,
                        paymentLinkPolling = false,
                        paymentLinkPaymentDetected = false,
                        paymentLinkInvoicePrintAttemptedOrderId = null,
                        paymentLinkManualPanelVisible = false,
                        paymentLinkManualErrorMessage = null,
                        pendingPaymentChangeSourceReleased = false,
                        pendingPaymentChangeOpen = false,
                        pendingPaymentChangeCompleted = true,
                        pendingPaymentChangeCancelDialogVisible = false,
                        pendingPaymentChangeCancelErrorMessage = null,
                        pendingPaymentChangeExitAction = null,
                        onsitePayment = null,
                        yappyOnsiteTransaction = null,
                        yappyOnsitePolling = false,
                        yappyOnsitePollingSuppressed = false,
                        yappyOnsiteExitCancelDialogVisible = false,
                        orderNumber = replacement.orderNumber.ifBlank { orderNumber },
                        invoiceStatus = InvoiceStatus.fromId(replacement.invoiceStatus),
                    )
                }
                showSuccess()
                onCreated()
            }.onFailure { error ->
                snackbarService.show(paymentLinkSwitchErrorMessage(error))
                showError()
            }
        }
    }

    fun confirmPendingPaymentChangeYappyOnsite(onCreated: () -> Unit) {
        val state = uiState.value
        val businessId = business?.businessId ?: return
        val orderId = state.createdOrderId ?: return
        val sourceMethod = state.pendingPaymentChangeSourceMethod ?: return
        if (sourceMethod == PendingPaymentIntentMethod.YAPPY_ONSITE) return
        if (!state.canCreateYappyOnsiteQr || !selectedBillingPointHasYappyOnsiteDevice()) return
        val amount = amountToCharge().takeIf { it > 0L }?.toDecimalString() ?: return

        stopPaymentLinkStatusPolling()
        showLoading()
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val released = releasePendingPaymentChangeIntentIfNeeded(
                        state = state,
                        businessId = businessId,
                        orderId = orderId,
                        sourceMethod = sourceMethod,
                        targetMethod = PaymentFlowMode.YAPPY_ONSITE,
                        requiredNextAction = "yappy_onsite_allowed",
                    )
                    if (!released) {
                        error("No se pudo liberar el cobro pendiente.")
                    }
                    orderService.createReplacementYappyOnsite(
                        businessId = businessId,
                        orderId = orderId,
                        amount = amount,
                        note = yappyOnsitePaymentDescription(),
                    )
                }
            }.onSuccess { replacement ->
                updateState {
                    copy(
                        paymentFlowMode = PaymentFlowMode.YAPPY_ONSITE,
                        paymentLink = "",
                        paymentLinkPolling = false,
                        paymentLinkPaymentDetected = false,
                        paymentLinkInvoicePrintAttemptedOrderId = null,
                        paymentLinkManualPanelVisible = false,
                        paymentLinkManualErrorMessage = null,
                        pendingPaymentChangeSourceReleased = false,
                        pendingPaymentChangeOpen = false,
                        pendingPaymentChangeCompleted = true,
                        pendingPaymentChangeCancelDialogVisible = false,
                        pendingPaymentChangeCancelErrorMessage = null,
                        pendingPaymentChangeExitAction = null,
                        onsitePayment = replacement.onsitePayment,
                        yappyOnsiteTransaction = null,
                        yappyOnsitePolling = false,
                        yappyOnsitePollingSuppressed = false,
                        yappyOnsiteInvoiceProcessingTimedOut = false,
                        yappyOnsitePrintAttemptedTransactionId = null,
                        yappyOnsiteExitCancelDialogVisible = false,
                        orderNumber = replacement.orderNumber.ifBlank { orderNumber },
                        invoiceStatus = InvoiceStatus.fromId(replacement.invoiceStatus),
                    )
                }
                showSuccess()
                onCreated()
            }.onFailure { error ->
                snackbarService.show(paymentLinkSwitchErrorMessage(error))
                showError()
            }
        }
    }

    fun switchPaymentLinkToManual() {
        val state = uiState.value
        val businessId = business?.businessId ?: return
        val orderId = state.createdOrderId ?: return
        if (!canSwitchPaymentLinkToManual()) return

        stopPaymentLinkStatusPolling()
        showLoading()
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val released = orderService.releasePendingPaymentIntent(
                        businessId = businessId,
                        orderId = orderId,
                        paymentMethod = "payment_link",
                        reason = "customer_selected_cash",
                    )
                    if (!released.released || !released.allowsNextAction("manual_payment_allowed")) {
                        error("No se pudo liberar el enlace de pago pendiente.")
                    }
                }
            }.onSuccess {
                updateState {
                    copy(
                        paymentFlowMode = PaymentFlowMode.MANUAL_OR_INSTALLMENTS,
                        paymentLink = "",
                        paymentLinkPolling = false,
                        paymentLinkManualPanelVisible = true,
                        paymentLinkManualErrorMessage = null,
                        charged = emptyMap(),
                        otherPaymentDescription = "",
                        installments = emptyList(),
                    )
                }
                showSuccess()
            }.onFailure { error ->
                updateState {
                    copy(paymentLinkManualErrorMessage = paymentLinkSwitchErrorMessage(error))
                }
                snackbarService.show(paymentLinkSwitchErrorMessage(error))
                showError()
            }
        }
    }

    fun submitPaymentLinkManualPayments() {
        val state = uiState.value
        val businessId = business?.businessId ?: return
        val orderId = state.createdOrderId ?: return
        val totalToCharge = amountToCharge()
        val paid = state.charged.values.sum()
        val requiresOtherDescription = state.charged.containsKey(99)
        val otherDescription = state.otherPaymentDescription.trim()

        val validation = when {
            paid < totalToCharge -> "El pago manual debe cubrir el total de la orden."
            requiresOtherDescription && otherDescription.length < 15 ->
                "Completa una descripción de al menos 15 caracteres para Otro."
            else -> null
        }
        if (validation != null) {
            updateState { copy(paymentLinkManualErrorMessage = validation) }
            return
        }

        val payments = state.charged.entries
            .filter { it.value > 0L }
            .map { (methodCode, amountCents) ->
                OrderPaymentSubmission(
                    methodCode = methodCode,
                    amountCents = amountCents,
                    paymentDateIso = currentPanamaDateTimeIsoWithOffset(),
                    otherDescription = if (methodCode == 99) otherDescription else null,
                )
            }

        if (payments.isEmpty()) {
            updateState { copy(paymentLinkManualErrorMessage = "Agrega al menos un método de pago.") }
            return
        }

        showLoading()
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    orderService.registerOrderPayments(
                        businessId = businessId,
                        orderId = orderId,
                        payments = payments,
                    )
                    orderService.refreshOrder(businessId = businessId, orderId = orderId)
                }
            }.onSuccess { freshOrder ->
                updateState {
                    copy(
                        orderNumber = freshOrder.internalNumber.ifBlank { orderNumber },
                        invoiceStatus = InvoiceStatus.fromId(freshOrder.invoiceStatus ?: InvoiceStatus.NONE.id),
                        paymentLinkManualPanelVisible = false,
                        paymentLinkManualErrorMessage = null,
                    )
                }
                showSuccess()
            }.onFailure { error ->
                updateState { copy(paymentLinkManualErrorMessage = paymentLinkSwitchErrorMessage(error)) }
                snackbarService.show(paymentLinkSwitchErrorMessage(error))
                showError()
            }
        }
    }

    fun switchPaymentLinkToYappyOnsite(onCreated: () -> Unit) {
        val state = uiState.value
        val businessId = business?.businessId ?: return
        val orderId = state.createdOrderId ?: return
        if (!canSwitchPaymentLinkToYappyOnsite()) return
        val amount = amountToCharge().takeIf { it > 0L }?.toDecimalString() ?: return

        stopPaymentLinkStatusPolling()
        showLoading()
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val released = orderService.releasePendingPaymentIntent(
                        businessId = businessId,
                        orderId = orderId,
                        paymentMethod = "payment_link",
                        reason = "customer_selected_yappy_onsite",
                    )
                    if (!released.released || !released.allowsNextAction("yappy_onsite_allowed")) {
                        error("No se pudo liberar el enlace de pago pendiente.")
                    }
                    orderService.createReplacementYappyOnsite(
                        businessId = businessId,
                        orderId = orderId,
                        amount = amount,
                        note = yappyOnsitePaymentDescription(),
                    )
                }
            }.onSuccess { replacement ->
                updateState {
                    copy(
                        paymentFlowMode = PaymentFlowMode.YAPPY_ONSITE,
                        paymentLink = "",
                        paymentLinkPolling = false,
                        paymentLinkManualPanelVisible = false,
                        paymentLinkManualErrorMessage = null,
                        onsitePayment = replacement.onsitePayment,
                        yappyOnsiteTransaction = null,
                        yappyOnsitePolling = false,
                        yappyOnsitePollingSuppressed = false,
                        yappyOnsiteInvoiceProcessingTimedOut = false,
                        yappyOnsitePrintAttemptedTransactionId = null,
                        yappyOnsiteExitCancelDialogVisible = false,
                    )
                }
                showSuccess()
                onCreated()
            }.onFailure { error ->
                snackbarService.show(paymentLinkSwitchErrorMessage(error))
                showError()
            }
        }
    }

    private suspend fun releasePendingPaymentChangeIntent(
        businessId: Int,
        orderId: Int,
        sourceMethod: PendingPaymentIntentMethod,
        targetMethod: PaymentFlowMode,
    ): PendingIntentReleaseResponse {
        when (sourceMethod) {
            PendingPaymentIntentMethod.PAYMENT_LINK -> stopPaymentLinkStatusPolling()
            PendingPaymentIntentMethod.YAPPY_ONSITE -> stopYappyOnsitePollingForRelease()
        }
        return orderService.releasePendingPaymentIntent(
            businessId = businessId,
            orderId = orderId,
            paymentMethod = sourceMethod.apiPaymentMethod(),
            reason = sourceMethod.releaseReasonFor(targetMethod),
        )
    }

    private suspend fun releasePendingPaymentChangeIntentIfNeeded(
        state: PosState,
        businessId: Int,
        orderId: Int,
        sourceMethod: PendingPaymentIntentMethod,
        targetMethod: PaymentFlowMode,
        requiredNextAction: String,
    ): Boolean {
        if (state.pendingPaymentChangeSourceReleased) return true
        val released = releasePendingPaymentChangeIntent(
            businessId = businessId,
            orderId = orderId,
            sourceMethod = sourceMethod,
            targetMethod = targetMethod,
        )
        return released.released && released.allowsNextAction(requiredNextAction)
    }

    private fun PendingPaymentIntentMethod.apiPaymentMethod(): String {
        return when (this) {
            PendingPaymentIntentMethod.PAYMENT_LINK -> "payment_link"
            PendingPaymentIntentMethod.YAPPY_ONSITE -> "yappy_onsite"
        }
    }

    private fun PendingPaymentIntentMethod.releaseReasonFor(targetMethod: PaymentFlowMode): String {
        return when (targetMethod) {
            PaymentFlowMode.MANUAL_OR_INSTALLMENTS -> "customer_selected_cash"
            PaymentFlowMode.PAYMENT_LINK -> "customer_selected_payment_link"
            PaymentFlowMode.YAPPY_ONSITE -> "customer_selected_yappy_onsite"
            PaymentFlowMode.DRAFT -> "customer_changed_payment_method"
        }
    }

    private fun hasActiveYappyOnsitePendingIntent(state: PosState): Boolean {
        val onsite = state.onsitePayment ?: return false
        val transactionId = onsite.transactionId.ifBlank {
            state.yappyOnsiteTransaction?.transaction?.transactionId.orEmpty()
        }
        if (transactionId.isBlank()) return false
        val status = (
            state.yappyOnsiteTransaction?.transaction?.status?.ifBlank { onsite.status }
                ?: onsite.status
            ).lowercase()
        return status !in setOf("cancelled", "canceled", "expired", "returned", "succeeded") &&
            state.invoiceStatus != InvoiceStatus.ISSUED &&
            state.invoiceStatus != InvoiceStatus.FAILED
    }

    private fun validateManualReplacementPayments(state: PosState): String? {
        val totalToCharge = amountToCharge()
        val paid = state.charged.values.sum() + state.installments.sumOf { it.amountCents }
        val requiresOtherDescription = state.charged.containsKey(99)
        val otherDescription = state.otherPaymentDescription.trim()
        return when {
            paid < totalToCharge -> "El pago manual debe cubrir el total de la orden."
            state.installments.any { it.dueDateIso.isBlank() } ->
                "Selecciona fecha de vencimiento para cada pago a crédito."
            requiresOtherDescription && otherDescription.length < 15 ->
                "Completa una descripción de al menos 15 caracteres para Otro."
            else -> null
        }
    }

    private fun manualReplacementPaymentSubmissions(state: PosState): List<OrderPaymentSubmission> {
        val paymentDate = currentPanamaDateTimeIsoWithOffset()
        val otherDescription = state.otherPaymentDescription.trim()
        return buildList {
            state.charged.entries
                .filter { it.value > 0L }
                .forEach { (methodCode, amountCents) ->
                    add(
                        OrderPaymentSubmission(
                            methodCode = methodCode,
                            amountCents = amountCents,
                            paymentDateIso = paymentDate,
                            otherDescription = if (methodCode == 99) otherDescription else null,
                        )
                    )
                }
            state.installments
                .filter { it.amountCents > 0L }
                .forEach { installment ->
                    add(
                        OrderPaymentSubmission(
                            methodCode = 11,
                            amountCents = installment.amountCents,
                            paymentDateIso = paymentDate,
                            dueDateIso = convertDateToIso8601(installment.dueDateIso),
                        )
                    )
                }
        }
    }

    private suspend fun completeManualPaymentMethodChange(
        response: RegisterManualPaymentsDataResponse,
        freshOrder: Order,
    ) {
        val invoiceStatus = InvoiceStatus.fromId(
            response.resolvedInvoiceStatus(
                freshOrderInvoiceStatus = freshOrder.invoiceStatus,
                freshOrderExternalInvoiceNumber = freshOrder.externalInvoiceNumber,
            )
        )
        val cufe = response.resolvedInvoiceCufe(freshOrder.externalInvoiceNumber)
        val stateBeforeCompletion = uiState.value
        val branchCode = stateBeforeCompletion.branches
            .getOrNull(stateBeforeCompletion.selectedBranchIndex)
            ?.branchCode
        val billingPoint = stateBeforeCompletion.billingPoints
            .getOrNull(stateBeforeCompletion.selectedBillingPointIndex)
            ?.billingPoint
        enqueueIssuedOrderTicketPrint(
            order = freshOrder,
            source = "pos_manual_replacement",
            providedTicket = response.resolvedTicketPayload(json),
            branchCode = branchCode,
            billingPoint = billingPoint,
            invoiceIssued = invoiceStatus == InvoiceStatus.ISSUED,
        )
        val pdf = if (invoiceStatus == InvoiceStatus.ISSUED && cufe != null) {
            withContext(Dispatchers.IO) {
                runCatching {
                    posService.getInvoiceDocsRaw(freshOrder.businessId, cufe).pdfBase64.orEmpty()
                }.getOrElse { error ->
                    if (error is CancellationException) throw error
                    ""
                }
            }
        } else {
            ""
        }

        updateState {
            copy(
                paymentFlowMode = PaymentFlowMode.MANUAL_OR_INSTALLMENTS,
                paymentLink = "",
                paymentLinkPolling = false,
                paymentLinkPaymentDetected = false,
                paymentLinkManualPanelVisible = false,
                paymentLinkManualErrorMessage = null,
                pendingPaymentChangeSourceReleased = false,
                pendingPaymentChangeOpen = false,
                pendingPaymentChangeCompleted = true,
                pendingPaymentChangeCancelDialogVisible = false,
                pendingPaymentChangeCancelErrorMessage = null,
                pendingPaymentChangeExitAction = null,
                onsitePayment = null,
                yappyOnsiteTransaction = null,
                yappyOnsitePolling = false,
                yappyOnsitePollingSuppressed = false,
                yappyOnsiteInvoiceProcessingTimedOut = false,
                orderNumber = freshOrder.internalNumber.ifBlank {
                    response.orderNumber.ifBlank { orderNumber }
                },
                invoiceStatus = invoiceStatus,
                pdfDocument = pdf.ifBlank { pdfDocument },
                postCreateInvoiceWarning = PostCreateInvoiceWarningState(),
            )
        }

        if (invoiceStatus == InvoiceStatus.ISSUED && pdf.isBlank()) {
            snackbarService.show("La factura fue generada, pero no fue posible descargar el PDF.")
        }
    }

    private fun paymentLinkSwitchErrorMessage(error: Throwable): String {
        val raw = error.message.orEmpty()
        return when {
            raw.contains("manual_refund_required", ignoreCase = true) ->
                "El proveedor ya confirmó el pago o el estado cambió. Requiere reembolso o conciliación manual."
            raw.contains("O_RP_005", ignoreCase = true) ->
                "No se puede cambiar el método por el estado actual del pago."
            raw.contains("O_RP_004", ignoreCase = true) ->
                "No se encontró una orden o enlace pendiente activo."
            raw.contains("O_RP_002", ignoreCase = true) ->
                "No se pudo liberar el cobro pendiente para cambiar el método de pago."
            else -> "No se pudo cambiar el método de pago."
        }
    }

    private suspend fun handlePaymentLinkInvoiceIssued(
        businessId: Int,
        freshOrder: Order,
    ) {
        val cufe = freshOrder.externalInvoiceNumber?.takeIf { it.isNotBlank() }
        val stateBeforeCompletion = uiState.value
        val branchCode = stateBeforeCompletion.branches
            .getOrNull(stateBeforeCompletion.selectedBranchIndex)
            ?.branchCode
        val billingPoint = stateBeforeCompletion.billingPoints
            .getOrNull(stateBeforeCompletion.selectedBillingPointIndex)
            ?.billingPoint
        enqueueIssuedOrderTicketPrint(
            order = freshOrder,
            source = "pos_payment_link",
            providedTicket = null,
            branchCode = branchCode,
            billingPoint = billingPoint,
        )
        val pdf = if (cufe != null) {
            withContext(Dispatchers.IO) {
                runCatching {
                    posService.getInvoiceDocsRaw(businessId, cufe).pdfBase64.orEmpty()
                }.getOrElse { error ->
                    if (error is CancellationException) throw error
                    ""
                }
            }
        } else {
            ""
        }

        updateState {
            copy(
                paymentLinkPaymentDetected = true,
                invoiceStatus = InvoiceStatus.ISSUED,
                orderNumber = freshOrder.internalNumber.ifBlank { orderNumber },
                pdfDocument = pdf.ifBlank { pdfDocument },
            )
        }

        if (pdf.isBlank()) {
            snackbarService.show("La factura fue generada, pero no fue posible descargar el PDF.")
        }
    }

    private fun enqueueIssuedOrderTicketPrint(
        order: Order,
        source: String,
        providedTicket: TicketDocumentPayload?,
        branchCode: String?,
        billingPoint: String?,
        invoiceIssued: Boolean = order.invoiceStatus == InvoiceStatus.ISSUED.id,
    ) {
        if (!invoiceIssued) return
        if (providedTicket == null && order.ticketEnabled != true) return
        if (uiState.value.paymentLinkInvoicePrintAttemptedOrderId == order.id) return
        if (branchCode.isNullOrBlank() || billingPoint.isNullOrBlank()) return
        if (printerService.resolveActivePrinter(branchCode, billingPoint) == null) return

        updateState { copy(paymentLinkInvoicePrintAttemptedOrderId = order.id) }
        launchProtectedTicketPrint {
            printIssuedOrderTicket(
                order = order,
                source = source,
                providedTicket = providedTicket,
                branchCode = branchCode,
                billingPoint = billingPoint,
                markAttempted = false,
                invoiceIssued = invoiceIssued,
            )
        }
    }

    private fun launchProtectedTicketPrint(block: suspend () -> Unit) {
        appScope.launch(Dispatchers.IO) {
            withContext(NonCancellable + Dispatchers.IO) {
                block()
            }
        }
    }

    private suspend fun printIssuedOrderTicket(
        order: Order,
        source: String,
        providedTicket: TicketDocumentPayload?,
        branchCode: String? = null,
        billingPoint: String? = null,
        markAttempted: Boolean = true,
        invoiceIssued: Boolean = order.invoiceStatus == InvoiceStatus.ISSUED.id,
    ) {
        if (!invoiceIssued) return
        if (providedTicket == null && order.ticketEnabled != true) return
        if (markAttempted && uiState.value.paymentLinkInvoicePrintAttemptedOrderId == order.id) return

        val state = uiState.value
        val resolvedBranchCode = branchCode
            ?: state.branches.getOrNull(state.selectedBranchIndex)?.branchCode
            ?: return
        val resolvedBillingPoint = billingPoint
            ?: state.billingPoints.getOrNull(state.selectedBillingPointIndex)?.billingPoint
            ?: return
        val printer = printerService.resolveActivePrinter(resolvedBranchCode, resolvedBillingPoint) ?: return

        if (markAttempted) {
            updateState { copy(paymentLinkInvoicePrintAttemptedOrderId = order.id) }
        }
        var stage = "ticket_payload"
        runCatching {
            val ticketPayload = providedTicket
                ?: fetchOrderTicketPayloadForPrint(order)
            stage = "print_ticket"
            printerService.printTicketPayload(
                printerConfig = printer,
                ticketPayload = ticketPayload,
                context = PrintContext(
                    source = source,
                    orderId = order.id,
                )
            )
        }.onFailure {
            loggerPrintFailure(order.internalNumber, it, stage)
            withContext(Dispatchers.Main) {
                snackbarService.show("El pago fue exitoso, pero la impresión del ticket falló.")
            }
        }
    }

    private suspend fun fetchOrderTicketPayloadForPrint(order: Order): TicketDocumentPayload {
        var lastCancellation: CancellationException? = null
        repeat(3) { attemptIndex ->
            try {
                return printerService.fetchOrderTicketLayout(orderId = order.id, businessId = order.businessId)
            } catch (error: CancellationException) {
                lastCancellation = error
                println(
                    "Ticket payload fetch cancelled for order ${order.internalNumber} " +
                        "attempt=${attemptIndex + 1}/3: ${error::class.simpleName}: ${error.message}"
                )
                delay(400L * (attemptIndex + 1))
            }
        }
        throw lastCancellation ?: CancellationException("Ticket payload fetch cancelled")
    }

    fun pollYappyOnsiteTransaction(markInvoiceProcessingTimedOut: Boolean = false) {
        val businessId = business?.businessId ?: return
        val state = uiState.value
        if (state.yappyOnsitePollingSuppressed || state.yappyOnsitePolling) return
        val transactionId = state.onsitePayment?.transactionId
            ?: state.yappyOnsiteTransaction?.transaction?.transactionId
            ?: return

        updateState { copy(yappyOnsitePolling = true) }
        val pollingJob = viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                paymentService.getYappyOnsiteTransaction(businessId, transactionId)
            }.onSuccess { payload ->
                withContext(Dispatchers.Main) {
                    if (uiState.value.yappyOnsitePollingSuppressed) {
                        updateState { copy(yappyOnsitePolling = false) }
                        return@withContext
                    }
                    updateState {
                        copy(
                            yappyOnsiteTransaction = payload,
                            yappyOnsitePolling = false,
                            orderNumber = payload.order.orderNumber.ifBlank { orderNumber },
                            invoiceStatus = InvoiceStatus.fromId(payload.invoice.status),
                            yappyOnsiteInvoiceProcessingTimedOut =
                                yappyOnsiteInvoiceProcessingTimedOut || markInvoiceProcessingTimedOut,
                        )
                    }
                }
                if (!uiState.value.yappyOnsitePollingSuppressed) {
                    maybePrintYappyOnsiteTicket(payload)
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    updateState { copy(yappyOnsitePolling = false) }
                }
            }
        }
        yappyOnsitePollingJob = pollingJob
        pollingJob.invokeOnCompletion {
            if (yappyOnsitePollingJob === pollingJob) {
                yappyOnsitePollingJob = null
            }
        }
    }

    private fun stopYappyOnsitePollingForRelease() {
        yappyOnsitePollingJob?.cancel()
        yappyOnsitePollingJob = null
        updateState {
            copy(
                yappyOnsitePolling = false,
                yappyOnsitePollingSuppressed = true,
            )
        }
    }

    fun requestYappyOnsiteQrExit(): Boolean {
        if (!hasActiveYappyOnsitePendingIntent(uiState.value)) return false
        updateState { copy(yappyOnsiteExitCancelDialogVisible = true) }
        return true
    }

    fun dismissYappyOnsiteQrExit() {
        updateState { copy(yappyOnsiteExitCancelDialogVisible = false) }
    }

    fun confirmYappyOnsiteQrExitCancellation(onCancelled: () -> Unit) {
        updateState { copy(yappyOnsiteExitCancelDialogVisible = false) }
        cancelYappyOnsiteTransaction(onCancelled = onCancelled)
    }

    fun cancelYappyOnsiteTransaction(onCancelled: (() -> Unit)? = null) {
        val businessId = business?.businessId ?: return
        val transactionId = uiState.value.onsitePayment?.transactionId
            ?: uiState.value.yappyOnsiteTransaction?.transaction?.transactionId
            ?: return

        stopYappyOnsitePollingForRelease()
        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                paymentService.cancelYappyOnsiteTransaction(
                    businessId = businessId,
                    transactionId = transactionId,
                    reason = "customer_changed_payment_method",
                )
            }.onSuccess { transaction ->
                withContext(Dispatchers.Main) {
                    updateState {
                        copy(
                            yappyOnsiteTransaction = yappyOnsiteTransaction?.copy(transaction = transaction),
                            onsitePayment = onsitePayment?.copy(
                                status = transaction.status,
                                providerStatus = transaction.providerStatus,
                            ),
                            yappyOnsiteExitCancelDialogVisible = false,
                        )
                    }
                    if (onCancelled == null) {
                        showSuccess()
                    } else {
                        hideLoading()
                        onCancelled()
                    }
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    updateState {
                        copy(
                            yappyOnsitePollingSuppressed = false,
                            yappyOnsiteExitCancelDialogVisible = false,
                        )
                    }
                    snackbarService.show("No fue posible cancelar el pago de Yappy.")
                    showError()
                }
            }
        }
    }

    fun cancelYappyOnsiteOrder() {
        val state = uiState.value
        val businessId = business?.businessId ?: return
        val transactionId = state.onsitePayment?.transactionId
            ?: state.yappyOnsiteTransaction?.transaction?.transactionId
            ?: return

        stopYappyOnsitePollingForRelease()
        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                paymentService.cancelYappyOnsiteTransaction(
                    businessId = businessId,
                    transactionId = transactionId,
                    reason = "yappy qr code cancelled",
                )
            }.onSuccess { transaction ->
                withContext(Dispatchers.Main) {
                    val orderCancelled = transaction.status.equals("cancelled", ignoreCase = true) ||
                        transaction.status.equals("canceled", ignoreCase = true) ||
                        transaction.status.equals("returned", ignoreCase = true)
                    if (orderCancelled) {
                        updateState {
                            copy(
                                onsitePayment = onsitePayment?.copy(
                                    status = "cancelled",
                                    providerStatus = transaction.providerStatus,
                                ),
                                yappyOnsiteTransaction = yappyOnsiteTransaction?.copy(
                                    transaction = transaction.copy(status = "cancelled"),
                                ),
                                yappyOnsiteExitCancelDialogVisible = false,
                                pendingPaymentChangeCancelDialogVisible = false,
                            )
                        }
                        showSuccess()
                    } else {
                        updateState { copy(yappyOnsitePollingSuppressed = false) }
                        snackbarService.show("No fue posible confirmar la cancelación de la orden.")
                        showError()
                    }
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    updateState { copy(yappyOnsitePollingSuppressed = false) }
                    snackbarService.show(paymentLinkSwitchErrorMessage(error))
                    showError()
                }
            }
        }
    }

    fun retryYappyOnsiteInvoice() {
        val businessId = business?.businessId ?: return
        val orderId = uiState.value.yappyOnsiteTransaction?.order?.id
            ?: uiState.value.onsitePayment?.orderId
            ?: return

        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                posService.retryElectronicInvoice(businessId, orderId)
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    showSuccess()
                    pollYappyOnsiteTransaction()
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    showError()
                    snackbarService.show("No fue posible reintentar la factura.")
                }
            }
        }
    }

    fun openYappyOnsiteInvoicePdf() {
        loadYappyOnsiteInvoicePdf { openPdfDocument() }
    }

    fun shareYappyOnsiteInvoicePdf() {
        loadYappyOnsiteInvoicePdf { sharePdfDocument() }
    }

    private fun loadYappyOnsiteInvoicePdf(onReady: () -> Unit) {
        if (uiState.value.pdfDocument.isNotBlank()) {
            onReady()
            return
        }

        val businessId = business?.businessId ?: return
        val invoice = uiState.value.yappyOnsiteTransaction?.invoice ?: return
        val cufe = invoice.cufe?.takeIf { it.isNotBlank() } ?: return

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                posService.getInvoiceDocsRaw(businessId, cufe)
            }.onSuccess { docs ->
                val pdf = docs.pdfBase64.orEmpty()
                if (pdf.isBlank()) return@onSuccess
                withContext(Dispatchers.Main) {
                    updateState { copy(pdfDocument = pdf) }
                    onReady()
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    snackbarService.show("No fue posible descargar la factura.")
                }
            }
        }
    }

    private fun maybePrintYappyOnsiteTicket(payload: com.teco.ventago.features.payments.domain.models.YappyOnsiteTransactionPayload) {
        if (payload.transaction.status.lowercase() != "succeeded") return
        if (payload.invoice.status != 2) return
        if (!payload.invoice.ticketEnabled) return
        val ticket = payload.invoice.ticket ?: return
        val transactionId = payload.transaction.transactionId
        if (uiState.value.yappyOnsitePrintAttemptedTransactionId == transactionId) return

        val state = uiState.value
        val branch = state.branches.getOrNull(state.selectedBranchIndex)?.branchCode ?: return
        val billingPoint = state.billingPoints.getOrNull(state.selectedBillingPointIndex)?.billingPoint ?: return
        val printer = printerService.resolveActivePrinter(branch, billingPoint) ?: return

        updateState { copy(yappyOnsitePrintAttemptedTransactionId = transactionId) }
        launchProtectedTicketPrint {
            runCatching {
                printerService.printTicketPayload(
                    printerConfig = printer,
                    ticketPayload = ticket,
                    context = PrintContext(
                        source = "pos_yappy_onsite",
                        orderId = payload.order.id
                    )
                )
            }.onFailure {
                loggerPrintFailure(payload.order.orderNumber, it)
                withContext(Dispatchers.Main) {
                    snackbarService.show("El pago fue exitoso, pero la impresión del ticket falló.")
                }
            }
        }
    }

    fun canReprintSuccessTicket(state: PosState = uiState.value): Boolean {
        val businessId = business?.businessId ?: return false
        val orderId = state.createdOrderId ?: return false
        return businessId > 0 &&
            orderId > 0 &&
            state.invoiceStatus == InvoiceStatus.ISSUED &&
            state.orderNumber.isNotBlank() &&
            !state.orderCreationFailed
    }

    fun reprintSuccessTicket() {
        val state = uiState.value
        if (state.successReprintInFlight || !canReprintSuccessTicket(state)) return

        val businessId = business?.businessId ?: return
        val orderId = state.createdOrderId ?: return
        val orderNumber = state.orderNumber
        val branchCode = state.branches.getOrNull(state.selectedBranchIndex)?.branchCode
        val billingPoint = state.billingPoints.getOrNull(state.selectedBillingPointIndex)?.billingPoint
        if (branchCode.isNullOrBlank() || billingPoint.isNullOrBlank()) return

        val printer = printerService.resolveActivePrinter(branchCode, billingPoint)
        if (printer == null) {
            viewModelScope.launch {
                snackbarService.show("No hay impresora activa disponible para reimprimir este ticket.")
            }
            return
        }

        showLoading()
        updateState { copy(successReprintInFlight = true) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val ticketPayload = printerService.fetchOrderTicketLayout(
                    orderId = orderId,
                    businessId = businessId
                )
                printerService.printTicketPayload(
                    printerConfig = printer,
                    ticketPayload = ticketPayload,
                    context = PrintContext(
                        source = "pos_success_reprint",
                        orderId = orderId
                    )
                )
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    hideLoading()
                    updateState { copy(successReprintInFlight = false) }
                    snackbarService.show("Ticket reenviado a la impresora correctamente.")
                }
            }.onFailure { error ->
                loggerPrintFailure(orderNumber, error, "success_reprint")
                withContext(Dispatchers.Main) {
                    hideLoading()
                    updateState { copy(successReprintInFlight = false) }
                    val detail = error.message?.takeIf { it.isNotBlank() }
                        ?: "Verifica la conexión de la impresora."
                    snackbarService.show("La reimpresión falló. $detail")
                }
            }
        }
    }

    fun setIncludeBottomNote(include: Boolean) {
        updateState {
            copy(
                includeBottomNote = if (
                    bottomNoteSettings != null &&
                    !bottomNoteRefreshFailed &&
                    bottomNoteSettings.title.isNotBlank() &&
                    bottomNoteSettings.body.isNotBlank()
                ) {
                    include
                } else {
                    null
                },
            )
        }
        saveOrderCreationCheckpoint(OrderCreationStep.CART)
    }

    private fun refreshBottomNoteSettingsInBackground() {
        viewModelScope.launch(Dispatchers.IO) {
            invoicingSettingsService.refreshBottomNoteSettings()
        }
    }

    private fun loggerPrintFailure(orderNumber: String, throwable: Throwable, stage: String? = null) {
        val stageText = stage?.let { " stage=$it" }.orEmpty()
        println(
            "Ticket print failed for order $orderNumber$stageText: " +
                "${throwable::class.simpleName}: ${throwable.message}"
        )
    }

    fun onBranchSelected(index: Int) {
        if (uiState.value.posProvisioningActive) return
        updateBranchSelection(index)
        persistCurrentBranchBillingPointSelection()
        saveOrderCreationCheckpoint(OrderCreationStep.CUSTOMER)
        refreshInventoryAvailability()
    }

    fun onBillingPointSelected(index: Int) {
        if (uiState.value.posProvisioningActive) return
        updateState { copy(selectedBillingPointIndex = index) }
        persistCurrentBranchBillingPointSelection()
        saveOrderCreationCheckpoint(OrderCreationStep.CUSTOMER)
        refreshInventoryAvailability()
    }

    private fun updateBranchSelection(
        index: Int,
        branches: List<BranchModel> = uiState.value.branches,
        resetBillingPoint: Boolean = true,
        forcedBillingPointIndex: Int? = null
    ) {
        if (branches.isEmpty()) return
        val safeIndex = index.coerceIn(0, branches.lastIndex)
        val selectedBranch = branches[safeIndex]
        val billingPoints = selectedBranch.fiscalBillingPoints
        val billingPointIndex = if (resetBillingPoint) {
            forcedBillingPointIndex?.coerceIn(0, (billingPoints.lastIndex).coerceAtLeast(0)) ?: 0
        } else {
            val forced = forcedBillingPointIndex
            if (forced != null && billingPoints.isNotEmpty()) {
                forced.coerceIn(0, billingPoints.lastIndex)
            } else {
                val currentIndex = uiState.value.selectedBillingPointIndex
                if (billingPoints.isEmpty()) 0 else currentIndex.coerceIn(0, billingPoints.lastIndex)
            }
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

    private fun resolvePersistedBranchBillingPointSelection(
        branches: List<BranchModel>
    ): Pair<Int, Int>? {
        val businessId = business?.businessId ?: return null
        val persisted = readPersistedBranchBillingPointSelection(businessId) ?: return null
        val branchIndex = branches.indexOfFirst { it.branchCode == persisted.branchCode }
        if (branchIndex < 0) return null
        val billingPoints = branches[branchIndex].fiscalBillingPoints
        val billingPointIndex = billingPoints.indexOfFirst { it.billingPoint == persisted.billingPoint }
        if (billingPointIndex < 0) return null
        return branchIndex to billingPointIndex
    }

    private fun applyPersistedBranchBillingPointSelectionIfPossible() {
        val state = uiState.value
        if (state.posProvisioningActive) return
        if (state.flowMode != FlowMode.SALE) return
        if (state.branches.isEmpty()) return
        val (branchIndex, billingPointIndex) = resolvePersistedBranchBillingPointSelection(state.branches) ?: return
        updateBranchSelection(
            index = branchIndex,
            branches = state.branches,
            resetBillingPoint = true,
            forcedBillingPointIndex = billingPointIndex
        )
    }

    private fun resolveProvisionedBranchBillingPointSelection(
        branches: List<BranchModel>
    ): Pair<Int, Int>? {
        val provisioning = posProvisioningService.currentState()
        if (!provisioning.required || !provisioning.isProvisioned) return null
        val branchCode = provisioning.fixedBranchCode?.takeIf { it.isNotBlank() } ?: return null
        val billingPointCode = provisioning.fixedBillingPointCode?.takeIf { it.isNotBlank() } ?: return null
        val branchIndex = branches.indexOfFirst { it.branchCode == branchCode }
        if (branchIndex < 0) return null
        val billingPoints = branches[branchIndex].fiscalBillingPoints
        val billingPointIndex = billingPoints.indexOfFirst { it.billingPoint == billingPointCode }
        if (billingPointIndex < 0) return null
        return branchIndex to billingPointIndex
    }

    private fun applyProvisionedBranchBillingPointSelectionIfPossible() {
        val state = uiState.value
        if (!state.posProvisioningActive) return
        if (state.flowMode != FlowMode.SALE) return
        if (state.branches.isEmpty()) return
        val (branchIndex, billingPointIndex) = resolveProvisionedBranchBillingPointSelection(state.branches) ?: return
        updateBranchSelection(
            index = branchIndex,
            branches = state.branches,
            resetBillingPoint = true,
            forcedBillingPointIndex = billingPointIndex
        )
    }

    private fun readPersistedBranchBillingPointSelection(businessId: Int): PersistedBranchBillingPoint? {
        val saved = localStorage.string(branchBillingPointKey(businessId)).orEmpty()
        if (saved.isBlank()) return null
        val parts = saved.split("|", limit = 2)
        if (parts.size != 2) return null
        val branchCode = parts[0].trim()
        val billingPoint = parts[1].trim()
        if (branchCode.isBlank() || billingPoint.isBlank()) return null
        return PersistedBranchBillingPoint(branchCode, billingPoint)
    }

    private fun persistCurrentBranchBillingPointSelection() {
        val state = uiState.value
        if (state.flowMode != FlowMode.SALE) return
        if (state.branches.isEmpty() || state.selectedBranchIndex !in state.branches.indices) return
        if (state.billingPoints.isEmpty() || state.selectedBillingPointIndex !in state.billingPoints.indices) return
        val businessId = business?.businessId ?: return
        val selectedBranchCode = state.branches[state.selectedBranchIndex].branchCode
        val selectedBillingPoint = state.billingPoints[state.selectedBillingPointIndex].billingPoint
        localStorage.set(
            branchBillingPointKey(businessId),
            "$selectedBranchCode|$selectedBillingPoint"
        )
    }

    private fun branchBillingPointKey(businessId: Int): String {
        return "$BRANCH_BILLING_POINT_KEY_PREFIX.$businessId"
    }

    private fun applyQuoteDefaultBranch() {
        val branches = uiState.value.branches
        if (branches.isEmpty()) return
        val defaultIndex = branches.indexOfFirst { it.branchCode == DEFAULT_QUOTE_BRANCH_CODE }
        val resolvedIndex = if (defaultIndex >= 0) defaultIndex else 0
        updateBranchSelection(resolvedIndex, branches)
    }

    fun docTypeOptions(): List<String> = PosDocumentTypeOptions.selectable.map { it.toString() }

    fun onDocTypeSelected(index: Int) {
        val key = PosDocumentTypeOptions.codeAt(index)
        updateState {
            copy(
                selectedDocTypeIndex = PosDocumentTypeOptions.indexOf(key).coerceAtLeast(0),
                selectedDocType = key,
                originalInvoiceNumber = if (key == "06") originalInvoiceNumber else "",
                originalInvoiceNumberError = null,
                originalInvoiceEmissionDateIso = if (key == "06") originalInvoiceEmissionDateIso else "",
                originalInvoiceEmissionDateError = null
            )
        }
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
        if (key in noteDocumentTypes()) {
            disableOrderCreationCheckpointForCurrentFlow(clearExisting = true)
        } else if (uiState.value.referencedNoteCUFE.isBlank() && uiState.value.flowMode == FlowMode.SALE) {
            orderCheckpointDisabledForCurrentFlow = false
            saveOrderCreationCheckpoint(OrderCreationStep.CUSTOMER)
        }
    }

    fun onOriginalInvoiceNumberChanged(value: String) {
        val normalized = value
        updateState {
            copy(
                originalInvoiceNumber = normalized,
                originalInvoiceNumberError = PosNoteValidators.validateOriginalInvoiceNumber(
                    selectedDocType = selectedDocType,
                    value = normalized
                )
            )
        }
    }

    fun onOriginalInvoiceEmissionDateSelected(isoDate: String) {
        updateState {
            copy(
                originalInvoiceEmissionDateIso = isoDate,
                originalInvoiceEmissionDateError = PosNoteValidators.validateOriginalInvoiceEmissionDate(
                    selectedDocType = selectedDocType,
                    value = isoDate
                )
            )
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
        saveOrderCreationCheckpoint(OrderCreationStep.CUSTOMER)
    }

    fun onInvoiceIssueDateSelected(isoDate: String) {
        updateState { copy(invoiceIssueDateIso = isoDate) }
        saveOrderCreationCheckpoint(OrderCreationStep.CUSTOMER)
    }

    // === Customer ===
    fun onPickCustomerClick() {
        // navigate to customer picker or open dialog
    }

    fun setFlowMode(mode: FlowMode, quoteId: Long? = null) {
        if (mode == FlowMode.QUOTE) {
            val allowed = if (quoteId == null) {
                canAction(ActionKey.QUOTES_CREATE)
            } else {
                canAction(ActionKey.QUOTES_UPDATE)
            }
            if (!allowed) {
                showError()
                return
            }
            disableOrderCreationCheckpointForCurrentFlow(clearExisting = true)
        } else if (quoteId == null && uiState.value.referencedNoteCUFE.isBlank()) {
            orderCheckpointDisabledForCurrentFlow = false
        }
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
        if (!canAction(ActionKey.ORDERS_OPEN_CREATE)) {
            showError()
            return
        }
        disableOrderCreationCheckpointForCurrentFlow(clearExisting = true)
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
                selectedCustomerFeCustomerType = null,
                finalName = if (isFinalCustomer) finalName else null,
                finalEmail = if (isFinalCustomer) finalEmail else null,
                finalPhone = if (isFinalCustomer) finalPhone else null,
                finalIdTypeIndex = if (isFinalCustomer) finalIdTypeIndex else 0,
                finalIdType = if (isFinalCustomer) resolvedFinalIdType else current.finalIdType,
                finalIdNumber = if (isFinalCustomer) {
                    normalizeFinalCustomerIdentificationNumber(resolvedFinalIdType, finalIdNumber)
                } else {
                    null
                },
                finalIdNumberError = if (isFinalCustomer) {
                    validateFinalCustomerIdentification(resolvedFinalIdType, finalIdNumber)
                } else {
                    null
                },
                finalCustomerCountryCode = if (isFinalCustomer) finalCountry else null,
                customerAddresses = emptyList(),
                selectedCustomerAddressId = null,
                customerAddressesLoading = false,
                globalDiscountMode = GlobalDiscountMode.NONE,
                globalDiscountPercent = 0,
                globalDiscountFixedCents = 0L,
                globalShippingCents = 0L,
                globalInsuranceCents = 0L,
                globalOtherChargesCents = 0L
            )
        }

        if (!isFinalCustomer) {
            quote.customerId?.let {
                hydrateSelectedCustomerTaxSettings(
                    customerId = it,
                    applyDefaultsOnlyWhenMissing = true,
                )
            }
            fetchCustomerAddresses()
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
                selectedCustomerFeCustomerType = null,
                finalName = if (isFinalCustomer) finalName else null,
                finalEmail = if (isFinalCustomer) finalEmail else null,
                finalPhone = if (isFinalCustomer) finalPhone else null,
                finalIdTypeIndex = if (isFinalCustomer) finalIdTypeIndex else 0,
                finalIdType = if (isFinalCustomer) resolvedFinalIdType else current.finalIdType,
                finalIdNumber = if (isFinalCustomer) {
                    normalizeFinalCustomerIdentificationNumber(resolvedFinalIdType, finalIdNumber)
                } else {
                    null
                },
                finalIdNumberError = if (isFinalCustomer) {
                    validateFinalCustomerIdentification(resolvedFinalIdType, finalIdNumber)
                } else {
                    null
                },
                finalCustomerCountryCode = if (isFinalCustomer) finalCountry else null,
                customerAddresses = emptyList(),
                selectedCustomerAddressId = null,
                customerAddressesLoading = false,
                globalDiscountMode = GlobalDiscountMode.NONE,
                globalDiscountPercent = 0,
                globalDiscountFixedCents = 0L,
                globalShippingCents = 0L,
                globalInsuranceCents = 0L,
                globalOtherChargesCents = 0L
            )
        }

        if (!isFinalCustomer) {
            quote.customerId?.let {
                hydrateSelectedCustomerTaxSettings(
                    customerId = it,
                    applyDefaultsOnlyWhenMissing = true,
                )
            }
            fetchCustomerAddresses()
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
            val quantity = normalizeQuantity(line.quantity ?: 1.0, minValue = 0.0001)
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
                tax = tax,
                costCents = product?.cost?.toLongCents(),
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
        check(canAction(ActionKey.QUOTES_CREATE)) { "No autorizado para crear cotizaciones." }
        val request = buildQuoteRequest().copy(quoteId = null)
        val quote = quotesService.createQuote(request)
        updateState { copy(lastQuoteId = quote.id, lastQuoteNumber = quote.displayNumberOrQuoteNumber) }
        clearOrderCreationCheckpoint()
        quote
    }

    suspend fun updateQuote(): Quote = withContext(Dispatchers.IO) {
        check(canAction(ActionKey.QUOTES_UPDATE)) { "No autorizado para editar cotizaciones." }
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
        clearOrderCreationCheckpoint()
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
            copy(
                finalCustomer = isFinal,
                customer = if (isFinal) null else customer,
                selectedCustomerFeCustomerType = if (isFinal) null else selectedCustomerFeCustomerType,
                customerAddresses = if (isFinal) emptyList() else customerAddresses,
                selectedCustomerAddressId = if (isFinal) null else selectedCustomerAddressId,
                customerAddressesLoading = false,
                finalEmailError = if (isFinal) validateFinalCustomerEmail(finalEmail) else null,
                finalIdNumberError = if (isFinal) {
                    validateFinalCustomerIdentification(finalIdType, finalIdNumber)
                } else {
                    null
                }
            )
        }
        if (!isFinal) {
            uiState.value.customer?.id?.let {
                hydrateSelectedCustomerTaxSettings(
                    customerId = it,
                    applyDefaultsOnlyWhenMissing = true,
                )
            }
            fetchCustomerAddresses()
        }
        saveOrderCreationCheckpoint(OrderCreationStep.CUSTOMER)
    }

    fun clearFinalCustomerInfo() {
        updateState {
            copy(
                finalName = null,
                finalEmail = null,
                finalEmailError = null,
                finalPhone = null,
                finalIdTypeIndex = 0,
                finalIdType = "cedula",
                finalIdNumber = null,
                finalIdNumberError = null,
                finalCustomerCountryCode = null
            )
        }
        saveOrderCreationCheckpoint(OrderCreationStep.CUSTOMER)
    }

    fun onFinalNameChanged(v: String) {
        updateState { copy(finalName = v) }
        saveOrderCreationCheckpoint(OrderCreationStep.CUSTOMER)
    }
    fun onFinalEmailChanged(v: String) {
        updateState {
            copy(
                finalEmail = v,
                finalEmailError = validateFinalCustomerEmail(v)
            )
        }
        saveOrderCreationCheckpoint(OrderCreationStep.CUSTOMER)
    }
    fun onFinalPhoneChanged(v: String) {
        updateState { copy(finalPhone = v) }
        saveOrderCreationCheckpoint(OrderCreationStep.CUSTOMER)
    }

    // TODO Convert to proper enum
    fun finalIdTypeDisplayNames(): List<String> = listOf(
        "Cédula",
        "Pasaporte",
        "Identificación Extranjera"
    )
    
    private fun finalIdTypeKeys(): List<String> = listOf("cedula", "passport", "foreing_taxid")

    private fun finalIdTypeRequiresCountry(type: String): Boolean {
        return type == "passport" || type == "foreing_taxid"
    }
    
    fun onFinalIdTypeSelected(idx: Int) {
        val types = finalIdTypeKeys()
        if (idx in types.indices) {
            val selectedType = types[idx]
            updateState {
                val normalizedIdNumber = normalizeFinalCustomerIdentificationNumber(selectedType, finalIdNumber)
                copy(
                    finalIdTypeIndex = idx,
                    finalIdType = selectedType,
                    finalIdNumber = normalizedIdNumber,
                    finalIdNumberError = validateFinalCustomerIdentification(selectedType, normalizedIdNumber),
                    finalCustomerCountryCode = if (finalIdTypeRequiresCountry(selectedType)) {
                        finalCustomerCountryCode ?: "CO"
                    } else {
                        null
                    }
                )
            }
            saveOrderCreationCheckpoint(OrderCreationStep.CUSTOMER)
        }
    }

    fun onFinalIdNumberChanged(v: String) {
        updateState {
            val normalizedIdNumber = normalizeFinalCustomerIdentificationNumber(finalIdType, v)
            copy(
                finalIdNumber = normalizedIdNumber,
                finalIdNumberError = validateFinalCustomerIdentification(finalIdType, normalizedIdNumber)
            )
        }
        saveOrderCreationCheckpoint(OrderCreationStep.CUSTOMER)
    }
    fun onFinalCustomerCountrySelected(code: String) {
        updateState { copy(finalCustomerCountryCode = code) }
        saveOrderCreationCheckpoint(OrderCreationStep.CUSTOMER)
    }

    fun validateFinalCustomerSelection(showFeedback: Boolean = true): Boolean {
        val state = uiState.value
        val normalizedEmail = state.finalEmail?.trim()?.takeIf { it.isNotEmpty() }
        val emailError = validateFinalCustomerEmail(normalizedEmail)
        val normalizedIdNumber = normalizeFinalCustomerIdentificationNumber(state.finalIdType, state.finalIdNumber)
        val error = validateFinalCustomerIdentification(state.finalIdType, normalizedIdNumber)

        if (
            normalizedEmail != state.finalEmail ||
            emailError != state.finalEmailError ||
            normalizedIdNumber != state.finalIdNumber ||
            error != state.finalIdNumberError
        ) {
            updateState {
                copy(
                    finalEmail = normalizedEmail,
                    finalEmailError = emailError,
                    finalIdNumber = normalizedIdNumber,
                    finalIdNumberError = error
                )
            }
        }

        if (emailError != null && showFeedback) {
            viewModelScope.launch {
                snackbarService.show(emailError)
            }
        }

        if (error != null && showFeedback) {
            viewModelScope.launch {
                snackbarService.show(error)
            }
        }

        return emailError == null && error == null
    }


    // ---------- Per-line extras ----------
    fun setLineShipping(lineId: String, cents: Long?) {
        updateState {
            copy(cart = cart.map {
                if (it.lineId == lineId) it.copy(
                    shippingCents = cents?.coerceAtLeast(
                        0L
                    )
                ) else it
            })
        }
        saveOrderCreationCheckpoint(OrderCreationStep.CART)
    }

    fun setLineInsurance(lineId: String, cents: Long?) {
        updateState {
            copy(cart = cart.map {
                if (it.lineId == lineId) it.copy(
                    insuranceCents = cents?.coerceAtLeast(
                        0L
                    )
                ) else it
            })
        }
        saveOrderCreationCheckpoint(OrderCreationStep.CART)
    }

    fun setLinePharma(lineId: String, batchNumber: String?, batchQty: Int?) {
        updateState {
            copy(cart = cart.map {
                if (it.lineId == lineId) it.copy(
                    pharmaBatchNumber = batchNumber?.takeIf { it.isNotBlank() },
                    pharmaBatchQty = batchQty?.coerceAtLeast(0)
                ) else it
            })
        }
        saveOrderCreationCheckpoint(OrderCreationStep.CART)
    }

    // ---------- Global discount / charges ----------
    fun applyGlobalSettings(
        mode: GlobalDiscountMode,
        discountValue: Long,           // if PERCENT: 0..100; if FIXED: cents; NONE: 0
        shippingCents: Long?,          // nullable; ignored if any item has shipping
        insuranceCents: Long?,         // nullable; ignored if any item has insurance
        otherCents: Long?
    ) {
        updateState {
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
        saveOrderCreationCheckpoint(OrderCreationStep.CART)
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
    private val retentionOptionsList = CustomerTaxRetentionCatalog.options

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

    private fun updateAdditionalInvoiceInfo(block: PosState.() -> PosState) {
        updateState(block)
        saveOrderCreationCheckpoint(OrderCreationStep.CART)
    }

    /* ---------- Logistics setters ---------- */
    fun onLogInfo(v: String) = updateAdditionalInvoiceInfo { copy(logisticsInfo = v) }
    fun onLogPlate(v: String) = updateAdditionalInvoiceInfo { copy(logisticsVehiclePlate = v) }
    fun onLogCarrierName(v: String) = updateAdditionalInvoiceInfo { copy(logisticsCarrierLegalName = v) }
    fun onLogCarrierRuc(v: String) = updateAdditionalInvoiceInfo { copy(logisticsCarrierRuc = v) }
    fun onLogCarrierDv(v: String) = updateAdditionalInvoiceInfo { copy(logisticsCarrierDv = v) }
    fun onLogCarrierType(idx: Int) = updateAdditionalInvoiceInfo { copy(logisticsCarrierTaxpayerTypeIndex = idx) }
    fun onLogBoxes(v: String) = updateAdditionalInvoiceInfo { copy(logisticsBoxesQty = v.filter { it.isDigit() }) }
    fun onLogWeightLb(v: String) = updateAdditionalInvoiceInfo {
        copy(logisticsTotalWeightLb = v.filter { it.isDigit() || it == '.' }.take(10))
    }

    /* ---------- Delivery setters ---------- */
    fun onDelName(v: String) = updateAdditionalInvoiceInfo { copy(deliveryReceiverLegalName = v) }
    fun onDelRuc(v: String) = updateAdditionalInvoiceInfo { copy(deliveryReceiverRuc = v) }
    fun onDelDv(v: String) = updateAdditionalInvoiceInfo { copy(deliveryReceiverDv = v) }
    fun onDelType(idx: Int) = updateAdditionalInvoiceInfo { copy(deliveryReceiverTaxpayerTypeIndex = idx) }
    fun onDelPhone(v: String) = updateAdditionalInvoiceInfo { copy(deliveryContactPhone = v) }
    fun onDelAltPhone(v: String) = updateAdditionalInvoiceInfo { copy(deliveryAltContactPhone = v) }
    fun onCustomerAddressSelected(addressId: Long) = updateAdditionalInvoiceInfo {
        copy(selectedCustomerAddressId = addressId)
    }

    fun onProvinceSelected(idx: Int) = updateAdditionalInvoiceInfo {
        copy(
            deliveryProvinceIndex = idx,
            deliveryDistrictIndex = 0,
            deliveryCorregIndex = 0
        )
    }

    fun onDistrictSelected(idx: Int) = updateAdditionalInvoiceInfo {
        copy(deliveryDistrictIndex = idx, deliveryCorregIndex = 0)
    }

    fun onCorregSelected(idx: Int) = updateAdditionalInvoiceInfo { copy(deliveryCorregIndex = idx) }

    /* ---------- Retention setters ---------- */
    fun onRetentionSelected(idx: Int) = updateAdditionalInvoiceInfo {
        val option = retentionOptionsList.getOrNull(idx) ?: retentionOptionsList.first()
        val nextAmount = when {
            option.code.isEmpty() -> ""
            option.defaultRate != null -> option.defaultRate.toString()
            option.code == "8" && retentionCodeAt(retentionCodeIndex) == "8" -> retentionAmount
            else -> ""
        }
        copy(retentionCodeIndex = idx, retentionAmount = nextAmount)
    }
    fun onRetentionAmount(v: String) = updateAdditionalInvoiceInfo {
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

    private fun applySelectedCustomerTaxDefaults(customer: CustomerListItem) {
        val normalizedCode = CustomerTaxRetentionCatalog.normalizeCode(customer.taxRetentionCode)
        updateState {
            copy(
                taxExempt = customer.taxExempt,
                retentionCodeIndex = CustomerTaxRetentionCatalog.indexOfCode(normalizedCode),
                retentionAmount = if (normalizedCode == "8") {
                    customer.taxRetentionPercent?.toString().orEmpty()
                } else {
                    ""
                },
            )
        }
    }

    private fun clearSelectedCustomerTaxDefaults() {
        updateState {
            copy(
                taxExempt = false,
                retentionCodeIndex = 0,
                retentionAmount = "",
            )
        }
    }

    private fun hasConfiguredTaxSettings(state: PosState = uiState.value): Boolean {
        val retentionCode = CustomerTaxRetentionCatalog.normalizeCode(retentionCodeAt(state.retentionCodeIndex))
        return state.taxExempt || retentionCode.isNotEmpty() || state.retentionAmount.isNotBlank()
    }

    private fun hydrateSelectedCustomerTaxSettings(
        customerId: Long,
        applyDefaultsOnlyWhenMissing: Boolean,
    ) {
        val businessId = business?.businessId ?: return
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    customerService.getCustomerById(businessId = businessId, customerId = customerId)
                }
            }.onSuccess { details ->
                applyHydratedCustomerTaxSettings(
                    customerId = customerId,
                    details = details,
                    applyDefaultsOnlyWhenMissing = applyDefaultsOnlyWhenMissing,
                )
            }
        }
    }

    private fun applyHydratedCustomerTaxSettings(
        customerId: Long,
        details: CustomerDetails,
        applyDefaultsOnlyWhenMissing: Boolean,
    ) {
        val currentCustomer = uiState.value.customer ?: return
        if (currentCustomer.id != customerId) return

        val hydratedCustomer = currentCustomer.copy(
            name = details.legalName?.takeIf { it.isNotBlank() } ?: currentCustomer.name,
            email = details.email ?: currentCustomer.email,
            ruc = details.rucNumber ?: currentCustomer.ruc,
            taxExempt = details.taxExempt,
            taxRetentionCode = details.taxRetentionCode,
            taxRetentionPercent = details.taxRetentionPercent,
        )

        val shouldApplyDefaults = !applyDefaultsOnlyWhenMissing || !hasConfiguredTaxSettings()
        updateState {
            copy(
                customer = hydratedCustomer,
                selectedCustomerFeCustomerType = details.feCustomerType,
            )
        }
        if (shouldApplyDefaults) {
            applySelectedCustomerTaxDefaults(hydratedCustomer)
        }
    }

    /* ---------- Exportation setters ---------- */
    fun onIncoterm(v: String) = updateAdditionalInvoiceInfo { copy(exportIncoterm = v.uppercase()) }
    fun onExportCurrency(v: String) = updateAdditionalInvoiceInfo { copy(exportCurrency = v.uppercase()) }
    fun onPortOfLoading(v: String) = updateAdditionalInvoiceInfo { copy(exportPortOfLoading = v) }


    // ---------- Totals helpers ----------
    /** Legal invoice total (goes to PAC): NO tips. */
    fun legalInvoiceTotal(): Long = CartCalc.summarize(uiState.value).totalBeforeTip

    /** Tips are no longer handled by the app. Kept as zero to ignore legacy restored state. */
    fun tipsTotal(): Long = 0L

    /** Amount to charge = legal invoice total only. */
    fun amountToCharge(): Long = legalInvoiceTotal().coerceAtLeast(0L)

    // ---------- Payment flow ----------
    fun setPaymentFlow(mode: PaymentFlowMode) {
        val currentMode = uiState.value.paymentFlowMode
        if (mode == currentMode) return
        if (mode == PaymentFlowMode.MANUAL_OR_INSTALLMENTS && !uiState.value.canUseManualPaymentMethods) return
        if (mode == PaymentFlowMode.PAYMENT_LINK && !uiState.value.canCreatePaymentLink) return
        if (mode == PaymentFlowMode.DRAFT && !uiState.value.canCreateDraft) return
        if (mode == PaymentFlowMode.YAPPY_ONSITE && !selectedBillingPointHasYappyOnsiteDevice()) {
            viewModelScope.launch {
                snackbarService.show("Configura un dispositivo Yappy en caja para esta sucursal y punto de facturación.")
            }
            return
        }

        analyticsService.logOrderCreationPaymentOptionSelected(
            mode = when (mode) {
                PaymentFlowMode.MANUAL_OR_INSTALLMENTS -> "MANUAL"
                PaymentFlowMode.PAYMENT_LINK -> "LINK"
                PaymentFlowMode.YAPPY_ONSITE -> "YAPPY_ONSITE"
                PaymentFlowMode.DRAFT -> "DRAFT"
            }
        )

        updateState {
            if (mode == PaymentFlowMode.PAYMENT_LINK || mode == PaymentFlowMode.YAPPY_ONSITE || mode == PaymentFlowMode.DRAFT) {
                business?.businessId?.let { businessId ->
                    localStorage.set(paymentLinkBadgeSeenKey(businessId), true)
                }
                copy(
                    paymentFlowMode = mode,
                    showPaymentLinkNewBadge = false,
                    charged = emptyMap(),
                    installments = emptyList()
                )
            } else {
                copy(paymentFlowMode = mode)
            }
        }
    }

    fun markPaymentLinkBadgeSeen() {
        val businessId = business?.businessId ?: return
        localStorage.set(paymentLinkBadgeSeenKey(businessId), true)
        updateState { copy(showPaymentLinkNewBadge = false) }
    }

    fun checkPaymentMethodsConfigured(onResult: (Boolean) -> Unit) {
        val configured = if (financialProfileService.hasLoadedProfile()) {
            financialProfileService.paymentLinkMethodsConfigured()
        } else {
            uiState.value.paymentLinkConfigured
        }
        updateState { copy(paymentLinkConfigured = configured) }
        onResult(configured)
    }

    fun selectedBillingPointHasYappyOnsiteDevice(): Boolean {
        val state = uiState.value
        val branch = state.branches.getOrNull(state.selectedBranchIndex)?.branchCode ?: return false
        val billingPoint = state.billingPoints.getOrNull(state.selectedBillingPointIndex)?.billingPoint ?: return false
        return state.yappyOnsiteConfigured &&
            state.yappyOnsiteDevices.any {
                it.branchCode == branch && it.billingPoint == billingPoint
            }
    }

    fun savePaymentLinkCheckpointForResume() {
        saveOrderCreationCheckpoint(OrderCreationStep.PAYMENT)
    }

    fun onPaymentScreenVisible() {
        business?.businessId?.let { businessId ->
            refreshPaymentConfigInBackground(
                businessId = businessId,
                force = !financialProfileService.hasLoadedProfile()
            )
            loadYappyOnsiteDevices()
        }
        saveOrderCreationCheckpoint(OrderCreationStep.PAYMENT)
    }

    fun warmYappyOnsiteAvailabilityForNewOrder() {
        if (uiState.value.flowMode != FlowMode.SALE) return
        val businessId = business?.businessId ?: return
        loadCachedYappyOnsiteDevices(businessId)
        refreshPaymentConfigInBackground(
            businessId = businessId,
            force = !financialProfileService.hasLoadedProfile()
        )
        if (financialProfileService.yappyOnsiteConfigured() || uiState.value.yappyOnsiteConfigured) {
            loadYappyOnsiteDevices(forceRefresh = true)
        }
    }

    private fun loadYappyOnsiteDevices(forceRefresh: Boolean = false) {
        val businessId = business?.businessId ?: return
        val current = uiState.value
        if (!current.yappyOnsiteConfigured) {
            updateState { copy(yappyOnsiteDevices = emptyList(), yappyOnsiteDevicesResolved = true) }
            return
        }
        if (!forceRefresh && current.yappyOnsiteDevicesResolved) return
        if (yappyOnsiteDevicesJob?.isActive == true) return

        if (current.yappyOnsiteDevices.isEmpty()) {
            updateState { copy(yappyOnsiteDevicesResolved = false) }
        }
        yappyOnsiteDevicesJob = viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val groups = paymentService.listYappyOnsiteGroups(businessId)
                paymentService.listAllYappyOnsiteDevices(businessId, groups)
            }.onSuccess { devices ->
                saveYappyOnsiteDevicesCache(businessId, devices)
                withContext(Dispatchers.Main) {
                    updateState {
                        copy(
                            yappyOnsiteDevices = devices,
                            yappyOnsiteDevicesResolved = true
                        )
                    }
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    updateState {
                        if (yappyOnsiteDevices.isEmpty()) {
                            copy(
                                yappyOnsiteDevices = emptyList(),
                                yappyOnsiteDevicesResolved = true
                            )
                        } else {
                            copy(yappyOnsiteDevicesResolved = true)
                        }
                    }
                }
            }
        }
    }

    private fun loadCachedYappyOnsiteDevices(businessId: Int) {
        val raw = localStorage.string(yappyOnsiteDevicesCacheKey(businessId)).orEmpty()
        if (raw.isBlank()) return
        val cachedDevices = runCatching {
            json.decodeFromString<List<YappyOnsiteDevice>>(raw)
        }.getOrNull() ?: return
        updateState {
            copy(
                yappyOnsiteDevices = cachedDevices,
                yappyOnsiteDevicesResolved = true
            )
        }
    }

    private fun saveYappyOnsiteDevicesCache(
        businessId: Int,
        devices: List<YappyOnsiteDevice>
    ) {
        runCatching {
            localStorage.set(
                yappyOnsiteDevicesCacheKey(businessId),
                json.encodeToString(devices)
            )
        }
    }

    private fun yappyOnsiteDevicesCacheKey(businessId: Int): String {
        return "$YAPPY_ONSITE_DEVICES_CACHE_KEY_PREFIX.$businessId"
    }

    private fun refreshPaymentConfigInBackground(
        businessId: Int,
        force: Boolean = false
    ) {
        val nowEpochSeconds = Clock.System.now().epochSeconds
        val isThrottled = !force &&
            (nowEpochSeconds - lastPaymentConfigRefreshAtEpochSeconds) < PAYMENT_CONFIG_REFRESH_THROTTLE_SECONDS
        if (isThrottled || paymentConfigRefreshJob?.isActive == true) return

        paymentConfigRefreshJob = viewModelScope.launch {
            lastPaymentConfigRefreshAtEpochSeconds = Clock.System.now().epochSeconds
            withContext(Dispatchers.IO) {
                financialProfileService.refresh(businessId)
            }
            updateState {
                copy(
                    paymentsConfigured = financialProfileService.paymentsConfigured(),
                    paymentsOnboardingCompleted = financialProfileService.paymentsOnboardingCompleted(),
                    paymentProfileResolved = financialProfileService.hasLoadedProfile(),
                    paymentLinkConfigured = financialProfileService.paymentLinkMethodsConfigured(),
                    yappyOnsiteConfigured = financialProfileService.yappyOnsiteConfigured(),
                    yappyOnsiteDevicesResolved = if (financialProfileService.yappyOnsiteConfigured()) {
                        yappyOnsiteDevicesResolved
                    } else {
                        true
                    }
                )
            }
        }
    }

    // ---------- Manual payments ----------
    /** Label shown to users for DGI codes. */
    fun manualMethodOptions(): List<Pair<Int, String>> = listOf(
        8 to "Transferencia bancaria",
        3 to "Tarjeta crédito",
        4 to "Tarjeta débito",
        2 to "Efectivo",
        5 to "Tarjeta fidelización",
        6 to "Vale",
        7 to "Tarjeta de regalo",
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
        val totalToCharge = amountToCharge()
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
    fun addInstallment(dueDateIso: String = "", amountCents: Long? = null) {
        val remainingForInstallment = remainingToAllocate()
        updateState {
            if (paymentFlowMode == PaymentFlowMode.PAYMENT_LINK) this
            else {
                copy(installments = installments + InstallmentUI(
                    amountCents = (amountCents ?: remainingForInstallment).coerceIn(
                        0L,
                        remainingForInstallment.coerceAtLeast(0L)
                    ),
                    dueDateIso = dueDateIso
                ))
            }
        }
    }

    fun removeInstallment(index: Int) = updateState {
        if (index !in installments.indices) this
        else copy(installments = installments.toMutableList().also { it.removeAt(index) })
    }

    fun setInstallmentAmount(index: Int, cents: Long) {
        val totalToCharge = amountToCharge()
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

    private fun refreshPaymentLinkBadge(businessId: Int) {
        val seen = localStorage.bool(paymentLinkBadgeSeenKey(businessId)) == true
        updateState { copy(showPaymentLinkNewBadge = !seen) }
    }

    private fun paymentLinkBadgeSeenKey(businessId: Int): String {
        return "$PAYMENT_LINK_BADGE_SEEN_KEY_PREFIX:$businessId"
    }

    /** Remaining invoice total minus manual payments and installments. */
    fun remainingToAllocate(): Long {
        val total = amountToCharge()
        val allocated = manualPaidSum() + installmentsSum()
        return (total - allocated).coerceAtLeast(0L)
    }

    /**
     * Converts a UTC timestamp to Panama timezone format.
     * Example: "2026-01-27T01:56:04.304173Z" -> "2026-01-26T15:57:56"
     */
    private fun convertUtcToPanamaTimezone(utcTimestamp: String): String {
        return try {
            if (utcTimestamp.isBlank()) return utcTimestamp

            // Parse the UTC timestamp to Instant
            val instant = Instant.parse(utcTimestamp)

            // Convert to Panama timezone (America/Panama is UTC-5)
            val panamaZone = TimeZone.of("America/Panama")
            val panamaDateTime = instant.toLocalDateTime(panamaZone)

            // Format as "YYYY-MM-DDTHH:mm:ss" (without timezone suffix)
            val yearStr = panamaDateTime.year.toString()
            val monthStr = panamaDateTime.monthNumber.toString().padStart(2, '0')
            val dayStr = panamaDateTime.dayOfMonth.toString().padStart(2, '0')
            val hourStr = panamaDateTime.hour.toString().padStart(2, '0')
            val minuteStr = panamaDateTime.minute.toString().padStart(2, '0')
            val secondStr = panamaDateTime.second.toString().padStart(2, '0')

            "$yearStr-$monthStr-${dayStr}T$hourStr:$minuteStr:$secondStr"
        } catch (e: Exception) {
            println("Error converting UTC to Panama timezone: ${e.message}")
            // Return original timestamp if parsing fails
            utcTimestamp
        }
    }

    private fun resolveInvoiceIssuedDatetime(invoiceIssueDateIso: String): String {
        val panamaNow = Clock.System.now().toLocalDateTime(TimeZone.of("America/Panama"))
        val selectedDate = parseIsoLocalDate(invoiceIssueDateIso) ?: panamaNow.date

        return if (selectedDate == panamaNow.date) {
            formatLocalDateTimeSeconds(panamaNow)
        } else {
            "${formatLocalDate(selectedDate)}T00:00:00"
        }
    }

    private fun currentPanamaDateIsoString(): String {
        return formatLocalDate(Clock.System.now().toLocalDateTime(TimeZone.of("America/Panama")).date)
    }

    private fun currentPanamaDateTimeIsoWithOffset(): String {
        val dateTime = Clock.System.now().toLocalDateTime(TimeZone.of("America/Panama"))
        return "${formatLocalDate(dateTime.date)}T${formatLocalTime(dateTime.time)}-05:00"
    }

    private fun parseIsoLocalDate(value: String): LocalDate? {
        return try {
            if (value.length < 10) return null
            LocalDate(
                value.substring(0, 4).toInt(),
                value.substring(5, 7).toInt(),
                value.substring(8, 10).toInt()
            )
        } catch (_: Throwable) {
            null
        }
    }

    private fun formatLocalDate(date: LocalDate): String {
        val yearStr = date.year.toString().padStart(4, '0')
        val monthStr = date.monthNumber.toString().padStart(2, '0')
        val dayStr = date.dayOfMonth.toString().padStart(2, '0')
        return "$yearStr-$monthStr-$dayStr"
    }

    private fun formatLocalTime(time: LocalTime): String {
        val hourStr = time.hour.toString().padStart(2, '0')
        val minuteStr = time.minute.toString().padStart(2, '0')
        val secondStr = time.second.toString().padStart(2, '0')
        return "$hourStr:$minuteStr:$secondStr"
    }

    private fun formatLocalDateTimeSeconds(dateTime: LocalDateTime): String {
        val dateStr = formatLocalDate(dateTime.date)
        val hourStr = dateTime.hour.toString().padStart(2, '0')
        val minuteStr = dateTime.minute.toString().padStart(2, '0')
        val secondStr = dateTime.second.toString().padStart(2, '0')
        return "${dateStr}T$hourStr:$minuteStr:$secondStr"
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

    fun sharePdfDocument() {
        val state = uiState.value
        if (state.pdfDocument.isBlank()) return
        val pdfB64 = state.pdfDocument

        @OptIn(ExperimentalEncodingApi::class)
        val pdfBytes = Base64.decode(pdfB64)
        val filename = "${state.orderNumber}.pdf"
        pdfSharer.sharePdf(filename, pdfBytes)
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

internal fun normalizeFinalCustomerIdentificationNumber(finalIdType: String, finalIdNumber: String?): String? {
    val trimmedIdNumber = finalIdNumber?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return if (finalIdType == "cedula") {
        normalizePanamaCedula(trimmedIdNumber)
    } else {
        trimmedIdNumber
    }
}

internal fun validateFinalCustomerIdentification(finalIdType: String, finalIdNumber: String?): String? {
    val normalizedIdNumber = normalizeFinalCustomerIdentificationNumber(finalIdType, finalIdNumber)
    if (finalIdType != "cedula" || normalizedIdNumber.isNullOrBlank()) {
        return null
    }
    return if (isValidPanamaCedula(normalizedIdNumber)) {
        null
    } else {
        INVALID_FINAL_CUSTOMER_CEDULA_MESSAGE
    }
}

internal fun validateFinalCustomerEmail(finalEmail: String?): String? {
    val normalizedEmail = finalEmail?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return if (emailRegex.matches(normalizedEmail)) null else INVALID_FINAL_CUSTOMER_EMAIL_MESSAGE
}
