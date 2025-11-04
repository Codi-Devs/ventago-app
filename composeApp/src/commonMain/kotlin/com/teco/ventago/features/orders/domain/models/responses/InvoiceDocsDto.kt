package com.teco.ventago.features.orders.domain.models.responses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InvoiceDocsDto(
    @SerialName("invoice_id") val invoiceId: Long,
    val cufe: String,
    @SerialName("pdf_base64") val pdfBase64: String? = null,
    @SerialName("xml_base64") val xmlBase64: String? = null
)