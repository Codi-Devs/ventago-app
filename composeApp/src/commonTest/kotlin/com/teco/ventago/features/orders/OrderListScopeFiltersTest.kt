package com.teco.ventago.features.orders

import com.teco.ventago.features.branches.domain.model.Branch
import com.teco.ventago.features.branches.domain.model.FiscalBillingPoint
import com.teco.ventago.features.orders.domain.billingPointFilterOptions
import com.teco.ventago.features.orders.domain.branchFilterOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OrderListScopeFiltersTest {
    @Test
    fun hidesFiltersWhenTheBusinessHasOneBranchAndOneBillingPoint() {
        val branches = listOf(branch("0000", "Casa matriz", point("001", "Caja")))

        assertNull(branchFilterOptions(branches))
        assertNull(billingPointFilterOptions(branches, null))
        assertNull(billingPointFilterOptions(branches, "0000"))
    }

    @Test
    fun showsOnlyActiveBranches() {
        val branches = listOf(
            branch("0000", "Casa matriz", point("001", "Caja")),
            branch("0001", "Costa", point("001", "Mostrador"), status = 0),
            branch("0002", "Interior", point("001", "Caja")),
        )

        assertEquals(listOf("0000", "0002"), branchFilterOptions(branches)?.map { it.code })
        assertEquals("0002 - Interior", branchFilterOptions(branches)?.last()?.label)
    }

    @Test
    fun billingPointsFollowTheSelectedBranch() {
        val branches = listOf(
            branch("0000", "Casa matriz", point("001", "Caja"), point("002", "Delivery")),
            branch("0001", "Costa", point("001", "Mostrador")),
        )

        assertEquals(listOf("001", "002"), billingPointFilterOptions(branches, "0000")?.map { it.code })
        assertEquals("002 - Delivery", billingPointFilterOptions(branches, "0000")?.last()?.label)
        assertNull(billingPointFilterOptions(branches, "0001"))
    }

    @Test
    fun allBranchesShareDistinctActiveBillingPoints() {
        val branches = listOf(
            branch("0000", "Casa matriz", point("001", "Caja"), point("002", "Delivery", status = 0)),
            branch("0001", "Costa", point("001", "Mostrador"), point("003", "Kiosco")),
        )

        val options = billingPointFilterOptions(branches, null)
        assertEquals(listOf("001", "003"), options?.map { it.code })
        assertEquals("001", options?.first()?.label)
        assertEquals("003 - Kiosco", options?.last()?.label)
    }

    private fun branch(
        code: String,
        name: String,
        vararg points: FiscalBillingPoint,
        status: Int = 1,
    ) = Branch(
        branchCode = code,
        name = name,
        addressLine = "",
        locationCode = "",
        longitude = "",
        latitude = "",
        status = status,
        fiscalBillingPoints = points.toList(),
    )

    private fun point(code: String, description: String, status: Int = 1) =
        FiscalBillingPoint(billingPoint = code, description = description, status = status)
}