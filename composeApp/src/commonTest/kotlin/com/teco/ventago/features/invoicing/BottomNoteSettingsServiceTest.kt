package com.teco.ventago.features.invoicing

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
import com.teco.ventago.features.invoicing.data.repository.IInvoicingSettingsRepository
import com.teco.ventago.features.invoicing.domain.InvoicingSettingsService
import com.teco.ventago.features.invoicing.domain.InvoicingSettingsStore
import com.teco.ventago.features.invoicing.domain.models.BottomNoteSettings
import com.teco.ventago.features.invoicing.domain.models.BottomNoteSettingsRequest
import com.teco.ventago.features.product.data.repository.IProductsRepository
import com.teco.ventago.features.product.domain.ProductService
import com.teco.ventago.features.product.domain.model.Category
import com.teco.ventago.features.product.domain.model.Item
import com.teco.ventago.features.product.domain.model.Products
import com.teco.ventago.json
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BottomNoteSettingsServiceTest {
    @Test
    fun refreshStoresConfiguredSettingsAndLoadsCachedValue() = runTest {
        val store = FakeInvoicingSettingsStore()
        val businessService = fakeBusinessService().also {
            it.business.value = sampleBusiness()
        }
        val remote = sampleSettings(title = "Información de pago", includeOnInvoice = true)
        val service = InvoicingSettingsService(
            repository = FakeInvoicingSettingsRepository(remoteSettings = remote),
            businessService = businessService,
            authService = FakeAuthService(),
            store = store,
            json = json,
            appScope = backgroundScope,
        )

        service.refreshBottomNoteSettings()

        assertEquals(remote, service.bottomNoteSettings().value.settings)
        assertFalse(service.bottomNoteSettings().value.refreshFailed)

        val cachedService = InvoicingSettingsService(
            repository = FakeInvoicingSettingsRepository(failRefresh = true),
            businessService = businessService,
            authService = FakeAuthService(),
            store = store,
            json = json,
            appScope = backgroundScope,
        )
        cachedService.loadCachedBottomNoteSettingsForCurrentBusiness()

        assertEquals(remote, cachedService.bottomNoteSettings().value.settings)
        assertFalse(cachedService.bottomNoteSettings().value.refreshFailed)
    }

    @Test
    fun refreshFailureKeepsCachedSettingsButMarksStateAsFailed() = runTest {
        val store = FakeInvoicingSettingsStore()
        val businessService = fakeBusinessService().also {
            it.business.value = sampleBusiness()
        }
        val cached = sampleSettings(title = "Métodos de pago", includeOnInvoice = false)
        val service = InvoicingSettingsService(
            repository = FakeInvoicingSettingsRepository(remoteSettings = cached),
            businessService = businessService,
            authService = FakeAuthService(),
            store = store,
            json = json,
            appScope = backgroundScope,
        )
        service.refreshBottomNoteSettings()

        val failingService = InvoicingSettingsService(
            repository = FakeInvoicingSettingsRepository(failRefresh = true),
            businessService = businessService,
            authService = FakeAuthService(),
            store = store,
            json = json,
            appScope = backgroundScope,
        )
        failingService.refreshBottomNoteSettings()

        assertEquals(cached, failingService.bottomNoteSettings().value.settings)
        assertTrue(failingService.bottomNoteSettings().value.refreshFailed)
    }

    @Test
    fun deleteClearsCachedAndObservedSettings() = runTest {
        val store = FakeInvoicingSettingsStore()
        val businessService = fakeBusinessService().also {
            it.business.value = sampleBusiness()
        }
        val service = InvoicingSettingsService(
            repository = FakeInvoicingSettingsRepository(remoteSettings = sampleSettings()),
            businessService = businessService,
            authService = FakeAuthService(),
            store = store,
            json = json,
            appScope = backgroundScope,
        )
        service.refreshBottomNoteSettings()
        assertNotNull(service.bottomNoteSettings().value.settings)

        assertTrue(service.deleteBottomNoteSettings())

        assertNull(service.bottomNoteSettings().value.settings)
        assertFalse(store.keys().any { it.contains("invoicing_bottom_note_settings_cache") })
    }

    private fun fakeBusinessService(): BusinessService {
        val auth = FakeAuthService()
        val changes = FakeChangesManager()
        val cache = FakeCacheService()
        val productService = ProductService(
            productsRepository = FakeProductsRepository(),
            cache = cache,
            changesManager = changes,
            authService = auth,
        )
        return BusinessService(
            repository = FakeBusinessRepository(),
            cache = cache,
            changesManager = changes,
            authService = auth,
            productService = productService,
        )
    }

    private fun sampleBusiness(): Business = Business(
        businessId = 123,
        name = "Demo",
        description = "Demo",
        active = true,
        currency = Currency(140, "US Dollar", "USD", "$"),
        logo = "",
        socialNetwork = BusinessSocialNetwork("", "", "", ""),
        phone = "60000000",
        address = BusinessAddress("", "Panamá", 0.0, 0.0),
        domain = "demo",
        isFull = true,
        ruc = "123",
        web = null,
        businessEmail = null,
    )

    private fun sampleSettings(
        title: String = "Información de pago",
        includeOnInvoice: Boolean = true,
    ): BottomNoteSettings = BottomNoteSettings(
        id = 1,
        businessId = 123,
        title = title,
        body = "<ol><li><strong>Notas</strong>:</li></ol>",
        includeOnInvoice = includeOnInvoice,
        createdAt = "2026-06-04T18:00:00Z",
        updatedAt = "2026-06-04T18:00:00Z",
    )

    private class FakeInvoicingSettingsStore : InvoicingSettingsStore {
        private val values = mutableMapOf<String, String>()
        override fun set(key: String, value: String): Boolean {
            values[key] = value
            return true
        }
        override fun string(key: String): String? = values[key]
        override fun deleteObject(key: String): Boolean = values.remove(key) != null
        fun keys(): Set<String> = values.keys
    }

    private class FakeInvoicingSettingsRepository(
        private val remoteSettings: BottomNoteSettings? = null,
        private val failRefresh: Boolean = false,
    ) : IInvoicingSettingsRepository {
        override suspend fun getBottomNoteSettings(businessId: Int): BottomNoteSettings? {
            if (failRefresh) error("network failed")
            return remoteSettings
        }

        override suspend fun createBottomNoteSettings(
            businessId: Int,
            request: BottomNoteSettingsRequest,
        ): BottomNoteSettings = BottomNoteSettings(
            id = 1,
            businessId = businessId,
            title = request.title,
            body = request.body,
            includeOnInvoice = request.includeOnInvoice,
        )

        override suspend fun updateBottomNoteSettings(
            businessId: Int,
            request: BottomNoteSettingsRequest,
        ): BottomNoteSettings = createBottomNoteSettings(businessId, request)

        override suspend fun deleteBottomNoteSettings(businessId: Int): Boolean = true
    }

    private class FakeAuthService : IAuthService {
        override suspend fun refreshToken(client: HttpClient) = Unit
        override suspend fun googleLogin(googleToken: String): AuthResponse = error("unused")
        override suspend fun emailLogin(request: EmailLoginRequest): AuthResponse = error("unused")
        override suspend fun emailRegister(request: CreateUserRequest): AuthResponse = error("unused")
        override suspend fun sendPasswordResetEmail(email: String) = Unit
        override suspend fun businessRegistered() = Unit
        override suspend fun signOut() = Unit
        override fun getFirebaseUser(): Flow<FirebaseUserDM?> = flowOf(null)
        override fun getUser(): Flow<User?> = flowOf(null)
        override fun getUserSync(): User? = null
        override fun getJwtToken(refresh: Boolean): String? = "token"
        override fun isAuthenticated(): Boolean = true
        override fun setPremium(premium: Boolean) = Unit
        override suspend fun deleteAccount(token: String): Boolean = true
    }

    private class FakeBusinessRepository : IBusinessRepository {
        override suspend fun updateWebStyle(
            businessId: Int,
            styleId: Int,
            primaryColor: String,
            secondaryColor: String,
        ): Boolean = true
        override suspend fun resetColors(businessId: Int): Boolean = true
        override suspend fun registerBusiness(request: RegisterBusinessRequest): BusinessRegisterResponse = error("unused")
        override suspend fun getBusinessById(businessId: Int): Business = error("unused")
        override suspend fun getBusinesses(): List<Business> = emptyList()
        override suspend fun deleteBusiness(businessId: Int): Boolean = true
    }

    private class FakeProductsRepository : IProductsRepository {
        override suspend fun setCategoryActive(categoryId: Int, active: Boolean): Boolean = true
        override suspend fun changeCategoryOrder(categories: List<Category>): Boolean = true
        override suspend fun editCategory(category: Category): Boolean = true
        override suspend fun addCategory(category: Category, menuId: Int): Category = category
        override suspend fun removeCategory(categoryId: Int): Boolean = true
        override suspend fun addItem(item: Item, categoryId: Int): Item = item
        override suspend fun editItem(item: Item, categoryId: Int): Boolean = true
        override suspend fun removeItem(itemId: Int): Boolean = true
        override suspend fun changeItemOrder(items: List<Item>): Boolean = true
        override suspend fun getProductsByBusinessId(businessId: Int): Products = Products(-1, true, emptyList())
        override suspend fun getProductIdByBusinessId(businessId: Int): Int = -1
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
