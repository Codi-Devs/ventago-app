package com.teco.ventago

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shop
import androidx.compose.material.icons.outlined.ShoppingCartCheckout
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ShoppingCartCheckout
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.teco.ventago.core.SnackbarService
import com.teco.ventago.core.firebase.AnalyticsService
import com.teco.ventago.design_system.molecules.AppChromeState
import com.teco.ventago.design_system.molecules.ColoredTopBarHost
import com.teco.ventago.design_system.molecules.DMTopAppBar
import com.teco.ventago.design_system.molecules.rememberAppChromeState
import com.teco.ventago.design_system.theme.DigitalMenuTheme
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.features.pos.ui.viewmodel.FlowMode
import com.teco.ventago.features.pos.ui.viewmodel.PosState
import com.teco.ventago.features.pos.ui.viewmodel.PosViewModel
import com.teco.ventago.features.payments.ui.yappy.viewmodel.YappyViewModel
import com.teco.ventago.navigation.LocalNavController
import com.teco.ventago.navigation.Navigation
import com.teco.ventago.navigation.PosScreens
import com.teco.ventago.navigation.toPosScreenOrNull
import io.ktor.util.reflect.instanceOf
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.pos_new_quote

val LocalAppChrome = staticCompositionLocalOf<AppChromeState> {
    error("AppChromeState not provided")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App(
    appViewModel: AppViewModel = koinViewModel<AppViewModel>(),
    navController: NavHostController = rememberNavController()
) {

    val mainState by appViewModel.mainState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarService: SnackbarService = koinInject()
    snackbarService.hostState = snackbarHostState
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    println("ASDADS currentRoute: $currentRoute")
    val currentScreen = currentRoute?.toPosScreenOrNull() ?: PosScreens.LoginScreen
    println("ASDADS currentScreen: $currentScreen")
    val appChrome = rememberAppChromeState()

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

            println("ASDADS invoiceActive ${currentBucket.invoiceActive}")
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
                println("ASDADS navigating to $target")
                navController.navigate(target) {
                    // Clear the stack to the start of the graph (safe alternative to popUpTo(0))
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }
    DigitalMenuTheme {
        CompositionLocalProvider(
            LocalAppChrome provides appChrome,
            LocalNavController provides navController,
            ){
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
                    val posBackStackEntry = remember(currentScreen) {
                        if (isPosTitleScreen) {
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
                        if (isPosTitleScreen && posUiState.flowMode == FlowMode.QUOTE) {
                            stringResource(Res.string.pos_new_quote)
                        } else {
                            stringResource(currentScreen.title)
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
                            navigateBack = { if (navController.previousBackStackEntry != null) navController.navigateUp() },
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
                        // Get Yappy ViewModel state for top app bar back button interception
                        val paymentsBackStackEntry = remember(currentScreen) {
                            if (currentScreen == PosScreens.PaymentsYappyScreen) {
                                runCatching {
                                    navController.getBackStackEntry(PosScreens.Payments.name)
                                }.getOrNull()
                            } else {
                                null
                            }
                        }
                        
                        val yappyViewModel = if (paymentsBackStackEntry != null) {
                            koinViewModel<YappyViewModel>(viewModelStoreOwner = paymentsBackStackEntry)
                        } else {
                            null
                        }
                        
                        val yappyUiState by yappyViewModel?.uiState?.collectAsState() 
                            ?: remember { mutableStateOf(com.teco.ventago.features.payments.ui.yappy.viewmodel.YappyUiState()) }
                        
                        val yappyHelpNeeded = if (currentScreen == PosScreens.PaymentsYappyScreen && yappyViewModel != null) {
                            !yappyUiState.linkedYappyAccount && !yappyViewModel.canConfigureYappy()
                        } else {
                            false
                        }
                        
                        DMTopAppBar(
                            title = appBarTitle,
                            showBackButton = currentScreen.showAppBar && currentScreen.showBackButton &&navController.previousBackStackEntry != null,
                            navigateBack = {
                                if (navController.previousBackStackEntry != null) {
                                    // Special handling for YappyScreen - show help modal if not configured
                                    if (yappyHelpNeeded) {
                                        // Store flag to show help sheet in YappyScreen
                                        backStackEntry?.savedStateHandle?.set("show_yappy_help", true)
                                    } else {
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
                        if (currentScreen != PosScreens.HomeScreen
                            && currentScreen != PosScreens.CategoriesManageScreen
                            && currentScreen != PosScreens.SettingsScreen
                            && currentScreen != PosScreens.OrdersScreen)
                            return@Scaffold

                        NavigationBar(
                            modifier = Modifier.fillMaxWidth().shadow(
                                elevation = 10.dp,
                                shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                            ),
                            containerColor = cardContainerColor(),
                            tonalElevation = 8.dp,

                            ) {
                            NavigationBarItem(
                                selected = currentScreen == PosScreens.HomeScreen,
                                onClick = {
                                    if (currentScreen != PosScreens.HomeScreen) {
                                        navController.navigate(PosScreens.HomeScreen.name)
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (currentScreen == PosScreens.HomeScreen)
                                            Icons.Filled.Home else Icons.Outlined.Home,
                                        contentDescription = "Home"
                                    )
                                },
                                label = {
                                    Text("Home")
                                }
                            )
                            NavigationBarItem(
                                selected = currentScreen == PosScreens.OrdersScreen,
                                onClick = {
                                    if (currentScreen != PosScreens.OrdersScreen) {
                                        navController.navigate(PosScreens.Orders.name)
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (currentScreen == PosScreens.OrdersScreen)
                                            Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong,
                                        contentDescription = "Home"
                                    )
                                },
                                label = {
                                    Text("Facturas")
                                }
                            )
                            NavigationBarItem(
                                selected = currentScreen == PosScreens.CategoriesManageScreen,
                                onClick = {
                                    if (currentScreen != PosScreens.CategoriesManageScreen) {
                                        navController.navigate(PosScreens.ProductsManage.name)
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (currentScreen == PosScreens.CategoriesManageScreen)
                                            Icons.Filled.Inventory else Icons.Outlined.Inventory,
                                        contentDescription = "Productos"
                                    )
                                },
                                label = {
                                    Text("Productos")
                                }
                            )
                            NavigationBarItem(
                                selected = currentScreen == PosScreens.SettingsScreen,
                                onClick = {
                                    if (currentScreen != PosScreens.SettingsScreen) {
                                        navController.navigate(PosScreens.Settings.name)
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (currentScreen == PosScreens.SettingsScreen)
                                            Icons.Filled.Settings else Icons.Outlined.Settings,
                                        contentDescription = "Settings"
                                    )
                                },
                                label = {
                                    Text("Opciones")
                                }
                            )

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
        }

    }



}
