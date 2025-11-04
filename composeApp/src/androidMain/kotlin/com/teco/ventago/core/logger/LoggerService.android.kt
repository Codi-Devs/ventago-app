package com.teco.ventago.core.logger

actual fun printLog(level: String, message: String) {
    println("[$level] $message")
}