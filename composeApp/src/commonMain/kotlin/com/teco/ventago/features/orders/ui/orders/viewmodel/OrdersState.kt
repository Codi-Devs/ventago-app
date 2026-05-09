package com.teco.ventago.features.orders.ui.orders.viewmodel

import com.teco.ventago.core.LoadableState
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.features.orders.domain.models.Order

data class OrdersState(
    val orders: List<Order> = emptyList(),
    val isLoadingOrders: Boolean = false,
    val refreshingOrder: Boolean = false,
    val noMoreOrders: Boolean = false,
    val selectedChip: Int = 0,
    val filterSelected: Int = 0,
    val ordersEnabled: Boolean = true,
    val hasQuotesAccess: Boolean = false,
    val canCreateOrderEntry: Boolean = false,
    val paymentStatusFilter: Int? = null,
    val customerIdFilter: Long? = null,
    val orderTypeFilter: String? = null,
    val customerRucFilter: String = "",
    val emissionStartDate: String = "",
    val emissionEndDate: String = "",
    val showScanner: Boolean = false,
    val showPermissionRationalDialog: Boolean = false,

    override val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
) : LoadableState<OrdersState> {

    override fun withLoading(state: LoadingBottomSheetState): OrdersState {
        return copy(loadingBottomSheet = state)
    }
}

sealed class OrdersUiEvent {
    data object OpenOrderDetails: OrdersUiEvent()
    data object LoadingOrdersError: OrdersUiEvent()
    data object LoadingOrdersConnectionError: OrdersUiEvent()
    data object LaunchSettings: OrdersUiEvent()
}
