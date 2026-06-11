package com.teco.ventago.features.reports

import com.teco.ventago.features.reports.domain.model.RealTimeReportCatalog
import com.teco.ventago.features.reports.domain.model.RealTimeReportCategory
import com.teco.ventago.features.reports.domain.model.ReportExportFormat
import com.teco.ventago.features.reports.domain.model.ReportPdfEndpoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RealTimeReportCatalogTest {

    @Test
    fun catalogKeepsAllSixWebCategoriesWithReports() {
        assertEquals(
            RealTimeReportCategory.entries.toList(),
            RealTimeReportCatalog.categories
        )

        RealTimeReportCategory.entries.forEach { category ->
            assertTrue(
                RealTimeReportCatalog.reportsFor(category).isNotEmpty(),
                "Expected reports for ${category.title}"
            )
        }
    }

    @Test
    fun catalogOnlyContainsRealTimeReports() {
        val keys = RealTimeReportCatalog.reports.map { it.key }.toSet()

        assertEquals(26, keys.size)
        assertFalse("1027" in keys)
        assertFalse("taxes" in keys)
        assertFalse("expense_auxiliary" in keys)
        assertFalse("dgi_anexos_72_94" in keys)
        assertTrue(keys.all { key -> RealTimeReportCatalog.reportByKey(key)?.endpoint?.contains("/real-time/") == true })
    }

    @Test
    fun exportExceptionsMatchWebGuide() {
        val financialComparison = assertNotNull(RealTimeReportCatalog.reportByKey("financial_comparison"))
        assertEquals(setOf(ReportExportFormat.XLSX), financialComparison.exportFormats)
        assertEquals(ReportPdfEndpoint.NONE, financialComparison.pdfEndpoint)

        val operatingMargin = assertNotNull(RealTimeReportCatalog.reportByKey("operating_margin"))
        assertTrue(operatingMargin.exportFormats.isEmpty())
        assertEquals(ReportPdfEndpoint.NONE, operatingMargin.pdfEndpoint)

        val businessOverview = assertNotNull(RealTimeReportCatalog.reportByKey("business_overview"))
        assertTrue(businessOverview.exportFormats.isEmpty())
        assertEquals(ReportPdfEndpoint.NONE, businessOverview.pdfEndpoint)

        val cashFlow = assertNotNull(RealTimeReportCatalog.reportByKey("cash_flow"))
        assertEquals(ReportPdfEndpoint.EXPORT_PDF, cashFlow.pdfEndpoint)
    }
}
