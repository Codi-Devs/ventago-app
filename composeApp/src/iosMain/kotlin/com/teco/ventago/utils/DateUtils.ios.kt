package com.teco.ventago.utils

import kotlinx.datetime.LocalDateTime
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localeIdentifier
import platform.Foundation.systemTimeZone
import platform.Foundation.timeZoneWithName

actual fun parseToLocalDateTime(
    date: String,
    formatString: String,
): LocalDateTime {
    TODO("Not yet implemented")
}

actual object DateFormat {
    private const val ORDERS: String = "yyyy-MM-dd'T'HH:mm:ss"
    private const val ORDERS_TIME_EN: String = "dd MMMM yyyy, 'at' hh:mm aaa"
    private const val ORDERS_TIME_ES: String = "dd MMMM yyyy, 'a las' hh:mm aaa"
    private const val FALLBACK_DATE: String = "00-00-0000 00:00"


    /**
     * Converts milliseconds to a formatted date string.
     */
    actual fun getDate(milliSeconds: Long, dateFormat: String): String {
        val date = NSDate.dateWithTimeIntervalSince1970((milliSeconds / 1000.0))
        val dateFormatter = NSDateFormatter()
        dateFormatter.dateFormat = dateFormat
        dateFormatter.timeZone = NSTimeZone.systemTimeZone()
        return dateFormatter.stringFromDate(date)
    }

    /**
     * Formats date for orders based on locale.
     */
    actual fun getOrdersFormattedDate(date: String): String {
        val locale = NSLocale.currentLocale.localeIdentifier
        val outputFormat = if (locale.contains("es")) ORDERS_TIME_ES else ORDERS_TIME_EN
        return getFormattedDate(date, ORDERS, outputFormat)
    }

    /**
     * Converts a date string from one format to another.
     */
    actual fun getFormattedDate(date: String, inputFormat: String, outputFormat: String): String {
        val parsedDate = parseDate(date, inputFormat) ?: return FALLBACK_DATE

        val outputFormatter = NSDateFormatter()
        outputFormatter.dateFormat = outputFormat
        outputFormatter.timeZone = NSTimeZone.systemTimeZone()

        return outputFormatter.stringFromDate(parsedDate)
    }

    private fun parseDate(date: String, inputFormat: String): NSDate? {
        val normalizedDate = date.trim()
        if (normalizedDate.isEmpty()) return null

        for (format in candidateInputFormats(inputFormat)) {
            val dateFormatter = NSDateFormatter()
            dateFormatter.locale = NSLocale(localeIdentifier = "en_US_POSIX")
            dateFormatter.dateFormat = format
            dateFormatter.timeZone = NSTimeZone.Companion.timeZoneWithName("UTC")!!

            dateFormatter.dateFromString(normalizedDate)?.let { return it }
        }

        return null
    }

    private fun candidateInputFormats(inputFormat: String): List<String> {
        if (inputFormat != ORDERS) return listOf(inputFormat)

        return listOf(
            ORDERS,
            "$ORDERS.SSS",
            "$ORDERS.SSSSSS",
            "${ORDERS}XXXXX",
            "$ORDERS.SSSXXXXX",
            "$ORDERS.SSSSSSXXXXX",
            "${ORDERS}ZZZZZ",
            "$ORDERS.SSSZZZZZ",
            "$ORDERS.SSSSSSZZZZZ"
        )
    }

}
