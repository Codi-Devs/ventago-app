package com.teco.ventago.core.authz

import com.teco.ventago.core.beta.BetaFeature
import com.teco.ventago.features.auth.domain.model.User

object AuthzEvaluator {
    private val menuPolicies = mapOf(
        MenuKey.HOME to AuthzPolicy(allowAll = true),
        MenuKey.MY_ACCOUNT to AuthzPolicy(allowAll = true),
        MenuKey.CUSTOMERS to AuthzPolicy(
            requiredAny = setOf(ScopeKey.CUSTOMER_VIEW, ScopeKey.CUSTOMER_CREATE, ScopeKey.CUSTOMER_DELETE)
        ),
        MenuKey.PRODUCTS_SECTION to AuthzPolicy(
            requiredAny = setOf(ScopeKey.PRODUCTS_VIEW, ScopeKey.PRODUCTS_CREATE, ScopeKey.PRODUCTS_DELETE)
        ),
        MenuKey.PRODUCTS_VIEW to AuthzPolicy(
            requiredAny = setOf(ScopeKey.PRODUCTS_VIEW, ScopeKey.PRODUCTS_CREATE, ScopeKey.PRODUCTS_DELETE)
        ),
        MenuKey.PRODUCTS_CREATE to AuthzPolicy(requiredAny = setOf(ScopeKey.PRODUCTS_CREATE)),
        MenuKey.ORDERS_SECTION to AuthzPolicy(
            requiredAny = setOf(
                ScopeKey.INVOICE_VIEW,
                ScopeKey.INVOICE_CREATE,
                ScopeKey.INVOICE_CREDIT_NOTES,
                ScopeKey.INVOICE_CREATE_DRAFT,
                ScopeKey.INVOICE_CANCEL
            )
        ),
        MenuKey.ORDERS_LIST to AuthzPolicy(requiredAny = setOf(ScopeKey.INVOICE_VIEW)),
        MenuKey.ORDERS_CREATE to AuthzPolicy(
            requiredAny = setOf(ScopeKey.INVOICE_CREATE, ScopeKey.INVOICE_CREATE_DRAFT)
        ),
        MenuKey.QUOTES_LIST to AuthzPolicy(
            requiredAny = setOf(ScopeKey.QUOTES_VIEW, ScopeKey.QUOTES_CREATE, ScopeKey.QUOTES_ACCEPT),
            betaFeature = BetaFeature.QUOTES
        ),
        MenuKey.RECURRING_LIST to AuthzPolicy(
            requiredAny = setOf(
                ScopeKey.RECURRING_VIEW,
                ScopeKey.RECURRING_CREATE,
                ScopeKey.RECURRING_CANCEL,
                ScopeKey.RECURRING_STOP,
                ScopeKey.RECURRING_EXECUTE_NOW,
                ScopeKey.RECURRING_SKIP_NEXT
            ),
            betaFeature = BetaFeature.RECURRING_INVOICING
        ),
        MenuKey.EXPENSES to AuthzPolicy(
            requiredAny = setOf(ScopeKey.EXPENSES_VIEW, ScopeKey.EXPENSES_CREATE, ScopeKey.EXPENSES_DELETE)
        ),
        MenuKey.REPORTS to AuthzPolicy(
            requiredAny = setOf(ScopeKey.REPORTS_VIEW, ScopeKey.REPORTS_EXECUTE),
            betaFeature = BetaFeature.REAL_TIME_REPORTS
        ),
        MenuKey.SETTINGS to AuthzPolicy(allowAll = true),
    )

    private val routePolicies = mapOf(
        RouteKey.HOME to AuthzPolicy(allowAll = true),
        RouteKey.HOME_SUMMARY to AuthzPolicy(
            requiredAny = setOf(ScopeKey.HOME_DASHBOARD),
            ownerBypass = true
        ),
        RouteKey.ORDERS_LIST to AuthzPolicy(requiredAny = setOf(ScopeKey.INVOICE_VIEW)),
        RouteKey.ORDER_DETAILS to AuthzPolicy(requiredAny = setOf(ScopeKey.INVOICE_VIEW)),
        RouteKey.INVOICE_PREVIEW to AuthzPolicy(requiredAny = setOf(ScopeKey.INVOICE_VIEW)),
        RouteKey.ORDERS_NEW to AuthzPolicy(
            requiredAny = setOf(ScopeKey.INVOICE_CREATE, ScopeKey.INVOICE_CREATE_DRAFT)
        ),
        RouteKey.CUSTOMERS_LIST to AuthzPolicy(
            requiredAny = setOf(ScopeKey.CUSTOMER_VIEW, ScopeKey.CUSTOMER_CREATE, ScopeKey.CUSTOMER_DELETE)
        ),
        RouteKey.CUSTOMER_DETAILS to AuthzPolicy(
            requiredAny = setOf(ScopeKey.CUSTOMER_VIEW, ScopeKey.CUSTOMER_CREATE, ScopeKey.CUSTOMER_DELETE)
        ),
        RouteKey.CUSTOMER_FORM to AuthzPolicy(requiredAny = setOf(ScopeKey.CUSTOMER_CREATE)),
        RouteKey.PRODUCTS_LIST to AuthzPolicy(
            requiredAny = setOf(ScopeKey.PRODUCTS_VIEW, ScopeKey.PRODUCTS_CREATE, ScopeKey.PRODUCTS_DELETE)
        ),
        RouteKey.PRODUCT_DETAILS to AuthzPolicy(
            requiredAny = setOf(ScopeKey.PRODUCTS_VIEW, ScopeKey.PRODUCTS_CREATE)
        ),
        RouteKey.PRODUCT_ADD to AuthzPolicy(requiredAny = setOf(ScopeKey.PRODUCTS_CREATE)),
        RouteKey.CATEGORY_MANAGE to AuthzPolicy(requiredAny = setOf(ScopeKey.PRODUCTS_CREATE)),
        RouteKey.QUOTES_LIST to AuthzPolicy(
            requiredAny = setOf(ScopeKey.QUOTES_VIEW, ScopeKey.QUOTES_CREATE, ScopeKey.QUOTES_ACCEPT),
            betaFeature = BetaFeature.QUOTES
        ),
        RouteKey.QUOTE_DETAILS to AuthzPolicy(
            requiredAny = setOf(ScopeKey.QUOTES_VIEW, ScopeKey.QUOTES_CREATE, ScopeKey.QUOTES_ACCEPT),
            betaFeature = BetaFeature.QUOTES
        ),
        RouteKey.QUOTE_NEW to AuthzPolicy(
            requiredAny = setOf(ScopeKey.QUOTES_CREATE),
            betaFeature = BetaFeature.QUOTES
        ),
        RouteKey.EXPENSES_LIST to AuthzPolicy(
            requiredAny = setOf(ScopeKey.EXPENSES_VIEW, ScopeKey.EXPENSES_CREATE, ScopeKey.EXPENSES_DELETE)
        ),
        RouteKey.EXPENSE_DETAILS to AuthzPolicy(
            requiredAny = setOf(ScopeKey.EXPENSES_VIEW, ScopeKey.EXPENSES_CREATE, ScopeKey.EXPENSES_DELETE)
        ),
        RouteKey.EXPENSE_NEW to AuthzPolicy(requiredAny = setOf(ScopeKey.EXPENSES_CREATE)),
        RouteKey.SETTINGS_PAGE to AuthzPolicy(allowAll = true),
        RouteKey.PAYMENTS_PAGE to AuthzPolicy(
            requiredAny = setOf(
                ScopeKey.PAYMENTS_CONFIGURE,
                ScopeKey.PAYMENTS_VIEW,
                ScopeKey.PAYMENTS_PAY
            )
        ),
        RouteKey.SETTINGS_BRANCHES_OWNER to AuthzPolicy(ownerOnly = true),
        RouteKey.SETTINGS_EXPENSE_ACCOUNTS_OWNER to AuthzPolicy(ownerOnly = true),
        RouteKey.SETTINGS_SUB_USERS to AuthzPolicy(ownerOnly = true, betaFeature = BetaFeature.MULTI_USERS),
        RouteKey.SETTINGS_POS_DEVICES to AuthzPolicy(requiredAny = setOf(ScopeKey.SETTINGS_MODIFY_POS_DEVICES)),
        RouteKey.ACH_PAYMENT_DETAILS to AuthzPolicy(requiredAny = setOf(ScopeKey.ACH_PAYMENT_VIEW)),
        RouteKey.RECURRING_LIST to AuthzPolicy(
            requiredAny = setOf(ScopeKey.RECURRING_VIEW),
            betaFeature = BetaFeature.RECURRING_INVOICING
        ),
        RouteKey.RECURRING_DETAILS to AuthzPolicy(
            requiredAny = setOf(ScopeKey.RECURRING_VIEW),
            betaFeature = BetaFeature.RECURRING_INVOICING
        ),
        RouteKey.RECURRING_NEW to AuthzPolicy(
            requiredAny = setOf(ScopeKey.RECURRING_CREATE),
            betaFeature = BetaFeature.RECURRING_INVOICING
        ),
        RouteKey.REPORTS_PAGE to AuthzPolicy(
            requiredAny = setOf(ScopeKey.REPORTS_VIEW),
            betaFeature = BetaFeature.REAL_TIME_REPORTS
        ),
        RouteKey.MY_ACCOUNT to AuthzPolicy(allowAll = true),
    )

    private val actionPolicies = mapOf(
        ActionKey.ORDERS_OPEN_CREATE to AuthzPolicy(
            requiredAny = setOf(ScopeKey.INVOICE_CREATE, ScopeKey.INVOICE_CREATE_DRAFT)
        ),
        ActionKey.ORDERS_CREATE to AuthzPolicy(requiredAny = setOf(ScopeKey.INVOICE_CREATE)),
        ActionKey.ORDERS_CREATE_DRAFT to AuthzPolicy(requiredAny = setOf(ScopeKey.INVOICE_CREATE_DRAFT)),
        ActionKey.ORDERS_PAYMENT_LINK to AuthzPolicy(requiredAny = setOf(ScopeKey.INVOICE_CREATE_PAYMENT_LINK)),
        ActionKey.ORDERS_MANUAL_PAYMENT to AuthzPolicy(requiredAny = setOf(ScopeKey.INVOICE_CREATE)),
        ActionKey.ORDERS_MARK_PAID to AuthzPolicy(requiredAny = setOf(ScopeKey.INVOICE_CREATE)),
        ActionKey.ORDERS_CANCEL to AuthzPolicy(requiredAny = setOf(ScopeKey.INVOICE_CANCEL)),
        ActionKey.ORDERS_CREDIT_NOTE to AuthzPolicy(requiredAny = setOf(ScopeKey.INVOICE_CREDIT_NOTES)),
        ActionKey.ORDERS_CUSTOM_PRODUCT to AuthzPolicy(requiredAny = setOf(ScopeKey.INVOICE_CUSTOM_PRODUCT)),
        ActionKey.ORDERS_EDIT_PRODUCT to AuthzPolicy(requiredAny = setOf(ScopeKey.INVOICE_EDIT_PRODUCT)),
        ActionKey.ORDERS_YAPPY_ONSITE to AuthzPolicy(requiredAny = setOf(ScopeKey.INVOICE_YAPPY_ONSITE)),
        ActionKey.ACH_PAYMENT_APPROVE to AuthzPolicy(requiredAny = setOf(ScopeKey.ACH_PAYMENT_APPROVE)),
        ActionKey.ACH_PAYMENT_REJECT to AuthzPolicy(requiredAny = setOf(ScopeKey.ACH_PAYMENT_REJECT)),
        ActionKey.PAYMENTS_CONFIGURE to AuthzPolicy(requiredAny = setOf(ScopeKey.PAYMENTS_CONFIGURE)),
        ActionKey.PAYMENTS_VIEW to AuthzPolicy(
            requiredAny = setOf(
                ScopeKey.PAYMENTS_CONFIGURE,
                ScopeKey.PAYMENTS_VIEW,
                ScopeKey.PAYMENTS_PAY
            )
        ),
        ActionKey.PAYMENTS_PAY to AuthzPolicy(requiredAny = setOf(ScopeKey.PAYMENTS_PAY)),
        ActionKey.CUSTOMERS_CREATE to AuthzPolicy(requiredAny = setOf(ScopeKey.CUSTOMER_CREATE)),
        ActionKey.CUSTOMERS_UPDATE to AuthzPolicy(requiredAny = setOf(ScopeKey.CUSTOMER_CREATE)),
        ActionKey.CUSTOMERS_DELETE to AuthzPolicy(requiredAny = setOf(ScopeKey.CUSTOMER_DELETE)),
        ActionKey.PRODUCTS_CREATE to AuthzPolicy(requiredAny = setOf(ScopeKey.PRODUCTS_CREATE)),
        ActionKey.PRODUCTS_UPDATE to AuthzPolicy(requiredAny = setOf(ScopeKey.PRODUCTS_CREATE)),
        ActionKey.PRODUCTS_DELETE to AuthzPolicy(requiredAny = setOf(ScopeKey.PRODUCTS_DELETE)),
        ActionKey.PRODUCTS_MANAGE_CATEGORIES to AuthzPolicy(requiredAny = setOf(ScopeKey.PRODUCTS_CREATE)),
        ActionKey.QUOTES_CREATE to AuthzPolicy(
            requiredAny = setOf(ScopeKey.QUOTES_CREATE),
            betaFeature = BetaFeature.QUOTES
        ),
        ActionKey.QUOTES_UPDATE to AuthzPolicy(
            requiredAny = setOf(ScopeKey.QUOTES_CREATE),
            betaFeature = BetaFeature.QUOTES
        ),
        ActionKey.QUOTES_ACCEPT to AuthzPolicy(
            requiredAny = setOf(ScopeKey.QUOTES_ACCEPT),
            betaFeature = BetaFeature.QUOTES
        ),
        ActionKey.EXPENSES_CREATE to AuthzPolicy(requiredAny = setOf(ScopeKey.EXPENSES_CREATE)),
        ActionKey.EXPENSES_UPDATE to AuthzPolicy(requiredAny = setOf(ScopeKey.EXPENSES_CREATE)),
        ActionKey.EXPENSES_DELETE to AuthzPolicy(requiredAny = setOf(ScopeKey.EXPENSES_DELETE)),
        ActionKey.REPORTS_EXECUTE to AuthzPolicy(
            requiredAny = setOf(ScopeKey.REPORTS_EXECUTE),
            betaFeature = BetaFeature.REAL_TIME_REPORTS
        ),
        ActionKey.SETTINGS_MODIFY to AuthzPolicy(ownerOnly = true),
        ActionKey.SETTINGS_MODIFY_POS_DEVICES to AuthzPolicy(
            requiredAny = setOf(ScopeKey.SETTINGS_MODIFY_POS_DEVICES)
        ),
        ActionKey.RECURRING_CREATE to AuthzPolicy(
            requiredAny = setOf(ScopeKey.RECURRING_CREATE),
            betaFeature = BetaFeature.RECURRING_INVOICING
        ),
        ActionKey.RECURRING_CANCEL to AuthzPolicy(
            requiredAny = setOf(ScopeKey.RECURRING_CANCEL),
            betaFeature = BetaFeature.RECURRING_INVOICING
        ),
        ActionKey.RECURRING_STOP to AuthzPolicy(
            requiredAny = setOf(ScopeKey.RECURRING_STOP),
            betaFeature = BetaFeature.RECURRING_INVOICING
        ),
        ActionKey.RECURRING_EXECUTE_NOW to AuthzPolicy(
            requiredAny = setOf(ScopeKey.RECURRING_EXECUTE_NOW),
            betaFeature = BetaFeature.RECURRING_INVOICING
        ),
        ActionKey.RECURRING_SKIP_NEXT to AuthzPolicy(
            requiredAny = setOf(ScopeKey.RECURRING_SKIP_NEXT),
            betaFeature = BetaFeature.RECURRING_INVOICING
        ),
    )

    fun canRoute(routeKey: RouteKey, user: User?, betaSnapshot: Set<BetaFeature>): Boolean {
        if (!PosDevicePermissionGate.canRoute(routeKey)) return false
        return evaluate(routePolicies.getValue(routeKey), user, betaSnapshot)
    }

    fun canMenu(menuKey: MenuKey, user: User?, betaSnapshot: Set<BetaFeature>): Boolean {
        if (!PosDevicePermissionGate.canMenu(menuKey)) return false
        return evaluate(menuPolicies.getValue(menuKey), user, betaSnapshot)
    }

    fun canAction(actionKey: ActionKey, user: User?, betaSnapshot: Set<BetaFeature>): Boolean {
        if (!PosDevicePermissionGate.canAction(actionKey)) return false
        return evaluate(actionPolicies.getValue(actionKey), user, betaSnapshot)
    }

    fun resolveFallbackRoute(user: User?, betaSnapshot: Set<BetaFeature>): RouteKey? {
        val order = listOf(
            RouteKey.HOME,
            RouteKey.ORDERS_LIST,
            RouteKey.CUSTOMERS_LIST,
            RouteKey.PRODUCTS_LIST,
            RouteKey.EXPENSES_LIST,
            RouteKey.SETTINGS_PAGE
        )
        return order.firstOrNull { canRoute(it, user, betaSnapshot) }
    }

    private fun evaluate(policy: AuthzPolicy, user: User?, betaSnapshot: Set<BetaFeature>): Boolean {
        user ?: return false

        if (policy.allowAll) return true
        if (policy.ownerOnly && user.isSubUser) return false
        if (policy.betaFeature != null && policy.betaFeature !in betaSnapshot) return false
        if (policy.ownerOnly && user.isOwnerMain) return true
        if (policy.ownerBypass && user.isOwnerMain) return true
        if (policy.requiredAll.isNotEmpty() && !policy.requiredAll.all(user.scopes::contains)) return false
        if (policy.requiredAny.isNotEmpty()) return policy.requiredAny.any(user.scopes::contains)

        return user.isOwnerMain
    }
}
