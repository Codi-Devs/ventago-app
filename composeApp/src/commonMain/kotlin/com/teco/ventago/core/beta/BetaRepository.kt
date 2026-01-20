package com.teco.ventago.core.beta

import com.teco.ventago.core.beta.models.BetaFeaturesResponse
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.utils.BadRequestException
import com.teco.ventago.utils.isError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class BetaRepository(
    private val provider: BetaProvider,
    private val logger: ILoggerService,
    private val json: Json
) {

    suspend fun getFeatures(businessId: Int): BetaFeaturesResponse {
        try {
            val response = provider.listFeatures(businessId)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }
            val dataObj = response.data?.jsonObject
                ?: JsonObject(emptyMap())
            return json.decodeFromJsonElement(BetaFeaturesResponse.serializer(), dataObj)
        } catch (e: Exception) {
            logger.sendLog(
                com.teco.ventago.core.logger.Log(
                    level = com.teco.ventago.core.logger.LogLevel.ERROR,
                    flow = "BetaRepository::getFeatures",
                    message = "Error getting features: ${e.message ?: "Unknown"}",
                )
            )
            return BetaFeaturesResponse(emptyList())
        }

    }
}
