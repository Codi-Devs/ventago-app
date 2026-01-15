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

    suspend fun getFeatures(): BetaFeaturesResponse {
        val response = provider.listFeatures()
        if (response.error.isError()) {
            throw BadRequestException(response.toJson())
        }
        val dataObj = response.data?.jsonObject
            ?: JsonObject(emptyMap())
        return json.decodeFromJsonElement(BetaFeaturesResponse.serializer(), dataObj)
    }
}
