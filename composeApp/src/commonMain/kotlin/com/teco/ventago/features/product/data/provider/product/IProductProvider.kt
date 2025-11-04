package com.teco.ventago.features.product.data.provider.product

import com.teco.ventago.utils.ApiResponse

interface IProductProvider {
    suspend fun getMenuByBusinessId(businessId: Int): ApiResponse
    suspend fun getMenuIdByBusinessId(businessId: Int): ApiResponse

}