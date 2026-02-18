package com.teco.ventago.features.home.domain.model

import com.teco.ventago.utils.LocaleHelper
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

object HomeSalesChartMapper {

    fun buildChart(
        summary: HomeSummary?,
        range: HomeSalesRange,
        now: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    ): List<Pair<String, Double>> {
        if (summary == null) return emptyList()

        val filledSeries = when (range) {
            HomeSalesRange.D7 -> buildDailySeries(
                points = summary.dailySalesChart,
                from = now.minus(6, DateTimeUnit.DAY),
                to = now
            )

            HomeSalesRange.D15 -> buildDailySeries(
                points = summary.dailySalesChart,
                from = now.minus(14, DateTimeUnit.DAY),
                to = now
            )

            HomeSalesRange.MONTH -> buildDailySeries(
                points = summary.dailySalesChart,
                from = LocalDate(now.year, now.monthNumber, 1),
                to = now
            )

            HomeSalesRange.YEAR -> buildYearSeries(
                points = summary.fiscalMonthlySales,
                year = now.year
            )
        }

        return if (filledSeries.any { it.second != 0.0 }) filledSeries else emptyList()
    }

    private fun buildDailySeries(
        points: List<DailySalesPoint>,
        from: LocalDate,
        to: LocalDate
    ): List<Pair<String, Double>> {
        val valuesByDate = points.associate { it.date to it.amount }
        val result = mutableListOf<Pair<String, Double>>()

        var cursor = from
        while (cursor <= to) {
            val key = cursor.toIsoDate()
            result.add(cursor.dayOfMonth.toString() to (valuesByDate[key] ?: 0.0))
            cursor = cursor.plus(1, DateTimeUnit.DAY)
        }

        return result
    }

    private fun buildYearSeries(
        points: List<FiscalMonthlySalesPoint>,
        year: Int
    ): List<Pair<String, Double>> {
        val valuesByMonth = points.associateBy(
            keySelector = { it.month },
            valueTransform = { it.amount }
        )

        return (1..12).map { monthNumber ->
            val key = "$year-${monthNumber.toString().padStart(2, '0')}"
            monthLabel(monthNumber) to (valuesByMonth[key] ?: 0.0)
        }
    }

    private fun LocalDate.toIsoDate(): String {
        val month = monthNumber.toString().padStart(2, '0')
        val day = dayOfMonth.toString().padStart(2, '0')
        return "$year-$month-$day"
    }

    private fun monthLabel(month: Int): String {
        val locale = LocaleHelper.getLocale()
        return when (month) {
            1 -> if (locale.contains("es", true)) "ene." else "jan."
            2 -> "feb."
            3 -> "mar."
            4 -> if (locale.contains("es", true)) "abr." else "apr."
            5 -> "may."
            6 -> "jun."
            7 -> "jul."
            8 -> if (locale.contains("es", true)) "ago." else "aug."
            9 -> "sep."
            10 -> "oct."
            11 -> "nov."
            12 -> if (locale.contains("es", true)) "dic." else "dec."
            else -> month.toString()
        }
    }
}
