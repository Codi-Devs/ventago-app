package com.teco.ventago.features.orders.ui.order_invoice.viewModel

import androidx.compose.ui.graphics.ImageBitmap
import com.teco.ventago.core.LoadableState
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.features.orders.domain.models.Order

data class OrderInvoiceState (
    val order: Order? = null,
    val invoiceBitmap: ImageBitmap? = null,
    override val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
): LoadableState<OrderInvoiceState> {
    override fun withLoading(state: LoadingBottomSheetState): OrderInvoiceState {
        return copy(loadingBottomSheet = state)
    }
}

sealed class OrderInvoiceUiEvent {
    data class ShowPaymentLinkSheet(val url: String) : OrderInvoiceUiEvent()
}