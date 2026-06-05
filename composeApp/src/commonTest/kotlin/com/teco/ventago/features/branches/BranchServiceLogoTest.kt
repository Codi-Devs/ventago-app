package com.teco.ventago.features.branches

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
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class BranchServiceLogoTest {

    @Test
    fun uploadBranchLogoReloadsBranchesAfterSuccess() = runTest {
        val repository = FakeBranchRepository(
            refreshedBranches = listOf(branch(logoUrl = "https://cdn.example.com/new-logo.jpg")),
        )
        val service = BranchService(
            branchRepository = repository,
            cache = FakeCacheService(),
            changesManager = FakeChangesManager(),
            appScope = TestScope(testScheduler),
        )

        val ok = service.uploadBranchLogo(
            businessId = 123,
            branchCode = "0000",
            logo = SharedFile(byteArrayOf(1, 2, 3), "branch_logo.jpg", "image/jpeg"),
        )

        assertTrue(ok)
        assertEquals(1, repository.uploadLogoCalls)
        assertEquals(1, repository.getBranchesCalls)
        assertEquals("https://cdn.example.com/new-logo.jpg", service.observe().value.first().logoUrl)
    }

    @Test
    fun deleteBranchLogoReloadsBranchesAfterSuccess() = runTest {
        val repository = FakeBranchRepository(
            refreshedBranches = listOf(branch(logoUrl = null)),
        )
        val service = BranchService(
            branchRepository = repository,
            cache = FakeCacheService(),
            changesManager = FakeChangesManager(),
            appScope = TestScope(testScheduler),
        )

        val ok = service.deleteBranchLogo(
            businessId = 123,
            branchCode = "0000",
        )

        assertTrue(ok)
        assertEquals(1, repository.deleteLogoCalls)
        assertEquals(1, repository.getBranchesCalls)
        assertEquals(null, service.observe().value.first().logoUrl)
    }

    private fun branch(logoUrl: String?): Branch {
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
            logoUrl = logoUrl,
        )
    }

    private class FakeBranchRepository(
        private val refreshedBranches: List<Branch>,
    ) : IBranchRepository {
        var getBranchesCalls = 0
        var uploadLogoCalls = 0
        var deleteLogoCalls = 0

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
        ): Boolean {
            uploadLogoCalls += 1
            return true
        }

        override suspend fun deleteBranchLogo(businessId: Int, branchCode: String): Boolean {
            deleteLogoCalls += 1
            return true
        }
    }

    private class FakeCacheService : ICacheService {
        override suspend fun <T : Any> getCache(klass: KClass<T>): T? = null
        override suspend fun <T : Any> getCache(key: String): T? = null
        override suspend fun <T> saveCache(data: T) = Unit
        override suspend fun <T> saveCache(key: String, data: T) = Unit
        override suspend fun clearCache(id: String) = Unit
        override suspend fun clearAllCache() = Unit
    }

    private class FakeChangesManager : IChangesManager {
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
        override suspend fun financialChanged() = Unit
        override suspend fun customersChanged() = Unit
        override suspend fun userChanged() = Unit
        override fun removeListeners() = Unit
        override fun initialize(businessId: Int, menuId: Int, userId: Int) = Unit
    }
}
