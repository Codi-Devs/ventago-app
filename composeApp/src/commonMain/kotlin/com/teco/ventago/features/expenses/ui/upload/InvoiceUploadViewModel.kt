package com.teco.ventago.features.expenses.ui.upload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.beta.BetaFeature
import com.teco.ventago.core.beta.BetaService
import com.teco.ventago.core.file.SharedFile
import com.teco.ventago.features.expenses.domain.CufeParser
import com.teco.ventago.features.expenses.domain.InvoiceImageQuality
import com.teco.ventago.features.expenses.domain.InvoiceQualityReason
import com.teco.ventago.features.expenses.domain.InvoiceQualityVerdict
import com.teco.ventago.features.expenses.domain.decodeInvoiceRaster
import com.teco.ventago.features.expenses.domain.fitInvoiceJpegUnderLimit
import com.teco.ventago.features.expenses.domain.isInvoiceRasterMime
import com.teco.ventago.features.expenses.domain.scanQrFromImageBytes
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
        viewModelScope.launch {
            val prepared = if (isInvoiceRasterMime(file.contentType, file.fileName)) {
                val fitted = withContext(Dispatchers.Default) {
                    fitInvoiceJpegUnderLimit(file.bytes, MAX_FILE_BYTES)
                }
                file.copy(bytes = fitted)
            } else {
                file
            }
            if (prepared.bytes.size > MAX_FILE_BYTES) {
                _uiState.value = _uiState.value.copy(
                    selectedFileName = prepared.fileName,
                    error = "El archivo no puede superar 10 MB."
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(
                isScanning = true,
                isUploading = false,
                selectedFileName = prepared.fileName,
                showAccepted = false,
                acceptedFromDgi = false,
                error = null
            )
            try {
                if (isInvoiceRasterMime(prepared.contentType, prepared.fileName)) {
                    val raster = withContext(Dispatchers.Default) {
                        decodeInvoiceRaster(prepared.bytes)
                    }
                    if (raster == null) {
                        _uiState.value = _uiState.value.copy(
                            isScanning = false,
                            error = InvoiceQualityReason.UNREADABLE.message
                        )
                        return@launch
                    }
                    when (val verdict = InvoiceImageQuality.assess(raster)) {
                        is InvoiceQualityVerdict.Reject -> {
                            _uiState.value = _uiState.value.copy(
                                isScanning = false,
                                error = verdict.message
                            )
                            return@launch
                        }
                        InvoiceQualityVerdict.Ok -> Unit
                    }
                    val qrPayload = withContext(Dispatchers.Default) {
                        scanQrFromImageBytes(prepared.bytes)
                    }
                    val cufe = qrPayload?.let { CufeParser.parse(it) }
                    if (cufe != null && tryCrawl(cufe)) {
                        return@launch
                    }
                }

                _uiState.value = _uiState.value.copy(isUploading = true)
                val result = withContext(Dispatchers.IO) {
                    expensesService.uploadOcr(
                        ExpenseProofFile(
                            bytes = prepared.bytes,
                            fileName = prepared.fileName,
                            contentType = prepared.contentType
                        )
                    )
                }
                if (!result.accepted) {
                    _uiState.value = _uiState.value.copy(
                        isScanning = false,
                        isUploading = false,
                        error = result.message ?: "No se pudo recibir la factura. Intente de nuevo."
                    )
                    return@launch
                }
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    isUploading = false,
                    showAccepted = true,
                    acceptedFromDgi = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    isUploading = false,
                    error = e.message ?: "No se pudo subir la factura. Intente de nuevo."
                )
            }
        }
    }

    private suspend fun tryCrawl(cufe: String): Boolean {
        return try {
            val job = withContext(Dispatchers.IO) {
                expensesService.crawlExpense(cufe)
            }
            val failed = job.status == "failed" || job.status == "error" ||
                job.status == "cancelled" || job.status == "timeout"
            if (failed) return false
            _uiState.value = _uiState.value.copy(
                isScanning = false,
                isUploading = false,
                showAccepted = true,
                acceptedFromDgi = true,
                error = null
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    fun dismissAccepted() {
        _uiState.value = _uiState.value.copy(
            showAccepted = false,
            acceptedFromDgi = false,
            isScanning = false,
            isUploading = false,
            selectedFileName = null,
            error = null
        )
    }

    fun clearError() {
        if (_uiState.value.error != null) {
            _uiState.value = _uiState.value.copy(error = null)
        }
    }
}
