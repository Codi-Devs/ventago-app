package com.teco.ventago.features.orders.domain.models.requests

import com.teco.ventago.features.invoicing.domain.models.InvoiceStatus
import com.teco.ventago.features.printers.domain.model.TicketDocumentPayload
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive


@Serializable
data class PaymentApplicationRequest(
    @SerialName("receivable_term_id") val receivableTermId: Long,
    @SerialName("amount") val amount: String
)

@Serializable
data class ManualPaymentItemRequest(
    @SerialName("type") val type: Int,
    @SerialName("amount") val amount: String,
    @SerialName("payment_date") val paymentDate: String,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("applications") val applications: List<PaymentApplicationRequest>? = null
)

@Serializable
data class RegisterManualPaymentsRequest(
    @SerialName("payments") val payments: List<ManualPaymentItemRequest>
)

@Serializable
data class RegisterManualPaymentsDataResponse(
    @SerialName("order_id") val orderId: Long,
    @SerialName("order_number") val orderNumber: String,
    @SerialName("payment_status") val paymentStatus: Int,
    @SerialName("invoice_status") val invoiceStatus: Int,
    @SerialName("invoiced") val invoiced: Boolean,
    @SerialName("invoice") val invoice: JsonObject? = null,
    @SerialName("order") val order: JsonObject? = null,
    @SerialName("ticket") val ticket: TicketDocumentPayload? = null
) {
    fun resolvedInvoiceStatus(
        freshOrderInvoiceStatus: Int?,
        freshOrderExternalInvoiceNumber: String? = null,
    ): Int {
        val nestedInvoiceStatus = invoice.intAt("status")
            ?: invoice.intAt("invoice_status")
            ?: invoice.intAt("invoiceStatus")
        val nestedOrderStatus = order.intAt("invoice_status")
            ?: order.intAt("invoiceStatus")
        val issuedEvidence = invoiced ||
            freshOrderExternalInvoiceNumber.isPresent() ||
            invoice.stringAt("cufe").isPresent() ||
            invoice.stringAt("external_invoice_number").isPresent() ||
            order.stringAt("external_invoice_number").isPresent()
        if (issuedEvidence || listOf(invoiceStatus, nestedInvoiceStatus, nestedOrderStatus, freshOrderInvoiceStatus)
                .any { it == InvoiceStatus.ISSUED.id }
        ) {
            return InvoiceStatus.ISSUED.id
        }

        return listOf(nestedInvoiceStatus, invoiceStatus, nestedOrderStatus, freshOrderInvoiceStatus)
            .firstOrNull { it != null && it != InvoiceStatus.NONE.id }
            ?: freshOrderInvoiceStatus
            ?: invoiceStatus
    }

    fun resolvedInvoiceCufe(freshOrderExternalInvoiceNumber: String?): String? {
        return freshOrderExternalInvoiceNumber?.takeIf { it.isNotBlank() }
            ?: invoice.stringAt("cufe")
            ?: invoice.stringAt("external_invoice_number")
            ?: order.stringAt("external_invoice_number")
    }

    fun resolvedTicketPayload(json: Json): TicketDocumentPayload? {
        return ticket ?: invoice?.get("ticket")?.let { element ->
            runCatching {
                json.decodeFromJsonElement<TicketDocumentPayload>(element)
            }.getOrNull()
        }
    }
}

private fun JsonObject?.intAt(key: String): Int? {
    return this?.get(key)?.jsonPrimitive?.intOrNull
}

private fun JsonObject?.stringAt(key: String): String? {
    return this?.get(key)?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }
}

private fun String?.isPresent(): Boolean {
    return !this.isNullOrBlank()
}
