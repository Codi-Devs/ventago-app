package com.teco.ventago.features.payments.data.provider

import com.teco.ventago.Configs
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.payments.domain.models.AchAccountConfigRequest
import com.teco.ventago.features.payments.domain.models.DirectCheckoutRequest
import com.teco.ventago.features.payments.domain.models.TiloPayCredentialsRequest
import com.teco.ventago.features.payments.domain.models.YappyOnsiteCancelPendingRequest
import com.teco.ventago.features.payments.domain.models.YappyOnsiteCancelRequest
import com.teco.ventago.features.payments.domain.models.YappyOnsiteDeviceConfigRequest
import com.teco.ventago.features.payments.domain.models.YappyOnsiteGroupConfigRequest
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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject


interface IPaymentsProvider {
    suspend fun onboardPayments(businessId: Int): ApiResponse
    suspend fun transference(businessId: Int, enabled: Boolean, instructions: String): ApiResponse
    suspend fun setAutoInvoice(businessId: Int, enabled: Boolean): ApiResponse
    suspend fun getAchStatus(businessId: Int): ApiResponse
    suspend fun getAchAccount(businessId: Int): ApiResponse
    suspend fun configureAchAccount(businessId: Int, request: AchAccountConfigRequest): ApiResponse
    suspend fun disableAch(businessId: Int): ApiResponse
    suspend fun getTiloPayStatus(businessId: Int): ApiResponse
    suspend fun configureTiloPayCredentials(businessId: Int, request: TiloPayCredentialsRequest): ApiResponse
    suspend fun disconnectTiloPay(businessId: Int): ApiResponse
    suspend fun getFeesSummary(businessId: Int, currencyCode: String): ApiResponse
    suspend fun getFeeTransactions(
        businessId: Int,
        page: Int,
        size: Int,
        status: String?,
        paymentMethod: String?,
        batchId: Long?,
        currencyCode: String
    ): ApiResponse
    suspend fun getFeeBatches(
        businessId: Int,
        page: Int,
        size: Int,
        status: String?,
        currencyCode: String
    ): ApiResponse
    suspend fun configureYappyOnsiteGroup(businessId: Int, groupId: String, request: YappyOnsiteGroupConfigRequest): ApiResponse
    suspend fun listYappyOnsiteGroups(businessId: Int): ApiResponse
    suspend fun deleteYappyOnsiteGroup(businessId: Int, groupId: String): ApiResponse
    suspend fun registerYappyOnsiteDevice(businessId: Int, groupId: String, request: YappyOnsiteDeviceConfigRequest): ApiResponse
    suspend fun updateYappyOnsiteDevice(businessId: Int, groupId: String, deviceId: String, request: YappyOnsiteDeviceConfigRequest): ApiResponse
    suspend fun deleteYappyOnsiteDevice(businessId: Int, groupId: String, deviceId: String): ApiResponse
    suspend fun listYappyOnsiteDevices(businessId: Int, groupId: String): ApiResponse
    suspend fun getYappyOnsiteTransaction(businessId: Int, transactionId: String): ApiResponse
    suspend fun cancelYappyOnsiteTransaction(businessId: Int, transactionId: String, request: YappyOnsiteCancelRequest): ApiResponse
    suspend fun cancelPendingYappyOnsiteTransaction(businessId: Int, request: YappyOnsiteCancelPendingRequest): ApiResponse
    suspend fun createDirectCheckout(businessId: Int, request: DirectCheckoutRequest): ApiResponse
}

class PaymentsProvider (private val client: HttpClient, private val authService: IAuthService): IPaymentsProvider {
    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    override suspend fun onboardPayments(businessId: Int): ApiResponse {
        val res = client.post(Configs.ordersBasePath+"/api/v1/business/payments/onboard") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(PaypalRequests.businessIdRequest(businessId))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                onboardPayments(businessId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun transference(businessId: Int, enabled: Boolean, instructions: String): ApiResponse {
        val res = client.put(Configs.ordersBasePath+"/api/v1/business/payment-methods/transference") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(PaypalRequests.transference(businessId, enabled, instructions))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                transference(businessId, enabled, instructions)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun setAutoInvoice(businessId: Int, enabled: Boolean): ApiResponse {
        val res = client.put(Configs.ordersBasePath + "/api/v1/business/payment-methods/auto-invoice") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("enabled", JsonPrimitive(enabled))
            })
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                setAutoInvoice(businessId, enabled)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun getAchStatus(businessId: Int): ApiResponse {
        val res = client.get(Configs.ordersBasePath + "/api/v1/payments/ach/status") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                getAchStatus(businessId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun getAchAccount(businessId: Int): ApiResponse {
        val res = client.get(Configs.ordersBasePath + "/api/v1/payments/ach/account") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                getAchAccount(businessId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun configureAchAccount(businessId: Int, request: AchAccountConfigRequest): ApiResponse {
        val res = client.put(Configs.ordersBasePath + "/api/v1/payments/ach/account") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(AchAccountConfigRequest.serializer(), request))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                configureAchAccount(businessId, request)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun disableAch(businessId: Int): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/payments/ach/disable") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { })
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                disableAch(businessId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun getTiloPayStatus(businessId: Int): ApiResponse {
        val res = client.get(Configs.ordersBasePath + "/api/v1/business/payment-methods/card/tilopay/status") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                getTiloPayStatus(businessId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun configureTiloPayCredentials(
        businessId: Int,
        request: TiloPayCredentialsRequest
    ): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/business/payment-methods/card/tilopay/credentials") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(TiloPayCredentialsRequest.serializer(), request))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                configureTiloPayCredentials(businessId, request)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun disconnectTiloPay(businessId: Int): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/business/payment-methods/card/tilopay/disconnect") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { })
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                disconnectTiloPay(businessId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun getFeesSummary(businessId: Int, currencyCode: String): ApiResponse {
        val res = client.get(Configs.ordersBasePath + "/api/v1/payments/fees/summary") {
            url { parameters.append("currency_code", currencyCode) }
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                getFeesSummary(businessId, currencyCode)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun getFeeTransactions(
        businessId: Int,
        page: Int,
        size: Int,
        status: String?,
        paymentMethod: String?,
        batchId: Long?,
        currencyCode: String
    ): ApiResponse {
        val res = client.get(Configs.ordersBasePath + "/api/v1/payments/fees/transactions") {
            url {
                parameters.append("page", page.toString())
                parameters.append("size", size.toString())
                status?.takeIf { it.isNotBlank() }?.let { parameters.append("status", it) }
                paymentMethod?.takeIf { it.isNotBlank() }?.let { parameters.append("payment_method", it) }
                batchId?.let { parameters.append("batch_id", it.toString()) }
                parameters.append("currency_code", currencyCode)
            }
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                getFeeTransactions(businessId, page, size, status, paymentMethod, batchId, currencyCode)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun getFeeBatches(
        businessId: Int,
        page: Int,
        size: Int,
        status: String?,
        currencyCode: String
    ): ApiResponse {
        val res = client.get(Configs.ordersBasePath + "/api/v1/payments/fees/batches") {
            url {
                parameters.append("page", page.toString())
                parameters.append("size", size.toString())
                status?.takeIf { it.isNotBlank() }?.let { parameters.append("status", it) }
                parameters.append("currency_code", currencyCode)
            }
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                getFeeBatches(businessId, page, size, status, currencyCode)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun configureYappyOnsiteGroup(
        businessId: Int,
        groupId: String,
        request: YappyOnsiteGroupConfigRequest
    ): ApiResponse {
        val res = client.put(Configs.ordersBasePath + "/api/v1/payments/yappy/onsite/groups/$groupId") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(YappyOnsiteGroupConfigRequest.serializer(), request))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                configureYappyOnsiteGroup(businessId, groupId, request)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun listYappyOnsiteGroups(businessId: Int): ApiResponse {
        val res = client.get(Configs.ordersBasePath + "/api/v1/payments/yappy/onsite/groups") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                listYappyOnsiteGroups(businessId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun deleteYappyOnsiteGroup(businessId: Int, groupId: String): ApiResponse {
        val res = client.delete(Configs.ordersBasePath + "/api/v1/payments/yappy/onsite/groups/$groupId") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                deleteYappyOnsiteGroup(businessId, groupId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun registerYappyOnsiteDevice(
        businessId: Int,
        groupId: String,
        request: YappyOnsiteDeviceConfigRequest
    ): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/payments/yappy/onsite/groups/$groupId/devices") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(YappyOnsiteDeviceConfigRequest.serializer(), request))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                registerYappyOnsiteDevice(businessId, groupId, request)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun updateYappyOnsiteDevice(
        businessId: Int,
        groupId: String,
        deviceId: String,
        request: YappyOnsiteDeviceConfigRequest
    ): ApiResponse {
        val res = client.put(Configs.ordersBasePath + "/api/v1/payments/yappy/onsite/groups/$groupId/devices/$deviceId") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(YappyOnsiteDeviceConfigRequest.serializer(), request))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                updateYappyOnsiteDevice(businessId, groupId, deviceId, request)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun deleteYappyOnsiteDevice(
        businessId: Int,
        groupId: String,
        deviceId: String
    ): ApiResponse {
        val res = client.delete(Configs.ordersBasePath + "/api/v1/payments/yappy/onsite/groups/$groupId/devices/$deviceId") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                deleteYappyOnsiteDevice(businessId, groupId, deviceId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun listYappyOnsiteDevices(businessId: Int, groupId: String): ApiResponse {
        val res = client.get(Configs.ordersBasePath + "/api/v1/payments/yappy/onsite/groups/$groupId/devices") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                listYappyOnsiteDevices(businessId, groupId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun getYappyOnsiteTransaction(businessId: Int, transactionId: String): ApiResponse {
        val res = client.get(Configs.ordersBasePath + "/api/v1/payments/yappy/onsite/transactions/$transactionId") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                getYappyOnsiteTransaction(businessId, transactionId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun cancelYappyOnsiteTransaction(
        businessId: Int,
        transactionId: String,
        request: YappyOnsiteCancelRequest
    ): ApiResponse {
        val res = client.put(Configs.ordersBasePath + "/api/v1/payments/yappy/onsite/transactions/$transactionId/cancel") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(YappyOnsiteCancelRequest.serializer(), request))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                cancelYappyOnsiteTransaction(businessId, transactionId, request)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun cancelPendingYappyOnsiteTransaction(
        businessId: Int,
        request: YappyOnsiteCancelPendingRequest
    ): ApiResponse {
        val res = client.put(Configs.ordersBasePath + "/api/v1/payments/yappy/onsite/transactions/cancel-pending") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(YappyOnsiteCancelPendingRequest.serializer(), request))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                cancelPendingYappyOnsiteTransaction(businessId, request)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun createDirectCheckout(
        businessId: Int,
        request: DirectCheckoutRequest
    ): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/business/billing/direct-checkouts") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(DirectCheckoutRequest.serializer(), request))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                createDirectCheckout(businessId, request)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

}
