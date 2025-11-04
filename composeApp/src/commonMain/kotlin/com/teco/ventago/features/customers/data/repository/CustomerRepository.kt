package com.teco.ventago.features.customers.data.repository

import com.teco.ventago.core.Paged
import com.teco.ventago.core.cache.room.models.json
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.customers.data.provider.ICustomerProvider
import com.teco.ventago.features.customers.data.repository.dto.CustomerCreatedDto
import com.teco.ventago.features.customers.domain.models.Customer
import com.teco.ventago.features.customers.domain.models.CustomerListItem
import com.teco.ventago.features.customers.domain.models.ValidateRucResponse
import com.teco.ventago.features.financialProfile.domain.model.BusinessFinancialProfile
import com.teco.ventago.utils.ApiError
import com.teco.ventago.utils.ApiResponse
import com.teco.ventago.utils.BadRequestException
import com.teco.ventago.utils.InvalidRucException
import com.teco.ventago.utils.isError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

interface ICustomerRepository {
    suspend fun createCustomer(customer: Customer, businessId: Int): CustomerCreatedDto
    suspend fun listCustomers(
        businessId: Int,
        page: Int,
        size: Int,
        ruc: String?,
        email: String?,
        name: String?
    ): Paged<CustomerListItem>

    suspend fun validateRUC(ruc: String, businessId: Int): ValidateRucResponse
    suspend fun validateRUCRegister(ruc: String): ValidateRucResponse
}

class CustomerRepository(
    private val provider: ICustomerProvider,
    private val logger: ILoggerService
) : ICustomerRepository {
    override suspend fun createCustomer(customer: Customer, businessId: Int): CustomerCreatedDto {
        try {
            val response = provider.createCustomer(customer, businessId)
            if (response.error.isError()) {
                if (response.error == ApiError.INVALID_RUC || response.error == ApiError.RUC_NOT_FOUND) {
                    throw InvalidRucException()
                }
                throw BadRequestException(response.toJson())
            }

            if (response.data is JsonObject) {
                return json.decodeFromJsonElement<CustomerCreatedDto>(response.data)
            } else {
                throw BadRequestException(response.toJson())
            }
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "CustomerRepository::createCustomer",
                    "Error creating customer. Error: ${e.message ?: "UNKNOWN"}"
                )
            )
            throw e
        }
    }

    override suspend fun listCustomers(
        businessId: Int,
        page: Int,
        size: Int,
        ruc: String?,
        email: String?,
        name: String?
    ): Paged<CustomerListItem> {
        try {

            val response = provider.listCustomers(businessId, page, size, ruc, email, name)
            println("ASDASD: Response from backend: ${response.toJson()}")
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            if (response.data is JsonObject) {
                return json.decodeFromJsonElement<Paged<CustomerListItem>>(response.data)
            } else {
                throw BadRequestException(response.toJson())
            }
        } catch (e: Exception) {
            println("ASDASD: Error listing customers. Error: ${e.message ?: "UNKNOWN"}")
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "CustomerRepository::listCustomers",
                    "Error listing customers. Error: ${e.message ?: "UNKNOWN"}"
                )
            )
            throw e
        }
    }

    override suspend fun validateRUC(ruc: String, businessId: Int): ValidateRucResponse {
        try {
            val response = provider.validateRUC(ruc, businessId)
            if (response.error.isError()) {
                if (response.error == ApiError.INVALID_RUC || response.error == ApiError.RUC_NOT_FOUND) {
                    throw InvalidRucException()
                }
                throw BadRequestException(response.toJson())
            }

            if (response.data is JsonObject) {
                return json.decodeFromJsonElement<ValidateRucResponse>(response.data)
            } else {
                throw BadRequestException(response.toJson())
            }
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "CustomerRepository::validateRUC",
                    "Error validating ruc. Error: ${e.message ?: "UNKNOWN"}"
                )
            )
            throw e
        }
    }

    override suspend fun validateRUCRegister(ruc: String): ValidateRucResponse {
        try {
            val response = provider.validateRUCRegister(ruc)
            if (response.error.isError()) {
                if (response.error == ApiError.INVALID_RUC || response.error == ApiError.RUC_NOT_FOUND) {
                    throw InvalidRucException()
                }
                throw BadRequestException(response.toJson())
            }

            if (response.data is JsonObject) {
                return json.decodeFromJsonElement<ValidateRucResponse>(response.data)
            } else {
                throw BadRequestException(response.toJson())
            }
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "CustomerRepository::validateRUC",
                    "Error validating ruc. Error: ${e.message ?: "UNKNOWN"}"
                )
            )
            throw e
        }
    }
}