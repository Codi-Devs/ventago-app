package com.teco.ventago.features.orders.data.provider

import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.orders.domain.models.requests.CancelOrderRequest
import com.teco.ventago.features.orders.domain.models.requests.CreatePaymentLinkRequest
import com.teco.ventago.features.orders.domain.models.requests.CreateOrderRequest
import com.teco.ventago.features.orders.domain.models.requests.DeleteOrderRequest
import com.teco.ventago.features.orders.domain.models.requests.ListOrdersRequest
import com.teco.ventago.features.orders.domain.models.requests.PendingIntentCreateRequest
import com.teco.ventago.features.orders.domain.models.requests.PendingIntentReleaseRequest
import com.teco.ventago.features.orders.domain.models.requests.RejectAchPaymentRequest
import com.teco.ventago.features.orders.domain.models.requests.RescheduleReceivablesRequest
import com.teco.ventago.features.orders.domain.models.requests.RegisterManualPaymentsRequest
import com.teco.ventago.features.orders.domain.models.requests.VoidOrderPaymentRequest
import com.teco.ventago.utils.ApiResponse


interface IOrdersProvider {
    suspend fun createOrder(businessId: Int, createOrderRequest: CreateOrderRequest): ApiResponse

    suspend fun cancelOrder(businessId: Int, request: CancelOrderRequest): ApiResponse
    suspend fun deleteOrder(businessId: Int, request: DeleteOrderRequest): ApiResponse
    suspend fun registerManualPayments(businessId: Int, orderId: Int, request: RegisterManualPaymentsRequest): ApiResponse
    suspend fun rescheduleOrderReceivables(
        businessId: Int,
        orderId: Int,
        request: RescheduleReceivablesRequest
    ): ApiResponse
    suspend fun voidOrderPayment(
        businessId: Int,
        paymentId: Long,
        request: VoidOrderPaymentRequest
    ): ApiResponse
    suspend fun retryElectronicInvoice(
        businessId: Int,
        orderId: Int,
    ): ApiResponse
    suspend fun getInvoiceDocsRaw(businessId: Int, cufe: String): ApiResponse
    suspend fun createPaymentLink(businessId: Int, request: CreatePaymentLinkRequest): ApiResponse
    suspend fun releasePendingPaymentIntent(
        businessId: Int,
        orderId: Int,
        request: PendingIntentReleaseRequest
    ): ApiResponse
    suspend fun createPendingPaymentIntent(
        businessId: Int,
        orderId: Int,
        request: PendingIntentCreateRequest
    ): ApiResponse
    suspend fun getAchPaymentByIntent(businessId: Int, paymentIntentId: String): ApiResponse
    suspend fun approveAchPayment(businessId: Int, paymentIntentId: String): ApiResponse
    suspend fun rejectAchPayment(
        businessId: Int,
        paymentIntentId: String,
        request: RejectAchPaymentRequest
    ): ApiResponse
    suspend fun downloadAchProofFile(
        businessId: Int,
        paymentId: String,
        proofId: String
    ): BinaryPayload

    // OLD METHODS> CHECK IF NEEDED LATER

    suspend fun isBusinessRegistered(businessId: Int): ApiResponse

    suspend fun loadOrders(
        request: ListOrdersRequest
    ): ApiResponse

    suspend fun changeOrderStatus(order: Order, status: Int, businessId: Int): ApiResponse

    suspend fun rejectOrder(order: Order, reason: String, businessId: Int): ApiResponse

    suspend fun changeOrdersEnabled(businessId: Int, enabled: Boolean): ApiResponse

    suspend fun getBusinessConfig(businessId: Int): ApiResponse

    suspend fun loadCustomers(businessId: Int, pageSize: Int, page: Int): ApiResponse
    suspend fun loadChangedCustomers(businessId: Int, lastUpdate: String): ApiResponse
    suspend fun removeCustomer(businessId: Int, customerId: Int): ApiResponse
    suspend fun getOrderPaymentLink(orderID: Int): ApiResponse

    suspend fun findOrderByOrderNumber(businessId: Int, orderNumber: String): ApiResponse
    suspend fun findOrderById(businessId: Int, orderId: Int): ApiResponse
    suspend fun findOrderByCUFE(businessId: Int, cufe: String): ApiResponse
    suspend fun registerManualTransference(businessId: Int, orderNumber: String, paymentReference: String, description: String): ApiResponse
}
