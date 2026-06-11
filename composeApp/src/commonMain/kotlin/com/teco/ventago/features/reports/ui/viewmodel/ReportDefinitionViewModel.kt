package com.teco.ventago.features.reports.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.BaseViewModel
import com.teco.ventago.core.LoadableState
import com.teco.ventago.core.PdfSharer
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.design_system.organism.LoadingState
import com.teco.ventago.features.reports.domain.RealTimeReportsService
import com.teco.ventago.features.reports.domain.model.RealTimeReportCatalog
import com.teco.ventago.features.reports.domain.model.RealTimeReportData
import com.teco.ventago.features.reports.domain.model.RealTimeReportDefinition
import com.teco.ventago.features.reports.domain.model.RealTimeReportExportRequest
import com.teco.ventago.features.reports.domain.model.RealTimeReportRequest
import com.teco.ventago.features.reports.domain.model.ReportExportFormat
import com.teco.ventago.features.reports.domain.model.ReportCustomerOption
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReportDefinitionState(
    val reportKey: String,
    val definition: RealTimeReportDefinition?,
    val request: RealTimeReportRequest?,
    val reportData: RealTimeReportData? = null,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val exportingFormat: ReportExportFormat? = null,
    val errorMessage: String? = null,
    val customerIdInput: String = "",
    val customerSearchQuery: String = "",
    val customerSearchResults: List<ReportCustomerOption> = emptyList(),
    val isSearchingCustomers: Boolean = false,
    val selectedCustomer: ReportCustomerOption? = null,
    override val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
) : LoadableState<ReportDefinitionState> {
    val requiresCustomerSelection: Boolean = reportKey == "customer_statement"

    override fun withLoading(state: LoadingBottomSheetState): ReportDefinitionState {
        return copy(loadingBottomSheet = state)
    }

    companion object {
        fun create(reportKey: String): ReportDefinitionState {
            val definition = RealTimeReportCatalog.reportByKey(reportKey)
            return ReportDefinitionState(
                reportKey = reportKey,
                definition = definition,
                request = definition?.let { RealTimeReportRequest.defaultFor(it) },
                isInitialLoading = definition != null
            )
        }
    }
}

sealed class ReportDefinitionUiEvent

class ReportDefinitionViewModel(
    reportKey: String,
    private val reportsService: RealTimeReportsService,
    private val pdfSharer: PdfSharer,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : BaseViewModel<ReportDefinitionState, ReportDefinitionUiEvent>(
    ReportDefinitionState.create(reportKey)
) {
    private var customerSearchJob: Job? = null

    init {
        loadInitialReport()
    }

    fun presetChanged(value: String) {
        updateState {
            val updatedRequest = request?.copy(
                preset = value,
                page = request.page?.let { 1 }
            )
            copy(request = updatedRequest)
        }
    }

    fun customerIdChanged(value: String) {
        filterChanged("customer_id", value)
    }

    fun customerSearchChanged(value: String) {
        customerSearchJob?.cancel()
        val query = value.trim()
        updateState {
            val cleanedRequest = request?.withFilterValue("customer_id", "")
            copy(
                customerSearchQuery = value,
                selectedCustomer = null,
                customerIdInput = "",
                customerSearchResults = if (query.length < MIN_CUSTOMER_SEARCH_LENGTH) emptyList() else customerSearchResults,
                isSearchingCustomers = query.length >= MIN_CUSTOMER_SEARCH_LENGTH,
                request = cleanedRequest,
                reportData = null,
                errorMessage = null
            )
        }

        if (query.length < MIN_CUSTOMER_SEARCH_LENGTH) {
            updateState { copy(isSearchingCustomers = false) }
            return
        }

        customerSearchJob = viewModelScope.launch {
            delay(CUSTOMER_SEARCH_DEBOUNCE_MS)
            try {
                val customers = withContext(ioDispatcher) {
                    reportsService.searchCustomers(query)
                }
                if (uiState.value.customerSearchQuery == value) {
                    updateState {
                        copy(
                            customerSearchResults = customers,
                            isSearchingCustomers = false,
                            errorMessage = if (customers.isEmpty()) "No encontramos clientes con ese nombre." else null
                        )
                    }
                }
            } catch (_: Exception) {
                if (uiState.value.customerSearchQuery == value) {
                    updateState {
                        copy(
                            customerSearchResults = emptyList(),
                            isSearchingCustomers = false,
                            errorMessage = "No se pudo buscar clientes. Intenta nuevamente."
                        )
                    }
                }
            }
        }
    }

    fun customerSelected(customer: ReportCustomerOption) {
        customerSearchJob?.cancel()
        updateState {
            val updatedRequest = request?.withFilterValue("customer_id", customer.id.toString())
            copy(
                selectedCustomer = customer,
                customerSearchQuery = customer.displayName,
                customerSearchResults = emptyList(),
                customerIdInput = customer.id.toString(),
                request = updatedRequest,
                errorMessage = null,
                reportData = null
            )
        }
    }

    fun filterChanged(key: String, value: String) {
        updateState {
            val updatedRequest = request?.withFilterValue(key, value)
            copy(
                customerIdInput = if (key == "customer_id") value else customerIdInput,
                request = updatedRequest,
                errorMessage = null
            )
        }
    }

    fun applyFilters() {
        val state = uiState.value
        if (state.requiresCustomerSelection && state.customerIdInput.isBlank()) {
            updateState {
                copy(
                    reportData = null,
                    isInitialLoading = false,
                    isRefreshing = false,
                    errorMessage = "Selecciona un cliente para consultar este reporte."
                )
            }
            return
        }

        updateState {
            val updatedRequest = request?.copy(page = request.page?.let { 1 })
            copy(request = updatedRequest)
        }
        loadReport(showFeedback = true)
    }

    fun refresh() {
        loadReport(showFeedback = true)
    }

    fun previousPage() {
        val current = uiState.value.request ?: return
        val page = current.page ?: return
        if (page <= 1) return
        updateState { copy(request = current.copy(page = page - 1)) }
        loadReport(showFeedback = true)
    }

    fun nextPage() {
        val state = uiState.value
        val current = state.request ?: return
        val page = current.page ?: return
        val pagination = state.reportData?.pagination
        if (pagination != null && !pagination.canGoNext) return
        updateState { copy(request = current.copy(page = page + 1)) }
        loadReport(showFeedback = true)
    }

    fun export(format: ReportExportFormat) {
        val state = uiState.value
        val definition = state.definition ?: return
        val currentRequest = state.request ?: return
        if (!definition.exportFormats.contains(format)) return

        viewModelScope.launch {
            updateState {
                copy(
                    exportingFormat = format,
                    loadingBottomSheet = LoadingBottomSheetState(
                        LoadingState.LOADING,
                        "Exportando ${format.label}..."
                    )
                )
            }
            try {
                val payload = withContext(ioDispatcher) {
                    reportsService.exportReport(
                        RealTimeReportExportRequest(
                            reportKey = definition.key,
                            format = format,
                            queryParameters = currentRequest.toQueryParameters(),
                            pdfEndpoint = definition.pdfEndpoint
                        )
                    )
                }
                val saved = pdfSharer.saveFileToDocuments(
                    filename = payload.fileName ?: fallbackFileName(definition.key, format),
                    bytes = payload.bytes,
                    mimeType = payload.contentType ?: fallbackMimeType(format)
                )
                updateState {
                    copy(
                        exportingFormat = null,
                        loadingBottomSheet = LoadingBottomSheetState(
                            if (saved) LoadingState.SUCCESS else LoadingState.ERROR,
                            if (saved) "Reporte guardado" else "No se pudo guardar el reporte"
                        )
                    )
                }
            } catch (_: Exception) {
                updateState {
                    copy(
                        exportingFormat = null,
                        loadingBottomSheet = LoadingBottomSheetState(
                            LoadingState.ERROR,
                            "No se pudo exportar el reporte"
                        )
                    )
                }
            }
        }
    }

    private fun loadInitialReport() {
        val state = uiState.value
        if (state.definition == null) {
            updateState { copy(isInitialLoading = false) }
            return
        }
        if (state.requiresCustomerSelection) {
            updateState {
                copy(
                    isInitialLoading = false,
                    errorMessage = "Selecciona un cliente para consultar este reporte."
                )
            }
            return
        }
        loadReport(showFeedback = false)
    }

    private fun loadReport(showFeedback: Boolean) {
        val currentRequest = uiState.value.request ?: return
        viewModelScope.launch {
            if (showFeedback) {
                updateState {
                    copy(
                        isRefreshing = true,
                        errorMessage = null,
                        loadingBottomSheet = LoadingBottomSheetState(LoadingState.LOADING, "Consultando reporte...")
                    )
                }
            } else {
                updateState {
                    copy(
                        isInitialLoading = reportData == null,
                        isRefreshing = reportData != null,
                        errorMessage = null
                    )
                }
            }

            try {
                val data = withContext(ioDispatcher) {
                    reportsService.getReport(currentRequest)
                }
                updateState {
                    copy(
                        reportData = data,
                        isInitialLoading = false,
                        isRefreshing = false,
                        errorMessage = null,
                        loadingBottomSheet = if (showFeedback) {
                            LoadingBottomSheetState(LoadingState.SUCCESS, "Reporte actualizado")
                        } else {
                            loadingBottomSheet
                        }
                    )
                }
            } catch (_: Exception) {
                updateState {
                    copy(
                        isInitialLoading = false,
                        isRefreshing = false,
                        errorMessage = "No se pudo consultar el reporte. Revisa los filtros e intenta nuevamente.",
                        loadingBottomSheet = if (showFeedback) {
                            LoadingBottomSheetState(LoadingState.ERROR, "No se pudo consultar el reporte")
                        } else {
                            loadingBottomSheet
                        }
                    )
                }
            }
        }
    }

    private fun fallbackFileName(reportKey: String, format: ReportExportFormat): String {
        val extension = when (format) {
            ReportExportFormat.XLSX -> "xlsx"
            ReportExportFormat.PDF -> "pdf"
        }
        return "${reportKey}_reporte.$extension"
    }

    private fun fallbackMimeType(format: ReportExportFormat): String {
        return when (format) {
            ReportExportFormat.XLSX -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            ReportExportFormat.PDF -> "application/pdf"
        }
    }

    private fun RealTimeReportRequest.withFilterValue(key: String, rawValue: String): RealTimeReportRequest {
        val value = rawValue.trim()
        val normalized = value.ifBlank { null }
        val cleanedExtraFilters = extraFilters - key
        val resetPage = page?.let { 1 }
        return when (key) {
            "start_date" -> copy(startDate = normalized, extraFilters = cleanedExtraFilters, page = resetPage)
            "end_date" -> copy(endDate = normalized, extraFilters = cleanedExtraFilters, page = resetPage)
            "compare_start_date" -> copy(compareStartDate = normalized, extraFilters = cleanedExtraFilters, page = resetPage)
            "compare_end_date" -> copy(compareEndDate = normalized, extraFilters = cleanedExtraFilters, page = resetPage)
            "customer_id" -> copy(customerId = normalized, extraFilters = cleanedExtraFilters, page = resetPage)
            else -> copy(
                extraFilters = if (normalized == null) cleanedExtraFilters else cleanedExtraFilters + (key to value),
                page = resetPage
            )
        }
    }

    companion object {
        private const val CUSTOMER_SEARCH_DEBOUNCE_MS = 450L
        private const val MIN_CUSTOMER_SEARCH_LENGTH = 2
    }
}
