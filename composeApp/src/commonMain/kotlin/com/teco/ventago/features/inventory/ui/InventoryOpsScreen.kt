package com.teco.ventago.features.inventory.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.utils.formatNumberToMoney

@Composable
fun InventoryOpsScreen(
    viewModel: InventoryOpsViewModel,
    onBack: () -> Unit,
) {
    val ui by viewModel.uiState.collectAsState()
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
                    InventoryKpiCard(summary)
                    InventoryOrdersCard(summary)
                }
            }

            DMOutlinedTextField(
                text = ui.itemId,
                label = "ID de producto",
                onChange = viewModel::onItemId,
                modifier = Modifier.fillMaxWidth(),
            )

            if (ui.canTransfer) {
                InventorySectionCard(
                    icon = Icons.Outlined.SwapHoriz,
                    title = "Transferir stock",
                ) {
                    Text(
                        "Origen y destino deben ser almacenes distintos. No transfiere reservado.",
                        style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                    LocationHint(ui.locations)
                    DMOutlinedTextField(
                        text = ui.fromLocationId,
                        label = "Origen ID",
                        onChange = viewModel::onFromLocation,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DMOutlinedTextField(
                        text = ui.toLocationId,
                        label = "Destino ID",
                        onChange = viewModel::onToLocation,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DMOutlinedTextField(
                        text = ui.quantity,
                        label = "Cantidad",
                        onChange = viewModel::onQuantity,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ButtonM(onClick = viewModel::transfer, enabled = !ui.loading) {
                        Text("Transferir")
                    }
                }
            }

            if (ui.canCount) {
                InventorySectionCard(
                    icon = Icons.Outlined.Inventory,
                    title = "Conteo físico",
                ) {
                    LocationHint(ui.locations)
                    DMOutlinedTextField(
                        text = ui.countLocationId,
                        label = "Ubicación ID",
                        onChange = viewModel::onCountLocation,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DMOutlinedTextField(
                        text = ui.countedQty,
                        label = "Cantidad contada",
                        onChange = viewModel::onCountedQty,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ButtonM(onClick = viewModel::commitCount, enabled = !ui.loading) {
                        Text("Aplicar conteo")
                    }
                }
            }

            if (ui.canView) {
                InventorySectionCard(
                    icon = Icons.Outlined.WarningAmber,
                    title = "Alertas de mínimo",
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Bajo mínimo", style = bodyMedium())
                        OutlinedButtonM(onClick = viewModel::loadAlerts, enabled = !ui.loading) {
                            Text("Actualizar")
                        }
                    }
                    if (ui.alerts.isEmpty()) {
                        Text("Sin alertas.", style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant))
                    } else {
                        ui.alerts.forEach { row ->
                            val status = when {
                                row.minQty.toDoubleOrNull() == null || (row.minQty.toDoubleOrNull() ?: 0.0) <= 0 -> "Sin umbral"
                                (row.available.toDoubleOrNull() ?: 0.0) < (row.minQty.toDoubleOrNull() ?: 0.0) -> "Bajo mínimo"
                                else -> "En rango"
                            }
                            Text(
                                "Item ${row.itemId}: disp ${row.available} · mín ${row.minQty} · $status · sugerido ${row.suggestedQty}",
                                style = bodyMedium(),
                            )
                        }
                    }
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
private fun InventoryKpiCard(summary: InventoryDashboardSummary) {
    InventorySectionCard(
        icon = Icons.Outlined.Inventory,
        title = "Stock actual",
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KpiCell("Unidades disponibles", summary.availableUnits, Modifier.weight(1f))
            KpiCell("Valor del stock", formatNumberToMoney(summary.stockValue), Modifier.weight(1f))
            KpiCell("Costo promedio", summary.averageUnitCost?.let { formatNumberToMoney(it) } ?: "—", Modifier.weight(1f))
        }
    }
}

@Composable
private fun InventoryOrdersCard(summary: InventoryDashboardSummary) {
    InventorySectionCard(
        icon = Icons.Outlined.Inventory,
        title = "Órdenes",
    ) {
        Text("Últimos 30 días", style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant))
        Text(
            "Unidades consumidas de inventario a precio de venta del catálogo.",
            style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KpiCell("Unidades usadas", summary.salesUnits30d, Modifier.weight(1f))
            KpiCell("Monto total", formatNumberToMoney(summary.salesAmount30d), Modifier.weight(1f))
        }
    }
}

@Composable
private fun InventorySectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                Text(title, style = bodyMediumBold())
            }
            content()
        }
    }
}

@Composable
private fun KpiCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(label, style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant))
        Text(value, style = bodyMediumBold(MaterialTheme.colorScheme.primary), textAlign = TextAlign.Start)
    }
}

@Composable
private fun LocationHint(locations: List<InventoryLocationOption>) {
    val hint = if (locations.isEmpty()) {
        "Sin ubicaciones"
    } else {
        locations.joinToString { "${it.name} (${it.id})" }
    }
    Text(hint, style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant))
}
