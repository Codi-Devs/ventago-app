package com.teco.ventago.features.orders.data.repository

import com.teco.ventago.features.orders.domain.models.ChangeOrderStatusResponse
import com.teco.ventago.features.orders.domain.models.IsBusinessRegisteredResponse
import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.orders.domain.models.requests.CancelOrderRequest
import com.teco.ventago.features.orders.domain.models.requests.CreateOrderRequest
import com.teco.ventago.features.orders.domain.models.requests.RegisterManualPaymentsDataResponse
import com.teco.ventago.features.orders.domain.models.requests.RegisterManualPaymentsRequest
import com.teco.ventago.features.orders.domain.models.requests.RetryInvoiceResponse
import com.teco.ventago.features.orders.domain.models.responses.CreateOrderResponse
import com.teco.ventago.features.orders.domain.models.responses.InvoiceDocsDto


interface IOrdersRepository {

    suspend fun isBusinessRegistered(businessId: Int): IsBusinessRegisteredResponse

    suspend fun loadOrders(businessId: Int, pageSize: Int, page: Int): List<Order>

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

    suspend fun registerManualPayments(
        businessId: Int,
        orderId: Int,
        request: RegisterManualPaymentsRequest
    ): RegisterManualPaymentsDataResponse

    suspend fun retryElectronicInvoice(
        businessId: Int,
        orderId: Int,
    ): RetryInvoiceResponse

    suspend fun getInvoiceDocsRaw(businessId: Int, cufe: String): InvoiceDocsDto

    suspend fun removeCustomer(businessId: Int, customerId: Int): Boolean

    suspend fun getOrderPaymentLink(orderID: Int): String?

    suspend fun registerManualTransference(
        businessId: Int,
        orderNumber: String,
        paymentReference: String,
        description: String
    ): Boolean

    suspend fun findOrderByOrderNumber(businessId: Int, orderNumber: String): Order

    suspend fun findOrderByCUFE(businessId: Int, cufe: String): Order
}