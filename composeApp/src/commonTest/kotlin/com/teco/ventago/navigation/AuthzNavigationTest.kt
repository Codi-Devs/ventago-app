package com.teco.ventago.navigation

import com.teco.ventago.core.authz.ScopeKey
import com.teco.ventago.features.auth.domain.model.User
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthzNavigationTest {

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

    private fun subUser(scopes: Set<String>) = User(
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
    fun ownerBottomNavIncludesSummaryByDefault() {
        assertEquals(
            listOf(
                BottomNavKey.HOME,
                BottomNavKey.SUMMARY,
                BottomNavKey.ORDERS,
                BottomNavKey.PRODUCTS,
                BottomNavKey.SETTINGS
            ),
            visibleBottomNavKeys(owner(), emptySet())
        )
    }

    @Test
    fun subUserBottomNavOmitsSummaryAndUnauthorizedSections() {
        val nav = visibleBottomNavKeys(
            subUser(
                setOf(
                    ScopeKey.HOME_DASHBOARD,
                    ScopeKey.INVOICE_VIEW,
                    ScopeKey.SETTINGS_VIEW
                )
            ),
            emptySet()
        )

        assertEquals(
            listOf(
                BottomNavKey.HOME,
                BottomNavKey.ORDERS,
                BottomNavKey.SETTINGS
            ),
            nav
        )
    }
}
