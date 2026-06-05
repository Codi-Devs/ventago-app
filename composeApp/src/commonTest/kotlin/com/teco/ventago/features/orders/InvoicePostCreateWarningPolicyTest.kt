package com.teco.ventago.features.orders

import com.teco.ventago.features.invoicing.domain.MANUAL_INVOICE_FALLBACK_MESSAGE
import com.teco.ventago.features.invoicing.domain.PENDING_VERIFICATION_FALLBACK_MESSAGE
import com.teco.ventago.features.invoicing.domain.PostCreateInvoiceConfirmationMode
import com.teco.ventago.features.invoicing.domain.resolvePostCreateInvoiceWarning
import com.teco.ventago.features.invoicing.domain.models.InvoiceStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InvoicePostCreateWarningPolicyTest {

    @Test
    fun pendingCodesAreMatchedCaseInsensitively() {
        val result = resolvePostCreateInvoiceWarning(
            invoiceStatus = InvoiceStatus.PENDING.id,
            invoiceWarningCode = "INV_001",
            invoiceWarningMessage = "Pendiente por proveedor",
            isImmediateInvoiceCreate = true
        )

        assertEquals(PostCreateInvoiceConfirmationMode.WARNING, result.mode)
        assertEquals("Pendiente por proveedor", result.warningMessage)
        assertFalse(result.invoiceActionsEnabled)
    }

    @Test
    fun pendingCodeWithoutMessageUsesPendingFallback() {
        val result = resolvePostCreateInvoiceWarning(
            invoiceStatus = InvoiceStatus.PENDING.id,
            invoiceWarningCode = "provider_api_intermittence",
            invoiceWarningMessage = "   ",
            isImmediateInvoiceCreate = true
        )

        assertEquals(PENDING_VERIFICATION_FALLBACK_MESSAGE, result.warningMessage)
        assertFalse(result.invoiceActionsEnabled)
    }

    @Test
    fun nonPendingWarningMessageKeepsInvoiceActionsEnabled() {
        val result = resolvePostCreateInvoiceWarning(
            invoiceStatus = InvoiceStatus.FAILED.id,
            invoiceWarningCode = "buyer_ruc_invalid",
            invoiceWarningMessage = "No se pudo crear la factura porque el comprador es inválido.",
            isImmediateInvoiceCreate = true
        )

        assertTrue(result.isWarning)
        assertEquals("No se pudo crear la factura porque el comprador es inválido.", result.warningMessage)
        assertTrue(result.invoiceActionsEnabled)
    }

    @Test
    fun missingWarningFieldsAndStatusNoneUsesManualFallback() {
        val result = resolvePostCreateInvoiceWarning(
            invoiceStatus = InvoiceStatus.NONE.id,
            invoiceWarningCode = null,
            invoiceWarningMessage = null,
            isImmediateInvoiceCreate = true
        )

        assertTrue(result.isWarning)
        assertEquals(MANUAL_INVOICE_FALLBACK_MESSAGE, result.warningMessage)
        assertFalse(result.invoiceActionsEnabled)
    }

    @Test
    fun cleanIssuedImmediateCreateStaysSuccess() {
        val result = resolvePostCreateInvoiceWarning(
            invoiceStatus = InvoiceStatus.ISSUED.id,
            invoiceWarningCode = null,
            invoiceWarningMessage = null,
            isImmediateInvoiceCreate = true
        )

        assertEquals(PostCreateInvoiceConfirmationMode.SUCCESS, result.mode)
        assertTrue(result.invoiceActionsEnabled)
        assertEquals(null, result.warningMessage)
    }
}
