package com.teco.ventago.features.payments.domain

import com.teco.ventago.features.payments.data.repository.IYappyRepository


class YappyHandler(
    private val repository: IYappyRepository
) {

    internal suspend fun connect(businessId: Int, merchantID: String, domain: String, secretKey: String): Boolean {
        return repository.connect(businessId, merchantID, domain, secretKey)
    }

    internal suspend fun unlink(businessId: Int): Boolean {
        return repository.unlink(businessId)
    }
}