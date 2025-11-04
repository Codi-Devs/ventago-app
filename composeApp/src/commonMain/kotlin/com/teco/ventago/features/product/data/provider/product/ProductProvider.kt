package com.teco.ventago.features.product.data.provider.product

import com.teco.ventago.Configs
import com.teco.ventago.utils.ApiError
import com.teco.ventago.utils.ApiResponse
import com.teco.ventago.utils.BadRequestException
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

class ProductProvider(private val client: HttpClient): IProductProvider {
    override suspend fun getMenuByBusinessId(businessId: Int): ApiResponse {
        val res = client.post(Configs.serverBasePath+"menu/get-menu-by-business"){
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.ContentType, "application/json; charset=UTF-8")
            }
            contentType(ContentType.Application.Json)
            setBody(ProductRequests.getMenuByBusinessId(businessId))
        }

        if (!res.status.isSuccess()) {
            throw BadRequestException("Error getting menu by business id $businessId")
        }

        val body = res.body<JsonObject>()
        println("ASDASD: $body")
        val status = body["status"]?.jsonPrimitive?.boolean ?: false
        return if (status) {
            ApiResponse(true, body["data"], ApiError.NO_ERROR)
        } else {
            ApiResponse(false, null, ApiError.NO_ERROR)
        }
    }

    override suspend fun getMenuIdByBusinessId(businessId: Int): ApiResponse {
        val res = client.post(Configs.serverBasePath+"menu/get-menu-id-by-business-id"){
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.ContentType, "application/json; charset=UTF-8")
            }
            contentType(ContentType.Application.Json)
            setBody(ProductRequests.getMenuByBusinessId(businessId))
        }

        if (!res.status.isSuccess()) {
            throw BadRequestException("Error getting menu by business id $businessId")
        }

        val body = res.body<JsonObject>()
        val status = body["status"]?.jsonPrimitive?.boolean ?: false
        return if (status) {
            ApiResponse(true, body["data"]?.jsonPrimitive ,ApiError.NO_ERROR)
        } else {
            ApiResponse(false, JsonPrimitive(-1) ,ApiError.NO_ERROR)
        }
    }
}