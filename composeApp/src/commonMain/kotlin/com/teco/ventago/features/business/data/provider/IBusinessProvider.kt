package com.teco.ventago.features.business.data.provider

import com.teco.ventago.features.business.domain.model.requests.RegisterBusinessRequest
import com.teco.ventago.utils.ApiResponse

interface IBusinessProvider {

    suspend fun updateWebStyle(
        businessId: Int,
        styleId: Int,
        primaryColor: String,
        secondaryColor: String
    ): ApiResponse

    suspend fun resetColors(businessId: Int): ApiResponse
    suspend fun registerBusiness(request: RegisterBusinessRequest): ApiResponse
    suspend fun checkDomain(domain: String): ApiResponse
    suspend fun getBusinessesByUser(): ApiResponse
    suspend fun getBusinessById(businessId: Int): ApiResponse
    suspend fun removeBusiness(businessId: Int): ApiResponse
}