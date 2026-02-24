package com.teco.ventago.features.customers.data.repository

import com.teco.ventago.core.Paged
import com.teco.ventago.core.cache.room.models.json
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.customers.data.provider.ICustomerProvider
import com.teco.ventago.features.customers.data.repository.dto.CustomerCreatedDto
import com.teco.ventago.features.customers.domain.models.CreateBillingAddressRequest
import com.teco.ventago.features.customers.domain.models.CustomerAddress
import com.teco.ventago.features.customers.domain.models.Customer
import com.teco.ventago.features.customers.domain.models.CustomerDetails
import com.teco.ventago.features.customers.domain.models.CustomerListItem
import com.teco.ventago.features.customers.domain.models.UpdateBillingAddressRequest
import com.teco.ventago.features.customers.domain.models.UpdateCustomerDetailsRequest
import com.teco.ventago.features.customers.domain.models.ValidateRucResponse
import com.teco.ventago.utils.ApiError
import com.teco.ventago.utils.BadRequestException
import com.teco.ventago.utils.InvalidRucException
import com.teco.ventago.utils.isError
import kotlinx.serialization.json.JsonArray
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
    suspend fun getCustomerById(businessId: Int, customerId: Long): CustomerDetails
    suspend fun updateCustomerDetails(
        businessId: Int,
        customerId: Long,
        request: UpdateCustomerDetailsRequest
    ): Boolean

    suspend fun deleteCustomer(businessId: Int, customerId: Long): Boolean
    suspend fun listCustomerAddresses(businessId: Int, invoiceCustomerId: Int): List<CustomerAddress>
    suspend fun createCustomerAddress(
        businessId: Int,
        customerId: Long,
        request: CreateBillingAddressRequest
    ): Boolean

    suspend fun updateCustomerAddress(
        businessId: Int,
        customerId: Long,
        addressId: Long,
        request: UpdateBillingAddressRequest
    ): Boolean

    suspend fun deleteCustomerAddress(
        businessId: Int,
        customerId: Long,
        addressId: Long
    ): Boolean
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
                    "Error creating customer. businessId: $businessId, customerName: ${customer.name}. Error: ${e.message ?: "UNKNOWN"}"
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
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            if (response.data is JsonObject) {
                return json.decodeFromJsonElement<Paged<CustomerListItem>>(response.data)
            } else {
                throw BadRequestException(response.toJson())
            }
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "CustomerRepository::listCustomers",
                    "Error listing customers. businessId: $businessId, page: $page, size: $size, ruc: ${ruc ?: "-"}, email: ${email ?: "-"}, name: ${name ?: "-"}. Error: ${e.message ?: "UNKNOWN"}"
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

    override suspend fun getCustomerById(businessId: Int, customerId: Long): CustomerDetails {
        try {
            val response = provider.getCustomerById(businessId, customerId)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            if (response.data is JsonObject) {
                return json.decodeFromJsonElement<CustomerDetails>(response.data)
            } else {
                throw BadRequestException(response.toJson())
            }
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "CustomerRepository::getCustomerById",
                    "Error getting customer details. businessId: $businessId, customerId: $customerId. Error: ${e.message ?: "UNKNOWN"}"
                )
            )
            throw e
        }
    }

    override suspend fun updateCustomerDetails(
        businessId: Int,
        customerId: Long,
        request: UpdateCustomerDetailsRequest
    ): Boolean {
        try {
            val response = provider.updateCustomerDetails(businessId, customerId, request)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }
            return true
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "CustomerRepository::updateCustomerDetails",
                    "Error updating customer details. businessId: $businessId, customerId: $customerId. Error: ${e.message ?: "UNKNOWN"}"
                )
            )
            throw e
        }
    }

    override suspend fun deleteCustomer(businessId: Int, customerId: Long): Boolean {
        try {
            val response = provider.deleteCustomer(businessId, customerId)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }
            return true
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "CustomerRepository::deleteCustomer",
                    "Error deleting customer. businessId: $businessId, customerId: $customerId. Error: ${e.message ?: "UNKNOWN"}"
                )
            )
            throw e
        }
    }

    override suspend fun listCustomerAddresses(
        businessId: Int,
        invoiceCustomerId: Int
    ): List<CustomerAddress> {
        try {
            val response = provider.listCustomerAddresses(businessId, invoiceCustomerId)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            val dataArray = response.data as? JsonArray ?: return emptyList()
            return dataArray.mapNotNull { item ->
                runCatching { json.decodeFromJsonElement<CustomerAddress>(item) }.getOrNull()
            }
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "CustomerRepository::listCustomerAddresses",
                    "Error listing customer addresses. businessId: $businessId, invoiceCustomerId: $invoiceCustomerId. Error: ${e.message ?: "UNKNOWN"}"
                )
            )
            throw e
        }
    }

    override suspend fun createCustomerAddress(
        businessId: Int,
        customerId: Long,
        request: CreateBillingAddressRequest
    ): Boolean {
        try {
            val response = provider.createCustomerAddress(businessId, customerId, request)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }
            return true
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "CustomerRepository::createCustomerAddress",
                    "Error creating customer address. businessId: $businessId, customerId: $customerId. Error: ${e.message ?: "UNKNOWN"}"
                )
            )
            throw e
        }
    }

    override suspend fun updateCustomerAddress(
        businessId: Int,
        customerId: Long,
        addressId: Long,
        request: UpdateBillingAddressRequest
    ): Boolean {
        try {
            val response = provider.updateCustomerAddress(businessId, customerId, addressId, request)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }
            return true
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "CustomerRepository::updateCustomerAddress",
                    "Error updating customer address. businessId: $businessId, customerId: $customerId, addressId: $addressId. Error: ${e.message ?: "UNKNOWN"}"
                )
            )
            throw e
        }
    }

    override suspend fun deleteCustomerAddress(
        businessId: Int,
        customerId: Long,
        addressId: Long
    ): Boolean {
        try {
            val response = provider.deleteCustomerAddress(businessId, customerId, addressId)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }
            return true
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "CustomerRepository::deleteCustomerAddress",
                    "Error deleting customer address. businessId: $businessId, customerId: $customerId, addressId: $addressId. Error: ${e.message ?: "UNKNOWN"}"
                )
            )
            throw e
        }
    }
}
