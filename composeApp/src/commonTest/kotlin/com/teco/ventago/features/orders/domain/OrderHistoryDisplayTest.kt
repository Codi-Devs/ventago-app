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
}
