package com.teco.ventago.features.printers.ui.viewmodel

import com.teco.ventago.core.LoadableState
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.features.printers.domain.model.PrinterConfig

data class PrinterListItem(
    val printerConfig: PrinterConfig,
    val branchLabel: String,
    val billingPointLabel: String,
)

data class PrintersState(
    val printers: List<PrinterListItem> = emptyList(),
    val pendingDelete: PrinterConfig? = null,
    override val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
) : LoadableState<PrintersState> {
    override fun withLoading(state: LoadingBottomSheetState): PrintersState = copy(
        loadingBottomSheet = state
    )
}

sealed class PrintersUiEvent {
    data class Message(val text: String) : PrintersUiEvent()
}
