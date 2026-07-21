package com.teco.ventago.features.pos.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Loyalty
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavOptionsBuilder
import com.teco.ventago.AppViewModel
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.buttons.TextButtonM
import com.teco.ventago.design_system.buttons.dashedBorder
import com.teco.ventago.design_system.molecules.InstallmentDueDateFieldKmp
import com.teco.ventago.design_system.molecules.DMAlertDialog
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.textfields.DMMoneyOutlinedTextField
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.bodySmall
import com.teco.ventago.design_system.theme.headlineSmall
import com.teco.ventago.design_system.theme.labelLarge
import com.teco.ventago.features.pos.ui.viewmodel.InstallmentUI
import com.teco.ventago.features.pos.ui.viewmodel.PendingPaymentIntentMethod
import com.teco.ventago.features.pos.ui.viewmodel.PaymentFlowMode
import com.teco.ventago.features.pos.ui.viewmodel.PosViewModel
import com.teco.ventago.features.pos.ui.viewmodel.shouldShowPendingPaymentChangeOption
import com.teco.ventago.navigation.PosScreens
import com.teco.ventago.utils.formatNumberToMoney
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.creating_order
import ventago.composeapp.generated.resources.yappy_logo_portrait

private sealed class PaymentAmountEditTarget {
    data class Manual(val code: Int) : PaymentAmountEditTarget()
    data class Credit(val index: Int) : PaymentAmountEditTarget()
}

private data class CreditPaymentDraft(
    val amountCents: Long,
    val dueDateIso: String = "",
)

private data class OtherPaymentDraft(
    val amountCents: Long,
    val description: String = "",
)

private enum class PaymentConfigurationTarget {
    PaymentLinks,
    YappyOnsite,
}

data class PendingPaymentReplacementUiConfig(
    val sourceMethod: PendingPaymentIntentMethod,
    val onConfirmManual: () -> Unit,
    val onConfirmPaymentLink: () -> Unit,
    val onConfirmYappyOnsite: () -> Unit,
)

@Composable
fun PaymentScreen(
    appViewModel: AppViewModel,
    viewModel: PosViewModel,
    navigate: (PosScreens, (NavOptionsBuilder.() -> Unit)?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.creatingOrderState.isLoading.value) {
        appViewModel.setHideAppVar(true)
        CreatingOrderContent()
    } else {
        appViewModel.setHideAppVar(false)
        PaymentScreenContent(viewModel, navigate)
    }

    if (uiState.creatingOrderState.isSuccess.value) {
        val target = if (
            uiState.paymentFlowMode == PaymentFlowMode.YAPPY_ONSITE &&
            uiState.onsitePayment != null
        ) {
            PosScreens.YappyOnsitePaymentScreen
        } else {
            PosScreens.SuccessScreen
        }
        navigate(target) {
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
    replacementConfig: PendingPaymentReplacementUiConfig? = null,
    showLoadingSheet: Boolean = true,
//    onConfirmManualOrInstallments: () -> Unit, // send invoice immediately (manual) or with installments
//    onConfirmPaymentLink: () -> Unit           // create payment link; invoice on backend after paid
) {
    val ui by viewModel.uiState.collectAsState()
    LaunchedEffect(replacementConfig == null) {
        if (replacementConfig == null) {
            viewModel.onPaymentScreenVisible()
        }
    }

    val totalToCharge = remember(ui) { viewModel.amountToCharge() }
    val remaining = remember(ui) { viewModel.remainingToAllocate() }
    val hasPositiveAmount = totalToCharge > 0L
    val canPreviewInvoice = ui.cart.isNotEmpty() &&
        ui.finalCustomer != null &&
        (ui.finalCustomer == true || ui.customer != null)

    val loadingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showGovernmentWarning by remember { mutableStateOf(false) }
    var governmentInvalidProducts by remember { mutableStateOf<List<String>>(emptyList()) }
    var pendingGovernmentAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showPaymentMethodsConfigDialog by remember { mutableStateOf(false) }
    var paymentConfigurationTarget by remember { mutableStateOf(PaymentConfigurationTarget.PaymentLinks) }
    var selectorHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current

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

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val selectorHeight = with(density) { selectorHeightPx.toDp() }
        val contentBottomPadding = 16.dp
        val manualSectionMinHeight = (maxHeight - selectorHeight - 12.dp - contentBottomPadding)
            .coerceAtLeast(0.dp)

        Column(
            modifier = Modifier
                .padding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        )
        {
        val isCreditOrDebitNote = ui.selectedDocType in setOf("04", "05", "06")
        val isReplacementMode = replacementConfig != null
        val showDraftOption = shouldShowPendingPaymentChangeOption(
            mode = PaymentFlowMode.DRAFT,
            sourceMethod = replacementConfig?.sourceMethod,
            normallyVisible = !isCreditOrDebitNote && ui.canCreateDraft,
        )
        val showPaymentLinkOption = shouldShowPendingPaymentChangeOption(
            mode = PaymentFlowMode.PAYMENT_LINK,
            sourceMethod = replacementConfig?.sourceMethod,
            normallyVisible = ui.canCreatePaymentLink,
        )
        val paymentLinkReady = ui.canCreatePaymentLink && ui.paymentLinkConfigured
        val paymentLinkNeedsConfiguration = ui.canCreatePaymentLink && !ui.paymentLinkConfigured
        val selectedBillingPointHasYappyOnsite = viewModel.selectedBillingPointHasYappyOnsiteDevice()
        val yappyOnsiteReady = ui.canCreateYappyOnsiteQr &&
            ui.canCreateInvoice &&
            selectedBillingPointHasYappyOnsite
        val canUseOrConfigureYappyOnsite = ui.canCreateYappyOnsiteQr || ui.canConfigureYappyOnsite
        val yappyOnsiteAvailabilityLoading = canUseOrConfigureYappyOnsite && (
            !ui.paymentProfileResolved ||
                (
                    ui.yappyOnsiteConfigured &&
                        ui.canCreateYappyOnsiteQr &&
                        ui.canCreateInvoice &&
                        !ui.yappyOnsiteDevicesResolved
                    )
            )
        val yappyOnsiteNeedsConfiguration = ui.paymentProfileResolved &&
            !ui.yappyOnsiteConfigured &&
            ui.canConfigureYappyOnsite
        val showYappyOnsiteOption = shouldShowPendingPaymentChangeOption(
            mode = PaymentFlowMode.YAPPY_ONSITE,
            sourceMethod = replacementConfig?.sourceMethod,
            normallyVisible = when {
                yappyOnsiteAvailabilityLoading -> true
                ui.yappyOnsiteConfigured -> yappyOnsiteReady
                else -> yappyOnsiteNeedsConfiguration
            },
        )

        LaunchedEffect(
            ui.paymentFlowMode,
            isCreditOrDebitNote,
            showDraftOption,
            paymentLinkReady,
            yappyOnsiteReady,
            replacementConfig?.sourceMethod,
        ) {
            if (
                replacementConfig?.sourceMethod == PendingPaymentIntentMethod.PAYMENT_LINK &&
                ui.paymentFlowMode == PaymentFlowMode.PAYMENT_LINK
            ) {
                viewModel.setPaymentFlow(PaymentFlowMode.MANUAL_OR_INSTALLMENTS)
            }
            if (
                replacementConfig?.sourceMethod == PendingPaymentIntentMethod.YAPPY_ONSITE &&
                ui.paymentFlowMode == PaymentFlowMode.YAPPY_ONSITE
            ) {
                viewModel.setPaymentFlow(PaymentFlowMode.MANUAL_OR_INSTALLMENTS)
            }
            if (ui.paymentFlowMode == PaymentFlowMode.PAYMENT_LINK && (isCreditOrDebitNote || !paymentLinkReady)) {
                viewModel.setPaymentFlow(PaymentFlowMode.MANUAL_OR_INSTALLMENTS)
            }
            if (ui.paymentFlowMode == PaymentFlowMode.YAPPY_ONSITE && (isCreditOrDebitNote || !yappyOnsiteReady)) {
                viewModel.setPaymentFlow(PaymentFlowMode.MANUAL_OR_INSTALLMENTS)
            }
            if (ui.paymentFlowMode == PaymentFlowMode.DRAFT && !showDraftOption) {
                viewModel.setPaymentFlow(PaymentFlowMode.MANUAL_OR_INSTALLMENTS)
            }
        }

        fun selectManualPayment() {
            viewModel.setPaymentFlow(PaymentFlowMode.MANUAL_OR_INSTALLMENTS)
        }

        fun navigateToPaymentConfiguration(target: PaymentConfigurationTarget) {
            val canConfigureTarget = when (target) {
                PaymentConfigurationTarget.PaymentLinks -> ui.canConfigurePayments
                PaymentConfigurationTarget.YappyOnsite -> ui.canConfigureYappyOnsite
            }
            if (!canConfigureTarget) {
                paymentConfigurationTarget = target
                showPaymentMethodsConfigDialog = true
                return
            }
            viewModel.savePaymentLinkCheckpointForResume()
            val screen = when (target) {
                PaymentConfigurationTarget.PaymentLinks -> PosScreens.Payments
                PaymentConfigurationTarget.YappyOnsite -> {
                    if (ui.paymentsOnboardingCompleted) {
                        PosScreens.PaymentsYappyOnsiteScreen
                    } else {
                        PosScreens.Payments
                    }
                }
            }
            navigate(screen, null)
        }

        fun selectPaymentLink() {
            viewModel.markPaymentLinkBadgeSeen()
            viewModel.checkPaymentMethodsConfigured { configured ->
                if (!configured) {
                    navigateToPaymentConfiguration(PaymentConfigurationTarget.PaymentLinks)
                } else {
                    viewModel.setPaymentFlow(PaymentFlowMode.PAYMENT_LINK)
                }
            }
        }

        fun selectYappyOnsite() {
            if (yappyOnsiteNeedsConfiguration) {
                navigateToPaymentConfiguration(PaymentConfigurationTarget.YappyOnsite)
                return
            }
            if (!yappyOnsiteReady) {
                return
            }
            viewModel.setPaymentFlow(PaymentFlowMode.YAPPY_ONSITE)
        }

        fun selectDraft() {
            viewModel.setPaymentFlow(PaymentFlowMode.DRAFT)
        }

            Column(
                modifier = Modifier.onSizeChanged { selectorHeightPx = it.height }
            ) {
                if (!isCreditOrDebitNote) {
                    PaymentMethodSelector(
                        selectedMode = ui.paymentFlowMode,
                        showDraftOption = showDraftOption,
                        showPaymentLinkOption = showPaymentLinkOption,
                        linkEnabled = paymentLinkReady,
                        paymentLinkNeedsConfiguration = paymentLinkNeedsConfiguration,
                        paymentLinkConfigureEnabled = true,
                        showYappyOnsiteOption = showYappyOnsiteOption,
                        yappyOnsiteEnabled = yappyOnsiteReady,
                        yappyOnsiteAvailabilityLoading = yappyOnsiteAvailabilityLoading,
                        yappyOnsiteNeedsConfiguration = yappyOnsiteNeedsConfiguration,
                        yappyOnsiteConfigureEnabled = ui.canConfigureYappyOnsite,
                        draftEnabled = ui.canCreateDraft,
                        onManual = { selectManualPayment() },
                        onPaymentLink = { selectPaymentLink() },
                        onConfigurePaymentLinks = {
                            navigateToPaymentConfiguration(PaymentConfigurationTarget.PaymentLinks)
                        },
                        onYappyOnsite = { selectYappyOnsite() },
                        onConfigureYappyOnsite = {
                            navigateToPaymentConfiguration(PaymentConfigurationTarget.YappyOnsite)
                        },
                        onDraft = { selectDraft() },
                    )
                }
            }

        Spacer(Modifier.height(12.dp))

        if (ui.paymentFlowMode == PaymentFlowMode.MANUAL_OR_INSTALLMENTS || isCreditOrDebitNote) {
            ManualAndInstallmentsSection(
                viewModel = viewModel,
                totalToCharge = totalToCharge,
                remaining = remaining,
                onToggleMethod = viewModel::toggleManualMethod,
                onAmountChange = viewModel::setManualAmount,
                onOtherDesc = viewModel::setOtherDescription,
                onAddInstallment = { dueDateIso, amountCents ->
                    viewModel.addInstallment(dueDateIso = dueDateIso, amountCents = amountCents)
                },
                onRemoveInstallment = viewModel::removeInstallment,
                onInstallmentAmount = viewModel::setInstallmentAmount,
                onInstallmentDate = viewModel::setInstallmentDueDate,
                methodOptions = viewModel.manualMethodOptions(),
                selectedDocType = ui.selectedDocType,
                onConfirm = {
                    if (replacementConfig != null) {
                        replacementConfig.onConfirmManual()
                    } else {
                        requestGovernmentWarningOrProceed {
                            viewModel.createOrder(createPaymentLink = false, saveAsDraft = false)
                        }
                    }
                },
                onSaveDraft = {
                    requestGovernmentWarningOrProceed {
                        viewModel.createOrder(createPaymentLink = false, saveAsDraft = true)
                    }
                },
                onPreviewInvoice = { navigate(PosScreens.InvoicePreviewScreen, null) },
                // Disable save draft for credit/debit notes
                saveDraftEnabled = hasPositiveAmount && !isCreditOrDebitNote,
                canPreviewInvoice = canPreviewInvoice,
                canCreateInvoice = ui.canCreateInvoice,
                canCreateDraft = !isReplacementMode && ui.canCreateDraft,
                minHeight = manualSectionMinHeight,
                createInvoiceLabelOverride = if (isReplacementMode) "Registrar pago" else null,
            )
        } else if (ui.paymentFlowMode == PaymentFlowMode.PAYMENT_LINK) {
            PaymentLinkSection(
                totalToCharge = totalToCharge,
                enabled = hasPositiveAmount && ui.canCreateInvoice,
                canPreviewInvoice = canPreviewInvoice,
                onPreviewInvoice = { navigate(PosScreens.InvoicePreviewScreen, null) },
                onConfirm = {
                    if (replacementConfig != null) {
                        viewModel.checkPaymentMethodsConfigured { configured ->
                            if (!configured) {
                                navigateToPaymentConfiguration(PaymentConfigurationTarget.PaymentLinks)
                            } else {
                                replacementConfig.onConfirmPaymentLink()
                            }
                        }
                    } else {
                        requestGovernmentWarningOrProceed {
                            viewModel.checkPaymentMethodsConfigured { configured ->
                                if (!configured) {
                                    navigateToPaymentConfiguration(PaymentConfigurationTarget.PaymentLinks)
                                } else {
                                    viewModel.createOrder(createPaymentLink = true, saveAsDraft = false)
                                }
                            }
                        }
                    }
                }
            )
        } else if (ui.paymentFlowMode == PaymentFlowMode.YAPPY_ONSITE) {
            YappyOnsiteCreateSection(
                totalToCharge = totalToCharge,
                enabled = hasPositiveAmount && yappyOnsiteReady,
                canPreviewInvoice = canPreviewInvoice,
                onPreviewInvoice = { navigate(PosScreens.InvoicePreviewScreen, null) },
                onConfirm = {
                    if (replacementConfig != null) {
                        replacementConfig.onConfirmYappyOnsite()
                    } else {
                        requestGovernmentWarningOrProceed {
                            viewModel.createOrder(createYappyOnsite = true, saveAsDraft = false)
                        }
                    }
                }
            )
        } else {
            DraftPaymentSection(
                enabled = hasPositiveAmount && ui.canCreateDraft,
                canPreviewInvoice = canPreviewInvoice,
                onPreviewInvoice = { navigate(PosScreens.InvoicePreviewScreen, null) },
                onConfirm = {
                    requestGovernmentWarningOrProceed {
                        viewModel.createOrder(createPaymentLink = false, saveAsDraft = true)
                    }
                }
            )
        }

        Spacer(Modifier.height(16.dp))
        }
    }

    if (showLoadingSheet && ui.loadingBottomSheet.isLoading()) {
        LoadingSheet(
            state = ui.loadingBottomSheet,
            sheetState = loadingSheetState
        ) {
            viewModel.hideLoading()

            val target = if (
                ui.paymentFlowMode == PaymentFlowMode.YAPPY_ONSITE &&
                ui.onsitePayment != null
            ) {
                PosScreens.YappyOnsitePaymentScreen
            } else {
                PosScreens.SuccessScreen
            }
            navigate(target) {
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

    if (showPaymentMethodsConfigDialog) {
        val canConfigureTarget = when (paymentConfigurationTarget) {
            PaymentConfigurationTarget.PaymentLinks -> ui.canConfigurePayments
            PaymentConfigurationTarget.YappyOnsite -> ui.canConfigureYappyOnsite
        }
        val yappyOnsiteRequiresPaymentsOnboarding =
            paymentConfigurationTarget == PaymentConfigurationTarget.YappyOnsite &&
                !ui.paymentsOnboardingCompleted
        DMAlertDialog(
            title = when {
                canConfigureTarget && yappyOnsiteRequiresPaymentsOnboarding ->
                    "Configura tus pagos"
                canConfigureTarget && paymentConfigurationTarget == PaymentConfigurationTarget.YappyOnsite ->
                    "Configura Yappy en caja"
                canConfigureTarget -> "Configura tus links de pago"
                else -> "Canal no configurado"
            },
            message = when {
                canConfigureTarget && yappyOnsiteRequiresPaymentsOnboarding ->
                    "Primero debes aceptar los términos y crear tu cuenta de pagos. Luego podrás configurar Yappy en caja."
                canConfigureTarget && paymentConfigurationTarget == PaymentConfigurationTarget.YappyOnsite ->
                    "Para usar Yappy en caja primero debes configurar las sucursales y unidades de cobro."
                canConfigureTarget ->
                    "Para usar links de pago primero debes configurar al menos un canal compatible."
                else ->
                    "Este canal no está configurado o no tienes permisos para configurarlo. Solicita a un administrador revisarlo."
            },
            show = showPaymentMethodsConfigDialog,
            confirmText = if (canConfigureTarget) "Configurar" else "Entendido",
            dismissText = if (canConfigureTarget) "Más tarde" else "Cerrar",
            onDismiss = { showPaymentMethodsConfigDialog = false },
            onConfirm = {
                showPaymentMethodsConfigDialog = false
                if (canConfigureTarget) {
                    viewModel.savePaymentLinkCheckpointForResume()
                    val screen = when (paymentConfigurationTarget) {
                        PaymentConfigurationTarget.PaymentLinks -> PosScreens.Payments
                        PaymentConfigurationTarget.YappyOnsite -> {
                            if (ui.paymentsOnboardingCompleted) {
                                PosScreens.PaymentsYappyOnsiteScreen
                            } else {
                                PosScreens.Payments
                            }
                        }
                    }
                    navigate(screen, null)
                }
            }
        )
    }

    if (ui.showYappyOnsitePendingConflictDialog) {
        DMAlertDialog(
            title = "Hay un QR de Yappy pendiente",
            message = "Esta sucursal y punto de facturación ya tienen un cobro de Yappy en caja activo.\n\nPara generar un nuevo QR, primero debemos cancelar el QR pendiente anterior.",
            show = true,
            confirmText = "Cancelar QR pendiente",
            dismissText = "Mantener QR activo",
            onDismiss = viewModel::keepYappyOnsitePendingConflictActive,
            onConfirm = viewModel::cancelPendingYappyOnsiteAndRetry,
        )
    }

}

@Composable
private fun PaymentMethodSelector(
    selectedMode: PaymentFlowMode,
    showDraftOption: Boolean,
    showPaymentLinkOption: Boolean,
    linkEnabled: Boolean,
    paymentLinkNeedsConfiguration: Boolean,
    paymentLinkConfigureEnabled: Boolean,
    showYappyOnsiteOption: Boolean,
    yappyOnsiteEnabled: Boolean,
    yappyOnsiteAvailabilityLoading: Boolean,
    yappyOnsiteNeedsConfiguration: Boolean,
    yappyOnsiteConfigureEnabled: Boolean,
    draftEnabled: Boolean,
    onManual: () -> Unit,
    onPaymentLink: () -> Unit,
    onConfigurePaymentLinks: () -> Unit,
    onYappyOnsite: () -> Unit,
    onConfigureYappyOnsite: () -> Unit,
    onDraft: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        PaymentMethodOptionCard(
            title = "Pago manual",
            subtitle = "Efectivo, transferencia, cheque o mixto",
            icon = Icons.Rounded.Payments,
            selected = selectedMode == PaymentFlowMode.MANUAL_OR_INSTALLMENTS,
            enabled = true,
            onClick = onManual,
        )
        if (showPaymentLinkOption) {
            Spacer(Modifier.height(10.dp))
            PaymentMethodOptionCard(
                title = "Crear enlace de pago",
                subtitle = if (linkEnabled) "Yappy, tarjeta, ACH o PayPal" else "Configura un canal antes de cobrar con links",
                icon = Icons.Rounded.Link,
                selected = selectedMode == PaymentFlowMode.PAYMENT_LINK,
                enabled = linkEnabled,
                actionLabel = if (paymentLinkNeedsConfiguration) "Configurar links de pago" else null,
                actionEnabled = paymentLinkConfigureEnabled,
                onActionClick = onConfigurePaymentLinks,
                onClick = onPaymentLink,
            )
        }
        if (showYappyOnsiteOption) {
            Spacer(Modifier.height(10.dp))
            PaymentMethodOptionCard(
                title = "Yappy en caja",
                subtitle = when {
                    yappyOnsiteAvailabilityLoading -> "Verificando disponibilidad para este punto"
                    yappyOnsiteEnabled -> "Genera QR y detecta el pago automáticamente"
                    else -> "Configura Yappy en caja antes de cobrar"
                },
                iconPainter = painterResource(Res.drawable.yappy_logo_portrait),
                selected = selectedMode == PaymentFlowMode.YAPPY_ONSITE,
                enabled = yappyOnsiteEnabled && !yappyOnsiteAvailabilityLoading,
                actionLabel = if (yappyOnsiteNeedsConfiguration) "Configurar Yappy en caja" else null,
                actionEnabled = yappyOnsiteConfigureEnabled,
                onActionClick = onConfigureYappyOnsite,
                onClick = onYappyOnsite,
            )
        }
        if (showDraftOption) {
            Spacer(Modifier.height(10.dp))
            PaymentMethodOptionCard(
                title = "Guardar sin facturar",
                subtitle = "Crea un borrador para facturar más tarde",
                icon = Icons.Rounded.Description,
                selected = selectedMode == PaymentFlowMode.DRAFT,
                enabled = draftEnabled,
                onClick = onDraft,
            )
        }
    }
}

@Composable
private fun PaymentMethodOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    iconPainter: Painter? = null,
    selected: Boolean,
    enabled: Boolean,
    actionLabel: String? = null,
    actionEnabled: Boolean = true,
    onActionClick: () -> Unit = {},
    onClick: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.secondary
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) accent.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 1.2.dp else 1.dp,
            color = if (selected) accent.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation = if (selected) 0.dp else 1.dp
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(15.dp),
                    color = if (selected) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Row(
                        Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (iconPainter != null) {
                            Icon(
                                painter = iconPainter,
                                contentDescription = title,
                                tint = Color.Unspecified,
                                modifier = Modifier
                                    .width(30.dp)
                                    .height(26.dp)
                            )
                        } else if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = bodyMediumBold(
                            color = if (enabled || actionLabel != null) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    )
                    Text(
                        subtitle,
                        style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(10.dp))
                Icon(
                    imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = when {
                        selected -> accent
                        enabled -> MaterialTheme.colorScheme.outline
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                    },
                    modifier = Modifier.size(24.dp)
                )
            }
            if (actionLabel != null) {
                Spacer(Modifier.height(12.dp))
                OutlinedButtonM(
                    onClick = onActionClick,
                    enabled = actionEnabled,
                    contentColor = MaterialTheme.colorScheme.secondary,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f))
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualAndInstallmentsSection(
    viewModel: PosViewModel,
    totalToCharge: Long,
    remaining: Long,
    methodOptions: List<Pair<Int, String>>,
    selectedDocType: String,
    onToggleMethod: (Int, Boolean) -> Unit,
    onAmountChange: (Int, Long) -> Unit,
    onOtherDesc: (String) -> Unit,
    onAddInstallment: (String, Long) -> Unit,
    onRemoveInstallment: (Int) -> Unit,
    onInstallmentAmount: (Int, Long) -> Unit,
    onInstallmentDate: (Int, String) -> Unit,
    onConfirm: () -> Unit,
    onSaveDraft: () -> Unit,
    onPreviewInvoice: () -> Unit,
    saveDraftEnabled: Boolean,
    canPreviewInvoice: Boolean,
    canCreateInvoice: Boolean,
    canCreateDraft: Boolean,
    minHeight: Dp = 0.dp,
    createInvoiceLabelOverride: String? = null,
) {
    val ui by viewModel.uiState.collectAsState()
    var showMethodSheet by remember { mutableStateOf(false) }
    var amountEditTarget by remember { mutableStateOf<PaymentAmountEditTarget?>(null) }
    var creditPaymentDraft by remember { mutableStateOf<CreditPaymentDraft?>(null) }
    var otherPaymentDraft by remember { mutableStateOf<OtherPaymentDraft?>(null) }
    val allocated = ui.charged.values.sum() + ui.installments.sumOf { it.amountCents }
    val change = remember(ui) { viewModel.calculateChange() }
    val creditDatesComplete = ui.installments.all { it.dueDateIso.isNotBlank() }
    val otherDescriptionComplete =
        !ui.charged.containsKey(99) || ui.otherPaymentDescription.trim().length >= 15
    val canConfirm = totalToCharge > 0L &&
        allocated >= totalToCharge &&
        creditDatesComplete &&
        otherDescriptionComplete

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        val contentMinHeight = (minHeight - 16.dp).coerceAtLeast(0.dp)
        if (canCreateInvoice) {
            val confirmButtonText = when (selectedDocType) {
                "04", "06" -> "Generar nota de crédito"
                "05" -> "Generar nota de débito"
                else -> "Crear factura"
            }.let { createInvoiceLabelOverride ?: it }

            PaymentAllocationActionsLayout(
                minHeight = contentMinHeight,
                allocationCard = {
                    PaymentAllocationCard(
                        methodOptions = methodOptions,
                        charged = ui.charged,
                        installments = ui.installments,
                        allocated = allocated,
                        remaining = remaining,
                        change = change,
                        otherPaymentDescription = ui.otherPaymentDescription,
                        creditDatesComplete = creditDatesComplete,
                        otherDescriptionComplete = otherDescriptionComplete,
                        onOpenMethodSheet = { showMethodSheet = true },
                        onRemoveMethod = { code -> onToggleMethod(code, false) },
                        onRemoveInstallment = onRemoveInstallment,
                        onEditMethodAmount = { code -> amountEditTarget = PaymentAmountEditTarget.Manual(code) },
                        onEditInstallmentAmount = { index -> amountEditTarget = PaymentAmountEditTarget.Credit(index) }
                    )
                },
                actions = {
                    PaymentInvoiceActions(
                        showCreateInvoice = true,
                        createInvoiceLabel = confirmButtonText,
                        createInvoiceEnabled = canConfirm,
                        canPreviewInvoice = canPreviewInvoice,
                        showSaveDraft = canCreateDraft && selectedDocType !in setOf("04", "05", "06"),
                        saveDraftEnabled = saveDraftEnabled,
                        onCreateInvoice = onConfirm,
                        onPreviewInvoice = onPreviewInvoice,
                        onSaveDraft = onSaveDraft,
                    )
                }
            )
        } else if (canCreateDraft && selectedDocType !in setOf("04", "05", "06")) {
            PaymentInvoiceActions(
                showCreateInvoice = false,
                createInvoiceLabel = "",
                createInvoiceEnabled = false,
                canPreviewInvoice = false,
                showSaveDraft = true,
                saveDraftEnabled = saveDraftEnabled,
                onCreateInvoice = {},
                onPreviewInvoice = {},
                onSaveDraft = onSaveDraft,
            )
        }
    }

    if (showMethodSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
            onDismissRequest = { showMethodSheet = false }
        ) {
            PaymentMethodSelectionSheet(
                methodOptions = methodOptions,
                charged = ui.charged,
                remaining = remaining,
                onAddMethod = { code ->
                    val remainingForPrefill =
                        (totalToCharge - ui.charged.values.sum() - ui.installments.sumOf { it.amountCents })
                            .coerceAtLeast(0L)
                    onToggleMethod(code, true)
                    onAmountChange(code, remainingForPrefill)
                    showMethodSheet = false
                },
                onSelectOther = {
                    val remainingForPrefill =
                        (totalToCharge - ui.charged.values.sum() - ui.installments.sumOf { it.amountCents })
                            .coerceAtLeast(0L)
                    showMethodSheet = false
                    otherPaymentDraft = OtherPaymentDraft(amountCents = remainingForPrefill)
                },
                onSelectCredit = {
                    val remainingForPrefill =
                        (totalToCharge - ui.charged.values.sum() - ui.installments.sumOf { it.amountCents })
                            .coerceAtLeast(0L)
                    showMethodSheet = false
                    creditPaymentDraft = CreditPaymentDraft(amountCents = remainingForPrefill)
                },
                onClose = { showMethodSheet = false }
            )
        }
    }

    otherPaymentDraft?.let { draft ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
            onDismissRequest = { otherPaymentDraft = null }
        ) {
            OtherPaymentCreateSheet(
                amountCents = draft.amountCents,
                description = draft.description,
                onAmountChange = { cents ->
                    otherPaymentDraft = otherPaymentDraft?.copy(amountCents = cents)
                },
                onDescriptionChange = { description ->
                    otherPaymentDraft = otherPaymentDraft?.copy(description = description)
                },
                onConfirm = {
                    otherPaymentDraft?.let { currentDraft ->
                        onOtherDesc(currentDraft.description.trim())
                        onToggleMethod(99, true)
                        onAmountChange(99, currentDraft.amountCents)
                    }
                    otherPaymentDraft = null
                }
            )
        }
    }

    creditPaymentDraft?.let { draft ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
            onDismissRequest = { creditPaymentDraft = null }
        ) {
            CreditPaymentCreateSheet(
                amountCents = draft.amountCents,
                dueDateIso = draft.dueDateIso,
                onAmountChange = { cents ->
                    creditPaymentDraft = creditPaymentDraft?.copy(amountCents = cents)
                },
                onDueDateChange = { dueDateIso ->
                    creditPaymentDraft = creditPaymentDraft?.copy(dueDateIso = dueDateIso)
                },
                onConfirm = {
                    creditPaymentDraft?.let { currentDraft ->
                        onAddInstallment(currentDraft.dueDateIso, currentDraft.amountCents)
                    }
                    creditPaymentDraft = null
                }
            )
        }
    }

    amountEditTarget?.let { target ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val title = when (target) {
            is PaymentAmountEditTarget.Manual ->
                methodOptions.firstOrNull { it.first == target.code }?.second ?: "Método ${target.code}"
            is PaymentAmountEditTarget.Credit -> "Crédito ${target.index + 1}"
        }
        val amountCents = when (target) {
            is PaymentAmountEditTarget.Manual -> ui.charged[target.code] ?: 0L
            is PaymentAmountEditTarget.Credit -> ui.installments.getOrNull(target.index)?.amountCents ?: 0L
        }
        ModalBottomSheet(
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
            onDismissRequest = { amountEditTarget = null }
        ) {
            PaymentAmountEditSheet(
                title = title,
                amountCents = amountCents,
                isCash = target is PaymentAmountEditTarget.Manual && target.code == 2,
                otherPaymentDescription = if (target is PaymentAmountEditTarget.Manual && target.code == 99) {
                    ui.otherPaymentDescription
                } else {
                    null
                },
                dueDateIso = if (target is PaymentAmountEditTarget.Credit) {
                    ui.installments.getOrNull(target.index)?.dueDateIso
                } else {
                    null
                },
                onAmountChange = { cents ->
                    when (target) {
                        is PaymentAmountEditTarget.Manual -> onAmountChange(target.code, cents)
                        is PaymentAmountEditTarget.Credit -> onInstallmentAmount(target.index, cents)
                    }
                },
                onOtherDesc = onOtherDesc,
                onDueDateChange = { iso ->
                    if (target is PaymentAmountEditTarget.Credit) {
                        onInstallmentDate(target.index, iso)
                    }
                },
                onClose = { amountEditTarget = null }
            )
        }
    }
}

@Composable
private fun PaymentAllocationActionsLayout(
    minHeight: Dp,
    allocationCard: @Composable () -> Unit,
    actions: @Composable () -> Unit,
) {
    Layout(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        content = {
            allocationCard()
            actions()
        }
    ) { measurables, constraints ->
        val cardPlaceable = measurables[0].measure(
            constraints.copy(minHeight = 0)
        )
        val actionsPlaceable = measurables[1].measure(
            constraints.copy(minHeight = 0)
        )
        val minimumGap = 12.dp.roundToPx()
        val contentHeight = cardPlaceable.height + minimumGap + actionsPlaceable.height
        val layoutHeight = maxOf(constraints.minHeight, contentHeight)
            .coerceIn(constraints.minHeight, constraints.maxHeight)
        val dynamicGap = maxOf(
            minimumGap,
            layoutHeight - cardPlaceable.height - actionsPlaceable.height
        )

        layout(constraints.maxWidth, layoutHeight) {
            cardPlaceable.placeRelative(0, 0)
            actionsPlaceable.placeRelative(0, cardPlaceable.height + dynamicGap)
        }
    }
}

@Composable
private fun PaymentAllocationCard(
    methodOptions: List<Pair<Int, String>>,
    charged: Map<Int, Long>,
    installments: List<InstallmentUI>,
    allocated: Long,
    remaining: Long,
    change: Long,
    otherPaymentDescription: String,
    creditDatesComplete: Boolean,
    otherDescriptionComplete: Boolean,
    onOpenMethodSheet: () -> Unit,
    onRemoveMethod: (Int) -> Unit,
    onRemoveInstallment: (Int) -> Unit,
    onEditMethodAmount: (Int) -> Unit,
    onEditInstallmentAmount: (Int) -> Unit,
) {
    val hasCharges = charged.isNotEmpty() || installments.isNotEmpty()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Distribución del cobro",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.W800)
            )
            Text(
                "Puedes dividir el total entre métodos",
                style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Spacer(Modifier.height(14.dp))

            if (!hasCharges) {
                SelectPaymentMethodButton(onClick = onOpenMethodSheet)
            } else {
                Divider()
                val manualCodes = charged.keys.sorted()
                manualCodes.forEachIndexed { index, code ->
                    PaymentChargeSummaryRow(
                        title = methodOptions.firstOrNull { it.first == code }?.second ?: "Método $code",
                        subtitle = if (code == 99) {
                            otherPaymentDescription.takeIf { it.isNotBlank() }
                        } else {
                            null
                        },
                        amountCents = charged[code] ?: 0L,
                        icon = paymentMethodIcon(code),
                        onAmountClick = { onEditMethodAmount(code) },
                        onRemove = { onRemoveMethod(code) }
                    )
                    if (index != manualCodes.lastIndex || installments.isNotEmpty()) {
                        Divider()
                    }
                }
                installments.forEachIndexed { index, installment ->
                    PaymentChargeSummaryRow(
                        title = "Crédito ${index + 1}",
                        subtitle = installment.dueDateIso.takeIf { it.isNotBlank() }
                            ?.let { "Vence $it" } ?: "Fecha de vencimiento pendiente",
                        amountCents = installment.amountCents,
                        icon = Icons.Rounded.CalendarMonth,
                        subtitleColor = if (installment.dueDateIso.isBlank()) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        onAmountClick = { onEditInstallmentAmount(index) },
                        onRemove = { onRemoveInstallment(index) }
                    )
                    if (index != installments.lastIndex) Divider()
                }
                if (remaining > 0) {
                    Spacer(Modifier.height(10.dp))
                    SelectPaymentMethodButton(onClick = onOpenMethodSheet)
                }
            }

            if (remaining > 0 || change > 0) {
                Divider(Modifier.padding(top = 10.dp, bottom = 8.dp))
                AllocationTotals(
                    allocated = allocated,
                    remaining = remaining,
                    change = change,
                )
            }
            if (!creditDatesComplete) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Selecciona fecha de vencimiento para cada pago a crédito.",
                    style = bodySmall(color = MaterialTheme.colorScheme.error)
                )
            }
            if (!otherDescriptionComplete) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Completa una descripción de al menos 15 caracteres para Otro.",
                    style = bodySmall(color = MaterialTheme.colorScheme.error)
                )
            }
        }
    }
}

private fun paymentMethodIcon(code: Int): ImageVector {
    return when (code) {
        8 -> Icons.Rounded.AccountBalance
        3, 4 -> Icons.Rounded.CreditCard
        2, 10 -> Icons.Rounded.AttachMoney
        5, 6 -> Icons.Rounded.Loyalty
        7 -> Icons.Rounded.Redeem
        9 -> Icons.AutoMirrored.Rounded.FactCheck
        99 -> Icons.Rounded.Add
        else -> Icons.Rounded.Add
    }
}

@Composable
private fun PaymentMethodBadge(
    icon: ImageVector,
    contentDescription: String?,
) {
    Surface(
        modifier = Modifier.size(34.dp),
        shape = RoundedCornerShape(13.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SelectPaymentMethodButton(onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.secondary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
                shape = RoundedCornerShape(16.dp)
            )
            .dashedBorder(
                strokeWidth = 1.4.dp,
                color = accent.copy(alpha = 0.75f),
                cornerRadiusDp = 16.dp
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "Seleccionar método de pago",
            style = bodyMediumBold(color = accent)
        )
    }
}

@Composable
private fun PaymentChargeSummaryRow(
    title: String,
    subtitle: String?,
    amountCents: Long,
    icon: ImageVector,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onAmountClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            PaymentMethodBadge(icon, title)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, style = bodyMediumBold())
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        subtitle,
                        style = bodySmall(color = subtitleColor),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.clickable(onClick = onAmountClick),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Text(
                    formatMoney(amountCents),
                    modifier = Modifier
                        .widthIn(min = 96.dp)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    style = bodyMediumBold(),
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Eliminar método",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AllocationTotals(
    allocated: Long,
    remaining: Long,
    change: Long,
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Asignado", style = bodyMedium())
            Text(formatMoney(allocated), style = bodyMediumBold())
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Pendiente por asignar", style = bodyMedium())
            Text(
                formatMoney(remaining),
                style = bodyMediumBold(
                    color = if (remaining > 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            )
        }
        if (change > 0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Cambio", style = bodyMedium(color = MaterialTheme.colorScheme.secondary))
                Text(formatMoney(change), style = bodyMediumBold(color = MaterialTheme.colorScheme.secondary))
            }
        }
    }
}

@Composable
private fun PaymentMethodSelectionSheet(
    methodOptions: List<Pair<Int, String>>,
    charged: Map<Int, Long>,
    remaining: Long,
    onAddMethod: (Int) -> Unit,
    onSelectOther: () -> Unit,
    onSelectCredit: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text("Seleccionar método de pago", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(14.dp))

        methodOptions.forEach { (code, label) ->
            val isOtherMethod = code == 99
            PaymentMethodPickerRow(
                icon = paymentMethodIcon(code),
                title = label,
                subtitle = null,
                enabled = !charged.containsKey(code) && remaining > 0,
                onClick = {
                    if (isOtherMethod) {
                        onSelectOther()
                    } else {
                        onAddMethod(code)
                    }
                }
            )
            if (code == 2) {
                PaymentMethodPickerRow(
                    icon = Icons.Rounded.CalendarMonth,
                    title = "Crédito",
                    subtitle = "Debe especificar fecha de vencimiento",
                    enabled = remaining > 0,
                    onClick = onSelectCredit
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        TextButtonM(
            label = "Cerrar",
            enabled = true,
            onClick = onClose
        )
    }
}

@Composable
private fun PaymentMethodPickerRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (enabled) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f)
        },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PaymentMethodBadge(icon, title)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = bodyMediumBold(
                            color = if (enabled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            subtitle,
                            style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                if (enabled) "Agregar" else "Agregado",
                style = bodySmall(
                    color = if (enabled) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            )
        }
    }
}

@Composable
private fun OtherPaymentCreateSheet(
    amountCents: Long,
    description: String,
    onAmountChange: (Long) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    val trimmedDescription = description.trim()
    val descriptionIsValid = trimmedDescription.length >= 15
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text("Otro método", style = MaterialTheme.typography.titleLarge)
        Text(
            "Indica el método usado por el cliente y el monto asignado.",
            style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Spacer(Modifier.height(12.dp))
        DMMoneyOutlinedTextField(
            text = if (amountCents == 0L) "" else amountCents.toString(),
            label = "Monto",
            onChange = { raw ->
                val cleanedDigits = raw.filter(Char::isDigit).trimStart('0')
                val cents = if (cleanedDigits.isEmpty()) {
                    0L
                } else {
                    cleanedDigits.toLongOrNull() ?: 0L
                }
                onAmountChange(cents)
            },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = null,
            maxLines = 1,
            imeAction = ImeAction.Next
        )
        DMOutlinedTextField(
            text = description,
            label = "Método usado",
            onChange = onDescriptionChange,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            maxLines = 2,
            imeAction = ImeAction.Done,
            supportingText = "Mínimo 15 caracteres",
            isError = description.isNotBlank() && !descriptionIsValid
        )
        Spacer(Modifier.height(14.dp))
        ButtonM(
            onClick = onConfirm,
            enabled = amountCents > 0L && descriptionIsValid
        ) {
            Text("Listo", style = labelLarge().copy(color = MaterialTheme.colorScheme.onPrimary))
        }
    }
}

@Composable
private fun CreditPaymentCreateSheet(
    amountCents: Long,
    dueDateIso: String,
    onAmountChange: (Long) -> Unit,
    onDueDateChange: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text("Crédito", style = MaterialTheme.typography.titleLarge)
        Text(
            "Define el monto y la fecha de vencimiento.",
            style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Spacer(Modifier.height(12.dp))
        DMMoneyOutlinedTextField(
            text = if (amountCents == 0L) "" else amountCents.toString(),
            label = "Monto",
            onChange = { raw ->
                val cleanedDigits = raw.filter(Char::isDigit).trimStart('0')
                val cents = if (cleanedDigits.isEmpty()) {
                    0L
                } else {
                    cleanedDigits.toLongOrNull() ?: 0L
                }
                onAmountChange(cents)
            },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = null,
            maxLines = 1,
            imeAction = ImeAction.Next
        )
        InstallmentDueDateFieldKmp(
            valueIso = dueDateIso,
            onDatePickedIso = onDueDateChange,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            label = "Fecha de vencimiento"
        )
        Spacer(Modifier.height(14.dp))
        ButtonM(
            onClick = onConfirm,
            enabled = amountCents > 0L && dueDateIso.isNotBlank()
        ) {
            Text("Listo", style = labelLarge().copy(color = MaterialTheme.colorScheme.onPrimary))
        }
    }
}

@Composable
private fun PaymentAmountEditSheet(
    title: String,
    amountCents: Long,
    isCash: Boolean,
    otherPaymentDescription: String?,
    dueDateIso: String?,
    onAmountChange: (Long) -> Unit,
    onOtherDesc: (String) -> Unit,
    onDueDateChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    val otherDescriptionIsValid = otherPaymentDescription == null ||
        otherPaymentDescription.trim().length >= 15
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(
            "Modifica el monto asignado a este método.",
            style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Spacer(Modifier.height(12.dp))
        DMMoneyOutlinedTextField(
            text = if (amountCents == 0L) "" else amountCents.toString(),
            label = "Monto",
            onChange = { raw ->
                val cleanedDigits = raw.filter(Char::isDigit).trimStart('0')
                val cents = if (cleanedDigits.isEmpty()) {
                    0L
                } else {
                    cleanedDigits.toLongOrNull() ?: 0L
                }
                onAmountChange(cents)
            },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = null,
            maxLines = 1,
            imeAction = if (otherPaymentDescription != null || dueDateIso != null) {
                ImeAction.Next
            } else {
                ImeAction.Done
            }
        )
        if (otherPaymentDescription != null) {
            DMOutlinedTextField(
                text = otherPaymentDescription,
                label = "Descripción de otro método",
                onChange = onOtherDesc,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                maxLines = 2,
                supportingText = "Mínimo 15 caracteres",
                isError = otherPaymentDescription.isNotBlank() && !otherDescriptionIsValid
            )
        }
        if (dueDateIso != null) {
            InstallmentDueDateFieldKmp(
                valueIso = dueDateIso,
                onDatePickedIso = onDueDateChange,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                label = "Fecha de vencimiento"
            )
        }
        if (isCash) {
            Spacer(Modifier.height(6.dp))
            Text(
                "El efectivo puede superar el total para calcular cambio.",
                style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
        Spacer(Modifier.height(14.dp))
        ButtonM(
            onClick = onClose,
            enabled = otherDescriptionIsValid
        ) {
            Text("Listo", style = labelLarge().copy(color = MaterialTheme.colorScheme.onPrimary))
        }
    }
}

private fun formatMoney(cents: Long): String {
    return formatNumberToMoney((cents / 100.0).toString())
}

@Composable
private fun PaymentInvoiceActions(
    showCreateInvoice: Boolean,
    createInvoiceLabel: String,
    createInvoiceEnabled: Boolean,
    canPreviewInvoice: Boolean,
    showSaveDraft: Boolean,
    saveDraftEnabled: Boolean,
    onCreateInvoice: () -> Unit,
    onPreviewInvoice: () -> Unit,
    onSaveDraft: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showCreateInvoice) {
            PaymentFilledActionButton(
                label = createInvoiceLabel,
                icon = Icons.Rounded.Description,
                enabled = createInvoiceEnabled,
                onClick = onCreateInvoice,
            )

            Spacer(Modifier.height(12.dp))

            PaymentOutlinedActionButton(
                label = "Vista previa",
                icon = Icons.Rounded.Visibility,
                enabled = canPreviewInvoice,
                onClick = onPreviewInvoice,
            )
        }

        if (showSaveDraft) {
            if (showCreateInvoice) {
                PaymentActionSeparator()
            }

            PaymentSaveDraftAction(
                enabled = saveDraftEnabled,
                onClick = onSaveDraft,
            )
        }
    }
}

@Composable
private fun PaymentFilledActionButton(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    ButtonM(
        onClick = onClick,
        enabled = enabled,
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.W800)
        )
    }
}

@Composable
private fun PaymentOutlinedActionButton(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButtonM(
        onClick = onClick,
        enabled = enabled,
        contentColor = MaterialTheme.colorScheme.secondary,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.W800)
        )
    }
}

@Composable
private fun PaymentActionSeparator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Divider(Modifier.weight(1f))
        Text(
            text = "o",
            modifier = Modifier.padding(horizontal = 32.dp),
            style = bodyMediumBold(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Divider(Modifier.weight(1f))
    }
}

@Composable
private fun PaymentSaveDraftAction(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val titleColor = if (enabled) {
            MaterialTheme.colorScheme.secondary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        }
        Text(
            text = "Guardar sin facturar",
            style = bodyMediumBold(),
            color = titleColor,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Puedes guardar esta factura como borrador y cobrar más tarde.",
            style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PaymentLinkSection(
    totalToCharge: Long,
    enabled: Boolean,
    canPreviewInvoice: Boolean,
    onPreviewInvoice: () -> Unit,
    onConfirm: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Cobro con enlace",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.W800)
            )
            Text(
                "Comparte un enlace para que el cliente pague en línea.",
                style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Spacer(Modifier.height(16.dp))
            Divider()
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total a cobrar",
                    style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Text(
                    text = formatMoney(totalToCharge),
                    style = bodyMediumBold(color = MaterialTheme.colorScheme.onSurface)
                )
            }
            Spacer(Modifier.height(16.dp))
            PaymentFilledActionButton(
                label = "Generar enlace",
                icon = Icons.Rounded.Link,
                enabled = enabled,
                onClick = onConfirm,
            )
            Spacer(Modifier.height(12.dp))
            PaymentOutlinedActionButton(
                label = "Vista previa",
                icon = Icons.Rounded.Visibility,
                enabled = canPreviewInvoice,
                onClick = onPreviewInvoice,
            )
        }
    }
}

@Composable
private fun YappyOnsiteCreateSection(
    totalToCharge: Long,
    enabled: Boolean,
    canPreviewInvoice: Boolean,
    onPreviewInvoice: () -> Unit,
    onConfirm: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Cobro con Yappy en caja",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.W800)
            )
            Text(
                "Genera un QR para que el cliente pague frente al cajero. VentaGo detectará el pago y emitirá la factura automáticamente.",
                style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Spacer(Modifier.height(16.dp))
            Divider()
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total a cobrar",
                    style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Text(
                    text = formatMoney(totalToCharge),
                    style = bodyMediumBold(color = MaterialTheme.colorScheme.onSurface)
                )
            }
            Spacer(Modifier.height(16.dp))
            PaymentFilledActionButton(
                label = "Generar QR",
                icon = Icons.Rounded.CreditCard,
                enabled = enabled,
                onClick = onConfirm,
            )
            Spacer(Modifier.height(12.dp))
            PaymentOutlinedActionButton(
                label = "Vista previa",
                icon = Icons.Rounded.Visibility,
                enabled = canPreviewInvoice,
                onClick = onPreviewInvoice,
            )
        }
    }
}

@Composable
private fun DraftPaymentSection(
    enabled: Boolean,
    canPreviewInvoice: Boolean,
    onPreviewInvoice: () -> Unit,
    onConfirm: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Guardar sin facturar",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.W800)
            )
            Text(
                "Crea un borrador para revisar, cobrar o facturar esta venta más tarde.",
                style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Spacer(Modifier.height(16.dp))
            PaymentFilledActionButton(
                label = "Guardar sin facturar",
                icon = Icons.Rounded.Description,
                enabled = enabled,
                onClick = onConfirm,
            )
            Spacer(Modifier.height(12.dp))
            PaymentOutlinedActionButton(
                label = "Vista previa",
                icon = Icons.Rounded.Visibility,
                enabled = canPreviewInvoice,
                onClick = onPreviewInvoice,
            )
        }
    }
}
