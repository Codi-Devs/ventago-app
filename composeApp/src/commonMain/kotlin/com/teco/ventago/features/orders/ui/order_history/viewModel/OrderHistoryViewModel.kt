package com.teco.ventago.features.orders.ui.order_history.viewModel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.business.domain.model.Business
import com.teco.ventago.features.orders.domain.OrderService
import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.orders.domain.models.OrderStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OrderHistoryViewModel(
    private val orderService: OrderService,
    private val businessService: BusinessService,
): BaseViewModel<OrderHistoryState, OrderHistoryUiEvent>(OrderHistoryState()) {
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

    private fun changeOrderStatus(order: Order, status: Int) {
        showLoading()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val changed = orderService.changeOrderStatus(order, status, business!!.businessId)
                    if (changed) {
                        val newOrder = orderService.findOrderById(order.id)
                        withContext(Dispatchers.Main) {
                            updateState {
                                copy(
                                    order = newOrder,

                                    )
                            }
                            showSuccess()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            showError()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        showError()
                    }
                }
            }
        }
    }

    fun historySecondaryOnClick() {
        updateState { copy(showRejectDialog = true) }
    }

    fun rejectOrder(reason: String) {
        val order = uiState.value.order!!
        showLoading()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val changed = orderService.rejectOrder(order, reason, business!!.businessId)
                    if (changed) {
                        val newOrder = orderService.findOrderById(order.id)
                        withContext(Dispatchers.Main) {
                            updateState {
                                copy(
                                    order = newOrder,

                                    )
                            }
                            showSuccess()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            showError()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        showError()
                    }
                }
            }
        }
    }

    fun showRejectDialog(show: Boolean) {
        updateState {
            copy(showRejectDialog = show)
        }
    }

    fun showReasonDialog(show: Boolean) {
        updateState {
            copy(showReasonDialog = show)
        }
    }
}