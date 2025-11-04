package com.teco.ventago.features.product.data.provider.item

import com.teco.ventago.Configs
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.product.domain.model.Item
import com.teco.ventago.utils.ApiError
import com.teco.ventago.utils.ApiResponse
import com.teco.ventago.utils.BadRequestException
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
import kotlin.math.PI
import kotlin.random.Random

class ItemProvider(private val client: HttpClient, private val authService: IAuthService): IItemProvider {
    override suspend fun addItem(item: Item, categoryId: Int): ApiResponse {
        val res = client.post(Configs.serverBasePath+"item/add-item"){
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
            }
            contentType(ContentType.Application.Json)
            setBody(ItemRequests.addItem(item, categoryId))
        }

        if (!res.status.isSuccess()) {
            throw BadRequestException("Error adding item $item")
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                addItem(item, categoryId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun editItem(item: Item): ApiResponse {
        val res = client.post(Configs.serverBasePath+"item/edit-item"){
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
            }
            contentType(ContentType.Application.Json)
            setBody(ItemRequests.editItem(item))
        }

        if (!res.status.isSuccess()) {
            throw BadRequestException("Error editing item $item")
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                editItem(item)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun removeItem(itemId: Int): ApiResponse {
        val res = client.post(Configs.serverBasePath+"item/remove-item"){
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
            }
            contentType(ContentType.Application.Json)
            setBody(ItemRequests.removeItem(itemId))
        }

        if (!res.status.isSuccess()) {
            throw BadRequestException("Error removeItem item $itemId")
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                removeItem(itemId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun changeItemOrder(items: List<Item>): ApiResponse {
        val value: Double = Random.nextDouble(until = PI / 6)
        val res = client.post(Configs.serverBasePath+"item/change-items-order"){
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
            }
            contentType(ContentType.Application.Json)
            setBody(ItemRequests.changeItemOrder(items, value))
        }

        if (!res.status.isSuccess()) {
            throw BadRequestException("Error changing items order")
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                changeItemOrder(items)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }
}