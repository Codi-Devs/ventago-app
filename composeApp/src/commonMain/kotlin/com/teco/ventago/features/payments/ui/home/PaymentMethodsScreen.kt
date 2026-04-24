package com.teco.ventago.features.payments.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
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
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.headlineMediumBold
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.design_system.theme.vanishedBackgroundColor
import com.teco.ventago.features.payments.ui.home.viewmodel.FeeBatchStatusUi
import com.teco.ventago.features.payments.ui.home.viewmodel.FeeTransactionStatusUi
import com.teco.ventago.features.payments.ui.home.viewmodel.PaymentMethodType
import com.teco.ventago.features.payments.ui.home.viewmodel.PaymentMethodsViewModel
import com.teco.ventago.features.payments.ui.home.viewmodel.PaymentScreenMode
import com.teco.ventago.features.payments.ui.home.viewmodel.PaymentUiEvent
import com.teco.ventago.features.payments.ui.home.viewmodel.PaymentUiState
import com.teco.ventago.features.payments.ui.home.viewmodel.PaymentViewMode
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
private const val AchPromoImageUrl = "https://paas.b-cdn.net/assets/ach-promo.png"
private const val PaypalPromoImageUrl = "https://paas.b-cdn.net/assets/ppcp-solutions-hero.png"

private data class AchBankOption(
    val code: String,
    val name: String,
)

private data class AchAccountTypeOption(
    val code: String,
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
    onNavigateSettingsRoot: () -> Unit,
    onNavigateBusinessAddress: () -> Unit,
    isMethodRoute: Boolean = false,
    methodRoute: PaymentMethodType? = null,
    onExitMethodRoute: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(confirmValueChange = { false })
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
                    PaymentScreenMode.GlobalOnboarding -> GlobalOnboardingSection(
                        uiState = uiState,
                        onStart = viewModel::onStartOnboarding,
                    )
                    PaymentScreenMode.ConfiguredList -> ConfiguredPaymentsSection(
                        uiState = uiState,
                        viewModel = viewModel,
                        onOpenMethod = onNavigateMethod,
                    )
                    PaymentScreenMode.MethodDetailOnboarding,
                    PaymentScreenMode.MethodDetailConfigured -> ConfiguredPaymentsSection(
                        uiState = uiState,
                        viewModel = viewModel,
                        onOpenMethod = onNavigateMethod,
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
            PaymentMethodType.Ach -> "ACH"
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
                    text = "No tienes permisos o beta habilitada para esta sección.",
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
                                onStart = onStart,
                                fullWidthButton = false,
                            )
                        }
                    } else {
                        HeroCopyBlock(
                            modifier = Modifier.fillMaxWidth(),
                            onStart = onStart,
                            fullWidthButton = true,
                        )
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "¿Qué vas a configurar?",
                    style = bodyMediumBold(),
                )
                Text(
                    text = "Activa Yappy, ACH con comprobante y PayPal, y controla tus comisiones desde un solo lugar.",
                    style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
                )
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
            text = "Recibe pagos como en web",
            style = headlineMediumBold(color = Color.White),
        )
        Text(
            text = "Configura métodos de pago y administra comisiones con una experiencia optimizada para móvil.",
            style = bodyMedium(color = Color.White.copy(alpha = 0.92f)),
        )

        BenefitRow("Cobros por enlace, Yappy, ACH y PayPal")
        BenefitRow("Comisiones y ciclos de cobro en tiempo real")
        BenefitRow("Facturación automática al recibir pagos")

        Spacer(modifier = Modifier.height(8.dp))
        ButtonM(
            modifier = if (fullWidthButton) Modifier.fillMaxWidth() else Modifier.widthIn(max = 320.dp),
            containerColor = Color.White,
            contentColor = Color(0xFF0A2E66),
            onClick = onStart,
        ) {
            Text(text = "Comenzar configuración", style = bodyMediumBold())
        }
    }
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
            text = "Administra tus canales y comisiones para cobros en línea.",
            style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )

        GeneralSummaryCard(uiState = uiState, viewModel = viewModel)
        ChannelsCard(uiState = uiState, viewModel = viewModel, onOpenMethod = onOpenMethod)
        CommissionsSection(uiState = uiState, viewModel = viewModel)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GeneralSummaryCard(
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

            if (viewModel.hasConfiguredFees()) {
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

                Divider()
            }

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
                Switch(
                    checked = uiState.autoInvoiceEnabled,
                    onCheckedChange = viewModel::onToggleAutoInvoice,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.secondary,
                        checkedTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
                    )
                )
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Canales disponibles", style = bodyMediumBold())

            val rows = remember(uiState.availablePaymentMethods) {
                listOf(
                    PaymentMethodType.Yappy,
                    PaymentMethodType.Ach,
                    PaymentMethodType.Paypal,
                )
            }

            rows.forEachIndexed { index, method ->
                if (viewModel.methodVisible(method)) {
                    ChannelRow(
                        method = method,
                        configured = viewModel.methodConfigured(method),
                        onClick = { onOpenMethod(method) },
                        subtitle = when (method) {
                            PaymentMethodType.Paypal -> uiState.availablePaymentMethods["paypal"]?.label
                            PaymentMethodType.Ach -> uiState.availablePaymentMethods["ach"]?.label
                            PaymentMethodType.Yappy -> null
                        }
                    )
                    if (index < rows.lastIndex) {
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelRow(
    method: PaymentMethodType,
    configured: Boolean,
    subtitle: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ChannelLogo(method)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when (method) {
                    PaymentMethodType.Yappy -> "Yappy"
                    PaymentMethodType.Ach -> "ACH con comprobante"
                    PaymentMethodType.Paypal -> "PayPal"
                },
                style = bodyMediumBold(),
            )
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (configured) {
            StatusPill(text = "Configurado", positive = true)
            Spacer(modifier = Modifier.width(6.dp))
            FeePill(method = method)
        }

        Icon(
            imageVector = Icons.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ChannelLogo(method: PaymentMethodType) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center
    ) {
        when (method) {
            PaymentMethodType.Yappy -> Icon(
                painter = painterResource(Res.drawable.yappy_logo),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp)
            )
            PaymentMethodType.Ach -> Icon(
                painter = painterResource(Res.drawable.ic_bank),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            PaymentMethodType.Paypal -> Icon(
                painter = painterResource(Res.drawable.ic_paypal_logo),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(26.dp)
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
private fun FeePill(method: PaymentMethodType) {
    val value = when (method) {
        PaymentMethodType.Yappy -> "1%"
        PaymentMethodType.Ach -> "$0.27"
        PaymentMethodType.Paypal -> "1%"
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(text = value, style = labelSmall(color = MaterialTheme.colorScheme.onSecondaryContainer))
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
private fun TabPill(selected: Boolean, label: String, onClick: () -> Unit) {
    val background = if (selected) MaterialTheme.colorScheme.secondary else Color.Transparent
    val textColor = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.secondary

    TextButtonS(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        label = label,
        color = textColor,
        onClick = onClick,
    )
}

@Composable
private fun TransactionsTab(
    uiState: PaymentUiState,
    viewModel: PaymentMethodsViewModel,
) {
    val statusItems = FeeTransactionStatusUi.entries
    val methodItems = listOf("Todos", "ACH", "PAYPAL", "YAPPY")

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
        selectedIndex = methodItems.indexOfFirst {
            val selected = uiState.filters.transactionsMethod ?: "Todos"
            it == selected
        }.takeIf { it >= 0 } ?: 0,
        onItemSelected = { _, item ->
            viewModel.onTransactionsMethodFilterChange(item.takeIf { it != "Todos" })
        },
        selectedItemToString = { it },
    )

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
                            text = "Método: ${item.paymentMethod.ifBlank { "-" }}",
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
    val statusItems = FeeBatchStatusUi.entries

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
                            Text("${item.periodStart} - ${item.periodEnd}", style = bodyMediumBold())
                            StatusPill(
                                text = FeeBatchStatusUi.fromApi(item.status).label,
                                positive = item.status.equals("paid", ignoreCase = true),
                            )
                        }
                        Text(
                            text = "Líneas: ${item.totalLines}",
                            style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = "Total: ${formatNumberToMoney(viewModel.formatCents(item.totalFeeTotal))}",
                            style = bodyMediumBold(color = MaterialTheme.colorScheme.primary),
                        )
                        Text(
                            text = "Vence: ${formatApiDate(item.dueAt)}",
                            style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        )
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
    val isAchOnboarding = isOnboardingFlow && activeMethod == PaymentMethodType.Ach
    val isPaypalOnboarding = isOnboardingFlow && activeMethod == PaymentMethodType.Paypal
    val yappyGuideStep = uiState.activeStep in 1..4
    val achGuideStep = uiState.activeStep in 1..3
    val paypalGuideStep = uiState.activeStep in 1..4
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isOnboardingFlow) {
            val shouldShowStepper = when {
                isYappyOnboarding -> uiState.activeStep <= 4
                isAchOnboarding -> uiState.activeStep <= 3
                isPaypalOnboarding -> uiState.activeStep <= 4
                else -> uiState.activeStep <= 2
            }
            if (shouldShowStepper) {
                StepperPills(
                    step = uiState.activeStep,
                    totalSteps = when {
                        isYappyOnboarding -> 4
                        isAchOnboarding -> 3
                        isPaypalOnboarding -> 4
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
            isAchOnboarding && achGuideStep -> AchOnboardingStep(
                step = uiState.activeStep,
                uiState = uiState,
                viewModel = viewModel,
            )
            isAchOnboarding && uiState.activeStep == 4 -> MethodSuccessStep(onDone = onExitToMethods)
            isPaypalOnboarding && paypalGuideStep -> PaypalOnboardingStep(
                step = uiState.activeStep,
                uiState = uiState,
                viewModel = viewModel,
            )
            isPaypalOnboarding && uiState.activeStep == 5 -> MethodSuccessStep(onDone = onExitToMethods)
            isOnboardingFlow && uiState.activeStep == 1 -> MethodIntroCarousel(activeMethod)
            isOnboardingFlow && uiState.activeStep == 3 -> MethodSuccessStep(onDone = onExitToMethods)
            else -> MethodConfigurationStep(uiState = uiState, viewModel = viewModel, method = activeMethod, isOnboardingFlow = isOnboardingFlow)
        }

        if (isYappyOnboarding && yappyGuideStep) {
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
                    if (uiState.activeStep < 3) {
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
                onPrimary = viewModel::onNextStep,
            )
        } else if (isOnboardingFlow && uiState.activeStep <= 2) {
            OnboardingFooter(step = uiState.activeStep, method = activeMethod, onBack = {
                if (uiState.activeStep <= 1) onExitToMethods() else viewModel.onPrevStep()
            }, onPrimary = {
                when {
                    uiState.activeStep == 1 -> viewModel.onNextStep()
                    activeMethod == PaymentMethodType.Ach -> viewModel.onSaveAch()
                    activeMethod == PaymentMethodType.Paypal -> viewModel.onNextStep()
                    else -> viewModel.onNextStep()
                }
            })
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
            "Autoriza el acuerdo de facturación de comisiones.",
            "Usa el estado conectado para completar el onboarding."
        )
        PaymentMethodType.Yappy -> listOf(
            "Configura Yappy con tus credenciales comerciales.",
            "Conecta tu cuenta y habilita cobros instantáneos.",
            "Gestiona tus cobros desde el módulo de métodos de pago."
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
        PaymentMethodType.Ach -> AchConfiguration(uiState, viewModel)
        PaymentMethodType.Paypal -> PaypalConfiguration(uiState, viewModel, isOnboardingFlow)
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
private fun AchOnboardingStep(
    step: Int,
    uiState: PaymentUiState,
    viewModel: PaymentMethodsViewModel,
) {
    when (step) {
        1 -> AchHowItWorksStep()
        2 -> AchFeesStep()
        3 -> AchConfigurationOnboarding(
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
            BulletItem("Para habilitarlo se necesitan 2 pasos: conectar cuenta y autorizar cobro automático.")
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
            BulletItem("Autorizar el cobro automático de comisiones de plataforma.")
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
            BulletItem("Comisión Ventago: 1% + 7% ITBMS por transacción exitosa.")
            BulletItem("El cobro se gestiona automáticamente con la autorización de PayPal.")

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
                text = "Permite que tus clientes paguen por transferencia ACH y suban su comprobante en el mismo link de pago. El sistema centraliza la evidencia y aplica validaciones automáticas para ayudarte a revisar cada pago con más rapidez.\n\nTu negocio recibe notificaciones, decide si acepta o rechaza el comprobante y, si no se revisa en 7 días, el comprobante expira automáticamente.",
                style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
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
            BulletItem("Comisión Ventago: \$0.27 por transacción aceptada (ITBMS incluido).")
            BulletItem("Cobro de plataforma en fechas periódicas (manual).")
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
            BulletItem(
                buildAnnotatedString {
                    append("Comisión de plataforma Ventago: ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("1% + 7% ITBMS")
                    }
                    append(" sobre ese 1%.")
                }
            )
            BulletItem("Yappy Comercial puede aplicar su propia comisión.")
            BulletItem("Cobro de plataforma en fechas periódicas (manual).")
            BulletItem(
                buildAnnotatedString {
                    append("Ejemplo por ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("\$100") }
                    append(": comisión Ventago ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("\$1.00") }
                    append(" + ITBMS ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("\$0.07 = \$1.07") }
                    append(" (más la comisión de Yappy Comercial).")
                }
            )

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
    val linkedBilling = summary?.linkedPaypalBillingAgreement == true

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
                active = linkedAccount,
                activeText = "Conectada",
            )
            PaypalStatusRow(
                label = "Autorización de cobro",
                active = linkedBilling,
                activeText = "Autorizada",
            )

            Text(
                text = "Debes completar ambos pasos para finalizar la configuración.",
                style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )

            ButtonM(
                onClick = viewModel::onConnectPaypal,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ) {
                Text("Conectar cuenta PayPal")
            }

            OutlinedButtonM(
                onClick = viewModel::onAuthorizePaypalBilling,
                contentColor = MaterialTheme.colorScheme.secondary,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
            ) {
                Text("Autorización de cobro")
            }

            if (!isOnboardingFlow && linkedAccount) {
                OutlinedButtonM(onClick = { viewModel.requestUnlinkMethod(PaymentMethodType.Paypal) }) {
                    Text("Desvincular PayPal")
                }
            }

            if (!isOnboardingFlow) {
                Text(
                    text = "Cuando ambas validaciones estén completas, PayPal quedará totalmente operativo.",
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
                label = if (configured) "Número de cuenta (reingresar para actualizar)" else "Número de cuenta",
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
    }
}

@Composable
private fun AchOnboardingFooter(
    step: Int,
    onBack: () -> Unit,
    onPrimary: () -> Unit,
) {
    val primaryLabel = if (step < 3) "Continuar" else "Guardar configuración ACH"
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
    }
}

@Composable
private fun PaypalOnboardingFooter(
    step: Int,
    onBack: () -> Unit,
    onPrimary: () -> Unit,
) {
    val primaryLabel = if (step < 4) "Continuar" else "Finalizar configuración"
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
        method == PaymentMethodType.Ach -> "Guardar configuración ACH"
        method == PaymentMethodType.Paypal -> "Continuar"
        else -> "Continuar"
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButtonM(onClick = onBack) {
            Text("Atrás")
        }
        ButtonM(onClick = onPrimary) {
            Text(primaryLabel)
        }
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
