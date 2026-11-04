package com.teco.ventago.features.orders.domain

import com.teco.ventago.features.orders.domain.models.OrderHistoryDto
import kotlin.test.Test
import kotlin.test.assertEquals

class OrderHistoryDisplayTest {
    @Test
    fun hidesSyncLineWhenAPersonAlreadyIssuedTheInvoice() {
        val visible = visibleOrderHistories(
            listOf(
                OrderHistoryDto(statusId = 2, note = "Pedido creado", changedBy = "Oscar Hernandez"),
                OrderHistoryDto(
                    historyType = 1,
                    statusId = 2,
                    note = "Factura emitida exitosamente",
                    changedBy = "Oscar Hernandez",
                ),
                OrderHistoryDto(
                    historyType = 1,
                    statusId = 2,
                    note = "Factura emitida exitosamente (sync pendiente)",
                    changedBy = "pending-sync-job",
                ),
            ),
        )
        assertEquals(2, visible.size)
        assertEquals(false, visible.any { it.changedBy == "pending-sync-job" || it.changedBy == "Sistema" })
    }

    @Test
    fun showsSystemWhenTheSyncJobConfirmedTheInvoice() {
        val visible = visibleOrderHistories(
            listOf(
                OrderHistoryDto(statusId = 2, note = "Pedido creado", changedBy = "Oscar Hernandez"),
                OrderHistoryDto(
                    historyType = 1,
                    statusId = 2,
                    note = "Factura emitida exitosamente (sync pendiente)",
                    changedBy = "pending-sync-job",
                ),
            ),
        )
        assertEquals("Sistema", visible[1].changedBy)
        assertEquals("Factura confirmada por el sistema", visible[1].note)
    }

    @Test
    fun hidesSyncLineWhenTheNoteNoLongerSaysPendingSync() {
        val visible = visibleOrderHistories(
            listOf(
                OrderHistoryDto(
                    historyType = 1,
                    statusId = 2,
                    note = "Factura emitida exitosamente",
                    changedBy = "Oscar Hernandez",
                ),
                OrderHistoryDto(
                    historyType = 1,
                    statusId = 2,
                    note = "Factura confirmada",
                    changedBy = "pending-sync-job",
                ),
            ),
        )
        assertEquals(1, visible.size)
        assertEquals("Oscar Hernandez", visible[0].changedBy)
    }

    @Test
    fun rewritesAFailedSyncLineAsSystem() {
        val visible = visibleOrderHistories(
            listOf(
                OrderHistoryDto(
                    historyType = 1,
                    statusId = 3,
                    note = "Facturación fallida tras verificación pendiente",
                    changedBy = "pending-sync-job",
                ),
            ),
        )
        assertEquals("Sistema", visible[0].changedBy)
        assertEquals("No se pudo confirmar la factura. Puedes reintentarla.", visible[0].note)
    }

    @Test
    fun dropsTheRetryHintWhenALaterLineAlreadyIssuedTheInvoice() {
        val visible = visibleOrderHistories(
            listOf(
                OrderHistoryDto(
                    historyType = 1,
                    statusId = 3,
                    note = "Facturación fallida tras verificación pendiente",
                    changedBy = "pending-sync-job",
                    createdAt = "2026-10-29T15:54:45Z",
                ),
                OrderHistoryDto(
                    historyType = 1,
                    statusId = 2,
                    note = "Factura emitida exitosamente vía reintento",
                    changedBy = "Oscar Hernandez",
                    createdAt = "2026-10-29T16:10:00Z",
                ),
            ),
        )
        val failed = visible.first { it.changedBy == "Sistema" }
        assertEquals("No se pudo confirmar la factura", failed.note)
    }
}
