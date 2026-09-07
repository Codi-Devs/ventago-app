package com.teco.ventago.features.inventory.domain

import com.teco.ventago.features.inventory.ui.InventoryLocationOption
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object InventoryKardexSupport {
    const val KARDEX_FETCH_LIMIT = 80
    const val KARDEX_PAGE_SIZE = 20

    private val orderRefRegex = Regex("^order:(\\d+):(.+)$")

    private val movementTypeLabels = mapOf(
        "manual.adjustment" to "Ajuste",
        "opening.balance" to "Saldo inicial",
        "purchase.receipt" to "Recepción",
        "purchase.received" to "Compra recibida",
        "purchase.returned" to "Devolución a proveedor",
        "landed.allocated" to "Costo adicional",
        "sale.reserve" to "Reserva",
        "sale.consume" to "Venta",
        "sale.release" to "Liberación",
        "reservation.reserve" to "Reserva",
        "reservation.consume" to "Venta",
        "reservation.release" to "Liberación",
        "reservation.expire" to "Expiró",
        "inventory.return" to "Devolución",
        "inventory.transfer" to "Traslado",
        "inventory.count" to "Conteo",
        "inventory.dispatch" to "Despacho",
    )

    private val stockStateLabels = mapOf(
        "available" to "Disponible",
        "reserved" to "Reservado",
        "quarantine" to "Cuarentena",
        "damaged" to "Dañado",
    )

    fun movementTypeLabel(type: String): String {
        val key = type.trim()
        return movementTypeLabels[key] ?: key.replace('.', ' ').replace('_', ' ').ifBlank { "Movimiento" }
    }

    fun stockStateLabel(state: String): String = stockStateLabels[state] ?: state.ifBlank { "—" }

    fun kardexSenseLabel(row: KardexMovementRow): String {
        if (row.direction.equals("valuation", ignoreCase = true)) return "Valuación"
        return if (row.direction.equals("debit", ignoreCase = true)) "Entrada" else "Salida"
    }

    fun kardexQtyDisplay(row: KardexMovementRow): String {
        if (row.direction.equals("valuation", ignoreCase = true)) return "—"
        val inbound = row.direction.equals("debit", ignoreCase = true)
        val qty = formatInventoryQuantity(row.quantity).ifBlank { row.quantity }
        return "${if (inbound) "+" else "−"}$qty"
    }

    fun isValuationOnlyRow(row: KardexMovementRow): Boolean {
        return row.direction.equals("valuation", ignoreCase = true)
    }

    fun isInboundLeg(direction: String): Boolean {
        return direction.equals("debit", ignoreCase = true)
    }

    fun orderRefFromMovement(row: KardexMovementRow): KardexOrderRef? {
        listOfNotNull(row.correlationId, row.sourceId).forEach { raw ->
            val match = orderRefRegex.find(raw.trim()) ?: return@forEach
            val orderNumber = match.groupValues[2]
            if (orderNumber.isNotBlank()) {
                return KardexOrderRef(label = "Orden $orderNumber", orderNumber = orderNumber)
            }
        }
        if (row.sourceType.equals("order", ignoreCase = true)) {
            val orderNumber = row.sourceId?.trim().orEmpty()
            if (orderNumber.isNotBlank()) {
                return KardexOrderRef(label = "Orden $orderNumber", orderNumber = orderNumber)
            }
        }
        return null
    }

    fun purchaseOrderRefFromMovement(row: KardexMovementRow): KardexPurchaseOrderRef? {
        if (!row.sourceType.equals("purchase_order", ignoreCase = true)) return null
        val purchaseOrderId = row.sourceId?.trim()?.toIntOrNull() ?: return null
        if (purchaseOrderId <= 0) return null
        return KardexPurchaseOrderRef(purchaseOrderId = purchaseOrderId)
    }

    fun buildMovementDetail(
        row: KardexMovementRow,
        locations: List<InventoryLocationOption> = emptyList(),
    ): KardexMovementDetail {
        val valuationOnly = isValuationOnlyRow(row)
        val inbound = isInboundLeg(row.direction)
        val order = orderRefFromMovement(row)
        val purchaseOrder = purchaseOrderRefFromMovement(row)
        val reason = row.reasonText?.trim().orEmpty()
        val isAdjust = row.movementType == "manual.adjustment"
        val typeLabel = row.displayType ?: movementTypeLabel(row.movementType)

        val adjustmentReason = if (isAdjust) reason.ifBlank { "Sin comentario" } else null
        val comment = if (!isAdjust && reason.isNotBlank() && purchaseOrder == null) reason else null
        val valuationNote = if (valuationOnly) {
            "Ajusta el costo del inventario; no cambia la cantidad en stock."
        } else {
            null
        }
        val genericReference = if (
            order == null &&
            purchaseOrder == null &&
            !row.sourceId.isNullOrBlank() &&
            row.sourceType != "purchase_landed_cost"
        ) {
            row.sourceId
        } else {
            null
        }

        return KardexMovementDetail(
            typeLabel = typeLabel,
            senseLabel = kardexSenseLabel(row),
            isInbound = inbound,
            isValuation = valuationOnly,
            quantityDisplay = kardexQtyDisplay(row),
            dateLabel = formatMovementDate(row.occurredAt, row.postedAt),
            locationLabel = locationDisplayName(row, locations),
            adjustmentReason = adjustmentReason,
            comment = comment,
            valuationNote = valuationNote,
            orderRef = order,
            purchaseOrderRef = purchaseOrder,
            genericReference = genericReference,
        )
    }

    fun sumBalanceState(rows: List<InventoryBalanceRow>, state: String): Double {
        return rows.filter { it.stockState == state }
            .sumOf { it.quantity.toDoubleOrNull() ?: 0.0 }
    }

    fun formatInventoryQuantity(raw: String?): String {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return ""
        if (trimmed == "—") return trimmed
        val parsed = trimmed.toDoubleOrNull() ?: return trimmed
        return formatQty(parsed)
    }

    fun formatQty(value: Double): String {
        if (!value.isFinite()) return "0"
        val rounded = kotlin.math.round(value * 1000.0) / 1000.0
        if (kotlin.math.abs(rounded - kotlin.math.round(rounded)) < 1e-9) {
            return kotlin.math.round(rounded).toLong().toString()
        }
        return rounded.toString().trimEnd('0').trimEnd('.')
    }

    fun alarmState(available: Double, minQty: String): Pair<String, Boolean> {
        val min = minQty.trim().toDoubleOrNull() ?: 0.0
        if (min <= 0.0) return "Sin umbral" to false
        return if (available < min) "Bajo mínimo" to true else "En rango" to false
    }

    fun locationDisplayName(
        row: KardexMovementRow,
        locations: List<InventoryLocationOption>,
    ): String {
        if (!row.locationName.isNullOrBlank()) return row.locationName
        val match = locations.firstOrNull { it.id == row.locationId }
        if (match != null) return match.name
        return row.locationCode ?: row.locationId.takeIf { it > 0 }?.toString() ?: "—"
    }

    fun parseBalances(data: JsonElement?, locations: List<InventoryLocationOption>): List<InventoryBalanceRow> {
        val array = when (data) {
            is JsonArray -> data
            is JsonObject -> data["items"]?.jsonArray ?: data["rows"]?.jsonArray
            else -> null
        } ?: return emptyList()
        return array.mapNotNull { element ->
            val obj = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
            val locationId = obj["location_id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            val stockState = obj["stock_state"]?.jsonPrimitive?.contentOrNull ?: "available"
            val qty = obj["available"]?.jsonPrimitive?.contentOrNull
                ?: obj["quantity"]?.jsonPrimitive?.contentOrNull
                ?: "0"
            val locationName = obj["location_name"]?.jsonPrimitive?.contentOrNull
                ?: obj["location_code"]?.jsonPrimitive?.contentOrNull
                ?: locations.firstOrNull { it.id == locationId }?.name
                ?: locationId.toString()
            InventoryBalanceRow(
                locationId = locationId,
                locationName = locationName,
                stockState = stockState,
                quantity = formatInventoryQuantity(qty),
            )
        }
    }

    fun parseKardexResponse(data: JsonElement?): KardexPageResult {
        val obj = data as? JsonObject ?: return KardexPageResult()
        val rowsArray = obj["rows"]?.jsonArray ?: return KardexPageResult()
        val rows = rowsArray.mapNotNull { element -> parseKardexRow(element.jsonObject) }
        val nextCursor = obj["next_before_movement_id"]?.jsonPrimitive?.intOrNull
        val displayRows = collapseKardexRows(rows)
        val hasMore = rows.size >= KARDEX_FETCH_LIMIT && nextCursor != null && nextCursor > 0
        return KardexPageResult(
            rows = rows,
            displayRows = displayRows,
            nextBeforeMovementId = nextCursor,
            hasMoreOnServer = hasMore,
        )
    }

    fun parseKardexRow(obj: JsonObject): KardexMovementRow? {
        val movementId = obj["movement_id"]?.jsonPrimitive?.intOrNull ?: return null
        return KardexMovementRow(
            movementId = movementId,
            movementType = obj["movement_type"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            locationId = obj["location_id"]?.jsonPrimitive?.intOrNull ?: 0,
            locationName = obj["location_name"]?.jsonPrimitive?.contentOrNull,
            locationCode = obj["location_code"]?.jsonPrimitive?.contentOrNull,
            direction = obj["direction"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            quantity = obj["quantity"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            occurredAt = obj["occurred_at"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            postedAt = obj["posted_at"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            stockState = obj["stock_state"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            lineNo = obj["line_no"]?.jsonPrimitive?.intOrNull ?: 0,
            sourceType = obj["source_type"]?.jsonPrimitive?.contentOrNull,
            sourceId = obj["source_id"]?.jsonPrimitive?.contentOrNull,
            correlationId = obj["correlation_id"]?.jsonPrimitive?.contentOrNull,
            reasonText = obj["reason_text"]?.jsonPrimitive?.contentOrNull,
        )
    }

    fun collapseKardexRows(rows: List<KardexMovementRow>): List<KardexMovementRow> {
        val consumed = mutableSetOf<String>()
        rows.forEach { row ->
            if (row.movementType != "reservation.consume") return@forEach
            movementLinkKeys(row).forEach { consumed.add(it) }
        }
        val seen = mutableSetOf<String>()
        val out = mutableListOf<KardexMovementRow>()
        rows.forEach { row ->
            if (row.locationId <= 0 || row.stockState != "available") return@forEach
            if (row.movementType == "reservation.consume") return@forEach
            if (row.movementType == "landed.allocated") return@forEach
            val key = listOf(row.movementId, row.locationId, row.direction, row.quantity, row.lineNo).joinToString(":")
            if (!seen.add(key)) return@forEach
            val displayType = if (row.movementType == "reservation.reserve") {
                if (movementLinkKeys(row).any { consumed.contains(it) }) "Venta" else "Reserva"
            } else {
                null
            }
            out.add(row.copy(displayType = displayType))
        }
        return out
    }

    fun buildAvailableSeries(currentAvailable: Double, rows: List<KardexMovementRow>): List<StockChartPoint> {
        val legs = rows
            .filter { it.stockState == "available" && it.locationId > 0 }
            .filter { !it.direction.equals("valuation", ignoreCase = true) }
            .mapIndexed { index, row -> Triple(row, movementTimestamp(row), index) }
            .filter { (_, _, _) -> true }
            .filter { (row, _, _) -> (row.quantity.toDoubleOrNull() ?: 0.0) > 0.0 }
            .sortedWith(
                compareByDescending<Triple<KardexMovementRow, Long, Int>> { it.second }
                    .thenByDescending { it.first.movementId }
                    .thenByDescending { it.third },
            )

        val history = mutableListOf<StockChartPoint>()
        var qty = currentAvailable
        legs.forEachIndexed { offset, (row, at, _) ->
            val amount = row.quantity.toDoubleOrNull() ?: 0.0
            val delta = if (row.direction.equals("debit", ignoreCase = true)) amount else -amount
            qty -= delta
            history.add(
                StockChartPoint(
                    qty = qty,
                    label = formatMovementDate(row.occurredAt.ifBlank { row.postedAt }),
                ),
            )
        }
        history.add(StockChartPoint(qty = currentAvailable, label = "Ahora"))
        return history
    }

    fun pageCount(totalRows: Int): Int {
        if (totalRows <= 0) return 1
        return (totalRows + KARDEX_PAGE_SIZE - 1) / KARDEX_PAGE_SIZE
    }

    fun pageSlice(rows: List<KardexMovementRow>, page: Int): List<KardexMovementRow> {
        if (rows.isEmpty()) return emptyList()
        val safePage = page.coerceIn(0, pageCount(rows.size) - 1)
        val from = safePage * KARDEX_PAGE_SIZE
        return rows.drop(from).take(KARDEX_PAGE_SIZE)
    }

    private fun movementLinkKeys(row: KardexMovementRow): List<String> {
        return listOfNotNull(
            row.sourceId?.trim()?.takeIf { it.isNotEmpty() },
            row.correlationId?.trim()?.takeIf { it.isNotEmpty() },
        )
    }

    private fun movementTimestamp(row: KardexMovementRow): Long {
        return parseIsoMillis(row.occurredAt.ifBlank { row.postedAt })
    }

    private fun parseIsoMillis(value: String): Long {
        if (value.isBlank()) return 0L
        return runCatching {
            kotlinx.datetime.Instant.parse(value.replace(' ', 'T').let {
                if (it.endsWith("Z")) it else "${it}Z"
            }).toEpochMilliseconds()
        }.getOrElse {
            runCatching {
                kotlinx.datetime.Instant.parse(value).toEpochMilliseconds()
            }.getOrDefault(0L)
        }
    }

    private fun formatMovementDate(value: String): String {
        if (value.isBlank()) return "—"
        return value.take(16).replace('T', ' ')
    }

    fun formatMovementDate(occurredAt: String, postedAt: String): String {
        return formatMovementDate(occurredAt.ifBlank { postedAt })
    }
}
