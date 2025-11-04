package com.teco.ventago.core.logger

data class Log(val level: LogLevel, val flow: String, var message: String)

enum class LogLevel(val level: String) {
    DEBUG("DEBUG"),
    INFO("INFO"),
    WARNING("WARNING"),
    ERROR("ERROR"),
    BI("BI"),
    ADMIN("ADMIN")
}