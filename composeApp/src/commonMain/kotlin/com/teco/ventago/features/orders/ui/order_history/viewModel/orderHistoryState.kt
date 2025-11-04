package com.teco.ventago.features.orders.ui.order_history.viewModel

import com.teco.ventago.core.LoadableState
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.orders.ui.order_details.viewModel.OrderDetailsState

data class OrderHistoryState(
    val order: Order? = null,

    val changingOrderError: Boolean = false,
    val showRejectDialog: Boolean = false,
    val showReasonDialog: Boolean = false,

    override val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
): LoadableState<OrderHistoryState> {
    override fun withLoading(state: LoadingBottomSheetState): OrderHistoryState {
        return copy(loadingBottomSheet = state)
    }
}

sealed class OrderHistoryUiEvent {
    data class ShowPaymentLinkSheet(val url: String) : OrderHistoryUiEvent()
}