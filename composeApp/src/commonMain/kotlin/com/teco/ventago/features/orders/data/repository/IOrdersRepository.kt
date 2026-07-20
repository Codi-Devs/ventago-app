package com.teco.ventago.features.orders.data.repository

import com.teco.ventago.features.orders.domain.models.ChangeOrderStatusResponse
import com.teco.ventago.features.orders.domain.models.AchPaymentDetail
import com.teco.ventago.features.orders.domain.models.AchProofFileDownload
import com.teco.ventago.features.orders.domain.models.IsBusinessRegisteredResponse
import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.orders.domain.models.requests.CancelOrderRequest
import com.teco.ventago.features.orders.domain.models.requests.CreatePaymentLinkRequest
import com.teco.ventago.features.orders.domain.models.requests.CreateOrderRequest
import com.teco.ventago.features.orders.domain.models.requests.DeleteOrderRequest
import com.teco.ventago.features.orders.domain.models.requests.ListOrdersRequest
import com.teco.ventago.features.orders.domain.models.requests.RejectAchPaymentRequest
import com.teco.ventago.features.orders.domain.models.requests.RescheduleReceivablesRequest
import com.teco.ventago.features.orders.domain.models.requests.RescheduleReceivablesResponse
import com.teco.ventago.features.orders.domain.models.requests.RegisterManualPaymentsDataResponse
import com.teco.ventago.features.orders.domain.models.requests.RegisterManualPaymentsRequest
import com.teco.ventago.features.orders.domain.models.requests.RetryInvoiceResponse
import com.teco.ventago.features.orders.domain.models.requests.VoidOrderPaymentRequest
import com.teco.ventago.features.orders.domain.models.requests.VoidOrderPaymentResponse
import com.teco.ventago.features.orders.domain.models.responses.CreateOrderResponse
import com.teco.ventago.features.orders.domain.models.responses.InvoiceDocsDto
import com.teco.ventago.core.Paged


interface IOrdersRepository {

    suspend fun isBusinessRegistered(businessId: Int): IsBusinessRegisteredResponse

    suspend fun loadOrders(
        request: ListOrdersRequest
    ): List<Order>

    suspend fun loadOrdersPaged(
        request: ListOrdersRequest
    ): Paged<Order>

    suspend fun changeOrderStatus(
        order: Order,
        status: Int,
        businessId: Int
    ): ChangeOrderStatusResponse

    suspend fun rejectOrder(
        order: Order,
        reason: String,
        businessId: Int
    ): ChangeOrderStatusResponse

    suspend fun changeOrdersEnabled(businessId: Int, enabled: Boolean): Boolean

    suspend fun createOrder(
        businessId: Int,
        createOrderRequest: CreateOrderRequest
    ): CreateOrderResponse

    suspend fun cancelOrder(businessId: Int, request: CancelOrderRequest): Boolean
    suspend fun deleteOrder(businessId: Int, request: DeleteOrderRequest): Boolean

    suspend fun registerManualPayments(
        businessId: Int,
        orderId: Int,
        request: RegisterManualPaymentsRequest
    ): RegisterManualPaymentsDataResponse
    suspend fun rescheduleOrderReceivables(
        businessId: Int,
        orderId: Int,
        request: RescheduleReceivablesRequest
    ): RescheduleReceivablesResponse
    suspend fun voidOrderPayment(
        businessId: Int,
        paymentId: Long,
        request: VoidOrderPaymentRequest
    ): VoidOrderPaymentResponse

    suspend fun retryElectronicInvoice(
        businessId: Int,
        orderId: Int,
    ): RetryInvoiceResponse

    suspend fun getInvoiceDocsRaw(businessId: Int, cufe: String): InvoiceDocsDto
    suspend fun createPaymentLink(businessId: Int, request: CreatePaymentLinkRequest): String?
    suspend fun releasePendingPaymentIntent(
        businessId: Int,
        orderId: Int,
        request: com.teco.ventago.features.orders.domain.models.requests.PendingIntentReleaseRequest
    ): com.teco.ventago.features.orders.domain.models.requests.PendingIntentReleaseResponse
    suspend fun createPendingPaymentIntent(
        businessId: Int,
        orderId: Int,
        request: com.teco.ventago.features.orders.domain.models.requests.PendingIntentCreateRequest
    ): com.teco.ventago.features.orders.domain.models.requests.PendingIntentCreateResponse
    suspend fun getAchPaymentByIntent(businessId: Int, paymentIntentId: String): AchPaymentDetail
    suspend fun approveAchPayment(businessId: Int, paymentIntentId: String): String
    suspend fun rejectAchPayment(
        businessId: Int,
        paymentIntentId: String,
        request: RejectAchPaymentRequest
    ): String
    suspend fun downloadAchProofFile(
        businessId: Int,
        paymentId: String,
        proofId: String
    ): AchProofFileDownload

    suspend fun removeCustomer(businessId: Int, customerId: Int): Boolean

    suspend fun getOrderPaymentLink(orderID: Int): String?

    suspend fun registerManualTransference(
        businessId: Int,
        orderNumber: String,
        paymentReference: String,
        description: String
    ): Boolean

    suspend fun findOrderByOrderNumber(businessId: Int, orderNumber: String): Order
    suspend fun findOrderById(businessId: Int, orderId: Int): Order

    suspend fun findOrderByCUFE(businessId: Int, cufe: String): Order
}
