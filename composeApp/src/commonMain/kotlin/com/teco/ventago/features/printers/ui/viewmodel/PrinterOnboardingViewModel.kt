package com.teco.ventago.features.printers.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.AppDistribution
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.core.firebase.AnalyticsService
import com.teco.ventago.core.logger.ILoggerService
import com.teco.ventago.core.logger.Log
import com.teco.ventago.core.logger.LogLevel
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.design_system.organism.LoadingState
import com.teco.ventago.features.branches.domain.BranchService
import com.teco.ventago.features.printers.domain.PrinterDiscoveryService
import com.teco.ventago.features.printers.domain.PrinterService
import com.teco.ventago.features.printers.domain.model.CreatePrinterRequest
import com.teco.ventago.features.printers.domain.model.PRINTER_INTEGRATION_EPSON_EPOS
import com.teco.ventago.features.printers.domain.model.PRINTER_INTEGRATION_H10P_INTERNAL
import com.teco.ventago.features.printers.domain.model.PrinterDiscoveryStatus
import com.teco.ventago.features.printers.domain.model.PrinterConfig
import com.teco.ventago.features.printers.domain.model.UpdatePrinterRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class PrinterOnboardingViewModel(
    private val printerService: PrinterService,
    private val discoveryService: PrinterDiscoveryService,
    private val branchService: BranchService,
    private val logger: ILoggerService,
    private val analyticsService: AnalyticsService,
    private val appDistribution: AppDistribution,
    entryContext: String = PrinterEntryContext.SETTINGS.name,
    private val preselectedBranchCode: String? = null,
    private val preselectedBillingPointCode: String? = null,
    startAtConfig: Boolean = false,
) : BaseViewModel<PrinterOnboardingState, PrinterOnboardingUiEvent>(
    PrinterOnboardingState(
        step = if (startAtConfig) PrinterOnboardingStep.CONFIG else PrinterOnboardingStep.LANDING,
        entryContext = entryContext.toPrinterEntryContextValue(),
        selectionLocked = entryContext.toPrinterEntryContextValue() == PrinterEntryContext.BRANCH,
        showSuccessOnSave = !startAtConfig,
        configMode = if (appDistribution.isPosBuild) PrinterConfigMode.MANUAL else PrinterConfigMode.AUTOMATIC,
        useInternalPrinter = appDistribution.isPosBuild,
        printerName = if (appDistribution.isPosBuild) "H10P Internal" else "TM-T20III",
        port = if (appDistribution.isPosBuild) "0" else "443",
        paperWidthMm = if (appDistribution.isPosBuild) 57 else 80,
        supportsCutter = !appDistribution.isPosBuild,
        timeoutMs = if (appDistribution.isPosBuild) 10_000 else 30_000,
    )
) {
    private var formSeeded = false
    private var lastSuccessfulTestFingerprint: String? = null
    private var discoveryAutoStopJob: Job? = null

    init {
        viewModelScope.launch {
            branchService.observe()
                .combine(printerService.observe()) { branches, printers ->
                    Pair(branches, printers)
                }
                .collect { (branches, printers) ->
                    val branchOptions = branches
                        .filter { it.status == 1 }
                        .mapNotNull { branch ->
                            val billingPoints = branch.fiscalBillingPoints
                                .filter { it.status == 1 }
                                .map {
                                    PrinterBillingPointOption(
                                        code = it.billingPoint,
                                        label = "${it.description ?: "Punto"} (${it.billingPoint})"
                                    )
                                }
                            if (billingPoints.isEmpty()) {
                                null
                            } else {
                                PrinterBranchOption(
                                    code = branch.branchCode,
                                    label = "${branch.name} (${branch.branchCode})",
                                    billingPoints = billingPoints
                                )
                            }
                        }

                    updateState {
                        copy(
                            branches = branchOptions
                        )
                    }

                    val selectedBranchCode = uiState.value.selectedBranchCode
                    val selectedBillingPointCode = uiState.value.selectedBillingPointCode
                    val validBranch = branchOptions.firstOrNull { it.code == selectedBranchCode }
                    val validBilling = validBranch?.billingPoints?.firstOrNull { it.code == selectedBillingPointCode }

                    if (validBranch == null || validBilling == null) {
                        autoResolveSelection(branchOptions)
                    }

                    if (!formSeeded) {
                        applyPrinterForCurrentSelection(printers)
                        formSeeded = true
                    }
                }
        }

        viewModelScope.launch {
            discoveryService.observe().collect { discoveryState ->
                val resolvedSelection = uiState.value.selectedDiscoveredPrinterId?.let { selectedId ->
                    discoveryState.printers.firstOrNull { it.id == selectedId }?.id
                }
                updateState {
                    copy(
                        discoveryState = discoveryState,
                        selectedDiscoveredPrinterId = resolvedSelection,
                    )
                }
            }
        }

        if (startAtConfig && !appDistribution.isPosBuild) {
            startAutoDiscoveryIfNeeded()
        }
    }

    fun startSetupFlow() {
        if (appDistribution.isPosBuild) {
            updateState { copy(step = PrinterOnboardingStep.CONFIG, validationMessage = null) }
            return
        }
        updateState {
            copy(step = PrinterOnboardingStep.SETUP, validationMessage = null)
        }
    }

    fun nextSetupSlide() {
        val current = uiState.value.setupSlideIndex
        if (current >= SETUP_SLIDES.lastIndex) {
            updateState { copy(step = PrinterOnboardingStep.NETWORK) }
            return
        }
        updateState { copy(setupSlideIndex = current + 1) }
    }

    fun previousSetupSlide() {
        val current = uiState.value.setupSlideIndex
        if (current <= 0) {
            updateState { copy(step = PrinterOnboardingStep.LANDING) }
            return
        }
        updateState { copy(setupSlideIndex = current - 1) }
    }

    fun goBackFromNetwork() {
        stopAutoDiscovery(markCompleted = false)
        updateState { copy(step = PrinterOnboardingStep.SETUP) }
    }

    fun goBackFromConfig() {
        stopAutoDiscovery(markCompleted = false)
        updateState {
            copy(
                step = if (showSuccessOnSave) PrinterOnboardingStep.NETWORK else PrinterOnboardingStep.CONFIG,
                validationMessage = null
            )
        }
    }

    fun continueToConfig() {
        updateState { copy(step = PrinterOnboardingStep.CONFIG) }
        if (!appDistribution.isPosBuild) {
            startAutoDiscoveryIfNeeded()
        }
    }

    fun setConfigMode(mode: PrinterConfigMode) {
        if (appDistribution.isPosBuild) return
        if (mode == uiState.value.configMode) return
        updateState {
            copy(
                configMode = mode,
                validationMessage = null,
                selectedDiscoveredPrinterId = if (mode == PrinterConfigMode.MANUAL) null else selectedDiscoveredPrinterId,
            )
        }
        if (mode == PrinterConfigMode.AUTOMATIC) {
            startAutoDiscoveryIfNeeded()
        } else {
            stopAutoDiscovery(markCompleted = false)
        }
    }

    fun rescanPrinters() {
        startAutoDiscoveryIfNeeded(force = true)
    }

    fun selectDiscoveredPrinter(printerId: String) {
        val discovered = uiState.value.discoveryState.printers.firstOrNull { it.id == printerId } ?: return
        val nextModel = discovered.displayName.takeIf { it.isNotBlank() } ?: uiState.value.printerName
        lastSuccessfulTestFingerprint = null
        updateState {
            copy(
                selectedDiscoveredPrinterId = printerId,
                printerName = nextModel,
                host = discovered.host,
                port = discovered.port.toString(),
                lastTestStatus = "not_tested",
                lastTestMessage = null,
                validationMessage = null,
            )
        }
    }

    fun selectBranch(branchCode: String) {
        val branch = uiState.value.branches.firstOrNull { it.code == branchCode } ?: return
        val nextBilling = branch.billingPoints.firstOrNull()?.code
        updateState {
            copy(
                selectedBranchCode = branch.code,
                selectedBillingPointCode = nextBilling,
                validationMessage = null,
                lastTestMessage = null
            )
        }
        applyPrinterForCurrentSelection()
    }

    fun selectBillingPoint(billingPointCode: String) {
        updateState {
            copy(
                selectedBillingPointCode = billingPointCode,
                validationMessage = null,
                lastTestMessage = null
            )
        }
        applyPrinterForCurrentSelection()
    }

    fun onPrinterNameChange(value: String) {
        lastSuccessfulTestFingerprint = null
        updateState {
            copy(
                printerName = value,
                validationMessage = null,
                lastTestMessage = null,
                lastTestStatus = "not_tested",
            )
        }
    }

    fun onHostChange(value: String) {
        lastSuccessfulTestFingerprint = null
        updateState {
            copy(
                host = value,
                validationMessage = null,
                lastTestMessage = null,
                lastTestStatus = "not_tested",
            )
        }
    }

    fun onPortChange(value: String) {
        lastSuccessfulTestFingerprint = null
        updateState {
            copy(
                port = value.filter { it.isDigit() },
                validationMessage = null,
                lastTestMessage = null,
                lastTestStatus = "not_tested",
            )
        }
    }

    fun onPrintByDefaultChange(value: Boolean) {
        updateState { copy(printByDefault = value) }
    }

    fun onIsActiveChange(value: Boolean) {
        updateState { copy(isActive = value) }
    }

    fun onPaperWidthChange(value: Int) {
        lastSuccessfulTestFingerprint = null
        updateState {
            copy(
                paperWidthMm = value,
                validationMessage = null,
                lastTestMessage = null,
                lastTestStatus = "not_tested",
            )
        }
    }

    fun configureAnotherPrinter() {
        formSeeded = true
        lastSuccessfulTestFingerprint = null
        val branchOptions = uiState.value.branches
        val fallbackBranch = branchOptions.firstOrNull()
        updateState {
            copy(
                step = PrinterOnboardingStep.CONFIG,
                configMode = PrinterConfigMode.AUTOMATIC,
                selectedBranchCode = fallbackBranch?.code,
                selectedBillingPointCode = fallbackBranch?.billingPoints?.firstOrNull()?.code,
                selectionLocked = false,
                selectedDiscoveredPrinterId = null,
                currentPrinter = null,
                printerName = "TM-T20III",
                host = "",
                port = "443",
                paperWidthMm = 80,
                printByDefault = true,
                isActive = true,
                supportsCutter = true,
                retryCount = 1,
                timeoutMs = 30_000,
                lastTestStatus = "not_tested",
                lastTestMessage = null,
                validationMessage = null,
                showSuccessOnSave = true,
            )
        }
        startAutoDiscoveryIfNeeded(force = true)
    }

    fun testPrint() {
        val draft = buildDraftPrinterConfig()?.copy(supportsCutter = true) ?: return
        analyticsService.logPrinterConfigAttempted(
            source = analyticsSource(),
            step = analyticsStep(),
            mode = "test"
        )
        updateState {
            copy(
                loadingBottomSheet = LoadingBottomSheetState(
                    LoadingState.LOADING,
                    "Probando impresión..."
                ),
                validationMessage = null,
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                printerService.testPrint(draft)
            }.onSuccess {
                analyticsService.logPrinterConfigSucceeded(
                    source = analyticsSource(),
                    step = analyticsStep(),
                    mode = "test"
                )
                lastSuccessfulTestFingerprint = fingerprint(draft)
                updateState {
                    copy(
                        lastTestStatus = "success",
                        lastTestMessage = "Prueba exitosa",
                        loadingBottomSheet = LoadingBottomSheetState(
                            LoadingState.SUCCESS,
                            "Prueba exitosa"
                        )
                    )
                }
            }.onFailure { throwable ->
                analyticsService.logPrinterConfigFailed(
                    source = analyticsSource(),
                    step = analyticsStep(),
                    mode = "test",
                    errorCode = analyticsService.extractErrorCode(throwable)
                )
                logger.sendLog(
                    Log(
                        LogLevel.ERROR,
                        "PrinterOnboardingViewModel::testPrint",
                        throwable.message ?: "UNKNOWN"
                    )
                )
                updateState {
                    copy(
                        lastTestStatus = "failed",
                        lastTestMessage = null,
                        loadingBottomSheet = LoadingBottomSheetState(
                            LoadingState.ERROR,
                            "No se pudo imprimir"
                        )
                    )
                }
                emitMessage(throwable.message ?: "No se pudo completar la prueba de impresión.")
            }
        }
    }

    fun savePrinter() {
        val draft = buildDraftPrinterConfig() ?: return
        val requiresRetest = draft.requiresRetestComparedTo(uiState.value.currentPrinter)
        if (requiresRetest && lastSuccessfulTestFingerprint != fingerprint(draft)) {
            updateState {
                copy(validationMessage = "Debes hacer una prueba de impresión exitosa antes de guardar.")
            }
            return
        }

        analyticsService.logPrinterConfigAttempted(
            source = analyticsSource(),
            step = analyticsStep(),
            mode = "save"
        )
        updateState {
            copy(
                loadingBottomSheet = LoadingBottomSheetState(
                    LoadingState.LOADING,
                    "Guardando impresora..."
                ),
                validationMessage = null
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val currentPrinter = uiState.value.currentPrinter
                if (currentPrinter == null) {
                    printerService.createPrinter(
                        CreatePrinterRequest(
                            branchCode = draft.branchCode,
                            billingPointCode = draft.billingPointCode,
                            printerModel = draft.printerModel,
                            host = draft.host,
                            port = draft.port,
                            deviceId = draft.deviceId,
                            paperWidthMm = draft.paperWidthMm,
                            supportsCutter = draft.supportsCutter,
                            timeoutMs = draft.timeoutMs,
                            retryCount = draft.retryCount,
                            printByDefault = draft.printByDefault,
                            isActive = draft.isActive,
                            lastTestStatus = draft.lastTestStatus,
                        )
                    )
                } else {
                    printerService.updatePrinter(
                        branchCode = currentPrinter.branchCode,
                        billingPointCode = currentPrinter.billingPointCode,
                        request = UpdatePrinterRequest(
                            printerModel = draft.printerModel,
                            integrationType = draft.integrationType,
                            host = draft.host,
                            port = draft.port,
                            deviceId = draft.deviceId,
                            paperWidthMm = draft.paperWidthMm,
                            supportsCutter = draft.supportsCutter,
                            timeoutMs = draft.timeoutMs,
                            retryCount = draft.retryCount,
                            printByDefault = draft.printByDefault,
                            isActive = draft.isActive,
                            lastTestStatus = draft.lastTestStatus,
                        )
                    )
                }
            }.onSuccess { savedPrinter ->
                analyticsService.logPrinterConfigSucceeded(
                    source = analyticsSource(),
                    step = analyticsStep(),
                    mode = "save"
                )
                stopAutoDiscovery(markCompleted = false)
                lastSuccessfulTestFingerprint = fingerprint(savedPrinter)
                updateState {
                    copy(
                        currentPrinter = savedPrinter,
                        printerName = savedPrinter.printerModel,
                        host = savedPrinter.host,
                        port = savedPrinter.port.toString(),
                        paperWidthMm = savedPrinter.paperWidthMm,
                        printByDefault = savedPrinter.printByDefault,
                        isActive = savedPrinter.isActive,
                        supportsCutter = savedPrinter.supportsCutter,
                        retryCount = savedPrinter.retryCount,
                        timeoutMs = savedPrinter.timeoutMs,
                        lastTestStatus = savedPrinter.lastTestStatus,
                        lastTestMessage = if (savedPrinter.lastTestStatus == "success") {
                            "Prueba exitosa"
                        } else {
                            lastTestMessage
                        },
                        step = if (showSuccessOnSave) PrinterOnboardingStep.SUCCESS else PrinterOnboardingStep.CONFIG,
                        loadingBottomSheet = LoadingBottomSheetState(
                            LoadingState.SUCCESS,
                            "Impresora guardada"
                        ),
                    )
                }
                if (!uiState.value.showSuccessOnSave) {
                    emitSavedAndClose()
                }
            }.onFailure { throwable ->
                analyticsService.logPrinterConfigFailed(
                    source = analyticsSource(),
                    step = analyticsStep(),
                    mode = "save",
                    errorCode = analyticsService.extractErrorCode(throwable)
                )
                logger.sendLog(
                    Log(
                        LogLevel.ERROR,
                        "PrinterOnboardingViewModel::savePrinter",
                        throwable.message ?: "UNKNOWN"
                    )
                )
                updateState {
                    copy(
                        loadingBottomSheet = LoadingBottomSheetState(
                            LoadingState.ERROR,
                            "No se pudo guardar"
                        )
                    )
                }
                emitMessage(throwable.message ?: "No se pudo guardar la configuración de la impresora.")
            }
        }
    }

    private fun autoResolveSelection(branchOptions: List<PrinterBranchOption>) {
        val preferredBranch = branchOptions.firstOrNull { branch ->
            branch.code == preselectedBranchCode &&
                branch.billingPoints.any { it.code == preselectedBillingPointCode }
        } ?: branchOptions.firstOrNull()

        updateState {
            copy(
                selectedBranchCode = preferredBranch?.code,
                selectedBillingPointCode = when {
                    preferredBranch == null -> null
                    preferredBranch.code == preselectedBranchCode &&
                        preferredBranch.billingPoints.any { it.code == preselectedBillingPointCode } -> preselectedBillingPointCode
                    else -> preferredBranch.billingPoints.firstOrNull()?.code
                }
            )
        }
    }

    private fun applyPrinterForCurrentSelection(printers: List<PrinterConfig> = printerService.getCachedPrinters()) {
        val state = uiState.value
        val branchCode = state.selectedBranchCode ?: return
        val billingPointCode = state.selectedBillingPointCode ?: return
        val existingPrinter = printers.firstOrNull {
            it.branchCode == branchCode && it.billingPointCode == billingPointCode
        }
        val selectedDiscovered = state.selectedDiscoveredPrinter
        val shouldKeepDiscoveredEndpoint =
            state.configMode == PrinterConfigMode.AUTOMATIC &&
                existingPrinter == null &&
                selectedDiscovered != null

        if (existingPrinter != null) {
            lastSuccessfulTestFingerprint = fingerprint(existingPrinter)
        }

        updateState {
            copy(
                currentPrinter = existingPrinter,
                printerName = when {
                    existingPrinter != null -> existingPrinter.printerModel
                    shouldKeepDiscoveredEndpoint -> selectedDiscovered?.displayName?.takeIf { it.isNotBlank() } ?: printerName
                    appDistribution.isPosBuild -> "H10P Internal"
                    else -> "TM-T20III"
                },
                host = when {
                    existingPrinter != null -> existingPrinter.host
                    shouldKeepDiscoveredEndpoint -> selectedDiscovered?.host.orEmpty()
                    else -> ""
                },
                port = when {
                    existingPrinter != null -> existingPrinter.port.toString()
                    shouldKeepDiscoveredEndpoint -> selectedDiscovered?.port?.toString() ?: "443"
                    appDistribution.isPosBuild -> "0"
                    else -> "443"
                },
                paperWidthMm = existingPrinter?.paperWidthMm ?: if (appDistribution.isPosBuild) 57 else 80,
                printByDefault = existingPrinter?.printByDefault ?: true,
                isActive = existingPrinter?.isActive ?: true,
                supportsCutter = existingPrinter?.supportsCutter ?: !appDistribution.isPosBuild,
                retryCount = existingPrinter?.retryCount ?: 1,
                timeoutMs = existingPrinter?.timeoutMs ?: if (appDistribution.isPosBuild) 10_000 else 30_000,
                lastTestStatus = existingPrinter?.lastTestStatus ?: "not_tested",
                lastTestMessage = if (existingPrinter?.lastTestStatus == "success") "Prueba exitosa" else null,
                validationMessage = null,
            )
        }
    }

    private fun buildDraftPrinterConfig(): PrinterConfig? {
        val state = uiState.value
        val isInternalPrinter = state.useInternalPrinter
        if (!isInternalPrinter && state.configMode == PrinterConfigMode.AUTOMATIC && state.selectedDiscoveredPrinter == null) {
            updateState {
                copy(validationMessage = "Selecciona una impresora descubierta para continuar.")
            }
            return null
        }
        val branchCode = state.selectedBranchCode
        val billingPointCode = state.selectedBillingPointCode
        if (branchCode.isNullOrBlank() || billingPointCode.isNullOrBlank()) {
            updateState {
                copy(
                    validationMessage = "Selecciona una sucursal con un punto de facturación activo para continuar con la configuración."
                )
            }
            return null
        }
        if (state.printerName.isBlank()) {
            updateState { copy(validationMessage = "Ingresa el nombre de la impresora.") }
            return null
        }
        if (!isInternalPrinter && state.host.isBlank()) {
            updateState { copy(validationMessage = "Ingresa la IP exacta de la impresora.") }
            return null
        }

        val integrationType = if (isInternalPrinter) {
            PRINTER_INTEGRATION_H10P_INTERNAL
        } else {
            state.currentPrinter?.integrationType ?: PRINTER_INTEGRATION_EPSON_EPOS
        }
        val port = if (isInternalPrinter) 0 else state.port.toIntOrNull() ?: 443
        val host = if (isInternalPrinter) "" else state.host.trim()
        return (state.currentPrinter ?: PrinterConfig(
            branchCode = branchCode,
            billingPointCode = billingPointCode,
            printerBrand = if (isInternalPrinter) "h10p" else "epson",
            printerModel = state.printerName.trim(),
            integrationType = integrationType,
            host = host,
        )).copy(
            branchCode = branchCode,
            billingPointCode = billingPointCode,
            printerBrand = if (isInternalPrinter) "h10p" else "epson",
            printerModel = state.printerName.trim(),
            integrationType = integrationType,
            host = host,
            port = port,
            paperWidthMm = state.paperWidthMm,
            printByDefault = state.printByDefault,
            isActive = state.isActive,
            supportsCutter = if (isInternalPrinter) false else state.supportsCutter,
            retryCount = state.retryCount,
            timeoutMs = state.timeoutMs,
            lastTestStatus = if (lastSuccessfulTestFingerprint == fingerprintForValues(
                    branchCode = branchCode,
                    billingPointCode = billingPointCode,
                    printerModel = state.printerName.trim(),
                    host = host,
                    port = port,
                    integrationType = integrationType,
                )
            ) {
                "success"
            } else {
                state.lastTestStatus
            }
        )
    }

    private fun fingerprint(printerConfig: PrinterConfig): String = fingerprintForValues(
        branchCode = printerConfig.branchCode,
        billingPointCode = printerConfig.billingPointCode,
        printerModel = printerConfig.printerModel,
        host = printerConfig.host,
        port = printerConfig.port,
        integrationType = printerConfig.integrationType,
    )

    private fun fingerprintForValues(
        branchCode: String,
        billingPointCode: String,
        printerModel: String,
        host: String,
        port: Int,
        integrationType: String,
    ): String {
        return listOf(
            branchCode.trim(),
            billingPointCode.trim(),
            printerModel.trim(),
            host.trim(),
            port.toString(),
            integrationType.trim()
        ).joinToString("|")
    }

    private fun startAutoDiscoveryIfNeeded(force: Boolean = false) {
        val state = uiState.value
        if (state.useInternalPrinter) return
        if (state.step != PrinterOnboardingStep.CONFIG) return
        if (state.configMode != PrinterConfigMode.AUTOMATIC) return
        if (!force && state.discoveryState.status == PrinterDiscoveryStatus.SCANNING) return

        discoveryAutoStopJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            discoveryService.start(resetResults = true)
            discoveryAutoStopJob = viewModelScope.launch {
                delay(AUTO_DISCOVERY_WINDOW_MS)
                stopAutoDiscovery(markCompleted = true)
            }
        }
    }

    private fun stopAutoDiscovery(markCompleted: Boolean) {
        discoveryAutoStopJob?.cancel()
        discoveryAutoStopJob = null
        viewModelScope.launch(Dispatchers.IO) {
            discoveryService.stop(markCompleted = markCompleted)
        }
    }

    override fun onCleared() {
        discoveryAutoStopJob?.cancel()
        runBlocking(Dispatchers.IO) {
            discoveryService.clear()
        }
        super.onCleared()
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch {
            emitEvent(PrinterOnboardingUiEvent.Message(message))
        }
    }

    private fun emitSavedAndClose() {
        viewModelScope.launch {
            emitEvent(PrinterOnboardingUiEvent.SavedAndClose)
        }
    }

    private fun analyticsSource(): String = uiState.value.entryContext.name.lowercase()

    private fun analyticsStep(): String = when (uiState.value.step) {
        PrinterOnboardingStep.LANDING -> "landing"
        PrinterOnboardingStep.SETUP -> "setup"
        PrinterOnboardingStep.NETWORK -> "network"
        PrinterOnboardingStep.CONFIG -> "config"
        PrinterOnboardingStep.SUCCESS -> "success"
    }

    companion object {
        private const val AUTO_DISCOVERY_WINDOW_MS = 8_000L

        val SETUP_SLIDES = listOf(
            "https://ventago.b-cdn.net/app/printer/image_no_bg.png",
            "https://ventago.b-cdn.net/app/printer/image2_no_bg.png",
            "https://ventago.b-cdn.net/app/printer/image3_no_bg.png",
            "https://ventago.b-cdn.net/app/printer/image4_no_bg.png",
        )
    }
}

private fun String.toPrinterEntryContextValue(): PrinterEntryContext {
    return runCatching { PrinterEntryContext.valueOf(this.uppercase()) }
        .getOrDefault(PrinterEntryContext.SETTINGS)
}
