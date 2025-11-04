package com.teco.ventago.features.payments.data.repository

import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.payments.data.provider.IYappyProvider
import com.teco.ventago.utils.BadRequestException
import com.teco.ventago.utils.isError


interface IYappyRepository {
    suspend fun connect(businessId: Int, merchantID: String, domain: String, secretKey: String): Boolean
    suspend fun unlink(businessId: Int): Boolean
}

class YappyRepository(val provider: IYappyProvider, private val logger: ILoggerService): IYappyRepository {
    override suspend fun connect(businessId: Int, merchantID: String, domain: String, secretKey: String): Boolean {
        try {
            val response = provider.connect(businessId, merchantID, domain, secretKey)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            return response.successful
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "YappyRepository::connect", "Error connecting business. Error: ${e.message ?: "UNKNOWN" }"))
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
            logger.sendLog(Log(LogLevel.ERROR, "unlink", "Error unlink yappy account. Error: ${e.message ?: "UNKNOWN" }"))
            throw e
        }
    }

}