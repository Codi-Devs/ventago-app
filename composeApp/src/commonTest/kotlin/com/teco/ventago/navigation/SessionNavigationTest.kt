package com.teco.ventago.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionNavigationTest {

    @Test
    fun authenticatedSessionStartsAtHomeNotLogin() {
        val start = SessionNavigation.resolveSessionStart(
            isAuthenticated = true,
            missingBusiness = false,
            invoicingConfigured = true,
        )

        assertEquals(PosScreens.HomeScreen.name, start.navHostStart)
        assertEquals(PosScreens.LoginScreen.name, start.loginGraphStart)
        assertEquals(
            PosScreens.HomeScreen.name,
            SessionNavigation.resolveAuthDestination(
                isAuthenticated = true,
                missingBusiness = false,
                invoicingConfigured = true,
            )
        )
    }

    @Test
    fun guestSessionStartsAtLogin() {
        val start = SessionNavigation.resolveSessionStart(
            isAuthenticated = false,
            missingBusiness = false,
            invoicingConfigured = true,
        )

        assertEquals(PosScreens.LoginRegister.name, start.navHostStart)
        assertEquals(PosScreens.LoginScreen.name, start.loginGraphStart)
        assertEquals(
            PosScreens.LoginScreen.name,
            SessionNavigation.resolveAuthDestination(
                isAuthenticated = false,
                missingBusiness = false,
                invoicingConfigured = true,
            )
        )
    }

    @Test
    fun unauthenticatedUserNeverLandsOnInvoiceSetup() {
        assertEquals(
            PosScreens.LoginScreen.name,
            SessionNavigation.resolveAuthDestination(
                isAuthenticated = false,
                missingBusiness = false,
                invoicingConfigured = false,
            )
        )
    }

    @Test
    fun authenticatedUserWithoutBusinessStartsAtBusinessRegister() {
        val start = SessionNavigation.resolveSessionStart(
            isAuthenticated = true,
            missingBusiness = true,
            invoicingConfigured = true,
        )

        assertEquals(PosScreens.LoginRegister.name, start.navHostStart)
        assertEquals(PosScreens.BusinessRegisterScreen.name, start.loginGraphStart)
        assertEquals(
            PosScreens.BusinessRegisterScreen.name,
            SessionNavigation.resolveAuthDestination(
                isAuthenticated = true,
                missingBusiness = true,
                invoicingConfigured = true,
            )
        )
    }

    @Test
    fun authenticatedUserWithoutInvoicingStartsAtInvoiceLanding() {
        val start = SessionNavigation.resolveSessionStart(
            isAuthenticated = true,
            missingBusiness = false,
            invoicingConfigured = false,
        )

        assertEquals(PosScreens.LoginRegister.name, start.navHostStart)
        assertEquals(PosScreens.InvoiceLandingScreen.name, start.loginGraphStart)
        assertEquals(
            PosScreens.InvoiceLandingScreen.name,
            SessionNavigation.resolveAuthDestination(
                isAuthenticated = true,
                missingBusiness = false,
                invoicingConfigured = false,
            )
        )
    }

    @Test
    fun initialRedirectStaysOnSplashUntilStartupRouteNeedsToMove() {
        assertFalse(
            SessionNavigation.shouldRedirect(
                currentRoute = PosScreens.HomeScreen.name,
                targetRoute = PosScreens.HomeScreen.name,
                isInitial = true,
                bucketChanged = false,
            )
        )
        assertTrue(
            SessionNavigation.shouldRedirect(
                currentRoute = PosScreens.LoginScreen.name,
                targetRoute = PosScreens.HomeScreen.name,
                isInitial = true,
                bucketChanged = false,
            )
        )
        assertFalse(
            SessionNavigation.shouldRedirect(
                currentRoute = PosScreens.OrderDetailsScreen.name,
                targetRoute = PosScreens.HomeScreen.name,
                isInitial = true,
                bucketChanged = false,
            )
        )
    }

    @Test
    fun authBucketChangeStillRedirectsFromInAppRoutes() {
        assertTrue(
            SessionNavigation.shouldRedirect(
                currentRoute = PosScreens.SettingsScreen.name,
                targetRoute = PosScreens.LoginScreen.name,
                isInitial = false,
                bucketChanged = true,
            )
        )
        assertFalse(
            SessionNavigation.shouldRedirect(
                currentRoute = PosScreens.HomeScreen.name,
                targetRoute = PosScreens.HomeScreen.name,
                isInitial = false,
                bucketChanged = true,
            )
        )
    }
}
