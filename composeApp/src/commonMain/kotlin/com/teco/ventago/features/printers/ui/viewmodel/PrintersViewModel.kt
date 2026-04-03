package com.teco.ventago.features.printers.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.design_system.organism.LoadingState
import com.teco.ventago.features.branches.domain.BranchService
import com.teco.ventago.features.printers.domain.PrinterService
import com.teco.ventago.features.printers.domain.model.PrinterConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class PrintersViewModel(
    private val printerService: PrinterService,
    private val branchService: BranchService,
    private val logger: ILoggerService,
) : BaseViewModel<PrintersState, PrintersUiEvent>(PrintersState()) {

    init {
        viewModelScope.launch {
            branchService.observe()
                .combine(printerService.observe()) { branches, printers ->
                    val branchMap = branches.associateBy { it.branchCode }
                    printers.sortedBy { "${it.branchCode}-${it.billingPointCode}" }
                        .map { printer ->
                            val branch = branchMap[printer.branchCode]
                            val billingPoint = branch?.fiscalBillingPoints?.firstOrNull {
                                it.billingPoint == printer.billingPointCode
                            }
                            PrinterListItem(
                                printerConfig = printer,
                                branchLabel = branch?.name ?: printer.branchCode,
                                billingPointLabel = billingPoint?.description ?: printer.billingPointCode
                            )
                        }
                }
                .collect { items ->
                    updateState { copy(printers = items) }
                }
        }
    }

    fun promptDelete(printerConfig: PrinterConfig) {
        updateState { copy(pendingDelete = printerConfig) }
    }

    fun dismissDeletePrompt() {
        updateState { copy(pendingDelete = null) }
    }

    fun deletePendingPrinter() {
        val pending = uiState.value.pendingDelete ?: return
        updateState {
            copy(
                loadingBottomSheet = LoadingBottomSheetState(
                    LoadingState.LOADING,
                    "Eliminando impresora..."
                )
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                printerService.deletePrinter(
                    branchCode = pending.branchCode,
                    billingPointCode = pending.billingPointCode
                )
            }.onSuccess {
                updateState {
                    copy(
                        pendingDelete = null,
                        loadingBottomSheet = LoadingBottomSheetState(
                            LoadingState.SUCCESS,
                            "Impresora eliminada"
                        )
                    )
                }
            }.onFailure { throwable ->
                logger.sendLog(
                    Log(
                        LogLevel.ERROR,
                        "PrintersViewModel::deletePendingPrinter",
                        throwable.message ?: "UNKNOWN"
                    )
                )
                updateState {
                    copy(
                        loadingBottomSheet = LoadingBottomSheetState(
                            LoadingState.ERROR,
                            "No se pudo eliminar"
                        )
                    )
                }
                emitMessage(throwable.message ?: "No se pudo eliminar la impresora.")
            }
        }
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch {
            emitEvent(PrintersUiEvent.Message(message))
        }
    }
}
