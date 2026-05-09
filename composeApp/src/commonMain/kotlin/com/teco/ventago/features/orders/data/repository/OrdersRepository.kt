package com.teco.ventago.features.orders.data.repository

import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.orders.data.provider.IOrdersProvider
import com.teco.ventago.features.orders.domain.AchPaymentNormalizer
import com.teco.ventago.features.orders.domain.models.AchPaymentDetail
import com.teco.ventago.features.orders.domain.models.AchProofFileDownload
import com.teco.ventago.features.orders.domain.models.ChangeOrderStatusResponse
import com.teco.ventago.features.orders.domain.models.IsBusinessRegisteredResponse
import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.orders.domain.models.requests.CancelOrderRequest
import com.teco.ventago.features.orders.domain.models.requests.CreatePaymentLinkRequest
import com.teco.ventago.features.orders.domain.models.requests.CreateOrderRequest
import com.teco.ventago.features.orders.domain.models.requests.DeleteOrderRequest
import com.teco.ventago.features.orders.domain.models.requests.ListOrdersRequest
import com.teco.ventago.features.orders.domain.models.requests.RejectAchPaymentRequest
import com.teco.ventago.features.orders.domain.models.requests.RescheduleReceivablesRequest
import com.teco.ventago.features.orders.domain.models.requests.RescheduleReceivablesResponse
import com.teco.ventago.features.orders.domain.models.requests.RegisterManualPaymentsDataResponse
import com.teco.ventago.features.orders.domain.models.requests.RegisterManualPaymentsRequest
import com.teco.ventago.features.orders.domain.models.requests.RetryInvoiceResponse
import com.teco.ventago.features.orders.domain.models.requests.VoidOrderPaymentRequest
import com.teco.ventago.features.orders.domain.models.requests.VoidOrderPaymentResponse
import com.teco.ventago.features.orders.domain.models.responses.CreateOrderResponse
import com.teco.ventago.features.orders.domain.models.responses.InvoiceDocsDto
import com.teco.ventago.core.Paged
import com.teco.ventago.json
import com.teco.ventago.utils.ApiResponse
import com.teco.ventago.utils.BadRequestException
import com.teco.ventago.utils.isError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class OrdersRepository(private val provider: IOrdersProvider, private val logger: ILoggerService) :
    IOrdersRepository {

    override suspend fun isBusinessRegistered(businessId: Int): IsBusinessRegisteredResponse {
        try {
            val response = provider.isBusinessRegistered(businessId)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            if (response.data is JsonObject) {
                val map = Json.decodeFromJsonElement<Map<String, JsonElement>>(response.data)
                return IsBusinessRegisteredResponse.fromMap(map)
            } else {
                throw BadRequestException(response.toJson())
            }
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "isBusinessRegistered",
                    "Error checking if isBusinessRegistered. Error: ${e.message ?: "UNKNOWN"}"
                )
            )
            throw e
        }
    }

    override suspend fun loadOrders(
        request: ListOrdersRequest
    ): List<Order> {
        return loadOrdersPaged(request).items
    }

    override suspend fun loadOrdersPaged(
        request: ListOrdersRequest
    ): Paged<Order> {
        try {
            val response = provider.loadOrders(request)

            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            // Ensure data exists
            val dataObj = response.data?.jsonObject
                ?: throw BadRequestException("Missing 'data' in response")

            // Extract the paginated "items" array
            val itemsArray = dataObj["items"]?.jsonArray
                ?: throw BadRequestException("Missing 'items' in paginated response")

            val orders = mutableListOf<Order>()
            for (item in itemsArray) {
                orders.add(json.decodeFromJsonElement<Order>(item))
            }

            // Optionally log pagination meta (for debugging or caching)
            val total = dataObj["total"]?.jsonPrimitive?.longOrNull ?: -1L
            val size = dataObj["size"]?.jsonPrimitive?.intOrNull ?: request.pageSize
            val pageNum = dataObj["page"]?.jsonPrimitive?.intOrNull ?: request.page

            logger.sendLog(
                Log(
                    LogLevel.INFO,
                    "loadOrders",
                    "Loaded ${orders.size} orders of total $total (page=$pageNum, size=$size, paymentStatus=${request.paymentStatus ?: "ALL"}, customerId=${request.customerId ?: "ALL"}, customerRuc=${request.customerRuc ?: "ALL"}, orderType=${request.orderType ?: "ALL"}, emissionStart=${request.emissionStartDate ?: "ALL"}, emissionEnd=${request.emissionEndDate ?: "ALL"})"
                )
            )

            return Paged(
                page = pageNum,
                size = size,
                total = total,
                items = orders
            )
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR, "loadOrders",
                    "Error loading orders. Error: ${e.message ?: "UNKNOWN"}, businessId: ${request.businessId}, pageSize: ${request.pageSize}, page: ${request.page}, paymentStatus: ${request.paymentStatus ?: "ALL"}, customerId: ${request.customerId ?: "ALL"}, customerRuc: ${request.customerRuc ?: "ALL"}, orderType: ${request.orderType ?: "ALL"}, emissionStart: ${request.emissionStartDate ?: "ALL"}, emissionEnd: ${request.emissionEndDate ?: "ALL"}"
                )
            )
            throw e
        }
    }

    override suspend fun changeOrderStatus(
        order: Order,
        status: Int,
        businessId: Int
    ): ChangeOrderStatusResponse {
        try {
            val response = provider.changeOrderStatus(order, status, businessId)

            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            if (response.data is JsonObject) {
                val map = json.decodeFromJsonElement<Map<String, JsonElement>>(response.data)
                return ChangeOrderStatusResponse.fromMap(map)
            } else {
                throw BadRequestException(response.toJson())
            }
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR, "changeOrderStatus",
                    "Error changing order status. Error: ${e.message ?: "UNKNOWN"}, businessId: $businessId, orderId: ${order.id}, status: $status"
                )
            )
            throw e
        }
    }

    override suspend fun rejectOrder(
        order: Order,
        reason: String,
        businessId: Int
    ): ChangeOrderStatusResponse {
        try {
            val response = provider.rejectOrder(order, reason, businessId)

            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            if (response.data is JsonObject) {
                val map = json.decodeFromJsonElement<Map<String, JsonElement>>(response.data)
                return ChangeOrderStatusResponse.fromMap(map)
            } else {
                throw BadRequestException(response.toJson())
            }
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR, "rejectOrder",
                    "Error rejecting order. Error: ${e.message ?: "UNKNOWN"}, businessId: $businessId, orderId: ${order.id}, reason: $reason"
                )
            )
            throw e
        }
    }

    override suspend fun changeOrdersEnabled(businessId: Int, enabled: Boolean): Boolean {
        try {
            val response = provider.changeOrdersEnabled(businessId, enabled)

            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            return response.successful
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR, "changeOrdersEnabled",
                    "Error changing orders enabled. Error: ${e.message ?: "UNKNOWN"}, businessId: $businessId, enabled: $enabled"
                )
            )
            throw e
        }
    }


    override suspend fun createOrder(
        businessId: Int,
        createOrderRequest: CreateOrderRequest
    ): CreateOrderResponse {
        try {
            val response = provider.createOrder(businessId, createOrderRequest)

            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            if (response.data is JsonObject) {
                return json.decodeFromJsonElement<CreateOrderResponse>(response.data)
            } else {
                throw BadRequestException(response.toJson())
            }
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "updateShippingMethods",
                    "Error updating shipping Methods. Error: ${e.message ?: "UNKNOWN"}. BusinessId: $businessId"
                )
            )
            throw e
        }
    }

    override suspend fun cancelOrder(businessId: Int, request: CancelOrderRequest): Boolean {
        try {
            val response = provider.cancelOrder(businessId, request)

            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            return response.successful
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "updateShippingMethods",
                    "Error canceling Order. Error: ${e.message ?: "UNKNOWN"}. BusinessId: $businessId"
                )
            )
            throw e
        }
    }

    override suspend fun deleteOrder(businessId: Int, request: DeleteOrderRequest): Boolean {
        try {
            val response = provider.deleteOrder(businessId, request)

            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            return response.successful
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "deleteOrder",
                    "Error deleting order. Error: ${e.message ?: "UNKNOWN"}, businessId: $businessId, orderId: ${request.orderId}"
                )
            )
            throw e
        }
    }

    override suspend fun registerManualPayments(
        businessId: Int,
        orderId: Int,
        request: RegisterManualPaymentsRequest
    ): RegisterManualPaymentsDataResponse {
        try {
            val response = provider.registerManualPayments(businessId, orderId, request)

            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            if (response.data is JsonObject) {
                return json.decodeFromJsonElement<RegisterManualPaymentsDataResponse>(response.data)
            } else {
                throw BadRequestException(response.toJson())
            }
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "updateShippingMethods",
                    "Error registering manual payment Methods. Error: ${e.message ?: "UNKNOWN"}. BusinessId: $businessId"
                )
            )
            throw e
        }
    }

    override suspend fun rescheduleOrderReceivables(
        businessId: Int,
        orderId: Int,
        request: RescheduleReceivablesRequest
    ): RescheduleReceivablesResponse {
        try {
            val response = provider.rescheduleOrderReceivables(businessId, orderId, request)

            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            if (response.data is JsonObject) {
                return json.decodeFromJsonElement(response.data)
            } else {
                throw BadRequestException(response.toJson())
            }
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "rescheduleOrderReceivables",
                    "Error rescheduling receivables. Error: ${e.message ?: "UNKNOWN"}. BusinessId: $businessId, orderId: $orderId"
                )
            )
            throw e
        }
    }

    override suspend fun voidOrderPayment(
        businessId: Int,
        paymentId: Long,
        request: VoidOrderPaymentRequest
    ): VoidOrderPaymentResponse {
        try {
            val response = provider.voidOrderPayment(businessId, paymentId, request)

            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            if (response.data is JsonObject) {
                return json.decodeFromJsonElement(response.data)
            } else {
                throw BadRequestException(response.toJson())
            }
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "voidOrderPayment",
                    "Error voiding payment. Error: ${e.message ?: "UNKNOWN"}. BusinessId: $businessId, paymentId: $paymentId"
                )
            )
            throw e
        }
    }


    override suspend fun retryElectronicInvoice(
        businessId: Int,
        orderId: Int,
    ): RetryInvoiceResponse {
        try {
            val response = provider.retryElectronicInvoice(businessId, orderId)

            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            if (response.data is JsonObject) {
                return json.decodeFromJsonElement<RetryInvoiceResponse>(response.data)
            } else {
                throw BadRequestException(response.toJson())
            }
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "updateShippingMethods",
                    "Error registering manual payment Methods. Error: ${e.message ?: "UNKNOWN"}. BusinessId: $businessId"
                )
            )
            throw e
        }
    }

    override suspend fun getInvoiceDocsRaw(businessId: Int, cufe: String): InvoiceDocsDto {
        try {
            val response = provider.getInvoiceDocsRaw(businessId, cufe)

            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            if (response.data is JsonObject) {
                return Json.decodeFromJsonElement<InvoiceDocsDto>(response.data)
            } else {
                throw BadRequestException(response.toJson())
            }
        } catch (e: Exception) {
            println("ASDASD: Error getting invoice docs raw: ${e.message}")
            logger.sendLog(
                Log(
                    LogLevel.ERROR, "getInvoiceDocsRaw",
                    "Error getting invoice docs raw. Error: ${e.message ?: "UNKNOWN"}, businessId: $businessId, cufe: $cufe"
                )
            )
            throw e
        }
    }

    override suspend fun createPaymentLink(businessId: Int, request: CreatePaymentLinkRequest): String? {
        try {
            val response = provider.createPaymentLink(businessId, request)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            val data = response.data?.jsonObject ?: return null
            return data["payment_link_url"]?.jsonPrimitive?.contentOrNull
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "createPaymentLink",
                    "Error creating payment link. Error: ${e.message ?: "UNKNOWN"}, businessId: $businessId, orderId: ${request.orderId}"
                )
            )
            throw e
        }
    }

    override suspend fun getAchPaymentByIntent(businessId: Int, paymentIntentId: String): AchPaymentDetail {
        try {
            val response = provider.getAchPaymentByIntent(businessId, paymentIntentId)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            val data = response.data?.jsonObject
            return AchPaymentNormalizer.fromApiPayload(data)
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "getAchPaymentByIntent",
                    "Error loading ACH payment detail. Error: ${e.message ?: "UNKNOWN"}, businessId: $businessId, paymentIntentId: $paymentIntentId"
                )
            )
            throw e
        }
    }

    override suspend fun approveAchPayment(businessId: Int, paymentIntentId: String): String {
        try {
            val response = provider.approveAchPayment(businessId, paymentIntentId)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }
            return response.data?.jsonObject?.get("status")?.jsonPrimitive?.contentOrNull.orEmpty()
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "approveAchPayment",
                    "Error approving ACH payment. Error: ${e.message ?: "UNKNOWN"}, businessId: $businessId, paymentIntentId: $paymentIntentId"
                )
            )
            throw e
        }
    }

    override suspend fun rejectAchPayment(
        businessId: Int,
        paymentIntentId: String,
        request: RejectAchPaymentRequest
    ): String {
        try {
            val response = provider.rejectAchPayment(businessId, paymentIntentId, request)
            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }
            return response.data?.jsonObject?.get("status")?.jsonPrimitive?.contentOrNull.orEmpty()
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "rejectAchPayment",
                    "Error rejecting ACH payment. Error: ${e.message ?: "UNKNOWN"}, businessId: $businessId, paymentIntentId: $paymentIntentId"
                )
            )
            throw e
        }
    }

    override suspend fun downloadAchProofFile(
        businessId: Int,
        paymentId: String,
        proofId: String
    ): AchProofFileDownload {
        try {
            val response = provider.downloadAchProofFile(businessId, paymentId, proofId)
            return AchProofFileDownload(
                bytes = response.bytes,
                contentType = response.contentType,
                fileName = response.fileName
            )
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR,
                    "downloadAchProofFile",
                    "Error downloading ACH proof file. Error: ${e.message ?: "UNKNOWN"}, businessId: $businessId, paymentId: $paymentId, proofId: $proofId"
                )
            )
            throw e
        }
    }

    override suspend fun removeCustomer(businessId: Int, customerId: Int): Boolean {
        try {
            val response = provider.removeCustomer(businessId, customerId)

            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            return response.successful
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR, "removeCustomer",
                    "Error removing customer. Error: ${e.message ?: "UNKNOWN"}, customerId: $customerId"
                )
            )
            return false
        }
    }

    override suspend fun registerManualTransference(
        businessId: Int,
        orderNumber: String,
        paymentReference: String,
        description: String
    ): Boolean {
        try {
            val response = provider.registerManualTransference(
                businessId,
                orderNumber,
                paymentReference,
                description
            )

            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            return response.successful
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR, "removeCustomer",
                    "Error registering manual payment. Error: ${e.message ?: "UNKNOWN"}, businessId: $businessId; orderNumber: $orderNumber"
                )
            )
            return false
        }
    }

    override suspend fun getOrderPaymentLink(orderID: Int): String? {
        try {
            val response = provider.getOrderPaymentLink(orderID)

            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            return response.data?.jsonPrimitive?.content
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR, "getOrderPaymentLink",
                    "Error getting order payment link. Error: ${e.message ?: "UNKNOWN"}, orderId: $orderID"
                )
            )
            throw e
        }
    }

    override suspend fun findOrderByOrderNumber(businessId: Int, orderNumber: String): Order {
        try {
            val response = provider.findOrderByOrderNumber(businessId, orderNumber)

            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            if (response.data is JsonObject) {
                return json.decodeFromJsonElement<Order>(response.data)
            } else {
                throw BadRequestException(response.toJson())
            }
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR, "findOrderByOrderNumber",
                    "Error finding order by order number. Error: ${e.message ?: "UNKNOWN"}, businessId: $businessId, orderNumber: $orderNumber"
                )
            )
            throw e
        }
    }

    override suspend fun findOrderById(businessId: Int, orderId: Int): Order {
        try {
            val response = provider.findOrderById(businessId, orderId)

            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            if (response.data is JsonObject) {
                return json.decodeFromJsonElement<Order>(response.data)
            } else {
                throw BadRequestException(response.toJson())
            }
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR, "findOrderById",
                    "Error finding order by id. Error: ${e.message ?: "UNKNOWN"}, businessId: $businessId, orderId: $orderId"
                )
            )
            throw e
        }
    }

    override suspend fun findOrderByCUFE(businessId: Int, cufe: String): Order {
        try {
            val response = provider.findOrderByCUFE(businessId, cufe)

            if (response.error.isError()) {
                throw BadRequestException(response.toJson())
            }

            if (response.data is JsonObject) {
                return json.decodeFromJsonElement<Order>(response.data)
            } else {
                throw BadRequestException(response.toJson())
            }
        } catch (e: Exception) {
            logger.sendLog(
                Log(
                    LogLevel.ERROR, "findOrderByCUFE",
                    "Error finding order by order number. Error: ${e.message ?: "UNKNOWN"}, businessId: $businessId, cufe: $cufe"
                )
            )
            throw e
        }
    }
}
