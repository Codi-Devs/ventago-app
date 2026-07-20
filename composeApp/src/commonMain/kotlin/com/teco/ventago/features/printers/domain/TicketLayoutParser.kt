package com.teco.ventago.features.printers.domain

import com.teco.ventago.features.printers.domain.model.InvalidTicketBlockException
import com.teco.ventago.features.printers.domain.model.PrintAlignment
import com.teco.ventago.features.printers.domain.model.PrintCommand
import com.teco.ventago.features.printers.domain.model.PrinterConfig
import com.teco.ventago.features.printers.domain.model.TicketBlock
import com.teco.ventago.features.printers.domain.model.TicketBlockStyle
import com.teco.ventago.features.printers.domain.model.TicketDocumentPayload
import com.teco.ventago.features.printers.domain.model.TicketLayout
import com.teco.ventago.features.printers.domain.model.UnknownTicketBlockException
import com.teco.ventago.features.printers.domain.model.toTicketPaperProfile
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class TicketLayoutParser {
    fun parse(payload: JsonElement): TicketLayout {
        val root = when (payload) {
            is JsonObject -> payload
            else -> throw InvalidTicketBlockException("ticket_layout", "Se esperaba un objeto JSON")
        }
        val blocks = when {
            root["blocks"] is JsonArray -> root["blocks"]!!.jsonArray
            root["ticket_layout"] is JsonObject -> {
                root["ticket_layout"]!!.jsonObject["blocks"]?.jsonArray
                    ?: throw InvalidTicketBlockException("ticket_layout", "Falta la lista de bloques")
            }
            root["ticketLayout"] is JsonObject -> {
                root["ticketLayout"]!!.jsonObject["blocks"]?.jsonArray
                    ?: throw InvalidTicketBlockException("ticket_layout", "Falta la lista de bloques")
            }
            else -> throw InvalidTicketBlockException("ticket_layout", "No se encontraron bloques")
        }

        return TicketLayout(blocks = blocks.flatMapIndexed { index, element ->
            parseBlocks(index, element.jsonObject)
        })
    }

    fun parse(payload: TicketDocumentPayload): TicketLayout {
        val element = payload.resolvedLayoutElement()
            ?: throw InvalidTicketBlockException("ticket_layout", "No se encontró el layout del ticket")
        return parse(element)
    }

    fun toCommands(layout: TicketLayout, printerConfig: PrinterConfig): List<PrintCommand> {
        val paperProfile = printerConfig.paperWidthMm.toTicketPaperProfile()
        val width = paperProfile.lineChars
        val commands = mutableListOf<PrintCommand>()
        var explicitCut = false

        layout.blocks.forEach { block ->
            when (block) {
                is TicketBlock.Text -> {
                    wrapText(block.text, width).forEach { line ->
                        commands += PrintCommand.Text(
                            text = padAlignedText(line, width, block.alignment),
                            alignment = PrintAlignment.LEFT,
                            style = block.style
                        )
                    }
                }

                is TicketBlock.Separator -> {
                    val separatorLine = if (block.label.isNullOrBlank()) {
                        block.character.toString().repeat(width)
                    } else {
                        buildLabeledSeparator(
                            label = block.label,
                            width = width,
                            character = block.character
                        )
                    }
                    commands += PrintCommand.Text(
                        text = separatorLine,
                        alignment = PrintAlignment.LEFT,
                        style = if (block.label.isNullOrBlank()) block.style else block.style.copy(bold = true)
                    )
                }

                is TicketBlock.Feed -> {
                    commands += PrintCommand.Feed(block.lines.coerceAtLeast(1))
                }

                is TicketBlock.Cut -> {
                    explicitCut = true
                    commands += PrintCommand.Cut(block.fullCut)
                }

                is TicketBlock.KeyValue -> {
                    val key = block.key.trim().authorizationDisplayLabel()
                    val value = block.value.trim()
                    if (key.isBlank() && value.isBlank()) return@forEach

                    if (block.key.isAuthorizationKey() && value.isNotBlank()) {
                        wrapFixedWidthText("$key: $value", width).forEach { line ->
                            commands += PrintCommand.Text(
                                text = padAlignedText(line, width, PrintAlignment.LEFT),
                                alignment = PrintAlignment.LEFT,
                                style = block.style.copy(
                                    bold = false,
                                    underline = false,
                                    doubleWidth = false,
                                    doubleHeight = false,
                                    inverse = false
                                )
                            )
                        }
                    } else if (block.alignValueRight && key.isNotBlank() && value.isNotBlank()) {
                        val valueWidth = (width * 0.45f).toInt().coerceIn(8, (width - 9).coerceAtLeast(8))
                        val keyWidth = (width - valueWidth - 1).coerceAtLeast(8)
                        val keyLines = wrapText(key, keyWidth)
                        val valueLines = wrapText(value, valueWidth)
                        val maxLines = maxOf(keyLines.size, valueLines.size)
                        repeat(maxLines) { row ->
                            val left = padAlignedText(keyLines.getOrElse(row) { "" }, keyWidth, PrintAlignment.LEFT)
                            val right = padAlignedText(valueLines.getOrElse(row) { "" }, valueWidth, PrintAlignment.RIGHT)
                            commands += PrintCommand.Text(
                                text = "$left $right",
                                alignment = PrintAlignment.LEFT,
                                style = block.style
                            )
                        }
                    } else if (key.isNotBlank() && value.isNotBlank()) {
                        val prefix = "$key: "
                        if (prefix.length >= width) {
                            wrapText("$key: $value", width).forEach { line ->
                                commands += PrintCommand.Text(
                                    text = padAlignedText(line, width, PrintAlignment.LEFT),
                                    alignment = PrintAlignment.LEFT,
                                    style = block.style
                                )
                            }
                        } else {
                            val valueWidth = (width - prefix.length).coerceAtLeast(1)
                            val valueLines = wrapText(value, valueWidth).ifEmpty { listOf("") }
                            commands += PrintCommand.Text(
                                text = padAlignedText(prefix + valueLines.first(), width, PrintAlignment.LEFT),
                                alignment = PrintAlignment.LEFT,
                                style = block.style
                            )
                            val continuationIndent = " ".repeat(prefix.length)
                            valueLines.drop(1).forEach { valueLine ->
                                commands += PrintCommand.Text(
                                    text = padAlignedText(continuationIndent + valueLine, width, PrintAlignment.LEFT),
                                    alignment = PrintAlignment.LEFT,
                                    style = block.style
                                )
                            }
                        }
                    } else {
                        val fallback = if (key.isNotBlank()) key else value
                        wrapText(fallback, width).forEach { line ->
                            commands += PrintCommand.Text(
                                text = padAlignedText(line, width, PrintAlignment.LEFT),
                                alignment = PrintAlignment.LEFT,
                                style = block.style
                            )
                        }
                    }
                }

                is TicketBlock.Table -> {
                    val columnCount = maxOf(1, block.headers.size.takeIf { it > 0 } ?: block.rows.firstOrNull()?.size ?: 1)
                    val widths = buildColumnWidths(
                        totalWidth = width,
                        columns = columnCount,
                        weights = block.columnWeights
                    )
                    val alignments = List(columnCount) { index ->
                        block.columnAlignments.getOrElse(index) { PrintAlignment.LEFT }
                    }

                    if (block.headers.isNotEmpty()) {
                        commands += PrintCommand.Text(
                            text = formatColumns(block.headers, widths, alignments),
                            alignment = PrintAlignment.LEFT,
                            style = block.style.copy(bold = true)
                        )
                    }

                    if (block.headers.isNotEmpty()) {
                        commands += PrintCommand.Text(
                            text = "-".repeat(width),
                            alignment = PrintAlignment.LEFT,
                            style = block.style
                        )
                    }

                    block.rows.forEach { row ->
                        val cellLines = row.mapIndexed { index, value ->
                            val cellWidth = widths.getOrElse(index) { 1 }
                            wrapText(value, cellWidth).ifEmpty { listOf("") }
                                .map { line -> padAlignedText(line, cellWidth, alignments.getOrElse(index) { PrintAlignment.LEFT }) }
                        }
                        val lineCount = cellLines.maxOfOrNull { it.size } ?: 1
                        repeat(lineCount) { lineIndex ->
                            val parts = widths.mapIndexed { index, cellWidth ->
                                cellLines.getOrNull(index)?.getOrNull(lineIndex) ?: " ".repeat(cellWidth)
                            }
                            commands += PrintCommand.Text(
                                text = parts.joinToString(" "),
                                alignment = PrintAlignment.LEFT,
                                style = block.style
                            )
                        }
                    }
                }

                is TicketBlock.ListBlock -> {
                    block.items.forEach { item ->
                        wrapText("- $item", width).forEach { line ->
                            commands += PrintCommand.Text(
                                text = padAlignedText(line, width, PrintAlignment.LEFT),
                                alignment = PrintAlignment.LEFT,
                                style = block.style
                            )
                        }
                    }
                }

                is TicketBlock.Image -> {
                    commands += PrintCommand.Image(
                        source = block.source,
                        alignment = block.alignment,
                        widthHintPercent = block.widthHintPercent
                    )
                }

                is TicketBlock.Qr -> {
                    val qrPrintLayout = resolveQrPrintLayout(
                        block = block,
                        paperWidthMm = printerConfig.paperWidthMm,
                        lineChars = width,
                        paperWidthDots = paperProfile.canvasWidthDots
                    )
                    commands += PrintCommand.Qr(
                        data = block.data,
                        alignment = PrintAlignment.LEFT,
                        size = qrPrintLayout.moduleWidth,
                        xPositionDots = qrPrintLayout.xPositionDots,
                        errorCorrection = block.errorCorrection
                    )
                }
            }
        }

        if (!explicitCut && printerConfig.supportsCutter) {
            commands += PrintCommand.Cut(fullCut = true)
        }

        return commands
    }

    private fun parseBlocks(index: Int, raw: JsonObject): List<TicketBlock> {
        val rawType = raw.string("type")
            ?: raw.string("block_type")
            ?: throw InvalidTicketBlockException("unknown", "El bloque $index no incluye type")

        val type = normalizeBlockType(rawType)
        val alignment = (raw.string("alignment") ?: raw.string("align"))?.toAlignment() ?: PrintAlignment.LEFT
        val emphasisBold = raw.string("emphasis").isBoldEmphasis()
        val style = TicketBlockStyle(
            bold = raw.boolean("bold") || raw.boolean("emphasis") || emphasisBold,
            underline = raw.boolean("underline"),
            doubleWidth = raw.boolean("double_width"),
            doubleHeight = raw.boolean("double_height"),
            inverse = raw.boolean("inverse")
        )

        return when (type) {
            "text" -> listOf(
                TicketBlock.Text(
                    text = raw.string("text")
                        ?: raw.string("content")
                        ?: raw.string("value")
                        ?: throw InvalidTicketBlockException(type, "Falta el texto"),
                    alignment = alignment,
                    style = style.copy(
                        bold = style.bold || raw.boolean("title")
                    )
                )
            )

            "separator" -> {
                listOf(
                    TicketBlock.Separator(
                    character = raw.string("character")?.firstOrNull()
                        ?: raw.string("char")?.firstOrNull()
                        ?: '-',
                    label = raw.string("label"),
                    alignment = alignment,
                    style = style
                )
                )
            }

            "feed" -> listOf(
                TicketBlock.Feed(
                    lines = raw.int("lines") ?: raw.int("count") ?: raw.int("size") ?: 1,
                    alignment = alignment,
                    style = style
                )
            )

            "cut" -> listOf(
                TicketBlock.Cut(
                    fullCut = (raw.string("mode") ?: "full") != "partial",
                    alignment = alignment,
                    style = style
                )
            )

            "key_value" -> {
                val entries = raw.keyValueEntries()
                val section = raw.string("section")?.trim()?.lowercase()
                val blockAlignRight = raw.boolean("align_value_right") || raw.boolean("alignValueRight")
                if (entries.isNotEmpty()) {
                    entries.map { entry ->
                        TicketBlock.KeyValue(
                            key = entry.key,
                            value = entry.value,
                            alignValueRight = entry.alignValueRight ?: blockAlignRight || section == "totals",
                            alignment = alignment,
                            style = style.copy(bold = style.bold || entry.bold)
                        )
                    }
                } else {
                    listOf(
                        TicketBlock.KeyValue(
                            key = raw.string("key")
                                ?: raw.string("label")
                                ?: throw InvalidTicketBlockException(type, "Falta la llave"),
                            value = raw.string("value")
                                ?: throw InvalidTicketBlockException(type, "Falta el valor"),
                            alignValueRight = blockAlignRight || section == "totals",
                            alignment = alignment,
                            style = style
                        )
                    )
                }
            }

            "table" -> {
                val columns = raw.tableColumns()
                val columnKeys = columns.mapNotNull { it.key }
                val headers = raw.stringList("headers").ifEmpty {
                    columns.mapNotNull { it.title?.takeIf { title -> title.isNotBlank() } }
                }
                val rows = raw.tableRows(columnKeys)
                if (headers.isEmpty() && rows.isEmpty()) {
                    throw InvalidTicketBlockException(type, "La tabla no contiene encabezados ni filas")
                }
                listOf(
                    TicketBlock.Table(
                        headers = headers,
                        rows = rows,
                        columnAlignments = columns.map { it.alignment },
                        columnWeights = columns.map { it.weight ?: 0 },
                        alignment = alignment,
                        style = style
                    )
                )
            }

            "list" -> {
                val items = raw.stringList("items")
                if (items.isEmpty()) {
                    throw InvalidTicketBlockException(type, "La lista no contiene items")
                }
                listOf(
                    TicketBlock.ListBlock(
                        items = items,
                        alignment = alignment,
                        style = style
                    )
                )
            }

            "image" -> {
                val source = raw.imageSource()
                val imageObject = raw.obj("image")
                val widthHintPercent = raw.percentHint("width_hint")
                    ?: raw.percentHint("widthHint")
                    ?: imageObject?.percentHint("width_hint")
                    ?: imageObject?.percentHint("widthHint")
                if (source == null) {
                    emptyList()
                } else {
                    listOf(
                        TicketBlock.Image(
                            source = source,
                            widthHintPercent = widthHintPercent,
                            alignment = alignment,
                            style = style
                        )
                    )
                }
            }

            "qr" -> {
                val qrObject = raw.obj("qr")
                val qrData = raw.string("data")
                    ?: raw.string("content")
                    ?: raw.string("text")
                    ?: qrObject?.string("data")
                    ?: qrObject?.string("content")
                    ?: qrObject?.string("text")
                    ?: throw InvalidTicketBlockException(type, "Faltan los datos del código QR")
                val qrSize = (raw.int("size")
                    ?: raw.int("module_size")
                    ?: raw.int("size_hint")
                    ?: qrObject?.int("size")
                    ?: qrObject?.int("module_size")
                    ?: qrObject?.int("size_hint")
                    ?: 8).coerceIn(1, 16)
                val qrWidthHint = raw.percentHint("width_hint")
                    ?: raw.percentHint("widthHint")
                    ?: qrObject?.percentHint("width_hint")
                    ?: qrObject?.percentHint("widthHint")
                val qrVersion = raw.int("version")
                    ?: raw.int("qr_version")
                    ?: raw.int("qrVersion")
                    ?: qrObject?.int("version")
                    ?: qrObject?.int("qr_version")
                    ?: qrObject?.int("qrVersion")
                val errorCorrection = raw.string("error_correction")
                    ?: raw.string("errorCorrection")
                    ?: raw.string("level")
                    ?: qrObject?.string("error_correction")
                    ?: qrObject?.string("errorCorrection")
                    ?: qrObject?.string("level")
                    ?: "M"

                listOf(
                    TicketBlock.Qr(
                        data = qrData,
                        size = qrSize,
                        widthHintPercent = qrWidthHint,
                        version = qrVersion?.coerceIn(1, QR_MAX_VERSION),
                        errorCorrection = errorCorrection.normalizeQrErrorCorrection(),
                        alignment = alignment,
                        style = style
                    )
                )
            }

            else -> throw UnknownTicketBlockException(rawType)
        }
    }

    private fun normalizeBlockType(value: String): String {
        return when (value.trim().lowercase()) {
            "text", "title", "line_text" -> "text"
            "separator", "divider", "hr", "line" -> "separator"
            "feed", "newline", "space" -> "feed"
            "key_value", "key-value", "keyvalue" -> "key_value"
            "list" -> "list"
            "table" -> "table"
            "image" -> "image"
            "qr", "qrcode" -> "qr"
            "cut" -> "cut"
            else -> value.trim().lowercase()
        }
    }

    private fun String.toAlignment(): PrintAlignment {
        return toPrintAlignment()
    }

    private fun wrapText(text: String, width: Int): List<String> {
        val normalized = text.replace("\r\n", "\n").replace('\r', '\n')
        return normalized.split('\n').flatMap { line ->
            val content = line.trimEnd()
            if (content.length <= width) {
                listOf(content)
            } else {
                buildList {
                    var remaining = content
                    while (remaining.isNotEmpty()) {
                        if (remaining.length <= width) {
                            add(remaining)
                            remaining = ""
                        } else {
                            val breakpoint = remaining.take(width + 1).lastIndexOf(' ').takeIf { it > 0 } ?: width
                            add(remaining.substring(0, breakpoint).trimEnd())
                            remaining = remaining.substring(breakpoint).trimStart()
                        }
                    }
                }
            }
        }
    }

    private fun wrapFixedWidthText(text: String, width: Int): List<String> {
        val safeWidth = width.coerceAtLeast(1)
        val normalized = text.replace("\r\n", "\n").replace('\r', '\n')
        return normalized.split('\n').flatMap { line ->
            if (line.isEmpty()) {
                listOf("")
            } else {
                line.chunked(safeWidth)
            }
        }
    }

    private fun buildColumnWidths(totalWidth: Int, columns: Int, weights: List<Int>): List<Int> {
        val safeColumns = columns.coerceAtLeast(1)
        val separators = safeColumns - 1
        val available = (totalWidth - separators).coerceAtLeast(safeColumns)

        val normalizedWeights = List(safeColumns) { index ->
            val explicit = weights.getOrNull(index)
            if (explicit != null && explicit > 0) {
                explicit
            } else if (index == 0 && safeColumns == 2) {
                3
            } else {
                1
            }
        }
        val totalWeight = normalizedWeights.sum().coerceAtLeast(1)

        val widths = MutableList(safeColumns) { 0 }
        var consumed = 0
        normalizedWeights.forEachIndexed { index, weight ->
            if (index == safeColumns - 1) {
                widths[index] = (available - consumed).coerceAtLeast(1)
            } else {
                val width = ((available * weight) / totalWeight).coerceAtLeast(1)
                widths[index] = width
                consumed += width
            }
        }
        return widths
    }

    private fun formatColumns(values: List<String>, widths: List<Int>, alignments: List<PrintAlignment>): String {
        return widths.mapIndexed { index, cellWidth ->
            val value = values.getOrElse(index) { "" }
            val alignment = alignments.getOrElse(index) { PrintAlignment.LEFT }
            padAlignedText(value.trim(), cellWidth, alignment)
        }.joinToString(" ")
    }

    private fun padAlignedText(value: String, width: Int, alignment: PrintAlignment): String {
        val safeWidth = width.coerceAtLeast(1)
        val content = value.take(safeWidth)
        val spaces = safeWidth - content.length
        return when (alignment) {
            PrintAlignment.RIGHT -> " ".repeat(spaces) + content
            PrintAlignment.CENTER -> {
                val left = spaces / 2
                val right = spaces - left
                " ".repeat(left) + content + " ".repeat(right)
            }
            PrintAlignment.LEFT -> content + " ".repeat(spaces)
        }
    }

    private fun buildLabeledSeparator(label: String, width: Int, character: Char): String {
        val safeWidth = width.coerceAtLeast(1)
        val normalizedLabel = " ${label.trim()} "
        if (normalizedLabel.length >= safeWidth) {
            return normalizedLabel.take(safeWidth)
        }

        val remaining = safeWidth - normalizedLabel.length
        val left = remaining / 2
        val right = remaining - left
        val separator = character.toString()
        return separator.repeat(left) + normalizedLabel + separator.repeat(right)
    }

    private fun resolveQrPrintLayout(
        block: TicketBlock.Qr,
        paperWidthMm: Int,
        lineChars: Int,
        paperWidthDots: Int,
    ): QrPrintLayout {
        val qrVersion = block.version ?: estimateQrVersion(block.data, block.errorCorrection)
        val moduleCount = QR_BASE_MODULE_COUNT + (qrVersion * QR_MODULES_PER_VERSION)

        return if (paperWidthMm == 57) {
            val moduleWidth = ((paperWidthDots - QR_57_MM_MARGIN_DOTS) / moduleCount)
                .coerceIn(QR_MIN_MODULE_WIDTH, QR_57_MM_MAX_MODULE_WIDTH)
            val xPosition = ((paperWidthDots - (moduleCount * moduleWidth)) / 2) + QR_57_MM_CENTER_OFFSET_DOTS
            QrPrintLayout(moduleWidth = moduleWidth, xPositionDots = xPosition.coerceAtLeast(0))
        } else {
            val maxModuleWidth = if (lineChars <= EXTRA_NARROW_LINE_CHARS) 8 else 12
            val moduleWidth = (maxModuleWidth * QR_NON_57_WIDTH_RATIO)
                .toIntWithRound()
                .coerceIn(QR_MIN_MODULE_WIDTH, maxModuleWidth)
            val xPosition = (paperWidthDots - (moduleCount * moduleWidth)) / 2
            QrPrintLayout(moduleWidth = moduleWidth, xPositionDots = xPosition.coerceAtLeast(0))
        }
    }

}

private data class QrPrintLayout(
    val moduleWidth: Int,
    val xPositionDots: Int,
)

private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull

private fun JsonObject.boolean(key: String): Boolean = (this[key] as? JsonPrimitive)?.booleanOrNull ?: false

private fun JsonObject.stringList(key: String): List<String> {
    val value = this[key] ?: return emptyList()
    return when (value) {
        is JsonArray -> value.mapNotNull { element ->
            when (element) {
                is JsonPrimitive -> element.contentOrNull
                is JsonObject -> element.string("text") ?: element.string("value") ?: element.string("label")
                else -> null
            }?.takeIf { it.isNotBlank() }
        }
        else -> emptyList()
    }
}

private fun JsonObject.tableRows(columnKeys: List<String> = emptyList()): List<List<String>> {
    val rows = this["rows"] ?: this["items"] ?: return emptyList()
    return when (rows) {
        is JsonArray -> rows.mapNotNull { row ->
            when (row) {
                is JsonArray -> row.map { it.jsonPrimitive.contentOrNull.orEmpty() }
                is JsonObject -> {
                    row.valuesObjectRow(columnKeys)
                        ?: row["columns"]?.jsonArray?.map { it.jsonPrimitive.contentOrNull.orEmpty() }
                        ?: listOfNotNull(row.string("label"), row.string("value")).takeIf { it.isNotEmpty() }
                }
                else -> null
            }
        }
        else -> emptyList()
    }
}

private data class ParsedKeyValueEntry(
    val key: String,
    val value: String,
    val bold: Boolean,
    val alignValueRight: Boolean? = null,
)

private data class ParsedTableColumn(
    val key: String?,
    val title: String?,
    val alignment: PrintAlignment = PrintAlignment.LEFT,
    val weight: Int? = null,
)

private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

private fun JsonObject.keyValueEntries(): List<ParsedKeyValueEntry> {
    val values = this["values"] as? JsonArray ?: return emptyList()
    return values.mapNotNull { item ->
        val valueObject = item as? JsonObject ?: return@mapNotNull null
        val key = valueObject.string("label") ?: valueObject.string("key") ?: return@mapNotNull null
        val value = valueObject.string("value") ?: ""
        val bold = valueObject.boolean("bold") || valueObject.string("emphasis").isBoldEmphasis()
        val alignValueRight = if (valueObject.hasBooleanKey("align_value_right") || valueObject.hasBooleanKey("alignValueRight")) {
            valueObject.boolean("align_value_right") || valueObject.boolean("alignValueRight")
        } else {
            null
        }
        ParsedKeyValueEntry(
            key = key,
            value = value,
            bold = bold,
            alignValueRight = alignValueRight,
        )
    }
}

private fun JsonObject.tableColumns(): List<ParsedTableColumn> {
    val columns = this["columns"] as? JsonArray ?: return emptyList()
    return columns.mapNotNull { item ->
        val columnObject = item as? JsonObject ?: return@mapNotNull null
        val key = columnObject.string("key")
        val title = columnObject.string("title") ?: columnObject.string("label") ?: key
        if (key == null && title == null) return@mapNotNull null
        val alignment = (columnObject.string("align") ?: columnObject.string("alignment"))
            ?.toPrintAlignment()
            ?: PrintAlignment.LEFT
        val weight = columnObject.int("weight")
        ParsedTableColumn(
            key = key,
            title = title,
            alignment = alignment,
            weight = weight
        )
    }
}

private fun JsonObject.valuesObjectRow(columnKeys: List<String>): List<String>? {
    val valuesObject = this["values"] as? JsonObject ?: return null
    if (columnKeys.isNotEmpty()) {
        val mapped = columnKeys.map { key ->
            (valuesObject[key] as? JsonPrimitive)?.contentOrNull.orEmpty()
        }
        if (mapped.any { it.isNotBlank() }) {
            return mapped
        }
    }

    val fallback = valuesObject.values.mapNotNull { value ->
        (value as? JsonPrimitive)?.contentOrNull
    }
    return fallback.takeIf { it.isNotEmpty() }
}

private fun JsonObject.imageSource(): String? {
    val direct = sequenceOf(
        string("base64"),
        string("data"),
        string("src"),
        string("source"),
    ).firstOrNull { it != null } ?: ""
    if (direct.isNotBlank()) {
        return direct
    }

    val imageObject = obj("image")
    val nested = sequenceOf(
        imageObject?.string("base64"),
        imageObject?.string("data"),
        imageObject?.string("image"),
        imageObject?.string("src"),
        imageObject?.string("source"),
    ).firstOrNull { it != null } ?: return null

    return nested
}

private fun String?.isBoldEmphasis(): Boolean {
    if (this.isNullOrBlank()) return false
    return trim().lowercase() in setOf("bold", "strong", "true", "1")
}

private fun JsonObject.hasBooleanKey(key: String): Boolean = this[key] is JsonPrimitive

private fun JsonObject.percentHint(key: String): Int? {
    val raw = string(key) ?: return null
    val normalized = raw.trim().removeSuffix("%").trim()
    val numeric = normalized.toIntOrNull() ?: return null
    return numeric.coerceIn(1, 100)
}

private fun String.toPrintAlignment(): PrintAlignment = when (trim().lowercase()) {
    "center", "centre", "centro" -> PrintAlignment.CENTER
    "right", "end", "derecha" -> PrintAlignment.RIGHT
    else -> PrintAlignment.LEFT
}

private fun String.isAuthorizationKey(): Boolean {
    val normalized = normalizeAuthorizationKey()
    return normalized == "autorizacion" || normalized == "authorization"
}

private fun String.authorizationDisplayLabel(): String {
    return if (isAuthorizationKey()) "Autorización" else this
}

private fun String.normalizeAuthorizationKey(): String {
    return trim()
        .lowercase()
        .removeSuffix(":")
        .replace("á", "a")
        .replace("é", "e")
        .replace("í", "i")
        .replace("ó", "o")
        .replace("ú", "u")
        .replace("_", " ")
        .replace("-", " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun String.normalizeQrErrorCorrection(): String {
    return when (trim().uppercase()) {
        "L", "M", "Q", "H" -> trim().uppercase()
        else -> "M"
    }
}

private fun estimateQrVersion(data: String, errorCorrection: String): Int {
    val byteCount = data.encodeToByteArray().size
    val capacities = when (errorCorrection.normalizeQrErrorCorrection()) {
        "L" -> QR_BYTE_CAPACITY_L
        "Q" -> QR_BYTE_CAPACITY_Q
        "H" -> QR_BYTE_CAPACITY_H
        else -> QR_BYTE_CAPACITY_M
    }
    return capacities.indexOfFirst { byteCount <= it }
        .takeIf { it >= 0 }
        ?.plus(1)
        ?: QR_MAX_VERSION
}

private fun Double.toIntWithRound(): Int = kotlin.math.round(this).toInt()

private const val QR_BASE_MODULE_COUNT = 17
private const val QR_MODULES_PER_VERSION = 4
private const val QR_MAX_VERSION = 40
private const val QR_MIN_MODULE_WIDTH = 3
private const val QR_57_MM_MARGIN_DOTS = 8
private const val QR_57_MM_CENTER_OFFSET_DOTS = 20
private const val QR_57_MM_MAX_MODULE_WIDTH = 16
private const val QR_NON_57_WIDTH_RATIO = 0.65
private const val EXTRA_NARROW_LINE_CHARS = 34

private val QR_BYTE_CAPACITY_L = intArrayOf(
    17, 32, 53, 78, 106, 134, 154, 192, 230, 271,
    321, 367, 425, 458, 520, 586, 644, 718, 792, 858,
    929, 1003, 1091, 1171, 1273, 1367, 1465, 1528, 1628, 1732,
    1840, 1952, 2068, 2188, 2303, 2431, 2563, 2699, 2809, 2953
)
private val QR_BYTE_CAPACITY_M = intArrayOf(
    14, 26, 42, 62, 84, 106, 122, 152, 180, 213,
    251, 287, 331, 362, 412, 450, 504, 560, 624, 666,
    711, 779, 857, 911, 997, 1059, 1125, 1190, 1264, 1370,
    1452, 1538, 1628, 1722, 1809, 1911, 1989, 2099, 2213, 2331
)
private val QR_BYTE_CAPACITY_Q = intArrayOf(
    11, 20, 32, 46, 60, 74, 86, 108, 130, 151,
    177, 203, 241, 258, 292, 322, 364, 394, 442, 482,
    509, 565, 611, 661, 715, 751, 805, 868, 908, 982,
    1030, 1112, 1168, 1228, 1283, 1351, 1423, 1499, 1579, 1663
)
private val QR_BYTE_CAPACITY_H = intArrayOf(
    7, 14, 24, 34, 44, 58, 64, 84, 98, 119,
    137, 155, 177, 194, 220, 250, 280, 310, 338, 382,
    403, 439, 461, 511, 535, 593, 625, 658, 698, 742,
    790, 842, 898, 958, 983, 1051, 1093, 1139, 1219, 1273
)
