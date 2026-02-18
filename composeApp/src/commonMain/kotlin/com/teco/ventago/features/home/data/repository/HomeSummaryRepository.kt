package com.teco.ventago.features.home.data.repository

import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.home.data.provider.IHomeSummaryProvider
import com.teco.ventago.features.home.domain.model.HomeSummary
import com.teco.ventago.features.home.domain.model.HomeSummaryParser
import com.teco.ventago.utils.BadRequestException
import com.teco.ventago.utils.isError
import kotlinx.serialization.json.JsonObject

class HomeSummaryRepository(
    private val provider: IHomeSummaryProvider,
    private val logger: ILoggerService
) : IHomeSummaryRepository {

    override suspend fun getHomeSummary(businessId: Int): HomeSummary {
        return try {
            val response = provider.getHomeSummary(businessId)
            if (response.error.isError() || !response.successful) {
                throw BadRequestException(response.toJson())
            }

            val data = response.data as? JsonObject ?: throw BadRequestException(response.toJson())
            HomeSummaryParser.parse(data)
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "HomeSummaryRepository::getHomeSummary",
                    "Error getting home summary. businessId: $businessId, error: ${e.message ?: "UNKNOWN"}"
                )
            )
            throw e
        }
    }
}
