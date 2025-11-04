package com.teco.ventago.features.business.domain

import com.teco.ventago.core.cache.ICacheService
import com.teco.ventago.core.changes.IChangesManager
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.branches.data.repository.IBranchRepository
import com.teco.ventago.features.business.data.repository.IBusinessRepository
import com.teco.ventago.features.branches.domain.model.Branch
import com.teco.ventago.features.business.domain.model.Business
import com.teco.ventago.features.branches.domain.model.FiscalBillingPoint
import com.teco.ventago.features.business.domain.model.requests.RegisterBusinessRequest
import com.teco.ventago.features.business.domain.model.responses.BusinessRegisterResponse
import com.teco.ventago.features.product.domain.ProductService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BusinessService(
    private val repository: IBusinessRepository,
    private val cache: ICacheService,
    private val changesManager: IChangesManager,
    private val authService: IAuthService,
    private val productService: ProductService,
) {

    val business = MutableStateFlow<Business?>(null)
    private var changesJob: Job? = null

    init {
        CoroutineScope(Dispatchers.IO).launch {
            cache.getCache(Business::class)?.let { businessData ->
                withContext(Dispatchers.Main) {
                    business.update {
                        businessData
                    }
                }
            }
            changesJob?.cancel()
            changesJob = changesManager.businessListener().onEach { value ->
                if (value != 1) {
                    business.value?.let {
                        getBusinessById(it.businessId)
                    }
                }
            }.launchIn(this)
        }
    }

    fun getBusiness(): Flow<Business?> = business

    fun clear() {
        changesJob?.cancel()
        changesJob = null
        business.update {
            null
        }
    }

    suspend fun registerBusiness(
        request: RegisterBusinessRequest
    ): BusinessRegisterResponse {
        val res = repository.registerBusiness(request)
        if (res.businessId > 0) {
            authService.getUserSync()?.let { user ->
                changesManager.initialize(res.businessId, res.menuId, user.userId)
            }
        }

        return res
    }

    suspend fun getBusinessById(businessId: Int): Business {
        val business = repository.getBusinessById(businessId)
        this.business.update {
            business
        }
        saveCache(ignoreChange = true)
        return business
    }


    suspend fun loadBusinesses(): List<Business> {
        return repository.getBusinesses()
    }

    suspend fun changeBusiness(businessId: Int): Boolean {
        val businessRecover = business.value
        val productRecover = productService.state.value
        try {
            val business = repository.getBusinessById(businessId)
            val products = productService.getProductsByBusinessId(businessId)
            if (business.businessId == -1) {
                return false
            }
            authService.getUserSync()?.let { user ->
                changesManager.removeListeners()
                changesManager.initialize(businessId, products.id, user.userId)
            }

            this.business.update {
                business
            }
            saveCache(ignoreChange = true)
            return true
        } catch (e: Exception) {
            this.business.update {
                businessRecover
            }
            productService.state.update {
                productRecover
            }
            return false
        }
    }

    suspend fun deleteBusiness(businessId: Int): Boolean {
        if (businessId == business.value?.businessId) {
            return false
        }
        return repository.deleteBusiness(businessId)
    }



    private fun saveCache(ignoreChange: Boolean = false) {
        CoroutineScope(Dispatchers.IO).launch {
            cache.saveCache(business.value)
            if (!ignoreChange) {
                changesManager.businessChanged()
            }
        }
    }

    fun saveBusiness(newBusiness: Business) {
        business.update {
            newBusiness
        }
        saveCache()
    }
}