package com.teco.ventago.features.product.data.provider.item

import com.teco.ventago.features.product.domain.model.Item
import com.teco.ventago.utils.ApiResponse

interface IItemProvider {
    suspend fun addItem(item: Item, categoryId: Int): ApiResponse
    suspend fun editItem(item: Item): ApiResponse
    suspend fun removeItem(itemId: Int): ApiResponse
    suspend fun changeItemOrder(items: List<Item>): ApiResponse
}