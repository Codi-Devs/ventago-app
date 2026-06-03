package com.teco.ventago.features.branches.data.provider

import com.teco.ventago.Configs
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.core.file.SharedFile
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.utils.ApiError
import com.teco.ventago.utils.ApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject

class BranchProvider(
    private val client: HttpClient,
    private val authService: IAuthService,
    private val logger: ILoggerService
) : IBranchProvider {

    override suspend fun getBranches(businessId: Int): ApiResponse {
        val res = client.get(Configs.ordersBasePath + "/api/v1/invoicing/branches") {
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
                getBranches(businessId)
            } catch (e: Exception) {
                logger.sendLog(
                    Log(
                        level = LogLevel.ERROR,
                        flow = "BranchProvider::addBranch",
                        message = "Error refreshing token in BranchProvider.addBranch: ${e.message ?: "Unknown"}",
                    )
                )
                response
            }
        }
        return response
    }

    override suspend fun addBillingPoint(businessId: Int, branchCode: String, name: String, code: String, status: Int): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/invoicing/branches/${branchCode}/billing-points") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(
                BranchRequests.addBillingPoint(name, code, status)
            )
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                addBillingPoint(businessId, branchCode, name, code,status)
            } catch (e: Exception) {
                logger.sendLog(
                    Log(
                        level = LogLevel.ERROR,
                        flow = "BranchProvider::addBranch",
                        message = "Error refreshing token in BranchProvider.addBranch: ${e.message ?: "Unknown"}",
                    )
                )
                response
            }
        }
        return response
    }

    override suspend fun updateBillingPoint(
        businessId: Int,
        branchCode: String,
        billingPoint: String,
        name: String,
        status: Int,
    ): ApiResponse {
        val res = client.put(Configs.ordersBasePath + "/api/v1/invoicing/branches/${branchCode}/billing-points/${billingPoint}") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(
                BranchRequests.updateBillingPoint(name, status)
            )
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                updateBillingPoint(businessId, branchCode, billingPoint, name, status)
            } catch (e: Exception) {
                logger.sendLog(
                    Log(
                        level = LogLevel.ERROR,
                        flow = "BranchProvider::addBranch",
                        message = "Error refreshing token in BranchProvider.addBranch: ${e.message ?: "Unknown"}",
                    )
                )
                response
            }
        }
        return response
    }

    override suspend fun uploadBranchLogo(
        businessId: Int,
        branchCode: String,
        logo: SharedFile,
    ): ApiResponse {
        val res = client.put(Configs.ordersBasePath + "/api/v1/invoicing/branches/${branchCode}/logo") {
            applyMultipartHeaders(businessId)
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            key = "logo",
                            value = logo.bytes,
                            headers = Headers.build {
                                append(HttpHeaders.ContentDisposition, "filename=\"${logo.fileName}\"")
                                append(HttpHeaders.ContentType, logo.contentType)
                            }
                        )
                    }
                )
            )
        }

        val body = res.body<JsonObject>()
        val response = normalizeLogoResponse(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                uploadBranchLogo(businessId, branchCode, logo)
            } catch (e: Exception) {
                logger.sendLog(
                    Log(
                        level = LogLevel.ERROR,
                        flow = "BranchProvider::uploadBranchLogo",
                        message = "Error refreshing token in BranchProvider.uploadBranchLogo: ${e.message ?: "Unknown"}",
                    )
                )
                response
            }
        }
        return response
    }

    override suspend fun deleteBranchLogo(businessId: Int, branchCode: String): ApiResponse {
        val res = client.delete(Configs.ordersBasePath + "/api/v1/invoicing/branches/${branchCode}/logo") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append("X-Business-ID", "$businessId")
            }
        }

        val body = res.body<JsonObject>()
        val response = normalizeLogoResponse(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                deleteBranchLogo(businessId, branchCode)
            } catch (e: Exception) {
                logger.sendLog(
                    Log(
                        level = LogLevel.ERROR,
                        flow = "BranchProvider::deleteBranchLogo",
                        message = "Error refreshing token in BranchProvider.deleteBranchLogo: ${e.message ?: "Unknown"}",
                    )
                )
                response
            }
        }
        return response
    }

    private fun io.ktor.client.request.HttpRequestBuilder.applyMultipartHeaders(businessId: Int) {
        headers {
            append(HttpHeaders.Accept, "*/*")
            append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
            append("X-Business-ID", "$businessId")
        }
    }

    private fun normalizeLogoResponse(body: JsonObject): ApiResponse {
        val hasSuccessOrError = body.containsKey("success") || body.containsKey("error")
        if (!hasSuccessOrError) {
            return ApiResponse(
                successful = true,
                data = body["data"] ?: body,
                error = ApiError.NO_ERROR,
            )
        }
        return ApiResponse.fromJson(body)
    }
}
