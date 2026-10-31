package com.teco.ventago.features.product.data.provider.category

import com.teco.ventago.Configs
import com.teco.ventago.features.auth.domain.IAuthService
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
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.PI
import kotlin.random.Random

class CategoryProvider(
    private val client: HttpClient,
    private val authService: IAuthService,
) : ICategoryProvider {
    override suspend fun setActive(categoryId: Int, active: Boolean): ApiResponse {
        val res = client.post(Configs.serverBasePath + "category/set-category-active") {
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
        val res = client.post(Configs.serverBasePath + "category/change-category-order") {
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
        return postWithJwt(
            path = "category/edit-category",
            body = CategoryRequests.editCategory(category),
            failure = "Error editing category ${category.id}",
        )
    }

    override suspend fun addCategory(category: Category, menuId: Int): ApiResponse {
        return postWithJwt(
            path = "category/add-category",
            body = CategoryRequests.addCategory(category, menuId),
            failure = "Error adding category $category",
        )
    }

    override suspend fun removeCategory(categoryId: Int): ApiResponse {
        return postWithJwt(
            path = "category/remove-category",
            body = CategoryRequests.removeCategory(categoryId),
            failure = "Error removing category $categoryId",
        )
    }

    private suspend fun postWithJwt(
        path: String,
        body: String,
        failure: String,
        retried: Boolean = false,
    ): ApiResponse {
        val res = client.post(Configs.serverBasePath + path) {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json; charset=UTF-8")
            }
            contentType(ContentType.Application.Json)
            setBody(body)
        }

        if (!res.status.isSuccess()) {
            throw BadRequestException(failure)
        }

        val json = res.body<JsonObject>()
        val status = json["status"]?.jsonPrimitive?.boolean ?: false
        val error = ApiError.fromError(json["error"]?.jsonPrimitive?.contentOrNull)
        if (error == ApiError.AUTH_001 && !retried) {
            return try {
                authService.refreshToken(client)
                postWithJwt(path, body, failure, retried = true)
            } catch (e: Exception) {
                ApiResponse(false, null, error)
            }
        }
        return if (status) {
            ApiResponse(true, json["data"], ApiError.NO_ERROR)
        } else {
            ApiResponse(false, null, error)
        }
    }
}
