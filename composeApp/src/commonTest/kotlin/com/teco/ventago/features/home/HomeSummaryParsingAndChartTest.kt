package com.teco.ventago.features.home

import com.teco.ventago.features.home.data.provider.normalizeHomeSummaryApiResponse
import com.teco.ventago.features.home.domain.model.DailySalesPoint
import com.teco.ventago.features.home.domain.model.FiscalMonthlySalesPoint
import com.teco.ventago.features.home.domain.model.HomeSalesChartMapper
import com.teco.ventago.features.home.domain.model.HomeSalesRange
import com.teco.ventago.features.home.domain.model.HomeSummary
import com.teco.ventago.features.home.domain.model.HomeSummaryParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class HomeSummaryParsingAndChartTest {

    @Test
    fun normalizeWrappedResponse() {
        val body = Json.parseToJsonElement(
            """
            {
              "message": "ok",
              "data": {
                "month_sales_total": "100.50"
              },
              "error": null
            }
            """.trimIndent()
        ) as JsonObject

        val normalized = normalizeHomeSummaryApiResponse(body)

        assertTrue(normalized.successful)
        assertNotNull(normalized.data)
        assertEquals("100.50", (normalized.data as JsonObject)["month_sales_total"]?.toString()?.trim('"'))
    }

    @Test
    fun normalizeDirectPayloadResponse() {
        val body = Json.parseToJsonElement(
            """
            {
              "month_sales_total": "75.00",
              "daily_sales_chart": []
            }
            """.trimIndent()
        ) as JsonObject

        val normalized = normalizeHomeSummaryApiResponse(body)

        assertTrue(normalized.successful)
        assertEquals(body, normalized.data)
    }

    @Test
    fun parserReadsStringAndNumberFieldsDefensively() {
        val data = Json.parseToJsonElement(
            """
            {
              "month_sales_total": "3330.93",
              "month_sales_tax_total": 208.71,
              "month_sales_total_with_taxes": "3539.64",
              "year_sales_total": "25501.82",
              "month_order_count": "71",
              "month_sales_change_perc": "-83.9",
              "today_sales_total": "824.78",
              "today_order_count": 7,
              "month_expense_total": "40.00",
              "month_expense_count": "2",
              "daily_sales_chart": [
                {"date": "2026-02-17", "amount": "28.60", "count": "6"},
                {"date": "2026-02-18", "amount": 824.78, "count": 7}
              ],
              "fiscal_monthly_sales": [
                {"month": "2026-01", "amount": "21962.18"},
                {"month": "2026-02", "amount": 3539.64}
              ]
            }
            """.trimIndent()
        ) as JsonObject

        val parsed = HomeSummaryParser.parse(data)

        assertEquals(3330.93, parsed.monthSalesTotal)
        assertEquals(208.71, parsed.monthSalesTaxTotal)
        assertEquals(3539.64, parsed.monthSalesTotalWithTaxes)
        assertEquals(25501.82, parsed.yearSalesTotal)
        assertEquals(71, parsed.monthOrderCount)
        assertEquals(-83.9, parsed.monthSalesChangePerc)
        assertEquals(2, parsed.dailySalesChart.size)
        assertEquals(2, parsed.fiscalMonthlySales.size)
    }

    @Test
    fun chartMapperBuildsSevenDaySeriesWithZeroFill() {
        val summary = HomeSummary(
            dailySalesChart = listOf(
                DailySalesPoint(date = "2026-02-16", amount = 150.09, count = 6),
                DailySalesPoint(date = "2026-02-18", amount = 824.78, count = 7)
            )
        )

        val chart = HomeSalesChartMapper.buildChart(
            summary = summary,
            range = HomeSalesRange.D7,
            now = LocalDate(2026, 2, 18)
        )

        assertEquals(7, chart.size)
        assertEquals("12", chart.first().first)
        assertEquals(0.0, chart[0].second)
        assertEquals(150.09, chart[4].second)
        assertEquals(824.78, chart.last().second)
    }

    @Test
    fun chartMapperBuildsYearSeriesForCurrentYearOnly() {
        val summary = HomeSummary(
            fiscalMonthlySales = listOf(
                FiscalMonthlySalesPoint(month = "2025-12", amount = 99.0),
                FiscalMonthlySalesPoint(month = "2026-01", amount = 21962.18),
                FiscalMonthlySalesPoint(month = "2026-02", amount = 3539.64)
            )
        )

        val chart = HomeSalesChartMapper.buildChart(
            summary = summary,
            range = HomeSalesRange.YEAR,
            now = LocalDate(2026, 6, 1)
        )

        assertEquals(12, chart.size)
        assertEquals(21962.18, chart[0].second)
        assertEquals(3539.64, chart[1].second)
        assertEquals(0.0, chart[2].second)
        assertEquals(0.0, chart[11].second)
    }
}
