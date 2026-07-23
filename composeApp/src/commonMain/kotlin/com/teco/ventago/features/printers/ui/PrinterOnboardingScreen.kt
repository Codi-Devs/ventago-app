@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.teco.ventago.features.printers.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.teco.ventago.core.SnackbarService
import com.teco.ventago.core.firebase.AnalyticsService
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.loaders.shimmerBrush
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.textfields.helpers.DMDropDownField
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.bodySmall
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.headlineSmall
import com.teco.ventago.design_system.theme.labelLarge
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.titleMedium
import com.teco.ventago.design_system.theme.vanishedBackgroundColor
import com.teco.ventago.features.printers.domain.model.PrinterDiscoveryStatus
import com.teco.ventago.features.printers.ui.viewmodel.PrinterBranchOption
import com.teco.ventago.features.printers.ui.viewmodel.PrinterConfigMode
import com.teco.ventago.features.printers.ui.viewmodel.PrinterEntryContext
import com.teco.ventago.features.printers.ui.viewmodel.PrinterOnboardingState
import com.teco.ventago.features.printers.ui.viewmodel.PrinterOnboardingStep
import com.teco.ventago.features.printers.ui.viewmodel.PrinterOnboardingUiEvent
import com.teco.ventago.features.printers.ui.viewmodel.PrinterOnboardingViewModel
import com.teco.ventago.utils.openWhatsappMessage
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ventago.composeapp.generated.resources.Res

@Composable
fun PrinterOnboardingScreen(
    entryContext: String,
    branchCode: String? = null,
    billingPointCode: String? = null,
    fromQr: Boolean = false,
    onDismiss: () -> Unit,
    onGoToBranches: () -> Unit,
    viewModel: PrinterOnboardingViewModel = koinViewModel(
        parameters = { parametersOf(entryContext, branchCode, billingPointCode, false) }
    ),
) {
    PrinterFlowContent(
        viewModel = viewModel,
        fromQr = fromQr,
        onDismiss = onDismiss,
        onGoToBranches = onGoToBranches,
        onSaveAndClose = onDismiss,
    )
}

@Composable
fun PrinterConfigScreen(
    entryContext: String,
    branchCode: String? = null,
    billingPointCode: String? = null,
    onDismiss: () -> Unit,
    onGoToBranches: () -> Unit = onDismiss,
    viewModel: PrinterOnboardingViewModel = koinViewModel(
        parameters = { parametersOf(entryContext, branchCode, billingPointCode, true) }
    ),
) {
    PrinterFlowContent(
        viewModel = viewModel,
        fromQr = false,
        onDismiss = onDismiss,
        onGoToBranches = onGoToBranches,
        onSaveAndClose = onDismiss,
    )
}

@Composable
private fun PrinterFlowContent(
    viewModel: PrinterOnboardingViewModel,
    fromQr: Boolean,
    onDismiss: () -> Unit,
    onGoToBranches: () -> Unit,
    onSaveAndClose: () -> Unit,
) {
    val snackbarService: SnackbarService = koinInject()
    val analyticsService: AnalyticsService = koinInject()
    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var completedEventSent by rememberSaveable { mutableStateOf(false) }
    val analyticsSource = remember(uiState.entryContext, fromQr) {
        printerAnalyticsSource(uiState.entryContext, fromQr)
    }

    LaunchedEffect(Unit) {
        analyticsService.logPrinterOnboardingOpened(
            source = analyticsSource,
            step = printerAnalyticsStep(uiState.step)
        )
        viewModel.events.collect { event ->
            when (event) {
                is PrinterOnboardingUiEvent.Message -> snackbarService.show(event.text)
                PrinterOnboardingUiEvent.SavedAndClose -> onSaveAndClose()
            }
        }
    }

    LaunchedEffect(uiState.step) {
        val step = printerAnalyticsStep(uiState.step)
        analyticsService.logPrinterOnboardingStepViewed(
            source = analyticsSource,
            step = step
        )
        if (uiState.step == PrinterOnboardingStep.SUCCESS && !completedEventSent) {
            completedEventSent = true
            analyticsService.logPrinterOnboardingCompleted(source = analyticsSource)
        }
    }

    when (uiState.step) {
        PrinterOnboardingStep.LANDING -> PrinterLandingStep(
            fromQr = fromQr,
            onBuyPrinter = {
                analyticsService.logPrinterOnboardingActionClicked(
                    source = analyticsSource,
                    step = "landing",
                    actionValue = "buy"
                )
                openWhatsappMessage(
                    "50763879477",
                    "Hola, quiero adquirir una impresora térmica para usarla con VentaGo."
                )
            },
            onHasPrinter = {
                analyticsService.logPrinterOnboardingActionClicked(
                    source = analyticsSource,
                    step = "landing",
                    actionValue = "configure"
                )
                viewModel.startSetupFlow()
            },
            onDismiss = {
                analyticsService.logPrinterOnboardingDismissed(
                    source = analyticsSource,
                    step = "landing"
                )
                onDismiss()
            },
        )

        PrinterOnboardingStep.SETUP -> PrinterSetupStep(
            slideIndex = uiState.setupSlideIndex,
            onBack = viewModel::previousSetupSlide,
            onNext = viewModel::nextSetupSlide,
        )

        PrinterOnboardingStep.NETWORK -> PrinterNetworkStep(
            onBack = viewModel::goBackFromNetwork,
            onContinue = viewModel::continueToConfig,
        )

        PrinterOnboardingStep.CONFIG -> PrinterConfigStep(
            uiState = uiState,
            onBack = {
                if (uiState.showSuccessOnSave) {
                    viewModel.goBackFromConfig()
                } else {
                    analyticsService.logPrinterOnboardingDismissed(
                        source = analyticsSource,
                        step = "config"
                    )
                    onDismiss()
                }
            },
            onConfigModeChange = viewModel::setConfigMode,
            onShowAlert = { message ->
                coroutineScope.launch {
                    snackbarService.show(message)
                }
            },
            onRescanPrinters = viewModel::rescanPrinters,
            onDiscoveredPrinterSelect = viewModel::selectDiscoveredPrinter,
            onBranchChange = viewModel::selectBranch,
            onBillingPointChange = viewModel::selectBillingPoint,
            onPrinterNameChange = viewModel::onPrinterNameChange,
            onHostChange = viewModel::onHostChange,
            onPaperWidthChange = viewModel::onPaperWidthChange,
            onPrintByDefaultChange = viewModel::onPrintByDefaultChange,
            onIsActiveChange = viewModel::onIsActiveChange,
            onTestPrint = viewModel::testPrint,
            onSave = viewModel::savePrinter,
        )

        PrinterOnboardingStep.SUCCESS -> PrinterSuccessStep(
            onGoToBranches = onGoToBranches,
            onConfigureAnother = {
                analyticsService.logPrinterOnboardingActionClicked(
                    source = analyticsSource,
                    step = "success",
                    actionValue = "configure_another"
                )
                viewModel.configureAnotherPrinter()
            },
        )
    }

    if (uiState.loadingBottomSheet.isLoading()) {
        LoadingSheet(
            state = uiState.loadingBottomSheet,
            sheetState = loadingSheetState,
            loadingAnimationFile = "files/61209-loading-loop.json"
        ) {
            viewModel.hideLoading()
        }
    }
}

private fun printerAnalyticsSource(entryContext: PrinterEntryContext, fromQr: Boolean): String {
    return if (fromQr) "qr" else entryContext.name.lowercase()
}

private fun printerAnalyticsStep(step: PrinterOnboardingStep): String {
    return when (step) {
        PrinterOnboardingStep.LANDING -> "landing"
        PrinterOnboardingStep.SETUP -> "setup"
        PrinterOnboardingStep.NETWORK -> "network"
        PrinterOnboardingStep.CONFIG -> "config"
        PrinterOnboardingStep.SUCCESS -> "success"
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun PrinterLandingStep(
    fromQr: Boolean,
    onBuyPrinter: () -> Unit,
    onHasPrinter: () -> Unit,
    onDismiss: () -> Unit,
) {
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/print-invoice-an.json").decodeToString()
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row (verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.ElectricBolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Lleva tu facturación al siguiente nivel",
                    style = labelLarge().copy(color = MaterialTheme.colorScheme.onSecondary)
                )
            }

        }

        Text(
            text = "Imprime y vende más rápido con una impresora térmica",
            style = headlineSmall().copy(fontWeight = FontWeight.SemiBold)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Image(
                painter = rememberLottiePainter(
                    composition = composition,
                    iterations = Compottie.IterateForever
                ),
                contentDescription = "Impresora térmica",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(top = 16.dp)
            )
            Text(
                text = "Conecta tu impresora y déjanos ayudarte a detectarla automáticamente.",
                modifier = Modifier.padding(16.dp),
                style = bodyMediumBold(color = MaterialTheme.colorScheme.onSecondary)
            )
        }
        Text(
            text = "Convierte cada factura en una experiencia más ágil y profesional para tus clientes. Con una impresora térmica reduces esperas, entregas comprobantes al instante y haces tu operación mucho más fluida.",
            style = bodyMedium()
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
        ) {
            Text(
                text = "Empieza a imprimir tus facturas en segundos y da una mejor experiencia a tus clientes.",
                modifier = Modifier.padding(16.dp),
                style = bodyMediumBold(color = MaterialTheme.colorScheme.primary)
            )
        }
        BenefitRow(
            text = "Agiliza el cobro en caja y evita filas innecesarias.",
            bulletColor = MaterialTheme.colorScheme.secondary
        )
        BenefitRow(
            text = "Entrega tickets claros y profesionales en el momento exacto.",
            bulletColor = MaterialTheme.colorScheme.secondary
        )
        BenefitRow(
            text = "Recibe acompañamiento de VentaGo para escoger y configurar tu impresora.",
            bulletColor = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = "Si ya tienes tu impresora, te guiamos paso a paso para dejarla lista. Si todavía no la tienes, te ayudamos a conseguir la ideal para tu negocio.",
            style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (!fromQr) {
            ButtonM(
                onClick = onBuyPrinter,
                containerColor = Color(0xFF25D366),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Chat,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Adquirir mi impresora", color = Color.White)
                }
            }
        }
        OutlinedButtonM(
            onClick = onHasPrinter,
            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
            contentColor = MaterialTheme.colorScheme.secondary,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
        ) {
            Text(if (fromQr) "Configurar impresora" else "Ya tengo mi impresora")
        }
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TextButtonS(label = "Configurar más tarde") { onDismiss() }
        }
    }
}

@Composable
private fun PrinterSetupStep(
    slideIndex: Int,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = slideIndex, pageCount = { 4 })
    LaunchedEffect(slideIndex) {
        if (pagerState.currentPage != slideIndex) {
            pagerState.scrollToPage(slideIndex)
        }
    }

    val slides = remember {
        listOf(
            "Conéctala a la corriente" to "Enchufa la impresora a la fuente de poder, pero todavía no la enciendas.",
            "Abre la impresora" to "Levanta la tapa para dejar listo el espacio donde irá el rollo de papel.",
            "Coloca el papel correctamente" to "Verifica la posición del rollo para que el papel salga por el frente sin trabarse.",
            "Ahora sí, enciéndela" to "Usa el botón de inicio para prender la impresora y dejarla lista para conectarla a la red.",
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("1. Pongamos tu impresora en marcha", style = titleMedium())
        Text(
            "Sigue este recorrido para preparar la impresora correctamente antes de conectarla a tu red.",
            style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier.weight(1f, fill = true)
        ) { page ->
            val slide = slides[page]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = vanishedBackgroundColor())
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AsyncImage(
                        model = PrinterOnboardingViewModel.SETUP_SLIDES[page],
                        contentDescription = slide.first,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    )
                    Text(slide.first, style = bodyMediumBold())
                    Text(slide.second, style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
            }
        }

        StepIndicator(
            current = slideIndex,
            total = 4,
            activeColor = MaterialTheme.colorScheme.secondary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButtonM(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("Atrás")
            }
            ButtonM(
                onClick = onNext,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ) {
                Text("Continuar")
            }
        }
    }
}

@Composable
private fun PrinterNetworkStep(
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("2. Conecta la impresora a tu red", style = titleMedium())
        Text(
            "Usa un cable ethernet para conectar la impresora a tu red. Después de unos segundos, la impresora debe sacar un papel pequeño con los datos de red.",
            style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Tu papel impreso debe verse parecido a este ejemplo", style = bodyMediumBold())
                Text(
                    "No necesitas que sea idéntico. Lo importante es que en el papel aparezcan estos datos de red, especialmente la línea IP Address.",
                    style = bodyMedium()
                )
                Divider()
                PaperField("IP Address", "XXX.XXX.XXX.XXX")
                PaperField("SubnetMask", "XXX.XXX.XXX.XXX")
                PaperField("Gateway", "XXX.XXX.XXX.XXX")
                PaperField("DHCP", "Enable")
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = vanishedBackgroundColor())
        ) {
            Text(
                text = "Asegúrate de que el dispositivo desde el que estás configurando VentaGo esté conectado a la misma red que la impresora.",
                modifier = Modifier.padding(16.dp),
                style = bodyMediumBold(color = MaterialTheme.colorScheme.primary)
            )
        }

        ButtonM(onClick = onContinue, containerColor = MaterialTheme.colorScheme.secondary) {
            Text("Ya tengo el papel con la dirección de red")
        }
        OutlinedButtonM(onClick = onBack) {
            Text("Atrás")
        }

    }
}

@Composable
private fun PrinterConfigStep(
    uiState: PrinterOnboardingState,
    onBack: () -> Unit,
    onConfigModeChange: (PrinterConfigMode) -> Unit,
    onShowAlert: (String) -> Unit,
    onRescanPrinters: () -> Unit,
    onDiscoveredPrinterSelect: (String) -> Unit,
    onBranchChange: (String) -> Unit,
    onBillingPointChange: (String) -> Unit,
    onPrinterNameChange: (String) -> Unit,
    onHostChange: (String) -> Unit,
    onPaperWidthChange: (Int) -> Unit,
    onPrintByDefaultChange: (Boolean) -> Unit,
    onIsActiveChange: (Boolean) -> Unit,
    onTestPrint: () -> Unit,
    onSave: () -> Unit,
) {
    LaunchedEffect(uiState.configMode, uiState.discoveryState.status) {
        if (uiState.configMode != PrinterConfigMode.AUTOMATIC) return@LaunchedEffect
        when (uiState.discoveryState.status) {
            PrinterDiscoveryStatus.UNSUPPORTED -> {
                onConfigModeChange(PrinterConfigMode.MANUAL)
                onShowAlert("Este dispositivo no soporta búsqueda automática.")
            }
            PrinterDiscoveryStatus.ERROR -> {
                onShowAlert("No se pudo iniciar la búsqueda automática.")
            }
            else -> Unit
        }
    }

    var showAdvancedConfig by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            if (uiState.useInternalPrinter) "Configura la impresora interna" else "3. Configura tu impresora en VentaGo",
            style = titleMedium()
        )

        if (!uiState.useInternalPrinter) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ConfigModeOption(
                    label = "Automático",
                    selected = uiState.configMode == PrinterConfigMode.AUTOMATIC,
                    onClick = { onConfigModeChange(PrinterConfigMode.AUTOMATIC) },
                    modifier = Modifier.weight(1f)
                )
                ConfigModeOption(
                    label = "Manual",
                    selected = uiState.configMode == PrinterConfigMode.MANUAL,
                    onClick = { onConfigModeChange(PrinterConfigMode.MANUAL) },
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Text(
                "Este equipo usa la impresora térmica integrada del POS. Solo selecciona la sucursal y el punto de facturación.",
                style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }

        if (!uiState.useInternalPrinter && uiState.isAutomaticMode) {
            AutomaticDiscoverySection(
                uiState = uiState,
                onRescanPrinters = onRescanPrinters,
                onDiscoveredPrinterSelect = onDiscoveredPrinterSelect,
            )
        }

        val shouldRenderForm = uiState.useInternalPrinter ||
            uiState.configMode == PrinterConfigMode.MANUAL ||
            uiState.selectedDiscoveredPrinter != null
        if (shouldRenderForm) {
            DMDropDownField(
                label = "Sucursal",
                items = uiState.branches,
                enabled = !uiState.selectionLocked,
                selectedIndex = uiState.branches.indexOfFirst { it.code == uiState.selectedBranchCode },
                onItemSelected = { _, item -> onBranchChange(item.code) },
                selectedItemToString = { it.label },
                isError = !uiState.hasValidSelection
            )

            val billingPoints = uiState.selectedBranch?.billingPoints.orEmpty()
            DMDropDownField(
                label = "Punto de facturación",
                items = billingPoints,
                enabled = !uiState.selectionLocked,
                selectedIndex = billingPoints.indexOfFirst { it.code == uiState.selectedBillingPointCode },
                onItemSelected = { _, item -> onBillingPointChange(item.code) },
                selectedItemToString = { it.label },
                isError = !uiState.hasValidSelection
            )
        }

        if (!uiState.hasValidSelection && shouldRenderForm) {
            Text(
                "Selecciona una sucursal con un punto de facturación activo para continuar con la configuración.",
                style = bodySmall(color = MaterialTheme.colorScheme.error)
            )
        }

        if (uiState.configMode == PrinterConfigMode.MANUAL) {
            DMOutlinedTextField(
                text = uiState.printerName,
                label = "Nombre de impresora",
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.useInternalPrinter,
                supportingText = if (uiState.useInternalPrinter) "Impresora interna del POS" else "Ej. TM-T20III",
                onChange = onPrinterNameChange,
            )

            if (!uiState.useInternalPrinter) {
                DMOutlinedTextField(
                    text = uiState.host,
                    label = "IP exacta de la impresora",
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = "Usa la IP que salió impresa en el papel de la red.",
                    onChange = onHostChange,
                )
            }
        }

        if (shouldRenderForm) {
            Text("Ancho del papel", style = bodyMediumBold())
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PaperWidthOption(
                    label = "57 mm",
                    selected = uiState.paperWidthMm == 57,
                    modifier = Modifier.weight(1f),
                    onClick = { onPaperWidthChange(57) }
                )
                PaperWidthOption(
                    label = "80 mm",
                    selected = uiState.paperWidthMm == 80,
                    modifier = Modifier.weight(1f),
                    onClick = { onPaperWidthChange(80) }
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = cardContainerColor())
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Otras configuraciones", style = bodyMediumBold())
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { showAdvancedConfig = !showAdvancedConfig }) {
                            Icon(
                                imageVector = if (showAdvancedConfig) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = if (showAdvancedConfig) "Contraer" else "Expandir"

                            )
                        }
                    }
                    if (showAdvancedConfig) {
                        Divider()
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            SwitchRow(
                                title = "Imprimir automáticamente al facturar",
                                description = "Cuando generes una factura, VentaGo enviará la impresión automáticamente a esta impresora.",
                                checked = uiState.printByDefault,
                                onCheckedChange = onPrintByDefaultChange,
                            )

                            SwitchRow(
                                title = "Impresora habilitada",
                                description = "",
                                checked = uiState.isActive,
                                onCheckedChange = onIsActiveChange,
                            )
                        }
                    }
                }
            }
        }

        uiState.lastTestMessage?.let {
            Text(
                text = it,
                style = bodySmall(color = MaterialTheme.colorScheme.primary)
            )
        }

        uiState.validationMessage?.let {
            Text(
                text = it,
                style = bodySmall(color = MaterialTheme.colorScheme.error)
            )
        }

        val canTestPrint = shouldRenderForm &&
            uiState.hasValidSelection &&
            uiState.host.isNotBlank() &&
            uiState.printerName.isNotBlank()

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButtonM(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Atrás")
            }
            OutlinedButtonM(
                onClick = onTestPrint,
                enabled = canTestPrint,
                modifier = Modifier.fillMaxWidth(),
                contentColor = MaterialTheme.colorScheme.secondary,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
            ) {
                Text("Probar impresión")
            }
            ButtonM(
                onClick = onSave,
                enabled = uiState.canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar")
            }
        }
    }
}

@Composable
private fun ConfigModeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
                containerColor = if (selected) {
                    vanishedBackgroundColor()
                } else {
                    cardContainerColor()
                }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.secondary
                )
            )
            Text(label, style = bodyMedium())
        }
    }
}

@Composable
private fun AutomaticDiscoverySection(
    uiState: PrinterOnboardingState,
    onRescanPrinters: () -> Unit,
    onDiscoveredPrinterSelect: (String) -> Unit,
) {
    val isScanning = uiState.discoveryState.status == PrinterDiscoveryStatus.SCANNING

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Impresoras disponibles", style = bodyMediumBold())
        Spacer(modifier = Modifier.weight(1f))
        if (isScanning) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp
            )
        } else {
            IconButton(
                onClick = onRescanPrinters,
                enabled = uiState.canRescanAutomatically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Reescanear",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (uiState.discoveryState.printers.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            uiState.discoveryState.printers.forEach { printer ->
                val selected = uiState.selectedDiscoveredPrinter?.id == printer.id
                Card(
                    onClick = { onDiscoveredPrinterSelect(printer.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) {
                           vanishedBackgroundColor()
                        } else {
                            cardContainerColor()
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { onDiscoveredPrinterSelect(printer.id) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.secondary
                            )
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(printer.displayName, style = bodyMediumBold())
                        }
                    }
                }
            }
        }
    }

    if (isScanning && uiState.discoveryState.printers.isEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) {
                Card(colors = CardDefaults.cardColors(containerColor = cardContainerColor())) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(shimmerBrush(), RoundedCornerShape(50))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(18.dp)
                                .background(shimmerBrush(), RoundedCornerShape(8.dp))
                        )
                    }
                }
            }
        }
    }

    if (uiState.discoveryState.printers.isEmpty() && !isScanning) {
        Card(colors = CardDefaults.cardColors(containerColor = cardContainerColor())) {
            Text(
                text = "No encontramos impresoras todavía. Revisa que estén en la misma red y vuelve a intentar.",
                modifier = Modifier.padding(16.dp),
                style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }

}

@Composable
private fun PrinterSuccessStep(
    onGoToBranches: () -> Unit,
    onConfigureAnother: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = vanishedBackgroundColor())
        ) {
            Text(
                text = "Todo listo para imprimir",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                style = bodyMediumBold(color = MaterialTheme.colorScheme.primary)
            )
        }
        Text("Tu impresora ya quedó configurada", style = headlineSmall())
        Text(
            "Desde este momento puedes imprimir tus facturas térmicas con una experiencia mucho más rápida en caja.",
            style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        BenefitRow("Tu punto de facturación ya tiene una impresora lista para trabajar.")
        BenefitRow("Puedes seguir configurando otras impresoras si manejas más sucursales o cajas.")
        BenefitRow("Ya puedes volver a sucursales y continuar con el resto de tu operación.")
        Spacer(modifier = Modifier.height(8.dp))
        ButtonM(onClick = onGoToBranches, containerColor = MaterialTheme.colorScheme.secondary) {
            Text("Ir a sucursales")
        }
        OutlinedButtonM(onClick = onConfigureAnother) {
            Text("Configurar otra impresora")
        }
    }
}

@Composable
private fun BenefitRow(
    text: String,
    bulletColor: Color = MaterialTheme.colorScheme.secondary,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(8.dp)
                .background(bulletColor, RoundedCornerShape(50))
        )
        Text(text = text, style = bodyMedium())
    }
}

@Composable
private fun StepIndicator(
    current: Int,
    total: Int,
    activeColor: Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(
                        color = if (index == current) activeColor else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(50)
                    )
            )
        }
    }
}

@Composable
private fun PaperField(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = bodyMedium())
        Text(value, style = bodyMediumBold())
    }
}

@Composable
private fun PaperWidthOption(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) vanishedBackgroundColor() else cardContainerColor()
        ),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.secondary
            ))
            Text(label, style = bodyMedium())
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(title, style = bodyMediumBold())
                if (description.isNotBlank()) {
                    Text(
                        description,
                        style = bodySmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
