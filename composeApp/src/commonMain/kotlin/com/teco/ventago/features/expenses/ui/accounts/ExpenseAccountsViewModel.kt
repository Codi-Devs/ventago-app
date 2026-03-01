package com.teco.ventago.features.expenses.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.design_system.organism.LoadingState
import com.teco.ventago.features.expenses.domain.ExpensesService
import com.teco.ventago.features.expenses.domain.models.ExpenseAccount
import com.teco.ventago.features.expenses.domain.models.requests.CreateExpenseAccountRequest
import com.teco.ventago.features.expenses.domain.models.requests.UpdateExpenseAccountRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExpenseAccountsViewModel(
    private val expensesService: ExpensesService
) : ViewModel() {

    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(ExpenseAccountsState())
    val uiState: kotlinx.coroutines.flow.StateFlow<ExpenseAccountsState> = _uiState

    init {
        loadAccounts(initial = true)
    }

    fun loadAccounts(initial: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingInitial = initial,
                isRefreshing = !initial
            )
            try {
                val accounts = withContext(kotlinx.coroutines.Dispatchers.Default) {
                    expensesService.getExpenseAccounts(includeInactive = _uiState.value.showInactive)
                }
                _uiState.value = _uiState.value.copy(
                    accounts = accounts.sortedBy { it.name.lowercase() },
                    isLoadingInitial = false,
                    isRefreshing = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingInitial = false,
                    isRefreshing = false,
                    formError = e.message ?: "No se pudieron cargar los conceptos de gasto."
                )
            }
        }
    }

    fun setShowInactive(show: Boolean) {
        _uiState.value = _uiState.value.copy(showInactive = show)
        loadAccounts(initial = false)
    }

    fun startCreate() {
        _uiState.value = ExpenseAccountsState(
            accounts = _uiState.value.accounts,
            showInactive = _uiState.value.showInactive,
            showFormSheet = true
        )
    }

    fun startEdit(account: ExpenseAccount) {
        _uiState.value = _uiState.value.copy(
            selectedAccount = account,
            showFormSheet = true,
            isEditMode = true,
            formCode = account.code,
            formName = account.name,
            formParentId = account.parentId,
            formParentName = _uiState.value.accounts.firstOrNull { it.id == account.parentId }?.name,
            formIsActive = account.isActive,
            formError = null
        )
    }

    fun dismissForm() {
        _uiState.value = _uiState.value.copy(
            showFormSheet = false,
            isEditMode = false,
            selectedAccount = null,
            formCode = "",
            formName = "",
            formParentId = null,
            formParentName = null,
            formIsActive = true,
            formError = null
        )
    }

    fun setFormCode(value: String) { _uiState.value = _uiState.value.copy(formCode = value, formError = null) }
    fun setFormName(value: String) { _uiState.value = _uiState.value.copy(formName = value, formError = null) }
    fun setFormParent(accountId: Long?, accountName: String?) {
        _uiState.value = _uiState.value.copy(
            formParentId = accountId,
            formParentName = accountName,
            formError = null
        )
    }
    fun setFormActive(active: Boolean) { _uiState.value = _uiState.value.copy(formIsActive = active) }

    fun save() {
        val state = _uiState.value
        if (state.formName.isBlank()) {
            _uiState.value = state.copy(formError = "El nombre es obligatorio.")
            return
        }
        if (!state.isEditMode && state.formCode.isBlank()) {
            _uiState.value = state.copy(formError = "El codigo es obligatorio.")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(
                loadingBottomSheet = LoadingBottomSheetState(LoadingState.LOADING, "Guardando concepto...")
            )
            try {
                withContext(kotlinx.coroutines.Dispatchers.Default) {
                    if (state.isEditMode) {
                        val accountId = state.selectedAccount?.id ?: error("Missing account")
                        expensesService.updateExpenseAccount(
                            accountId = accountId,
                            request = UpdateExpenseAccountRequest(
                                name = state.formName,
                                isActive = state.formIsActive
                            )
                        )
                    } else {
                        expensesService.createExpenseAccount(
                            CreateExpenseAccountRequest(
                                code = state.formCode,
                                name = state.formName,
                                parentId = state.formParentId
                            )
                        )
                    }
                }
                _uiState.value = _uiState.value.copy(
                    loadingBottomSheet = LoadingBottomSheetState(
                        LoadingState.SUCCESS,
                        "Concepto guardado correctamente."
                    )
                )
                dismissForm()
                loadAccounts(initial = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    formError = e.message ?: "No se pudo guardar el concepto.",
                    loadingBottomSheet = LoadingBottomSheetState(
                        LoadingState.ERROR,
                        "No se pudo guardar el concepto."
                    )
                )
            }
        }
    }

    fun promptDeactivate(account: ExpenseAccount) {
        _uiState.value = _uiState.value.copy(
            selectedAccount = account,
            showDeactivateDialog = true
        )
    }

    fun dismissDeactivateDialog() {
        _uiState.value = _uiState.value.copy(showDeactivateDialog = false)
    }

    fun deactivateSelectedAccount() {
        val account = _uiState.value.selectedAccount ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                showDeactivateDialog = false,
                loadingBottomSheet = LoadingBottomSheetState(LoadingState.LOADING, "Desactivando concepto...")
            )
            try {
                withContext(kotlinx.coroutines.Dispatchers.Default) {
                    expensesService.deactivateExpenseAccount(account.id)
                }
                _uiState.value = _uiState.value.copy(
                    loadingBottomSheet = LoadingBottomSheetState(
                        LoadingState.SUCCESS,
                        "Concepto desactivado correctamente."
                    )
                )
                loadAccounts(initial = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    formError = e.message ?: "No se pudo desactivar el concepto.",
                    loadingBottomSheet = LoadingBottomSheetState(
                        LoadingState.ERROR,
                        "No se pudo desactivar el concepto."
                    )
                )
            }
        }
    }

    fun hideLoading() {
        _uiState.value = _uiState.value.copy(loadingBottomSheet = LoadingBottomSheetState())
    }
}
