package com.teco.ventago.features.business.data.repository

import com.teco.ventago.features.business.domain.model.Business
import com.teco.ventago.features.business.domain.model.requests.RegisterBusinessRequest
import com.teco.ventago.features.business.domain.model.responses.BusinessRegisterResponse


interface IBusinessRepository {
    suspend fun updateWebStyle(
        businessId: Int,
        styleId: Int,
        primaryColor: String,
        secondaryColor: String
    ): Boolean

    suspend fun resetColors(businessId: Int): Boolean
    suspend fun registerBusiness(request: RegisterBusinessRequest): BusinessRegisterResponse
    suspend fun getBusinessById(businessId: Int): Business
    suspend fun getBusinesses(): List<Business>
    suspend fun deleteBusiness(businessId: Int): Boolean


}