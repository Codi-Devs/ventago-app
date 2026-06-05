package com.teco.ventago.features.pos

import com.teco.ventago.features.customers.domain.models.CustomerListItem
import com.teco.ventago.features.pos.ui.viewmodel.INVALID_FINAL_CUSTOMER_CEDULA_MESSAGE
import com.teco.ventago.features.pos.ui.viewmodel.PosState
import com.teco.ventago.features.pos.ui.viewmodel.cartCustomerDisplay
import com.teco.ventago.features.pos.ui.viewmodel.normalizeFinalCustomerIdentificationNumber
import com.teco.ventago.features.pos.ui.viewmodel.validateFinalCustomerIdentification
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PosFinalCustomerValidationTest {

    @Test
    fun cedulaValidationRejectsInvalidFinalCustomerCedula() {
        assertEquals(
            INVALID_FINAL_CUSTOMER_CEDULA_MESSAGE,
            validateFinalCustomerIdentification(finalIdType = "cedula", finalIdNumber = "123")
        )
    }

    @Test
    fun cedulaValidationAcceptsAndNormalizesSupportedFormats() {
        val normalizedCedula = normalizeFinalCustomerIdentificationNumber(
            finalIdType = "cedula",
            finalIdNumber = "  pe-1234-12345  "
        )

        assertEquals("PE-1234-12345", normalizedCedula)
        assertNull(
            validateFinalCustomerIdentification(
                finalIdType = "cedula",
                finalIdNumber = normalizedCedula
            )
        )
    }

    @Test
    fun nonCedulaIdentificationSkipsPanamaCedulaValidation() {
        assertNull(
            validateFinalCustomerIdentification(
                finalIdType = "passport",
                finalIdNumber = "abc-123"
            )
        )
    }

    @Test
    fun cartCustomerDisplayUsesTypedFinalCustomerIdentity() {
        val display = PosState(
            finalCustomer = true,
            finalName = "John Smith",
            finalEmail = "john@example.com",
            finalIdType = "passport",
            finalIdNumber = "US123456789"
        ).cartCustomerDisplay()

        assertNotNull(display)
        assertFalse(display.isRegisteredCustomer)
        assertEquals("John Smith", display.name)
        assertEquals("Consumidor final", display.customerTypeLabel)
        assertEquals("Pasaporte", display.identificationLabel)
        assertEquals("US123456789", display.identificationValue)
        assertEquals("john@example.com", display.email)
    }

    @Test
    fun cartCustomerDisplayPrioritizesSavedCustomerSelection() {
        val display = PosState(
            customer = CustomerListItem(
                id = 10L,
                name = "Cliente guardado",
                email = "saved@example.com",
                ruc = "1234567-8-901234",
                status = 1,
                invoiceCustomer = 1,
                updatedAt = 123L
            ),
            finalCustomer = true,
            finalName = "John Smith",
            finalIdNumber = "8-123-456"
        ).cartCustomerDisplay()

        assertNotNull(display)
        assertTrue(display.isRegisteredCustomer)
        assertEquals("Cliente guardado", display.name)
        assertEquals("Ruc", display.identificationLabel)
        assertEquals("1234567-8-901234", display.identificationValue)
        assertNull(display.customerTypeLabel)
    }
}
