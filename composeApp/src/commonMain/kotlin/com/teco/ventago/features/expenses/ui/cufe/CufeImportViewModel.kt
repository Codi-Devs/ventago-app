package com.teco.ventago.features.expenses.ui.cufe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.beta.BetaFeature
import com.teco.ventago.core.beta.BetaService
import com.teco.ventago.features.expenses.domain.ExpensesService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CufeImportViewModel(
    private val expensesService: ExpensesService,
    private val betaService: BetaService
) : ViewModel() {
    companion object {
        private const val POLL_INTERVAL_MS = 5_000L
        private const val MAX_POLL_ATTEMPTS = 12 // 1 minute max
    }

    private val _uiState = MutableStateFlow(CufeImportState())
    val uiState: StateFlow<CufeImportState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var pollingJobId: Long? = null
    private var isScreenVisible: Boolean = false

    init {
        viewModelScope.launch {
            betaService.accessFlow(BetaFeature.EXPENSES_QR)
                .onEach { hasAccess ->
                    _uiState.value = _uiState.value.copy(hasExpensesQr = hasAccess)
                }
                .launchIn(this)
        }
    }

    fun setCufeInput(value: String) {
        _uiState.value = _uiState.value.copy(cufeInput = value, error = null)
    }

    /**
     * Parse CUFE from various input formats:
     * - URL with ?chFE=FE...
     * - URL with /FacturasPorCUFE/FE...
     * - Direct FE... string
     */
    private fun parseCufe(input: String): String? {
        val trimmed = input.trim()

        // Try URL query param: chFE=FE...
        val queryMatch = Regex("[?&]chFE=([^&]+)").find(trimmed)
        if (queryMatch != null) return queryMatch.groupValues[1]

        // Try URL path: /FacturasPorCUFE/FE...
        val pathMatch = Regex("/FacturasPorCUFE/(FE[^/\\s]+)").find(trimmed)
        if (pathMatch != null) return pathMatch.groupValues[1]

        // Direct CUFE
        if (trimmed.startsWith("FE") && trimmed.length >= 50) return trimmed

        return null
    }

    fun importCufe() {
        val input = _uiState.value.cufeInput
        val cufe = parseCufe(input)
        if (cufe == null) {
            _uiState.value = _uiState.value.copy(
                error = "CUFE inválido. Debe comenzar con 'FE' y tener al menos 50 caracteres."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isImporting = true,
                isPolling = false,
                pollingTimedOut = false,
                error = null,
                importSuccess = false,
                importedExpenseId = null
            )
            try {
                val job = withContext(Dispatchers.IO) {
                    expensesService.crawlExpense(cufe)
                }
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    currentJob = job
                )

                if (job.isTerminal) {
                    handleTerminalStatus(job)
                } else if (isScreenVisible) {
                    job.resolvedId?.let { startPolling(it) }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    error = e.message ?: "Error al importar"
                )
            }
        }
    }

    fun onScreenVisible() {
        isScreenVisible = true
        val state = _uiState.value
        val job = state.currentJob ?: return
        val jobId = job.resolvedId ?: return
        if (!job.isTerminal && !state.isPolling) {
            startPolling(jobId)
        }
    }

    fun onScreenHidden() {
        isScreenVisible = false
        pollingJob?.cancel()
        pollingJob = null
        pollingJobId = null
        if (_uiState.value.isPolling) {
            _uiState.value = _uiState.value.copy(isPolling = false)
        }
    }

    fun openImportedExpense(onExpenseImported: (Long) -> Unit) {
        val expenseId = _uiState.value.importedExpenseId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isOpeningExpense = true, error = null)
            try {
                withContext(Dispatchers.IO) {
                    expensesService.getExpense(expenseId)
                }
                _uiState.value = _uiState.value.copy(isOpeningExpense = false)
                onExpenseImported(expenseId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isOpeningExpense = false,
                    error = e.message ?: "No se pudo abrir el gasto importado."
                )
            }
        }
    }

    private fun startPolling(jobId: Long) {
        if (pollingJobId == jobId && pollingJob?.isActive == true) return

        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            pollingJobId = jobId
            _uiState.value = _uiState.value.copy(
                isPolling = true,
                pollingTimedOut = false,
                error = null
            )
            var attempts = 0

            while (attempts < MAX_POLL_ATTEMPTS && isScreenVisible) {
                delay(POLL_INTERVAL_MS)
                attempts++
                try {
                    val status = withContext(Dispatchers.IO) {
                        expensesService.getCrawlJobStatus(jobId)
                    }
                    _uiState.value = _uiState.value.copy(
                        currentJob = status,
                        importedExpenseId = status.expenseId ?: _uiState.value.importedExpenseId
                    )

                    if (status.isTerminal) {
                        _uiState.value = _uiState.value.copy(isPolling = false, pollingTimedOut = false)
                        handleTerminalStatus(status)
                        return@launch
                    }
                } catch (e: Exception) {
                    // Continue polling on transient errors
                }
            }

            pollingJobId = null
            if (isScreenVisible && attempts >= MAX_POLL_ATTEMPTS) {
                _uiState.value = _uiState.value.copy(
                    isPolling = false,
                    pollingTimedOut = true,
                    error = "No se pudo confirmar la importación en 1 minuto. Intente nuevamente."
                )
            } else {
                _uiState.value = _uiState.value.copy(isPolling = false)
            }
        }
    }

    private fun handleTerminalStatus(job: com.teco.ventago.features.expenses.domain.models.CrawlJob) {
        val isSuccess = job.status == "success" || job.status == "completed"
        _uiState.value = _uiState.value.copy(
            importSuccess = isSuccess,
            importedExpenseId = if (isSuccess) job.expenseId else _uiState.value.importedExpenseId
        )
    }

    fun showScanner(show: Boolean) {
        _uiState.value = _uiState.value.copy(showScanner = show)
    }

    fun showPermissionDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showPermissionDialog = show)
    }

    fun onQrScanned(rawValue: String) {
        val cufe = parseCufe(rawValue)
        if (cufe != null) {
            _uiState.value = _uiState.value.copy(
                cufeInput = cufe,
                showScanner = false,
                error = null
            )
            importCufe()
        } else {
            _uiState.value = _uiState.value.copy(
                showScanner = false,
                error = "No se encontró un CUFE válido en el código QR escaneado."
            )
        }
    }

    fun reset() {
        pollingJob?.cancel()
        pollingJobId = null
        _uiState.value = CufeImportState(hasExpensesQr = _uiState.value.hasExpensesQr)
    }

    override fun onCleared() {
        pollingJob?.cancel()
        pollingJobId = null
        super.onCleared()
    }
}
