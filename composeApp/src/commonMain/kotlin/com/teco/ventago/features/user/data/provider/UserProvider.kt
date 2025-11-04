package com.teco.ventago.features.user.data.provider

import com.teco.ventago.Configs
import com.teco.ventago.utils.ApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject

class UserProvider(private val client: HttpClient): IUserProvider {
    override suspend fun setPremium(premium: Boolean): ApiResponse {
        TODO("Not yet implemented")
    }

    override suspend fun getUserData(token: String): ApiResponse {
        val res = client.post(Configs.serverBasePath+"auth/get-user-data") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer $token")
                append(HttpHeaders.ContentType, "application/json")
            }
            contentType(ContentType.Application.Json)
        }

        val body = res.body<JsonObject>()
        return ApiResponse.fromJson(body)
    }

}