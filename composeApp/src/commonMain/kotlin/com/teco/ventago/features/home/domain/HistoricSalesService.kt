package com.teco.ventago.features.home.domain

import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.utils.LocaleHelper
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.database
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

class HistoricSalesService(private val loggerService: ILoggerService) {
    private val db = Firebase.firestore
    private val sales = MutableStateFlow<List<Pair<String, Double>>?>(null)

    fun getSales(): MutableStateFlow<List<Pair<String, Double>>?> = sales

    suspend fun initialize(businessId: Int) {
        val db = Firebase.firestore

        val docRef = db.collection("sales")
            .document(businessId.toString())
            .collection("daily")

        docRef.snapshots.collect{
            val data = mutableListOf<Pair<String, Double>>()
            if (it.documents.isEmpty()) {
                sales.value = data
                return@collect
            }

            val values = it.documents.map { doc ->
                val date = doc.id
                val value = readTotalAmount(doc)
                Pair(date, value)
            }

            val sortedValues = values.sortedBy { (key, _) -> parseDate(key) }
            val recentValues = sortedValues.takeLast(6)

            for (entry in recentValues) {
                data.add(Pair(formatVisitDate(entry.first), entry.second))
            }

            sales.emit(data)
        }
    }

    private fun sortMapByDate(map: Map<String, Long>): Map<String, Long> {
        return map.toList().sortedBy { (key, _) -> parseDate(key) }.toMap()
    }

    private fun parseDate(inputDate: String): LocalDate {
        val (year, month, day) = inputDate.split("-").map { it.toInt() }
        return LocalDate(year, month, day)
    }

    private fun readTotalAmount(doc: DocumentSnapshot): Double {
        val doubleValue = runCatching { doc.get<Double>("total_amount") }.getOrNull()
        if (doubleValue != null) {
            return doubleValue
        }

        val longValue = runCatching { doc.get<Long>("total_amount") }.getOrNull()
        if (longValue != null) {
            return longValue.toDouble()
        }

        val stringValue = runCatching { doc.get<String>("total_amount") }.getOrNull()
        return stringValue?.toDoubleOrNull() ?: 0.0
    }

    private fun formatDate(date: LocalDate): String {
        val day = if (date.dayOfMonth < 10) "0${date.dayOfMonth}" else "${date.dayOfMonth}"
        val month = if (date.monthNumber < 10) "0${date.monthNumber}" else "${date.monthNumber}"
        return "$day-$month-${date.year}"
    }

    private fun getPreviousDays(inputDate: String, missing: Int): List<String> {
        val startDate = parseDate(inputDate)

        val previousDays = mutableListOf<String>()

        for (i in 1..missing) {
            val previousDay = startDate.minus(i, DateTimeUnit.DAY)
            previousDays.add(formatDate(previousDay))
        }

        return previousDays
    }

    private fun formatVisitDate(date: String): String {
        val locale = LocaleHelper.getLocale()
        val dateParts = date.split("-")
        val month = when (dateParts[1]) {
            "01" -> if (locale.contains("es", true)) "ene." else "jan."
            "02" -> "feb."
            "03" -> "mar."
            "04" -> if (locale.contains("es", true)) "abr." else "apr."
            "05" -> "may."
            "06" -> "jun."
            "07" -> "jul."
            "08" -> if (locale.contains("es", true)) "ago." else "aug."
            "09" -> "sep."
            "10" -> "oct."
            "11" -> "nov."
            "12" -> if (locale.contains("es", true)) "dic." else "dec."
            else -> dateParts[1]
        }
        return "${getDayFromDate(dateParts)} $month"
    }

    fun getDayFromDate(date: List<String>): String {
        // return the 0 if length is not 4
        return if (date[0].length != 2) date[2]
        else date[0]
    }
}
