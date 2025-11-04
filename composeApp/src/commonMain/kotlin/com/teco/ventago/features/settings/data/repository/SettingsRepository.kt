package com.teco.ventago.features.settings.data.repository

import com.teco.ventago.features.business.domain.model.BusinessAddress
import com.teco.ventago.features.business.domain.model.BusinessSocialNetwork
import com.teco.ventago.features.settings.data.provider.ISettingsProvider
import com.teco.ventago.utils.BadRequestException
import com.teco.ventago.utils.isError

class SettingsRepository(private val provider: ISettingsProvider) : ISettingsRepository {
    override suspend fun allowWhatsappOrders(allow: Boolean, businessId: Int): Boolean {
        try {
            val response = provider.allowWhatsappOrders(allow, businessId)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            return response.successful
        } catch (e: Exception) {
            //TODO add logs
            throw e
        }
    }

    override suspend fun updateBusinessSocialNetworks(
        social: BusinessSocialNetwork,
        businessId: Int,
    ): Boolean {
        try {
            val response = provider.updateBusinessSocialNetworks(social, businessId)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            return response.successful
        } catch (e: Exception) {
            //TODO add logs
            throw e
        }
    }

    override suspend fun changeBusinessName(name: String, businessId: Int): Boolean {
        try {
            val response = provider.changeBusinessName(name, businessId)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            return response.successful
        } catch (e: Exception) {
            //TODO add logs
            throw e
        }
    }


    override suspend fun changeBusinessInfo(name: String, phone: String, ruc: String,
                                            website: String, businessEmail: String,
                                            businessId: Int): Boolean {
        try {
            val response = provider.changeBusinessInfo(name, phone, ruc, website,
                businessEmail, businessId)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            return response.successful
        } catch (e: Exception) {
            //TODO add logs
            throw e
        }
    }




    override suspend fun changeBusinessPhone(phone: String, businessId: Int): Boolean {
        try {
            val response = provider.changeBusinessPhone(phone, businessId)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            return response.successful
        } catch (e: Exception) {
            //TODO add logs
            throw e
        }
    }

    override suspend fun changeBusinessAddress(address: BusinessAddress, businessId: Int): Boolean {
        try {
            val response = provider.changeBusinessAddress(address, businessId)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            return response.successful
        } catch (e: Exception) {
            //TODO add logs
            throw e
        }
    }

    override suspend fun updateBusinessLogo(logo: String, businessId: Int): Boolean {
        try {
            val response = provider.updateBusinessLogo(businessId, logo)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            return response.successful
        } catch (e: Exception) {
            //TODO add logs
            throw e
        }
    }

}