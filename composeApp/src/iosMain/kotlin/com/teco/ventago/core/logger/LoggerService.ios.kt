package com.teco.ventago.core.logger

import platform.Foundation.NSLog

actual fun printLog(level: String, message: String) {
    NSLog("$level:$message")
}