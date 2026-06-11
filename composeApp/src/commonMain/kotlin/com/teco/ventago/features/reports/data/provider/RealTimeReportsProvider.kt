package com.teco.ventago.features.reports.data.provider

import com.teco.ventago.Configs
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.reports.domain.model.RealTimeReportExportPayload
import com.teco.ventago.features.reports.domain.model.RealTimeReportExportRequest
import com.teco.ventago.features.reports.domain.model.RealTimeReportRequest
import com.teco.ventago.features.reports.domain.model.ReportExportFormat
import com.teco.ventago.features.reports.domain.model.ReportPdfEndpoint
import com.teco.ventago.utils.ApiError
import com.teco.ventago.utils.ApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject

interface IRealTimeReportsProvider {
    suspend fun getReport(businessId: Int, request: RealTimeReportRequest): ApiResponse
    suspend fun searchCustomers(businessId: Int, name: String): ApiResponse
    suspend fun exportReport(businessId: Int, request: RealTimeReportExportRequest): RealTimeReportExportPayload
}

class RealTimeReportsProvider(
    private val client: HttpClient,
    private val authService: IAuthService,
) : IRealTimeReportsProvider {

    override suspend fun getReport(
        businessId: Int,
        request: RealTimeReportRequest,
    ): ApiResponse {
        return requestReport(businessId, request, allowRetry = true)
    }

    override suspend fun searchCustomers(businessId: Int, name: String): ApiResponse {
        return requestCustomerSearch(businessId, name, allowRetry = true)
    }

    override suspend fun exportReport(
        businessId: Int,
        request: RealTimeReportExportRequest,
    ): RealTimeReportExportPayload {
        return requestExport(businessId, request, allowRetry = true)
    }

    private suspend fun requestReport(
        businessId: Int,
        request: RealTimeReportRequest,
        allowRetry: Boolean,
    ): ApiResponse {
        val token = authService.getJwtToken()
        val response = client.get(Configs.ordersBasePath + "/api/v1/reports/real-time/${request.reportKey}") {
            url {
                request.toQueryParameters().forEach { (key, value) ->
                    parameters.append(key, value)
                }
            }
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer $token")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
        }

        val body = response.body<JsonObject>()
        val apiResponse = ApiResponse.fromJson(body)
        val shouldRefreshToken = apiResponse.error == ApiError.AUTH_001 || response.status == HttpStatusCode.Unauthorized

        if (allowRetry && shouldRefreshToken) {
            return try {
                authService.refreshToken(client, token)
                requestReport(businessId, request, allowRetry = false)
            } catch (_: Exception) {
                apiResponse
            }
        }

        return apiResponse
    }

    private suspend fun requestCustomerSearch(
        businessId: Int,
        name: String,
        allowRetry: Boolean,
    ): ApiResponse {
        val token = authService.getJwtToken()
        val response = client.get(Configs.ordersBasePath + "/api/v1/customers/") {
            url {
                parameters.append("page", "0")
                parameters.append("size", "8")
                parameters.append("name", name)
            }
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer $token")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
        }

        val body = response.body<JsonObject>()
        val apiResponse = ApiResponse.fromJson(body)
        val shouldRefreshToken = apiResponse.error == ApiError.AUTH_001 || response.status == HttpStatusCode.Unauthorized

        if (allowRetry && shouldRefreshToken) {
            return try {
                authService.refreshToken(client, token)
                requestCustomerSearch(businessId, name, allowRetry = false)
            } catch (_: Exception) {
                apiResponse
            }
        }

        return apiResponse
    }

    private suspend fun requestExport(
        businessId: Int,
        request: RealTimeReportExportRequest,
        allowRetry: Boolean,
    ): RealTimeReportExportPayload {
        val token = authService.getJwtToken()
        val response = client.get(exportUrl(request)) {
            url {
                exportQueryParameters(request).forEach { (key, value) ->
                    parameters.append(key, value)
                }
            }
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer $token")
                append("X-Business-ID", "$businessId")
            }
        }

        if (allowRetry && response.status == HttpStatusCode.Unauthorized) {
            authService.refreshToken(client, token)
            return requestExport(businessId, request, allowRetry = false)
        }

        return RealTimeReportExportPayload(
            bytes = response.body(),
            contentType = response.headers[HttpHeaders.ContentType],
            fileName = response.headers[HttpHeaders.ContentDisposition].contentDispositionFileName(),
            format = request.format
        )
    }

    private fun exportUrl(request: RealTimeReportExportRequest): String {
        val base = "${Configs.ordersBasePath}/api/v1/reports/real-time/${request.reportKey}"
        return when (request.format) {
            ReportExportFormat.XLSX -> "$base/export"
            ReportExportFormat.PDF -> when (request.pdfEndpoint) {
                ReportPdfEndpoint.DOWNLOAD_PDF -> "$base/download.pdf"
                ReportPdfEndpoint.EXPORT_PDF -> "$base/export"
                ReportPdfEndpoint.NONE -> "$base/export"
            }
        }
    }

    private fun exportQueryParameters(request: RealTimeReportExportRequest): Map<String, String> {
        val parameters = linkedMapOf<String, String>()
        if (request.format == ReportExportFormat.XLSX || request.pdfEndpoint == ReportPdfEndpoint.EXPORT_PDF) {
            parameters["format"] = request.format.name.lowercase()
        }
        parameters.putAll(request.queryParameters)
        return parameters
    }

    private fun String?.contentDispositionFileName(): String? {
        return this
            ?.substringAfter("filename=", "")
            ?.trim()
            ?.trim('"')
            ?.ifBlank { null }
    }
}
