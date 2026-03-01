package com.teco.ventago.features.expenses.data.provider

import com.teco.ventago.Configs
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.expenses.domain.models.requests.CategorizeExpenseRequest
import com.teco.ventago.features.expenses.domain.models.requests.CreateExpenseAccountRequest
import com.teco.ventago.features.expenses.domain.models.requests.ExpenseProofFile
import com.teco.ventago.features.expenses.domain.models.requests.ListExpensesRequest
import com.teco.ventago.features.expenses.domain.models.requests.UpdateExpenseAccountRequest
import com.teco.ventago.features.expenses.domain.models.requests.UpsertExpensePaymentRequest
import com.teco.ventago.features.expenses.domain.models.requests.UpsertExpenseRequest
import com.teco.ventago.json
import com.teco.ventago.utils.ApiError
import com.teco.ventago.utils.ApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class ExpensesProvider(
    private val client: HttpClient,
    private val authService: IAuthService,
    private val logger: ILoggerService
) : IExpensesProvider {

    private suspend fun handleAuth(
        response: ApiResponse,
        statusCode: HttpStatusCode,
        retry: suspend () -> ApiResponse
    ): ApiResponse {
        if (response.error == ApiError.AUTH_001 || statusCode == HttpStatusCode.Unauthorized) {
            return try {
                authService.refreshToken(client)
                retry()
            } catch (_: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun listExpenses(businessId: Int, request: ListExpensesRequest): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/expenses/list") {
            applyAuthorizedHeaders(businessId)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(ListExpensesRequest.serializer(), request))
        }
        val response = normalizeResponse(res.body())
        return handleAuth(response, res.status) { listExpenses(businessId, request) }
    }

    override suspend fun getExpense(businessId: Int, expenseId: Long): ApiResponse {
        val res = client.get(Configs.ordersBasePath + "/api/v1/expenses/$expenseId") {
            applyJsonHeaders(businessId)
        }
        val response = normalizeResponse(res.body())
        return handleAuth(response, res.status) { getExpense(businessId, expenseId) }
    }

    override suspend fun createExpense(
        businessId: Int,
        request: UpsertExpenseRequest,
        file: ExpenseProofFile?,
        paymentProofFiles: List<ExpenseProofFile>
    ): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/expenses/create") {
            val hasMultipartFiles = file != null || paymentProofFiles.isNotEmpty()
            if (hasMultipartFiles) {
                applyMultipartHeaders(businessId)
                val payload = json.encodeToString(UpsertExpenseRequest.serializer(), request)
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("payload", payload)
                            if (file != null) {
                                appendFile("file", file)
                            }
                            paymentProofFiles.forEachIndexed { index, proof ->
                                appendFile("proof_file_$index", proof)
                            }
                        }
                    )
                )
            } else {
                applyAuthorizedHeaders(businessId)
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(UpsertExpenseRequest.serializer(), request))
            }
        }
        val response = normalizeResponse(res.body())
        return handleAuth(response, res.status) { createExpense(businessId, request, file, paymentProofFiles) }
    }

    override suspend fun updateExpense(
        businessId: Int,
        expenseId: Long,
        request: UpsertExpenseRequest,
        file: ExpenseProofFile?
    ): ApiResponse {
        val res = client.put(Configs.ordersBasePath + "/api/v1/expenses/$expenseId") {
            val payload = json.encodeToString(UpsertExpenseRequest.serializer(), request)

            if (file != null) {
                applyMultipartHeaders(businessId)
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("payload", payload)
                            appendFile("file", file)
                        }
                    )
                )
            } else {
                applyAuthorizedHeaders(businessId)
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(UpsertExpenseRequest.serializer(), request))
            }
        }
        val response = normalizeResponse(res.body())
        return handleAuth(response, res.status) { updateExpense(businessId, expenseId, request, file) }
    }

    override suspend fun deleteExpense(businessId: Int, expenseId: Long): ApiResponse {
        val res = client.delete(Configs.ordersBasePath + "/api/v1/expenses/$expenseId") {
            applyJsonHeaders(businessId)
        }
        val response = normalizeResponse(res.body())
        return handleAuth(response, res.status) { deleteExpense(businessId, expenseId) }
    }

    override suspend fun categorizeExpense(
        businessId: Int,
        expenseId: Long,
        request: CategorizeExpenseRequest
    ): ApiResponse {
        val payload = buildCategorizationPayload(request)
        logger.sendLog(
            Log(
                level = LogLevel.INFO,
                flow = "ExpensesProvider::categorizeExpense",
                message = "PATCH /api/v1/expenses/$expenseId/categorization request. businessId: $businessId, request: $request, payload: ${payload}"
            )
        )
        val res = client.patch(Configs.ordersBasePath + "/api/v1/expenses/$expenseId/categorization") {
            applyAuthorizedHeaders(businessId)
            contentType(ContentType.Application.Json)
            setBody(payload.toString())
        }
        val response = normalizeResponse(res.body())
        if (!response.successful) {
            logger.sendLog(
                Log(
                    level = LogLevel.WARNING,
                    flow = "ExpensesProvider::categorizeExpense",
                    message = "PATCH /api/v1/expenses/$expenseId/categorization failed. businessId: $businessId, status: ${res.status.value}, errorCode: ${response.errorCode}, errorMessage: ${response.errorMessage}, payload: ${payload}"
                )
            )
        }
        return handleAuth(response, res.status) { categorizeExpense(businessId, expenseId, request) }
    }

    override suspend fun getExpenseAccounts(businessId: Int, includeInactive: Boolean): ApiResponse {
        val res = client.get(
            Configs.ordersBasePath + "/api/v1/expenses/accounts?include_inactive=$includeInactive"
        ) {
            applyJsonHeaders(businessId)
        }
        val response = normalizeResponse(res.body())
        return handleAuth(response, res.status) { getExpenseAccounts(businessId, includeInactive) }
    }

    override suspend fun createExpenseAccount(
        businessId: Int,
        request: CreateExpenseAccountRequest
    ): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/expenses/accounts") {
            applyAuthorizedHeaders(businessId)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CreateExpenseAccountRequest.serializer(), request))
        }
        val response = normalizeResponse(res.body())
        return handleAuth(response, res.status) { createExpenseAccount(businessId, request) }
    }

    override suspend fun updateExpenseAccount(
        businessId: Int,
        accountId: Long,
        request: UpdateExpenseAccountRequest
    ): ApiResponse {
        val res = client.put(Configs.ordersBasePath + "/api/v1/expenses/accounts/$accountId") {
            applyAuthorizedHeaders(businessId)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateExpenseAccountRequest.serializer(), request))
        }
        val response = normalizeResponse(res.body())
        return handleAuth(response, res.status) { updateExpenseAccount(businessId, accountId, request) }
    }

    override suspend fun deactivateExpenseAccount(businessId: Int, accountId: Long): ApiResponse {
        val res = client.delete(Configs.ordersBasePath + "/api/v1/expenses/accounts/$accountId") {
            applyJsonHeaders(businessId)
        }
        val response = normalizeResponse(res.body())
        return handleAuth(response, res.status) { deactivateExpenseAccount(businessId, accountId) }
    }

    override suspend fun createPayment(
        businessId: Int,
        expenseId: Long,
        request: UpsertExpensePaymentRequest,
        proofFile: ExpenseProofFile?
    ): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/expenses/$expenseId/payments") {
            if (proofFile != null) {
                applyMultipartHeaders(businessId)
                val payload = json.encodeToString(UpsertExpensePaymentRequest.serializer(), request)
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("payload", payload)
                            appendFile("proof_file", proofFile)
                        }
                    )
                )
            } else {
                applyAuthorizedHeaders(businessId)
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(UpsertExpensePaymentRequest.serializer(), request))
            }
        }
        val response = normalizeResponse(res.body())
        return handleAuth(response, res.status) { createPayment(businessId, expenseId, request, proofFile) }
    }

    override suspend fun listPayments(businessId: Int, expenseId: Long): ApiResponse {
        val res = client.get(Configs.ordersBasePath + "/api/v1/expenses/$expenseId/payments") {
            applyJsonHeaders(businessId)
        }
        val response = normalizeResponse(res.body())
        return handleAuth(response, res.status) { listPayments(businessId, expenseId) }
    }

    override suspend fun updatePayment(
        businessId: Int,
        expenseId: Long,
        paymentId: Long,
        request: UpsertExpensePaymentRequest,
        proofFile: ExpenseProofFile?
    ): ApiResponse {
        val res = client.put(Configs.ordersBasePath + "/api/v1/expenses/$expenseId/payments/$paymentId") {
            if (proofFile != null) {
                applyMultipartHeaders(businessId)
                val payload = json.encodeToString(UpsertExpensePaymentRequest.serializer(), request)
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("payload", payload)
                            appendFile("proof_file", proofFile)
                        }
                    )
                )
            } else {
                applyAuthorizedHeaders(businessId)
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(UpsertExpensePaymentRequest.serializer(), request))
            }
        }
        val response = normalizeResponse(res.body())
        return handleAuth(response, res.status) { updatePayment(businessId, expenseId, paymentId, request, proofFile) }
    }

    override suspend fun deletePayment(businessId: Int, expenseId: Long, paymentId: Long): ApiResponse {
        val res = client.delete(Configs.ordersBasePath + "/api/v1/expenses/$expenseId/payments/$paymentId") {
            applyJsonHeaders(businessId)
        }
        val response = normalizeResponse(res.body())
        return handleAuth(response, res.status) { deletePayment(businessId, expenseId, paymentId) }
    }

    override suspend fun crawlExpense(businessId: Int, payload: String): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/expenses/crawl") {
            applyJsonHeaders(businessId)
            setBody(payload)
        }
        val response = normalizeResponse(res.body())
        return handleAuth(response, res.status) { crawlExpense(businessId, payload) }
    }

    override suspend fun getCrawlJobStatus(businessId: Int, jobId: Long): ApiResponse {
        val res = client.get(Configs.ordersBasePath + "/api/v1/expenses/crawl/$jobId/status") {
            applyJsonHeaders(businessId)
        }
        val response = normalizeResponse(res.body())
        return handleAuth(response, res.status) { getCrawlJobStatus(businessId, jobId) }
    }

    override suspend fun listCrawlJobs(businessId: Int, page: Int, pageSize: Int): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/expenses/crawl/list") {
            applyJsonHeaders(businessId)
            setBody("""{"page":$page,"page_size":$pageSize}""")
        }
        val response = normalizeResponse(res.body())
        return handleAuth(response, res.status) { listCrawlJobs(businessId, page, pageSize) }
    }

    override suspend fun importExpenses(businessId: Int, payload: String): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/expenses/import") {
            applyJsonHeaders(businessId)
            setBody(payload)
        }
        val response = normalizeResponse(res.body())
        return handleAuth(response, res.status) { importExpenses(businessId, payload) }
    }

    override suspend fun listImports(businessId: Int): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/expenses/import/list") {
            applyJsonHeaders(businessId)
            setBody("{}")
        }
        val response = normalizeResponse(res.body())
        return handleAuth(response, res.status) { listImports(businessId) }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.applyAuthorizedHeaders(businessId: Int) {
        headers {
            append(HttpHeaders.Accept, "*/*")
            append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
            append(HttpHeaders.ContentType, "application/json")
            append("X-Business-ID", "$businessId")
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.applyMultipartHeaders(businessId: Int) {
        headers {
            append(HttpHeaders.Accept, "*/*")
            append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
            append("X-Business-ID", "$businessId")
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.applyJsonHeaders(businessId: Int) {
        applyAuthorizedHeaders(businessId)
        applyJsonContentType()
    }

    private fun io.ktor.client.request.HttpRequestBuilder.applyJsonContentType() {
        headers {
            remove(HttpHeaders.ContentType)
            append(HttpHeaders.ContentType, "application/json")
        }
        contentType(ContentType.Application.Json)
    }

    private fun io.ktor.client.request.forms.FormBuilder.appendFile(
        key: String,
        file: ExpenseProofFile
    ) {
        append(
            key = key,
            value = file.bytes,
            headers = Headers.build {
                append(HttpHeaders.ContentDisposition, "filename=\"${file.fileName}\"")
                append(HttpHeaders.ContentType, file.contentType)
            }
        )
    }
}

internal fun normalizeExpensesApiResponse(body: JsonObject): ApiResponse {
    val hasWrappedShape = body.containsKey("data") && (
        body.containsKey("error") ||
            body.containsKey("message") ||
            body.containsKey("timestamp") ||
            body.containsKey("success")
        )

    if (!hasWrappedShape) {
        return ApiResponse(successful = true, data = body, error = ApiError.NO_ERROR)
    }

    val parsedError = parseExpensesError(body["error"])
    val success = body["success"]?.jsonPrimitive?.booleanOrNull ?: parsedError.code.isNullOrBlank()

    return ApiResponse(
        successful = success,
        data = body["data"],
        error = ApiError.fromError(parsedError.code),
        errorCode = parsedError.code,
        errorMessage = parsedError.message
    )
}

private fun normalizeResponse(body: JsonObject): ApiResponse = normalizeExpensesApiResponse(body)

private data class ExpensesParsedError(
    val code: String? = null,
    val message: String? = null
)

private fun parseExpensesError(errorElement: JsonElement?): ExpensesParsedError {
    return when (errorElement) {
        null, JsonNull -> ExpensesParsedError()
        is JsonPrimitive -> {
            val content = errorElement.contentOrNull
            ExpensesParsedError(
                code = content?.takeIf { it.isNotBlank() },
                message = content?.takeIf { it.isNotBlank() }
            )
        }
        is JsonObject -> ExpensesParsedError(
            code = errorElement["code"]?.jsonPrimitive?.contentOrNull,
            message = errorElement["message"]?.jsonPrimitive?.contentOrNull
        )
        else -> ExpensesParsedError(message = errorElement.toString())
    }
}

internal fun buildCategorizationPayload(request: CategorizeExpenseRequest): JsonObject {
    return buildJsonObject {
        request.defaultAccountId?.let { put("default_account_id", JsonPrimitive(it)) }
        put("only_uncategorized", JsonPrimitive(request.onlyUncategorized))
        put(
            "items",
            buildJsonArray {
                request.items.forEach { item ->
                    add(
                        buildJsonObject {
                            item.itemId?.takeIf { it > 0 }?.let { put("item_id", JsonPrimitive(it)) }
                            if (item.itemId == null || item.itemId <= 0) {
                                item.lineNumber?.let { put("line_number", JsonPrimitive(it)) }
                            }
                            put("account_id", JsonPrimitive(item.accountId))
                        }
                    )
                }
            }
        )
    }
}

