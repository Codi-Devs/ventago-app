package com.teco.ventago.features.payments.domain

import com.teco.ventago.features.financialProfile.domain.FinancialProfileService
import com.teco.ventago.features.payments.data.repository.IPaymentsRepository
import com.teco.ventago.features.payments.data.repository.IPaypalRepository
import com.teco.ventago.features.payments.data.repository.IYappyRepository
import com.teco.ventago.features.payments.domain.models.AchAccount
import com.teco.ventago.features.payments.domain.models.AchAccountConfigRequest
import com.teco.ventago.features.payments.domain.models.AchStatus
import com.teco.ventago.features.payments.domain.models.DirectCheckoutRequest
import com.teco.ventago.features.payments.domain.models.DirectCheckoutResponse
import com.teco.ventago.features.payments.domain.models.FeeBatchItem
import com.teco.ventago.features.payments.domain.models.FeeSummary
import com.teco.ventago.features.payments.domain.models.FeeTransactionItem
import com.teco.ventago.features.payments.domain.models.TiloPayCredentialsRequest
import com.teco.ventago.features.payments.domain.models.TiloPayStatus
import com.teco.ventago.features.payments.domain.models.YappyOnsiteCancelPendingRequest
import com.teco.ventago.features.payments.domain.models.YappyOnsiteCancelPendingResponse
import com.teco.ventago.features.payments.domain.models.YappyOnsiteCancelRequest
import com.teco.ventago.features.payments.domain.models.YappyOnsiteDevice
import com.teco.ventago.features.payments.domain.models.YappyOnsiteDeviceConfigRequest
import com.teco.ventago.features.payments.domain.models.YappyOnsiteGroup
import com.teco.ventago.features.payments.domain.models.YappyOnsiteGroupConfigRequest
import com.teco.ventago.features.payments.domain.models.YappyOnsiteTransactionPayload
import com.teco.ventago.features.payments.domain.models.YappyOnsiteTransactionStatus


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
                                configured = false,
                                enabled = false,
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

    suspend fun getTiloPayStatus(businessId: Int): TiloPayStatus {
        return paymentsRepository.getTiloPayStatus(businessId)
    }

    suspend fun configureTiloPayCredentials(
        businessId: Int,
        apiUser: String,
        password: String,
        apiKey: String
    ): TiloPayStatus {
        val status = paymentsRepository.configureTiloPayCredentials(
            businessId = businessId,
            request = TiloPayCredentialsRequest(
                apiUser = apiUser,
                password = password,
                apiKey = apiKey,
            )
        )
        financialProfileService.refresh(businessId)
        return status
    }

    suspend fun disconnectTiloPay(businessId: Int): TiloPayStatus {
        val status = paymentsRepository.disconnectTiloPay(businessId)
        financialProfileService.refresh(businessId)
        return status
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
        batchId: Long? = null,
        currencyCode: String
    ): Pair<List<FeeTransactionItem>, Int> {
        return paymentsRepository.getFeeTransactions(
            businessId = businessId,
            page = page,
            size = size,
            status = status,
            paymentMethod = paymentMethod,
            batchId = batchId,
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

    suspend fun configureYappyOnsiteGroup(
        businessId: Int,
        groupId: String,
        request: YappyOnsiteGroupConfigRequest
    ): Boolean {
        val success = paymentsRepository.configureYappyOnsiteGroup(businessId, groupId, request)
        if (success) {
            financialProfileService.refresh(businessId)
        }
        return success
    }

    suspend fun listYappyOnsiteGroups(businessId: Int): List<YappyOnsiteGroup> {
        return paymentsRepository.listYappyOnsiteGroups(businessId)
    }

    suspend fun deleteYappyOnsiteGroup(businessId: Int, groupId: String): Boolean {
        val success = paymentsRepository.deleteYappyOnsiteGroup(businessId, groupId)
        if (success) {
            financialProfileService.refresh(businessId)
        }
        return success
    }

    suspend fun registerYappyOnsiteDevice(
        businessId: Int,
        groupId: String,
        request: YappyOnsiteDeviceConfigRequest
    ): Boolean {
        val success = paymentsRepository.registerYappyOnsiteDevice(businessId, groupId, request)
        if (success) {
            financialProfileService.refresh(businessId)
        }
        return success
    }

    suspend fun updateYappyOnsiteDevice(
        businessId: Int,
        groupId: String,
        deviceId: String,
        request: YappyOnsiteDeviceConfigRequest
    ): Boolean {
        val success = paymentsRepository.updateYappyOnsiteDevice(businessId, groupId, deviceId, request)
        if (success) {
            financialProfileService.refresh(businessId)
        }
        return success
    }

    suspend fun deleteYappyOnsiteDevice(
        businessId: Int,
        groupId: String,
        deviceId: String
    ): Boolean {
        val success = paymentsRepository.deleteYappyOnsiteDevice(businessId, groupId, deviceId)
        if (success) {
            financialProfileService.refresh(businessId)
        }
        return success
    }

    suspend fun listYappyOnsiteDevices(businessId: Int, groupId: String): List<YappyOnsiteDevice> {
        return paymentsRepository.listYappyOnsiteDevices(businessId, groupId)
    }

    suspend fun listAllYappyOnsiteDevices(businessId: Int, groups: List<YappyOnsiteGroup>): List<YappyOnsiteDevice> {
        return groups.flatMap { group ->
            group.groupId
                .takeIf { it.isNotBlank() }
                ?.let { groupId ->
                    paymentsRepository.listYappyOnsiteDevices(businessId, groupId)
                        .map { device ->
                            device.copy(
                                groupId = device.groupId.ifBlank { groupId },
                                branchCode = device.branchCode.ifBlank { group.branchCode },
                            )
                        }
                }
                ?: emptyList()
        }
    }

    suspend fun getYappyOnsiteTransaction(
        businessId: Int,
        transactionId: String
    ): YappyOnsiteTransactionPayload {
        return paymentsRepository.getYappyOnsiteTransaction(businessId, transactionId)
    }

    suspend fun cancelYappyOnsiteTransaction(
        businessId: Int,
        transactionId: String,
        reason: String
    ): YappyOnsiteTransactionStatus {
        return paymentsRepository.cancelYappyOnsiteTransaction(
            businessId = businessId,
            transactionId = transactionId,
            request = YappyOnsiteCancelRequest(reason = reason)
        )
    }

    suspend fun cancelPendingYappyOnsiteTransaction(
        businessId: Int,
        branchCode: String,
        billingPoint: String,
        reason: String
    ): YappyOnsiteCancelPendingResponse {
        return paymentsRepository.cancelPendingYappyOnsiteTransaction(
            businessId = businessId,
            request = YappyOnsiteCancelPendingRequest(
                branchCode = branchCode,
                billingPoint = billingPoint,
                reason = reason,
            )
        )
    }

    suspend fun createDirectCheckout(
        businessId: Int,
        successUrl: String,
        cancelUrl: String
    ): DirectCheckoutResponse {
        return paymentsRepository.createDirectCheckout(
            businessId = businessId,
            request = DirectCheckoutRequest(
                successUrl = successUrl,
                cancelUrl = cancelUrl,
            )
        )
    }

}
