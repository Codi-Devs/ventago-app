package com.teco.ventago.features.customers

import com.teco.ventago.features.customers.domain.models.FiscalCountryValidation
import com.teco.ventago.features.customers.ui.form.viewmodel.CustomerFormState
import com.teco.ventago.features.orders.domain.models.InvoiceRetryMetadata
import com.teco.ventago.features.orders.domain.models.requests.RetryInvoiceRequest
import com.teco.ventago.features.pos.ui.customer.add.viewmodel.AddCustomerState
import com.teco.ventago.features.customers.domain.models.CustomerListItem
import com.teco.ventago.features.pos.ui.viewmodel.PaymentFlowMode
import com.teco.ventago.features.pos.ui.viewmodel.startFiscalCustomerHydration
import com.teco.ventago.features.pos.ui.viewmodel.failFiscalCustomerHydration
import com.teco.ventago.features.pos.ui.viewmodel.completeFiscalCustomerHydration
import com.teco.ventago.features.pos.ui.viewmodel.PosState
import com.teco.ventago.features.pos.ui.viewmodel.fiscalHydrationNeedsKickoff
import com.teco.ventago.features.pos.ui.viewmodel.fiscalIssueWaitingOnHydration
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

    private fun registeredState() = PosState(
        finalCustomer = false,
        customer = CustomerListItem(id = 15L, name = "Cliente", status = 1, invoiceCustomer = 1, updatedAt = 0),
        selectedDocType = "10",
        retentionAmount = "700",
    )

    @Test fun fiscalHydrationFailureCanRetryAndSucceedWithoutReselectingCustomer() {
        val original = registeredState()
        val loading = original.startFiscalCustomerHydration(15)
        assertEquals(true, loading.selectedCustomerFiscalCountryLoading)
        assertNotNull(posFiscalCountryError(loading))
        val failed = loading.failFiscalCustomerHydration(15)
        assertEquals(false, failed.selectedCustomerFiscalCountryLoading)
        assertEquals(failed.selectedCustomerFiscalCountryError, posFiscalCountryError(failed))
        val retrying = failed.startFiscalCustomerHydration(15)
        assertNull(retrying.selectedCustomerFiscalCountryError)
        val ready = retrying.completeFiscalCustomerHydration(15, "04", " us ")
        assertNull(posFiscalCountryError(ready))
        assertEquals("US", ready.selectedCustomerFiscalCountry)
        assertEquals(original.customer, ready.customer)
        assertEquals(original.retentionAmount, ready.retentionAmount)
    }

    @Test fun obsoleteHydrationResultDoesNotMutateCurrentCustomer() {
        val selected = registeredState()
        assertEquals(selected, selected.failFiscalCustomerHydration(14))
        assertEquals(selected, selected.completeFiscalCustomerHydration(14, "04", "PA"))
    }

    @Test fun waitingForFiscalDataRetriesUntilItStartsOrFails() {
        val waiting = registeredState()
        assertEquals(true, fiscalHydrationNeedsKickoff(waiting))
        assertEquals(true, fiscalIssueWaitingOnHydration(waiting, saveAsDraft = false))
        assertEquals(false, fiscalIssueWaitingOnHydration(waiting, saveAsDraft = true))

        val loading = waiting.startFiscalCustomerHydration(15)
        assertEquals(false, fiscalHydrationNeedsKickoff(loading))
        assertEquals(true, fiscalIssueWaitingOnHydration(loading, saveAsDraft = false))

        val failed = loading.failFiscalCustomerHydration(15)
        assertEquals(false, fiscalHydrationNeedsKickoff(failed))
        assertEquals(false, fiscalIssueWaitingOnHydration(failed, saveAsDraft = false))

        val ready = waiting.completeFiscalCustomerHydration(15, "01", "PA")
        assertEquals(false, fiscalHydrationNeedsKickoff(ready))
        assertEquals(false, fiscalIssueWaitingOnHydration(ready, saveAsDraft = false))
    }

    @Test fun draftsAndInternalDocumentsDoNotRequireFiscalHydration() {
        val loading = registeredState().startFiscalCustomerHydration(15)
        val failed = loading.failFiscalCustomerHydration(15)
        for (state in listOf(loading, failed)) {
            assertNotNull(posFiscalCountryError(state))
            assertNull(posFiscalCountryError(state, saveAsDraft = true))
            assertNull(posFiscalCountryError(state.copy(internalDocument = true)))
            assertNull(posFiscalCountryError(state.copy(paymentFlowMode = PaymentFlowMode.NON_FISCAL)))
        }
        val invalidCountry = PosState(finalCustomer = true, finalIdType = "passport", finalCustomerCountryCode = "PA", selectedDocType = "10")
        assertNotNull(posFiscalCountryError(invalidCountry))
        assertNull(posFiscalCountryError(invalidCountry, saveAsDraft = true))
        assertNull(posFiscalCountryError(invalidCountry.copy(internalDocument = true)))
    }
}
