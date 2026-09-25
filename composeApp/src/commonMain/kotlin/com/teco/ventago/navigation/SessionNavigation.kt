package com.teco.ventago.navigation

data class SessionStartDestinations(
    val navHostStart: String,
    val loginGraphStart: String,
)

object SessionNavigation {
    fun resolveAuthDestination(
        isAuthenticated: Boolean,
        missingBusiness: Boolean,
        invoicingConfigured: Boolean,
    ): String {
        if (!isAuthenticated) {
            return PosScreens.LoginScreen.name
        }
        if (missingBusiness) {
            return PosScreens.BusinessRegisterScreen.name
        }
        if (!invoicingConfigured) {
            return PosScreens.InvoiceLandingScreen.name
        }
        return PosScreens.HomeScreen.name
    }

    fun resolveSessionStart(
        isAuthenticated: Boolean,
        missingBusiness: Boolean,
        invoicingConfigured: Boolean,
    ): SessionStartDestinations {
        if (!isAuthenticated) {
            return SessionStartDestinations(
                navHostStart = PosScreens.LoginRegister.name,
                loginGraphStart = PosScreens.LoginScreen.name,
            )
        }
        if (missingBusiness) {
            return SessionStartDestinations(
                navHostStart = PosScreens.LoginRegister.name,
                loginGraphStart = PosScreens.BusinessRegisterScreen.name,
            )
        }
        if (!invoicingConfigured) {
            return SessionStartDestinations(
                navHostStart = PosScreens.LoginRegister.name,
                loginGraphStart = PosScreens.InvoiceLandingScreen.name,
            )
        }
        return SessionStartDestinations(
            navHostStart = PosScreens.HomeScreen.name,
            loginGraphStart = PosScreens.LoginScreen.name,
        )
    }

    fun isStartupRoute(route: String?): Boolean {
        return route == null ||
            route == PosScreens.LoginRegister.name ||
            route == PosScreens.LoginScreen.name ||
            route == PosScreens.BusinessRegisterScreen.name ||
            route == PosScreens.InvoiceLandingScreen.name
    }

    fun shouldRedirect(
        currentRoute: String?,
        targetRoute: String,
        isInitial: Boolean,
        bucketChanged: Boolean,
    ): Boolean {
        if (currentRoute == targetRoute) {
            return false
        }
        if (bucketChanged) {
            return true
        }
        return isInitial && isStartupRoute(currentRoute)
    }
}
