package com.teco.ventago.features.reports

import com.teco.ventago.features.reports.domain.model.RealTimeReportCatalog
import com.teco.ventago.features.reports.domain.model.RealTimeReportParser
import com.teco.ventago.features.reports.domain.model.RealTimeReportRequest
import com.teco.ventago.features.reports.domain.model.ReportCustomerOption
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RealTimeReportApiModelsTest {

    @Test
    fun salesSummaryDefaultRequestMatchesRealTimeApiContract() {
        val definition = assertNotNull(RealTimeReportCatalog.reportByKey("sales_summary"))
        val params = RealTimeReportRequest.defaultFor(definition).toQueryParameters()

        assertEquals("current_month", params["preset"])
        assertEquals("all", params["document_status"])
        assertTrue("group_by" !in params, "Sales real-time reports must not send group_by by default")
        assertTrue("page" !in params)
        assertTrue("per_page" !in params)
    }

    @Test
    fun paginatedRequestIncludesPageAndPerPage() {
        val definition = assertNotNull(RealTimeReportCatalog.reportByKey("cash_flow"))
        val params = RealTimeReportRequest.defaultFor(definition).toQueryParameters()

        assertEquals("next_30_days", params["preset"])
        assertEquals("1", params["page"])
        assertEquals("50", params["per_page"])
        assertEquals("week", params["group_by"])
        assertEquals("true", params["include_overdue"])
    }

    @Test
    fun parserReadsSummaryRowsChartsAndPaginationFromBackendData() {
        val definition = assertNotNull(RealTimeReportCatalog.reportByKey("sales_adjustments"))
        val payload = Json.parseToJsonElement(
            """
            {
              "summary": {
                "total_adjusted": 125.5,
                "adjusted_document_count": 3
              },
              "rows": [
                {
                  "type": "credit_note",
                  "document": "NC-001",
                  "customer_name": "Cliente Demo",
                  "total": 125.5
                }
              ],
              "charts": {
                "trend": [
                  {"label": "Jun", "total": 125.5}
                ]
              },
              "pagination": {
                "page": 2,
                "per_page": 50,
                "total": 125,
                "total_pages": 3
              }
            }
            """.trimIndent()
        ) as JsonObject

        val data = RealTimeReportParser.parse(definition, payload)

        assertEquals(2, data.metrics.size)
        assertEquals("Total ajustado", data.metrics.first().label)
        assertEquals(1, data.rows.size)
        assertEquals("Cliente Demo", data.rows.first().cells.first { it.key == "customer_name" }.value)
        assertEquals(1, data.chartSections.size)
        assertEquals(2, data.pagination?.page)
        assertEquals(51, data.pagination?.from)
        assertEquals(100, data.pagination?.to)
        assertEquals(true, data.pagination?.canGoPrevious)
        assertEquals(true, data.pagination?.canGoNext)
    }

    @Test
    fun customerSearchOptionsParseNameAndRucForDropdown() {
        val payload = Json.parseToJsonElement(
            """
            {
              "total": 1,
              "page": 0,
              "size": 8,
              "items": [
                {
                  "id": 1226,
                  "name": "RODRIGO OSTIA OUTSOURCING  S A",
                  "ruc": "155592325-2-2015"
                }
              ]
            }
            """.trimIndent()
        ) as JsonObject

        val options = ReportCustomerOption.listFromResponse(payload)

        assertEquals(1, options.size)
        assertEquals(1226, options.first().id)
        assertEquals("RODRIGO OSTIA OUTSOURCING  S A - 155592325-2-2015", options.first().displayName)
    }
}
