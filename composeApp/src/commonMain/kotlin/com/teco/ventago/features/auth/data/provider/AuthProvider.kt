package com.teco.ventago.features.auth.data.provider

import com.teco.ventago.utils.ApiResponse
import com.teco.ventago.Configs
import com.teco.ventago.features.auth.domain.model.requests.CreateUserRequest
import com.teco.ventago.features.auth.domain.model.requests.EmailLoginRequest
import com.teco.ventago.utils.ApiError
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
import io.ktor.util.reflect.instanceOf
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive

class AuthProvider(private val client: HttpClient): IAuthProvider {
    override suspend fun userExists(email: String): ApiResponse {
        val res = client.post(Configs.serverBasePath+"auth/user-exists") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append("http-x-api-token", generateHashWithHmac256(email) ?: "")
                append(HttpHeaders.ContentType, "application/json")
            }
            contentType(ContentType.Application.Json)
            setBody(AuthRequests.userExistsRequest(email))
        }

        val body = res.body<JsonObject>()
        return ApiResponse.fromJson(body)
    }

    override suspend fun googleLogin(googleToken: String): ApiResponse {
        val res = client.post(Configs.serverBasePath+"auth/google-login") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.ContentType, "application/json")
            }
            contentType(ContentType.Application.Json)
            setBody(AuthRequests.googleLoginRequest(googleToken))
        }
        val body = res.body<JsonObject>()
        return ApiResponse.fromJson(body)
    }

    override suspend fun emailLogin(request: EmailLoginRequest): ApiResponse {
        val res = client.post(Configs.serverBasePath+"auth/email-login881j-iasq9js921") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.ContentType, "application/json")
            }
            contentType(ContentType.Application.Json)
            setBody(AuthRequests.emailLoginRequest(request))
        }
        val body = res.body<JsonObject>()
        return ApiResponse.fromJson(body)
    }

    override suspend fun emailRegister(request: CreateUserRequest): ApiResponse {
        val res = client.post(Configs.serverBasePath+"auth/email-register881j-iasq9js921") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.ContentType, "application/json")
            }
            contentType(ContentType.Application.Json)
            setBody(AuthRequests.emailRegisterRequest(request))
        }
        val body = res.body<JsonObject>()
        return ApiResponse.fromJson(body)
    }

    override suspend fun forgotPassword(email: String): ApiResponse {
        val res = client.post(Configs.serverBasePath+"auth/forgot-password") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append("http-x-api-token", generateHashWithHmac256(email) ?: "")
                append(HttpHeaders.ContentType, "application/json")
            }
            contentType(ContentType.Application.Json)
            setBody(AuthRequests.userExistsRequest(email))
        }

        val body = res.body<JsonObject>()
        return ApiResponse.fromJson(body)
    }

    override suspend fun deleteAccount(token: String): ApiResponse {
        val res = client.post(Configs.serverBasePath+"user/delete-account") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.ContentType, "application/json")
            }
            contentType(ContentType.Application.Json)
            setBody("{\"token\": \"$token\"}")
        }
        if (!res.status.isSuccess()) {
            return ApiResponse(false, JsonPrimitive(false), ApiError.NO_ERROR)
        }

        val body = res.body<JsonObject>()
        return if (body.containsKey("status") &&
            body["status"]!!.instanceOf(Boolean::class) &&
            body["status"]!!.jsonPrimitive.boolean) {
            ApiResponse(true, JsonPrimitive(true), ApiError.NO_ERROR)
        } else {
            ApiResponse(false, JsonPrimitive(false), ApiError.NO_ERROR)
        }
    }
}