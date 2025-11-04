package com.teco.ventago.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.*
import kotlinx.datetime.format.char
import kotlinx.datetime.until

expect fun parseToLocalDateTime(date :String, formatString: String): LocalDateTime

fun isExpired(createdTime: String, expiryTime: Long): Boolean {
    return try {
        isExpired(Instant.parse(createdTime), expiryTime)
    } catch (e: Exception) {
        true
    }
}

fun isExpired(createdTime: Instant, expiryTime: Long): Boolean {
    val diffInSeconds = createdTime.until(Clock.System.now(), DateTimeUnit.SECOND, TimeZone.UTC)
    return expiryTime > diffInSeconds
}

fun isExpired(expiryTime: Instant): Boolean {
    return expiryTime <= Clock.System.now()
}

fun isExpired(expiryTime: String): Boolean {
    return try {
        isExpired(Instant.parse(expiryTime))
    } catch (e: Exception) {
        true
    }
}

fun LocalTime.formatted(): String {
    return try {
        val hour = this.hour.toString().padStart(2, '0')
        val minute = this.minute.toString().padStart(2, '0')
        val seconds = this.second.toString().padStart(2, '0')
        "$hour:$minute:$seconds"
    } catch (e: Exception) {
        "00:00:00"
    }
}

fun LocalDateTime.dbFormat(): String {
    val customFormat = LocalDateTime.Format {
        year(); char('-'); monthNumber(); char('-'); dayOfMonth()
        char('T')
        hour(); char(':'); minute(); char(':'); second()
    }

    return this.format(customFormat)
}


expect object DateFormat {
    fun getDate(milliSeconds: Long, dateFormat: String): String
    fun getOrdersFormattedDate(date: String): String
    fun getFormattedDate(date: String, inputFormat: String, outputFormat: String): String
}

