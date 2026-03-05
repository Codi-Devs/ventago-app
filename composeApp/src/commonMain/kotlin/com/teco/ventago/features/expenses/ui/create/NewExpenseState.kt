package com.teco.ventago.features.expenses.ui.create

import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.features.expenses.domain.models.ExpenseAccount
import com.teco.ventago.features.expenses.domain.models.Expense
import com.teco.ventago.features.expenses.domain.models.ExpenseItem
import com.teco.ventago.features.expenses.domain.models.ExpenseMerchant

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
    val issuerAddress: String = "",
    val issuerPhone: String = "",

    // Merchant (proveedor) autocomplete
    val selectedMerchantId: Long? = null,
    val selectedMerchantName: String? = null,
    val merchantSuggestions: List<ExpenseMerchant> = emptyList(),
    val isMerchantSearching: Boolean = false,
    val saveMerchant: Boolean = false,
    val originalMerchantId: Long? = null,

    // Receiver (prefilled from business)
    val receiverName: String = "",
    val receiverRuc: String = "",
    val receiverDv: String = "",

    // Items
    val items: List<EditableExpenseItem> = listOf(EditableExpenseItem()),

    // Concepts
    val defaultExpenseAccountId: Long? = null,
    val defaultExpenseAccountName: String? = null,
    val applyConceptPerItem: Boolean = false,
    val expenseAccounts: List<ExpenseAccount> = emptyList(),
    val expenseAccountsLoading: Boolean = false,
    val conceptValidationError: String? = null,
    val hadExistingConcepts: Boolean = false,

    // Initial payments (create mode only)
    val includePayment: Boolean = false,
    val initialPayments: List<EditableInitialPayment> = listOf(EditableInitialPayment()),
    // Legacy single-payment fields kept for compatibility
    val paymentAmount: String = "",
    val paymentMethodForPayment: String = "cash",
    val paymentDate: String = "",
    val paymentDueDate: String = "",

    // Invoice file
    val fileUrl: String? = null,
    val localFileName: String? = null,
    val originalFileUrl: String? = null,
    val isUploadingFile: Boolean = false,
    val hasSelectedFile: Boolean = false,
    val initialPaymentProofName: String? = null,

    // Submission
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,

    // Feature flags
    val hasExpensesQr: Boolean = false,
    val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState()
)

data class EditableInitialPayment(
    val paymentMethod: String = "cash",
    val amount: String = "",
    val paymentDate: String = "",
    val dueDate: String = "",
    val proofFileName: String? = null
)

data class EditableExpenseItem(
    val itemId: Long? = null,
    val lineNumber: Int = 1,
    val description: String = "",
    val quantity: String = "1",
    val unitPrice: String = "",
    val discountAmount: String = "0",
    val itbmsAmount: String = "0",
    val expenseAccountId: Long? = null,
    val expenseAccountName: String? = null,
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

    fun toExpenseItem(lineNumber: Int = this.lineNumber) = ExpenseItem(
        id = itemId,
        lineNumber = lineNumber,
        description = description,
        quantity = quantity.toDoubleOrNull() ?: 0.0,
        unitPrice = unitPrice.toDoubleOrNull() ?: 0.0,
        discountAmount = discountAmount.toDoubleOrNull() ?: 0.0,
        subtotal = subtotalValue,
        itbmsAmount = itbmsAmount.toDoubleOrNull() ?: 0.0,
        total = totalValue,
        expenseAccountId = expenseAccountId
    )
}
