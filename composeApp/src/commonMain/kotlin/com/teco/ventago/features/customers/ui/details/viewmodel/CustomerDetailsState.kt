package com.teco.ventago.features.customers.ui.details.viewmodel

import com.teco.ventago.core.LoadableState
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.features.customers.domain.models.CustomerAddress
import com.teco.ventago.features.customers.domain.models.CustomerDetails
import com.teco.ventago.features.orders.domain.models.Order

data class CustomerDetailsState(
    val customerId: Long? = null,
    val customer: CustomerDetails? = null,
    val canEditCustomerAction: Boolean = false,
    val canDeleteCustomerAction: Boolean = false,
    val addresses: List<CustomerAddress> = emptyList(),
    val recentOrders: List<Order> = emptyList(),
    val ordersCount: Long = 0,
    val recentOrdersAmount: Double = 0.0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    override val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
) : LoadableState<CustomerDetailsState> {
    override fun withLoading(state: LoadingBottomSheetState): CustomerDetailsState =
        copy(loadingBottomSheet = state)
}

sealed class CustomerDetailsUiEvent {
    data object CustomerDeleted : CustomerDetailsUiEvent()
    data object AddressMutationSuccess : CustomerDetailsUiEvent()
}
