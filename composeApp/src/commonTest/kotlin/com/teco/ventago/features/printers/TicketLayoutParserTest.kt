package com.teco.ventago.features.printers

import com.teco.ventago.features.printers.domain.TicketLayoutParser
import com.teco.ventago.features.printers.domain.model.EXTRA_NARROW_PAPER_COLUMNS
import com.teco.ventago.features.printers.domain.model.NARROW_PAPER_COLUMNS
import com.teco.ventago.features.printers.domain.model.PrintCommand
import com.teco.ventago.features.printers.domain.model.PrinterConfig
import com.teco.ventago.features.printers.domain.model.UnknownTicketBlockException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonObject

class TicketLayoutParserTest {
    private val parser = TicketLayoutParser()
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun parse_acceptsTopLevelBlocksAndAliases() {
        val payload = json.parseToJsonElement(
            """
            {
              "blocks": [
                {"type": "title", "text": "VentaGo"},
                {"type": "divider"},
                {"type": "keyvalue", "key": "Total", "value": "${'$'}10.00"},
                {"type": "qrcode", "data": "ORD-123"}
              ]
            }
            """.trimIndent()
        )

        val layout = parser.parse(payload)

        assertEquals(4, layout.blocks.size)
        assertEquals("text", layout.blocks[0].type)
        assertEquals("separator", layout.blocks[1].type)
        assertEquals("key_value", layout.blocks[2].type)
        assertEquals("qr", layout.blocks[3].type)
    }

    @Test
    fun parse_acceptsNestedTicketLayoutShape() {
        val payload = buildJsonObject {
            putJsonObject("ticket_layout") {
                put("blocks", json.parseToJsonElement("""[{"type":"text","text":"Hola"}]"""))
            }
        }

        val layout = parser.parse(payload)

        assertEquals(1, layout.blocks.size)
        assertEquals("text", layout.blocks.single().type)
    }

    @Test
    fun parse_acceptsBackendInvoiceLayoutShapeUsedByOrderReprint() {
        val payload = json.parseToJsonElement(
            """
            {
              "ticket_layout": {
                "blocks": [
                  {"type":"text","align":"center","emphasis":"bold","text":"Comprobante auxiliar"},
                  {"type":"image","image":{"source":"https://ventago.b-cdn.net/logo.jpg","alt":"Logo","width_hint":"40%"}},
                  {"type":"key_value","values":[
                    {"key":"issuer_name","label":"Razón social","value":"Teco Digi"},
                    {"key":"invoice_total","label":"Total","value":"${'$'}12.31","emphasis":"bold"}
                  ]},
                  {"type":"table","columns":[
                    {"key":"detail","title":"Descripción"},
                    {"key":"total","title":"Total"}
                  ],"rows":[
                    {"values":{"detail":"Agua mineral\\n2.00 x ${'$'}1.5","total":"${'$'}3.00"}},
                    {"values":{"detail":"ALGODON","total":"${'$'}8.50"}}
                  ]},
                  {"type":"qr","qr":{"content":"https://dgi.example/cufe/123","image":"data:image/png;base64,AAECAwQFBgcICQoLDA0ODw=="}},
                  {"type":"cut"}
                ]
              }
            }
            """.trimIndent()
        )

        val layout = parser.parse(payload)
        val commands = parser.toCommands(
            layout = layout,
            printerConfig = PrinterConfig(
                branchCode = "001",
                billingPointCode = "001",
                printerModel = "TM-T20III",
                host = "192.168.0.12",
                paperWidthMm = 57,
                supportsCutter = true,
            )
        )

        assertEquals(7, layout.blocks.size)
        assertEquals(2, layout.blocks.count { it.type == "key_value" })
        assertEquals(1, layout.blocks.count { it.type == "table" })
        assertEquals(1, layout.blocks.count { it.type == "qr" })
        assertEquals(1, layout.blocks.count { it.type == "cut" })
        assertEquals(1, commands.filterIsInstance<PrintCommand.Cut>().size)
        assertEquals(1, commands.filterIsInstance<PrintCommand.Qr>().size)
        assertEquals(1, commands.filterIsInstance<PrintCommand.Image>().size)
        assertEquals(40, commands.filterIsInstance<PrintCommand.Image>().single().widthHintPercent)
        assertTrue(
            commands
                .filterIsInstance<PrintCommand.Text>()
                .any { it.text.contains("Razón social: Teco Digi") }
        )
    }

    @Test
    fun toCommands_appendsAutoCutOnlyWhenLayoutDoesNotContainOne() {
        val printer = PrinterConfig(
            branchCode = "001",
            billingPointCode = "001",
            printerModel = "TM-T20III",
            host = "192.168.0.12",
            paperWidthMm = 58,
            supportsCutter = true,
        )

        val noExplicitCut = parser.parse(
            json.parseToJsonElement("""{"blocks":[{"type":"text","text":"Hola mundo"}]}""")
        )
        val explicitCut = parser.parse(
            json.parseToJsonElement("""{"blocks":[{"type":"text","text":"Hola mundo"},{"type":"cut"}]}""")
        )

        val autoCutCommands = parser.toCommands(noExplicitCut, printer)
        val explicitCutCommands = parser.toCommands(explicitCut, printer)

        assertIs<PrintCommand.Cut>(autoCutCommands.last())
        assertEquals(1, explicitCutCommands.filterIsInstance<PrintCommand.Cut>().size)
    }

    @Test
    fun toCommands_wrapsTextUsingNarrowColumnsFor57MmPaper() {
        val printer = PrinterConfig(
            branchCode = "001",
            billingPointCode = "001",
            printerModel = "TM-T20III",
            host = "192.168.0.12",
            paperWidthMm = 57,
            supportsCutter = false,
        )

        val layout = parser.parse(
            json.parseToJsonElement(
                """
                {
                  "blocks": [
                    {"type":"text","text":"Esta linea de prueba es lo suficientemente larga para validar el ajuste automatico del ancho del papel"},
                    {"type":"separator"}
                  ]
                }
                """.trimIndent()
            )
        )

        val commands = parser.toCommands(layout, printer)
        val textCommands = commands.filterIsInstance<PrintCommand.Text>()

        assertTrue(textCommands.isNotEmpty())
        assertTrue(textCommands.all { it.text.length <= EXTRA_NARROW_PAPER_COLUMNS })
        assertEquals(EXTRA_NARROW_PAPER_COLUMNS, textCommands.last().text.length)
    }

    @Test
    fun toCommands_rendersLabeledDividerInSingleLine() {
        val printer = PrinterConfig(
            branchCode = "001",
            billingPointCode = "001",
            printerModel = "TM-T20III",
            host = "192.168.0.12",
            paperWidthMm = 58,
            supportsCutter = false,
        )

        val layout = parser.parse(
            json.parseToJsonElement(
                """
                {
                  "blocks": [
                    {"type":"divider","label":"Datos del Consumidor"}
                  ]
                }
                """.trimIndent()
            )
        )

        val text = parser.toCommands(layout, printer).filterIsInstance<PrintCommand.Text>().single().text

        assertTrue(text.contains("Datos del Consumidor"))
        assertEquals(NARROW_PAPER_COLUMNS, text.length)
        assertTrue(text.first() == '-')
        assertTrue(text.last() == '-')
    }

    @Test
    fun parse_failsFastOnUnknownBlockType() {
        val payload = json.parseToJsonElement("""{"blocks":[{"type":"mystery"}]}""") as JsonObject

        assertFailsWith<UnknownTicketBlockException> {
            parser.parse(payload)
        }
    }

    @Test
    fun printerConfig_usesPlainTcpTargetAndKeepsSecureFallbackOnlyAsSecondaryCandidate() {
        val printer = PrinterConfig(
            branchCode = "001",
            billingPointCode = "001",
            printerModel = "TM-T20III",
            host = " 192.168.0.12 ",
            port = 443,
        )

        assertEquals("TCP:192.168.0.12", printer.connectionTarget())
        assertEquals(listOf("TCP:192.168.0.12", "TCPS:192.168.0.12"), printer.connectionTargets())
    }

    @Test
    fun printerConfig_usesConfiguredNonDefaultPortFirstWithDefaultTcpFallback() {
        val printer = PrinterConfig(
            branchCode = "001",
            billingPointCode = "001",
            printerModel = "TM-T20III",
            host = "http://192.168.0.12",
            port = 9101,
        )

        assertEquals("TCP:192.168.0.12", printer.connectionTarget())
        assertEquals(listOf("TCP:192.168.0.12:9101", "TCP:192.168.0.12"), printer.connectionTargets())
    }

    @Test
    fun printerConfig_extractsHostAndInlinePortWhenHostIncludesProtocolAndPath() {
        val printer = PrinterConfig(
            branchCode = "001",
            billingPointCode = "001",
            printerModel = "TM-T20III",
            host = "tcp://192.168.0.12:9102/config/index.html",
            port = 443,
        )

        assertEquals("TCP:192.168.0.12", printer.connectionTarget())
        assertEquals(listOf("TCP:192.168.0.12:9102", "TCP:192.168.0.12"), printer.connectionTargets())
    }
}
