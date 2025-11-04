package com.teco.ventago.features.product.data.provider.category

import com.teco.ventago.Configs
import com.teco.ventago.features.product.domain.model.Category
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

class CategoryProvider(private val client: HttpClient): ICategoryProvider {
    override suspend fun setActive(categoryId: Int, active: Boolean): ApiResponse {
        val res = client.post(Configs.serverBasePath+"category/set-category-active"){
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.ContentType, "application/json; charset=UTF-8")
                append("http-x-api-token", generateHashWithHmac256(categoryId.toString())!!)
            }
            contentType(ContentType.Application.Json)
            setBody(CategoryRequests.setActive(categoryId, active))
        }

        if (!res.status.isSuccess()) {
            throw BadRequestException("Error activating category $categoryId")
        }

        val body = res.body<JsonObject>()
        val status = body["status"]?.jsonPrimitive?.boolean ?: false
        return ApiResponse(status, JsonPrimitive(status), ApiError.NO_ERROR)
    }

    override suspend fun changeCategoryOrder(categories: List<Category>): ApiResponse {
        val value: Double = Random.nextDouble(until = PI / 6)
        val res = client.post(Configs.serverBasePath+"category/change-category-order"){
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.ContentType, "application/json; charset=UTF-8")
                append("http-x-api-token", generateHashWithHmac256(value.toString())!!)
            }
            contentType(ContentType.Application.Json)
            setBody(CategoryRequests.changeCategoryOrder(categories, value))
        }

        if (!res.status.isSuccess()) {
            throw BadRequestException("Error changing categories order")
        }

        val body = res.body<JsonObject>()
        val status = body["status"]?.jsonPrimitive?.boolean ?: false
        return ApiResponse(status, JsonPrimitive(status), ApiError.NO_ERROR)
    }

    override suspend fun editCategory(category: Category): ApiResponse {
        val res = client.post(Configs.serverBasePath+"category/edit-category"){
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.ContentType, "application/json; charset=UTF-8")
                append("http-x-api-token", generateHashWithHmac256(category.id.toString()+category.name+category.desc)!!)
            }
            contentType(ContentType.Application.Json)
            setBody(CategoryRequests.editCategory(category))
        }

        if (!res.status.isSuccess()) {
            throw BadRequestException("Error editing category ${category.id}")
        }

        val body = res.body<JsonObject>()
        val status = body["status"]?.jsonPrimitive?.boolean ?: false
        return ApiResponse(status, JsonPrimitive(status), ApiError.NO_ERROR)
    }

    override suspend fun addCategory(category: Category, menuId: Int): ApiResponse {
        val res = client.post(Configs.serverBasePath+"category/add-category"){
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.ContentType, "application/json; charset=UTF-8")
                append("http-x-api-token", generateHashWithHmac256(category.name+category.desc+menuId.toString()+category.order.toString())!!)
            }
            contentType(ContentType.Application.Json)
            setBody(CategoryRequests.addCategory(category, menuId))
        }

        if (!res.status.isSuccess()) {
            throw BadRequestException("Error adding category $category")
        }

        val body = res.body<JsonObject>()
        val status = body["status"]?.jsonPrimitive?.boolean ?: false
        return if (status) {
            ApiResponse(true, body["data"], ApiError.NO_ERROR)
        } else {
            ApiResponse(false, null, ApiError.NO_ERROR)
        }
    }

    override suspend fun removeCategory(categoryId: Int): ApiResponse {
        val res = client.post(Configs.serverBasePath+"category/remove-category"){
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.ContentType, "application/json; charset=UTF-8")
                append("http-x-api-token", generateHashWithHmac256(categoryId.toString())!!)
            }
            contentType(ContentType.Application.Json)
            setBody(CategoryRequests.removeCategory(categoryId))
        }

        if (!res.status.isSuccess()) {
            throw BadRequestException("Error removing category $categoryId")
        }

        val body = res.body<JsonObject>()
        val status = body["status"]?.jsonPrimitive?.boolean ?: false
        return ApiResponse(status, JsonPrimitive(status), ApiError.NO_ERROR)
    }
}