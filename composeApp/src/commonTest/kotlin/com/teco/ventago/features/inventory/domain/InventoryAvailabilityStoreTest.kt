package com.teco.ventago.features.inventory.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InventoryAvailabilityStoreTest {

    private fun snapshot(
        tracked: Boolean = true,
        available: String? = "0",
        allowNegative: Boolean = false,
        enabled: Boolean = true,
    ) = InventoryAvailabilitySnapshot(
        enabled = enabled,
        locationId = 9,
        byItemId = mapOf(
            10 to InventoryAvailabilityRow(
                itemId = 10,
                tracked = tracked,
                available = available,
                allowNegativeStock = allowNegative,
            )
        ),
    )

    @Test
    fun blocksTrackedZeroStockWhenNegativesAreOff() {
        assertTrue(snapshot().shouldBlockUnstocked(itemId = 10, canView = true, isPersonalized = false))
    }

    @Test
    fun allowsZeroStockWhenNegativesAreOn() {
        assertFalse(
            snapshot(allowNegative = true).shouldBlockUnstocked(itemId = 10, canView = true, isPersonalized = false)
        )
    }

    @Test
    fun doesNotBlockWithoutViewOrUnknownAvailability() {
        assertFalse(snapshot().shouldBlockUnstocked(itemId = 10, canView = false, isPersonalized = false))
        assertFalse(snapshot(available = null).shouldBlockUnstocked(itemId = 10, canView = true, isPersonalized = false))
        assertFalse(snapshot(tracked = false).shouldBlockUnstocked(itemId = 10, canView = true, isPersonalized = false))
        assertFalse(snapshot().shouldBlockUnstocked(itemId = 10, canView = true, isPersonalized = true))
        assertFalse(snapshot(enabled = false).shouldBlockUnstocked(itemId = 10, canView = true, isPersonalized = false))
    }
}
