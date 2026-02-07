package com.teco.ventago.features.expenses.ui.details

import com.teco.ventago.design_system.organism.LoadingBottomSheetState
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
    // Payment editing
    val editingPayment: ExpensePayment? = null,
    // Action feedback
    val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState()
)
