package com.teco.ventago.features.pos.ui.customer.list.viewmodel

import com.teco.ventago.core.LoadableState
import com.teco.ventago.core.Paged
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.features.customers.domain.models.CustomerListItem

data class ClientListState(
    val customers: Paged<CustomerListItem> = Paged(0, 0, 0, emptyList()),
    val selectedCustomer: CustomerListItem? = null,
    val canAddCustomerAction: Boolean = false,
    val invoicingEnabled: Boolean = false,
    val isLoading: Boolean = true,
    override val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
) : LoadableState<ClientListState> {
    override fun withLoading(state: LoadingBottomSheetState): ClientListState {
        return copy(loadingBottomSheet = state)
    }
}

sealed class ClientListStateUiEvent {
    data object CustomerCreated : ClientListStateUiEvent()
    data object CustomerUpdated : ClientListStateUiEvent()
}
