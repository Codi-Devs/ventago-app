package com.teco.ventago.features.customers.domain

import com.teco.ventago.core.Paged
import com.teco.ventago.core.cache.CacheUtils
import com.teco.ventago.core.cache.ICacheService
import com.teco.ventago.core.changes.IChangesManager
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.features.customers.data.provider.json
import com.teco.ventago.features.customers.data.repository.ICustomerRepository
import com.teco.ventago.features.customers.data.repository.dto.CustomerCreatedDto
import com.teco.ventago.features.customers.domain.models.CustomerAddress
import com.teco.ventago.features.customers.domain.models.Customer
import com.teco.ventago.features.customers.domain.models.CustomerListItem
import com.teco.ventago.features.customers.domain.models.ValidateRucResponse
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
import kotlinx.serialization.encodeToString
import kotlin.time.Clock.System.now
import kotlin.time.ExperimentalTime

class CustomerService(
    private val repository: ICustomerRepository,
    private val loggerService: ILoggerService,
    private val cache: ICacheService,
    private val changesManager: IChangesManager,
    private val appScope: CoroutineScope
) {

    private val state = MutableStateFlow<Paged<CustomerListItem>>(empty())
    private val loadMutex = Mutex()
    private var currentBusinessId: Int? = null
    private var changesJob: Job? = null

    var editingCustomer: CustomerListItem? = null


    fun observe(): StateFlow<Paged<CustomerListItem>> = state.asStateFlow()

    fun initialize(businessId: Int, loadData: Boolean = false) {
        currentBusinessId = businessId
        appScope.launch(Dispatchers.IO) {
            // 1) try cache fast-path
            cacheGet().let {
                println("ASDASD: Loaded customers from cache: ${json.encodeToString(it)}")
                state.value = it
            }
            // 3) start RTDB/Listener for invalidations
            startRealtimeSync()
            if (loadData) {
                refresh(businessId)
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun createCustomer(
        customer: Customer,
        businessId: Int
    ): CustomerCreatedDto {
        val res =  repository.createCustomer(customer, businessId)
        val customers = state.value
        val newList = listOf(CustomerListItem(
            id = res.id,
            name = customer.name,
            email = customer.email,
            ruc = customer.ruc,
            status = 1,
            invoiceCustomer = if (customer.invoiceCustomer) 1 else 0,
            updatedAt = now().epochSeconds,
        )) + customers.items

        val newPaged = Paged(
            items = newList,
            page = customers.page,
            size = customers.size,
            total = customers.total + 1,
        )
        state.value = newPaged
        saveCache(newPaged)
        return res
    }

    suspend fun listCustomers(
        businessId: Int,
        page: Int,
        size: Int,
        ruc: String? = null,
        email: String? = null,
        name: String? = null
    ): Paged<CustomerListItem> {
        val customers = repository.listCustomers(businessId, page, size, ruc, email, name)
        state.value = customers
        return customers
    }

    suspend fun validateRUC(ruc: String, businessId: Int): ValidateRucResponse {
        return repository.validateRUC(ruc, businessId)
    }

    suspend fun validateRUCRegister(ruc: String): ValidateRucResponse {
        return repository.validateRUCRegister(ruc)
    }

    suspend fun listCustomerAddresses(
        businessId: Int,
        invoiceCustomerId: Int
    ): List<CustomerAddress> {
        return repository.listCustomerAddresses(businessId, invoiceCustomerId)
    }

    suspend fun refresh(businessIdOpt: Int? = currentBusinessId) {
        if (businessIdOpt != null && businessIdOpt != currentBusinessId) {
            currentBusinessId = businessIdOpt
        }
        val businessId = currentBusinessId ?: return
        loadMutex.withLock {
            runCatching { repository.listCustomers(businessId, 0, 50, null, null, null) }
                .onSuccess { customers ->
                    state.value = customers
                    saveCache(customers, ignoreChange = true)
                }
                .onFailure {
                    loggerService.sendLog(
                        Log(
                            LogLevel.ERROR,
                            "FinancialProfileService::refresh",
                            "Error refreshing financial profile: ${it.message ?: "UNKNOWN"}"
                        )
                    )
                }
        }
    }

    private fun startRealtimeSync() {
        changesJob?.cancel()
        changesJob = changesManager.customersListener()
            .onEach { value ->
                if (value != 1) {
                    refresh()
                }
            }
            .catch { e ->
                loggerService.sendLog(
                    Log(
                        LogLevel.ERROR,
                        "CustomerService::startRealtimeSync",
                        "Error listening for customer cache changes: ${e.message ?: "UNKNOWN"}"
                    )
                )
            }
            .launchIn(appScope)
    }

    private suspend fun cacheGet(): Paged<CustomerListItem> =
        runCatching { cache.getCache<Paged<CustomerListItem>>(CacheUtils.CUSTOMERS) }.getOrNull()
            ?: empty()

    private fun saveCache(value: Paged<CustomerListItem>, ignoreChange: Boolean = false) {
        appScope.launch(Dispatchers.IO) {
            runCatching { cache.saveCache(CacheUtils.CUSTOMERS, value) }
            if (!ignoreChange) {
                changesManager.customersChanged()
            }
        }
    }

    fun clear() {
        currentBusinessId = null
        changesJob?.cancel()
        changesJob = null
        state.value = empty()
    }

    private fun empty(): Paged<CustomerListItem> {
        return Paged(
            items = emptyList(),
            page = 0,
            size = 50,
            total = 0,
        )
    }

}
