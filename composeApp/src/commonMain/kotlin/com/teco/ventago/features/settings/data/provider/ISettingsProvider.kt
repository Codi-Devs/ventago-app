package com.teco.ventago.features.settings.data.provider

import com.teco.ventago.features.business.domain.model.BusinessAddress
import com.teco.ventago.features.business.domain.model.BusinessSocialNetwork
import com.teco.ventago.utils.ApiResponse

interface ISettingsProvider {
    suspend fun allowWhatsappOrders(allow: Boolean, businessId: Int) : ApiResponse
    suspend fun updateBusinessSocialNetworks(social: BusinessSocialNetwork, businessId: Int) : ApiResponse
    suspend fun changeBusinessName(name: String, businessId: Int) : ApiResponse
    suspend fun changeBusinessInfo(name: String, phone: String, ruc: String, website: String, businessEmail: String, businessId: Int): ApiResponse
    suspend fun changeBusinessPhone(phone: String, businessId: Int) : ApiResponse
    suspend fun changeBusinessAddress(address: BusinessAddress, businessId: Int) : ApiResponse
    suspend fun updateBusinessLogo(businessId: Int, logo: String): ApiResponse

}