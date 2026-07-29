package com.teco.ventago.features.customers.data.provider

import com.teco.ventago.Configs
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.customers.data.repository.dto.CreateCustomerDto
import com.teco.ventago.features.customers.domain.models.CreateBillingAddressRequest
import com.teco.ventago.features.customers.domain.models.Customer
import com.teco.ventago.features.customers.domain.models.nullIfBlank
import com.teco.ventago.features.customers.domain.models.UpdateBillingAddressRequest
import com.teco.ventago.features.customers.domain.models.UpdateCustomerDetailsRequest
import com.teco.ventago.features.customers.domain.models.ValidateRucRequest
import com.teco.ventago.features.invoicing.domain.models.FeCustomerType
import com.teco.ventago.utils.ApiError
import com.teco.ventago.utils.ApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

interface ICustomerProvider {
    suspend fun createCustomer(customer: Customer, businessId: Int): ApiResponse
    suspend fun listCustomers(
        businessId: Int,
        page: Int,
        size: Int,
        ruc: String? = null,
        email: String? = null,
        name: String? = null
    ): ApiResponse

    suspend fun validateRUC(ruc: String, businessId: Int): ApiResponse
    suspend fun validateRUCRegister(ruc: String): ApiResponse
    suspend fun getCustomerById(businessId: Int, customerId: Long): ApiResponse
    suspend fun updateCustomerDetails(
        businessId: Int,
        customerId: Long,
        request: UpdateCustomerDetailsRequest
    ): ApiResponse

    suspend fun deleteCustomer(businessId: Int, customerId: Long): ApiResponse
    suspend fun listCustomerAddresses(businessId: Int, invoiceCustomerId: Int): ApiResponse
    suspend fun createCustomerAddress(
        businessId: Int,
        customerId: Long,
        request: CreateBillingAddressRequest
    ): ApiResponse

    suspend fun updateCustomerAddress(
        businessId: Int,
        customerId: Long,
        addressId: Long,
        request: UpdateBillingAddressRequest
    ): ApiResponse

    suspend fun deleteCustomerAddress(
        businessId: Int,
        customerId: Long,
        addressId: Long
    ): ApiResponse
}

private val createRequestJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    explicitNulls = true
}

val json: Json = createRequestJson

private val updateRequestJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    explicitNulls = true
}

class CustomerProvider(private val client: HttpClient, private val authService: IAuthService) :
    ICustomerProvider {
    override suspend fun createCustomer(customer: Customer, businessId: Int): ApiResponse {
        val requestBody = buildCreateCustomerRequestBody(customer)
        val res = client.post(Configs.ordersBasePath + "/api/v1/customers/create") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                createCustomer(customer, businessId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun listCustomers(
        businessId: Int,
        page: Int,
        size: Int,
        ruc: String?,
        email: String?,
        name: String?
    ): ApiResponse {
        fun String?.nullIfBlank() = this?.takeIf { it.isNotBlank() }

        val res = client.get("${Configs.ordersBasePath}/api/v1/customers/") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            url {
                parameters.append("page", page.toString())
                parameters.append("size", size.toString())
                ruc.nullIfBlank()?.let { parameters.append("ruc", it) }
                email.nullIfBlank()?.let { parameters.append("email", it) }
                name.nullIfBlank()?.let { parameters.append("name", it) }
            }
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)

        // Re-auth and retry once if needed
        return if (response.error == ApiError.AUTH_001) {
            try {
                authService.refreshToken(client)
                listCustomers(businessId, page, size, ruc, email, name)
            } catch (_: Exception) {
                response
            }
        } else {
            response
        }
    }

    override suspend fun validateRUC(ruc: String, businessId: Int): ApiResponse {
        val res = client.post("${Configs.ordersBasePath}/api/v1/customers/validate-ruc") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(ValidateRucRequest(ruc))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)

        // Re-auth and retry once if needed
        return if (response.error == ApiError.AUTH_001) {
            try {
                authService.refreshToken(client)
                validateRUC(ruc, businessId)
            } catch (_: Exception) {
                response
            }
        } else {
            response
        }
    }

    override suspend fun validateRUCRegister(ruc: String): ApiResponse {
        val res = client.post("${Configs.ordersBasePath}/api/v1/customers/register/validate-ruc") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.ContentType, "application/json")
            }
            contentType(ContentType.Application.Json)
            setBody(ValidateRucRequest(ruc))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)

        // Re-auth and retry once if needed
        return if (response.error == ApiError.AUTH_001) {
            try {
                authService.refreshToken(client)
                validateRUCRegister(ruc)
            } catch (_: Exception) {
                response
            }
        } else {
            response
        }
    }

    override suspend fun getCustomerById(businessId: Int, customerId: Long): ApiResponse {
        val res = client.get("${Configs.ordersBasePath}/api/v1/customers/$customerId") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        return if (response.error == ApiError.AUTH_001) {
            try {
                authService.refreshToken(client)
                getCustomerById(businessId, customerId)
            } catch (_: Exception) {
                response
            }
        } else {
            response
        }
    }

    override suspend fun updateCustomerDetails(
        businessId: Int,
        customerId: Long,
        request: UpdateCustomerDetailsRequest
    ): ApiResponse {
        val payload = updateRequestJson.encodeToString(UpdateCustomerDetailsRequest.serializer(), request)
        val res = client.put("${Configs.ordersBasePath}/api/v1/customers/$customerId/details") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(payload)
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        return if (response.error == ApiError.AUTH_001) {
            try {
                authService.refreshToken(client)
                updateCustomerDetails(businessId, customerId, request)
            } catch (_: Exception) {
                response
            }
        } else {
            response
        }
    }

    override suspend fun deleteCustomer(businessId: Int, customerId: Long): ApiResponse {
        val res = client.delete("${Configs.ordersBasePath}/api/v1/customers/$customerId") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        return if (response.error == ApiError.AUTH_001) {
            try {
                authService.refreshToken(client)
                deleteCustomer(businessId, customerId)
            } catch (_: Exception) {
                response
            }
        } else {
            response
        }
    }

    override suspend fun listCustomerAddresses(businessId: Int, invoiceCustomerId: Int): ApiResponse {
        val res = client.get("${Configs.ordersBasePath}/api/v1/invoicing/customers/$invoiceCustomerId/addresses") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)

        return if (response.error == ApiError.AUTH_001) {
            try {
                authService.refreshToken(client)
                listCustomerAddresses(businessId, invoiceCustomerId)
            } catch (_: Exception) {
                response
            }
        } else {
            response
        }
    }

    override suspend fun createCustomerAddress(
        businessId: Int,
        customerId: Long,
        request: CreateBillingAddressRequest
    ): ApiResponse {
        val res = client.post("${Configs.ordersBasePath}/api/v1/invoicing/customers/$customerId/addresses") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        return if (response.error == ApiError.AUTH_001) {
            try {
                authService.refreshToken(client)
                createCustomerAddress(businessId, customerId, request)
            } catch (_: Exception) {
                response
            }
        } else {
            response
        }
    }

    override suspend fun updateCustomerAddress(
        businessId: Int,
        customerId: Long,
        addressId: Long,
        request: UpdateBillingAddressRequest
    ): ApiResponse {
        val res = client.put("${Configs.ordersBasePath}/api/v1/invoicing/customers/$customerId/addresses/$addressId") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        return if (response.error == ApiError.AUTH_001) {
            try {
                authService.refreshToken(client)
                updateCustomerAddress(businessId, customerId, addressId, request)
            } catch (_: Exception) {
                response
            }
        } else {
            response
        }
    }

    override suspend fun deleteCustomerAddress(
        businessId: Int,
        customerId: Long,
        addressId: Long
    ): ApiResponse {
        val res = client.delete("${Configs.ordersBasePath}/api/v1/invoicing/customers/$customerId/addresses/$addressId") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        return if (response.error == ApiError.AUTH_001) {
            try {
                authService.refreshToken(client)
                deleteCustomerAddress(businessId, customerId, addressId)
            } catch (_: Exception) {
                response
            }
        } else {
            response
        }
    }
}

internal fun buildCreateCustomerRequestBody(customer: Customer): String {
    return createRequestJson.encodeToString(buildCreateCustomerDto(customer))
}

internal fun buildCreateCustomerDto(customer: Customer): CreateCustomerDto {
    val countryCode = if (customer.customerType == FeCustomerType.FOREIGNER) {
        customer.countryCode.nullIfBlank() ?: PANAMA_COUNTRY_CODE
    } else {
        PANAMA_COUNTRY_CODE
    }

    return CreateCustomerDto(
        name = customer.name.trim(),
        email = customer.email.nullIfBlank(),
        phone = customer.phone.nullIfBlank(),
        ruc = customer.ruc.nullIfBlank(),
        countryCode = countryCode,
        tags = customer.tags.mapNotNull { it.nullIfBlank() }.takeIf { it.isNotEmpty() }?.joinToString(","),
        customerType = customer.customerType?.code,
        taxPayerType = customer.taxPayerType?.code,
        addressLine = customer.addressLine.nullIfBlank(),
        locationCode = customer.locationCode.nullIfBlank(),
        province = customer.province.nullIfBlank(),
        district = customer.district.nullIfBlank(),
        corregimiento = customer.corregimiento.nullIfBlank(),
        foreignIdType = customer.foreignIdType.nullIfBlank(),
        foreignIdNumber = customer.foreignIdNumber.nullIfBlank(),
        cedulaCF = customer.cedulaCF.nullIfBlank(),
        countryOtherName = null,
        taxExempt = customer.taxExempt,
        taxRetentionCode = customer.taxRetentionCode,
        taxRetentionPercent = customer.taxRetentionPercent,
    )
}

private const val PANAMA_COUNTRY_CODE = "PA"
