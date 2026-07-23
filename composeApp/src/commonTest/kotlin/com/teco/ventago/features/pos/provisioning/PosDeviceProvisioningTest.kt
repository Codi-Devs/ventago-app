package com.teco.ventago.features.pos.provisioning

import com.teco.ventago.AppDistribution
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.features.auth.domain.model.response.AuthResponse
import com.teco.ventago.features.auth.domain.model.response.BusinessIds
import com.teco.ventago.features.pos.provisioning.data.provider.IPosDeviceProvisioningProvider
import com.teco.ventago.features.pos.provisioning.data.repository.IPosDeviceProvisioningRepository
import com.teco.ventago.features.pos.provisioning.data.repository.PosDeviceProvisioningRepository
import com.teco.ventago.features.pos.provisioning.domain.IPosAgentConfigReader
import com.teco.ventago.features.pos.provisioning.domain.PosDeviceProvisioningService
import com.teco.ventago.features.pos.provisioning.domain.PosProvisioningException
import com.teco.ventago.features.pos.provisioning.domain.model.PosAgentConfigResult
import com.teco.ventago.features.pos.provisioning.domain.model.PosDeviceConfig
import com.teco.ventago.features.pos.provisioning.domain.model.PosDevicePermissions
import com.teco.ventago.utils.ApiResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class PosDeviceProvisioningTest {
    @Test
    fun repositoryParsesPublicPosConfigResponseWithUnknownFields() = runTest {
        val body = Json.parseToJsonElement(
            """
            {
              "success": true,
              "data": {
                "device_id": "pos_123",
                "business_id": 7,
                "branch_code": "0000",
                "billing_point_code": "865",
                "status": "active",
                "unknown": "ignored",
                "permissions": {
                  "device_id": "pos_123",
                  "expenses_view": true,
                  "expenses_create": false,
                  "products_view": true,
                  "products_create": true,
                  "clients_view": true,
                  "clients_create": false,
                  "quotes_view": false,
                  "quotes_create": false,
                  "payment_methods_configure": true,
                  "payment_yappy_onsite": true,
                  "payment_link": false,
                  "payment_manual_methods": true,
                  "reports_view": true
                }
              }
            }
            """.trimIndent()
        ).jsonObject
        val provider = FakeProvider(ApiResponse.fromJson(body))
        val repository = PosDeviceProvisioningRepository(
            provider = provider,
            logger = NoopLoggerService()
        )

        val config = repository.getPosConfig("pos_123", "access-token")

        assertEquals("pos_123", config.deviceId)
        assertEquals(7, config.businessId)
        assertEquals("0000", config.branchCode)
        assertEquals("865", config.billingPointCode)
        assertTrue(config.active)
        assertTrue(config.permissions.productsCreate)
        assertTrue(config.permissions.paymentMethodsConfigure)
        assertTrue(config.permissions.paymentYappyOnsite)
        assertTrue(config.permissions.paymentManualMethods)
        assertEquals(false, config.permissions.paymentLink)
        assertEquals("access-token", provider.accessTokens.single())
    }

    @Test
    fun validateLoginAcceptsMatchingProvisionedBusiness() = runTest {
        val repository = FakeRepository(
            PosDeviceConfig(
                deviceId = "pos_123",
                businessId = 7,
                branchCode = "0000",
                billingPointCode = "865",
                status = "active",
                permissions = PosDevicePermissions(deviceId = "pos_123", productsView = true)
            )
        )
        val service = provisioningService(repository = repository)

        service.validateLogin(authResponse(businessId = 7))

        assertTrue(service.currentState().isProvisioned)
        assertEquals("0000", service.currentState().fixedBranchCode)
        assertEquals("865", service.currentState().fixedBillingPointCode)
        assertEquals("access", repository.accessTokens.single())
    }

    @Test
    fun validateLoginDoesNotPublishInvalidIntermediateAgentStateWhileBackendConfigIsPending() = runTest {
        val backendRelease = CompletableDeferred<Unit>()
        val repository = BlockingRepository(backendRelease)
        val service = provisioningService(repository = repository)

        val validation = async {
            service.validateLogin(authResponse(businessId = 7))
        }

        repository.requestStarted.await()
        val pendingState = service.currentState()

        assertNull(pendingState.agentConfig)
        assertFalse(pendingState.isProvisioned)

        backendRelease.complete(Unit)
        validation.await()
        assertTrue(service.currentState().isProvisioned)
    }

    @Test
    fun validateLoginRejectsMismatchedBusiness() = runTest {
        val service = provisioningService()

        assertFailsWith<PosProvisioningException.BusinessMismatch> {
            service.validateLogin(authResponse(businessId = 8))
        }
    }

    @Test
    fun validateLoginRejectsInactiveBackendDevice() = runTest {
        val service = provisioningService(
            repository = FakeRepository(
                PosDeviceConfig(
                    deviceId = "pos_123",
                    businessId = 7,
                    branchCode = "0000",
                    billingPointCode = "865",
                    status = "inactive",
                    permissions = PosDevicePermissions(deviceId = "pos_123")
                )
            )
        )

        assertFailsWith<PosProvisioningException.DeviceInactive> {
            service.validateLogin(authResponse(businessId = 7))
        }
    }

    @Test
    fun publicDistributionSkipsProvisioningValidation() = runTest {
        val service = PosDeviceProvisioningService(
            appDistribution = AppDistribution(isPosBuild = false),
            agentConfigReader = FakeAgentReader(activated = false),
            repository = FakeRepository(),
            logger = NoopLoggerService()
        )

        service.validateLogin(authResponse(businessId = 8))

        assertTrue(service.currentState().isProvisioned)
    }

    private fun provisioningService(
        repository: IPosDeviceProvisioningRepository = FakeRepository(),
    ): PosDeviceProvisioningService =
        PosDeviceProvisioningService(
            appDistribution = AppDistribution(isPosBuild = true),
            agentConfigReader = FakeAgentReader(),
            repository = repository,
            logger = NoopLoggerService()
        )

    private fun authResponse(businessId: Int): AuthResponse =
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
            businesses = listOf(BusinessIds(businessId = businessId, menuId = 1))
        )

    private class FakeAgentReader(
        private val activated: Boolean = true,
    ) : IPosAgentConfigReader {
        override suspend fun getDeviceConfig(): PosAgentConfigResult =
            PosAgentConfigResult(
                activated = activated,
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

    private class FakeRepository(
        private val config: PosDeviceConfig = PosDeviceConfig(
            deviceId = "pos_123",
            businessId = 7,
            branchCode = "0000",
            billingPointCode = "865",
            status = "active",
            permissions = PosDevicePermissions(deviceId = "pos_123")
        )
    ) : IPosDeviceProvisioningRepository {
        val accessTokens = mutableListOf<String?>()

        override suspend fun getPosConfig(deviceId: String, accessToken: String?): PosDeviceConfig {
            accessTokens += accessToken
            return config
        }
    }

    private class BlockingRepository(
        private val release: CompletableDeferred<Unit>,
    ) : IPosDeviceProvisioningRepository {
        val requestStarted = CompletableDeferred<Unit>()

        override suspend fun getPosConfig(deviceId: String, accessToken: String?): PosDeviceConfig {
            requestStarted.complete(Unit)
            release.await()
            return PosDeviceConfig(
                deviceId = "pos_123",
                businessId = 7,
                branchCode = "0000",
                billingPointCode = "865",
                status = "active",
                permissions = PosDevicePermissions(deviceId = "pos_123")
            )
        }
    }

    private class FakeProvider(
        private val response: ApiResponse,
    ) : IPosDeviceProvisioningProvider {
        val accessTokens = mutableListOf<String?>()

        override suspend fun getPosConfig(deviceId: String, accessToken: String?): ApiResponse {
            accessTokens += accessToken
            return response
        }
    }

    private class NoopLoggerService : ILoggerService {
        override fun sendLog(log: Log) = Unit
    }
}
