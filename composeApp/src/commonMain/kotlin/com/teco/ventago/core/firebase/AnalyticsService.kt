package com.teco.ventago.core.firebase

import androidx.compose.ui.text.intl.Locale
import com.teco.ventago.features.auth.domain.model.response.AuthResponse
import com.teco.ventago.features.business.domain.model.responses.BusinessRegisterResponse
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.FirebaseAnalytics
import dev.gitlive.firebase.analytics.FirebaseAnalyticsEvents
import dev.gitlive.firebase.analytics.FirebaseAnalyticsParam
import dev.gitlive.firebase.analytics.analytics

class AnalyticsService {
    val analytics: FirebaseAnalytics by lazy {
        Firebase.analytics
    }

    private val defaultParamsBundle: MutableMap<String, Any> = mutableMapOf(
        "country" to Locale.current.region,
        "language" to Locale.current.language
    )

    fun logEvent(event: String, bundle: Map<String, Any> = getAnalyticsBundle()) {
        try {
            analytics.logEvent(event, bundle)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setDefaultParametersEvent(userId: Int, businessId: Int, premium: Boolean) {
        defaultParamsBundle.put("business_id", businessId.toString())
        defaultParamsBundle.put("user_id", userId.toString())
        defaultParamsBundle.put("premium", premium)
    }

    fun setUserId(userId: Int) {
        analytics.setUserId(userId.toString())
    }

    fun setDefaultEventParameters(default: Map<String, Any>) {
        analytics.setDefaultEventParameters(default as Map<String, String>)
    }


    /**
     * Returns a map with the current country and language.
     */
    fun getAnalyticsBundle(): Map<String, Any> {
        return mapOf(
            "country" to Locale.current.region,
            "language" to Locale.current.language
        )
    }

    fun authBundle(response: AuthResponse, googleLogin: Boolean = false): Map<String, Any> {
        return mapOf(
            "user_id" to response.userId.toString(),
            "uid" to response.uid,
            "email" to response.email,
            "missing_business" to response.missingBusiness.toString(),
            "business_id" to if(response.businesses.isNotEmpty())
                response.businesses[0].toString() else "-1",
            "premium" to response.premium.toString(),
            "login_type" to if (googleLogin) "google" else "email",
            "country" to Locale.current.region,
            "language" to Locale.current.language
        )
    }

    fun businessCreatedBundle(response: BusinessRegisterResponse): Map<String, Any>  {
        return mapOf(
            "business_id" to response.businessId.toString(),
            "menu_id" to response.menuId.toString(),
            "country" to Locale.current.region,
            "language" to Locale.current.language
        )
    }

    fun defaultBundle(response: AuthResponse): Map<String, Any>  {
        return mapOf(
            "user_id" to response.userId.toString(),
            "business_id" to if(response.businesses.isNotEmpty())
                response.businesses[0].toString() else "-1",
            "premium" to response.premium.toString(),
            "country" to Locale.current.region,
            "language" to Locale.current.language
        )
    }

    fun logScreenView(screenName: String) {
        FirebaseAnalyticsEvents.SCREEN_VIEW
        analytics.logEvent(FirebaseAnalyticsEvents.SCREEN_VIEW, defaultParamsBundle.apply {
            FirebaseAnalyticsParam.SCREEN_NAME to screenName
            FirebaseAnalyticsParam.SCREEN_CLASS to "MainActivity"
        })
    }
}