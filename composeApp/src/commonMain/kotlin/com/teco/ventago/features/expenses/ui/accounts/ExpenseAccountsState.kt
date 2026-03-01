package com.teco.ventago.features.expenses.ui.accounts

import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.features.expenses.domain.models.ExpenseAccount

data class ExpenseAccountsState(
    val accounts: List<ExpenseAccount> = emptyList(),
    val showInactive: Boolean = false,
    val isLoadingInitial: Boolean = false,
    val isRefreshing: Boolean = false,
    val selectedAccount: ExpenseAccount? = null,
    val showFormSheet: Boolean = false,
    val isEditMode: Boolean = false,
    val formCode: String = "",
    val formName: String = "",
    val formParentId: Long? = null,
    val formParentName: String? = null,
    val formIsActive: Boolean = true,
    val formError: String? = null,
    val showDeactivateDialog: Boolean = false,
    val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState()
)
