package com.teco.ventago.features.printers.ui.viewmodel

import com.teco.ventago.core.LoadableState
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.features.printers.domain.model.DiscoveredPrinter
import com.teco.ventago.features.printers.domain.model.PrinterDiscoveryState
import com.teco.ventago.features.printers.domain.model.PrinterDiscoveryStatus
import com.teco.ventago.features.printers.domain.model.PrinterConfig

enum class PrinterEntryContext {
    SETTINGS,
    BRANCH,
}

enum class PrinterOnboardingStep {
    LANDING,
    SETUP,
    NETWORK,
    CONFIG,
    SUCCESS,
}

enum class PrinterConfigMode {
    AUTOMATIC,
    MANUAL,
}

data class PrinterBranchOption(
    val code: String,
    val label: String,
    val billingPoints: List<PrinterBillingPointOption>,
)

data class PrinterBillingPointOption(
    val code: String,
    val label: String,
)

data class PrinterOnboardingState(
    val step: PrinterOnboardingStep = PrinterOnboardingStep.LANDING,
    val entryContext: PrinterEntryContext = PrinterEntryContext.SETTINGS,
    val branches: List<PrinterBranchOption> = emptyList(),
    val selectedBranchCode: String? = null,
    val selectedBillingPointCode: String? = null,
    val selectionLocked: Boolean = false,
    val configMode: PrinterConfigMode = PrinterConfigMode.AUTOMATIC,
    val useInternalPrinter: Boolean = false,
    val discoveryState: PrinterDiscoveryState = PrinterDiscoveryState(),
    val selectedDiscoveredPrinterId: String? = null,
    val setupSlideIndex: Int = 0,
    val printerName: String = "TM-T20III",
    val host: String = "",
    val port: String = "443",
    val paperWidthMm: Int = 80,
    val printByDefault: Boolean = true,
    val isActive: Boolean = true,
    val supportsCutter: Boolean = true,
    val retryCount: Int = 1,
    val timeoutMs: Int = 30_000,
    val currentPrinter: PrinterConfig? = null,
    val lastTestStatus: String = "not_tested",
    val lastTestMessage: String? = null,
    val validationMessage: String? = null,
    val showSuccessOnSave: Boolean = true,
    override val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
) : LoadableState<PrinterOnboardingState> {
    override fun withLoading(state: LoadingBottomSheetState): PrinterOnboardingState = copy(
        loadingBottomSheet = state
    )

    val selectedBranch: PrinterBranchOption?
        get() = branches.firstOrNull { it.code == selectedBranchCode }

    val selectedBillingPoint: PrinterBillingPointOption?
        get() = selectedBranch?.billingPoints?.firstOrNull { it.code == selectedBillingPointCode }

    val hasValidSelection: Boolean
        get() = selectedBranch != null && selectedBillingPoint != null

    val selectedDiscoveredPrinter: DiscoveredPrinter?
        get() = discoveryState.printers.firstOrNull { it.id == selectedDiscoveredPrinterId }

    val isAutomaticMode: Boolean
        get() = configMode == PrinterConfigMode.AUTOMATIC

    val canRescanAutomatically: Boolean
        get() = isAutomaticMode && discoveryState.status != PrinterDiscoveryStatus.SCANNING

    val canSave: Boolean
        get() = hasValidSelection &&
            printerName.isNotBlank() &&
            host.isNotBlank() &&
            lastTestStatus == "success" &&
            (!isAutomaticMode || selectedDiscoveredPrinter != null)
}

sealed class PrinterOnboardingUiEvent {
    data class Message(val text: String) : PrinterOnboardingUiEvent()
    data object SavedAndClose : PrinterOnboardingUiEvent()
}
