package com.teco.ventago.features.expenses.ui.cufe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teco.ventago.features.expenses.domain.ExpensesService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CufeImportViewModel(
    private val expensesService: ExpensesService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CufeImportState())
    val uiState: StateFlow<CufeImportState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

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
            _uiState.value = _uiState.value.copy(isImporting = true, error = null, importSuccess = false)
            try {
                val job = withContext(Dispatchers.IO) {
                    expensesService.crawlExpense(cufe)
                }
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    currentJob = job
                )
                // Start polling
                if (job.id != null && !job.isTerminal) {
                    startPolling(job.id)
                } else if (job.status == "success") {
                    _uiState.value = _uiState.value.copy(
                        importSuccess = true,
                        importedExpenseId = job.expenseId
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    error = e.message ?: "Error al importar"
                )
            }
        }
    }

    private fun startPolling(jobId: Long) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPolling = true)
            var attempts = 0
            val maxAttempts = 30 // 60 seconds max

            while (attempts < maxAttempts) {
                delay(2000)
                attempts++
                try {
                    val status = withContext(Dispatchers.IO) {
                        expensesService.getCrawlJobStatus(jobId)
                    }
                    _uiState.value = _uiState.value.copy(currentJob = status)

                    if (status.isTerminal) {
                        _uiState.value = _uiState.value.copy(isPolling = false)
                        if (status.status == "success") {
                            _uiState.value = _uiState.value.copy(
                                importSuccess = true,
                                importedExpenseId = status.expenseId
                            )
                        }
                        return@launch
                    }
                } catch (e: Exception) {
                    // Continue polling on transient errors
                }
            }

            // Timeout
            _uiState.value = _uiState.value.copy(
                isPolling = false,
                error = "Tiempo de espera agotado. La importación sigue en proceso."
            )
        }
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
        _uiState.value = CufeImportState()
    }

    override fun onCleared() {
        pollingJob?.cancel()
        super.onCleared()
    }
}
