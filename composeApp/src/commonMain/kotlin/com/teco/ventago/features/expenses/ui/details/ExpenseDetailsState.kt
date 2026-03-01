package com.teco.ventago.features.expenses.ui.details

import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.features.expenses.domain.models.ExpenseAccount
import com.teco.ventago.features.expenses.domain.models.Expense
import com.teco.ventago.features.expenses.domain.models.ExpensePayment

data class ExpenseDetailsState(
    val expense: Expense? = null,
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val error: String? = null,
    // Payment submission
    val isSubmittingPayment: Boolean = false,
    val paymentSuccess: Boolean = false,
    val paymentError: String? = null,
    val hasExpensesQr: Boolean = false,
    val showConceptSheet: Boolean = false,
    val conceptEditorState: ExpenseConceptEditorState = ExpenseConceptEditorState(),
    val isSavingConcepts: Boolean = false,
    val conceptError: String? = null,
    val pendingOpenCategorization: Boolean = false,
    val snackbarMessage: String? = null,
    // Payment editing
    val editingPayment: ExpensePayment? = null,
    // Action feedback
    val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState()
)

data class ExpenseConceptEditorState(
    val defaultAccountId: Long? = null,
    val defaultAccountName: String? = null,
    val applyConceptPerItem: Boolean = false,
    val items: List<ExpenseConceptEditableItem> = emptyList(),
    val expenseAccounts: List<ExpenseAccount> = emptyList(),
    val isLoadingAccounts: Boolean = false
)

data class ExpenseConceptEditableItem(
    val itemId: Long? = null,
    val lineNumber: Int? = null,
    val description: String = "",
    val expenseAccountId: Long? = null,
    val expenseAccountName: String? = null
)
