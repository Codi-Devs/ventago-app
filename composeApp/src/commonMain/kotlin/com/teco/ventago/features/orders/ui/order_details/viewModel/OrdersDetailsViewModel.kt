package com.teco.ventago.features.orders.ui.order_details.viewModel

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.core.PdfSharer
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.business.domain.model.Business
import com.teco.ventago.features.financialProfile.domain.FinancialProfileService
import com.teco.ventago.features.invoicing.domain.models.InvoiceStatus
import com.teco.ventago.features.orders.domain.OrderService
import com.teco.ventago.features.orders.domain.PaymentAllocation
import com.teco.ventago.features.orders.domain.models.ManualPaymentMethodOption
import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.orders.domain.models.OrderStatus
import com.teco.ventago.utils.doubleTryParse
import com.teco.ventago.utils.toLongCents
import com.teco.ventago.viewModels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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


    /// Payment methods sheet handling
    fun showManualPaymentSheet(show: Boolean) {
        val orderTotalCents = uiState.value.order?.totalAmount?.toLongCents()
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