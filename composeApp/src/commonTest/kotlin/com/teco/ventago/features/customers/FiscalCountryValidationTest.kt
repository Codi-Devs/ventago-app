package com.teco.ventago.features.customers

import com.teco.ventago.features.customers.domain.models.FiscalCountryValidation
import com.teco.ventago.features.customers.ui.form.viewmodel.CustomerFormState
import com.teco.ventago.features.orders.domain.models.InvoiceRetryMetadata
import com.teco.ventago.features.orders.domain.models.requests.RetryInvoiceRequest
import com.teco.ventago.features.pos.ui.customer.add.viewmodel.AddCustomerState
import com.teco.ventago.features.pos.ui.viewmodel.PosState
import com.teco.ventago.features.pos.ui.viewmodel.posFiscalCountryError
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FiscalCountryValidationTest {
    @Test fun foreignCountryMustBeSelectedFromCatalog() {
        listOf(null, "", "PA", " pa ", "ZZ").forEach {
            assertNotNull(FiscalCountryValidation.error(it, foreignReceiver = true))
        }
        assertNull(FiscalCountryValidation.error(" us ", foreignReceiver = true))
        assertEquals("US", FiscalCountryValidation.normalize(" us "))
        assertEquals("", CustomerFormState().selectedCountryCode)
        assertEquals("", AddCustomerState().selectedCountryCode)
    }

    @Test fun restoredFinalCustomerUsesEffectiveCountryAndDestination() {
        val foreign = PosState(finalCustomer = true, finalIdType = "passport", finalCustomerCountryCode = "CO", selectedDocType = "10")
        assertNull(posFiscalCountryError(foreign))
        assertNotNull(posFiscalCountryError(foreign.copy(finalCustomerCountryCode = "PA")))
        assertNotNull(posFiscalCountryError(foreign.copy(finalCustomerCountryCode = null)))
        assertNotNull(posFiscalCountryError(foreign.copy(finalIdType = "cedula", finalCustomerCountryCode = "US")))
        assertNull(posFiscalCountryError(foreign.copy(finalIdType = "cedula", selectedDocType = "01")))
    }

    @Test fun registeredForeignCustomerCountryIsRevalidated() {
        val state = PosState(finalCustomer = false, selectedCustomerFeCustomerType = "04", selectedCustomerFiscalCountry = "PA", selectedCustomerFiscalCountryLoaded = true)
        assertNotNull(posFiscalCountryError(state))
        assertNull(posFiscalCountryError(state.copy(selectedCustomerFiscalCountry = "US")))
        assertNotNull(posFiscalCountryError(state.copy(selectedCustomerFeCustomerType = "01", selectedDocType = "10")))
    }

    @Test fun retryMetadataAndCorrectionAreMinimalTypedPayloads() {
        val metadata = Json.decodeFromString<InvoiceRetryMetadata>("""{"country_correction_required":true,"final_customer_country_code":"PA","final_customer_foreign":true}""")
        assertEquals("PA", metadata.finalCustomerCountryCode)
        assertEquals(true, metadata.countryCorrectionRequired)
        assertEquals("""{"final_customer_country_code":"US"}""", Json.encodeToString(RetryInvoiceRequest("US")))
        assertEquals("{}", Json.encodeToString(RetryInvoiceRequest()))
    }
}
