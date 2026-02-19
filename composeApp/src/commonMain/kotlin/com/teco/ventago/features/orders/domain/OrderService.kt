package com.teco.ventago.features.orders.domain

import com.teco.ventago.core.Paged
import com.teco.ventago.features.business.domain.model.Business
import com.teco.ventago.features.customers.domain.models.CustomerListItem
import com.teco.ventago.features.orders.data.repository.IOrdersRepository
import com.teco.ventago.features.orders.domain.models.ManualPaymentMethodOption

import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.orders.domain.models.OrderStatus
import com.teco.ventago.features.orders.domain.models.requests.CancelOrderRequest
import com.teco.ventago.features.orders.domain.models.requests.DeleteOrderRequest
import com.teco.ventago.features.orders.domain.models.requests.ManualPaymentItemRequest
import com.teco.ventago.features.orders.domain.models.requests.RegisterManualPaymentsDataResponse
import com.teco.ventago.features.orders.domain.models.requests.RegisterManualPaymentsRequest
import com.teco.ventago.features.orders.domain.models.requests.RetryInvoiceResponse
import com.teco.ventago.features.orders.domain.models.responses.InvoiceDocsDto
import com.teco.ventago.utils.toDecimalString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class PaymentAllocation(
    val methodCode: Int,
    val amountCents: Long
)

class OrderService(private val repository: IOrdersRepository) {

    val orders = mutableListOf<Order>()
    val ordersFlow = MutableStateFlow<List<Order>>(emptyList())
    val selectedOrder = MutableStateFlow<Order?>(null)
    private val pageSize = 10
    private var page = 0

    private val mutex = Mutex()

    fun observe(): StateFlow<List<Order>> = ordersFlow.asStateFlow()


    fun clear() {
        orders.clear()
        ordersFlow.value = emptyList()
        page = 0
        selectedOrder.value = null
    }


    suspend fun loadOrders(
        businessId: Int,
        paymentStatus: Int? = null
    ): List<Order> {
        val newOrders = repository.loadOrders(
            businessId = businessId,
            pageSize = pageSize,
            page = page,
            paymentStatus = paymentStatus
        )
        if (newOrders.isEmpty()) {
            return emptyList()
        }

        mutex.withLock {
            val combined = (orders + newOrders).associateBy { it.id }.values.toMutableList()
            combined.sortByDescending { it.id }
            orders.clear()
            orders.addAll(combined)
            ordersFlow.value = orders.toList()
            page += 1
        }
        return ordersFlow.value
    }

    suspend fun resetOrders(
        businessId: Int,
        paymentStatus: Int? = null
    ): List<Order> {
        mutex.withLock {
            page = 0
            orders.clear()
            ordersFlow.value = emptyList()
        }
        return loadOrders(businessId, paymentStatus)
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
        issueInvoice: Boolean      // not used by this endpoint
    ): RegisterManualPaymentsDataResponse {

        // Map cents -> "0.00" strings
        val items = allocations
            .filter { it.amountCents > 0L }
            .map { alloc ->
                if (alloc.methodCode == ManualPaymentMethodOption.OTHER_SPECIFY.id && !otherDescription.isNullOrBlank()) {
                    // Other specified
                    ManualPaymentItemRequest(
                        type = alloc.methodCode,
                        amount = alloc.amountCents.toDecimalString(),
                        description = otherDescription
                    )
                } else {
                    ManualPaymentItemRequest(
                        type = alloc.methodCode,
                        amount = alloc.amountCents.toDecimalString()
                    )
                }
            }

        val req = RegisterManualPaymentsRequest(payments = items)

        val result = repository.registerManualPayments(
            businessId = businessId,
            orderId = orderId,
            request = req
        )

        return result
    }

    suspend fun retryElectronicInvoice(
        businessId: Int,
        orderId: Int
    ): RetryInvoiceResponse {
        return repository.retryElectronicInvoice(businessId = businessId, orderId = orderId)
    }

    suspend fun refreshOrder(businessId: Int, orderId: Int): Order {
        val order = findOrderById(orderId)
        val fresh = repository.findOrderByOrderNumber(businessId, order!!.internalNumber)
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

    suspend fun getDocumentByCufe(businessId: Int, cufe: String): InvoiceDocsDto {
        return repository.getInvoiceDocsRaw(businessId, cufe)
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
            sb.appendLine("*${item.quantity}x ${item.itemName} $currency ${item.baseUnitPrice}")
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

        order.paymentLink?.takeIf { it.isNotBlank() }?.let {
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
}
