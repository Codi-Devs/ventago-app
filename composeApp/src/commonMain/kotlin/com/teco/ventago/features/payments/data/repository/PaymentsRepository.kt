package com.teco.ventago.features.payments.data.repository

import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.payments.data.provider.IPaymentsProvider
import com.teco.ventago.features.payments.domain.models.AchAccount
import com.teco.ventago.features.payments.domain.models.AchAccountConfigRequest
import com.teco.ventago.features.payments.domain.models.AchStatus
import com.teco.ventago.features.payments.domain.models.DirectCheckoutRequest
import com.teco.ventago.features.payments.domain.models.DirectCheckoutResponse
import com.teco.ventago.features.payments.domain.models.FeeBatchItem
import com.teco.ventago.features.payments.domain.models.FeeBatchesPayload
import com.teco.ventago.features.payments.domain.models.FeeSummary
import com.teco.ventago.features.payments.domain.models.FeeTransactionItem
import com.teco.ventago.features.payments.domain.models.FeeTransactionsPayload
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
import com.teco.ventago.utils.ApiResponse
import com.teco.ventago.utils.BadRequestException
import com.teco.ventago.utils.isError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

interface IPaymentsRepository {
    suspend fun onboardPayments(businessId: Int): Boolean
    suspend fun transference(businessId: Int, enabled: Boolean, instructions: String): Boolean
    suspend fun setAutoInvoice(businessId: Int, enabled: Boolean): Boolean
    suspend fun getAchStatus(businessId: Int): AchStatus
    suspend fun getAchAccount(businessId: Int): AchAccount
    suspend fun configureAchAccount(businessId: Int, request: AchAccountConfigRequest): Boolean
    suspend fun disableAch(businessId: Int): Boolean
    suspend fun getTiloPayStatus(businessId: Int): TiloPayStatus
    suspend fun configureTiloPayCredentials(businessId: Int, request: TiloPayCredentialsRequest): TiloPayStatus
    suspend fun disconnectTiloPay(businessId: Int): TiloPayStatus
    suspend fun getFeesSummary(businessId: Int, currencyCode: String): FeeSummary
    suspend fun getFeeTransactions(
        businessId: Int,
        page: Int,
        size: Int,
        status: String?,
        paymentMethod: String?,
        batchId: Long?,
        currencyCode: String
    ): Pair<List<FeeTransactionItem>, Int>
    suspend fun getFeeBatches(
        businessId: Int,
        page: Int,
        size: Int,
        status: String?,
        currencyCode: String
    ): Pair<List<FeeBatchItem>, Int>
    suspend fun configureYappyOnsiteGroup(businessId: Int, groupId: String, request: YappyOnsiteGroupConfigRequest): Boolean
    suspend fun listYappyOnsiteGroups(businessId: Int): List<YappyOnsiteGroup>
    suspend fun deleteYappyOnsiteGroup(businessId: Int, groupId: String): Boolean
    suspend fun registerYappyOnsiteDevice(businessId: Int, groupId: String, request: YappyOnsiteDeviceConfigRequest): Boolean
    suspend fun updateYappyOnsiteDevice(businessId: Int, groupId: String, deviceId: String, request: YappyOnsiteDeviceConfigRequest): Boolean
    suspend fun deleteYappyOnsiteDevice(businessId: Int, groupId: String, deviceId: String): Boolean
    suspend fun listYappyOnsiteDevices(businessId: Int, groupId: String): List<YappyOnsiteDevice>
    suspend fun getYappyOnsiteTransaction(businessId: Int, transactionId: String): YappyOnsiteTransactionPayload
    suspend fun cancelYappyOnsiteTransaction(
        businessId: Int,
        transactionId: String,
        request: YappyOnsiteCancelRequest
    ): YappyOnsiteTransactionStatus
    suspend fun cancelPendingYappyOnsiteTransaction(
        businessId: Int,
        request: YappyOnsiteCancelPendingRequest
    ): YappyOnsiteCancelPendingResponse
    suspend fun createDirectCheckout(businessId: Int, request: DirectCheckoutRequest): DirectCheckoutResponse
}

class PaymentsRepository(
    private val provider: IPaymentsProvider,
    private val logger: ILoggerService
) : IPaymentsRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun onboardPayments(businessId: Int): Boolean {
        return try {
            val response = provider.onboardPayments(businessId)
            ensureOk(response)
            extractBooleanData(response)
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "PaymentsRepository::onboardPayments", "Error onboarding payments. Error: ${e.message ?: "UNKNOWN"}"))
            throw e
        }
    }

    override suspend fun transference(businessId: Int, enabled: Boolean, instructions: String): Boolean {
        return try {
            val response = provider.transference(businessId, enabled, instructions)
            ensureOk(response)
            extractBooleanData(response)
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "PaymentsRepository::transference", "Error updating transference. Error: ${e.message ?: "UNKNOWN"}"))
            throw e
        }
    }

    override suspend fun setAutoInvoice(businessId: Int, enabled: Boolean): Boolean {
        return try {
            val response = provider.setAutoInvoice(businessId, enabled)
            ensureOk(response)
            extractBooleanData(response)
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "PaymentsRepository::setAutoInvoice", "Error updating auto-invoice setting. Error: ${e.message ?: "UNKNOWN"}"))
            throw e
        }
    }

    override suspend fun getAchStatus(businessId: Int): AchStatus {
        return try {
            val response = provider.getAchStatus(businessId)
            ensureOk(response)
            val data = response.data as? JsonObject ?: JsonObject(emptyMap())
            json.decodeFromJsonElement<AchStatus>(data)
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "PaymentsRepository::getAchStatus", "Error loading ACH status. Error: ${e.message ?: "UNKNOWN"}"))
            throw e
        }
    }

    override suspend fun getAchAccount(businessId: Int): AchAccount {
        return try {
            val response = provider.getAchAccount(businessId)
            ensureOk(response)
            val data = response.data as? JsonObject ?: JsonObject(emptyMap())
            json.decodeFromJsonElement<AchAccount>(data)
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "PaymentsRepository::getAchAccount", "Error loading ACH account. Error: ${e.message ?: "UNKNOWN"}"))
            throw e
        }
    }

    override suspend fun configureAchAccount(businessId: Int, request: AchAccountConfigRequest): Boolean {
        return try {
            val response = provider.configureAchAccount(businessId, request)
            ensureOk(response)
            extractBooleanData(response)
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "PaymentsRepository::configureAchAccount", "Error configuring ACH account. Error: ${e.message ?: "UNKNOWN"}"))
            throw e
        }
    }

    override suspend fun disableAch(businessId: Int): Boolean {
        return try {
            val response = provider.disableAch(businessId)
            ensureOk(response)
            extractBooleanData(response)
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "PaymentsRepository::disableAch", "Error disabling ACH. Error: ${e.message ?: "UNKNOWN"}"))
            throw e
        }
    }

    override suspend fun getTiloPayStatus(businessId: Int): TiloPayStatus {
        return try {
            val response = provider.getTiloPayStatus(businessId)
            ensureOk(response)
            val data = response.data as? JsonObject ?: JsonObject(emptyMap())
            json.decodeFromJsonElement<TiloPayStatus>(data)
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "PaymentsRepository::getTiloPayStatus", "Error loading TiloPay status. Error: ${e.message ?: "UNKNOWN"}, businessId: $businessId"))
            throw e
        }
    }

    override suspend fun configureTiloPayCredentials(
        businessId: Int,
        request: TiloPayCredentialsRequest
    ): TiloPayStatus {
        return try {
            val response = provider.configureTiloPayCredentials(businessId, request)
            ensureOk(response)
            val data = response.data as? JsonObject ?: JsonObject(emptyMap())
            json.decodeFromJsonElement<TiloPayStatus>(data)
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "PaymentsRepository::configureTiloPayCredentials", "Error configuring TiloPay. Error: ${e.message ?: "UNKNOWN"}, businessId: $businessId"))
            throw e
        }
    }

    override suspend fun disconnectTiloPay(businessId: Int): TiloPayStatus {
        return try {
            val response = provider.disconnectTiloPay(businessId)
            ensureOk(response)
            val data = response.data as? JsonObject ?: JsonObject(emptyMap())
            json.decodeFromJsonElement<TiloPayStatus>(data)
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "PaymentsRepository::disconnectTiloPay", "Error disconnecting TiloPay. Error: ${e.message ?: "UNKNOWN"}, businessId: $businessId"))
            throw e
        }
    }

    override suspend fun getFeesSummary(businessId: Int, currencyCode: String): FeeSummary {
        return try {
            val response = provider.getFeesSummary(businessId, currencyCode)
            ensureOk(response)
            val data = response.data as? JsonObject ?: JsonObject(emptyMap())
            json.decodeFromJsonElement<FeeSummary>(data)
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "PaymentsRepository::getFeesSummary", "Error loading fee summary. Error: ${e.message ?: "UNKNOWN"}"))
            throw e
        }
    }

    override suspend fun getFeeTransactions(
        businessId: Int,
        page: Int,
        size: Int,
        status: String?,
        paymentMethod: String?,
        batchId: Long?,
        currencyCode: String
    ): Pair<List<FeeTransactionItem>, Int> {
        return try {
            val response = provider.getFeeTransactions(
                businessId = businessId,
                page = page,
                size = size,
                status = status,
                paymentMethod = paymentMethod,
                batchId = batchId,
                currencyCode = currencyCode
            )
            ensureOk(response)
            val data = response.data as? JsonObject ?: JsonObject(emptyMap())
            val payload = json.decodeFromJsonElement<FeeTransactionsPayload>(data)
            payload.items to (payload.total.takeIf { it > 0 } ?: payload.pagination.total)
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "PaymentsRepository::getFeeTransactions", "Error loading fee transactions. Error: ${e.message ?: "UNKNOWN"}"))
            throw e
        }
    }

    override suspend fun getFeeBatches(
        businessId: Int,
        page: Int,
        size: Int,
        status: String?,
        currencyCode: String
    ): Pair<List<FeeBatchItem>, Int> {
        return try {
            val response = provider.getFeeBatches(
                businessId = businessId,
                page = page,
                size = size,
                status = status,
                currencyCode = currencyCode
            )
            ensureOk(response)
            val data = response.data as? JsonObject ?: JsonObject(emptyMap())
            val payload = json.decodeFromJsonElement<FeeBatchesPayload>(data)
            payload.items to (payload.total.takeIf { it > 0 } ?: payload.pagination.total)
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "PaymentsRepository::getFeeBatches", "Error loading fee batches. Error: ${e.message ?: "UNKNOWN"}"))
            throw e
        }
    }

    override suspend fun configureYappyOnsiteGroup(
        businessId: Int,
        groupId: String,
        request: YappyOnsiteGroupConfigRequest
    ): Boolean {
        return try {
            val response = provider.configureYappyOnsiteGroup(businessId, groupId, request)
            ensureOk(response)
            response.successful
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "PaymentsRepository::configureYappyOnsiteGroup", "Error configuring Yappy onsite group. Error: ${e.message ?: "UNKNOWN"}, businessId: $businessId, groupId: $groupId"))
            throw e
        }
    }

    override suspend fun listYappyOnsiteGroups(businessId: Int): List<YappyOnsiteGroup> {
        return try {
            val response = provider.listYappyOnsiteGroups(businessId)
            ensureOk(response)
            val data = response.data as? JsonArray ?: JsonArray(emptyList())
            json.decodeFromJsonElement<List<YappyOnsiteGroup>>(data)
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "PaymentsRepository::listYappyOnsiteGroups", "Error listing Yappy onsite groups. Error: ${e.message ?: "UNKNOWN"}, businessId: $businessId"))
            throw e
        }
    }

    override suspend fun deleteYappyOnsiteGroup(businessId: Int, groupId: String): Boolean {
        return try {
            val response = provider.deleteYappyOnsiteGroup(businessId, groupId)
            ensureOk(response)
            extractBooleanData(response)
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "PaymentsRepository::deleteYappyOnsiteGroup", "Error deleting Yappy onsite group. Error: ${e.message ?: "UNKNOWN"}, businessId: $businessId, groupId: $groupId"))
            throw e
        }
    }

    override suspend fun registerYappyOnsiteDevice(
        businessId: Int,
        groupId: String,
        request: YappyOnsiteDeviceConfigRequest
    ): Boolean {
        return try {
            val response = provider.registerYappyOnsiteDevice(businessId, groupId, request)
            ensureOk(response)
            response.successful
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "PaymentsRepository::registerYappyOnsiteDevice", "Error registering Yappy onsite device. Error: ${e.message ?: "UNKNOWN"}, businessId: $businessId, groupId: $groupId"))
            throw e
        }
    }

    override suspend fun updateYappyOnsiteDevice(
        businessId: Int,
        groupId: String,
        deviceId: String,
        request: YappyOnsiteDeviceConfigRequest
    ): Boolean {
        return try {
            val response = provider.updateYappyOnsiteDevice(businessId, groupId, deviceId, request)
            ensureOk(response)
            extractBooleanData(response)
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "PaymentsRepository::updateYappyOnsiteDevice", "Error updating Yappy onsite device. Error: ${e.message ?: "UNKNOWN"}, businessId: $businessId, groupId: $groupId, deviceId: $deviceId"))
            throw e
        }
    }

    override suspend fun deleteYappyOnsiteDevice(
        businessId: Int,
        groupId: String,
        deviceId: String
    ): Boolean {
        return try {
            val response = provider.deleteYappyOnsiteDevice(businessId, groupId, deviceId)
            ensureOk(response)
            extractBooleanData(response)
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "PaymentsRepository::deleteYappyOnsiteDevice", "Error deleting Yappy onsite device. Error: ${e.message ?: "UNKNOWN"}, businessId: $businessId, groupId: $groupId, deviceId: $deviceId"))
            throw e
        }
    }

    override suspend fun listYappyOnsiteDevices(businessId: Int, groupId: String): List<YappyOnsiteDevice> {
        return try {
            val response = provider.listYappyOnsiteDevices(businessId, groupId)
            ensureOk(response)
            val data = response.data as? JsonArray ?: JsonArray(emptyList())
            json.decodeFromJsonElement<List<YappyOnsiteDevice>>(data)
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "PaymentsRepository::listYappyOnsiteDevices", "Error listing Yappy onsite devices. Error: ${e.message ?: "UNKNOWN"}, businessId: $businessId, groupId: $groupId"))
            throw e
        }
    }

    override suspend fun getYappyOnsiteTransaction(
        businessId: Int,
        transactionId: String
    ): YappyOnsiteTransactionPayload {
        return try {
            val response = provider.getYappyOnsiteTransaction(businessId, transactionId)
            ensureOk(response)
            val data = response.data as? JsonObject ?: JsonObject(emptyMap())
            json.decodeFromJsonElement<YappyOnsiteTransactionPayload>(data)
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "PaymentsRepository::getYappyOnsiteTransaction", "Error polling Yappy onsite transaction. Error: ${e.message ?: "UNKNOWN"}, businessId: $businessId, transactionId: $transactionId"))
            throw e
        }
    }

    override suspend fun cancelYappyOnsiteTransaction(
        businessId: Int,
        transactionId: String,
        request: YappyOnsiteCancelRequest
    ): YappyOnsiteTransactionStatus {
        return try {
            val response = provider.cancelYappyOnsiteTransaction(businessId, transactionId, request)
            ensureOk(response)
            val data = response.data as? JsonObject ?: JsonObject(emptyMap())
            json.decodeFromJsonElement<YappyOnsiteTransactionStatus>(data)
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "PaymentsRepository::cancelYappyOnsiteTransaction", "Error cancelling Yappy onsite transaction. Error: ${e.message ?: "UNKNOWN"}, businessId: $businessId, transactionId: $transactionId"))
            throw e
        }
    }

    override suspend fun cancelPendingYappyOnsiteTransaction(
        businessId: Int,
        request: YappyOnsiteCancelPendingRequest
    ): YappyOnsiteCancelPendingResponse {
        return try {
            val response = provider.cancelPendingYappyOnsiteTransaction(businessId, request)
            ensureOk(response)
            val data = response.data as? JsonObject ?: JsonObject(emptyMap())
            json.decodeFromJsonElement<YappyOnsiteCancelPendingResponse>(data)
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "PaymentsRepository::cancelPendingYappyOnsiteTransaction", "Error cancelling pending Yappy onsite transaction. Error: ${e.message ?: "UNKNOWN"}, businessId: $businessId, branchCode: ${request.branchCode}, billingPoint: ${request.billingPoint}"))
            throw e
        }
    }

    override suspend fun createDirectCheckout(
        businessId: Int,
        request: DirectCheckoutRequest
    ): DirectCheckoutResponse {
        return try {
            val response = provider.createDirectCheckout(businessId, request)
            ensureOk(response)
            val data = response.data as? JsonObject ?: JsonObject(emptyMap())
            json.decodeFromJsonElement<DirectCheckoutResponse>(data)
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "PaymentsRepository::createDirectCheckout", "Error creating direct checkout. Error: ${e.message ?: "UNKNOWN"}, businessId: $businessId, productType: ${request.productType}"))
            throw e
        }
    }

    private fun ensureOk(response: ApiResponse) {
        if (response.error.isError()) {
            throw BadRequestException(response.toJson())
        }
    }

    private fun extractBooleanData(response: ApiResponse): Boolean {
        val data = response.data
        return when (data) {
            null -> response.successful
            else -> {
                val primitiveBool = runCatching { data.jsonPrimitive.booleanOrNull }.getOrNull()
                if (primitiveBool != null) return primitiveBool

                val obj = runCatching { data.jsonObject }.getOrNull()
                if (obj != null) {
                    val deleted = obj["deleted"]?.jsonPrimitive?.booleanOrNull
                    if (deleted != null) return deleted

                    val cancelled = obj["cancelled"]?.jsonPrimitive?.booleanOrNull
                    if (cancelled != null) return cancelled

                    val configured = obj["configured"]?.jsonPrimitive?.booleanOrNull
                    if (configured != null) return configured

                    val enabled = obj["enabled"]?.jsonPrimitive?.booleanOrNull
                    if (enabled != null) return enabled

                    val successful = obj["successful"]?.jsonPrimitive?.booleanOrNull
                    if (successful != null) return successful
                    val status = obj["status"]?.jsonPrimitive?.contentOrNull
                    if (!status.isNullOrBlank()) {
                        return status.equals("approved", ignoreCase = true) ||
                            status.equals("rejected", ignoreCase = true) ||
                            status.equals("ok", ignoreCase = true)
                    }
                }

                response.successful
            }
        }
    }
}
