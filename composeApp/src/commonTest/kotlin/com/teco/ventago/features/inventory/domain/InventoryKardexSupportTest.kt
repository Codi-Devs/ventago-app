package com.teco.ventago.features.inventory.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InventoryKardexSupportTest {

    private fun row(
        movementId: Int,
        type: String,
        direction: String,
        qty: String = "1",
        locationId: Int = 1,
        stockState: String = "available",
        sourceId: String? = null,
        correlationId: String? = null,
    ) = KardexMovementRow(
        movementId = movementId,
        movementType = type,
        locationId = locationId,
        direction = direction,
        quantity = qty,
        occurredAt = "2026-01-01T10:00:00Z",
        postedAt = "2026-01-01T10:00:00Z",
        stockState = stockState,
        sourceId = sourceId,
        correlationId = correlationId,
    )

    @Test
    fun collapsesConsumeAndLandedRows() {
        val rows = listOf(
            row(1, "reservation.reserve", "credit", sourceId = "order:1:100"),
            row(2, "reservation.consume", "credit", sourceId = "order:1:100"),
            row(3, "landed.allocated", "valuation"),
            row(4, "purchase.received", "debit", qty = "5"),
        )
        val collapsed = InventoryKardexSupport.collapseKardexRows(rows)
        assertEquals(2, collapsed.size)
        assertEquals("Venta", collapsed.first { it.movementId == 1 }.displayType)
    }

    @Test
    fun buildsChartSeriesEndingAtCurrentAvailable() {
        val rows = InventoryKardexSupport.collapseKardexRows(
            listOf(
                row(10, "purchase.received", "debit", qty = "5"),
                row(11, "sale.consume", "credit", qty = "2"),
            ),
        )
        val series = InventoryKardexSupport.buildAvailableSeries(3.0, rows)
        assertTrue(series.isNotEmpty())
        assertEquals(3.0, series.last().qty)
        assertEquals("Ahora", series.last().label)
    }

    @Test
    fun pagesKardexRowsByTwenty() {
        val rows = (1..45).map {
            row(it, "manual.adjustment", if (it % 2 == 0) "debit" else "credit")
        }
        assertEquals(3, InventoryKardexSupport.pageCount(rows.size))
        assertEquals(20, InventoryKardexSupport.pageSlice(rows, 0).size)
        assertEquals(5, InventoryKardexSupport.pageSlice(rows, 2).size)
    }

    @Test
    fun formatsInventoryQuantitiesWithoutTrailingZeros() {
        assertEquals("15", InventoryKardexSupport.formatInventoryQuantity("15.000"))
        assertEquals("15.5", InventoryKardexSupport.formatInventoryQuantity("15.500"))
        assertEquals("15.125", InventoryKardexSupport.formatInventoryQuantity("15.125"))
        assertEquals("0", InventoryKardexSupport.formatQty(0.0))
    }

    @Test
    fun resolvesOrderReferenceFromKardexMovement() {
        val fromCorrelation = row(
            movementId = 1,
            type = "reservation.reserve",
            direction = "credit",
            sourceId = "order:1:ORD-100",
        )
        val ref = InventoryKardexSupport.orderRefFromMovement(fromCorrelation)
        assertEquals("ORD-100", ref?.orderNumber)
        assertEquals("Orden ORD-100", ref?.label)

        val fromSourceType = row(2, "sale.consume", "credit").copy(
            sourceType = "order",
            sourceId = "ORD-200",
        )
        assertEquals("ORD-200", InventoryKardexSupport.orderRefFromMovement(fromSourceType)?.orderNumber)

        val detail = InventoryKardexSupport.buildMovementDetail(
            fromCorrelation.copy(displayType = "Venta"),
        )
        assertEquals("Venta", detail.typeLabel)
        assertEquals("ORD-100", detail.orderRef?.orderNumber)
    }

    @Test
    fun buildsAdjustmentDetailWithReason() {
        val detail = InventoryKardexSupport.buildMovementDetail(
            row(3, "manual.adjustment", "debit", sourceId = null).copy(
                reasonText = "Conteo físico",
            ),
        )
        assertEquals("Conteo físico", detail.adjustmentReason)
        assertEquals(null, detail.orderRef)
    }
}
