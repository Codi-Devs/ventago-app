package com.teco.ventago.features.reports.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.loaders.shimmerBrush
import com.teco.ventago.design_system.molecules.InstallmentDueDateFieldKmp
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.features.reports.domain.model.RealTimeReportCatalog
import com.teco.ventago.features.reports.domain.model.RealTimeReportCategory
import com.teco.ventago.features.reports.domain.model.RealTimeReportCell
import com.teco.ventago.features.reports.domain.model.RealTimeReportChartPoint
import com.teco.ventago.features.reports.domain.model.RealTimeReportChartSection
import com.teco.ventago.features.reports.domain.model.RealTimeReportData
import com.teco.ventago.features.reports.domain.model.RealTimeReportDefinition
import com.teco.ventago.features.reports.domain.model.RealTimeReportFilterCatalog
import com.teco.ventago.features.reports.domain.model.RealTimeReportFilterDefinition
import com.teco.ventago.features.reports.domain.model.RealTimeReportFilterType
import com.teco.ventago.features.reports.domain.model.RealTimeReportMetric
import com.teco.ventago.features.reports.domain.model.RealTimeReportRow
import com.teco.ventago.features.reports.domain.model.ReportExportFormat
import com.teco.ventago.features.reports.domain.model.ReportCustomerOption
import com.teco.ventago.features.reports.ui.viewmodel.ReportDefinitionState
import com.teco.ventago.features.reports.ui.viewmodel.ReportDefinitionViewModel
import com.teco.ventago.isTablet
import com.teco.ventago.utils.formatNumberToMoney
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ReportsScreen(
    onReportSelected: (String) -> Unit,
) {
    var selectedCategoryName by rememberSaveable { mutableStateOf(RealTimeReportCategory.SALES.name) }
    val selectedCategory = remember(selectedCategoryName) {
        RealTimeReportCategory.entries.firstOrNull { it.name == selectedCategoryName }
            ?: RealTimeReportCategory.SALES
    }
    val reports = remember(selectedCategory) {
        RealTimeReportCatalog.reportsFor(selectedCategory)
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ReportsHeader(
            totalReports = RealTimeReportCatalog.reports.size,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
        )
        CategorySelector(
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategoryName = it.name },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        CategoryIntro(
            category = selectedCategory,
            reportCount = reports.size,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        ReportsGrid(
            reports = reports,
            onReportSelected = onReportSelected,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDefinitionScreen(
    reportKey: String,
    onBack: () -> Unit,
    onOpenOrders: () -> Unit,
    onOpenOrderDetails: (String) -> Unit,
) {
    val viewModel: ReportDefinitionViewModel = koinViewModel(
        parameters = { parametersOf(reportKey) }
    )
    val uiState by viewModel.uiState.collectAsState()
    val report = uiState.definition
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .pointerInput(focusManager) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (report == null) {
            ReportNotFoundCard()
            return@Column
        }

        ReportDefinitionHeader(report)
        ReportFilterBand(
            state = uiState,
            onPresetChanged = viewModel::presetChanged,
            onFilterChanged = viewModel::filterChanged,
            onCustomerSearchChanged = viewModel::customerSearchChanged,
            onCustomerSelected = viewModel::customerSelected,
            onApply = viewModel::applyFilters,
            onRefresh = viewModel::refresh
        )

        when {
            uiState.isInitialLoading || uiState.isRefreshing -> ReportLoadingSkeleton()
            uiState.reportData != null -> ReportDataContent(
                data = uiState.reportData!!,
                state = uiState,
                onPreviousPage = viewModel::previousPage,
                onNextPage = viewModel::nextPage,
                onExport = viewModel::export,
                onOpenOrders = onOpenOrders,
                onOpenOrderDetails = onOpenOrderDetails
            )
            else -> ReportEmptyState(
                title = "Sin datos para mostrar",
                message = uiState.errorMessage ?: "Aplica filtros para consultar la informacion del reporte."
            )
        }
    }

    if (uiState.loadingBottomSheet.isLoading()) {
        LoadingSheet(
            state = uiState.loadingBottomSheet,
            sheetState = sheetState,
            onDismissRequest = viewModel::hideLoading
        )
    }
}

@Composable
private fun ReportsHeader(totalReports: Int, modifier: Modifier = Modifier) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.tertiary
        )
    )
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .background(gradient)
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Assessment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Reportes en tiempo real",
                    style = titleMediumBold(color = MaterialTheme.colorScheme.onPrimary)
                )
                Text(
                    text = "$totalReports reportes agrupados por categoria",
                    style = bodyMedium(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f))
                )
            }
        }
    }
}

@Composable
private fun CategorySelector(
    selectedCategory: RealTimeReportCategory,
    onCategorySelected: (RealTimeReportCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RealTimeReportCatalog.categories.forEach { category ->
            CategoryChip(
                category = category,
                selected = category == selectedCategory,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
private fun CategoryChip(
    category: RealTimeReportCategory,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(if (selected) 3.dp else 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondary else cardContainerColor()
        ),
        modifier = Modifier.widthIn(min = 132.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = category.icon(),
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = category.title,
                style = bodyMediumBold(
                    color = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CategoryIntro(
    category: RealTimeReportCategory,
    reportCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = category.icon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(text = category.title, style = titleMediumBold(color = MaterialTheme.colorScheme.secondary))
            Spacer(Modifier.weight(1f))
            StatusPill(text = "$reportCount reportes")
        }
        Text(
            text = category.description,
            style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
    }
}

@Composable
private fun ReportsGrid(
    reports: List<RealTimeReportDefinition>,
    onReportSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tablet = isTablet()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (tablet) {
            reports.chunked(2).forEach { rowReports ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowReports.forEach { report ->
                        ReportCard(
                            report = report,
                            onClick = { onReportSelected(report.key) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowReports.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            reports.forEach { report ->
                ReportCard(
                    report = report,
                    onClick = { onReportSelected(report.key) }
                )
            }
        }
    }
}

@Composable
private fun ReportCard(
    report: RealTimeReportDefinition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = report.title,
                    style = bodyMediumBold(color = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = report.description,
                style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}

@Composable
private fun ReportDefinitionHeader(report: RealTimeReportDefinition) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = report.category.icon(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = report.title, style = titleMediumBold(color = MaterialTheme.colorScheme.secondary))
                    Text(
                        text = report.category.title,
                        style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
            Text(
                text = report.description,
                style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}

@Composable
private fun ReportNotFoundCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = "Reporte no disponible", style = bodyMediumBold())
            Text(
                text = "No encontramos este reporte en tiempo real.",
                style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportFilterBand(
    state: ReportDefinitionState,
    onPresetChanged: (String) -> Unit,
    onFilterChanged: (String, String) -> Unit,
    onCustomerSearchChanged: (String) -> Unit,
    onCustomerSelected: (ReportCustomerOption) -> Unit,
    onApply: () -> Unit,
    onRefresh: () -> Unit,
) {
    val definition = state.definition ?: return
    val request = state.request ?: return
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.FilterAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(text = "Filtros", style = bodyMediumBold(), modifier = Modifier.weight(1f))
                if (state.isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetOptions(definition).forEach { preset ->
                    SelectablePill(
                        text = presetLabel(preset),
                        selected = request.preset == preset,
                        onClick = { onPresetChanged(preset) }
                    )
                }
            }

            val filters = RealTimeReportFilterCatalog.filtersFor(definition)
            filters.forEach { filter ->
                if (filter.key == "customer_id" && state.requiresCustomerSelection) {
                    CustomerStatementSearchField(
                        state = state,
                        onQueryChanged = onCustomerSearchChanged,
                        onCustomerSelected = onCustomerSelected
                    )
                } else {
                    ReportFilterInput(
                        filter = filter,
                        value = request.toQueryParameters()[filter.key].orEmpty(),
                        onChange = { value -> onFilterChanged(filter.key, value) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ButtonM(
                    onClick = onApply,
                    modifier = Modifier.weight(1f),
                    enabled = !state.isRefreshing,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ) {
                    Text(text = "Aplicar", style = bodyMediumBold(color = MaterialTheme.colorScheme.onSecondary))
                }
                OutlinedButtonM(
                    onClick = onRefresh,
                    modifier = Modifier.weight(1f),
                    enabled = !state.isRefreshing && state.reportData != null,
                    contentColor = MaterialTheme.colorScheme.secondary,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                ) {
                    Text(text = "Actualizar", style = bodyMediumBold(color = MaterialTheme.colorScheme.secondary))
                }
            }
        }
    }
}

@Composable
private fun ReportDataContent(
    data: RealTimeReportData,
    state: ReportDefinitionState,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onExport: (ReportExportFormat) -> Unit,
    onOpenOrders: () -> Unit,
    onOpenOrderDetails: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (!state.errorMessage.isNullOrBlank()) {
            ReportEmptyState(
                title = "Aviso",
                message = state.errorMessage
            )
        }
        ReportMetricsSection(metrics = metricsFor(data))
        ReportChartsSection(sections = chartSectionsFor(data))
        ReportRowsSection(
            data = data,
            onOpenOrderDetails = onOpenOrderDetails
        )
        ReportRelatedActions(data = data, onOpenOrders = onOpenOrders)
        ReportPaginationSection(
            data = data,
            onPreviousPage = onPreviousPage,
            onNextPage = onNextPage
        )
        ReportExportSection(
            definition = data.definition,
            exportingFormat = state.exportingFormat,
            onExport = onExport
        )
    }
}

@Composable
private fun ReportRelatedActions(
    data: RealTimeReportData,
    onOpenOrders: () -> Unit,
) {
    if (!data.definition.isSalesSummary()) return
    ButtonM(
        onClick = onOpenOrders,
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary
    ) {
        Text(text = "Ver órdenes", style = bodyMediumBold(color = MaterialTheme.colorScheme.onSecondary))
    }
}

@Composable
private fun CustomerStatementSearchField(
    state: ReportDefinitionState,
    onQueryChanged: (String) -> Unit,
    onCustomerSelected: (ReportCustomerOption) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DMOutlinedTextField(
            text = state.customerSearchQuery,
            label = "Cliente",
            modifier = Modifier.fillMaxWidth(),
            onChange = onQueryChanged,
            supportingText = when {
                state.selectedCustomer != null -> "Cliente seleccionado para el estado de cuenta"
                state.isSearchingCustomers -> "Buscando clientes..."
                state.customerSearchQuery.length in 1..1 -> "Escribe al menos 2 caracteres"
                else -> "Busca por nombre y selecciona un cliente"
            }
        )

        if (state.isSearchingCustomers) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Text(
                    text = "Buscando coincidencias",
                    style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }

        if (state.customerSearchResults.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    state.customerSearchResults.forEach { customer ->
                        Surface(
                            onClick = { onCustomerSelected(customer) },
                            color = Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = customer.name,
                                    style = bodyMediumBold(color = MaterialTheme.colorScheme.secondary),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = customer.ruc.ifBlank { "Sin RUC" },
                                    style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportFilterInput(
    filter: RealTimeReportFilterDefinition,
    value: String,
    onChange: (String) -> Unit,
) {
    when (filter.type) {
        RealTimeReportFilterType.DATE -> {
            InstallmentDueDateFieldKmp(
                valueIso = value,
                onDatePickedIso = onChange,
                modifier = Modifier.fillMaxWidth(),
                label = filter.label
            )
        }
        RealTimeReportFilterType.TEXT,
        RealTimeReportFilterType.NUMBER -> {
            DMOutlinedTextField(
                text = value,
                label = filter.label,
                modifier = Modifier.fillMaxWidth(),
                onChange = onChange,
                keyboardType = if (filter.type == RealTimeReportFilterType.NUMBER) KeyboardType.Number else KeyboardType.Text,
                supportingText = if (filter.key == "customer_id") "Requerido para consultar este reporte" else ""
            )
        }
        RealTimeReportFilterType.SELECT -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = filter.label, style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SelectablePill(
                        text = "Sin filtro",
                        selected = value.isBlank(),
                        onClick = { onChange("") }
                    )
                    filter.options.forEach { option ->
                        SelectablePill(
                            text = option.label,
                            selected = value == option.value,
                            onClick = { onChange(option.value) }
                        )
                    }
                }
            }
        }
        RealTimeReportFilterType.BOOLEAN -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = filter.label, style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectablePill(text = "Sin filtro", selected = value.isBlank(), onClick = { onChange("") })
                    SelectablePill(text = "Sí", selected = value == "true", onClick = { onChange("true") })
                    SelectablePill(text = "No", selected = value == "false", onClick = { onChange("false") })
                }
            }
        }
    }
}

@Composable
private fun ReportMetricsSection(metrics: List<RealTimeReportMetric>) {
    if (metrics.isEmpty()) {
        ReportEmptyState(
            title = "Indicadores sin datos",
            message = "No hay indicadores para los filtros seleccionados."
        )
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle(icon = Icons.Filled.Assessment, title = "Indicadores principales")
            val tablet = isTablet()
            val chunkSize = if (tablet) 3 else 2
            metrics.chunked(chunkSize).forEach { rowMetrics ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowMetrics.forEach { metric ->
                        MetricTile(
                            metric = metric,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(chunkSize - rowMetrics.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricTile(metric: RealTimeReportMetric, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(84.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = metric.label,
                style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = metric.value,
                style = bodyMediumBold(color = MaterialTheme.colorScheme.secondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ReportChartsSection(sections: List<RealTimeReportChartSection>) {
    val chartSections = sections.filter { it.points.isNotEmpty() }
    if (chartSections.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        chartSections.take(3).forEachIndexed { index, section ->
            ChartCard(
                section = section,
                useDonut = section.key.contains("collected_pending") ||
                    section.key.contains("donut") ||
                    section.key.contains("concentration") ||
                    section.key.contains("charge_payment") ||
                    index == 1 &&
                    !section.key.contains("sales_summary_trend") &&
                    !section.key.contains("composition") &&
                    !section.key.contains("top_suppliers") &&
                    !section.key.contains("combo") &&
                    !section.key.contains("grouped") ||
                    section.key.contains("breakdown") ||
                    section.key.contains("aging"),
                useVertical = section.key.contains("vertical") || section.key.contains("net_by_period"),
                useCombo = section.key.contains("combo"),
                useGrouped = section.key.contains("grouped"),
                useLine = section.key.contains("line"),
                barColor = REPORT_CHART_GREEN
            )
        }
    }
}

@Composable
private fun ChartCard(
    section: RealTimeReportChartSection,
    useDonut: Boolean,
    useVertical: Boolean,
    useCombo: Boolean,
    useGrouped: Boolean,
    useLine: Boolean,
    barColor: Color,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle(
                icon = Icons.Rounded.Search,
                title = section.title
            )
            if (useDonut && section.points.size > 1) {
                DonutChart(points = section.points)
            } else if (useGrouped) {
                GroupedHorizontalBarChart(section = section, barColor = barColor)
            } else if (useCombo) {
                ComboBarLineChart(section = section, barColor = barColor)
            } else if (useLine) {
                LineChart(section = section, lineColor = barColor)
            } else if (useVertical) {
                VerticalBarChart(points = section.points, barColor = barColor)
            } else {
                HorizontalBarChart(points = section.points, barColor = barColor)
            }
        }
    }
}

@Composable
private fun VerticalBarChart(points: List<RealTimeReportChartPoint>, barColor: Color) {
    val visiblePoints = points.take(MAX_REPORT_CHART_POINTS)
    val maxValue = visiblePoints.maxOfOrNull { kotlin.math.abs(it.value) }?.takeIf { it > 0.0 } ?: 1.0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        visiblePoints.forEach { point ->
            val ratio = (kotlin.math.abs(point.value) / maxValue).toFloat().coerceIn(0.04f, 1f)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = point.formattedValue,
                    style = labelSmall(color = barColor),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp)
                        .padding(top = 6.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((96.dp * ratio).coerceAtLeast(8.dp))
                            .background(barColor, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    )
                }
                Text(
                    text = point.label,
                    style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun LineChart(section: RealTimeReportChartSection, lineColor: Color) {
    val series = section.lines.mapNotNull(::groupedChartPoint)
        .groupBy { it.series }
        .takeIf { it.isNotEmpty() }
        ?: mapOf("Actual" to section.points.take(MAX_REPORT_CHART_POINTS).map {
            GroupedChartPoint(it.label, "Actual", it.value, it.formattedValue)
        })
    val visiblePoints = series.values.flatten().take(MAX_REPORT_CHART_POINTS * 3)
    if (visiblePoints.isEmpty()) return
    val labels = series.values.flatten().map { it.group }.distinct().take(MAX_REPORT_CHART_POINTS)
    val maxValue = visiblePoints.maxOfOrNull { it.value } ?: 1.0
    val minValue = visiblePoints.minOfOrNull { it.value } ?: 0.0
    val range = (maxValue - minValue).takeIf { it != 0.0 } ?: 1.0
    val colors = listOf(
        lineColor,
        MaterialTheme.colorScheme.secondary,
        lineColor.copy(alpha = 0.56f)
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(144.dp)
                .padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            val step = if (labels.size == 1) 0f else size.width / (labels.size - 1)
            series.entries.forEachIndexed { seriesIndex, (_, points) ->
                val coordinates = labels.mapIndexedNotNull { index, label ->
                    val point = points.firstOrNull { it.group == label } ?: return@mapIndexedNotNull null
                    val yRatio = ((point.value - minValue) / range).toFloat().coerceIn(0f, 1f)
                    Offset(x = step * index, y = size.height - (size.height * yRatio))
                }
                if (coordinates.size > 1) {
                    val path = Path().apply {
                        moveTo(coordinates.first().x, coordinates.first().y)
                        coordinates.drop(1).forEach { point -> lineTo(point.x, point.y) }
                    }
                    drawPath(
                        path = path,
                        color = colors[seriesIndex % colors.size],
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                    )
                }
                coordinates.forEach { offset ->
                    drawCircle(color = colors[seriesIndex % colors.size], radius = 5f, center = offset)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            series.keys.take(3).forEachIndexed { index, name ->
                LegendDot(color = colors[index % colors.size], text = name)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            labels.forEach { label ->
                Text(
                    text = label,
                    style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ComboBarLineChart(section: RealTimeReportChartSection, barColor: Color) {
    val bars = section.points.take(MAX_REPORT_CHART_POINTS)
    val linePoints = section.lines.mapNotNull(::comboLinePoint).take(MAX_REPORT_CHART_POINTS)
    val lineLabel = linePoints.firstOrNull()?.seriesLabel ?: "Línea"
    val maxValue = (bars.map { kotlin.math.abs(it.value) } + linePoints.map { kotlin.math.abs(it.value) })
        .maxOrNull()
        ?.takeIf { it > 0.0 }
        ?: 1.0
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(184.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                bars.forEach { point ->
                    val ratio = (kotlin.math.abs(point.value) / maxValue).toFloat().coerceIn(0.04f, 1f)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = point.formattedValue,
                            style = labelSmall(color = barColor),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((116.dp * ratio).coerceAtLeast(8.dp))
                                .background(barColor.copy(alpha = 0.78f), RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        )
                        Text(
                            text = point.label,
                            style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (linePoints.size > 1) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    val step = if (linePoints.size == 1) 0f else size.width / (linePoints.size - 1)
                    val coordinates = linePoints.mapIndexed { index, point ->
                        val yRatio = (kotlin.math.abs(point.value) / maxValue).toFloat().coerceIn(0f, 1f)
                        Offset(x = step * index, y = size.height - (size.height * yRatio))
                    }
                    coordinates.zipWithNext().forEach { (start, end) ->
                        drawLine(
                            color = secondaryColor,
                            start = start,
                            end = end,
                            strokeWidth = 4f
                        )
                    }
                    coordinates.forEach { offset ->
                        drawCircle(color = secondaryColor, radius = 5f, center = offset)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            LegendDot(color = barColor, text = "Total")
            LegendDot(color = secondaryColor, text = lineLabel)
        }
    }
}

@Composable
private fun GroupedHorizontalBarChart(section: RealTimeReportChartSection, barColor: Color) {
    val groups = section.lines.mapNotNull(::groupedChartPoint)
        .groupBy { it.group }
        .entries
        .take(MAX_REPORT_CHART_POINTS)
    val maxValue = groups.flatMap { it.value }
        .maxOfOrNull { kotlin.math.abs(it.value) }
        ?.takeIf { it > 0.0 }
        ?: 1.0
    val colors = listOf(
        barColor,
        barColor.copy(alpha = 0.68f),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.62f)
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        groups.forEach { (group, values) ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = group,
                    style = bodyMediumBold(color = MaterialTheme.colorScheme.secondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                values.forEachIndexed { index, item ->
                    val color = colors[index % colors.size]
                    val ratio = (kotlin.math.abs(item.value) / maxValue).toFloat().coerceIn(0.04f, 1f)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.series,
                            style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.formattedValue,
                            style = labelSmall(color = color),
                            textAlign = TextAlign.End
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(color.copy(alpha = 0.12f), RoundedCornerShape(50))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratio)
                                .height(8.dp)
                                .background(color, RoundedCornerShape(50))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(color, RoundedCornerShape(50))
        )
        Text(text = text, style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant))
    }
}

@Composable
private fun HorizontalBarChart(points: List<RealTimeReportChartPoint>, barColor: Color) {
    val maxValue = points.maxOfOrNull { kotlin.math.abs(it.value) }?.takeIf { it > 0.0 } ?: 1.0
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        points.forEach { point ->
            val ratio = (kotlin.math.abs(point.value) / maxValue).toFloat().coerceIn(0.04f, 1f)
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = point.label,
                        style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = point.formattedValue,
                        style = labelSmall(color = barColor),
                        textAlign = TextAlign.End
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .background(barColor.copy(alpha = 0.12f), RoundedCornerShape(50))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ratio)
                            .height(10.dp)
                            .background(barColor, RoundedCornerShape(50))
                    )
                }
            }
        }
    }
}

@Composable
private fun DonutChart(points: List<RealTimeReportChartPoint>) {
    val colors = listOf(
        REPORT_CHART_GREEN,
        REPORT_CHART_GREEN.copy(alpha = 0.74f),
        REPORT_CHART_GREEN.copy(alpha = 0.52f),
        REPORT_CHART_GREEN.copy(alpha = 0.34f),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.42f),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.24f),
    )
    val total = points.sumOf { kotlin.math.abs(it.value) }.takeIf { it > 0.0 } ?: 1.0
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(132.dp)) {
            var startAngle = -90f
            val stroke = size.minDimension * 0.18f
            points.forEachIndexed { index, point ->
                val sweep = ((kotlin.math.abs(point.value) / total) * 360.0).toFloat()
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                )
                startAngle += sweep
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            points.take(6).forEachIndexed { index, point ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(colors[index % colors.size], RoundedCornerShape(50))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = point.label,
                        style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = point.formattedValue,
                        style = labelSmall(color = MaterialTheme.colorScheme.secondary),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportRowsSection(
    data: RealTimeReportData,
    onOpenOrderDetails: (String) -> Unit,
) {
    if (data.definition.isCashFlow()) {
        CashFlowRowsSection(
            data = data,
            onOpenOrderDetails = onOpenOrderDetails
        )
        return
    }

    val rows = rowsFor(data)
    val showAllRows = data.definition.isSalesAdjustments() || data.definition.isSalesPendingCollection()
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle(
                icon = Icons.Rounded.TableChart,
                title = "Detalle"
            )

            if (rows.isEmpty()) {
                Text(
                    text = "No hay registros para los filtros seleccionados.",
                    style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                return@Column
            }

            if (isTablet()) {
                ReportDesktopRows(rows, showAllRows)
            } else {
                val visibleRows = if (showAllRows) rows else rows.take(MAX_VISIBLE_DETAIL_ROWS)
                visibleRows.forEachIndexed { index, row ->
                    ReportMobileRow(index = index + 1, cells = row.cells)
                }
                if (!showAllRows && rows.size > MAX_VISIBLE_DETAIL_ROWS) {
                    Text(
                        text = "Mostrando $MAX_VISIBLE_DETAIL_ROWS de ${rows.size} registros.",
                        style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }
    }
}

@Composable
private fun CashFlowRowsSection(
    data: RealTimeReportData,
    onOpenOrderDetails: (String) -> Unit,
) {
    val tabs = remember(data) { cashFlowTabs(data) }
    var selectedTabKey by rememberSaveable(data.definition.key) { mutableStateOf(CASH_FLOW_SUMMARY_TAB_KEY) }
    val selectedTab = tabs.firstOrNull { it.key == selectedTabKey } ?: tabs.first()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle(
                icon = Icons.Rounded.TableChart,
                title = "Detalle"
            )

            TabRow(
                selectedTabIndex = tabs.indexOfFirst { it.key == selectedTab.key }.coerceAtLeast(0),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.secondary
            ) {
                tabs.forEach { tab ->
                    Tab(
                        selected = selectedTab.key == tab.key,
                        onClick = { selectedTabKey = tab.key },
                        text = {
                            Text(
                                text = tab.title,
                                style = labelSmall(
                                    color = if (selectedTab.key == tab.key) {
                                        MaterialTheme.colorScheme.secondary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }

            if (selectedTab.rows.isEmpty()) {
                Text(
                    text = "No hay registros para esta sección.",
                    style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            } else if (isTablet()) {
                ReportDesktopRows(
                    rows = selectedTab.rows,
                    showAllRows = true,
                    onOpenOrderDetails = if (selectedTab.canOpenOrderDetails) onOpenOrderDetails else null
                )
            } else {
                selectedTab.rows.forEachIndexed { index, row ->
                    ReportMobileRow(
                        index = index + 1,
                        cells = row.cells,
                        onOpenOrderDetails = if (selectedTab.canOpenOrderDetails) {
                            row.orderNumberForDetails()?.let { orderNumber ->
                                { onOpenOrderDetails(orderNumber) }
                            }
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportDesktopRows(
    rows: List<RealTimeReportRow>,
    showAllRows: Boolean,
    onOpenOrderDetails: ((String) -> Unit)? = null,
) {
    val visibleCells = rows.firstOrNull()?.cells.orEmpty().take(MAX_DETAIL_COLUMNS)
    val visibleRows = if (showAllRows) rows else rows.take(MAX_VISIBLE_DETAIL_ROWS)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            visibleCells.forEach { cell ->
                Text(
                    text = cell.label,
                    style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.width(142.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        HorizontalDivider()
        visibleRows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.cells.take(MAX_DETAIL_COLUMNS).forEach { cell ->
                    if (cell.label == "Acciones" && onOpenOrderDetails != null) {
                        val orderNumber = row.orderNumberForDetails()
                        OutlinedButtonM(
                            onClick = { orderNumber?.let(onOpenOrderDetails) },
                            enabled = !orderNumber.isNullOrBlank(),
                            modifier = Modifier.width(142.dp),
                            contentColor = MaterialTheme.colorScheme.secondary,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                        ) {
                            Text(text = "Ver", style = labelSmall(color = MaterialTheme.colorScheme.secondary))
                        }
                    } else {
                        Text(
                            text = cell.value,
                            style = bodyMedium(),
                            modifier = Modifier.width(142.dp),
                            textAlign = if (cell.alignEnd) TextAlign.End else TextAlign.Start,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        if (!showAllRows && rows.size > MAX_VISIBLE_DETAIL_ROWS) {
            Text(
                text = "Mostrando $MAX_VISIBLE_DETAIL_ROWS de ${rows.size} registros.",
                style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}

@Composable
private fun ReportMobileRow(
    index: Int,
    cells: List<RealTimeReportCell>,
    onOpenOrderDetails: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Registro $index",
                style = labelSmall(color = MaterialTheme.colorScheme.secondary)
            )
            cells.take(MAX_DETAIL_COLUMNS).filterNot { it.label == "Acciones" }.forEach { cell ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = cell.label,
                        style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.weight(0.45f)
                    )
                    Text(
                        text = cell.value,
                        style = bodyMedium(),
                        modifier = Modifier.weight(0.55f),
                        textAlign = TextAlign.End,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (onOpenOrderDetails != null) {
                OutlinedButtonM(
                    onClick = onOpenOrderDetails,
                    modifier = Modifier.fillMaxWidth(),
                    contentColor = MaterialTheme.colorScheme.secondary,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                ) {
                    Text(text = "Ver detalles", style = bodyMediumBold(color = MaterialTheme.colorScheme.secondary))
                }
            }
        }
    }
}

@Composable
private fun ReportPaginationSection(
    data: RealTimeReportData,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
) {
    val pagination = data.pagination ?: return
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "${pagination.from}-${pagination.to} de ${pagination.total}",
                style = bodyMediumBold()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButtonM(
                    onClick = onPreviousPage,
                    modifier = Modifier.weight(1f),
                    enabled = pagination.canGoPrevious,
                    contentColor = MaterialTheme.colorScheme.secondary,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                ) {
                    Text(text = "Anterior", style = bodyMediumBold(color = MaterialTheme.colorScheme.secondary))
                }
                ButtonM(
                    onClick = onNextPage,
                    modifier = Modifier.weight(1f),
                    enabled = pagination.canGoNext,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ) {
                    Text(text = "Siguiente", style = bodyMediumBold(color = MaterialTheme.colorScheme.onSecondary))
                }
            }
        }
    }
}

@Composable
private fun ReportExportSection(
    definition: RealTimeReportDefinition,
    exportingFormat: ReportExportFormat?,
    onExport: (ReportExportFormat) -> Unit,
) {
    if (definition.exportFormats.isEmpty()) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionTitle(icon = Icons.Rounded.Description, title = "Exportar")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (definition.exportFormats.contains(ReportExportFormat.XLSX)) {
                    OutlinedButtonM(
                        onClick = { onExport(ReportExportFormat.XLSX) },
                        modifier = Modifier.weight(1f),
                        enabled = exportingFormat == null,
                        contentColor = MaterialTheme.colorScheme.secondary,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                    ) {
                        Text(
                            text = if (exportingFormat == ReportExportFormat.XLSX) "Exportando..." else "Excel",
                            style = bodyMediumBold(color = MaterialTheme.colorScheme.secondary)
                        )
                    }
                }
                if (definition.exportFormats.contains(ReportExportFormat.PDF)) {
                    ButtonM(
                        onClick = { onExport(ReportExportFormat.PDF) },
                        modifier = Modifier.weight(1f),
                        enabled = exportingFormat == null,
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ) {
                        Text(
                            text = if (exportingFormat == ReportExportFormat.PDF) "Exportando..." else "PDF",
                            style = bodyMediumBold(color = MaterialTheme.colorScheme.onSecondary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportLoadingSkeleton() {
    val brush = shimmerBrush()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(3) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = cardContainerColor())
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(18.dp)
                            .background(brush, RoundedCornerShape(6.dp))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .background(brush, RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportEmptyState(title: String, message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = title, style = bodyMediumBold(color = MaterialTheme.colorScheme.secondary))
            Text(
                text = message,
                style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}

@Composable
private fun SectionTitle(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(text = title, style = bodyMediumBold(color = MaterialTheme.colorScheme.secondary))
    }
}

@Composable
private fun SelectablePill(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f)
    ) {
        Text(
            text = text,
            style = labelSmall(
                color = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.secondary
            ),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatusPill(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f)
    ) {
        Text(
            text = text,
            style = labelSmall(color = MaterialTheme.colorScheme.secondary),
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun presetOptions(definition: RealTimeReportDefinition): List<String> {
    return when (definition.key) {
        "cash_flow" -> listOf("next_30_days", "next_7_days", "current_month")
        "expense_aging" -> listOf("all_open", "overdue", "due_next_7_days")
        "financial_comparison" -> listOf("custom")
        "expense_summary",
        "expense_by_account",
        "expense_by_supplier" -> listOf("current_year", "current_month", "last_12_months")
        "recurring_expense_report",
        "operating_margin" -> listOf("last_12_months", "current_year", "current_month")
        else -> listOf("current_month", "current_year", "last_12_months")
    }.let { options -> (listOf(definition.defaultPreset) + options).distinct() }
}

private fun presetLabel(preset: String): String {
    return when (preset) {
        "current_month" -> "Mes actual"
        "current_year" -> "Año actual"
        "last_12_months" -> "Ultimos 12 meses"
        "next_30_days" -> "Prox. 30 dias"
        "next_7_days" -> "Prox. 7 dias"
        "all_open" -> "Abiertos"
        "overdue" -> "Vencidos"
        "due_next_7_days" -> "Vencen 7 dias"
        "custom" -> "Personalizado"
        else -> preset
    }
}

private fun RealTimeReportCategory.icon(): ImageVector {
    return when (this) {
        RealTimeReportCategory.SALES -> Icons.Rounded.TrendingUp
        RealTimeReportCategory.CUSTOMERS -> Icons.Rounded.Person
        RealTimeReportCategory.TAXES -> Icons.Rounded.ReceiptLong
        RealTimeReportCategory.EXPENSES -> Icons.Rounded.Payment
        RealTimeReportCategory.FINANCE -> Icons.Rounded.Business
        RealTimeReportCategory.OPERATIONS -> Icons.Rounded.EventAvailable
    }
}

private fun metricsFor(data: RealTimeReportData): List<RealTimeReportMetric> {
    return when {
        data.definition.isSalesSummary() -> SALES_SUMMARY_METRICS.mapNotNull { rule ->
            data.metrics.firstMatching(rule.aliases)?.copy(label = rule.label)
        }
        data.definition.isSalesByProduct() -> salesByProductMetrics(data)
        data.definition.isSalesByPaymentMethod() -> salesByPaymentMethodMetrics(data)
        data.definition.isSalesByUser() -> salesByUserMetrics(data)
        data.definition.isSalesByBranch() -> salesByBranchMetrics(data)
        data.definition.isSalesAdjustments() -> salesAdjustmentsMetrics(data)
        data.definition.isSalesPendingCollection() -> salesPendingCollectionMetrics(data)
        data.definition.isCustomerReport() -> customerReportMetrics(data)
        data.definition.isSalesTaxSummary() -> salesTaxMetrics(data)
        data.definition.isExpenseTaxSummary() -> expenseTaxMetrics(data)
        data.definition.isExpenseReport() -> expenseReportMetrics(data)
        data.definition.isFinancialOrOperativeReport() -> financialOperativeMetrics(data)
        else -> data.metrics
    }
}

private fun chartSectionsFor(data: RealTimeReportData): List<RealTimeReportChartSection> {
    if (data.definition.isSalesSummary()) return salesSummaryChartSections(data)
    if (data.definition.isSalesByProduct()) return salesByProductChartSections(data)
    if (data.definition.isSalesByPaymentMethod()) {
        return rowAmountChartSections(
            data = data,
            key = "sales_by_payment_method_amount",
            title = "Ventas por metodo de pago",
            labelAliases = PAYMENT_METHOD_ALIASES,
            valueAliases = TOTAL_SALES_ALIASES
        )
    }
    if (data.definition.isSalesByUser()) {
        return rowAmountChartSections(
            data = data,
            key = "sales_by_user_amount",
            title = "Ventas por vendedor",
            labelAliases = USER_ALIASES,
            valueAliases = TOTAL_SALES_ALIASES
        )
    }
    if (data.definition.isSalesByBranch()) {
        return rowAmountChartSections(
            data = data,
            key = "sales_by_branch_amount",
            title = "Ventas por sucursal",
            labelAliases = BRANCH_NAME_ALIASES,
            valueAliases = TOTAL_SALES_ALIASES
        )
    }
    if (data.definition.isSalesAdjustments()) return salesAdjustmentsChartSections(data)
    if (data.definition.isSalesPendingCollection()) return salesPendingCollectionChartSections(data)
    if (data.definition.isCustomerReport()) return customerReportChartSections(data)
    if (data.definition.isSalesTaxSummary()) return salesTaxChartSections(data)
    if (data.definition.isExpenseTaxSummary()) return expenseTaxChartSections(data)
    if (data.definition.isExpenseReport()) return expenseReportChartSections(data)
    if (data.definition.isFinancialOrOperativeReport()) return financialOperativeChartSections(data)

    val explicitCharts = data.chartSections.filter { it.points.isNotEmpty() }
    if (explicitCharts.isNotEmpty()) return explicitCharts

    val rowPoints = data.rows.mapNotNull { row ->
        val label = row.cells.firstOrNull { !it.alignEnd && it.value.isNotBlank() && it.value != "-" }?.value
            ?: row.cells.firstOrNull()?.value
            ?: return@mapNotNull null
        val numericCell = row.cells.firstOrNull { cell ->
            cell.alignEnd && numberFromDisplay(cell.value) != null
        } ?: return@mapNotNull null
        RealTimeReportChartPoint(
            label = label,
            value = numberFromDisplay(numericCell.value) ?: return@mapNotNull null,
            formattedValue = numericCell.value
        )
    }.take(8)
    if (rowPoints.isNotEmpty()) {
        return listOf(
            RealTimeReportChartSection(
                key = "detail",
                title = "Vista comparativa",
                points = rowPoints,
                lines = emptyList()
            )
        )
    }

    val metricPoints = data.metrics.mapNotNull { metric ->
        RealTimeReportChartPoint(
            label = metric.label,
            value = numberFromDisplay(metric.value) ?: return@mapNotNull null,
            formattedValue = metric.value
        )
    }.take(8)
    return if (metricPoints.isEmpty()) {
        emptyList()
    } else {
        listOf(
            RealTimeReportChartSection(
                key = "metrics",
                title = "Indicadores graficados",
                points = metricPoints,
                lines = emptyList()
            )
        )
    }
}

private fun salesSummaryChartSections(data: RealTimeReportData): List<RealTimeReportChartSection> {
    val sections = mutableListOf<RealTimeReportChartSection>()
    val trendPoints = salesSummaryTrendPoints(data)
    if (trendPoints.isNotEmpty()) {
        sections.add(
            RealTimeReportChartSection(
                key = "sales_summary_trend",
                title = "Ventas por fecha",
                points = trendPoints,
                lines = emptyList()
            )
        )
    }

    val collectedPending = salesSummaryCollectedPendingPoints(data)
    if (collectedPending.size == 2) {
        sections.add(
            RealTimeReportChartSection(
                key = "sales_summary_collected_pending",
                title = "Cobrado vs pendiente",
                points = collectedPending,
                lines = emptyList()
            )
        )
    }
    return sections
}

private fun salesSummaryTrendPoints(data: RealTimeReportData): List<RealTimeReportChartPoint> {
    val explicit = data.chartSections
        .firstOrNull { it.points.isNotEmpty() && !it.key.contains("payment") && !it.key.contains("status") }
        ?.points
        .orEmpty()
    val points = explicit.ifEmpty {
        data.rows.mapNotNull { row ->
            val label = row.cells.firstByKeys(DATE_ALIASES)?.value
                ?: row.cells.firstOrNull { !it.alignEnd }?.value
                ?: return@mapNotNull null
            val valueCell = row.cells.firstByKeys(TOTAL_SALES_ALIASES)
                ?: row.cells.firstByKeys(listOf("total", "total_sold", "revenue"))
                ?: return@mapNotNull null
            RealTimeReportChartPoint(
                label = label,
                value = numberFromDisplay(valueCell.value) ?: return@mapNotNull null,
                formattedValue = valueCell.value
            )
        }
    }
    return points.sortedByDescending { dateSortKey(it.label) ?: it.label }.take(MAX_REPORT_CHART_POINTS)
}

private fun salesSummaryCollectedPendingPoints(data: RealTimeReportData): List<RealTimeReportChartPoint> {
    val chargedMetric = data.metrics.firstMatching(CHARGED_ALIASES)
    val pendingMetric = data.metrics.firstMatching(PENDING_ALIASES)
    val charged = chargedMetric?.value?.let(::numberFromDisplay)
        ?: data.rows.sumOfCells(CHARGED_ALIASES).takeIf { it > 0.0 }
    val pending = pendingMetric?.value?.let(::numberFromDisplay)
        ?: data.rows.sumOfCells(PENDING_ALIASES).takeIf { it > 0.0 }
    if (charged == null || pending == null) return emptyList()
    return listOf(
        RealTimeReportChartPoint("Cobrado", charged, chargedMetric?.value ?: formatNumberToMoney(charged.toString())),
        RealTimeReportChartPoint("Pendiente", pending, pendingMetric?.value ?: formatNumberToMoney(pending.toString()))
    )
}

private fun salesByProductMetrics(data: RealTimeReportData): List<RealTimeReportMetric> {
    return PRODUCT_SALES_METRICS.mapNotNull { rule ->
        data.metrics.firstMatching(rule.aliases)?.copy(label = rule.label)
            ?: productSalesDerivedMetric(data, rule)
    }
}

private fun productSalesDerivedMetric(
    data: RealTimeReportData,
    rule: ReportMetricRule,
): RealTimeReportMetric? {
    return when (rule.key) {
        "total_sold" -> data.rows.sumOfCells(PRODUCT_TOTAL_WITH_ITBMS_ALIASES)
            .takeIf { it > 0.0 }
            ?.let { RealTimeReportMetric(rule.key, rule.label, formatNumberToMoney(it.toString())) }
        "items_sold" -> data.rows.sumOfCells(PRODUCT_QUANTITY_ALIASES)
            .takeIf { it > 0.0 }
            ?.let { RealTimeReportMetric(rule.key, rule.label, trimCount(it)) }
        "top_sales_product" -> data.rows.maxLabelBy(
            labelAliases = PRODUCT_NAME_ALIASES,
            valueAliases = PRODUCT_TOTAL_WITH_ITBMS_ALIASES
        )?.let { RealTimeReportMetric(rule.key, rule.label, it) }
        "top_quantity_product" -> data.rows.maxLabelBy(
            labelAliases = PRODUCT_NAME_ALIASES,
            valueAliases = PRODUCT_QUANTITY_ALIASES
        )?.let { RealTimeReportMetric(rule.key, rule.label, it) }
        else -> null
    }
}

private fun salesByProductChartSections(data: RealTimeReportData): List<RealTimeReportChartSection> {
    val points = data.rows.mapNotNull { row ->
        val label = row.cells.firstByKeys(PRODUCT_NAME_ALIASES)?.value?.takeIf { it.isNotBlank() && it != "-" }
            ?: return@mapNotNull null
        val valueCell = row.cells.firstByKeys(PRODUCT_TOTAL_WITH_ITBMS_ALIASES)
            ?: return@mapNotNull null
        val value = numberFromDisplay(valueCell.value) ?: return@mapNotNull null
        RealTimeReportChartPoint(
            label = label,
            value = value,
            formattedValue = valueCell.value
        )
    }
        .sortedByDescending { it.value }
        .take(MAX_REPORT_CHART_POINTS)
        .ifEmpty {
            data.chartSections.firstOrNull { it.points.isNotEmpty() }?.points.orEmpty()
                .sortedByDescending { it.value }
                .take(MAX_REPORT_CHART_POINTS)
        }

    return if (points.isEmpty()) {
        emptyList()
    } else {
        listOf(
            RealTimeReportChartSection(
                key = "sales_by_product_top_sales",
                title = "Top productos por ventas",
                points = points,
                lines = emptyList()
            )
        )
    }
}

private fun salesByPaymentMethodMetrics(data: RealTimeReportData): List<RealTimeReportMetric> {
    return PAYMENT_METHOD_METRICS.mapNotNull { rule ->
        data.metrics.firstMatching(rule.aliases)?.let { metric ->
            metric.copy(
                label = rule.label,
                value = if (rule.key == "main_payment_method") friendlyPaymentMethod(metric.value) else metric.value
            )
        }
            ?: when (rule.key) {
                "main_payment_method" -> data.rows.maxLabelBy(PAYMENT_METHOD_ALIASES, CHARGED_ALIASES + TOTAL_SALES_ALIASES)
                    ?.let { RealTimeReportMetric(rule.key, rule.label, friendlyPaymentMethod(it)) }
                else -> null
            }
    }
}

private fun salesByUserMetrics(data: RealTimeReportData): List<RealTimeReportMetric> {
    return SALES_BY_USER_METRICS.mapNotNull { rule ->
        data.metrics.firstMatching(rule.aliases)?.copy(label = rule.label)
            ?: when (rule.key) {
                "lead_salesman" -> data.rows.maxLabelBy(USER_ALIASES, TOTAL_SALES_ALIASES)
                    ?.let { RealTimeReportMetric(rule.key, rule.label, it) }
                else -> null
            }
    }
}

private fun salesByBranchMetrics(data: RealTimeReportData): List<RealTimeReportMetric> {
    return SALES_BY_BRANCH_METRICS.mapNotNull { rule ->
        data.metrics.firstMatching(rule.aliases)?.copy(label = rule.label)
            ?: when (rule.key) {
                "lead_branch_code" -> data.rows.maxValueBy(BRANCH_CODE_ALIASES, TOTAL_SALES_ALIASES)
                    ?.let { RealTimeReportMetric(rule.key, rule.label, it) }
                else -> null
            }
    }
}

private fun salesAdjustmentsMetrics(data: RealTimeReportData): List<RealTimeReportMetric> {
    return SALES_ADJUSTMENTS_METRICS.mapNotNull { rule ->
        data.metrics.firstMatching(rule.aliases)?.let { metric ->
            metric.copy(
                label = rule.label,
                value = when (rule.key) {
                    "main_type" -> friendlyAdjustmentType(metric.value)
                    else -> metric.value
                }
            )
        }
            ?: when (rule.key) {
                "main_user" -> data.rows.maxLabelBy(USER_ALIASES, ADJUSTMENT_AMOUNT_ALIASES)
                    ?.let { RealTimeReportMetric(rule.key, rule.label, it) }
                "main_type" -> data.rows.maxLabelBy(ADJUSTMENT_TYPE_ALIASES, ADJUSTMENT_AMOUNT_ALIASES)
                    ?.let { RealTimeReportMetric(rule.key, rule.label, friendlyAdjustmentType(it)) }
                else -> null
            }
    }
}

private fun salesPendingCollectionMetrics(data: RealTimeReportData): List<RealTimeReportMetric> {
    return SALES_PENDING_COLLECTION_METRICS.mapNotNull { rule ->
        data.metrics.firstMatching(rule.aliases)?.copy(label = rule.label)
            ?: when (rule.key) {
                "top_pending_client" -> data.rows.maxLabelBy(CLIENT_ALIASES, PENDING_ALIASES)
                    ?.let { RealTimeReportMetric(rule.key, rule.label, it) }
                else -> null
            }
    }
}

private fun rowAmountChartSections(
    data: RealTimeReportData,
    key: String,
    title: String,
    labelAliases: List<String>,
    valueAliases: List<String>,
): List<RealTimeReportChartSection> {
    val points = data.rows.mapNotNull { row ->
        val label = row.cells.firstByKeys(labelAliases)?.value?.takeIf { it.isNotBlank() && it != "-" }
            ?: return@mapNotNull null
        val valueCell = row.cells.firstByKeys(valueAliases) ?: return@mapNotNull null
        val value = numberFromDisplay(valueCell.value) ?: return@mapNotNull null
        RealTimeReportChartPoint(
            label = friendlyChartLabel(label),
            value = value,
            formattedValue = valueCell.value
        )
    }
        .sortedByDescending { it.value }
        .take(MAX_REPORT_CHART_POINTS)

    return if (points.isEmpty()) {
        emptyList()
    } else {
        listOf(RealTimeReportChartSection(key = key, title = title, points = points, lines = emptyList()))
    }
}

private fun salesAdjustmentsChartSections(data: RealTimeReportData): List<RealTimeReportChartSection> {
    val explicit = data.chartSections
        .filter { section ->
            section.points.isNotEmpty() &&
                !section.key.contains("aging", ignoreCase = true) &&
                !section.title.contains("aging", ignoreCase = true)
        }
        .map { section ->
            section.copy(
                points = section.points.map { point ->
                    point.copy(label = friendlyAdjustmentType(point.label))
                }
            )
        }
    if (explicit.isNotEmpty()) return explicit

    return rowAmountChartSections(
        data = data,
        key = "sales_adjustments_by_type",
        title = "Ajustes por tipo",
        labelAliases = ADJUSTMENT_TYPE_ALIASES,
        valueAliases = ADJUSTMENT_AMOUNT_ALIASES
    ).map { section ->
        section.copy(points = section.points.map { it.copy(label = friendlyAdjustmentType(it.label)) })
    }
}

private fun salesPendingCollectionChartSections(data: RealTimeReportData): List<RealTimeReportChartSection> {
    val explicit = data.chartSections
        .filter { it.points.isNotEmpty() }
        .map { section ->
            val isAging = section.key.contains("aging", ignoreCase = true) ||
                section.title.contains("aging", ignoreCase = true)
            section.copy(
                points = section.points.map { point ->
                    if (isAging) point.copy(label = friendlyAgingLabel(point.label)) else point
                }
            )
        }
    if (explicit.isNotEmpty()) return explicit

    return rowAmountChartSections(
        data = data,
        key = "sales_pending_collection_amount",
        title = "Saldos pendientes por cliente",
        labelAliases = CLIENT_ALIASES,
        valueAliases = PENDING_ALIASES
    )
}

private fun customerReportMetrics(data: RealTimeReportData): List<RealTimeReportMetric> {
    return when {
        data.definition.isSalesByCustomer() -> listOfNotNull(
            summaryMetric(data, "total_sold", "Total vendido", TOTAL_SALES_ALIASES),
            summaryMetric(data, "group_count", "Clientes", listOf("group_count", "customer_count", "customers")),
            nestedSummaryMetric(data, "top_by_amount", "Cliente principal", "label", "total"),
            summaryMetric(data, "top5_concentration", "Top 5", listOf("top5_concentration", "top_5_concentration", "top5_sales_percent"))
        )
        data.definition.isCustomerStatement() -> listOfNotNull(
            summaryMetric(data, "total_billed", "Total facturado", listOf("total_billed", "total_invoiced", "total"), "document_count"),
            summaryMetric(data, "total_paid", "Total pagado", listOf("total_paid", "paid")),
            summaryMetric(data, "outstanding_balance", "Saldo pendiente", listOf("outstanding_balance", "pending", "balance")),
            summaryMetric(data, "overdue_balance", "Saldo vencido", listOf("overdue_balance", "overdue")),
            summaryMetric(data, "last_payment_date", "Ultimo pago", listOf("last_payment_date"))
        )
        data.definition.isCustomersWithBalance() -> listOfNotNull(
            summaryMetric(data, "total_pending", "Total pendiente", listOf("total_pending", "pending"), "customer_count"),
            summaryMetric(data, "total_overdue", "Total vencido", listOf("total_overdue", "overdue")),
            summaryMetric(data, "top_customer", "Cliente con mayor saldo", listOf("top_customer", "main_customer")),
            summaryMetric(data, "average_pending_per_customer", "Promedio por cliente", listOf("average_pending_per_customer")),
            summaryMetric(data, "average_days_overdue", "Días promedio vencido", AVERAGE_DAYS_OVERDUE_ALIASES)
        )
        data.definition.isNewCustomers() -> listOfNotNull(
            summaryMetric(data, "new_customer_count", "Clientes nuevos", listOf("new_customer_count")),
            summaryMetric(data, "new_customers_with_purchase", "Con compra", listOf("new_customers_with_purchase")),
            summaryMetric(data, "sales_to_new_customers", "Ventas a nuevos", listOf("sales_to_new_customers")),
            summaryMetric(data, "first_purchase_avg_ticket", "Ticket primera compra", listOf("first_purchase_avg_ticket", "average_ticket")),
            summaryMetric(data, "top_user", "Mejor vendedor", listOf("top_user", "main_user"))
        )
        data.definition.isInactiveCustomers() -> listOfNotNull(
            summaryMetric(data, "inactive_customer_count", "Clientes inactivos", listOf("inactive_customer_count")),
            summaryMetric(data, "inactive_historical_sales", "Ventas historicas", listOf("inactive_historical_sales", "historical_sales")),
            summaryMetric(data, "average_inactive_days", "Días sin compra", listOf("average_inactive_days", "avg_inactive_days")),
            summaryMetric(data, "inactive_with_balance", "Con saldo pendiente", listOf("inactive_with_balance")),
            summaryMetric(data, "top_customer", "Mayor valor inactivo", listOf("top_customer", "main_customer"))
        )
        data.definition.isCustomerRanking() -> listOfNotNull(
            summaryMetric(data, "total_invoiced", "Total facturado", listOf("total_invoiced", "total_billed", "total"))
                ?: rowsSumMetric(data, "total_invoiced", "Total facturado", TOTAL_SALES_ALIASES),
            summaryMetric(data, "outstanding_balance", "Saldo pendiente", listOf("outstanding_balance", "total_pending", "pending"))
                ?: rowsSumMetric(data, "outstanding_balance", "Saldo pendiente", PENDING_ALIASES),
            summaryMetric(data, "top_customer", "Cliente lider", listOf("top_customer", "main_customer")),
            summaryMetric(data, "top10_sales_percent", "Top 10", listOf("top10_sales_percent", "top10_concentration")),
            summaryMetric(data, "average_ticket_per_customer", "Ticket por cliente", listOf("average_ticket_per_customer", "average_ticket"))
        )
        else -> data.metrics
    }
}

private fun customerReportChartSections(data: RealTimeReportData): List<RealTimeReportChartSection> {
    return when {
        data.definition.isSalesByCustomer() -> listOfNotNull(
            chartByKeys(data, listOf("top_by_amount", "bars"), "Top clientes por ventas")
                ?: rowChart(data, "sales_by_customer_top", "Top clientes por ventas", CUSTOMER_NAME_ALIASES, TOTAL_SALES_ALIASES),
            chartByKeys(data, listOf("breakdown"), "Concentración de ingresos")
                ?: totalsDonut(data, "sales_by_customer_concentration_donut", "Concentración de ingresos")
        )
        data.definition.isCustomerStatement() -> listOfNotNull(
            chartByKeys(data, listOf("trend", "timeline"), "Movimientos del cliente")
                ?: rowChart(data, "customer_statement_movements", "Movimientos del cliente", listOf("label", "key", "description", "document"), listOf("amount", "total", "value")),
            billedPaidBalanceDonut(data)
        )
        data.definition.isCustomersWithBalance() -> listOfNotNull(
            chartByKeys(data, listOf("top_by_amount", "bars"), "Top clientes deudores")
                ?: rowChart(data, "customers_with_balance_top", "Top clientes deudores", CUSTOMER_NAME_ALIASES, listOf("total") + PENDING_ALIASES),
            agingDonut(data, "customers_with_balance_aging_donut", "Aging por saldo")
        )
        data.definition.isNewCustomers() -> listOfNotNull(
            chartByKeys(data, listOf("top_by_amount", "bars"), "Clientes nuevos")
                ?: rowChart(data, "new_customers_top", "Clientes nuevos", CUSTOMER_NAME_ALIASES, listOf("total", "total_purchased")),
            chartByKeys(data, listOf("breakdown"), "Ventas a clientes nuevos")
                ?: totalsDonut(data, "new_customers_sales_donut", "Ventas a clientes nuevos")
        )
        data.definition.isInactiveCustomers() -> listOfNotNull(
            chartByKeys(data, listOf("top_by_amount", "bars"), "Valor historico inactivo")
                ?: rowChart(data, "inactive_customers_historical", "Valor historico inactivo", CUSTOMER_NAME_ALIASES, listOf("total", "historical_sales")),
            agingDonut(data, "inactive_customers_buckets_donut", "Buckets de inactividad")
                ?: chartByKeys(data, listOf("breakdown"), "Buckets de inactividad")
        )
        data.definition.isCustomerRanking() -> listOfNotNull(
            chartByKeys(data, listOf("top_by_amount", "bars"), "Top clientes por ventas")
                ?: rowChart(data, "customer_ranking_top", "Top clientes por ventas", CUSTOMER_NAME_ALIASES, TOTAL_SALES_ALIASES),
            customerRankingPendingChart(data)
        )
        else -> emptyList()
    }
}

private fun salesTaxMetrics(data: RealTimeReportData): List<RealTimeReportMetric> {
    val summary = data.rawData.objectValue("summary")
    val grossItbms = summary?.numericField(listOf("gross_itbms_generated"))
        ?: data.rows.sumOfCells(ITBMS_ALIASES).takeIf { it != 0.0 }
        ?: summary?.numericField(listOf("itbms_generated", "generated_itbms"))
    val retained = summary?.numericField(listOf("tax_retained"))
        ?: data.rows.sumOfCells(TAX_RETENTION_ALIASES).takeIf { it != 0.0 }
    val creditNoteItbms = summary?.numericField(listOf("credit_note_itbms"))
        ?: data.rows.creditNoteItbms().takeIf { it != 0.0 }
    val netItbms = summary?.numericField(listOf("net_itbms_payable"))
        ?: data.rows.sumOf { rowNetItbms(it) }.takeIf { it != 0.0 }

    return listOfNotNull(
        summaryMetric(data, "taxable_subtotal", "Subtotal gravado", listOf("taxable_subtotal")),
        grossItbms?.let { RealTimeReportMetric("gross_itbms_generated", "ITBMS bruto", formatMoneyValue(it)) },
        retained?.let { RealTimeReportMetric("tax_retained", "Retenciones", formatNegativeMoneyValue(it)) },
        creditNoteItbms?.let { RealTimeReportMetric("credit_note_itbms", "Notas de crédito", formatNegativeMoneyValue(it)) },
        netItbms?.let { RealTimeReportMetric("net_itbms_payable", "ITBMS neto", formatMoneyValue(it)) },
        summaryMetric(data, "total_invoiced", "Total facturado", listOf("total_invoiced"), "document_count"),
        summaryMetric(data, "exempt_sales", "Ventas exentas", listOf("exempt_sales")),
        summaryMetric(data, "non_taxed_sales", "No gravado", listOf("non_taxed_sales"))
    )
}

private fun expenseTaxMetrics(data: RealTimeReportData): List<RealTimeReportMetric> {
    return listOfNotNull(
        summaryMetric(data, "taxable_subtotal", "Subtotal gravado", listOf("taxable_subtotal")),
        summaryMetric(data, "itbms_total", "ITBMS total", listOf("itbms_total")),
        summaryMetric(data, "total_with_itbms", "Total con ITBMS", listOf("total_with_itbms")),
        summaryMetric(data, "without_itbms_total", "Gastos sin ITBMS", listOf("without_itbms_total"), "document_count"),
        summaryMetric(data, "document_count", "Cantidad de documentos", DOCUMENT_COUNT_ALIASES),
        summaryMetric(data, "average_itbms_per_document", "Promedio ITBMS", listOf("average_itbms_per_document"))
    )
}

private fun salesTaxChartSections(data: RealTimeReportData): List<RealTimeReportChartSection> {
    return listOfNotNull(
        salesTaxNetByPeriodChart(data),
        salesTaxCompositionChart(data)
    )
}

private fun salesTaxNetByPeriodChart(data: RealTimeReportData): RealTimeReportChartSection? {
    val points = data.rows
        .groupBy { row ->
            row.cells.firstByKeys(TAX_PERIOD_LABEL_ALIASES)?.value?.takeIf { it.isNotBlank() && it != "-" }
                ?: "Sin periodo"
        }
        .map { (label, rows) ->
            val value = rows.sumOf(::rowNetItbms)
            RealTimeReportChartPoint(
                label = label,
                value = value,
                formattedValue = formatMoneyValue(value)
            )
        }
        .filter { it.value != 0.0 }
        .sortedByDescending { dateSortKey(it.label) ?: it.label }
        .take(MAX_REPORT_CHART_POINTS)

    return points.takeIf { it.isNotEmpty() }?.let {
        RealTimeReportChartSection(
            key = "sales_tax_net_by_period_vertical",
            title = "ITBMS neto por periodo",
            points = it,
            lines = emptyList()
        )
    }
}

private fun salesTaxCompositionChart(data: RealTimeReportData): RealTimeReportChartSection? {
    val summary = data.rawData.objectValue("summary")
    val grossItbms = summary?.numericField(listOf("gross_itbms_generated"))
        ?: data.rows.sumOfCells(ITBMS_ALIASES).takeIf { it != 0.0 }
        ?: summary?.numericField(listOf("itbms_generated", "generated_itbms"))
    val retained = summary?.numericField(listOf("tax_retained"))
        ?: data.rows.sumOfCells(TAX_RETENTION_ALIASES).takeIf { it != 0.0 }
    val creditNoteItbms = summary?.numericField(listOf("credit_note_itbms"))
        ?: data.rows.creditNoteItbms().takeIf { it != 0.0 }
    val netItbms = summary?.numericField(listOf("net_itbms_payable"))
        ?: data.rows.sumOf { rowNetItbms(it) }.takeIf { it != 0.0 }

    val points = listOfNotNull(
        grossItbms?.let { RealTimeReportChartPoint("ITBMS bruto", it, formatMoneyValue(it)) },
        retained?.let { RealTimeReportChartPoint("Retenciones", -kotlin.math.abs(it), formatNegativeMoneyValue(it)) },
        creditNoteItbms?.let { RealTimeReportChartPoint("Notas de crédito", -kotlin.math.abs(it), formatNegativeMoneyValue(it)) },
        netItbms?.let { RealTimeReportChartPoint("ITBMS neto", it, formatMoneyValue(it)) }
    )

    return points.takeIf { it.isNotEmpty() }?.let {
        RealTimeReportChartSection(
            key = "sales_tax_itbms_composition",
            title = "Composición fiscal del ITBMS",
            points = it,
            lines = emptyList()
        )
    }
}

private fun expenseTaxChartSections(data: RealTimeReportData): List<RealTimeReportChartSection> {
    return listOfNotNull(
        expenseTaxMonthlyComboChart(data),
        chartByArrayRows(
            data = data,
            chartKey = "top_suppliers",
            sectionKey = "expense_tax_top_suppliers",
            title = "Top proveedores por ITBMS",
            labelAliases = listOf("supplier_name", "supplier_ruc"),
            valueAliases = listOf("itbms")
        ),
        chartByKeys(data, listOf("tax_breakdown"), "Desglose fiscal")
            ?: chartByKeys(data, listOf("breakdown"), "Desglose fiscal")
    )
}

private fun expenseTaxMonthlyComboChart(data: RealTimeReportData): RealTimeReportChartSection? {
    val rows = data.chartArray("itbms_by_month")?.mapNotNull { it as? JsonObject }.orEmpty()
    val sourceRows = rows.ifEmpty {
        data.rows.map { row ->
            JsonObject(
                mapOf(
                    "period_label" to JsonPrimitive(row.cells.firstByKeys(TAX_PERIOD_LABEL_ALIASES)?.value ?: "Sin periodo"),
                    "total" to JsonPrimitive(row.cells.firstByKeys(TOTAL_SALES_ALIASES)?.value?.let(::numberFromDisplay) ?: 0.0),
                    "itbms" to JsonPrimitive(row.cells.firstByKeys(ITBMS_ALIASES)?.value?.let(::numberFromDisplay) ?: 0.0)
                )
            )
        }
    }
    val points = sourceRows.mapNotNull { row ->
        val label = row.firstText(TAX_PERIOD_LABEL_ALIASES) ?: "Sin periodo"
        val total = row.numericField(listOf("total")) ?: return@mapNotNull null
        RealTimeReportChartPoint(label, total, formatMoneyValue(total))
    }.take(MAX_REPORT_CHART_POINTS)
    val lines = sourceRows.mapNotNull { row ->
        val label = row.firstText(TAX_PERIOD_LABEL_ALIASES) ?: "Sin periodo"
        val itbms = row.numericField(listOf("itbms")) ?: return@mapNotNull null
        comboLinePayload(label, itbms, formatMoneyValue(itbms))
    }.take(MAX_REPORT_CHART_POINTS)

    return points.takeIf { it.isNotEmpty() }?.let {
        RealTimeReportChartSection(
            key = "expense_tax_itbms_by_month_combo",
            title = "ITBMS por mes",
            points = it,
            lines = lines
        )
    }
}

private fun chartByArrayRows(
    data: RealTimeReportData,
    chartKey: String,
    sectionKey: String,
    title: String,
    labelAliases: List<String>,
    valueAliases: List<String>,
): RealTimeReportChartSection? {
    val points = data.chartArray(chartKey)
        ?.mapNotNull { it as? JsonObject }
        ?.mapNotNull { row ->
            val label = row.firstText(labelAliases) ?: return@mapNotNull null
            val value = row.numericField(valueAliases) ?: return@mapNotNull null
            RealTimeReportChartPoint(label, value, formatMoneyValue(value))
        }
        ?.sortedByDescending { it.value }
        ?.take(10)
        .orEmpty()
    return points.takeIf { it.isNotEmpty() }?.let {
        RealTimeReportChartSection(sectionKey, title, it, emptyList())
    }
}

private fun expenseReportMetrics(data: RealTimeReportData): List<RealTimeReportMetric> {
    return when {
        data.definition.isExpenseSummary() -> listOfNotNull(
            summaryMetric(data, "total_expenses", "Total de gastos", listOf("total_expenses"), "document_count"),
            summaryMetric(data, "subtotal", "Subtotal", listOf("subtotal")),
            summaryMetric(data, "itbms_total", "ITBMS", listOf("itbms_total", "itbms")),
            summaryMetric(data, "average_per_expense", "Promedio por gasto", listOf("average_per_expense"), "highest_period")
        )
        data.definition.isExpenseAging() -> listOfNotNull(
            summaryMetric(data, "total_payable", "Total por pagar", listOf("total_payable")),
            summaryMetric(data, "total_overdue", "Vencido", listOf("total_overdue"), "overdue_document_count"),
            summaryMetric(data, "total_not_due", "No vencido", listOf("total_not_due")),
            summaryMetric(data, "total_partially_paid", "Parcialmente pagado", listOf("total_partially_paid"))
        )
        data.definition.isExpenseByAccount() -> expenseByAccountMetrics(data)
        data.definition.isExpenseBySupplier() -> expenseBySupplierMetrics(data)
        data.definition.isExpenseDetail() -> listOfNotNull(
            summaryMetric(data, "document_count", "Total documentos", DOCUMENT_COUNT_ALIASES),
            summaryMetric(data, "subtotal", "Subtotal", listOf("subtotal")),
            summaryMetric(data, "itbms", "ITBMS", listOf("itbms", "itbms_total")),
            summaryMetric(data, "total", "Total", listOf("total")),
            summaryMetric(data, "total_paid", "Total pagado", listOf("total_paid", "paid")),
            summaryMetric(data, "total_pending", "Total pendiente", listOf("total_pending", "pending"))
        )
        data.definition.isRecurringExpenseReport() -> listOfNotNull(
            summaryMetric(data, "estimated_monthly_recurring_total", "Gasto mensual estimado", listOf("estimated_monthly_recurring_total")),
            summaryMetric(data, "recurring_expense_count", "Gastos recurrentes", listOf("recurring_expense_count")),
            nestedSummaryMetric(data, "top_recurring_supplier", "Proveedor principal", "supplier_name"),
            nestedSummaryMetric(data, "top_recurring_category", "Categoría principal", "category_name")
        )
        else -> data.metrics
    }
}

private fun expenseByAccountMetrics(data: RealTimeReportData): List<RealTimeReportMetric> {
    val summary = data.rawData.objectValue("summary")
    val totals = data.rawData.objectValue("totals")
    val expenseCount = summary?.displayField("expense_count")
    val itemCount = summary?.displayField("item_count")
    val topCategory = summary?.objectValue("top_category")
    val topCategoryName = topCategory?.displayField("name")
    val topCategoryNote = listOfNotNull(
        topCategory?.displayField("code"),
        topCategory?.displayField("total")
    ).joinToString(" · ").takeIf { it.isNotBlank() }

    return listOfNotNull(
        summaryMetric(data, "total_categorized", "Total categorizado", listOf("total_categorized"), "categorized_percent"),
        summaryMetric(data, "total_uncategorized", "Sin categorizar", listOf("total_uncategorized")),
        if (!expenseCount.isNullOrBlank() || !itemCount.isNullOrBlank()) {
            RealTimeReportMetric(
                key = "expense_item_count",
                label = "Gastos / items",
                value = appendNote("${expenseCount ?: "0"} / ${itemCount ?: "0"}", totals?.displayField("total"))
            )
        } else {
            null
        },
        topCategoryName?.let {
            RealTimeReportMetric(
                key = "top_category",
                label = "Cuenta principal",
                value = appendNote(it, topCategoryNote)
            )
        }
    )
}

private fun expenseBySupplierMetrics(data: RealTimeReportData): List<RealTimeReportMetric> {
    val topSupplier = data.rawData.objectValue("summary")?.objectValue("top_supplier")
    val topSupplierName = topSupplier?.displayField("supplier_name")
    val topSupplierNote = listOfNotNull(
        topSupplier?.displayField("supplier_ruc"),
        topSupplier?.displayField("total")
    ).joinToString(" · ").takeIf { it.isNotBlank() }

    return listOfNotNull(
        summaryMetric(data, "total_spent", "Total gastado", listOf("total_spent"), "invoice_count"),
        summaryMetric(data, "supplier_count", "Proveedores", listOf("supplier_count")),
        topSupplierName?.let {
            RealTimeReportMetric(
                key = "top_supplier",
                label = "Proveedor principal",
                value = appendNote(it, topSupplierNote)
            )
        },
        summaryMetric(data, "top_5_concentration_percent", "Concentración top 5", listOf("top_5_concentration_percent", "top5_concentration_percent"))
    )
}

private fun expenseReportChartSections(data: RealTimeReportData): List<RealTimeReportChartSection> {
    return when {
        data.definition.isExpenseSummary() -> listOfNotNull(
            expenseSummaryBarsChart(data),
            expenseSummaryTrendChart(data)
        )
        data.definition.isExpenseAging() -> listOfNotNull(expenseAgingBucketsChart(data))
        data.definition.isExpenseByAccount() -> listOfNotNull(
            chartByArrayRows(data, "categories", "expense_account_categories", "Top cuentas/categorias", listOf("label", "key", "name"), TOTAL_SALES_ALIASES),
            chartByKeys(data, listOf("classifications"), "Clasificaciones")
        )
        data.definition.isExpenseBySupplier() -> listOfNotNull(
            chartByArrayRows(data, "top_suppliers", "expense_supplier_top", "Top proveedores", listOf("supplier_name", "supplier_ruc"), TOTAL_SALES_ALIASES),
            expenseSupplierConcentrationChart(data)
        )
        data.definition.isExpenseDetail() -> emptyList()
        data.definition.isRecurringExpenseReport() -> listOfNotNull(
            recurringFrequencyChart(data),
            chartByArrayRows(data, "suppliers", "recurring_expense_suppliers", "Proveedores", listOf("supplier_name", "label", "supplier_ruc"), TOTAL_SALES_ALIASES)
                ?: rowChart(data, "recurring_expense_suppliers", "Proveedores", listOf("supplier_name", "label", "supplier_ruc"), TOTAL_SALES_ALIASES),
            chartByArrayRows(data, "categories", "recurring_expense_categories", "Categorías", listOf("category_name", "label"), TOTAL_SALES_ALIASES)
                ?: rowChart(data, "recurring_expense_categories", "Categorías", listOf("category_name", "label"), TOTAL_SALES_ALIASES)
        )
        else -> emptyList()
    }
}

private fun expenseSummaryBarsChart(data: RealTimeReportData): RealTimeReportChartSection? {
    val rows = data.chartArray("bars")?.mapNotNull { it as? JsonObject }.orEmpty()
    val sourceRows = rows.ifEmpty { data.rows.map(::rowAsChartObject) }
    val lines = sourceRows.flatMap { row ->
        val label = row.firstText(TAX_PERIOD_LABEL_ALIASES) ?: "Sin periodo"
        listOfNotNull(
            row.numericField(listOf("subtotal"))?.let { groupedChartPayload(label, "Subtotal", it, formatMoneyValue(it)) },
            row.numericField(ITBMS_ALIASES)?.let { groupedChartPayload(label, "ITBMS", it, formatMoneyValue(it)) },
            row.numericField(TOTAL_SALES_ALIASES)?.let { groupedChartPayload(label, "Total", it, formatMoneyValue(it)) }
        )
    }.take(MAX_REPORT_CHART_POINTS * 3)
    val points = sourceRows.mapNotNull { row ->
        val label = row.firstText(TAX_PERIOD_LABEL_ALIASES) ?: "Sin periodo"
        val total = row.numericField(TOTAL_SALES_ALIASES) ?: return@mapNotNull null
        RealTimeReportChartPoint(label, total, formatMoneyValue(total))
    }.take(MAX_REPORT_CHART_POINTS)

    return points.takeIf { it.isNotEmpty() && lines.isNotEmpty() }?.let {
        RealTimeReportChartSection("expense_summary_bars_grouped", "Gastos por periodo", it, lines)
    }
}

private fun expenseSummaryTrendChart(data: RealTimeReportData): RealTimeReportChartSection? {
    val rows = data.chartArray("trend")?.mapNotNull { it as? JsonObject }.orEmpty()
    val points = rows.mapNotNull { row ->
        val label = row.firstText(TAX_PERIOD_LABEL_ALIASES) ?: "Sin periodo"
        val total = row.numericField(TOTAL_SALES_ALIASES) ?: return@mapNotNull null
        RealTimeReportChartPoint(label, total, formatMoneyValue(total))
    }.take(MAX_REPORT_CHART_POINTS)
    val lines = rows.mapNotNull { row ->
        val label = row.firstText(TAX_PERIOD_LABEL_ALIASES) ?: "Sin periodo"
        val count = row.numericField(listOf("count", "document_count")) ?: return@mapNotNull null
        comboLinePayload(label, count, trimCount(count), "Documentos")
    }.take(MAX_REPORT_CHART_POINTS)

    return points.takeIf { it.isNotEmpty() }?.let {
        RealTimeReportChartSection("expense_summary_trend_combo", "Tendencia de gastos", it, lines)
    }
}

private fun expenseAgingBucketsChart(data: RealTimeReportData): RealTimeReportChartSection? {
    val rows = data.chartArray("buckets")?.mapNotNull { it as? JsonObject }
        ?: data.rawData["buckets"].let { buckets ->
            when (buckets) {
                is JsonArray -> buckets.mapNotNull { it as? JsonObject }
                is JsonObject -> buckets.entries.mapNotNull { (bucket, value) ->
                    val amount = (value as? JsonPrimitive)?.doubleOrNull ?: return@mapNotNull null
                    JsonObject(
                        mapOf(
                            "bucket" to JsonPrimitive(bucket),
                            "label" to JsonPrimitive(friendlyAgingLabel(bucket)),
                            "total" to JsonPrimitive(amount),
                            "count" to JsonPrimitive(0)
                        )
                    )
                }
                else -> null
            }
        }
        ?: return null

    val lines = rows.flatMap { row ->
        val label = row.firstText(listOf("label", "bucket"))?.let(::friendlyAgingLabel) ?: "Sin antiguedad"
        listOfNotNull(
            row.numericField(listOf("total", "balance"))?.let { groupedChartPayload(label, "Saldo", it, formatMoneyValue(it)) },
            row.numericField(listOf("count", "document_count"))?.let { groupedChartPayload(label, "Documentos", it, trimCount(it)) }
        )
    }.take(MAX_REPORT_CHART_POINTS * 2)
    val points = rows.mapNotNull { row ->
        val label = row.firstText(listOf("label", "bucket"))?.let(::friendlyAgingLabel) ?: "Sin antiguedad"
        val total = row.numericField(listOf("total", "balance")) ?: return@mapNotNull null
        RealTimeReportChartPoint(label, total, formatMoneyValue(total))
    }.take(MAX_REPORT_CHART_POINTS)

    return points.takeIf { it.isNotEmpty() && lines.isNotEmpty() }?.let {
        RealTimeReportChartSection("expense_aging_buckets_grouped", "Saldos por antiguedad", it, lines)
    }
}

private fun expenseSupplierConcentrationChart(data: RealTimeReportData): RealTimeReportChartSection? {
    val rows = data.chartArray("concentration")?.mapNotNull { it as? JsonObject }.orEmpty()
    val lines = rows.flatMap { row ->
        val label = row.firstText(listOf("label", "supplier_name", "supplier_ruc")) ?: "Sin proveedor"
        listOfNotNull(
            row.numericField(TOTAL_SALES_ALIASES)?.let { groupedChartPayload(label, "Total", it, formatMoneyValue(it)) },
            row.numericField(listOf("percent", "percent_of_total"))?.let { groupedChartPayload(label, "%", it, "${trimCount(it)}%") }
        )
    }.take(MAX_REPORT_CHART_POINTS * 2)
    val points = rows.mapNotNull { row ->
        val label = row.firstText(listOf("label", "supplier_name", "supplier_ruc")) ?: "Sin proveedor"
        val total = row.numericField(TOTAL_SALES_ALIASES) ?: return@mapNotNull null
        RealTimeReportChartPoint(label, total, formatMoneyValue(total))
    }.take(MAX_REPORT_CHART_POINTS)

    return points.takeIf { it.isNotEmpty() && lines.isNotEmpty() }?.let {
        RealTimeReportChartSection("expense_supplier_concentration_grouped", "Concentración", it, lines)
    }
}

private fun recurringFrequencyChart(data: RealTimeReportData): RealTimeReportChartSection? {
    val rows = data.chartArray("frequencies")?.mapNotNull { it as? JsonObject }.orEmpty()
    val lines = rows.flatMap { row ->
        val label = row.firstText(listOf("frequency", "label", "key")) ?: "Sin frecuencia"
        listOfNotNull(
            row.numericField(TOTAL_SALES_ALIASES)?.let { groupedChartPayload(label, "Estimado mensual", it, formatMoneyValue(it)) },
            row.numericField(listOf("count", "pattern_count", "patterns"))?.let { groupedChartPayload(label, "Patrones", it, trimCount(it)) }
        )
    }.take(MAX_REPORT_CHART_POINTS * 2)
    val points = rows.mapNotNull { row ->
        val label = row.firstText(listOf("frequency", "label", "key")) ?: "Sin frecuencia"
        val total = row.numericField(TOTAL_SALES_ALIASES) ?: return@mapNotNull null
        RealTimeReportChartPoint(label, total, formatMoneyValue(total))
    }.take(MAX_REPORT_CHART_POINTS)

    return points.takeIf { it.isNotEmpty() && lines.isNotEmpty() }?.let {
        RealTimeReportChartSection("recurring_expense_frequency_grouped", "Frecuencia", it, lines)
    }
}

private fun financialOperativeMetrics(data: RealTimeReportData): List<RealTimeReportMetric> {
    return when {
        data.definition.isProfitAndLoss() -> listOfNotNull(
            summaryMetric(data, "revenue", "Ingresos", listOf("revenue")),
            summaryMetric(data, "costs", "Costos", listOf("costs")),
            summaryMetric(data, "expenses", "Gastos", listOf("expenses"), "uncategorized_expenses"),
            summaryMetric(data, "gross_profit", "Utilidad bruta", listOf("gross_profit")),
            summaryMetric(data, "operating_profit", "Utilidad operativa", listOf("operating_profit")),
            summaryPercentMetric(data, "operating_margin", "Margen operativo", listOf("operating_margin"))
        )
        data.definition.isFinancialComparison() -> listOfNotNull(
            summaryMetric(data, "current_revenue", "Ingresos actuales", listOf("current_revenue"), "previous_revenue"),
            summaryPercentMetric(data, "revenue_variation", "Variación de ingresos", listOf("revenue_variation"), "revenue_difference"),
            summaryMetric(data, "current_expenses", "Gastos actuales", listOf("current_expenses"), "previous_expenses"),
            summaryPercentMetric(data, "expenses_variation", "Variación de gastos", listOf("expenses_variation")),
            summaryMetric(data, "current_profit", "Utilidad actual", listOf("current_profit"), "previous_profit"),
            summaryPercentMetric(data, "profit_variation", "Variación de utilidad", listOf("profit_variation"))
        )
        data.definition.isOperatingMargin() -> listOfNotNull(
            summaryMetric(data, "revenue", "Ingresos", listOf("revenue")),
            summaryMetric(data, "expenses", "Gastos", listOf("expenses")),
            summaryMetric(data, "operating_profit", "Utilidad operativa", listOf("operating_profit")),
            summaryPercentMetric(data, "operating_margin", "Margen operativo", listOf("operating_margin"), "operating_margin_variation")
        )
        data.definition.isBusinessOverview() -> listOfNotNull(
            summaryMetric(data, "sales", "Ventas", listOf("sales"), "invoice_count"),
            summaryMetric(data, "collections", "Cobros", listOf("collections")),
            summaryMetric(data, "expenses", "Gastos", listOf("expenses")),
            summaryMetric(data, "payments", "Pagos", listOf("payments")),
            summaryMetric(data, "net_cash_flow", "Flujo neto", listOf("net_cash_flow")),
            summaryMetric(data, "operating_profit", "Utilidad operativa", listOf("operating_profit")),
            summaryMetric(data, "accounts_receivable", "CxC", listOf("accounts_receivable")),
            summaryMetric(data, "accounts_payable", "CxP", listOf("accounts_payable"), "new_customers")
        )
        data.definition.isCashFlow() -> listOfNotNull(
            summaryMetric(data, "total_receivable", "Total por cobrar", listOf("total_receivable"), "receivable_count"),
            summaryMetric(data, "overdue_receivable", "Cobros vencidos", listOf("overdue_receivable"), "overdue_receivable_count"),
            summaryMetric(data, "total_payable", "Total por pagar", listOf("total_payable"), "payable_count"),
            summaryMetric(data, "net_cash_flow", "Flujo neto", listOf("net_cash_flow"), "net_cash_flow_with_overdue"),
            summaryMetric(data, "expected_receivable", "Cobros esperados", listOf("expected_receivable")),
            summaryMetric(data, "expected_payable", "Pagos esperados", listOf("expected_payable")),
            summaryMetric(data, "coverage_ratio", "Cobertura", listOf("coverage_ratio"), "coverage_status"),
            summaryMetric(data, "overdue_payable", "Pagos vencidos", listOf("overdue_payable"), "overdue_payable_count")
        )
        else -> data.metrics
    }
}

private fun financialOperativeChartSections(data: RealTimeReportData): List<RealTimeReportChartSection> {
    return when {
        data.definition.isProfitAndLoss() -> listOfNotNull(
            financialTrendChart(data, "profit_and_loss_trend_line", "Tendencia de resultados", listOf("revenue", "expenses", "profit")),
            chartByKeys(data, listOf("breakdown"), "Composición financiera")
                ?: chartByKeys(data, listOf("top_expenses"), "Composición financiera")
        )
        data.definition.isFinancialComparison() -> listOfNotNull(
            financialComparisonActualPreviousChart(data),
            rowChart(data, "financial_comparison_variation", "Variación porcentual", FINANCIAL_LABEL_ALIASES, VARIATION_PERCENT_ALIASES)
        )
        data.definition.isOperatingMargin() -> listOfNotNull(
            operatingMarginTrendChart(data),
            operatingMarginDonut(data)
        )
        data.definition.isBusinessOverview() -> listOfNotNull(
            businessOverviewIndicatorsChart(data),
            chartByArrayRows(data, "top_customers", "business_overview_top_customers", "Clientes principales", listOf("label", "key", "name"), TOTAL_SALES_ALIASES + CURRENT_VALUE_ALIASES)
                ?: chartByKeys(data, listOf("breakdown"), "Clientes principales")
        )
        data.definition.isCashFlow() -> listOfNotNull(cashFlowTimelineChart(data))
        else -> emptyList()
    }
}

private fun financialTrendChart(
    data: RealTimeReportData,
    key: String,
    title: String,
    seriesKeys: List<String>,
): RealTimeReportChartSection? {
    val rows = data.chartArray("trend")?.mapNotNull { it as? JsonObject }.orEmpty()
        .ifEmpty { data.rows.map(::rowAsChartObject) }
    val lines = rows.flatMap { row ->
        val label = row.firstText(FINANCIAL_LABEL_ALIASES) ?: "Sin periodo"
        seriesKeys.mapNotNull { seriesKey ->
            val value = row.numericField(listOf(seriesKey)) ?: return@mapNotNull null
            groupedChartPayload(label, friendlyFinancialSeries(seriesKey), value, formatMoneyValue(value))
        }
    }.take(MAX_REPORT_CHART_POINTS * seriesKeys.size)
    val points = rows.mapNotNull { row ->
        val label = row.firstText(FINANCIAL_LABEL_ALIASES) ?: "Sin periodo"
        val value = row.numericField(listOf(seriesKeys.last())) ?: return@mapNotNull null
        RealTimeReportChartPoint(label, value, formatMoneyValue(value))
    }.take(MAX_REPORT_CHART_POINTS)
    return points.takeIf { it.isNotEmpty() && lines.isNotEmpty() }?.let {
        RealTimeReportChartSection(key, title, it, lines)
    }
}

private fun financialComparisonActualPreviousChart(data: RealTimeReportData): RealTimeReportChartSection? {
    val lines = data.rows.flatMap { row ->
        val label = row.cells.firstByKeys(FINANCIAL_LABEL_ALIASES)?.value?.let(::friendlyReportLabel) ?: "Sin métrica"
        listOfNotNull(
            row.cells.firstByKeys(CURRENT_VALUE_ALIASES)?.value?.let(::numberFromDisplay)
                ?.let { groupedChartPayload(label, "Actual", it, formatMoneyValue(it)) },
            row.cells.firstByKeys(PREVIOUS_VALUE_ALIASES)?.value?.let(::numberFromDisplay)
                ?.let { groupedChartPayload(label, "Anterior", it, formatMoneyValue(it)) }
        )
    }.take(MAX_REPORT_CHART_POINTS * 2)
    val points = data.rows.mapNotNull { row ->
        val label = row.cells.firstByKeys(FINANCIAL_LABEL_ALIASES)?.value?.let(::friendlyReportLabel) ?: "Sin métrica"
        val value = row.cells.firstByKeys(CURRENT_VALUE_ALIASES)?.value?.let(::numberFromDisplay) ?: return@mapNotNull null
        RealTimeReportChartPoint(label, value, formatMoneyValue(value))
    }.take(MAX_REPORT_CHART_POINTS)
    return points.takeIf { it.isNotEmpty() && lines.isNotEmpty() }?.let {
        RealTimeReportChartSection("financial_comparison_current_previous_grouped", "Actual vs anterior", it, lines)
    }
}

private fun operatingMarginTrendChart(data: RealTimeReportData): RealTimeReportChartSection? {
    val rows = data.chartArray("trend")?.mapNotNull { it as? JsonObject }.orEmpty()
        .ifEmpty { data.rows.map(::rowAsChartObject) }
    val points = rows.mapNotNull { row ->
        val label = row.firstText(FINANCIAL_LABEL_ALIASES) ?: "Sin periodo"
        val value = row.numericField(OPERATING_MARGIN_ALIASES) ?: return@mapNotNull null
        RealTimeReportChartPoint(label, value, percentDisplay(value))
    }.take(MAX_REPORT_CHART_POINTS)
    return points.takeIf { it.isNotEmpty() }?.let {
        RealTimeReportChartSection("operating_margin_trend_line", "Margen por periodo", it, emptyList())
    }
}

private fun operatingMarginDonut(data: RealTimeReportData): RealTimeReportChartSection? {
    val summary = data.rawData.objectValue("summary") ?: return null
    val points = listOfNotNull(
        summary.numericField(listOf("revenue"))?.let { RealTimeReportChartPoint("Ingresos", it, formatMoneyValue(it)) },
        summary.numericField(listOf("expenses"))?.let { RealTimeReportChartPoint("Gastos", it, formatMoneyValue(it)) },
        summary.numericField(listOf("operating_profit"))?.let { RealTimeReportChartPoint("Utilidad operativa", it, formatMoneyValue(it)) }
    )
    return points.takeIf { it.isNotEmpty() }?.let {
        RealTimeReportChartSection("operating_margin_income_expenses_donut", "Ingresos vs gastos", it, emptyList())
    }
}

private fun businessOverviewIndicatorsChart(data: RealTimeReportData): RealTimeReportChartSection? {
    val points = data.rows.mapNotNull { row ->
        val key = row.cells.firstByKeys(listOf("key"))?.value.orEmpty()
        if (isCountOnlyBusinessOverviewKey(key)) return@mapNotNull null
        val value = row.cells.firstByKeys(CURRENT_VALUE_ALIASES)?.value?.let(::numberFromDisplay) ?: return@mapNotNull null
        val label = row.cells.firstByKeys(FINANCIAL_LABEL_ALIASES)?.value?.let(::friendlyReportLabel) ?: key.ifBlank { "Indicador" }
        RealTimeReportChartPoint(label, value, formatMoneyValue(value))
    }.take(MAX_REPORT_CHART_POINTS)
    return points.takeIf { it.isNotEmpty() }?.let {
        RealTimeReportChartSection("business_overview_indicators", "Principales indicadores", it, emptyList())
    }
}

private fun cashFlowTimelineChart(data: RealTimeReportData): RealTimeReportChartSection? {
    val rows = (data.rawData["timeline"] as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
    val lines = rows.flatMap { row ->
        val label = row.firstText(listOf("period_label", "period_key", "label")) ?: "Sin periodo"
        listOfNotNull(
            row.numericField(listOf("receivables"))?.let { groupedChartPayload(label, "Cobros", it, formatMoneyValue(it)) },
            row.numericField(listOf("payables"))?.let { groupedChartPayload(label, "Pagos", -kotlin.math.abs(it), formatNegativeMoneyValue(it)) },
            row.numericField(listOf("net"))?.let { groupedChartPayload(label, "Neto", it, formatMoneyValue(it)) }
        )
    }.take(MAX_REPORT_CHART_POINTS * 3)
    val points = rows.mapNotNull { row ->
        val label = row.firstText(listOf("period_label", "period_key", "label")) ?: "Sin periodo"
        val value = row.numericField(listOf("net")) ?: row.numericField(listOf("receivables")) ?: return@mapNotNull null
        RealTimeReportChartPoint(label, value, formatMoneyValue(value))
    }.take(MAX_REPORT_CHART_POINTS)
    return points.takeIf { it.isNotEmpty() && lines.isNotEmpty() }?.let {
        RealTimeReportChartSection("cash_flow_timeline_grouped", "Flujo por periodo", it, lines)
    }
}

private fun cashFlowTabs(data: RealTimeReportData): List<CashFlowDetailTab> {
    return listOf(
        CashFlowDetailTab(
            key = CASH_FLOW_SUMMARY_TAB_KEY,
            title = "Resumen",
            rows = cashFlowSummaryRows(data),
            canOpenOrderDetails = false
        ),
        CashFlowDetailTab(
            key = "detail",
            title = "Detalle",
            rows = data.rows.map { row -> cashFlowDetailRow(row) }.filter { it.cells.isNotEmpty() },
            canOpenOrderDetails = true
        ),
        CashFlowDetailTab(
            key = "charges",
            title = "Cobros",
            rows = cashFlowRowsFromArrays(data, listOf("charges", "receivables", "collections", "accounts_receivable", "receivable_documents")),
            canOpenOrderDetails = true
        ),
        CashFlowDetailTab(
            key = "payments",
            title = "Pagos",
            rows = cashFlowRowsFromArrays(data, listOf("payments", "payables", "accounts_payable", "payable_documents")),
            canOpenOrderDetails = true
        ),
        CashFlowDetailTab(
            key = "aging",
            title = "Antigüedad",
            rows = cashFlowAgingRows(data),
            canOpenOrderDetails = false
        )
    )
}

private fun cashFlowSummaryRows(data: RealTimeReportData): List<RealTimeReportRow> {
    val summaryRows = data.rawData.objectValue("summary")?.entries
        ?.map { (key, value) ->
            RealTimeReportRow(
                listOf(
                    RealTimeReportCell("label", "Métrica", friendlyReportLabel(key), false),
                    RealTimeReportCell(key, "Valor", friendlyReportValue(key, value), shouldAlignReportValue(key, value))
                )
            )
        }
        .orEmpty()

    return summaryRows.ifEmpty {
        metricsFor(data).map { metric ->
            RealTimeReportRow(
                listOf(
                    RealTimeReportCell(metric.key, "Métrica", friendlyReportLabel(metric.label), false),
                    RealTimeReportCell(metric.key, "Valor", friendlyReportValue(metric.key, metric.value), true)
                )
            )
        }
    }
}

private fun cashFlowRowsFromArrays(
    data: RealTimeReportData,
    keys: List<String>,
): List<RealTimeReportRow> {
    return keys.firstNotNullOfOrNull { key ->
        data.rawData[key].asRowsFromJsonArray()
            ?.map { row -> cashFlowDetailRow(row) }
            ?.filter { it.cells.isNotEmpty() }
            ?.takeIf { it.isNotEmpty() }
    }.orEmpty()
}

private fun cashFlowAgingRows(data: RealTimeReportData): List<RealTimeReportRow> {
    val source = data.rawData["aging"] ?: data.rawData["buckets"] ?: return emptyList()
    return when (source) {
        is JsonArray -> source.mapNotNull { element ->
            val row = element.asReportRowOrNull() ?: return@mapNotNull null
            row.copy(
                cells = row.cells.map { cell ->
                    when (cell.key.lowercase()) {
                        "bucket", "bucket_label", "aging_bucket", "status" -> cell.copy(value = friendlyReportLabel(cell.value))
                        else -> cell
                    }
                }
            )
        }
        is JsonObject -> source.entries.map { (bucket, value) ->
            val cells = mutableListOf(
                RealTimeReportCell(
                    key = "bucket",
                    label = "Antigüedad",
                    value = friendlyReportLabel(bucket),
                    alignEnd = false
                )
            )
            if (value is JsonObject) {
                value.entries.forEach { (key, child) ->
                    cells.add(
                        RealTimeReportCell(
                            key = key,
                            label = friendlyReportLabel(key),
                            value = friendlyReportValue(key, child),
                            alignEnd = shouldAlignReportValue(key, child)
                        )
                    )
                }
            } else {
                cells.add(
                    RealTimeReportCell(
                        key = "total",
                        label = "Total",
                        value = friendlyReportValue("total", value),
                        alignEnd = shouldAlignReportValue("total", value)
                    )
                )
            }
            RealTimeReportRow(cells)
        }
        else -> emptyList()
    }
}

private fun JsonElement?.asRowsFromJsonArray(): List<RealTimeReportRow>? {
    return (this as? JsonArray)?.mapNotNull { element -> element.asReportRowOrNull() }
}

private fun JsonElement.asReportRowOrNull(): RealTimeReportRow? {
    val obj = this as? JsonObject ?: return null
    return RealTimeReportRow(
        cells = obj.entries.map { (key, value) ->
            RealTimeReportCell(
                key = key,
                label = friendlyReportLabel(key),
                value = friendlyReportValue(key, value),
                alignEnd = shouldAlignReportValue(key, value)
            )
        }
    )
}

private fun RealTimeReportRow.orderNumberForDetails(): String? {
    return cells.firstByKeys(DOCUMENT_NUMBER_ALIASES + listOf("order_number", "order_no", "document_number", "document_no", "document"))
        ?.value
        ?.takeIf { it.isNotBlank() && it != "-" && !it.equals("Sin documento", ignoreCase = true) }
}

private fun rowsFor(data: RealTimeReportData): List<RealTimeReportRow> {
    return when {
        data.definition.isSalesSummary() -> data.rows
            .sortedByDescending { row -> row.cells.firstByKeys(DATE_ALIASES)?.value?.let(::dateSortKey).orEmpty() }
            .map { row ->
                curatedRow(row, SALES_SUMMARY_DETAIL_COLUMNS)
            }
            .filter { it.cells.isNotEmpty() }
        data.definition.isSalesByProduct() -> data.rows.map { row -> curatedRow(row, PRODUCT_SALES_DETAIL_COLUMNS) }
            .filter { it.cells.isNotEmpty() }
        data.definition.isSalesByPaymentMethod() -> data.rows.map { row ->
            curatedRow(row, PAYMENT_METHOD_DETAIL_COLUMNS) { column, cell ->
                if (column.label == "Método de pago") friendlyPaymentMethod(cell.value) else cell.value
            }
        }
            .filter { it.cells.isNotEmpty() }
        data.definition.isSalesByUser() -> data.rows.map { row -> curatedRow(row, SALES_BY_USER_DETAIL_COLUMNS) }
            .filter { it.cells.isNotEmpty() }
        data.definition.isSalesByBranch() -> data.rows.map { row -> curatedRow(row, SALES_BY_BRANCH_DETAIL_COLUMNS) }
            .filter { it.cells.isNotEmpty() }
        data.definition.isSalesAdjustments() -> data.rows.map { row ->
            curatedRow(row, SALES_ADJUSTMENTS_DETAIL_COLUMNS) { column, cell ->
                if (column.label == "Tipo") friendlyAdjustmentType(cell.value) else cell.value
            }
        }
            .filter { it.cells.isNotEmpty() }
        data.definition.isSalesPendingCollection() -> data.rows.map { row ->
            curatedRow(row, SALES_PENDING_COLLECTION_DETAIL_COLUMNS)
        }
            .filter { it.cells.isNotEmpty() }
        data.definition.isSalesByCustomer() -> data.rows.map { row -> curatedRow(row, SALES_BY_CUSTOMER_DETAIL_COLUMNS) }
            .filter { it.cells.isNotEmpty() }
        data.definition.isCustomerStatement() -> data.rows.map { row ->
            curatedRow(row, CUSTOMER_STATEMENT_DETAIL_COLUMNS) { column, cell ->
                if (column.label == "Tipo") friendlyMovementType(cell.value) else cell.value
            }
        }
            .filter { it.cells.isNotEmpty() }
        data.definition.isCustomersWithBalance() -> data.rows.map { row -> curatedRow(row, CUSTOMERS_WITH_BALANCE_DETAIL_COLUMNS) }
            .filter { it.cells.isNotEmpty() }
        data.definition.isNewCustomers() -> data.rows.map { row -> curatedRow(row, NEW_CUSTOMERS_DETAIL_COLUMNS) }
            .filter { it.cells.isNotEmpty() }
        data.definition.isInactiveCustomers() -> data.rows.map { row -> curatedRow(row, INACTIVE_CUSTOMERS_DETAIL_COLUMNS) }
            .filter { it.cells.isNotEmpty() }
        data.definition.isCustomerRanking() -> data.rows.map { row -> curatedRow(row, CUSTOMER_RANKING_DETAIL_COLUMNS) }
            .filter { it.cells.isNotEmpty() }
        data.definition.isSalesTaxSummary() -> data.rows.map { row ->
            salesTaxDetailRow(row)
        }
            .filter { it.cells.isNotEmpty() }
        data.definition.isExpenseTaxSummary() -> data.rows.map { row -> curatedRow(row, EXPENSE_TAX_DETAIL_COLUMNS) }
            .filter { it.cells.isNotEmpty() }
        data.definition.isExpenseSummary() -> data.rows.map { row -> curatedRow(row, EXPENSE_SUMMARY_DETAIL_COLUMNS) }
            .filter { it.cells.isNotEmpty() }
        data.definition.isExpenseAging() -> data.rows.map { row ->
            curatedRow(row, EXPENSE_AGING_DETAIL_COLUMNS) { column, cell ->
                if (column.label == "Antigüedad") friendlyAgingLabel(cell.value) else if (column.label == "Estado") friendlyPaymentStatus(cell.value) else cell.value
            }
        }
            .filter { it.cells.isNotEmpty() }
        data.definition.isExpenseByAccount() -> data.rows.map { row -> expenseAccountDetailRow(row) }
            .filter { it.cells.isNotEmpty() }
        data.definition.isExpenseBySupplier() -> data.rows.map { row -> curatedRow(row, EXPENSE_BY_SUPPLIER_DETAIL_COLUMNS) }
            .filter { it.cells.isNotEmpty() }
        data.definition.isExpenseDetail() -> data.rows.map { row -> expenseDetailRow(row) }
            .filter { it.cells.isNotEmpty() }
        data.definition.isRecurringExpenseReport() -> data.rows.map { row -> recurringExpenseDetailRow(row) }
            .filter { it.cells.isNotEmpty() }
        data.definition.isProfitAndLoss() -> data.rows.map { row -> profitAndLossDetailRow(row) }
            .filter { it.cells.isNotEmpty() }
        data.definition.isFinancialComparison() -> data.rows.map { row ->
            curatedRow(row, FINANCIAL_COMPARISON_DETAIL_COLUMNS) { column, cell ->
                if (column.label == "Métrica") friendlyReportLabel(cell.value) else cell.value
            }
        }
            .filter { it.cells.isNotEmpty() }
        data.definition.isOperatingMargin() -> data.rows.map { row -> operatingMarginDetailRow(row) }
            .filter { it.cells.isNotEmpty() }
        data.definition.isBusinessOverview() -> data.rows.map { row -> businessOverviewDetailRow(row) }
            .filter { it.cells.isNotEmpty() }
        data.definition.isCashFlow() -> data.rows.map { row -> cashFlowDetailRow(row) }
            .filter { it.cells.isNotEmpty() }
        else -> data.rows
    }
}

private fun curatedRow(
    row: RealTimeReportRow,
    columns: List<ReportColumnRule>,
    transform: (ReportColumnRule, RealTimeReportCell) -> String = { _, cell -> cell.value },
): RealTimeReportRow {
    return RealTimeReportRow(
        cells = columns.mapNotNull { column ->
            val existing = row.cells.firstByKeys(column.aliases)
            when {
                existing != null -> existing.copy(label = column.label, value = transform(column, existing))
                column.defaultValue != null -> RealTimeReportCell(
                    key = column.aliases.firstOrNull().orEmpty(),
                    label = column.label,
                    value = column.defaultValue,
                    alignEnd = false
                )
                else -> null
            }
        }
    )
}

private fun salesTaxDetailRow(row: RealTimeReportRow): RealTimeReportRow {
    val cells = SALES_TAX_DETAIL_COLUMNS.mapNotNull { column ->
        val existing = row.cells.firstByKeys(column.aliases)
        when {
            existing != null -> existing.copy(
                label = column.label,
                value = when (column.label) {
                    "Tipo" -> friendlyTaxDocumentType(existing.value)
                    "Retención" -> negativeMoneyDisplay(existing.value)
                    else -> existing.value
                }
            )
            column.label == "ITBMS neto" -> RealTimeReportCell(
                key = NET_ITBMS_ALIASES.first(),
                label = column.label,
                value = formatMoneyValue(rowNetItbms(row)),
                alignEnd = true
            )
            column.defaultValue != null -> RealTimeReportCell(
                key = column.aliases.firstOrNull().orEmpty(),
                label = column.label,
                value = column.defaultValue,
                alignEnd = false
            )
            else -> null
        }
    }
    return RealTimeReportRow(cells)
}

private fun expenseAccountDetailRow(row: RealTimeReportRow): RealTimeReportRow {
    val base = curatedRow(row, EXPENSE_BY_ACCOUNT_DETAIL_COLUMNS)
    return base.copy(
        cells = base.cells.map { cell ->
            if (cell.label == "Cuenta") {
                cell.copy(value = appendNote(cell.value, row.cells.firstByKeys(listOf("code"))?.value))
            } else {
                cell
            }
        }
    )
}

private fun expenseDetailRow(row: RealTimeReportRow): RealTimeReportRow {
    val base = curatedRow(row, EXPENSE_DETAIL_COLUMNS)
    return base.copy(
        cells = base.cells.map { cell ->
            when (cell.label) {
                "Categoría" -> cell.copy(value = appendNote(cell.value, row.cells.firstByKeys(listOf("categorization_status"))?.value?.let(::friendlyReportLabel)))
                "Acciones" -> cell.copy(value = friendlyActions(cell.value))
                else -> cell
            }
        }
    )
}

private fun recurringExpenseDetailRow(row: RealTimeReportRow): RealTimeReportRow {
    val base = curatedRow(row, RECURRING_EXPENSE_DETAIL_COLUMNS)
    return base.copy(
        cells = base.cells.map { cell ->
            when (cell.label) {
                "Proveedor" -> cell.copy(value = appendNote(cell.value, row.cells.firstByKeys(listOf("supplier_ruc"))?.value))
                "Frecuencia" -> cell.copy(value = appendNote(friendlyFrequency(cell.value), row.cells.firstByKeys(listOf("occurrence_count"))?.value))
                "Ultimo gasto" -> cell.copy(value = appendNote(cell.value, row.cells.firstByKeys(listOf("last_expense_date"))?.value))
                "Acciones" -> cell.copy(value = friendlyActions(cell.value))
                else -> cell
            }
        }
    )
}

private fun profitAndLossDetailRow(row: RealTimeReportRow): RealTimeReportRow {
    val key = row.cells.firstByKeys(listOf("key"))?.value.orEmpty()
    val base = curatedRow(row, PROFIT_AND_LOSS_DETAIL_COLUMNS)
    return base.copy(
        cells = base.cells.map { cell ->
            when {
                cell.label == "Concepto" -> cell.copy(value = friendlyReportLabel(cell.value))
                cell.label == "Actual" && key == "operating_margin" -> {
                    cell.copy(value = row.cells.firstByKeys(OPERATING_MARGIN_ALIASES)?.value?.let(::percentDisplayFromString) ?: cell.value)
                }
                else -> cell
            }
        }
    )
}

private fun operatingMarginDetailRow(row: RealTimeReportRow): RealTimeReportRow {
    val base = curatedRow(row, OPERATING_MARGIN_DETAIL_COLUMNS)
    return base.copy(
        cells = base.cells.map { cell ->
            if (cell.label == "Margen") {
                cell.copy(value = percentDisplayFromString(cell.value))
            } else {
                cell
            }
        }
    )
}

private fun businessOverviewDetailRow(row: RealTimeReportRow): RealTimeReportRow {
    val key = row.cells.firstByKeys(listOf("key"))?.value.orEmpty()
    val countOnly = isCountOnlyBusinessOverviewKey(key)
    val label = row.cells.firstByKeys(FINANCIAL_LABEL_ALIASES)?.value?.let(::friendlyReportLabel) ?: key.ifBlank { "Indicador" }
    val current = if (countOnly) {
        row.cells.firstByKeys(listOf("count"))?.value ?: row.cells.firstByKeys(CURRENT_VALUE_ALIASES)?.value ?: "-"
    } else {
        row.cells.firstByKeys(CURRENT_VALUE_ALIASES)?.value ?: "-"
    }
    val count = if (countOnly) "-" else row.cells.firstByKeys(listOf("count"))?.value ?: "-"
    return RealTimeReportRow(
        listOf(
            RealTimeReportCell("label", "Indicador", label, false),
            RealTimeReportCell("current", "Actual", current, !countOnly),
            RealTimeReportCell("count", "Cantidad", count, false)
        )
    )
}

private fun cashFlowDetailRow(row: RealTimeReportRow): RealTimeReportRow {
    val base = curatedRow(row, CASH_FLOW_DETAIL_COLUMNS) { column, cell ->
        when (column.label) {
            "Tipo" -> friendlyCashFlowType(cell.value)
            "Cliente/proveedor" -> friendlyPartyName(cell.value)
            "Estado" -> friendlyCashFlowStatus(cell.value)
            "Acciones" -> "Ver detalles"
            "Documento" -> cell.value
            else -> cell.value
        }
    }
    return if (base.cells.any { it.label == "Acciones" }) {
        base
    } else {
        base.copy(
            cells = base.cells + RealTimeReportCell("actions", "Acciones", "Ver detalles", false)
        )
    }
}

private fun numberFromDisplay(value: String): Double? {
    val numeric = value
        .replace("%", "")
        .replace(Regex("[^0-9,.-]"), "")
        .takeIf { it.any(Char::isDigit) }
        ?: return null
    val normalized = when {
        numeric.contains(",") && numeric.contains(".") -> numeric.replace(",", "")
        numeric.contains(",") -> numeric.replace(",", ".")
        else -> numeric
    }
    return normalized.toDoubleOrNull()
}

private fun trimCount(value: Double): String {
    return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}

private fun RealTimeReportDefinition.isSalesSummary(): Boolean = key == SALES_SUMMARY_REPORT_KEY

private fun RealTimeReportDefinition.isSalesByProduct(): Boolean = key == SALES_BY_PRODUCT_REPORT_KEY

private fun RealTimeReportDefinition.isSalesByPaymentMethod(): Boolean = key == SALES_BY_PAYMENT_METHOD_REPORT_KEY

private fun RealTimeReportDefinition.isSalesByUser(): Boolean = key == SALES_BY_USER_REPORT_KEY

private fun RealTimeReportDefinition.isSalesByBranch(): Boolean = key == SALES_BY_BRANCH_REPORT_KEY

private fun RealTimeReportDefinition.isSalesAdjustments(): Boolean = key == SALES_ADJUSTMENTS_REPORT_KEY

private fun RealTimeReportDefinition.isSalesPendingCollection(): Boolean = key == SALES_PENDING_COLLECTION_REPORT_KEY

private fun RealTimeReportDefinition.isSalesByCustomer(): Boolean = key == SALES_BY_CUSTOMER_REPORT_KEY

private fun RealTimeReportDefinition.isCustomerStatement(): Boolean = key == CUSTOMER_STATEMENT_REPORT_KEY

private fun RealTimeReportDefinition.isCustomersWithBalance(): Boolean = key == CUSTOMERS_WITH_BALANCE_REPORT_KEY

private fun RealTimeReportDefinition.isNewCustomers(): Boolean = key == NEW_CUSTOMERS_REPORT_KEY

private fun RealTimeReportDefinition.isInactiveCustomers(): Boolean = key == INACTIVE_CUSTOMERS_REPORT_KEY

private fun RealTimeReportDefinition.isCustomerRanking(): Boolean = key == CUSTOMER_RANKING_REPORT_KEY

private fun RealTimeReportDefinition.isSalesTaxSummary(): Boolean = key == SALES_TAX_SUMMARY_REPORT_KEY

private fun RealTimeReportDefinition.isExpenseTaxSummary(): Boolean = key == EXPENSE_TAX_SUMMARY_REPORT_KEY

private fun RealTimeReportDefinition.isExpenseSummary(): Boolean = key == EXPENSE_SUMMARY_REPORT_KEY

private fun RealTimeReportDefinition.isExpenseAging(): Boolean = key == EXPENSE_AGING_REPORT_KEY

private fun RealTimeReportDefinition.isExpenseByAccount(): Boolean = key == EXPENSE_BY_ACCOUNT_REPORT_KEY

private fun RealTimeReportDefinition.isExpenseBySupplier(): Boolean = key == EXPENSE_BY_SUPPLIER_REPORT_KEY

private fun RealTimeReportDefinition.isExpenseDetail(): Boolean = key == EXPENSE_DETAIL_REPORT_KEY

private fun RealTimeReportDefinition.isRecurringExpenseReport(): Boolean = key == RECURRING_EXPENSE_REPORT_KEY

private fun RealTimeReportDefinition.isProfitAndLoss(): Boolean = key == PROFIT_AND_LOSS_REPORT_KEY

private fun RealTimeReportDefinition.isFinancialComparison(): Boolean = key == FINANCIAL_COMPARISON_REPORT_KEY

private fun RealTimeReportDefinition.isOperatingMargin(): Boolean = key == OPERATING_MARGIN_REPORT_KEY

private fun RealTimeReportDefinition.isBusinessOverview(): Boolean = key == BUSINESS_OVERVIEW_REPORT_KEY

private fun RealTimeReportDefinition.isCashFlow(): Boolean = key == CASH_FLOW_REPORT_KEY

private fun RealTimeReportDefinition.isFinancialOrOperativeReport(): Boolean {
    return isProfitAndLoss() ||
        isFinancialComparison() ||
        isOperatingMargin() ||
        isBusinessOverview() ||
        isCashFlow()
}

private fun RealTimeReportDefinition.isExpenseReport(): Boolean {
    return isExpenseSummary() ||
        isExpenseAging() ||
        isExpenseByAccount() ||
        isExpenseBySupplier() ||
        isExpenseDetail() ||
        isRecurringExpenseReport()
}

private fun RealTimeReportDefinition.isCustomerReport(): Boolean {
    return isSalesByCustomer() ||
        isCustomerStatement() ||
        isCustomersWithBalance() ||
        isNewCustomers() ||
        isInactiveCustomers() ||
        isCustomerRanking()
}

private fun List<RealTimeReportMetric>.firstMatching(aliases: List<String>): RealTimeReportMetric? {
    return firstOrNull { metric -> aliases.any { alias -> metric.key.equals(alias, ignoreCase = true) } }
}

private fun List<RealTimeReportCell>.firstByKeys(aliases: List<String>): RealTimeReportCell? {
    return firstOrNull { cell -> aliases.any { alias -> cell.key.equals(alias, ignoreCase = true) } }
}

private fun List<RealTimeReportRow>.sumOfCells(aliases: List<String>): Double {
    return sumOf { row -> row.cells.firstByKeys(aliases)?.value?.let(::numberFromDisplay) ?: 0.0 }
}

private fun List<RealTimeReportRow>.maxLabelBy(
    labelAliases: List<String>,
    valueAliases: List<String>,
): String? {
    return mapNotNull { row ->
        val label = row.cells.firstByKeys(labelAliases)?.value?.takeIf { it.isNotBlank() && it != "-" }
            ?: return@mapNotNull null
        val value = row.cells.firstByKeys(valueAliases)?.value?.let(::numberFromDisplay)
            ?: return@mapNotNull null
        label to value
    }
        .maxByOrNull { it.second }
        ?.first
}

private fun List<RealTimeReportRow>.maxValueBy(
    resultAliases: List<String>,
    valueAliases: List<String>,
): String? {
    return mapNotNull { row ->
        val result = row.cells.firstByKeys(resultAliases)?.value?.takeIf { it.isNotBlank() && it != "-" }
            ?: return@mapNotNull null
        val value = row.cells.firstByKeys(valueAliases)?.value?.let(::numberFromDisplay)
            ?: return@mapNotNull null
        result to value
    }
        .maxByOrNull { it.second }
        ?.first
}

private fun summaryMetric(
    data: RealTimeReportData,
    key: String,
    label: String,
    aliases: List<String>,
    noteKey: String? = null,
): RealTimeReportMetric? {
    val summary = data.rawData.objectValue("summary")
    val value = aliases.firstNotNullOfOrNull { alias -> summary?.displayField(alias) }
        ?: data.metrics.firstMatching(aliases)?.value
        ?: return null
    val note = noteKey?.let { summary?.displayField(it) }
    return RealTimeReportMetric(key, label, appendNote(value, note))
}

private fun summaryPercentMetric(
    data: RealTimeReportData,
    key: String,
    label: String,
    aliases: List<String>,
    noteKey: String? = null,
): RealTimeReportMetric? {
    val summary = data.rawData.objectValue("summary")
    val value = summary?.numericField(aliases)
        ?: data.metrics.firstMatching(aliases)?.value?.let(::numberFromDisplay)
        ?: return null
    val note = noteKey?.let { summary?.displayField(it) }
    return RealTimeReportMetric(key, label, appendNote(percentDisplay(value), note))
}

private fun nestedSummaryMetric(
    data: RealTimeReportData,
    objectKey: String,
    label: String,
    valueKey: String,
    noteKey: String? = null,
): RealTimeReportMetric? {
    val nested = data.rawData.objectValue("summary")?.objectValue(objectKey) ?: return null
    val value = nested.labelField(valueKey) ?: return null
    val note = noteKey?.let { nested.displayField(it) }
    return RealTimeReportMetric(objectKey, label, appendNote(value, note))
}

private fun rowsSumMetric(
    data: RealTimeReportData,
    key: String,
    label: String,
    aliases: List<String>,
): RealTimeReportMetric? {
    val value = data.rows.sumOfCells(aliases).takeIf { it > 0.0 } ?: return null
    return RealTimeReportMetric(key, label, formatNumberToMoney(value.toString()))
}

private fun appendNote(value: String, note: String?): String {
    return if (note.isNullOrBlank() || note == "-") value else "$value · $note"
}

private fun chartByKeys(
    data: RealTimeReportData,
    keys: List<String>,
    title: String,
): RealTimeReportChartSection? {
    return data.chartSections
        .firstOrNull { section -> keys.any { section.key.equals(it, ignoreCase = true) } && section.points.isNotEmpty() }
        ?.copy(
            title = title,
            points = data.chartSections
                .first { section -> keys.any { section.key.equals(it, ignoreCase = true) } && section.points.isNotEmpty() }
                .points
                .map { it.copy(label = friendlyAgingLabel(it.label)) }
        )
}

private fun rowChart(
    data: RealTimeReportData,
    key: String,
    title: String,
    labelAliases: List<String>,
    valueAliases: List<String>,
): RealTimeReportChartSection? {
    return rowAmountChartSections(data, key, title, labelAliases, valueAliases).firstOrNull()
}

private fun totalsDonut(
    data: RealTimeReportData,
    key: String,
    title: String,
): RealTimeReportChartSection? {
    val source = data.rawData.objectValue("totals") ?: data.rawData.objectValue("summary") ?: return null
    val points = listOfNotNull(
        rawPoint(source, "collected", "Cobrado"),
        rawPoint(source, "pending", "Pendiente"),
        rawPoint(source, "canceled", "Anulado") ?: rawPoint(source, "cancelled", "Anulado")
    )
    return points.takeIf { it.isNotEmpty() }?.let {
        RealTimeReportChartSection(key = key, title = title, points = it, lines = emptyList())
    }
}

private fun billedPaidBalanceDonut(data: RealTimeReportData): RealTimeReportChartSection? {
    val summary = data.rawData.objectValue("summary")
    val points = if (summary != null) {
        listOfNotNull(
            rawPoint(summary, "total_billed", "Facturado"),
            rawPoint(summary, "total_paid", "Pagado"),
            rawPoint(summary, "outstanding_balance", "Saldo pendiente")
        )
    } else {
        emptyList()
    }.ifEmpty {
        listOf(
            RealTimeReportChartPoint("Cargo", data.rows.sumOfCells(listOf("charge")), formatNumberToMoney(data.rows.sumOfCells(listOf("charge")).toString())),
            RealTimeReportChartPoint("Pago", data.rows.sumOfCells(listOf("payment")), formatNumberToMoney(data.rows.sumOfCells(listOf("payment")).toString()))
        ).filter { it.value > 0.0 }
    }
    return points.takeIf { it.isNotEmpty() }?.let {
        RealTimeReportChartSection(
            key = "customer_statement_charge_payment_donut",
            title = "Cargo vs pago",
            points = it,
            lines = emptyList()
        )
    }
}

private fun agingDonut(
    data: RealTimeReportData,
    key: String,
    title: String,
): RealTimeReportChartSection? {
    val chart = data.chartSections.firstOrNull { section ->
        (section.key.contains("aging", ignoreCase = true) || section.key.contains("buckets", ignoreCase = true)) &&
            section.points.isNotEmpty()
    }
    if (chart != null) {
        return chart.copy(
            key = key,
            title = title,
            points = chart.points.map { it.copy(label = friendlyAgingLabel(it.label)) }
        )
    }

    val source = data.rawData.objectValue("aging") ?: data.rawData.objectValue("buckets") ?: return null
    val points = source.entries.mapNotNull { (bucket, value) ->
        val number = (value as? JsonPrimitive)?.doubleOrNull ?: return@mapNotNull null
        RealTimeReportChartPoint(
            label = friendlyAgingLabel(bucket),
            value = number,
            formattedValue = jsonDisplay(bucket, value)
        )
    }
    return points.takeIf { it.isNotEmpty() }?.let {
        RealTimeReportChartSection(key = key, title = title, points = it, lines = emptyList())
    }
}

private fun customerRankingPendingChart(data: RealTimeReportData): RealTimeReportChartSection? {
    val points = data.rows.mapNotNull { row ->
        val pendingCell = row.cells.firstByKeys(PENDING_ALIASES) ?: return@mapNotNull null
        val value = numberFromDisplay(pendingCell.value)?.takeIf { it > 0.0 } ?: return@mapNotNull null
        val label = row.cells.firstByKeys(CUSTOMER_NAME_ALIASES)?.value?.takeIf { it.isNotBlank() && it != "-" }
            ?: "Consumidor Final"
        RealTimeReportChartPoint(label, value, pendingCell.value)
    }.sortedByDescending { it.value }.take(MAX_REPORT_CHART_POINTS)
    return points.takeIf { it.isNotEmpty() }?.let {
        RealTimeReportChartSection(
            key = "customer_ranking_pending",
            title = "Saldo pendiente",
            points = it,
            lines = emptyList()
        )
    }
}

private fun rowNetItbms(row: RealTimeReportRow): Double {
    val explicit = row.cells.firstByKeys(NET_ITBMS_ALIASES)?.value?.let(::numberFromDisplay)
    if (explicit != null) return explicit
    val itbms = row.cells.firstByKeys(ITBMS_ALIASES)?.value?.let(::numberFromDisplay) ?: 0.0
    val retained = row.cells.firstByKeys(TAX_RETENTION_ALIASES)?.value?.let(::numberFromDisplay) ?: 0.0
    return if (row.isCreditNote()) {
        -kotlin.math.abs(itbms)
    } else {
        itbms - kotlin.math.abs(retained)
    }
}

private fun RealTimeReportRow.isCreditNote(): Boolean {
    val type = cells.firstByKeys(listOf("type", "document_type"))?.value.orEmpty()
    return type.trim().lowercase().replace("-", "_").replace(" ", "_") in setOf(
        "credit_note",
        "credit_notes",
        "nota_credito",
        "nota_de_credito"
    )
}

private fun List<RealTimeReportRow>.creditNoteItbms(): Double {
    return filter { it.isCreditNote() }.sumOf { row ->
        kotlin.math.abs(row.cells.firstByKeys(ITBMS_ALIASES)?.value?.let(::numberFromDisplay) ?: 0.0)
    }
}

private fun RealTimeReportData.chartArray(key: String): JsonArray? {
    return rawData.objectValue("charts")?.get(key) as? JsonArray
}

private fun rowAsChartObject(row: RealTimeReportRow): JsonObject {
    return JsonObject(
        row.cells.associate { cell ->
            val number = numberFromDisplay(cell.value)
            cell.key to if (number != null) JsonPrimitive(number) else JsonPrimitive(cell.value)
        }
    )
}

private fun JsonObject.numericField(aliases: List<String>): Double? {
    return aliases.firstNotNullOfOrNull { alias ->
        (this[alias] as? JsonPrimitive)?.doubleOrNull
    }
}

private fun JsonObject.firstText(aliases: List<String>): String? {
    return aliases.firstNotNullOfOrNull { alias ->
        this[alias]?.let { value ->
            (value as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
        }
    }
}

private fun formatMoneyValue(value: Double): String = formatNumberToMoney(value.toString())

private fun formatNegativeMoneyValue(value: Double): String {
    return "-${formatNumberToMoney(kotlin.math.abs(value).toString())}"
}

private fun percentDisplay(value: Double): String {
    return "${trimCount(value)}%"
}

private fun percentDisplayFromString(value: String): String {
    val number = numberFromDisplay(value) ?: return value
    return percentDisplay(number)
}

private fun negativeMoneyDisplay(value: String): String {
    val number = numberFromDisplay(value) ?: return value
    return formatNegativeMoneyValue(number)
}

private fun comboLinePayload(label: String, value: Double, formattedValue: String): String {
    return comboLinePayload(label, value, formattedValue, "Línea")
}

private fun comboLinePayload(label: String, value: Double, formattedValue: String, seriesLabel: String): String {
    return listOf(label, value.toString(), formattedValue, seriesLabel).joinToString("\t")
}

private fun groupedChartPayload(group: String, series: String, value: Double, formattedValue: String): String {
    return listOf(group, series, value.toString(), formattedValue).joinToString("\t")
}

private data class ComboLinePoint(
    val label: String,
    val value: Double,
    val formattedValue: String,
    val seriesLabel: String,
)

private fun comboLinePoint(payload: String): ComboLinePoint? {
    val parts = payload.split("\t")
    if (parts.size !in 3..4) return null
    return ComboLinePoint(
        label = parts[0],
        value = parts[1].toDoubleOrNull() ?: return null,
        formattedValue = parts[2],
        seriesLabel = parts.getOrNull(3) ?: "Línea"
    )
}

private data class GroupedChartPoint(
    val group: String,
    val series: String,
    val value: Double,
    val formattedValue: String,
)

private fun groupedChartPoint(payload: String): GroupedChartPoint? {
    val parts = payload.split("\t")
    if (parts.size != 4) return null
    return GroupedChartPoint(
        group = parts[0],
        series = parts[1],
        value = parts[2].toDoubleOrNull() ?: return null,
        formattedValue = parts[3]
    )
}

private fun rawPoint(source: JsonObject, key: String, label: String): RealTimeReportChartPoint? {
    val primitive = source[key] as? JsonPrimitive ?: return null
    val value = primitive.doubleOrNull ?: return null
    return RealTimeReportChartPoint(label, value, jsonDisplay(key, primitive))
}

private fun JsonObject.objectValue(key: String): JsonObject? = this[key] as? JsonObject

private fun JsonObject.displayField(key: String): String? {
    return this[key]?.let { jsonDisplay(key, it) }
}

private fun JsonObject.labelField(key: String): String? {
    val value = this[key] ?: return null
    if (value is JsonObject) {
        return listOf("label", "customer_name", "name", "user", "branch_name")
            .firstNotNullOfOrNull { childKey -> value.displayField(childKey) }
    }
    return jsonDisplay(key, value)
}

private fun jsonDisplay(key: String, value: JsonElement): String {
    val primitive = value as? JsonPrimitive ?: return value.toString()
    val raw = primitive.contentOrNull ?: return "-"
    val number = primitive.doubleOrNull
    val lowerKey = key.lowercase()
    return when {
        number != null && listOf("percent", "concentration", "rate").any { lowerKey.contains(it) } -> "${trimCount(number)}%"
        number != null && listOf("count", "days", "ranking").any { lowerKey.contains(it) } -> trimCount(number)
        number != null && listOf(
            "amount",
            "balance",
            "billed",
            "charge",
            "collected",
            "exempt",
            "historical",
            "invoiced",
            "itbms",
            "paid",
            "pending",
            "purchased",
            "sales",
            "sold",
            "subtotal",
            "tax",
            "ticket",
            "total",
            "value"
        ).any { lowerKey.contains(it) } -> formatNumberToMoney(number.toString())
        number != null -> trimCount(number)
        else -> raw.ifBlank { "-" }
    }
}

private fun friendlyChartLabel(value: String): String {
    return friendlyReportLabel(friendlyPaymentMethod(friendlyAdjustmentType(value)))
}

private fun friendlyPaymentMethod(value: String): String {
    return when (value.trim().lowercase().replace("-", "_").replace(" ", "_")) {
        "cash", "efectivo" -> "Efectivo"
        "card", "credit_card", "debit_card", "tarjeta", "tarjeta_credito", "tarjeta_de_credito" -> "Tarjeta"
        "bank_transfer", "transfer", "transferencia", "ach" -> "Transferencia"
        "digital_wallet", "wallet", "billetera_digital", "yappy", "nequi" -> "Pago digital"
        "check", "cheque" -> "Cheque"
        "mixed", "mixto" -> "Mixto"
        "credit", "credito" -> "Crédito"
        "other", "otro", "others" -> "Otro"
        else -> value
    }
}

private fun friendlyAdjustmentType(value: String): String {
    return when (value.trim().lowercase().replace("-", "_").replace(" ", "_")) {
        "cancel", "canceled", "cancelled", "cancellation", "annulment", "anulacion", "anulado" -> "Anulación"
        "credit_note", "credit_notes", "nota_credito", "nota_de_credito" -> "Nota de crédito"
        else -> value
    }
}

private fun friendlyAgingLabel(value: String): String {
    return when (value.trim().lowercase()) {
        "days_1_30", "1_30", "1-30" -> "1-30 días"
        "days_31_60", "31_60", "31-60" -> "31-60 días"
        "days_61_90", "61_90", "61-90" -> "61-90 días"
        "days_91_plus", "days_over_90", "over_90", "90_plus", "91+" -> "Más de 90 días"
        "current", "not_due" -> "Por vencer"
        "overdue" -> "Vencido"
        "no_due_date" -> "Sin vencimiento"
        "not_paid" -> "No pagado"
        "paid" -> "Pagado"
        else -> value
    }
}

private fun friendlyMovementType(value: String): String {
    return when (value.trim().lowercase().replace("-", "_").replace(" ", "_")) {
        "invoice", "bill", "sale", "order", "factura" -> "Factura"
        "payment", "paid", "pago" -> "Pago"
        "credit_note", "nota_credito", "nota_de_credito" -> "Nota de crédito"
        "adjustment", "ajuste" -> "Ajuste"
        else -> value
    }
}

private fun friendlyTaxDocumentType(value: String): String {
    return when (value.trim().lowercase().replace("-", "_").replace(" ", "_")) {
        "invoice", "bill", "sale", "order", "factura" -> "Factura"
        "credit_note", "credit_notes", "nota_credito", "nota_de_credito" -> "Nota de crédito"
        else -> value
    }
}

private fun friendlyFrequency(value: String): String {
    return when (value.trim().lowercase().replace("-", "_").replace(" ", "_")) {
        "monthly", "month", "mensual" -> "Mensual"
        "weekly", "week", "semanal" -> "Semanal"
        "biweekly", "quincenal" -> "Quincenal"
        "quarterly", "trimestral" -> "Trimestral"
        "yearly", "annual", "anual" -> "Anual"
        else -> value
    }
}

private fun friendlyFinancialSeries(value: String): String {
    return when (value.trim().lowercase()) {
        "revenue" -> "Ingresos"
        "sales" -> "Ventas"
        "costs" -> "Costos"
        "expenses" -> "Gastos"
        "gross_profit" -> "Utilidad bruta"
        "profit" -> "Utilidad"
        "operating_expenses" -> "Gastos operativos"
        "operating_profit" -> "Utilidad operativa"
        "operating_margin" -> "Margen operativo"
        "net_cash_flow" -> "Flujo neto de caja"
        "uncategorized", "uncategorized_expenses" -> "Sin categorizar"
        else -> value
    }
}

private fun friendlyReportLabel(value: String): String {
    val normalized = value.trim().lowercase().replace("-", "_").replace(" ", "_")
    return when (normalized) {
        "revenue" -> "Ingresos"
        "sales" -> "Ventas"
        "costs" -> "Costos"
        "gross_profit" -> "Utilidad bruta"
        "operating_expenses" -> "Gastos operativos"
        "operating_profit" -> "Utilidad operativa"
        "operating_margin" -> "Margen operativo"
        "expenses" -> "Gastos"
        "profit" -> "Utilidad"
        "net_cash_flow" -> "Flujo neto de caja"
        "uncategorized", "uncategorized_expenses" -> "Sin categorizar"
        "summary" -> "Resumen"
        "detail", "details" -> "Detalle"
        "charges", "receivables", "collections" -> "Cobros"
        "payments", "payables" -> "Pagos"
        "aging", "aging_bucket" -> "Antigüedad"
        "no_due_date" -> "Sin vencimiento"
        "not_paid", "unpaid" -> "No pagado"
        "paid" -> "Pagado"
        "partially_paid", "partial" -> "Parcial"
        "pending" -> "Pendiente"
        "overdue" -> "Vencido"
        "due_soon" -> "Por vencer"
        "current", "not_due" -> "Actual"
        "document", "document_number", "order_number" -> "Documento"
        "party", "party_name" -> "Cliente/proveedor"
        "customer_name" -> "Cliente"
        "supplier_name" -> "Proveedor"
        "type" -> "Tipo"
        "status", "payment_status", "due_status" -> "Estado"
        "due_date" -> "Vencimiento"
        "total", "total_amount" -> "Total"
        "paid_amount" -> "Pagado"
        "pending_amount", "balance" -> "Pendiente"
        "count", "document_count" -> "Documentos"
        "metric", "key", "label" -> "Métrica"
        "value", "current", "actual" -> "Actual"
        "previous", "previous_value" -> "Anterior"
        "difference", "diff" -> "Diferencia"
        "variation", "variation_percent", "percent_change" -> "Variación"
        else -> value.split("_").filter { it.isNotBlank() }.joinToString(" ") { token ->
            token.replaceFirstChar { char -> char.uppercase() }
        }
    }
}

private fun friendlyReportValue(key: String, value: JsonElement): String {
    val display = jsonDisplay(key, value)
    return if ((value as? JsonPrimitive)?.doubleOrNull == null) {
        friendlyReportLabel(display)
    } else {
        display
    }
}

private fun friendlyReportValue(key: String, value: String): String {
    return if (numberFromDisplay(value) == null) friendlyReportLabel(value) else value
}

private fun shouldAlignReportValue(key: String, value: JsonElement): Boolean {
    val lowerKey = key.lowercase()
    return (value as? JsonPrimitive)?.doubleOrNull != null ||
        lowerKey.containsAny(CASH_FLOW_TOTAL_ALIASES + CASH_FLOW_PAID_ALIASES + CASH_FLOW_PENDING_ALIASES + listOf("amount", "total", "count", "days", "percent", "margin"))
}

private fun String.containsAny(tokens: List<String>): Boolean {
    return tokens.any { contains(it) }
}

private fun friendlyPaymentStatus(value: String): String {
    return when (value.trim().lowercase().replace("-", "_").replace(" ", "_")) {
        "paid", "pagado" -> "Pagado"
        "not_paid", "unpaid", "no_pagado" -> "No pagado"
        "partial", "partially_paid", "parcial" -> "Parcial"
        "pending", "pendiente" -> "Pendiente"
        "overdue", "vencido" -> "Vencido"
        "no_due_date", "sin_vencimiento" -> "Sin vencimiento"
        else -> friendlyReportLabel(value)
    }
}

private fun isCountOnlyBusinessOverviewKey(value: String): Boolean {
    return value.trim().lowercase().replace("-", "_").replace(" ", "_") in setOf(
        "new_customers",
        "invoices",
        "invoice_count"
    )
}

private fun friendlyCashFlowType(value: String): String {
    return when (value.trim().lowercase().replace("-", "_").replace(" ", "_")) {
        "receivable", "invoice", "income", "collection", "cobro" -> "Cobro"
        "payable", "expense", "payment", "pago" -> "Pago"
        else -> value
    }
}

private fun friendlyCashFlowStatus(value: String): String {
    return when (value.trim().lowercase().replace("-", "_").replace(" ", "_")) {
        "overdue", "vencido" -> "Vencido"
        "due_soon", "por_vencer" -> "Por vencer"
        "current", "not_due", "vigente" -> "Actual"
        "paid", "pagado" -> "Pagado"
        "not_paid", "unpaid" -> "No pagado"
        "no_due_date" -> "Sin vencimiento"
        "partial", "partially_paid" -> "Parcial"
        else -> friendlyReportLabel(value)
    }
}

private fun friendlyPartyName(value: String): String {
    val nameMatch = Regex(""""name"\s*:\s*"([^"]+)"""").find(value)
    return nameMatch?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() } ?: value
}

private fun friendlyActions(value: String): String {
    val tokens = value
        .replace("[", "")
        .replace("]", "")
        .replace("\"", "")
        .split(",", "|")
        .map { it.trim() }
        .filter { it.isNotBlank() && it != "-" }
    return tokens.map { token ->
        when (token.lowercase().replace("-", "_").replace(" ", "_")) {
            "view", "view_expense", "view_expenses", "ver" -> "Ver"
            "edit", "editar" -> "Editar"
            "filter_supplier" -> "Filtrar proveedor"
            "filter_category" -> "Filtrar categoría"
            else -> token
        }
    }.distinct().joinToString(" · ").ifBlank { value }
}

private fun dateSortKey(value: String): String? {
    val clean = value.substringBefore("T").trim()
    val iso = Regex("""\d{4}-\d{2}-\d{2}""").find(clean)?.value
    if (iso != null) return iso
    val slash = Regex("""(\d{2})/(\d{2})/(\d{4})""").find(clean) ?: return null
    val day = slash.groupValues[1]
    val month = slash.groupValues[2]
    val year = slash.groupValues[3]
    return "$year-$month-$day"
}

private data class ReportMetricRule(
    val key: String,
    val label: String,
    val aliases: List<String>,
)

private data class ReportColumnRule(
    val label: String,
    val aliases: List<String>,
    val defaultValue: String? = null,
)

private data class CashFlowDetailTab(
    val key: String,
    val title: String,
    val rows: List<RealTimeReportRow>,
    val canOpenOrderDetails: Boolean,
)

private const val SALES_SUMMARY_REPORT_KEY = "sales_summary"
private const val SALES_BY_PRODUCT_REPORT_KEY = "sales_by_product"
private const val SALES_BY_PAYMENT_METHOD_REPORT_KEY = "sales_by_payment_method"
private const val SALES_BY_USER_REPORT_KEY = "sales_by_user"
private const val SALES_BY_BRANCH_REPORT_KEY = "sales_by_branch"
private const val SALES_ADJUSTMENTS_REPORT_KEY = "sales_adjustments"
private const val SALES_PENDING_COLLECTION_REPORT_KEY = "sales_pending_collection"
private const val SALES_BY_CUSTOMER_REPORT_KEY = "sales_by_customer"
private const val CUSTOMER_STATEMENT_REPORT_KEY = "customer_statement"
private const val CUSTOMERS_WITH_BALANCE_REPORT_KEY = "customers_with_balance"
private const val NEW_CUSTOMERS_REPORT_KEY = "new_customers"
private const val INACTIVE_CUSTOMERS_REPORT_KEY = "inactive_customers"
private const val CUSTOMER_RANKING_REPORT_KEY = "customer_ranking"
private const val SALES_TAX_SUMMARY_REPORT_KEY = "sales_tax_summary"
private const val EXPENSE_TAX_SUMMARY_REPORT_KEY = "expense_tax_summary"
private const val EXPENSE_SUMMARY_REPORT_KEY = "expense_summary"
private const val EXPENSE_AGING_REPORT_KEY = "expense_aging"
private const val EXPENSE_BY_ACCOUNT_REPORT_KEY = "expense_by_account"
private const val EXPENSE_BY_SUPPLIER_REPORT_KEY = "expense_by_supplier"
private const val EXPENSE_DETAIL_REPORT_KEY = "expense_detail"
private const val RECURRING_EXPENSE_REPORT_KEY = "recurring_expense_report"
private const val PROFIT_AND_LOSS_REPORT_KEY = "profit_and_loss"
private const val FINANCIAL_COMPARISON_REPORT_KEY = "financial_comparison"
private const val OPERATING_MARGIN_REPORT_KEY = "operating_margin"
private const val BUSINESS_OVERVIEW_REPORT_KEY = "business_overview"
private const val CASH_FLOW_REPORT_KEY = "cash_flow"
private const val CASH_FLOW_SUMMARY_TAB_KEY = "summary"
private const val MAX_REPORT_CHART_POINTS = 8
private const val MAX_DETAIL_COLUMNS = 13
private const val MAX_VISIBLE_DETAIL_ROWS = 12
private val REPORT_CHART_GREEN = Color(0xFF2E7D32)

private val TOTAL_SALES_ALIASES = listOf(
    "total_sales",
    "sales_total",
    "gross_sales",
    "net_sales",
    "total_sold",
    "total_amount",
    "sale_total",
    "revenue",
    "sales",
    "total"
)
private val CHARGED_ALIASES = listOf(
    "charged",
    "charged_amount",
    "charged_total",
    "collected",
    "collected_amount",
    "collected_total",
    "total_collected",
    "paid",
    "paid_amount"
)
private val PENDING_ALIASES = listOf(
    "pending_to_charge",
    "pending_charge",
    "pending_to_collect",
    "pending_collection",
    "pending_amount",
    "pending_total",
    "pending",
    "balance"
)
private val DATE_ALIASES = listOf("date", "emission_date", "period", "period_label", "period_start", "day")
private val DOCUMENT_COUNT_ALIASES = listOf("document_count", "documents_count", "documents", "invoice_count", "documentos", "count")
private val TRANSACTION_COUNT_ALIASES = listOf("transaction_count", "transactions", "txn_count", "tx_count", "payment_count", "payments_count", "count")
private val MEAN_TICKET_ALIASES = listOf("mean_ticket", "average_ticket", "avg_ticket", "ticket", "average_sale")
private val SUBTOTAL_ALIASES = listOf("subtotal", "taxable_subtotal", "net_subtotal")
private val ITBMS_ALIASES = listOf("itbms", "itbms_amount", "itbms_total", "generated_itbms", "itbms_generated", "tax_total")
private val USER_ALIASES = listOf("salesman_name", "seller_name", "salesperson_name", "user_name", "user", "name", "label")
private val BRANCH_NAME_ALIASES = listOf("branch_name", "branch", "sucursal", "name", "label")
private val BRANCH_CODE_ALIASES = listOf("branch_code", "code", "codigo")
private val CLIENT_ALIASES = listOf("client_name", "customer_name", "client", "customer", "name", "label")
private val CUSTOMER_NAME_ALIASES = listOf("customer_name", "client_name", "customer", "client", "label", "name")
private val CUSTOMER_RUC_ALIASES = listOf("customer_ruc", "ruc", "tax_id")
private val DOCUMENT_NUMBER_ALIASES = listOf("order_number", "document_number", "invoice_number", "document", "number", "order_no", "document_no")
private val PAID_ALIASES = listOf("paid", "paid_amount", "amount_paid", "charged", "charged_amount", "collected", "collected_amount")
private val PAYMENT_METHOD_ALIASES = listOf(
    "payment_method_label",
    "payment_method_name",
    "method_label",
    "method_name",
    "payment_method",
    "method",
    "name",
    "label"
)
private val DIGITAL_PAYMENT_PERCENT_ALIASES = listOf(
    "digital_payment_percentage",
    "digital_payments_percentage",
    "digital_payment_percent",
    "digital_payments_percent",
    "percent_digital_payments",
    "digital_payment_rate",
    "digital_rate"
)
private val USAGE_PERCENT_ALIASES = listOf("percent_usage", "usage_percent", "percent_of_usage", "percent_of_total", "percent_of_sales", "share")
private val ADJUSTMENT_TYPE_ALIASES = listOf("adjustment_type", "type", "document_type", "kind", "label")
private val ADJUSTMENT_AMOUNT_ALIASES = listOf(
    "total_adjusted",
    "adjusted_total",
    "total_adjustment",
    "adjustment_total",
    "adjustment_amount",
    "amount",
    "total"
)
private val CANCELED_AMOUNT_ALIASES = listOf("total_canceled", "canceled_total", "cancelled_total", "canceled_amount", "cancelled_amount", "cancellation_total")
private val CREDIT_NOTE_AMOUNT_ALIASES = listOf("credit_notes_total", "credit_note_total", "credit_notes_amount", "credit_note_amount", "total_credit_notes")
private val ADJUSTMENT_PERCENT_ALIASES = listOf("adjustment_sales_percent", "percent_adjusted_sales", "adjusted_sales_percent", "percent_of_sales", "adjustment_rate")
private val REASON_ALIASES = listOf("reason", "motive", "motivo", "adjustment_reason", "cancellation_reason", "credit_note_reason")
private val OVERDUE_AMOUNT_ALIASES = listOf("overdue_amount", "overdue_total", "past_due_amount", "past_due_total", "expired_amount")
private val PARTIAL_PAID_AMOUNT_ALIASES = listOf("partially_paid_amount", "partial_paid_amount", "partial_payment_amount", "partial_amount", "partial_total")
private val AVERAGE_DAYS_OVERDUE_ALIASES = listOf("average_days_overdue", "avg_days_overdue", "overdue_days_average", "mean_days_overdue")
private val LAST_PAYMENT_DATE_ALIASES = listOf("last_payment_date", "last_payment", "last_paid_at")
private val LAST_PURCHASE_DATE_ALIASES = listOf("last_purchase_date", "last_purchase", "last_order_date")
private val TAX_PERIOD_LABEL_ALIASES = listOf("period_label", "period_key", "group_label", "group_key", "period", "month", "date", "label")
private val TAX_RETENTION_ALIASES = listOf("retention_amount", "tax_retained", "retained_tax", "retention")
private val NET_ITBMS_ALIASES = listOf("net_itbms_payable", "net_itbms", "itbms_net")
private val EXEMPT_ALIASES = listOf("exempt", "exempt_sales", "exempt_amount")
private val NON_TAXED_ALIASES = listOf("non_taxed", "non_taxed_sales", "non_taxed_amount")
private val SUPPLIER_NAME_ALIASES = listOf("supplier_name", "supplier", "provider_name", "name", "label")
private val SUPPLIER_RUC_ALIASES = listOf("supplier_ruc", "ruc", "provider_ruc", "tax_id")
private val CATEGORY_NAME_ALIASES = listOf("category_name", "category", "account_name", "name", "label")
private val EXPENSE_PERCENT_ALIASES = listOf("percent_of_total", "percent", "percentage", "share")
private val EXPENSE_ACTION_ALIASES = listOf("actions", "action", "available_actions")
private val FINANCIAL_LABEL_ALIASES = listOf("label", "period_label", "key", "name")
private val CURRENT_VALUE_ALIASES = listOf("current", "actual", "value", "amount", "total")
private val PREVIOUS_VALUE_ALIASES = listOf("previous", "previous_value")
private val DIFFERENCE_ALIASES = listOf("difference", "diff")
private val VARIATION_PERCENT_ALIASES = listOf("variation_percent", "variation", "percent", "percent_change")
private val REVENUE_ALIASES = listOf("revenue", "sales", "income")
private val EXPENSES_ALIASES = listOf("expenses", "expense_total", "total_expenses")
private val PROFIT_ALIASES = listOf("profit", "operating_profit", "gross_profit", "net_profit")
private val OPERATING_MARGIN_ALIASES = listOf("operating_margin", "margin")
private val CASH_FLOW_PARTY_ALIASES = listOf("party", "party_name", "customer_name", "supplier_name", "name")
private val CASH_FLOW_TOTAL_ALIASES = listOf("total_amount", "total")
private val CASH_FLOW_PAID_ALIASES = listOf("paid_amount", "paid")
private val CASH_FLOW_PENDING_ALIASES = listOf("pending_amount", "pending", "balance")

private val SALES_SUMMARY_METRICS = listOf(
    ReportMetricRule("total_sales", "Ventas totales", TOTAL_SALES_ALIASES),
    ReportMetricRule("generated_itbms", "ITBMS generado", listOf("itbms_generated", "generated_itbms", "itbms_total", "itbms")),
    ReportMetricRule("mean_ticket", "Ticket promedio", listOf("mean_ticket", "average_ticket", "avg_ticket", "ticket")),
    ReportMetricRule("pending", "Pendiente por cobrar", PENDING_ALIASES),
    ReportMetricRule("discounts", "Descuentos", listOf("discounts", "discount", "discount_amount", "total_discount", "discount_total")),
    ReportMetricRule("canceled_sales", "Ventas anuladas", listOf("canceled_sales", "cancelled_sales", "canceled", "cancelled", "canceled_total", "cancelled_total"))
)

private val SALES_SUMMARY_DETAIL_COLUMNS = listOf(
    ReportColumnRule("Fecha", DATE_ALIASES),
    ReportColumnRule("# documentos", listOf("document_count", "documents_count", "documents", "invoice_count", "count")),
    ReportColumnRule("Subtotal", listOf("subtotal", "taxable_subtotal")),
    ReportColumnRule("Descuento", listOf("discount", "discounts", "discount_amount", "total_discount", "discount_total")),
    ReportColumnRule("ITBMS", listOf("itbms", "itbms_amount", "itbms_generated", "generated_itbms", "itbms_total")),
    ReportColumnRule("Total", TOTAL_SALES_ALIASES),
    ReportColumnRule("Cobrado", CHARGED_ALIASES),
    ReportColumnRule("Pendiente por cobrar", PENDING_ALIASES),
    ReportColumnRule("Anulado", listOf("canceled", "cancelled", "canceled_sales", "cancelled_sales", "canceled_total", "cancelled_total"))
)

private val PRODUCT_NAME_ALIASES = listOf(
    "product_service",
    "product_or_service",
    "product_name",
    "service_name",
    "item_name",
    "product",
    "name",
    "label",
    "description"
)

private val PRODUCT_QUANTITY_ALIASES = listOf(
    "items_sold",
    "units_sold",
    "quantity_sold",
    "sold_quantity",
    "total_quantity",
    "quantity",
    "qty"
)

private val PRODUCT_TOTAL_WITH_ITBMS_ALIASES = listOf(
    "total_sales_with_itbms",
    "sales_with_itbms",
    "total_with_itbms",
    "total_including_itbms",
    "total_sales_including_itbms",
    "total_amount_with_itbms",
    "sales_amount_with_itbms",
    "with_itbms",
    "total_sales",
    "sales_total",
    "total_sold",
    "amount",
    "total"
)

private val PRODUCT_SALES_METRICS = listOf(
    ReportMetricRule("total_sold", "Total vendido", TOTAL_SALES_ALIASES),
    ReportMetricRule("items_sold", "Items vendidos", PRODUCT_QUANTITY_ALIASES),
    ReportMetricRule(
        key = "top_sales_product",
        label = "Producto con mayor venta",
        aliases = listOf(
            "top_product_by_sales",
            "top_product_sales",
            "best_selling_product_by_amount",
            "top_sales_product",
            "highest_sales_product",
            "product_with_biggest_amount_of_sales",
            "product_with_biggest_amount_sales"
        )
    ),
    ReportMetricRule(
        key = "top_quantity_product",
        label = "Producto con mayor cantidad",
        aliases = listOf(
            "top_product_by_quantity",
            "top_product_quantity",
            "best_selling_product_by_quantity",
            "top_quantity_product",
            "highest_quantity_product",
            "product_with_biggest_qty_of_sales"
        )
    )
)

private val PRODUCT_SALES_DETAIL_COLUMNS = listOf(
    ReportColumnRule("Producto/servicio", PRODUCT_NAME_ALIASES),
    ReportColumnRule("Items vendidos", PRODUCT_QUANTITY_ALIASES),
    ReportColumnRule("% ventas", listOf("percent_of_sales", "sales_percent", "sales_percentage", "percentage_of_sales")),
    ReportColumnRule("ITBMS", listOf("itbms", "itbms_amount", "itbms_total", "generated_itbms", "itbms_generated", "tax_total")),
    ReportColumnRule("Total con ITBMS", PRODUCT_TOTAL_WITH_ITBMS_ALIASES)
)

private val PAYMENT_METHOD_METRICS = listOf(
    ReportMetricRule("total_sold", "Total vendido", TOTAL_SALES_ALIASES),
    ReportMetricRule("total_charged", "Total cobrado", CHARGED_ALIASES),
    ReportMetricRule("main_payment_method", "Método principal", listOf("main_payment_method", "primary_payment_method", "top_payment_method", "payment_method_label")),
    ReportMetricRule("digital_payment_percent", "% pagos digitales", DIGITAL_PAYMENT_PERCENT_ALIASES)
)

private val PAYMENT_METHOD_DETAIL_COLUMNS = listOf(
    ReportColumnRule("Método de pago", PAYMENT_METHOD_ALIASES),
    ReportColumnRule("# transacciones", TRANSACTION_COUNT_ALIASES),
    ReportColumnRule("Total vendido", TOTAL_SALES_ALIASES),
    ReportColumnRule("Total cobrado", CHARGED_ALIASES),
    ReportColumnRule("Pendiente por cobrar", PENDING_ALIASES),
    ReportColumnRule("Ticket promedio", MEAN_TICKET_ALIASES),
    ReportColumnRule("% de uso", USAGE_PERCENT_ALIASES)
)

private val SALES_BY_USER_METRICS = listOf(
    ReportMetricRule("total_sold", "Total vendido", TOTAL_SALES_ALIASES),
    ReportMetricRule("total_charged", "Total cobrado", CHARGED_ALIASES),
    ReportMetricRule("mean_ticket", "Ticket promedio", MEAN_TICKET_ALIASES),
    ReportMetricRule("lead_salesman", "Vendedor lider", listOf("lead_salesman", "top_salesman", "main_user", "best_salesman", "top_user"))
)

private val SALES_BY_USER_DETAIL_COLUMNS = listOf(
    ReportColumnRule("Vendedor", USER_ALIASES),
    ReportColumnRule("Documentos", DOCUMENT_COUNT_ALIASES),
    ReportColumnRule("Subtotal", SUBTOTAL_ALIASES),
    ReportColumnRule("ITBMS", ITBMS_ALIASES),
    ReportColumnRule("Total vendido", TOTAL_SALES_ALIASES),
    ReportColumnRule("Ticket promedio", MEAN_TICKET_ALIASES)
)

private val SALES_BY_BRANCH_METRICS = listOf(
    ReportMetricRule("total_sold", "Total vendido", TOTAL_SALES_ALIASES),
    ReportMetricRule("lead_branch_code", "Sucursal lider", listOf("lead_branch_code", "top_branch_code", "main_branch_code", "branch_code")),
    ReportMetricRule("total_charged", "Total cobrado", CHARGED_ALIASES),
    ReportMetricRule("mean_ticket", "Ticket promedio", MEAN_TICKET_ALIASES)
)

private val SALES_BY_BRANCH_DETAIL_COLUMNS = listOf(
    ReportColumnRule("Sucursal", BRANCH_NAME_ALIASES),
    ReportColumnRule("Código", BRANCH_CODE_ALIASES),
    ReportColumnRule("ITBMS", ITBMS_ALIASES),
    ReportColumnRule("Total vendido", TOTAL_SALES_ALIASES),
    ReportColumnRule("Ticket promedio", MEAN_TICKET_ALIASES)
)

private val SALES_ADJUSTMENTS_METRICS = listOf(
    ReportMetricRule("total_adjustment", "Total ajustes", ADJUSTMENT_AMOUNT_ALIASES),
    ReportMetricRule("total_canceled", "Total anulado", CANCELED_AMOUNT_ALIASES),
    ReportMetricRule("credit_notes_total", "Notas de crédito", CREDIT_NOTE_AMOUNT_ALIASES),
    ReportMetricRule("adjustment_sales_percent", "% ventas ajustadas", ADJUSTMENT_PERCENT_ALIASES),
    ReportMetricRule("main_user", "Usuario principal", listOf("main_user", "top_user", "primary_user", "user")),
    ReportMetricRule("main_type", "Tipo principal", listOf("main_type", "top_type", "primary_type", "adjustment_type"))
)

private val SALES_ADJUSTMENTS_DETAIL_COLUMNS = listOf(
    ReportColumnRule("Cliente", CLIENT_ALIASES),
    ReportColumnRule("Orden", DOCUMENT_NUMBER_ALIASES),
    ReportColumnRule("Monto", ADJUSTMENT_AMOUNT_ALIASES),
    ReportColumnRule("Motivo", REASON_ALIASES),
    ReportColumnRule("Tipo", ADJUSTMENT_TYPE_ALIASES),
    ReportColumnRule("Usuario", USER_ALIASES)
)

private val SALES_PENDING_COLLECTION_METRICS = listOf(
    ReportMetricRule("pending_amount", "Pendiente por cobrar", PENDING_ALIASES),
    ReportMetricRule("overdue_amount", "Monto vencido", OVERDUE_AMOUNT_ALIASES),
    ReportMetricRule("partial_paid_amount", "Monto parcialmente pagado", PARTIAL_PAID_AMOUNT_ALIASES),
    ReportMetricRule("top_pending_client", "Cliente con mayor saldo", listOf("top_pending_client", "client_with_biggest_pending", "main_customer", "top_customer")),
    ReportMetricRule("average_days_overdue", "Días vencidos promedio", AVERAGE_DAYS_OVERDUE_ALIASES)
)

private val SALES_PENDING_COLLECTION_DETAIL_COLUMNS = listOf(
    ReportColumnRule("Documento", DOCUMENT_NUMBER_ALIASES),
    ReportColumnRule("Cliente", CLIENT_ALIASES),
    ReportColumnRule("Emisión", listOf("emission_date", "issue_date", "date")),
    ReportColumnRule("Vencimiento", listOf("due_date", "expiration_date", "deadline")),
    ReportColumnRule("Total", TOTAL_SALES_ALIASES),
    ReportColumnRule("Pagado", PAID_ALIASES),
    ReportColumnRule("Pendiente", PENDING_ALIASES)
)

private val SALES_BY_CUSTOMER_DETAIL_COLUMNS = listOf(
    ReportColumnRule("Cliente", CUSTOMER_NAME_ALIASES, "Consumidor Final"),
    ReportColumnRule("RUC", CUSTOMER_RUC_ALIASES, ""),
    ReportColumnRule("Facturas", DOCUMENT_COUNT_ALIASES),
    ReportColumnRule("Subtotal", SUBTOTAL_ALIASES),
    ReportColumnRule("ITBMS", ITBMS_ALIASES),
    ReportColumnRule("Total vendido", TOTAL_SALES_ALIASES),
    ReportColumnRule("Cobrado", CHARGED_ALIASES),
    ReportColumnRule("Pendiente", PENDING_ALIASES),
    ReportColumnRule("% ventas", listOf("percent_of_sales", "sales_percent", "sales_percentage"))
)

private val CUSTOMER_STATEMENT_DETAIL_COLUMNS = listOf(
    ReportColumnRule("Fecha", DATE_ALIASES),
    ReportColumnRule("Tipo", listOf("type")),
    ReportColumnRule("Documento", DOCUMENT_NUMBER_ALIASES, "-"),
    ReportColumnRule("Descripción", listOf("description", "detail", "concept"), "-"),
    ReportColumnRule("Cargo", listOf("charge", "amount_charged", "debit")),
    ReportColumnRule("Pago", listOf("payment", "paid", "credit")),
    ReportColumnRule("Saldo", listOf("balance", "outstanding_balance")),
    ReportColumnRule("Estado", listOf("status"))
)

private val CUSTOMERS_WITH_BALANCE_DETAIL_COLUMNS = listOf(
    ReportColumnRule("Cliente", CUSTOMER_NAME_ALIASES, "Consumidor Final"),
    ReportColumnRule("RUC", CUSTOMER_RUC_ALIASES, ""),
    ReportColumnRule("Documentos pendientes", DOCUMENT_COUNT_ALIASES),
    ReportColumnRule("Saldo total", PENDING_ALIASES + listOf("total")),
    ReportColumnRule("Saldo vencido", OVERDUE_AMOUNT_ALIASES + listOf("overdue")),
    ReportColumnRule("Ultimo pago", LAST_PAYMENT_DATE_ALIASES),
    ReportColumnRule("Días max. vencido", listOf("max_days_overdue", "maximum_days_overdue"))
)

private val NEW_CUSTOMERS_DETAIL_COLUMNS = listOf(
    ReportColumnRule("Cliente", CUSTOMER_NAME_ALIASES, "Cliente sin nombre"),
    ReportColumnRule("RUC", CUSTOMER_RUC_ALIASES, ""),
    ReportColumnRule("Tipo", listOf("customer_type", "type")),
    ReportColumnRule("Fecha creacion", listOf("date", "created_at", "creation_date")),
    ReportColumnRule("Primera compra", listOf("first_purchase_date"), "Sin compra"),
    ReportColumnRule("Total comprado", listOf("total_purchased", "total", "sales_to_customer")),
    ReportColumnRule("Vendedor", USER_ALIASES + listOf("created_by"), "-"),
    ReportColumnRule("Sucursal", BRANCH_NAME_ALIASES + BRANCH_CODE_ALIASES, "-")
)

private val INACTIVE_CUSTOMERS_DETAIL_COLUMNS = listOf(
    ReportColumnRule("Cliente", CUSTOMER_NAME_ALIASES, "Cliente sin nombre"),
    ReportColumnRule("Ultima compra", LAST_PURCHASE_DATE_ALIASES),
    ReportColumnRule("Días inactivo", listOf("days_inactive", "inactive_days")),
    ReportColumnRule("Ventas historicas", listOf("historical_sales", "total", "inactive_historical_sales")),
    ReportColumnRule("Saldo pendiente", PENDING_ALIASES),
    ReportColumnRule("Vendedor", USER_ALIASES + listOf("created_by"), "-"),
    ReportColumnRule("Acción sugerida", listOf("action_suggestion", "suggested_action"))
)

private val CUSTOMER_RANKING_DETAIL_COLUMNS = listOf(
    ReportColumnRule("Ranking", listOf("ranking", "rank")),
    ReportColumnRule("Cliente", CUSTOMER_NAME_ALIASES, "Consumidor Final"),
    ReportColumnRule("Ventas", TOTAL_SALES_ALIASES),
    ReportColumnRule("Facturas", listOf("invoice_count") + DOCUMENT_COUNT_ALIASES),
    ReportColumnRule("Ticket promedio", listOf("ticket_average") + MEAN_TICKET_ALIASES),
    ReportColumnRule("Ultima compra", LAST_PURCHASE_DATE_ALIASES),
    ReportColumnRule("Saldo pendiente", PENDING_ALIASES),
    ReportColumnRule("% ventas", listOf("percent_of_sales", "sales_percent", "sales_percentage"))
)

private val SALES_TAX_DETAIL_COLUMNS = listOf(
    ReportColumnRule("Documento", DOCUMENT_NUMBER_ALIASES, "-"),
    ReportColumnRule("Tipo", listOf("type", "document_type")),
    ReportColumnRule("Fecha", listOf("issue_date", "date")),
    ReportColumnRule("Cliente", CUSTOMER_NAME_ALIASES, "Consumidor Final"),
    ReportColumnRule("Subtotal gravado", listOf("taxable_subtotal")),
    ReportColumnRule("ITBMS bruto", ITBMS_ALIASES),
    ReportColumnRule("Retención", TAX_RETENTION_ALIASES),
    ReportColumnRule("ITBMS neto", NET_ITBMS_ALIASES),
    ReportColumnRule("Exento", EXEMPT_ALIASES),
    ReportColumnRule("No gravado", NON_TAXED_ALIASES),
    ReportColumnRule("Total", TOTAL_SALES_ALIASES)
)

private val EXPENSE_TAX_DETAIL_COLUMNS = listOf(
    ReportColumnRule("Periodo", listOf("group_label", "period_label", "group_key", "period_key", "period"), "-"),
    ReportColumnRule("Proveedor", listOf("supplier_name", "supplier_ruc", "category_name", "source", "group_label"), "Sin proveedor"),
    ReportColumnRule("Cantidad", DOCUMENT_COUNT_ALIASES),
    ReportColumnRule("Subtotal", listOf("taxable_subtotal", "subtotal")),
    ReportColumnRule("ITBMS", ITBMS_ALIASES),
    ReportColumnRule("Total", TOTAL_SALES_ALIASES)
)

private val EXPENSE_SUMMARY_DETAIL_COLUMNS = listOf(
    ReportColumnRule("Periodo", listOf("period_label", "period_key", "period")),
    ReportColumnRule("Documentos", listOf("count") + DOCUMENT_COUNT_ALIASES),
    ReportColumnRule("Subtotal", listOf("subtotal")),
    ReportColumnRule("ITBMS", ITBMS_ALIASES),
    ReportColumnRule("Total", TOTAL_SALES_ALIASES)
)

private val EXPENSE_AGING_DETAIL_COLUMNS = listOf(
    ReportColumnRule("Documento", listOf("document_number", "document"), "-"),
    ReportColumnRule("Proveedor", SUPPLIER_NAME_ALIASES),
    ReportColumnRule("Vencimiento", listOf("due_date")),
    ReportColumnRule("Antigüedad", listOf("bucket_label", "bucket")),
    ReportColumnRule("Estado", listOf("status", "payment_status")),
    ReportColumnRule("Total", TOTAL_SALES_ALIASES),
    ReportColumnRule("Pagado", PAID_ALIASES),
    ReportColumnRule("Saldo", listOf("balance", "pending"))
)

private val EXPENSE_BY_ACCOUNT_DETAIL_COLUMNS = listOf(
    ReportColumnRule("Cuenta", listOf("name", "label", "category_name")),
    ReportColumnRule("Clasificación", listOf("classification")),
    ReportColumnRule("Items", listOf("item_count")),
    ReportColumnRule("Gastos", listOf("expense_count")),
    ReportColumnRule("Subtotal", listOf("subtotal")),
    ReportColumnRule("ITBMS", ITBMS_ALIASES),
    ReportColumnRule("Total", TOTAL_SALES_ALIASES),
    ReportColumnRule("% total", EXPENSE_PERCENT_ALIASES)
)

private val EXPENSE_BY_SUPPLIER_DETAIL_COLUMNS = listOf(
    ReportColumnRule("RUC", SUPPLIER_RUC_ALIASES),
    ReportColumnRule("Proveedor", SUPPLIER_NAME_ALIASES),
    ReportColumnRule("Cantidad facturas", listOf("invoice_count") + DOCUMENT_COUNT_ALIASES),
    ReportColumnRule("Subtotal", listOf("subtotal")),
    ReportColumnRule("ITBMS", ITBMS_ALIASES),
    ReportColumnRule("Total", TOTAL_SALES_ALIASES),
    ReportColumnRule("% del gasto", EXPENSE_PERCENT_ALIASES)
)

private val EXPENSE_DETAIL_COLUMNS = listOf(
    ReportColumnRule("Documento", listOf("document_number", "document"), "-"),
    ReportColumnRule("Proveedor", SUPPLIER_NAME_ALIASES),
    ReportColumnRule("RUC", SUPPLIER_RUC_ALIASES),
    ReportColumnRule("Emisión", listOf("emission_date", "issue_date", "date")),
    ReportColumnRule("Vencimiento", listOf("due_date")),
    ReportColumnRule("Subtotal", listOf("subtotal")),
    ReportColumnRule("ITBMS", ITBMS_ALIASES),
    ReportColumnRule("Total", TOTAL_SALES_ALIASES),
    ReportColumnRule("Pagado", PAID_ALIASES),
    ReportColumnRule("Pendiente", PENDING_ALIASES),
    ReportColumnRule("Estado", listOf("payment_status", "status")),
    ReportColumnRule("Categoría", CATEGORY_NAME_ALIASES),
    ReportColumnRule("Fuente", listOf("source")),
    ReportColumnRule("Acciones", EXPENSE_ACTION_ALIASES)
)

private val RECURRING_EXPENSE_DETAIL_COLUMNS = listOf(
    ReportColumnRule("Proveedor", SUPPLIER_NAME_ALIASES),
    ReportColumnRule("Categoría", CATEGORY_NAME_ALIASES),
    ReportColumnRule("Frecuencia", listOf("frequency")),
    ReportColumnRule("Ultimo gasto", listOf("last_document_number", "document_number")),
    ReportColumnRule("Monto promedio", listOf("average_amount")),
    ReportColumnRule("Próximo vencimiento", listOf("next_due_date")),
    ReportColumnRule("Estado", listOf("payment_status", "status")),
    ReportColumnRule("Acciones", EXPENSE_ACTION_ALIASES)
)

private val PROFIT_AND_LOSS_DETAIL_COLUMNS = listOf(
    ReportColumnRule("Concepto", listOf("label", "key")),
    ReportColumnRule("Actual", CURRENT_VALUE_ALIASES + OPERATING_MARGIN_ALIASES)
)

private val FINANCIAL_COMPARISON_DETAIL_COLUMNS = listOf(
    ReportColumnRule("Métrica", listOf("label", "key")),
    ReportColumnRule("Actual", CURRENT_VALUE_ALIASES),
    ReportColumnRule("Anterior", PREVIOUS_VALUE_ALIASES),
    ReportColumnRule("Diferencia", DIFFERENCE_ALIASES),
    ReportColumnRule("Variación", VARIATION_PERCENT_ALIASES)
)

private val OPERATING_MARGIN_DETAIL_COLUMNS = listOf(
    ReportColumnRule("Periodo", listOf("label", "key", "period_label", "period_key")),
    ReportColumnRule("Ingresos", REVENUE_ALIASES),
    ReportColumnRule("Gastos", EXPENSES_ALIASES),
    ReportColumnRule("Utilidad operativa", listOf("operating_profit")),
    ReportColumnRule("Margen", OPERATING_MARGIN_ALIASES)
)

private val CASH_FLOW_DETAIL_COLUMNS = listOf(
    ReportColumnRule("Documento", listOf("document_number", "document"), "-"),
    ReportColumnRule("Cliente/proveedor", CASH_FLOW_PARTY_ALIASES),
    ReportColumnRule("Tipo", listOf("type")),
    ReportColumnRule("Vencimiento", listOf("due_date")),
    ReportColumnRule("Estado", listOf("due_status", "payment_status", "status")),
    ReportColumnRule("Total", CASH_FLOW_TOTAL_ALIASES),
    ReportColumnRule("Pagado", CASH_FLOW_PAID_ALIASES),
    ReportColumnRule("Pendiente", CASH_FLOW_PENDING_ALIASES),
    ReportColumnRule("Acciones", EXPENSE_ACTION_ALIASES)
)
