package com.teco.ventago.features.pos.devices.ui.viewmodel

import com.teco.ventago.core.LoadableState
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.features.pos.devices.domain.model.PosDevice
import com.teco.ventago.features.pos.provisioning.domain.model.PosDevicePermissions

data class PosDeviceBillingPointOption(
    val code: String,
    val label: String,
)

data class PosDeviceBranchOption(
    val code: String,
    val label: String,
    val billingPoints: List<PosDeviceBillingPointOption>,
)

enum class PosDevicePermissionKey {
    EXPENSES_VIEW,
    EXPENSES_CREATE,
    PRODUCTS_VIEW,
    PRODUCTS_CREATE,
    CLIENTS_VIEW,
    CLIENTS_CREATE,
    QUOTES_VIEW,
    QUOTES_CREATE,
    PAYMENT_METHODS_CONFIGURE,
    PAYMENT_YAPPY_ONSITE,
    PAYMENT_LINK,
    PAYMENT_MANUAL_METHODS,
    REPORTS_VIEW,
}

data class PosDevicesState(
    val devices: List<PosDevice> = emptyList(),
    val selectedDeviceId: String? = null,
    val isLoadingInitial: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingDeviceConfig: Boolean = false,
    val loadFailed: Boolean = false,
    val branchOptions: List<PosDeviceBranchOption> = emptyList(),
    val showBranchBillingSheet: Boolean = false,
    val draftBranchCode: String? = null,
    val draftBillingPointCode: String? = null,
    val branchCodeError: String? = null,
    val billingPointCodeError: String? = null,
    val permissionDraft: PosDevicePermissions? = null,
    override val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
) : LoadableState<PosDevicesState> {
    override fun withLoading(state: LoadingBottomSheetState): PosDevicesState =
        copy(loadingBottomSheet = state)

    val selectedDevice: PosDevice?
        get() = devices.firstOrNull { it.deviceId == selectedDeviceId }

    val selectedBranch: PosDeviceBranchOption?
        get() = branchOptions.firstOrNull { it.code == draftBranchCode }

    val selectableBranchBillingCount: Int
        get() = branchOptions.sumOf { it.billingPoints.size }

    val canEditBranchBilling: Boolean
        get() = selectableBranchBillingCount > 1

    val hasValidBranchBillingDraft: Boolean
        get() = selectedBranch?.billingPoints?.any { it.code == draftBillingPointCode } == true &&
            branchCodeError == null &&
            billingPointCodeError == null

    val canEditPermissions: Boolean
        get() = selectedDevice?.permissions != null && permissionDraft != null
}

sealed class PosDevicesUiEvent {
    data class Message(val text: String) : PosDevicesUiEvent()
}
