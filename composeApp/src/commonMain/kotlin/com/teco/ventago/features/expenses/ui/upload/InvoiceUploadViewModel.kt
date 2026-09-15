package com.teco.ventago.features.expenses.ui.upload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.beta.BetaFeature
import com.teco.ventago.core.beta.BetaService
import com.teco.ventago.core.file.SharedFile
import com.teco.ventago.features.expenses.domain.ExpensesService
import com.teco.ventago.features.expenses.domain.models.requests.ExpenseProofFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InvoiceUploadViewModel(
    private val expensesService: ExpensesService,
    private val betaService: BetaService
) : ViewModel() {
    companion object {
        private const val MAX_FILE_BYTES = 10 * 1024 * 1024
    }

    private val _uiState = MutableStateFlow(InvoiceUploadState())
    val uiState: StateFlow<InvoiceUploadState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            betaService.features()
                .onEach { response ->
                    val keys = response?.features.orEmpty().toSet()
                    val hasAccess = BetaFeature.EXPENSES_OCR.key in keys ||
                        BetaFeature.EXPENSES_QR.key in keys
                    _uiState.value = _uiState.value.copy(hasInvoiceUploadAccess = hasAccess)
                }
                .launchIn(this)
        }
    }

    fun uploadFile(file: SharedFile) {
        if (!_uiState.value.hasInvoiceUploadAccess) {
            _uiState.value = _uiState.value.copy(
                error = "La carga de facturas no está disponible para este negocio."
            )
            return
        }
        if (file.bytes.size > MAX_FILE_BYTES) {
            _uiState.value = _uiState.value.copy(
                selectedFileName = file.fileName,
                error = "El archivo no puede superar 10 MB."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUploading = true,
                selectedFileName = file.fileName,
                showAccepted = false,
                error = null
            )
            try {
                val result = withContext(Dispatchers.IO) {
                    expensesService.uploadOcr(
                        ExpenseProofFile(
                            bytes = file.bytes,
                            fileName = file.fileName,
                            contentType = file.contentType
                        )
                    )
                }
                if (!result.accepted) {
                    _uiState.value = _uiState.value.copy(
                        isUploading = false,
                        error = result.message ?: "No se pudo recibir la factura. Intente de nuevo."
                    )
                    return@launch
                }
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    showAccepted = true,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    error = e.message ?: "No se pudo subir la factura. Intente de nuevo."
                )
            }
        }
    }

    fun dismissAccepted() {
        _uiState.value = _uiState.value.copy(
            showAccepted = false,
            selectedFileName = null,
            error = null
        )
    }
}
