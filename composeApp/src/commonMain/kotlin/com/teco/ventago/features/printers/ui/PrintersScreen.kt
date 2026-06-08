@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.teco.ventago.features.printers.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import com.teco.ventago.core.SnackbarService
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.bodySmall
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.features.printers.ui.viewmodel.PrinterEntryContext
import com.teco.ventago.features.printers.ui.viewmodel.PrintersUiEvent
import com.teco.ventago.features.printers.ui.viewmodel.PrintersViewModel
import com.teco.ventago.navigation.PrinterConfigRoute
import com.teco.ventago.navigation.PrinterOnboardingRoute
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PrintersScreen(
    navigate: (Any) -> Unit,
    viewModel: PrintersViewModel = koinViewModel<PrintersViewModel>(),
) {
    val snackbarService: SnackbarService = koinInject()
    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
        if (uiState.printers.isEmpty()) {
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

                item {
                    ButtonM(
                        onClick = {
                            navigate(
                                PrinterConfigRoute(
                                    entryContext = PrinterEntryContext.SETTINGS.name
                                )
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.secondary,
                    ) {
                        Text("Configurar nueva impresora")
                    }
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
