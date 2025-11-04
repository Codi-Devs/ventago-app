package com.teco.ventago.features.financialProfile.data.repository

import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.financialProfile.data.provider.IFinancialProfileProvider
import com.teco.ventago.features.financialProfile.domain.model.BusinessFinancialProfile
import com.teco.ventago.utils.BadRequestException
import com.teco.ventago.utils.isError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

interface IFinancialProfileRepository {
    suspend fun getFinancialProfile(businessId: Int): BusinessFinancialProfile
}


class FinancialProfileRepository (val provider: IFinancialProfileProvider, private val logger: ILoggerService): IFinancialProfileRepository {

    val json = Json {
        ignoreUnknownKeys = true // Optional: skip unknown fields
    }

    override suspend fun getFinancialProfile(businessId: Int): BusinessFinancialProfile {
        try {
            val response = provider.getFinancialProfile(businessId)
            println("ASDASD: Response from backend: ${response.toJson()}")
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }


            if (response.data is JsonObject) {
                return json.decodeFromJsonElement<BusinessFinancialProfile>(response.data)
            } else {
                throw BadRequestException(response.toJson())
            }
        } catch (e: Exception) {
            println("ASDASD: Error getting business financial profile. Error: ${e.message ?: "UNKNOWN" }")
            logger.sendLog(Log(LogLevel.ERROR, "FinancialProfileRepository::getFinancialProfile", "Error getting business financial profile. Error: ${e.message ?: "UNKNOWN" }"))
            throw e
        }
    }
}