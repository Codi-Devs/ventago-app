package com.teco.ventago.features.business

import com.teco.ventago.core.cache.ICacheService
import com.teco.ventago.core.changes.IChangesManager
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.auth.domain.model.User
import com.teco.ventago.features.auth.domain.model.firebase.FirebaseUserDM
import com.teco.ventago.features.auth.domain.model.requests.CreateUserRequest
import com.teco.ventago.features.auth.domain.model.requests.EmailLoginRequest
import com.teco.ventago.features.auth.domain.model.response.AuthResponse
import com.teco.ventago.features.business.data.repository.IBusinessRepository
import com.teco.ventago.features.business.domain.BusinessService
import com.teco.ventago.features.business.domain.model.Business
import com.teco.ventago.features.business.domain.model.BusinessAddress
import com.teco.ventago.features.business.domain.model.BusinessSocialNetwork
import com.teco.ventago.features.business.domain.model.Currency
import com.teco.ventago.features.business.domain.model.requests.RegisterBusinessRequest
import com.teco.ventago.features.business.domain.model.responses.BusinessRegisterResponse
import com.teco.ventago.features.product.data.repository.IProductsRepository
import com.teco.ventago.features.product.domain.ProductService
import com.teco.ventago.features.product.domain.model.Category
import com.teco.ventago.features.product.domain.model.Item
import com.teco.ventago.features.product.domain.model.Products
import io.ktor.client.HttpClient
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class BusinessServiceLogoCacheTest {

    @Test
    fun logoSaveDoesNotPublishBusinessToken() = runTest {
        val changes = FakeChangesManager()
        val service = service(changes, this)
        service.saveBusiness(sampleBusiness(), publishChange = false)
        advanceUntilIdle()
        assertEquals(0, changes.businessChangedCalls)
    }

    @Test
    fun localBusinessSavePublishesOneToken() = runTest {
        val changes = FakeChangesManager()
        val service = service(changes, this)
        service.saveBusiness(sampleBusiness(), publishChange = true)
        advanceUntilIdle()
        assertEquals(1, changes.businessChangedCalls)
    }

    private fun service(changes: FakeChangesManager, scope: CoroutineScope): BusinessService {
        val cache = FakeCacheService()
        return BusinessService(
            repository = FakeBusinessRepository(),
            cache = cache,
            changesManager = changes,
            authService = FakeAuthService(),
            productService = ProductService(
                productsRepository = FakeProductsRepository(),
                cache = cache,
                changesManager = changes,
                authService = FakeAuthService(),
            ),
            appScope = scope,
        )
    }

    private fun sampleBusiness(): Business {
        return Business(
            businessId = 9,
            name = "Demo",
            description = "",
            active = true,
            currency = Currency(1, "Balboa", "PAB", "B/."),
            logo = "https://cdn.example/logo.jpg",
            socialNetwork = BusinessSocialNetwork("", "", "", ""),
            phone = "",
            address = BusinessAddress("", "", 0.0, 0.0),
            domain = "",
            isFull = false,
            ruc = null,
            web = null,
            businessEmail = null,
        )
    }

    private class FakeBusinessRepository : IBusinessRepository {
        override suspend fun updateWebStyle(businessId: Int, styleId: Int, primaryColor: String, secondaryColor: String) = false
        override suspend fun resetColors(businessId: Int) = false
        override suspend fun registerBusiness(request: RegisterBusinessRequest) = BusinessRegisterResponse(0, 0)
        override suspend fun getBusinessById(businessId: Int): Business = error("unused")
        override suspend fun getBusinesses(): List<Business> = emptyList()
        override suspend fun deleteBusiness(businessId: Int) = false
    }

    private class FakeProductsRepository : IProductsRepository {
        override suspend fun setCategoryActive(categoryId: Int, active: Boolean) = false
        override suspend fun changeCategoryOrder(categories: List<Category>) = false
        override suspend fun editCategory(category: Category) = false
        override suspend fun addCategory(category: Category, menuId: Int) = category
        override suspend fun removeCategory(categoryId: Int) = false
        override suspend fun addItem(item: Item, categoryId: Int) = item
        override suspend fun editItem(item: Item, categoryId: Int) = false
        override suspend fun removeItem(itemId: Int) = false
        override suspend fun changeItemOrder(items: List<Item>) = false
        override suspend fun getProductsByBusinessId(businessId: Int) = Products(0, true, emptyList())
        override suspend fun getProductIdByBusinessId(businessId: Int) = 0
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
        var businessChangedCalls = 0
        override fun productsListener(): Flow<Int> = emptyFlow()
        override fun businessListener(): Flow<Int> = emptyFlow()
        override fun financialListener(): Flow<Int> = emptyFlow()
        override fun customersListener(): Flow<Int> = emptyFlow()
        override fun branchesListener(): Flow<Int> = emptyFlow()
        override fun userListener(): Flow<Int> = emptyFlow()
        override fun purchaseListener(): Flow<Int> = emptyFlow()
        override fun addedBusinessListener(): Flow<Int> = emptyFlow()
        override suspend fun productsChanged() = Unit
        override suspend fun businessChanged() {
            businessChangedCalls += 1
        }
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
