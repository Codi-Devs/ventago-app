package com.teco.ventago.features.expenses.data.provider

import com.teco.ventago.Configs
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.expenses.domain.models.requests.ListExpensesRequest
import com.teco.ventago.json
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
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

class ExpensesProvider(
    private val client: HttpClient,
    private val authService: IAuthService
) : IExpensesProvider {

    private suspend fun authHeaders(businessId: Int): Map<String, String> = mapOf(
        HttpHeaders.Accept to "*/*",
        HttpHeaders.Authorization to "Bearer ${authService.getJwtToken()}",
        HttpHeaders.ContentType to "application/json",
        "X-Business-ID" to "$businessId"
    )

    private suspend fun handleAuth(
        response: ApiResponse,
        statusCode: HttpStatusCode,
        retry: suspend () -> ApiResponse
    ): ApiResponse {
        if (response.error == ApiError.AUTH_001 || statusCode == HttpStatusCode.Unauthorized) {
            return try {
                authService.refreshToken(client)
                retry()
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun listExpenses(businessId: Int, request: ListExpensesRequest): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/expenses/list") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }
        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        return handleAuth(response, res.status) { listExpenses(businessId, request) }
    }

    override suspend fun getExpense(businessId: Int, expenseId: Long): ApiResponse {
        val res = client.get(Configs.ordersBasePath + "/api/v1/expenses/$expenseId") {
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
        return handleAuth(response, res.status) { getExpense(businessId, expenseId) }
    }

    override suspend fun createExpense(businessId: Int, payload: String): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/expenses/create") {
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
        return handleAuth(response, res.status) { createExpense(businessId, payload) }
    }

    override suspend fun updateExpense(businessId: Int, expenseId: Long, payload: String): ApiResponse {
        val res = client.put(Configs.ordersBasePath + "/api/v1/expenses/$expenseId") {
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
        return handleAuth(response, res.status) { updateExpense(businessId, expenseId, payload) }
    }

    override suspend fun deleteExpense(businessId: Int, expenseId: Long): ApiResponse {
        val res = client.delete(Configs.ordersBasePath + "/api/v1/expenses/$expenseId") {
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
        return handleAuth(response, res.status) { deleteExpense(businessId, expenseId) }
    }

    // Payments

    override suspend fun createPayment(businessId: Int, expenseId: Long, payload: String): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/expenses/$expenseId/payments") {
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
        return handleAuth(response, res.status) { createPayment(businessId, expenseId, payload) }
    }

    override suspend fun listPayments(businessId: Int, expenseId: Long): ApiResponse {
        val res = client.get(Configs.ordersBasePath + "/api/v1/expenses/$expenseId/payments") {
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
        return handleAuth(response, res.status) { listPayments(businessId, expenseId) }
    }

    override suspend fun updatePayment(businessId: Int, expenseId: Long, paymentId: Long, payload: String): ApiResponse {
        val res = client.put(Configs.ordersBasePath + "/api/v1/expenses/$expenseId/payments/$paymentId") {
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
        return handleAuth(response, res.status) { updatePayment(businessId, expenseId, paymentId, payload) }
    }

    override suspend fun deletePayment(businessId: Int, expenseId: Long, paymentId: Long): ApiResponse {
        val res = client.delete(Configs.ordersBasePath + "/api/v1/expenses/$expenseId/payments/$paymentId") {
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
        return handleAuth(response, res.status) { deletePayment(businessId, expenseId, paymentId) }
    }

    // Crawl jobs

    override suspend fun crawlExpense(businessId: Int, payload: String): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/expenses/crawl") {
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
        return handleAuth(response, res.status) { crawlExpense(businessId, payload) }
    }

    override suspend fun getCrawlJobStatus(businessId: Int, jobId: Long): ApiResponse {
        val res = client.get(Configs.ordersBasePath + "/api/v1/expenses/crawl/$jobId/status") {
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
        return handleAuth(response, res.status) { getCrawlJobStatus(businessId, jobId) }
    }

    override suspend fun listCrawlJobs(businessId: Int, page: Int, pageSize: Int): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/expenses/crawl/list") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody("""{"page":$page,"page_size":$pageSize}""")
        }
        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        return handleAuth(response, res.status) { listCrawlJobs(businessId, page, pageSize) }
    }

    // Excel import

    override suspend fun importExpenses(businessId: Int, payload: String): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/expenses/import") {
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
        return handleAuth(response, res.status) { importExpenses(businessId, payload) }
    }

    override suspend fun listImports(businessId: Int): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/expenses/import/list") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        return handleAuth(response, res.status) { listImports(businessId) }
    }
}
