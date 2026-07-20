package com.teco.ventago.features.orders.ui.order_details.viewModel

import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.orders.domain.models.OrderStatus
import com.teco.ventago.features.orders.domain.models.PaymentStatus
import com.teco.ventago.features.orders.domain.models.requests.RetryInvoiceResponse
import com.teco.ventago.features.invoicing.domain.models.InvoiceStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OrdersDetailsViewModelCxcTest {

    @Test
    fun validateRegisterPaymentRejectsFutureDate() {
        val result = OrderCxcValidators.validateRegisterPayment(
            mode = RegisterPaymentMode.AUTOMATIC,
            rows = listOf(
                RegisterPaymentRowState(
                    id = 1,
                    amountInput = "10000",
                    paymentDateIso = "2026-03-29"
                )
            ),
            openBalanceCents = 20000L,
            openByTermCents = emptyMap(),
            todayIso = "2026-03-28"
        )

        assertNotNull(result)
        assertTrue(result.contains("no puede ser futura"))
    }

    @Test
    fun validateRegisterPaymentManualRequiresApplicationsSumToMatchPayment() {
        val result = OrderCxcValidators.validateRegisterPayment(
            mode = RegisterPaymentMode.MANUAL,
            rows = listOf(
                RegisterPaymentRowState(
                    id = 1,
                    amountInput = "10000",
                    paymentDateIso = "2026-03-28",
                    applications = listOf(
                        RegisterPaymentApplicationState(
                            id = 1,
                            receivableTermId = 9001L,
                            amountInput = "9000"
                        )
                    )
                )
            ),
            openBalanceCents = 20000L,
            openByTermCents = mapOf(9001L to 15000L),
            todayIso = "2026-03-28"
        )

        assertNotNull(result)
        assertTrue(result.contains("debe coincidir"))
    }

    @Test
    fun validateRegisterPaymentManualRejectsWhenAssignedAmountExceedsTermOpen() {
        val result = OrderCxcValidators.validateRegisterPayment(
            mode = RegisterPaymentMode.MANUAL,
            rows = listOf(
                RegisterPaymentRowState(
                    id = 1,
                    amountInput = "6000",
                    paymentDateIso = "2026-03-28",
                    applications = listOf(
                        RegisterPaymentApplicationState(
                            id = 1,
                            receivableTermId = 9001L,
                            amountInput = "6000"
                        )
                    )
                )
            ),
            openBalanceCents = 10000L,
            openByTermCents = mapOf(9001L to 5000L),
            todayIso = "2026-03-28"
        )

        assertNotNull(result)
        assertTrue(result.contains("excede su saldo adeudado"))
    }

    @Test
    fun validateRescheduleRequiresExactAmountSum() {
        val result = OrderCxcValidators.validateReschedule(
            terms = listOf(
                RescheduleTermState(id = 1, dueDateIso = "2026-04-15", amountInput = "10000"),
                RescheduleTermState(id = 2, dueDateIso = "2026-05-15", amountInput = "5000"),
            ),
            totalOpenCents = 20000L
        )

        assertEquals(
            "La suma de los nuevos vencimientos debe coincidir exactamente con el saldo abierto total.",
            result
        )
    }

    @Test
    fun validateVoidReasonRequiresNonBlankValue() {
        val blank = OrderCxcValidators.validateVoidReason("   ")
        val ok = OrderCxcValidators.validateVoidReason("Pago mal digitado")

        assertEquals("La razón es obligatoria.", blank)
        assertNull(ok)
    }

    @Test
    fun validateCancelOrderReasonRequiresAtLeastFifteenCharacters() {
        val blank = OrderCxcValidators.validateCancelOrderReason("   ")
        val short = OrderCxcValidators.validateCancelOrderReason("12345678901234")
        val valid = OrderCxcValidators.validateCancelOrderReason("123456789012345")

        assertEquals("La razón es obligatoria.", blank)
        assertEquals("La razón debe tener al menos 15 caracteres.", short)
        assertNull(valid)
    }

    @Test
    fun resolveRetryInvoiceFeedbackReturnsSuccessWhenRefreshedOrderIsIssuedWithCufe() {
        val result = resolveRetryInvoiceFeedback(
            freshOrder = sampleOrder(
                invoiceStatus = InvoiceStatus.ISSUED.id,
                externalInvoiceNumber = "CUFE-123"
            ),
            retryResponse = RetryInvoiceResponse(invoiceWarningMessage = "Pendiente")
        )

        assertEquals(RetryInvoiceFeedback.Success, result)
    }

    @Test
    fun resolveRetryInvoiceFeedbackUsesRetryWarningMessageBeforeGenericPendingMessage() {
        val result = resolveRetryInvoiceFeedback(
            freshOrder = sampleOrder(invoiceStatus = InvoiceStatus.PENDING.id),
            retryResponse = RetryInvoiceResponse(invoiceWarningMessage = "Respuesta backend")
        )

        assertEquals(RetryInvoiceFeedback.Warning("Respuesta backend"), result)
    }

    @Test
    fun resolveRetryInvoiceFeedbackFallsBackToGenericPendingVerificationMessage() {
        val result = resolveRetryInvoiceFeedback(
            freshOrder = sampleOrder(invoiceStatus = InvoiceStatus.PENDING.id),
            retryResponse = RetryInvoiceResponse(invoiceWarningMessage = "  ")
        )

        assertEquals(
            RetryInvoiceFeedback.Warning(RETRY_INVOICE_PENDING_VERIFICATION_MESSAGE),
            result
        )
    }

    @Test
    fun shouldShowRetryInvoiceButtonRequiresPaidPendingOrFailedInvoiceAndNotCancelled() {
        assertTrue(
            shouldShowRetryInvoiceButton(
                sampleOrder(
                    paymentStatus = PaymentStatus.PAID.id,
                    invoiceStatus = InvoiceStatus.NONE.id,
                    status = OrderStatus.CONFIRMED
                )
            )
        )

        assertEquals(
            false,
            shouldShowRetryInvoiceButton(
                sampleOrder(
                    paymentStatus = PaymentStatus.UNPAID.id,
                    invoiceStatus = InvoiceStatus.NONE.id,
                    status = OrderStatus.CONFIRMED
                )
            )
        )
        assertEquals(
            false,
            shouldShowRetryInvoiceButton(
                sampleOrder(
                    paymentStatus = PaymentStatus.PAID.id,
                    invoiceStatus = InvoiceStatus.ISSUED.id,
                    status = OrderStatus.CONFIRMED,
                    externalInvoiceNumber = "CUFE-123"
                )
            )
        )
        assertEquals(
            true,
            shouldShowRetryInvoiceButton(
                sampleOrder(
                    paymentStatus = PaymentStatus.PAID.id,
                    invoiceStatus = InvoiceStatus.FAILED.id,
                    status = OrderStatus.CONFIRMED
                )
            )
        )
        assertEquals(
            false,
            shouldShowRetryInvoiceButton(
                sampleOrder(
                    paymentStatus = PaymentStatus.PAID.id,
                    invoiceStatus = InvoiceStatus.PENDING.id,
                    status = OrderStatus.CANCELLED
                )
            )
        )
    }

    private fun sampleOrder(
        paymentStatus: Int = PaymentStatus.PAID.id,
        invoiceStatus: Int = InvoiceStatus.NONE.id,
        status: Int = OrderStatus.CONFIRMED,
        externalInvoiceNumber: String? = null
    ): Order {
        return Order(
            id = 1,
            orderType = "sale",
            businessId = 4,
            internalNumber = "ORD-4-0000-865-0000000594",
            externalInvoiceNumber = externalInvoiceNumber,
            invoiceStatus = invoiceStatus,
            currencyCode = "USD",
            subtotal = "1.50",
            discountTotal = "0.00",
            taxableBase = "1.50",
            taxTotal = "0.11",
            tipsTotal = "0.00",
            totalAmount = "1.61",
            status = status,
            paymentStatus = paymentStatus,
            createdAt = "2026-06-05T16:41:30.212815-05:00"
        )
    }
}
