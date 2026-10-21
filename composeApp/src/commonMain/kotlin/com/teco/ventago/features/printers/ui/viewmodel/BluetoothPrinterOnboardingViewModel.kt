package com.teco.ventago.features.printers.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.core.LoadableState
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.features.printers.domain.NearbyBluetoothPrinter
import com.teco.ventago.features.printers.domain.PrinterService
import com.teco.ventago.features.printers.domain.listBluetoothPrinters
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class BluetoothPrinterOnboardingState(
    val loading: Boolean = true,
    val bluetoothEnabled: Boolean = true,
    val permissionDenied: Boolean = false,
    val printers: List<NearbyBluetoothPrinter> = emptyList(),
    val message: String? = null,
    val linkedId: String? = null,
    val linking: Boolean = false,
    override val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
) : LoadableState<BluetoothPrinterOnboardingState> {
    override fun withLoading(state: LoadingBottomSheetState): BluetoothPrinterOnboardingState {
        return copy(loadingBottomSheet = state)
    }
}

sealed class BluetoothPrinterOnboardingUiEvent {
    data class Linked(val name: String) : BluetoothPrinterOnboardingUiEvent()
}

class BluetoothPrinterOnboardingViewModel(
    private val printerService: PrinterService,
) : BaseViewModel<BluetoothPrinterOnboardingState, BluetoothPrinterOnboardingUiEvent>(
    BluetoothPrinterOnboardingState(linkedId = printerService.linkedBluetoothPrinter()?.id)
) {
    private var refreshJob: Job? = null

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            updateState {
                copy(
                    loading = printers.isEmpty(),
                    permissionDenied = false,
                    message = null,
                )
            }
            val availability = listBluetoothPrinters()
            updateState {
                copy(
                    loading = false,
                    bluetoothEnabled = availability.enabled,
                    permissionDenied = availability.needsPermission,
                    printers = availability.printers,
                    message = availability.message,
                    linkedId = printerService.linkedBluetoothPrinter()?.id,
                )
            }
        }
    }

    fun onPermissionDenied() {
        refreshJob?.cancel()
        updateState {
            copy(
                loading = false,
                bluetoothEnabled = false,
                permissionDenied = true,
                printers = emptyList(),
                message = "VentaGO necesita permiso de Bluetooth.",
            )
        }
    }

    fun link(printer: NearbyBluetoothPrinter) {
        if (uiState.value.linking) return
        updateState { copy(linking = true, linkedId = printer.id) }
        viewModelScope.launch {
            printerService.saveDeviceBluetoothPrinter(id = printer.id, name = printer.name)
            emitEvent(BluetoothPrinterOnboardingUiEvent.Linked(printer.name))
        }
    }
}
