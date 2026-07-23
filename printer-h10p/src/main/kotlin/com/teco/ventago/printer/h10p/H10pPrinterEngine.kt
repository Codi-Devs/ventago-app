package com.teco.ventago.printer.h10p

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.IBinder
import android.os.RemoteException
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.printers.domain.PrinterEngine
import com.teco.ventago.features.printers.domain.model.PrintAlignment
import com.teco.ventago.features.printers.domain.model.PrintCommand
import com.teco.ventago.features.printers.domain.model.PrintContext
import com.teco.ventago.features.printers.domain.model.PrintResult
import com.teco.ventago.features.printers.domain.model.PrintResultContext
import com.teco.ventago.features.printers.domain.model.PrinterConfig
import com.teco.ventago.features.printers.domain.model.PrinterConnectionException
import com.teco.ventago.features.printers.domain.model.PrinterSendException
import com.teco.ventago.features.printers.domain.model.toTicketPaperProfile
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import recieptservice.com.recieptservice.PrinterInterface

class H10pPrinterEngine(
    context: Context,
    private val logger: ILoggerService,
) : PrinterEngine {
    private val appContext = context.applicationContext
    private val connector = H10pPrinterConnector(appContext)
    private val printMutex = Mutex()

    override suspend fun execute(
        printerConfig: PrinterConfig,
        commands: List<PrintCommand>,
        context: PrintContext,
        maxAttempts: Int,
    ): PrintResult = withContext(Dispatchers.IO) {
        var lastError: Throwable? = null
        repeat(maxAttempts.coerceAtLeast(1)) { index ->
            val attempt = index + 1
            try {
                return@withContext printMutex.withLock {
                    executeAttempt(
                        printerConfig = printerConfig,
                        commands = commands,
                        context = context,
                        attempt = attempt,
                        maxAttempts = maxAttempts.coerceAtLeast(1)
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                logger.sendLog(
                    Log(
                        LogLevel.ERROR,
                        "H10pPrinterEngine",
                        "Attempt $attempt/${maxAttempts.coerceAtLeast(1)} failed: ${e.message ?: "UNKNOWN"}"
                    )
                )
            }
        }

        throw PrinterSendException(
            message = "No se pudo imprimir en la impresora interna H10P",
            cause = lastError
        )
    }

    suspend fun isAvailable(timeoutMs: Int = DEFAULT_BIND_TIMEOUT_MS): Boolean {
        return runCatching {
            connector.getPrinter(timeoutMs).getServiceVersion().isNotBlank()
        }.getOrDefault(false)
    }

    suspend fun printRaw(bytes: ByteArray, timeoutMs: Int = DEFAULT_BIND_TIMEOUT_MS) {
        printMutex.withLock {
            try {
                connector.getPrinter(timeoutMs).printEpson(bytes)
            } catch (e: RemoteException) {
                throw PrinterSendException("H10P SDK error enviando bytes crudos", e)
            }
        }
    }

    private suspend fun executeAttempt(
        printerConfig: PrinterConfig,
        commands: List<PrintCommand>,
        context: PrintContext,
        attempt: Int,
        maxAttempts: Int,
    ): PrintResult {
        val timeoutMs = printerConfig.timeoutMs.takeIf { it > 0 } ?: DEFAULT_BIND_TIMEOUT_MS
        val printer = try {
            connector.getPrinter(timeoutMs)
        } catch (e: TimeoutCancellationException) {
            throw PrinterConnectionException("Timeout conectando con la impresora interna H10P", e)
        }

        try {
            val version = printer.getServiceVersion()
            if (version.isBlank()) {
                throw PrinterConnectionException("Servicio de impresora H10P sin versión disponible")
            }

            printer.beginWork()
            resetTextStyle(printer)
            commands.forEach { command -> applyCommand(printer, command, printerConfig) }
            printer.endWork()

            return PrintResult(
                success = true,
                attempt = attempt,
                attempts = maxAttempts,
                connectPort = printerConfig.port,
                deviceId = printerConfig.deviceId,
                context = PrintResultContext(source = context.source, orderId = context.orderId),
                message = "H10P printer service $version"
            )
        } catch (e: RemoteException) {
            throw PrinterSendException("H10P SDK error de impresión: ${e.message ?: "UNKNOWN"}", e)
        } catch (e: IllegalStateException) {
            throw PrinterSendException("H10P SDK estado inválido: ${e.message ?: "UNKNOWN"}", e)
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun applyCommand(
        printer: PrinterInterface,
        command: PrintCommand,
        printerConfig: PrinterConfig,
    ) {
        when (command) {
            is PrintCommand.Text -> {
                printer.setAlignment(command.alignment.toH10pAlignment())
                printer.setTextBold(command.style.bold)
                printer.setTextDoubleWidth(command.style.doubleWidth)
                printer.setTextDoubleHeight(command.style.doubleHeight)
                printer.printText(command.text.ensureLineEnding())
                resetTextStyle(printer)
            }

            is PrintCommand.Feed -> printer.nextLine(command.lines.coerceAtLeast(0))
            is PrintCommand.Cut -> printer.nextLine(if (command.fullCut) 4 else 3)
            is PrintCommand.Qr -> {
                printer.setAlignment(command.alignment.toH10pAlignment())
                printer.printQRCode(
                    command.data,
                    command.size.coerceIn(1, 16),
                    command.errorCorrection.toH10pQrErrorLevel()
                )
                printer.nextLine(1)
            }

            is PrintCommand.Image -> {
                val bytes = command.source.loadImageBytes()
                val sourceBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: throw PrinterConnectionException("No se pudo decodificar la imagen del ticket")
                val renderedBitmap = sourceBitmap.toPaperAwareBitmap(
                    paperWidthMm = printerConfig.paperWidthMm,
                    widthHintPercent = command.widthHintPercent
                )
                try {
                    printer.setAlignment(command.alignment.toH10pAlignment())
                    printer.printBitmap(renderedBitmap)
                    printer.nextLine(1)
                } finally {
                    if (renderedBitmap !== sourceBitmap) {
                        renderedBitmap.recycle()
                    }
                    sourceBitmap.recycle()
                }
            }
        }
    }

    private fun resetTextStyle(printer: PrinterInterface) {
        printer.setTextBold(false)
        printer.setTextDoubleWidth(false)
        printer.setTextDoubleHeight(false)
        printer.setAlignment(H10P_ALIGN_LEFT)
    }
}

private class H10pPrinterConnector(
    private val context: Context,
) {
    private val bindMutex = Mutex()
    private var printer: PrinterInterface? = null

    suspend fun getPrinter(timeoutMs: Int): PrinterInterface {
        printer?.takeIf { it.asBinder().isBinderAlive }?.let { return it }

        return bindMutex.withLock {
            printer?.takeIf { it.asBinder().isBinderAlive }?.let { return@withLock it }
            withTimeout(timeoutMs.toLong().coerceAtLeast(MIN_BIND_TIMEOUT_MS)) {
                suspendCancellableCoroutine { continuation ->
                    var resumed = false
                    lateinit var connection: ServiceConnection
                    connection = object : ServiceConnection {
                        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                            if (resumed || !continuation.isActive) return
                            resumed = true
                            val boundPrinter = PrinterInterface.Stub.asInterface(service)
                            printer = boundPrinter
                            continuation.resume(boundPrinter)
                        }

                        override fun onServiceDisconnected(name: ComponentName?) {
                            printer = null
                        }

                        override fun onBindingDied(name: ComponentName?) {
                            printer = null
                        }

                        override fun onNullBinding(name: ComponentName?) {
                            printer = null
                            if (resumed || !continuation.isActive) return
                            resumed = true
                            continuation.resumeWithException(
                                PrinterConnectionException("Servicio de impresora H10P devolvió null binding")
                            )
                        }
                    }

                    continuation.invokeOnCancellation {
                        if (!resumed) {
                            runCatching { context.unbindService(connection) }
                        }
                    }

                    val intent = Intent().setClassName(
                        H10P_SERVICE_PACKAGE,
                        H10P_SERVICE_CLASS
                    )
                    val didBind = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
                    if (!didBind && !resumed && continuation.isActive) {
                        resumed = true
                        continuation.resumeWithException(
                            PrinterConnectionException("No se pudo enlazar con el servicio de impresora H10P")
                        )
                    }
                }
            }
        }
    }
}

private fun PrintAlignment.toH10pAlignment(): Int = when (this) {
    PrintAlignment.LEFT -> H10P_ALIGN_LEFT
    PrintAlignment.CENTER -> H10P_ALIGN_CENTER
    PrintAlignment.RIGHT -> H10P_ALIGN_RIGHT
}

private fun String.toH10pQrErrorLevel(): Int = when (uppercase()) {
    "L" -> 0
    "M" -> 1
    "Q" -> 2
    "H" -> 3
    else -> 1
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
    val profile = paperWidthMm.toTicketPaperProfile()
    val maxWidth = profile.canvasWidthDots
    val requestedWidth = widthHintPercent
        ?.coerceIn(1, 100)
        ?.let { (maxWidth * it / 100.0).roundToInt() }
        ?: width
    val targetWidth = requestedWidth.coerceAtMost(maxWidth).coerceAtLeast(1)
    val scale = targetWidth.toFloat() / width.toFloat()
    val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
    if (targetWidth == width && targetHeight == height) return this

    val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    canvas.drawColor(Color.WHITE)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    canvas.drawBitmap(this, null, android.graphics.Rect(0, 0, targetWidth, targetHeight), paint)
    return output
}

private const val H10P_SERVICE_PACKAGE = "recieptservice.com.recieptservice"
private const val H10P_SERVICE_CLASS = "recieptservice.com.recieptservice.service.PrinterService"
private const val H10P_ALIGN_LEFT = 0
private const val H10P_ALIGN_CENTER = 1
private const val H10P_ALIGN_RIGHT = 2
private const val DEFAULT_BIND_TIMEOUT_MS = 10_000
private const val MIN_BIND_TIMEOUT_MS = 1_000L
private const val IMAGE_DOWNLOAD_TIMEOUT_MS = 10_000
