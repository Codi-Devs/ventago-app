package com.teco.ventago.features.branches

import com.teco.ventago.core.cache.CacheUtils
import com.teco.ventago.core.cache.ICacheService
import com.teco.ventago.core.changes.IChangesManager
import com.teco.ventago.core.file.SharedFile
import com.teco.ventago.features.branches.data.repository.IBranchRepository
import com.teco.ventago.features.branches.domain.BranchService
import com.teco.ventago.features.branches.domain.model.Branch
import com.teco.ventago.features.branches.domain.model.FiscalBillingPoint
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout

class BranchServiceRefreshTest {

    @Test
    fun refreshCachesBranchesWithoutPublishingBranchInvalidation() = runTest {
        val changesManager = FakeChangesManager()
        val cache = FakeCacheService()
        val repository = FakeBranchRepository(refreshedBranches = listOf(branch()))
        val service = BranchService(
            branchRepository = repository,
            cache = cache,
            changesManager = changesManager,
            appScope = this,
        )

        service.refresh(businessId = 123)
        cache.awaitSave()

        assertEquals(1, repository.getBranchesCalls)
        assertEquals(0, changesManager.branchesChangedCalls)
    }

    @Test
    fun localBillingPointMutationPublishesBranchInvalidation() = runTest {
        val changesManager = FakeChangesManager()
        val cache = FakeCacheService()
        val service = BranchService(
            branchRepository = FakeBranchRepository(refreshedBranches = listOf(branch())),
            cache = cache,
            changesManager = changesManager,
            appScope = this,
        )
        service.refresh(businessId = 123)
        cache.awaitSave()

        service.addBillingPoint(
            businessId = 123,
            branchCode = "0000",
            name = "Caja 2",
        )
        cache.awaitSave()
        changesManager.awaitBranchesChanged()

        assertEquals(1, changesManager.branchesChangedCalls)
    }

    private fun branch(): Branch {
        return Branch(
            branchCode = "0000",
            name = "Sucursal Principal",
            addressLine = "Calle 50",
            locationCode = "",
            longitude = "",
            latitude = "",
            status = 1,
            fiscalBillingPoints = emptyList(),
            tradeName = "VentaGo Centro",
            logoUrl = null,
        )
    }

    private class FakeBranchRepository(
        private val refreshedBranches: List<Branch>,
    ) : IBranchRepository {
        var getBranchesCalls = 0

        override suspend fun getBranches(businessId: Int): List<Branch> {
            getBranchesCalls += 1
            return refreshedBranches
        }

        override suspend fun addBillingPoint(
            businessId: Int,
            branchCode: String,
            name: String,
            code: String,
            status: Int,
        ): FiscalBillingPoint {
            return FiscalBillingPoint(code, name, status)
        }

        override suspend fun updateBillingPoint(
            businessId: Int,
            branchCode: String,
            billingPoint: String,
            name: String,
            status: Int,
        ): Boolean = true

        override suspend fun uploadBranchLogo(
            businessId: Int,
            branchCode: String,
            logo: SharedFile,
        ): Boolean = true

        override suspend fun deleteBranchLogo(businessId: Int, branchCode: String): Boolean = true
    }

    private class FakeCacheService : ICacheService {
        private var branches: List<Branch>? = null
        private val saves = Channel<Unit>(capacity = Channel.UNLIMITED)

        suspend fun awaitSave() {
            withTimeout(2_000) {
                saves.receive()
            }
        }

        override suspend fun <T : Any> getCache(klass: KClass<T>): T? = null

        @Suppress("UNCHECKED_CAST")
        override suspend fun <T : Any> getCache(key: String): T? =
            if (key == CacheUtils.BRANCHES) branches as T? else null

        override suspend fun <T> saveCache(data: T) = Unit

        @Suppress("UNCHECKED_CAST")
        override suspend fun <T> saveCache(key: String, data: T) {
            if (key == CacheUtils.BRANCHES) {
                branches = data as List<Branch>
                saves.trySend(Unit)
            }
        }

        override suspend fun clearCache(id: String) = Unit
        override suspend fun clearAllCache() = Unit
    }

    private class FakeChangesManager : IChangesManager {
        var branchesChangedCalls = 0
        private val branchChanges = Channel<Unit>(capacity = Channel.UNLIMITED)

        suspend fun awaitBranchesChanged() {
            withTimeout(2_000) {
                branchChanges.receive()
            }
        }

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
        override suspend fun branchesChanged() {
            branchesChangedCalls += 1
            branchChanges.trySend(Unit)
        }
        override suspend fun financialChanged() = Unit
        override suspend fun customersChanged() = Unit
        override suspend fun userChanged() = Unit
        override fun removeListeners() = Unit
        override fun initialize(businessId: Int, menuId: Int, userId: Int) = Unit
    }
}
