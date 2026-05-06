package com.teco.ventago.features.customers.ui.list.viewmodel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.core.authz.ActionKey
import com.teco.ventago.core.authz.AuthzEvaluator
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.customers.domain.CustomerService
import com.teco.ventago.features.financialProfile.domain.FinancialProfileService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CustomersListViewModel(
    private val authService: IAuthService,
    private val customerService: CustomerService,
    private val financialProfileService: FinancialProfileService,
) : BaseViewModel<CustomersListState, CustomersListUiEvent>(CustomersListState()) {

    private var businessId: Int = -1

    init {
        viewModelScope.launch {
            authService.getUser().onEach { user ->
                val canAddCustomerAction =
                    AuthzEvaluator.canAction(ActionKey.CUSTOMERS_CREATE, user, emptySet()) ||
                        AuthzEvaluator.canAction(ActionKey.CUSTOMERS_DELETE, user, emptySet())
                updateState { copy(canAddCustomerAction = canAddCustomerAction) }
            }.launchIn(this)
        }

        viewModelScope.launch {
            financialProfileService.observe().onEach { profile ->
                val newBusinessId = profile?.businessId ?: -1
                updateState {
                    copy(
                        invoicingEnabled = profile?.invoicingActive == true,
                    )
                }
                if (newBusinessId <= 0) return@onEach
                if (businessId == newBusinessId && uiState.value.customers.items.isNotEmpty()) return@onEach

                businessId = newBusinessId
                loadCustomers(reset = true)
            }.launchIn(this)
        }

        viewModelScope.launch {
            customerService.observe().onEach { customers ->
                updateState {
                    copy(
                        customers = customers,
                        page = customers.page + 1
                    )
                }
            }.launchIn(this)
        }
    }

    fun setNameFilter(value: String) = updateState { copy(nameFilter = value) }
    fun setRucFilter(value: String) = updateState { copy(rucFilter = value) }
    fun setEmailFilter(value: String) = updateState { copy(emailFilter = value) }

    fun clearFilters() {
        updateState {
            copy(
                nameFilter = "",
                rucFilter = "",
                emailFilter = "",
                page = 0
            )
        }
        loadCustomers(reset = true)
    }

    fun applyFilters() {
        updateState { copy(page = 0) }
        loadCustomers(reset = true)
    }

    fun loadCustomers(reset: Boolean = false) {
        if (businessId <= 0) return
        val state = uiState.value
        if (!reset && (state.isLoading || !state.canLoadMore)) return

        val targetPage = if (reset) 0 else state.page

        viewModelScope.launch {
            updateState {
                copy(
                    isLoading = true,
                    isRefreshing = reset,
                    errorMessage = null
                )
            }

            runCatching {
                withContext(Dispatchers.Default) {
                    customerService.listCustomers(
                        businessId = businessId,
                        page = targetPage,
                        size = state.pageSize,
                        ruc = state.rucFilter.ifBlank { null },
                        email = state.emailFilter.ifBlank { null },
                        name = state.nameFilter.ifBlank { null },
                    )
                }
            }.onSuccess { response ->
                updateState {
                    copy(
                        customers = response,
                        page = response.page + 1,
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = null,
                    )
                }
            }.onFailure { throwable ->
                updateState {
                    copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = throwable.message ?: "Error loading customers"
                    )
                }
            }
        }
    }

    fun openCustomer(customerId: Long) {
        viewModelScope.launch {
            emitEvent(CustomersListUiEvent.OpenCustomerDetails(customerId))
        }
    }
}
