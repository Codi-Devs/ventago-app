package com.teco.ventago.features.pos.domain

import com.teco.ventago.features.orders.domain.models.OrderLineDto
import com.teco.ventago.features.orders.domain.models.requests.NameValue
import com.teco.ventago.features.product.domain.model.AdditionalInfoKey
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OrderLineAdditionalInfoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun clonesPanamaCodesAndSkipsEmptyValues() {
        val cloned = cloneOrderLineAdditionalInfo(
            listOf(
                NameValue(AdditionalInfoKey.PANAMA_GOODS_SERVICES_CODE.keyName, JsonPrimitive("1010")),
                NameValue(AdditionalInfoKey.PANAMA_GOODS_SERVICES_UNIT_CODE.keyName, JsonPrimitive("und")),
                NameValue("skip_empty", JsonPrimitive(" ")),
            )
        )
        requireNotNull(cloned)
        assertEquals("1010", cloned.getValue(AdditionalInfoKey.PANAMA_GOODS_SERVICES_CODE.keyName).jsonPrimitive.content)
        assertEquals("und", cloned.getValue(AdditionalInfoKey.PANAMA_GOODS_SERVICES_UNIT_CODE.keyName).jsonPrimitive.content)
        assertNull(cloned["skip_empty"])
    }

    @Test
    fun prefersInvoiceSnapshotOverLiveCatalog() {
        val fromLine = resolveCartAdditionalInfo(
            listOf(NameValue(AdditionalInfoKey.PANAMA_GOODS_SERVICES_CODE.keyName, JsonPrimitive("1010"))),
            buildJsonObject { put(AdditionalInfoKey.PANAMA_GOODS_SERVICES_CODE.keyName, "9999") },
        )
        assertEquals("1010", fromLine?.getValue(AdditionalInfoKey.PANAMA_GOODS_SERVICES_CODE.keyName)?.jsonPrimitive?.content)

        val fromCatalog = resolveCartAdditionalInfo(
            emptyList(),
            buildJsonObject { put(AdditionalInfoKey.PANAMA_GOODS_SERVICES_UNIT_CODE.keyName, "kg") },
        )
        assertEquals("kg", fromCatalog?.getValue(AdditionalInfoKey.PANAMA_GOODS_SERVICES_UNIT_CODE.keyName)?.jsonPrimitive?.content)
        assertNull(cloneOrderLineAdditionalInfo(null))
        assertTrue(additionalInfoToNameValues(fromLine).isNotEmpty())
    }

    @Test
    fun orderLineDtoParsesAdditionalInfoFromFindCufe() {
        val line = json.decodeFromString<OrderLineDto>(
            """
            {
              "item_id": 0,
              "item_name": "asdasdasd",
              "base_unit_price": "1.00",
              "quantity": 1,
              "discount_mode": 0,
              "discount_value": "0",
              "tax_name": "ITBMS",
              "tax_rate": "0.07",
              "line_subtotal": "1.00",
              "tax_amount": "0.07",
              "line_total": "1.07",
              "additional_info": [
                {"name": "panama_goods_services_code", "value": "1010"},
                {"name": "panama_goods_services_unit_code", "value": "und"}
              ]
            }
            """.trimIndent()
        )
        assertEquals(0, line.itemId)
        assertEquals(2, line.additionalInfo.size)
        val resolved = resolveCartAdditionalInfo(line.additionalInfo, null)
        assertEquals(
            "1010",
            resolved?.getValue(AdditionalInfoKey.PANAMA_GOODS_SERVICES_CODE.keyName)?.jsonPrimitive?.contentOrNull
        )
        assertEquals(
            "und",
            resolved?.getValue(AdditionalInfoKey.PANAMA_GOODS_SERVICES_UNIT_CODE.keyName)?.jsonPrimitive?.contentOrNull
        )
    }
}
