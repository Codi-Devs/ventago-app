package com.teco.ventago.core.authz

import com.teco.ventago.core.beta.BetaFeature
import com.teco.ventago.features.auth.domain.model.User
import com.teco.ventago.features.pos.provisioning.domain.model.PosDevicePermissions
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthzEvaluatorTest {
    @BeforeTest
    fun clearPosPermissionGate() {
        PosDevicePermissionGate.clear()
    }

    private fun owner(scopes: Set<String> = emptySet()) = User(
        uid = "owner",
        email = "owner@test.com",
        name = "Owner",
        premium = true,
        active = true,
        missingBusiness = false,
        userId = 1,
        businessIds = emptyList(),
        scopes = scopes,
        isSubUser = false,
        isOwnerMain = true,
    )

    private fun subUser(scopes: Set<String> = emptySet()) = User(
        uid = "sub",
        email = "sub@test.com",
        name = "Sub User",
        premium = false,
        active = true,
        missingBusiness = false,
        userId = 2,
        businessIds = emptyList(),
        scopes = scopes,
        isSubUser = true,
        isOwnerMain = false,
    )

    @Test
    fun allowAllRouteKeepsMyAccountAccessibleToSubUsers() {
        assertTrue(AuthzEvaluator.canRoute(RouteKey.MY_ACCOUNT, subUser(), emptySet()))
    }

    @Test
    fun homeSummaryAllowsScopedSubUserAndOwnerBypass() {
        assertTrue(AuthzEvaluator.canRoute(RouteKey.HOME_SUMMARY, subUser(setOf(ScopeKey.HOME_DASHBOARD)), emptySet()))
        assertTrue(AuthzEvaluator.canRoute(RouteKey.HOME_SUMMARY, owner(), emptySet()))
    }

    @Test
    fun allowAllHomeRouteLetsSubUserOpenHomeWithoutExplicitScope() {
        assertTrue(AuthzEvaluator.canRoute(RouteKey.HOME, owner(), emptySet()))
        assertTrue(AuthzEvaluator.canRoute(RouteKey.HOME, subUser(), emptySet()))
    }

    @Test
    fun betaGatedRoutesRequireEnabledFeature() {
        val user = subUser(setOf(ScopeKey.QUOTES_VIEW))

        assertFalse(AuthzEvaluator.canRoute(RouteKey.QUOTES_LIST, user, emptySet()))
        assertTrue(AuthzEvaluator.canRoute(RouteKey.QUOTES_LIST, user, setOf(BetaFeature.QUOTES)))
    }

    @Test
    fun inventoryMenuAndRouteRequireInventoryModuleBeta() {
        val owner = owner()
        val viewer = subUser(setOf(ScopeKey.INVENTORY_VIEW))

        assertFalse(AuthzEvaluator.canMenu(MenuKey.INVENTORY, owner, emptySet()))
        assertFalse(AuthzEvaluator.canRoute(RouteKey.INVENTORY_OPS, owner, emptySet()))
        assertFalse(AuthzEvaluator.canMenu(MenuKey.INVENTORY, viewer, emptySet()))
        assertFalse(AuthzEvaluator.canRoute(RouteKey.INVENTORY_OPS, viewer, emptySet()))

        val beta = setOf(BetaFeature.INVENTORY_MODULE)
        assertTrue(AuthzEvaluator.canMenu(MenuKey.INVENTORY, owner, beta))
        assertTrue(AuthzEvaluator.canRoute(RouteKey.INVENTORY_OPS, owner, beta))
        assertTrue(AuthzEvaluator.canMenu(MenuKey.INVENTORY, viewer, beta))
        assertTrue(AuthzEvaluator.canRoute(RouteKey.INVENTORY_OPS, viewer, beta))
    }

    @Test
    fun reportsRouteAndExecuteActionRequireRealTimeReportsBeta() {
        val viewer = subUser(setOf(ScopeKey.REPORTS_VIEW))
        val executor = subUser(setOf(ScopeKey.REPORTS_EXECUTE))

        assertFalse(AuthzEvaluator.canRoute(RouteKey.REPORTS_PAGE, viewer, emptySet()))
        assertFalse(AuthzEvaluator.canMenu(MenuKey.REPORTS, viewer, emptySet()))
        assertFalse(AuthzEvaluator.canAction(ActionKey.REPORTS_EXECUTE, executor, emptySet()))

        val beta = setOf(BetaFeature.REAL_TIME_REPORTS)
        assertTrue(AuthzEvaluator.canRoute(RouteKey.REPORTS_PAGE, viewer, beta))
        assertTrue(AuthzEvaluator.canMenu(MenuKey.REPORTS, viewer, beta))
        assertTrue(AuthzEvaluator.canAction(ActionKey.REPORTS_EXECUTE, executor, beta))
    }

    @Test
    fun fallbackRouteUsesConfiguredPriorityOrder() {
        val expensesUser = subUser(setOf(ScopeKey.EXPENSES_CREATE))

        assertEquals(RouteKey.HOME, AuthzEvaluator.resolveFallbackRoute(expensesUser, emptySet()))
        assertEquals(RouteKey.HOME, AuthzEvaluator.resolveFallbackRoute(subUser(), emptySet()))
    }

    @Test
    fun settingsRouteIsAvailableWithoutSettingsScope() {
        assertTrue(AuthzEvaluator.canRoute(RouteKey.SETTINGS_PAGE, subUser(), emptySet()))
    }

    @Test
    fun posDevicesSettingsRequiresDedicatedScopeForSubUsers() {
        assertFalse(AuthzEvaluator.canRoute(RouteKey.SETTINGS_POS_DEVICES, subUser(), emptySet()))
        assertFalse(AuthzEvaluator.canAction(ActionKey.SETTINGS_MODIFY_POS_DEVICES, subUser(), emptySet()))

        val scopedUser = subUser(setOf(ScopeKey.SETTINGS_MODIFY_POS_DEVICES))
        assertTrue(AuthzEvaluator.canRoute(RouteKey.SETTINGS_POS_DEVICES, scopedUser, emptySet()))
        assertTrue(AuthzEvaluator.canAction(ActionKey.SETTINGS_MODIFY_POS_DEVICES, scopedUser, emptySet()))
        assertTrue(AuthzEvaluator.canRoute(RouteKey.SETTINGS_POS_DEVICES, owner(), emptySet()))
    }

    @Test
    fun paymentLinkAndAchPoliciesRequireScopesWithoutPaymentsBeta() {
        val scopedUser = subUser(
            setOf(
                ScopeKey.INVOICE_CREATE_PAYMENT_LINK,
                ScopeKey.ACH_PAYMENT_VIEW,
                ScopeKey.ACH_PAYMENT_APPROVE,
                ScopeKey.ACH_PAYMENT_REJECT
            )
        )

        assertTrue(AuthzEvaluator.canAction(ActionKey.ORDERS_PAYMENT_LINK, scopedUser, emptySet()))
        assertTrue(AuthzEvaluator.canAction(ActionKey.ACH_PAYMENT_APPROVE, scopedUser, emptySet()))
        assertTrue(AuthzEvaluator.canAction(ActionKey.ACH_PAYMENT_REJECT, scopedUser, emptySet()))
        assertTrue(AuthzEvaluator.canRoute(RouteKey.ACH_PAYMENT_DETAILS, scopedUser, emptySet()))
        assertFalse(AuthzEvaluator.canRoute(RouteKey.PAYMENTS_PAGE, scopedUser, emptySet()))
    }

    @Test
    fun paymentsRouteDeniesSubUserWithoutScopes() {
        assertFalse(
            AuthzEvaluator.canRoute(
                RouteKey.PAYMENTS_PAGE,
                subUser(),
                emptySet()
            )
        )
    }

    @Test
    fun ownerCanAccessPaymentsAndYappyOnsiteQrWithoutScopes() {
        val owner = owner()

        assertTrue(AuthzEvaluator.canRoute(RouteKey.PAYMENTS_PAGE, owner, emptySet()))
        assertTrue(AuthzEvaluator.canAction(ActionKey.PAYMENTS_CONFIGURE, owner, emptySet()))
        assertTrue(AuthzEvaluator.canAction(ActionKey.PAYMENTS_PAY, owner, emptySet()))
        assertTrue(AuthzEvaluator.canAction(ActionKey.ORDERS_YAPPY_ONSITE, owner, emptySet()))
    }

    @Test
    fun paymentsRouteAllowsEachPaymentsScope() {
        assertTrue(AuthzEvaluator.canRoute(RouteKey.PAYMENTS_PAGE, subUser(setOf(ScopeKey.PAYMENTS_CONFIGURE)), emptySet()))
        assertTrue(AuthzEvaluator.canRoute(RouteKey.PAYMENTS_PAGE, subUser(setOf(ScopeKey.PAYMENTS_VIEW)), emptySet()))
        assertTrue(AuthzEvaluator.canRoute(RouteKey.PAYMENTS_PAGE, subUser(setOf(ScopeKey.PAYMENTS_PAY)), emptySet()))
    }

    @Test
    fun paymentsActionsRequireMatchingScopes() {
        val configureUser = subUser(setOf(ScopeKey.PAYMENTS_CONFIGURE))
        val payUser = subUser(setOf(ScopeKey.PAYMENTS_PAY))
        val viewUser = subUser(setOf(ScopeKey.PAYMENTS_VIEW))

        assertTrue(AuthzEvaluator.canAction(ActionKey.PAYMENTS_CONFIGURE, configureUser, emptySet()))
        assertFalse(AuthzEvaluator.canAction(ActionKey.PAYMENTS_PAY, configureUser, emptySet()))
        assertTrue(AuthzEvaluator.canAction(ActionKey.PAYMENTS_PAY, payUser, emptySet()))
        assertFalse(AuthzEvaluator.canAction(ActionKey.PAYMENTS_CONFIGURE, payUser, emptySet()))
        assertTrue(AuthzEvaluator.canAction(ActionKey.PAYMENTS_VIEW, viewUser, emptySet()))
    }

    @Test
    fun yappyOnsiteQrRequiresDedicatedInvoiceScope() {
        assertTrue(
            AuthzEvaluator.canAction(
                ActionKey.ORDERS_YAPPY_ONSITE,
                subUser(setOf(ScopeKey.INVOICE_YAPPY_ONSITE)),
                emptySet()
            )
        )
        assertFalse(
            AuthzEvaluator.canAction(
                ActionKey.ORDERS_YAPPY_ONSITE,
                subUser(setOf(ScopeKey.INVOICE_CREATE_PAYMENT_LINK)),
                emptySet()
            )
        )
    }

    @Test
    fun posDevicePermissionsRestrictRoutesAndActionsWithoutGrantingAccess() {
        val owner = owner()
        PosDevicePermissionGate.update(
            PosDevicePermissions(
                deviceId = "pos_123",
                expensesView = false,
                expensesCreate = false,
                productsView = true,
                productsCreate = false,
                clientsView = true,
                clientsCreate = false,
                quotesView = false,
                quotesCreate = false,
                paymentMethodsConfigure = false,
                paymentYappyOnsite = true,
                paymentLink = false,
                paymentManualMethods = true,
                reportsView = false,
            )
        )

        assertTrue(AuthzEvaluator.canRoute(RouteKey.PRODUCTS_LIST, owner, emptySet()))
        assertFalse(AuthzEvaluator.canAction(ActionKey.PRODUCTS_CREATE, owner, emptySet()))
        assertFalse(AuthzEvaluator.canRoute(RouteKey.EXPENSES_LIST, owner, emptySet()))
        assertFalse(AuthzEvaluator.canRoute(RouteKey.REPORTS_PAGE, owner, setOf(BetaFeature.REAL_TIME_REPORTS)))
        assertFalse(AuthzEvaluator.canAction(ActionKey.PAYMENTS_CONFIGURE, owner, emptySet()))
        assertFalse(AuthzEvaluator.canAction(ActionKey.ORDERS_PAYMENT_LINK, owner, emptySet()))
        assertTrue(AuthzEvaluator.canAction(ActionKey.ORDERS_YAPPY_ONSITE, owner, emptySet()))
        assertTrue(AuthzEvaluator.canAction(ActionKey.ORDERS_MANUAL_PAYMENT, owner, emptySet()))

        val customerSubUserWithoutJwtScope = subUser()
        assertFalse(AuthzEvaluator.canRoute(RouteKey.CUSTOMERS_LIST, customerSubUserWithoutJwtScope, emptySet()))
    }
}
