package com.teco.ventago.features.orders

import com.teco.ventago.features.orders.domain.PaymentLinkResolver
import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.orders.domain.models.OrderPaymentLinkDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PaymentLinkResolverTest {

    @Test
    fun resolvesFromPaymentLinksBeforeLegacyFields() {
        val order = baseOrder(
            paymentLink = "https://legacy-link",
            paymentLinks = listOf(
                OrderPaymentLinkDto(link = "https://preferred-link", status = "active")
            ),
            links = listOf(OrderPaymentLinkDto(url = "https://fallback-link", status = "pending"))
        )

        val resolved = PaymentLinkResolver.resolveCurrent(order)

        assertNotNull(resolved)
        assertEquals("https://preferred-link", resolved.url)
        assertEquals("payment_links", resolved.source)
    }

    @Test
    fun fallsBackToLegacyPaymentLinkThenLinks() {
        val order = baseOrder(
            paymentLink = "https://legacy-link",
            paymentLinks = emptyList(),
            links = listOf(OrderPaymentLinkDto(url = "https://fallback-link", status = "pending"))
        )

        val resolved = PaymentLinkResolver.resolveCurrent(order)

        assertNotNull(resolved)
        assertEquals("https://legacy-link", resolved.url)
        assertEquals("payment_link", resolved.source)
    }

    @Test
    fun detectsPendingLinkByStatus() {
        val orderWithPending = baseOrder(
            paymentLinks = listOf(OrderPaymentLinkDto(url = "https://pending", status = "pending_review"))
        )
        val orderWithTerminal = baseOrder(
            paymentLinks = listOf(OrderPaymentLinkDto(url = "https://done", status = "completed"))
        )
        val orderWithActive = baseOrder(
            paymentLinks = listOf(OrderPaymentLinkDto(url = "https://active", status = "active"))
        )

        assertTrue(PaymentLinkResolver.hasPendingLink(orderWithPending))
        assertFalse(PaymentLinkResolver.hasPendingLink(orderWithTerminal))
        assertFalse(PaymentLinkResolver.hasPendingLink(orderWithActive))
        assertTrue(PaymentLinkResolver.hasActiveLink(orderWithActive))
        assertFalse(PaymentLinkResolver.hasActiveLink(orderWithTerminal))
    }

    @Test
    fun resolvesMostRecentPaymentLinkFromPaymentLinksList() {
        val order = baseOrder(
            paymentLinks = listOf(
                OrderPaymentLinkDto(
                    url = "https://old-expired",
                    status = "expired",
                    createdAt = "2026-04-18T01:00:00Z"
                ),
                OrderPaymentLinkDto(
                    url = "https://new-active",
                    status = "active",
                    createdAt = "2026-04-20T01:00:00Z"
                )
            )
        )

        val resolved = PaymentLinkResolver.resolveCurrent(order)

        assertNotNull(resolved)
        assertEquals("https://new-active", resolved.url)
        assertTrue(PaymentLinkResolver.hasActiveLink(order))
        assertTrue(PaymentLinkResolver.hasOpenLink(order))
    }

    @Test
    fun detectsOpenLinkFromFallbackLinksWhenPaymentLinksAreTerminal() {
        val order = baseOrder(
            paymentLinks = listOf(
                OrderPaymentLinkDto(
                    url = "https://terminal",
                    status = "completed",
                    createdAt = "2026-04-20T01:00:00Z"
                )
            ),
            links = listOf(
                OrderPaymentLinkDto(
                    url = "https://active-fallback",
                    status = "active",
                    createdAt = "2026-04-19T01:00:00Z"
                )
            )
        )

        assertTrue(PaymentLinkResolver.hasActiveLink(order))
        assertTrue(PaymentLinkResolver.hasOpenLink(order))
    }

    @Test
    fun treatsRequiresActionAsOpenButNotActive() {
        val order = baseOrder(
            paymentLinks = listOf(
                OrderPaymentLinkDto(
                    link = "https://requires-action",
                    status = "requires_action",
                    createdAt = "2026-04-23T22:37:39Z"
                )
            )
        )

        assertTrue(PaymentLinkResolver.hasOpenLink(order))
        assertFalse(PaymentLinkResolver.hasActiveLink(order))
    }

    private fun baseOrder(
        paymentLink: String? = null,
        paymentLinks: List<OrderPaymentLinkDto> = emptyList(),
        links: List<OrderPaymentLinkDto> = emptyList(),
    ): Order {
        return Order(
            id = 1,
            orderType = "SALE",
            businessId = 10,
            internalNumber = "ORD-100",
            currencyCode = "USD",
            subtotal = "10.00",
            discountTotal = "0.00",
            taxableBase = "10.00",
            taxTotal = "0.00",
            tipsTotal = "0.00",
            totalAmount = "10.00",
            status = 1,
            createdAt = "2026-04-17T09:00:00Z",
            paymentLink = paymentLink,
            paymentLinks = paymentLinks,
            links = links,
        )
    }
}
