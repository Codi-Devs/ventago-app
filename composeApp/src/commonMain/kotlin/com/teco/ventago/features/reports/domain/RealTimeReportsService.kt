package com.teco.ventago.features.reports.domain

import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.reports.data.repository.IRealTimeReportsRepository
import com.teco.ventago.features.reports.domain.model.RealTimeReportData
import com.teco.ventago.features.reports.domain.model.RealTimeReportExportPayload
import com.teco.ventago.features.reports.domain.model.RealTimeReportExportRequest
import com.teco.ventago.features.reports.domain.model.RealTimeReportRequest
import com.teco.ventago.features.reports.domain.model.ReportCustomerOption

class RealTimeReportsService(
    private val repository: IRealTimeReportsRepository,
    private val businessService: BusinessService,
) {
    suspend fun getReport(request: RealTimeReportRequest): RealTimeReportData {
        return repository.getReport(resolveBusinessId(), request)
    }

    suspend fun searchCustomers(name: String): List<ReportCustomerOption> {
        return repository.searchCustomers(resolveBusinessId(), name)
    }

    suspend fun exportReport(request: RealTimeReportExportRequest): RealTimeReportExportPayload {
        return repository.exportReport(resolveBusinessId(), request)
    }

    private fun resolveBusinessId(): Int {
        return businessService.business.value?.businessId
            ?: throw IllegalStateException("No business selected for real-time reports")
    }
}
