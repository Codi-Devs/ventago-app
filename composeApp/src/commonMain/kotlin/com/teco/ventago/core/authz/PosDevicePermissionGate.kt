package com.teco.ventago.core.authz

import com.teco.ventago.features.pos.provisioning.domain.model.PosDevicePermissions

object PosDevicePermissionGate {
    private var permissions: PosDevicePermissions? = null

    fun update(value: PosDevicePermissions?) {
        permissions = value
    }

    fun clear() {
        permissions = null
    }

    fun canMenu(menuKey: MenuKey): Boolean {
        val p = permissions ?: return true
        return when (menuKey) {
            MenuKey.CUSTOMERS -> p.clientsView || p.clientsCreate
            MenuKey.PRODUCTS_SECTION,
            MenuKey.PRODUCTS_VIEW -> p.productsView || p.productsCreate
            MenuKey.PRODUCTS_CREATE -> p.productsCreate
            MenuKey.QUOTES_LIST -> p.quotesView || p.quotesCreate
            MenuKey.EXPENSES -> p.expensesView || p.expensesCreate
            MenuKey.REPORTS -> p.reportsView
            else -> true
        }
    }

    fun canRoute(routeKey: RouteKey): Boolean {
        val p = permissions ?: return true
        return when (routeKey) {
            RouteKey.CUSTOMERS_LIST,
            RouteKey.CUSTOMER_DETAILS -> p.clientsView || p.clientsCreate
            RouteKey.CUSTOMER_FORM -> p.clientsCreate
            RouteKey.PRODUCTS_LIST,
            RouteKey.PRODUCT_DETAILS -> p.productsView || p.productsCreate
            RouteKey.PRODUCT_ADD,
            RouteKey.CATEGORY_MANAGE -> p.productsCreate
            RouteKey.QUOTES_LIST,
            RouteKey.QUOTE_DETAILS -> p.quotesView || p.quotesCreate
            RouteKey.QUOTE_NEW -> p.quotesCreate
            RouteKey.EXPENSES_LIST,
            RouteKey.EXPENSE_DETAILS -> p.expensesView || p.expensesCreate
            RouteKey.EXPENSE_NEW -> p.expensesCreate
            RouteKey.REPORTS_PAGE -> p.reportsView
            else -> true
        }
    }

    fun canAction(actionKey: ActionKey): Boolean {
        val p = permissions ?: return true
        return when (actionKey) {
            ActionKey.PAYMENTS_CONFIGURE -> p.paymentMethodsConfigure
            ActionKey.ORDERS_PAYMENT_LINK -> p.paymentLink
            ActionKey.ORDERS_YAPPY_ONSITE -> p.paymentYappyOnsite
            ActionKey.ORDERS_MANUAL_PAYMENT -> p.paymentManualMethods
            ActionKey.CUSTOMERS_CREATE,
            ActionKey.CUSTOMERS_UPDATE,
            ActionKey.CUSTOMERS_DELETE -> p.clientsCreate
            ActionKey.PRODUCTS_CREATE,
            ActionKey.PRODUCTS_UPDATE,
            ActionKey.PRODUCTS_DELETE,
            ActionKey.PRODUCTS_MANAGE_CATEGORIES -> p.productsCreate
            ActionKey.QUOTES_CREATE,
            ActionKey.QUOTES_UPDATE,
            ActionKey.QUOTES_ACCEPT -> p.quotesCreate
            ActionKey.EXPENSES_CREATE,
            ActionKey.EXPENSES_UPDATE,
            ActionKey.EXPENSES_DELETE -> p.expensesCreate
            ActionKey.REPORTS_EXECUTE -> p.reportsView
            else -> true
        }
    }
}
