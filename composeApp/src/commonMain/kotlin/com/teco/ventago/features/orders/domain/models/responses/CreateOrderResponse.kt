package com.teco.ventago.features.orders.domain.models.responses

import com.teco.ventago.features.printers.domain.model.TicketDocumentPayload
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateOrderResponse(
    @SerialName("id")
    val id: Int,

    @SerialName("order_number")
    val orderNumber: String,

    @SerialName("order_date")
    val orderDate: String, // ISO-8601 string (e.g., "2025-10-21T19:13:28.932105-05:00")

    @SerialName("order_amount")
    val orderAmount: String, // use BigDecimal or Double if you want numeric

    @SerialName("tax_amount")
    val taxAmount: String,

    @SerialName("payment_status")
    val paymentStatus: Int,

    @SerialName("invoice_status")
    val invoiceStatus: Int,

    @SerialName("links")
    val links: List<CreateOrderLinkDto>? = null,

    @SerialName("invoice_files")
    val invoiceFiles: InvoiceFilesDto? = null
)


@Serializable
data class CreateOrderLinkDto(
    @SerialName("url")
    val url: String,

    @SerialName("action")
    val action: String
)

@Serializable
data class InvoiceFilesDto(
    @SerialName("PDF")
    val pdf: String? = null,

    @SerialName("XML")
    val xml: String? = null,

    @SerialName("TICKET")
    val ticket: TicketDocumentPayload? = null
)
