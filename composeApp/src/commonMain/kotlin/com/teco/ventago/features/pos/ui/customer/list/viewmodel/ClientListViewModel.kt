package com.teco.ventago.features.pos.ui.customer.list.viewmodel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.core.authz.ActionKey
import com.teco.ventago.core.authz.AuthzEvaluator
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.customers.domain.CustomerService
import com.teco.ventago.features.financialProfile.domain.FinancialProfileService
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ClientListViewModel(
    private val authService: IAuthService,
    private val financialProfileService: FinancialProfileService,
    private val customerService: CustomerService,
    private val loggerService: ILoggerService,
) : BaseViewModel<ClientListState, ClientListViewModel>(ClientListState()) {

    var businessId = -1

    init {
        viewModelScope.launch {
            authService.getUser().onEach { user ->
                val canAddCustomerAction =
                    AuthzEvaluator.canAction(ActionKey.CUSTOMERS_CREATE, user, emptySet()) ||
                        AuthzEvaluator.canAction(ActionKey.CUSTOMERS_DELETE, user, emptySet())
                updateState { copy(canAddCustomerAction = canAddCustomerAction) }
            }.launchIn(this)

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
