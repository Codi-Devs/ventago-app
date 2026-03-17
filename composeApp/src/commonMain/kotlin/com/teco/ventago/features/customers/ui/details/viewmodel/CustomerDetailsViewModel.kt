package com.teco.ventago.features.customers.ui.details.viewmodel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.core.authz.ActionKey
import com.teco.ventago.core.authz.AuthzEvaluator
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.customers.domain.CustomerService
import com.teco.ventago.features.customers.domain.models.CreateBillingAddressRequest
import com.teco.ventago.features.customers.domain.models.UpdateBillingAddressRequest
import com.teco.ventago.features.orders.domain.OrderService
import com.teco.ventago.features.financialProfile.domain.FinancialProfileService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CustomerDetailsViewModel(
    private val authService: IAuthService,
    private val customerService: CustomerService,
    private val orderService: OrderService,
    private val financialProfileService: FinancialProfileService,
) : BaseViewModel<CustomerDetailsState, CustomerDetailsUiEvent>(CustomerDetailsState()) {

    private var businessId: Int = -1
    private var pendingCustomerId: Long? = null

    init {
        viewModelScope.launch {
            authService.getUser().onEach { user ->
                val canEditCustomerAction =
                    AuthzEvaluator.canAction(ActionKey.CUSTOMERS_CREATE, user, emptySet())
                val canDeleteCustomerAction =
                    AuthzEvaluator.canAction(ActionKey.CUSTOMERS_DELETE, user, emptySet())
                updateState {
                    copy(
                        canEditCustomerAction = canEditCustomerAction,
                        canDeleteCustomerAction = canDeleteCustomerAction
                    )
                }
            }.launchIn(this)
        }

        viewModelScope.launch {
            financialProfileService.observe().onEach { profile ->
                val newBusinessId = profile?.businessId ?: -1
                if (newBusinessId <= 0) return@onEach
                businessId = newBusinessId
                pendingCustomerId?.let { customerId ->
                    if (uiState.value.customer?.id != customerId || uiState.value.errorMessage != null) {
                        loadInternal(customerId)
                    }
                }
            }.launchIn(this)
        }
    }

    fun load(customerId: Long) {
        pendingCustomerId = customerId
        if (businessId <= 0) {
            updateState {
                copy(
                    customerId = customerId,
                    isLoading = true,
                    errorMessage = null
                )
            }
            return
        }
        loadInternal(customerId)
    }

    private fun loadInternal(customerId: Long) {
        viewModelScope.launch {
            updateState {
                copy(
                    customerId = customerId,
                    isLoading = true,
                    errorMessage = null,
                )
            }

            runCatching {
                withContext(Dispatchers.Default) {
                    val customer = customerService.getCustomerById(businessId, customerId)
                    val addresses = customerService.listCustomerAddresses(
                        businessId = businessId,
                        invoiceCustomerId = customerId.toInt()
                    )
                    val recentOrdersPaged = orderService.listOrdersForCustomerPaged(
                        businessId = businessId,
                        customerId = customerId,
                        pageSize = 5,
                        page = 0
                    )

                    Triple(customer, addresses, recentOrdersPaged)
                }
            }.onSuccess { (customer, addresses, recentOrdersPaged) ->
                val recentAmount = recentOrdersPaged.items.sumOf { order ->
                    order.totalAmount.replace(",", ".").toDoubleOrNull() ?: 0.0
                }

                updateState {
                    copy(
                        customer = customer,
                        addresses = addresses,
                        recentOrders = recentOrdersPaged.items,
                        ordersCount = recentOrdersPaged.total,
                        recentOrdersAmount = recentAmount,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            }.onFailure {
                updateState {
                    copy(
                        isLoading = false,
                        errorMessage = it.message ?: "Failed to load customer"
                    )
                }
            }
        }
    }

    fun reloadAddresses() {
        val customerId = uiState.value.customerId ?: return
        if (businessId <= 0) return

        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.Default) {
                    customerService.listCustomerAddresses(businessId, customerId.toInt())
                }
            }.onSuccess { addresses ->
                updateState { copy(addresses = addresses) }
            }
        }
    }

    fun deleteCustomer() {
        if (!uiState.value.canDeleteCustomerAction) return
        val customerId = uiState.value.customerId ?: return
        if (businessId <= 0) return

        showLoading()
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.Default) {
                    customerService.deleteCustomer(businessId, customerId)
                }
            }.onSuccess {
                showSuccess()
                delay(900)
                emitEvent(CustomerDetailsUiEvent.CustomerDeleted)
            }.onFailure {
                showError()
            }
        }
    }

    fun createAddress(addressLine: String, locationCode: String?, email: String?) {
        if (!uiState.value.canEditCustomerAction) return
        val customerId = uiState.value.customerId ?: return
        if (businessId <= 0) return
        val normalizedEmail = email?.trim()?.takeIf { it.isNotEmpty() }

        showLoading()
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.Default) {
                    customerService.createCustomerAddress(
                        businessId = businessId,
                        customerId = customerId,
                        request = CreateBillingAddressRequest(
                            addressLine = addressLine,
                            locationCode = locationCode,
                            email = normalizedEmail,
                        )
                    )
                }
            }.onSuccess {
                showSuccess()
                reloadAddresses()
                emitEvent(CustomerDetailsUiEvent.AddressMutationSuccess)
            }.onFailure {
                showError()
            }
        }
    }

    fun updateAddress(addressId: Long, addressLine: String, locationCode: String?, email: String?) {
        if (!uiState.value.canEditCustomerAction) return
        val customerId = uiState.value.customerId ?: return
        if (businessId <= 0) return
        val normalizedEmail = email?.trim()?.takeIf { it.isNotEmpty() }

        showLoading()
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.Default) {
                    customerService.updateCustomerAddress(
                        businessId = businessId,
                        customerId = customerId,
                        addressId = addressId,
                        request = UpdateBillingAddressRequest(
                            addressLine = addressLine,
                            locationCode = locationCode,
                            email = normalizedEmail,
                        )
                    )
                }
            }.onSuccess {
                showSuccess()
                reloadAddresses()
                emitEvent(CustomerDetailsUiEvent.AddressMutationSuccess)
            }.onFailure {
                showError()
            }
        }
    }

    fun deleteAddress(addressId: Long) {
        if (!uiState.value.canEditCustomerAction) return
        val customerId = uiState.value.customerId ?: return
        if (businessId <= 0) return

        showLoading()
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.Default) {
                    customerService.deleteCustomerAddress(
                        businessId = businessId,
                        customerId = customerId,
                        addressId = addressId,
                    )
                }
            }.onSuccess {
                showSuccess()
                reloadAddresses()
            }.onFailure {
                showError()
            }
        }
    }
}
