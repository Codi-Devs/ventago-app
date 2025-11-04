package com.teco.ventago.features.pos.domain

import com.teco.ventago.features.orders.data.repository.IOrdersRepository
import com.teco.ventago.features.orders.domain.models.requests.CreateOrderRequest
import com.teco.ventago.features.orders.domain.models.responses.CreateOrderResponse

class PosService(
    private val repository: IOrdersRepository,
) {
    suspend fun createOrder(businessId: Int, createOrderRequest: CreateOrderRequest): CreateOrderResponse {
        return repository.createOrder(businessId, createOrderRequest)
    }
}