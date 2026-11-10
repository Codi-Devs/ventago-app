package com.teco.ventago.features.branches.domain

import com.teco.ventago.core.cache.CacheUtils
import com.teco.ventago.core.cache.ICacheService
import com.teco.ventago.core.changes.IChangesManager
import com.teco.ventago.core.file.SharedFile
import com.teco.ventago.features.branches.data.repository.IBranchRepository
import com.teco.ventago.features.branches.domain.model.Branch
import com.teco.ventago.features.branches.domain.model.FiscalBillingPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BranchService(
    private val branchRepository: IBranchRepository,
    private val cache: ICacheService,
    private val changesManager: IChangesManager,
    private val appScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    private val state = MutableStateFlow<List<Branch>>(emptyList())
    private val loadMutex = Mutex()
    private var currentBusinessId: Int? = null
    private var changesJob: Job? = null
    private var loadJob: Job? = null
    private var generation = 0
    private var acceptCache = true

    var isInitialized = false


    fun observe(): StateFlow<List<Branch>> = state.asStateFlow()

    fun initialize(businessId: Int) {
        val owned = ++generation
        val useCache = acceptCache
        acceptCache = true
        currentBusinessId = businessId
        isInitialized = true
        loadJob?.cancel()
        loadJob = appScope.launch(ioDispatcher) {
            try {
                if (useCache) {
                    cacheGet()?.let { cached ->
                        if (owned == generation) state.value = cached
                    } ?: refresh(businessId, owned)
                } else {
                    refresh(businessId, owned)
                }
                if (owned == generation) startRealtimeSync()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // The previous business answered AUTH_001 after the switch.
            }
        }
    }

    suspend fun refresh(businessId: Int) {
        refresh(businessId, generation)
    }

    private suspend fun refresh(businessId: Int, owned: Int) {
        loadMutex.withLock {
            if (owned != generation) return@withLock
            val branches = branchRepository.getBranches(businessId)
            if (owned != generation) return@withLock
            state.value = branches
            saveCache(branches, ignoreChange = true)
        }
    }

    suspend fun addBillingPoint(
        businessId: Int,
        branchCode: String,
        name: String,
    ): FiscalBillingPoint? {
        val branches = state.value.toMutableList()
        var pointCode = "001"
        // Get next billing code from list of existing points
        val points = state.value
            .firstOrNull { it.branchCode == branchCode }
            ?.fiscalBillingPoints

        if (points != null) {
            val maxCode = points.maxOfOrNull {
                it.billingPoint.toIntOrNull() ?: 0
            } ?: 0

            pointCode = (maxCode + 1).toString().padStart(3, '0')
        }

        val created = branchRepository.addBillingPoint(businessId, branchCode, name, pointCode, 1)

        // Update cache if we have a business loaded

        val branchIdx = branches.indexOfFirst { it.branchCode == branchCode }
        if (branchIdx >= 0) {
            val branch = branches[branchIdx]
            val updatedPoints = branch.fiscalBillingPoints.toMutableList().apply { add(created) }
            branches[branchIdx] = branch.copy(fiscalBillingPoints = updatedPoints)
            state.value = branches
            saveCache(branches)
        }
        return created
    }

    suspend fun updateBillingPoint(
        businessId: Int,
        branchCode: String,
        billingPoint: String,
        name: String,
        status: Int,
    ): Boolean {
        val ok = branchRepository.updateBillingPoint(businessId, branchCode, billingPoint, name, status)
        if (!ok) return false

        // Update cache
        val branches = state.value.toMutableList()
        val bIdx = branches.indexOfFirst { it.branchCode == branchCode }
        if (bIdx >= 0) {
            val branch = branches[bIdx]
            val points = branch.fiscalBillingPoints.toMutableList()
            val pIdx = points.indexOfFirst { it.billingPoint == billingPoint }
            if (pIdx >= 0) {
                val updatedPoint = points[pIdx].copy(description = name, status = status)
                points[pIdx] = updatedPoint
                branches[bIdx] = branch.copy(fiscalBillingPoints = points)
                state.value = branches
                saveCache(branches)
            }
        }
        return true
    }

    suspend fun uploadBranchLogo(
        businessId: Int,
        branchCode: String,
        logo: SharedFile,
    ): Boolean {
        val ok = branchRepository.uploadBranchLogo(businessId, branchCode, logo)
        if (ok) refresh(businessId)
        return ok
    }

    suspend fun deleteBranchLogo(
        businessId: Int,
        branchCode: String,
    ): Boolean {
        val ok = branchRepository.deleteBranchLogo(businessId, branchCode)
        if (ok) refresh(businessId)
        return ok
    }

    private fun startRealtimeSync() {
        changesJob?.cancel()
        changesJob = changesManager.branchesListener()
            .onEach { value ->
                if (value != 1) {
                    val businessId = currentBusinessId ?: return@onEach
                    try {
                        refresh(businessId)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                    }
                }
            }
            .catch { _ ->
            }
            .launchIn(appScope)
    }

    private suspend fun saveCache(value: List<Branch>, ignoreChange: Boolean = false) {
        runCatching { cache.saveCache(CacheUtils.BRANCHES, value) }
        if (!ignoreChange) {
            changesManager.branchesChanged()
        }
    }

    private suspend fun cacheGet(): List<Branch>? =
        runCatching { cache.getCache<List<Branch>>(CacheUtils.BRANCHES) }.getOrNull()

    fun clear() {
        generation += 1
        acceptCache = false
        loadJob?.cancel()
        loadJob = null
        currentBusinessId = null
        isInitialized = false
        changesJob?.cancel()
        changesJob = null
        state.value = emptyList()
        appScope.launch(ioDispatcher) {
            runCatching { cache.clearCache(CacheUtils.BRANCHES) }
        }
    }
}
