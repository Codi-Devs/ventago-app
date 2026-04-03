package com.teco.ventago.features.printers.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class TicketDocumentPayload(
    @SerialName("ticket_layout") val ticketLayout: JsonObject? = null,
    @SerialName("ticketLayout") val ticketLayoutCamel: JsonObject? = null,
    val blocks: JsonArray? = null,
) {
    fun resolvedLayoutElement(): JsonElement? {
        return ticketLayout ?: ticketLayoutCamel ?: blocks?.let { JsonObject(mapOf("blocks" to it)) }
    }
}

data class TicketLayout(
    val blocks: List<TicketBlock>,
)

sealed class TicketBlock {
    abstract val type: String
    abstract val alignment: PrintAlignment
    abstract val style: TicketBlockStyle

    data class Text(
        override val type: String = "text",
        val text: String,
        override val alignment: PrintAlignment = PrintAlignment.LEFT,
        override val style: TicketBlockStyle = TicketBlockStyle(),
    ) : TicketBlock()

    data class Separator(
        override val type: String = "separator",
        val character: Char = '-',
        val label: String? = null,
        override val alignment: PrintAlignment = PrintAlignment.LEFT,
        override val style: TicketBlockStyle = TicketBlockStyle(),
    ) : TicketBlock()

    data class Feed(
        override val type: String = "feed",
        val lines: Int = 1,
        override val alignment: PrintAlignment = PrintAlignment.LEFT,
        override val style: TicketBlockStyle = TicketBlockStyle(),
    ) : TicketBlock()

    data class Cut(
        override val type: String = "cut",
        val fullCut: Boolean = true,
        override val alignment: PrintAlignment = PrintAlignment.LEFT,
        override val style: TicketBlockStyle = TicketBlockStyle(),
    ) : TicketBlock()

    data class KeyValue(
        override val type: String = "key_value",
        val key: String,
        val value: String,
        val alignValueRight: Boolean = false,
        override val alignment: PrintAlignment = PrintAlignment.LEFT,
        override val style: TicketBlockStyle = TicketBlockStyle(),
    ) : TicketBlock()

    data class Table(
        override val type: String = "table",
        val headers: List<String>,
        val rows: List<List<String>>,
        val columnAlignments: List<PrintAlignment> = emptyList(),
        val columnWeights: List<Int> = emptyList(),
        override val alignment: PrintAlignment = PrintAlignment.LEFT,
        override val style: TicketBlockStyle = TicketBlockStyle(),
    ) : TicketBlock()

    data class ListBlock(
        override val type: String = "list",
        val items: List<String>,
        override val alignment: PrintAlignment = PrintAlignment.LEFT,
        override val style: TicketBlockStyle = TicketBlockStyle(),
    ) : TicketBlock()

    data class Image(
        override val type: String = "image",
        val source: String,
        val widthHintPercent: Int? = null,
        override val alignment: PrintAlignment = PrintAlignment.CENTER,
        override val style: TicketBlockStyle = TicketBlockStyle(),
    ) : TicketBlock()

    data class Qr(
        override val type: String = "qr",
        val data: String,
        val size: Int = 8,
        val widthHintPercent: Int? = null,
        override val alignment: PrintAlignment = PrintAlignment.CENTER,
        override val style: TicketBlockStyle = TicketBlockStyle(),
    ) : TicketBlock()
}

@Serializable
data class TicketBlockStyle(
    val bold: Boolean = false,
    val underline: Boolean = false,
    val doubleWidth: Boolean = false,
    val doubleHeight: Boolean = false,
    val inverse: Boolean = false,
)

enum class PrintAlignment {
    LEFT,
    CENTER,
    RIGHT,
}

const val EXTRA_NARROW_PAPER_COLUMNS = 34
const val NARROW_PAPER_COLUMNS = 32
const val WIDE_PAPER_COLUMNS = 48

fun Int.toPaperColumns(): Int = when (this) {
    in Int.MIN_VALUE..57 -> EXTRA_NARROW_PAPER_COLUMNS
    58 -> NARROW_PAPER_COLUMNS
    else -> WIDE_PAPER_COLUMNS
}

fun Int.isNarrowPaperWidth(): Boolean = this <= 58

sealed class PrintCommand {
    data class Text(
        val text: String,
        val alignment: PrintAlignment = PrintAlignment.LEFT,
        val style: TicketBlockStyle = TicketBlockStyle(),
    ) : PrintCommand()

    data class Feed(val lines: Int = 1) : PrintCommand()
    data class Image(
        val source: String,
        val alignment: PrintAlignment = PrintAlignment.CENTER,
        val widthHintPercent: Int? = null,
    ) : PrintCommand()
    data class Qr(
        val data: String,
        val alignment: PrintAlignment = PrintAlignment.CENTER,
        val size: Int = 8,
    ) : PrintCommand()

    data class Cut(val fullCut: Boolean = true) : PrintCommand()
}

data class PrintContext(
    val source: String,
    val orderId: Int? = null,
    val metadata: Map<String, String> = emptyMap(),
)

@Serializable
data class PrintResult(
    val success: Boolean,
    val attempt: Int,
    val attempts: Int,
    @SerialName("connect_port") val connectPort: Int,
    @SerialName("device_id") val deviceId: String,
    val context: PrintResultContext,
    val message: String? = null,
)

@Serializable
data class PrintResultContext(
    val source: String,
    @SerialName("order_id") val orderId: Int? = null,
)

open class PrinterException(message: String, cause: Throwable? = null) : Exception(message, cause)

class UnknownTicketBlockException(blockType: String) :
    PrinterException("Tipo de bloque no soportado: $blockType")

class InvalidTicketBlockException(blockType: String, reason: String) :
    PrinterException("Bloque '$blockType' inválido: $reason")

class PrinterConnectionException(message: String, cause: Throwable? = null) :
    PrinterException(message, cause)

class PrinterSendException(message: String, cause: Throwable? = null) :
    PrinterException(message, cause)

class TicketUnavailableException(message: String = "El ticket no está disponible") :
    PrinterException(message)
