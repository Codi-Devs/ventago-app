package com.teco.ventago.features.reports.data.repository

import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.reports.data.provider.IRealTimeReportsProvider
import com.teco.ventago.features.reports.domain.model.RealTimeReportCatalog
import com.teco.ventago.features.reports.domain.model.RealTimeReportData
import com.teco.ventago.features.reports.domain.model.RealTimeReportExportPayload
import com.teco.ventago.features.reports.domain.model.RealTimeReportExportRequest
import com.teco.ventago.features.reports.domain.model.RealTimeReportParser
import com.teco.ventago.features.reports.domain.model.RealTimeReportRequest
import com.teco.ventago.features.reports.domain.model.ReportCustomerOption
import com.teco.ventago.utils.BadRequestException
import com.teco.ventago.utils.isError
import kotlinx.serialization.json.JsonObject

interface IRealTimeReportsRepository {
    suspend fun getReport(businessId: Int, request: RealTimeReportRequest): RealTimeReportData
    suspend fun searchCustomers(businessId: Int, name: String): List<ReportCustomerOption>
    suspend fun exportReport(businessId: Int, request: RealTimeReportExportRequest): RealTimeReportExportPayload
}

class RealTimeReportsRepository(
    private val provider: IRealTimeReportsProvider,
    private val logger: ILoggerService,
) : IRealTimeReportsRepository {

    override suspend fun getReport(
        businessId: Int,
        request: RealTimeReportRequest,
    ): RealTimeReportData {
        return try {
            val definition = RealTimeReportCatalog.reportByKey(request.reportKey)
                ?: throw BadRequestException("Reporte real-time no registrado: ${request.reportKey}")
            val response = provider.getReport(businessId, request)
            if (response.error.isError() || !response.successful) {
                throw BadRequestException(response.toJson())
            }
            val data = response.data as? JsonObject ?: throw BadRequestException(response.toJson())
            RealTimeReportParser.parse(definition, data)
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "RealTimeReportsRepository::getReport",
                    "Error getting real-time report. businessId: $businessId, reportKey: ${request.reportKey}, error: ${e.message ?: "UNKNOWN"}"
                )
            )
            throw e
        }
    }

    override suspend fun searchCustomers(businessId: Int, name: String): List<ReportCustomerOption> {
        return try {
            val response = provider.searchCustomers(businessId, name)
            if (response.error.isError() || !response.successful) {
                throw BadRequestException(response.toJson())
            }
            val data = response.data as? JsonObject ?: throw BadRequestException(response.toJson())
            ReportCustomerOption.listFromResponse(data)
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "RealTimeReportsRepository::searchCustomers",
                    "Error searching customers for report. businessId: $businessId, name: $name, error: ${e.message ?: "UNKNOWN"}"
                )
            )
            throw e
        }
    }

    override suspend fun exportReport(
        businessId: Int,
        request: RealTimeReportExportRequest,
    ): RealTimeReportExportPayload {
        return try {
            provider.exportReport(businessId, request)
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "RealTimeReportsRepository::exportReport",
                    "Error exporting real-time report. businessId: $businessId, reportKey: ${request.reportKey}, format: ${request.format}, error: ${e.message ?: "UNKNOWN"}"
                )
            )
            throw e
        }
    }
}
