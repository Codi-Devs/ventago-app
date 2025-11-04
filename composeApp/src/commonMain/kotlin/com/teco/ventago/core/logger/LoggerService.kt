package com.teco.ventago.core.logger

import com.teco.ventago.Configs
import com.teco.ventago.core.SecureStorage
import com.teco.ventago.features.auth.domain.SecureConstants
import com.teco.ventago.utils.LocaleHelper
import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LoggerService(private val client: HttpClient, private val secure: SecureStorage): ILoggerService {

    /**
     * Sends a log to the server in background
     */
    override fun sendLog(log: Log) {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            sendLogInternal(log)
        }
    }

    private suspend fun sendLogInternal(log: Log) {
        try {
            val jwt = secure.string(forKey = SecureConstants.JWT_TOKEN)
            log.message = log.message.replace("\"", "'")
            println("Log: ${log.message}")
            if (jwt == null) {
                client.post(Configs.ordersBasePath+"/api/logger/log") {
                    headers {
                        append(HttpHeaders.Accept, "*/*")
                        append(HttpHeaders.ContentType, "application/json")
                        append(HttpHeaders.AcceptLanguage, LocaleHelper.getLocale())
                    }
                    contentType(ContentType.Application.Json)
                    setBody(logBody(log))
                }
            } else {
                client.post(Configs.ordersBasePath+"/api/logger/log") {
                    headers {
                        append(HttpHeaders.Accept, "*/*")
                        append(HttpHeaders.Authorization, "Bearer $jwt")
                        append(HttpHeaders.ContentType, "application/json")
                        append(HttpHeaders.AcceptLanguage, LocaleHelper.getLocale())
                    }
                    contentType(ContentType.Application.Json)
                    setBody(logBody(log))
                }
            }
        } catch (_: Exception) {
            // Do nothing
        }
    }

    private fun logBody(log: Log): String =
        """
            {
                "level": "${log.level.level}",
                "flow": "${log.flow}",
                "message": "${log.message}"
            }
        """.trimIndent()
}

expect fun printLog(level: String, message: String)