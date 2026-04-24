package com.teco.ventago.features.payments.domain

import com.teco.ventago.features.financialProfile.domain.FinancialProfileService
import com.teco.ventago.features.payments.data.repository.IPaymentsRepository
import com.teco.ventago.features.payments.data.repository.IPaypalRepository
import com.teco.ventago.features.payments.data.repository.IYappyRepository
import com.teco.ventago.features.payments.domain.models.AchAccount
import com.teco.ventago.features.payments.domain.models.AchAccountConfigRequest
import com.teco.ventago.features.payments.domain.models.AchStatus
import com.teco.ventago.features.payments.domain.models.FeeBatchItem
import com.teco.ventago.features.payments.domain.models.FeeSummary
import com.teco.ventago.features.payments.domain.models.FeeTransactionItem


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

    suspend fun setAutoInvoiceOnPaymentSuccess(businessId: Int, enabled: Boolean): Boolean {
        val success = paymentsRepository.setAutoInvoice(businessId, enabled)
        if (success) {
            val profile = financialProfileService.observe().value
            profile?.let {
                val newProfile = profile.copy(
                    paymentSummary = profile.paymentSummary.copy(
                        autoInvoiceOnPaymentSuccess = enabled
                    )
                )
                financialProfileService.updateProfile(newProfile, ignoreChange = true)
            }
        }
        return success
    }

    suspend fun getAchStatus(businessId: Int): AchStatus {
        return paymentsRepository.getAchStatus(businessId)
    }

    suspend fun getAchAccount(businessId: Int): AchAccount {
        return paymentsRepository.getAchAccount(businessId)
    }

    suspend fun configureAchAccount(businessId: Int, request: AchAccountConfigRequest): Boolean {
        val success = paymentsRepository.configureAchAccount(businessId, request)
        if (success) {
            financialProfileService.refresh()
        }
        return success
    }

    suspend fun disableAch(businessId: Int): Boolean {
        val success = paymentsRepository.disableAch(businessId)
        if (success) {
            financialProfileService.refresh()
        }
        return success
    }

    suspend fun getFeesSummary(businessId: Int, currencyCode: String): FeeSummary {
        return paymentsRepository.getFeesSummary(businessId, currencyCode)
    }

    suspend fun getFeeTransactions(
        businessId: Int,
        page: Int,
        size: Int,
        status: String?,
        paymentMethod: String?,
        currencyCode: String
    ): Pair<List<FeeTransactionItem>, Int> {
        return paymentsRepository.getFeeTransactions(
            businessId = businessId,
            page = page,
            size = size,
            status = status,
            paymentMethod = paymentMethod,
            currencyCode = currencyCode
        )
    }

    suspend fun getFeeBatches(
        businessId: Int,
        page: Int,
        size: Int,
        status: String?,
        currencyCode: String
    ): Pair<List<FeeBatchItem>, Int> {
        return paymentsRepository.getFeeBatches(
            businessId = businessId,
            page = page,
            size = size,
            status = status,
            currencyCode = currencyCode
        )
    }

}
