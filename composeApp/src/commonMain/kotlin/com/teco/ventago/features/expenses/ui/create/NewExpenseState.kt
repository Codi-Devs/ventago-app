package com.teco.ventago.features.expenses.ui.create

import com.teco.ventago.features.expenses.domain.models.Expense
import com.teco.ventago.features.expenses.domain.models.ExpenseItem

data class NewExpenseState(
    // Mode
    val isEditMode: Boolean = false,
    val editingExpenseId: Long? = null,

    // Form fields
    val invoiceNumber: String = "",
    val cufe: String = "",
    val emissionDate: String = "",
    val paymentMethod: String = "",
    val notes: String = "",

    // Issuer
    val issuerName: String = "",
    val issuerRuc: String = "",
    val issuerDv: String = "",

    // Receiver (prefilled from business)
    val receiverName: String = "",
    val receiverRuc: String = "",
    val receiverDv: String = "",

    // Items
    val items: List<EditableExpenseItem> = listOf(EditableExpenseItem()),

    // Initial payment (create mode only)
    val includePayment: Boolean = false,
    val paymentAmount: String = "",
    val paymentMethodForPayment: String = "cash",
    val paymentDate: String = "",
    val paymentDueDate: String = "",

    // Invoice file
    val fileUrl: String? = null,
    val originalFileUrl: String? = null,
    val isUploadingFile: Boolean = false,
    val hasSelectedFile: Boolean = false,

    // Submission
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

data class EditableExpenseItem(
    val description: String = "",
    val quantity: String = "1",
    val unitPrice: String = "",
    val discountAmount: String = "0",
    val itbmsAmount: String = "0",
) {
    val subtotalValue: Double
        get() {
            val qty = quantity.toDoubleOrNull() ?: 0.0
            val price = unitPrice.toDoubleOrNull() ?: 0.0
            val discount = discountAmount.toDoubleOrNull() ?: 0.0
            return (qty * price) - discount
        }

    val totalValue: Double
        get() = subtotalValue + (itbmsAmount.toDoubleOrNull() ?: 0.0)

    fun toExpenseItem(lineNumber: Int) = ExpenseItem(
        lineNumber = lineNumber,
        description = description,
        quantity = quantity.toDoubleOrNull() ?: 0.0,
        unitPrice = unitPrice.toDoubleOrNull() ?: 0.0,
        discountAmount = discountAmount.toDoubleOrNull() ?: 0.0,
        subtotal = subtotalValue,
        itbmsAmount = itbmsAmount.toDoubleOrNull() ?: 0.0,
        total = totalValue
    )
}
