package com.teco.ventago.features.payments.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalFocusManager
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.teco.ventago.core.SnackbarService
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.loaders.shimmerBrush
import com.teco.ventago.design_system.molecules.DMAlertDialog
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.textfields.helpers.DMDropDownField
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.bodySmall
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.headlineMediumBold
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.design_system.theme.vanishedBackgroundColor
import com.teco.ventago.features.branches.domain.model.Branch
import com.teco.ventago.features.payments.domain.models.YappyOnsiteDevice
import com.teco.ventago.features.payments.domain.models.YappyOnsiteGroup
import com.teco.ventago.features.payments.ui.home.viewmodel.FeeBatchStatusUi
import com.teco.ventago.features.payments.ui.home.viewmodel.FeeTransactionStatusUi
import com.teco.ventago.features.payments.ui.home.viewmodel.PaymentMethodType
import com.teco.ventago.features.payments.ui.home.viewmodel.PaymentMethodsViewModel
import com.teco.ventago.features.payments.ui.home.viewmodel.PaymentScreenMode
import com.teco.ventago.features.payments.ui.home.viewmodel.PaymentUiEvent
import com.teco.ventago.features.payments.ui.home.viewmodel.PaymentUiState
import com.teco.ventago.features.payments.ui.home.viewmodel.PaymentViewMode
import com.teco.ventago.features.pos.ui.dismissKeyboardOnOutsideTap
import com.teco.ventago.utils.DateFormat
import com.teco.ventago.utils.formatNumberToMoney
import com.teco.ventago.utils.getImageRequest
import com.teco.ventago.utils.openCustomTab
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.accept
import ventago.composeapp.generated.resources.cancel
import ventago.composeapp.generated.resources.ic_bank
import ventago.composeapp.generated.resources.ic_paypal_logo
import ventago.composeapp.generated.resources.paypal
import ventago.composeapp.generated.resources.yappy_logo

private val PaymentsMaxWidth = 1140.dp
private const val YappyPromoImageUrl = "https://paas.b-cdn.net/assets/yappy-promo1.png"
private const val YappyBusinessGuideUrl = "https://www.bgeneral.com/wp-content/uploads/2021/03/Guia%20de%20Activacion%20de%20Yappy%20Comercial%20.pdf"
private const val YappyEntrepreneurGuideUrl = "https://www.bgeneral.com/wp-content/uploads/2021/03/Yappy%20emp%20guia%20de%20registro.pdf"
private const val YappyCredentialsTutorialUrl = "https://www.youtube.com/watch?v=h6Z4_V0QnDY"
private const val YappyOnsiteTutorialUrl = "https://comercial.yappy.com.pa/auth/login"
private const val TiloPayAffiliationUrl = "https://web.tilopay.com/start/affiliation-pty"
private const val AchPromoImageUrl = "https://paas.b-cdn.net/assets/ach-promo.png"
private const val PaypalPromoImageUrl = "https://paas.b-cdn.net/assets/ppcp-solutions-hero.png"
private const val PaymentTermsUrl = "https://tecodigi.com/paas-terminos-condiciones/"

private data class AchBankOption(
    val code: String,
    val name: String,
)

private data class AchAccountTypeOption(
    val code: String,
    val label: String,
)

private data class FeeFilterOption(
    val api: String,
    val label: String,
)

private val AchBankOptions = listOf(
    AchBankOption(code = "", name = "Selecciona un banco"),
    AchBankOption(code = "ATLAS_BANK", name = "Atlas Bank (Panama), S.A."),
    AchBankOption(code = "BAC_INTERNATIONAL", name = "BAC International Bank, Inc."),
    AchBankOption(code = "BANCO_ALIADO", name = "Banco Aliado S.A."),
    AchBankOption(code = "BANCO_AZTECA", name = "Banco Azteca (Panamá), S.A."),
    AchBankOption(code = "BBP_BANK", name = "BBP Bank S.A."),
    AchBankOption(code = "DAVIVIENDA", name = "Banco Davivienda (Panamá) S.A."),
    AchBankOption(code = "BANCO_DELTA", name = "Banco Delta, S.A."),
    AchBankOption(code = "BANCO_BOGOTA", name = "Banco de Bogotá, S.A."),
    AchBankOption(code = "BANISI", name = "BANISI, S.A."),
    AchBankOption(code = "BANCO_FICOHSA", name = "Banco Ficohsa (Panamá), S.A."),
    AchBankOption(code = "BANCO_GENERAL", name = "Banco General, S.A."),
    AchBankOption(code = "BICSA", name = "Banco Internacional de Costa Rica, S.A. (BICSA)"),
    AchBankOption(code = "BANCO_LAFISE", name = "Banco Lafise Panamá S.A."),
    AchBankOption(code = "BLADEX", name = "Banco Latinoamericano de Comercio Exterior, S.A. (BLADEX)"),
    AchBankOption(code = "LA_HIPOTECARIA", name = "Banco La Hipotecaria, S.A"),
    AchBankOption(code = "BANCOLOMBIA", name = "Bancolombia S.A."),
    AchBankOption(code = "PACIFIC_BANK", name = "Pacific Bank, S. A."),
    AchBankOption(code = "BANCO_PICHINCHA", name = "Banco Pichincha Panamá, S.A."),
    AchBankOption(code = "BANCO_PRIVAL", name = "Banco Prival, S.A. (Español) o Prival Bank, S.A. (en inglés)"),
    AchBankOption(code = "BANESCO", name = "Banesco (Panamá), S.A."),
    AchBankOption(code = "BANK_OF_CHINA", name = "Bank of China Limited"),
    AchBankOption(code = "BCT_BANK", name = "BCT Bank International S.A."),
    AchBankOption(code = "BI_BANK", name = "BI-BANK, S. A."),
    AchBankOption(code = "CANAL_BANK", name = "Canal Bank S.A."),
    AchBankOption(code = "CAPITAL_BANK", name = "Capital Bank Inc."),
    AchBankOption(code = "CITIBANK", name = "Citibank, N.A. Sucursal Panamá"),
    AchBankOption(code = "CREDICORP", name = "Credicorp Bank S.A."),
    AchBankOption(code = "GLOBAL_BANK", name = "Global Bank Corporation"),
    AchBankOption(code = "BANISTMO", name = "Banistmo S.A."),
    AchBankOption(code = "KEB_HANA", name = "KEB HANA BANK"),
    AchBankOption(code = "MEGA_INTERNATIONAL", name = "Mega International Commercial Bank Co. Ltd."),
    AchBankOption(code = "MERCANTIL", name = "Mercantil Banco, S.A."),
    AchBankOption(code = "METROBANK", name = "Metrobank, S.A."),
    AchBankOption(code = "MMG_BANK", name = "MMG Bank Corporation"),
    AchBankOption(code = "MULTIBANK", name = "Multibank Inc."),
    AchBankOption(code = "ST_GEORGES", name = "St. Georges Bank & Company, Inc."),
    AchBankOption(code = "SCOTIABANK", name = "The Bank of Nova Scotia (SCOTIABANK)"),
    AchBankOption(code = "TOWERBANK", name = "Towerbank International Inc."),
    AchBankOption(code = "UNIBANK", name = "Unibank, S.A."),
    AchBankOption(code = "ICBC", name = "Industrial and Commercial Bank of China Limited"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingPaymentScreen(
    viewModel: PaymentMethodsViewModel,
    onNavigateMethod: (PaymentMethodType) -> Unit,
    onNavigateFees: () -> Unit = {},
    onNavigateSettingsRoot: () -> Unit,
    onNavigateBusinessAddress: () -> Unit,
    isMethodRoute: Boolean = false,
    methodRoute: PaymentMethodType? = null,
    onExitMethodRoute: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val yappyOnsiteEditSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarService: SnackbarService = koinInject()

    LaunchedEffect(isMethodRoute, methodRoute) {
        if (isMethodRoute) {
            val method = methodRoute ?: return@LaunchedEffect
            viewModel.onOpenMethod(method)
        } else {
            viewModel.onEnterHomeRoute()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PaymentUiEvent.OpenExternalUrl -> openCustomTab(event.url)
                is PaymentUiEvent.ShowWarning -> snackbarService.show(event.message)
                PaymentUiEvent.NavigateToSettingsRoot -> onNavigateSettingsRoot()
                PaymentUiEvent.NavigateToPaymentMethodsHome -> {
                    if (isMethodRoute) {
                        onExitMethodRoute()
                    } else {
                        viewModel.onBackToMethods()
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = PaymentsMaxWidth)
                .fillMaxSize()
        ) {
            if (isMethodRoute) {
                when (uiState.screenMode) {
                    PaymentScreenMode.Loading -> PaymentMethodsShimmerScreen()
                    PaymentScreenMode.BlockedNoPaymentsAccess -> PaymentAccessBlockedScreen()
                    PaymentScreenMode.BlockedPaymentsModuleInactive -> HiddenPaymentsModuleRedirect(onNavigateSettingsRoot)
                    else -> MethodDetailSection(
                        uiState = uiState,
                        viewModel = viewModel,
                        isOnboardingFlow = uiState.screenMode == PaymentScreenMode.MethodDetailOnboarding,
                        onExitToMethods = onExitMethodRoute,
                    )
                }
            } else {
                when (uiState.screenMode) {
                    PaymentScreenMode.Loading -> PaymentMethodsShimmerScreen()
                    PaymentScreenMode.BlockedNoPaymentsAccess -> PaymentAccessBlockedScreen()
                    PaymentScreenMode.BlockedPaymentsModuleInactive -> HiddenPaymentsModuleRedirect(onNavigateSettingsRoot)
                    PaymentScreenMode.GlobalOnboarding -> GlobalOnboardingSection(
                        uiState = uiState,
                        onStart = viewModel::onStartOnboarding,
                    )
                    PaymentScreenMode.ConfiguredList -> ConfiguredPaymentsSection(
                        uiState = uiState,
                        viewModel = viewModel,
                        onOpenMethod = onNavigateMethod,
                        onOpenFees = onNavigateFees,
                    )
                    PaymentScreenMode.MethodDetailOnboarding,
                    PaymentScreenMode.MethodDetailConfigured -> ConfiguredPaymentsSection(
                        uiState = uiState,
                        viewModel = viewModel,
                        onOpenMethod = onNavigateMethod,
                        onOpenFees = onNavigateFees,
                    )
                }
            }
        }

        if (uiState.loadingBottomSheet.isLoading()) {
            LoadingSheet(
                state = uiState.loadingBottomSheet,
                sheetState = loadingSheetState,
            ) {
                viewModel.hideLoading()
            }
        }

        if (uiState.showYappyOnsiteGroupSheet) {
            ModalBottomSheet(
                onDismissRequest = viewModel::onCancelYappyOnsiteGroupEdit,
                sheetState = yappyOnsiteEditSheetState,
            ) {
                YappyOnsiteGroupEditSheet(
                    uiState = uiState,
                    viewModel = viewModel,
                )
            }
        }

        if (uiState.showYappyOnsiteDeviceSheet) {
            ModalBottomSheet(
                onDismissRequest = viewModel::onCancelYappyOnsiteDeviceEdit,
                sheetState = yappyOnsiteEditSheetState,
            ) {
                YappyOnsiteDeviceEditSheet(
                    uiState = uiState,
                    viewModel = viewModel,
                )
            }
        }
    }

    if (uiState.showAddressRequiredDialog) {
        DMAlertDialog(
            title = "Configura la dirección del negocio",
            message = "Para activar pagos, primero debes registrar una dirección válida.",
            show = true,
            onDismiss = viewModel::dismissAddressRequiredDialog,
            onConfirm = {
                viewModel.dismissAddressRequiredDialog()
                onNavigateBusinessAddress()
            },
            confirmText = "Ir a dirección",
            dismissText = "Más tarde"
        )
    }

    uiState.confirmUnlinkMethod?.let { method ->
        val methodName = when (method) {
            PaymentMethodType.Paypal -> "PayPal"
            PaymentMethodType.Yappy -> "Yappy"
            PaymentMethodType.YappyOnsite -> "Yappy en caja"
            PaymentMethodType.Ach -> "ACH"
            PaymentMethodType.CardTilopay -> "TiloPay"
        }
        DMAlertDialog(
            title = "Desvincular $methodName",
            message = "¿Deseas continuar? Podrás volver a configurarlo después.",
            show = true,
            onDismiss = viewModel::dismissUnlinkDialog,
            onConfirm = viewModel::confirmUnlinkMethod,
            confirmText = "Desvincular",
            dismissText = "Cancelar"
        )
    }

    if (uiState.confirmDisableAch) {
        DMAlertDialog(
            title = "Desactivar ACH",
            message = "Se dejarán de aceptar pagos ACH con comprobante. ¿Deseas continuar?",
            show = true,
            onDismiss = viewModel::dismissDisableAchDialog,
            onConfirm = viewModel::onConfirmDisableAch,
            confirmText = "Desactivar",
            dismissText = "Cancelar"
        )
    }

    uiState.confirmDeleteYappyOnsiteGroupId?.let { groupId ->
        DMAlertDialog(
            title = "Eliminar grupo",
            message = "Se eliminará el grupo $groupId de Yappy en caja. Revisa que no tengas unidades de cobro activas antes de continuar.",
            show = true,
            onDismiss = viewModel::dismissDeleteYappyOnsiteGroupDialog,
            onConfirm = viewModel::confirmDeleteYappyOnsiteGroup,
            confirmText = "Eliminar",
            dismissText = "Cancelar"
        )
    }

    uiState.confirmDeleteYappyOnsiteDevice?.let { device ->
        DMAlertDialog(
            title = "Eliminar unidad de cobro",
            message = "Se eliminará ${device.deviceId} del grupo ${device.groupId}.",
            show = true,
            onDismiss = viewModel::dismissDeleteYappyOnsiteDeviceDialog,
            onConfirm = viewModel::confirmDeleteYappyOnsiteDevice,
            confirmText = "Eliminar",
            dismissText = "Cancelar"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentFeesScreen(
    viewModel: PaymentMethodsViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarService: SnackbarService = koinInject()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PaymentUiEvent.OpenExternalUrl -> openCustomTab(event.url)
                is PaymentUiEvent.ShowWarning -> snackbarService.show(event.message)
                PaymentUiEvent.NavigateToSettingsRoot,
                PaymentUiEvent.NavigateToPaymentMethodsHome -> Unit
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = PaymentsMaxWidth)
                .fillMaxSize()
        ) {
            when (uiState.screenMode) {
                PaymentScreenMode.Loading -> PaymentMethodsShimmerScreen()
                PaymentScreenMode.BlockedNoPaymentsAccess -> PaymentAccessBlockedScreen()
                else -> LegacyFeesDisabledScreen()
            }
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
}

@Composable
private fun PaymentAccessBlockedScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Sin acceso a pagos",
                    style = titleMediumBold(),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "No tienes permisos para esta sección.",
                    style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun HiddenPaymentsModuleRedirect(onNavigateSettingsRoot: () -> Unit) {
    LaunchedEffect(Unit) {
        onNavigateSettingsRoot()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        PaymentMethodsShimmerScreen()
    }
}

@Composable
private fun LegacyFeesDisabledScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text("Comisiones deshabilitadas", style = titleMediumBold(), textAlign = TextAlign.Center)
                Text(
                    "VentaGo ya no cobra comisiones por transacción al cliente. Esta vista se conserva solo para referencia operativa.",
                    style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GlobalOnboardingSection(
    uiState: PaymentUiState,
    onStart: () -> Unit,
) {
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BoxWithConstraints {
            val desktop = maxWidth >= 860.dp
            val heroBrush = Brush.linearGradient(
                colors = listOf(Color(0xFF0A2E66), Color(0xFF0E5AB5), Color(0xFF20A77B))
            )

            Card(
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(3.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .background(brush = heroBrush)
                        .padding(24.dp)
                ) {
                    if (desktop) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            HeroMediaBlock(
                                modifier = Modifier
                                    .weight(0.45f)
                                    .height(280.dp)
                            )
                            HeroCopyBlock(
                                modifier = Modifier.weight(0.55f),
                                canConfigurePayments = uiState.canConfigurePayments,
                                onStart = onStart,
                                fullWidthButton = false,
                            )
                        }
                    } else {
                        HeroCopyBlock(
                            modifier = Modifier.fillMaxWidth(),
                            canConfigurePayments = uiState.canConfigurePayments,
                            onStart = onStart,
                            fullWidthButton = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroMediaBlock(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Cobros protegidos y control total",
                style = bodyMediumBold(color = Color.White),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HeroCopyBlock(
    modifier: Modifier,
    canConfigurePayments: Boolean,
    onStart: () -> Unit,
    fullWidthButton: Boolean,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "PAGOS Y COBROS",
            style = labelSmall(color = Color.White.copy(alpha = 0.85f)),
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Configura canales de cobro en VentaGo",
            style = headlineMediumBold(color = Color.White),
        )
        Text(
            text = "Prepara tu negocio para cobrar en caja, por QR o con links de pago, y mantener cada venta conectada con su factura electrónica en el mismo sistema.",
            style = bodyMedium(color = Color.White.copy(alpha = 0.92f)),
        )

        BenefitRow("Cobros en sitio: acepta pagos en caja o punto de venta con QR y confirmación para tu equipo.")
        BenefitRow("Cobros a distancia: envía links de pago por WhatsApp, correo o redes sociales cuando el cliente no está en el local.")
        BenefitRow("Estrategia comercial completa: pagos y factura electrónica juntos dan una experiencia más ordenada, confiable y profesional.")

        Text(
            text = if (canConfigurePayments) {
                "Después podrás activar los canales de pago que uses en tu operación diaria."
            } else {
                "No tienes permisos para activar canales de pago. Puedes revisar esta sección cuando el negocio ya tenga canales configurados."
            },
            style = bodyMediumBold(color = Color.White),
        )

        if (canConfigurePayments) {
            Spacer(modifier = Modifier.height(8.dp))
            ButtonM(
                modifier = if (fullWidthButton) Modifier.fillMaxWidth() else Modifier.widthIn(max = 320.dp),
                containerColor = Color.White,
                contentColor = Color(0xFF0A2E66),
                onClick = onStart,
            ) {
                Text(text = "Comenzar configuración", style = bodyMediumBold())
            }
            PaymentTermsText()
        }
    }
}

@Composable
private fun PaymentTermsText(
    textColor: Color = Color.White.copy(alpha = 0.86f),
    linkColor: Color = Color.White,
) {
    Text(
        text = buildAnnotatedString {
            append("Al continuar, aceptas los ")
            withLink(
                LinkAnnotation.Url(
                    url = PaymentTermsUrl,
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.SemiBold,
                        )
                    )
                )
            ) {
                append("términos y condiciones de pagos")
            }
            append(" de TecoDigi.")
        },
        style = labelSmall(color = textColor),
        textAlign = TextAlign.Start,
        modifier = Modifier.widthIn(max = 420.dp),
    )
}

@Composable
private fun BenefitRow(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Color(0xFF22C55E)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = text,
            style = bodyMedium(color = Color.White)
        )
    }
}

@Composable
private fun ConfiguredPaymentsSection(
    uiState: PaymentUiState,
    viewModel: PaymentMethodsViewModel,
    onOpenMethod: (PaymentMethodType) -> Unit,
    onOpenFees: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Administra tus canales para cobros en línea.",
            style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )

        GeneralConfigurationCard(uiState = uiState, viewModel = viewModel)
        ChannelsCard(uiState = uiState, viewModel = viewModel, onOpenMethod = onOpenMethod)
    }
}

@Composable
private fun GeneralConfigurationCard(
    uiState: PaymentUiState,
    viewModel: PaymentMethodsViewModel,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Configuración general", style = bodyMediumBold())
                    Text(
                        "Emitir factura automáticamente al recibir un pago",
                        style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                }
                if (uiState.canConfigurePayments) {
                    Switch(
                        checked = uiState.autoInvoiceEnabled,
                        onCheckedChange = viewModel::onToggleAutoInvoice,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.secondary,
                            checkedTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
                        )
                    )
                } else {
                    Text(
                        text = if (uiState.autoInvoiceEnabled) "Activada" else "Desactivada",
                        style = bodyMediumBold(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                }
            }
        }
    }
}

@Composable
private fun FeesNavigationCard(
    uiState: PaymentUiState,
    viewModel: PaymentMethodsViewModel,
    onOpenFees: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenFees),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Payment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Comisiones fees", style = bodyMediumBold())
                Text(
                    text = if (viewModel.hasConfiguredFees()) {
                        "Por pagar: ${formatNumberToMoney(viewModel.formatCents(viewModel.feesHeadlineCents()))}"
                    } else {
                        "Consulta transacciones y ciclos de cobro"
                    },
                    style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (uiState.feeTransactions.loading || uiState.feeBatches.loading) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
            Icon(
                imageVector = Icons.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeesSummaryCard(
    uiState: PaymentUiState,
    viewModel: PaymentMethodsViewModel,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Resumen de comisiones", style = bodyMediumBold())

            val headline = formatNumberToMoney(viewModel.formatCents(viewModel.feesHeadlineCents()))
            Text(
                text = headline,
                style = headlineMediumBold(color = MaterialTheme.colorScheme.primary),
            )

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val perRow = when {
                    maxWidth >= 1000.dp -> 4
                    maxWidth >= 700.dp -> 2
                    else -> 1
                }
                val bucketWidth = when (perRow) {
                    4 -> 0.24f
                    2 -> 0.49f
                    else -> 1f
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = perRow,
                ) {
                    FeeBucket("Por pagar", uiState.feeSummary.pendingDueAmount, bucketWidth, viewModel)
                    FeeBucket("Vencido", uiState.feeSummary.overdueAmount, bucketWidth, viewModel)
                    FeeBucket("Acumulado del período", uiState.feeSummary.accruedCurrentPeriodAmount, bucketWidth, viewModel)
                    FeeBucket("Pagado histórico", uiState.feeSummary.paidAmount, bucketWidth, viewModel)
                }
            }

            Text(
                text = "${viewModel.feeDateLabel()}: ${formatApiDate(viewModel.resolveFeeDateValue())}",
                style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            if (uiState.canPayFees) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButtonM(
                        modifier = Modifier.weight(1f),
                        onClick = viewModel::refreshCommissions,
                    ) {
                        Text("Actualizar")
                    }
                    ButtonM(
                        modifier = Modifier.weight(1f),
                        onClick = viewModel::onPayCommissions,
                        enabled = viewModel.feesHeadlineCents() > 0L,
                    ) {
                        Text("Pagar comisiones")
                    }
                }
            } else {
                OutlinedButtonM(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = viewModel::refreshCommissions,
                ) {
                    Text("Actualizar")
                }
            }
        }
    }
}

@Composable
private fun FeeBucket(
    label: String,
    cents: Long,
    widthFraction: Float,
    viewModel: PaymentMethodsViewModel,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .shadow(0.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = vanishedBackgroundColor()),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = label, style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant))
            Text(
                text = formatNumberToMoney(viewModel.formatCents(cents)),
                style = bodyMediumBold(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ChannelsCard(
    uiState: PaymentUiState,
    viewModel: PaymentMethodsViewModel,
    onOpenMethod: (PaymentMethodType) -> Unit,
) {
    fun shouldShowMethod(method: PaymentMethodType): Boolean {
        return viewModel.methodVisible(method) &&
            (uiState.canConfigurePayments || viewModel.methodConfigured(method))
    }

    val physicalRows = listOf(PaymentMethodType.YappyOnsite).filter(::shouldShowMethod)
    val linkRows = listOf(
        PaymentMethodType.CardTilopay,
        PaymentMethodType.Yappy,
        PaymentMethodType.Ach,
        PaymentMethodType.Paypal,
    ).filter(::shouldShowMethod)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Canales disponibles",
                style = titleMediumBold(color = MaterialTheme.colorScheme.onSurface),
            )
            Text(
                text = "Elige el método según el tipo de cobro que quieres ofrecer.",
                style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
        }

        if (physicalRows.isNotEmpty()) {
            ChannelGroupHeader(
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Payment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp),
                    )
                },
                title = "Cobros físicos",
                description = "Métodos para ventas presenciales en caja con confirmación automática del pago.",
            )
            ChannelGroupCard {
                physicalRows.forEachIndexed { index, method ->
                    ChannelRow(
                        method = method,
                        configured = viewModel.methodConfigured(method),
                        enabled = uiState.canConfigurePayments,
                        onClick = { onOpenMethod(method) },
                    )
                    if (index < physicalRows.lastIndex) ChannelDivider()
                }
            }
        }

        if (linkRows.isNotEmpty()) {
            ChannelGroupHeader(
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp),
                    )
                },
                title = "QR y links de pago",
                description = "Métodos para cobrar desde links enviados por WhatsApp, correo, redes sociales o QR compartidos.",
            )
            ChannelGroupCard {
                linkRows.forEachIndexed { index, method ->
                    ChannelRow(
                        method = method,
                        configured = viewModel.methodConfigured(method),
                        enabled = uiState.canConfigurePayments,
                        onClick = { onOpenMethod(method) },
                    )
                    if (index < linkRows.lastIndex) ChannelDivider()
                }
            }
        }
    }
}

@Composable
private fun ChannelGroupHeader(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = bodyMediumBold(color = MaterialTheme.colorScheme.onSurface),
            )
            Text(
                text = description,
                style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
        }
    }
}

@Composable
private fun ChannelGroupCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun ChannelRow(
    method: PaymentMethodType,
    configured: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ChannelLogo(method)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = channelTitle(method),
                style = bodyMediumBold(color = MaterialTheme.colorScheme.onSurface),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (configured) {
                ChannelConfiguredBadge()
            }
        }

        if (enabled) {
            Icon(
                imageVector = Icons.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun ChannelDivider() {
    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
}

private fun channelTitle(method: PaymentMethodType): String = when (method) {
    PaymentMethodType.Yappy -> "Botón de pago Yappy"
    PaymentMethodType.YappyOnsite -> "Yappy en caja"
    PaymentMethodType.Ach -> "ACH con comprobante de pago"
    PaymentMethodType.Paypal -> "PayPal"
    PaymentMethodType.CardTilopay -> "Tarjetas de crédito o débito | Tilopay"
}

@Composable
private fun ChannelLogo(method: PaymentMethodType) {
    Box(
        modifier = Modifier
            .width(86.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
            ),
        contentAlignment = Alignment.Center
    ) {
        when (method) {
            PaymentMethodType.Yappy -> Icon(
                painter = painterResource(Res.drawable.yappy_logo),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .width(68.dp)
                    .height(22.dp)
            )
            PaymentMethodType.YappyOnsite -> Icon(
                painter = painterResource(Res.drawable.yappy_logo),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .width(68.dp)
                    .height(22.dp)
            )
            PaymentMethodType.Ach -> Icon(
                painter = painterResource(Res.drawable.ic_bank),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
            PaymentMethodType.Paypal -> Icon(
                painter = painterResource(Res.drawable.ic_paypal_logo),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .width(66.dp)
                    .height(22.dp)
            )
            PaymentMethodType.CardTilopay -> CardBrandLogo()
        }
    }
}

@Composable
private fun CardBrandLogo() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = "VISA",
            style = bodyMediumBold(color = Color(0xFF172B85)),
            maxLines = 1,
        )
        Box(modifier = Modifier.width(24.dp).height(16.dp)) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.CenterStart)
                    .clip(CircleShape)
                    .background(Color(0xFFEA001B)),
            )
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.CenterEnd)
                    .clip(CircleShape)
                    .background(Color(0xFFFFA200).copy(alpha = 0.92f)),
            )
        }
    }
}

@Composable
private fun StatusPill(text: String, positive: Boolean) {
    val bg = if (positive) Color(0xFFECFDF3) else Color(0xFFFFF4E6)
    val fg = if (positive) Color(0xFF0E9F6E) else Color(0xFFB45309)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = text, style = labelSmall(color = fg))
    }
}

@Composable
private fun ChannelConfiguredBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.secondary)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = "Configurado",
            style = labelSmall(color = MaterialTheme.colorScheme.onSecondary),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CommissionsSection(
    uiState: PaymentUiState,
    viewModel: PaymentMethodsViewModel,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Detalle de comisiones", style = bodyMediumBold())
                IconButton(onClick = viewModel::refreshCommissions) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TabPill(
                    selected = uiState.viewMode == PaymentViewMode.Transactions,
                    label = "Transacciones",
                    onClick = { viewModel.onViewModeSelected(PaymentViewMode.Transactions) }
                )
                TabPill(
                    selected = uiState.viewMode == PaymentViewMode.BillingCycles,
                    label = "Ciclos de cobro",
                    onClick = { viewModel.onViewModeSelected(PaymentViewMode.BillingCycles) }
                )
            }

            when (uiState.viewMode) {
                PaymentViewMode.Transactions -> TransactionsTab(uiState = uiState, viewModel = viewModel)
                PaymentViewMode.BillingCycles -> BatchesTab(uiState = uiState, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun FeesDetailsSection(
    uiState: PaymentUiState,
    viewModel: PaymentMethodsViewModel,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Consulta el resumen, las transacciones y los ciclos de cobro de tus comisiones.",
            style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        if (viewModel.hasConfiguredFees()) {
            FeesSummaryCard(uiState = uiState, viewModel = viewModel)
        }
        CommissionsSection(uiState = uiState, viewModel = viewModel)
    }
}

@Composable
private fun TabPill(selected: Boolean, label: String, onClick: () -> Unit) {
    val background = if (selected) MaterialTheme.colorScheme.secondary else Color.Transparent
    val textColor = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.secondary

    Text(
        text = label,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        style = bodyMedium(color = textColor),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun TransactionsTab(
    uiState: PaymentUiState,
    viewModel: PaymentMethodsViewModel,
) {
    val statusItems = listOf(
        FeeFilterOption("", "Todos"),
        FeeFilterOption("unpaid", "Pendiente"),
        FeeFilterOption("paid", "Pagado"),
    )
    val methodItems = listOf(
        FeeFilterOption("", "Todos"),
        FeeFilterOption("YAPPY", "Yappy"),
        FeeFilterOption("ACH", "ACH"),
        FeeFilterOption("TILOPAY_CARD", "Tarjeta"),
    )

    DMDropDownField(
        label = "Estado",
        items = statusItems,
        selectedIndex = statusItems.indexOfFirst { it.api == uiState.filters.transactionsStatus }
            .takeIf { it >= 0 } ?: 0,
        onItemSelected = { _, item -> viewModel.onTransactionsStatusFilterChange(item.api) },
        selectedItemToString = { it.label },
    )

    DMDropDownField(
        label = "Método",
        items = methodItems,
        selectedIndex = methodItems.indexOfFirst { it.api == uiState.filters.transactionsMethod.orEmpty() }
            .takeIf { it >= 0 } ?: 0,
        onItemSelected = { _, item ->
            viewModel.onTransactionsMethodFilterChange(item.api.takeIf { it.isNotBlank() })
        },
        selectedItemToString = { it.label },
    )

    uiState.filters.selectedBatchId?.let { batchId ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Ciclo #$batchId",
                style = bodyMediumBold(color = MaterialTheme.colorScheme.onSecondaryContainer),
            )
            TextButtonS(
                label = "Ver todo de nuevo",
                onClick = viewModel::onClearFeeBatchFilter,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }

    if (uiState.feeTransactions.loading && uiState.feeTransactions.items.isEmpty()) {
        InlineShimmerList()
    } else if (uiState.feeTransactions.items.isEmpty()) {
        EmptyListState(message = "No hay transacciones para los filtros seleccionados.")
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            uiState.feeTransactions.items.forEach { item ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(item.relatedOrderNumber.ifBlank { "Orden #${item.orderId ?: "-"}" }, style = bodyMediumBold())
                            StatusPill(
                                text = FeeTransactionStatusUi.fromApi(item.feeStatus).label,
                                positive = item.feeStatus.equals("paid", ignoreCase = true),
                            )
                        }
                        Text(
                            text = "Método: ${feePaymentMethodLabel(item.paymentMethod)}",
                            style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        )
                        Text(
                            text = "Fecha: ${formatApiDate(item.date)}",
                            style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        )
                        Text(
                            text = "Comisión: ${formatNumberToMoney(viewModel.formatCents(item.feeGenerated))}",
                            style = bodyMediumBold(color = MaterialTheme.colorScheme.primary),
                        )
                    }
                }
            }

            if (uiState.feeTransactions.items.size < uiState.feeTransactions.total) {
                OutlinedButtonM(
                    onClick = viewModel::loadMoreTransactions,
                    enabled = !uiState.feeTransactions.loadingMore,
                ) {
                    Text(if (uiState.feeTransactions.loadingMore) "Cargando..." else "Cargar más")
                }
            }
        }
    }
}

@Composable
private fun BatchesTab(
    uiState: PaymentUiState,
    viewModel: PaymentMethodsViewModel,
) {
    val statusItems = listOf(
        FeeFilterOption("", "Todos"),
        FeeFilterOption("issued", "Emitido"),
        FeeFilterOption("pending_due", "Por pagar"),
        FeeFilterOption("paid", "Pagado"),
    )

    DMDropDownField(
        label = "Estado",
        items = statusItems,
        selectedIndex = statusItems.indexOfFirst { it.api == uiState.filters.batchesStatus }
            .takeIf { it >= 0 } ?: 0,
        onItemSelected = { _, item -> viewModel.onBatchesStatusFilterChange(item.api) },
        selectedItemToString = { it.label },
    )

    if (uiState.feeBatches.loading && uiState.feeBatches.items.isEmpty()) {
        InlineShimmerList()
    } else if (uiState.feeBatches.items.isEmpty()) {
        EmptyListState(message = "No hay ciclos de cobro para los filtros seleccionados.")
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            uiState.feeBatches.items.forEach { item ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(formatBatchPeriodLabel(item.periodStart, item.periodEnd), style = bodyMediumBold())
                            StatusPill(
                                text = FeeBatchStatusUi.fromApi(item.status).label,
                                positive = item.status.equals("paid", ignoreCase = true),
                            )
                        }
                        Text(
                            text = "Total fees: ${formatNumberToMoney(viewModel.formatCents(item.totalFeeTotal))}",
                            style = bodyMediumBold(color = MaterialTheme.colorScheme.primary),
                        )
                        Text(
                            text = "Vence: ${formatApiDate(item.dueAt)}",
                            style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        )
                        item.id?.let { batchId ->
                            OutlinedButtonM(onClick = { viewModel.onFeeBatchSelected(batchId) }) {
                                Text("Ver detalles")
                            }
                        }
                    }
                }
            }

            if (uiState.feeBatches.items.size < uiState.feeBatches.total) {
                OutlinedButtonM(
                    onClick = viewModel::loadMoreBatches,
                    enabled = !uiState.feeBatches.loadingMore,
                ) {
                    Text(if (uiState.feeBatches.loadingMore) "Cargando..." else "Cargar más")
                }
            }
        }
    }
}

@Composable
private fun MethodDetailSection(
    uiState: PaymentUiState,
    viewModel: PaymentMethodsViewModel,
    isOnboardingFlow: Boolean,
    onExitToMethods: () -> Unit,
) {
    val activeMethod = uiState.activeMethod ?: return
    val isYappyOnboarding = isOnboardingFlow && activeMethod == PaymentMethodType.Yappy
    val isYappyOnsiteOnboarding = isOnboardingFlow && activeMethod == PaymentMethodType.YappyOnsite
    val isAchOnboarding = isOnboardingFlow && activeMethod == PaymentMethodType.Ach
    val isPaypalOnboarding = isOnboardingFlow && activeMethod == PaymentMethodType.Paypal
    val isTiloPayOnboarding = isOnboardingFlow && activeMethod == PaymentMethodType.CardTilopay
    val yappyGuideStep = uiState.activeStep in 1..4
    val yappyOnsiteGuideStep = uiState.activeStep in 1..4
    val achGuideStep = uiState.activeStep in 1..4
    val paypalGuideStep = uiState.activeStep in 1..4
    val tiloPayGuideStep = uiState.activeStep in 1..4
    val scroll = rememberScrollState()
    val focusManager = LocalFocusManager.current

    if (!uiState.canConfigurePayments) {
        ReadOnlyMethodDetailSection(
            method = activeMethod,
            onExitToMethods = onExitToMethods,
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .dismissKeyboardOnOutsideTap(focusManager)
            .verticalScroll(scroll)
            .padding(
                horizontal = if (activeMethod == PaymentMethodType.YappyOnsite) 10.dp else 16.dp,
                vertical = 16.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isOnboardingFlow) {
            val shouldShowStepper = when {
                isYappyOnboarding -> uiState.activeStep <= 4
                isYappyOnsiteOnboarding -> uiState.activeStep <= 4
                isAchOnboarding -> uiState.activeStep <= 4
                isPaypalOnboarding -> uiState.activeStep <= 4
                isTiloPayOnboarding -> uiState.activeStep <= 4
                else -> uiState.activeStep <= 2
            }
            if (shouldShowStepper) {
                StepperPills(
                    step = uiState.activeStep,
                    totalSteps = when {
                        isYappyOnboarding -> 4
                        isYappyOnsiteOnboarding -> 4
                        isAchOnboarding -> 4
                        isPaypalOnboarding -> 4
                        isTiloPayOnboarding -> 4
                        else -> 3
                    },
                )
            }
        }

        when {
            isYappyOnboarding && yappyGuideStep -> YappyOnboardingStep(
                step = uiState.activeStep,
                uiState = uiState,
                viewModel = viewModel,
            )
            isYappyOnboarding && uiState.activeStep == 5 -> MethodSuccessStep(
                onDone = onExitToMethods,
            )
            isYappyOnsiteOnboarding && yappyOnsiteGuideStep -> YappyOnsiteOnboardingStep(
                step = uiState.activeStep,
                uiState = uiState,
                viewModel = viewModel,
            )
            isAchOnboarding && achGuideStep -> AchOnboardingStep(
                step = uiState.activeStep,
                uiState = uiState,
                viewModel = viewModel,
            )
            isAchOnboarding && uiState.activeStep == 5 -> MethodSuccessStep(onDone = onExitToMethods)
            isPaypalOnboarding && paypalGuideStep -> PaypalOnboardingStep(
                step = uiState.activeStep,
                uiState = uiState,
                viewModel = viewModel,
            )
            isPaypalOnboarding && uiState.activeStep == 5 -> MethodSuccessStep(onDone = onExitToMethods)
            isTiloPayOnboarding && tiloPayGuideStep -> TiloPayOnboardingStep(
                step = uiState.activeStep,
                uiState = uiState,
                viewModel = viewModel,
            )
            isTiloPayOnboarding && uiState.activeStep == 5 -> MethodSuccessStep(onDone = onExitToMethods)
            isOnboardingFlow && uiState.activeStep == 1 -> MethodIntroCarousel(activeMethod)
            isOnboardingFlow && uiState.activeStep == 3 -> MethodSuccessStep(onDone = onExitToMethods)
            else -> MethodConfigurationStep(uiState = uiState, viewModel = viewModel, method = activeMethod, isOnboardingFlow = isOnboardingFlow)
        }

        if (isYappyOnsiteOnboarding && yappyOnsiteGuideStep) {
            YappyOnsiteOnboardingFooter(
                step = uiState.activeStep,
                editingGroup = uiState.editingYappyOnsiteGroupId != null,
                editingDevice = uiState.editingYappyOnsiteDeviceId != null,
                onBack = {
                    if (uiState.activeStep <= 1) onExitToMethods() else viewModel.onPrevStep()
                },
                onPrimary = {
                    when (uiState.activeStep) {
                        2 -> viewModel.onSaveYappyOnsiteGroups()
                        3 -> viewModel.onSaveYappyOnsiteDevices()
                        4 -> onExitToMethods()
                        else -> viewModel.onNextStep()
                    }
                },
            )
        } else if (isYappyOnboarding && yappyGuideStep) {
            YappyOnboardingFooter(
                step = uiState.activeStep,
                onBack = {
                    if (uiState.activeStep <= 1) onExitToMethods() else viewModel.onPrevStep()
                },
                onPrimary = {
                    if (uiState.activeStep < 4) {
                        viewModel.onNextStep()
                    } else {
                        viewModel.onSaveYappy()
                    }
                },
            )
        } else if (isAchOnboarding && achGuideStep) {
            AchOnboardingFooter(
                step = uiState.activeStep,
                onBack = {
                    if (uiState.activeStep <= 1) onExitToMethods() else viewModel.onPrevStep()
                },
                onPrimary = {
                    if (uiState.activeStep < 4) {
                        viewModel.onNextStep()
                    } else {
                        viewModel.onSaveAch()
                    }
                },
            )
        } else if (isPaypalOnboarding && paypalGuideStep) {
            PaypalOnboardingFooter(
                step = uiState.activeStep,
                onBack = {
                    if (uiState.activeStep <= 1) onExitToMethods() else viewModel.onPrevStep()
                },
                onPrimary = {
                    if (uiState.activeStep < 4) viewModel.onNextStep() else viewModel.onConnectPaypal()
                },
            )
        } else if (isTiloPayOnboarding && tiloPayGuideStep) {
            TiloPayOnboardingFooter(
                step = uiState.activeStep,
                onBack = {
                    if (uiState.activeStep <= 1) onExitToMethods() else viewModel.onPrevStep()
                },
                onPrimary = {
                    if (uiState.activeStep < 4) viewModel.onNextStep() else viewModel.onSaveTiloPay()
                },
            )
        } else if (isOnboardingFlow && uiState.activeStep <= 2) {
            OnboardingFooter(step = uiState.activeStep, method = activeMethod, onBack = {
                if (uiState.activeStep <= 1) onExitToMethods() else viewModel.onPrevStep()
            }, onPrimary = {
                when {
                    uiState.activeStep == 1 -> viewModel.onNextStep()
                    activeMethod == PaymentMethodType.Ach -> viewModel.onSaveAch()
                    activeMethod == PaymentMethodType.Paypal -> viewModel.onNextStep()
                    activeMethod == PaymentMethodType.CardTilopay -> viewModel.onSaveTiloPay()
                    else -> viewModel.onNextStep()
                }
            })
        }
    }
}

@Composable
private fun ReadOnlyMethodDetailSection(
    method: PaymentMethodType,
    onExitToMethods: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
            shape = RoundedCornerShape(18.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(channelTitle(method), style = titleMediumBold())
                StatusPill(text = "Configurado", positive = true)
                Text(
                    text = "Tu usuario puede ver este canal, pero no modificar su configuración.",
                    style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }
        }
        OutlinedButtonM(onClick = onExitToMethods) {
            Text("Volver")
        }
    }
}

@Composable
private fun StepperPills(step: Int, totalSteps: Int = 3) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        (1..totalSteps).forEach { index ->
            val active = index <= step
            val background = if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
            val color = if (active) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(background),
                contentAlignment = Alignment.Center
            ) {
                Text(text = index.toString(), style = bodyMediumBold(color = color))
            }
        }
    }
}

@Composable
private fun MethodIntroCarousel(method: PaymentMethodType) {
    val items = when (method) {
        PaymentMethodType.Ach -> listOf(
            "Comparte datos bancarios para transferencias ACH.",
            "Tus clientes suben comprobante para revisión.",
            "Activa o desactiva ACH cuando lo necesites."
        )
        PaymentMethodType.Paypal -> listOf(
            "Conecta tu cuenta PayPal empresarial.",
            "Completa el flujo seguro de conexión de PayPal.",
            "Usa el estado conectado para completar el onboarding."
        )
        PaymentMethodType.Yappy -> listOf(
            "Configura Yappy con tus credenciales comerciales.",
            "Conecta tu cuenta y habilita cobros instantáneos.",
            "Gestiona tus cobros desde el módulo de métodos de pago."
        )
        PaymentMethodType.YappyOnsite -> listOf(
            "Genera un QR en caja para que el cliente pague al momento.",
            "Detecta el pago automáticamente y reduce comprobantes falsos.",
            "Emite la factura cuando el pago queda confirmado."
        )
        PaymentMethodType.CardTilopay -> listOf(
            "Acepta tarjetas en links de pago mediante TiloPay.",
            "Guarda credenciales del comercio para habilitar el proveedor.",
            "TiloPay gestiona procesamiento, disputas y liquidaciones."
        )
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items) { text ->
            Card(
                modifier = Modifier
                    .width(270.dp)
                    .height(150.dp),
                colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = text,
                        style = bodyMedium(),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun MethodConfigurationStep(
    uiState: PaymentUiState,
    viewModel: PaymentMethodsViewModel,
    method: PaymentMethodType,
    isOnboardingFlow: Boolean,
) {
    when (method) {
        PaymentMethodType.Yappy -> YappyConfiguration(uiState, viewModel)
        PaymentMethodType.YappyOnsite -> YappyOnsiteSummaryStep(uiState, viewModel)
        PaymentMethodType.Ach -> AchConfiguration(uiState, viewModel)
        PaymentMethodType.Paypal -> PaypalConfiguration(uiState, viewModel, isOnboardingFlow)
        PaymentMethodType.CardTilopay -> TiloPayConfiguration(uiState, viewModel, isOnboardingFlow)
    }
}

@Composable
private fun YappyOnboardingStep(
    step: Int,
    uiState: PaymentUiState,
    viewModel: PaymentMethodsViewModel,
) {
    when (step) {
        1 -> YappyProductInfoStep()
        2 -> YappyRequirementsStep(
            onOpenBusinessGuide = { viewModel.onOpenExternalUrl(YappyBusinessGuideUrl) },
            onOpenEntrepreneurGuide = { viewModel.onOpenExternalUrl(YappyEntrepreneurGuideUrl) },
        )
        3 -> YappyCostsStep()
        4 -> YappyConfiguration(
            uiState = uiState,
            viewModel = viewModel,
            showTutorialLink = true,
            showUnlink = false,
        )
    }
}

@Composable
private fun YappyOnsiteOnboardingStep(
    step: Int,
    uiState: PaymentUiState,
    viewModel: PaymentMethodsViewModel,
) {
    when (step) {
        1 -> YappyOnsiteHowItWorksStep()
        2 -> YappyOnsiteGroupStep(uiState, viewModel)
        3 -> YappyOnsiteDeviceStep(uiState, viewModel)
        4 -> YappyOnsiteReadyStep()
    }
}

@Composable
private fun AchOnboardingStep(
    step: Int,
    uiState: PaymentUiState,
    viewModel: PaymentMethodsViewModel,
) {
    when (step) {
        1 -> AchHowItWorksStep()
        2 -> AchKnowledgeStep()
        3 -> AchFeesStep()
        4 -> AchConfigurationOnboarding(
            uiState = uiState,
            viewModel = viewModel,
        )
    }
}

@Composable
private fun PaypalOnboardingStep(
    step: Int,
    uiState: PaymentUiState,
    viewModel: PaymentMethodsViewModel,
) {
    when (step) {
        1 -> PaypalHowItWorksStep()
        2 -> PaypalRequirementsStep()
        3 -> PaypalFeesStep()
        4 -> PaypalConfiguration(
            uiState = uiState,
            viewModel = viewModel,
            isOnboardingFlow = true,
        )
    }
}

@Composable
private fun TiloPayOnboardingStep(
    step: Int,
    uiState: PaymentUiState,
    viewModel: PaymentMethodsViewModel,
) {
    when (step) {
        1 -> TiloPayHowItWorksStep()
        2 -> TiloPayFeesStep()
        3 -> TiloPayAccountStep(
            onOpenAffiliation = { viewModel.onOpenExternalUrl(TiloPayAffiliationUrl) },
        )
        4 -> TiloPayConfiguration(
            uiState = uiState,
            viewModel = viewModel,
            isOnboardingFlow = true,
        )
    }
}

@Composable
private fun TiloPayHowItWorksStep() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "Conoce Tarjetas de crédito o débito | Tilopay", style = titleMediumBold())
            Text(
                text = "Revisa cómo funciona, los requisitos y los costos antes de conectar este método.",
                style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
            CardBrandStrip()
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            Text(text = "Cómo funciona", style = bodyMediumBold())
            Text(
                text = "Permite cobrar con tarjetas Visa y Mastercard desde el link de pago usando TiloPay.",
                style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
            BulletItem("VentaGo usa únicamente la integración de pagos con tarjeta de TiloPay; no habilitamos otros productos de TiloPay.")
            BulletItem("VentaGo no procesa pagos ni responde por disputas, contracargos o situaciones del cargo con tarjeta.")
            BulletItem("El manejo operativo del pago, liquidaciones y reclamos se coordina directamente con TiloPay.")
        }
    }
}

@Composable
private fun TiloPayFeesStep() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(text = "Costos del procesador", style = titleMediumBold())
            Text(
                text = "Estos cargos aplican cuando el pago con tarjeta se completa correctamente. TiloPay confirma las condiciones finales de tu afiliación.",
                style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
            TiloPayCardFeeSection(
                brand = "Visa",
                logo = { VisaLogo() },
            )
            ChannelDivider()
            TiloPayCardFeeSection(
                brand = "Mastercard",
                logo = { MastercardLogo() },
            )
            ChannelDivider()
            TiloPayCardFeeSection(
                brand = "American Express",
                logo = { AmericanExpressLogo() },
            )
            Text(
                text = "VentaGo habilita este canal cuando el módulo de pagos y cobros está activo. No somos procesador de pagos; disputas, contracargos, liquidaciones y reclamos del cargo se gestionan directamente con TiloPay.",
                style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
            Text(
                text = "Solo compatible con links de pago.",
                style = bodyMediumBold(color = MaterialTheme.colorScheme.onSurface),
            )
        }
    }
}

@Composable
private fun TiloPayCardFeeSection(
    brand: String,
    logo: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = brand,
            style = bodyMediumBold(color = MaterialTheme.colorScheme.onSurface),
        )
        logo()
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "TILOPAY",
                    style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "3.75% + $0.50",
                    style = headlineMediumBold(color = MaterialTheme.colorScheme.primary),
                    maxLines = 1,
                )
                Text(
                    text = "por transacción + ITBMS",
                    style = labelSmall(color = MaterialTheme.colorScheme.primary),
                    maxLines = 1,
                )
            }
            Text(
                text = "+",
                style = titleMediumBold(color = MaterialTheme.colorScheme.onSurface),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "VENTAGO",
                    style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "0.50%",
                    style = headlineMediumBold(color = Color(0xFF0F766E)),
                    maxLines = 1,
                )
                Text(
                    text = "por transacción exitosa + ITBMS",
                    style = labelSmall(color = Color(0xFF0F766E)),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun CardBrandStrip() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VisaLogo()
        MastercardLogo()
        AmericanExpressLogo()
    }
}

@Composable
private fun VisaLogo() {
    Box(
        modifier = Modifier
            .width(56.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "VISA",
                style = labelSmall(color = Color(0xFF172B85)),
                fontWeight = FontWeight.Black,
            )
            Box(
                modifier = Modifier
                    .width(34.dp)
                    .height(4.dp)
                    .background(Color(0xFFFFA200)),
            )
        }
    }
}

@Composable
private fun MastercardLogo() {
    Box(
        modifier = Modifier
            .width(42.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF4678D7)),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.width(26.dp).height(16.dp)) {
            Box(
                modifier = Modifier
                    .size(15.dp)
                    .align(Alignment.CenterStart)
                    .clip(CircleShape)
                    .background(Color(0xFFFF5F00)),
            )
            Box(
                modifier = Modifier
                    .size(15.dp)
                    .align(Alignment.CenterEnd)
                    .clip(CircleShape)
                    .background(Color(0xFFFFC14D).copy(alpha = 0.92f)),
            )
        }
    }
}

@Composable
private fun AmericanExpressLogo() {
    Box(
        modifier = Modifier
            .width(56.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF4DB6D7)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "AMERICAN\nEXPRESS",
            style = labelSmall(color = Color.White),
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun TiloPayAccountStep(onOpenAffiliation: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Cuenta TiloPay", style = titleMediumBold())
            Text("¿Aún no tienes cuenta TiloPay?", style = bodyMediumBold())
            Text(
                text = "Solicita tu afiliación antes de configurar credenciales",
                style = bodyMediumBold(color = MaterialTheme.colorScheme.primary),
            )
            Text(
                text = "Para cobrar con tarjeta necesitas una cuenta comercial aprobada por TiloPay. Cuando la tengas, TiloPay te entrega el Usuario API, Contraseña API y Llave API para conectarla con VentaGo.",
                style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
            OutlinedButtonM(
                onClick = onOpenAffiliation,
                contentColor = MaterialTheme.colorScheme.primary,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            ) {
                Text("Solicitar cuenta TiloPay")
            }
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            Text(
                text = "Si ya tienes una cuenta comercial activa, continúa para ingresar tu Usuario API, Contraseña API y Llave API de TiloPay.",
                style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
        }
    }
}

@Composable
private fun TiloPayConfiguration(
    uiState: PaymentUiState,
    viewModel: PaymentMethodsViewModel,
    isOnboardingFlow: Boolean,
) {
    val configured = viewModel.methodConfigured(PaymentMethodType.CardTilopay)
    val status = uiState.tiloPayStatus
    val maskedCredential = "*********"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Credenciales TiloPay", style = titleMediumBold())
            Text(
                "Ingresa las credenciales API entregadas por TiloPay para aceptar tarjetas en links de pago.",
                style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )

            StatusRow(
                label = "Canal habilitado",
                active = configured || status?.readyForPayments() == true,
            )

            DMOutlinedTextField(
                text = credentialDisplayValue(uiState.tiloPayForm.apiKey, configured, maskedCredential),
                label = "Llave API",
                modifier = Modifier.fillMaxWidth(),
                onChange = { value ->
                    viewModel.onTiloPayApiKeyChange(credentialInputValue(value, maskedCredential))
                },
            )
            DMOutlinedTextField(
                text = credentialDisplayValue(uiState.tiloPayForm.apiUser, configured, maskedCredential),
                label = "Usuario API",
                modifier = Modifier.fillMaxWidth(),
                onChange = { value ->
                    viewModel.onTiloPayApiUserChange(credentialInputValue(value, maskedCredential))
                },
            )
            DMOutlinedTextField(
                text = credentialDisplayValue(uiState.tiloPayForm.password, configured, maskedCredential),
                label = "Contraseña API",
                modifier = Modifier.fillMaxWidth(),
                onChange = { value ->
                    viewModel.onTiloPayPasswordChange(credentialInputValue(value, maskedCredential))
                },
            )

            if (configured) {
                Text(
                    text = "Por seguridad no mostramos credenciales guardadas. Para actualizar el canal, ingresa los tres valores nuevamente.",
                    style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }

            if (!isOnboardingFlow) {
                ButtonM(onClick = viewModel::onSaveTiloPay) {
                    Text(if (configured) "Actualizar cuenta TiloPay" else "Vincular cuenta TiloPay")
                }
            }
            if (!isOnboardingFlow && configured) {
                OutlinedButtonM(onClick = { viewModel.requestUnlinkMethod(PaymentMethodType.CardTilopay) }) {
                    Text("Desconectar TiloPay")
                }
            }
        }
    }
}

private fun credentialDisplayValue(
    value: String,
    configured: Boolean,
    maskedCredential: String,
): String = if (configured && value.isBlank()) maskedCredential else value

private fun credentialInputValue(
    value: String,
    maskedCredential: String,
): String = value.replace(maskedCredential, "")

@Composable
private fun PaypalHowItWorksStep() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = getImageRequest(LocalPlatformContext.current, PaypalPromoImageUrl),
                contentDescription = "Promo PayPal",
                placeholder = ColorPainter(Color(0xFFE5E7EB)),
                error = ColorPainter(Color(0xFFE5E7EB)),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(14.dp)),
            )
            Text(text = "Cómo funciona", style = titleMediumBold())
            BulletItem("Conecta la cuenta PayPal empresarial del negocio.")
            BulletItem("El cliente puede pagar con su cuenta o métodos compatibles de PayPal.")
            BulletItem("En Panamá es un canal complementario para ventas online.")
        }
    }
}

@Composable
private fun PaypalRequirementsStep() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "Requisitos", style = titleMediumBold())
            BulletItem("Iniciar sesión con la cuenta PayPal del negocio.")
            BulletItem("Completar la autorización de PayPal en la página segura que se abrirá.")
        }
    }
}

@Composable
private fun PaypalFeesStep() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "Costos y cobros", style = titleMediumBold())
            BulletItem("PayPal puede aplicar sus propias tarifas de procesamiento.")
            BulletItem("VentaGo habilita este canal cuando el módulo de pagos y cobros está activo.")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFFBEB))
                    .border(
                        width = 1.dp,
                        color = Color(0xFFFCD34D),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = Color(0xFFB45309),
                )
                Text(
                    text = "Solo compatible con links de pago.",
                    style = bodyMediumBold(color = Color(0xFF92400E)),
                )
            }
        }
    }
}

@Composable
private fun AchHowItWorksStep() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = getImageRequest(LocalPlatformContext.current, AchPromoImageUrl),
                contentDescription = "Promo ACH",
                placeholder = ColorPainter(Color(0xFFE5E7EB)),
                error = ColorPainter(Color(0xFFE5E7EB)),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(14.dp)),
            )
            Text(text = "Cómo funciona", style = titleMediumBold())
            Text(
                text = "Permite que tus clientes paguen por transferencia ACH y suban su comprobante en el mismo link de pago. El sistema centraliza la evidencia y te muestra señales de riesgo para ayudarte a revisar cada pago con más rapidez.\n\nTu negocio recibe notificaciones, decide si acepta o rechaza el comprobante y, si no se revisa en 7 días, el comprobante expira automáticamente.",
                style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
            SurfaceBadge(
                title = "Validación no automática",
                description = "No consultamos directamente el banco del cliente; revisamos el comprobante subido y te mostramos señales de riesgo.",
                icon = Icons.Filled.Info,
            )
        }
    }
}

@Composable
private fun AchKnowledgeStep() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(text = "Conoce ACH con comprobante de pago", style = titleMediumBold())
            Text(
                text = "Revisa cómo funciona, los requisitos y los costos antes de conectar este método.",
                style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )

            SurfaceBadge(
                title = "Validación del comprobante",
                description = "ACH con comprobante no confirma el movimiento directamente con el banco del cliente. Te ayuda a revisar mejor la evidencia antes de aprobarla.",
                icon = Icons.Filled.Info,
            )

            AchProcessCard(
                title = "1 El cliente sube el comprobante",
                description = "Desde el link de pago carga la imagen o PDF del ticket de transferencia.",
            )
            AchProcessCard(
                title = "2 Ventago lee los datos clave",
                description = "Identificamos banco, monto, fecha y referencias visibles para compararlos con la orden.",
            )
            AchProcessCard(
                title = "3 Se calcula el riesgo",
                description = "Te avisamos si el monto no coincide, si faltan datos o si el comprobante muestra señales de alteración.",
            )
            AchProcessCard(
                title = "4 Tu negocio decide",
                description = "Con los detalles encontrados, apruebas o rechazas el comprobante antes de confirmar el pago.",
            )
        }
    }
}

@Composable
private fun AchProcessCard(
    title: String,
    description: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = bodyMediumBold())
            Text(
                description,
                style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
        }
    }
}

@Composable
private fun SurfaceBadge(
    title: String,
    description: String,
    icon: ImageVector,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = bodyMediumBold(color = MaterialTheme.colorScheme.secondary))
            Text(
                description,
                style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
        }
    }
}

@Composable
private fun AchFeesStep() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "Costos y cobros", style = titleMediumBold())
            Text(
                text = "El banco puede aplicar sus propios costos operativos según la cuenta.",
                style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
            Text(
                text = "VentaGo habilita este canal cuando el módulo de pagos y cobros está activo.",
                style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = "Solo compatible con links de pago.",
                    style = bodyMediumBold(color = MaterialTheme.colorScheme.secondary),
                )
            }
        }
    }
}

@Composable
private fun AchConfigurationOnboarding(
    uiState: PaymentUiState,
    viewModel: PaymentMethodsViewModel,
) {
    val accountTypeOptions = listOf(
        AchAccountTypeOption(code = "savings", label = "Ahorros"),
        AchAccountTypeOption(code = "checking", label = "Corriente"),
    )

    val selectedBankIndex = AchBankOptions.indexOfFirst { it.code == uiState.achForm.bankCode }
        .takeIf { it >= 0 } ?: 0
    val selectedAccountTypeIndex = accountTypeOptions.indexOfFirst { it.code == uiState.achForm.accountType }
        .takeIf { it >= 0 } ?: 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Configuración ACH", style = bodyMediumBold())
            Text(
                "Completa la cuenta bancaria donde recibirás pagos ACH con comprobante.",
                style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )

            DMDropDownField(
                label = "Banco",
                items = AchBankOptions,
                selectedIndex = selectedBankIndex,
                onItemSelected = { _, option ->
                    viewModel.onAchBankCodeChange(option.code)
                    viewModel.onAchBankNameChange(option.name.takeIf { option.code.isNotBlank() }.orEmpty())
                },
                selectedItemToString = { it.name },
            )

            DMDropDownField(
                label = "Tipo de cuenta",
                items = accountTypeOptions,
                selectedIndex = selectedAccountTypeIndex,
                onItemSelected = { _, option -> viewModel.onAchAccountTypeChange(option.code) },
                selectedItemToString = { it.label },
            )

            DMOutlinedTextField(
                text = uiState.achForm.accountNumber,
                label = "Número de cuenta",
                modifier = Modifier.fillMaxWidth(),
                onChange = viewModel::onAchAccountNumberChange,
            )

            DMOutlinedTextField(
                text = uiState.achForm.accountHolderName,
                label = "Nombre de la cuenta",
                modifier = Modifier.fillMaxWidth(),
                onChange = viewModel::onAchAccountHolderChange,
            )
        }
    }
}

@Composable
private fun YappyProductInfoStep() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = getImageRequest(LocalPlatformContext.current, YappyPromoImageUrl),
                contentDescription = "Promo Yappy",
                placeholder = ColorPainter(Color(0xFFE5E7EB)),
                error = ColorPainter(Color(0xFFE5E7EB)),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(14.dp)),
            )
            Text(text = "Cómo funciona", style = titleMediumBold())
            Text(
                text = "Ofrécele a tus clientes la opción de pagar con Yappy en tu enlace de pago. Solo deben seleccionar \"Pagar con Yappy\", escribir su número de teléfono asociado y recibirán un pedir a su Yappy para completar el pago al instante. ¡Rápido, seguro y sin complicaciones! Los fondos van directo a tu cuenta de negocio. La conciliación es automática: detectamos el pago sin validación manual.",
                style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
        }
    }
}

@Composable
private fun YappyRequirementsStep(
    onOpenBusinessGuide: () -> Unit,
    onOpenEntrepreneurGuide: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "Requisitos", style = titleMediumBold())
            BulletItem("Cuenta de Yappy Comercial activa.")
            BulletItem("Merchant ID de Yappy Comercial.")
            BulletItem("Secret Key de Yappy Comercial.")

            Divider()
            Text(
                text = "Guías útiles",
                style = bodyMediumBold(),
            )

            TextButtonS(
                label = "Registrar su negocio",
                onClick = onOpenBusinessGuide,
                color = MaterialTheme.colorScheme.secondary,
            )
            TextButtonS(
                label = "Registrar su emprendimiento",
                onClick = onOpenEntrepreneurGuide,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun YappyCostsStep() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "Costos y cobros", style = titleMediumBold())
            BulletItem("Yappy Comercial puede aplicar su propia comisión.")
            BulletItem("VentaGo habilita este canal cuando el módulo de pagos y cobros está activo.")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFFBEB))
                    .border(
                        width = 1.dp,
                        color = Color(0xFFFCD34D),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = Color(0xFFB45309),
                )
                Text(
                    text = "Solo compatible con links de pago.",
                    style = bodyMediumBold(color = Color(0xFF92400E)),
                )
            }
        }
    }
}

@Composable
private fun YappyOnsiteHowItWorksStep() {
    var showFeesDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(text = "¿Cómo funciona Yappy en caja?", style = titleMediumBold())
            Text(
                text = "Cobra con un QR dinámico en el punto de venta y reduce validaciones manuales.",
                style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.24f),
                        shape = RoundedCornerShape(50),
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "Pago confirmado automáticamente",
                    style = labelSmall(color = MaterialTheme.colorScheme.secondary),
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                "Evita capturas falsas. Más ventas cerradas en caja.",
                style = bodyMediumBold(color = MaterialTheme.colorScheme.primary),
            )
            Text(
                "Genera un QR por venta, muéstralo al cliente y deja que Ventago detecte el pago automáticamente. El monto viaja exacto, el cobro se confirma sin revisión manual y la factura se genera al recibir la confirmación.",
                style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )

            YappyOnsiteFeatureRow(
                title = "QR dinámico",
                description = "Un código único por transacción.",
            )
            YappyOnsiteFeatureRow(
                title = "Pago detectado",
                description = "Evita depender de capturas o mensajes del cliente.",
            )
            YappyOnsiteFeatureRow(
                title = "Factura automática",
                description = "La factura se emite cuando el pago queda confirmado.",
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Módulo de pagos activo",
                        style = bodyMediumBold(color = MaterialTheme.colorScheme.secondary),
                    )
                    Text(
                        "Sin comisión VentaGo adicional por transacción.",
                        style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                }
                IconButton(onClick = { showFeesDialog = true }) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("?", style = bodyMediumBold(color = MaterialTheme.colorScheme.onSecondary))
                    }
                }
            }
        }
    }

    DMAlertDialog(
        title = "Costos y cobros",
        message = "Yappy Comercial puede aplicar su propia comisión.\n\nVentaGo habilita este canal cuando el módulo de pagos y cobros está activo.",
        show = showFeesDialog,
        onDismiss = { showFeesDialog = false },
        onConfirm = { showFeesDialog = false },
        confirmText = "Entendido",
        dismissText = "Cerrar",
    )
}

@Composable
private fun YappyOnsiteFeatureRow(
    title: String,
    description: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = bodyMediumBold())
            Text(
                description,
                style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
        }
    }
}

@Composable
private fun YappyOnsiteGroupStep(
    uiState: PaymentUiState,
    viewModel: PaymentMethodsViewModel,
) {
    LaunchedEffect(uiState.branches.size, uiState.yappyOnsiteGroupDrafts.isEmpty(), uiState.yappyOnsiteGroups.isEmpty()) {
        if (
            uiState.branches.isNotEmpty() &&
            uiState.yappyOnsiteGroupDrafts.isEmpty() &&
            uiState.yappyOnsiteGroups.isEmpty()
        ) {
            viewModel.prepareYappyOnsiteOnboardingDrafts()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Grupos de Yappy", style = bodyMediumBold())
            Text(
                "Crea los grupos en el dashboard comercial de Yappy, copia cada ID y selecciona la sucursal correspondiente. Usaremos el nombre de la sucursal como nombre del grupo.",
                style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButtonS(
                    label = "Abrir Yappy Comercial",
                    icon = Icons.Filled.Link,
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = { viewModel.onOpenExternalUrl(YappyOnsiteTutorialUrl) },
                )
                TextButtonS(
                    label = "Ver tutorial",
                    icon = Icons.Filled.Visibility,
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = { viewModel.onOpenExternalUrl(YappyOnsiteTutorialUrl) },
                )
            }

            if (uiState.yappyOnsiteGroups.isNotEmpty()) {
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                Text("Grupos registrados", style = bodyMediumBold())
                uiState.yappyOnsiteGroups.forEach { group ->
                    YappyOnsiteSavedGroupRow(
                        title = "${group.groupId} · ${group.name}",
                        subtitle = "Sucursal ${group.branchCode} · ${if (group.enabled) "Activo" else "Inactivo"}",
                        onDelete = { viewModel.requestDeleteYappyOnsiteGroup(group.groupId) },
                    )
                }
            }

            if (uiState.branches.isEmpty()) {
                InlineEmptyState("No hay sucursales disponibles para crear grupos.")
            } else {
                uiState.yappyOnsiteGroupDrafts.forEachIndexed { index, draft ->
                    YappyOnsiteGroupDraftCard(
                        index = index,
                        draft = draft,
                        uiState = uiState,
                        viewModel = viewModel,
                    )
                }
                val canAddMore = uiState.yappyOnsiteGroups.size + uiState.yappyOnsiteGroupDrafts.size < uiState.branches.size
                if (canAddMore) {
                    TextButtonS(
                        label = "Agregar grupo",
                        icon = Icons.Filled.Add,
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = viewModel::onAddYappyOnsiteGroupDraft,
                    )
                }
            }
        }
    }
}

@Composable
private fun YappyOnsiteSavedGroupRow(
    title: String,
    subtitle: String,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = bodyMediumBold())
        Text(
            subtitle,
            style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        TextButtonS(
            label = "Eliminar",
            icon = Icons.Filled.Delete,
            color = MaterialTheme.colorScheme.error,
            onClick = onDelete,
        )
    }
}

@Composable
private fun YappyOnsiteGroupDraftCard(
    index: Int,
    draft: com.teco.ventago.features.payments.ui.home.viewmodel.YappyOnsiteGroupDraftState,
    uiState: PaymentUiState,
    viewModel: PaymentMethodsViewModel,
) {
    val branches = uiState.branches
    val selectedBranchIndex = branches.indexOfFirst { it.branchCode == draft.branchCode }
        .takeIf { it >= 0 } ?: 0
    val branchName = branches.firstOrNull { it.branchCode == draft.branchCode }?.name.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Grupo ${index + 1}", style = bodyMediumBold())
                Text(
                    listOf(draft.groupId.ifBlank { "Sin ID" }, branchName.ifBlank { "Sucursal pendiente" }).joinToString(" · "),
                    style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButtonS(
                label = if (draft.collapsed) "Mostrar" else "Ocultar",
                color = MaterialTheme.colorScheme.secondary,
                onClick = { viewModel.onToggleYappyOnsiteGroupDraft(draft.localId) },
            )
            if (uiState.yappyOnsiteGroupDrafts.size > 1) {
                IconButton(onClick = { viewModel.onRemoveYappyOnsiteGroupDraft(draft.localId) }) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Eliminar grupo",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        if (!draft.collapsed) {
            DMOutlinedTextField(
                text = draft.groupId,
                label = "ID del grupo",
                modifier = Modifier.fillMaxWidth(),
                onChange = { viewModel.onYappyOnsiteGroupDraftIdChange(draft.localId, it) },
            )
            DMDropDownField(
                label = "Sucursal",
                items = branches,
                selectedIndex = selectedBranchIndex,
                onItemSelected = { _, branch ->
                    viewModel.onYappyOnsiteGroupDraftBranchChange(draft.localId, branch.branchCode)
                },
                selectedItemToString = { branchLabel(it.branchCode, branches) },
            )
            DMOutlinedTextField(
                text = draft.apiKey,
                label = "API key",
                modifier = Modifier.fillMaxWidth(),
                onChange = { viewModel.onYappyOnsiteGroupDraftApiKeyChange(draft.localId, it) },
            )
            DMOutlinedTextField(
                text = draft.secretKey,
                label = "Secret key",
                modifier = Modifier.fillMaxWidth(),
                onChange = { viewModel.onYappyOnsiteGroupDraftSecretKeyChange(draft.localId, it) },
            )
        }
    }
}

@Composable
private fun YappyOnsiteDeviceStep(
    uiState: PaymentUiState,
    viewModel: PaymentMethodsViewModel,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Agregar unidades de cobro", style = titleMediumBold())
            Text(
                "Para cada grupo puedes registrar varias unidades de cobro. Asocia cada unidad con el punto de facturación donde se usará la caja.",
                style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
            if (uiState.yappyOnsiteGroups.isEmpty()) {
                InlineEmptyState("Guarda al menos un grupo antes de registrar unidades de cobro.")
            } else {
                uiState.yappyOnsiteDeviceDrafts.forEachIndexed { index, draft ->
                    YappyOnsiteDeviceDraftCard(
                        index = index,
                        draft = draft,
                        uiState = uiState,
                        viewModel = viewModel,
                    )
                }
                val billingPointCount = uiState.branches.sumOf { it.fiscalBillingPoints.size }
                val canAddMore = uiState.yappyOnsiteDevices.size + uiState.yappyOnsiteDeviceDrafts.size < billingPointCount
                if (canAddMore) {
                    TextButtonS(
                        label = "Agregar unidad de cobro",
                        icon = Icons.Filled.Add,
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = viewModel::onAddYappyOnsiteDeviceDraft,
                    )
                }
            }
        }
    }
}

@Composable
private fun YappyOnsiteDeviceDraftCard(
    index: Int,
    draft: com.teco.ventago.features.payments.ui.home.viewmodel.YappyOnsiteDeviceDraftState,
    uiState: PaymentUiState,
    viewModel: PaymentMethodsViewModel,
) {
    val groups = uiState.yappyOnsiteGroups
    val selectedGroupIndex = groups.indexOfFirst { it.groupId == draft.groupId }
        .takeIf { it >= 0 } ?: 0
    val billingPoints = uiState.branches
        .firstOrNull { it.branchCode == draft.branchCode }
        ?.fiscalBillingPoints
        .orEmpty()
    val selectedBillingPointIndex = billingPoints.indexOfFirst { it.billingPoint == draft.billingPoint }
        .takeIf { it >= 0 } ?: 0
    val pointLabel = billingPoints.firstOrNull { it.billingPoint == draft.billingPoint }
        ?.let { billingPointLabel(draft.branchCode, it.billingPoint, uiState.branches) }
        ?: "Punto pendiente"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Unidad de cobro ${index + 1}", style = bodyMediumBold())
                Text(
                    listOf(draft.deviceId.ifBlank { "Sin Device ID" }, pointLabel).joinToString(" · "),
                    style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButtonS(
                label = if (draft.collapsed) "Mostrar" else "Ocultar",
                color = MaterialTheme.colorScheme.secondary,
                onClick = { viewModel.onToggleYappyOnsiteDeviceDraft(draft.localId) },
            )
            if (uiState.yappyOnsiteDeviceDrafts.size > 1) {
                IconButton(onClick = { viewModel.onRemoveYappyOnsiteDeviceDraft(draft.localId) }) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Eliminar unidad de cobro",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        if (!draft.collapsed) {
            DMDropDownField(
                label = "Grupo",
                items = groups,
                selectedIndex = selectedGroupIndex,
                onItemSelected = { _, group ->
                    viewModel.onYappyOnsiteDeviceDraftGroupChange(draft.localId, group.groupId)
                },
                    selectedItemToString = { it.name.ifBlank { it.groupId } },
            )
            if (billingPoints.isNotEmpty()) {
                DMDropDownField(
                    label = "Punto de facturación",
                    items = billingPoints,
                    selectedIndex = selectedBillingPointIndex,
                    onItemSelected = { _, point ->
                        viewModel.onYappyOnsiteDeviceDraftBillingPointChange(draft.localId, point.billingPoint)
                    },
                    selectedItemToString = { billingPointLabel(draft.branchCode, it.billingPoint, uiState.branches) },
                )
            }
            DMOutlinedTextField(
                text = draft.deviceId,
                label = "Device ID",
                modifier = Modifier.fillMaxWidth(),
                onChange = { viewModel.onYappyOnsiteDeviceDraftIdChange(draft.localId, it) },
            )
        }
    }
}

@Composable
private fun YappyOnsiteReadyStep() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2E7D32)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                )
            }
            Text(
                text = "Yappy en caja está listo",
                style = titleMediumBold(),
                textAlign = TextAlign.Center,
            )
            Text(
                text = "La configuración de grupos y unidades de cobro quedó guardada. Ya puedes generar QR dinámicos en caja y confirmar pagos automáticamente desde VentaGo.",
                style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun YappyOnsiteSummaryStep(
    uiState: PaymentUiState,
    viewModel: PaymentMethodsViewModel,
) {
    val onsite = uiState.paymentSummary?.paymentMethods?.yappy?.onsite
    val canAddGroup = uiState.yappyOnsiteGroups.size < uiState.branches.size
    val canAddDevice = uiState.yappyOnsiteDevices.size < uiState.branches.sumOf { it.fiscalBillingPoints.size }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Configuración Yappy en caja", style = bodyMediumBold())
            if (uiState.yappyOnsiteLoading) {
                InlineShimmerList()
            } else {
                StatusRow(
                    label = "Canal habilitado",
                    active = onsite?.enabled == true && onsite.configured,
                )
                Text(
                    "Grupos: ${onsite?.groupsCount ?: uiState.yappyOnsiteGroups.size} · Unidades de cobro: ${onsite?.devicesCount ?: uiState.yappyOnsiteDevices.size} · Sesiones abiertas: ${onsite?.openSessionsCount ?: 0}",
                    style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (canAddGroup) {
                        TextButtonS(
                            label = "Agregar grupo",
                            icon = Icons.Filled.Edit,
                            color = MaterialTheme.colorScheme.secondary,
                            onClick = viewModel::onAddYappyOnsiteGroup,
                        )
                    }
                    if (canAddDevice) {
                        TextButtonS(
                            label = "Agregar unidad de cobro",
                            icon = Icons.Filled.CreditCard,
                            color = MaterialTheme.colorScheme.secondary,
                            onClick = viewModel::onAddYappyOnsiteDevice,
                        )
                    }
                }
                Divider()
                if (uiState.yappyOnsiteGroups.isEmpty()) {
                    InlineEmptyState("Aún no hay grupos configurados.")
                } else {
                    uiState.yappyOnsiteGroups.forEach { group ->
                        YappyOnsiteGroupConfiguredCard(
                            group = group,
                            devices = uiState.yappyOnsiteDevices.filter {
                                it.groupId.equals(group.groupId, ignoreCase = true)
                            },
                            branches = uiState.branches,
                            viewModel = viewModel,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun YappyOnsiteGroupConfiguredCard(
    group: YappyOnsiteGroup,
    devices: List<YappyOnsiteDevice>,
    branches: List<Branch>,
    viewModel: PaymentMethodsViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(group.name.ifBlank { group.groupId }, style = bodyMediumBold())
                Text(
                    "ID ${group.groupId} · ${branchLabel(group.branchCode, branches)} · ${if (group.enabled) "Activo" else "Inactivo"}",
                    style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButtonS(
                label = "Editar",
                icon = Icons.Filled.Edit,
                color = MaterialTheme.colorScheme.secondary,
                onClick = { viewModel.onEditYappyOnsiteGroup(group.groupId) },
            )
            TextButtonS(
                label = "Eliminar",
                icon = Icons.Filled.Delete,
                color = MaterialTheme.colorScheme.error,
                onClick = { viewModel.requestDeleteYappyOnsiteGroup(group.groupId) },
            )
        }

        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        Text("Unidades de cobro", style = bodyMediumBold())
        if (devices.isEmpty()) {
            InlineEmptyState("Aún no hay unidades de cobro registradas para este grupo.")
        } else {
            devices.forEach { device ->
                YappyOnsiteDeviceConfiguredRow(
                    device = device,
                    branches = branches,
                    viewModel = viewModel,
                )
            }
        }
    }
}

@Composable
private fun YappyOnsiteDeviceConfiguredRow(
    device: YappyOnsiteDevice,
    branches: List<Branch>,
    viewModel: PaymentMethodsViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.background)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp),
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(device.name.ifBlank { "Unidad de cobro ${device.deviceId}" }, style = bodyMediumBold())
        Text(
            "${billingPointLabel(device.branchCode, device.billingPoint, branches)} · Device ID ${device.deviceId} · ${if (device.hasOpenSession) "Sesión abierta" else if (device.enabled) "Activa" else "Inactiva"}",
            style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButtonS(
                label = "Editar",
                icon = Icons.Filled.Edit,
                color = MaterialTheme.colorScheme.secondary,
                onClick = { viewModel.onEditYappyOnsiteDevice(device) },
            )
            TextButtonS(
                label = "Eliminar",
                icon = Icons.Filled.Delete,
                color = MaterialTheme.colorScheme.error,
                onClick = { viewModel.requestDeleteYappyOnsiteDevice(device) },
            )
        }
    }
}

@Composable
private fun YappyOnsiteGroupEditSheet(
    uiState: PaymentUiState,
    viewModel: PaymentMethodsViewModel,
) {
    val form = uiState.yappyOnsiteGroupForm
    val branches = uiState.branches
    val usedBranchCodes = uiState.yappyOnsiteGroups
        .filterNot { it.groupId.equals(uiState.editingYappyOnsiteGroupId.orEmpty(), ignoreCase = true) }
        .map { it.branchCode }
        .toSet()
    val availableBranches = branches.filter {
        it.branchCode == form.branchCode || it.branchCode !in usedBranchCodes
    }
    val selectedBranchIndex = availableBranches.indexOfFirst { it.branchCode == form.branchCode }
        .takeIf { it >= 0 } ?: 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (uiState.editingYappyOnsiteGroupId != null) "Editar grupo" else "Agregar grupo",
            style = titleMediumBold(),
        )
        DMOutlinedTextField(
            text = form.groupId,
            label = "ID del grupo",
            modifier = Modifier.fillMaxWidth(),
            onChange = viewModel::onYappyOnsiteGroupIdChange,
        )
        if (availableBranches.isNotEmpty()) {
            DMDropDownField(
                label = "Sucursal",
                items = availableBranches,
                selectedIndex = selectedBranchIndex,
                onItemSelected = { _, branch ->
                    viewModel.onYappyOnsiteGroupBranchChange(branch.branchCode)
                },
                selectedItemToString = { branchLabel(it.branchCode, branches) },
            )
        }
        DMOutlinedTextField(
            text = form.apiKey,
            label = "API key",
            modifier = Modifier.fillMaxWidth(),
            onChange = viewModel::onYappyOnsiteGroupApiKeyChange,
        )
        DMOutlinedTextField(
            text = form.secretKey,
            label = "Secret key",
            modifier = Modifier.fillMaxWidth(),
            onChange = viewModel::onYappyOnsiteGroupSecretKeyChange,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButtonM(
                modifier = Modifier.weight(1f),
                onClick = viewModel::onCancelYappyOnsiteGroupEdit,
            ) {
                Text("Cancelar")
            }
            ButtonM(
                modifier = Modifier.weight(1f),
                onClick = viewModel::onSaveYappyOnsiteGroup,
            ) {
                Text("Guardar")
            }
        }
    }
}

@Composable
private fun YappyOnsiteDeviceEditSheet(
    uiState: PaymentUiState,
    viewModel: PaymentMethodsViewModel,
) {
    val form = uiState.yappyOnsiteDeviceForm
    val isEditing = uiState.editingYappyOnsiteDeviceId != null
    val groups = uiState.yappyOnsiteGroups
    val selectedGroupIndex = groups.indexOfFirst { it.groupId.equals(form.groupId, ignoreCase = true) }
        .takeIf { it >= 0 } ?: 0
    val group = uiState.yappyOnsiteGroups.firstOrNull {
        it.groupId.equals(form.groupId, ignoreCase = true)
    }
    val branchCode = form.branchCode.ifBlank { group?.branchCode.orEmpty() }
    val currentDeviceId = uiState.editingYappyOnsiteDeviceId.orEmpty()
    val usedBillingPoints = uiState.yappyOnsiteDevices
        .filter {
            it.groupId.equals(form.groupId, ignoreCase = true) &&
                !it.deviceId.equals(currentDeviceId, ignoreCase = true)
        }
        .map { it.billingPoint }
        .toSet()
    val billingPoints = uiState.branches
        .firstOrNull { it.branchCode == branchCode }
        ?.fiscalBillingPoints
        ?.filter {
            it.billingPoint == form.billingPoint || it.billingPoint !in usedBillingPoints
        }
        .orEmpty()
    val selectedBillingPointIndex = billingPoints.indexOfFirst { it.billingPoint == form.billingPoint }
        .takeIf { it >= 0 } ?: 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(if (isEditing) "Editar unidad de cobro" else "Agregar unidad de cobro", style = titleMediumBold())
        if (!isEditing && groups.isNotEmpty()) {
            DMDropDownField(
                label = "Grupo",
                items = groups,
                selectedIndex = selectedGroupIndex,
                onItemSelected = { _, selectedGroup ->
                    viewModel.onYappyOnsiteDeviceGroupChange(selectedGroup.groupId)
                },
                selectedItemToString = { it.name.ifBlank { it.groupId } },
            )
        } else {
            group?.let {
                Text(
                    "Grupo ${it.groupId} · ${branchLabel(it.branchCode, uiState.branches)}",
                    style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }
        }
        if (!isEditing && groups.isEmpty()) {
            Text(
                "Guarda al menos un grupo antes de registrar unidades de cobro.",
                style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
        }
        DMOutlinedTextField(
            text = form.deviceId,
            label = "Device ID",
            modifier = Modifier.fillMaxWidth(),
            onChange = viewModel::onYappyOnsiteDeviceIdChange,
        )
        if (billingPoints.isNotEmpty()) {
            DMDropDownField(
                label = "Punto de facturación",
                items = billingPoints,
                selectedIndex = selectedBillingPointIndex,
                onItemSelected = { _, point ->
                    viewModel.onYappyOnsiteDeviceBillingPointChange(point.billingPoint)
                },
                selectedItemToString = {
                    billingPointLabel(branchCode, it.billingPoint, uiState.branches)
                },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButtonM(
                modifier = Modifier.weight(1f),
                onClick = viewModel::onCancelYappyOnsiteDeviceEdit,
            ) {
                Text("Cancelar")
            }
            ButtonM(
                modifier = Modifier.weight(1f),
                onClick = viewModel::onSaveYappyOnsiteDevice,
            ) {
                Text("Guardar")
            }
        }
    }
}

private fun branchLabel(branchCode: String, branches: List<Branch>): String {
    return branches.firstOrNull { it.branchCode == branchCode }
        ?.name
        ?.trim()
        .orEmpty()
        .ifBlank { "Sucursal" }
}

private fun billingPointLabel(
    branchCode: String,
    billingPoint: String,
    branches: List<Branch>,
): String {
    val branchName = branchLabel(branchCode, branches)
    val point = branches
        .firstOrNull { it.branchCode == branchCode }
        ?.fiscalBillingPoints
        ?.firstOrNull { it.billingPoint == billingPoint }
    val pointName = point?.description?.trim().orEmpty().ifBlank { "Punto" }
    return "$branchName - $pointName"
}

@Composable
private fun InlineEmptyState(message: String) {
    Text(
        text = message,
        modifier = Modifier.fillMaxWidth(),
        style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun BulletItem(text: String) {
    BulletItem(AnnotatedString(text))
}

@Composable
private fun BulletItem(text: AnnotatedString) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Text(
            text = text,
            style = bodyMedium(color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun YappyConfiguration(
    uiState: PaymentUiState,
    viewModel: PaymentMethodsViewModel,
    showTutorialLink: Boolean = false,
    showUnlink: Boolean = true,
) {
    val configured = viewModel.methodConfigured(PaymentMethodType.Yappy)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Configura Yappy", style = bodyMediumBold())
            Text(
                "Ingresa las credenciales de tu comercio para habilitar cobros por Yappy.",
                style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            DMOutlinedTextField(
                text = uiState.yappyMerchantId,
                label = "Merchant ID",
                modifier = Modifier.fillMaxWidth(),
                onChange = viewModel::onYappyMerchantIdChange,
            )

            DMOutlinedTextField(
                text = uiState.yappySecretKey,
                label = "Clave secreta",
                modifier = Modifier.fillMaxWidth(),
                onChange = viewModel::onYappySecretKeyChange,
            )

            if (showTutorialLink) {
                TextButtonS(
                    label = "Ver tutorial para obtener tus credenciales",
                    icon = Icons.Filled.Visibility,
                    onClick = { viewModel.onOpenExternalUrl(YappyCredentialsTutorialUrl) },
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (configured && showUnlink) {
                StatusPill(text = "Configurado", positive = true)
                OutlinedButtonM(onClick = { viewModel.requestUnlinkMethod(PaymentMethodType.Yappy) }) {
                    Text("Desvincular Yappy")
                }
            }
        }
    }
}

@Composable
private fun PaypalConfiguration(
    uiState: PaymentUiState,
    viewModel: PaymentMethodsViewModel,
    isOnboardingFlow: Boolean,
) {
    val summary = uiState.paymentSummary
    val linkedAccount = summary?.paymentMethods?.paypal?.linkedAccount == true
    val ready = summary?.paymentMethods?.paypal?.readyForPayments() == true

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Configuración de PayPal", style = bodyMediumBold())

            PaypalStatusRow(
                label = "Cuenta PayPal conectada",
                active = ready || linkedAccount,
                activeText = if (ready) "Lista" else "Conectada",
            )

            Text(
                text = "Se abrirá PayPal para completar la conexión segura de la cuenta.",
                style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )

            if (!isOnboardingFlow) {
                ButtonM(
                    onClick = viewModel::onConnectPaypal,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ) {
                    Text("Conectar cuenta PayPal")
                }
            }

            if (!isOnboardingFlow && linkedAccount) {
                OutlinedButtonM(onClick = { viewModel.requestUnlinkMethod(PaymentMethodType.Paypal) }) {
                    Text("Desvincular PayPal")
                }
            }

            if (!isOnboardingFlow) {
                Text(
                    text = "Cuando PayPal confirme la conexión, el canal quedará disponible para links de pago.",
                    style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }
        }
    }
}

@Composable
private fun AchConfiguration(
    uiState: PaymentUiState,
    viewModel: PaymentMethodsViewModel,
) {
    val configured = viewModel.methodConfigured(PaymentMethodType.Ach)
    val accountTypeOptions = listOf(
        AchAccountTypeOption(code = "savings", label = "Ahorros"),
        AchAccountTypeOption(code = "checking", label = "Corriente"),
    )
    val selectedBankIndex = AchBankOptions.indexOfFirst { it.code == uiState.achForm.bankCode }
        .takeIf { it >= 0 } ?: 0
    val selectedAccountTypeIndex = accountTypeOptions.indexOfFirst { it.code == uiState.achForm.accountType }
        .takeIf { it >= 0 } ?: 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Configuración ACH con comprobante", style = bodyMediumBold())
            Text(
                "Este canal reemplaza la transferencia manual anterior. Tus clientes cargan comprobante para revisión.",
                style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            DMDropDownField(
                label = "Banco",
                items = AchBankOptions,
                selectedIndex = selectedBankIndex,
                onItemSelected = { _, option ->
                    viewModel.onAchBankCodeChange(option.code)
                    viewModel.onAchBankNameChange(option.name.takeIf { option.code.isNotBlank() }.orEmpty())
                },
                selectedItemToString = { it.name },
            )

            DMDropDownField(
                label = "Tipo de cuenta",
                items = accountTypeOptions,
                selectedIndex = selectedAccountTypeIndex,
                onItemSelected = { _, option -> viewModel.onAchAccountTypeChange(option.code) },
                selectedItemToString = { it.label },
            )

            if (configured && uiState.achAccountNumberMasked.isNotBlank()) {
                Text(
                    text = "Número de cuenta registrado: ${uiState.achAccountNumberMasked}",
                    style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }

            DMOutlinedTextField(
                text = uiState.achForm.accountNumber,
                label = if (configured) "Número de cuenta (opcional si no cambia)" else "Número de cuenta",
                modifier = Modifier.fillMaxWidth(),
                onChange = viewModel::onAchAccountNumberChange,
            )
            DMOutlinedTextField(
                text = uiState.achForm.accountHolderName,
                label = "Titular de la cuenta",
                modifier = Modifier.fillMaxWidth(),
                onChange = viewModel::onAchAccountHolderChange,
            )

            ButtonM(onClick = viewModel::onSaveAch) {
                Text(if (configured) "Actualizar configuración ACH" else "Guardar configuración ACH")
            }

            if (configured) {
                StatusPill(text = "Configurado", positive = true)
                OutlinedButtonM(
                    onClick = viewModel::requestDisableAch,
                    contentColor = MaterialTheme.colorScheme.error,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                ) {
                    Text("Desactivar ACH")
                }
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, active: Boolean) {
    val iconTint = if (active) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant
    val iconBackground = if (active) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (active) Icons.Filled.Check else Icons.Filled.Info,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = label,
            style = bodyMedium(),
            modifier = Modifier.weight(1f),
        )
        StatusPill(text = if (active) "Completado" else "Pendiente", positive = active)
    }
}

@Composable
private fun PaypalStatusRow(
    label: String,
    active: Boolean,
    activeText: String,
) {
    val iconTint = if (active) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant
    val iconBackground = if (active) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (active) Icons.Filled.Check else Icons.Filled.Info,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = label,
            style = bodyMedium(),
            modifier = Modifier.weight(1f),
        )
        StatusPill(
            text = if (active) activeText else "Pendiente",
            positive = active,
        )
    }
}

@Composable
private fun MethodSuccessStep(onDone: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDCFCE7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color(0xFF16A34A)
                )
            }
            Text("Configuración completada", style = titleMediumBold())
            Text(
                "El método de pago quedó listo para recibir cobros.",
                textAlign = TextAlign.Center,
                style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            ButtonM(onClick = onDone) {
                Text("Volver a métodos de pago")
            }
        }
    }
}

@Composable
private fun YappyOnboardingFooter(
    step: Int,
    onBack: () -> Unit,
    onPrimary: () -> Unit,
) {
    val primaryLabel = if (step < 4) "Continuar" else "Conectar Yappy"
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButtonM(onClick = onBack) {
            Text("Atrás")
        }
        ButtonM(
            onClick = onPrimary,
            containerColor = MaterialTheme.colorScheme.secondary,
            ) {
            Text(primaryLabel)
        }
        OnboardingTermsIfFirstStep(step)
    }
}

@Composable
private fun YappyOnsiteOnboardingFooter(
    step: Int,
    editingGroup: Boolean,
    editingDevice: Boolean,
    onBack: () -> Unit,
    onPrimary: () -> Unit,
) {
    val primaryLabel = when (step) {
        2 -> if (editingGroup) "Guardar cambios" else "Continuar"
        3 -> if (editingDevice) "Guardar cambios" else "Guardar unidades de cobro"
        4 -> "Finalizar"
        else -> "Continuar"
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButtonM(onClick = onBack) {
            Text("Atrás")
        }
        ButtonM(
            onClick = onPrimary,
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
        ) {
            Text(primaryLabel)
        }
        OnboardingTermsIfFirstStep(step)
    }
}

@Composable
private fun AchOnboardingFooter(
    step: Int,
    onBack: () -> Unit,
    onPrimary: () -> Unit,
) {
    val primaryLabel = if (step < 4) "Continuar" else "Guardar configuración ACH"
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButtonM(
            onClick = onBack,
            contentColor = MaterialTheme.colorScheme.secondary,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
        ) {
            Text("Atrás")
        }
        ButtonM(
            onClick = onPrimary,
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
        ) {
            Text(primaryLabel)
        }
        OnboardingTermsIfFirstStep(step)
    }
}

@Composable
private fun PaypalOnboardingFooter(
    step: Int,
    onBack: () -> Unit,
    onPrimary: () -> Unit,
) {
    val primaryLabel = if (step < 4) "Continuar" else "Conectar PayPal"
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButtonM(
            onClick = onBack,
            contentColor = MaterialTheme.colorScheme.secondary,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
        ) {
            Text("Atrás")
        }
        ButtonM(
            onClick = onPrimary,
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
        ) {
            Text(primaryLabel)
        }
        OnboardingTermsIfFirstStep(step)
    }
}

@Composable
private fun TiloPayOnboardingFooter(
    step: Int,
    onBack: () -> Unit,
    onPrimary: () -> Unit,
) {
    val primaryLabel = if (step < 4) "Continuar" else "Vincular cuenta TiloPay"
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButtonM(
            onClick = onBack,
            contentColor = MaterialTheme.colorScheme.secondary,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
        ) {
            Text("Atrás")
        }
        ButtonM(
            onClick = onPrimary,
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
        ) {
            Text(primaryLabel)
        }
        OnboardingTermsIfFirstStep(step)
    }
}

@Composable
private fun OnboardingFooter(
    step: Int,
    method: PaymentMethodType,
    onBack: () -> Unit,
    onPrimary: () -> Unit,
) {
    val primaryLabel = when {
        step == 1 -> "Continuar"
        method == PaymentMethodType.Yappy -> "Conectar Yappy"
        method == PaymentMethodType.YappyOnsite -> "Continuar"
        method == PaymentMethodType.Ach -> "Guardar configuración ACH"
        method == PaymentMethodType.Paypal -> "Continuar"
        method == PaymentMethodType.CardTilopay -> "Vincular cuenta TiloPay"
        else -> "Continuar"
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButtonM(onClick = onBack) {
            Text("Atrás")
        }
        ButtonM(onClick = onPrimary) {
            Text(primaryLabel)
        }
        OnboardingTermsIfFirstStep(step)
    }
}

@Composable
private fun OnboardingTermsIfFirstStep(step: Int) {
    if (step == 1) {
        PaymentTermsText(
            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
            linkColor = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun PaymentMethodsShimmerScreen() {
    val brush = shimmerBrush()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .height(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(brush)
        )

        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(brush)
            )
        }
    }
}

@Composable
private fun InlineShimmerList() {
    val brush = shimmerBrush()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(brush)
            )
        }
    }
}

@Composable
private fun EmptyListState(message: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = message,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
            textAlign = TextAlign.Center,
        )
    }
}

private fun formatApiDate(raw: String?): String {
    val value = raw.orEmpty().trim()
    if (value.isBlank()) return "-"

    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd",
    )

    patterns.forEach { pattern ->
        val formatted = runCatching {
            DateFormat.getFormattedDate(value, pattern, "dd MMM yyyy")
        }.getOrNull()
        if (!formatted.isNullOrBlank() && formatted != value) {
            return formatted
        }
    }

    return value
}

private fun formatBatchPeriodLabel(periodStart: String?, periodEnd: String?): String {
    val value = periodStart.orEmpty().ifBlank { periodEnd.orEmpty() }.trim()
    if (value.isBlank()) return "-"

    val datePart = value.take(10)
    val parts = datePart.split("-")
    if (parts.size < 2) return value

    val year = parts[0].toIntOrNull() ?: return value
    val month = parts[1].toIntOrNull() ?: return value
    val monthName = listOf(
        "Enero",
        "Febrero",
        "Marzo",
        "Abril",
        "Mayo",
        "Junio",
        "Julio",
        "Agosto",
        "Septiembre",
        "Octubre",
        "Noviembre",
        "Diciembre",
    ).getOrNull(month - 1) ?: return value

    return "$monthName $year"
}

private fun feePaymentMethodLabel(method: String): String {
    return when (method.trim().uppercase()) {
        "YAPPY", "YAPPY_ONSITE" -> "Yappy"
        "ACH" -> "ACH"
        "TILOPAY_CARD", "CARD", "CARD_TILOPAY" -> "Tarjeta"
        else -> method.ifBlank { "-" }
    }
}
