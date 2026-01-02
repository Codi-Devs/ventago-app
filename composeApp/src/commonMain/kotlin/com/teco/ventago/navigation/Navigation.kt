package com.teco.ventago.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.NavUri
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.teco.ventago.AppViewModel
import com.teco.ventago.Greetings
import com.teco.ventago.core.deeplink.ExternalUriHandler
import com.teco.ventago.core.firebase.AnalyticsService
import com.teco.ventago.design_system.organism.ItemScreenActions
import com.teco.ventago.features.auth.ui.invoice_landing.InvoiceLandingScreen
import com.teco.ventago.features.auth.ui.login.ForgotPasswordResultScreen
import com.teco.ventago.features.auth.ui.login.ForgotPasswordScreen
import com.teco.ventago.features.auth.ui.login.LoginScreen
import com.teco.ventago.features.auth.ui.register.business.BusinessRegisterScreen
import com.teco.ventago.features.auth.ui.register.user.RegisterScreen
import com.teco.ventago.features.branches.ui.billing_point.add.AddBillingPointScreen
import com.teco.ventago.features.branches.ui.billing_point.add.viewmodel.AddBillingPointViewModel
import com.teco.ventago.features.branches.ui.billing_point.edit.EditBillingPointScreen
import com.teco.ventago.features.branches.ui.billing_point.edit.viewmodel.EditBillingPointViewModel
import com.teco.ventago.features.branches.ui.billing_point.manage.BillingPointManageActions
import com.teco.ventago.features.branches.ui.billing_point.manage.BillingPointManageScreen
import com.teco.ventago.features.branches.ui.billing_point.manage.viewmodel.BillingPointsManageViewModel
import com.teco.ventago.features.branches.ui.branches.manage.BranchesManageScreen
import com.teco.ventago.features.customers.data.provider.json
import com.teco.ventago.features.customers.domain.models.CustomerListItem
import com.teco.ventago.features.home.ui.HomeScreen
import com.teco.ventago.features.invoicing.ui.InvoicingLandingScreen
import com.teco.ventago.features.orders.domain.OrderService
import com.teco.ventago.features.orders.domain.models.CustomerSnapshot
import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.orders.ui.order_invoice.OrderInvoiceActions
import com.teco.ventago.features.orders.ui.order_details.OrderDetailsScreen
import com.teco.ventago.features.orders.ui.order_history.OrderHistoryScreen
import com.teco.ventago.features.orders.ui.order_invoice.OrderInvoiceContent
import com.teco.ventago.features.orders.ui.orders.OrdersScreen
import com.teco.ventago.features.orders.ui.orders.OrdersScreenActions
import com.teco.ventago.features.orders.ui.order_details.OrderDetailsActions
import com.teco.ventago.features.orders.ui.order_details.viewModel.OrdersDetailsViewModel
import com.teco.ventago.features.orders.ui.order_history.viewModel.OrderHistoryViewModel
import com.teco.ventago.features.orders.ui.order_invoice.viewModel.OrderInvoiceViewModel
import com.teco.ventago.features.orders.ui.orders.viewmodel.OrdersViewModel
import com.teco.ventago.features.payments.ui.home.OnboardingPaymentScreen
import com.teco.ventago.features.payments.ui.home.viewmodel.PaymentMethodsViewModel
import com.teco.ventago.features.payments.ui.paypal.OnboardingPaypalScreen
import com.teco.ventago.features.payments.ui.paypal.PaypalScreenContent
import com.teco.ventago.features.payments.ui.paypal.viewmodel.PaypalViewModel
import com.teco.ventago.features.payments.ui.transference.TransferenceScreenView
import com.teco.ventago.features.payments.ui.yappy.YappyScreenView
import com.teco.ventago.features.payments.ui.yappy.viewmodel.YappyViewModel
import com.teco.ventago.features.pos.ui.CartScreen
import com.teco.ventago.features.pos.ui.CartScreenBottomBar
import com.teco.ventago.features.pos.ui.PaymentScreen
import com.teco.ventago.features.pos.ui.PosInvoiceContent
import com.teco.ventago.features.pos.ui.PosProductScreen
import com.teco.ventago.features.pos.ui.PosProductScreenBottomBar
import com.teco.ventago.features.pos.ui.PosScreen
import com.teco.ventago.features.pos.ui.SuccessScreen
import com.teco.ventago.features.pos.ui.customer.add.AddCustomerScreen
import com.teco.ventago.features.pos.ui.customer.list.ClientListActions
import com.teco.ventago.features.pos.ui.customer.list.ClientListScreen
import com.teco.ventago.features.pos.ui.customer.search.SearchCustomerView
import com.teco.ventago.features.pos.ui.viewmodel.PosViewModel
import com.teco.ventago.features.product.ui.category.add.AddCategoryScreen
import com.teco.ventago.features.product.ui.category.add.ModifyCategoryScreen
import com.teco.ventago.features.product.ui.category.edit.EditCategoryActions
import com.teco.ventago.features.product.ui.category.edit.EditCategoryScreen
import com.teco.ventago.features.product.ui.category.manage.CategoriesManageActions
import com.teco.ventago.features.product.ui.category.manage.CategoriesManageScreen
import com.teco.ventago.features.pos.domain.models.Tax
import com.teco.ventago.features.product.domain.model.Item
import com.teco.ventago.features.product.ui.item.add.AddItemScreen
import com.teco.ventago.features.product.ui.item.add.AddItemScreenActions
import com.teco.ventago.features.product.ui.item.edit.EditItemScreen
import com.teco.ventago.features.settings.ui.address.SetBusinessAddressScreen
import com.teco.ventago.features.settings.ui.address.viewmodel.SetAddressViewModel
import com.teco.ventago.features.settings.ui.logo.ChangeBusinessImageContent
import com.teco.ventago.features.settings.ui.logo.viewmodel.ChangeImageViewModel
import com.teco.ventago.features.settings.ui.name.ChangeNameContent
import com.teco.ventago.features.settings.ui.name.viewmodel.ChangeNameViewModel
import com.teco.ventago.features.settings.ui.settings.SettingsScreen
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.StringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.action_settings
import ventago.composeapp.generated.resources.add_address
import ventago.composeapp.generated.resources.add_image
import ventago.composeapp.generated.resources.add_new_category
import ventago.composeapp.generated.resources.bank_transfer
import ventago.composeapp.generated.resources.billing_points
import ventago.composeapp.generated.resources.branch
import ventago.composeapp.generated.resources.branches
import ventago.composeapp.generated.resources.categories
import ventago.composeapp.generated.resources.change_business_name
import ventago.composeapp.generated.resources.edit
import ventago.composeapp.generated.resources.home
import ventago.composeapp.generated.resources.invoice_title
import ventago.composeapp.generated.resources.invoicing
import ventago.composeapp.generated.resources.invoicing_landing_title
import ventago.composeapp.generated.resources.items
import ventago.composeapp.generated.resources.login
import ventago.composeapp.generated.resources.order_details
import ventago.composeapp.generated.resources.order_status_history
import ventago.composeapp.generated.resources.orders
import ventago.composeapp.generated.resources.payments
import ventago.composeapp.generated.resources.paypal
import ventago.composeapp.generated.resources.pos
import ventago.composeapp.generated.resources.pos_cart
import ventago.composeapp.generated.resources.pos_clients
import ventago.composeapp.generated.resources.pos_invoice
import ventago.composeapp.generated.resources.pos_payment
import ventago.composeapp.generated.resources.register
import ventago.composeapp.generated.resources.register_business
import ventago.composeapp.generated.resources.reset_password
import ventago.composeapp.generated.resources.search
import ventago.composeapp.generated.resources.yappy

object NavResults {
    const val KEY_SELECTED_CUSTOMER = "selectedCustomer"
    const val KEY_IS_PERSONALIZED_PRODUCT = "isPersonalizedProduct"
    const val KEY_PERSONALIZED_PRODUCT = "personalizedProduct"
}

val LocalNavController = staticCompositionLocalOf<NavHostController> {
    error("No NavController provided")
}

enum class PosScreens(
    val title: StringResource,
    val showAppBar: Boolean = true,
    val showBackButton: Boolean = true,
    val actions: @Composable (NavBackStackEntry?, (PosScreens) -> Unit, (Any) -> Unit) -> Unit = { _, _, _ -> },
    val bottomBar: @Composable ((NavBackStackEntry?, (PosScreens) -> Unit) -> Unit)? = null,
) {

    // AUTH Screens
    LoginRegister(Res.string.login, false), LoginScreen(
        Res.string.login,
        false
    ),
    RegisterScreen(Res.string.register, false),
    BusinessRegisterScreen(Res.string.register_business, false),
    InvoiceLandingScreen(Res.string.invoice_title, false),
    ForgotPasswordScreen(Res.string.reset_password), ForgotPasswordResultScreen(
        Res.string.reset_password,
        true
    ),
    Greetings(Res.string.pos), HomeScreen(Res.string.home, false, showBackButton = false),

    //    Products Screens
    ProductsManage(Res.string.categories, false), CategoriesManageScreen(
        Res.string.categories,
        true,
        showBackButton = false,
        actions = { _, navigate, _ -> CategoriesManageActions(navigate) }),
    AddCategoryScreen(Res.string.add_new_category), EditCategoryScreen(
        Res.string.edit, true, actions = { _, navigate, _ -> EditCategoryActions(navigate) }),
    ModifyCategoryScreen(Res.string.edit, true), AddItemScreen(
        Res.string.items,
        showAppBar = true,
        actions = { backStackEntry, _, _ -> AddItemScreenActions(backStackEntry) }),
    EditItemScreen(
        Res.string.items,
        true,
        actions = { backStackEntry, _, _ -> ItemScreenActions(backStackEntry) }),

    // POS Screens
    POS(Res.string.pos),

    POSScreen(Res.string.pos),
    POSProductScreen(
        Res.string.pos,
        bottomBar = { backStackEntry, navigate -> PosProductScreenBottomBar(backStackEntry, navigate) }),
    CartScreen(
        Res.string.pos_cart,
        bottomBar = { backStackEntry, navigate -> CartScreenBottomBar(backStackEntry, navigate) }),
    PaymentScreen(
        Res.string.pos_payment
    ),
    SuccessScreen(Res.string.pos, false), PosInvoiceScreen(
        Res.string.invoice_title,
        true
    ),
    CustomersScreen(
        Res.string.pos_clients,
        actions = { backStackEntry, navigate, _ -> ClientListActions(backStackEntry, navigate) }),

    SearchCustomerScreen(
        Res.string.search,
        actions = { backStackEntry, navigate, _ -> ClientListActions(backStackEntry, navigate) }),

    AddCustomerScreen(Res.string.pos_clients),


    // Payments Screens
    Payments(Res.string.payments), PaymentsHomeScreen(
        Res.string.payments,
        true
    ),
    PaymentsTransferenceScreen(
        Res.string.bank_transfer,
        true
    ),
    PaymentsPaypalOnboardingScreen(Res.string.paypal, true), PaymentsPaypalScreen(
        Res.string.paypal,
        true
    ),
    PaymentsYappyScreen(Res.string.yappy, true),

    // Orders Screens
    Orders(Res.string.orders),

    OrdersScreen(
        Res.string.orders,
        true,
        actions = { backStackEntry, navigate, _ ->
            OrdersScreenActions(backStackEntry)
        }),
    OrderDetailsScreen(
        Res.string.order_details, true,
        actions = { backStackEntry, _, navigateAny -> OrderDetailsActions(backStackEntry, navigateAny) },
    ),
    OrderHistoryScreen(Res.string.order_status_history, true), OrderInvoiceScreen(
        Res.string.pos_invoice, true,
        actions = { backStackEntry, navigate, _ -> OrderInvoiceActions(backStackEntry) },
    ),


    // Invoicing Screens
    Invoicing(Res.string.invoicing), InvoicingLandingScreen(
        Res.string.invoicing_landing_title, showAppBar = false, showBackButton = true
    ),

    // Settings Screens
    Settings(Res.string.action_settings), SettingsScreen(Res.string.action_settings), BusinessLogoSettingsScreen(
        Res.string.add_image
    ),

    ChangeBusinessNameScreen(
        Res.string.change_business_name
    ),
    BusinessAddressSettingsScreen(Res.string.add_address),

    //Branches Screens
    Branches(Res.string.branch),
    BranchesManageScreen(Res.string.branches),
    BillingPointManageScreen(
        Res.string.billing_points,
        actions = { backStackEntry, _, navigate ->
            BillingPointManageActions(
                backStackEntry
            ) { routeObj -> navigate(routeObj)}
        }),
    AddBillingPointScreen(Res.string.billing_points),
    EditBillingPointScreen(Res.string.billing_points);


    fun isPosScreens(): Boolean {
        return this == POSProductScreen || this == CartScreen || this == PaymentScreen || this == SuccessScreen || this == CustomersScreen || this == AddCustomerScreen || this == PosInvoiceScreen
    }
}

@Composable
fun Navigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    appViewModel: AppViewModel,
    analyticsService: AnalyticsService
) {
    DisposableEffect(Unit) {
        ExternalUriHandler.listener = { uri ->
            navController.navigate(NavUri(uri))
        }
        onDispose { ExternalUriHandler.listener = null }
    }

    NavHost(
        navController = navController,
        startDestination = PosScreens.LoginRegister.name,
        modifier = modifier,
        enterTransition = NavTransitions.enterTransition,
        exitTransition = NavTransitions.exitTransition,
        popEnterTransition = NavTransitions.popEnterTransition,
        popExitTransition = NavTransitions.popExitTransition
    ) {
        addLoginNavigation(navController, analyticsService)

        composable(route = PosScreens.HomeScreen.name) {
            HomeScreen(
                appViewModel = appViewModel
            ) {
                navController.navigate(it.name)
            }
        }

        composable(route = PosScreens.Greetings.name) {
            Greetings()
        }

        addProductsNavigation(navController, analyticsService)

        addPOSNavigation(navController, appViewModel, analyticsService)

        addPaymentsNavigation(navController, appViewModel, analyticsService)

        addOrdersNavigation(navController, analyticsService)

        addSettingsNavigation(navController, analyticsService)

        addInvoicingNavigation(navController, analyticsService)

        addBranchesNavigation(navController, analyticsService)
    }
}

private fun NavGraphBuilder.addLoginNavigation(
    navController: NavHostController, analyticsService: AnalyticsService
) {
    navigation(
        route = PosScreens.LoginRegister.name, startDestination = PosScreens.LoginScreen.name
    ) {
        composable(route = PosScreens.LoginScreen.name) {
            analyticsService.logScreenView("LoginScreen")
            LoginScreen { route ->
                navController.navigate(route.name)
            }
        }

        composable(route = PosScreens.BusinessRegisterScreen.name) {
            analyticsService.logScreenView("BusinessRegisterScreen")
            BusinessRegisterScreen { route ->
                navController.navigate(route.name)
            }
        }

        composable(route = PosScreens.InvoiceLandingScreen.name) {
            analyticsService.logScreenView("InvoiceLandingScreen")
            InvoiceLandingScreen()
        }

        composable(route = PosScreens.RegisterScreen.name) {
            analyticsService.logScreenView("RegisterScreen")
            RegisterScreen({ route ->
                navController.navigate(route.name)
            }, { navController.navigateUp() })
        }

        composable(route = PosScreens.ForgotPasswordScreen.name) {
            analyticsService.logScreenView("ForgotPasswordScreen")
            ForgotPasswordScreen { route ->
                navController.navigate(route.name) {
                    popUpTo(PosScreens.LoginScreen.name) { inclusive = false }
                }
            }
        }

        composable(route = PosScreens.ForgotPasswordResultScreen.name) {
            analyticsService.logScreenView("ForgotPasswordResultScreen")
            ForgotPasswordResultScreen { route ->
                navController.navigate(route.name)
            }
        }
    }
}

private fun NavGraphBuilder.addProductsNavigation(
    navController: NavHostController, analyticsService: AnalyticsService
) {
    navigation(
        route = PosScreens.ProductsManage.name,
        startDestination = PosScreens.CategoriesManageScreen.name
    ) {
        composable(route = PosScreens.CategoriesManageScreen.name) {
            analyticsService.logScreenView("CategoriesManageScreen")
            CategoriesManageScreen { route ->
                navController.navigate(route.name)
            }
        }

        composable(route = PosScreens.AddCategoryScreen.name) {
            analyticsService.logScreenView("AddCategoryScreen")
            AddCategoryScreen {
                navController.navigateUp()
            }
        }

        composable(route = PosScreens.EditCategoryScreen.name) {
            analyticsService.logScreenView("EditCategoryScreen")
            EditCategoryScreen(navigateBack = { navController.navigateUp() }) { route ->
                navController.navigate(route.name)
            }
        }

        composable(route = PosScreens.ModifyCategoryScreen.name) {
            analyticsService.logScreenView("ModifyCategoryScreen")
            ModifyCategoryScreen(navigateBack = { navController.navigateUp() })
        }

        composable(route = PosScreens.AddItemScreen.name) { backStackEntry ->
            analyticsService.logScreenView("AddItemScreen")
            
            // Check if we're coming from POS - if previous destination is POSProductScreen, set the flag
            LaunchedEffect(backStackEntry) {
                val previousRoute = navController.previousBackStackEntry?.destination?.route
                if (previousRoute == PosScreens.POSProductScreen.name) {
                    backStackEntry.savedStateHandle[NavResults.KEY_IS_PERSONALIZED_PRODUCT] = true
                }
            }
            
            // Handle personalized product return - store in both POS graph and ProductsManage graph, then navigate back
            val productsGraphEntry = remember { 
                runCatching { navController.getBackStackEntry(PosScreens.ProductsManage.name) }.getOrNull()
            }
            
            val posGraphEntryForProduct = remember {
                runCatching { navController.getBackStackEntry(PosScreens.POS.name) }.getOrNull()
            }
            
            val personalizedProductJson by backStackEntry
                .savedStateHandle
                .getStateFlow<String?>(NavResults.KEY_PERSONALIZED_PRODUCT, null)
                .collectAsState()
            
            LaunchedEffect(personalizedProductJson) {
                println("ASDASD: $personalizedProductJson")
                personalizedProductJson?.let { json ->
                    // Store in POS graph's savedStateHandle (primary - always accessible)
                    posGraphEntryForProduct?.let { entry ->
                        entry.savedStateHandle[NavResults.KEY_PERSONALIZED_PRODUCT] = json
                    }
                    // Also store in ProductsManage graph's savedStateHandle (fallback)
                    productsGraphEntry?.let { entry ->
                        entry.savedStateHandle[NavResults.KEY_PERSONALIZED_PRODUCT] = json
                    }
                    // Clear from AddItemScreen's savedStateHandle
                    backStackEntry.savedStateHandle[NavResults.KEY_PERSONALIZED_PRODUCT] = null
                    // Navigate back to POS
                    navController.popBackStack()
                }
            }
            
            AddItemScreen(backStackEntry = backStackEntry) {
                navController.navigateUp()
            }
        }

        composable(route = PosScreens.EditItemScreen.name) {
            analyticsService.logScreenView("EditItemScreen")
            EditItemScreen {
                navController.navigateUp()
            }
        }

    }
}

private fun NavGraphBuilder.addPOSNavigation(
    navController: NavHostController, appViewModel: AppViewModel, analyticsService: AnalyticsService
) {
    navigation(
        route = PosScreens.POS.name, startDestination = PosScreens.POSScreen.name
    ) {
        composable(route = PosScreens.POSScreen.name) {
            val backStackEntry = remember { navController.getBackStackEntry(PosScreens.POS.name) }
            val viewModel: PosViewModel = koinViewModel(viewModelStoreOwner = backStackEntry)
            analyticsService.logScreenView("POSScreen")

            val posGraphEntry = remember { backStackEntry }

            posGraphEntry.let {
                val selectedCustomerJson by posGraphEntry
                    .savedStateHandle
                    .getStateFlow<String?>(NavResults.KEY_SELECTED_CUSTOMER, null)
                    .collectAsState()

                // When it changes, consume it and set in VM
                LaunchedEffect(selectedCustomerJson) {
                    selectedCustomerJson?.let { json ->
                        val customer = Json.decodeFromString<CustomerListItem>(json)
                        viewModel.selectCustomer(customer)              // <-- your VM update
                        posGraphEntry.savedStateHandle[NavResults.KEY_SELECTED_CUSTOMER] =
                            null // consume
                    }
                }
            }

            PosScreen(viewModel) { route ->
                navController.navigate(route.name)
            }
        }

        // NEW: typed note route
        composable<PosNoteRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<PosNoteRoute>()

            val backStackEntry = remember { navController.getBackStackEntry(PosScreens.POS.name) }
            val viewModel: PosViewModel = koinViewModel(viewModelStoreOwner = backStackEntry)
            analyticsService.logScreenView("POSScreen(Note)")

            // Seed VM once
//            LaunchedEffect(args.op, args.cufe, args.createdAt) {
//
//            }

            val posGraphEntry = remember { backStackEntry }

            posGraphEntry.let {
                val selectedCustomerJson by posGraphEntry
                    .savedStateHandle
                    .getStateFlow<String?>(NavResults.KEY_SELECTED_CUSTOMER, null)
                    .collectAsState()

                // When it changes, consume it and set in VM
                LaunchedEffect(selectedCustomerJson) {
                    selectedCustomerJson?.let { json ->
                        val customer = Json.decodeFromString<CustomerListItem>(json)
                        viewModel.selectCustomer(customer)              // <-- your VM update
                        posGraphEntry.savedStateHandle[NavResults.KEY_SELECTED_CUSTOMER] =
                            null // consume
                    }
                }
            }

            viewModel.startNoteFromInvoice(args)

            // Render the normal POS screen
            PosScreen(viewModel) { route ->
                navController.navigate(route.name)
            }
        }

        composable(route = PosScreens.POSProductScreen.name) {
            val backStackEntry = remember { navController.getBackStackEntry(PosScreens.POS.name) }
            val viewModel: PosViewModel = koinViewModel(viewModelStoreOwner = backStackEntry)
            analyticsService.logScreenView("POSScreen")

            val posGraphEntry = remember { backStackEntry }

            posGraphEntry.let {
                println("ASDASD: Entered posGraphEntry")
                val selectedCustomerJson by posGraphEntry
                    .savedStateHandle
                    .getStateFlow<String?>(NavResults.KEY_SELECTED_CUSTOMER, null)
                    .collectAsState()

                // When it changes, consume it and set in VM
                LaunchedEffect(selectedCustomerJson) {
                    selectedCustomerJson?.let { json ->
                        val customer = Json.decodeFromString<CustomerListItem>(json)
                        viewModel.selectCustomer(customer)              // <-- your VM update
                        posGraphEntry.savedStateHandle[NavResults.KEY_SELECTED_CUSTOMER] =
                            null // consume
                    }
                }

                // Handle personalized product return from AddItemScreen
                // Check POS graph's savedStateHandle first (since we know it exists)
                // Also try to get from ProductsManage graph as fallback
                val personalizedProductFromPos by posGraphEntry
                    .savedStateHandle
                    .getStateFlow<String?>(NavResults.KEY_PERSONALIZED_PRODUCT, null)
                    .collectAsState()
                
                val productsGraphEntry = remember { 
                    runCatching { navController.getBackStackEntry(PosScreens.ProductsManage.name) }.getOrNull()
                }
                
                val personalizedProductFromProducts by remember(productsGraphEntry) {
                    productsGraphEntry?.savedStateHandle
                        ?.getStateFlow<String?>(NavResults.KEY_PERSONALIZED_PRODUCT, null)
                        ?: kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
                }.collectAsState()

                // Use the first non-null value
                val personalizedProductJson = personalizedProductFromPos ?: personalizedProductFromProducts

                println("ASDASD: personalizedProductFromPos = $personalizedProductFromPos")
                println("ASDASD: personalizedProductFromProducts = $personalizedProductFromProducts")
                println("ASDASD: personalizedProductJson = $personalizedProductJson")

                LaunchedEffect(personalizedProductJson) {
                    personalizedProductJson?.let { json ->
                        println("ASDASD: Processing personalized product: $json")
                        val item = Json.decodeFromString<Item>(json)
                        // Add to cart with tax
                        viewModel.addItemToCart(
                            item, 
                            tax = item.taxPercent?.let { tax ->
                                Tax(
                                    id = tax, 
                                    name = "$tax", 
                                    rateBps = tax * 100
                                )
                            }
                        )
                        // Clear from both locations
                        posGraphEntry.savedStateHandle[NavResults.KEY_PERSONALIZED_PRODUCT] = null
                        productsGraphEntry?.savedStateHandle?.set(NavResults.KEY_PERSONALIZED_PRODUCT, null)
                    }
                }
            }

            PosProductScreen(viewModel) { route ->
                navController.navigate(route.name)
            }
        }

        composable(route = PosScreens.CartScreen.name) {
            val backStackEntry = remember { navController.getBackStackEntry(PosScreens.POS.name) }
            val viewModel: PosViewModel = koinViewModel(viewModelStoreOwner = backStackEntry)
            analyticsService.logScreenView("CartScreen")
            CartScreen(viewModel) { route ->
                navController.navigate(route.name)
            }
        }

        composable(route = PosScreens.PaymentScreen.name) {
            val backStackEntry = remember { navController.getBackStackEntry(PosScreens.POS.name) }
            val viewModel: PosViewModel = koinViewModel(viewModelStoreOwner = backStackEntry)
            analyticsService.logScreenView("PaymentScreen")
            PaymentScreen(appViewModel, viewModel) { route, builder ->
                navController.navigate(route, builder)
            }
        }

        composable(route = PosScreens.SuccessScreen.name) {
            val backStackEntry = remember { navController.getBackStackEntry(PosScreens.POS.name) }
            val viewModel: PosViewModel = koinViewModel(viewModelStoreOwner = backStackEntry)
            analyticsService.logScreenView("SuccessScreen")
            SuccessScreen(viewModel, navController) { route, builder ->
                navController.navigate(route, builder)
            }
        }

        composable(route = PosScreens.PosInvoiceScreen.name) {
            val backStackEntry = remember { navController.getBackStackEntry(PosScreens.POS.name) }
            val viewModel: PosViewModel = koinViewModel(viewModelStoreOwner = backStackEntry)
            analyticsService.logScreenView("PosInvoiceScreen")
            PosInvoiceContent(appViewModel, viewModel)
        }

        composable(route = PosScreens.SearchCustomerScreen.name) {
            SearchCustomerView {
                navController.navigate(PosScreens.CustomersScreen.name)
            }

        }

        composable(route = PosScreens.CustomersScreen.name) {
//            val backStackEntry = remember { navController.getBackStackEntry(PosScreens.POS.name) }
//            val viewModel: PosViewModel = koinViewModel(viewModelStoreOwner = backStackEntry)
            analyticsService.logScreenView("ClientsScreen")
            ClientListScreen(
                onCustomerSelected = { customer ->
                    val payload = Json.encodeToString(customer)
                    val posEntry = navController.getBackStackEntry(PosScreens.POS.name)
                    posEntry.savedStateHandle[NavResults.KEY_SELECTED_CUSTOMER] = payload
                    // Go back to POS

                    val hasPosScreen = runCatching { navController.getBackStackEntry(PosScreens.POSScreen.name) }.isSuccess

                    if (hasPosScreen) {
                        navController.popBackStack(
                            route = PosScreens.POSScreen.name, // 👈 your actual POS screen route
                            inclusive = false, // keep the POS itself
                            saveState = false
                        )
                    } else {
                        var guard = 0
                        while (navController.currentDestination?.route?.contains("pos_note") == false) {
                            val ok = navController.navigateUp()
                            if (!ok) return@ClientListScreen // can't go further
                            guard++
                            if (guard > 100) return@ClientListScreen // can't go further
                        }
//                        navController.popBackStack(
//                            route = "pos_note", // 👈 your actual POS screen route
//                            inclusive = false, // keep the POS itself
//                            saveState = false
//                        )
                    }
                }
            ) {
                navController.navigateUp()
            }
        }

        composable(route = PosScreens.AddCustomerScreen.name) {
//            val backStackEntry = remember { navController.getBackStackEntry(PosScreens.POS.name) }
//            val viewModel: PosViewModel = koinViewModel(viewModelStoreOwner = backStackEntry)
            analyticsService.logScreenView("AddClientsScrees")
//            AddClientScreen(viewModel) {
//                navController.navigateUp()
//            }

            AddCustomerScreen {
                navController.navigateUp()
            }
        }


    }
}

private fun NavGraphBuilder.addPaymentsNavigation(
    navController: NavHostController, appViewModel: AppViewModel, analyticsService: AnalyticsService
) {
    navigation(
        route = PosScreens.Payments.name, startDestination = PosScreens.PaymentsHomeScreen.name
    ) {

        composable(route = PosScreens.PaymentsHomeScreen.name) {
            val backStackEntry =
                remember { navController.getBackStackEntry(PosScreens.Payments.name) }
            val viewModel: PaymentMethodsViewModel =
                koinViewModel(viewModelStoreOwner = backStackEntry)
            analyticsService.logScreenView("PaymentsHomeScreen")
            OnboardingPaymentScreen(viewModel) { route ->
                navController.navigate(route.name)
            }
        }

        composable(route = PosScreens.PaymentsTransferenceScreen.name) {
            val backStackEntry =
                remember { navController.getBackStackEntry(PosScreens.Payments.name) }
            val viewModel: PaymentMethodsViewModel =
                koinViewModel(viewModelStoreOwner = backStackEntry)
            analyticsService.logScreenView("PaymentsTransferenceScreen")
            TransferenceScreenView(viewModel)
        }

        composable(route = PosScreens.PaymentsPaypalOnboardingScreen.name) {
            val backStackEntry =
                remember { navController.getBackStackEntry(PosScreens.Payments.name) }
            val viewModel: PaypalViewModel = koinViewModel(viewModelStoreOwner = backStackEntry)
            analyticsService.logScreenView("PaymentsPaypalOnboardingScreen")
            OnboardingPaypalScreen(viewModel)
        }

        composable(route = PosScreens.PaymentsPaypalScreen.name) {
            val backStackEntry =
                remember { navController.getBackStackEntry(PosScreens.Payments.name) }
            val viewModel: PaypalViewModel = koinViewModel(viewModelStoreOwner = backStackEntry)
            analyticsService.logScreenView("PaymentsPaypalScreen")
            PaypalScreenContent(viewModel)
        }

        composable(route = PosScreens.PaymentsYappyScreen.name) { yappyBackStackEntry ->
            val backStackEntry =
                remember { navController.getBackStackEntry(PosScreens.Payments.name) }
            val viewModel: YappyViewModel = koinViewModel(viewModelStoreOwner = backStackEntry)
            analyticsService.logScreenView("PaymentsYappyScreen")
            
            // Watch for flag from top app bar back button
            val showYappyHelpFlag by yappyBackStackEntry
                .savedStateHandle
                .getStateFlow<Boolean?>("show_yappy_help", null)
                .collectAsState()
            
            LaunchedEffect(showYappyHelpFlag) {
                if (showYappyHelpFlag == true) {
                    // The flag will be handled by YappyScreenView
                    // We'll pass it as a parameter or use a callback
                    yappyBackStackEntry.savedStateHandle["show_yappy_help"] = null
                }
            }
            
            YappyScreenView(
                viewModel = viewModel,
                showHelpFromTopBar = showYappyHelpFlag == true,
                navigateBack = {
                    navController.navigateUp()
                }
            )
        }
    }
}

private fun NavGraphBuilder.addOrdersNavigation(
    navController: NavHostController, analyticsService: AnalyticsService
) {
    navigation(
        route = PosScreens.Orders.name, startDestination = PosScreens.OrdersScreen.name
    ) {

        composable(route = PosScreens.OrdersScreen.name) { backStackEntry ->
//            val ordersOwner = rememberSafeGraphOwner(
//                navController = navController,
//                graphRoute = PosScreens.Orders.name,
//                fallback = backStackEntry
//            )
            val ordersOwner = rememberGraphOwner(navController, PosScreens.Orders.name)
            val viewModel: OrdersViewModel = koinViewModel(viewModelStoreOwner = ordersOwner)

            analyticsService.logScreenView("OrdersScreen")
            OrdersScreen(viewModel) { route, builder ->
                navController.navigate(route, builder)
            }
        }

        composable<OrdersScreenRoute>(
            deepLinks = listOf(
                // Generated pattern for typed route (orderNumber is optional → query param)
                navDeepLink<OrdersScreenRoute>(basePath = "https://tecodigi.com/orders"),
                // Explicit pattern (handy if you want to be super clear)
                navDeepLink { uriPattern = "https://tecodigi.com/orders?orderNumber={orderNumber}" }
            )
        ) { backStackEntry ->
            val args = backStackEntry.toRoute<OrdersScreenRoute>()
            val orderNumber = args.orderNumber

//            val ordersOwner = rememberSafeGraphOwner(
//                navController = navController,
//                graphRoute = PosScreens.Orders.name,
//                fallback = backStackEntry
//            )
            val ordersOwner = rememberGraphOwner(navController, PosScreens.Orders.name)
            val viewModel: OrdersViewModel = koinViewModel(viewModelStoreOwner = ordersOwner)

            analyticsService.logScreenView("OrdersScreen")

            if (!orderNumber.isNullOrEmpty()) {
                viewModel.findOrderByOrderNumber(orderNumber)
            }

            OrdersScreen(viewModel) { route, builder ->
                navController.navigate(route, builder)
            }
        }

        composable(route = PosScreens.OrderDetailsScreen.name) { backStackEntry ->
            val ordersOwner = rememberGraphOwner(navController, PosScreens.Orders.name)
            val viewModel: OrdersDetailsViewModel = koinViewModel(viewModelStoreOwner = ordersOwner)
            analyticsService.logScreenView("OrderDetailsScreen")
            OrderDetailsScreen(viewModel) { route, builder ->
                navController.navigate(route, builder)
            }
        }

        composable(route = PosScreens.OrderInvoiceScreen.name) {
            val ordersOwner = rememberGraphOwner(navController, PosScreens.Orders.name)
            val viewModel: OrderInvoiceViewModel = koinViewModel(viewModelStoreOwner = ordersOwner)
            analyticsService.logScreenView("OrderInvoiceScreen")
            OrderInvoiceContent(viewModel)
        }

        composable(route = PosScreens.OrderHistoryScreen.name) {
            val ordersOwner = rememberGraphOwner(navController, PosScreens.Orders.name)
            val viewModel: OrderHistoryViewModel = koinViewModel(viewModelStoreOwner = ordersOwner)
            analyticsService.logScreenView("OrderHistoryScreen")
            OrderHistoryScreen(viewModel) {
                navController.navigateUp()
            }
        }
    }
}

private fun NavGraphBuilder.addSettingsNavigation(
    navController: NavHostController, analyticsService: AnalyticsService
) {
    navigation(
        route = PosScreens.Settings.name, startDestination = PosScreens.SettingsScreen.name
    ) {

        composable(route = PosScreens.SettingsScreen.name) {
            analyticsService.logScreenView("SettingsScreen")
            SettingsScreen { route ->
                navController.navigate(route.name)
            }
        }

        composable(route = PosScreens.BusinessLogoSettingsScreen.name) {
            analyticsService.logScreenView("BusinessLogoSettingsScreen")
            ChangeBusinessImageContent(koinViewModel<ChangeImageViewModel>()) {
                navController.navigateUp()
            }
        }

        composable(route = PosScreens.ChangeBusinessNameScreen.name) {
            analyticsService.logScreenView("ChangeBusinessNameScreen")
            ChangeNameContent(koinViewModel<ChangeNameViewModel>())
        }

        composable(route = PosScreens.BusinessAddressSettingsScreen.name) {
            analyticsService.logScreenView("BusinessAddressSettingsScreen")
            SetBusinessAddressScreen(koinViewModel<SetAddressViewModel>())
        }
    }
}

private fun NavGraphBuilder.addInvoicingNavigation(
    navController: NavHostController, analyticsService: AnalyticsService
) {
    navigation(
        route = PosScreens.Invoicing.name, startDestination = PosScreens.InvoicingLandingScreen.name
    ) {
        composable(route = PosScreens.InvoicingLandingScreen.name) {
            analyticsService.logScreenView("InvoicingLandingScreen")
            InvoicingLandingScreen()
        }
    }
}

private fun NavGraphBuilder.addBranchesNavigation(
    navController: NavHostController, analyticsService: AnalyticsService
) {
    navigation(
        route = PosScreens.Branches.name, startDestination = PosScreens.BranchesManageScreen.name
    ) {
        composable(route = PosScreens.BranchesManageScreen.name) {
            analyticsService.logScreenView("BranchesManageScreen")
            BranchesManageScreen(navController = navController)
        }

        composable<BillingPointManageRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<BillingPointManageRoute>()
            val branchCode = args.branchCode

            analyticsService.logScreenView("BillingPointManageScreen")

            val viewModel: BillingPointsManageViewModel = koinViewModel(
                viewModelStoreOwner = backStackEntry,
                parameters = { parametersOf(branchCode) }
            )

            BillingPointManageScreen(viewModel) { route: Any ->
                navController.navigate(route)
            }
        }

        composable<AddBillingPointRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<AddBillingPointRoute>()
            val branchCode = args.branchCode

            analyticsService.logScreenView("AddBillingPointScreen")

            val viewModel: AddBillingPointViewModel = koinViewModel(
                parameters = { parametersOf(branchCode) }
            )
            AddBillingPointScreen(viewModel) { navController.navigateUp() }
        }

        composable<EditBillingPointRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<EditBillingPointRoute>()
            val branchCode = args.branchCode
            val billingCode = args.billingCode

            analyticsService.logScreenView("AddBillingPointScreen")

            val viewModel: EditBillingPointViewModel = koinViewModel(
                parameters = { parametersOf(branchCode, billingCode) }
            )
            EditBillingPointScreen(viewModel) { navController.navigateUp() }
        }
    }
}

fun NavController.navigate(route: PosScreens, builder: (NavOptionsBuilder.() -> Unit)? = null) {
    if (builder != null) {
        this.navigate(route.name, builder)
    } else {
        this.navigate(route.name)
    }
}

fun PosScreens.withArgs(vararg args: Pair<String, String>): String {
    val base = this.name
    val query = args.joinToString("&") { "${it.first}=${it.second}" }
    return "$base?$query"
}

fun String.toPosScreenOrNull(): PosScreens? {
    if (this.contains("OrdersScreenRoute")) return PosScreens.OrdersScreen
    if (this.contains("PosNoteRoute")) return PosScreens.POSScreen
    if (this.contains("pos_note")) return PosScreens.POSScreen
    if (this.contains("OrderDetailsRoute")) return PosScreens.OrderDetailsScreen
    if (this.contains("BillingPointManageRoute")) return PosScreens.BillingPointManageScreen
    if (this.contains("AddBillingPointRoute")) return PosScreens.AddBillingPointScreen
    if (this.contains("EditBillingPointRoute")) return PosScreens.EditBillingPointScreen
    val base = this.substringBefore("?")     // strip query params
        .substringBefore("/{")    // strip path param segments
        .substringBefore("{")     // strip any leftover placeholder
    return PosScreens.entries.firstOrNull { it.name == base }
}

@Composable
fun rememberGraphOwner(
    navController: NavHostController, graphRoute: String
): NavBackStackEntry =
    remember(navController, graphRoute) { navController.getBackStackEntry(graphRoute) }

@Composable
private fun rememberSafeGraphOwner(
    navController: NavHostController,
    graphRoute: String,
    fallback: NavBackStackEntry
): NavBackStackEntry {
    return remember(navController, graphRoute, fallback) {
        runCatching { navController.getBackStackEntry(graphRoute) }
            .getOrElse { fallback }
    }
}

@Serializable
data class OrdersScreenRoute(val orderNumber: String? = null)

@Serializable
@SerialName("pos_note")
data class PosNoteRoute(
    val op: String,         // "04" (crédito) or "05" (débito)
    val cufe: String,       // order externalInvoiceNumber (CUFE)
    val createdAt: String,   // ISO-8601 or whatever your POS expects
    // Customer data
    val customerId: Int? = null,
    val customerName: String? = null,
    val customerEmail: String? = null,
    val customerphone: String? = null,
    val customerRuc: String? = null,
    val customerStatus: Int = 1,
    val customerInvoiceID: Int? = null,
)

@Serializable
data class OrderDetailsRoute(val orderJson: String? = null)

@Serializable
data class BillingPointManageRoute(val branchCode: String)

@Serializable
data class AddBillingPointRoute(val branchCode: String)

@Serializable
data class EditBillingPointRoute(val branchCode: String, val billingCode: String)