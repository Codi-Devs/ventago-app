package com.teco.ventago.features.orders.domain

import com.teco.ventago.core.LocalStorage
import com.teco.ventago.core.Paged
import com.teco.ventago.core.changes.IChangesManager
import com.teco.ventago.features.business.domain.model.Business
import com.teco.ventago.features.customers.domain.models.CustomerListItem
import com.teco.ventago.features.orders.data.repository.IOrdersRepository
import com.teco.ventago.features.orders.domain.models.AchPaymentDetail
import com.teco.ventago.features.orders.domain.models.AchProofFileDownload
import com.teco.ventago.features.orders.domain.models.ManualPaymentMethodOption

import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.orders.domain.models.OrderStatus
import com.teco.ventago.features.orders.domain.models.requests.CancelOrderRequest
import com.teco.ventago.features.orders.domain.models.requests.CreatePaymentLinkRequest
import com.teco.ventago.features.orders.domain.models.requests.DeleteOrderRequest
import com.teco.ventago.features.orders.domain.models.requests.ManualPaymentItemRequest
import com.teco.ventago.features.orders.domain.models.requests.ListOrdersRequest
import com.teco.ventago.features.orders.domain.models.requests.PendingIntentCreateRequest
import com.teco.ventago.features.orders.domain.models.requests.PendingIntentCreateResponse
import com.teco.ventago.features.orders.domain.models.requests.PendingIntentReleaseRequest
import com.teco.ventago.features.orders.domain.models.requests.PendingIntentReleaseResponse
import com.teco.ventago.features.orders.domain.models.requests.PaymentApplicationRequest
import com.teco.ventago.features.orders.domain.models.requests.RejectAchPaymentRequest
import com.teco.ventago.features.orders.domain.models.requests.RescheduleReceivableTermRequest
import com.teco.ventago.features.orders.domain.models.requests.RescheduleReceivablesRequest
import com.teco.ventago.features.orders.domain.models.requests.RescheduleReceivablesResponse
import com.teco.ventago.features.orders.domain.models.requests.RegisterManualPaymentsDataResponse
import com.teco.ventago.features.orders.domain.models.requests.RegisterManualPaymentsRequest
import com.teco.ventago.features.orders.domain.models.requests.RetryInvoiceResponse
import com.teco.ventago.features.orders.domain.models.requests.VoidOrderPaymentRequest
import com.teco.ventago.features.orders.domain.models.requests.VoidOrderPaymentResponse
import com.teco.ventago.features.orders.domain.models.responses.InvoiceDocsDto
import com.teco.ventago.features.pos.provisioning.domain.PosDeviceProvisioningService
import com.teco.ventago.utils.toDecimalString
import com.teco.ventago.utils.toQuantityUiString
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class PaymentAllocation(
    val methodCode: Int,
    val amountCents: Long
)

data class ReceivableApplicationAllocation(
    val receivableTermId: Long,
    val amountCents: Long
)

data class OrderPaymentSubmission(
    val methodCode: Int,
    val amountCents: Long,
    val paymentDateIso: String,
    val otherDescription: String? = null,
    val dueDateIso: String? = null,
    val applications: List<ReceivableApplicationAllocation> = emptyList()
)

data class ReceivableRescheduleTerm(
    val dueDateIso: String,
    val amountCents: Long
)

class OrderService(
    private val repository: IOrdersRepository,
    private val posProvisioningService: PosDeviceProvisioningService,
    private val changesManager: IChangesManager? = null,
    private val storage: LocalStorage? = null,
) {

    private companion object {
        const val INITIAL_PAGE = 1
        const val ORDERS_LIST_CACHE_KEY = "orders_list_pages"
        const val ORDERS_LIST_CACHE_LIMIT = 4
        val ordersListJson = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }

    val orders = mutableListOf<Order>()
    val ordersFlow = MutableStateFlow<List<Order>>(emptyList())
    val selectedOrder = MutableStateFlow<Order?>(null)
    private val pageSize = 10
    private var page = INITIAL_PAGE

    private val mutex = Mutex()

    fun observe(): StateFlow<List<Order>> = ordersFlow.asStateFlow()


    fun clear() {
        orders.clear()
        ordersFlow.value = emptyList()
        page = INITIAL_PAGE
        selectedOrder.value = null
    }


    suspend fun loadOrders(
        businessId: Int,
        paymentStatus: Int? = null,
        customerId: Long? = null,
        emissionStartDate: String? = null,
        emissionEndDate: String? = null,
        orderType: String? = null,
        customerRuc: String? = null,
        force: Boolean = false,
    ): List<Order> {
        val request = withPosProvisioningFilters(
            ListOrdersRequest(
                businessId = businessId,
                pageSize = pageSize,
                page = page,
                paymentStatus = paymentStatus,
                customerId = customerId,
                emissionStartDate = emissionStartDate,
                emissionEndDate = emissionEndDate,
                orderType = orderType,
                customerRuc = customerRuc
            )
        ) ?: return emptyOrdersForUnprovisionedPos()
        val cacheKey = ordersListCacheKey(request)
        val token = changesManager?.ordersToken().orEmpty()
        if (!force) {
            val cached = peekOrdersList(cacheKey)
            if (cached != null && token.isNotEmpty() && cached.token == token) {
                if (request.page == INITIAL_PAGE) {
                    replaceOrders(cached.items)
                } else {
                    appendOrders(cached.items)
                }
                page += 1
                return ordersFlow.value
            }
            if (cached != null && request.page == INITIAL_PAGE) {
                replaceOrders(cached.items)
            }
        }
        val newOrders = repository.loadOrders(request)
        if (newOrders.isEmpty()) {
            return emptyList()
        }

        mutex.withLock {
            val base = if (request.page == INITIAL_PAGE) emptyList() else orders.toList()
            val combined = (base + newOrders).associateBy { it.id }.values.sortedByDescending { it.id }
            orders.clear()
            orders.addAll(combined)
            ordersFlow.value = orders.toList()
            page += 1
        }
        rememberOrdersList(cacheKey, token, newOrders, request.businessId)
        return ordersFlow.value
    }

    fun observeOrderListInvalidation(): Flow<Int> = changesManager?.ordersListener() ?: emptyFlow()

    suspend fun reloadVisibleOrders(
        businessId: Int,
        paymentStatus: Int? = null,
        customerId: Long? = null,
        emissionStartDate: String? = null,
        emissionEndDate: String? = null,
        orderType: String? = null,
        customerRuc: String? = null,
    ): List<Order> {
        mutex.withLock {
            page = INITIAL_PAGE
        }
        return loadOrders(
            businessId = businessId,
            paymentStatus = paymentStatus,
            customerId = customerId,
            emissionStartDate = emissionStartDate,
            emissionEndDate = emissionEndDate,
            orderType = orderType,
            customerRuc = customerRuc,
            force = false,
        )
    }

    private fun replaceOrders(items: List<Order>) {
        orders.clear()
        orders.addAll(items.sortedByDescending { it.id })
        ordersFlow.value = orders.toList()
    }

    private fun appendOrders(items: List<Order>) {
        val combined = (orders + items).associateBy { it.id }.values.sortedByDescending { it.id }
        orders.clear()
        orders.addAll(combined)
        ordersFlow.value = orders.toList()
    }

    suspend fun resetOrders(
        businessId: Int,
        paymentStatus: Int? = null,
        customerId: Long? = null,
        emissionStartDate: String? = null,
        emissionEndDate: String? = null,
        orderType: String? = null,
        customerRuc: String? = null,
    ): List<Order> {
        mutex.withLock {
            page = INITIAL_PAGE
            orders.clear()
            ordersFlow.value = emptyList()
        }
        return loadOrders(
            businessId = businessId,
            paymentStatus = paymentStatus,
            customerId = customerId,
            emissionStartDate = emissionStartDate,
            emissionEndDate = emissionEndDate,
            orderType = orderType,
            customerRuc = customerRuc,
            force = true,
        )
    }

    suspend fun listOrdersForCustomerPaged(
        businessId: Int,
        customerId: Long,
        pageSize: Int = 5,
        page: Int = 0
    ): Paged<Order> {
        return repository.loadOrdersPaged(
            withPosProvisioningFilters(
                ListOrdersRequest(
                    businessId = businessId,
                    pageSize = pageSize,
                    page = page,
                    paymentStatus = null,
                    customerId = customerId
                )
            ) ?: return Paged(page = page, size = pageSize, total = 0, items = emptyList())
        )
    }

    private fun withPosProvisioningFilters(request: ListOrdersRequest): ListOrdersRequest? {
        if (!posProvisioningService.isRequired()) return request

        val state = posProvisioningService.currentState()
        if (!state.locksBranchPoint) {
            return if (state.isProvisioned) request else null
        }
        val branchCode = state.fixedBranchCode?.takeIf { it.isNotBlank() } ?: return null
        val billingPointCode = state.fixedBillingPointCode?.takeIf { it.isNotBlank() } ?: return null

        return request.copy(
            branchCode = branchCode,
            billingPointCode = billingPointCode
        )
    }

    private suspend fun emptyOrdersForUnprovisionedPos(): List<Order> {
        mutex.withLock {
            orders.clear()
            ordersFlow.value = emptyList()
            page = INITIAL_PAGE
        }
        return emptyList()
    }

    suspend fun cancelOrder(
        businessId: Int,
        orderID: Int,
        reason: String,
        userName: String
    ): Boolean {
        val canceled = repository.cancelOrder(
            businessId,
            CancelOrderRequest(
                orderId = orderID.toLong(),
                reason = reason,
                userName = userName
            )
        )
        if (canceled) {
            val i = orders.indexOfFirst { it.id == orderID }
            if (i == -1) {
                resetOrders(businessId)
            } else {
                var order = orders[i]
                order = order.copy(
                    status = OrderStatus.CANCELLED,
                    invoiceStatus = 4 // Cancelled
                )
                orders[i] = order
            }
        }
        return canceled
    }

    suspend fun deleteOrder(
        businessId: Int,
        orderId: Int,
        reason: String
    ): Boolean {
        val deleted = repository.deleteOrder(
            businessId = businessId,
            request = DeleteOrderRequest(
                orderId = orderId.toLong(),
                deleteReason = reason
            )
        )

        if (deleted) {
            mutex.withLock {
                orders.removeAll { it.id == orderId }
                ordersFlow.value = orders.toList()
            }

            selectedOrder.update { current ->
                if (current?.id == orderId) null else current
            }
        }

        return deleted
    }

    suspend fun registerManualPayment(
        businessId: Int,
        orderId: Int,
        allocations: List<PaymentAllocation>,
        otherDescription: String?, // not used by this endpoint
        issueInvoice: Boolean,     // not used by this endpoint
        skipInvoicing: Boolean = false,
    ): RegisterManualPaymentsDataResponse {
        val payments = allocations.map { allocation ->
            OrderPaymentSubmission(
                methodCode = allocation.methodCode,
                amountCents = allocation.amountCents,
                paymentDateIso = currentPanamaDateTimeIso(),
                otherDescription = otherDescription
            )
        }
        return registerOrderPayments(
            businessId = businessId,
            orderId = orderId,
            payments = payments,
            skipInvoicing = skipInvoicing,
        )
    }

    suspend fun registerOrderPayments(
        businessId: Int,
        orderId: Int,
        payments: List<OrderPaymentSubmission>,
        skipInvoicing: Boolean = false,
    ): RegisterManualPaymentsDataResponse {
        val items = payments
            .filter { it.amountCents > 0L }
            .map { payment ->
                val applications = payment.applications
                    .filter { it.amountCents > 0L }
                    .takeIf { it.isNotEmpty() }
                    ?.map { app ->
                        PaymentApplicationRequest(
                            receivableTermId = app.receivableTermId,
                            amount = app.amountCents.toDecimalString()
                        )
                    }

                ManualPaymentItemRequest(
                    type = payment.methodCode,
                    amount = payment.amountCents.toDecimalString(),
                    paymentDate = payment.paymentDateIso,
                    dueDate = payment.dueDateIso,
                    description = if (
                        payment.methodCode == ManualPaymentMethodOption.OTHER_SPECIFY.id &&
                        !payment.otherDescription.isNullOrBlank()
                    ) payment.otherDescription else null,
                    applications = applications
                )
            }

        val req = RegisterManualPaymentsRequest(
            payments = items,
            skipInvoicing = skipInvoicing.takeIf { it },
        )
        return repository.registerManualPayments(
            businessId = businessId,
            orderId = orderId,
            request = req
        )
    }

    suspend fun rescheduleOrderReceivables(
        businessId: Int,
        orderId: Int,
        sourceTermIds: List<Long>,
        newTerms: List<ReceivableRescheduleTerm>
    ): RescheduleReceivablesResponse {
        val request = RescheduleReceivablesRequest(
            sourceTermIds = sourceTermIds,
            newTerms = newTerms.map { term ->
                RescheduleReceivableTermRequest(
                    dueDate = term.dueDateIso,
                    amount = term.amountCents.toDecimalString(),
                    notes = ""
                )
            },
            note = ""
        )

        return repository.rescheduleOrderReceivables(
            businessId = businessId,
            orderId = orderId,
            request = request
        )
    }

    suspend fun voidOrderPayment(
        businessId: Int,
        paymentId: Long,
        reason: String
    ): VoidOrderPaymentResponse {
        return repository.voidOrderPayment(
            businessId = businessId,
            paymentId = paymentId,
            request = VoidOrderPaymentRequest(reason = reason)
        )
    }

    private fun currentPanamaDateTimeIso(): String {
        val dateTime = Clock.System.now().toLocalDateTime(TimeZone.of("America/Panama"))
        val year = dateTime.year.toString().padStart(4, '0')
        val month = dateTime.monthNumber.toString().padStart(2, '0')
        val day = dateTime.dayOfMonth.toString().padStart(2, '0')
        val hour = dateTime.hour.toString().padStart(2, '0')
        val minute = dateTime.minute.toString().padStart(2, '0')
        val second = dateTime.second.toString().padStart(2, '0')
        return "$year-$month-${day}T$hour:$minute:$second-05:00"
    }

    suspend fun retryElectronicInvoice(
        businessId: Int,
        orderId: Int
    ): RetryInvoiceResponse {
        return repository.retryElectronicInvoice(businessId = businessId, orderId = orderId)
    }

    suspend fun refreshOrder(businessId: Int, orderId: Int): Order {
        val fresh = repository.findOrderById(businessId, orderId)
        upsertAndEmit(fresh)
        selectedOrder.value = fresh
        return fresh
    }

    /** Uniquely insert/replace by id, backend copy wins; keep list sorted desc by id. */
    private suspend fun upsertAndEmit(order: Order) = mutex.withLock {
        val idx = orders.indexOfFirst { it.id == order.id }
        if (idx >= 0) orders[idx] = order else orders.add(order)
        orders.sortByDescending { it.id }
        ordersFlow.value = orders.toList()
    }

    suspend fun getDocumentByCufe(businessId: Int, cufe: String, orderId: Long? = null): InvoiceDocsDto {
        return repository.getInvoiceDocsRaw(businessId, cufe, orderId)
    }

    suspend fun confirmNonFiscal(businessId: Int, orderId: Int): Order {
        repository.confirmNonFiscal(businessId, orderId)
        return refreshOrder(businessId, orderId)
    }

    suspend fun changeOrderStatus(order: Order, status: Int, businessId: Int): Boolean {
        val response = repository.changeOrderStatus(order, status, businessId)
        if (response.changed) {
            val i = orders.indexOfFirst { it.id == order.id }
            if (i == -1) {
                resetOrders(businessId)
            } else {
//                val newHistory = OrderStatusHistory(status, response.changedAt)
//                order.status = status
//                val newList = ArrayList<OrderStatusHistory>()
//                newList.addAll(order.orderHistory)
//                newList.add(newHistory)
//                order.orderHistory = newList
//                orders[i] = order
            }

        }

        return response.changed
    }

    suspend fun rejectOrder(order: Order, reason: String, businessId: Int): Boolean {
        val response = repository.rejectOrder(order, reason, businessId)
        if (response.changed) {
            val i = orders.indexOfFirst { it.id == order.id }
            if (i == -1) {
                resetOrders(businessId)
            } else {
//                val newHistory = OrderStatusHistory(OrderStatus.REJECT, response.changedAt)
//                order.status = OrderStatus.REJECT
//                val newList = ArrayList<OrderStatusHistory>()
//                newList.addAll(order.orderHistory)
//                newList.add(newHistory)
//                order.orderHistory = newList
//                order.rejectReason = reason
//                orders[i] = order
            }

        }

        return response.changed
    }

    fun findOrderById(id: Int): Order? {
        return orders.find { it.id == id }
    }

    suspend fun getOrderPaymentLink(orderID: Int): String? {
        return repository.getOrderPaymentLink(orderID)
    }

    suspend fun createPaymentLink(
        businessId: Int,
        orderId: Int,
        amount: String?,
        expireInMinutes: Int
    ): String? {
        return repository.createPaymentLink(
            businessId = businessId,
            request = CreatePaymentLinkRequest(
                orderId = orderId,
                amount = amount?.takeIf { it.isNotBlank() },
                expireInMinutes = expireInMinutes
            )
        )
    }

    suspend fun releasePendingPaymentIntent(
        businessId: Int,
        orderId: Int,
        paymentMethod: String,
        reason: String
    ): PendingIntentReleaseResponse {
        return repository.releasePendingPaymentIntent(
            businessId = businessId,
            orderId = orderId,
            request = PendingIntentReleaseRequest(
                paymentMethod = paymentMethod,
                reason = reason,
            )
        )
    }

    suspend fun createReplacementPaymentLink(
        businessId: Int,
        orderId: Int,
        amount: String,
        note: String = "Customer selected online checkout"
    ): PendingIntentCreateResponse {
        return repository.createPendingPaymentIntent(
            businessId = businessId,
            orderId = orderId,
            request = PendingIntentCreateRequest(
                paymentMethod = "payment_link",
                amount = amount,
                expireInMinutes = 1440,
                note = note,
            )
        )
    }

    suspend fun createReplacementYappyOnsite(
        businessId: Int,
        orderId: Int,
        amount: String,
        note: String = "Pago Yappy"
    ): PendingIntentCreateResponse {
        return repository.createPendingPaymentIntent(
            businessId = businessId,
            orderId = orderId,
            request = PendingIntentCreateRequest(
                paymentMethod = "yappy_onsite",
                amount = amount,
                expireInMinutes = 5,
                note = note,
            )
        )
    }

    suspend fun getAchPaymentByIntent(
        businessId: Int,
        paymentIntentId: String
    ): AchPaymentDetail {
        return repository.getAchPaymentByIntent(
            businessId = businessId,
            paymentIntentId = paymentIntentId
        )
    }

    suspend fun approveAchPayment(
        businessId: Int,
        paymentIntentId: String
    ): String {
        return repository.approveAchPayment(
            businessId = businessId,
            paymentIntentId = paymentIntentId
        )
    }

    suspend fun rejectAchPayment(
        businessId: Int,
        paymentIntentId: String,
        reasonCode: String,
        reasonText: String
    ): String {
        return repository.rejectAchPayment(
            businessId = businessId,
            paymentIntentId = paymentIntentId,
            request = RejectAchPaymentRequest(reasonCode = reasonCode, reasonText = reasonText)
        )
    }

    suspend fun downloadAchProofFile(
        businessId: Int,
        paymentId: String,
        proofId: String
    ): AchProofFileDownload {
        return repository.downloadAchProofFile(
            businessId = businessId,
            paymentId = paymentId,
            proofId = proofId
        )
    }

    suspend fun findOrderByOrderNumber(businessId: Int, orderNumber: String): Order {
        val order = orders.find { it.internalNumber == orderNumber }
        if (order != null) {
            return order
        }
        return repository.findOrderByOrderNumber(businessId, orderNumber)
    }

    suspend fun findOrderByCUFE(businessId: Int, cufe: String): Order {
        val order = orders.find { it.externalInvoiceNumber == cufe }
        if (order != null) {
            return order
        }
        return repository.findOrderByCUFE(businessId, cufe)
    }

    fun buildOrderShareMessage(order: Order, business: Business): String {
        val sb = StringBuilder()

        val currency = business.currency.symbol.ifEmpty {
            business.currency.currencyCode
        }

        sb.append("Pedido #${order.internalNumber}\n")
        sb.appendLine()

        order.lines.forEach { item ->
            sb.appendLine("*${item.quantity.toQuantityUiString()}x ${item.itemName} $currency ${item.baseUnitPrice}")
        }

        sb.appendLine()
        sb.appendLine("Subtotal: ${currency}${order.subtotal}")
        if (order.discountTotal.isNotEmpty() && (order.discountTotal.toDoubleOrNull()
                ?: 0.0) > 0.0
        ) {
            sb.appendLine("Descuentos: ${currency}${order.discountTotal}")
        }

        if (order.taxTotal.isNotEmpty()) {
            sb.appendLine("Impuestos: ${currency}${order.taxTotal}")
        }
        if (order.tipsTotal.isNotEmpty() && (order.tipsTotal.toDoubleOrNull() ?: 0.0) > 0.0) {
            sb.appendLine("Propinas: ${currency}${order.tipsTotal}")
        }
        sb.appendLine("Total: ${currency}${order.totalAmount}")

        PaymentLinkResolver.resolveCurrent(order)?.url?.takeIf { it.isNotBlank() }?.let {
            sb.appendLine()
            sb.appendLine("Enlace de pago: $it")
        }

        sb.appendLine()
        sb.append("Powered by VentaGo App")

        return sb.toString().trim()
    }

    fun selectOrder(order: Order?) {
        selectedOrder.update {
            order
        }
    }

    private fun ordersListCacheKey(request: ListOrdersRequest): String {
        return listOf(
            request.businessId,
            request.page,
            request.pageSize,
            request.paymentStatus ?: "",
            request.customerId ?: "",
            request.customerRuc.orEmpty(),
            request.orderType.orEmpty(),
            request.emissionStartDate.orEmpty(),
            request.emissionEndDate.orEmpty(),
            request.branchCode.orEmpty(),
            request.billingPointCode.orEmpty(),
        ).joinToString("|")
    }

    private fun peekOrdersList(key: String): OrdersListCachePage? {
        val store = storage ?: return null
        return readOrdersListCache(store)[key]
    }

    private fun rememberOrdersList(key: String, token: String, items: List<Order>, businessId: Int) {
        val store = storage ?: return
        if (token.isEmpty()) return
        val cache = readOrdersListCache(store).toMutableMap()
        val prefix = "$businessId|"
        cache.keys.filter { it.startsWith(prefix) && cache[it]?.token != token }.toList()
            .forEach { cache.remove(it) }
        cache[key] = OrdersListCachePage(token = token, savedAt = kotlin.time.Clock.System.now().toEpochMilliseconds(), items = items)
        val businessKeys = cache.keys.filter { it.startsWith(prefix) }
            .sortedByDescending { cache[it]?.savedAt ?: 0L }
        businessKeys.drop(ORDERS_LIST_CACHE_LIMIT).forEach { cache.remove(it) }
        store.set(ORDERS_LIST_CACHE_KEY, ordersListJson.encodeToString(OrdersListCacheFile(cache)))
    }

    private fun readOrdersListCache(store: LocalStorage): Map<String, OrdersListCachePage> {
        val raw = store.string(ORDERS_LIST_CACHE_KEY).orEmpty()
        if (raw.isBlank()) return emptyMap()
        return try {
            ordersListJson.decodeFromString(OrdersListCacheFile.serializer(), raw).pages
        } catch (_: Exception) {
            emptyMap()
        }
    }
}

@Serializable
private data class OrdersListCacheFile(
    val pages: Map<String, OrdersListCachePage> = emptyMap(),
)

@Serializable
private data class OrdersListCachePage(
    val token: String = "",
    val savedAt: Long = 0L,
    val items: List<Order> = emptyList(),
)
