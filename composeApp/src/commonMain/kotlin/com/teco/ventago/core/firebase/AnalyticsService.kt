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

    private fun sanitizedString(value: String?): String? {
        val safe = value?.trim()
        return if (safe.isNullOrBlank()) null else safe
    }

    private fun buildBundle(params: Map<String, Any?>): Map<String, Any> {
        val bundle = getAnalyticsBundle().toMutableMap()
        params.forEach { (key, value) ->
            when (value) {
                null -> Unit
                is String -> if (value.isNotBlank()) bundle[key] = value
                else -> bundle[key] = value
            }
        }
        return bundle
    }

    private fun logPaymentEvent(
        event: String,
        surface: String,
        flow: String,
        action: String,
        extraParams: Map<String, Any?> = emptyMap()
    ) {
        val params = mutableMapOf<String, Any?>(
            "surface" to surface,
            "flow" to flow,
            "action" to action,
        )
        params.putAll(extraParams)
        logEvent(event, buildBundle(params))
    }

    fun sanitizeErrorCode(code: String?): String {
        val normalized = code
            ?.trim()
            ?.uppercase()
            ?.replace("-", "_")
            ?.replace(" ", "_")
            .orEmpty()
        return if (normalized.isBlank()) "UNKNOWN" else normalized.take(64)
    }

    fun extractErrorCode(throwable: Throwable?): String {
        val raw = throwable?.message.orEmpty()
        val fromJson = Regex("\"(?:error|errorCode)\"\\s*:\\s*\"([A-Za-z0-9_-]+)\"")
            .find(raw)
            ?.groupValues
            ?.getOrNull(1)
        val fromToken = Regex("\\b[A-Z]{2,}(?:_[A-Z0-9]+)+\\b")
            .find(raw)
            ?.value
        return sanitizeErrorCode(fromJson ?: fromToken)
    }

    // Payments core
    fun logPaymentSettingsViewed() {
        logPaymentEvent(
            event = "payment_settings_viewed",
            surface = "settings_payment_methods",
            flow = "payments_core",
            action = "open",
        )
    }

    fun logPaymentOnboardingViewed(source: String) {
        logPaymentEvent(
            event = "payment_onboarding_viewed",
            surface = "settings_payment_methods",
            flow = "payment_onboarding",
            action = "view",
            extraParams = mapOf("source" to source)
        )
    }

    fun logPaymentOnboardingStarted(source: String) {
        logPaymentEvent(
            event = "payment_onboarding_started",
            surface = "settings_payment_methods",
            flow = "payment_onboarding",
            action = "start_click",
            extraParams = mapOf("source" to source)
        )
    }

    fun logPaymentOnboardingBlocked(source: String, errorCode: String?) {
        logPaymentEvent(
            event = "payment_onboarding_blocked",
            surface = "settings_payment_methods",
            flow = "payment_onboarding",
            action = "blocked",
            extraParams = mapOf(
                "source" to source,
                "error_code" to sanitizeErrorCode(errorCode)
            )
        )
    }

    fun logPaymentOnboardingCompleted(source: String) {
        logPaymentEvent(
            event = "payment_onboarding_completed",
            surface = "settings_payment_methods",
            flow = "payment_onboarding",
            action = "completed",
            extraParams = mapOf("source" to source)
        )
    }

    fun logPaymentOnboardingFailed(source: String, errorCode: String?) {
        logPaymentEvent(
            event = "payment_onboarding_failed",
            surface = "settings_payment_methods",
            flow = "payment_onboarding",
            action = "failed",
            extraParams = mapOf(
                "source" to source,
                "error_code" to sanitizeErrorCode(errorCode)
            )
        )
    }

    fun logPaymentOnboardingSkipped(source: String) {
        logPaymentEvent(
            event = "payment_onboarding_skipped",
            surface = "settings_payment_methods",
            flow = "payment_onboarding",
            action = "skipped",
            extraParams = mapOf("source" to source)
        )
    }

    fun logPaymentMethodConfigAttempted(
        paymentMethod: String,
        mode: String,
    ) {
        logPaymentEvent(
            event = "payment_method_config_attempted",
            surface = "settings_payment_methods",
            flow = "payment_method_config",
            action = "attempt",
            extraParams = mapOf(
                "payment_method" to paymentMethod,
                "mode" to mode,
                "outcome" to "attempted",
            )
        )
    }

    fun logPaymentMethodConfigSucceeded(
        paymentMethod: String,
        mode: String,
    ) {
        logPaymentEvent(
            event = "payment_method_config_succeeded",
            surface = "settings_payment_methods",
            flow = "payment_method_config",
            action = "success",
            extraParams = mapOf(
                "payment_method" to paymentMethod,
                "mode" to mode,
                "outcome" to "success",
            )
        )
    }

    fun logPaymentMethodConfigFailed(
        paymentMethod: String,
        mode: String,
        errorCode: String?,
    ) {
        logPaymentEvent(
            event = "payment_method_config_failed",
            surface = "settings_payment_methods",
            flow = "payment_method_config",
            action = "failed",
            extraParams = mapOf(
                "payment_method" to paymentMethod,
                "mode" to mode,
                "outcome" to "failed",
                "error_code" to sanitizeErrorCode(errorCode),
            )
        )
    }

    fun logPrinterOnboardingOpened(source: String, step: String) {
        logPaymentEvent(
            event = "printer_onboarding_opened",
            surface = "settings_printer",
            flow = "printer_onboarding",
            action = "open",
            extraParams = mapOf(
                "source" to source,
                "step" to step
            )
        )
    }

    fun logPrinterOnboardingStepViewed(source: String, step: String) {
        logPaymentEvent(
            event = "printer_onboarding_step_viewed",
            surface = "settings_printer",
            flow = "printer_onboarding",
            action = "view_step",
            extraParams = mapOf(
                "source" to source,
                "step" to step
            )
        )
    }

    fun logPrinterOnboardingDismissed(source: String, step: String) {
        logPaymentEvent(
            event = "printer_onboarding_dismissed",
            surface = "settings_printer",
            flow = "printer_onboarding",
            action = "dismiss",
            extraParams = mapOf(
                "source" to source,
                "step" to step
            )
        )
    }

    fun logPrinterOnboardingCompleted(source: String) {
        logPaymentEvent(
            event = "printer_onboarding_completed",
            surface = "settings_printer",
            flow = "printer_onboarding",
            action = "completed",
            extraParams = mapOf(
                "source" to source,
                "step" to "success"
            )
        )
    }

    fun logPrinterOnboardingActionClicked(source: String, step: String, actionValue: String) {
        logPaymentEvent(
            event = "printer_onboarding_action_clicked",
            surface = "settings_printer",
            flow = "printer_onboarding",
            action = actionValue,
            extraParams = mapOf(
                "source" to source,
                "step" to step
            )
        )
    }

    fun logPrinterConfigAttempted(source: String, step: String, mode: String) {
        logPaymentEvent(
            event = "printer_config_attempted",
            surface = "settings_printer",
            flow = "printer_config",
            action = "attempt",
            extraParams = mapOf(
                "source" to source,
                "step" to step,
                "mode" to mode,
                "outcome" to "attempted"
            )
        )
    }

    fun logPrinterConfigSucceeded(source: String, step: String, mode: String) {
        logPaymentEvent(
            event = "printer_config_succeeded",
            surface = "settings_printer",
            flow = "printer_config",
            action = "success",
            extraParams = mapOf(
                "source" to source,
                "step" to step,
                "mode" to mode,
                "outcome" to "success"
            )
        )
    }

    fun logPrinterConfigFailed(source: String, step: String, mode: String, errorCode: String?) {
        logPaymentEvent(
            event = "printer_config_failed",
            surface = "settings_printer",
            flow = "printer_config",
            action = "failed",
            extraParams = mapOf(
                "source" to source,
                "step" to step,
                "mode" to mode,
                "outcome" to "failed",
                "error_code" to sanitizeErrorCode(errorCode),
            )
        )
    }

    fun logExpensePaymentActionOpened(mode: String) {
        logPaymentEvent(
            event = "expense_payment_action_opened",
            surface = "expense_details",
            flow = "expense_payments",
            action = "open_action",
            extraParams = mapOf("mode" to mode)
        )
    }

    fun logExpensePaymentSubmitAttempted(mode: String) {
        logPaymentEvent(
            event = "expense_payment_submit_attempted",
            surface = "expense_details",
            flow = "expense_payments",
            action = "submit",
            extraParams = mapOf(
                "mode" to mode,
                "outcome" to "attempted"
            )
        )
    }

    fun logExpensePaymentSubmitSucceeded(mode: String) {
        logPaymentEvent(
            event = "expense_payment_submit_succeeded",
            surface = "expense_details",
            flow = "expense_payments",
            action = "submit_success",
            extraParams = mapOf(
                "mode" to mode,
                "outcome" to "success"
            )
        )
    }

    fun logExpensePaymentSubmitFailed(mode: String, errorCode: String?) {
        logPaymentEvent(
            event = "expense_payment_submit_failed",
            surface = "expense_details",
            flow = "expense_payments",
            action = "submit_failed",
            extraParams = mapOf(
                "mode" to mode,
                "outcome" to "failed",
                "error_code" to sanitizeErrorCode(errorCode),
            )
        )
    }

    fun logExpensePaymentDeleteSucceeded() {
        logPaymentEvent(
            event = "expense_payment_delete_succeeded",
            surface = "expense_details",
            flow = "expense_payments",
            action = "delete_success",
        )
    }

    fun logExpensePaymentDeleteFailed(errorCode: String?) {
        logPaymentEvent(
            event = "expense_payment_delete_failed",
            surface = "expense_details",
            flow = "expense_payments",
            action = "delete_failed",
            extraParams = mapOf("error_code" to sanitizeErrorCode(errorCode))
        )
    }

    fun logOrderPaymentActionOpened(mode: String) {
        logPaymentEvent(
            event = "order_payment_action_opened",
            surface = "order_details",
            flow = "order_payments",
            action = "open_action",
            extraParams = mapOf("mode" to mode)
        )
    }

    fun logOrderPaymentSubmitAttempted(mode: String) {
        logPaymentEvent(
            event = "order_payment_submit_attempted",
            surface = "order_details",
            flow = "order_payments",
            action = "submit",
            extraParams = mapOf(
                "mode" to mode,
                "outcome" to "attempted"
            )
        )
    }

    fun logOrderPaymentSubmitSucceeded(mode: String) {
        logPaymentEvent(
            event = "order_payment_submit_succeeded",
            surface = "order_details",
            flow = "order_payments",
            action = "submit_success",
            extraParams = mapOf(
                "mode" to mode,
                "outcome" to "success"
            )
        )
    }

    fun logOrderPaymentSubmitFailed(mode: String, errorCode: String?) {
        logPaymentEvent(
            event = "order_payment_submit_failed",
            surface = "order_details",
            flow = "order_payments",
            action = "submit_failed",
            extraParams = mapOf(
                "mode" to mode,
                "outcome" to "failed",
                "error_code" to sanitizeErrorCode(errorCode),
            )
        )
    }

    fun logOrderPaymentLinkAction(actionValue: String) {
        logPaymentEvent(
            event = "order_payment_link_action",
            surface = "order_details",
            flow = "payment_link",
            action = actionValue
        )
    }

    fun logOrderCreationPaymentOptionSelected(mode: String) {
        logPaymentEvent(
            event = "order_creation_payment_option_selected",
            surface = "order_creation",
            flow = "order_payments",
            action = "option_selected",
            extraParams = mapOf("mode" to mode.uppercase())
        )
    }

    // Notifications
    private fun notificationParams(
        businessId: Int?,
        userId: Int?,
        userEmail: String?,
        notificationId: Long,
        notificationType: String?,
    ): Map<String, Any> {
        return buildBundle(
            mapOf(
                "business_id" to (businessId?.toString() ?: "-1"),
                "user_id" to (userId?.toString() ?: "-1"),
                "user_email" to (sanitizedString(userEmail) ?: "UNKNOWN"),
                "notification_id" to notificationId.toString(),
                "notification_type" to (sanitizedString(notificationType) ?: "unknown"),
            )
        )
    }

    fun logNotificationReceived(
        businessId: Int?,
        userId: Int?,
        userEmail: String?,
        notificationId: Long,
        notificationType: String?,
    ) {
        logEvent(
            "notification_received",
            notificationParams(businessId, userId, userEmail, notificationId, notificationType)
        )
    }

    fun logNotificationOpened(
        businessId: Int?,
        userId: Int?,
        userEmail: String?,
        notificationId: Long,
        notificationType: String?,
    ) {
        logEvent(
            "notification_opened",
            notificationParams(businessId, userId, userEmail, notificationId, notificationType)
        )
    }

    fun logNotificationRead(
        businessId: Int?,
        userId: Int?,
        userEmail: String?,
        notificationId: Long,
        notificationType: String?,
    ) {
        logEvent(
            "notification_read",
            notificationParams(businessId, userId, userEmail, notificationId, notificationType)
        )
    }

    fun logNotificationArchived(
        businessId: Int?,
        userId: Int?,
        userEmail: String?,
        notificationId: Long,
        notificationType: String?,
    ) {
        logEvent(
            "notification_archived",
            notificationParams(businessId, userId, userEmail, notificationId, notificationType)
        )
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
        val params = defaultParamsBundle.toMutableMap()
        params[FirebaseAnalyticsParam.SCREEN_NAME] = screenName
        params[FirebaseAnalyticsParam.SCREEN_CLASS] = "MainActivity"
        analytics.logEvent(FirebaseAnalyticsEvents.SCREEN_VIEW, params)
    }
}
