package com.teco.ventago.features.business.data.repository

import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.business.data.provider.IBusinessProvider
import com.teco.ventago.features.business.domain.model.Business
import com.teco.ventago.features.business.domain.model.requests.RegisterBusinessRequest
import com.teco.ventago.features.business.domain.model.responses.BusinessRegisterResponse
import com.teco.ventago.utils.BadRequestException
import com.teco.ventago.utils.isError
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class BusinessRepository(private val provider: IBusinessProvider, private val logger: ILoggerService): IBusinessRepository {
    override suspend fun updateWebStyle(
        businessId: Int,
        styleId: Int,
        primaryColor: String,
        secondaryColor: String,
    ): Boolean {
        return try {
            val response = provider.updateWebStyle(businessId, styleId, primaryColor, secondaryColor)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            if (response.data is JsonPrimitive) {
                response.data.boolean
            } else {
                false
            }
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR, "updateWebStyle",
                    "Error updating web style. Error: ${e.message ?: "UNKNOWN" }, businessId: $businessId, styleId: $styleId, primaryColor: $primaryColor, secondaryColor: $secondaryColor"
                )
            )
            throw e
        }
    }

    override suspend fun resetColors(businessId: Int): Boolean {
        return try {
            val response = provider.resetColors(businessId)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            if (response.data is JsonPrimitive) {
                response.data.boolean
            } else {
                false
            }
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR, "resetColors",
                    "Error resetting colors. Error: ${e.message ?: "UNKNOWN" }, businessId: $businessId"
                )
            )
            throw e
        }
    }

    override suspend fun registerBusiness(request: RegisterBusinessRequest): BusinessRegisterResponse {
        return try {
            val response = provider.registerBusiness(request)

            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            if (response.data is JsonObject) {
                BusinessRegisterResponse(
                    response.data["business_id"]?.jsonPrimitive?.int ?: -1,
                    response.data["menu_id"]?.jsonPrimitive?.int ?: -1,
                )
            } else {
                throw BadRequestException(response.toJson())
            }
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "Business::registerBusiness", "Error registering Business. Error: ${e.message ?: "UNKNOWN" }"))
            throw e
        }
    }

    override suspend fun getBusinessById(businessId: Int): Business {
        return try {
            val response = provider.getBusinessById(businessId)

            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            if (response.data is JsonObject) {
                Business(response.data)
            } else {
                throw BadRequestException(response.toJson())
            }
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "Business::getBusinessById", "Error getting Business. Error: ${e.message ?: "UNKNOWN" }"))
            throw e
        }
    }

    override suspend fun getBusinesses(): List<Business> {
        return try {
            val response = provider.getBusinessesByUser()

            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }
            val businesses = mutableListOf<Business>()
            if (response.data is JsonArray) {
                val arr = response.data.jsonArray
                for (item in arr) {
                    val business = Business(item.jsonObject)
                    businesses.add(business)
                }
            }
            businesses
        } catch (e: Exception) {
            logger.sendLog(Log(LogLevel.ERROR, "Business::getBusinesses", "Error getting Businesses. Error: ${e.message ?: "UNKNOWN" }"))
            throw e
        }
    }

    override suspend fun deleteBusiness(businessId: Int): Boolean {
        return try {
            val response = provider.removeBusiness(businessId)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            if (response.data is JsonPrimitive) {
                response.data.boolean
            } else {
                false
            }
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR, "deleteBusiness",
                    "Error deleting business. Error: ${e.message ?: "UNKNOWN" }, businessId: $businessId"
                )
            )
            throw e
        }
    }


}