package com.teco.ventago.features.customers

import com.teco.ventago.core.Paged
import com.teco.ventago.core.cache.ICacheService
import com.teco.ventago.core.changes.IChangesManager
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.features.customers.data.provider.ICustomerProvider
import com.teco.ventago.features.customers.data.provider.buildCreateCustomerRequestBody
import com.teco.ventago.features.customers.data.repository.CustomerRepository
import com.teco.ventago.features.customers.data.repository.ICustomerRepository
import com.teco.ventago.features.customers.data.repository.dto.CustomerCreatedDto
import com.teco.ventago.features.customers.domain.CustomerService
import com.teco.ventago.features.customers.domain.models.CreateBillingAddressRequest
import com.teco.ventago.features.customers.domain.models.Customer
import com.teco.ventago.features.customers.domain.models.CustomerAddress
import com.teco.ventago.features.customers.domain.models.CustomerCreateValidation
import com.teco.ventago.features.customers.domain.models.CustomerDetails
import com.teco.ventago.features.customers.domain.models.CustomerListItem
import com.teco.ventago.features.customers.domain.models.CustomerTaxRetentionCatalog
import com.teco.ventago.features.customers.domain.models.UpdateBillingAddressRequest
import com.teco.ventago.features.customers.domain.models.UpdateCustomerDetailsRequest
import com.teco.ventago.features.customers.domain.models.ValidateRucResponse
import com.teco.ventago.features.customers.ui.form.viewmodel.CustomerFormState
import com.teco.ventago.features.invoicing.domain.TaxPayerType
import com.teco.ventago.features.invoicing.domain.models.FeCustomerType
import com.teco.ventago.features.orders.data.provider.OrdersRequests
import com.teco.ventago.features.orders.domain.models.requests.AdditionalAddress
import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.utils.ApiError
import com.teco.ventago.utils.ApiResponse
import com.teco.ventago.utils.DuplicateCustomerException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.assertNull

class CustomerModelsAndOrdersRequestTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun customerFormStateUsesFullForeignCountryCatalog() {
        val countries = CustomerFormState().countryOptions
        val countriesByCode = countries.associateBy { it.code }

        assertEquals(219, countries.size)
        assertEquals("Afghanistan", countries.first().name)
        assertEquals("Zimbabwe", countries.last().name)
        assertEquals("Anguilla", countriesByCode["AI"]?.name)
        assertEquals("Turks and Caicos Islands", countriesByCode["TC"]?.name)
        assertEquals("Virgin Islands (U.S.)", countriesByCode["VI"]?.name)
    }

    @Test
    fun parseCustomerDetailsPayload() {
        val payload = json.parseToJsonElement(
            """
            {
              "id": 123,
              "fe_customer_type": "01",
              "taxpayer_type": "1",
              "ruc_number": "15552345",
              "ruc_check_digit": "12",
              "legal_name": "EMPRESA DEMO, S.A.",
              "address_line": "Calle 50",
              "location_code": "8-8-0",
              "province": "PANAMA",
              "district": "PANAMA",
              "corregimiento": "BELLA VISTA",
              "country_code": "PA",
              "phone1": "6000-0000",
              "email": "facturas@demo.com",
              "tax_exempt": true,
              "tax_retention_code": "08",
              "tax_retention_percent": 35,
              "status": 1
            }
            """.trimIndent()
        )

        val details = json.decodeFromJsonElement<CustomerDetails>(payload)
        assertEquals(123, details.id)
        assertEquals("01", details.feCustomerType)
        assertEquals("facturas@demo.com", details.email)
        assertTrue(details.taxExempt)
        assertEquals(8, details.taxRetentionCode)
        assertEquals(35, details.taxRetentionPercent)
    }

    @Test
    fun parseCustomerListItemAcceptsCamelCaseTaxFields() {
        val payload = json.parseToJsonElement(
            """
            {
              "id": 12,
              "name": "Cliente Demo",
              "email": "cliente@demo.com",
              "ruc": "15552345",
              "status": 1,
              "invoice_customer": 1,
              "updated_at": 10,
              "taxExempt": false,
              "taxRetentionCode": "01",
              "taxRetentionPercent": 0
            }
            """.trimIndent()
        )

        val customer = json.decodeFromJsonElement<CustomerListItem>(payload)
        assertEquals(false, customer.taxExempt)
        assertEquals(1, customer.taxRetentionCode)
        assertEquals(0, customer.taxRetentionPercent)
        assertEquals(1, CustomerTaxRetentionCatalog.indexOfCode(customer.taxRetentionCode))
    }

    @Test
    fun parseCustomerCreatedResponseWithoutOptionalPhoneTaxIdAndTags() {
        val payload = json.parseToJsonElement(
            """
            {
              "id": 1022,
              "name": "asdasd",
              "email": "noemail@pos.com",
              "country_code": "PA",
              "tax_exempt": false,
              "tax_retention_code": 0,
              "tax_retention_percent": 0
            }
            """.trimIndent()
        )

        val created = json.decodeFromJsonElement<CustomerCreatedDto>(payload)
        assertEquals(1022L, created.id)
        assertEquals("asdasd", created.name)
        assertEquals("noemail@pos.com", created.email)
        assertNull(created.phone)
        assertNull(created.ruc)
        assertNull(created.tags)
        assertEquals("PA", created.countryCode)
        assertEquals(false, created.taxExempt)
        assertEquals(0, created.taxRetentionCode)
        assertEquals(0, created.taxRetentionPercent)
    }

    @Test
    fun parseCustomerAddressesWithoutCustomerIdField() {
        val payload = json.parseToJsonElement(
            """
            [
              {
                "id": 10,
                "address_line": "Altaplaza Mall",
                "location_code": "8-8-0",
                "email": "facturacion.sucursal@cliente.com",
                "province": "PANAMA",
                "district": "PANAMA",
                "corregimiento": "ANCON",
                "is_default": false
              }
            ]
            """.trimIndent()
        )

        val addresses = payload.jsonArray.map {
            json.decodeFromJsonElement<CustomerAddress>(it)
        }

        assertEquals(1, addresses.size)
        assertEquals(10L, addresses.first().id)
        assertEquals("Altaplaza Mall", addresses.first().addressLine)
        assertEquals("facturacion.sucursal@cliente.com", addresses.first().email)
        assertEquals(null, addresses.first().customerId)
    }

    @Test
    fun createAndUpdateBillingAddressRequestsSerializeEmail() {
        val createBody = json.encodeToJsonElement(
            CreateBillingAddressRequest(
                addressLine = "Av. Balboa",
                locationCode = "8-8-8",
                email = "facturacion@cliente.com"
            )
        ).jsonObject

        val updateBody = json.encodeToJsonElement(
            UpdateBillingAddressRequest(
                addressLine = "Av. Balboa Piso 15",
                locationCode = "8-8-8",
                email = "cuentas@cliente.com"
            )
        ).jsonObject

        assertEquals("\"facturacion@cliente.com\"", createBody["email"]?.toString())
        assertEquals("\"cuentas@cliente.com\"", updateBody["email"]?.toString())
    }

    @Test
    fun additionalAddressSerializesEmailWhenProvided() {
        val payload = json.encodeToJsonElement(
            AdditionalAddress(
                addressLine = "Av. Balboa, Torre B",
                locationCode = "8-8-8",
                email = "facturacion@cliente.com"
            )
        ).jsonObject

        assertEquals("\"facturacion@cliente.com\"", payload["email"]?.toString())
    }

    @Test
    fun ordersRequestIncludesCustomerIdWhenProvided() {
        val request = OrdersRequests.loadOrders(
            businessId = 1,
            pageSize = 10,
            page = 0,
            paymentStatus = null,
            customerId = 123
        )

        val body = json.parseToJsonElement(request).jsonObject
        assertEquals(123, body["customer_id"]?.toString()?.toInt())
    }

    @Test
    fun ordersRequestOmitsCustomerIdWhenNotProvided() {
        val request = OrdersRequests.loadOrders(
            businessId = 1,
            pageSize = 10,
            page = 0,
            paymentStatus = null,
            customerId = null
        )

        val body = json.parseToJsonElement(request).jsonObject
        assertTrue(body["customer_id"] == null)
    }

    @Test
    fun repositoryTreatsNullErrorAsSuccessForCustomerMutations() = runBlocking {
        val provider = FakeCustomerProvider()
        val repository = CustomerRepository(provider = provider, logger = FakeLogger())

        val updateCustomerOk = repository.updateCustomerDetails(
            businessId = 1,
            customerId = 123,
            request = UpdateCustomerDetailsRequest(
                email = "nuevo@mail.com",
                taxExempt = false,
                taxRetentionCode = null,
                taxRetentionPercent = null,
            )
        )
        val deleteCustomerOk = repository.deleteCustomer(
            businessId = 1,
            customerId = 123
        )
        val createAddressOk = repository.createCustomerAddress(
            businessId = 1,
            customerId = 123,
            request = CreateBillingAddressRequest(addressLine = "Altaplaza Mall", locationCode = "8-8-0")
        )
        val updateAddressOk = repository.updateCustomerAddress(
            businessId = 1,
            customerId = 123,
            addressId = 10,
            request = UpdateBillingAddressRequest(addressLine = "Costa del Este", locationCode = "8-8-0")
        )
        val deleteAddressOk = repository.deleteCustomerAddress(
            businessId = 1,
            customerId = 123,
            addressId = 10
        )

        assertTrue(updateCustomerOk)
        assertTrue(deleteCustomerOk)
        assertTrue(createAddressOk)
        assertTrue(updateAddressOk)
        assertTrue(deleteAddressOk)
    }

    @Test
    fun repositoryThrowsDuplicateCustomerExceptionForCu004() {
        runBlocking {
            val provider = FakeCustomerProvider(
                createCustomerResponse = ApiResponse(
                    successful = false,
                    data = JsonNull,
                    error = ApiError.CUSTOMER_ALREADY_EXISTS,
                    errorCode = "CU_004",
                )
            )
            val repository = CustomerRepository(provider = provider, logger = FakeLogger())

            assertFailsWith<DuplicateCustomerException> {
                repository.createCustomer(sampleCustomer(name = "Cliente Demo"), businessId = 1)
            }
        }
    }

    @Test
    fun createCustomerRequestBodyKeepsExplicitNullsAndNormalizesBlankOptionals() {
        val body = json.parseToJsonElement(
            buildCreateCustomerRequestBody(
                Customer(
                    id = -1,
                    name = "  asdasd  ",
                    phone = "",
                    email = " ",
                    ruc = "",
                    invoiceCustomer = true,
                    rucCheckDigit = null,
                    tags = listOf("", "   "),
                    customerType = FeCustomerType.FINAL_CONSUMER,
                    taxPayerType = TaxPayerType.NATURAL,
                    addressLine = "Panama",
                    province = "COLON",
                    district = "COLON",
                    corregimiento = "BARRIO SUR",
                    locationCode = "3-1-2",
                    foreignIdType = "",
                    foreignIdNumber = "",
                    cedulaCF = "8-666-9885",
                    countryCode = "PA",
                    taxExempt = false,
                    taxRetentionCode = null,
                    taxRetentionPercent = null,
                )
            )
        ).jsonObject

        assertEquals("\"asdasd\"", body["name"]?.toString())
        assertEquals(JsonNull, body["email"])
        assertEquals(JsonNull, body["phone"])
        assertEquals(JsonNull, body["tax_id"])
        assertEquals(JsonNull, body["tags"])
        assertEquals("\"02\"", body["fe_customer_type"]?.toString())
        assertEquals("\"1\"", body["taxpayer_type"]?.toString())
        assertEquals("\"Panama\"", body["address_line"]?.toString())
        assertEquals("\"3-1-2\"", body["location_code"]?.toString())
        assertEquals("\"COLON\"", body["province"]?.toString())
        assertEquals("\"COLON\"", body["district"]?.toString())
        assertEquals("\"BARRIO SUR\"", body["corregimiento"]?.toString())
        assertEquals(JsonNull, body["foreign_id_type"])
        assertEquals(JsonNull, body["foreign_id_number"])
        assertEquals("\"8-666-9885\"", body["cedula_cf"]?.toString())
        assertEquals(JsonNull, body["country_other_name"])
        assertEquals("false", body["tax_exempt"]?.toString())
        assertEquals(JsonNull, body["tax_retention_code"])
        assertEquals(JsonNull, body["tax_retention_percent"])
    }

    @Test
    fun createCustomerValidationRequiresPanamaLocationForNonForeignCustomers() {
        assertEquals(
            "La provincia es requerida.",
            CustomerCreateValidation.requiredLocationMessage(
                customerType = FeCustomerType.FINAL_CONSUMER,
                addressLine = "Panama",
                province = null,
                district = "COLON",
                corregimiento = "BARRIO SUR",
            )
        )
        assertEquals(
            "La direccion es requerida.",
            CustomerCreateValidation.requiredLocationMessage(
                customerType = FeCustomerType.CONTRIBUTING,
                addressLine = " ",
                province = "COLON",
                district = "COLON",
                corregimiento = "BARRIO SUR",
            )
        )
        assertNull(
            CustomerCreateValidation.requiredLocationMessage(
                customerType = FeCustomerType.FOREIGNER,
                addressLine = null,
                province = null,
                district = null,
                corregimiento = null,
            )
        )
    }

    @Test
    fun serviceUpdatesStateAndCacheAfterCreateUpdateDelete() = runBlocking {
        val repository = FakeCustomerRepository()
        val cache = FakeCacheService()
        val changes = FakeChangesManager()
        val service = CustomerService(
            repository = repository,
            loggerService = FakeLogger(),
            cache = cache,
            changesManager = changes,
            appScope = CoroutineScope(Dispatchers.IO)
        )

        service.listCustomers(
            businessId = 1,
            page = 0,
            size = 10
        )

        val created = service.createCustomer(sampleCustomer(name = "New Customer"), businessId = 1)
        service.updateCustomerDetails(
            businessId = 1,
            customerId = 1,
            request = UpdateCustomerDetailsRequest(
                email = "updated@mail.com",
                taxExempt = true,
                taxRetentionCode = 8,
                taxRetentionPercent = 35,
            )
        )
        val updatedCustomer = service.observe().value.items.first { it.id == 1L }
        assertTrue(updatedCustomer.taxExempt)
        assertEquals(8, updatedCustomer.taxRetentionCode)
        assertEquals(35, updatedCustomer.taxRetentionPercent)
        service.deleteCustomer(
            businessId = 1,
            customerId = 1
        )

        repeat(40) {
            if (cache.savedValues.isNotEmpty() && changes.customersChangedCalls >= 3) return@repeat
            delay(10)
        }

        val customers = service.observe().value
        assertTrue(customers.items.any { it.id == created.id })
        assertTrue(customers.items.none { it.id == 1L })
        assertTrue(cache.savedValues.isNotEmpty())
        assertTrue(changes.customersChangedCalls >= 3)
    }

    private fun sampleCustomer(name: String): Customer {
        return Customer(
            id = -1,
            name = name,
            phone = "6000-0000",
            email = "customer@mail.com",
            ruc = "15552345",
            invoiceCustomer = true,
            rucCheckDigit = "12",
            tags = emptyList(),
            customerType = FeCustomerType.CONTRIBUTING,
            taxPayerType = TaxPayerType.NATURAL,
            addressLine = "Calle 50",
            province = "PANAMA",
            district = "PANAMA",
            corregimiento = "BELLA VISTA",
            locationCode = "8-8-0",
            foreignIdType = null,
            foreignIdNumber = null,
            cedulaCF = null,
            countryCode = "PA",
            taxExempt = true,
            taxRetentionCode = 2,
            taxRetentionPercent = 50,
        )
    }

    private class FakeCustomerProvider(
        private val createCustomerResponse: ApiResponse = defaultSuccessResponse
    ) : ICustomerProvider {
        private val successResponse = defaultSuccessResponse

        override suspend fun createCustomer(customer: Customer, businessId: Int): ApiResponse =
            createCustomerResponse

        companion object {
            private val defaultSuccessResponse = ApiResponse(
                successful = false,
                data = JsonNull,
                error = ApiError.NO_ERROR
            )
        }

        override suspend fun listCustomers(
            businessId: Int,
            page: Int,
            size: Int,
            ruc: String?,
            email: String?,
            name: String?
        ): ApiResponse = successResponse

        override suspend fun validateRUC(ruc: String, businessId: Int): ApiResponse = successResponse
        override suspend fun validateRUCRegister(ruc: String): ApiResponse = successResponse
        override suspend fun getCustomerById(businessId: Int, customerId: Long): ApiResponse = successResponse
        override suspend fun updateCustomerDetails(
            businessId: Int,
            customerId: Long,
            request: UpdateCustomerDetailsRequest
        ): ApiResponse = successResponse

        override suspend fun deleteCustomer(businessId: Int, customerId: Long): ApiResponse = successResponse
        override suspend fun listCustomerAddresses(businessId: Int, invoiceCustomerId: Int): ApiResponse =
            successResponse

        override suspend fun createCustomerAddress(
            businessId: Int,
            customerId: Long,
            request: CreateBillingAddressRequest
        ): ApiResponse = successResponse

        override suspend fun updateCustomerAddress(
            businessId: Int,
            customerId: Long,
            addressId: Long,
            request: UpdateBillingAddressRequest
        ): ApiResponse = successResponse

        override suspend fun deleteCustomerAddress(
            businessId: Int,
            customerId: Long,
            addressId: Long
        ): ApiResponse = successResponse
    }

    private class FakeCustomerRepository : ICustomerRepository {
        private var customers = Paged(
            page = 0,
            size = 10,
            total = 1,
            items = listOf(
                CustomerListItem(
                    id = 1,
                    name = "Cliente Inicial",
                    email = "old@mail.com",
                    ruc = "15552345",
                    status = 1,
                    invoiceCustomer = 1,
                    updatedAt = 1,
                    taxExempt = false,
                    taxRetentionCode = null,
                    taxRetentionPercent = null,
                )
            )
        )

        override suspend fun createCustomer(customer: Customer, businessId: Int): CustomerCreatedDto {
            return CustomerCreatedDto(
                id = 99,
                name = customer.name,
                email = customer.email,
                phone = customer.phone,
                ruc = customer.ruc,
                countryCode = customer.countryCode,
                tags = null
            )
        }

        override suspend fun listCustomers(
            businessId: Int,
            page: Int,
            size: Int,
            ruc: String?,
            email: String?,
            name: String?
        ): Paged<CustomerListItem> = customers

        override suspend fun validateRUC(ruc: String, businessId: Int): ValidateRucResponse {
            return ValidateRucResponse("1", ruc, "12", "EMPRESA DEMO, S.A.", true)
        }

        override suspend fun validateRUCRegister(ruc: String): ValidateRucResponse {
            return ValidateRucResponse("1", ruc, "12", "EMPRESA DEMO, S.A.", true)
        }

        override suspend fun getCustomerById(businessId: Int, customerId: Long): CustomerDetails {
            return CustomerDetails(
                id = customerId,
                taxExempt = true,
                taxRetentionCode = 8,
                taxRetentionPercent = 35,
            )
        }

        override suspend fun updateCustomerDetails(
            businessId: Int,
            customerId: Long,
            request: UpdateCustomerDetailsRequest
        ): Boolean = true

        override suspend fun deleteCustomer(businessId: Int, customerId: Long): Boolean = true

        override suspend fun listCustomerAddresses(
            businessId: Int,
            invoiceCustomerId: Int
        ): List<CustomerAddress> = emptyList()

        override suspend fun createCustomerAddress(
            businessId: Int,
            customerId: Long,
            request: CreateBillingAddressRequest
        ): Boolean = true

        override suspend fun updateCustomerAddress(
            businessId: Int,
            customerId: Long,
            addressId: Long,
            request: UpdateBillingAddressRequest
        ): Boolean = true

        override suspend fun deleteCustomerAddress(
            businessId: Int,
            customerId: Long,
            addressId: Long
        ): Boolean = true
    }

    private class FakeCacheService : ICacheService {
        val savedValues = mutableListOf<Any?>()

        override suspend fun <T : Any> getCache(klass: kotlin.reflect.KClass<T>): T? = null
        override suspend fun <T : Any> getCache(key: String): T? = null
        override suspend fun <T> saveCache(data: T) {
            savedValues.add(data)
        }

        override suspend fun <T> saveCache(key: String, data: T) {
            savedValues.add(data)
        }

        override suspend fun clearCache(id: String) = Unit
        override suspend fun clearAllCache() = Unit
    }

    private class FakeChangesManager : IChangesManager {
        var customersChangedCalls = 0

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
        override suspend fun customersChanged() {
            customersChangedCalls++
        }

        override suspend fun userChanged() = Unit
        override fun removeListeners() = Unit
        override fun initialize(businessId: Int, menuId: Int, userId: Int) = Unit
    }

    private class FakeLogger : ILoggerService {
        override fun sendLog(log: Log) = Unit
    }
}
