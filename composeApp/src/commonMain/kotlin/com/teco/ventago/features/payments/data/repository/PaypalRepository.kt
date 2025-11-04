package com.teco.ventago.features.payments.data.repository

import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.payments.data.provider.IPaypalProvider
import com.teco.ventago.utils.BadRequestException
import com.teco.ventago.utils.isError
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

interface IPaypalRepository {
    suspend fun connect(businessId: Int): String
    suspend fun createBillingAgreement(businessId: Int): String
    suspend fun unlink(businessId: Int): Boolean
}

class PaypalRepository(val provider: IPaypalProvider, private val logger: ILoggerService): IPaypalRepository {
    override suspend fun connect(businessId: Int): String {
        try {
            val response = provider.connect(businessId)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            if (response.successful) {
                return response.data?.jsonPrimitive?.contentOrNull ?: ""
            } else {
                logger.sendLog(Log(LogLevel.ERROR, "Paypal::connect", "Error connecting business. Error: ${response.error}"))
                throw BadRequestException("Error connecting business. Error: ${response.error}")
            }
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "Paypal::connect", "Error connecting business. Error: ${e.message ?: "UNKNOWN" }"))
            throw e
        }
    }


    override suspend fun createBillingAgreement(businessId: Int): String {
        try {
            val response = provider.createBillingAgreement(businessId)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            if (response.successful) {
                return response.data?.jsonPrimitive?.contentOrNull ?: ""
            } else {
                logger.sendLog(Log(LogLevel.ERROR, "Paypal::createBillingAgreement", "Error creating billing agreement. Error: ${response.error}"))
                throw BadRequestException("Error creating billing agreement. Error: ${response.error}")
            }
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "Paypal::createBillingAgreement", "Error creating billing agreement. Error: ${e.message ?: "UNKNOWN" }"))
            throw e
        }
    }


    override suspend fun unlink(businessId: Int): Boolean {
        try {
            val response = provider.unlink(businessId)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            return response.successful
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "unlink", "Error unlink paypal account. Error: ${e.message ?: "UNKNOWN" }"))
            throw e
        }
    }

}