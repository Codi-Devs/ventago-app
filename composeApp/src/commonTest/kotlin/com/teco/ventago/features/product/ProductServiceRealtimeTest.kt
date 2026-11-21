package com.teco.ventago.features.product

import com.teco.ventago.core.cache.ICacheService
import com.teco.ventago.core.changes.IChangesManager
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.core.cache.CacheUtils
import com.teco.ventago.features.auth.domain.model.User
import com.teco.ventago.features.auth.domain.model.firebase.FirebaseUserDM
import com.teco.ventago.features.auth.domain.model.response.BusinessIds
import com.teco.ventago.features.auth.domain.model.requests.CreateUserRequest
import com.teco.ventago.features.auth.domain.model.requests.EmailLoginRequest
import com.teco.ventago.features.auth.domain.model.response.AuthResponse
import com.teco.ventago.features.product.data.repository.IProductsRepository
import com.teco.ventago.features.product.domain.ProductService
import com.teco.ventago.features.product.domain.hasDuplicateCatalogIds
import com.teco.ventago.features.product.domain.model.Category
import com.teco.ventago.features.product.domain.model.Item
import com.teco.ventago.features.product.domain.model.ProductType
import com.teco.ventago.features.product.domain.model.Products
import io.ktor.client.HttpClient
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class ProductServiceRealtimeTest {

    @Test
    fun remoteMenuRefreshDoesNotPublishMenuToken() = runTest {
        val changes = FakeChangesManager()
        val cache = FakeCacheService()
        val service = ProductService(
            productsRepository = FakeProductsRepository(menu()),
            cache = cache,
            changesManager = changes,
            authService = FakeAuthService(),
        )

        service.getProductsByBusinessId(10)
        cache.awaitSave()

        assertEquals(0, changes.productsChangedCalls)
    }

    @Test
    fun localCategoryEditPublishesOneMenuToken() = runTest {
        val changes = FakeChangesManager()
        val cache = FakeCacheService()
        val service = ProductService(
            productsRepository = FakeProductsRepository(menu()),
            cache = cache,
            changesManager = changes,
            authService = FakeAuthService(),
        )
        service.getProductsByBusinessId(10)
        cache.awaitSave()

        service.setCategoryActive(categoryId = 1, active = false)
        cache.awaitSave()
        changes.awaitProductsChanged()

        assertEquals(1, changes.productsChangedCalls)
    }

    @Test
    fun cacheFailureStillPublishesMenuToken() = runTest {
        val changes = FakeChangesManager()
        val service = ProductService(
            productsRepository = FakeProductsRepository(menu()),
            cache = FakeCacheService(failSaves = true),
            changesManager = changes,
            authService = FakeAuthService(),
        )
        service.getProductsByBusinessId(10)

        val saved = service.addItem(sampleItem(), categoryId = 1)
        changes.awaitProductsChanged()

        assertEquals(99, saved.itemId)
        assertEquals(1, changes.productsChangedCalls)
        assertEquals(listOf(99), service.state.value!!.categories.single().items.map { it.itemId })
    }

    @Test
    fun addItemReplacesAnItemTheMenuRefreshAlreadyLoaded() = runTest {
        val service = ProductService(
            productsRepository = FakeProductsRepository(menuWith(sampleItem())),
            cache = FakeCacheService(),
            changesManager = FakeChangesManager(),
            authService = FakeAuthService(),
        )
        service.getProductsByBusinessId(10)

        service.addItem(sampleItem(), categoryId = 1)

        assertEquals(listOf(99), service.state.value!!.categories.single().items.map { it.itemId })
    }

    @Test
    fun addCategoryReplacesACategoryTheMenuRefreshAlreadyLoaded() = runTest {
        val changes = FakeChangesManager()
        val existing = Category(
            id = 4,
            name = "Bebidas",
            desc = "",
            active = true,
            items = emptyList(),
            order = 1,
        )
        val service = ProductService(
            productsRepository = FakeProductsRepository(
                menu = Products(id = 7, active = true, categories = listOf(existing)),
                addedCategory = existing.copy(desc = "fria"),
            ),
            cache = FakeCacheService(),
            changesManager = changes,
            authService = FakeAuthService(),
        )
        service.getProductsByBusinessId(10)

        val saved = service.addCategory("Bebidas", "fria")
        changes.awaitProductsChanged()

        assertEquals(4, saved.id)
        assertEquals(listOf(4), service.state.value!!.categories.map { it.id })
        assertEquals("fria", service.state.value!!.categories.single().desc)
        assertEquals(1, changes.productsChangedCalls)
    }

    @Test
    fun localEditThatDuplicatesACategoryPublishesTheCleanMenu() = runTest {
        val changes = FakeChangesManager()
        val first = Category(
            id = 4,
            name = "Bebidas",
            desc = "",
            active = true,
            items = emptyList(),
            order = 1,
        )
        val second = first.copy(id = 5, name = "Comidas", order = 2)
        val service = ProductService(
            productsRepository = FakeProductsRepository(
                menu = Products(id = 7, active = true, categories = listOf(first, second)),
            ),
            cache = FakeCacheService(),
            changesManager = changes,
            authService = FakeAuthService(),
        )
        service.getProductsByBusinessId(10)

        val saved = service.changeCategoryOrder(
            listOf(first.copy(order = 2), first.copy(name = "Bebidas 2", order = 1)),
        )
        changes.awaitProductsChanged()

        assertTrue(saved)
        assertEquals(listOf(4), service.state.value!!.categories.map { it.id })
        assertFalse(service.state.value!!.hasDuplicateCatalogIds())
        assertEquals(1, changes.productsChangedCalls)
    }

    @Test
    fun duplicateItemCacheIsDeletedAndReplacedFromTheApi() = runTest {
        val item = sampleItem()
        val cache = FakeCacheService(initial = menuWith(item, item))
        val repository = FakeProductsRepository(menuWith(item))
        val changes = FakeChangesManager()
        val service = ProductService(
            productsRepository = repository,
            cache = cache,
            changesManager = changes,
            authService = FakeAuthService(user = ownerOfMenu(menuId = 7, businessId = 10)),
        )

        assertEquals(CacheUtils.PRODUCTS, cache.awaitClear())
        repository.awaitFetch()
        awaitUniqueMenu(service)
        cache.awaitSave()

        assertEquals(listOf(99), service.state.value!!.categories.single().items.map { it.itemId })
        assertTrue(cache.savedMenus.isNotEmpty())
        assertTrue(cache.savedMenus.all { !it.hasDuplicateCatalogIds() })
        assertEquals(0, changes.productsChangedCalls)
    }

    @Test
    fun duplicateItemCacheSurvivesAnApiFailureWithoutDuplicateIds() = runTest {
        val item = sampleItem()
        val cache = FakeCacheService(initial = menuWith(item, item))
        val repository = FakeProductsRepository(menuWith(item), failGet = true)
        val service = ProductService(
            productsRepository = repository,
            cache = cache,
            changesManager = FakeChangesManager(),
            authService = FakeAuthService(user = ownerOfMenu(menuId = 7, businessId = 10)),
        )

        cache.awaitClear()
        repository.awaitFetch()
        cache.awaitSave()
        awaitUniqueMenu(service)

        assertEquals(listOf(99), service.state.value!!.categories.single().items.map { it.itemId })
        assertFalse(cache.savedMenus.any { it.hasDuplicateCatalogIds() })
    }

    private suspend fun awaitUniqueMenu(service: ProductService) {
        withContext(Dispatchers.Default) {
            withTimeout(3_000) {
                while (service.state.value?.hasDuplicateCatalogIds() != false ||
                    service.state.value?.categories?.singleOrNull()?.items?.size != 1
                ) {
                    delay(15)
                }
            }
        }
    }

    private fun ownerOfMenu(menuId: Int, businessId: Int): User {
        return User(
            uid = "user",
            email = "owner@example.com",
            name = "Owner",
            premium = true,
            active = true,
            missingBusiness = false,
            userId = 1,
            businessIds = listOf(BusinessIds(businessId = businessId, menuId = menuId)),
        )
    }

    private fun sampleItem(): Item {
        return Item(
            itemId = 99,
            barcode = null,
            sku = null,
            name = "Prueba",
            description = "",
            img = "",
            price = 1.0,
            cost = null,
            active = true,
            order = 1,
            taxPercent = 7,
            productType = ProductType.GOOD,
        )
    }

    private fun menu(): Products = menuWith()

    private fun menuWith(vararg items: Item): Products {
        return Products(
            id = 7,
            active = true,
            categories = listOf(
                Category(
                    id = 1,
                    name = "Bebidas",
                    desc = "",
                    active = true,
                    items = items.toList(),
                    order = 1,
                )
            ),
        )
    }

    private class FakeProductsRepository(
        private val menu: Products,
        private val failGet: Boolean = false,
        private val addedCategory: Category? = null,
    ) : IProductsRepository {
        private val fetches = Channel<Unit>(capacity = Channel.UNLIMITED)

        suspend fun awaitFetch() {
            withContext(Dispatchers.Default.limitedParallelism(1)) {
                withTimeout(3_000) {
                    fetches.receive()
                }
            }
        }

        override suspend fun setCategoryActive(categoryId: Int, active: Boolean): Boolean = true
        override suspend fun changeCategoryOrder(categories: List<Category>): Boolean = true
        override suspend fun editCategory(category: Category): Boolean = true
        override suspend fun addCategory(category: Category, menuId: Int): Category =
            addedCategory ?: category
        override suspend fun removeCategory(categoryId: Int): Boolean = true
        override suspend fun addItem(item: Item, categoryId: Int): Item = item
        override suspend fun editItem(item: Item, categoryId: Int): Boolean = true
        override suspend fun removeItem(itemId: Int): Boolean = true
        override suspend fun changeItemOrder(items: List<Item>): Boolean = true
        override suspend fun getProductsByBusinessId(businessId: Int): Products {
            fetches.trySend(Unit)
            if (failGet) error("network")
            return menu
        }
        override suspend fun getProductIdByBusinessId(businessId: Int): Int = menu.id
    }

    private class FakeCacheService(
        private val failSaves: Boolean = false,
        private val initial: Products? = null,
    ) : ICacheService {
        val savedMenus = mutableListOf<Products>()
        private val saves = Channel<Unit>(capacity = Channel.UNLIMITED)
        private val clears = Channel<String>(capacity = Channel.UNLIMITED)

        suspend fun awaitSave() {
            withContext(Dispatchers.Default.limitedParallelism(1)) {
                withTimeout(2_000) {
                    saves.receive()
                }
            }
        }

        suspend fun awaitClear(): String {
            return withContext(Dispatchers.Default.limitedParallelism(1)) {
                withTimeout(3_000) {
                    clears.receive()
                }
            }
        }

        override suspend fun <T : Any> getCache(klass: KClass<T>): T? {
            if (klass == Products::class && initial != null) return initial as T
            return null
        }
        override suspend fun <T : Any> getCache(key: String): T? = null
        override suspend fun <T> saveCache(data: T) {
            if (data is Products) savedMenus += data
            if (failSaves) error("UNIQUE constraint failed: ProductsCache.id")
            saves.trySend(Unit)
        }
        override suspend fun <T> saveCache(key: String, data: T) = Unit
        override suspend fun clearCache(id: String) {
            clears.trySend(id)
        }
        override suspend fun clearAllCache() = Unit
    }

    private class FakeChangesManager : IChangesManager {
        var productsChangedCalls = 0
        private val productChanges = Channel<Unit>(capacity = Channel.UNLIMITED)

        suspend fun awaitProductsChanged() {
            withContext(Dispatchers.Default.limitedParallelism(1)) {
                withTimeout(2_000) {
                    productChanges.receive()
                }
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
        override suspend fun productsChanged() {
            productsChangedCalls += 1
            productChanges.trySend(Unit)
        }
        override suspend fun businessChanged() = Unit
        override suspend fun branchesChanged() = Unit
        override suspend fun financialChanged() = Unit
        override suspend fun customersChanged() = Unit
        override suspend fun userChanged() = Unit
        override fun removeListeners() = Unit
        override fun initialize(businessId: Int, menuId: Int, userId: Int) = Unit
    }

    private class FakeAuthService(
        private val user: User? = null,
    ) : IAuthService {
        override suspend fun refreshToken(client: HttpClient, failedAccessToken: String?) = Unit
        override suspend fun googleLogin(googleToken: String, businessId: Int?): AuthResponse = error("unused")
        override suspend fun emailLogin(request: EmailLoginRequest): AuthResponse = error("unused")
        override suspend fun emailRegister(request: CreateUserRequest): AuthResponse = error("unused")
        override suspend fun sendPasswordResetEmail(email: String) = Unit
        override suspend fun businessRegistered() = Unit
        override suspend fun signOut() = Unit
        override fun getFirebaseUser(): Flow<FirebaseUserDM?> = emptyFlow()
        override fun getUser(): Flow<User?> = emptyFlow()
        override fun getUserSync(): User? = user
        override fun getJwtToken(refresh: Boolean): String? = null
        override fun isAuthenticated(): Boolean = true
        override fun setPremium(premium: Boolean) = Unit
        override suspend fun deleteAccount(token: String): Boolean = false
    }
}
