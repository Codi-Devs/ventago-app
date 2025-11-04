package com.teco.ventago.features.settings.data.provider

import com.teco.ventago.Configs
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.business.domain.model.BusinessAddress
import com.teco.ventago.features.business.domain.model.BusinessSocialNetwork
import com.teco.ventago.utils.ApiError
import com.teco.ventago.utils.ApiResponse
import com.teco.ventago.utils.generateHashWithHmac256
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive

class SettingsProvider(private val client: HttpClient, private val authService: IAuthService): ISettingsProvider {
    override suspend fun allowWhatsappOrders(allow: Boolean, businessId: Int): ApiResponse {
        val res = client.post(Configs.serverBasePath+"bussiness/allow-whatsapp-orders"){
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.ContentType, "application/json; charset=UTF-8")
                append("http-x-api-token", generateHashWithHmac256(businessId.toString())!!)
            }
            contentType(ContentType.Application.Json)
            setBody(SettingsRequests.allowWhatsapp(businessId, allow))
        }

        val body = res.body<JsonObject>()
        val status = body["status"]!!.jsonPrimitive.boolean
        return if (status) {
            ApiResponse(true, JsonPrimitive(true),ApiError.NO_ERROR)
        } else {
            ApiResponse(false, JsonPrimitive(false),ApiError.NO_ERROR)
        }
    }

    override suspend fun updateBusinessSocialNetworks(
        social: BusinessSocialNetwork,
        businessId: Int,
    ): ApiResponse {
        val res = client.post(Configs.serverBasePath+"bussiness/update-business-social-networks"){
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.ContentType, "application/json; charset=UTF-8")
                append("http-x-api-token", generateHashWithHmac256(businessId.toString())!!)
            }
            contentType(ContentType.Application.Json)
            setBody(SettingsRequests.updateBusinessSocialNetworks(businessId, social))
        }

        val body = res.body<JsonObject>()
        val status = body["status"]!!.jsonPrimitive.boolean
        return if (status) {
            ApiResponse(true, JsonPrimitive(true),ApiError.NO_ERROR)
        } else {
            ApiResponse(false, JsonPrimitive(false),ApiError.NO_ERROR)
        }
    }

    override suspend fun changeBusinessName(name: String, businessId: Int): ApiResponse {
        val res = client.post(Configs.serverBasePath+"business/change-name") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json; charset=UTF-8")
            }
            contentType(ContentType.Application.Json)
            setBody(SettingsRequests.changeBusinessName(businessId, name))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                changeBusinessName(name, businessId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun changeBusinessInfo(name: String, phone: String, ruc: String, website: String, businessEmail: String, businessId: Int): ApiResponse {
        val res = client.post(Configs.serverBasePath+"business/update-info") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json; charset=UTF-8")
            }
            contentType(ContentType.Application.Json)
            setBody(SettingsRequests.changeBusinessInfo(name, phone, ruc, website,businessEmail, businessId))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                changeBusinessInfo(name, phone, ruc, website,businessEmail, businessId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun changeBusinessPhone(phone: String, businessId: Int): ApiResponse {
        val res = client.post(Configs.serverBasePath+"business/update-phone") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
            }
            contentType(ContentType.Application.Json)
            setBody(SettingsRequests.changeBusinessPhone(businessId, phone))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                changeBusinessPhone(phone, businessId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun changeBusinessAddress(
        address: BusinessAddress,
        businessId: Int,
    ): ApiResponse {
        val res = client.post(Configs.serverBasePath+"business/update-business-address") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
            }
            contentType(ContentType.Application.Json)
            setBody(SettingsRequests.changeBusinessAddress(address, businessId))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                changeBusinessAddress(address, businessId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun updateBusinessLogo(businessId: Int, logo: String): ApiResponse {
        val res = client.post(Configs.serverBasePath+"business/update-business-logo") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
            }
            contentType(ContentType.Application.Json)
            setBody(SettingsRequests.updateBusinessLogo(businessId, logo))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                updateBusinessLogo(businessId, logo)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }
}