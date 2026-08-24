package com.teco.ventago.features.inventory.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Inventario", style = MaterialTheme.typography.titleLarge)
        if (ui.message.isNotBlank()) {
            Text(ui.message, color = MaterialTheme.colorScheme.primary)
        }
        if (!ui.enabled) {
            Text("El módulo de inventario no está activo.")
        } else {
            val locationHint = ui.locations.joinToString { "${it.id}=${it.name}" }.ifBlank { "Sin ubicaciones" }
            Text(locationHint, style = MaterialTheme.typography.bodySmall)

            OutlinedTextField(
                value = ui.itemId,
                onValueChange = viewModel::onItemId,
                label = { Text("Item ID") },
                modifier = Modifier.fillMaxWidth(),
            )

            if (ui.canTransfer) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Transferencia")
                        OutlinedTextField(
                            value = ui.fromLocationId,
                            onValueChange = viewModel::onFromLocation,
                            label = { Text("Origen ID") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = ui.toLocationId,
                            onValueChange = viewModel::onToLocation,
                            label = { Text("Destino ID") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = ui.quantity,
                            onValueChange = viewModel::onQuantity,
                            label = { Text("Cantidad") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(onClick = viewModel::transfer, enabled = !ui.loading) { Text("Transferir") }
                    }
                }
            }

            if (ui.canCount) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Conteo")
                        OutlinedTextField(
                            value = ui.countLocationId,
                            onValueChange = viewModel::onCountLocation,
                            label = { Text("Ubicación ID") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = ui.countedQty,
                            onValueChange = viewModel::onCountedQty,
                            label = { Text("Cantidad contada") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(onClick = viewModel::commitCount, enabled = !ui.loading) { Text("Postear conteo") }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Alertas below-min")
                        Button(onClick = viewModel::loadAlerts) { Text("Actualizar") }
                    }
                    if (ui.alerts.isEmpty()) {
                        Text("Sin alertas.")
                    } else {
                        ui.alerts.forEach { row ->
                            Text("Item ${row.itemId}: disp ${row.available} / min ${row.minQty} / sugerido ${row.suggestedQty}")
                        }
                    }
                }
            }
        }
        Button(onClick = onBack) { Text("Volver") }
    }
}
