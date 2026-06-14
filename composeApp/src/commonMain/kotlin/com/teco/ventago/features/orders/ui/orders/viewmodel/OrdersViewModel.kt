package com.teco.ventago.features.orders.ui.orders.viewmodel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.core.authz.ActionKey
import com.teco.ventago.core.authz.AuthzEvaluator
import com.teco.ventago.core.authz.RouteKey
import com.teco.ventago.core.beta.BetaFeature
import com.teco.ventago.core.beta.BetaService
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.business.domain.model.Business
import com.teco.ventago.features.customers.domain.CustomerService
import com.teco.ventago.features.customers.domain.models.CustomerListItem
import com.teco.ventago.features.orders.domain.OrderService
import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.orders.domain.models.OrderStatus
import com.teco.ventago.features.orders.domain.models.requests.orderEmissionEndDate
import com.teco.ventago.features.orders.domain.models.requests.orderEmissionStartDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.*

class OrdersViewModel(
    private val authService: IAuthService,
    private val orderService: OrderService,
    private val customerService: CustomerService,
    private val businessService: BusinessService,
    private val betaService: BetaService,
) : BaseViewModel<OrdersState, OrdersUiEvent>(OrdersState()) {

    private companion object {
        const val CUSTOMER_SEARCH_LIMIT = 4
        const val MIN_CUSTOMER_SEARCH_LENGTH = 2
        const val CUSTOMER_SEARCH_DEBOUNCE_MS = 300L
    }

    var businessId: Int = businessService.business.value?.businessId ?: -1
    var business: Business? = null
    private var hasLoadedInitialOrders = false
    private var customerSearchJob: Job? = null

    override fun onCleared() {
        customerSearchJob?.cancel()
        orderService.clear()
        super.onCleared()
    }

    init {
        updateState { copy(isLoadingOrders = true) }
        if (businessId > 0) {
            hasLoadedInitialOrders = true
            loadOrders()
        }
        viewModelScope.launch {
            betaService.getFeatures()
        }
        viewModelScope.launch {
            authService.getUser()
                .combine(betaService.features()) { user, betaResponse ->
                    val betaSnapshot = betaResponse?.features.orEmpty()
                        .mapNotNull(BetaFeature::fromKey)
                        .toSet()
                    Pair(
                        AuthzEvaluator.canRoute(RouteKey.QUOTES_LIST, user, betaSnapshot),
                        AuthzEvaluator.canAction(ActionKey.ORDERS_OPEN_CREATE, user, betaSnapshot)
                    )
                }
                .collect { (hasQuotesAccess, canCreateOrderEntry) ->
                    updateState {
                        copy(
                            hasQuotesAccess = hasQuotesAccess,
                            canCreateOrderEntry = canCreateOrderEntry
                        )
                    }
                }
        }
        viewModelScope.launch {
            orderService.observe().onEach { orders ->
                if (orders.isNotEmpty()) {
                    updateState {
                        copy(
                            orders = orders.toList(),
                            isLoadingOrders = false,
                        )
                    }
                }
            }.launchIn(this)
        }
        viewModelScope.launch {
            businessService.getBusiness().collect { businessData ->
                businessData?.let {
                    val previousBusinessId = businessId
                    business = businessData
                    businessId = it.businessId
                    if (businessId > 0 && (!hasLoadedInitialOrders || previousBusinessId != businessId)) {
                        hasLoadedInitialOrders = true
                        loadOrders()
                    }
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
                        paymentStatus = uiState.value.paymentStatusFilter,
                        customerId = uiState.value.customerIdFilter,
                        emissionStartDate = orderEmissionStartDate(uiState.value.emissionStartDate),
                        emissionEndDate = orderEmissionEndDate(uiState.value.emissionEndDate),
                        orderType = uiState.value.orderTypeFilter,
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
                        paymentStatus = uiState.value.paymentStatusFilter,
                        customerId = uiState.value.customerIdFilter,
                        emissionStartDate = orderEmissionStartDate(uiState.value.emissionStartDate),
                        emissionEndDate = orderEmissionEndDate(uiState.value.emissionEndDate),
                        orderType = uiState.value.orderTypeFilter,
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
        filterOrders(filter, orderService.orders.toList())
    }

    private fun filterOrders(filter: Int, orders: List<Order>) {
        val ordersSnapshot = orders.toList()
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
                    ordersSnapshot
                } else {
                    ordersSnapshot.filter { order -> order.status == status }
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

    fun setPaymentStatusFilter(paymentStatus: Int?) {
        updateState { copy(paymentStatusFilter = paymentStatus) }
    }

    fun setOrderTypeFilter(orderType: String?) {
        updateState { copy(orderTypeFilter = orderType) }
    }

    fun setCustomerNameFilter(customerName: String) {
        customerSearchJob?.cancel()
        val query = customerName.trim()

        updateState {
            copy(
                customerNameFilter = customerName,
                customerIdFilter = null,
                customerSearchResults = if (query.length < MIN_CUSTOMER_SEARCH_LENGTH) emptyList() else customerSearchResults,
                isSearchingCustomers = query.length >= MIN_CUSTOMER_SEARCH_LENGTH
            )
        }

        if (query.length < MIN_CUSTOMER_SEARCH_LENGTH || businessId <= 0) {
            updateState { copy(isSearchingCustomers = false) }
            return
        }

        customerSearchJob = viewModelScope.launch {
            delay(CUSTOMER_SEARCH_DEBOUNCE_MS)
            try {
                val customers = withContext(Dispatchers.IO) {
                    customerService.searchCustomersByName(
                        businessId = businessId,
                        name = query,
                        limit = CUSTOMER_SEARCH_LIMIT
                    )
                }
                if (uiState.value.customerNameFilter == customerName) {
                    updateState {
                        copy(
                            customerSearchResults = customers.take(CUSTOMER_SEARCH_LIMIT),
                            isSearchingCustomers = false
                        )
                    }
                }
            } catch (_: Exception) {
                if (uiState.value.customerNameFilter == customerName) {
                    updateState {
                        copy(
                            customerSearchResults = emptyList(),
                            isSearchingCustomers = false
                        )
                    }
                }
            }
        }
    }

    fun selectCustomerFilter(customer: CustomerListItem) {
        customerSearchJob?.cancel()
        updateState {
            copy(
                customerIdFilter = customer.id,
                customerNameFilter = customer.name,
                customerSearchResults = emptyList(),
                isSearchingCustomers = false
            )
        }
    }

    fun clearCustomerFilter() {
        customerSearchJob?.cancel()
        updateState {
            copy(
                customerIdFilter = null,
                customerNameFilter = "",
                customerSearchResults = emptyList(),
                isSearchingCustomers = false
            )
        }
    }

    fun setEmissionStartDate(value: String) {
        updateState { copy(emissionStartDate = value) }
    }

    fun setEmissionEndDate(value: String) {
        updateState { copy(emissionEndDate = value) }
    }

    fun setEmissionDateRange(startDate: String, endDate: String) {
        updateState {
            copy(
                emissionStartDate = startDate,
                emissionEndDate = endDate
            )
        }
    }

    fun applyQuickEmissionDateRange(startDate: String, endDate: String) {
        updateState {
            copy(
                paymentStatusFilter = null,
                customerIdFilter = null,
                customerNameFilter = "",
                customerSearchResults = emptyList(),
                isSearchingCustomers = false,
                orderTypeFilter = null,
                emissionStartDate = startDate,
                emissionEndDate = endDate,
                orders = emptyList(),
                noMoreOrders = false,
                filterSelected = 0,
                selectedChip = 0
            )
        }
        refreshOrders()
    }

    fun applyFilters() {
        updateState {
            copy(
                orders = emptyList(),
                noMoreOrders = false,
                filterSelected = 0,
                selectedChip = 0
            )
        }
        refreshOrders()
    }

    fun clearFilters() {
        updateState {
            copy(
                paymentStatusFilter = null,
                customerIdFilter = null,
                customerNameFilter = "",
                customerSearchResults = emptyList(),
                isSearchingCustomers = false,
                orderTypeFilter = null,
                emissionStartDate = "",
                emissionEndDate = "",
                orders = emptyList(),
                noMoreOrders = false,
                filterSelected = 0,
                selectedChip = 0
            )
        }
        refreshOrders()
    }

    fun activeFilterCount(): Int {
        val state = uiState.value
        return listOf(
            state.paymentStatusFilter != null,
            state.customerIdFilter != null,
            state.orderTypeFilter != null,
            state.emissionStartDate.isNotBlank() || state.emissionEndDate.isNotBlank()
        ).count { it }
    }

    fun applyCustomerFilter(customerId: Long?) {
        val currentState = uiState.value
        if (currentState.customerIdFilter == customerId && currentState.customerNameFilter.isBlank()) return

        customerSearchJob?.cancel()
        updateState {
            copy(
                customerIdFilter = customerId,
                customerNameFilter = "",
                customerSearchResults = emptyList(),
                isSearchingCustomers = false,
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
