package com.teco.ventago.features.pos.devices.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.features.branches.domain.BranchService
import com.teco.ventago.features.branches.domain.model.Branch
import com.teco.ventago.features.pos.devices.domain.PosDevicesService
import com.teco.ventago.features.pos.devices.domain.model.UpdatePosDevicePermissionsRequest
import com.teco.ventago.features.pos.devices.domain.model.isValidPosDeviceBillingPointCode
import com.teco.ventago.features.pos.devices.domain.model.isValidPosDeviceBranchCode
import com.teco.ventago.features.pos.provisioning.domain.model.PosDevicePermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PosDevicesViewModel(
    private val devicesService: PosDevicesService,
    private val branchService: BranchService,
) : BaseViewModel<PosDevicesState, PosDevicesUiEvent>(PosDevicesState()) {

    init {
        devicesService.observe()
            .onEach { devices ->
                updateState {
                    val selectedStillExists = selectedDeviceId == null ||
                        devices.any { it.deviceId == selectedDeviceId }
                    val selectedId = selectedDeviceId.takeIf { selectedStillExists }
                    val selectedDevice = devices.firstOrNull { it.deviceId == selectedId }
                    copy(
                        devices = devices.toList(),
                        selectedDeviceId = selectedId,
                        permissionDraft = selectedDevice?.permissions ?: permissionDraft.takeIf { selectedId != null }
                    )
                }
            }
            .launchIn(viewModelScope)

        branchService.observe()
            .onEach { branches ->
                updateState { copy(branchOptions = branches.toDeviceBranchOptions()) }
            }
            .launchIn(viewModelScope)
    }

    fun loadDevices(initial: Boolean = false) {
        viewModelScope.launch {
            updateState {
                copy(
                    isLoadingInitial = initial,
                    isRefreshing = !initial,
                    loadFailed = false
                )
            }
            try {
                withContext(Dispatchers.IO) {
                    devicesService.listActiveDevices()
                }
                updateState {
                    copy(
                        isLoadingInitial = false,
                        isRefreshing = false,
                        loadFailed = false
                    )
                }
            } catch (_: Exception) {
                updateState {
                    copy(
                        isLoadingInitial = false,
                        isRefreshing = false,
                        loadFailed = true
                    )
                }
                emitEvent(PosDevicesUiEvent.Message("No pudimos cargar los dispositivos POS. Intenta nuevamente."))
            }
        }
    }

    fun selectDevice(deviceId: String) {
        updateState {
            copy(
                selectedDeviceId = deviceId,
                permissionDraft = null,
                isLoadingDeviceConfig = false,
                showBranchBillingSheet = false,
            )
        }
    }

    fun loadSelectedDeviceConfig() {
        val deviceId = uiState.value.selectedDeviceId ?: return
        viewModelScope.launch {
            updateState { copy(isLoadingDeviceConfig = true, permissionDraft = null) }
            try {
                val config = withContext(Dispatchers.IO) {
                    devicesService.loadDeviceConfig(deviceId)
                }
                updateState {
                    copy(
                        isLoadingDeviceConfig = false,
                        permissionDraft = config.permissions,
                    )
                }
            } catch (_: Exception) {
                updateState {
                    copy(
                        isLoadingDeviceConfig = false,
                        permissionDraft = null,
                    )
                }
                emitEvent(PosDevicesUiEvent.Message("No pudimos cargar la configuración del dispositivo POS."))
            }
        }
    }

    fun openBranchBillingSheet() {
        val state = uiState.value
        val device = state.selectedDevice ?: return
        if (!state.canEditBranchBilling) {
            viewModelScope.launch {
                emitEvent(PosDevicesUiEvent.Message("Solo hay una sucursal y punto de facturación disponible."))
            }
            return
        }
        updateState {
            copy(
                showBranchBillingSheet = true,
                draftBranchCode = device.branchCode,
                draftBillingPointCode = device.billingPointCode,
                branchCodeError = validateBranchCode(device.branchCode),
                billingPointCodeError = validateBillingPointCode(device.billingPointCode),
            )
        }
    }

    fun dismissBranchBillingSheet() {
        updateState {
            copy(
                showBranchBillingSheet = false,
                draftBranchCode = null,
                draftBillingPointCode = null,
                branchCodeError = null,
                billingPointCodeError = null,
            )
        }
    }

    fun selectBranch(branchCode: String) {
        val branch = uiState.value.branchOptions.firstOrNull { it.code == branchCode }
        val billingPoint = branch?.billingPoints?.firstOrNull()?.code
        updateState {
            copy(
                draftBranchCode = branchCode,
                draftBillingPointCode = billingPoint,
                branchCodeError = validateBranchCode(branchCode),
                billingPointCodeError = validateBillingPointCode(billingPoint.orEmpty()),
            )
        }
    }

    fun selectBillingPoint(billingPointCode: String) {
        updateState {
            copy(
                draftBillingPointCode = billingPointCode,
                billingPointCodeError = validateBillingPointCode(billingPointCode),
            )
        }
    }

    fun saveBranchBilling() {
        val state = uiState.value
        val device = state.selectedDevice ?: return
        val branchCode = state.draftBranchCode.orEmpty()
        val billingPointCode = state.draftBillingPointCode.orEmpty()
        val branchError = validateBranchCode(branchCode)
        val billingError = validateBillingPointCode(billingPointCode)
        if (branchError != null || billingError != null) {
            updateState {
                copy(
                    branchCodeError = branchError,
                    billingPointCodeError = billingError
                )
            }
            return
        }

        viewModelScope.launch {
            showLoading()
            try {
                withContext(Dispatchers.IO) {
                    devicesService.updateDeviceBranchBilling(device, branchCode, billingPointCode)
                }
                updateState { copy(showBranchBillingSheet = false) }
                showSuccess()
                emitEvent(PosDevicesUiEvent.Message("Dispositivo POS actualizado."))
            } catch (_: Exception) {
                showError()
                emitEvent(PosDevicesUiEvent.Message("No pudimos actualizar el dispositivo POS."))
            }
        }
    }

    fun updatePermission(key: PosDevicePermissionKey, value: Boolean) {
        val current = uiState.value.permissionDraft ?: return
        updateState {
            copy(permissionDraft = current.withPermission(key, value))
        }
    }

    fun savePermissions() {
        val state = uiState.value
        val device = state.selectedDevice ?: return
        val draft = state.permissionDraft ?: return

        viewModelScope.launch {
            showLoading()
            try {
                withContext(Dispatchers.IO) {
                    devicesService.updatePermissions(
                        deviceId = device.deviceId,
                        request = UpdatePosDevicePermissionsRequest.fromPermissions(draft)
                    )
                }
                showSuccess()
                emitEvent(PosDevicesUiEvent.Message("Permisos del dispositivo actualizados."))
            } catch (_: Exception) {
                showError()
                emitEvent(PosDevicesUiEvent.Message("No pudimos actualizar los permisos del dispositivo."))
            }
        }
    }

    private fun validateBranchCode(value: String): String? =
        if (isValidPosDeviceBranchCode(value)) null else "La sucursal debe tener 4 dígitos."

    private fun validateBillingPointCode(value: String): String? =
        if (isValidPosDeviceBillingPointCode(value)) null else "El punto debe tener 3 dígitos y no puede ser 000."

    private fun List<Branch>.toDeviceBranchOptions(): List<PosDeviceBranchOption> =
        filter { it.status == 1 && it.branchCode.isNotBlank() }
            .mapNotNull { branch ->
                val billingPoints = branch.fiscalBillingPoints
                    .filter { point -> point.status == 1 && isValidPosDeviceBillingPointCode(point.billingPoint) }
                    .map { point ->
                        PosDeviceBillingPointOption(
                            code = point.billingPoint,
                            label = point.description?.takeIf { it.isNotBlank() } ?: "Punto de facturación"
                        )
                    }
                if (billingPoints.isEmpty()) {
                    null
                } else {
                    PosDeviceBranchOption(
                        code = branch.branchCode,
                        label = branch.name.ifBlank { "Sucursal" },
                        billingPoints = billingPoints,
                    )
                }
            }
}

private fun PosDevicePermissions.withPermission(
    key: PosDevicePermissionKey,
    value: Boolean,
): PosDevicePermissions =
    when (key) {
        PosDevicePermissionKey.EXPENSES_VIEW -> copy(expensesView = value)
        PosDevicePermissionKey.EXPENSES_CREATE -> copy(expensesCreate = value)
        PosDevicePermissionKey.PRODUCTS_VIEW -> copy(productsView = value)
        PosDevicePermissionKey.PRODUCTS_CREATE -> copy(productsCreate = value)
        PosDevicePermissionKey.CLIENTS_VIEW -> copy(clientsView = value)
        PosDevicePermissionKey.CLIENTS_CREATE -> copy(clientsCreate = value)
        PosDevicePermissionKey.QUOTES_VIEW -> copy(quotesView = value)
        PosDevicePermissionKey.QUOTES_CREATE -> copy(quotesCreate = value)
        PosDevicePermissionKey.PAYMENT_METHODS_CONFIGURE -> copy(paymentMethodsConfigure = value)
        PosDevicePermissionKey.PAYMENT_YAPPY_ONSITE -> copy(paymentYappyOnsite = value)
        PosDevicePermissionKey.PAYMENT_LINK -> copy(paymentLink = value)
        PosDevicePermissionKey.PAYMENT_MANUAL_METHODS -> copy(paymentManualMethods = value)
        PosDevicePermissionKey.REPORTS_VIEW -> copy(reportsView = value)
    }
