package com.teco.ventago.features.orders.domain.models.responses

import com.teco.ventago.features.printers.domain.model.TicketDocumentPayload
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

const val YAPPY_ONSITE_PENDING_TRANSACTION_EXISTS = "yappy_onsite_pending_transaction_exists"

@Serializable
data class YappyOnsitePendingTransactionDto(
    @SerialName("billing_point")
    val billingPoint: String = "",
    @SerialName("branch_code")
    val branchCode: String = "",
    @SerialName("expires_at")
    val expiresAt: String = "",
    @SerialName("session_id")
    val sessionId: String = "",
    @SerialName("transaction_id")
    val transactionId: String = "",
)

class YappyOnsitePendingTransactionExistsException(
    val pendingTransaction: YappyOnsitePendingTransactionDto,
) : Exception(YAPPY_ONSITE_PENDING_TRANSACTION_EXISTS)

@OptIn(ExperimentalSerializationApi::class)
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

    @SerialName("payment_flow_type")
    val paymentFlowType: String? = null,

    @SerialName("payment_status")
    val paymentStatus: Int,

    @SerialName("invoice_status")
    val invoiceStatus: Int,

    @JsonNames("invoiceWarningCode")
    @SerialName("invoice_warning_code")
    val invoiceWarningCode: String? = null,

    @JsonNames("invoiceWarningMessage")
    @SerialName("invoice_warning_message")
    val invoiceWarningMessage: String? = null,

    @SerialName("links")
    val links: List<CreateOrderLinkDto>? = null,

    @SerialName("invoice_files")
    val invoiceFiles: InvoiceFilesDto? = null,

    @SerialName("onsite_payment")
    val onsitePayment: OnsitePaymentDto? = null,
)


@Serializable
data class CreateOrderLinkDto(
    @SerialName("url")
    val url: String = "",

    @SerialName("action")
    val action: String = "",

    @SerialName("status")
    val status: String? = null,

    @SerialName("link")
    val link: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("expires_at")
    val expiresAt: String? = null,
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

@Serializable
data class OnsitePaymentDto(
    @SerialName("id") val id: String? = null,
    @SerialName("transaction_id") val transactionId: String = "",
    @SerialName("order_id") val orderId: Int? = null,
    @SerialName("session_id") val sessionId: String = "",
    @SerialName("qr_hash") val qrHash: String = "",
    @SerialName("qr_type") val qrType: String = "",
    @SerialName("status") val status: String = "",
    @SerialName("provider_status") val providerStatus: String = "",
    @SerialName("amount") val amount: String = "",
    @SerialName("currency") val currency: String = "USD",
    @SerialName("expires_at") val expiresAt: String? = null,
)
