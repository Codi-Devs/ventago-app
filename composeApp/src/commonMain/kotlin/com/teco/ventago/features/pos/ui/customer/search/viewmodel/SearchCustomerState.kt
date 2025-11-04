package com.teco.ventago.features.pos.ui.customer.search.viewmodel

import com.teco.ventago.core.LoadableState
import com.teco.ventago.core.Paged
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.features.customers.domain.models.CustomerListItem

data class SearchCustomerState(
    val rucSearch: String = "",
    val nameSearch: String = "",
    val emailSearch: String = "",
    override val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
) : LoadableState<SearchCustomerState>{
    override fun withLoading(state: LoadingBottomSheetState): SearchCustomerState {
        return copy(loadingBottomSheet = state)
    }
}

sealed class SearchCustomerStateUiEvent {
    data object RucNotFound: SearchCustomerStateUiEvent()
    data class CustomersFound(val customers: Paged<CustomerListItem>): SearchCustomerStateUiEvent()
    data object CustomerNotFound: SearchCustomerStateUiEvent()
    data object CustomerNotCreated: SearchCustomerStateUiEvent()
}