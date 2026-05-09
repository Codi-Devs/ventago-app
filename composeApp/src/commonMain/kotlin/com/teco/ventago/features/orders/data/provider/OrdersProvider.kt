package com.teco.ventago.features.orders.data.provider

import com.teco.ventago.Configs
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.orders.domain.models.requests.CancelOrderRequest
import com.teco.ventago.features.orders.domain.models.requests.CreatePaymentLinkRequest
import com.teco.ventago.features.orders.domain.models.requests.CreateOrderRequest
import com.teco.ventago.features.orders.domain.models.requests.DeleteOrderRequest
import com.teco.ventago.features.orders.domain.models.requests.FindOrderByIdRequest
import com.teco.ventago.features.orders.domain.models.requests.ListOrdersRequest
import com.teco.ventago.features.orders.domain.models.requests.RejectAchPaymentRequest
import com.teco.ventago.features.orders.domain.models.requests.RescheduleReceivablesRequest
import com.teco.ventago.features.orders.domain.models.requests.RegisterManualPaymentsRequest
import com.teco.ventago.features.orders.domain.models.requests.VoidOrderPaymentRequest
import com.teco.ventago.features.orders.domain.models.requests.toApiJsonString
import com.teco.ventago.utils.ApiError
import com.teco.ventago.utils.ApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

class OrdersProvider(private val client: HttpClient, private val authService: IAuthService) : IOrdersProvider {
    val json = Json {
        ignoreUnknownKeys = true // Optional: skip unknown fields
        isLenient = true
        encodeDefaults = true
    }

    override suspend fun createOrder(businessId: Int, createOrderRequest: CreateOrderRequest): ApiResponse {
        val res = client.post(Configs.ordersBasePath+"/api/v1/orders/create") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(createOrderRequest))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                createOrder(businessId, createOrderRequest)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    // POST /v1/orders/cancel
    override suspend fun cancelOrder(businessId: Int, request: CancelOrderRequest): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/orders/cancel") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CancelOrderRequest.serializer(), request))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body) // expected: {"success":true,"data":true,"error":null}
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                cancelOrder(businessId, request)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun deleteOrder(businessId: Int, request: DeleteOrderRequest): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/orders/delete") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(DeleteOrderRequest.serializer(), request))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001 || res.status == HttpStatusCode.Unauthorized) {
            return try {
                authService.refreshToken(client)
                deleteOrder(businessId, request)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun registerManualPayments(
        businessId: Int,
        orderId: Int,
        request: RegisterManualPaymentsRequest
    ): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/orders/$orderId/payments/manual") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                registerManualPayments(businessId, orderId, request)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun rescheduleOrderReceivables(
        businessId: Int,
        orderId: Int,
        request: RescheduleReceivablesRequest
    ): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/orders/$orderId/receivables/reschedule") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001 || res.status == HttpStatusCode.Unauthorized) {
            return try {
                authService.refreshToken(client)
                rescheduleOrderReceivables(businessId, orderId, request)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun voidOrderPayment(
        businessId: Int,
        paymentId: Long,
        request: VoidOrderPaymentRequest
    ): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/orders/payments/$paymentId/void") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001 || res.status == HttpStatusCode.Unauthorized) {
            return try {
                authService.refreshToken(client)
                voidOrderPayment(businessId, paymentId, request)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun retryElectronicInvoice(
        businessId: Int,
        orderId: Int,
    ): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/orders/$orderId/retry-invoice") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                retryElectronicInvoice(businessId, orderId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun getInvoiceDocsRaw(businessId: Int, cufe: String): ApiResponse {
        val res = client.get(Configs.ordersBasePath + "/api/v1/orders/invoices/docs") {
            url {
                parameters.append("cufe", cufe)
            }
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                getInvoiceDocsRaw(businessId, cufe)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun createPaymentLink(businessId: Int, request: CreatePaymentLinkRequest): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/payments/links") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CreatePaymentLinkRequest.serializer(), request))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                createPaymentLink(businessId, request)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun getAchPaymentByIntent(businessId: Int, paymentIntentId: String): ApiResponse {
        val res = client.get(Configs.ordersBasePath + "/api/v1/payments/ach/payments/$paymentIntentId") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                getAchPaymentByIntent(businessId, paymentIntentId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun approveAchPayment(businessId: Int, paymentIntentId: String): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/payments/ach/payments/$paymentIntentId/approve") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { })
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                approveAchPayment(businessId, paymentIntentId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun rejectAchPayment(
        businessId: Int,
        paymentIntentId: String,
        request: RejectAchPaymentRequest
    ): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/payments/ach/payments/$paymentIntentId/reject") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(RejectAchPaymentRequest.serializer(), request))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                rejectAchPayment(businessId, paymentIntentId, request)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun downloadAchProofFile(
        businessId: Int,
        paymentId: String,
        proofId: String
    ): BinaryPayload {
        val res = client.get(
            Configs.ordersBasePath + "/api/v1/payments/ach/payments/$paymentId/proofs/$proofId/file"
        ) {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append("X-Business-ID", "$businessId")
            }
        }

        if (res.status == HttpStatusCode.Unauthorized) {
            authService.refreshToken(client)
            return downloadAchProofFile(businessId, paymentId, proofId)
        }

        val contentDisposition = res.headers[HttpHeaders.ContentDisposition]
        val fileName = contentDisposition
            ?.substringAfter("filename=", "")
            ?.trim()
            ?.trim('"')
            ?.ifBlank { null }

        return BinaryPayload(
            bytes = res.body(),
            contentType = res.headers[HttpHeaders.ContentType],
            fileName = fileName
        )
    }


    override suspend fun loadOrders(
        request: ListOrdersRequest
    ): ApiResponse {
        val res = client.post(Configs.ordersBasePath+"/api/v1/orders/get-orders") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "${request.businessId}")
            }
            contentType(ContentType.Application.Json)
            setBody(request.toApiJsonString())
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001 || res.status == HttpStatusCode.Unauthorized) {
            return try {
                authService.refreshToken(client)
                loadOrders(request)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun findOrderByOrderNumber(businessId: Int, orderNumber: String): ApiResponse {
        val res = client.post(Configs.ordersBasePath+"/api/v1/orders/find/order-number") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(OrdersRequests.findOrderByOrderNumber(businessId, orderNumber))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001 || res.status == HttpStatusCode.Unauthorized) {
            return try {
                authService.refreshToken(client)
                findOrderByOrderNumber(businessId, orderNumber)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun findOrderById(businessId: Int, orderId: Int): ApiResponse {
        val res = client.post(Configs.ordersBasePath + "/api/v1/orders/find/order-id") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(FindOrderByIdRequest(orderId = orderId)))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001 || res.status == HttpStatusCode.Unauthorized) {
            return try {
                authService.refreshToken(client)
                findOrderById(businessId, orderId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun findOrderByCUFE(businessId: Int, cufe: String): ApiResponse {
        val res = client.post(Configs.ordersBasePath+"/api/v1/orders/find/cufe") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
                append("X-Business-ID", "$businessId")
            }
            contentType(ContentType.Application.Json)
            setBody(OrdersRequests.findOrderByCUFE(businessId, cufe))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001 || res.status == HttpStatusCode.Unauthorized) {
            return try {
                authService.refreshToken(client)
                findOrderByCUFE(businessId, cufe)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }


    // ===== OLD METHODS. CHECK IF SILL NEEDED =====

    override suspend fun isBusinessRegistered(businessId: Int): ApiResponse {
        val res = client.post(Configs.ordersBasePath+"/api/business/registered") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
            }
            contentType(ContentType.Application.Json)
            setBody(OrdersRequests.isBusinessRegistered(businessId))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                isBusinessRegistered(businessId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun changeOrderStatus(order: Order, status: Int, businessId: Int): ApiResponse {
        val res = client.post(Configs.ordersBasePath+"/api/orders/status/change") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
            }
            contentType(ContentType.Application.Json)
            setBody(OrdersRequests.changeOrderStatus(order.id, status, businessId))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                changeOrderStatus(order, status, businessId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun rejectOrder(order: Order, reason: String, businessId: Int): ApiResponse {
        val res = client.post(Configs.ordersBasePath+"/api/orders/reject") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
            }
            contentType(ContentType.Application.Json)
            setBody(OrdersRequests.rejectOrder(order.id, reason, businessId))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                rejectOrder(order, reason, businessId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun changeOrdersEnabled(businessId: Int, enabled: Boolean): ApiResponse {
        val res = client.post(Configs.ordersBasePath+"/api/business/enable") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
            }
            contentType(ContentType.Application.Json)
            setBody(OrdersRequests.changeOrdersEnabled(businessId, enabled))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                changeOrdersEnabled(businessId, enabled)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun getBusinessConfig(businessId: Int): ApiResponse {
        val res = client.post(Configs.ordersBasePath+"/api/business/config") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
            }
            contentType(ContentType.Application.Json)
            setBody(OrdersRequests.isBusinessRegistered(businessId))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                getBusinessConfig(businessId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }


    override suspend fun loadCustomers(businessId: Int, pageSize: Int, page: Int): ApiResponse {
        val res = client.post(Configs.ordersBasePath+"/api/customer/") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
            }
            contentType(ContentType.Application.Json)
            setBody(OrdersRequests.loadOrders(businessId, pageSize, page))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                loadCustomers(businessId, pageSize, page)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun loadChangedCustomers(businessId: Int, lastUpdate: String): ApiResponse {
        val res = client.get(Configs.ordersBasePath+"/api/customer/changed?businessId=$businessId&since=$lastUpdate") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
            }
            contentType(ContentType.Application.Json)
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                loadChangedCustomers(businessId, lastUpdate)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun removeCustomer(businessId: Int, customerId: Int): ApiResponse {
        val res = client.patch(Configs.ordersBasePath+"/api/customer/remove?businessId=$businessId&customerId=$customerId") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
            }
            contentType(ContentType.Application.Json)
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                removeCustomer(businessId, customerId)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }

    override suspend fun getOrderPaymentLink(orderID: Int): ApiResponse {
        val res = client.get("${Configs.ordersBasePath}/api/pos/orders/$orderID/payment-link") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
            }
            contentType(ContentType.Application.Json)
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)

        // Retry on expired token
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                getOrderPaymentLink(orderID)
            } catch (e: Exception) {
                response
            }
        }

        return response
    }



    override suspend fun registerManualTransference(businessId: Int, orderNumber: String, paymentReference: String, description: String): ApiResponse {
        val res = client.post(Configs.ordersBasePath+"/api/pos/orders/manual-payment/register") {
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.Authorization, "Bearer ${authService.getJwtToken()}")
                append(HttpHeaders.ContentType, "application/json")
            }
            contentType(ContentType.Application.Json)
            setBody(OrdersRequests.registerManualTransference(businessId, orderNumber, paymentReference, description))
        }

        val body = res.body<JsonObject>()
        val response = ApiResponse.fromJson(body)
        if (response.error == ApiError.AUTH_001) {
            return try {
                authService.refreshToken(client)
                registerManualTransference(businessId, orderNumber, paymentReference, description)
            } catch (e: Exception) {
                response
            }
        }
        return response
    }
}
