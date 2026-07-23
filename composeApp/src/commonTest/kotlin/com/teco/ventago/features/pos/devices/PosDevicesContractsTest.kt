package com.teco.ventago.features.pos.devices

import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.features.pos.devices.data.provider.IPosDevicesProvider
import com.teco.ventago.features.pos.devices.data.repository.PosDevicesRepository
import com.teco.ventago.features.pos.devices.domain.model.PosDevicesListRequest
import com.teco.ventago.features.pos.devices.domain.model.UpdatePosDevicePermissionsRequest
import com.teco.ventago.features.pos.devices.domain.model.UpdatePosDeviceRequest
import com.teco.ventago.features.pos.devices.domain.model.isValidPosDeviceBillingPointCode
import com.teco.ventago.features.pos.devices.domain.model.isValidPosDeviceBranchCode
import com.teco.ventago.features.pos.provisioning.domain.model.PosDevicePermissions
import com.teco.ventago.json
import com.teco.ventago.utils.ApiError
import com.teco.ventago.utils.ApiResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class PosDevicesContractsTest {
    @Test
    fun branchAndBillingPointValidationMatchBackendRules() {
        assertTrue(isValidPosDeviceBranchCode("0001"))
        assertTrue(isValidPosDeviceBranchCode("9999"))
        assertFalse(isValidPosDeviceBranchCode("001"))
        assertFalse(isValidPosDeviceBranchCode("00001"))
        assertFalse(isValidPosDeviceBranchCode("00A1"))

        assertTrue(isValidPosDeviceBillingPointCode("001"))
        assertTrue(isValidPosDeviceBillingPointCode("999"))
        assertFalse(isValidPosDeviceBillingPointCode("000"))
        assertFalse(isValidPosDeviceBillingPointCode("01"))
        assertFalse(isValidPosDeviceBillingPointCode("0A1"))
    }

    @Test
    fun permissionUpdateRequestDoesNotSerializeDeviceId() {
        val request = UpdatePosDevicePermissionsRequest.fromPermissions(
            PosDevicePermissions(
                deviceId = "pos_1",
                expensesView = true,
                productsView = true,
                paymentLink = true,
            )
        )

        val encoded = json
            .encodeToString(UpdatePosDevicePermissionsRequest.serializer(), request)
            .let { json.parseToJsonElement(it).jsonObject }

        assertFalse("device_id" in encoded)
        assertEquals("true", encoded.getValue("expenses_view").jsonPrimitive.content)
        assertEquals("true", encoded.getValue("products_view").jsonPrimitive.content)
        assertEquals("true", encoded.getValue("payment_link").jsonPrimitive.content)
    }

    @Test
    fun repositoryParsesPagedListAndKeepsMissingPermissionsNull() = runBlocking {
        val repository = PosDevicesRepository(
            provider = StaticPosDevicesProvider(
                listResponse = ApiResponse(
                    successful = true,
                    data = json.parseToJsonElement(
                        """
                        {
                          "items": [
                            {
                              "device_id": "pos_1",
                              "business_id": 7,
                              "name": "POS Caja 1",
                              "serial_number": "SN-001",
                              "status": "active",
                              "branch_code": "0000",
                              "billing_point_code": "001"
                            }
                          ],
                          "total": 1,
                          "page": 1,
                          "page_size": 20,
                          "total_pages": 1
                        }
                        """.trimIndent()
                    ),
                    error = ApiError.NO_ERROR,
                )
            ),
            logger = NoopLogger,
        )

        val page = repository.listDevices(7, PosDevicesListRequest())

        assertEquals(1, page.total)
        assertEquals("pos_1", page.items.single().deviceId)
        assertEquals("H10P", page.items.single().displayModel)
        assertNull(page.items.single().permissions)
    }

    @Test
    fun repositoryParsesPosConfigPermissions() = runBlocking {
        val repository = PosDevicesRepository(
            provider = StaticPosDevicesProvider(
                posConfigResponse = ApiResponse(
                    successful = true,
                    data = json.parseToJsonElement(
                        """
                        {
                          "device_id": "pos_1",
                          "business_id": 7,
                          "branch_code": "0000",
                          "billing_point_code": "001",
                          "status": "active",
                          "permissions": {
                            "device_id": "pos_1",
                            "expenses_view": true,
                            "expenses_create": true,
                            "products_view": true,
                            "products_create": false,
                            "clients_view": true,
                            "clients_create": false,
                            "quotes_view": true,
                            "quotes_create": false,
                            "payment_methods_configure": true,
                            "payment_yappy_onsite": true,
                            "payment_link": true,
                            "payment_manual_methods": true,
                            "reports_view": true
                          }
                        }
                        """.trimIndent()
                    ),
                    error = ApiError.NO_ERROR,
                )
            ),
            logger = NoopLogger,
        )

        val config = repository.getPosConfig("pos_1")

        assertEquals("pos_1", config.deviceId)
        assertEquals("0000", config.branchCode)
        assertEquals("001", config.billingPointCode)
        assertEquals(true, config.permissions?.expensesView)
        assertEquals(false, config.permissions?.productsCreate)
        assertEquals(true, config.permissions?.paymentMethodsConfigure)
    }

    private class StaticPosDevicesProvider(
        private val listResponse: ApiResponse = ApiResponse(
            successful = true,
            data = JsonObject(emptyMap()),
            error = ApiError.NO_ERROR
        ),
        private val posConfigResponse: ApiResponse = ApiResponse(
            successful = true,
            data = JsonObject(emptyMap()),
            error = ApiError.NO_ERROR
        ),
    ) : IPosDevicesProvider {
        override suspend fun listDevices(businessId: Int, request: PosDevicesListRequest): ApiResponse =
            listResponse

        override suspend fun getPosConfig(deviceId: String): ApiResponse =
            posConfigResponse

        override suspend fun updateDevice(
            businessId: Int,
            deviceId: String,
            request: UpdatePosDeviceRequest,
        ): ApiResponse = ApiResponse(successful = true, data = JsonObject(emptyMap()), error = ApiError.NO_ERROR)

        override suspend fun updatePermissions(
            businessId: Int,
            deviceId: String,
            request: UpdatePosDevicePermissionsRequest,
        ): ApiResponse = ApiResponse(successful = true, data = JsonObject(emptyMap()), error = ApiError.NO_ERROR)
    }

    private object NoopLogger : ILoggerService {
        override fun sendLog(log: Log) = Unit
    }
}
