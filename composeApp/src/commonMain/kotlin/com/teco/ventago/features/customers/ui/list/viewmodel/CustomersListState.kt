package com.teco.ventago.features.customers.ui.list.viewmodel

import com.teco.ventago.core.LoadableState
import com.teco.ventago.core.Paged
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.features.customers.domain.models.CustomerListItem

private val EmptyPagedCustomers = Paged(
    page = 0,
    size = 10,
    total = 0,
    items = emptyList<CustomerListItem>()
)

data class CustomersListState(
    val customers: Paged<CustomerListItem> = EmptyPagedCustomers,
    val nameFilter: String = "",
    val rucFilter: String = "",
    val emailFilter: String = "",
    val page: Int = 0,
    val pageSize: Int = 10,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    override val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
) : LoadableState<CustomersListState> {
    val canLoadMore: Boolean
        get() = customers.items.size < customers.total

    override fun withLoading(state: LoadingBottomSheetState): CustomersListState =
        copy(loadingBottomSheet = state)
}

sealed class CustomersListUiEvent {
    data class OpenCustomerDetails(val customerId: Long) : CustomersListUiEvent()
}
