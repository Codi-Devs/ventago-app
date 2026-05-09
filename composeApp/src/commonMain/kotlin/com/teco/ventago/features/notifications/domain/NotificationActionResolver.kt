package com.teco.ventago.features.notifications.domain

sealed interface NotificationActionResolution {
    data object None : NotificationActionResolution
    data class NavigateToAchPayment(val paymentUid: String) : NotificationActionResolution
    data class NavigateToOrderDetails(val orderNumber: String) : NotificationActionResolution
    data class OpenExternalUrl(val url: String) : NotificationActionResolution
    data class UnsupportedRelativeUrl(val url: String) : NotificationActionResolution
}

object NotificationActionResolver {
    private const val ACH_PATH = "/payments/ach/index.html"
    private const val ORDER_DETAILS_PATH = "/orders/order-details.html"
    private const val PAYMENT_UID = "payment_uid"
    private const val PAYMENT_INTENT_ID = "payment_intent_id"
    private const val PAYMENT_ID = "payment_id"
    private const val ID = "id"
    private const val ORDER_NUMBER = "orderNumber"

    fun resolve(actionUrl: String?): NotificationActionResolution {
        val normalizedUrl = actionUrl?.trim().orEmpty()
        if (normalizedUrl.isBlank()) return NotificationActionResolution.None

        val isAbsoluteUrl = normalizedUrl.startsWith("http://") || normalizedUrl.startsWith("https://")
        if (normalizedUrl.contains(ACH_PATH)) {
            val paymentUid = queryParam(normalizedUrl, PAYMENT_UID)
                ?: queryParam(normalizedUrl, PAYMENT_INTENT_ID)
                ?: queryParam(normalizedUrl, PAYMENT_ID)
                ?: queryParam(normalizedUrl, ID)
            if (!paymentUid.isNullOrBlank()) {
                return NotificationActionResolution.NavigateToAchPayment(paymentUid)
            }
        }
        if (normalizedUrl.contains(ORDER_DETAILS_PATH)) {
            val orderNumber = queryParam(normalizedUrl, ORDER_NUMBER)
            if (!orderNumber.isNullOrBlank()) {
                return NotificationActionResolution.NavigateToOrderDetails(orderNumber)
            }
        }

        if (isAbsoluteUrl) return NotificationActionResolution.OpenExternalUrl(normalizedUrl)
        return NotificationActionResolution.UnsupportedRelativeUrl(normalizedUrl)
    }

    private fun queryParam(url: String, key: String): String? {
        val query = url.substringAfter('?', "")
        if (query.isBlank()) return null
        return query
            .substringBefore('#')
            .split('&')
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { segment ->
                val name = segment.substringBefore('=')
                val value = segment.substringAfter('=', "")
                name to value
            }
            .firstOrNull { it.first == key }
            ?.second
            ?.takeIf { it.isNotBlank() }
    }
}
