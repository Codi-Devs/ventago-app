package com.teco.ventago.features.orders.domain.models.responses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InvoiceDocsDto(
    @SerialName("invoice_id") val invoiceId: Long? = null,
    val cufe: String? = null,
    @SerialName("pdf_base64") val pdfBase64: String? = null,
    @SerialName("xml_base64") val xmlBase64: String? = null,
    @SerialName("document_kind") val documentKind: String? = null,
    @SerialName("order_id") val orderId: Long? = null
)