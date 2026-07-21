package com.teco.ventago

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shop
import androidx.compose.material.icons.outlined.ShoppingCartCheckout
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ShoppingCartCheckout
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.teco.ventago.core.SnackbarService
import com.teco.ventago.core.LocalStorage
import com.teco.ventago.core.authz.AuthzEvaluator
import com.teco.ventago.core.beta.BetaFeature
import com.teco.ventago.core.beta.BetaService
import com.teco.ventago.core.flags.IFlagsService
import com.teco.ventago.core.firebase.AnalyticsService
import com.teco.ventago.design_system.molecules.AppChromeState
import com.teco.ventago.design_system.molecules.ColoredTopBarHost
import com.teco.ventago.design_system.molecules.DMAlertDialog
import com.teco.ventago.design_system.molecules.DMTopAppBar
import com.teco.ventago.design_system.molecules.flags.MaintenanceModeOverlay
import com.teco.ventago.design_system.molecules.rememberAppChromeState
import com.teco.ventago.design_system.theme.DigitalMenuTheme
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.features.auth.domain.IAuthService
import com.teco.ventago.features.printers.domain.PrinterQrEntry
import com.teco.ventago.features.printers.ui.viewmodel.PrinterEntryContext
import com.teco.ventago.features.payments.ui.home.viewmodel.PaymentMethodsViewModel
import com.teco.ventago.features.pos.ui.viewmodel.FlowMode
import com.teco.ventago.features.pos.ui.viewmodel.PosState
import com.teco.ventago.features.pos.ui.viewmodel.PosViewModel
import com.teco.ventago.navigation.BottomNavKey
import com.teco.ventago.navigation.LocalNavController
import com.teco.ventago.navigation.Navigation
import com.teco.ventago.navigation.PrinterOnboardingRoute
import com.teco.ventago.navigation.PosScreens
import com.teco.ventago.navigation.fallbackScreenFor
import com.teco.ventago.navigation.routeKeyForScreen
import com.teco.ventago.navigation.toPosScreenOrNull
import com.teco.ventago.navigation.visibleBottomNavKeys
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.home_summary_tab
import ventago.composeapp.generated.resources.pos_edit_quote
import ventago.composeapp.generated.resources.pos_new_invoice
import ventago.composeapp.generated.resources.pos_new_quote

val LocalAppChrome = staticCompositionLocalOf<AppChromeState> {
    error("AppChromeState not provided")
}

private const val SUMMARY_TAB_HINT_SEEN_KEY_PREFIX = "summary_tab_hint_seen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App(
    appViewModel: AppViewModel = koinViewModel<AppViewModel>(),
    navController: NavHostController = rememberNavController()
) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory())
            }
            .build()
    }

    val mainState by appViewModel.mainState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarService: SnackbarService = koinInject()
    snackbarService.hostState = snackbarHostState
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentScreen = currentRoute?.toPosScreenOrNull() ?: PosScreens.LoginScreen
    val appChrome = rememberAppChromeState()
    val flagsService = koinInject<IFlagsService>()
    val authService = koinInject<IAuthService>()
    val betaService = koinInject<BetaService>()
    val localStorage: LocalStorage = koinInject()
    val flagsState by flagsService.flags().collectAsState()
    val currentUser by authService.getUser().collectAsState(initial = null)
    val betaResponse by betaService.features().collectAsState()
    val betaSnapshot = remember(betaResponse) {
        betaResponse?.features.orEmpty()
            .mapNotNull(BetaFeature::fromKey)
            .toSet()
    }
    val bottomNavKeys = remember(currentUser, betaSnapshot) {
        visibleBottomNavKeys(currentUser, betaSnapshot)
    }
    val currentRouteKey = remember(currentScreen) { routeKeyForScreen(currentScreen) }
    val summaryHintSeenKey = remember(mainState.business?.businessId, mainState.isAuthenticated) {
        if (mainState.isAuthenticated) {
            "$SUMMARY_TAB_HINT_SEEN_KEY_PREFIX:${mainState.business?.businessId ?: "default"}"
        } else {
            "$SUMMARY_TAB_HINT_SEEN_KEY_PREFIX:guest"
        }
    }
    var showSummaryHintDot by remember(summaryHintSeenKey) {
        mutableStateOf(localStorage.bool(summaryHintSeenKey) != true)
    }
    var showPendingPrinterQrDialog by remember { mutableStateOf(false) }

//    LaunchedEffect(mainState.isAuthenticated, mainState.missingBusiness) {
//        println("newState isAuthenticated: ${mainState.isAuthenticated}")
//        if (mainState.isAuthenticated && !mainState.missingBusiness) {
//            navController.navigate(PosScreens.HomeScreen.name) {
//                popUpTo(0) { inclusive = true }
//            }
//        } else if (!mainState.isAuthenticated) {
//            navController.navigate(PosScreens.LoginScreen.name) {
//                popUpTo(0) { inclusive = true }
//            }
//        }
//    }

    val graphReady = backStackEntry != null
    data class AuthBucket(val authed: Boolean, val hasBusiness: Boolean, val invoiceActive: Boolean)
    val currentBucket = AuthBucket(
        authed = mainState.isAuthenticated,
        hasBusiness = !mainState.missingBusiness,
        invoiceActive = mainState.invoicingConfigured
    )
    var lastBucket by remember { mutableStateOf<AuthBucket?>(null) }
    var didInitialRedirect by remember { mutableStateOf(false) }

    LaunchedEffect(graphReady, currentBucket) {
        if (!graphReady) return@LaunchedEffect

        val prev = lastBucket
        val initial = prev == null && !didInitialRedirect
        val changed = prev != null && prev != currentBucket

        if (initial || changed) {
            lastBucket = currentBucket
            didInitialRedirect = true

            val target = if (currentBucket.authed && currentBucket.hasBusiness && currentBucket.invoiceActive) {
                PosScreens.HomeScreen.name
            } else if (currentBucket.authed && !currentBucket.hasBusiness) {
                PosScreens.BusinessRegisterScreen.name
            } else if (!currentBucket.invoiceActive) {
                PosScreens.InvoiceLandingScreen.name
            } else {
                PosScreens.LoginScreen.name
            }

            // Only jump if we're not already there
            val currentRoute = backStackEntry?.destination?.route
            if (currentRoute != target) {
                navController.navigate(target) {
                    // Clear the stack to the start of the graph (safe alternative to popUpTo(0))
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    LaunchedEffect(mainState.isAuthenticated) {
        if (mainState.isAuthenticated) {
            flagsService.initialize()
            betaService.getFeatures()
        } else {
            flagsService.destroy()
            betaService.clear()
        }
    }

    LaunchedEffect(currentScreen, showSummaryHintDot, summaryHintSeenKey) {
        if (currentScreen == PosScreens.SummaryScreen && showSummaryHintDot) {
            localStorage.set(summaryHintSeenKey, true)
            showSummaryHintDot = false
        }
    }

    LaunchedEffect(graphReady, mainState.isAuthenticated, mainState.business?.businessId) {
        if (!graphReady || !mainState.isAuthenticated || mainState.business == null) {
            showPendingPrinterQrDialog = false
            return@LaunchedEffect
        }
        showPendingPrinterQrDialog =
            localStorage.bool(PrinterQrEntry.KEY_PENDING_ONBOARDING) == true
    }

    LaunchedEffect(graphReady, currentRoute, currentRouteKey, currentUser, betaSnapshot, currentBucket) {
        if (!graphReady || !currentBucket.authed || !currentBucket.hasBusiness || !currentBucket.invoiceActive) {
            return@LaunchedEffect
        }
        if (currentUser == null) {
            return@LaunchedEffect
        }
        val routeKey = currentRouteKey ?: return@LaunchedEffect
        if (AuthzEvaluator.canRoute(routeKey, currentUser, betaSnapshot)) {
            return@LaunchedEffect
        }
        val fallback = fallbackScreenFor(currentUser, betaSnapshot) ?: PosScreens.UnauthorizedScreen
        if (currentRoute == fallback.name) {
            return@LaunchedEffect
        }
        val popTarget = navController.currentDestination?.id ?: navController.graph.findStartDestination().id
        navController.navigate(fallback.name) {
            popUpTo(popTarget) { inclusive = true }
            launchSingleTop = true
        }
    }

    DigitalMenuTheme {
        CompositionLocalProvider(
            LocalAppChrome provides appChrome,
            LocalNavController provides navController,
            ){
            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState)
                    },
                    topBar = {
                    if (mainState.hideAppVar || !currentScreen.showAppBar) {
                        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                        return@Scaffold
                    }
                    // TODO: decide when to use colored app bar
                    val wantsColored = false
                    val isPosTitleScreen = currentScreen == PosScreens.POSScreen ||
                        currentScreen == PosScreens.POSProductScreen ||
                        currentScreen == PosScreens.CartScreen
                    val isQuoteSummaryScreen = currentScreen == PosScreens.QuoteSummaryScreen
                    val isYappyOnsitePaymentScreen = currentScreen == PosScreens.YappyOnsitePaymentScreen
                    val posBackStackEntry = remember(currentScreen) {
                        if (isPosTitleScreen || isQuoteSummaryScreen || isYappyOnsitePaymentScreen) {
                            runCatching { navController.getBackStackEntry(PosScreens.POS.name) }.getOrNull()
                        } else {
                            null
                        }
                    }
                    val posViewModel = posBackStackEntry?.let {
                        koinViewModel<PosViewModel>(viewModelStoreOwner = it)
                    }
                    val posUiState by posViewModel?.uiState?.collectAsState()
                        ?: remember { mutableStateOf(PosState()) }
                    val appBarTitle = if (currentScreen.showAppBar) {
                        when {
                            (isPosTitleScreen || isQuoteSummaryScreen) && posUiState.flowMode == FlowMode.QUOTE -> {
                                if (posUiState.quoteId != null) {
                                    stringResource(Res.string.pos_edit_quote)
                                } else {
                                    stringResource(Res.string.pos_new_quote)
                                }
                            }
                            isPosTitleScreen -> stringResource(Res.string.pos_new_invoice)
                            else -> stringResource(currentScreen.title)
                        }
                    } else {
                        ""
                    }
//                        currentScreen == PosScreens.SearchCustomerScreen
                    if (wantsColored) {
                        val onPrimary = MaterialTheme.colorScheme.onSecondary
                        val transparentColors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            scrolledContainerColor = MaterialTheme.colorScheme.secondary,
                            navigationIconContentColor = onPrimary,
                            titleContentColor = onPrimary,
                            actionIconContentColor = onPrimary
                        )
                        DMTopAppBar(
                            title = appBarTitle,
                            showBackButton = currentScreen.showBackButton && navController.previousBackStackEntry != null,
                            navigateBack = {
                                if (navController.previousBackStackEntry != null) {
                                    val handled = isYappyOnsitePaymentScreen &&
                                        posViewModel?.requestYappyOnsiteQrExit() == true
                                    if (!handled) {
                                        navController.navigateUp()
                                    }
                                }
                            },
                            // make sure your DMTopAppBar uses containerColor = Color.Transparent inside
                            actions = {
                                // keep your existing actions routing logic
                                if (currentScreen.isPosScreens()) {
                                    val bse = remember { navController.getBackStackEntry(PosScreens.POS.name) }
                                    currentScreen.actions(
                                        bse,
                                        { navController.navigate(it.name) },
                                        { navController.navigate(it) }
                                    )
                                } else {
                                    currentScreen.actions(
                                        backStackEntry,
                                        { navController.navigate(it.name) },
                                        { navController.navigate(it) }
                                    )
                                }
                            },
                            colors = transparentColors,

                        )
                    } else {
                        val isPaymentMethodScreen = currentScreen == PosScreens.PaymentsYappyScreen ||
                            currentScreen == PosScreens.PaymentsYappyOnsiteScreen ||
                            currentScreen == PosScreens.PaymentsTransferenceScreen ||
                            currentScreen == PosScreens.PaymentsPaypalScreen ||
                            currentScreen == PosScreens.PaymentsPaypalOnboardingScreen ||
                            currentScreen == PosScreens.PaymentsTiloPayScreen ||
                            currentScreen == PosScreens.PaymentsFeesScreen
                        val paymentsBackStackEntry = remember(currentScreen) {
                            if (isPaymentMethodScreen) {
                                runCatching { navController.getBackStackEntry(PosScreens.Payments.name) }.getOrNull()
                            } else {
                                null
                            }
                        }
                        val paymentMethodsViewModel = paymentsBackStackEntry?.let {
                            koinViewModel<PaymentMethodsViewModel>(viewModelStoreOwner = it)
                        }
                        DMTopAppBar(
                            title = appBarTitle,
                            showBackButton = currentScreen.showAppBar && currentScreen.showBackButton &&navController.previousBackStackEntry != null,
                            navigateBack = {
                                if (navController.previousBackStackEntry != null) {
                                    if (isPaymentMethodScreen) {
                                        paymentMethodsViewModel?.onEnterHomeRoute()
                                    }
                                    val handled = isYappyOnsitePaymentScreen &&
                                        posViewModel?.requestYappyOnsiteQrExit() == true
                                    if (!handled) {
                                        navController.navigateUp()
                                    }
                                }
                            },
                            actions = {
                                if (currentScreen.isPosScreens()) {
                                    val backStackEntryAux = remember { navController.getBackStackEntry(PosScreens.POS.name) }
                                    currentScreen.actions(backStackEntryAux, { destination ->
                                        navController.navigate(destination.name)
                                    }, { route ->
                                        navController.navigate(route)
                                    })
                                } else {
                                    currentScreen.actions(backStackEntry, { destination ->
                                        navController.navigate(destination.name)
                                    }, { route ->
                                        navController.navigate(route)
                                    })
                                }

                            },
                        )
                    }

                    },
                    bottomBar = {
                    currentScreen.bottomBar?.let {
                        if (currentScreen == PosScreens.POSScreen || currentScreen == PosScreens.POSProductScreen || currentScreen == PosScreens.CartScreen) {
                            val auxbackStackEntry = remember { navController.getBackStackEntry(PosScreens.POS.name) }
                            it(auxbackStackEntry) { destination ->
                                navController.navigate(destination.name)
                            }
                        } else {
                            it(backStackEntry) { destination ->
                                navController.navigate(destination.name)
                            }
                        }

                    }?: run {
                        val visibleBottomScreens = bottomNavKeys.map(BottomNavKey::selectedScreen)
                        if (currentScreen !in visibleBottomScreens || bottomNavKeys.isEmpty())
                            return@Scaffold

                        NavigationBar(
                            modifier = Modifier.fillMaxWidth().shadow(
                                elevation = 10.dp,
                                shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                            ),
                            containerColor = cardContainerColor(),
                            tonalElevation = 8.dp,

                            ) {
                            bottomNavKeys.forEach { item ->
                                NavigationBarItem(
                                    selected = currentScreen == item.selectedScreen,
                                    onClick = {
                                        if (currentScreen != item.selectedScreen) {
                                            navController.navigate(item.destinationScreen.name)
                                        }
                                    },
                                    icon = {
                                        when (item) {
                                            BottomNavKey.HOME -> {
                                                Icon(
                                                    imageVector = if (currentScreen == item.selectedScreen) {
                                                        Icons.Filled.Home
                                                    } else {
                                                        Icons.Outlined.Home
                                                    },
                                                    contentDescription = "Home"
                                                )
                                            }
                                            BottomNavKey.SUMMARY -> {
                                                BadgedBox(
                                                    badge = {
                                                        if (showSummaryHintDot) {
                                                            Badge(
                                                                containerColor = MaterialTheme.colorScheme.secondary,
                                                                contentColor = MaterialTheme.colorScheme.secondary
                                                            )
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = if (currentScreen == item.selectedScreen) {
                                                            Icons.Filled.Assessment
                                                        } else {
                                                            Icons.Outlined.Assessment
                                                        },
                                                        contentDescription = "Resumen"
                                                    )
                                                }
                                            }
                                            BottomNavKey.ORDERS -> {
                                                Icon(
                                                    imageVector = if (currentScreen == item.selectedScreen) {
                                                        Icons.Filled.ReceiptLong
                                                    } else {
                                                        Icons.Outlined.ReceiptLong
                                                    },
                                                    contentDescription = "Facturas"
                                                )
                                            }
                                            BottomNavKey.MENU -> {
                                                Icon(
                                                    imageVector = Icons.Filled.Menu,
                                                    contentDescription = "Menú"
                                                )
                                            }
                                            BottomNavKey.SETTINGS -> {
                                                Icon(
                                                    imageVector = if (currentScreen == item.selectedScreen) {
                                                        Icons.Filled.Settings
                                                    } else {
                                                        Icons.Outlined.Settings
                                                    },
                                                    contentDescription = "Settings"
                                                )
                                            }
                                        }
                                    },
                                    label = {
                                        Text(
                                            when (item) {
                                                BottomNavKey.HOME -> "Home"
                                                BottomNavKey.SUMMARY -> stringResource(Res.string.home_summary_tab)
                                                BottomNavKey.ORDERS -> "Facturas"
                                                BottomNavKey.MENU -> "Menú"
                                                BottomNavKey.SETTINGS -> "Opciones"
                                            }
                                        )
                                    }
                                )
                            }

                        }
                    }
                    }
                ) { innerPadding ->
                    Navigation(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        navController = navController,
                        appViewModel = appViewModel,
                        analyticsService = koinInject<AnalyticsService>()
                    )
                }

                if (flagsState.maintenanceMode) {
                    MaintenanceModeOverlay(
                        modifier = Modifier.fillMaxSize()
                    )
                }

                DMAlertDialog(
                    title = "Configurar impresora",
                    message = "Detectamos que escaneaste el QR de una impresora. ¿Deseas configurarla ahora?",
                    show = showPendingPrinterQrDialog,
                    confirmText = "Configurar",
                    dismissText = "Ahora no",
                    onConfirm = {
                        localStorage.deleteObject(PrinterQrEntry.KEY_PENDING_ONBOARDING)
                        showPendingPrinterQrDialog = false
                        navController.navigate(
                            PrinterOnboardingRoute(
                                entryContext = PrinterEntryContext.SETTINGS.name,
                                fromQr = true
                            )
                        ) {
                            launchSingleTop = true
                        }
                    },
                    onDismiss = {
                        localStorage.deleteObject(PrinterQrEntry.KEY_PENDING_ONBOARDING)
                        showPendingPrinterQrDialog = false
                    }
                )
            }
        }

    }



}
