package com.teco.ventago.features.settings.domain

import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.business.domain.model.BusinessAddress
import com.teco.ventago.features.business.domain.model.BusinessSocialNetwork
import com.teco.ventago.features.settings.data.repository.ISettingsRepository

class SettingsService(val repository: ISettingsRepository, private val businessService: BusinessService) {

    suspend fun updateBusinessSocialNetworks(businessId: Int, social: BusinessSocialNetwork): Boolean {
        val res = repository.updateBusinessSocialNetworks(social, businessId)
        if (res) {
            businessService.business.value?.let {
                businessService.saveBusiness(it.copy(socialNetwork = social))
            }
        }
        return res
    }

    suspend fun changeBusinessName(businessId: Int, name: String): Boolean {
        val res = repository.changeBusinessName(name, businessId)
        if (res) {
            businessService.business.value?.let {
                businessService.saveBusiness(it.copy(name = name))
            }
        }
        return res
    }


    suspend fun changeBusinessInfo(name: String, phone: String, ruc: String, website: String,
                                   businessEmail: String, businessId: Int): Boolean {
        val res = repository.changeBusinessInfo(name, phone, ruc, website, businessEmail, businessId)
        if (res) {
            businessService.business.value?.let {
                businessService.saveBusiness(it.copy(
                    name = name,
                    phone = phone,
                    ruc = ruc,
                    web = website,
                    businessEmail = businessEmail
                    ))
            }
        }
        return res
    }




    suspend fun changeBusinessPhone(businessId: Int, phone: String): Boolean {
        val res = repository.changeBusinessPhone(phone, businessId)
        if (res) {
            businessService.business.value?.let {
                businessService.saveBusiness(it.copy(phone = phone))
            }
        }
        return res
    }

    suspend fun changeBusinessAddress(businessId: Int, address: BusinessAddress): Boolean {
        val res = repository.changeBusinessAddress(address, businessId)
        if (res) {
            businessService.business.value?.let {
                businessService.saveBusiness(it.copy(address = address))
            }
        }
        return res
    }

    suspend fun updateBusinessLogo(businessId: Int, logo: String): Boolean {
        val res = repository.updateBusinessLogo(logo, businessId)
        if (res) {
            businessService.business.value?.let {
                businessService.saveBusiness(it.copy(logo = logo))
            }
        }
        return res
    }

}