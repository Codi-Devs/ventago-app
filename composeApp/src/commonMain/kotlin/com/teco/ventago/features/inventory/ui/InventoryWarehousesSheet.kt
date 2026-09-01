package com.teco.ventago.features.inventory.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.textfields.helpers.DMDropDownField
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.titleMediumBold

@Composable
fun InventoryWarehousesSheet(
    ui: InventoryOpsState,
    viewModel: InventoryOpsViewModel,
) {
    val stockableLocations = InventoryOpsSupport.stockableLocations(ui.locations)
    val fallback = InventoryOpsSupport.findFallbackSaleLocation(ui.locations)
    val activeDefaults = InventoryOpsSupport.activeScopeDefaults(ui.scopeDefaults)
    val parentOptions = InventoryOpsSupport.parentWarehouseOptions(ui.locations)

    WarehouseSheetScaffold(title = "Almacenes") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Ubicaciones de inventario",
                style = bodyMediumBold(MaterialTheme.colorScheme.secondary),
            )
            IconButton(
                onClick = viewModel::loadWarehousesPanel,
                enabled = !ui.warehousesLoading,
            ) {
                if (ui.warehousesLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                }
            }
        }

        if (ui.locations.isEmpty()) {
            Text(
                "Aún no hay almacenes. Crea uno para empezar a controlar stock.",
                style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ui.locations.forEach { location ->
                    WarehouseLocationCard(
                        location = location,
                        locations = ui.locations,
                        canConfigure = ui.canConfigure,
                        isRetireTarget = ui.retireTargetLocationId == location.id,
                        retireReason = ui.retireReason,
                        loading = ui.loading,
                        onRetireClick = { viewModel.onRetireTarget(location.id) },
                        onRetireCancel = { viewModel.onRetireTarget(null) },
                        onRetireReasonChange = viewModel::onRetireReason,
                        onRetireConfirm = viewModel::retireWarehouse,
                    )
                }
            }
        }

        if (ui.canConfigure) {
            Spacer(Modifier.height(8.dp))
            Text("Agregar almacén", style = bodyMediumBold(MaterialTheme.colorScheme.secondary))
            DMOutlinedTextField(
                text = ui.warehouseCode,
                label = "Código",
                onChange = viewModel::onWarehouseCode,
                modifier = Modifier.fillMaxWidth(),
            )
            DMOutlinedTextField(
                text = ui.warehouseName,
                label = "Nombre",
                onChange = viewModel::onWarehouseName,
                modifier = Modifier.fillMaxWidth(),
            )
            WarehouseAdvancedSection(
                expanded = ui.warehouseAdvancedExpanded,
                locationType = ui.warehouseLocationType,
                parentLocationId = ui.warehouseParentLocationId,
                stockable = ui.warehouseStockable,
                parentOptions = parentOptions,
                onExpandedChange = viewModel::onWarehouseAdvancedExpanded,
                onLocationTypeChange = viewModel::onWarehouseLocationType,
                onParentChange = viewModel::onWarehouseParentLocation,
                onStockableChange = viewModel::onWarehouseStockable,
            )
            ButtonM(
                onClick = viewModel::createWarehouse,
                enabled = !ui.loading,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ) {
                Text("Crear")
            }
        } else if (ui.canView) {
            Text(
                "No tienes permisos para crear o retirar ubicaciones.",
                style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
            )
        }

        if (ui.canView) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text("Ubicación de venta", style = bodyMediumBold(MaterialTheme.colorScheme.secondary))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
            ) {
                Text(
                    "Si un punto no tiene configuración explícita, usará ${fallback?.let { InventoryOpsSupport.locationLabel(it) } ?: "Principal"}${fallback?.code?.let { " ($it)" }.orEmpty()}.",
                    style = bodyMedium(),
                    modifier = Modifier.padding(12.dp),
                )
            }

            Text(
                "Configurados (${activeDefaults.size})",
                style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.padding(top = 8.dp),
            )

            if (activeDefaults.isEmpty()) {
                Text(
                    "No hay puntos con almacén asignado explícitamente.",
                    style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    activeDefaults.forEach { mapped ->
                        ScopeDefaultCard(
                            mapped = mapped,
                            ui = ui,
                            stockableLocations = stockableLocations,
                            onLocationSelected = viewModel::onMappingLocationEdit,
                            onSave = { viewModel.saveScopeDefault(mapped.branchCode, mapped.billingPoint, mapped.version) },
                            onRemove = { viewModel.removeScopeDefault(mapped.branchCode, mapped.billingPoint, mapped.version) },
                        )
                    }
                }
            }

            if (ui.canConfigure && ui.unconfiguredScopes.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                WarehouseCollapsibleHeader(
                    title = "Agregar configuración",
                    expanded = ui.mappingAddExpanded,
                    onToggle = { viewModel.onMappingAddExpanded(!ui.mappingAddExpanded) },
                )
                if (ui.mappingAddExpanded) {
                    MappingAddForm(
                        ui = ui,
                        stockableLocations = stockableLocations,
                        onBranchChange = viewModel::onMappingAddBranchCode,
                        onPointChange = viewModel::onMappingAddBillingPoint,
                        onLocationChange = viewModel::onMappingAddLocationId,
                        onSave = viewModel::saveNewScopeDefault,
                        loading = ui.loading,
                    )
                }
            }
        }
    }
}

@Composable
private fun WarehouseLocationCard(
    location: InventoryLocationOption,
    locations: List<InventoryLocationOption>,
    canConfigure: Boolean,
    isRetireTarget: Boolean,
    retireReason: String,
    loading: Boolean,
    onRetireClick: () -> Unit,
    onRetireCancel: () -> Unit,
    onRetireReasonChange: (String) -> Unit,
    onRetireConfirm: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .background(cardContainerColor())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    InventoryOpsSupport.locationLabel(location),
                    style = bodyMediumBold(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    location.code?.let { "Código $it" } ?: "ID ${location.id}",
                    style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }
            Surface(
                color = if (location.active) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(999.dp),
            ) {
                Text(
                    if (location.active) "Activa" else "Retirada",
                    style = labelSmall(
                        if (location.active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WarehouseMetaChip("Tipo", InventoryOpsSupport.locationTypeLabel(location.locationType), Modifier.weight(1f))
            WarehouseMetaChip(
                "Inventariable",
                if (location.stockable) "Sí" else "No",
                Modifier.weight(1f),
            )
            WarehouseMetaChip(
                "Padre",
                InventoryOpsSupport.parentLabel(location, locations),
                Modifier.weight(1f),
            )
        }
        if (canConfigure && location.active) {
            if (isRetireTarget) {
                DMOutlinedTextField(
                    text = retireReason,
                    label = "Motivo de retiro",
                    onChange = onRetireReasonChange,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ButtonM(
                        onClick = onRetireConfirm,
                        enabled = !loading,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Confirmar retiro")
                    }
                    OutlinedButtonM(
                        onClick = onRetireCancel,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Cancelar")
                    }
                }
            } else {
                OutlinedButtonM(
                    onClick = onRetireClick,
                    modifier = Modifier.fillMaxWidth(),
                    contentColor = MaterialTheme.colorScheme.error,
                ) {
                    Text("Retirar")
                }
            }
        }
    }
}

@Composable
private fun WarehouseMetaChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Text(label, style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant))
        Text(value, style = bodyMediumBold(MaterialTheme.colorScheme.secondary), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun WarehouseAdvancedSection(
    expanded: Boolean,
    locationType: String,
    parentLocationId: String,
    stockable: Boolean,
    parentOptions: List<InventoryLocationOption>,
    onExpandedChange: (Boolean) -> Unit,
    onLocationTypeChange: (String) -> Unit,
    onParentChange: (String) -> Unit,
    onStockableChange: (Boolean) -> Unit,
) {
    WarehouseCollapsibleHeader(
        title = "Opciones avanzadas",
        expanded = expanded,
        onToggle = { onExpandedChange(!expanded) },
    )
    if (expanded) {
        val types = listOf("warehouse" to "Almacén", "bin" to "Bin")
        val typeIndex = types.indexOfFirst { it.first == locationType }.coerceAtLeast(0)
        DMDropDownField(
            label = "Tipo",
            items = types,
            selectedIndex = typeIndex,
            onItemSelected = { _, item -> onLocationTypeChange(item.first) },
            selectedItemToString = { it.second },
            modifier = Modifier.fillMaxWidth(),
        )
        if (locationType == "bin") {
            val parentItems = listOf(
                InventoryLocationOption(id = 0, name = "Selecciona almacén", stockable = false),
            ) + parentOptions
            val parentIndex = parentItems.indexOfFirst { it.id.toString() == parentLocationId }
            DMDropDownField(
                label = "Almacén padre",
                items = parentItems,
                selectedIndex = parentIndex,
                onItemSelected = { _, item -> onParentChange(if (item.id <= 0) "" else item.id.toString()) },
                selectedItemToString = { if (it.id <= 0) it.name else InventoryOpsSupport.locationLabel(it) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = stockable, onCheckedChange = onStockableChange)
            Text("Inventariable", style = bodyMedium())
        }
    }
}

@Composable
private fun WarehouseCollapsibleHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = bodyMediumBold())
        Icon(
            if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
        )
    }
}

@Composable
private fun ScopeDefaultCard(
    mapped: InventoryScopeDefault,
    ui: InventoryOpsState,
    stockableLocations: List<InventoryLocationOption>,
    onLocationSelected: (String, Int) -> Unit,
    onSave: () -> Unit,
    onRemove: () -> Unit,
) {
    val key = InventoryOpsSupport.scopeDefaultKey(mapped.branchCode, mapped.billingPoint)
    val (branch, point) = InventoryOpsSupport.resolveBranchPoint(
        ui.mappingBranches,
        mapped.branchCode,
        mapped.billingPoint,
    )
    val selectedLocationId = ui.mappingLocationEdits[key] ?: mapped.locationId
    val selectedIndex = stockableLocations.indexOfFirst { it.id == selectedLocationId }
    val isFallback = InventoryOpsSupport.isImplicitFallbackDefault(mapped, ui.locations)
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
        Column {
            Text(
                InventoryOpsSupport.scopeDisplayLabel(branch, point),
                style = bodyMediumBold(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                InventoryOpsSupport.scopeDisplayMeta(branch, point),
                style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
            )
        }
        if (ui.canConfigure) {
            DMDropDownField(
                label = "Almacén de venta",
                items = stockableLocations,
                selectedIndex = selectedIndex,
                onItemSelected = { _, item -> onLocationSelected(key, item.id) },
                selectedItemToString = { InventoryOpsSupport.locationLabel(it) },
                modifier = Modifier.fillMaxWidth(),
            )
            if (isFallback) {
                Text(
                    "Igual al default",
                    style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ButtonM(
                    onClick = onSave,
                    enabled = !ui.loading,
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ) {
                    Text("Guardar")
                }
                OutlinedButtonM(
                    onClick = onRemove,
                    enabled = !ui.loading,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Quitar")
                }
            }
        } else {
            val location = ui.locations.firstOrNull { it.id == mapped.locationId }
            Text(
                "Almacén de venta: ${location?.let { InventoryOpsSupport.locationLabel(it) } ?: "No disponible"}",
                style = bodyMedium(),
            )
        }
    }
}

@Composable
private fun MappingAddForm(
    ui: InventoryOpsState,
    stockableLocations: List<InventoryLocationOption>,
    onBranchChange: (String) -> Unit,
    onPointChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onSave: () -> Unit,
    loading: Boolean,
) {
    val branchOptions = ui.unconfiguredScopes
        .map { it.branchCode to it.branchName }
        .distinctBy { it.first }
    val branchIndex = branchOptions.indexOfFirst { it.first == ui.mappingAddBranchCode }
    val pointOptions = ui.unconfiguredScopes.filter { it.branchCode == ui.mappingAddBranchCode }
    val pointIndex = pointOptions.indexOfFirst { it.billingPoint == ui.mappingAddBillingPoint }
    val locationIndex = stockableLocations.indexOfFirst { it.id.toString() == ui.mappingAddLocationId }

    if (branchOptions.isNotEmpty()) {
        DMDropDownField(
            label = "Sucursal",
            items = branchOptions,
            selectedIndex = branchIndex.coerceAtLeast(0),
            onItemSelected = { _, item -> onBranchChange(item.first) },
            selectedItemToString = { "${it.second} (${it.first})" },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (pointOptions.isNotEmpty()) {
        DMDropDownField(
            label = "Punto",
            items = pointOptions,
            selectedIndex = pointIndex.coerceAtLeast(0),
            onItemSelected = { _, item -> onPointChange(item.billingPoint) },
            selectedItemToString = { "${it.billingPointLabel} (${it.billingPoint})" },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (stockableLocations.isNotEmpty()) {
        DMDropDownField(
            label = "Almacén",
            items = stockableLocations,
            selectedIndex = locationIndex,
            onItemSelected = { _, item -> onLocationChange(item.id.toString()) },
            selectedItemToString = { InventoryOpsSupport.locationLabel(it) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    ButtonM(
        onClick = onSave,
        enabled = !loading,
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
    ) {
        Text("Guardar configuración")
    }
}

@Composable
private fun WarehouseSheetScaffold(
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
