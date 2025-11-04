package com.teco.ventago.features.payments.domain

import com.teco.ventago.features.financialProfile.domain.FinancialProfileService
import com.teco.ventago.features.payments.data.repository.IPaymentsRepository
import com.teco.ventago.features.payments.data.repository.IPaypalRepository
import com.teco.ventago.features.payments.data.repository.IYappyRepository


class PaymentService(
    paypalRepository: IPaypalRepository,
    yappyRepository: IYappyRepository,
    private val paymentsRepository: IPaymentsRepository,
    private val financialProfileService: FinancialProfileService,
) {
    val paypal = PaypalHandler(paypalRepository)
    val yappy = YappyHandler(yappyRepository)

    // Paypal
    suspend fun connectPaypal(businessId: Int): String {
        return paypal.connect(businessId)
    }

    suspend fun createBillingAgreement(businessId: Int): String {
        return paypal.createBillingAgreement(businessId)
    }

    suspend fun unlinkPaypal(businessId: Int): Boolean {
        val unlinked = paypal.unlink(businessId)
        if (unlinked) {
            val profile = financialProfileService.observe().value
            profile?.let {
                val newProfile =  profile.copy(
                    paymentSummary = profile.paymentSummary.copy(
                        paymentMethods = profile.paymentSummary.paymentMethods.copy(
                            paypal = profile.paymentSummary.paymentMethods.paypal.copy(
                                linkedAccount = false,
                                email = ""
                            )
                        )
                    )
                )
                financialProfileService.updateProfile(newProfile, ignoreChange = true)
            }
        }
        return unlinked
    }

    // Yappy
    suspend fun connectYappy(
        businessId: Int,
        merchantID: String,
        domain: String,
        secretKey: String
    ): Boolean {
        val connected = yappy.connect(businessId, merchantID, domain, secretKey)
        if (connected) {
            val profile = financialProfileService.observe().value
            profile?.let {
                val newProfile =  profile.copy(
                    paymentSummary = profile.paymentSummary.copy(
                        paymentMethods = profile.paymentSummary.paymentMethods.copy(
                            yappy = profile.paymentSummary.paymentMethods.yappy.copy(
                                linkedAccount = true,
                                visible = true,
                            )
                        )
                    )
                )
                financialProfileService.updateProfile(newProfile, ignoreChange = true)
            }
        }
        return connected
    }

    suspend fun unlinkYappy(businessId: Int): Boolean {
        val unlinked = yappy.unlink(businessId)
        if (unlinked) {
            val profile = financialProfileService.observe().value
            profile?.let {
                val newProfile =  profile.copy(
                    paymentSummary = profile.paymentSummary.copy(
                        paymentMethods = profile.paymentSummary.paymentMethods.copy(
                            yappy = profile.paymentSummary.paymentMethods.yappy.copy(
                                linkedAccount = false,
                            )
                        )
                    )
                )
                financialProfileService.updateProfile(newProfile, ignoreChange = true)
            }
        }
        return unlinked
    }

    suspend fun onboardPayments(businessId: Int): Boolean {
        val success = paymentsRepository.onboardPayments(businessId)
        if (success) {
            financialProfileService.refresh()
        }
        return success
    }

    suspend fun transference(businessId: Int, enabled: Boolean, instructions: String): Boolean {
        val success = paymentsRepository.transference(businessId, enabled, instructions)
        if (success) {
            val profile = financialProfileService.observe().value
            profile?.let {
                val newProfile =  profile.copy(
                    paymentSummary = profile.paymentSummary.copy(
                        paymentMethods = profile.paymentSummary.paymentMethods.copy(
                            manualTransference = profile.paymentSummary.paymentMethods.manualTransference.copy(
                                enabled = enabled,
                                paymentInstructions = instructions
                            )
                        )
                    )
                )
                financialProfileService.updateProfile(newProfile, ignoreChange = true)
            }
        }
        return success
    }

}