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
import com.teco.ventago.features.pos.provisioning.domain.IPosDeviceBindingStore
import com.teco.ventago.features.pos.provisioning.domain.InMemoryPosDeviceBindingStore
import com.teco.ventago.features.pos.provisioning.domain.PosDeviceProvisioningService
import com.teco.ventago.features.pos.provisioning.domain.PosProvisioningException
import com.teco.ventago.features.pos.provisioning.domain.model.PosAgentConfigResult
import com.teco.ventago.features.pos.provisioning.domain.model.PosAgentDeviceConfig
import com.teco.ventago.features.pos.provisioning.domain.model.PosDeviceConfig
import com.teco.ventago.features.pos.provisioning.domain.model.PosDevicePermissions
import com.teco.ventago.features.pos.provisioning.domain.model.PosLinkMode
import com.teco.ventago.features.pos.provisioning.domain.model.PosProvisioningState
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
        val bindingStore = InMemoryPosDeviceBindingStore()
        val repository = FakeRepository()
        val service = provisioningService(repository = repository, bindingStore = bindingStore)

        service.validateLogin(authResponse(businessId = 7))

        val state = service.currentState()
        assertTrue(state.isProvisioned)
        assertTrue(state.locksBranchPoint)
        assertEquals(PosLinkMode.Linked, state.linkMode)
        assertEquals("0000", state.fixedBranchCode)
        assertEquals("865", state.fixedBillingPointCode)
        assertNull(state.bannerMessage)
        assertEquals("pos_123", bindingStore.load()?.deviceId)
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
        assertEquals(PosLinkMode.Linked, service.currentState().linkMode)
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
            agentConfigReader = FakeAgentReader(result = null),
            repository = FakeRepository(),
            logger = NoopLoggerService(),
            bindingStore = InMemoryPosDeviceBindingStore(),
        )

        service.validateLogin(authResponse(businessId = 8))

        assertTrue(service.currentState().isProvisioned)
        assertEquals(PosLinkMode.NotRequired, service.currentState().linkMode)
        assertFalse(service.currentState().locksBranchPoint)
    }

    @Test
    fun validateLoginFallsBackToCachedDeviceWhenAgentIsDown() = runTest {
        val bindingStore = InMemoryPosDeviceBindingStore(
            initial = PosAgentDeviceConfig(
                deviceId = "pos_123",
                businessId = 7,
                branchCode = "0000",
                billingPointCode = "865",
            )
        )
        val service = provisioningService(
            agentConfigReader = FakeAgentReader(result = null),
            bindingStore = bindingStore,
        )

        service.validateLogin(authResponse(businessId = 7))

        val state = service.currentState()
        assertTrue(state.isProvisioned)
        assertTrue(state.locksBranchPoint)
        assertEquals(PosLinkMode.Degraded, state.linkMode)
        assertEquals(PosProvisioningState.BANNER_DEGRADED, state.bannerMessage)
        assertEquals("0000", state.fixedBranchCode)
        assertEquals("865", state.fixedBillingPointCode)
    }

    @Test
    fun validateLoginEntersUnlinkedModeWhenAgentAndCacheAreMissing() = runTest {
        val service = provisioningService(
            agentConfigReader = FakeAgentReader(result = null),
            bindingStore = InMemoryPosDeviceBindingStore(),
        )

        service.validateLogin(authResponse(businessId = 7))

        val state = service.currentState()
        assertTrue(state.isProvisioned)
        assertFalse(state.locksBranchPoint)
        assertEquals(PosLinkMode.Unlinked, state.linkMode)
        assertNull(state.bannerMessage)
        assertNull(state.fixedBranchCode)
    }

    @Test
    fun validateLoginUsesCachedDeviceWhenAgentReportsInactive() = runTest {
        val bindingStore = InMemoryPosDeviceBindingStore(
            initial = PosAgentDeviceConfig(
                deviceId = "pos_123",
                businessId = 7,
                branchCode = "0000",
                billingPointCode = "865",
            )
        )
        val service = provisioningService(
            agentConfigReader = FakeAgentReader(
                result = PosAgentConfigResult(activated = false, configJson = "{}")
            ),
            bindingStore = bindingStore,
        )

        service.validateLogin(authResponse(businessId = 7))

        assertEquals(PosLinkMode.Degraded, service.currentState().linkMode)
        assertTrue(service.currentState().locksBranchPoint)
    }

    @Test
    fun validateLoginKeepsCachedScopeWhenPosConfigIsUnavailable() = runTest {
        val bindingStore = InMemoryPosDeviceBindingStore(
            initial = PosAgentDeviceConfig(
                deviceId = "pos_123",
                businessId = 7,
                branchCode = "0000",
                billingPointCode = "865",
            )
        )
        val service = provisioningService(
            agentConfigReader = FakeAgentReader(result = null),
            repository = FailingRepository(),
            bindingStore = bindingStore,
        )

        service.validateLogin(authResponse(businessId = 7))

        val state = service.currentState()
        assertTrue(state.isProvisioned)
        assertTrue(state.locksBranchPoint)
        assertEquals(PosLinkMode.Degraded, state.linkMode)
        assertEquals("0000", state.fixedBranchCode)
    }

    @Test
    fun validateLoginPrefersBackendScopeWhenCachedCodesDiffer() = runTest {
        val bindingStore = InMemoryPosDeviceBindingStore(
            initial = PosAgentDeviceConfig(
                deviceId = "pos_123",
                businessId = 7,
                branchCode = "0000",
                billingPointCode = "001",
            )
        )
        val service = provisioningService(
            agentConfigReader = FakeAgentReader(result = null),
            repository = FakeRepository(
                PosDeviceConfig(
                    deviceId = "pos_123",
                    businessId = 7,
                    branchCode = "0001",
                    billingPointCode = "002",
                    status = "active",
                    permissions = PosDevicePermissions(deviceId = "pos_123")
                )
            ),
            bindingStore = bindingStore,
        )

        service.validateLogin(authResponse(businessId = 7))

        val state = service.currentState()
        assertEquals(PosLinkMode.Degraded, state.linkMode)
        assertEquals("0001", state.fixedBranchCode)
        assertEquals("002", state.fixedBillingPointCode)
        assertEquals("0001", bindingStore.load()?.branchCode)
    }

    @Test
    fun validateLoginRejectsLiveAgentMismatchWithBackend() = runTest {
        val service = provisioningService(
            repository = FakeRepository(
                PosDeviceConfig(
                    deviceId = "pos_123",
                    businessId = 7,
                    branchCode = "9999",
                    billingPointCode = "999",
                    status = "active",
                    permissions = PosDevicePermissions(deviceId = "pos_123")
                )
            )
        )

        assertFailsWith<PosProvisioningException.DeviceConfigMismatch> {
            service.validateLogin(authResponse(businessId = 7))
        }
    }

    private fun provisioningService(
        repository: IPosDeviceProvisioningRepository = FakeRepository(),
        agentConfigReader: IPosAgentConfigReader = FakeAgentReader(),
        bindingStore: IPosDeviceBindingStore = InMemoryPosDeviceBindingStore(),
    ): PosDeviceProvisioningService =
        PosDeviceProvisioningService(
            appDistribution = AppDistribution(isPosBuild = true),
            agentConfigReader = agentConfigReader,
            repository = repository,
            logger = NoopLoggerService(),
            bindingStore = bindingStore,
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
        private val result: PosAgentConfigResult? = PosAgentConfigResult(
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
    ) : IPosAgentConfigReader {
        override suspend fun getDeviceConfig(): PosAgentConfigResult? = result
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

    private class FailingRepository : IPosDeviceProvisioningRepository {
        override suspend fun getPosConfig(deviceId: String, accessToken: String?): PosDeviceConfig {
            error("pos-config unavailable")
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
