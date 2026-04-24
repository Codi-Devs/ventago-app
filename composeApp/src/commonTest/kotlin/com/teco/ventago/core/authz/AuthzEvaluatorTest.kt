package com.teco.ventago.core.authz

import com.teco.ventago.core.beta.BetaFeature
import com.teco.ventago.features.auth.domain.model.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthzEvaluatorTest {

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
    fun paymentLinkAndAchPoliciesRequirePaymentsBetaAndScopes() {
        val scopedUser = subUser(
            setOf(
                ScopeKey.INVOICE_CREATE_PAYMENT_LINK,
                ScopeKey.ACH_PAYMENT_VIEW,
                ScopeKey.ACH_PAYMENT_APPROVE,
                ScopeKey.ACH_PAYMENT_REJECT
            )
        )

        assertFalse(AuthzEvaluator.canAction(ActionKey.ORDERS_PAYMENT_LINK, scopedUser, emptySet()))
        assertFalse(AuthzEvaluator.canRoute(RouteKey.ACH_PAYMENT_DETAILS, scopedUser, emptySet()))
        assertFalse(AuthzEvaluator.canRoute(RouteKey.PAYMENTS_PAGE, scopedUser, emptySet()))

        val beta = setOf(BetaFeature.PAYMENTS)
        assertTrue(AuthzEvaluator.canAction(ActionKey.ORDERS_PAYMENT_LINK, scopedUser, beta))
        assertTrue(AuthzEvaluator.canAction(ActionKey.ACH_PAYMENT_APPROVE, scopedUser, beta))
        assertTrue(AuthzEvaluator.canAction(ActionKey.ACH_PAYMENT_REJECT, scopedUser, beta))
        assertTrue(AuthzEvaluator.canRoute(RouteKey.ACH_PAYMENT_DETAILS, scopedUser, beta))
        assertTrue(AuthzEvaluator.canRoute(RouteKey.PAYMENTS_PAGE, scopedUser, beta))
    }

    @Test
    fun paymentsRouteDeniesSubUserWithoutScopesEvenIfPaymentsBetaEnabled() {
        assertFalse(
            AuthzEvaluator.canRoute(
                RouteKey.PAYMENTS_PAGE,
                subUser(),
                setOf(BetaFeature.PAYMENTS)
            )
        )
    }
}
