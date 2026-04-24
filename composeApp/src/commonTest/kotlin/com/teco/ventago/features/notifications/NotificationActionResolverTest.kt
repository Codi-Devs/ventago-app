package com.teco.ventago.features.notifications

import com.teco.ventago.features.notifications.domain.NotificationActionResolution
import com.teco.ventago.features.notifications.domain.NotificationActionResolver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NotificationActionResolverTest {

    @Test
    fun resolvesAchNavigationFromAbsoluteUrl() {
        val result = NotificationActionResolver.resolve(
            "https://invoice-vg.tecodigi.com/payments/ach/index.html?payment_uid=pi_abc123"
        )

        assertIs<NotificationActionResolution.NavigateToAchPayment>(result)
        assertEquals("pi_abc123", result.paymentUid)
    }

    @Test
    fun resolvesAchNavigationFromRelativeUrl() {
        val result = NotificationActionResolver.resolve(
            "/payments/ach/index.html?payment_uid=pi_foo"
        )

        assertIs<NotificationActionResolution.NavigateToAchPayment>(result)
        assertEquals("pi_foo", result.paymentUid)
    }

    @Test
    fun resolvesAchNavigationFromPaymentIntentIdParam() {
        val result = NotificationActionResolver.resolve(
            "https://invoice-vg.tecodigi.com/payments/ach/index.html?payment_intent_id=pi_intent_123"
        )

        assertIs<NotificationActionResolution.NavigateToAchPayment>(result)
        assertEquals("pi_intent_123", result.paymentUid)
    }

    @Test
    fun routesUnknownAbsoluteUrlToExternalBrowser() {
        val result = NotificationActionResolver.resolve(
            "https://tecodigi.com/some/path?x=1"
        )

        assertIs<NotificationActionResolution.OpenExternalUrl>(result)
        assertEquals("https://tecodigi.com/some/path?x=1", result.url)
    }

    @Test
    fun routesUnknownRelativeUrlToUnsupported() {
        val result = NotificationActionResolver.resolve("/unknown/path")

        assertIs<NotificationActionResolution.UnsupportedRelativeUrl>(result)
        assertEquals("/unknown/path", result.url)
    }
}
