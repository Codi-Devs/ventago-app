package com.teco.ventago.features.product.domain

import com.teco.ventago.core.cache.ICacheService
import com.teco.ventago.core.changes.IChangesManager
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.financialProfile.domain.model.BusinessFinancialProfile
import com.teco.ventago.features.product.data.repository.IProductsRepository
import com.teco.ventago.features.product.domain.model.Category
import com.teco.ventago.features.product.domain.model.Item
import com.teco.ventago.features.product.domain.model.Products
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductService(
    private val productsRepository: IProductsRepository,
    private val cache: ICacheService,
    private val changesManager: IChangesManager,
    private val authService: IAuthService,
) {

    val state = MutableStateFlow<Products?>(null)

    var selectedCategoryId: Int? = null
    var selectedItemId: Int? = null

    init {


        CoroutineScope(Dispatchers.IO).launch {
            cache.getCache(Products::class)?.let { productsData ->
                withContext(Dispatchers.Main) {
                    state.update {
                        productsData
                    }
                }
            }

            changesManager.productsListener().onEach { value ->
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
            }.launchIn(this)
        }
    }

    fun observe(): StateFlow<Products?> = state.asStateFlow()
    fun getMenu(): Flow<Products?> = state


    fun getAllActiveItems(products: Products): List<Item> {
        // Filter active items.
        return products.categories.filter { it.active }.flatMap { it.items.filter { item -> item.active }.toList()}
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
        state.update {
            it?.copy(categories = it.categories.map { cat ->
                if (cat.id == categoryId) {
                    cat.copy(items = cat.items + savedItem, active = if (activated) true else cat.active)
                } else {
                    cat
                }
            })
        }
        saveCache()
        return savedItem
    }

    suspend fun editItem(item: Item): Boolean {
        val success = productsRepository.editItem(item)
        if (success) {
            state.update { menuAux ->
                menuAux?.copy(categories = menuAux.categories.map { cat ->
                    cat.copy(items = cat.items.map {
                        if (it.itemId == item.itemId) {
                            item
                        } else {
                            it
                        }
                    })
                })
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
        state.update {
            res
        }
        CoroutineScope(Dispatchers.IO).launch {
            cache.saveCache(state.value)
        }
        return res
    }

    fun signOut() {
        state.update {
            null
        }
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
        CoroutineScope(Dispatchers.IO).launch {
            cache.saveCache(state.value)
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