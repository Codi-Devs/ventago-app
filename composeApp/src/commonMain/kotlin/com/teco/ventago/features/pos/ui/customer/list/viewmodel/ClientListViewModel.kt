package com.teco.ventago.features.pos.ui.customer.list.viewmodel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.features.customers.domain.CustomerService
import com.teco.ventago.features.financialProfile.domain.FinancialProfileService
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ClientListViewModel(
    private val financialProfileService: FinancialProfileService,
    private val customerService: CustomerService,
    private val loggerService: ILoggerService,
) : BaseViewModel<ClientListState, ClientListViewModel>(ClientListState()) {

    var businessId = -1

    init {
        viewModelScope.launch {
            financialProfileService.observe().onEach {
                businessId = it?.businessId ?: -1
                updateState {
                    copy(
                        invoicingEnabled = it?.invoicingActive == true,
                    )
                }
            }.launchIn(this)

            customerService.observe().onEach { pagedCustomers ->
                updateState {
                    copy(
                        customers = pagedCustomers,
                        isLoading = false,
                    )
                }
            }.launchIn(this)
        }
    }

    fun onCustomerSelected(customer: ClientListState) {
        updateState {
            copy(
                selectedCustomer = customer.selectedCustomer,
            )
        }
    }
}