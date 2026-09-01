package com.teco.ventago.features.inventory.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class InventorySaleErrorMapperTest {

    @Test
    fun mapsStringErrorCodeFromCreateOrderPayload() {
        val error = Exception("""{"successful":false,"data":{"detail":"Inventory balance is insufficient"},"error":"INV_STK_001"}""")
        assertEquals(
            InventorySaleErrorMapper.INSUFFICIENT_STOCK,
            InventorySaleErrorMapper.messageFor(error, "fallback"),
        )
    }

    @Test
    fun mapsLocationMissingWithoutUsingEnglishBackendDetail() {
        val error = Exception("""{"error":"INV_STK_002","data":{"detail":"missing location"}}""")
        assertEquals(
            InventorySaleErrorMapper.LOCATION_MISSING,
            InventorySaleErrorMapper.messageFor(error, "fallback"),
        )
    }

    @Test
    fun fallsBackWhenCodeIsUnknown() {
        val error = Exception("""{"error":"O_RP_002"}""")
        assertEquals("No se pudo crear el pedido.", InventorySaleErrorMapper.messageFor(error, "No se pudo crear el pedido."))
    }
}
