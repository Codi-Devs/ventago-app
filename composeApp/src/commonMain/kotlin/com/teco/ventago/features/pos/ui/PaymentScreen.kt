package com.teco.ventago.features.pos.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavOptionsBuilder
import com.teco.ventago.AppViewModel
import com.teco.ventago.core.firebase.AnalyticsService
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.TextButtonM
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.molecules.InstallmentDueDateFieldKmp
import com.teco.ventago.design_system.molecules.DMAlertDialog
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.organism.PriceElevatedDecimalText
import com.teco.ventago.design_system.organism.ShortcutItem
import com.teco.ventago.design_system.textfields.DMMoneyOutlinedTextField
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.theme.Gray30
import com.teco.ventago.design_system.theme.bodyLargeBold
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.bodySmall
import com.teco.ventago.design_system.theme.headlineMediumBold
import com.teco.ventago.design_system.theme.headlineSmall
import com.teco.ventago.design_system.theme.labelLarge
import com.teco.ventago.design_system.theme.labelMediumBold
import com.teco.ventago.design_system.theme.titleLarge
import com.teco.ventago.design_system.theme.vanishedBackgroundColor
import com.teco.ventago.features.pos.ui.viewmodel.PaymentFlowMode
import com.teco.ventago.features.pos.ui.viewmodel.PosState
import com.teco.ventago.features.pos.ui.viewmodel.PosViewModel
import com.teco.ventago.features.product.ui.item.add.viewmodel.ItemEditMode
import com.teco.ventago.navigation.PosNoteRoute
import com.teco.ventago.navigation.PosScreens
import com.teco.ventago.utils.formatNumberToMoney
import com.teco.ventago.utils.toDecimalString
import com.teco.ventago.utils.toScaledDouble
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.cancel
import ventago.composeapp.generated.resources.creating_order
import ventago.composeapp.generated.resources.error_try_later
import ventago.composeapp.generated.resources.ic_paypal_logo
import ventago.composeapp.generated.resources.payment_link
import ventago.composeapp.generated.resources.pos_add_tip
import ventago.composeapp.generated.resources.pos_card
import ventago.composeapp.generated.resources.pos_cash
import ventago.composeapp.generated.resources.pos_change
import ventago.composeapp.generated.resources.pos_charge
import ventago.composeapp.generated.resources.pos_discount_amount
import ventago.composeapp.generated.resources.pos_discount_percent
import ventago.composeapp.generated.resources.pos_missing
import ventago.composeapp.generated.resources.pos_other
import ventago.composeapp.generated.resources.pos_received_amount
import ventago.composeapp.generated.resources.pos_tip
import ventago.composeapp.generated.resources.pos_tips

@Composable
fun PaymentScreen(
    appViewModel: AppViewModel,
    viewModel: PosViewModel,
    navigate: (PosScreens, (NavOptionsBuilder.() -> Unit)?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    if (remember { uiState.creatingOrderState.isLoading }.value) {
        appViewModel.setHideAppVar(true)
        CreatingOrderContent()
    } else {
        appViewModel.setHideAppVar(false)
        PaymentScreenContent(viewModel, navigate)
    }

    if (remember { uiState.creatingOrderState.isSuccess }.value) {
        navigate(PosScreens.SuccessScreen) {
//            popUpTo(PosScreens.POS.name) {
//                inclusive = true
//            }
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun CreatingOrderContent() {
    val snackbarHostState = remember { SnackbarHostState() }
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/61209-loading-loop.json").decodeToString()
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                modifier = Modifier.size(height = 180.dp, width = 180.dp),
                painter = rememberLottiePainter(
                    composition = composition,
                    iterations = Compottie.IterateForever
                ),
                contentDescription = "Lottie animation"
            )

            Text(
                text = stringResource(Res.string.creating_order),
                modifier = Modifier.padding(top = 100.dp),
                style = headlineSmall().merge(TextStyle(fontWeight = FontWeight(400)))
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreenContent(
    viewModel: PosViewModel,
    navigate: (PosScreens, (NavOptionsBuilder.() -> Unit)?) -> Unit,
//    onConfirmManualOrInstallments: () -> Unit, // send invoice immediately (manual) or with installments
//    onConfirmPaymentLink: () -> Unit           // create payment link; invoice on backend after paid
) {
    val ui by viewModel.uiState.collectAsState()

    // Amounts
    val legal = remember(ui) { viewModel.legalInvoiceTotal() }
    val tips = remember(ui) { viewModel.tipsTotal() }
    val totalToCharge = remember(ui) { viewModel.amountToCharge() }
    val remaining = remember(ui) { viewModel.remainingToAllocate() }
    val hasPositiveAmount = totalToCharge > 0L

    val loadingSheetState = rememberModalBottomSheetState(confirmValueChange = { false })

    var showGovernmentWarning by remember { mutableStateOf(false) }
    var governmentInvalidProducts by remember { mutableStateOf<List<String>>(emptyList()) }
    var pendingGovernmentAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun requestGovernmentWarningOrProceed(action: () -> Unit) {
        val invalidProducts = viewModel.governmentWarningInvalidProducts()
        if (invalidProducts.isNotEmpty()) {
            governmentInvalidProducts = invalidProducts
            pendingGovernmentAction = action
            showGovernmentWarning = true
        } else {
            action()
        }
    }

//    var editMod

    Column(
        modifier = Modifier
            .padding()
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    )
    {
        // Totals
        ElevatedCard(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = vanishedBackgroundColor()
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal legal (sin propina)")
                    Text(formatNumberToMoney((legal / 100.0).toString()))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Propina")
                    Text(formatNumberToMoney((tips / 100.0).toString()))
                }
                Divider(Modifier.padding(vertical = 8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total a cobrar", style = bodyMediumBold())
                    Text(
                        formatNumberToMoney((totalToCharge / 100.0).toString()),
                        style = bodyMediumBold(color = MaterialTheme.colorScheme.primary)
                    )
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { viewModel.openTipsSheet() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                ) { Text("Añadir propina") }
            }
        }

        // Credit notes (04) and debit notes (05) cannot use payment links or be saved as drafts
        val isCreditOrDebitNote = ui.selectedDocType == "04" || ui.selectedDocType == "05"

        // Only show tabs if not a credit/debit note
        if (!isCreditOrDebitNote) {
            val modes = listOf(PaymentFlowMode.MANUAL_OR_INSTALLMENTS, PaymentFlowMode.PAYMENT_LINK)
            val labels = listOf("Manual/Cuotas", "Enlace de Pago")
            TabRow(
                selectedTabIndex = modes.indexOf(ui.paymentFlowMode),
                modifier = Modifier,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[modes.indexOf(ui.paymentFlowMode)]),
                        color = MaterialTheme.colorScheme.secondary
                    )
                },
                containerColor = MaterialTheme.colorScheme.background) {
                modes.forEachIndexed { i, m ->
                    Tab(
                        selected = (m == ui.paymentFlowMode),
                        onClick = {
                            viewModel.setPaymentFlow(m)
                        },
                        text = { Text(labels[i]) }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // For credit/debit notes, always show manual payment (no payment links or drafts)
        if (isCreditOrDebitNote || ui.paymentFlowMode == PaymentFlowMode.MANUAL_OR_INSTALLMENTS) {
            ManualAndInstallmentsSection(
                viewModel = viewModel,
                legal = legal,
                tips = tips,
                totalToCharge = totalToCharge,
                remaining = remaining,
                onToggleMethod = viewModel::toggleManualMethod,
                onAmountChange = viewModel::setManualAmount,
                onOtherDesc = viewModel::setOtherDescription,
                onAddInstallment = viewModel::addInstallment,
                onRemoveInstallment = viewModel::removeInstallment,
                onInstallmentAmount = viewModel::setInstallmentAmount,
                onInstallmentDate = viewModel::setInstallmentDueDate,
                methodOptions = viewModel.manualMethodOptions(),
                selectedDocType = ui.selectedDocType,
                onConfirm = {
                    requestGovernmentWarningOrProceed {
                        viewModel.createOrder(createPaymentLink = false, saveAsDraft = false)
                    }
                },
                onSaveDraft = {
                    requestGovernmentWarningOrProceed {
                        viewModel.createOrder(createPaymentLink = false, saveAsDraft = true)
                    }
                },
                // Disable save draft for credit/debit notes
                saveDraftEnabled = hasPositiveAmount && !isCreditOrDebitNote
            )
        } else {
            PaymentLinkSection(
                totalToCharge = totalToCharge,
                enabled = hasPositiveAmount,
                onConfirm = {
                    requestGovernmentWarningOrProceed {
                        if (!ui.paymentsConfigured) {
                            navigate(PosScreens.Payments, null)
                        } else {
                            viewModel.createOrder(createPaymentLink = true, saveAsDraft = false)
                        }
                    }
                }
            )
        }
    }

    if (ui.loadingBottomSheet.isLoading()) {
        LoadingSheet(
            state = ui.loadingBottomSheet,
            sheetState = loadingSheetState
        ) {
            viewModel.hideLoading()

            navigate(PosScreens.SuccessScreen) {
                popUpTo(PosScreens.POSScreen.name) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    if (showGovernmentWarning) {
        val warningMessage = buildString {
            append("El RUC del cliente parece de gobierno. ")
            append("Las facturas para gobierno requieren que cada producto tenga configurado:\n")
            append("- Código de bienes/servicios de Panamá\n")
            append("- Unidad de bienes/servicios de Panamá\n\n")
            append("Productos sin estos valores:\n")
            governmentInvalidProducts.forEach { name ->
                append("- ").append(name).append("\n")
            }
        }.trimEnd()

        DMAlertDialog(
            title = "Advertencia: Posible factura de gobierno",
            message = warningMessage,
            show = showGovernmentWarning,
            onDismiss = {
                showGovernmentWarning = false
                pendingGovernmentAction = null
            },
            onConfirm = {
                showGovernmentWarning = false
                val action = pendingGovernmentAction
                pendingGovernmentAction = null
                action?.invoke()
            },
            confirmText = "Continuar",
            dismissText = "Volver"
        )
    }

    // Tips bottom sheet
    if (ui.showTipsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
            onDismissRequest = { viewModel.closeTipsSheet() }
        ) {
            TipsEditor(
                isPercent = ui.tipIsPercentage,
                percent = viewModel.tipPercent(),
                fixedCents = viewModel.tipFixedCents(),
                onMode = { viewModel.setTipIsPercent(it) },
                onPercent = { viewModel.setTipPercentRaw(it) },
                onFixedMajor = { viewModel.setTipAmountRaw(it) },
                onClose = { viewModel.closeTipsSheet() }
            )
        }
    }
}

@Composable
private fun ManualAndInstallmentsSection(
    viewModel: PosViewModel,
    legal: Long,
    tips: Long,
    totalToCharge: Long,
    remaining: Long,
    methodOptions: List<Pair<Int, String>>,
    selectedDocType: String,
    onToggleMethod: (Int, Boolean) -> Unit,
    onAmountChange: (Int, Long) -> Unit,
    onOtherDesc: (String) -> Unit,
    onAddInstallment: () -> Unit,
    onRemoveInstallment: (Int) -> Unit,
    onInstallmentAmount: (Int, Long) -> Unit,
    onInstallmentDate: (Int, String) -> Unit,
    onConfirm: () -> Unit,
    onSaveDraft: () -> Unit,
    saveDraftEnabled: Boolean
) {
    val ui by viewModel.uiState.collectAsState()

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = vanishedBackgroundColor()
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Pagos manuales", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            fun allocatedNowCents(): Long =
                ui.charged.values.sum() + ui.installments.sumOf { it.amountCents }

            // Method chips
            FlowRow() {
                methodOptions.forEach { (code, label) ->
                    val selected = ui.charged.containsKey(code)
                    FilterChip(
                        modifier = Modifier.padding(end = 4.dp),
                        selected = selected,
                        onClick = {
                            val newSelected = !selected
                            if (newSelected) {
                                // Prefill with REMAINING at the moment of selection.
                                // Example: total 2000, already allocated 1500 → prefill 500.
                                val remainingForPrefill =
                                    (totalToCharge - allocatedNowCents()).coerceAtLeast(0L)

                                onToggleMethod(code, true)
                                onAmountChange(code, remainingForPrefill)
                            } else {
                                // Turning OFF: just toggle off, no amount change needed
                                onToggleMethod(code, false)
                            }
                        },
                        label = { Text(label) }
                    )
                }
            }

            // Amount inputs for selected methods
            ui.charged.keys.sorted().forEach { code ->
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(methodOptions.find { it.first == code }?.second ?: code.toString())
                    Spacer(Modifier.weight(1f).width(8.dp))
                    DMMoneyOutlinedTextField(
                        text = (ui.charged[code] ?: 0L).toString(),
                        label = "Monto",
                        onChange = { digits ->
                            val cents = digits.filter(Char::isDigit).toLongOrNull() ?: 0L
                            onAmountChange(code, cents)
                        },
                        modifier = Modifier.widthIn(min = 160.dp).padding(start = 8.dp),
                        leadingIcon = null,
                        maxLines = 1,
                        imeAction = ImeAction.Done
                    )
                }
                if (code == 11) {
                    DMOutlinedTextField(
                        text = ui.otherPaymentDescription,
                        label = "Descripción (requerida para 'Otro')",
                        onChange = onOtherDesc,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        maxLines = 2
                    )
                } else if (code == 2) {
                    Text(
                        "Se permite exceso de pago (se calculará cambio)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Divider(Modifier.padding(vertical = 12.dp))

            // Installments
            Text("Crédito/Plazo", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            ui.installments.forEachIndexed { idx, inst ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Cuota ${idx + 1}")
                            IconButton(onClick = { onRemoveInstallment(idx) }) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        DMMoneyOutlinedTextField(
                            text = if (inst.amountCents == 0L) "" else inst.amountCents.toString(),
                            label = "Monto",
                            onChange = { raw ->
                                // Filter digits only
                                val digits = raw.filter(Char::isDigit)
                                
                                // Handle empty input or remove leading zeros: "02" -> "2", "002" -> "2"
                                val cleanedDigits = digits.trimStart('0')
                                val cents = if (cleanedDigits.isEmpty()) {
                                    0L
                                } else {
                                    cleanedDigits.toLongOrNull() ?: 0L
                                }
                                
                                onInstallmentAmount(idx, cents)
                            },
                            leadingIcon = null,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1,
                            imeAction = ImeAction.Next
                        )
                        InstallmentDueDateFieldKmp(
                            valueIso = inst.dueDateIso,
                            onDatePickedIso = { picked -> onInstallmentDate(idx, picked) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            TextButton(
                onClick = onAddInstallment,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
            ) { Text("Añadir cuota") }

            Spacer(Modifier.height(12.dp))
            // Allocation summary
            val allocated = ui.charged.values.sum() + ui.installments.sumOf { it.amountCents }
            val change = remember(ui) { viewModel.calculateChange() }
            val canConfirm = totalToCharge > 0L && allocated >= totalToCharge

            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Asignado")
                    Text(formatNumberToMoney((allocated / 100.0).toString()))
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Restante")
                    Text(formatNumberToMoney((remaining / 100.0).toString()))
                }

                if (change > 0) {
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cambio", color = MaterialTheme.colorScheme.secondary)
                        Text(
                            formatNumberToMoney((change / 100.0).toString()),
                            style = bodyMediumBold(color = MaterialTheme.colorScheme.secondary)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Determine button text based on document type
            val confirmButtonText = when (selectedDocType) {
                "04" -> "Generar nota de crédito"
                "05" -> "Generar nota de débito"
                else -> "Confirmar cobro"
            }

            ButtonM(
                onClick = onConfirm,
                enabled = canConfirm,
            ) {
                Text(
                    confirmButtonText,
                    style = labelLarge().copy(color = MaterialTheme.colorScheme.onPrimary)
                )
            }
            // Only show save draft button for regular invoices, not credit/debit notes
            if (selectedDocType != "04" && selectedDocType != "05") {
                Spacer(Modifier.height(12.dp))

                TextButtonM(
                    label = "Guardar sin cobrar",
                    enabled = saveDraftEnabled,
                    onClick = {
                        onSaveDraft()
                    }
                )
            }
        }
    }
}

@Composable
private fun PaymentLinkSection(
    totalToCharge: Long,
    enabled: Boolean,
    onConfirm: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = vanishedBackgroundColor()
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Cobro con enlace", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Se generará un link por el monto total (incluye propina si la agregaste). " +
                        "La factura se emite cuando el pago se complete."
            )
            Spacer(Modifier.height(12.dp))
            ButtonM(
                onClick = onConfirm,
                enabled = enabled,
            ) {
                Text(
                    text = "Generar enlace por ${formatNumberToMoney((totalToCharge / 100.0).toString())}",
                    style = labelLarge().copy(color = MaterialTheme.colorScheme.onPrimary)
                )
            }
        }
    }
}

@Composable
private fun TipsEditor(
    isPercent: Boolean,
    percent: Int,
    fixedCents: Long,
    onMode: (Boolean) -> Unit,
    onPercent: (String) -> Unit,
    onFixedMajor: (String) -> Unit,
    onClose: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding()
            .imePadding()
    ) {
        Text("Propina", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        Row {
            FilterChip(selected = isPercent, onClick = { onMode(true) }, label = { Text("%") })
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = !isPercent,
                onClick = { onMode(false) },
                label = { Text("Monto fijo") })
        }

        Spacer(Modifier.height(12.dp))
        if (isPercent) {
            DMOutlinedTextField(
                text = percent.toString(),
                label = "Porcentaje (0-100)",
                onChange = onPercent,
                keyboardType = KeyboardType.Number,
                maxLines = 1,
                imeAction = ImeAction.Done,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            // Monto fijo en "¢" tal como vienes usando en tu app para simplificar
            DMMoneyOutlinedTextField(
                text = fixedCents.toString(),
                label = "Monto fijo (¢)",
                onChange = { raw -> onFixedMajor(raw) },
                leadingIcon = null,
                maxLines = 1,
                imeAction = ImeAction.Done,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onClose) { Text("Cerrar") }
        }
    }
}
