package com.teco.ventago.features.branches.ui.billing_point.manage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.toRoute
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.molecules.FiscalBillingPointItem
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.headlineSmall
import com.teco.ventago.design_system.theme.labelLarge
import com.teco.ventago.design_system.theme.vanishedBackgroundColor
import com.teco.ventago.features.printers.domain.PrinterService
import com.teco.ventago.features.printers.ui.viewmodel.PrinterEntryContext
import com.teco.ventago.features.branches.ui.billing_point.manage.viewmodel.BillingPointsManageViewModel
import com.teco.ventago.features.branches.domain.model.FiscalBillingPoint
import com.teco.ventago.navigation.AddBillingPointRoute
import com.teco.ventago.navigation.BillingPointManageRoute
import com.teco.ventago.navigation.EditBillingPointRoute
import com.teco.ventago.navigation.PrinterConfigRoute
import com.teco.ventago.navigation.PrinterOnboardingRoute
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.cancel
import ventago.composeapp.generated.resources.delete


@Composable
fun BillingPointManageActions(backStackEntry: NavBackStackEntry?,
                              onNavigate: (Any) -> Unit) {
    val args = backStackEntry!!.toRoute<BillingPointManageRoute>()
    val branchCode = args.branchCode

    // Same owner + same parameters => same VM instance
    val viewModel: BillingPointsManageViewModel = koinViewModel(
        viewModelStoreOwner = backStackEntry,
        parameters = { parametersOf(branchCode) }
    )

    val uiState by viewModel.uiState.collectAsState()

    IconButton(onClick = {
        onNavigate(AddBillingPointRoute(uiState.selectedBranchCode ?: branchCode))
    }) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingPointManageScreen(
    viewModel: BillingPointsManageViewModel,
    onNavigate: (Any) -> Unit,
) {

    val uiState by viewModel.uiState.collectAsState()
    val printerService: PrinterService = koinInject()
    val loadingSheetState = rememberModalBottomSheetState(confirmValueChange = { false })
    val lazyListState = rememberLazyListState()

    var showBillingPointEdit by remember { mutableStateOf(false) }
    val billingPointEditSheetState = rememberModalBottomSheetState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var selectedBillingPoint by remember { mutableStateOf<FiscalBillingPoint?>(null) }

    Column(
        modifier = Modifier
            .padding(horizontal = 0.dp)
            .fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = vanishedBackgroundColor(), shape = RoundedCornerShape(12.dp)
                )
                .padding(top = 8.dp, bottom = 8.dp),
            state = lazyListState,
        ) {
            items(uiState.billingPoints, key = { it.billingPoint }) { billingPoints ->
                val hasConfiguredPrinter = uiState.selectedBranchCode?.let { selectedBranchCode ->
                    printerService.getCachedPrinters().any { printer ->
                        printer.branchCode == selectedBranchCode &&
                            printer.billingPointCode == billingPoints.billingPoint
                    }
                } ?: false

                FiscalBillingPointItem(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 8.dp
                    ),
                    billingPoint = billingPoints,
                    hasConfiguredPrinter = hasConfiguredPrinter,
                    onClick = {},
                    onOptionsClick = {
                        selectedBillingPoint = billingPoints
                        showBillingPointEdit = true
                    }
                )
            }

        }

        if (uiState.loadingBottomSheet.isLoading()) {
            LoadingSheet(
                state = uiState.loadingBottomSheet,
                sheetState = loadingSheetState
            ) {
                viewModel.hideLoading()
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteDialog = false
                },
                confirmButton = {
                    TextButtonS(label = stringResource(Res.string.delete)) {
                        showDeleteDialog = false
                        viewModel.deleteBillingPoint(selectedBillingPoint?.billingPoint ?: "")
                        scope.launch { billingPointEditSheetState.hide() }.invokeOnCompletion {
                            if (!billingPointEditSheetState.isVisible) {
                                showBillingPointEdit = false
                                selectedBillingPoint = null
                            }
                        }
                    }
                },
                dismissButton = {
                    TextButtonS(label = stringResource(Res.string.cancel)) {
                        showDeleteDialog = false
                    }
                },
                title = {
                    Text(
                        text = "Eliminar punto", style = headlineSmall()
                    )
                },
                text = {
                    Text(
                        text = "Esta seguro que desea eliminar el punto de facturación? Esta acción no se puede deshacer.",
                        style = bodyMedium(MaterialTheme.colorScheme.onSurfaceVariant).merge(textAlign = TextAlign.Start),
                    )
                },
                containerColor = cardContainerColor(),
            )
        }

        if (showBillingPointEdit) {
            ModalBottomSheet(containerColor = cardContainerColor(), onDismissRequest = {
                showBillingPointEdit = false
                selectedBillingPoint = null
            }, sheetState = billingPointEditSheetState) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ButtonM(
                            modifier = Modifier.weight(1f, fill = true),
                            onClick = {
                                onNavigate(EditBillingPointRoute(uiState.selectedBranchCode ?: "", selectedBillingPoint?.billingPoint ?: ""))
//                                navigateRoute(PosScreens.EditBillingPointScreen.editBillingPointRoute(uiState.selectedBranchCode ?: "", selectedBillingPoint?.billingPoint ?: ""))
                                scope.launch { billingPointEditSheetState.hide() }.invokeOnCompletion {
                                    if (!billingPointEditSheetState.isVisible) {
                                        showBillingPointEdit = false
                                        selectedBillingPoint = null
                                    }
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                        ) {
                            Text(text = "Modificar", style = labelLarge().copy(color = MaterialTheme.colorScheme.onPrimary))
                        }

                        if (selectedBillingPoint?.billingPoint != "001") {
                            ButtonM(
                                modifier = Modifier.weight(1f, fill = true),
                                onClick = {
                                    showDeleteDialog = true
                                },
                                containerColor = MaterialTheme.colorScheme.error,
                            ) {
                                Text(text = stringResource(Res.string.delete), style = labelLarge().copy(color = MaterialTheme.colorScheme.onPrimary))
                            }
                        }
                    }

                    OutlinedButtonM(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val branchCode = uiState.selectedBranchCode ?: ""
                            val billingPointCode = selectedBillingPoint?.billingPoint ?: ""
                            val existingPrinter = printerService.getCachedPrinters().firstOrNull {
                                it.branchCode == branchCode && it.billingPointCode == billingPointCode
                            }
                            onNavigate(
                                if (existingPrinter != null) {
                                    PrinterConfigRoute(
                                        entryContext = PrinterEntryContext.BRANCH.name,
                                        branchCode = branchCode,
                                        billingPointCode = billingPointCode
                                    )
                                } else {
                                    PrinterOnboardingRoute(
                                        entryContext = PrinterEntryContext.BRANCH.name,
                                        branchCode = branchCode,
                                        billingPointCode = billingPointCode
                                    )
                                }
                            )
                            scope.launch { billingPointEditSheetState.hide() }.invokeOnCompletion {
                                if (!billingPointEditSheetState.isVisible) {
                                    showBillingPointEdit = false
                                    selectedBillingPoint = null
                                }
                            }
                        },
                        contentColor = MaterialTheme.colorScheme.secondary,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                    ) {
                        Text(
                            text = "Configurar impresora",
                            style = labelLarge().copy(color = MaterialTheme.colorScheme.secondary)
                        )
                    }
                }
            }
        }
    }

}
