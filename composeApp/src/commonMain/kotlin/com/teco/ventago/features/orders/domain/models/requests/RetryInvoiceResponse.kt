package com.teco.ventago.features.orders.domain.models.requests

import com.teco.ventago.features.orders.domain.models.responses.InvoiceFilesDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class RetryInvoiceResponse(
    @SerialName("order_id") val orderId: Long = 0L,
    @SerialName("order_number") val orderNumber: String = "",
    @SerialName("invoice_status") val invoiceStatus: Int = 0,
    @SerialName("cufe") val cufe: String = "",
    @SerialName("invoice_id") val invoiceId: String = "",
    @SerialName("invoice_warning_message") val invoiceWarningMessage: String? = null,
    @SerialName("invoice_files") val invoiceFiles: InvoiceFilesDto? = null
)
