@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.teco.ventago.features.printers.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.teco.ventago.core.SnackbarService
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.bodySmall
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.titleMedium
import com.teco.ventago.design_system.theme.vanishedBackgroundColor
import com.teco.ventago.features.printers.ui.viewmodel.PrinterEntryContext
import com.teco.ventago.features.printers.ui.viewmodel.PrintersUiEvent
import com.teco.ventago.features.printers.ui.viewmodel.PrintersViewModel
import com.teco.ventago.navigation.BluetoothPrinterOnboardingRoute
import com.teco.ventago.navigation.PrinterConfigRoute
import com.teco.ventago.navigation.PrinterOnboardingRoute
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.ic_arrow_forward_ios

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintersScreenActions(navigate: (Any) -> Unit) {
    var showKindSheet by remember { mutableStateOf(false) }
    val kindSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    IconButton(onClick = { showKindSheet = true }) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "Agregar impresora",
            tint = MaterialTheme.colorScheme.secondary,
        )
    }

    if (showKindSheet) {
        ModalBottomSheet(
            onDismissRequest = { showKindSheet = false },
            sheetState = kindSheetState,
            containerColor = cardContainerColor(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("¿Cómo se conecta?", style = titleMedium())
                PrinterKindCard(
                    icon = Icons.Rounded.Wifi,
                    title = "Impresora Epson de red",
                    subtitle = "La usan varios equipos de la caja",
                    onClick = {
                        showKindSheet = false
                        navigate(
                            PrinterOnboardingRoute(
                                entryContext = PrinterEntryContext.SETTINGS.name,
                                openNetwork = true,
                            )
                        )
                    },
                )
                PrinterKindCard(
                    icon = Icons.Rounded.Bluetooth,
                    title = "Impresora Bluetooth",
                    subtitle = "Solo este celular",
                    onClick = {
                        showKindSheet = false
                        navigate(BluetoothPrinterOnboardingRoute)
                    },
                )
            }
        }
    }
}

@Composable
private fun PrinterKindCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(vanishedBackgroundColor()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = bodyMediumBold())
                Text(
                    subtitle,
                    style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }
            Icon(
                painter = painterResource(Res.drawable.ic_arrow_forward_ios),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
fun PrintersScreen(
    navigate: (Any) -> Unit,
    viewModel: PrintersViewModel = koinViewModel<PrintersViewModel>(),
) {
    val snackbarService: SnackbarService = koinInject()
    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshBluetoothReachability()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PrintersUiEvent.Message -> snackbarService.show(event.text)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        if (uiState.printers.isEmpty() && uiState.deviceBluetooth == null) {
            EmptyPrintersCard(
                onCreate = {
                    navigate(
                        PrinterOnboardingRoute(
                            entryContext = PrinterEntryContext.SETTINGS.name
                        )
                    )
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                uiState.deviceBluetooth?.let { bluetooth ->
                    item(key = "bluetooth-${bluetooth.id}") {
                        DeviceBluetoothCard(
                            name = bluetooth.name,
                            reachable = uiState.bluetoothReachable,
                            onUnlink = viewModel::unlinkBluetooth,
                        )
                    }
                }

                items(uiState.printers, key = { "${it.printerConfig.branchCode}-${it.printerConfig.billingPointCode}" }) { item ->
                    PrinterCard(
                        branchLabel = item.branchLabel,
                        billingPointLabel = item.billingPointLabel,
                        onEdit = {
                            navigate(
                                PrinterConfigRoute(
                                    entryContext = PrinterEntryContext.SETTINGS.name,
                                    branchCode = item.printerConfig.branchCode,
                                    billingPointCode = item.printerConfig.billingPointCode,
                                )
                            )
                        },
                        onDelete = { viewModel.promptDelete(item.printerConfig) },
                        printerModel = item.printerConfig.printerModel,
                        isActive = item.printerConfig.isActive,
                    )
                }
            }
        }
    }

    uiState.pendingDelete?.let {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeletePrompt,
            confirmButton = {
                TextButtonS(label = "Eliminar") { viewModel.deletePendingPrinter() }
            },
            dismissButton = {
                TextButtonS(label = "Cancelar") { viewModel.dismissDeletePrompt() }
            },
            title = { Text("Eliminar impresora") },
            text = {
                Text(
                    "Esta acción eliminará la configuración de ${it.printerModel} para ${it.branchCode} - ${it.billingPointCode}.",
                    style = bodyMedium()
                )
            },
            containerColor = cardContainerColor(),
        )
    }

    if (uiState.loadingBottomSheet.isLoading()) {
        LoadingSheet(
            state = uiState.loadingBottomSheet,
            sheetState = loadingSheetState,
        ) {
            viewModel.hideLoading()
        }
    }
}

@Composable
private fun DeviceBluetoothCard(
    name: String,
    reachable: Boolean?,
    onUnlink: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(vanishedBackgroundColor()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bluetooth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = bodyMediumBold())
                Text(
                    "Bluetooth · este equipo",
                    style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }
            if (reachable != null) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (reachable) Color(0xFF2E7D32) else Color(0xFFD32F2F)),
                )
            }
            IconButton(onClick = onUnlink) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Quitar impresora",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun EmptyPrintersCard(
    onCreate: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Aún no tienes impresoras configuradas", style = bodyMediumBold())
            Text(
                "Configura una impresora térmica para imprimir facturas directamente desde la app.",
                style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Spacer(modifier = Modifier.height(4.dp))
            ButtonM(onClick = onCreate) {
                Text("Configurar impresora")
            }
        }
    }
}

@Composable
private fun PrinterCard(
    printerModel: String,
    branchLabel: String,
    billingPointLabel: String,
    isActive: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    val statusText = if (isActive) "Habilitada" else "Deshabilitada"
    val statusContainerColor = if (isActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val statusTextColor = if (isActive) Color(0xFF2E7D32) else Color(0xFFD32F2F)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.padding(16.dp).weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = printerModel,
                    style = bodyMediumBold(),

                )
                Text(
                    text = "$branchLabel - $billingPointLabel",
                    style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusContainerColor
                ) {
                    Text(
                        text = statusText,
                        style = bodySmall(color = statusTextColor),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }



            IconButton(onClick = { isMenuExpanded = true }) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Opciones de impresora"
                )
            }
            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Modificar") },
                    onClick = {
                        isMenuExpanded = false
                        onEdit()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Eliminar") },
                    onClick = {
                        isMenuExpanded = false
                        onDelete()
                    }
                )
            }
        }


    }
}
