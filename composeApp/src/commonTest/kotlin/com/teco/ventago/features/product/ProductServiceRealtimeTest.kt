package com.teco.ventago.features.product

import com.teco.ventago.core.cache.ICacheService
import com.teco.ventago.core.changes.IChangesManager
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.auth.domain.model.User
import com.teco.ventago.features.auth.domain.model.firebase.FirebaseUserDM
import com.teco.ventago.features.auth.domain.model.requests.CreateUserRequest
import com.teco.ventago.features.auth.domain.model.requests.EmailLoginRequest
import com.teco.ventago.features.auth.domain.model.response.AuthResponse
import com.teco.ventago.features.product.data.repository.IProductsRepository
import com.teco.ventago.features.product.domain.ProductService
import com.teco.ventago.features.product.domain.model.Category
import com.teco.ventago.features.product.domain.model.Item
import com.teco.ventago.features.product.domain.model.Products
import io.ktor.client.HttpClient
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
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

    private fun menu(): Products {
        return Products(
            id = 7,
            active = true,
            categories = listOf(
                Category(
                    id = 1,
                    name = "Bebidas",
                    desc = "",
                    active = true,
                    items = emptyList(),
                    order = 1,
                )
            ),
        )
    }

    private class FakeProductsRepository(
        private val menu: Products,
    ) : IProductsRepository {
        override suspend fun setCategoryActive(categoryId: Int, active: Boolean): Boolean = true
        override suspend fun changeCategoryOrder(categories: List<Category>): Boolean = true
        override suspend fun editCategory(category: Category): Boolean = true
        override suspend fun addCategory(category: Category, menuId: Int): Category = category
        override suspend fun removeCategory(categoryId: Int): Boolean = true
        override suspend fun addItem(item: Item, categoryId: Int): Item = item
        override suspend fun editItem(item: Item, categoryId: Int): Boolean = true
        override suspend fun removeItem(itemId: Int): Boolean = true
        override suspend fun changeItemOrder(items: List<Item>): Boolean = true
        override suspend fun getProductsByBusinessId(businessId: Int): Products = menu
        override suspend fun getProductIdByBusinessId(businessId: Int): Int = menu.id
    }

    private class FakeCacheService : ICacheService {
        private val saves = Channel<Unit>(capacity = Channel.UNLIMITED)

        suspend fun awaitSave() {
            withTimeout(2_000) {
                saves.receive()
            }
        }

        override suspend fun <T : Any> getCache(klass: KClass<T>): T? = null
        override suspend fun <T : Any> getCache(key: String): T? = null
        override suspend fun <T> saveCache(data: T) {
            saves.trySend(Unit)
        }
        override suspend fun <T> saveCache(key: String, data: T) = Unit
        override suspend fun clearCache(id: String) = Unit
        override suspend fun clearAllCache() = Unit
    }

    private class FakeChangesManager : IChangesManager {
        var productsChangedCalls = 0
        private val productChanges = Channel<Unit>(capacity = Channel.UNLIMITED)

        suspend fun awaitProductsChanged() {
            withTimeout(2_000) {
                productChanges.receive()
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

    private class FakeAuthService : IAuthService {
        override suspend fun refreshToken(client: HttpClient, failedAccessToken: String?) = Unit
        override suspend fun googleLogin(googleToken: String): AuthResponse = error("unused")
        override suspend fun emailLogin(request: EmailLoginRequest): AuthResponse = error("unused")
        override suspend fun emailRegister(request: CreateUserRequest): AuthResponse = error("unused")
        override suspend fun sendPasswordResetEmail(email: String) = Unit
        override suspend fun businessRegistered() = Unit
        override suspend fun signOut() = Unit
        override fun getFirebaseUser(): Flow<FirebaseUserDM?> = emptyFlow()
        override fun getUser(): Flow<User?> = emptyFlow()
        override fun getUserSync(): User? = null
        override fun getJwtToken(refresh: Boolean): String? = null
        override fun isAuthenticated(): Boolean = true
        override fun setPremium(premium: Boolean) = Unit
        override suspend fun deleteAccount(token: String): Boolean = false
    }
}
