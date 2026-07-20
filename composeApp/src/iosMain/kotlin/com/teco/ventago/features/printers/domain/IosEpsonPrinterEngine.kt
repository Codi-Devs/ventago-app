@file:OptIn(kotlinx.cinterop.BetaInteropApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package com.teco.ventago.features.printers.domain

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
import com.teco.ventago.vendor.epson.EPOS2_ALIGN_CENTER
import com.teco.ventago.vendor.epson.EPOS2_ALIGN_LEFT
import com.teco.ventago.vendor.epson.EPOS2_ALIGN_RIGHT
import com.teco.ventago.vendor.epson.EPOS2_COLOR_1
import com.teco.ventago.vendor.epson.EPOS2_COMPRESS_AUTO
import com.teco.ventago.vendor.epson.EPOS2_CUT_FEED
import com.teco.ventago.vendor.epson.EPOS2_ERR_ILLEGAL
import com.teco.ventago.vendor.epson.EPOS2_ERR_PARAM
import com.teco.ventago.vendor.epson.EPOS2_ERR_UNSUPPORTED
import com.teco.ventago.vendor.epson.EPOS2_FULL_CUT_FEED
import com.teco.ventago.vendor.epson.EPOS2_HALFTONE_DITHER
import com.teco.ventago.vendor.epson.EPOS2_LEVEL_H
import com.teco.ventago.vendor.epson.EPOS2_LEVEL_L
import com.teco.ventago.vendor.epson.EPOS2_LEVEL_M
import com.teco.ventago.vendor.epson.EPOS2_LEVEL_Q
import com.teco.ventago.vendor.epson.EPOS2_MODE_MONO
import com.teco.ventago.vendor.epson.EPOS2_MODEL_ANK
import com.teco.ventago.vendor.epson.EPOS2_SUCCESS
import com.teco.ventago.vendor.epson.EPOS2_SYMBOL_QRCODE_MODEL_2
import com.teco.ventago.vendor.epson.EPOS2_TM_L100
import com.teco.ventago.vendor.epson.EPOS2_TM_M30
import com.teco.ventago.vendor.epson.EPOS2_TM_M30II
import com.teco.ventago.vendor.epson.EPOS2_TM_M30III
import com.teco.ventago.vendor.epson.EPOS2_TM_M50
import com.teco.ventago.vendor.epson.EPOS2_TM_M50II
import com.teco.ventago.vendor.epson.EPOS2_TM_P20II
import com.teco.ventago.vendor.epson.EPOS2_TM_P80II
import com.teco.ventago.vendor.epson.EPOS2_TM_T20
import com.teco.ventago.vendor.epson.EPOS2_TM_T70
import com.teco.ventago.vendor.epson.EPOS2_TM_T82
import com.teco.ventago.vendor.epson.EPOS2_TM_T83III
import com.teco.ventago.vendor.epson.EPOS2_TM_T88VII
import com.teco.ventago.vendor.epson.EPOS2_TM_T90
import com.teco.ventago.vendor.epson.Epos2Printer
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.NSURL
import platform.UIKit.UIColor
import platform.UIKit.UIImage
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIRectFill
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.roundToLong

class IosEpsonPrinterEngine(
    private val logger: ILoggerService,
) : PrinterEngine {
    override suspend fun execute(
        printerConfig: PrinterConfig,
        commands: List<PrintCommand>,
        context: PrintContext,
        maxAttempts: Int,
    ): PrintResult = withContext(Dispatchers.IO) {
        var lastError: Throwable? = null
        repeat(maxAttempts) { index ->
            val attempt = index + 1
            val printer = Epos2Printer(resolvePrinterSeries(printerConfig.printerModel), EPOS2_MODEL_ANK.toInt())
            try {
                ensureSuccess(printer.clearCommandBuffer().toInt(), "clearCommandBuffer")
                ensureSuccess(
                    printer.connect(printerConfig.connectionTarget(), printerConfig.timeoutMs.toLong()).toInt(),
                    "connect"
                )
                commands.forEach { command -> applyCommand(printer, command, printerConfig) }
                ensureSuccess(printer.sendData(printerConfig.timeoutMs.toLong()).toInt(), "sendData")
                return@withContext PrintResult(
                    success = true,
                    attempt = attempt,
                    attempts = maxAttempts,
                    connectPort = printerConfig.port,
                    deviceId = printerConfig.deviceId,
                    context = PrintResultContext(source = context.source, orderId = context.orderId),
                )
            } catch (e: Exception) {
                lastError = e
                logger.sendLog(
                    Log(
                        LogLevel.ERROR,
                        "IosEpsonPrinterEngine",
                        "Attempt $attempt/$maxAttempts failed for ${printerConfig.host}: ${e.message ?: "UNKNOWN"}"
                    )
                )
            } finally {
                runCatching { printer.disconnect() }
                runCatching { printer.clearCommandBuffer() }
            }
        }

        throw PrinterSendException("No se pudo imprimir en ${printerConfig.host}", lastError)
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun applyCommand(printer: Epos2Printer, command: PrintCommand, printerConfig: PrinterConfig) {
        when (command) {
            is PrintCommand.Text -> {
                ensureSuccess(printer.addTextAlign(command.alignment.toEpsonAlignment()).toInt(), "addTextAlign")
                ensureSuccess(
                    printer.addTextStyle(
                        if (command.style.inverse) 1 else 0,
                        if (command.style.underline) 1 else 0,
                        if (command.style.bold) 1 else 0,
                        EPOS2_COLOR_1.toInt()
                    ).toInt(),
                    "addTextStyle"
                )
                ensureSuccess(
                    printer.addTextSize(
                        if (command.style.doubleWidth) 2 else 1,
                        if (command.style.doubleHeight) 2 else 1
                    ).toInt(),
                    "addTextSize"
                )
                ensureSuccess(printer.addText(command.text.ensureLineEnding()).toInt(), "addText")
            }

            is PrintCommand.Feed -> ensureSuccess(printer.addFeedLine(command.lines.toLong()).toInt(), "addFeedLine")
            is PrintCommand.Cut -> {
                addCutWithFallback(printer, command)
            }

            is PrintCommand.Qr -> {
                ensureSuccess(printer.addTextAlign(EPOS2_ALIGN_LEFT.toInt()).toInt(), "addTextAlign")
                ensureSuccess(printer.addHPosition(command.xPositionDots.toLong()).toInt(), "addHPosition")
                ensureSuccess(
                    printer.addSymbol(
                        command.data,
                        EPOS2_SYMBOL_QRCODE_MODEL_2.toInt(),
                        command.errorCorrection.toEpsonQrLevel(),
                        command.size.toLong(),
                        command.size.toLong(),
                        0
                    ).toInt(),
                    "addSymbol"
                )
                ensureSuccess(printer.addHPosition(0).toInt(), "resetHPosition")
                ensureSuccess(printer.addFeedLine(1).toInt(), "addFeedLine")
            }

            is PrintCommand.Image -> {
                ensureSuccess(printer.addTextAlign(EPOS2_ALIGN_LEFT.toInt()).toInt(), "addTextAlign")
                val image = command.source.toUiImage()
                    ?: throw PrinterConnectionException("No se pudo decodificar la imagen del ticket")
                val renderedImage = image.toPaperAwareImage(
                    paperWidthMm = printerConfig.paperWidthMm,
                    widthHintPercent = command.widthHintPercent
                )
                val width = renderedImage.size.useContents { this.width.toLong() }
                val height = renderedImage.size.useContents { this.height.toLong() }
                ensureSuccess(
                    printer.addImage(
                        renderedImage,
                        0,
                        0,
                        width,
                        height,
                        EPOS2_COLOR_1.toInt(),
                        EPOS2_MODE_MONO.toInt(),
                        EPOS2_HALFTONE_DITHER.toInt(),
                        1.0,
                        EPOS2_COMPRESS_AUTO.toInt()
                    ).toInt(),
                    "addImage"
                )
                ensureSuccess(printer.addFeedLine(1).toInt(), "addFeedLine")
            }
        }
    }

    private fun ensureSuccess(code: Int, action: String) {
        if (code != EPOS2_SUCCESS.toInt()) {
            throw PrinterConnectionException("Epson iOS SDK error en $action: código $code")
        }
    }

    private fun addCutWithFallback(printer: Epos2Printer, command: PrintCommand.Cut) {
        if (!command.fullCut) {
            ensureSuccess(printer.addCut(EPOS2_CUT_FEED.toInt()).toInt(), "addCut")
            return
        }

        val fullCutCode = printer.addCut(EPOS2_FULL_CUT_FEED.toInt()).toInt()
        if (fullCutCode == EPOS2_SUCCESS.toInt()) return
        if (!fullCutCode.isCutFallbackCompatible()) {
            ensureSuccess(fullCutCode, "addCut")
            return
        }

        logger.sendLog(
            Log(
                LogLevel.WARNING,
                "IosEpsonPrinterEngine",
                "FULL_CUT_FEED no soportado (code=$fullCutCode). Reintentando con CUT_FEED."
            )
        )
        ensureSuccess(printer.addCut(EPOS2_CUT_FEED.toInt()).toInt(), "addCutFallback")
    }
}

private fun resolvePrinterSeries(model: String): Int {
    val normalized = model.uppercase().replace("-", "").replace(" ", "")
    return when {
        normalized.contains("T88VII") -> EPOS2_TM_T88VII.toInt()
        normalized.contains("T83III") -> EPOS2_TM_T83III.toInt()
        normalized.contains("M30III") -> EPOS2_TM_M30III.toInt()
        normalized.contains("M30II") -> EPOS2_TM_M30II.toInt()
        normalized.contains("M30") -> EPOS2_TM_M30.toInt()
        normalized.contains("M50II") -> EPOS2_TM_M50II.toInt()
        normalized.contains("M50") -> EPOS2_TM_M50.toInt()
        normalized.contains("L100") -> EPOS2_TM_L100.toInt()
        normalized.contains("P80II") -> EPOS2_TM_P80II.toInt()
        normalized.contains("P20II") -> EPOS2_TM_P20II.toInt()
        normalized.contains("T82") -> EPOS2_TM_T82.toInt()
        normalized.contains("T90") -> EPOS2_TM_T90.toInt()
        normalized.contains("T70") -> EPOS2_TM_T70.toInt()
        normalized.contains("T20") -> EPOS2_TM_T20.toInt()
        else -> EPOS2_TM_T20.toInt()
    }
}

private fun PrintAlignment.toEpsonAlignment(): Int = when (this) {
    PrintAlignment.LEFT -> EPOS2_ALIGN_LEFT.toInt()
    PrintAlignment.CENTER -> EPOS2_ALIGN_CENTER.toInt()
    PrintAlignment.RIGHT -> EPOS2_ALIGN_RIGHT.toInt()
}

private fun String.toEpsonQrLevel(): Int = when (trim().uppercase()) {
    "L" -> EPOS2_LEVEL_L.toInt()
    "Q" -> EPOS2_LEVEL_Q.toInt()
    "H" -> EPOS2_LEVEL_H.toInt()
    else -> EPOS2_LEVEL_M.toInt()
}

private fun String.ensureLineEnding(): String = if (endsWith('\n')) this else "$this\n"

@OptIn(ExperimentalEncodingApi::class)
private fun String.toUiImage(): UIImage? {
    val source = trim()
    return if (source.startsWith("http://", ignoreCase = true) || source.startsWith("https://", ignoreCase = true)) {
        val url = NSURL.URLWithString(source) ?: return null
        UIImage.imageWithData(NSData.dataWithContentsOfURL(url) ?: return null)
    } else {
        val bytes = Base64.decode(source.substringAfter("base64,", source))
        UIImage.imageWithData(bytes.toNSData())
    }
}

private fun Int.isCutFallbackCompatible(): Boolean = this == EPOS2_ERR_PARAM.toInt() ||
    this == EPOS2_ERR_ILLEGAL.toInt() ||
    this == EPOS2_ERR_UNSUPPORTED.toInt()

@OptIn(ExperimentalEncodingApi::class)
private fun ByteArray.toNSData(): NSData =
    usePinned { pinned -> NSData.create(bytes = pinned.addressOf(0), length = size.toULong()) }

private fun UIImage.toPaperAwareImage(
    paperWidthMm: Int,
    widthHintPercent: Int?,
): UIImage {
    val canvasWidth = paperWidthMm.toTicketPaperProfile().canvasWidthDots
    val sourceWidth = size.useContents { this.width }.coerceAtLeast(1.0)
    val sourceHeight = size.useContents { this.height }.coerceAtLeast(1.0)
    val fallbackWidth = sourceWidth.coerceAtMost(canvasWidth.toDouble())
    val targetWidth = widthHintPercent
        ?.coerceIn(1, 100)
        ?.let { hint -> ((canvasWidth * hint) / 100.0).roundToLong().toDouble() }
        ?.coerceIn(MIN_IMAGE_TARGET_WIDTH_PX.toDouble(), canvasWidth.toDouble())
        ?: fallbackWidth.coerceIn(MIN_IMAGE_TARGET_WIDTH_PX.toDouble(), canvasWidth.toDouble())
    val targetHeight = (sourceHeight * (targetWidth / sourceWidth))
        .roundToLong()
        .coerceIn(1L, MAX_IMAGE_CANVAS_HEIGHT_PX.toLong())
        .toDouble()
    val xOffset = ((canvasWidth - targetWidth) / 2.0).coerceAtLeast(0.0)

    UIGraphicsBeginImageContextWithOptions(
        CGSizeMake(canvasWidth.toDouble(), targetHeight),
        true,
        1.0
    )
    try {
        UIColor.whiteColor.setFill()
        UIRectFill(CGRectMake(0.0, 0.0, canvasWidth.toDouble(), targetHeight))
        drawInRect(CGRectMake(xOffset, 0.0, targetWidth, targetHeight))
        return UIGraphicsGetImageFromCurrentImageContext() ?: this
    } finally {
        UIGraphicsEndImageContext()
    }
}

private const val MIN_IMAGE_TARGET_WIDTH_PX = 32
private const val MAX_IMAGE_CANVAS_HEIGHT_PX = 1200
