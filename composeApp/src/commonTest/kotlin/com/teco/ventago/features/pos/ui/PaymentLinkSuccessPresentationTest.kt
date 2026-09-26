package com.teco.ventago.features.pos.ui

import com.teco.ventago.features.invoicing.domain.models.InvoiceStatus
import com.teco.ventago.features.pos.ui.viewmodel.paymentLinkSuccessPresentation
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PaymentLinkSuccessPresentationTest {

    @Test
    fun paidInternalDocumentDoesNotWaitForElectronicInvoice() {
        val presentation = paymentLinkSuccessPresentation(
            isPaymentLink = true,
            paymentDetected = true,
            invoiceStatus = InvoiceStatus.NONE,
            autoInvoiceOnPaymentSuccess = true,
            internalDocument = true,
        )

        assertTrue(presentation.completedInternalDocument)
        assertFalse(presentation.showGeneratingInvoice)
        assertFalse(presentation.showManualInvoiceRequired)
        assertFalse(presentation.awaitingElectronicInvoice)
    }

    @Test
    fun unpaidInternalPaymentLinkStillWaitsForThePayment() {
        val presentation = paymentLinkSuccessPresentation(
            isPaymentLink = true,
            paymentDetected = false,
            invoiceStatus = InvoiceStatus.NONE,
            autoInvoiceOnPaymentSuccess = true,
            internalDocument = true,
        )

        assertFalse(presentation.completedInternalDocument)
        assertFalse(presentation.showGeneratingInvoice)
    }

    @Test
    fun electronicPaymentLinkKeepsGeneratingInvoiceUntilIssued() {
        val pending = paymentLinkSuccessPresentation(
            isPaymentLink = true,
            paymentDetected = true,
            invoiceStatus = InvoiceStatus.PENDING,
            autoInvoiceOnPaymentSuccess = true,
            internalDocument = false,
        )
        val issued = paymentLinkSuccessPresentation(
            isPaymentLink = true,
            paymentDetected = true,
            invoiceStatus = InvoiceStatus.ISSUED,
            autoInvoiceOnPaymentSuccess = true,
            internalDocument = false,
        )

        assertTrue(pending.showGeneratingInvoice)
        assertFalse(pending.completedInternalDocument)
        assertFalse(issued.showGeneratingInvoice)
        assertFalse(issued.awaitingElectronicInvoice)
    }

    @Test
    fun electronicPaymentLinkWithoutAutoInvoiceAsksForManualInvoice() {
        val presentation = paymentLinkSuccessPresentation(
            isPaymentLink = true,
            paymentDetected = true,
            invoiceStatus = InvoiceStatus.NONE,
            autoInvoiceOnPaymentSuccess = false,
            internalDocument = false,
        )

        assertTrue(presentation.showManualInvoiceRequired)
        assertFalse(presentation.showGeneratingInvoice)
        assertFalse(presentation.completedInternalDocument)
    }
}
