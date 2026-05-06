package com.teco.ventago.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.teco.ventago.core.LocalStorage
import com.teco.ventago.core.deeplink.ExternalUriHandler
import com.teco.ventago.core.firebase.AnalyticsService
import com.teco.ventago.design_system.organism.ItemScreenActions
import com.teco.ventago.features.auth.domain.IAuthService
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
import com.teco.ventago.features.customers.domain.models.CustomerListItem
import com.teco.ventago.features.customers.ui.details.CustomerDetailsScreen
import com.teco.ventago.features.customers.ui.form.CustomerFormScreen
import com.teco.ventago.features.customers.ui.list.CustomersListScreen
import com.teco.ventago.features.home.ui.HomeSummaryScreen
import com.teco.ventago.features.home.ui.HomeScreen
import com.teco.ventago.features.invoicing.ui.InvoicingLandingScreen
import com.teco.ventago.features.notifications.ui.NotificationsScreen
import com.teco.ventago.features.orders.domain.OrderService
import com.teco.ventago.features.orders.domain.models.CustomerSnapshot
import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.orders.ui.ach_review.AchPaymentReviewScreen
import com.teco.ventago.features.orders.ui.order_invoice.OrderInvoiceActions
import com.teco.ventago.features.orders.ui.order_details.OrderDetailsScreen
import com.teco.ventago.features.orders.ui.order_history.OrderHistoryScreen
import com.teco.ventago.features.orders.ui.order_invoice.OrderInvoiceContent
import com.teco.ventago.features.orders.ui.orders.OrdersScreen
import com.teco.ventago.features.orders.ui.orders.OrdersScreenActions
import com.teco.ventago.features.orders.ui.order_details.OrderDetailsActions
import com.teco.ventago.features.quotes.ui.details.QuoteDetailsActions
import com.teco.ventago.features.orders.ui.order_details.viewModel.OrdersDetailsViewModel
import com.teco.ventago.features.orders.ui.order_history.viewModel.OrderHistoryViewModel
import com.teco.ventago.features.orders.ui.order_invoice.viewModel.OrderInvoiceViewModel
import com.teco.ventago.features.orders.ui.orders.viewmodel.OrdersViewModel
import com.teco.ventago.features.printers.domain.PrinterQrEntry
import com.teco.ventago.features.printers.ui.PrinterConfigScreen as PrinterConfigContent
import com.teco.ventago.features.printers.ui.PrinterOnboardingScreen as PrinterOnboardingContent
import com.teco.ventago.features.printers.ui.PrintersScreen as PrintersContent
import com.teco.ventago.features.payments.ui.home.OnboardingPaymentScreen
import com.teco.ventago.features.payments.ui.home.viewmodel.PaymentMethodType
import com.teco.ventago.features.payments.ui.home.viewmodel.PaymentMethodsViewModel
import com.teco.ventago.features.pos.ui.CartScreen
import com.teco.ventago.features.pos.ui.CartScreenBottomBar
import com.teco.ventago.features.pos.ui.PaymentScreen
import com.teco.ventago.features.pos.ui.PosInvoiceContent
import com.teco.ventago.features.pos.ui.PosProductScreen
import com.teco.ventago.features.pos.ui.PosProductScreenBottomBar
import com.teco.ventago.features.pos.ui.PosScreen
import com.teco.ventago.features.pos.ui.SuccessScreen
import com.teco.ventago.features.pos.ui.invoice_preview.InvoicePreviewScreen
import com.teco.ventago.features.pos.ui.customer.add.AddCustomerScreen
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
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.action_settings
import ventago.composeapp.generated.resources.add_address
import ventago.composeapp.generated.resources.add_image
import ventago.composeapp.generated.resources.add_new_category
import ventago.composeapp.generated.resources.ach_with_proof
import ventago.composeapp.generated.resources.authz_access_denied_title
import ventago.composeapp.generated.resources.bank_transfer
import ventago.composeapp.generated.resources.billing_points
import ventago.composeapp.generated.resources.branch
import ventago.composeapp.generated.resources.branches
import ventago.composeapp.generated.resources.categories
import ventago.composeapp.generated.resources.change_business_name
import ventago.composeapp.generated.resources.details
import ventago.composeapp.generated.resources.edit
import ventago.composeapp.generated.resources.home
import ventago.composeapp.generated.resources.home_summary_tab
import ventago.composeapp.generated.resources.invoice_title
import ventago.composeapp.generated.resources.invoice_preview_title
import ventago.composeapp.generated.resources.invoicing
import ventago.composeapp.generated.resources.invoicing_landing_title
import ventago.composeapp.generated.resources.items
import ventago.composeapp.generated.resources.login
import ventago.composeapp.generated.resources.order_details
import ventago.composeapp.generated.resources.order_status_history
import ventago.composeapp.generated.resources.orders
import ventago.composeapp.generated.resources.payments
import ventago.composeapp.generated.resources.payment_methods
import ventago.composeapp.generated.resources.paypal
import ventago.composeapp.generated.resources.pos
import ventago.composeapp.generated.resources.pos_add_client
import ventago.composeapp.generated.resources.pos_cart
import ventago.composeapp.generated.resources.pos_clients
import ventago.composeapp.generated.resources.pos_invoice
import ventago.composeapp.generated.resources.pos_payment
import ventago.composeapp.generated.resources.printer_config
import ventago.composeapp.generated.resources.printer_onboarding
import ventago.composeapp.generated.resources.printers
import ventago.composeapp.generated.resources.quote_details
import ventago.composeapp.generated.resources.quote_success
import ventago.composeapp.generated.resources.quote_summary
import ventago.composeapp.generated.resources.quotes
import ventago.composeapp.generated.resources.expenses
import ventago.composeapp.generated.resources.expense_details
import ventago.composeapp.generated.resources.expense_accounts_settings
import ventago.composeapp.generated.resources.new_expense
import ventago.composeapp.generated.resources.notifications
import ventago.composeapp.generated.resources.cufe_import
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
    Greetings(Res.string.pos),
    UnauthorizedScreen(Res.string.authz_access_denied_title, showBackButton = false),
    HomeScreen(Res.string.home, false, showBackButton = false),
    NotificationsScreen(Res.string.notifications),
    SummaryScreen(Res.string.home_summary_tab, true, showBackButton = false),

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
    InvoicePreviewScreen(Res.string.invoice_preview_title),
    SuccessScreen(Res.string.pos, false), PosInvoiceScreen(
        Res.string.invoice_title,
        true
    ),
    // Quotes creation screens
    Quotes(Res.string.quotes),
    QuoteSummaryScreen(Res.string.quote_summary),
    QuoteSuccessScreen(Res.string.quote_success, showAppBar = false, showBackButton = false),
    QuotesListScreen(Res.string.quotes, true, showBackButton = true),
    QuoteDetailsScreen(
        Res.string.quote_details,
        true,
        showBackButton = true,
        actions = { backStackEntry, navigate, navigateAny ->
            QuoteDetailsActions(backStackEntry, navigate, navigateAny)
        }
    ),
    CustomersScreen(Res.string.pos_clients),

    SearchCustomerScreen(Res.string.search),

    AddCustomerScreen(Res.string.pos_clients),

    CustomersManage(Res.string.pos_clients),
    CustomersListScreen(Res.string.pos_clients),
    CustomerDetailsScreen(Res.string.details),
    CustomerCreateScreen(Res.string.pos_add_client),
    CustomerEditScreen(Res.string.edit),


    // Payments Screens
    Payments(Res.string.payments), PaymentsHomeScreen(
        Res.string.payment_methods,
        true
    ),
    PaymentsTransferenceScreen(
        Res.string.ach_with_proof,
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
    AchPaymentDetailsScreen(Res.string.details, true),
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
    PrintersScreen(Res.string.printers),
    PrinterOnboardingScreen(Res.string.printer_onboarding),
    PrinterConfigScreen(Res.string.printer_config),

    ChangeBusinessNameScreen(
        Res.string.change_business_name
    ),
    BusinessAddressSettingsScreen(Res.string.add_address),
    ExpenseAccountsSettingsScreen(Res.string.expense_accounts_settings),

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
    EditBillingPointScreen(Res.string.billing_points),

    // Expenses Screens
    Expenses(Res.string.expenses),
    ExpensesListScreen(Res.string.expenses, true, showBackButton = true),
    ExpenseDetailsScreen(Res.string.expense_details, true, showBackButton = true),
    NewExpenseScreen(Res.string.new_expense, true, showBackButton = true),
    EditExpenseScreen(Res.string.expense_details, true, showBackButton = true),
    DuplicateExpenseScreen(Res.string.new_expense, true, showBackButton = true),
    CufeImportScreen(Res.string.cufe_import, true, showBackButton = true);


    fun isPosScreens(): Boolean {
        return this == POSProductScreen || this == CartScreen || this == PaymentScreen || this == InvoicePreviewScreen || this == SuccessScreen || this == CustomersScreen || this == AddCustomerScreen || this == PosInvoiceScreen
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

        composable(route = PosScreens.NotificationsScreen.name) {
            analyticsService.logScreenView("NotificationsScreen")
            NotificationsScreen { route ->
                when (route) {
                    is PosScreens -> navController.navigate(route.name)
                    is AchPaymentDetailsRoute -> navController.navigate(route)
                    else -> Unit
                }
            }
        }

        composable(route = PosScreens.SummaryScreen.name) {
            analyticsService.logScreenView("HomeSummaryScreen")
            HomeSummaryScreen {
                navController.navigate(it.name)
            }
        }

        composable(route = PosScreens.Greetings.name) {
            Greetings()
        }

        composable(route = PosScreens.UnauthorizedScreen.name) {
            val authService: com.teco.ventago.features.auth.domain.IAuthService = koinInject()
            val scope = rememberCoroutineScope()
            UnauthorizedScreen(
                onRetry = {
                    navController.navigateUp()
                },
                onSignOut = {
                    scope.launch { authService.signOut() }
                }
            )
        }

        addProductsNavigation(navController, analyticsService)

        addPOSNavigation(navController, appViewModel, analyticsService)
        addQuotesNavigation(navController, analyticsService)

        addPaymentsNavigation(navController, appViewModel, analyticsService)

        addOrdersNavigation(navController, analyticsService)
        addCustomersNavigation(navController, analyticsService)

        addSettingsNavigation(navController, analyticsService)

        addInvoicingNavigation(navController, analyticsService)

        addBranchesNavigation(navController, analyticsService)

        addExpensesNavigation(navController, analyticsService)
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
    fun returnSelectedCustomerToPos(customer: CustomerListItem) {
        val payload = Json.encodeToString(customer)
        val posEntry = navController.getBackStackEntry(PosScreens.POS.name)
        posEntry.savedStateHandle[NavResults.KEY_SELECTED_CUSTOMER] = payload

        val hasPosScreen = runCatching {
            navController.getBackStackEntry(PosScreens.POSScreen.name)
        }.isSuccess

        if (hasPosScreen) {
            navController.popBackStack(
                route = PosScreens.POSScreen.name,
                inclusive = false,
                saveState = false
            )
        } else {
            var guard = 0
            while (navController.currentDestination?.route?.contains("pos_note") == false) {
                val ok = navController.navigateUp()
                if (!ok) return
                guard++
                if (guard > 100) return
            }
        }
    }

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

        composable(route = PosScreens.InvoicePreviewScreen.name) {
            val backStackEntry = remember { navController.getBackStackEntry(PosScreens.POS.name) }
            val viewModel: PosViewModel = koinViewModel(viewModelStoreOwner = backStackEntry)
            analyticsService.logScreenView("InvoicePreviewScreen")
            InvoicePreviewScreen(viewModel) {
                navController.navigateUp()
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
            analyticsService.logScreenView("POSCustomerPickerScreen")
            CustomersListScreen(
                onCreateCustomer = { navController.navigate(PosScreens.AddCustomerScreen.name) },
                onCustomerSelected = ::returnSelectedCustomerToPos,
                enforceInvoiceCustomerSelection = true,
            )
        }

        composable(route = PosScreens.CustomersScreen.name) {
            analyticsService.logScreenView("ClientsScreen")
            CustomersListScreen(
                onCreateCustomer = { navController.navigate(PosScreens.AddCustomerScreen.name) },
                onCustomerSelected = ::returnSelectedCustomerToPos,
                enforceInvoiceCustomerSelection = true,
            )
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
    fun PaymentMethodType.toScreen(): PosScreens {
        return when (this) {
            PaymentMethodType.Yappy -> PosScreens.PaymentsYappyScreen
            PaymentMethodType.Ach -> PosScreens.PaymentsTransferenceScreen
            PaymentMethodType.Paypal -> PosScreens.PaymentsPaypalScreen
        }
    }

    navigation(
        route = PosScreens.Payments.name, startDestination = PosScreens.PaymentsHomeScreen.name
    ) {

        composable(route = PosScreens.PaymentsHomeScreen.name) { backStackEntry ->
            val owner = rememberSafeGraphOwner(
                navController = navController,
                graphRoute = PosScreens.Payments.name,
                fallback = backStackEntry
            )
            val viewModel: PaymentMethodsViewModel =
                koinViewModel(viewModelStoreOwner = owner)
            analyticsService.logScreenView("PaymentsHomeScreen")
            OnboardingPaymentScreen(
                viewModel = viewModel,
                onNavigateMethod = { method ->
                    viewModel.onOpenMethod(method)
                    navController.navigate(method.toScreen().name)
                },
                onNavigateSettingsRoot = {
                    navController.navigate(PosScreens.SettingsScreen.name) {
                        popUpTo(PosScreens.Payments.name) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateBusinessAddress = {
                    navController.navigate(PosScreens.BusinessAddressSettingsScreen.name)
                },
            )
        }

        composable(route = PosScreens.PaymentsYappyScreen.name) { backStackEntry ->
            val owner = rememberSafeGraphOwner(
                navController = navController,
                graphRoute = PosScreens.Payments.name,
                fallback = backStackEntry
            )
            val viewModel: PaymentMethodsViewModel = koinViewModel(viewModelStoreOwner = owner)
            analyticsService.logScreenView("PaymentsYappyScreen")
            OnboardingPaymentScreen(
                viewModel = viewModel,
                isMethodRoute = true,
                methodRoute = PaymentMethodType.Yappy,
                onNavigateMethod = {},
                onNavigateSettingsRoot = {
                    navController.navigate(PosScreens.SettingsScreen.name) {
                        popUpTo(PosScreens.Payments.name) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateBusinessAddress = {
                    navController.navigate(PosScreens.BusinessAddressSettingsScreen.name)
                },
                onExitMethodRoute = {
                    viewModel.onEnterHomeRoute()
                    navController.navigateUp()
                },
            )
        }

        composable(route = PosScreens.PaymentsTransferenceScreen.name) { backStackEntry ->
            val owner = rememberSafeGraphOwner(
                navController = navController,
                graphRoute = PosScreens.Payments.name,
                fallback = backStackEntry
            )
            val viewModel: PaymentMethodsViewModel = koinViewModel(viewModelStoreOwner = owner)
            analyticsService.logScreenView("PaymentsAchScreen")
            OnboardingPaymentScreen(
                viewModel = viewModel,
                isMethodRoute = true,
                methodRoute = PaymentMethodType.Ach,
                onNavigateMethod = {},
                onNavigateSettingsRoot = {
                    navController.navigate(PosScreens.SettingsScreen.name) {
                        popUpTo(PosScreens.Payments.name) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateBusinessAddress = {
                    navController.navigate(PosScreens.BusinessAddressSettingsScreen.name)
                },
                onExitMethodRoute = {
                    viewModel.onEnterHomeRoute()
                    navController.navigateUp()
                },
            )
        }

        composable(route = PosScreens.PaymentsPaypalScreen.name) { backStackEntry ->
            val owner = rememberSafeGraphOwner(
                navController = navController,
                graphRoute = PosScreens.Payments.name,
                fallback = backStackEntry
            )
            val viewModel: PaymentMethodsViewModel = koinViewModel(viewModelStoreOwner = owner)
            analyticsService.logScreenView("PaymentsPaypalScreen")
            OnboardingPaymentScreen(
                viewModel = viewModel,
                isMethodRoute = true,
                methodRoute = PaymentMethodType.Paypal,
                onNavigateMethod = {},
                onNavigateSettingsRoot = {
                    navController.navigate(PosScreens.SettingsScreen.name) {
                        popUpTo(PosScreens.Payments.name) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateBusinessAddress = {
                    navController.navigate(PosScreens.BusinessAddressSettingsScreen.name)
                },
                onExitMethodRoute = {
                    viewModel.onEnterHomeRoute()
                    navController.navigateUp()
                },
            )
        }

        composable(route = PosScreens.PaymentsPaypalOnboardingScreen.name) { backStackEntry ->
            val owner = rememberSafeGraphOwner(
                navController = navController,
                graphRoute = PosScreens.Payments.name,
                fallback = backStackEntry
            )
            val viewModel: PaymentMethodsViewModel = koinViewModel(viewModelStoreOwner = owner)
            analyticsService.logScreenView("PaymentsPaypalOnboardingScreen")
            OnboardingPaymentScreen(
                viewModel = viewModel,
                isMethodRoute = true,
                methodRoute = PaymentMethodType.Paypal,
                onNavigateMethod = {},
                onNavigateSettingsRoot = {
                    navController.navigate(PosScreens.SettingsScreen.name) {
                        popUpTo(PosScreens.Payments.name) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateBusinessAddress = {
                    navController.navigate(PosScreens.BusinessAddressSettingsScreen.name)
                },
                onExitMethodRoute = {
                    viewModel.onEnterHomeRoute()
                    navController.navigateUp()
                },
            )
        }
    }
}

private fun NavGraphBuilder.addQuotesNavigation(
    navController: NavHostController, analyticsService: AnalyticsService
) {
    navigation(
        route = PosScreens.Quotes.name, startDestination = PosScreens.QuotesListScreen.name
    ) {
        composable(route = PosScreens.QuotesListScreen.name) {
            val quotesOwner = rememberGraphOwner(navController, PosScreens.Quotes.name)
            val viewModel: com.teco.ventago.features.quotes.ui.list.QuotesListViewModel =
                koinViewModel(viewModelStoreOwner = quotesOwner)
            analyticsService.logScreenView("QuotesListScreen")
            com.teco.ventago.features.quotes.ui.list.QuotesListScreen(
                viewModel = viewModel,
                navigate = { route -> navController.navigate(route.name) },
                onBack = { navController.navigateUp() }
            )
        }

            composable(route = PosScreens.QuoteDetailsScreen.name) {
                val quotesOwner = rememberGraphOwner(navController, PosScreens.Quotes.name)
                val viewModel: com.teco.ventago.features.quotes.ui.details.QuoteDetailsViewModel =
                    koinViewModel(viewModelStoreOwner = quotesOwner)
                analyticsService.logScreenView("QuoteDetailsScreen")
                com.teco.ventago.features.quotes.ui.details.QuoteDetailsScreen(
                    viewModel = viewModel,
                    onBack = { navController.navigateUp() },
                    onModify = {
                        com.teco.ventago.features.quotes.domain.QuoteSelectionStore.startQuoteFlow = true
                        com.teco.ventago.features.quotes.domain.QuoteSelectionStore.startOrderFlowFromQuote = false
                        navController.navigate(PosScreens.POSScreen.name)
                    },
                    onCreateOrder = {
                        com.teco.ventago.features.quotes.domain.QuoteSelectionStore.startQuoteFlow = false
                        com.teco.ventago.features.quotes.domain.QuoteSelectionStore.startOrderFlowFromQuote = true
                        navController.navigate(PosScreens.POSScreen.name)
                    }
                )
            }

        composable(route = PosScreens.QuoteSummaryScreen.name) {
            val quotesOwner = rememberGraphOwner(navController, PosScreens.Quotes.name)
            val posOwner = rememberSafeGraphOwner(navController, PosScreens.POS.name, quotesOwner)
            val viewModel: PosViewModel = koinViewModel(viewModelStoreOwner = posOwner)
            analyticsService.logScreenView("QuoteSummaryScreen")
            com.teco.ventago.features.quotes.ui.summary.QuoteSummaryScreen(viewModel) { route, builder ->
                navController.navigate(route, builder)
            }
        }

        composable(route = PosScreens.QuoteSuccessScreen.name) {
            val quotesOwner = rememberGraphOwner(navController, PosScreens.Quotes.name)
            val posOwner = rememberSafeGraphOwner(navController, PosScreens.POS.name, quotesOwner)
            val viewModel: PosViewModel = koinViewModel(viewModelStoreOwner = posOwner)
            analyticsService.logScreenView("QuoteSuccessScreen")
            com.teco.ventago.features.quotes.ui.success.QuoteSuccessScreen(
                viewModel = viewModel,
                navController = navController
            ) { route, builder ->
                navController.navigate(route, builder)
            }
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
            LaunchedEffect(Unit) {
                viewModel.applyCustomerFilter(null)
            }
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
            val paymentStatus = args.paymentStatus
            val customerId = args.customerId

//            val ordersOwner = rememberSafeGraphOwner(
//                navController = navController,
//                graphRoute = PosScreens.Orders.name,
//                fallback = backStackEntry
//            )
            val ordersOwner = rememberGraphOwner(navController, PosScreens.Orders.name)
            val viewModel: OrdersViewModel = koinViewModel(viewModelStoreOwner = ordersOwner)

            analyticsService.logScreenView("OrdersScreen")

            LaunchedEffect(orderNumber, paymentStatus, customerId) {
                viewModel.applyCustomerFilter(customerId)
                if (paymentStatus != null) {
                    viewModel.applyPaymentStatusFilter(paymentStatus)
                }
                if (!orderNumber.isNullOrEmpty()) {
                    viewModel.findOrderByOrderNumber(orderNumber)
                }
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
                when (route) {
                    is PosScreens -> navController.navigate(route, builder)
                    is AchPaymentDetailsRoute -> navController.navigate(route)
                    else -> Unit
                }
            }
        }

        composable<AchPaymentDetailsRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<AchPaymentDetailsRoute>()
            val ordersOwner = rememberGraphOwner(navController, PosScreens.Orders.name)
            val viewModel: OrdersDetailsViewModel = koinViewModel(viewModelStoreOwner = ordersOwner)
            analyticsService.logScreenView("AchPaymentDetailsScreen")
            AchPaymentReviewScreen(
                viewModel = viewModel,
                paymentUid = args.paymentUid
            )
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

private fun NavGraphBuilder.addCustomersNavigation(
    navController: NavHostController,
    analyticsService: AnalyticsService
) {
    navigation(
        route = PosScreens.CustomersManage.name,
        startDestination = PosScreens.CustomersListScreen.name
    ) {
        composable(route = PosScreens.CustomersListScreen.name) {
            analyticsService.logScreenView("CustomersListScreen")
            CustomersListScreen(
                onCreateCustomer = { navController.navigate(PosScreens.CustomerCreateScreen.name) },
                onCustomerSelected = { customer ->
                    navController.navigate(CustomerDetailsRoute(customerId = customer.id))
                }
            )
        }

        composable<CustomerDetailsRoute> { backStackEntry ->
            analyticsService.logScreenView("CustomerDetailsScreen")
            val args = backStackEntry.toRoute<CustomerDetailsRoute>()

            CustomerDetailsScreen(
                customerId = args.customerId,
                onBackAfterDelete = { navController.navigateUp() },
                onEdit = { customerId ->
                    navController.navigate(CustomerEditRoute(customerId = customerId))
                },
                onSeeAllOrders = { customerId ->
                    navController.navigate(OrdersScreenRoute(customerId = customerId))
                }
            )
        }

        composable(route = PosScreens.CustomerCreateScreen.name) {
            analyticsService.logScreenView("CustomerCreateScreen")
            CustomerFormScreen(
                customerId = null,
                onSaved = { navController.navigateUp() },
                onValidationError = { /* no-op for now */ }
            )
        }

        composable<CustomerEditRoute> { backStackEntry ->
            analyticsService.logScreenView("CustomerEditScreen")
            val args = backStackEntry.toRoute<CustomerEditRoute>()
            CustomerFormScreen(
                customerId = args.customerId,
                onSaved = { navController.navigateUp() },
                onValidationError = { /* no-op for now */ }
            )
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
                navController.navigateRoute(route)
            }
        }

        composable(route = PosScreens.PrintersScreen.name) {
            analyticsService.logScreenView("PrintersScreen")
            PrintersContent(navigate = { route: Any ->
                navController.navigateRoute(route)
            })
        }

        composable<PrinterOnboardingRoute>(
            deepLinks = listOf(
                navDeepLink<PrinterOnboardingRoute>(basePath = "https://tecodigi.com/printer-onboarding"),
                navDeepLink { uriPattern = "https://tecodigi.com/printer-onboarding?fromQr={fromQr}" }
            )
        ) { backStackEntry ->
            val args = backStackEntry.toRoute<PrinterOnboardingRoute>()
            val authService: IAuthService = koinInject()
            val localStorage: LocalStorage = koinInject()

            LaunchedEffect(args.fromQr) {
                if (args.fromQr && !authService.isAuthenticated()) {
                    localStorage.set(PrinterQrEntry.KEY_PENDING_ONBOARDING, true)
                }
            }

            analyticsService.logScreenView("PrinterOnboardingScreen")
            PrinterOnboardingContent(
                entryContext = args.entryContext,
                branchCode = args.branchCode,
                billingPointCode = args.billingPointCode,
                fromQr = args.fromQr,
                onDismiss = { navController.navigateUp() },
                onGoToBranches = {
                    navController.navigate(PosScreens.Branches.name) {
                        popUpTo(PosScreens.Settings.name) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable<PrinterConfigRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<PrinterConfigRoute>()
            analyticsService.logScreenView("PrinterConfigScreen")
            PrinterConfigContent(
                entryContext = args.entryContext,
                branchCode = args.branchCode,
                billingPointCode = args.billingPointCode,
                onDismiss = { navController.navigateUp() },
            )
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

        composable(route = PosScreens.ExpenseAccountsSettingsScreen.name) {
            analyticsService.logScreenView("ExpenseAccountsSettingsScreen")
            com.teco.ventago.features.expenses.ui.accounts.ExpenseAccountsScreen()
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
                navController.navigateRoute(route)
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

private fun NavGraphBuilder.addExpensesNavigation(
    navController: NavHostController, analyticsService: AnalyticsService
) {
    navigation(
        route = PosScreens.Expenses.name, startDestination = PosScreens.ExpensesListScreen.name
    ) {
        composable(route = PosScreens.ExpensesListScreen.name) {
            val expensesOwner = rememberGraphOwner(navController, PosScreens.Expenses.name)
            val viewModel: com.teco.ventago.features.expenses.ui.list.ExpensesListViewModel =
                koinViewModel(viewModelStoreOwner = expensesOwner)
            analyticsService.logScreenView("ExpensesListScreen")
            com.teco.ventago.features.expenses.ui.list.ExpensesListScreen(
                viewModel = viewModel,
                navigate = { route -> navController.navigate(route.name) },
                onBack = { navController.navigateUp() }
            )
        }

        composable<ExpensesListScreenRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<ExpensesListScreenRoute>()
            val expensesOwner = rememberGraphOwner(navController, PosScreens.Expenses.name)
            val viewModel: com.teco.ventago.features.expenses.ui.list.ExpensesListViewModel =
                koinViewModel(viewModelStoreOwner = expensesOwner)
            analyticsService.logScreenView("ExpensesListScreen")

            LaunchedEffect(args.initialPaymentStatus) {
                if (!args.initialPaymentStatus.isNullOrBlank()) {
                    viewModel.applyInitialPaymentStatus(args.initialPaymentStatus)
                }
            }

            com.teco.ventago.features.expenses.ui.list.ExpensesListScreen(
                viewModel = viewModel,
                navigate = { route -> navController.navigate(route.name) },
                onBack = { navController.navigateUp() }
            )
        }

        composable(route = PosScreens.ExpenseDetailsScreen.name) {
            val expensesOwner = rememberGraphOwner(navController, PosScreens.Expenses.name)
            val viewModel: com.teco.ventago.features.expenses.ui.details.ExpenseDetailsViewModel =
                koinViewModel(viewModelStoreOwner = expensesOwner)
            analyticsService.logScreenView("ExpenseDetailsScreen")
            com.teco.ventago.features.expenses.ui.details.ExpenseDetailsScreen(
                viewModel = viewModel,
                onBack = { navController.navigateUp() },
                openCategorization = false,
                onEdit = { navController.navigate(PosScreens.EditExpenseScreen.name) },
                onDuplicate = { navController.navigate(PosScreens.DuplicateExpenseScreen.name) }
            )
        }

        composable<ExpenseDetailsRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<ExpenseDetailsRoute>()
            val expensesOwner = rememberGraphOwner(navController, PosScreens.Expenses.name)
            val viewModel: com.teco.ventago.features.expenses.ui.details.ExpenseDetailsViewModel =
                koinViewModel(viewModelStoreOwner = expensesOwner)
            analyticsService.logScreenView("ExpenseDetailsScreen")
            if (args.expenseId != null) {
                com.teco.ventago.features.expenses.domain.ExpensesSelectionStore.selected =
                    com.teco.ventago.features.expenses.domain.models.Expense(id = args.expenseId)
            }
            com.teco.ventago.features.expenses.ui.details.ExpenseDetailsScreen(
                viewModel = viewModel,
                onBack = { navController.navigateUp() },
                openCategorization = args.openCategorization,
                onEdit = { navController.navigate(PosScreens.EditExpenseScreen.name) },
                onDuplicate = { navController.navigate(PosScreens.DuplicateExpenseScreen.name) }
            )
        }

        composable(route = PosScreens.NewExpenseScreen.name) {
            val expensesOwner = rememberGraphOwner(navController, PosScreens.Expenses.name)
            val viewModel: com.teco.ventago.features.expenses.ui.create.NewExpenseViewModel =
                koinViewModel(viewModelStoreOwner = expensesOwner)
            analyticsService.logScreenView("NewExpenseScreen")
            com.teco.ventago.features.expenses.ui.create.NewExpenseScreen(
                viewModel = viewModel,
                onBack = { navController.navigateUp() }
            )
        }

        composable(route = PosScreens.EditExpenseScreen.name) {
            val expensesOwner = rememberGraphOwner(navController, PosScreens.Expenses.name)
            val viewModel: com.teco.ventago.features.expenses.ui.create.NewExpenseViewModel =
                koinViewModel(viewModelStoreOwner = expensesOwner)
            analyticsService.logScreenView("EditExpenseScreen")
            com.teco.ventago.features.expenses.ui.create.NewExpenseScreen(
                viewModel = viewModel,
                isEditMode = true,
                onBack = { navController.navigateUp() }
            )
        }

        composable(route = PosScreens.DuplicateExpenseScreen.name) {
            val expensesOwner = rememberGraphOwner(navController, PosScreens.Expenses.name)
            val viewModel: com.teco.ventago.features.expenses.ui.create.NewExpenseViewModel =
                koinViewModel(viewModelStoreOwner = expensesOwner)
            analyticsService.logScreenView("DuplicateExpenseScreen")
            com.teco.ventago.features.expenses.ui.create.NewExpenseScreen(
                viewModel = viewModel,
                isDuplicateMode = true,
                onBack = { navController.navigateUp() }
            )
        }

        composable(route = PosScreens.CufeImportScreen.name) {
            val expensesOwner = rememberGraphOwner(navController, PosScreens.Expenses.name)
            val viewModel: com.teco.ventago.features.expenses.ui.cufe.CufeImportViewModel =
                koinViewModel(viewModelStoreOwner = expensesOwner)
            analyticsService.logScreenView("CufeImportScreen")
            com.teco.ventago.features.expenses.ui.cufe.CufeImportScreen(
                viewModel = viewModel,
                onBack = { navController.navigateUp() },
                onExpenseImported = { expenseId ->
                    navController.navigate(
                        ExpenseDetailsRoute(
                            expenseId = expenseId,
                            openCategorization = true
                        )
                    ) {
                        popUpTo(PosScreens.ExpensesListScreen.name) { inclusive = false }
                    }
                }
            )
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

private fun NavHostController.navigateRoute(route: Any) {
    when (route) {
        is PosScreens -> navigate(route.name)
        else -> navigate(route)
    }
}

fun PosScreens.withArgs(vararg args: Pair<String, String>): String {
    val base = this.name
    val query = args.joinToString("&") { "${it.first}=${it.second}" }
    return "$base?$query"
}

fun String.toPosScreenOrNull(): PosScreens? {
    if (this.contains("OrdersScreenRoute")) return PosScreens.OrdersScreen
    if (this.contains("CustomerDetailsRoute")) return PosScreens.CustomerDetailsScreen
    if (this.contains("CustomerEditRoute")) return PosScreens.CustomerEditScreen
    if (this.contains("ExpensesListScreenRoute")) return PosScreens.ExpensesListScreen
    if (this.contains("PosNoteRoute")) return PosScreens.POSScreen
    if (this.contains("pos_note")) return PosScreens.POSScreen
    if (this.contains("OrderDetailsRoute")) return PosScreens.OrderDetailsScreen
    if (this.contains("AchPaymentDetailsRoute")) return PosScreens.AchPaymentDetailsScreen
    if (this.contains("BillingPointManageRoute")) return PosScreens.BillingPointManageScreen
    if (this.contains("AddBillingPointRoute")) return PosScreens.AddBillingPointScreen
    if (this.contains("EditBillingPointRoute")) return PosScreens.EditBillingPointScreen
    if (this.contains("PrinterOnboardingRoute")) return PosScreens.PrinterOnboardingScreen
    if (this.contains("PrinterConfigRoute")) return PosScreens.PrinterConfigScreen
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
data class OrdersScreenRoute(
    val orderNumber: String? = null,
    val paymentStatus: Int? = null,
    val customerId: Long? = null
)

@Serializable
data class CustomerDetailsRoute(val customerId: Long)

@Serializable
data class CustomerEditRoute(val customerId: Long)

@Serializable
data class ExpensesListScreenRoute(
    val initialPaymentStatus: String? = null
)

@Serializable
data class ExpenseDetailsRoute(
    val expenseId: Long? = null,
    val openCategorization: Boolean = false
)

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
    // Order lines (serialized JSON string)
    val orderLinesJson: String? = null,
)

@Serializable
data class OrderDetailsRoute(val orderJson: String? = null)

@Serializable
data class AchPaymentDetailsRoute(val paymentUid: String)

@Serializable
data class BillingPointManageRoute(val branchCode: String)

@Serializable
data class AddBillingPointRoute(val branchCode: String)

@Serializable
data class EditBillingPointRoute(val branchCode: String, val billingCode: String)

@Serializable
data class PrinterOnboardingRoute(
    val entryContext: String = "SETTINGS",
    val branchCode: String? = null,
    val billingPointCode: String? = null,
    val fromQr: Boolean = false,
)

@Serializable
data class PrinterConfigRoute(
    val entryContext: String = "SETTINGS",
    val branchCode: String? = null,
    val billingPointCode: String? = null,
)
