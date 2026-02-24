package com.teco.ventago.features.orders.data.provider

import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.orders.domain.models.requests.CancelOrderRequest
import com.teco.ventago.features.orders.domain.models.requests.CreateOrderRequest
import com.teco.ventago.features.orders.domain.models.requests.DeleteOrderRequest
import com.teco.ventago.features.orders.domain.models.requests.RegisterManualPaymentsRequest
import com.teco.ventago.utils.ApiResponse


interface IOrdersProvider {
    suspend fun createOrder(businessId: Int, createOrderRequest: CreateOrderRequest): ApiResponse

    suspend fun cancelOrder(businessId: Int, request: CancelOrderRequest): ApiResponse
    suspend fun deleteOrder(businessId: Int, request: DeleteOrderRequest): ApiResponse
    suspend fun registerManualPayments(businessId: Int, orderId: Int, request: RegisterManualPaymentsRequest): ApiResponse
    suspend fun retryElectronicInvoice(
        businessId: Int,
        orderId: Int,
    ): ApiResponse
    suspend fun getInvoiceDocsRaw(businessId: Int, cufe: String): ApiResponse

    // OLD METHODS> CHECK IF NEEDED LATER

    suspend fun isBusinessRegistered(businessId: Int): ApiResponse

    suspend fun loadOrders(
        businessId: Int,
        pageSize: Int,
        page: Int,
        paymentStatus: Int? = null,
        customerId: Long? = null
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
    suspend fun findOrderByCUFE(businessId: Int, cufe: String): ApiResponse
    suspend fun registerManualTransference(businessId: Int, orderNumber: String, paymentReference: String, description: String): ApiResponse
}
