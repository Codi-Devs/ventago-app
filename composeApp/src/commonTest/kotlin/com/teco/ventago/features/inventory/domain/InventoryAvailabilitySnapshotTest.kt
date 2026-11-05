package com.teco.ventago.features.inventory.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InventoryAvailabilitySnapshotTest {
    @Test
    fun positiveStockRequiresTrackedQuantityAboveZero() {
        val snapshot = InventoryAvailabilitySnapshot(
            enabled = true,
            byItemId = mapOf(
                1 to InventoryAvailabilityRow(itemId = 1, tracked = true, available = "2.5"),
                2 to InventoryAvailabilityRow(itemId = 2, tracked = true, available = "0"),
                3 to InventoryAvailabilityRow(itemId = 3, tracked = false, available = "4"),
                4 to InventoryAvailabilityRow(itemId = 4, tracked = true, available = null),
            ),
        )

        assertTrue(snapshot.hasPositiveAvailableStock(1))
        assertFalse(snapshot.hasPositiveAvailableStock(2))
        assertFalse(snapshot.hasPositiveAvailableStock(3))
        assertFalse(snapshot.hasPositiveAvailableStock(4))
        assertFalse(snapshot.hasPositiveAvailableStock(9))
        assertEquals("2.5", snapshot.positiveAvailableStockLabel(1))
        assertNull(snapshot.positiveAvailableStockLabel(2))
    }
}
