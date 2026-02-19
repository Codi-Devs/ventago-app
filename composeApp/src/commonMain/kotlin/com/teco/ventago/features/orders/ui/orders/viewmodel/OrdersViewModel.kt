package com.teco.ventago.features.orders.ui.orders.viewmodel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.core.beta.BetaFeature
import com.teco.ventago.core.beta.BetaService
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.business.domain.model.Business
import com.teco.ventago.features.orders.domain.OrderService
import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.orders.domain.models.OrderStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.*

class OrdersViewModel(
    private val orderService: OrderService,
    private val businessService: BusinessService,
    private val betaService: BetaService,
) : BaseViewModel<OrdersState, OrdersUiEvent>(OrdersState()) {

    var businessId: Int = businessService.business.value?.businessId ?: -1
    var business: Business? = null

    override fun onCleared() {
        orderService.clear()
        super.onCleared()
    }

    init {
        println("ASDADS: viewmodel instance: $this")
        updateState { copy(isLoadingOrders = true) }
        loadOrders()
        viewModelScope.launch {
            betaService.getFeatures()
        }
        viewModelScope.launch {
            betaService.accessFlow(BetaFeature.QUOTES).collect { hasAccess ->
                updateState { copy(hasQuotesAccess = hasAccess) }
            }
        }
        viewModelScope.launch {
            orderService.observe().onEach { orders ->
                if (orders.isNotEmpty()) {
                    updateState {
                        copy(
                            orders = orderService.orders,
                            isLoadingOrders = false,
                        )
                    }
                }
            }.launchIn(this)
        }
        viewModelScope.launch {
            businessService.getBusiness().collect { businessData ->
                businessData?.let {
                    business = businessData
                    businessId = it.businessId
                }
            }
        }
    }

    fun loadOrders() {
        updateState {
            copy(
                isLoadingOrders = true,
                refreshingOrder = false
            )
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val aux = orderService.loadOrders(
                        businessId = businessId,
                        paymentStatus = uiState.value.paymentStatusFilter
                    )
                    withContext(Dispatchers.Main) {
                        if (aux.isEmpty()) {
                            updateState { copy(noMoreOrders = true) }
                            filterOrders(uiState.value.filterSelected)
                        } else {
                            filterOrders(uiState.value.filterSelected, aux)
                        }
                    }
                    delay(2000)
                    withContext(Dispatchers.Main) {
                        updateState { copy(isLoadingOrders = false) }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        updateState { copy(isLoadingOrders = false) }
                        emitEvent(OrdersUiEvent.LoadingOrdersError)
                    }
                }
            }
        }
    }

    fun refreshOrders() {
        updateState {
            copy(
                refreshingOrder = true,
                isLoadingOrders = true,
                noMoreOrders = false,
            )
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    orderService.resetOrders(
                        businessId = businessId,
                        paymentStatus = uiState.value.paymentStatusFilter
                    )
                    withContext(Dispatchers.Main) {
                        filterOrders(uiState.value.filterSelected)
                        updateState {
                            copy(
                                refreshingOrder = false,
                                isLoadingOrders = false,
                                noMoreOrders = false,
                            )
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        updateState {
                            copy(
                                refreshingOrder = false,
                                orders =  emptyList(),
                                isLoadingOrders = false,
                            )
                        }
                        emitEvent(OrdersUiEvent.LoadingOrdersError)
                    }
                }
            }
        }

    }

    fun selectOrder(order: Order) {
        orderService.selectOrder(order)
    }

    fun unselectOrder() {
        orderService.selectOrder(null)
    }

    /**
     * Filter the orders List using filter values
     * 0 - All
     * 1 - New(Created Status)
     * 2 - Failed(Rejected/Cancelled Status)
     * 3 - Processing(Processing Status)
     */
    fun filterOrders(filter: Int) {
        filterOrders(filter, orderService.orders)
    }

    private fun filterOrders(filter: Int, orders: List<Order>) {
        updateState { copy(filterSelected = filter) }
        val status = when (filter) {
            0 -> {
                -1
            }

            1 -> {
                OrderStatus.DRAFT
            }

            2 -> {
                OrderStatus.REJECT
            }

            3 -> {
                OrderStatus.PROCESSING
            }

            else -> {
                -1
            }
        }

//        updateState { copy(orders = emptyList()) }
        updateState {
            copy(
                orders = if (status == -1) {
                    orders
                } else {
                    orders.filter { order -> order.status == status }
                },
                selectedChip =filter
            )
        }
    }

    /**
     * Get the label id of the shipping method by th id
     */
//    fun getShippingMethodLabel(id: Int): StringResource {
//        return when (id) {
//            ShippingMethod.DELIVERY -> Res.string.delivery
////            ShippingMethod.PICKUP -> Res.string.pick_up
//            else -> Res.string.delivery
//        }
//    }


//    fun seeOnMap(address: ShippingAddress) {
//        val url = "https://maps.google.com/?q=${address.latitude},${address.longitude}"
//        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
//        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//        getApplication<Application>().startActivity(browserIntent)
//    }
//
//    fun sendEmail(email: String) {
//        utils.sendEmail(email, getApplication())
//    }


    fun findOrderByOrderNumber(orderNumber: String) {
        showLoading()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val order = orderService.findOrderByOrderNumber(businessId, orderNumber)
                    withContext(Dispatchers.Main) {
                        selectOrder(order)
                        _events.emit(OrdersUiEvent.OpenOrderDetails)
                        showSuccess()
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) {
                        showError()
                    }
                }
            }
        }
    }

    fun findOrderByCUFE(cufe: String) {
        var finalCufe = cufe
        if (finalCufe.contains("https://dgi-fep.mef.gob.pa/Consultas/FacturasPorCUFE/")) {
            finalCufe = finalCufe.replace("https://dgi-fep.mef.gob.pa/Consultas/FacturasPorCUFE/", "")
        }
        showLoading()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val order = orderService.findOrderByCUFE(businessId, finalCufe)
                    withContext(Dispatchers.Main) {
                        selectOrder(order)
                        _events.emit(OrdersUiEvent.OpenOrderDetails)
                        showSuccess()
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) {
                        showError()
                    }
                }
            }
        }
    }

    fun showScanner(showScanner: Boolean) {
        println("ASDADS: viewmodel instance: $this")
        updateState { copy(showScanner = showScanner) }
    }

    fun applyPaymentStatusFilter(paymentStatus: Int?) {
        if (uiState.value.paymentStatusFilter == paymentStatus) return

        updateState {
            copy(
                paymentStatusFilter = paymentStatus,
                orders = emptyList(),
                noMoreOrders = false
            )
        }
        refreshOrders()
    }

    fun showPermissionRationalDialog(showPermissionRationalDialog: Boolean) {
        updateState { copy(showPermissionRationalDialog = showPermissionRationalDialog) }
    }

    fun launchSettings() {
        viewModelScope.launch {
            emitEvent(OrdersUiEvent.LaunchSettings)
        }
    }

}
