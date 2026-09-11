package com.teco.ventago.features.pos.domain

import com.teco.ventago.features.orders.data.repository.IOrdersRepository
import com.teco.ventago.features.orders.domain.models.requests.CreateOrderRequest
import com.teco.ventago.features.orders.domain.models.requests.RetryInvoiceResponse
import com.teco.ventago.features.orders.domain.models.responses.CreateOrderResponse
import com.teco.ventago.features.orders.domain.models.responses.InvoiceDocsDto

class PosService(
    private val repository: IOrdersRepository,
) {
    suspend fun createOrder(businessId: Int, createOrderRequest: CreateOrderRequest): CreateOrderResponse {
        return repository.createOrder(businessId, createOrderRequest)
    }

    suspend fun retryElectronicInvoice(businessId: Int, orderId: Int): RetryInvoiceResponse {
        return repository.retryElectronicInvoice(businessId, orderId)
    }

    suspend fun getInvoiceDocsRaw(businessId: Int, cufe: String, orderId: Long? = null): InvoiceDocsDto {
        return repository.getInvoiceDocsRaw(businessId, cufe, orderId)
    }
}
