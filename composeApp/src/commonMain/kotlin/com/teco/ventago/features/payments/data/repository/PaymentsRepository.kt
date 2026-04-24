package com.teco.ventago.features.payments.data.repository

import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.payments.data.provider.IPaymentsProvider
import com.teco.ventago.features.payments.domain.models.AchAccount
import com.teco.ventago.features.payments.domain.models.AchAccountConfigRequest
import com.teco.ventago.features.payments.domain.models.AchStatus
import com.teco.ventago.features.payments.domain.models.FeeBatchItem
import com.teco.ventago.features.payments.domain.models.FeeBatchesPayload
import com.teco.ventago.features.payments.domain.models.FeeSummary
import com.teco.ventago.features.payments.domain.models.FeeTransactionItem
import com.teco.ventago.features.payments.domain.models.FeeTransactionsPayload
import com.teco.ventago.utils.ApiResponse
import com.teco.ventago.utils.BadRequestException
import com.teco.ventago.utils.isError
import kotlinx.serialization.json.Json
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
    suspend fun getFeesSummary(businessId: Int, currencyCode: String): FeeSummary
    suspend fun getFeeTransactions(
        businessId: Int,
        page: Int,
        size: Int,
        status: String?,
        paymentMethod: String?,
        currencyCode: String
    ): Pair<List<FeeTransactionItem>, Int>
    suspend fun getFeeBatches(
        businessId: Int,
        page: Int,
        size: Int,
        status: String?,
        currencyCode: String
    ): Pair<List<FeeBatchItem>, Int>
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
        currencyCode: String
    ): Pair<List<FeeTransactionItem>, Int> {
        return try {
            val response = provider.getFeeTransactions(
                businessId = businessId,
                page = page,
                size = size,
                status = status,
                paymentMethod = paymentMethod,
                currencyCode = currencyCode
            )
            ensureOk(response)
            val data = response.data as? JsonObject ?: JsonObject(emptyMap())
            val payload = json.decodeFromJsonElement<FeeTransactionsPayload>(data)
            payload.items to payload.pagination.total
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
            payload.items to payload.pagination.total
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "PaymentsRepository::getFeeBatches", "Error loading fee batches. Error: ${e.message ?: "UNKNOWN"}"))
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
