package com.teco.ventago.features.invoicing.domain

import com.teco.ventago.features.invoicing.domain.models.InvoiceStatus

const val PENDING_VERIFICATION_FALLBACK_MESSAGE: String =
    "Se detectó una intermitencia al momento de generar la factura electrónica. " +
        "El sistema intentará recuperarla automáticamente en segundo plano. " +
        "Por favor verifica el estado de la orden más tarde."

const val MANUAL_INVOICE_FALLBACK_MESSAGE: String =
    "La factura electrónica no se pudo generar. La orden fue creada exitosamente, " +
        "pero deberás generar la factura manualmente."

enum class PostCreateInvoiceConfirmationMode {
    SUCCESS,
    WARNING
}

data class PostCreateInvoiceWarningState(
    val mode: PostCreateInvoiceConfirmationMode = PostCreateInvoiceConfirmationMode.SUCCESS,
    val warningCode: String? = null,
    val warningMessage: String? = null,
    val invoiceActionsEnabled: Boolean = true,
) {
    val isWarning: Boolean
        get() = mode == PostCreateInvoiceConfirmationMode.WARNING
}

private val pendingVerificationCodes = setOf(
    "inv_001",
    "provider_api_intermittence",
)

fun resolvePostCreateInvoiceWarning(
    invoiceStatus: Int,
    invoiceWarningCode: String?,
    invoiceWarningMessage: String?,
    isImmediateInvoiceCreate: Boolean,
): PostCreateInvoiceWarningState {
    if (!isImmediateInvoiceCreate) return PostCreateInvoiceWarningState()

    val normalizedCode = invoiceWarningCode?.trim()?.takeIf { it.isNotEmpty() }
    val normalizedMessage = invoiceWarningMessage?.trim()?.takeIf { it.isNotEmpty() }

    if (normalizedCode != null && pendingVerificationCodes.contains(normalizedCode.lowercase())) {
        return PostCreateInvoiceWarningState(
            mode = PostCreateInvoiceConfirmationMode.WARNING,
            warningCode = normalizedCode,
            warningMessage = normalizedMessage ?: PENDING_VERIFICATION_FALLBACK_MESSAGE,
            invoiceActionsEnabled = false,
        )
    }

    if (normalizedMessage != null) {
        return PostCreateInvoiceWarningState(
            mode = PostCreateInvoiceConfirmationMode.WARNING,
            warningCode = normalizedCode,
            warningMessage = normalizedMessage,
            invoiceActionsEnabled = true,
        )
    }

    if (invoiceStatus == InvoiceStatus.NONE.id || invoiceStatus == InvoiceStatus.FAILED.id) {
        return PostCreateInvoiceWarningState(
            mode = PostCreateInvoiceConfirmationMode.WARNING,
            warningCode = normalizedCode,
            warningMessage = MANUAL_INVOICE_FALLBACK_MESSAGE,
            invoiceActionsEnabled = false,
        )
    }

    return PostCreateInvoiceWarningState(
        warningCode = normalizedCode,
    )
}
