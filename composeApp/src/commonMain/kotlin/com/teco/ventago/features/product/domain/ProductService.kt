package com.teco.ventago.features.product.domain

import com.teco.ventago.core.cache.CacheUtils
import com.teco.ventago.core.cache.ICacheService
import com.teco.ventago.core.changes.IChangesManager
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.financialProfile.domain.model.BusinessFinancialProfile
import com.teco.ventago.features.product.data.repository.IProductsRepository
import com.teco.ventago.features.product.domain.model.Category
import com.teco.ventago.features.product.domain.model.Item
import com.teco.ventago.features.product.domain.model.Products
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal fun preferLoadedMenu(current: Products?, cached: Products): Products = current ?: cached

class ProductService(
    private val productsRepository: IProductsRepository,
    private val cache: ICacheService,
    private val changesManager: IChangesManager,
    private val authService: IAuthService,
) {

    val state = MutableStateFlow<Products?>(null)

    var selectedCategoryId: Int? = null
    var selectedItemId: Int? = null

    private var listenerJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val corruptMenuRecovery = Mutex()

    init {
        serviceScope.launch {
            val productsData = cache.getCache(Products::class)
            if (productsData != null && productsData.hasDuplicateCatalogIds()) {
                // Room won the race that appended a product the server already
                // had. Publishing it crashes LazyColumn/LazyGrid on the key.
                discardCorruptMenuCache(productsData)
            } else if (productsData != null) {
                withContext(Dispatchers.Main) {
                    // A menu already fetched from the network wins. A late Room
                    // read must not put back a snapshot that dropped ITBMS.
                    state.update { current -> preferLoadedMenu(current, productsData) }
                }
            }

            listenerJob?.cancel()
            startProductsListener()
        }
    }

    fun observe(): StateFlow<Products?> = state.asStateFlow()
    fun getMenu(): Flow<Products?> = state

    private fun startProductsListener() {
        listenerJob?.cancel()
        listenerJob = changesManager.productsListener().onEach { value ->
            if (value != 1) {
                state.value?.let { menuAux ->
                    authService.getUserSync()?.businessIds?.let {
                        val businessId =
                            it.firstOrNull { item -> item.menuId == menuAux.id }?.businessId
                        if (businessId != null) {
                            getProductsByBusinessId(businessId)
                        }
                    }
                }
            }
        }.launchIn(serviceScope)
    }


    fun getAllActiveItems(products: Products): List<Item> {
        return products.categories
            .filter { it.active }
            .flatMap { category -> category.items.filter { item -> item.active } }
            .distinctBy { it.itemId }
    }

    suspend fun setCategoryActive(categoryId: Int, active: Boolean): Boolean {
        val success = productsRepository.setCategoryActive(categoryId, active)
        if (success) {
            state.update {
                it?.copy(categories = it.categories.map { category ->
                    if (category.id == categoryId) {
                        category.copy(active = active)
                    } else {
                        category
                    }
                })
            }
            saveCache()
        }
        return success
    }

    suspend fun changeCategoryOrder(categories: List<Category>): Boolean {
        if (categories.sameCategoryOrder(state.value?.categories ?: listOf())) {
            return true
        }
        categories.resetCategoryOrders()
        val success = productsRepository.changeCategoryOrder(categories)
        if (success) {
            state.update {
                it?.copy(categories = categories)
            }
            saveCache()
        }
        return success
    }

    suspend fun editCategory(category: Category): Boolean {
        val success = productsRepository.editCategory(category)
        if (success) {
            state.update {
                it?.copy(categories = it.categories.map { cat ->
                    if (cat.id == category.id) {
                        category
                    } else {
                        cat
                    }
                })
            }
            saveCache()
        }
        return success
    }

    suspend fun addCategory(name: String, desc: String): Category {
        val category = Category(
            -1, name, desc, false, listOf(), -1
        )
        state.value?.let { menuObj ->
            category.order = menuObj.getLastCategoryOrder() + 1
            val res = productsRepository.addCategory(category, menuObj.id)
            if (res.id > 0) {
                state.update {
                    it?.copy(categories = it.categories + res)
                }
                saveCache()
            }
            return res
        }
        return category.copy(id = -1)
    }

    suspend fun removeCategory(categoryId: Int): Boolean {
        val success = productsRepository.removeCategory(categoryId)
        if (success) {
            state.update {
                it?.copy(categories = it.categories.filter { cat ->
                    cat.id != categoryId
                })
            }
            saveCache()
        }
        return success
    }

    suspend fun addItem(item: Item, categoryId: Int): Item {
        var activated = false
        val category = state.value?.categories?.find { category -> category.id == categoryId }
        val savedItem = productsRepository.addItem(item, categoryId)
        if (category != null) {
            if (category.items.isEmpty() && !category.active) {
                activated = productsRepository.setCategoryActive(categoryId, true)
            }
        }
        productsRepository.setCategoryActive(categoryId, true)
        state.update { menu ->
            if (menu == null) return@update null
            val updated = menu.copy(categories = menu.categories.map { cat ->
                val remaining = cat.items.filter { it.itemId != savedItem.itemId }
                if (cat.id == categoryId) {
                    cat.copy(
                        items = remaining + savedItem,
                        active = if (activated) true else cat.active,
                    )
                } else if (remaining.size != cat.items.size) {
                    cat.copy(items = remaining)
                } else {
                    cat
                }
            })
            // The menu refresh can land while setCategoryActive is still in
            // flight and already contain savedItem. Replacing by id keeps one row.
            updated.withoutDuplicateCatalogIds()
        }
        saveCache()
        return savedItem
    }

    suspend fun editItem(item: Item, categoryId: Int): Boolean {
        val success = productsRepository.editItem(item, categoryId)
        if (success) {
            state.update { menuAux ->
                val updated = menuAux?.copy(categories = menuAux.categories.map { cat ->
                    if (cat.id == categoryId) {
                        // Target category: add item if not present, or update if present
                        val existingItemIndex = cat.items.indexOfFirst { it.itemId == item.itemId }
                        if (existingItemIndex >= 0) {
                            // Item already in this category, update it
                            cat.copy(items = cat.items.map {
                                if (it.itemId == item.itemId) item else it
                            })
                        } else {
                            // Item moved to this category, add it
                            cat.copy(items = cat.items + item)
                        }
                    } else {
                        // Other categories: remove item if present
                        cat.copy(items = cat.items.filter { it.itemId != item.itemId })
                    }
                }) ?: return@update null
                updated.withoutDuplicateCatalogIds()
            }
            saveCache()
        }
        return success
    }

    suspend fun removeItem(itemId: Int): Boolean {
        val success = productsRepository.removeItem(itemId)
        println("ASDASD: Product service $success")
        if (success) {
            state.update { menuAux ->
                menuAux?.copy(categories = menuAux.categories.map { cat ->
                    cat.copy(items = cat.items.filter {
                        it.itemId != itemId
                    })
                })
            }
            saveCache()
        }
        return success
    }

    suspend fun changeItemOrder(items: List<Item>): Boolean {
        if (items.sameItemOrder(
                state.value?.categories?.firstOrNull { it.id == selectedCategoryId }?.items
                    ?: listOf()
            )
        ) {
            return true
        }
        items.resetItemOrders()
        val success = productsRepository.changeItemOrder(items)
        if (success) {
            state.update { menuAux ->
                menuAux?.copy(categories = menuAux.categories.map { cat ->
                    if (cat.id == selectedCategoryId) {
                        cat.copy(items = items)
                    } else {
                        cat
                    }
                })
            }
            saveCache()
        }
        return success
    }

    suspend fun getProductsByBusinessId(businessId: Int): Products {
        val res = productsRepository.getProductsByBusinessId(businessId)
            .withoutDuplicateCatalogIds()
        state.update {
            res
        }
        if (listenerJob?.isActive != true) {
            startProductsListener()
        }
        persistProducts(res, publishChange = false)
        return res
    }

    fun signOut() {
        // Cancel Firebase Realtime Database listener
        listenerJob?.cancel()
        listenerJob = null
        // Clear state
        state.update {
            null
        }
        // Reset selection state
        selectedCategoryId = null
        selectedItemId = null
    }

    fun canAddItem(category: Category): Boolean {
        if (authService.getUserSync()?.premium == false) {
            return category.items.size < 10
        }
        return true
    }

    fun canAddCategory(): Boolean {
        return (state.value?.categories?.size ?: 0) < 6
    }

    suspend fun getMenuIdByBusinessId(businessId: Int): Int {
        return productsRepository.getProductIdByBusinessId(businessId)
    }

    private fun saveCache() {
        val snapshot = state.value ?: return
        if (snapshot.hasDuplicateCatalogIds()) {
            serviceScope.launch { discardCorruptMenuCache(snapshot) }
            return
        }
        persistProducts(snapshot, publishChange = true)
    }

    private suspend fun discardCorruptMenuCache(corrupt: Products) {
        if (!corrupt.hasDuplicateCatalogIds()) return
        corruptMenuRecovery.withLock {
            if (state.value?.hasDuplicateCatalogIds() == false && corrupt !== state.value) {
                // A clean menu is already showing. Drop Room only when that
                // row is still the corrupt snapshot. A refresh may have
                // replaced it while this recovery waited.
                val latest = cache.getCache(Products::class)
                if (latest == null || latest.hasDuplicateCatalogIds()) {
                    clearProductsCache()
                }
                return@withLock
            }
            clearProductsCache()
            val preview = corrupt.withoutDuplicateCatalogIds()
            state.update { current ->
                if (current != null && !current.hasDuplicateCatalogIds()) current else preview
            }
            val businessId = businessIdForMenu(state.value?.id)
            if (businessId == null) {
                persistCleanInMemoryMenu()
                return@withLock
            }
            try {
                getProductsByBusinessId(businessId)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                persistCleanInMemoryMenu()
            }
        }
    }

    private suspend fun clearProductsCache() {
        try {
            cache.clearCache(CacheUtils.PRODUCTS)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
        }
    }

    private fun persistCleanInMemoryMenu() {
        val clean = state.value?.withoutDuplicateCatalogIds() ?: return
        if (clean.hasDuplicateCatalogIds()) return
        persistProducts(clean, publishChange = false)
    }

    private fun businessIdForMenu(menuId: Int?): Int? {
        val businesses = authService.getUserSync()?.businessIds ?: return null
        if (menuId != null) {
            businesses.firstOrNull { it.menuId == menuId }?.businessId?.let { return it }
        }
        return null
    }

    private fun persistProducts(snapshot: Products?, publishChange: Boolean) {
        if (snapshot == null) return
        serviceScope.launch {
            try {
                cache.saveCache(snapshot)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // The product is already stored on the server. An uncaught
                // Room error in this coroutine aborts the iOS process.
            }
            if (!publishChange) return@launch
            try {
                changesManager.productsChanged()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
            }
        }
    }

    private fun List<Category>.sameCategoryOrder(compare: List<Category>): Boolean {
        for (i in this.indices) {
            if (this[i].order != compare[i].order) {
                return false
            }
        }
        return true
    }

    private fun List<Category>.resetCategoryOrders() {
        var order = 1
        this.map {
            it.copy(order = order++)
        }
    }

    private fun List<Item>.sameItemOrder(compare: List<Item>): Boolean {
        for (i in this.indices) {
            if (this[i].order != compare[i].order) {
                return false
            }
        }
        return true
    }

    private fun List<Item>.resetItemOrders() {
        var order = 1
        this.map {
            it.copy(order = order++)
        }
    }
}