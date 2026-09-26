package com.teco.ventago.features.pos.ui.customer.add.viewmodel

import com.teco.ventago.features.customers.domain.models.CustomerListItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AddCustomerDuplicateRucTest {

    @Test
    fun pickExistingCustomerMatchesNormalizedRuc() {
        val existing = sampleCustomer(id = 42, ruc = " 155-623-189 ")
        val picked = pickExistingCustomerByRuc(
            ruc = "155-623-189",
            candidates = listOf(
                sampleCustomer(id = 7, ruc = "8-123-456"),
                existing,
            ),
        )

        assertEquals(42L, picked?.id)
    }

    @Test
    fun pickExistingCustomerIgnoresWhitespaceAndCase() {
        val existing = sampleCustomer(id = 9, ruc = "pe-123-456")
        val picked = pickExistingCustomerByRuc(" PE-123-456 ", listOf(existing))
        assertEquals(9L, picked?.id)
    }

    @Test
    fun pickExistingCustomerReturnsNullWhenRucDoesNotMatch() {
        val picked = pickExistingCustomerByRuc(
            ruc = "155623189",
            candidates = listOf(sampleCustomer(id = 1, ruc = "8-123-456")),
        )
        assertNull(picked)
    }

    @Test
    fun previousAddCustomerStepStopsAtType() {
        assertEquals(null, previousAddCustomerStep(AddCustomerStep.TYPE))
        assertEquals(AddCustomerStep.TYPE, previousAddCustomerStep(AddCustomerStep.MAIN_INFO))
        assertEquals(AddCustomerStep.MAIN_INFO, previousAddCustomerStep(AddCustomerStep.OPTIONAL_INFO))
    }

    @Test
    fun normalizeCustomerRucStripsSpaces() {
        assertEquals("155623189", normalizeCustomerRuc(" 155 623 189 "))
    }

    private fun sampleCustomer(id: Long, ruc: String): CustomerListItem {
        return CustomerListItem(
            id = id,
            name = "Cliente $id",
            email = null,
            ruc = ruc,
            status = 1,
            invoiceCustomer = 1,
            updatedAt = 1,
        )
    }
}
