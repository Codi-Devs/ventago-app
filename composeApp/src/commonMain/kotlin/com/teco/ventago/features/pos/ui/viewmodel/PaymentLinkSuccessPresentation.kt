package com.teco.ventago.features.pos.ui.viewmodel

import com.teco.ventago.features.invoicing.domain.models.InvoiceStatus

internal data class PaymentLinkSuccessPresentation(
    val awaitingElectronicInvoice: Boolean,
    val showGeneratingInvoice: Boolean,
    val showManualInvoiceRequired: Boolean,
    val completedInternalDocument: Boolean,
)

/**
 * A paid internal document never becomes an electronic invoice. Success must
 * finish when the payment is detected instead of waiting on invoice status.
 */
internal fun paymentLinkSuccessPresentation(
    isPaymentLink: Boolean,
    paymentDetected: Boolean,
    invoiceStatus: InvoiceStatus,
    autoInvoiceOnPaymentSuccess: Boolean,
    internalDocument: Boolean,
): PaymentLinkSuccessPresentation {
    val invoiceSettled = invoiceStatus == InvoiceStatus.ISSUED || invoiceStatus == InvoiceStatus.FAILED
    val completedInternalDocument = isPaymentLink &&
        internalDocument &&
        paymentDetected &&
        !invoiceSettled
    val awaitingElectronicInvoice = isPaymentLink &&
        paymentDetected &&
        !invoiceSettled &&
        !completedInternalDocument
    return PaymentLinkSuccessPresentation(
        awaitingElectronicInvoice = awaitingElectronicInvoice,
        showGeneratingInvoice = awaitingElectronicInvoice && autoInvoiceOnPaymentSuccess,
        showManualInvoiceRequired = awaitingElectronicInvoice && !autoInvoiceOnPaymentSuccess,
        completedInternalDocument = completedInternalDocument,
    )
}
