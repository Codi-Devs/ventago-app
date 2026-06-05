package com.teco.ventago.features.invoicing.data.repository

import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.invoicing.data.provider.IInvoicingSettingsProvider
import com.teco.ventago.features.invoicing.domain.models.BottomNoteSettings
import com.teco.ventago.features.invoicing.domain.models.BottomNoteSettingsRequest
import com.teco.ventago.json
import com.teco.ventago.utils.BadRequestException
import com.teco.ventago.utils.isError
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

class InvoicingSettingsRepository(
    private val provider: IInvoicingSettingsProvider,
    private val logger: ILoggerService,
) : IInvoicingSettingsRepository {
    override suspend fun getBottomNoteSettings(businessId: Int): BottomNoteSettings? {
        return try {
            val response = provider.getBottomNoteSettings(businessId)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }
            val data = response.data
            if (data == null || data is JsonNull) {
                null
            } else {
                json.decodeFromJsonElement(BottomNoteSettings.serializer(), data.jsonObject)
            }
        } catch (e: Exception) {
            logError("getBottomNoteSettings", businessId, e)
            throw e
        }
    }

    override suspend fun createBottomNoteSettings(
        businessId: Int,
        request: BottomNoteSettingsRequest,
    ): BottomNoteSettings {
        return mutateBottomNoteSettings(
            action = "createBottomNoteSettings",
            businessId = businessId,
            request = request,
            call = provider::createBottomNoteSettings,
        )
    }

    override suspend fun updateBottomNoteSettings(
        businessId: Int,
        request: BottomNoteSettingsRequest,
    ): BottomNoteSettings {
        return mutateBottomNoteSettings(
            action = "updateBottomNoteSettings",
            businessId = businessId,
            request = request,
            call = provider::updateBottomNoteSettings,
        )
    }

    override suspend fun deleteBottomNoteSettings(businessId: Int): Boolean {
        return try {
            val response = provider.deleteBottomNoteSettings(businessId)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }
            response.successful
        } catch (e: Exception) {
            logError("deleteBottomNoteSettings", businessId, e)
            throw e
        }
    }

    private suspend fun mutateBottomNoteSettings(
        action: String,
        businessId: Int,
        request: BottomNoteSettingsRequest,
        call: suspend (Int, BottomNoteSettingsRequest) -> com.teco.ventago.utils.ApiResponse,
    ): BottomNoteSettings {
        return try {
            val response = call(businessId, request)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }
            val data = response.data?.jsonObject ?: throw BadRequestException("Missing data")
            json.decodeFromJsonElement(BottomNoteSettings.serializer(), data)
        } catch (e: Exception) {
            logError(action, businessId, e)
            throw e
        }
    }

    private fun logError(action: String, businessId: Int, e: Exception) {
        logger.sendLog(
            Log(
                LogLevel.ERROR,
                "InvoicingSettingsRepository::$action",
                "Error in $action. businessId: $businessId, error: ${e.message ?: "UNKNOWN"}",
            ),
        )
    }
}
