package com.teco.ventago.features.orders.ui.order_details.viewModel

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.core.PdfSharer
import com.teco.ventago.core.authz.ActionKey
import com.teco.ventago.core.authz.AuthzEvaluator
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.business.domain.model.Business
import com.teco.ventago.features.financialProfile.domain.FinancialProfileService
import com.teco.ventago.features.invoicing.domain.models.InvoiceStatus
import com.teco.ventago.features.orders.domain.OrderService
import com.teco.ventago.features.orders.domain.OrderPaymentSubmission
import com.teco.ventago.features.orders.domain.PaymentAllocation
import com.teco.ventago.features.orders.domain.ReceivableApplicationAllocation
import com.teco.ventago.features.orders.domain.ReceivableRescheduleTerm
import com.teco.ventago.features.orders.domain.models.ManualPaymentMethodOption
import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.orders.domain.models.PaymentStatus
import com.teco.ventago.features.orders.domain.models.ReceivableTermDto
import com.teco.ventago.features.orders.domain.models.OrderStatus
import com.teco.ventago.utils.doubleTryParse
import com.teco.ventago.utils.toLongCents
import com.teco.ventago.viewModels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
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
    private val businessService: BusinessService,
    private val financialProfileService: FinancialProfileService,
    private val authService: IAuthService,
    private val pdfSharer: PdfSharer,
) : BaseViewModel<OrderDetailsState, OrderDetailsUiEvent>(OrderDetailsState()) {

    var business: Business? = null

    init {
        viewModelScope.launch {
            authService.getUser().collect { user ->
                updateState {
                    copy(
                        canMarkPaid = AuthzEvaluator.canAction(
                            ActionKey.ORDERS_MARK_PAID,
                            user,
                            emptySet()
                        )
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
        if (!uiState.value.canMarkPaid) return
        val order = uiState.value.order ?: return
        order.paymentLink?.let { link ->
            updateState {
                copy(
                    paymentLink = link,
                    showPaymentLinkSheet = true
                )
            }
        }
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
            copy(paymentLink = null, errorLoadingPaymentLink = false)
        }
    }

    fun cancelOrder(reason: String) {
        val order = uiState.value.order ?: return
        val businessId = business?.businessId ?: return
        showLoading()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val response = orderService.cancelOrder(
                        businessId,
                        order.id,
                        reason,
                        authService.getUserSync()?.name ?: "App"
                    )
                    withContext(Dispatchers.Main) {
                        if (response) {
                            refreshOrder(order.id)
                        } else {
                            showError()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
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
            withContext(Dispatchers.IO) {
                try {
                    val response = orderService.retryElectronicInvoice(
                        businessId,
                        order.id
                    )
                    withContext(Dispatchers.Main) {
                        if (response.invoiceStatus == InvoiceStatus.ISSUED.id) {
                            refreshOrder(order.id)
                        } else {
                            showError()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        showError()
                    }
                }
            }
        }
    }

    fun canInvoiceDraftOrder(order: Order? = uiState.value.order): Boolean {
        val safeOrder = order ?: return false
        val invoiceStatus = safeOrder.invoiceStatus ?: InvoiceStatus.NONE.id
        val isDraftNotInvoiced = safeOrder.status == OrderStatus.DRAFT &&
                (invoiceStatus == InvoiceStatus.NONE.id || invoiceStatus == InvoiceStatus.PENDING.id)
        val hasOutstandingPayment = safeOrder.paymentStatus != PaymentStatus.PAID.id
        return uiState.value.canMarkPaid &&
                isDraftNotInvoiced &&
                hasOutstandingPayment &&
                safeOrder.totalAmount.toLongCents() > 0L
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
        return nonCancelledReceivableTerms(order).sumOf { it.openAmount.toLongCents() }
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
        if (order.status == OrderStatus.CANCELLED || order.invoiceStatus != InvoiceStatus.ISSUED.id) return
        if (totalOpenReceivableCents(order) <= 0L) return

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
                refreshOrder(order.id)
                updateState { copy(registerPaymentState = registerPaymentState.copy(showSheet = false, errorMessage = null)) }
            }.onFailure { e ->
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
        if (totalOpenReceivableCents(order) <= 0L) return
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
        val rescheduleState = state.rescheduleState
        val businessId = business?.businessId ?: return

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
        showLoading()
        viewModelScope.launch {
            runCatching {
                orderService.voidOrderPayment(
                    businessId = businessId,
                    paymentId = paymentId,
                    reason = reason
                )
            }.onSuccess {
                refreshOrder(order.id)
                updateState { copy(voidPaymentState = voidPaymentState.copy(showSheet = false, errorMessage = null)) }
            }.onFailure { e ->
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

    private fun mapOrderMutationError(error: Throwable, fallback: String): String {
        val raw = error.message.orEmpty()
        if (raw.contains("O_RP_002")) {
            return "La suma de los nuevos vencimientos debe coincidir exactamente con el saldo abierto total."
        }
        val errorCode = Regex("\"error\":\"([^\"]+)\"").find(raw)?.groupValues?.getOrNull(1)
            ?: Regex("\"errorCode\":\"([^\"]+)\"").find(raw)?.groupValues?.getOrNull(1)
        return if (!errorCode.isNullOrBlank() && errorCode != "null") {
            "$fallback Código: $errorCode"
        } else {
            fallback
        }
    }


    /// Payment methods sheet handling
    fun showManualPaymentSheet(show: Boolean) {
        if (show && !uiState.value.canMarkPaid) return
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

        val clamped = amountCents.coerceAtLeast(0L)
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
                    manualPayment = mp.copy(errorMessage = "Verifica montos y descripción.")
                )
            }
            return
        }

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
                orderService.registerManualPayment(
                    businessId = businessId,
                    orderId = order.id,
                    allocations = allocations,
                    otherDescription = otherDesc,
                    issueInvoice = issueInvoice
                )
            }

            result.onSuccess {
                // Refresh order details if needed
                refreshOrder(order.id)
            }.onFailure { e ->
                showError()
            }
        }
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
