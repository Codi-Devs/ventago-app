package com.teco.ventago.utils

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone


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
    /**
     * Return date in specified format.
     * @param milliSeconds Date in milliseconds
     * @param dateFormat Date format
     * @return String representing date in specified format
     */
    actual fun getDate(milliSeconds: Long, dateFormat: String): String {
        // Create a DateFormatter object for displaying date in specified format.
        val simpleDateFormat = SimpleDateFormat(dateFormat, Locale.US)

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSeconds
        return simpleDateFormat.format(calendar.time)
    }

    /**
     * Return date in orders format based on locale.
     * @param date Date in string format
     * @return String representing date in specified format
     */
    actual fun getOrdersFormattedDate(date: String): String {
        var locale = "en"
        if (Locale.getDefault().language.contains("es")) {
            locale = "es"
        }
        var outputFormat = ORDERS_TIME_EN
        if (locale.equals("es", ignoreCase = true)) {
            outputFormat = ORDERS_TIME_ES
        }
        return getFormattedDate(date, ORDERS, outputFormat)
    }


    /**
     * Return date in specified format based on locale.
     * @param date Date in string format
     * @return String representing date in specified format
     */
    actual fun getFormattedDate(date: String, inputFormat: String, outputFormat: String): String {
        return try {
            val formatter = SimpleDateFormat(inputFormat, Locale.getDefault())
            formatter.timeZone = TimeZone.getTimeZone("UTC")
            val value = formatter.parse(date)

            val dateFormatter = SimpleDateFormat(
                outputFormat,
                Locale.getDefault()
            ) //this format changeable

            dateFormatter.timeZone = TimeZone.getDefault()
            if (value == null) return "00-00-0000 00:00"
            dateFormatter.format(value)
        } catch (e: Exception) {
            "00-00-0000 00:00"
        }
    }
}