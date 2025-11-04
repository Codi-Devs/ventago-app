package com.teco.ventago.features.settings.data.repository

import com.teco.ventago.features.business.domain.model.BusinessAddress
import com.teco.ventago.features.business.domain.model.BusinessSocialNetwork
import com.teco.ventago.utils.ApiResponse

interface ISettingsRepository {
    suspend fun allowWhatsappOrders(allow: Boolean, businessId: Int) : Boolean
    suspend fun updateBusinessSocialNetworks(social: BusinessSocialNetwork, businessId: Int) : Boolean
    suspend fun changeBusinessName(name: String, businessId: Int) : Boolean
    suspend fun changeBusinessInfo(name: String, phone: String, ruc: String, website: String, businessEmail: String, businessId: Int): Boolean
    suspend fun changeBusinessPhone(phone: String, businessId: Int): Boolean
    suspend fun changeBusinessAddress(address: BusinessAddress, businessId: Int) : Boolean
    suspend fun updateBusinessLogo(logo: String, businessId: Int): Boolean
}