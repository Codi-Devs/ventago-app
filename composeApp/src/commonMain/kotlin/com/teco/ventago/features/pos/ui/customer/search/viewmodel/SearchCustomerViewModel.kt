package com.teco.ventago.features.pos.ui.customer.search.viewmodel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.features.customers.domain.CustomerService
import com.teco.ventago.features.financialProfile.domain.FinancialProfileService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchCustomerViewModel(
    private val customerService: CustomerService,
    private val financialProfileService: FinancialProfileService,
) : BaseViewModel<SearchCustomerState, SearchCustomerStateUiEvent>(SearchCustomerState()) {


    var businessId = -1

    init {
        viewModelScope.launch {
            financialProfileService.observe().onEach { profile ->
                businessId = profile?.businessId ?: -1
            }.launchIn(this)
        }
    }


    fun onSearchRucChanged(ruc: String) {
        updateState { copy(rucSearch = ruc) }
    }

    fun onSearchNameChanged(name: String) {
        updateState { copy(nameSearch = name) }
    }

    fun onSearchEmailChanged(email: String) {
        updateState { copy(emailSearch = email) }
    }

    fun seeAll() {
        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = customerService.listCustomers(
                    businessId,
                    0,
                    50,
                    null,
                    null,
                    null
                )

                if (res.items.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        hideLoading()
                        emitEvent(SearchCustomerStateUiEvent.CustomerNotFound)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        hideLoading()
                        emitEvent(SearchCustomerStateUiEvent.CustomersFound(res))
                    }
                }
            } catch (e: Exception) {
                showError()

            }
        }
    }

    fun search() {
        val state = uiState.value
        if (state.rucSearch.isBlank() && state.nameSearch.isBlank() && state.emailSearch.isBlank()) {
            return
        }

        showLoading()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = customerService.listCustomers(
                    businessId,
                    0,
                    50,
                    state.rucSearch,
                    state.emailSearch,
                    state.nameSearch
                )

                if (res.items.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        hideLoading()
                        emitEvent(SearchCustomerStateUiEvent.CustomerNotFound)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        hideLoading()
                        emitEvent(SearchCustomerStateUiEvent.CustomersFound(res))
                    }
                }
            } catch (e: Exception) {
                showError()

            }
        }
    }


}