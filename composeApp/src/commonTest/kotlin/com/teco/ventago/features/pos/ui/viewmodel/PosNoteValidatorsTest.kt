package com.teco.ventago.features.pos.ui.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PosNoteValidatorsTest {

    @Test
    fun creditNoteExceedingRemainingAmountIsBlocked() {
        val error = PosNoteValidators.validateCreditNoteAmountLimit(
            selectedDocType = "04",
            referencedNoteCUFE = "CUFE-123",
            maxCreditNoteAmountCents = 300L,
            requestedCreditNoteCents = 301L,
            sourceOrderNumber = "ORD-10"
        )

        assertNotNull(error)
    }

    @Test
    fun creditNoteEqualToRemainingAmountIsAllowed() {
        val error = PosNoteValidators.validateCreditNoteAmountLimit(
            selectedDocType = "04",
            referencedNoteCUFE = "CUFE-123",
            maxCreditNoteAmountCents = 300L,
            requestedCreditNoteCents = 300L,
            sourceOrderNumber = "ORD-10"
        )

        assertNull(error)
    }

    @Test
    fun debitNoteIsNotBlockedByCreditNoteLimit() {
        val error = PosNoteValidators.validateCreditNoteAmountLimit(
            selectedDocType = "05",
            referencedNoteCUFE = "CUFE-123",
            maxCreditNoteAmountCents = 300L,
            requestedCreditNoteCents = 500L,
            sourceOrderNumber = "ORD-10"
        )

        assertNull(error)
    }

    @Test
    fun normalInvoiceTypeOptionsExcludePresetAndUnusedNoteTypes() {
        val codes = PosDocumentTypeOptions.selectable.map { it.code }

        assertFalse("04" in codes)
        assertFalse("05" in codes)
        assertFalse("07" in codes)
        assertEquals("06", PosDocumentTypeOptions.codeAt(PosDocumentTypeOptions.indexOf("06")))
    }

    @Test
    fun removedOrInvalidDocumentTypeSelectionsFallbackToInternalInvoice() {
        assertEquals(0 to "01", PosDocumentTypeOptions.sanitizedSelection("04", 3))
        assertEquals(0 to "01", PosDocumentTypeOptions.sanitizedSelection("05", 4))
        assertEquals(0 to "01", PosDocumentTypeOptions.sanitizedSelection("07", 6))
    }

    @Test
    fun genericCreditNoteOriginalInvoiceNumberValidation() {
        assertEquals(
            PosNoteValidators.ORIGINAL_INVOICE_NUMBER_REQUIRED_MESSAGE,
            PosNoteValidators.validateOriginalInvoiceNumber("06", " ")
        )
        assertEquals(
            PosNoteValidators.ORIGINAL_INVOICE_NUMBER_MAX_LENGTH_MESSAGE,
            PosNoteValidators.validateOriginalInvoiceNumber("06", "12345678901234567890123")
        )
        assertNull(PosNoteValidators.validateOriginalInvoiceNumber("06", "1234567890123456789012"))
        assertNull(PosNoteValidators.validateOriginalInvoiceNumber("01", ""))
    }

    @Test
    fun genericCreditNoteOriginalEmissionDateValidation() {
        assertEquals(
            PosNoteValidators.ORIGINAL_INVOICE_DATE_REQUIRED_MESSAGE,
            PosNoteValidators.validateOriginalInvoiceEmissionDate("06", "")
        )
        assertNull(PosNoteValidators.validateOriginalInvoiceEmissionDate("06", "2026-07-01"))
        assertNull(PosNoteValidators.validateOriginalInvoiceEmissionDate("01", ""))
    }
}
