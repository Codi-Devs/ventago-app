package com.teco.ventago.features.printers.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.epson.epos2.Epos2Exception
import com.epson.epos2.Epos2CallbackCode
import com.epson.epos2.printer.Printer
import com.epson.epos2.printer.PrinterStatusInfo
import com.epson.epos2.printer.ReceiveListener
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.printers.domain.model.PrintAlignment
import com.teco.ventago.features.printers.domain.model.PrintCommand
import com.teco.ventago.features.printers.domain.model.PrintContext
import com.teco.ventago.features.printers.domain.model.PrintResult
import com.teco.ventago.features.printers.domain.model.PrintResultContext
import com.teco.ventago.features.printers.domain.model.PrinterConfig
import com.teco.ventago.features.printers.domain.model.PrinterConnectionException
import com.teco.ventago.features.printers.domain.model.PrinterSendException
import com.teco.ventago.features.printers.domain.model.toTicketPaperProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

class AndroidEpsonPrinterEngine(
    private val context: Context,
    private val logger: ILoggerService,
) : PrinterEngine {
    override suspend fun execute(
        printerConfig: PrinterConfig,
        commands: List<PrintCommand>,
        context: PrintContext,
        maxAttempts: Int,
    ): PrintResult = withContext(Dispatchers.IO) {
        var lastError: Throwable? = null
        val targets = printerConfig.connectionTargets().ifEmpty { listOf(printerConfig.connectionTarget()) }
        val printer = createPrinter(printerConfig)
        try {
            repeat(maxAttempts) { index ->
                val attempt = index + 1
                for (target in targets) {
                    try {
                        return@withContext executeAttempt(
                            printer = printer,
                            printerConfig = printerConfig,
                            target = target,
                            commands = commands,
                            context = context,
                            attempt = attempt,
                            maxAttempts = maxAttempts
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        lastError = e
                        logger.sendLog(
                            Log(
                                LogLevel.ERROR,
                                "AndroidEpsonPrinterEngine",
                                "Attempt $attempt/$maxAttempts failed for ${printerConfig.host} target=$target: ${e.message ?: "UNKNOWN"}"
                            )
                        )
                    } finally {
                        cleanupPrinterState(printer)
                    }
                }
            }
        } finally {
            cleanupPrinterState(printer)
        }

        throw PrinterSendException(
            message = "No se pudo imprimir en ${printerConfig.host}",
            cause = lastError
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun applyCommand(
        printer: Printer,
        command: PrintCommand,
        printerConfig: PrinterConfig,
    ) {
        when (command) {
            is PrintCommand.Text -> {
                printer.addTextAlign(command.alignment.toEpsonAlignment())
                printer.addTextStyle(
                    if (command.style.inverse) Printer.TRUE else Printer.FALSE,
                    if (command.style.underline) Printer.TRUE else Printer.FALSE,
                    if (command.style.bold) Printer.TRUE else Printer.FALSE,
                    Printer.COLOR_1
                )
                printer.addTextSize(
                    if (command.style.doubleWidth) 2 else 1,
                    if (command.style.doubleHeight) 2 else 1
                )
                printer.addText(command.text.ensureLineEnding())
            }

            is PrintCommand.Feed -> printer.addFeedLine(command.lines)
            is PrintCommand.Cut -> {
                addCutWithFallback(printer, command)
            }

            is PrintCommand.Qr -> {
                printer.addTextAlign(Printer.ALIGN_LEFT)
                printer.addHPosition(command.xPositionDots)
                printer.addSymbol(
                    command.data,
                    Printer.SYMBOL_QRCODE_MODEL_2,
                    command.errorCorrection.toEpsonQrLevel(),
                    command.size,
                    command.size,
                    0
                )
                printer.addHPosition(0)
                printer.addFeedLine(1)
            }

            is PrintCommand.Image -> {
                printer.addTextAlign(Printer.ALIGN_LEFT)
                val bytes = command.source.loadImageBytes()
                val sourceBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: throw PrinterConnectionException("No se pudo decodificar la imagen del ticket")
                val renderedBitmap = sourceBitmap.toPaperAwareBitmap(
                    paperWidthMm = printerConfig.paperWidthMm,
                    widthHintPercent = command.widthHintPercent
                )
                try {
                    printer.addImage(
                        renderedBitmap,
                        0,
                        0,
                        renderedBitmap.width,
                        renderedBitmap.height,
                        Printer.COLOR_1,
                        Printer.MODE_MONO,
                        Printer.HALFTONE_DITHER,
                        1.0,
                        Printer.COMPRESS_AUTO
                    )
                } finally {
                    if (renderedBitmap !== sourceBitmap) {
                        renderedBitmap.recycle()
                    }
                    sourceBitmap.recycle()
                }
                printer.addFeedLine(1)
            }
        }
    }

    private fun createPrinter(config: PrinterConfig): Printer {
        return try {
            Printer(resolvePrinterSeries(config.printerModel), Printer.MODEL_ANK, context)
        } catch (e: Epos2Exception) {
            throw PrinterConnectionException("No se pudo inicializar la impresora Epson", e)
        }
    }

    private fun addCutWithFallback(printer: Printer, command: PrintCommand.Cut) {
        // SDK docs: addCut must be issued at beginning of a line.
        printer.addText("\n")

        val cutModes = if (command.fullCut) {
            // Prefer compatibility-first mode (works on more devices than full-cut variants).
            listOf(Printer.CUT_FEED, Printer.PARAM_DEFAULT, Printer.FULL_CUT_FEED)
        } else {
            listOf(Printer.CUT_FEED, Printer.PARAM_DEFAULT)
        }

        var lastError: Epos2Exception? = null
        cutModes.forEachIndexed { index, mode ->
            try {
                printer.addCut(mode)
                if (index > 0) {
                    logger.sendLog(
                        Log(
                            LogLevel.WARNING,
                            "AndroidEpsonPrinterEngine",
                            "addCut fallback aplicado con ${mode.cutModeName()}."
                        )
                    )
                }
                return
            } catch (e: Epos2Exception) {
                lastError = e
                if (!e.errorStatus.isCutFallbackCompatible()) throw e
                logger.sendLog(
                    Log(
                        LogLevel.WARNING,
                        "AndroidEpsonPrinterEngine",
                        "${mode.cutModeName()} no soportado (${epsonErrorName(e.errorStatus)}). Probando siguiente modo."
                    )
                )
            }
        }

        throw lastError ?: PrinterSendException("No se pudo ejecutar addCut en modo compatible")
    }

    private suspend fun executeAttempt(
        printer: Printer,
        printerConfig: PrinterConfig,
        target: String,
        commands: List<PrintCommand>,
        context: PrintContext,
        attempt: Int,
        maxAttempts: Int,
    ): PrintResult {
        var stage = "connect"
        try {
            printer.clearCommandBuffer()
            val transportTimeout = printerConfig.timeoutMs.takeIf { it > 0 } ?: Printer.PARAM_DEFAULT
            printer.connect(target, transportTimeout)

            stage = "build_commands"
            printer.addTextLang(Printer.LANG_EN)
            commands.forEach { command -> applyCommand(printer, command, printerConfig) }

            stage = "receive_callback"
            awaitSendCompletion(
                printer = printer,
                timeoutMs = printerConfig.timeoutMs,
                sendTimeoutMs = transportTimeout
            )

            return PrintResult(
                success = true,
                attempt = attempt,
                attempts = maxAttempts,
                connectPort = printerConfig.port,
                deviceId = printerConfig.deviceId,
                context = PrintResultContext(source = context.source, orderId = context.orderId),
            )
        } catch (e: TimeoutCancellationException) {
            throw PrinterSendException(
                message = "Epson Android SDK timeout esperando confirmación de impresión en $stage target=$target",
                cause = e
            )
        } catch (e: Epos2Exception) {
            throw buildStageException(printerConfig, target, stage, e)
        }
    }

    private suspend fun awaitSendCompletion(
        printer: Printer,
        timeoutMs: Int,
        sendTimeoutMs: Int,
    ) {
        val effectiveTimeout = timeoutMs.takeIf { it > 0 }?.toLong() ?: DEFAULT_RECEIVE_TIMEOUT_MS
        withTimeout(effectiveTimeout) {
            suspendCancellableCoroutine { continuation ->
                val listener = ReceiveListener { _, code, status, _ ->
                    if (!continuation.isActive) return@ReceiveListener
                    runCatching { printer.setReceiveEventListener(null) }
                    if (code == Epos2CallbackCode.CODE_SUCCESS) {
                        continuation.resume(Unit)
                    } else {
                        continuation.resumeWithException(
                            PrinterSendException(
                                message = "Epson callback error ${callbackCodeName(code)} (${code}) ${status.summary()}"
                            )
                        )
                    }
                }

                continuation.invokeOnCancellation {
                    runCatching { printer.setReceiveEventListener(null) }
                }

                try {
                    printer.setReceiveEventListener(listener)
                    printer.sendData(sendTimeoutMs)
                } catch (e: Exception) {
                    runCatching { printer.setReceiveEventListener(null) }
                    continuation.resumeWithException(e)
                }
            }
        }
    }

    private fun buildStageException(
        printerConfig: PrinterConfig,
        target: String,
        stage: String,
        cause: Epos2Exception,
    ): Exception {
        val errorName = epsonErrorName(cause.errorStatus)
        val message = "Epson Android SDK error en $stage host=${printerConfig.host} target=$target: $errorName (${cause.errorStatus})"
        return when (stage) {
            "connect" -> PrinterConnectionException(message, cause)
            else -> PrinterSendException(message, cause)
        }
    }

    private fun disconnectPrinter(printer: Printer) {
        while (true) {
            try {
                printer.disconnect()
                return
            } catch (e: Epos2Exception) {
                if (e.errorStatus != Epos2Exception.ERR_PROCESSING) {
                    return
                }
                Thread.sleep(DISCONNECT_RETRY_DELAY_MS)
            } catch (_: Exception) {
                return
            }
        }
    }

    private fun cleanupPrinterState(printer: Printer) {
        runCatching { printer.setReceiveEventListener(null) }
        runCatching { disconnectPrinter(printer) }
        runCatching { printer.clearCommandBuffer() }
    }
}

private fun PrintAlignment.toEpsonAlignment(): Int = when (this) {
    PrintAlignment.LEFT -> Printer.ALIGN_LEFT
    PrintAlignment.CENTER -> Printer.ALIGN_CENTER
    PrintAlignment.RIGHT -> Printer.ALIGN_RIGHT
}

private fun String.ensureLineEnding(): String = if (endsWith('\n')) this else "$this\n"

@OptIn(ExperimentalEncodingApi::class)
private fun String.loadImageBytes(): ByteArray {
    val source = trim()
    return if (source.startsWith("http://", ignoreCase = true) || source.startsWith("https://", ignoreCase = true)) {
        downloadImageBytes(source)
    } else {
        Base64.decode(source.substringAfter("base64,", source))
    }
}

private fun downloadImageBytes(url: String): ByteArray {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = IMAGE_DOWNLOAD_TIMEOUT_MS
    connection.readTimeout = IMAGE_DOWNLOAD_TIMEOUT_MS
    connection.instanceFollowRedirects = true
    return connection.inputStream.use { input -> input.readBytes() }
}

private fun Bitmap.toPaperAwareBitmap(
    paperWidthMm: Int,
    widthHintPercent: Int?,
): Bitmap {
    val canvasWidth = paperWidthMm.toTicketPaperProfile().canvasWidthDots
    val fallbackWidth = width.coerceAtMost(canvasWidth)
    val targetWidth = widthHintPercent
        ?.coerceIn(1, 100)
        ?.let { hint -> ((canvasWidth * hint) / 100f).roundToInt() }
        ?.coerceIn(MIN_IMAGE_TARGET_WIDTH_PX, canvasWidth)
        ?: fallbackWidth.coerceIn(MIN_IMAGE_TARGET_WIDTH_PX, canvasWidth)

    val ratio = targetWidth.toFloat() / width.toFloat()
    val targetHeight = (height * ratio).roundToInt().coerceIn(1, MAX_IMAGE_CANVAS_HEIGHT_PX)

    val scaled = if (width == targetWidth && height == targetHeight) {
        this
    } else {
        Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
    }

    val output = Bitmap.createBitmap(canvasWidth, targetHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    canvas.drawColor(Color.WHITE)
    val xOffset = ((canvasWidth - targetWidth) / 2).coerceAtLeast(0)

    canvas.drawBitmap(scaled, xOffset.toFloat(), 0f, Paint(Paint.FILTER_BITMAP_FLAG))
    if (scaled !== this) {
        scaled.recycle()
    }

    return output
}

private fun String.toEpsonQrLevel(): Int = when (trim().uppercase()) {
    "L" -> Printer.LEVEL_L
    "Q" -> Printer.LEVEL_Q
    "H" -> Printer.LEVEL_H
    else -> Printer.LEVEL_M
}

private fun epsonErrorName(code: Int): String = when (code) {
    Epos2Exception.ERR_PARAM -> "ERR_PARAM"
    Epos2Exception.ERR_CONNECT -> "ERR_CONNECT"
    Epos2Exception.ERR_TIMEOUT -> "ERR_TIMEOUT"
    Epos2Exception.ERR_MEMORY -> "ERR_MEMORY"
    Epos2Exception.ERR_ILLEGAL -> "ERR_ILLEGAL"
    Epos2Exception.ERR_PROCESSING -> "ERR_PROCESSING"
    Epos2Exception.ERR_NOT_FOUND -> "ERR_NOT_FOUND"
    Epos2Exception.ERR_IN_USE -> "ERR_IN_USE"
    Epos2Exception.ERR_TYPE_INVALID -> "ERR_TYPE_INVALID"
    Epos2Exception.ERR_DISCONNECT -> "ERR_DISCONNECT"
    Epos2Exception.ERR_ALREADY_OPENED -> "ERR_ALREADY_OPENED"
    Epos2Exception.ERR_ALREADY_USED -> "ERR_ALREADY_USED"
    Epos2Exception.ERR_BOX_COUNT_OVER -> "ERR_BOX_COUNT_OVER"
    Epos2Exception.ERR_BOX_CLIENT_OVER -> "ERR_BOX_CLIENT_OVER"
    Epos2Exception.ERR_UNSUPPORTED -> "ERR_UNSUPPORTED"
    Epos2Exception.ERR_RECOVERY_FAILURE -> "ERR_RECOVERY_FAILURE"
    Epos2Exception.ERR_FAILURE -> "ERR_FAILURE"
    else -> "ERR_UNKNOWN"
}

private fun callbackCodeName(code: Int): String = when (code) {
    Epos2CallbackCode.CODE_SUCCESS -> "CODE_SUCCESS"
    Epos2CallbackCode.CODE_ERR_TIMEOUT -> "CODE_ERR_TIMEOUT"
    Epos2CallbackCode.CODE_ERR_NOT_FOUND -> "CODE_ERR_NOT_FOUND"
    Epos2CallbackCode.CODE_ERR_AUTORECOVER -> "CODE_ERR_AUTORECOVER"
    Epos2CallbackCode.CODE_ERR_COVER_OPEN -> "CODE_ERR_COVER_OPEN"
    Epos2CallbackCode.CODE_ERR_CUTTER -> "CODE_ERR_CUTTER"
    Epos2CallbackCode.CODE_ERR_MECHANICAL -> "CODE_ERR_MECHANICAL"
    Epos2CallbackCode.CODE_ERR_EMPTY -> "CODE_ERR_EMPTY"
    Epos2CallbackCode.CODE_ERR_UNRECOVERABLE -> "CODE_ERR_UNRECOVERABLE"
    Epos2CallbackCode.CODE_ERR_SYSTEM -> "CODE_ERR_SYSTEM"
    Epos2CallbackCode.CODE_ERR_PORT -> "CODE_ERR_PORT"
    Epos2CallbackCode.CODE_PRINTING -> "CODE_PRINTING"
    Epos2CallbackCode.CODE_ERR_SPOOLER -> "CODE_ERR_SPOOLER"
    Epos2CallbackCode.CODE_ERR_BATTERY_LOW -> "CODE_ERR_BATTERY_LOW"
    Epos2CallbackCode.CODE_ERR_TOO_MANY_REQUESTS -> "CODE_ERR_TOO_MANY_REQUESTS"
    Epos2CallbackCode.CODE_CANCELED -> "CODE_CANCELED"
    Epos2CallbackCode.CODE_ERR_ILLEGAL -> "CODE_ERR_ILLEGAL"
    Epos2CallbackCode.CODE_ERR_CONNECT -> "CODE_ERR_CONNECT"
    Epos2CallbackCode.CODE_ERR_DISCONNECT -> "CODE_ERR_DISCONNECT"
    Epos2CallbackCode.CODE_ERR_MEMORY -> "CODE_ERR_MEMORY"
    Epos2CallbackCode.CODE_ERR_PROCESSING -> "CODE_ERR_PROCESSING"
    Epos2CallbackCode.CODE_ERR_PARAM -> "CODE_ERR_PARAM"
    Epos2CallbackCode.CODE_ERR_RECOVERY_FAILURE -> "CODE_ERR_RECOVERY_FAILURE"
    Epos2CallbackCode.CODE_ERR_FAILURE -> "CODE_ERR_FAILURE"
    else -> "CODE_UNKNOWN"
}

private fun Int.isCutFallbackCompatible(): Boolean = this == Epos2Exception.ERR_PARAM ||
    this == Epos2Exception.ERR_ILLEGAL ||
    this == Epos2Exception.ERR_UNSUPPORTED

private fun Int.cutModeName(): String = when (this) {
    Printer.CUT_FEED -> "CUT_FEED"
    Printer.PARAM_DEFAULT -> "PARAM_DEFAULT"
    Printer.FULL_CUT_FEED -> "FULL_CUT_FEED"
    else -> "CUT_UNKNOWN"
}

private fun PrinterStatusInfo?.summary(): String {
    if (this == null) return "status=unavailable"
    return "status(connection=${getConnection()}, online=${getOnline()}, paper=${getPaper()}, coverOpen=${getCoverOpen()}, error=${getErrorStatus()})"
}

private const val DEFAULT_RECEIVE_TIMEOUT_MS = 30_000L
private const val DISCONNECT_RETRY_DELAY_MS = 200L
private const val IMAGE_DOWNLOAD_TIMEOUT_MS = 10_000
private const val MIN_IMAGE_TARGET_WIDTH_PX = 32
private const val MAX_IMAGE_CANVAS_HEIGHT_PX = 1200

private fun resolvePrinterSeries(model: String): Int {
    val normalized = model.uppercase().replace("-", "").replace(" ", "")
    return when {
        normalized.contains("T88VII") -> Printer.TM_T88VII
        normalized.contains("T83III") -> Printer.TM_T83III
        normalized.contains("M30III") -> Printer.TM_M30III
        normalized.contains("M30II") -> Printer.TM_M30II
        normalized.contains("M30") -> Printer.TM_M30
        normalized.contains("M50II") -> Printer.TM_M50II
        normalized.contains("M50") -> Printer.TM_M50
        normalized.contains("L100") -> Printer.TM_L100
        normalized.contains("P80II") -> Printer.TM_P80II
        normalized.contains("P20II") -> Printer.TM_P20II
        normalized.contains("T82") -> Printer.TM_T82
        normalized.contains("T90") -> Printer.TM_T90
        normalized.contains("T70") -> Printer.TM_T70
        normalized.contains("T20") -> Printer.TM_T20
        else -> Printer.TM_T20
    }
}
