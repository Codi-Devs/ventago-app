package com.teco.ventago.features.expenses.ui.details

import com.teco.ventago.features.expenses.domain.models.Expense
import com.teco.ventago.features.expenses.domain.models.PaymentMethod
import com.teco.ventago.utils.formatNumberToMoney

/**
 * Generates a simple non-fiscal PDF receipt for an expense.
 * Uses raw PDF 1.4 format (no external library needed).
 */
object ExpensePdfGenerator {

    fun generate(expense: Expense): ByteArray {
        val lines = buildContent(expense)
        return buildPdf(lines)
    }

    private fun buildContent(expense: Expense): List<PdfLine> {
        val lines = mutableListOf<PdfLine>()

        // Title
        lines += PdfLine("Comprobante de Gasto", fontSize = 18, bold = true)
        lines += PdfLine("")
        lines += PdfLine("Documento informativo - NO ES FACTURA FISCAL", fontSize = 12, bold = true)
        lines += PdfLine("")
        lines += PdfLine("=" .repeat(60), fontSize = 10)
        lines += PdfLine("")

        // Metadata
        lines += PdfLine("Factura: ${expense.invoiceNumber ?: "Sin numero"}", fontSize = 11)
        expense.cufe?.let {
            if (it.isNotBlank()) lines += PdfLine("CUFE: $it", fontSize = 9)
        }
        val emissionDate = expense.emissionDate?.take(10) ?: "-"
        lines += PdfLine("Fecha de emision: $emissionDate", fontSize = 11)
        expense.paymentMethod?.let {
            lines += PdfLine("Metodo de pago: ${paymentMethodLabel(it)}", fontSize = 11)
        }
        val sourceLabel = if (expense.source == "manual") "Manual" else "Importado"
        lines += PdfLine("Fuente: $sourceLabel", fontSize = 11)
        lines += PdfLine("")

        // Issuer
        lines += PdfLine("-" .repeat(60), fontSize = 10)
        lines += PdfLine("EMISOR", fontSize = 12, bold = true)
        expense.issuer?.let { party ->
            lines += PdfLine("Nombre: ${party.name ?: "-"}", fontSize = 11)
            val ruc = buildString {
                append(party.ruc ?: "-")
                party.dv?.let { append("-$it") }
            }
            lines += PdfLine("RUC: $ruc", fontSize = 11)
        }
        lines += PdfLine("")

        // Receiver
        lines += PdfLine("RECEPTOR", fontSize = 12, bold = true)
        expense.receiver?.let { party ->
            lines += PdfLine("Nombre: ${party.name ?: "-"}", fontSize = 11)
            val ruc = buildString {
                append(party.ruc ?: "-")
                party.dv?.let { append("-$it") }
            }
            lines += PdfLine("RUC: $ruc", fontSize = 11)
        }
        lines += PdfLine("")

        // Items
        if (!expense.items.isNullOrEmpty()) {
            lines += PdfLine("-" .repeat(60), fontSize = 10)
            lines += PdfLine("ARTICULOS", fontSize = 12, bold = true)
            lines += PdfLine("")
            expense.items.forEachIndexed { index, item ->
                lines += PdfLine("${index + 1}. ${item.description ?: "Sin descripcion"}", fontSize = 11, bold = true)
                lines += PdfLine("   Cant: ${item.quantity ?: 0} x ${formatNumberToMoney("${item.unitPrice ?: 0.0}")}", fontSize = 10)
                if ((item.discountAmount ?: 0.0) > 0.0) {
                    lines += PdfLine("   Descuento: ${formatNumberToMoney("${item.discountAmount}")}", fontSize = 10)
                }
                if ((item.itbmsAmount ?: 0.0) > 0.0) {
                    lines += PdfLine("   ITBMS: ${formatNumberToMoney("${item.itbmsAmount}")}", fontSize = 10)
                }
                lines += PdfLine("   Total: ${formatNumberToMoney("${item.total ?: 0.0}")}", fontSize = 10)
                lines += PdfLine("")
            }
        }

        // Totals
        lines += PdfLine("-" .repeat(60), fontSize = 10)
        lines += PdfLine("TOTALES", fontSize = 12, bold = true)
        lines += PdfLine("Subtotal: ${formatNumberToMoney("${expense.subtotal ?: 0.0}")}", fontSize = 11)
        lines += PdfLine("ITBMS: ${formatNumberToMoney("${expense.itbmsTotal ?: 0.0}")}", fontSize = 11)
        lines += PdfLine("Total: ${formatNumberToMoney("${expense.totalAmount ?: 0.0}")}", fontSize = 12, bold = true)
        lines += PdfLine("")

        // Payments
        if (!expense.payments.isNullOrEmpty()) {
            lines += PdfLine("-" .repeat(60), fontSize = 10)
            lines += PdfLine("PAGOS", fontSize = 12, bold = true)
            lines += PdfLine("")
            expense.payments.forEach { payment ->
                val method = paymentMethodLabel(payment.paymentMethod)
                val amount = formatNumberToMoney("${payment.amountPaid ?: 0.0}")
                val status = when (payment.paymentStatus) {
                    "paid" -> "Pagado"
                    "pending" -> "Pendiente"
                    else -> payment.paymentStatus ?: ""
                }
                lines += PdfLine("$method - $amount ($status)", fontSize = 11)
                payment.reference?.let {
                    if (it.isNotBlank()) lines += PdfLine("  Ref: $it", fontSize = 10)
                }
                payment.paymentDate?.let {
                    lines += PdfLine("  Fecha: ${it.take(10)}", fontSize = 10)
                }
                payment.dueDate?.let {
                    lines += PdfLine("  Vence: ${it.take(10)}", fontSize = 10)
                }
                lines += PdfLine("")
            }

            val totalPaid = expense.paymentSummary?.totalPaid ?: expense.totalPaid ?: 0.0
            val remaining = (expense.totalAmount ?: 0.0) - totalPaid
            lines += PdfLine("Total pagado: ${formatNumberToMoney("$totalPaid")}", fontSize = 11)
            lines += PdfLine("Pendiente: ${formatNumberToMoney("$remaining")}", fontSize = 11)
        }

        // Notes
        if (!expense.notes.isNullOrBlank()) {
            lines += PdfLine("")
            lines += PdfLine("-" .repeat(60), fontSize = 10)
            lines += PdfLine("NOTAS", fontSize = 12, bold = true)
            lines += PdfLine(expense.notes, fontSize = 11)
        }

        lines += PdfLine("")
        lines += PdfLine("=" .repeat(60), fontSize = 10)
        lines += PdfLine("Este documento es informativo y NO ES FACTURA FISCAL", fontSize = 10, bold = true)

        return lines
    }

    private fun paymentMethodLabel(method: String?): String = PaymentMethod.getLabel(method)

    private data class PdfLine(
        val text: String,
        val fontSize: Int = 11,
        val bold: Boolean = false
    )

    /**
     * Build a minimal PDF 1.4 document using Helvetica (built-in font).
     * Only ASCII text supported (accented chars replaced).
     */
    private fun buildPdf(lines: List<PdfLine>): ByteArray {
        val objects = mutableListOf<String>()
        val offsets = mutableListOf<Int>()

        // We'll build the entire PDF as a string and convert to bytes
        val sb = StringBuilder()

        // Header
        sb.append("%PDF-1.4\n")

        // Object 1: Catalog
        offsets += sb.length
        objects += "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n"
        sb.append(objects.last())

        // Object 2: Pages (we'll fill Kids later)
        val pagesObjOffset = sb.length
        // Placeholder - we'll calculate pages after building content streams
        offsets += pagesObjOffset

        // Build pages - each page holds ~50 lines
        val linesPerPage = 50
        val pageChunks = lines.chunked(linesPerPage)
        val pageObjectIds = mutableListOf<Int>()
        val pageWidth = 595 // A4
        val pageHeight = 842

        var nextObjId = 3 // 1=catalog, 2=pages, 3+ = fonts and pages

        // Object 3: Font (Helvetica)
        val fontObjId = nextObjId++
        offsets += sb.length
        objects += "$fontObjId 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n"
        sb.append(objects.last())

        // Object 4: Bold Font (Helvetica-Bold)
        val boldFontObjId = nextObjId++
        offsets += sb.length
        objects += "$boldFontObjId 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\nendobj\n"
        sb.append(objects.last())

        // Build each page
        for (chunk in pageChunks) {
            val contentObjId = nextObjId++
            val pageObjId = nextObjId++
            pageObjectIds += pageObjId

            // Build content stream
            val stream = StringBuilder()
            stream.append("BT\n")
            var y = pageHeight - 50 // start from top
            for (line in chunk) {
                val fontRef = if (line.bold) "/F2" else "/F1"
                val safeText = sanitizeText(line.text)
                stream.append("$fontRef ${line.fontSize} Tf\n")
                // Use absolute positioning for each line; relative Td can push text off-page.
                stream.append("1 0 0 1 40 $y Tm\n")
                stream.append("($safeText) Tj\n")
                y -= (line.fontSize + 4)
            }
            stream.append("ET\n")
            val streamStr = stream.toString()

            // Content stream object
            offsets += sb.length
            objects += "$contentObjId 0 obj\n<< /Length ${streamStr.length} >>\nstream\n${streamStr}endstream\nendobj\n"
            sb.append(objects.last())

            // Page object
            offsets += sb.length
            objects += "$pageObjId 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $pageWidth $pageHeight] /Contents $contentObjId 0 R /Resources << /Font << /F1 $fontObjId 0 R /F2 $boldFontObjId 0 R >> >> >>\nendobj\n"
            sb.append(objects.last())
        }

        // Now replace the pages object (object 2)
        val kidsStr = pageObjectIds.joinToString(" ") { "$it 0 R" }
        val pagesObj = "2 0 obj\n<< /Type /Pages /Kids [$kidsStr] /Count ${pageObjectIds.size} >>\nendobj\n"

        // Rebuild the whole PDF with correct pages object
        val finalSb = StringBuilder()
        finalSb.append("%PDF-1.4\n")

        val finalOffsets = mutableListOf<Int>()
        // Object 1
        finalOffsets += finalSb.length
        finalSb.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
        // Object 2 (pages)
        finalOffsets += finalSb.length
        finalSb.append(pagesObj)
        // Remaining objects (index 2+ in objects list correspond to obj 3+)
        for (i in 2 until objects.size) {
            finalOffsets += finalSb.length
            finalSb.append(objects[i])
        }

        // Cross-reference table
        val xrefOffset = finalSb.length
        val totalObjects = finalOffsets.size + 1 // +1 for free entry
        finalSb.append("xref\n")
        finalSb.append("0 $totalObjects\n")
        finalSb.append("0000000000 65535 f \n")
        for (offset in finalOffsets) {
            finalSb.append("${offset.toString().padStart(10, '0')} 00000 n \n")
        }

        // Trailer
        finalSb.append("trailer\n")
        finalSb.append("<< /Size $totalObjects /Root 1 0 R >>\n")
        finalSb.append("startxref\n")
        finalSb.append("$xrefOffset\n")
        finalSb.append("%%EOF\n")

        return finalSb.toString().encodeToByteArray()
    }

    /**
     * Replace non-ASCII characters with ASCII approximations for PDF compatibility.
     * Also escape special PDF string characters.
     */
    private fun sanitizeText(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
            .replace("ñ", "n")
            .replace("Á", "A")
            .replace("É", "E")
            .replace("Í", "I")
            .replace("Ó", "O")
            .replace("Ú", "U")
            .replace("Ñ", "N")
            .replace("¿", "?")
            .replace("¡", "!")
    }
}
