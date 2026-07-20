package com.teco.ventago.features.payments.ui.home.viewmodel

import com.teco.ventago.features.branches.domain.model.Branch
import com.teco.ventago.features.branches.domain.model.FiscalBillingPoint
import com.teco.ventago.features.payments.domain.models.YappyOnsiteDevice
import com.teco.ventago.features.payments.domain.models.YappyOnsiteGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class YappyOnsiteOnboardingPolicyTest {

    @Test
    fun groupLimitCountsSavedGroupsAndDraftsAgainstBranches() {
        val branches = listOf(branch("001"), branch("002"))
        val saved = listOf(group("G-1", "001"))
        val drafts = listOf(
            groupDraft(localId = 1, groupId = "G-2", branchCode = "002"),
            groupDraft(localId = 2, groupId = "G-3", branchCode = "003"),
        )

        assertTrue(YappyOnsiteOnboardingPolicy.exceedsGroupLimit(saved, drafts, branches))
    }

    @Test
    fun groupValidationRejectsDuplicateBranchesAndGroupIds() {
        val saved = listOf(group("G-1", "001"))

        assertTrue(
            YappyOnsiteOnboardingPolicy.hasDuplicateGroupBranches(
                savedGroups = saved,
                drafts = listOf(groupDraft(localId = 1, groupId = "G-2", branchCode = "001")),
            )
        )
        assertTrue(
            YappyOnsiteOnboardingPolicy.hasDuplicateGroupIds(
                savedGroups = saved,
                drafts = listOf(groupDraft(localId = 1, groupId = "g-1", branchCode = "002")),
            )
        )
        assertFalse(
            YappyOnsiteOnboardingPolicy.hasDuplicateGroupBranches(
                savedGroups = saved,
                drafts = listOf(groupDraft(localId = 1, groupId = "G-2", branchCode = "002")),
            )
        )
    }

    @Test
    fun groupNameIsDerivedFromSelectedBranch() {
        val branches = listOf(branch("001", name = "Casa Matriz"))

        assertEquals(
            "Casa Matriz",
            YappyOnsiteOnboardingPolicy.groupNameForBranch("001", branches),
        )
        assertEquals(
            "Sucursal 999",
            YappyOnsiteOnboardingPolicy.groupNameForBranch("999", branches),
        )
    }

    @Test
    fun deviceLimitCountsSavedDevicesAndDraftsAgainstBillingPoints() {
        val branches = listOf(
            branch("001", points = listOf(point("001"), point("002"))),
        )
        val saved = listOf(device("G-1", "D-1", "001", "001"))
        val drafts = listOf(
            deviceDraft(localId = 1, groupId = "G-1", branchCode = "001", billingPoint = "002", deviceId = "D-2"),
            deviceDraft(localId = 2, groupId = "G-1", branchCode = "001", billingPoint = "003", deviceId = "D-3"),
        )

        assertTrue(YappyOnsiteOnboardingPolicy.exceedsDeviceLimit(saved, drafts, branches))
    }

    @Test
    fun deviceValidationRejectsDuplicateDeviceIdsAndBillingPointsPerGroup() {
        val saved = listOf(device("G-1", "D-1", "001", "001"))

        assertTrue(
            YappyOnsiteOnboardingPolicy.hasDuplicateDeviceIds(
                savedDevices = saved,
                drafts = listOf(deviceDraft(localId = 1, groupId = "g-1", deviceId = "d-1")),
            )
        )
        assertTrue(
            YappyOnsiteOnboardingPolicy.hasDuplicateDeviceBillingPoints(
                savedDevices = saved,
                drafts = listOf(deviceDraft(localId = 1, groupId = "G-1", billingPoint = "001")),
            )
        )
        assertFalse(
            YappyOnsiteOnboardingPolicy.hasDuplicateDeviceBillingPoints(
                savedDevices = saved,
                drafts = listOf(deviceDraft(localId = 1, groupId = "G-1", billingPoint = "002")),
            )
        )
    }

    @Test
    fun deviceNameIsDerivedFromBillingPointDescription() {
        val branches = listOf(
            branch(
                code = "001",
                points = listOf(point("001", "Caja principal")),
            )
        )

        assertEquals(
            "Caja principal",
            YappyOnsiteOnboardingPolicy.deviceNameForBillingPoint("001", "001", branches),
        )
        assertEquals(
            "Punto 999",
            YappyOnsiteOnboardingPolicy.deviceNameForBillingPoint("001", "999", branches),
        )
    }

    private fun branch(
        code: String,
        name: String = "Sucursal $code",
        points: List<FiscalBillingPoint> = listOf(point("001")),
    ): Branch {
        return Branch(
            branchCode = code,
            name = name,
            addressLine = "",
            locationCode = "",
            longitude = "",
            latitude = "",
            status = 1,
            fiscalBillingPoints = points,
        )
    }

    private fun point(code: String, description: String? = "Punto $code"): FiscalBillingPoint {
        return FiscalBillingPoint(
            billingPoint = code,
            description = description,
            status = 1,
        )
    }

    private fun group(groupId: String, branchCode: String): YappyOnsiteGroup {
        return YappyOnsiteGroup(
            groupId = groupId,
            branchCode = branchCode,
        )
    }

    private fun device(
        groupId: String,
        deviceId: String,
        branchCode: String,
        billingPoint: String,
    ): YappyOnsiteDevice {
        return YappyOnsiteDevice(
            groupId = groupId,
            deviceId = deviceId,
            branchCode = branchCode,
            billingPoint = billingPoint,
        )
    }

    private fun groupDraft(
        localId: Int,
        groupId: String,
        branchCode: String,
    ): YappyOnsiteGroupDraftState {
        return YappyOnsiteGroupDraftState(
            localId = localId,
            groupId = groupId,
            branchCode = branchCode,
            apiKey = "api",
            secretKey = "secret",
        )
    }

    private fun deviceDraft(
        localId: Int,
        groupId: String,
        branchCode: String = "001",
        billingPoint: String = "001",
        deviceId: String = "D-1",
    ): YappyOnsiteDeviceDraftState {
        return YappyOnsiteDeviceDraftState(
            localId = localId,
            groupId = groupId,
            branchCode = branchCode,
            billingPoint = billingPoint,
            deviceId = deviceId,
        )
    }
}
