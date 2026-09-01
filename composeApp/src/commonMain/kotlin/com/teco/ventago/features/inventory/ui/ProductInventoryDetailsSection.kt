package com.teco.ventago.features.inventory.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.features.inventory.domain.InventoryBalanceRow
import com.teco.ventago.features.inventory.domain.InventoryKardexSupport
import com.teco.ventago.features.inventory.domain.ProductInventoryDetails
import com.teco.ventago.utils.formatNumberToMoney

@Composable
fun ProductInventoryDetailsSection(
    details: ProductInventoryDetails,
    loading: Boolean,
    onOpenKardex: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!details.visible) return
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Inventory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text("Inventario", style = bodyMediumBold())
            }

            if (!details.tracked) {
                Text(
                    "Este producto no está marcado como inventariable.",
                    style = bodyMedium(MaterialTheme.colorScheme.onSurfaceVariant),
                )
                return@Column
            }

            if (loading && details.balances.isEmpty()) {
                Text("Cargando inventario…", style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant))
                return@Column
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                InventoryKpiTile(
                    label = "Disponible",
                    value = details.available,
                    modifier = Modifier.weight(1f),
                )
                InventoryKpiTile(
                    label = "Reservado",
                    value = details.reserved,
                    modifier = Modifier.weight(1f),
                )
                InventoryKpiTile(
                    label = "Alarma",
                    value = details.alarmLabel,
                    modifier = Modifier.weight(1f),
                    emphasized = details.alarmIsAlert,
                )
            }

            if (details.avgCost.isNotBlank()) {
                Text(
                    "Costo promedio: ${formatNumberToMoney(details.avgCost)}",
                    style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }
            if (details.minQty.isNotBlank()) {
                Text(
                    "Mínimo: ${InventoryKardexSupport.formatInventoryQuantity(details.minQty)}",
                    style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Stock disponible", style = bodyMediumBold())
                        Text("Kardex", style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant))
                    }
                    InventoryStockChart(
                        points = details.chartPoints,
                        minQty = details.minQty.takeIf { it.isNotBlank() },
                    )
                }
            }

            if (details.balances.isNotEmpty()) {
                InventoryBalancesTable(details.balances)
            }

            OutlinedButtonM(onClick = onOpenKardex, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.ListAlt, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Ver kardex")
            }
        }
    }
}

@Composable
private fun InventoryKpiTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (emphasized) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (emphasized) {
                Icon(
                    Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
            Text(label, style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant), textAlign = TextAlign.Center)
            Text(
                value,
                style = bodyMediumBold(
                    if (emphasized) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                ),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun InventoryBalancesTable(rows: List<InventoryBalanceRow>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Saldos por ubicación", style = bodyMediumBold())
        Row(Modifier.fillMaxWidth()) {
            Text("Ubicación", style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant), modifier = Modifier.weight(1.2f))
            Text("Estado", style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant), modifier = Modifier.weight(0.9f))
            Text("Cant.", style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant), modifier = Modifier.weight(0.6f), textAlign = TextAlign.End)
        }
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                Text(row.locationName, style = bodyMedium(), modifier = Modifier.weight(1.2f))
                Text(
                    InventoryKardexSupport.stockStateLabel(row.stockState),
                    style = bodyMedium(),
                    modifier = Modifier.weight(0.9f),
                )
                Text(
                    InventoryKardexSupport.formatInventoryQuantity(row.quantity),
                    style = bodyMediumBold(),
                    modifier = Modifier.weight(0.6f),
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}
