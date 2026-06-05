package com.teco.ventago.features.branches

import com.teco.ventago.features.branches.domain.model.Branch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class BranchModelTest {

    @Test
    fun parsesTradeNameAndLogoUrlWhenPresent() {
        val branch = Branch(jsonObject("""
            {
              "code": "0001",
              "name": "Sucursal Centro",
              "trade_name": "VentaGo Centro",
              "address_line": "Calle 50",
              "location_code": "8-8-8",
              "longitude": "-79.52",
              "latitude": "8.98",
              "logo_url": "https://cdn.example.com/branch-logo.jpg",
              "status": 1,
              "billing_points": []
            }
            """.trimIndent()
        ))

        assertEquals("VentaGo Centro", branch.tradeName)
        assertEquals("https://cdn.example.com/branch-logo.jpg", branch.logoUrl)
        assertEquals("Sucursal Centro", branch.name)
    }

    @Test
    fun keepsCompatibilityWhenTradeNameAndLogoUrlAreMissing() {
        val branch = Branch(jsonObject("""
            {
              "code": "0000",
              "name": "Sucursal Principal",
              "address_line": "",
              "location_code": "",
              "longitude": "",
              "latitude": "",
              "status": 1,
              "billing_points": []
            }
            """.trimIndent()
        ))

        assertNull(branch.tradeName)
        assertNull(branch.logoUrl)
        assertEquals("0000", branch.branchCode)
    }

    private fun jsonObject(value: String): JsonObject {
        return Json.parseToJsonElement(value) as JsonObject
    }
}
