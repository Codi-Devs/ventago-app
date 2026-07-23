package com.teco.ventago.features.orders

import com.teco.ventago.AppDistribution
import com.teco.ventago.core.Paged
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.features.auth.domain.model.response.AuthResponse
import com.teco.ventago.features.auth.domain.model.response.BusinessIds
import com.teco.ventago.features.orders.data.repository.IOrdersRepository
import com.teco.ventago.features.orders.domain.OrderService
import com.teco.ventago.features.orders.domain.models.AchPaymentDetail
import com.teco.ventago.features.orders.domain.models.AchProofFileDownload
import com.teco.ventago.features.orders.domain.models.ChangeOrderStatusResponse
import com.teco.ventago.features.orders.domain.models.IsBusinessRegisteredResponse
import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.orders.domain.models.requests.CancelOrderRequest
import com.teco.ventago.features.orders.domain.models.requests.CreateOrderRequest
import com.teco.ventago.features.orders.domain.models.requests.CreatePaymentLinkRequest
import com.teco.ventago.features.orders.domain.models.requests.DeleteOrderRequest
import com.teco.ventago.features.orders.domain.models.requests.ListOrdersRequest
import com.teco.ventago.features.orders.domain.models.requests.PendingIntentCreateRequest
import com.teco.ventago.features.orders.domain.models.requests.PendingIntentCreateResponse
import com.teco.ventago.features.orders.domain.models.requests.PendingIntentReleaseRequest
import com.teco.ventago.features.orders.domain.models.requests.PendingIntentReleaseResponse
import com.teco.ventago.features.orders.domain.models.requests.RegisterManualPaymentsDataResponse
import com.teco.ventago.features.orders.domain.models.requests.RegisterManualPaymentsRequest
import com.teco.ventago.features.orders.domain.models.requests.RejectAchPaymentRequest
import com.teco.ventago.features.orders.domain.models.requests.RescheduleReceivablesRequest
import com.teco.ventago.features.orders.domain.models.requests.RescheduleReceivablesResponse
import com.teco.ventago.features.orders.domain.models.requests.RetryInvoiceResponse
import com.teco.ventago.features.orders.domain.models.requests.VoidOrderPaymentRequest
import com.teco.ventago.features.orders.domain.models.requests.VoidOrderPaymentResponse
import com.teco.ventago.features.orders.domain.models.responses.CreateOrderResponse
import com.teco.ventago.features.orders.domain.models.responses.InvoiceDocsDto
import com.teco.ventago.features.pos.provisioning.data.repository.IPosDeviceProvisioningRepository
import com.teco.ventago.features.pos.provisioning.domain.IPosAgentConfigReader
import com.teco.ventago.features.pos.provisioning.domain.PosDeviceProvisioningService
import com.teco.ventago.features.pos.provisioning.domain.model.PosAgentConfigResult
import com.teco.ventago.features.pos.provisioning.domain.model.PosDeviceConfig
import com.teco.ventago.features.pos.provisioning.domain.model.PosDevicePermissions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class OrderServiceProvisioningTest {
    @Test
    fun posOrderLoadsUseProvisionedBranchAndBillingPointFilters() = runTest {
        val ordersRepository = CapturingOrdersRepository()
        val provisioningService = provisioningService(isPosBuild = true)
        provisioningService.validateLogin(authResponse())
        val service = OrderService(ordersRepository, provisioningService)

        service.loadOrders(businessId = 7, paymentStatus = 1)

        val request = ordersRepository.requests.single()
        assertEquals("0000", request.branchCode)
        assertEquals("865", request.billingPointCode)
        assertEquals(1, request.paymentStatus)
    }

    @Test
    fun posOrderLoadsDoNotCallRepositoryWithoutValidProvisioning() = runTest {
        val ordersRepository = CapturingOrdersRepository()
        val service = OrderService(ordersRepository, provisioningService(isPosBuild = true))

        val result = service.loadOrders(businessId = 7)

        assertTrue(result.isEmpty())
        assertTrue(ordersRepository.requests.isEmpty())
    }

    @Test
    fun publicOrderLoadsKeepUnscopedBranchAndBillingFilters() = runTest {
        val ordersRepository = CapturingOrdersRepository()
        val service = OrderService(ordersRepository, provisioningService(isPosBuild = false))

        service.loadOrders(businessId = 7)

        val request = ordersRepository.requests.single()
        assertNull(request.branchCode)
        assertNull(request.billingPointCode)
    }

    private fun provisioningService(isPosBuild: Boolean): PosDeviceProvisioningService =
        PosDeviceProvisioningService(
            appDistribution = AppDistribution(isPosBuild = isPosBuild),
            agentConfigReader = FakeAgentReader(),
            repository = FakeProvisioningRepository(),
            logger = NoopLoggerService()
        )

    private fun authResponse(): AuthResponse =
        AuthResponse(
            uid = "uid",
            email = "test@example.com",
            name = "Tester",
            premium = false,
            active = true,
            missingBusiness = false,
            userId = 1,
            accessToken = "access",
            refreshToken = "refresh",
            providerToken = "provider",
            message = "",
            businesses = listOf(BusinessIds(businessId = 7, menuId = 1))
        )

    private class FakeAgentReader : IPosAgentConfigReader {
        override suspend fun getDeviceConfig(): PosAgentConfigResult =
            PosAgentConfigResult(
                activated = true,
                configJson = """
                    {
                      "device_id": "pos_123",
                      "business_id": 7,
                      "branch_code": "0000",
                      "billing_point_code": "865"
                    }
                """.trimIndent()
            )
    }

    private class FakeProvisioningRepository : IPosDeviceProvisioningRepository {
        override suspend fun getPosConfig(deviceId: String, accessToken: String?): PosDeviceConfig =
            PosDeviceConfig(
                deviceId = "pos_123",
                businessId = 7,
                branchCode = "0000",
                billingPointCode = "865",
                status = "active",
                permissions = PosDevicePermissions(deviceId = "pos_123")
            )
    }

    private class CapturingOrdersRepository : IOrdersRepository {
        val requests = mutableListOf<ListOrdersRequest>()

        override suspend fun loadOrders(request: ListOrdersRequest): List<Order> {
            requests += request
            return emptyList()
        }

        override suspend fun loadOrdersPaged(request: ListOrdersRequest): Paged<Order> {
            requests += request
            return Paged(page = request.page, size = request.pageSize, total = 0, items = emptyList())
        }

        override suspend fun isBusinessRegistered(businessId: Int): IsBusinessRegisteredResponse = unused()
        override suspend fun changeOrderStatus(order: Order, status: Int, businessId: Int): ChangeOrderStatusResponse = unused()
        override suspend fun rejectOrder(order: Order, reason: String, businessId: Int): ChangeOrderStatusResponse = unused()
        override suspend fun changeOrdersEnabled(businessId: Int, enabled: Boolean): Boolean = unused()
        override suspend fun createOrder(businessId: Int, createOrderRequest: CreateOrderRequest): CreateOrderResponse = unused()
        override suspend fun cancelOrder(businessId: Int, request: CancelOrderRequest): Boolean = unused()
        override suspend fun deleteOrder(businessId: Int, request: DeleteOrderRequest): Boolean = unused()
        override suspend fun registerManualPayments(
            businessId: Int,
            orderId: Int,
            request: RegisterManualPaymentsRequest
        ): RegisterManualPaymentsDataResponse = unused()
        override suspend fun rescheduleOrderReceivables(
            businessId: Int,
            orderId: Int,
            request: RescheduleReceivablesRequest
        ): RescheduleReceivablesResponse = unused()
        override suspend fun voidOrderPayment(
            businessId: Int,
            paymentId: Long,
            request: VoidOrderPaymentRequest
        ): VoidOrderPaymentResponse = unused()
        override suspend fun retryElectronicInvoice(businessId: Int, orderId: Int): RetryInvoiceResponse = unused()
        override suspend fun getInvoiceDocsRaw(businessId: Int, cufe: String): InvoiceDocsDto = unused()
        override suspend fun createPaymentLink(businessId: Int, request: CreatePaymentLinkRequest): String? = unused()
        override suspend fun releasePendingPaymentIntent(
            businessId: Int,
            orderId: Int,
            request: PendingIntentReleaseRequest
        ): PendingIntentReleaseResponse = unused()
        override suspend fun createPendingPaymentIntent(
            businessId: Int,
            orderId: Int,
            request: PendingIntentCreateRequest
        ): PendingIntentCreateResponse = unused()
        override suspend fun getAchPaymentByIntent(businessId: Int, paymentIntentId: String): AchPaymentDetail = unused()
        override suspend fun approveAchPayment(businessId: Int, paymentIntentId: String): String = unused()
        override suspend fun rejectAchPayment(
            businessId: Int,
            paymentIntentId: String,
            request: RejectAchPaymentRequest
        ): String = unused()
        override suspend fun downloadAchProofFile(
            businessId: Int,
            paymentId: String,
            proofId: String
        ): AchProofFileDownload = unused()
        override suspend fun removeCustomer(businessId: Int, customerId: Int): Boolean = unused()
        override suspend fun getOrderPaymentLink(orderID: Int): String? = unused()
        override suspend fun registerManualTransference(
            businessId: Int,
            orderNumber: String,
            paymentReference: String,
            description: String
        ): Boolean = unused()
        override suspend fun findOrderByOrderNumber(businessId: Int, orderNumber: String): Order = unused()
        override suspend fun findOrderById(businessId: Int, orderId: Int): Order = unused()
        override suspend fun findOrderByCUFE(businessId: Int, cufe: String): Order = unused()

        private fun <T> unused(): T = error("Unused repository method in this test")
    }

    private class NoopLoggerService : ILoggerService {
        override fun sendLog(log: Log) = Unit
    }
}
