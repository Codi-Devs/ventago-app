package com.teco.ventago.features.payments.domain

import com.teco.ventago.features.payments.data.repository.IPaypalRepository


class PaypalHandler(
    private val repository: IPaypalRepository
) {

    internal suspend fun connect(businessId: Int): String {
        return repository.connect(businessId)
    }

    internal suspend fun createBillingAgreement(businessId: Int): String {
        return repository.createBillingAgreement(businessId)
    }

    internal suspend fun unlink(businessId: Int): Boolean {
        return repository.unlink(businessId)
    }
}