package com.teco.ventago.navigation

import com.teco.ventago.core.authz.AuthzEvaluator
import com.teco.ventago.core.authz.MenuKey
import com.teco.ventago.core.authz.RouteKey
import com.teco.ventago.core.beta.BetaFeature
import com.teco.ventago.features.auth.domain.model.User

enum class BottomNavKey(
    val selectedScreen: PosScreens,
    val destinationScreen: PosScreens,
) {
    HOME(PosScreens.HomeScreen, PosScreens.HomeScreen),
    SUMMARY(PosScreens.SummaryScreen, PosScreens.SummaryScreen),
    ORDERS(PosScreens.OrdersScreen, PosScreens.Orders),
    PRODUCTS(PosScreens.CategoriesManageScreen, PosScreens.ProductsManage),
    SETTINGS(PosScreens.SettingsScreen, PosScreens.Settings),
}

fun visibleBottomNavKeys(user: User?, betaSnapshot: Set<BetaFeature>): List<BottomNavKey> {
    return buildList {
        if (AuthzEvaluator.canMenu(MenuKey.HOME, user, betaSnapshot)) add(BottomNavKey.HOME)
        if (AuthzEvaluator.canRoute(RouteKey.HOME_SUMMARY, user, betaSnapshot)) add(BottomNavKey.SUMMARY)
        if (AuthzEvaluator.canRoute(RouteKey.ORDERS_LIST, user, betaSnapshot)) add(BottomNavKey.ORDERS)
        if (AuthzEvaluator.canRoute(RouteKey.PRODUCTS_LIST, user, betaSnapshot)) add(BottomNavKey.PRODUCTS)
        if (AuthzEvaluator.canRoute(RouteKey.SETTINGS_PAGE, user, betaSnapshot)) add(BottomNavKey.SETTINGS)
    }
}

fun routeKeyForScreen(screen: PosScreens): RouteKey? {
    return when (screen) {
        PosScreens.HomeScreen -> RouteKey.HOME
        PosScreens.NotificationsScreen -> RouteKey.HOME
        PosScreens.SummaryScreen -> RouteKey.HOME_SUMMARY
        PosScreens.OrdersScreen -> RouteKey.ORDERS_LIST
        PosScreens.OrderDetailsScreen -> RouteKey.ORDER_DETAILS
        PosScreens.AchPaymentDetailsScreen -> RouteKey.ACH_PAYMENT_DETAILS
        PosScreens.OrderInvoiceScreen -> RouteKey.INVOICE_PREVIEW
        PosScreens.CustomersListScreen -> RouteKey.CUSTOMERS_LIST
        PosScreens.CustomerDetailsScreen -> RouteKey.CUSTOMER_DETAILS
        PosScreens.CustomerCreateScreen,
        PosScreens.CustomerEditScreen -> RouteKey.CUSTOMER_FORM
        PosScreens.CategoriesManageScreen -> RouteKey.PRODUCTS_LIST
        PosScreens.EditCategoryScreen -> RouteKey.PRODUCT_DETAILS
        PosScreens.AddCategoryScreen,
        PosScreens.ModifyCategoryScreen -> RouteKey.CATEGORY_MANAGE
        PosScreens.QuotesListScreen -> RouteKey.QUOTES_LIST
        PosScreens.QuoteDetailsScreen -> RouteKey.QUOTE_DETAILS
        PosScreens.QuoteSummaryScreen,
        PosScreens.QuoteSuccessScreen -> RouteKey.QUOTE_NEW
        PosScreens.ExpensesListScreen -> RouteKey.EXPENSES_LIST
        PosScreens.ExpenseDetailsScreen -> RouteKey.EXPENSE_DETAILS
        PosScreens.NewExpenseScreen,
        PosScreens.EditExpenseScreen,
        PosScreens.DuplicateExpenseScreen,
        PosScreens.CufeImportScreen -> RouteKey.EXPENSE_NEW
        PosScreens.SettingsScreen -> RouteKey.SETTINGS_PAGE
        PosScreens.Payments,
        PosScreens.PaymentsHomeScreen,
        PosScreens.PaymentsTransferenceScreen,
        PosScreens.PaymentsPaypalScreen,
        PosScreens.PaymentsPaypalOnboardingScreen,
        PosScreens.PaymentsYappyScreen -> RouteKey.PAYMENTS_PAGE
        PosScreens.BranchesManageScreen,
        PosScreens.BillingPointManageScreen,
        PosScreens.AddBillingPointScreen,
        PosScreens.EditBillingPointScreen -> RouteKey.SETTINGS_BRANCHES_OWNER
        PosScreens.ExpenseAccountsSettingsScreen -> RouteKey.SETTINGS_EXPENSE_ACCOUNTS_OWNER
        else -> null
    }
}

fun fallbackScreenFor(user: User?, betaSnapshot: Set<BetaFeature>): PosScreens? {
    return when (AuthzEvaluator.resolveFallbackRoute(user, betaSnapshot)) {
        RouteKey.HOME -> PosScreens.HomeScreen
        RouteKey.ORDERS_LIST -> PosScreens.Orders
        RouteKey.CUSTOMERS_LIST -> PosScreens.CustomersManage
        RouteKey.PRODUCTS_LIST -> PosScreens.ProductsManage
        RouteKey.EXPENSES_LIST -> PosScreens.Expenses
        RouteKey.SETTINGS_PAGE -> PosScreens.Settings
        else -> null
    }
}
