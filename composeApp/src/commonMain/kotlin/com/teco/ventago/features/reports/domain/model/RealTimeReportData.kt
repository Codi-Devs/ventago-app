package com.teco.ventago.features.reports.domain.model

import com.teco.ventago.utils.formatNumberToMoney
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

data class RealTimeReportRequest(
    val reportKey: String,
    val preset: String,
    val startDate: String? = null,
    val endDate: String? = null,
    val compareStartDate: String? = null,
    val compareEndDate: String? = null,
    val customerId: String? = null,
    val page: Int? = null,
    val perPage: Int? = null,
    val extraFilters: Map<String, String> = emptyMap(),
) {
    fun toQueryParameters(): Map<String, String> {
        return RealTimeReportQueryDefaults.parametersFor(this)
    }

    companion object {
        fun defaultFor(definition: RealTimeReportDefinition): RealTimeReportRequest {
            val paginationPage = if (definition.isPaginated) 1 else null
            val paginationSize = if (definition.isPaginated) DEFAULT_PER_PAGE else null
            return RealTimeReportRequest(
                reportKey = definition.key,
                preset = definition.defaultPreset,
                page = paginationPage,
                perPage = paginationSize,
                extraFilters = RealTimeReportQueryDefaults.defaultExtraFilters(definition.key)
            )
        }
    }
}

data class RealTimeReportExportRequest(
    val reportKey: String,
    val format: ReportExportFormat,
    val queryParameters: Map<String, String>,
    val pdfEndpoint: ReportPdfEndpoint,
)

data class RealTimeReportExportPayload(
    val bytes: ByteArray,
    val contentType: String?,
    val fileName: String?,
    val format: ReportExportFormat,
)

data class RealTimeReportData(
    val definition: RealTimeReportDefinition,
    val filters: JsonObject?,
    val metrics: List<RealTimeReportMetric>,
    val rows: List<RealTimeReportRow>,
    val rowSourceLabel: String,
    val chartSections: List<RealTimeReportChartSection>,
    val pagination: RealTimeReportPagination?,
    val rawData: JsonObject,
)

data class RealTimeReportMetric(
    val key: String,
    val label: String,
    val value: String,
)

data class RealTimeReportRow(
    val cells: List<RealTimeReportCell>,
)

data class RealTimeReportCell(
    val key: String,
    val label: String,
    val value: String,
    val alignEnd: Boolean,
)

data class RealTimeReportChartSection(
    val key: String,
    val title: String,
    val points: List<RealTimeReportChartPoint>,
    val lines: List<String>,
)

data class RealTimeReportChartPoint(
    val label: String,
    val value: Double,
    val formattedValue: String,
)

data class RealTimeReportPagination(
    val page: Int,
    val perPage: Int,
    val total: Int,
    val totalPages: Int,
) {
    val from: Int = if (total == 0) 0 else ((page - 1) * perPage) + 1
    val to: Int = minOf(page * perPage, total)
    val canGoPrevious: Boolean = page > 1
    val canGoNext: Boolean = page < totalPages
}

object RealTimeReportParser {
    fun parse(
        definition: RealTimeReportDefinition,
        data: JsonObject,
    ): RealTimeReportData {
        val rowSource = firstArray(data, listOf("rows", "items", "timeline"))
        return RealTimeReportData(
            definition = definition,
            filters = data["filters"].asObjectOrNull(),
            metrics = parseMetrics(data["summary"].asObjectOrNull()),
            rows = parseRows(rowSource?.second),
            rowSourceLabel = rowSource?.first ?: "rows",
            chartSections = parseChartSections(data),
            pagination = parsePagination(data["pagination"].asObjectOrNull()),
            rawData = data
        )
    }

    private fun parseMetrics(summary: JsonObject?): List<RealTimeReportMetric> {
        return summary?.entries
            ?.map { (key, value) ->
                RealTimeReportMetric(
                    key = key,
                    label = labelFor(key),
                    value = displayValue(key, value)
                )
            }
            .orEmpty()
    }

    private fun parseRows(rows: JsonArray?): List<RealTimeReportRow> {
        return rows?.mapNotNull { element ->
            val obj = element.asObjectOrNull() ?: return@mapNotNull null
            val cells = obj.entries.map { (key, value) ->
                RealTimeReportCell(
                    key = key,
                    label = labelFor(key),
                    value = displayValue(key, value),
                    alignEnd = shouldAlignEnd(key, value)
                )
            }
            RealTimeReportRow(cells)
        }.orEmpty()
    }

    private fun parseChartSections(data: JsonObject): List<RealTimeReportChartSection> {
        val sections = mutableListOf<RealTimeReportChartSection>()
        data["charts"].asObjectOrNull()?.entries?.forEach { (key, value) ->
            sections.add(
                RealTimeReportChartSection(
                    key = key,
                    title = labelFor(key),
                    points = chartPointsFrom(value),
                    lines = summarizeJsonValue(value)
                )
            )
        }
        listOf("aging", "buckets", "timeline", "data_quality").forEach { key ->
            val value = data[key] ?: return@forEach
            sections.add(
                RealTimeReportChartSection(
                    key = key,
                    title = labelFor(key),
                    points = chartPointsFrom(value),
                    lines = summarizeJsonValue(value)
                )
            )
        }
        return sections
    }

    private fun parsePagination(pagination: JsonObject?): RealTimeReportPagination? {
        pagination ?: return null
        val page = pagination.intValue("page") ?: 1
        val perPage = pagination.intValue("per_page") ?: pagination.intValue("perPage") ?: DEFAULT_PER_PAGE
        val total = pagination.intValue("total") ?: 0
        val totalPages = pagination.intValue("total_pages") ?: pagination.intValue("totalPages") ?: 1
        return RealTimeReportPagination(
            page = page.coerceAtLeast(1),
            perPage = perPage.coerceAtLeast(1),
            total = total.coerceAtLeast(0),
            totalPages = totalPages.coerceAtLeast(1)
        )
    }

    private fun firstArray(data: JsonObject, keys: List<String>): Pair<String, JsonArray>? {
        return keys.firstNotNullOfOrNull { key ->
            data[key].asArrayOrNull()?.let { key to it }
        }
    }

    private fun chartPointsFrom(value: JsonElement): List<RealTimeReportChartPoint> {
        return when (value) {
            is JsonArray -> pointsFromArray(value)
            is JsonObject -> {
                val nestedArray = value.entries.firstNotNullOfOrNull { (_, child) ->
                    child.asArrayOrNull()?.takeIf { it.isNotEmpty() }
                }
                when {
                    nestedArray != null -> pointsFromArray(nestedArray)
                    value.entries.all { (_, child) -> (child as? JsonPrimitive)?.doubleOrNull != null } -> {
                        value.entries.mapNotNull { (key, child) ->
                            val number = (child as? JsonPrimitive)?.doubleOrNull ?: return@mapNotNull null
                            chartPoint(key, number, key)
                        }
                    }
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }.take(MAX_CHART_POINTS)
    }

    private fun pointsFromArray(array: JsonArray): List<RealTimeReportChartPoint> {
        return array.mapNotNull { element ->
            val obj = element.asObjectOrNull() ?: return@mapNotNull null
            val valueEntry = obj.entries.firstOrNull { (key, child) ->
                val lowerKey = key.lowercase()
                (child as? JsonPrimitive)?.doubleOrNull != null &&
                    !lowerKey.containsAny(listOf("id", "code", "ruc", "year", "page"))
            } ?: return@mapNotNull null
            val number = (valueEntry.value as? JsonPrimitive)?.doubleOrNull ?: return@mapNotNull null
            val label = firstLabelValue(obj) ?: labelFor(valueEntry.key)
            chartPoint(label, number, valueEntry.key)
        }
    }

    private fun chartPoint(label: String, value: Double, valueKey: String): RealTimeReportChartPoint {
        return RealTimeReportChartPoint(
            label = label.ifBlank { "Sin etiqueta" },
            value = value,
            formattedValue = displayValue(valueKey, JsonPrimitive(value))
        )
    }

    private fun firstLabelValue(obj: JsonObject): String? {
        val preferred = listOf("label", "name", "period_label", "group_label", "customer_name", "product_name", "supplier_name", "branch_name", "user", "type")
        preferred.forEach { key ->
            obj[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return obj.entries.firstNotNullOfOrNull { (key, value) ->
            if ((value as? JsonPrimitive)?.doubleOrNull != null) return@firstNotNullOfOrNull null
            if (key.lowercase().containsAny(listOf("id", "code", "ruc"))) return@firstNotNullOfOrNull null
            value.asDisplayString().takeIf { it.isNotBlank() }
        }
    }
}

object RealTimeReportQueryDefaults {
    fun parametersFor(request: RealTimeReportRequest): Map<String, String> {
        val params = linkedMapOf<String, String>()
        params.putAll(request.extraFilters.filterValues { it.isNotBlank() })
        params["preset"] = request.preset
        request.startDate?.takeIf { it.isNotBlank() }?.let { params["start_date"] = it }
        request.endDate?.takeIf { it.isNotBlank() }?.let { params["end_date"] = it }
        request.compareStartDate?.takeIf { it.isNotBlank() }?.let { params["compare_start_date"] = it }
        request.compareEndDate?.takeIf { it.isNotBlank() }?.let { params["compare_end_date"] = it }
        request.customerId?.takeIf { it.isNotBlank() }?.let { params["customer_id"] = it }
        request.page?.let { params["page"] = it.toString() }
        request.perPage?.let { params["per_page"] = it.toString() }
        return params
    }

    fun defaultExtraFilters(reportKey: String): Map<String, String> {
        return when (reportKey) {
            "sales_summary",
            "sales_by_product",
            "sales_by_payment_method",
            "sales_by_user",
            "sales_by_branch",
            "sales_by_customer" -> mapOf("document_status" to "all")
            "sales_adjustments" -> mapOf("adjustment_type" to "all")
            "sales_pending_collection" -> mapOf(
                "date_field" to "due",
                "balance_status" to "open"
            )
            "customer_statement" -> mapOf(
                "include_paid" to "true",
                "include_credit_notes" to "true"
            )
            "customers_with_balance" -> mapOf(
                "balance_status" to "open",
                "min_amount" to "0"
            )
            "new_customers" -> mapOf("has_first_purchase" to "true")
            "inactive_customers" -> mapOf(
                "inactive_since" to currentYearStart(),
                "min_amount" to "0"
            )
            "customer_ranking" -> mapOf(
                "document_status" to "all",
                "min_amount" to "0"
            )
            "sales_tax_summary" -> mapOf("include_credit_notes" to "true")
            "expense_tax_summary" -> mapOf(
                "group_by" to "month",
                "tax_filter" to "all"
            )
            "expense_summary" -> mapOf("group_by" to "month")
            "expense_by_account" -> mapOf("group_by" to "account")
            "expense_by_supplier" -> mapOf("group_by" to "supplier")
            "expense_detail" -> mapOf(
                "sort_by" to "emission_date",
                "sort_direction" to "desc"
            )
            "recurring_expense_report" -> mapOf(
                "frequency" to "monthly",
                "payment_status" to "paid"
            )
            "profit_and_loss" -> mapOf(
                "group_by" to "month",
                "base" to "issued",
                "currency_code" to "USD"
            )
            "financial_comparison" -> financialComparisonDefaults()
            "operating_margin" -> mapOf(
                "group_by" to "month",
                "base" to "issued"
            )
            "business_overview" -> mapOf(
                "base" to "issued",
                "currency_code" to "USD"
            )
            "cash_flow" -> mapOf(
                "group_by" to "week",
                "include_overdue" to "true",
                "type" to "all",
                "status" to "all",
                "currency_code" to "USD",
                "sort_by" to "due_date",
                "sort_direction" to "asc"
            )
            else -> emptyMap()
        }
    }

    private fun currentYearStart(): String {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return "${today.year}-01-01"
    }

    private fun financialComparisonDefaults(): Map<String, String> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val currentStart = isoDate(today.year, today.monthNumber, 1)
        val currentEnd = isoDate(today.year, today.monthNumber, daysInMonth(today.year, today.monthNumber))
        val previousMonth = if (today.monthNumber == 1) 12 else today.monthNumber - 1
        val previousYear = if (today.monthNumber == 1) today.year - 1 else today.year
        return mapOf(
            "start_date" to currentStart,
            "end_date" to currentEnd,
            "compare_start_date" to isoDate(previousYear, previousMonth, 1),
            "compare_end_date" to isoDate(previousYear, previousMonth, daysInMonth(previousYear, previousMonth)),
            "base" to "collected"
        )
    }
}

const val DEFAULT_PER_PAGE = 50
private const val MAX_CHART_POINTS = 8

private val MONEY_KEYS = listOf(
    "amount", "average", "balance", "charge", "collected", "cost", "expense", "expenses",
    "gross", "itbms", "paid", "payable", "payment", "payments", "pending", "profit",
    "receivable", "revenue", "sales", "sold", "spent", "subtotal", "tax", "ticket",
    "total", "with_itbms"
)

private val COUNT_KEYS = listOf(
    "count", "quantity", "days", "document_count", "invoice_count", "ranking",
    "page", "per_page", "year"
)

private val PERCENT_KEYS = listOf(
    "percent", "percentage", "rate", "ratio", "margin", "variation"
)

private val FIELD_LABELS = mapOf(
    "adjusted_document_count" to "Documentos ajustados",
    "average_days_overdue" to "Dias vencidos promedio",
    "average_itbms_per_document" to "Promedio ITBMS",
    "average_pending_per_customer" to "Promedio por cliente",
    "average_ticket" to "Ticket promedio",
    "balance" to "Saldo",
    "branch_code" to "Codigo",
    "branch_name" to "Sucursal",
    "cash_flow" to "Flujo de caja",
    "charts" to "Graficas",
    "collected" to "Cobrado",
    "customer_name" to "Cliente",
    "customer_ruc" to "RUC",
    "date" to "Fecha",
    "document" to "Documento",
    "document_count" to "Documentos",
    "due_date" to "Vencimiento",
    "emission_date" to "Emision",
    "expense_count" to "Gastos",
    "expenses" to "Gastos",
    "filters" to "Filtros",
    "group_label" to "Grupo",
    "itbms" to "ITBMS",
    "generated_itbms" to "ITBMS generado",
    "itbms_generated" to "ITBMS generado",
    "itbms_total" to "ITBMS",
    "net_cash_flow" to "Flujo neto",
    "net_itbms_payable" to "ITBMS neto",
    "paid" to "Pagado",
    "pending" to "Pendiente",
    "percent_of_sales" to "% ventas",
    "percent_of_total" to "% total",
    "product_category" to "Categoria",
    "product_code" to "Codigo",
    "product_name" to "Producto",
    "profit" to "Utilidad",
    "revenue" to "Ingresos",
    "rows" to "Filas",
    "status" to "Estado",
    "subtotal" to "Subtotal",
    "total_sales" to "Ventas totales",
    "supplier_name" to "Proveedor",
    "supplier_ruc" to "RUC",
    "tax_retained" to "Retenciones",
    "taxable_subtotal" to "Subtotal gravable",
    "timeline" to "Linea de tiempo",
    "total" to "Total",
    "total_adjusted" to "Total ajustado",
    "total_sold" to "Total vendido",
    "type" to "Tipo",
    "user" to "Usuario",
    "year" to "Año",
)

private fun JsonElement?.asObjectOrNull(): JsonObject? = this as? JsonObject

private fun JsonElement?.asArrayOrNull(): JsonArray? = this as? JsonArray

private fun JsonObject.intValue(key: String): Int? {
    val primitive = this[key] as? JsonPrimitive ?: return null
    return primitive.contentOrNull?.toIntOrNull()
}

private fun summarizeJsonValue(value: JsonElement): List<String> {
    return when (value) {
        is JsonArray -> listOf("${value.size} puntos recibidos")
        is JsonObject -> value.entries.take(6).map { (key, child) ->
            "${labelFor(key)}: ${summaryValue(child)}"
        }.ifEmpty { listOf("Objeto sin campos") }
        else -> listOf(summaryValue(value))
    }
}

private fun summaryValue(value: JsonElement): String {
    return when (value) {
        is JsonArray -> "${value.size} registros"
        is JsonObject -> "${value.size} campos"
        else -> value.asDisplayString()
    }
}

private fun displayValue(key: String, value: JsonElement): String {
    val raw = value.asDisplayString()
    val lowerKey = key.lowercase()
    val number = (value as? JsonPrimitive)?.doubleOrNull
    return when {
        number != null && lowerKey.containsAny(listOf("generated_itbms", "itbms_generated")) -> {
            formatNumberToMoney(raw)
        }
        number != null && lowerKey.containsAny(PERCENT_KEYS) -> "${trimNumber(number)}%"
        number != null && lowerKey.containsAny(MONEY_KEYS) && !lowerKey.containsAny(COUNT_KEYS) -> {
            formatNumberToMoney(raw)
        }
        number != null -> trimNumber(number)
        raw.matches(Regex("\\d{4}-\\d{2}-\\d{2}.*")) -> formatDateLike(raw)
        else -> raw.ifBlank { "-" }
    }
}

private fun JsonElement.asDisplayString(): String {
    return when (this) {
        is JsonPrimitive -> {
            booleanOrNull?.let { if (it) "Si" else "No" }
                ?: contentOrNull.orEmpty()
        }
        is JsonArray -> "${size} registros"
        is JsonObject -> "${size} campos"
        else -> toString()
    }
}

private fun labelFor(key: String): String {
    FIELD_LABELS[key]?.let { return it }
    return key.split("_").filter { it.isNotBlank() }.joinToString(" ") { token ->
        token.replaceFirstChar { char -> char.uppercase() }
    }
}

private fun shouldAlignEnd(key: String, value: JsonElement): Boolean {
    val lowerKey = key.lowercase()
    return (value as? JsonPrimitive)?.doubleOrNull != null ||
        lowerKey.containsAny(MONEY_KEYS) ||
        lowerKey.containsAny(PERCENT_KEYS) ||
        lowerKey.containsAny(COUNT_KEYS)
}

private fun String.containsAny(tokens: List<String>): Boolean {
    return tokens.any { contains(it) }
}

private fun trimNumber(value: Double): String {
    val long = value.toLong()
    return if (value == long.toDouble()) long.toString() else value.toString()
}

private fun formatDateLike(raw: String): String {
    val date = raw.substringBefore("T")
    val parts = date.split("-")
    if (parts.size != 3) return raw
    return "${parts[2]}/${parts[1]}/${parts[0]}"
}

private fun isoDate(year: Int, month: Int, day: Int): String {
    return "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}

private fun daysInMonth(year: Int, month: Int): Int {
    val leap = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (leap) 29 else 28
        else -> 30
    }
}
