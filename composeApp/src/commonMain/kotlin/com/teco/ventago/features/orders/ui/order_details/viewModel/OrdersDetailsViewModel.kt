package com.teco.ventago.features.orders.ui.order_details.viewModel

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.core.PdfSharer
import com.teco.ventago.core.SnackbarService
import com.teco.ventago.core.authz.ActionKey
import com.teco.ventago.core.authz.AuthzEvaluator
import com.teco.ventago.core.authz.RouteKey
import com.teco.ventago.core.beta.BetaFeature
import com.teco.ventago.core.beta.BetaService
import com.teco.ventago.core.firebase.AnalyticsService
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.branches.domain.BranchService
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.business.domain.model.Business
import com.teco.ventago.features.financialProfile.domain.FinancialProfileService
import com.teco.ventago.features.invoicing.domain.models.InvoiceStatus
import com.teco.ventago.features.orders.domain.OrderService
import com.teco.ventago.features.orders.domain.OrderPaymentSubmission
import com.teco.ventago.features.orders.domain.OrderReceivableResolver
import com.teco.ventago.features.orders.domain.PaymentLinkResolver
import com.teco.ventago.features.orders.domain.PaymentAllocation
import com.teco.ventago.features.orders.domain.ReceivableApplicationAllocation
import com.teco.ventago.features.orders.domain.ReceivableRescheduleTerm
import com.teco.ventago.features.orders.domain.models.AchPaymentDetail
import com.teco.ventago.features.orders.domain.models.ManualPaymentMethodOption
import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.orders.domain.models.OrderPaymentDto
import com.teco.ventago.features.orders.domain.models.PaymentStatus
import com.teco.ventago.features.orders.domain.models.ReceivableTermDto
import com.teco.ventago.features.orders.domain.models.OrderStatus
import com.teco.ventago.features.orders.domain.models.requests.RetryInvoiceResponse
import com.teco.ventago.features.payments.domain.PaymentService
import com.teco.ventago.features.printers.domain.PrinterService
import com.teco.ventago.utils.doubleTryParse
import com.teco.ventago.utils.toDecimalString
import com.teco.ventago.utils.toLongCents
import com.teco.ventago.viewModels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.delivery
import ventago.composeapp.generated.resources.invoice_i_subtotal
import ventago.composeapp.generated.resources.manual_transference
import ventago.composeapp.generated.resources.paypal
import ventago.composeapp.generated.resources.pos_card
import ventago.composeapp.generated.resources.pos_cash
import ventago.composeapp.generated.resources.pos_discount
import ventago.composeapp.generated.resources.pos_mixed
import ventago.composeapp.generated.resources.pos_none
import ventago.composeapp.generated.resources.pos_other
import ventago.composeapp.generated.resources.pos_tips
import ventago.composeapp.generated.resources.yappy
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class OrdersDetailsViewModel(
    private val orderService: OrderService,
    private val paymentService: PaymentService,
    private val branchService: BranchService,
    private val businessService: BusinessService,
    private val financialProfileService: FinancialProfileService,
    private val authService: IAuthService,
    private val betaService: BetaService,
    private val pdfSharer: PdfSharer,
    private val printerService: PrinterService,
    private val snackbarService: SnackbarService,
    private val analyticsService: AnalyticsService,
) : BaseViewModel<OrderDetailsState, OrderDetailsUiEvent>(OrderDetailsState()) {

    var business: Business? = null
    private val achDetailsInFlight = mutableSetOf<String>()
    private val achProofBinaryCache = mutableMapOf<String, AchProofBinary>()
    private val hydratedOrderDetails = mutableSetOf<Int>()
    private var replacementYappyPollingJob: Job? = null

    private data class AchProofBinary(
        val bytes: ByteArray,
        val contentType: String?,
        val fileName: String?
    )

    private fun betaSnapshot(): Set<BetaFeature> {
        return betaService.features().value?.features.orEmpty()
            .mapNotNull(BetaFeature::fromKey)
            .toSet()
    }

    init {
        viewModelScope.launch {
            authService.getUser().collect { user ->
                val beta = betaSnapshot()
                updateState {
                    copy(
                        canMarkPaid = AuthzEvaluator.canAction(
                            ActionKey.ORDERS_MARK_PAID,
                            user,
                            beta
                        ),
                        canCreatePaymentLink = AuthzEvaluator.canAction(
                            ActionKey.ORDERS_PAYMENT_LINK,
                            user,
                            beta
                        ),
                        canViewAchPayment = AuthzEvaluator.canRoute(
                            RouteKey.ACH_PAYMENT_DETAILS,
                            user,
                            beta
                        ),
                        canApproveAchPayment = AuthzEvaluator.canAction(
                            ActionKey.ACH_PAYMENT_APPROVE,
                            user,
                            beta
                        ),
                        canRejectAchPayment = AuthzEvaluator.canAction(
                            ActionKey.ACH_PAYMENT_REJECT,
                            user,
                            beta
                        ),
                    )
                }
            }
        }
        viewModelScope.launch {
            financialProfileService.observe().onEach { profile ->
                profile?.let {
                    updateState {
                        copy(
                            havePaymentsConfigured = financialProfileService.paymentsConfigured(),
                            invoicingEnabled = financialProfileService.invoicingEnabled()
                        )
                    }
                }
            }.launchIn(this)

            businessService.getBusiness().onEach { businessData ->
                businessData?.let {
                    business = businessData

                }
            }.launchIn(this)

            branchService.observe().onEach { branches ->
                updateState {
                    copy(branches = branches)
                }
            }.launchIn(this)

            orderService.selectedOrder.onEach { order ->
                updateState {
                    copy(
                        order = order,
                    )
                }
            }.launchIn(this)
        }
    }


    fun showShareSheet(show: Boolean) {
        updateState {
            copy(showShareSheet = show)
        }
    }

    fun hydrateSelectedOrderDetailsIfNeeded() {
        val selectedOrder = uiState.value.order ?: return
        if (selectedOrder.relatedDocuments.isNotEmpty()) return
        if (!hydratedOrderDetails.add(selectedOrder.id)) return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    orderService.refreshOrder(
                        businessId = selectedOrder.businessId,
                        orderId = selectedOrder.id
                    )
                }.onFailure {
                    hydratedOrderDetails.remove(selectedOrder.id)
                }
            }
        }
    }

    fun getOrderDetailsMessage(): String {
        return orderService.buildOrderShareMessage(uiState.value.order!!, business!!)
    }

    @Composable
    fun getSummaryFromOrder(): List<Pair<String, String>> {
        val order = uiState.value.order!!

        val data: ArrayList<Pair<String, String>> = ArrayList()

        data.add(Pair("Subtotal:", order.subtotal))

        if (order.discountTotal.isNotEmpty() && order.discountTotal.doubleTryParse() > 0.00) {
            data.add(Pair("Descuentos:", order.discountTotal))
        }

        data.add(Pair("Impuestos:", order.taxTotal))

        if (order.tipsTotal.isNotEmpty() && order.tipsTotal.doubleTryParse() > 0.00) {
            data.add(Pair("Propinas:", order.tipsTotal))
        }

        order.acarreoTotal?.let { acarreo ->
            if (acarreo.isNotEmpty() && acarreo.doubleTryParse() > 0.00) {
                data.add(Pair("Acarreo:", acarreo))
            }
        }

        order.insuranceTotal?.let { insurance ->
            if (insurance.isNotEmpty() && insurance.doubleTryParse() > 0.00) {
                data.add(Pair("Seguro:", insurance))
            }
        }

        order.otherChargesTotal?.let { otherCharges ->
            if (otherCharges.isNotEmpty() && otherCharges.doubleTryParse() > 0.00) {
                data.add(Pair("Otros cargos:", otherCharges))
            }
        }

        return data
    }

    fun getOrderPaymentLink() {
        val order = uiState.value.order ?: return
        val resolved = PaymentLinkResolver.resolveCurrent(order) ?: return
        if (resolved.isTerminal) return
        analyticsService.logOrderPaymentLinkAction(actionValue = "open_modal")
        updateState {
            copy(
                paymentLink = resolved.url,
                showPaymentLinkSheet = true
            )
        }
    }

    fun openGeneratePaymentLinkSheet() {
        if (!uiState.value.canCreatePaymentLink) return
        val order = uiState.value.order ?: return
        val canGenerate = canGeneratePaymentLink(order)
        if (!canGenerate) return
        val pendingAmountInput = centsToAmountInput(totalOpenReceivableCents(order))
        analyticsService.logOrderPaymentLinkAction(actionValue = "open_modal")
        updateState {
            copy(
                generatePaymentLinkState = generatePaymentLinkState.copy(
                    showSheet = true,
                    amountInput = pendingAmountInput,
                    errorMessage = null
                )
            )
        }
    }

    fun closeGeneratePaymentLinkSheet() {
        updateState {
            copy(
                generatePaymentLinkState = generatePaymentLinkState.copy(
                    showSheet = false,
                    errorMessage = null
                )
            )
        }
    }

    fun updateGeneratePaymentAmountInput(value: String) {
        updateState {
            copy(
                generatePaymentLinkState = generatePaymentLinkState.copy(
                    amountInput = value,
                    errorMessage = null
                )
            )
        }
    }

    fun selectGenerateExpiryPreset(minutes: Int) {
        updateState {
            copy(
                generatePaymentLinkState = generatePaymentLinkState.copy(
                    selectedExpiryPresetMinutes = minutes,
                    useCustomExpiry = false,
                    customExpiryMinutesInput = "",
                    errorMessage = null
                )
            )
        }
    }

    fun setGenerateCustomExpiryEnabled(enabled: Boolean) {
        updateState {
            copy(
                generatePaymentLinkState = generatePaymentLinkState.copy(
                    useCustomExpiry = enabled,
                    errorMessage = null
                )
            )
        }
    }

    fun updateGenerateCustomExpiryInput(value: String) {
        updateState {
            copy(
                generatePaymentLinkState = generatePaymentLinkState.copy(
                    customExpiryMinutesInput = value.filter(Char::isDigit),
                    errorMessage = null
                )
            )
        }
    }

    fun generatePaymentLink() {
        if (!uiState.value.canCreatePaymentLink) return
        val order = uiState.value.order ?: return
        if (!order.supportsReceivableActions()) return
        val businessId = business?.businessId ?: return
        val remainingCents = totalOpenReceivableCents(order)
        val state = uiState.value.generatePaymentLinkState
        val expireInMinutes = if (state.useCustomExpiry) {
            state.customExpiryMinutesInput.toIntOrNull() ?: 0
        } else {
            state.selectedExpiryPresetMinutes
        }
        if (expireInMinutes <= 0) {
            updateState {
                copy(
                    generatePaymentLinkState = generatePaymentLinkState.copy(
                        errorMessage = "La expiración debe ser mayor a 0 minutos."
                    )
                )
            }
            return
        }

        val amountInput = state.amountInput.trim().replace(",", ".")
        val amountNumber = amountInput.toDoubleOrNull()
        if (amountInput.isNotBlank() && (amountNumber == null || amountNumber <= 0.0)) {
            updateState {
                copy(
                    generatePaymentLinkState = generatePaymentLinkState.copy(
                        errorMessage = "El monto debe ser mayor a 0."
                    )
                )
            }
            return
        }
        val maxAmount = remainingCents / 100.0
        if (amountNumber != null && amountNumber > maxAmount) {
            updateState {
                copy(
                    generatePaymentLinkState = generatePaymentLinkState.copy(
                        errorMessage = "El monto no puede exceder el saldo pendiente."
                    )
                )
            }
            return
        }

        showLoading()
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    orderService.createPaymentLink(
                        businessId = businessId,
                        orderId = order.id,
                        amount = amountInput.ifBlank { null },
                        expireInMinutes = expireInMinutes
                    )
                }
            }.onSuccess { generated ->
                runCatching {
                    orderService.refreshOrder(businessId = businessId, orderId = order.id)
                }.onSuccess { fresh ->
                    updateState {
                        copy(
                            order = fresh,
                            paymentLink = generated ?: PaymentLinkResolver.resolveCurrent(fresh)?.url,
                            showPaymentLinkSheet = true,
                            generatePaymentLinkState = generatePaymentLinkState.copy(
                                showSheet = false,
                                errorMessage = null
                            )
                        )
                    }
                    showSuccess()
                }.onFailure {
                    showError()
                }
            }.onFailure { error ->
                updateState {
                    copy(
                        generatePaymentLinkState = generatePaymentLinkState.copy(
                            errorMessage = mapOrderMutationError(error, "No se pudo generar el link de pago.")
                        )
                    )
                }
                showError()
            }
        }
    }

    fun createReplacementPaymentLink() {
        if (!uiState.value.canCreatePaymentLink) return
        val order = uiState.value.order ?: return
        if (!order.supportsReceivableActions()) return
        val businessId = business?.businessId ?: return
        val amount = totalOpenReceivableCents(order).takeIf { it > 0L }?.toDecimalString() ?: return

        showLoading()
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    releaseOpenIntentForReplacement(
                        businessId = businessId,
                        order = order,
                        reason = "customer_selected_payment_link",
                        expectedNextAction = "payment_link_allowed",
                    )
                    orderService.createReplacementPaymentLink(
                        businessId = businessId,
                        orderId = order.id,
                        amount = amount,
                    )
                }
            }.onSuccess { replacement ->
                runCatching {
                    orderService.refreshOrder(businessId = businessId, orderId = order.id)
                }.onSuccess { fresh ->
                    updateState {
                        copy(
                            order = fresh,
                            paymentLink = replacement.resolvedPaymentLinkUrl(),
                            showPaymentLinkSheet = true,
                        )
                    }
                    showSuccess()
                }.onFailure {
                    showError()
                }
            }.onFailure { error ->
                updateState {
                    copy(
                        generatePaymentLinkState = generatePaymentLinkState.copy(
                            errorMessage = mapOrderMutationError(error, "No se pudo crear el nuevo link de pago.")
                        )
                    )
                }
                showError()
            }
        }
    }

    fun createReplacementYappyOnsite() {
        if (!uiState.value.canCreatePaymentLink) return
        val order = uiState.value.order ?: return
        if (!order.supportsReceivableActions()) return
        val businessId = business?.businessId ?: return
        val amount = totalOpenReceivableCents(order).takeIf { it > 0L }?.toDecimalString() ?: return

        showLoading()
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    releaseOpenIntentForReplacement(
                        businessId = businessId,
                        order = order,
                        reason = "customer_selected_yappy_onsite",
                        expectedNextAction = "yappy_onsite_allowed",
                    )
                    orderService.createReplacementYappyOnsite(
                        businessId = businessId,
                        orderId = order.id,
                        amount = amount,
                        note = yappyOnsitePaymentDescription(),
                    )
                }
            }.onSuccess { replacement ->
                runCatching {
                    orderService.refreshOrder(businessId = businessId, orderId = order.id)
                }.onSuccess { fresh ->
                    val onsite = replacement.onsitePayment
                    updateState {
                        copy(
                            order = fresh,
                            replacementYappyOnsite = onsite,
                            replacementYappyOnsitePayload = null,
                            replacementYappyOnsitePolling = false,
                        )
                    }
                    onsite?.transactionId
                        ?.takeIf { it.isNotBlank() }
                        ?.let { startReplacementYappyOnsitePolling(it) }
                    showSuccess()
                }.onFailure {
                    showError()
                }
            }.onFailure { error ->
                snackbarService.show(mapOrderMutationError(error, "No se pudo crear el nuevo QR de Yappy."))
                showError()
            }
        }
    }

    fun dismissReplacementYappyOnsite() {
        replacementYappyPollingJob?.cancel()
        replacementYappyPollingJob = null
        updateState {
            copy(
                replacementYappyOnsite = null,
                replacementYappyOnsitePayload = null,
                replacementYappyOnsitePolling = false,
                showReplacementYappyCancelDialog = false,
            )
        }
    }

    fun startReplacementYappyOnsitePolling(transactionId: String? = uiState.value.replacementYappyOnsite?.transactionId) {
        val normalizedTransactionId = transactionId?.trim().orEmpty()
        val businessId = business?.businessId ?: return
        if (normalizedTransactionId.isBlank()) return

        replacementYappyPollingJob?.cancel()
        replacementYappyPollingJob = viewModelScope.launch {
            updateState { copy(replacementYappyOnsitePolling = true) }
            while (true) {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        paymentService.getYappyOnsiteTransaction(
                            businessId = businessId,
                            transactionId = normalizedTransactionId,
                        )
                    }
                }

                val payload = result.getOrElse { error ->
                    updateState { copy(replacementYappyOnsitePolling = false) }
                    snackbarService.show(
                        error.message?.takeIf { it.isNotBlank() }
                            ?: "No fue posible consultar el estado del QR de Yappy."
                    )
                    return@launch
                }

                updateState { copy(replacementYappyOnsitePayload = payload) }
                val status = payload.transaction.status
                val invoiceStatus = payload.invoice.status.takeIf { it != 0 } ?: payload.order.invoiceStatus
                if (shouldStopReplacementYappyPolling(status, invoiceStatus)) {
                    val orderId = payload.order.id ?: uiState.value.order?.id
                    if (orderId != null) {
                        withContext(Dispatchers.IO) {
                            runCatching {
                                orderService.refreshOrder(businessId = businessId, orderId = orderId)
                            }
                        }.onSuccess { fresh ->
                            updateState { copy(order = fresh) }
                        }
                    }
                    updateState { copy(replacementYappyOnsitePolling = false) }
                    return@launch
                }

                delay(REPLACEMENT_YAPPY_ONSITE_POLL_MS)
            }
        }
    }

    fun requestCancelReplacementYappyOnsite() {
        updateState { copy(showReplacementYappyCancelDialog = true) }
    }

    fun dismissCancelReplacementYappyOnsiteDialog() {
        updateState { copy(showReplacementYappyCancelDialog = false) }
    }

    fun cancelReplacementYappyOnsite() {
        val businessId = business?.businessId ?: return
        val transactionId = uiState.value.replacementYappyOnsite?.transactionId?.trim().orEmpty()
        if (transactionId.isBlank()) return

        updateState { copy(showReplacementYappyCancelDialog = false) }
        showLoading()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    paymentService.cancelYappyOnsiteTransaction(
                        businessId = businessId,
                        transactionId = transactionId,
                        reason = "customer_changed_payment_method",
                    )
                }
            }.onSuccess { cancelled ->
                replacementYappyPollingJob?.cancel()
                replacementYappyPollingJob = null
                updateState {
                    copy(
                        replacementYappyOnsitePayload = replacementYappyOnsitePayload?.copy(
                            transaction = cancelled
                        ),
                        replacementYappyOnsitePolling = false,
                    )
                }
                uiState.value.order?.id?.let { orderId ->
                    withContext(Dispatchers.IO) {
                        runCatching { orderService.refreshOrder(businessId = businessId, orderId = orderId) }
                    }.onSuccess { fresh -> updateState { copy(order = fresh) } }
                }
                showSuccess()
            }.onFailure {
                showError()
                snackbarService.show("No fue posible cancelar el QR de Yappy.")
            }
        }
    }

    private fun shouldStopReplacementYappyPolling(status: String, invoiceStatus: Int): Boolean {
        val normalized = status.trim().lowercase()
        return normalized in setOf("cancelled", "canceled", "expired", "returned") ||
            invoiceStatus == 2 ||
            invoiceStatus == 3
    }

    private fun yappyOnsitePaymentDescription(): String {
        return business?.name?.trim()?.takeIf { it.isNotBlank() } ?: "Pago Yappy"
    }

    fun resetManualPaymentFields() {
        updateState {
            copy(
                manualPaymentReference = "",
                manualPaymentDescription = "",
            )
        }
    }

    fun resetPaymentLink() {
        updateState {
            copy(paymentLink = null, showPaymentLinkSheet = false, errorLoadingPaymentLink = false)
        }
    }

    fun clearCancelOrderError() {
        updateState { copy(cancelOrderErrorMessage = null) }
    }

    fun cancelOrder(reason: String) {
        val trimmedReason = reason.trim()
        val reasonValidation = OrderCxcValidators.validateCancelOrderReason(trimmedReason)
        if (reasonValidation != null) {
            viewModelScope.launch {
                snackbarService.show(reasonValidation)
            }
            return
        }

        val order = uiState.value.order ?: return
        val businessId = business?.businessId ?: return
        clearCancelOrderError()
        showLoading()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val response = orderService.cancelOrder(
                        businessId,
                        order.id,
                        trimmedReason,
                        authService.getUserSync()?.name ?: "App"
                    )
                    withContext(Dispatchers.Main) {
                        if (response) {
                            refreshOrder(order.id)
                            emitEvent(OrderDetailsUiEvent.OrderCancelled)
                        } else {
                            showError()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        val errorMessage = mapOrderMutationError(
                            error = e,
                            fallback = "No se pudo anular el pedido.",
                            codeOverrides = mapOf("O_RP_001" to "No se pudo anular el pedido.")
                        )
                        updateState {
                            copy(cancelOrderErrorMessage = errorMessage)
                        }
                        snackbarService.show(errorMessage)
                        showError()
                    }
                }
            }
        }
    }

    fun deleteOrder(reason: String) {
        val order = uiState.value.order ?: return
        val businessId = business?.businessId ?: return
        showLoading()
        viewModelScope.launch {
            try {
                val deleted = withContext(Dispatchers.IO) {
                    orderService.deleteOrder(
                        businessId = businessId,
                        orderId = order.id,
                        reason = reason
                    )
                }

                if (deleted) {
                    showSuccess()
                    delay(1200)
                    emitEvent(OrderDetailsUiEvent.OrderDeleted)
                } else {
                    showError()
                }
            } catch (_: Exception) {
                showError()
            }
        }
    }

    fun retryElectronicInvoice() {
        val order = uiState.value.order ?: return
        val businessId = business?.businessId ?: return
        showLoading()
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val response = orderService.retryElectronicInvoice(businessId, order.id)
                    val freshOrder = runCatching {
                        orderService.refreshOrder(businessId = businessId, orderId = order.id)
                    }.getOrNull()
                    response to freshOrder
                }
            }.onSuccess { result ->
                val retryResponse = result.first
                val freshOrder = result.second ?: uiState.value.order
                when (val feedback = resolveRetryInvoiceFeedback(freshOrder, retryResponse)) {
                    RetryInvoiceFeedback.Success -> {
                    updateState {
                        copy(
                            order = freshOrder,
                            invoiceRetryState = invoiceRetryState.copy(
                                showSuccessDialog = true,
                                showWarningDialog = false,
                                warningMessage = null
                            )
                        )
                    }
                    showSuccess()
                    }

                    is RetryInvoiceFeedback.Warning -> {
                    updateState {
                        copy(
                            order = freshOrder,
                            invoiceRetryState = invoiceRetryState.copy(
                                showSuccessDialog = false,
                                showWarningDialog = true,
                                warningMessage = feedback.message
                            )
                        )
                    }
                    hideLoading()
                    }
                }
            }.onFailure { error ->
                snackbarService.show(mapOrderMutationError(error, "No se pudo reintentar la facturación."))
                showError()
            }
        }
    }

    fun dismissRetryInvoiceDialogs() {
        updateState {
            copy(
                invoiceRetryState = invoiceRetryState.copy(
                    showSuccessDialog = false,
                    showWarningDialog = false,
                    warningMessage = null
                )
            )
        }
    }

    fun buildInvoiceShareMessage(): String {
        val order = uiState.value.order ?: return ""
        val cufe = order.externalInvoiceNumber.orEmpty()
        val total = order.totalAmount
        return buildString {
            append("Factura del pedido #")
            append(order.internalNumber)
            if (total.isNotBlank()) {
                append("\nTotal: ")
                append(total)
            }
            if (cufe.isNotBlank()) {
                append("\nCUFE: ")
                append(cufe)
            }
        }
    }

    fun canShowRetryInvoiceButton(order: Order? = uiState.value.order): Boolean {
        return shouldShowRetryInvoiceButton(order)
    }

    fun canShowPaidPaymentLinkInvoiceButton(order: Order? = uiState.value.order): Boolean {
        val safeOrder = order ?: return false
        if (!uiState.value.canMarkPaid) return false
        if (!safeOrder.supportsReceivableActions()) return false
        if (safeOrder.status == OrderStatus.CANCELLED) return false
        if (!safeOrder.paymentFlowType.equals("payment_link", ignoreCase = true)) return false
        if (safeOrder.paymentStatus != PaymentStatus.PAID.id) return false
        if ((safeOrder.invoiceStatus ?: InvoiceStatus.NONE.id) != InvoiceStatus.NONE.id) return false
        if (!safeOrder.externalInvoiceNumber.isNullOrBlank()) return false
        return safeOrder.totalAmount.toLongCents() > 0L
    }

    fun canGeneratePaymentLink(order: Order? = uiState.value.order): Boolean {
        if (!uiState.value.canCreatePaymentLink) return false
        if (!uiState.value.havePaymentsConfigured) return false
        val safeOrder = order ?: return false
        if (!safeOrder.supportsReceivableActions()) return false
        if (safeOrder.status == OrderStatus.CANCELLED) return false
        if (safeOrder.paymentStatus == PaymentStatus.PAID.id) return false
        if (totalOpenReceivableCents(safeOrder) <= 0L) return false
        if (PaymentLinkResolver.hasOpenLink(safeOrder)) return false
        return true
    }

    fun canCopyOrSharePaymentLink(order: Order? = uiState.value.order): Boolean {
        val safeOrder = order ?: return false
        if (!safeOrder.supportsReceivableActions()) return false
        if (safeOrder.paymentStatus == PaymentStatus.PAID.id) return false
        return PaymentLinkResolver.hasOpenLink(safeOrder)
    }

    fun canInvoiceDraftOrder(order: Order? = uiState.value.order): Boolean {
        val safeOrder = order ?: return false
        if (!safeOrder.supportsReceivableActions()) return false
        val invoiceStatus = safeOrder.invoiceStatus ?: InvoiceStatus.NONE.id
        val isNotInvoiced = invoiceStatus == InvoiceStatus.NONE.id ||
                invoiceStatus == InvoiceStatus.PENDING.id
        val isDraftNotInvoiced = safeOrder.status == OrderStatus.DRAFT && isNotInvoiced
        val isConfirmedPaymentLinkNotInvoiced = safeOrder.status == OrderStatus.CONFIRMED &&
                safeOrder.paymentFlowType.equals("payment_link", ignoreCase = true) &&
                isNotInvoiced
        val hasOutstandingPayment = safeOrder.paymentStatus != PaymentStatus.PAID.id
        return uiState.value.canMarkPaid &&
                (isDraftNotInvoiced || isConfirmedPaymentLinkNotInvoiced) &&
                hasOutstandingPayment &&
                safeOrder.totalAmount.toLongCents() > 0L
    }

    fun achRejectReasonOptions(): List<Pair<String, String>> = listOf(
        "fraud" to "Fraude",
        "invalid_proof" to "Comprobante inválido",
        "amount_mismatch" to "Monto no coincide",
        "reference_mismatch" to "Referencia no coincide",
        "other" to "Otro"
    )

    fun isAutomaticAchPayment(payment: OrderPaymentDto): Boolean {
        if (!payment.isAutomatic) return false
        val methodName = payment.paymentMethod.name.lowercase()
        val methodDescription = payment.paymentMethod.description.lowercase()
        return methodName.contains("ach") ||
            methodDescription.contains("ach")
    }

    fun achStatusLabel(rawStatus: String?): String {
        return when (normalizeAchStatus(rawStatus)) {
            "pending_review" -> "Pendiente revisión"
            "requires_action" -> "Requiere acción"
            "pending", "processing", "created" -> "Pendiente"
            "approved", "paid", "succeeded", "completed" -> "Pagado"
            "rejected", "declined", "cancelled" -> "Rechazado"
            else -> rawStatus?.replace('_', ' ')?.replaceFirstChar { it.uppercase() } ?: "Pendiente"
        }
    }

    fun canShowAchApproveAction(rawStatus: String?): Boolean {
        if (!uiState.value.canApproveAchPayment) return false
        return when (normalizeAchStatus(rawStatus)) {
            "pending_review", "requires_action", "pending", "processing", "created" -> true
            else -> false
        }
    }

    fun canShowAchRejectAction(rawStatus: String?): Boolean {
        if (!uiState.value.canRejectAchPayment) return false
        return when (normalizeAchStatus(rawStatus)) {
            "pending_review", "requires_action", "pending", "processing", "created" -> true
            else -> false
        }
    }

    fun canShowAchProofAction(detail: AchPaymentDetail?): Boolean {
        if (!uiState.value.canViewAchPayment) return false
        val safeDetail = detail ?: return false
        if (normalizeAchStatus(safeDetail.paymentStatus) == "rejected") return false
        return safeDetail.proofId != null || !safeDetail.proofFileUrl.isNullOrBlank()
    }

    fun canDownloadAchProof(detail: AchPaymentDetail?): Boolean {
        val safeDetail = detail ?: return false
        return normalizeAchStatus(safeDetail.paymentStatus) in setOf("approved", "paid", "succeeded", "completed")
    }

    fun achDetailState(paymentIntentId: String): AchIntentDetailState {
        return uiState.value.achIntentStates[paymentIntentId] ?: AchIntentDetailState()
    }

    fun loadAchDetailIfNeeded(paymentIntentId: String, force: Boolean = false) {
        if (paymentIntentId.isBlank()) return
        viewModelScope.launch {
            fetchAndCacheAchDetail(paymentIntentId, force = force)
        }
    }

    fun openAchApproveDialog(paymentIntentId: String) {
        if (!uiState.value.canApproveAchPayment || paymentIntentId.isBlank()) return
        val detail = achDetailState(paymentIntentId).detail
        val highRisk = isHighRisk(detail)
        updateState {
            copy(
                achApproveDialog = AchApproveDialogState(
                    show = true,
                    paymentIntentId = paymentIntentId,
                    highRisk = highRisk
                )
            )
        }
    }

    fun dismissAchApproveDialog() {
        updateState { copy(achApproveDialog = AchApproveDialogState()) }
    }

    fun confirmApproveAchPayment() {
        if (!uiState.value.canApproveAchPayment) return
        val paymentIntentId = uiState.value.achApproveDialog.paymentIntentId ?: return
        val order = uiState.value.order
        val businessId = business?.businessId ?: order?.businessId
        if (businessId == null || businessId <= 0) {
            viewModelScope.launch {
                snackbarService.show("No se pudo determinar el negocio para aprobar el pago ACH.")
            }
            return
        }

        showLoading()
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    orderService.approveAchPayment(businessId, paymentIntentId)
                    runCatching { financialProfileService.refresh(businessId) }
                    val freshOrder = order?.let {
                        runCatching { orderService.refreshOrder(businessId, it.id) }.getOrNull()
                    }
                    val freshAchDetail = runCatching {
                        orderService.getAchPaymentByIntent(businessId, paymentIntentId)
                    }.getOrNull()
                    freshOrder to freshAchDetail
                }
            }.onSuccess { result ->
                val freshOrder = result.first
                val freshAchDetail = result.second
                updateState {
                    val updatedMap = if (freshAchDetail != null) {
                        achIntentStates + (paymentIntentId to AchIntentDetailState(detail = freshAchDetail))
                    } else {
                        achIntentStates
                    }
                    copy(
                        order = freshOrder ?: order,
                        achIntentStates = updatedMap,
                        achApproveDialog = AchApproveDialogState(),
                        achRejectDialog = AchRejectDialogState()
                    )
                }
                showSuccess()
            }.onFailure { error ->
                snackbarService.show(mapOrderMutationError(error, "No se pudo aprobar el pago ACH."))
                showError()
            }
        }
    }

    fun openAchRejectDialog(paymentIntentId: String) {
        if (!uiState.value.canRejectAchPayment || paymentIntentId.isBlank()) return
        updateState {
            copy(
                achRejectDialog = AchRejectDialogState(
                    show = true,
                    paymentIntentId = paymentIntentId,
                    reasonCode = "fraud",
                    customReasonText = "",
                    errorMessage = null
                )
            )
        }
    }

    fun dismissAchRejectDialog() {
        updateState { copy(achRejectDialog = AchRejectDialogState()) }
    }

    fun updateAchRejectReasonCode(reasonCode: String) {
        updateState {
            copy(
                achRejectDialog = achRejectDialog.copy(
                    reasonCode = reasonCode,
                    errorMessage = null
                )
            )
        }
    }

    fun updateAchRejectCustomReasonText(value: String) {
        updateState {
            copy(
                achRejectDialog = achRejectDialog.copy(
                    customReasonText = value,
                    errorMessage = null
                )
            )
        }
    }

    fun confirmRejectAchPayment() {
        if (!uiState.value.canRejectAchPayment) return
        val dialog = uiState.value.achRejectDialog
        val paymentIntentId = dialog.paymentIntentId ?: return
        val order = uiState.value.order
        val businessId = business?.businessId ?: order?.businessId
        if (businessId == null || businessId <= 0) {
            viewModelScope.launch {
                snackbarService.show("No se pudo determinar el negocio para rechazar el pago ACH.")
            }
            return
        }
        val reasonText = if (dialog.reasonCode == "other") {
            dialog.customReasonText.trim()
        } else {
            defaultAchRejectReasonText(dialog.reasonCode)
        }

        if (reasonText.isBlank()) {
            updateState {
                copy(
                    achRejectDialog = achRejectDialog.copy(
                        errorMessage = "Debes especificar una razón para rechazar."
                    )
                )
            }
            return
        }

        showLoading()
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    orderService.rejectAchPayment(
                        businessId = businessId,
                        paymentIntentId = paymentIntentId,
                        reasonCode = dialog.reasonCode,
                        reasonText = reasonText
                    )
                    runCatching { financialProfileService.refresh(businessId) }
                    val freshOrder = order?.let {
                        runCatching { orderService.refreshOrder(businessId, it.id) }.getOrNull()
                    }
                    val freshAchDetail = runCatching {
                        orderService.getAchPaymentByIntent(businessId, paymentIntentId)
                    }.getOrNull()
                    freshOrder to freshAchDetail
                }
            }.onSuccess { result ->
                val freshOrder = result.first
                val freshAchDetail = result.second
                updateState {
                    val updatedMap = if (freshAchDetail != null) {
                        achIntentStates + (paymentIntentId to AchIntentDetailState(detail = freshAchDetail))
                    } else {
                        achIntentStates
                    }
                    copy(
                        order = freshOrder ?: order,
                        achIntentStates = updatedMap,
                        achRejectDialog = AchRejectDialogState(),
                        achApproveDialog = AchApproveDialogState()
                    )
                }
                showSuccess()
            }.onFailure { error ->
                snackbarService.show(mapOrderMutationError(error, "No se pudo rechazar el pago ACH."))
                showError()
            }
        }
    }

    fun openAchProofPreview(paymentIntentId: String) {
        if (paymentIntentId.isBlank()) return
        updateState {
            copy(
                achProofPreviewState = AchProofPreviewState(
                    show = true,
                    paymentIntentId = paymentIntentId,
                    isLoading = true
                )
            )
        }
        viewModelScope.launch {
            prepareAchProofPreview(paymentIntentId = paymentIntentId, openSheet = true)
        }
    }

    fun closeAchProofPreview() {
        updateState {
            copy(
                achProofPreviewState = achProofPreviewState.copy(show = false)
            )
        }
    }

    fun loadAchReview(paymentIntentId: String) {
        if (paymentIntentId.isBlank()) return
        updateState {
            copy(
                achReviewState = AchReviewState(
                    paymentIntentId = paymentIntentId,
                    isLoading = true,
                    errorMessage = null
                ),
                achProofPreviewState = achProofPreviewState.copy(
                    show = false,
                    paymentIntentId = paymentIntentId,
                    isLoading = true,
                    imageDataUri = null,
                    previewUrl = null,
                    contentType = null,
                    fileName = null,
                    errorMessage = null
                )
            )
        }
        viewModelScope.launch {
            val detail = fetchAndCacheAchDetail(paymentIntentId, force = false)
            if (detail == null) {
                updateState {
                    copy(
                        achReviewState = achReviewState.copy(
                            isLoading = false,
                            errorMessage = achDetailState(paymentIntentId).errorMessage
                                ?: "No se pudo cargar el detalle ACH."
                        ),
                        achProofPreviewState = achProofPreviewState.copy(
                            paymentIntentId = paymentIntentId,
                            isLoading = false,
                            imageDataUri = null,
                            previewUrl = null,
                            contentType = null,
                            fileName = null,
                            errorMessage = "No se pudo cargar el comprobante ACH."
                        )
                    )
                }
                return@launch
            }

            updateState {
                copy(
                    achReviewState = achReviewState.copy(
                        paymentIntentId = paymentIntentId,
                        isLoading = false,
                        errorMessage = null
                    )
                )
            }
            prepareAchProofPreview(paymentIntentId = paymentIntentId, openSheet = false)
        }
    }

    fun retryAchReview() {
        val paymentIntentId = uiState.value.achReviewState.paymentIntentId ?: return
        loadAchReview(paymentIntentId)
    }

    fun setAchScoreInfoDialog(show: Boolean) {
        updateState { copy(showAchScoreInfoDialog = show) }
    }

    fun openAchProofDocumentForDownload(paymentIntentId: String) {
        if (paymentIntentId.isBlank()) return
        val order = uiState.value.order
        val businessId = business?.businessId ?: order?.businessId ?: return
        val detail = resolveAchDetailForIntent(paymentIntentId)
        if (detail == null) {
            viewModelScope.launch {
                snackbarService.show("No hay detalle ACH disponible para descargar el comprobante.")
            }
            return
        }
        if (!canDownloadAchProof(detail)) {
            viewModelScope.launch {
                snackbarService.show("El comprobante solo se puede descargar cuando el pago ACH está aprobado.")
            }
            return
        }

        val cacheKeys = achProofCacheKeys(paymentIntentId, detail)
        val cachedBinary = cacheKeys.asSequence()
            .mapNotNull { key -> achProofBinaryCache[key] }
            .firstOrNull()
        if (cachedBinary != null) {
            cacheKeys.forEach { key -> achProofBinaryCache[key] = cachedBinary }
            viewModelScope.launch {
                handleAchProofDownloadSuccess(
                    detail = detail,
                    binary = cachedBinary,
                    withLoadingFeedback = false
                )
            }
            return
        }

        showLoading()
        viewModelScope.launch {
            runCatching {
                val paymentId = detail.paymentId ?: error("No se encontró el identificador del pago.")
                val proofId = detail.proofId ?: error("No se encontró el identificador del comprobante.")
                val downloaded = orderService.downloadAchProofFile(
                    businessId = businessId,
                    paymentId = paymentId,
                    proofId = proofId
                )
                AchProofBinary(
                    bytes = downloaded.bytes,
                    contentType = downloaded.contentType ?: detail.proofContentType,
                    fileName = downloaded.fileName ?: detail.proofFileName
                )
            }.onSuccess { binary ->
                cacheKeys.forEach { key -> achProofBinaryCache[key] = binary }
                handleAchProofDownloadSuccess(
                    detail = detail,
                    binary = binary,
                    withLoadingFeedback = true
                )
            }.onFailure { error ->
                val fallbackUrl = detail.proofFileUrl
                if (!fallbackUrl.isNullOrBlank()) {
                    emitEvent(OrderDetailsUiEvent.OpenExternalUrl(fallbackUrl))
                    showSuccess()
                } else {
                    snackbarService.show(mapOrderMutationError(error, "No se pudo descargar el comprobante ACH."))
                    showError()
                }
            }
        }
    }

    private fun resolveAchDetailForIntent(paymentIntentId: String): AchPaymentDetail? {
        val direct = achDetailState(paymentIntentId).detail
        if (direct != null) return direct
        return uiState.value.achIntentStates.values
            .asSequence()
            .mapNotNull { state -> state.detail }
            .firstOrNull { detail -> detail.paymentUid == paymentIntentId }
    }

    private fun achProofCacheKeys(
        paymentIntentId: String,
        detail: AchPaymentDetail
    ): List<String> {
        val stateKeys = uiState.value.achIntentStates
            .asSequence()
            .filter { (_, state) -> state.detail?.paymentUid == detail.paymentUid }
            .map { (key, _) -> key }
            .toList()
        val previewKey = uiState.value.achProofPreviewState.paymentIntentId

        return buildList {
            add(paymentIntentId)
            if (detail.paymentUid.isNotBlank()) add(detail.paymentUid)
            if (!previewKey.isNullOrBlank()) add(previewKey)
            addAll(stateKeys)
        }.distinct()
    }

    private suspend fun handleAchProofDownloadSuccess(
        detail: AchPaymentDetail,
        binary: AchProofBinary,
        withLoadingFeedback: Boolean
    ) {
        val mimeType = normalizeMimeType(binary.contentType ?: detail.proofContentType)
        val effectiveMimeType = if (mimeType.isBlank()) "application/octet-stream" else mimeType
        val fileName = resolveAchProofFileName(
            suggestedName = binary.fileName ?: detail.proofFileName,
            paymentIntentId = detail.paymentUid,
            mimeType = effectiveMimeType
        )
        val isImage = effectiveMimeType.startsWith("image/")

        val saved = if (isImage) {
            pdfSharer.saveImageToGallery(
                filename = fileName,
                bytes = binary.bytes,
                mimeType = effectiveMimeType
            )
        } else {
            pdfSharer.saveFileToDocuments(
                filename = fileName,
                bytes = binary.bytes,
                mimeType = effectiveMimeType
            )
        }

        if (saved) {
            snackbarService.show(
                if (isImage) "Comprobante guardado en la galería."
                else "Comprobante guardado en documentos."
            )
            if (withLoadingFeedback) showSuccess() else hideLoading()
        } else {
            snackbarService.show("No se pudo guardar el comprobante ACH.")
            if (withLoadingFeedback) showError() else hideLoading()
        }
    }

    private fun normalizeMimeType(rawMimeType: String?): String {
        return rawMimeType
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase()
            .orEmpty()
    }

    private fun resolveAchProofFileName(
        suggestedName: String?,
        paymentIntentId: String,
        mimeType: String
    ): String {
        val cleaned = suggestedName
            ?.substringAfterLast('/')
            ?.substringBefore('?')
            ?.trim()
            ?.trim('"')
            .orEmpty()
        val extension = inferFileExtension(mimeType)

        if (cleaned.isNotBlank()) {
            val hasExtension = cleaned.substringAfterLast('.', "").isNotBlank()
            return if (hasExtension || extension == "bin") cleaned else "$cleaned.$extension"
        }

        return "comprobante_ach_${paymentIntentId.ifBlank { "archivo" }}.$extension"
    }

    private fun inferFileExtension(mimeType: String): String {
        return when {
            mimeType.contains("pdf") -> "pdf"
            mimeType.contains("png") -> "png"
            mimeType.contains("jpeg") || mimeType.contains("jpg") -> "jpg"
            mimeType.contains("webp") -> "webp"
            mimeType.contains("gif") -> "gif"
            else -> "bin"
        }
    }

    fun getDocumentByCufe() {
        val order = uiState.value.order ?: return
        val cufe = order.externalInvoiceNumber ?: return
        val businessId = business?.businessId ?: return
        showLoading()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val docs = orderService.getDocumentByCufe(
                        businessId,
                        cufe
                    ) // must return pdf in base64
                    val pdfB64 = docs.pdfBase64 ?: error("No PDF in response")

                    @OptIn(ExperimentalEncodingApi::class)
                    val pdfBytes = Base64.decode(pdfB64)

                    withContext(Dispatchers.Main) {
                        // Open native share sheet
                        val filename = "${order.internalNumber}.pdf"
                        pdfSharer.openPdf(filename, pdfBytes)
                        showSuccess()
                    }
                } catch (e: Exception) {
                    println("Error getting document by CUFE: ${e.message}")
                    withContext(Dispatchers.Main) {
                        showError()
                    }
                }
            }
        }
    }

    fun canShowReprintAction(order: Order? = uiState.value.order): Boolean {
        val safeOrder = order ?: return false
        val businessId = business?.businessId ?: safeOrder.businessId
        return !uiState.value.hideReprintTicketAction &&
            !uiState.value.reprintInFlight &&
            safeOrder.invoiceStatus == InvoiceStatus.ISSUED.id &&
            safeOrder.id > 0 &&
            businessId > 0 &&
            safeOrder.ticketEnabled == true
    }

    fun reprintTicket() {
        if (uiState.value.reprintInFlight) return
        val order = uiState.value.order ?: return
        val businessId = business?.businessId ?: order.businessId
        if (businessId <= 0) return

        showLoading()
        updateState {
            copy(
                reprintInFlight = true,
                showPrinterSelectionSheet = false,
                printerSelectionOptions = emptyList(),
                reprintTicketState = null
            )
        }

        viewModelScope.launch {
            runCatching {
                val payload = printerService.fetchOrderTicketLayout(
                    orderId = order.id,
                    businessId = businessId
                )
                val layout = printerService.parseTicketLayout(payload)
                val reprintState = com.teco.ventago.features.printers.domain.model.ReprintTicketState(
                    businessId = businessId,
                    orderId = order.id,
                    orderNumber = order.internalNumber,
                    ticketLayout = layout
                )
                val printers = printerService.resolveSelectionOptions().ifEmpty {
                    runCatching {
                        printerService.refresh(businessId = businessId, force = true)
                    }
                    printerService.resolveSelectionOptions()
                }
                Triple(reprintState, layout, printers)
            }.onSuccess { result ->
                val reprintState = result.first
                val options = result.third
                when {
                    options.isEmpty() -> {
                        hideLoading()
                        updateState { copy(reprintInFlight = false) }
                        snackbarService.show("No hay impresoras activas disponibles para reimprimir este ticket.")
                    }

                    options.size == 1 -> {
                        printTicketWithSelection(options.first().printerConfig, reprintState)
                    }

                    else -> {
                        hideLoading()
                        updateState {
                            copy(
                                reprintInFlight = false,
                                showPrinterSelectionSheet = true,
                                printerSelectionOptions = options,
                                reprintTicketState = reprintState
                            )
                        }
                    }
                }
            }.onFailure { error ->
                hideLoading()
                updateState {
                    copy(
                        reprintInFlight = false,
                        hideReprintTicketAction = error is com.teco.ventago.features.printers.domain.model.TicketUnavailableException
                    )
                }
                val message = if (error is com.teco.ventago.features.printers.domain.model.TicketUnavailableException) {
                    "Este pedido no tiene ticket disponible para reimprimir."
                } else {
                    "No se pudo cargar el ticket para reimprimir."
                }
                snackbarService.show(message)
            }
        }
    }

    fun dismissPrinterSelectionSheet() {
        updateState {
            copy(
                showPrinterSelectionSheet = false,
                printerSelectionOptions = emptyList(),
                reprintTicketState = null,
                reprintInFlight = false
            )
        }
    }

    fun printSelectedPrinter(optionIndex: Int) {
        val state = uiState.value
        val option = state.printerSelectionOptions.getOrNull(optionIndex) ?: return
        val reprintState = state.reprintTicketState ?: return
        printTicketWithSelection(option.printerConfig, reprintState)
    }

    private fun printTicketWithSelection(
        printerConfig: com.teco.ventago.features.printers.domain.model.PrinterConfig,
        reprintState: com.teco.ventago.features.printers.domain.model.ReprintTicketState,
    ) {
        showLoading()
        updateState {
            copy(
                reprintInFlight = true,
                showPrinterSelectionSheet = false
            )
        }
        viewModelScope.launch {
            runCatching {
                printerService.reprintTicket(
                    reprintState = reprintState,
                    printerConfig = printerConfig
                )
            }.onSuccess {
                hideLoading()
                updateState {
                    copy(
                        reprintInFlight = false,
                        reprintTicketState = null,
                        printerSelectionOptions = emptyList()
                    )
                }
                snackbarService.show("Ticket reenviado a la impresora correctamente.")
            }.onFailure { error ->
                hideLoading()
                updateState {
                    copy(
                        reprintInFlight = false,
                        reprintTicketState = null,
                        printerSelectionOptions = emptyList()
                    )
                }
                val detail = error.message?.takeIf { it.isNotBlank() } ?: "Verifica la conexión de la impresora."
                snackbarService.show("La reimpresión falló. $detail")
            }
        }
    }


    fun isOrderCancellable(status: Int): Boolean = when (status) {
        OrderStatus.DRAFT,
        OrderStatus.CONFIRMED,
        OrderStatus.PROCESSING,
        OrderStatus.READY -> true

        else -> false // COMPLETED, CANCELLED, REJECT, REFUNDED are not cancellable
    }

    fun nonCancelledReceivableTerms(order: Order? = uiState.value.order): List<ReceivableTermDto> {
        val safeOrder = order ?: return emptyList()
        return safeOrder.receivableTerms.filter { it.status != 4 }
    }

    fun openReceivableTerms(order: Order? = uiState.value.order): List<ReceivableTermDto> {
        return nonCancelledReceivableTerms(order).filter { it.openAmount.toLongCents() > 0L }
    }

    fun totalOpenReceivableCents(order: Order? = uiState.value.order): Long {
        val safeOrder = order ?: return 0L
        return OrderReceivableResolver.totalOpenCents(safeOrder)
    }

    private fun centsToAmountInput(amountCents: Long): String {
        val safeCents = amountCents.coerceAtLeast(0L)
        val units = safeCents / 100L
        val decimals = (safeCents % 100L).toString().padStart(2, '0')
        return "$units.$decimals"
    }

    fun totalOverdueReceivableCents(order: Order? = uiState.value.order): Long {
        val nowEpoch = Clock.System.now().epochSeconds
        return nonCancelledReceivableTerms(order)
            .filter { it.openAmount.toLongCents() > 0L && (it.status == 0 || it.status == 1) && it.dueDateUnixSeconds < nowEpoch }
            .sumOf { it.openAmount.toLongCents() }
    }

    fun openRegisterPaymentSheet() {
        if (!uiState.value.canMarkPaid) return
        val order = uiState.value.order ?: return
        if (!order.supportsReceivableActions()) return
        if (order.status == OrderStatus.CANCELLED || order.invoiceStatus != InvoiceStatus.ISSUED.id) return
        if (totalOpenReceivableCents(order) <= 0L) return

        analyticsService.logOrderPaymentActionOpened(mode = "mark_paid_full")
        val today = todayPanamaIso()
        updateState {
            copy(
                registerPaymentState = RegisterPaymentState(
                    showSheet = true,
                    mode = RegisterPaymentMode.AUTOMATIC,
                    rows = listOf(RegisterPaymentRowState(id = 1, paymentDateIso = today)),
                    nextRowId = 2,
                    nextApplicationId = 1,
                    errorMessage = null
                )
            )
        }
    }

    fun closeRegisterPaymentSheet() {
        updateState {
            copy(registerPaymentState = registerPaymentState.copy(showSheet = false, errorMessage = null))
        }
    }

    fun setRegisterPaymentMode(mode: RegisterPaymentMode) {
        val current = uiState.value.registerPaymentState
        analyticsService.logOrderPaymentActionOpened(
            mode = when (mode) {
                RegisterPaymentMode.AUTOMATIC -> "mark_paid_full"
                RegisterPaymentMode.MANUAL -> "mark_paid_installments"
            }
        )
        updateState {
            copy(
                registerPaymentState = current.copy(
                    mode = mode,
                    rows = current.rows.map { row ->
                        if (mode == RegisterPaymentMode.AUTOMATIC) {
                            row.copy(applications = emptyList())
                        } else {
                            row.copy(
                                applications = if (row.applications.isEmpty()) {
                                    listOf(RegisterPaymentApplicationState(id = current.nextApplicationId))
                                } else row.applications
                            )
                        }
                    },
                    nextApplicationId = if (mode == RegisterPaymentMode.AUTOMATIC) current.nextApplicationId else current.nextApplicationId + 1,
                    errorMessage = null
                )
            )
        }
    }

    fun addRegisterPaymentRow() {
        val current = uiState.value.registerPaymentState
        val today = todayPanamaIso()
        val newRow = RegisterPaymentRowState(
            id = current.nextRowId,
            paymentDateIso = today,
            applications = if (current.mode == RegisterPaymentMode.MANUAL) {
                listOf(RegisterPaymentApplicationState(id = current.nextApplicationId))
            } else {
                emptyList()
            }
        )
        updateState {
            copy(
                registerPaymentState = current.copy(
                    rows = current.rows + newRow,
                    nextRowId = current.nextRowId + 1,
                    nextApplicationId = if (current.mode == RegisterPaymentMode.MANUAL) {
                        current.nextApplicationId + 1
                    } else {
                        current.nextApplicationId
                    },
                    errorMessage = null
                )
            )
        }
    }

    fun removeRegisterPaymentRow(rowId: Int) {
        val current = uiState.value.registerPaymentState
        val updated = current.rows.filterNot { it.id == rowId }
        updateState {
            copy(
                registerPaymentState = current.copy(
                    rows = updated.ifEmpty {
                        listOf(RegisterPaymentRowState(id = current.nextRowId, paymentDateIso = todayPanamaIso()))
                    },
                    nextRowId = if (updated.isEmpty()) current.nextRowId + 1 else current.nextRowId,
                    errorMessage = null
                )
            )
        }
    }

    fun updateRegisterPaymentMethod(rowId: Int, methodCode: Int) {
        val current = uiState.value.registerPaymentState
        updateState {
            copy(
                registerPaymentState = current.copy(
                    rows = current.rows.map { row ->
                        if (row.id == rowId) row.copy(paymentMethodCode = methodCode) else row
                    },
                    errorMessage = null
                )
            )
        }
    }

    fun updateRegisterPaymentAmount(rowId: Int, amountInput: String) {
        val current = uiState.value.registerPaymentState
        updateState {
            copy(
                registerPaymentState = current.copy(
                    rows = current.rows.map { row ->
                        if (row.id == rowId) row.copy(amountInput = amountInput.filter(Char::isDigit)) else row
                    },
                    errorMessage = null
                )
            )
        }
    }

    fun updateRegisterPaymentDate(rowId: Int, paymentDateIso: String) {
        val current = uiState.value.registerPaymentState
        updateState {
            copy(
                registerPaymentState = current.copy(
                    rows = current.rows.map { row ->
                        if (row.id == rowId) row.copy(paymentDateIso = paymentDateIso) else row
                    },
                    errorMessage = null
                )
            )
        }
    }

    fun addRegisterPaymentApplication(rowId: Int) {
        val current = uiState.value.registerPaymentState
        if (current.mode != RegisterPaymentMode.MANUAL) return
        val app = RegisterPaymentApplicationState(id = current.nextApplicationId)
        updateState {
            copy(
                registerPaymentState = current.copy(
                    rows = current.rows.map { row ->
                        if (row.id == rowId) row.copy(applications = row.applications + app) else row
                    },
                    nextApplicationId = current.nextApplicationId + 1,
                    errorMessage = null
                )
            )
        }
    }

    fun removeRegisterPaymentApplication(rowId: Int, applicationId: Int) {
        val current = uiState.value.registerPaymentState
        updateState {
            copy(
                registerPaymentState = current.copy(
                    rows = current.rows.map { row ->
                        if (row.id == rowId) {
                            row.copy(applications = row.applications.filterNot { it.id == applicationId })
                        } else {
                            row
                        }
                    },
                    errorMessage = null
                )
            )
        }
    }

    fun updateRegisterPaymentApplicationTerm(rowId: Int, applicationId: Int, receivableTermId: Long?) {
        val current = uiState.value.registerPaymentState
        updateState {
            copy(
                registerPaymentState = current.copy(
                    rows = current.rows.map { row ->
                        if (row.id == rowId) {
                            row.copy(
                                applications = row.applications.map { app ->
                                    if (app.id == applicationId) app.copy(receivableTermId = receivableTermId) else app
                                }
                            )
                        } else {
                            row
                        }
                    },
                    errorMessage = null
                )
            )
        }
    }

    fun updateRegisterPaymentApplicationAmount(rowId: Int, applicationId: Int, amountInput: String) {
        val current = uiState.value.registerPaymentState
        updateState {
            copy(
                registerPaymentState = current.copy(
                    rows = current.rows.map { row ->
                        if (row.id == rowId) {
                            row.copy(
                                applications = row.applications.map { app ->
                                    if (app.id == applicationId) app.copy(amountInput = amountInput.filter(Char::isDigit)) else app
                                }
                            )
                        } else {
                            row
                        }
                    },
                    errorMessage = null
                )
            )
        }
    }

    fun submitRegisterPayments() {
        val state = uiState.value
        val order = state.order ?: return
        if (!order.supportsReceivableActions()) return
        val registerState = state.registerPaymentState
        val totalOpen = totalOpenReceivableCents(order)
        val openByTerm = openReceivableTerms(order).associate { it.id to it.openAmount.toLongCents() }

        val validation = OrderCxcValidators.validateRegisterPayment(
            mode = registerState.mode,
            rows = registerState.rows,
            openBalanceCents = totalOpen,
            openByTermCents = openByTerm,
            todayIso = todayPanamaIso()
        )

        if (validation != null) {
            updateState { copy(registerPaymentState = registerPaymentState.copy(errorMessage = validation)) }
            return
        }

        val submitMode = when (registerState.mode) {
            RegisterPaymentMode.AUTOMATIC -> "mark_paid_full"
            RegisterPaymentMode.MANUAL -> "mark_paid_installments"
        }
        analyticsService.logOrderPaymentSubmitAttempted(mode = submitMode)
        showLoading()
        viewModelScope.launch {
            val businessId = business?.businessId ?: return@launch
            val payments = registerState.rows.map { row ->
                OrderPaymentSubmission(
                    methodCode = row.paymentMethodCode,
                    amountCents = OrderCxcValidators.parseCents(row.amountInput),
                    paymentDateIso = toPanamaDateTime(row.paymentDateIso),
                    applications = if (registerState.mode == RegisterPaymentMode.MANUAL) {
                        row.applications.mapNotNull { app ->
                            val termId = app.receivableTermId ?: return@mapNotNull null
                            ReceivableApplicationAllocation(
                                receivableTermId = termId,
                                amountCents = OrderCxcValidators.parseCents(app.amountInput)
                            )
                        }
                    } else {
                        emptyList()
                    }
                )
            }

            runCatching {
                orderService.registerOrderPayments(
                    businessId = businessId,
                    orderId = order.id,
                    payments = payments
                )
            }.onSuccess {
                analyticsService.logOrderPaymentSubmitSucceeded(mode = submitMode)
                refreshOrder(order.id)
                updateState { copy(registerPaymentState = registerPaymentState.copy(showSheet = false, errorMessage = null)) }
            }.onFailure { e ->
                analyticsService.logOrderPaymentSubmitFailed(
                    mode = submitMode,
                    errorCode = analyticsService.extractErrorCode(e)
                )
                updateState {
                    copy(
                        registerPaymentState = registerPaymentState.copy(
                            errorMessage = mapOrderMutationError(e, "No se pudo registrar el pago.")
                        )
                    )
                }
                showError()
            }
        }
    }

    fun openRescheduleSheet() {
        val order = uiState.value.order ?: return
        if (!order.supportsReceivableActions()) return
        if (totalOpenReceivableCents(order) <= 0L) return
        analyticsService.logOrderPaymentActionOpened(mode = "reschedule")
        updateState {
            copy(
                rescheduleState = RescheduleState(
                    showSheet = true,
                    terms = listOf(RescheduleTermState(id = 1)),
                    nextTermId = 2,
                    errorMessage = null
                )
            )
        }
    }

    fun closeRescheduleSheet() {
        updateState {
            copy(rescheduleState = rescheduleState.copy(showSheet = false, showConfirmDialog = false, errorMessage = null))
        }
    }

    fun addRescheduleTerm() {
        val current = uiState.value.rescheduleState
        val newTerm = RescheduleTermState(id = current.nextTermId)
        updateState {
            copy(
                rescheduleState = current.copy(
                    terms = current.terms + newTerm,
                    nextTermId = current.nextTermId + 1,
                    errorMessage = null
                )
            )
        }
    }

    fun removeRescheduleTerm(termId: Int) {
        val current = uiState.value.rescheduleState
        val updated = current.terms.filterNot { it.id == termId }
        updateState {
            copy(
                rescheduleState = current.copy(
                    terms = updated.ifEmpty { listOf(RescheduleTermState(id = current.nextTermId)) },
                    nextTermId = if (updated.isEmpty()) current.nextTermId + 1 else current.nextTermId,
                    errorMessage = null
                )
            )
        }
    }

    fun updateRescheduleTermDate(termId: Int, dueDateIso: String) {
        val current = uiState.value.rescheduleState
        updateState {
            copy(
                rescheduleState = current.copy(
                    terms = current.terms.map { term ->
                        if (term.id == termId) term.copy(dueDateIso = dueDateIso) else term
                    },
                    errorMessage = null
                )
            )
        }
    }

    fun updateRescheduleTermAmount(termId: Int, amountInput: String) {
        val current = uiState.value.rescheduleState
        updateState {
            copy(
                rescheduleState = current.copy(
                    terms = current.terms.map { term ->
                        if (term.id == termId) term.copy(amountInput = amountInput.filter(Char::isDigit)) else term
                    },
                    errorMessage = null
                )
            )
        }
    }

    fun requestRescheduleConfirmation() {
        val order = uiState.value.order ?: return
        if (!order.supportsReceivableActions()) return
        val state = uiState.value.rescheduleState
        val validation = OrderCxcValidators.validateReschedule(
            terms = state.terms,
            totalOpenCents = totalOpenReceivableCents(order)
        )
        if (validation != null) {
            updateState { copy(rescheduleState = rescheduleState.copy(errorMessage = validation, showConfirmDialog = false)) }
            return
        }
        updateState { copy(rescheduleState = rescheduleState.copy(showConfirmDialog = true, errorMessage = null)) }
    }

    fun dismissRescheduleConfirmation() {
        updateState { copy(rescheduleState = rescheduleState.copy(showConfirmDialog = false)) }
    }

    fun confirmReschedule() {
        val state = uiState.value
        val order = state.order ?: return
        if (!order.supportsReceivableActions()) return
        val rescheduleState = state.rescheduleState
        val businessId = business?.businessId ?: return

        analyticsService.logOrderPaymentSubmitAttempted(mode = "reschedule")
        showLoading()
        viewModelScope.launch {
            val sourceIds = openReceivableTerms(order).map { it.id }
            val terms = rescheduleState.terms.map { term ->
                ReceivableRescheduleTerm(
                    dueDateIso = toPanamaDateTime(term.dueDateIso),
                    amountCents = OrderCxcValidators.parseCents(term.amountInput)
                )
            }

            runCatching {
                orderService.rescheduleOrderReceivables(
                    businessId = businessId,
                    orderId = order.id,
                    sourceTermIds = sourceIds,
                    newTerms = terms
                )
            }.onSuccess {
                analyticsService.logOrderPaymentSubmitSucceeded(mode = "reschedule")
                refreshOrder(order.id)
                updateState {
                    copy(
                        rescheduleState = this.rescheduleState.copy(
                            showSheet = false,
                            showConfirmDialog = false,
                            errorMessage = null
                        )
                    )
                }
            }.onFailure { e ->
                analyticsService.logOrderPaymentSubmitFailed(
                    mode = "reschedule",
                    errorCode = analyticsService.extractErrorCode(e)
                )
                updateState {
                    copy(
                        rescheduleState = this.rescheduleState.copy(
                            errorMessage = mapOrderMutationError(e, "No se pudo reprogramar las cuotas."),
                            showConfirmDialog = false
                        )
                    )
                }
                showError()
            }
        }
    }

    fun openVoidPaymentSheet(paymentId: Long) {
        val payment = uiState.value.order
            ?.orderPayments
            ?.firstOrNull { it.id == paymentId }
        if (payment?.isAutomatic == true) return

        analyticsService.logOrderPaymentActionOpened(mode = "void")
        updateState {
            copy(
                voidPaymentState = VoidPaymentState(
                    showSheet = true,
                    paymentId = paymentId,
                    reason = "",
                    errorMessage = null
                )
            )
        }
    }

    fun closeVoidPaymentSheet() {
        updateState { copy(voidPaymentState = voidPaymentState.copy(showSheet = false, errorMessage = null)) }
    }

    fun onVoidReasonChanged(value: String) {
        updateState { copy(voidPaymentState = voidPaymentState.copy(reason = value, errorMessage = null)) }
    }

    fun confirmVoidPayment() {
        val state = uiState.value
        val paymentId = state.voidPaymentState.paymentId ?: return
        val reason = state.voidPaymentState.reason.trim()
        val order = state.order ?: return

        val reasonValidation = OrderCxcValidators.validateVoidReason(reason)
        if (reasonValidation != null) {
            updateState { copy(voidPaymentState = voidPaymentState.copy(errorMessage = reasonValidation)) }
            return
        }

        val businessId = business?.businessId ?: return
        analyticsService.logOrderPaymentSubmitAttempted(mode = "void")
        showLoading()
        viewModelScope.launch {
            runCatching {
                orderService.voidOrderPayment(
                    businessId = businessId,
                    paymentId = paymentId,
                    reason = reason
                )
            }.onSuccess {
                analyticsService.logOrderPaymentSubmitSucceeded(mode = "void")
                refreshOrder(order.id)
                updateState { copy(voidPaymentState = voidPaymentState.copy(showSheet = false, errorMessage = null)) }
            }.onFailure { e ->
                analyticsService.logOrderPaymentSubmitFailed(
                    mode = "void",
                    errorCode = analyticsService.extractErrorCode(e)
                )
                updateState {
                    copy(
                        voidPaymentState = voidPaymentState.copy(
                            errorMessage = mapOrderMutationError(e, "No se pudo anular el pago.")
                        )
                    )
                }
                showError()
            }
        }
    }

    private fun normalizeAchStatus(rawStatus: String?): String {
        return rawStatus.orEmpty()
            .trim()
            .lowercase()
            .replace('-', '_')
            .replace(' ', '_')
    }

    private fun defaultAchRejectReasonText(reasonCode: String): String {
        return when (reasonCode) {
            "fraud" -> "Comprobante de pago fraudulento"
            "invalid_proof" -> "Comprobante de pago inválido"
            "amount_mismatch" -> "Monto del comprobante no coincide con la orden"
            "reference_mismatch" -> "Referencia del comprobante no coincide con la orden"
            "other" -> ""
            else -> reasonCode
        }
    }

    private fun isHighRisk(detail: AchPaymentDetail?): Boolean {
        val safeDetail = detail ?: return false
        val riskLevel = safeDetail.riskLevel.orEmpty().trim().lowercase()
        val riskScore = safeDetail.riskScore ?: 0
        return riskLevel == "high" || riskScore >= 70
    }

    private suspend fun fetchAndCacheAchDetail(
        paymentIntentId: String,
        force: Boolean
    ): AchPaymentDetail? {
        val existing = achDetailState(paymentIntentId)
        if (!force && existing.detail != null) return existing.detail
        if (achDetailsInFlight.contains(paymentIntentId)) {
            return waitForAchDetailInFlight(paymentIntentId) ?: achDetailState(paymentIntentId).detail
        }

        achDetailsInFlight.add(paymentIntentId)
        updateState {
            copy(
                achIntentStates = achIntentStates + (
                    paymentIntentId to achDetailState(paymentIntentId).copy(
                        isLoading = true,
                        errorMessage = null
                    )
                )
            )
        }

        val businessId = business?.businessId ?: uiState.value.order?.businessId ?: -1
        if (businessId <= 0) {
            achDetailsInFlight.remove(paymentIntentId)
            updateState {
                copy(
                    achIntentStates = achIntentStates + (
                        paymentIntentId to achDetailState(paymentIntentId).copy(
                            isLoading = false,
                            errorMessage = "No se encontró el negocio activo para cargar el pago ACH."
                        )
                    )
                )
            }
            return null
        }

        return runCatching {
            withContext(Dispatchers.IO) {
                orderService.getAchPaymentByIntent(
                    businessId = businessId,
                    paymentIntentId = paymentIntentId
                )
            }
        }.onSuccess { detail ->
            updateState {
                copy(
                    achIntentStates = achIntentStates + (
                        paymentIntentId to AchIntentDetailState(
                            detail = detail,
                            isLoading = false,
                            errorMessage = null
                        )
                    )
                )
            }
        }.onFailure { error ->
            val message = mapOrderMutationError(error, "No se pudo cargar el detalle ACH.")
            updateState {
                copy(
                    achIntentStates = achIntentStates + (
                        paymentIntentId to achDetailState(paymentIntentId).copy(
                            isLoading = false,
                            errorMessage = message
                        )
                    )
                )
            }
        }.getOrNull().also {
            achDetailsInFlight.remove(paymentIntentId)
        }
    }

    private suspend fun waitForAchDetailInFlight(paymentIntentId: String): AchPaymentDetail? {
        repeat(40) {
            val current = achDetailState(paymentIntentId).detail
            if (current != null) return current
            if (!achDetailsInFlight.contains(paymentIntentId)) {
                return achDetailState(paymentIntentId).detail
            }
            delay(50)
        }
        return achDetailState(paymentIntentId).detail
    }

    private suspend fun prepareAchProofPreview(
        paymentIntentId: String,
        openSheet: Boolean
    ) {
        val detail = fetchAndCacheAchDetail(paymentIntentId, force = false)
            ?: achDetailState(paymentIntentId).detail
        if (detail == null) {
            updateState {
                copy(
                    achProofPreviewState = achProofPreviewState.copy(
                        show = if (openSheet) true else achProofPreviewState.show,
                        paymentIntentId = paymentIntentId,
                        isLoading = false,
                        errorMessage = "No se pudo cargar el comprobante ACH."
                    )
                )
            }
            return
        }

        if (normalizeAchStatus(detail.paymentStatus) == "rejected") {
            updateState {
                copy(
                    achProofPreviewState = achProofPreviewState.copy(
                        show = if (openSheet) true else achProofPreviewState.show,
                        paymentIntentId = paymentIntentId,
                        isLoading = false,
                        imageDataUri = null,
                        previewUrl = null,
                        contentType = null,
                        fileName = null,
                        errorMessage = "El comprobante no está disponible para pagos rechazados."
                    )
                )
            }
            return
        }

        val fallbackUrl = detail.proofFileUrl?.takeIf { it.isNotBlank() }
        val businessId = business?.businessId ?: uiState.value.order?.businessId ?: -1

        if (detail.paymentId == null || detail.proofId == null || businessId <= 0) {
            updateState {
                copy(
                    achProofPreviewState = achProofPreviewState.copy(
                        show = if (openSheet) true else achProofPreviewState.show,
                        paymentIntentId = paymentIntentId,
                        isLoading = false,
                        imageDataUri = null,
                        previewUrl = fallbackUrl,
                        contentType = detail.proofContentType,
                        fileName = detail.proofFileName,
                        errorMessage = if (fallbackUrl == null) "No hay comprobante disponible." else null
                    )
                )
            }
            return
        }

        val cachedBinary = achProofBinaryCache[paymentIntentId]
        val binary = if (cachedBinary != null) {
            cachedBinary
        } else {
            runCatching {
                withContext(Dispatchers.IO) {
                    val downloaded = orderService.downloadAchProofFile(
                        businessId = businessId,
                        paymentId = detail.paymentId,
                        proofId = detail.proofId
                    )
                    AchProofBinary(
                        bytes = downloaded.bytes,
                        contentType = downloaded.contentType ?: detail.proofContentType,
                        fileName = downloaded.fileName ?: detail.proofFileName
                    )
                }
            }.getOrNull()?.also { achProofBinaryCache[paymentIntentId] = it }
        }

        if (binary == null) {
            updateState {
                copy(
                    achProofPreviewState = achProofPreviewState.copy(
                        show = if (openSheet) true else achProofPreviewState.show,
                        paymentIntentId = paymentIntentId,
                        isLoading = false,
                        imageDataUri = null,
                        previewUrl = fallbackUrl,
                        contentType = detail.proofContentType,
                        fileName = detail.proofFileName,
                        errorMessage = if (fallbackUrl == null) "No se pudo obtener el comprobante ACH." else null
                    )
                )
            }
            return
        }

        val contentType = binary.contentType ?: detail.proofContentType
        val normalizedContentType = contentType.orEmpty().lowercase()
        val imageDataUri = if (normalizedContentType.startsWith("image/")) {
            @OptIn(ExperimentalEncodingApi::class)
            "data:${contentType ?: "image/jpeg"};base64,${Base64.encode(binary.bytes)}"
        } else {
            null
        }
        val pdfDataUri = if (normalizedContentType.contains("pdf")) {
            @OptIn(ExperimentalEncodingApi::class)
            "data:application/pdf;base64,${Base64.encode(binary.bytes)}"
        } else {
            null
        }

        updateState {
            copy(
                achProofPreviewState = achProofPreviewState.copy(
                    show = if (openSheet) true else achProofPreviewState.show,
                    paymentIntentId = paymentIntentId,
                    isLoading = false,
                    imageDataUri = imageDataUri,
                    previewUrl = when {
                        imageDataUri != null -> null
                        pdfDataUri != null -> pdfDataUri
                        else -> fallbackUrl
                    },
                    contentType = contentType,
                    fileName = binary.fileName ?: detail.proofFileName,
                    errorMessage = null
                )
            )
        }
    }

    private fun todayPanamaIso(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.of("America/Panama")).date
        val year = now.year.toString().padStart(4, '0')
        val month = now.monthNumber.toString().padStart(2, '0')
        val day = now.dayOfMonth.toString().padStart(2, '0')
        return "$year-$month-$day"
    }

    private fun toPanamaDateTime(dateIso: String): String {
        val safeDate = runCatching {
            LocalDate.parse(dateIso)
        }.getOrElse { Clock.System.now().toLocalDateTime(TimeZone.of("America/Panama")).date }
        val dateTime = LocalDateTime(safeDate, LocalTime(0, 0, 0))
        val year = dateTime.year.toString().padStart(4, '0')
        val month = dateTime.monthNumber.toString().padStart(2, '0')
        val day = dateTime.dayOfMonth.toString().padStart(2, '0')
        val hour = dateTime.hour.toString().padStart(2, '0')
        val minute = dateTime.minute.toString().padStart(2, '0')
        val second = dateTime.second.toString().padStart(2, '0')
        return "$year-$month-${day}T$hour:$minute:$second-05:00"
    }

    private fun mapOrderMutationError(
        error: Throwable,
        fallback: String,
        codeOverrides: Map<String, String> = emptyMap()
    ): String {
        return OrderMutationErrorMapper.messageFor(error, fallback, codeOverrides)
    }


    /// Payment methods sheet handling
    fun showManualPaymentSheet(show: Boolean) {
        if (show && !uiState.value.canMarkPaid) return
        if (show) {
            analyticsService.logOrderPaymentActionOpened(mode = "operational")
        }
        val order = uiState.value.order
        val orderTotalCents = order?.let { safeOrder ->
            totalOpenReceivableCents(safeOrder).takeIf { it > 0L } ?: safeOrder.totalAmount.toLongCents()
        }
        val current = uiState.value.manualPayment
        updateState {
            copy(
                manualPayment = current.copy(
                    showSheet = show,
                    // reset when opening; keep when closing
                    totalToChargeCents = orderTotalCents ?: current.totalToChargeCents,
                    methodOptions = ManualPaymentMethodOption.getAllOptions(),
                    charged = if (show) emptyMap() else current.charged,
                    otherPaymentDescription = if (show) "" else current.otherPaymentDescription,
                    errorMessage = null
                )
            )
        }
    }

    /* ---------- Interactions from the sheet ---------- */

    /** Toggle a payment method; if turning on, prefill with remaining amount. */
    fun onToggleMethod(code: Int, selected: Boolean) {
        val s = uiState.value
        val mp = s.manualPayment
        val nextCharged = mp.charged.toMutableMap()

        if (selected) {
            // Prefill with remaining at the moment of toggle
            val remaining = mp.remaining
            nextCharged[code] = remaining
        } else {
            nextCharged.remove(code)
        }
        updateState {
            copy(
                manualPayment = mp.copy(
                    charged = nextCharged,
                    errorMessage = null
                )
            )
        }
    }

    /** Set amount (in cents) for a selected method. */
    fun onAmountChange(code: Int, amountCents: Long) {
        val s = uiState.value
        val mp = s.manualPayment
        if (!mp.charged.containsKey(code)) return

        val nonNegative = amountCents.coerceAtLeast(0L)
        val currentAmount = mp.charged[code] ?: 0L
        val otherAllocated = (mp.allocated - currentAmount).coerceAtLeast(0L)
        val clamped = if (code == ManualPaymentMethodOption.CASH.id) {
            nonNegative
        } else {
            nonNegative.coerceAtMost((mp.totalToChargeCents - otherAllocated).coerceAtLeast(0L))
        }
        updateState {
            copy(
                manualPayment = mp.copy(
                    charged = mp.charged.toMutableMap().apply { this[code] = clamped }
                )
            )
        }
    }

    fun onOtherDescription(value: String) {
        updateState {
            copy(
                manualPayment = manualPayment.copy(otherPaymentDescription = value)
            )
        }
    }

    /* ---------- Confirm action ---------- */

    fun onConfirmManualPayment() {
        if (!uiState.value.canMarkPaid) return
        val state = _uiState.value
        val order = state.order ?: return
        val mp = state.manualPayment

        if (!mp.isConfirmEnabled) {
            updateState {
                copy(
                    manualPayment = mp.copy(errorMessage = "Verifica montos y usa una descripción de al menos 15 caracteres si seleccionas Otro.")
                )
            }
            return
        }

        analyticsService.logOrderPaymentSubmitAttempted(mode = "operational")
        updateState {
            copy(
                manualPayment = mp.copy(
                    showSheet = false,
                )
            )
        }
        showLoading()
        viewModelScope.launch {
            val state = uiState.value
            val businessId = state.order?.businessId ?: return@launch

            val allocations = mp.charged.entries
                .filter { it.value > 0L }
                .map { PaymentAllocation(methodCode = it.key, amountCents = it.value) }

            val otherDesc = if (mp.requiresOtherDesc) mp.otherPaymentDescription.trim()
                .ifBlank { null } else null

            val issueInvoice = state.invoicingEnabled

            val result = runCatching {
                releaseOpenIntentForReplacement(
                    businessId = businessId,
                    order = order,
                    reason = "customer_selected_cash",
                    expectedNextAction = "manual_payment_allowed",
                )
                orderService.registerManualPayment(
                    businessId = businessId,
                    orderId = order.id,
                    allocations = allocations,
                    otherDescription = otherDesc,
                    issueInvoice = issueInvoice
                )
            }

            result.onSuccess {
                analyticsService.logOrderPaymentSubmitSucceeded(mode = "operational")
                // Refresh order details if needed
                refreshOrder(order.id)
            }.onFailure { e ->
                analyticsService.logOrderPaymentSubmitFailed(
                    mode = "operational",
                    errorCode = analyticsService.extractErrorCode(e)
                )
                showError()
            }
        }
    }

    private suspend fun releaseOpenIntentForReplacement(
        businessId: Int,
        order: Order,
        reason: String,
        expectedNextAction: String,
    ) {
        val sourceMethod = if (order.paymentFlowType.equals("yappy_onsite", ignoreCase = true) ||
            order.paymentFlowType.equals("in_place", ignoreCase = true)
        ) {
            "yappy_onsite"
        } else {
            "payment_link"
        }
        val hasOpenExternalIntent = PaymentLinkResolver.hasOpenLink(order) ||
            (
                sourceMethod == "yappy_onsite" &&
                    order.status != OrderStatus.CANCELLED &&
                    order.paymentStatus != PaymentStatus.PAID.id &&
                    order.paymentStatus != PaymentStatus.CANCELLED.id
                )
        if (!hasOpenExternalIntent) return

        val released = orderService.releasePendingPaymentIntent(
            businessId = businessId,
            orderId = order.id,
            paymentMethod = sourceMethod,
            reason = reason,
        )
        if (!released.released || !released.allowsNextAction(expectedNextAction)) {
            error("No se pudo liberar el cobro pendiente para cambiar el método de pago.")
        }
    }

    fun trackOrderPaymentLinkAction(action: String) {
        analyticsService.logOrderPaymentLinkAction(actionValue = action)
    }

    fun refreshOrder(orderId: Int) {
        viewModelScope.launch {
            runCatching {
                val businessId = business?.businessId ?: error("No business ID")
                orderService.refreshOrder(businessId, orderId)
            }.onSuccess { fresh ->
                updateState { copy(order = fresh) }
                showSuccess()
            }.onFailure {
                showError()
            }
        }
    }

    /* ---------- Utilities ---------- */

    fun seedTotalFromOrder() {
        val total = _uiState.value.order?.totalAmount?.toLongCents() ?: 0L
        updateState {
            copy(
                manualPayment = manualPayment.copy(totalToChargeCents = total)
            )
        }
    }
}

internal const val RETRY_INVOICE_PENDING_VERIFICATION_MESSAGE: String =
    "La facturación se está verificando. Revisa el estado de la orden en unos minutos."
private const val REPLACEMENT_YAPPY_ONSITE_POLL_MS: Long = 1_500L

internal sealed interface RetryInvoiceFeedback {
    data object Success : RetryInvoiceFeedback
    data class Warning(val message: String) : RetryInvoiceFeedback
}

internal fun resolveRetryInvoiceFeedback(
    freshOrder: Order?,
    retryResponse: RetryInvoiceResponse,
): RetryInvoiceFeedback {
    val issued = freshOrder?.invoiceStatus == InvoiceStatus.ISSUED.id &&
        !freshOrder.externalInvoiceNumber.isNullOrBlank()
    if (issued) return RetryInvoiceFeedback.Success

    val warningMessage = retryResponse.invoiceWarningMessage
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: RETRY_INVOICE_PENDING_VERIFICATION_MESSAGE

    return RetryInvoiceFeedback.Warning(warningMessage)
}

internal fun shouldShowRetryInvoiceButton(order: Order?): Boolean {
    val safeOrder = order ?: return false
    if (safeOrder.status == OrderStatus.CANCELLED) return false

    val isPaid = safeOrder.paymentStatus == PaymentStatus.PAID.id
    val invoiceStatus = safeOrder.invoiceStatus ?: InvoiceStatus.NONE.id
    val isInvoiceAttemptedButNotIssued = invoiceStatus == InvoiceStatus.PENDING.id ||
        invoiceStatus == InvoiceStatus.FAILED.id

    return isPaid && isInvoiceAttemptedButNotIssued
}
