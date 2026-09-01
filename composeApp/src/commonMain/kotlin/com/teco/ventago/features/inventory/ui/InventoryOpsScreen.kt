package com.teco.ventago.features.inventory.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.textfields.helpers.DMDropDownField
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.vanishedBackgroundColor
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.features.inventory.domain.InventoryKardexSupport
import com.teco.ventago.utils.formatNumberToMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryOpsScreen(
    viewModel: InventoryOpsViewModel,
    onBack: () -> Unit,
    onOpenOrders: () -> Unit,
    onOpenProducts: () -> Unit,
) {
    val ui by viewModel.uiState.collectAsState()
    var activeSheet by remember { mutableStateOf<InventoryActionSheet?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(activeSheet) {
        val sheet = activeSheet ?: return@LaunchedEffect
        when (sheet) {
            InventoryActionSheet.ALERTS -> viewModel.loadAlerts()
            InventoryActionSheet.WAREHOUSES -> viewModel.loadWarehousesPanel()
            InventoryActionSheet.ADJUST, InventoryActionSheet.TRANSFER, InventoryActionSheet.COUNT -> {
                viewModel.resetProductSelection()
                if (sheet == InventoryActionSheet.ADJUST || sheet == InventoryActionSheet.COUNT) {
                    viewModel.ensureSheetDefaults(sheet)
                }
            }
        }
    }

    val currentSheet = activeSheet
    if (currentSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            sheetState = sheetState,
            containerColor = cardContainerColor(),
        ) {
            when (currentSheet) {
                InventoryActionSheet.ADJUST -> AdjustStockSheet(ui, viewModel) { activeSheet = null }
                InventoryActionSheet.TRANSFER -> TransferStockSheet(ui, viewModel) { activeSheet = null }
                InventoryActionSheet.COUNT -> PhysicalCountSheet(ui, viewModel) { activeSheet = null }
                InventoryActionSheet.ALERTS -> MinAlertsSheet(ui, viewModel)
                InventoryActionSheet.WAREHOUSES -> InventoryWarehousesSheet(ui, viewModel)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Inventario", style = titleMediumBold())
        if (ui.message.isNotBlank()) {
            Text(ui.message, style = bodyMedium(MaterialTheme.colorScheme.primary))
        }
        if (!ui.enabled) {
            Text("El módulo de inventario no está activo.", style = bodyMedium())
        } else {
            if (ui.canView) {
                ui.dashboard?.let { summary ->
                    InventoryKpiCard(summary, onClick = onOpenProducts)
                    InventoryOrdersCard(summary, onClick = onOpenOrders)
                }
            }

            if (ui.canView) {
                Text("Acciones", style = bodyMediumBold(MaterialTheme.colorScheme.secondary))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (ui.canAdjust) {
                        InventoryActionRow(
                            icon = Icons.Outlined.AddCircleOutline,
                            title = "Actualizar stock",
                            subtitle = "Entradas y salidas manuales de inventario.",
                            onClick = { activeSheet = InventoryActionSheet.ADJUST },
                        )
                    }
                    if (ui.canTransfer) {
                        InventoryActionRow(
                            icon = Icons.Outlined.SwapHoriz,
                            title = "Transferir stock",
                            subtitle = "Mueve unidades entre almacenes.",
                            onClick = { activeSheet = InventoryActionSheet.TRANSFER },
                        )
                    }
                    if (ui.canCount) {
                        InventoryActionRow(
                            icon = Icons.Outlined.Assignment,
                            title = "Conteo físico",
                            subtitle = "Compara lo contado con el stock del sistema.",
                            onClick = { activeSheet = InventoryActionSheet.COUNT },
                        )
                    }
                    InventoryActionRow(
                        icon = Icons.Outlined.NotificationsNone,
                        title = "Alertas de mínimo",
                        subtitle = "Productos por debajo del stock mínimo.",
                        onClick = { activeSheet = InventoryActionSheet.ALERTS },
                    )
                    InventoryActionRow(
                        icon = Icons.Outlined.HomeWork,
                        title = "Almacenes",
                        subtitle = "Consulta y crea ubicaciones de inventario.",
                        onClick = { activeSheet = InventoryActionSheet.WAREHOUSES },
                    )
                }
            }
        }
        OutlinedButtonM(onClick = onBack) {
            Text("Volver")
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun InventoryKpiCard(
    summary: InventoryDashboardSummary,
    onClick: () -> Unit,
) {
    InventorySectionCard(
        icon = Icons.Outlined.Inventory,
        title = "Stock actual",
        onClick = onClick,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KpiCell(
                "Unidades disponibles",
                InventoryKardexSupport.formatInventoryQuantity(summary.availableUnits),
                Modifier.weight(1f),
            )
            KpiCell("Valor del stock", formatNumberToMoney(summary.stockValue), Modifier.weight(1f))
            KpiCell(
                "Costo promedio",
                summary.averageUnitCost?.let { formatNumberToMoney(it) } ?: "—",
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun InventoryOrdersCard(
    summary: InventoryDashboardSummary,
    onClick: () -> Unit,
) {
    InventorySectionCard(
        icon = Icons.Outlined.ReceiptLong,
        title = "Órdenes",
        onClick = onClick,
    ) {
        Text("Últimos 30 días", style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant))
        Text(
            "Unidades consumidas de inventario a precio de venta del catálogo.",
            style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KpiCell(
                "Unidades usadas",
                InventoryKardexSupport.formatInventoryQuantity(summary.salesUnits30d),
                Modifier.weight(1f),
            )
            KpiCell("Monto total", formatNumberToMoney(summary.salesAmount30d), Modifier.weight(1f))
        }
    }
}

@Composable
private fun InventoryActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .background(cardContainerColor())
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(vanishedBackgroundColor()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = bodyMediumBold(MaterialTheme.colorScheme.secondary))
            Text(subtitle, style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant))
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InventorySectionCard(
    icon: ImageVector,
    title: String,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(vanishedBackgroundColor()),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    title,
                    style = bodyMediumBold(MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            content()
        }
    }
}

@Composable
private fun KpiCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(label, style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant))
        Text(
            value,
            style = bodyMediumBold(MaterialTheme.colorScheme.secondary),
            textAlign = TextAlign.Start,
        )
    }
}

@Composable
private fun AdjustStockSheet(
    ui: InventoryOpsState,
    viewModel: InventoryOpsViewModel,
    onDismiss: () -> Unit,
) {
    InventorySheetScaffold(title = "Actualizar stock") {
        Text(
            "Registra entradas o salidas manuales de inventario.",
            style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
        )
        InventoryProductAutocompleteField(
            query = ui.productQuery,
            suggestions = ui.productSuggestions,
            onQueryChange = viewModel::onProductQueryChange,
            onSelect = viewModel::selectProduct,
        )
        LocationDropdown(
            label = "Ubicación",
            locations = ui.locations,
            selectedId = ui.adjustLocationId,
            onSelected = { viewModel.onAdjustLocation(it.toString()) },
        )
        AdjustOperationToggle(
            selected = ui.adjustOperation,
            onSelected = viewModel::onAdjustOperation,
        )
        DMOutlinedTextField(
            text = ui.quantity,
            label = "Cantidad",
            onChange = viewModel::onQuantity,
            modifier = Modifier.fillMaxWidth(),
        )
        DMOutlinedTextField(
            text = ui.adjustReason,
            label = "Motivo",
            onChange = viewModel::onAdjustReason,
            modifier = Modifier.fillMaxWidth(),
        )
        DMOutlinedTextField(
            text = ui.adjustUnitCost,
            label = "Costo unitario (opcional)",
            onChange = viewModel::onAdjustUnitCost,
            modifier = Modifier.fillMaxWidth(),
        )
        SheetActions(
            loading = ui.loading,
            onSubmit = viewModel::adjustStock,
            onDismiss = onDismiss,
            submitLabel = "Aplicar ajuste",
            submitContainerColor = MaterialTheme.colorScheme.secondary,
            submitContentColor = MaterialTheme.colorScheme.onSecondary,
        )
    }
}

@Composable
private fun TransferStockSheet(
    ui: InventoryOpsState,
    viewModel: InventoryOpsViewModel,
    onDismiss: () -> Unit,
) {
    InventorySheetScaffold(title = "Transferir stock") {
        Text(
            "Origen y destino deben ser almacenes distintos.",
            style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
        )
        LocationDropdown(
            label = "Origen",
            locations = ui.locations,
            selectedId = ui.fromLocationId,
            onSelected = { viewModel.onFromLocation(it.toString()) },
        )
        LocationDropdown(
            label = "Destino",
            locations = ui.locations,
            selectedId = ui.toLocationId,
            onSelected = { viewModel.onToLocation(it.toString()) },
        )
        InventoryProductAutocompleteField(
            query = ui.productQuery,
            suggestions = ui.productSuggestions,
            onQueryChange = viewModel::onProductQueryChange,
            onSelect = viewModel::selectProduct,
        )
        DMOutlinedTextField(
            text = ui.quantity,
            label = "Cantidad",
            onChange = viewModel::onQuantity,
            modifier = Modifier.fillMaxWidth(),
        )
        SheetActions(
            loading = ui.loading,
            onSubmit = viewModel::transfer,
            onDismiss = onDismiss,
            submitLabel = "Transferir",
        )
    }
}

@Composable
private fun PhysicalCountSheet(
    ui: InventoryOpsState,
    viewModel: InventoryOpsViewModel,
    onDismiss: () -> Unit,
) {
    InventorySheetScaffold(title = "Conteo físico") {
        Text(
            "Compara lo contado con el stock del sistema.",
            style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
        )
        LocationDropdown(
            label = "Ubicación",
            locations = ui.locations,
            selectedId = ui.countLocationId,
            onSelected = { viewModel.onCountLocation(it.toString()) },
        )
        InventoryProductAutocompleteField(
            query = ui.productQuery,
            suggestions = ui.productSuggestions,
            onQueryChange = viewModel::onProductQueryChange,
            onSelect = viewModel::selectProduct,
        )
        DMOutlinedTextField(
            text = ui.countedQty,
            label = "Cantidad contada",
            onChange = viewModel::onCountedQty,
            modifier = Modifier.fillMaxWidth(),
        )
        SheetActions(
            loading = ui.loading,
            onSubmit = viewModel::commitCount,
            onDismiss = onDismiss,
            submitLabel = "Aplicar conteo",
        )
    }
}

@Composable
private fun MinAlertsSheet(
    ui: InventoryOpsState,
    viewModel: InventoryOpsViewModel,
) {
    InventorySheetScaffold(title = "Alertas de mínimo") {
        LocationDropdown(
            label = "Filtrar por ubicación (opcional)",
            locations = ui.locations,
            selectedId = ui.alertsLocationId,
            onSelected = { id ->
                viewModel.onAlertsLocation(if (id <= 0) "" else id.toString())
                viewModel.loadAlerts()
            },
            allowEmpty = true,
        )
        OutlinedButtonM(
            onClick = viewModel::loadAlerts,
            enabled = !ui.loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Actualizar alertas")
        }
        if (ui.alerts.isEmpty()) {
            Text("Sin alertas.", style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ui.alerts.forEach { row ->
                    InventoryAlertCard(row)
                }
            }
        }
    }
}

@Composable
private fun InventorySheetScaffold(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = titleMediumBold(MaterialTheme.colorScheme.secondary))
        content()
    }
}

@Composable
private fun ColumnScope.SheetActions(
    loading: Boolean,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    submitLabel: String,
    submitContainerColor: Color? = null,
    submitContentColor: Color? = null,
) {
    ButtonM(
        onClick = onSubmit,
        enabled = !loading,
        modifier = Modifier.fillMaxWidth(),
        containerColor = submitContainerColor,
        contentColor = submitContentColor,
    ) {
        Text(submitLabel)
    }
    OutlinedButtonM(
        onClick = onDismiss,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Cerrar")
    }
}

@Composable
private fun LocationDropdown(
    label: String,
    locations: List<InventoryLocationOption>,
    selectedId: String,
    onSelected: (Int) -> Unit,
    allowEmpty: Boolean = false,
) {
    val stockable = locations.filter { it.stockable }
    val items = if (allowEmpty) {
        listOf(InventoryLocationOption(id = 0, name = "Todas las ubicaciones", stockable = true)) + stockable
    } else {
        stockable
    }
    val selectedIndex = items.indexOfFirst { it.id.toString() == selectedId }
    DMDropDownField(
        label = label,
        items = items,
        selectedIndex = selectedIndex,
        onItemSelected = { _, item -> onSelected(item.id) },
        selectedItemToString = { if (it.id == 0) it.name else "${it.name} (${it.id})" },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun AdjustOperationToggle(
    selected: String,
    onSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AdjustOperationOption(
            label = "Entrada",
            selected = selected == "increase",
            onClick = { onSelected("increase") },
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedBorderColor = MaterialTheme.colorScheme.secondary,
            selectedTextColor = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
        )
        AdjustOperationOption(
            label = "Salida",
            selected = selected == "decrease",
            onClick = { onSelected("decrease") },
            selectedContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
            selectedBorderColor = MaterialTheme.colorScheme.error,
            selectedTextColor = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AdjustOperationOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedContainerColor: Color,
    selectedBorderColor: Color,
    selectedTextColor: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    val borderColor = if (selected) selectedBorderColor else MaterialTheme.colorScheme.outlineVariant
    val background = if (selected) selectedContainerColor else cardContainerColor()
    val textColor = if (selected) selectedTextColor else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .clip(shape)
            .border(1.dp, borderColor, shape)
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = bodyMediumBold(textColor))
    }
}

@Composable
private fun InventoryAlertCard(row: InventoryAlertRow) {
    val status = InventoryOpsSupport.alertStatus(row)
    val statusLabel = InventoryOpsSupport.alertStatusLabel(status)
    val statusColor = when (status) {
        InventoryAlertStatus.BELOW_MIN -> MaterialTheme.colorScheme.error
        InventoryAlertStatus.IN_RANGE -> MaterialTheme.colorScheme.secondary
        InventoryAlertStatus.NO_THRESHOLD -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusBackground = when (status) {
        InventoryAlertStatus.BELOW_MIN -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
        InventoryAlertStatus.IN_RANGE -> MaterialTheme.colorScheme.secondaryContainer
        InventoryAlertStatus.NO_THRESHOLD -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val title = row.productName.takeIf { it.isNotBlank() } ?: "Producto ${row.itemId}"
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .background(cardContainerColor())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = bodyMediumBold(), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "ID ${row.itemId}",
                    style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }
            Surface(
                color = statusBackground,
                shape = RoundedCornerShape(999.dp),
            ) {
                Text(
                    statusLabel,
                    style = labelSmall(statusColor),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AlertMetricCell(
                label = "Disponible",
                value = InventoryKardexSupport.formatInventoryQuantity(row.available),
                modifier = Modifier.weight(1f),
            )
            AlertMetricCell(
                label = "Mínimo",
                value = InventoryKardexSupport.formatInventoryQuantity(row.minQty),
                modifier = Modifier.weight(1f),
            )
            AlertMetricCell(
                label = "Sugerido",
                value = InventoryKardexSupport.formatInventoryQuantity(row.suggestedQty),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AlertMetricCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(label, style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant))
        Text(value, style = bodyMediumBold(MaterialTheme.colorScheme.secondary))
    }
}
