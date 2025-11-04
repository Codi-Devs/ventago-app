package com.teco.ventago.features.orders.ui.order_invoice.viewModel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.business.domain.model.Business
import com.teco.ventago.features.orders.domain.OrderService
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class OrderInvoiceViewModel(
    private val orderService: OrderService,
    private val businessService: BusinessService,
) : BaseViewModel<OrderInvoiceState, OrderInvoiceUiEvent>(OrderInvoiceState()) {
    var business: Business? = null

    init {
        viewModelScope.launch {

            businessService.getBusiness().onEach{businessData ->
                businessData?.let {
                    business = businessData
                }
            }.launchIn(this)

            orderService.selectedOrder.onEach { order ->
                updateState {
                    copy(
                        order = order,
                    )
                }
            }.launchIn(this)
        }
    }


    fun setInvoiceBitmap(bitmap: ImageBitmap?) {
        updateState {
            copy(invoiceBitmap = bitmap)
        }
    }
}