package com.teco.ventago.features.payments.data.repository

import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.payments.data.provider.IPaymentsProvider
import com.teco.ventago.utils.BadRequestException
import com.teco.ventago.utils.isError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

interface IPaymentsRepository {
    suspend fun onboardPayments(businessId: Int): Boolean
    suspend fun transference(businessId: Int, enabled: Boolean, instructions: String): Boolean
}

class PaymentsRepository(val provider: IPaymentsProvider, private val logger: ILoggerService): IPaymentsRepository {

    val json = Json {
        ignoreUnknownKeys = true // Optional: skip unknown fields
    }

    override suspend fun onboardPayments(businessId: Int): Boolean {
        try {
            val response = provider.onboardPayments(businessId)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            return response.successful && response.data?.jsonPrimitive?.booleanOrNull ?: false
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "Paypal::connect", "Error connecting business. Error: ${e.message ?: "UNKNOWN" }"))
            throw e
        }
    }

    override suspend fun transference(businessId: Int, enabled: Boolean, instructions: String): Boolean {
        try {
            val response = provider.transference(businessId, enabled, instructions)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            return response.successful && response.data?.jsonPrimitive?.booleanOrNull ?: false
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "Paypal::connect", "Error connecting business. Error: ${e.message ?: "UNKNOWN" }"))
            throw e
        }
    }

}