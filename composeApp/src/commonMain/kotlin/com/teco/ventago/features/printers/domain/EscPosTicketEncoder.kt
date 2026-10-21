package com.teco.ventago.features.printers.domain

import com.teco.ventago.features.printers.domain.model.PrintAlignment
import com.teco.ventago.features.printers.domain.model.PrintCommand
import com.teco.ventago.features.printers.domain.model.TicketBlockStyle

/**
 * Bytes ESC/POS que entienden las termicas portatiles (MPT-II y equivalentes).
 * El corte se traduce a avance de papel: esas impresoras no traen cuchilla.
 * Logo y QR llegan como [PrintCommand.Raster]: el firmware de estas termicas
 * rechaza el QR nativo (`2QR CREAT ERR!`) y no comparte la fuente del H10.
 */
object EscPosTicketEncoder {
    fun encode(commands: List<PrintCommand>): ByteArray {
        val out = Bytes()
        fun raw(vararg bytes: Int) {
            bytes.forEach { out.write(it) }
        }

        raw(0x1B, 0x40)
        selectPosFont(out)

        commands.forEach { command ->
            when (command) {
                is PrintCommand.Text -> writeText(out, command.text, command.alignment, command.style)
                is PrintCommand.Feed -> raw(0x1B, 0x64, command.lines.coerceIn(1, 255))
                is PrintCommand.Cut -> raw(0x1B, 0x64, 0x04)
                is PrintCommand.Qr -> writeQr(out, command.data, command.size, command.errorCorrection, command.alignment)
                is PrintCommand.Image -> Unit
                is PrintCommand.Raster -> out.write(command.bytes)
            }
        }
        return out.toByteArray()
    }
}

fun bluetoothPrinterNameScore(name: String?): Int {
    val value = name?.trim().orEmpty()
    if (value.isEmpty()) return 0
    var score = 1
    if (value.contains("MPT", ignoreCase = true)) score += 100
    if (value.contains("printer", ignoreCase = true) || value.contains("print", ignoreCase = true)) score += 50
    if (value.contains("thermal", ignoreCase = true) || value.contains("pos", ignoreCase = true)) score += 20
    if (value.contains("goojprt", ignoreCase = true) || value.contains("rpp", ignoreCase = true)) score += 40
    return score
}

private fun selectPosFont(out: Bytes) {
    out.write(0x1C)
    out.write(0x2E)
    out.write(0x1B)
    out.write(0x74)
    out.write(0x02)
    out.write(0x1B)
    out.write(0x4D)
    out.write(0x00)
    out.write(0x1B)
    out.write(0x32)
    out.write(0x1B)
    out.write(0x20)
    out.write(0x00)
}

private fun writeText(
    out: Bytes,
    text: String,
    alignment: PrintAlignment,
    style: TicketBlockStyle,
) {
    val align = when (alignment) {
        PrintAlignment.LEFT -> 0
        PrintAlignment.CENTER -> 1
        PrintAlignment.RIGHT -> 2
    }
    var mode = 0
    if (style.doubleHeight) mode = mode or 0x10
    if (style.doubleWidth) mode = mode or 0x20
    selectPosFont(out)
    out.write(0x1B)
    out.write(0x61)
    out.write(align)
    out.write(0x1B)
    out.write(0x45)
    out.write(if (style.bold) 1 else 0)
    out.write(0x1B)
    out.write(0x2D)
    out.write(if (style.underline) 1 else 0)
    out.write(0x1D)
    out.write(0x42)
    out.write(if (style.inverse) 1 else 0)
    out.write(0x1B)
    out.write(0x21)
    out.write(mode)
    out.write(encodePc850(text))
    out.write(0x0A)
}

private fun writeQr(
    out: Bytes,
    data: String,
    moduleSize: Int,
    errorCorrection: String,
    alignment: PrintAlignment,
) {
    val payload = data.encodeToByteArray()
    if (payload.isEmpty()) return
    val align = when (alignment) {
        PrintAlignment.LEFT -> 0
        PrintAlignment.CENTER -> 1
        PrintAlignment.RIGHT -> 2
    }
    val size = moduleSize.coerceIn(2, 3)
    val level = when (errorCorrection.trim().uppercase()) {
        "L" -> 48
        "Q" -> 50
        "H" -> 51
        else -> 49
    }
    val storeLength = payload.size + 3
    selectPosFont(out)
    out.write(0x1B)
    out.write(0x21)
    out.write(0x00)
    out.write(0x1B)
    out.write(0x61)
    out.write(align)
    out.write(0x0A)
    out.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, size.toByte()))
    out.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, level.toByte()))
    out.write(0x1D)
    out.write(0x28)
    out.write(0x6B)
    out.write(storeLength and 0xFF)
    out.write((storeLength shr 8) and 0xFF)
    out.write(0x31)
    out.write(0x50)
    out.write(0x30)
    out.write(payload)
    out.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30))
    out.write(0x0A)
}

private fun encodePc850(text: String): ByteArray {
    val out = Bytes()
    val composed = composeSpanishMarks(text)
    composed.forEach { character ->
        out.write(pc850Byte(character))
    }
    return out.toByteArray()
}

private fun composeSpanishMarks(text: String): String {
    val out = StringBuilder(text.length)
    var index = 0
    while (index < text.length) {
        val current = text[index]
        val mark = text.getOrNull(index + 1)
        val composed = when (mark) {
            '\u0301' -> ACUTE_MARKS[current]
            '\u0303' -> TILDE_MARKS[current]
            else -> null
        }
        if (composed != null) {
            out.append(composed)
            index += 2
        } else {
            out.append(current)
            index += 1
        }
    }
    return out.toString()
}

private class Bytes {
    private val data = ArrayList<Byte>()

    fun write(value: Int) {
        data.add(value.toByte())
    }

    fun write(value: ByteArray) {
        value.forEach { data.add(it) }
    }

    fun toByteArray(): ByteArray = ByteArray(data.size) { index -> data[index] }
}

private fun pc850Byte(character: Char): Int {
    if (character.code in 0..127) return character.code
    return SPANISH_PC850[character] ?: ASCII_FOLD[character] ?: '?'.code
}

private val SPANISH_PC850 = mapOf(
    'á' to 0xA0,
    'é' to 0x82,
    'í' to 0xA1,
    'ó' to 0xA2,
    'ú' to 0xA3,
    'ñ' to 0xA4,
    'Ñ' to 0xA5,
    'Á' to 0xB5,
    'É' to 0x90,
    'Í' to 0xD6,
    'Ó' to 0xE0,
    'Ú' to 0xE9,
    'ü' to 0x81,
    'Ü' to 0x9A,
    '¿' to 0xA8,
    '¡' to 0xAD,
)

private val ACUTE_MARKS = mapOf(
    'a' to 'á',
    'e' to 'é',
    'i' to 'í',
    'o' to 'ó',
    'u' to 'ú',
    'A' to 'Á',
    'E' to 'É',
    'I' to 'Í',
    'O' to 'Ó',
    'U' to 'Ú',
)

private val TILDE_MARKS = mapOf(
    'n' to 'ñ',
    'N' to 'Ñ',
)

private val ASCII_FOLD = mapOf(
    'à' to 'a'.code,
    'ä' to 'a'.code,
    'â' to 'a'.code,
    'è' to 'e'.code,
    'ë' to 'e'.code,
    'ê' to 'e'.code,
    'ì' to 'i'.code,
    'ï' to 'i'.code,
    'î' to 'i'.code,
    'ò' to 'o'.code,
    'ö' to 'o'.code,
    'ô' to 'o'.code,
    'ù' to 'u'.code,
    'û' to 'u'.code,
)
