package com.teco.ventago.features.financialProfile

import com.teco.ventago.AppDistribution
import com.teco.ventago.core.cache.ICacheService
import com.teco.ventago.core.changes.IChangesManager
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.features.financialProfile.data.repository.IFinancialProfileRepository
import com.teco.ventago.features.financialProfile.domain.FinancialProfileService
import com.teco.ventago.features.financialProfile.domain.model.BusinessFinancialProfile
import com.teco.ventago.features.financialProfile.domain.model.PaymentSummary
import com.teco.ventago.features.pos.provisioning.data.repository.IPosDeviceProvisioningRepository
import com.teco.ventago.features.pos.provisioning.domain.IPosAgentConfigReader
import com.teco.ventago.features.pos.provisioning.domain.PosDeviceProvisioningService
import com.teco.ventago.features.pos.provisioning.domain.model.PosAgentConfigResult
import com.teco.ventago.features.pos.provisioning.domain.model.PosDeviceConfig
import com.teco.ventago.features.pos.provisioning.domain.model.PosDevicePermissions
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout

class FinancialProfileServiceTest {

    @Test
    fun setBusinessUsesMatchingCacheButStillRefreshesWhenRequested() = runTest {
        val cache = FakeCacheService(profile(businessId = 7, invoicingActive = false))
        val repository = FakeFinancialProfileRepository(profile(businessId = 7, invoicingActive = true))
        val service = createService(cache, repository, this)

        service.setBusiness(7, refresh = true)

        assertEquals(listOf(7), repository.calls)
        assertTrue(service.observe().value?.invoicingActive == true)
    }

    @Test
    fun setBusinessRefreshTrueRefreshesEvenWhenSameBusinessAlreadyHasState() = runTest {
        val repository = FakeFinancialProfileRepository(
            profile(businessId = 7, invoicingActive = true),
            profile(businessId = 7, invoicingActive = false),
        )
        val service = createService(FakeCacheService(), repository, this)

        service.setBusiness(7, refresh = true)
        service.setBusiness(7, refresh = true)

        assertEquals(listOf(7, 7), repository.calls)
        assertEquals(false, service.observe().value?.invoicingActive)
    }

    @Test
    fun setBusinessDoesNotPublishCacheFromAnotherBusiness() = runTest {
        val cache = FakeCacheService(profile(businessId = 99, invoicingActive = true))
        val repository = FakeFinancialProfileRepository(profile(businessId = 7, invoicingActive = false))
        val service = createService(cache, repository, this)

        service.setBusiness(7, refresh = false)

        assertNull(service.observe().value)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun setBusinessRefreshFalseUsesMatchingCacheWithoutBackendCall() = runTest {
        val cache = FakeCacheService(profile(businessId = 7, invoicingActive = true))
        val repository = FakeFinancialProfileRepository(profile(businessId = 7, invoicingActive = false))
        val service = createService(cache, repository, this)

        service.setBusiness(7, refresh = false)

        assertTrue(service.observe().value?.invoicingActive == true)
        assertTrue(repository.calls.isEmpty())
    }

    @Test
    fun refreshCachesProfileWithoutPublishingFinancialInvalidation() = runTest {
        val changesManager = FakeChangesManager()
        val cache = FakeCacheService()
        val repository = FakeFinancialProfileRepository(profile(businessId = 7, invoicingActive = true))
        val service = createService(cache, repository, this, changesManager)

        service.refresh(businessIdOpt = 7)
        cache.awaitSave()

        assertEquals(listOf(7), repository.calls)
        assertEquals(0, changesManager.financialChangedCalls)
    }

    @Test
    fun refreshUpdatesPosDeviceConfigWithoutPublishingFinancialInvalidation() = runTest {
        val changesManager = FakeChangesManager()
        val cache = FakeCacheService()
        val repository = FakeFinancialProfileRepository(profile(businessId = 7, invoicingActive = true))
        val posRepository = FakePosDeviceProvisioningRepository()
        val posService = PosDeviceProvisioningService(
            appDistribution = AppDistribution(isPosBuild = true),
            agentConfigReader = FakePosAgentConfigReader(),
            repository = posRepository,
            logger = NoopLoggerService(),
        )
        val service = createService(cache, repository, this, changesManager, posService)

        service.refresh(businessIdOpt = 7)
        cache.awaitSave()

        assertEquals(listOf(7), repository.calls)
        assertEquals(listOf("pos_123"), posRepository.calls)
        assertEquals(0, changesManager.financialChangedCalls)
    }

    private fun createService(
        cache: FakeCacheService,
        repository: FakeFinancialProfileRepository,
        appScope: CoroutineScope,
        changesManager: FakeChangesManager = FakeChangesManager(),
        posProvisioningService: PosDeviceProvisioningService = noopPosProvisioningService(),
    ): FinancialProfileService =
        FinancialProfileService(
            cache = cache,
            changesManager = changesManager,
            repo = repository,
            loggerService = NoopLoggerService(),
            appScope = appScope,
            posProvisioningService = posProvisioningService,
        )

    private fun noopPosProvisioningService(): PosDeviceProvisioningService =
        PosDeviceProvisioningService(
            appDistribution = AppDistribution(isPosBuild = false),
            agentConfigReader = FakePosAgentConfigReader(),
            repository = FakePosDeviceProvisioningRepository(),
            logger = NoopLoggerService(),
        )

    private fun profile(
        businessId: Int,
        invoicingActive: Boolean,
    ): BusinessFinancialProfile =
        BusinessFinancialProfile(
            businessId = businessId,
            invoicingActive = invoicingActive,
            invoiceSummary = null,
            paymentSummary = PaymentSummary(),
        )

    private class FakeFinancialProfileRepository(
        vararg responses: BusinessFinancialProfile,
    ) : IFinancialProfileRepository {
        private val responses = ArrayDeque(responses.toList())
        val calls = mutableListOf<Int>()

        override suspend fun getFinancialProfile(businessId: Int): BusinessFinancialProfile {
            calls += businessId
            return responses.removeFirst().copy(businessId = businessId)
        }
    }

    private class FakeCacheService(
        private var financialProfile: BusinessFinancialProfile? = null,
    ) : ICacheService {
        private val saves = Channel<Unit>(capacity = Channel.UNLIMITED)

        suspend fun awaitSave() {
            withTimeout(2_000) {
                saves.receive()
            }
        }

        @Suppress("UNCHECKED_CAST")
        override suspend fun <T : Any> getCache(klass: KClass<T>): T? =
            if (klass == BusinessFinancialProfile::class) financialProfile as T? else null

        override suspend fun <T : Any> getCache(key: String): T? = null

        override suspend fun <T> saveCache(data: T) {
            if (data is BusinessFinancialProfile) {
                financialProfile = data
                saves.trySend(Unit)
            }
        }

        override suspend fun <T> saveCache(key: String, data: T) = Unit

        override suspend fun clearCache(id: String) {
            financialProfile = null
        }

        override suspend fun clearAllCache() {
            financialProfile = null
        }
    }

    private class FakeChangesManager : IChangesManager {
        var financialChangedCalls = 0

        override fun productsListener(): Flow<Int> = emptyFlow()
        override fun businessListener(): Flow<Int> = emptyFlow()
        override fun financialListener(): Flow<Int> = emptyFlow()
        override fun customersListener(): Flow<Int> = emptyFlow()
        override fun branchesListener(): Flow<Int> = emptyFlow()
        override fun userListener(): Flow<Int> = emptyFlow()
        override fun purchaseListener(): Flow<Int> = emptyFlow()
        override fun addedBusinessListener(): Flow<Int> = emptyFlow()
        override suspend fun productsChanged() = Unit
        override suspend fun businessChanged() = Unit
        override suspend fun branchesChanged() = Unit
        override suspend fun financialChanged() {
            financialChangedCalls += 1
        }
        override suspend fun customersChanged() = Unit
        override suspend fun userChanged() = Unit
        override fun removeListeners() = Unit
        override fun initialize(businessId: Int, menuId: Int, userId: Int) = Unit
    }

    private class NoopLoggerService : ILoggerService {
        override fun sendLog(log: Log) = Unit
    }

    private class FakePosAgentConfigReader : IPosAgentConfigReader {
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

    private class FakePosDeviceProvisioningRepository : IPosDeviceProvisioningRepository {
        val calls = mutableListOf<String>()

        override suspend fun getPosConfig(deviceId: String, accessToken: String?): PosDeviceConfig {
            calls += deviceId
            return PosDeviceConfig(
                deviceId = deviceId,
                businessId = 7,
                branchCode = "0000",
                billingPointCode = "865",
                status = "active",
                permissions = PosDevicePermissions(deviceId = deviceId)
            )
        }
    }
}
