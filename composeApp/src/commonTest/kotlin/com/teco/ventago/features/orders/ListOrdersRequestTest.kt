package com.teco.ventago.features.orders

import com.teco.ventago.features.orders.domain.models.requests.ListOrdersRequest
import com.teco.ventago.features.orders.domain.models.requests.orderEmissionEndDate
import com.teco.ventago.features.orders.domain.models.requests.orderEmissionStartDate
import com.teco.ventago.features.orders.domain.models.requests.toApiJsonString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ListOrdersRequestTest {

    @Test
    fun yesterdayDateOnlyRequestMatchesWebPayloadShape() {
        val request = ListOrdersRequest(
            businessId = 4,
            page = 1,
            pageSize = 10,
            emissionStartDate = orderEmissionStartDate("2026-05-07"),
            emissionEndDate = orderEmissionEndDate("2026-05-07")
        )

        val encoded = Json.parseToJsonElement(request.toApiJsonString()).jsonObject

        assertEquals(5, encoded.size)
        assertEquals(4, encoded["business_id"]?.jsonPrimitive?.content?.toInt())
        assertEquals(1, encoded["page"]?.jsonPrimitive?.content?.toInt())
        assertEquals(10, encoded["page_size"]?.jsonPrimitive?.content?.toInt())
        assertEquals("2026-05-07T00:00:00-05:00", encoded["emission_start_date"]?.jsonPrimitive?.content)
        assertEquals("2026-05-07T23:59:59-05:00", encoded["emission_end_date"]?.jsonPrimitive?.content)
        assertFalse(encoded.containsKey("payment_status"))
        assertFalse(encoded.containsKey("order_type"))
        assertFalse(encoded.containsKey("customer_ruc"))
        assertFalse(encoded.containsKey("customer_id"))
    }
}
