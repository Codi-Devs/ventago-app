package com.teco.ventago.features.orders

import com.teco.ventago.features.orders.domain.OrderReceivableResolver
import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.orders.domain.models.OrderPaymentDto
import com.teco.ventago.features.orders.domain.models.ReceivableTermDto
import kotlin.test.Test
import kotlin.test.assertEquals

class OrderReceivableResolverTest {

    @Test
    fun usesTotalAmountWhenReceivableTermsAreMissing() {
        val order = baseOrder(
            totalAmount = "10.00"
        )

        val open = OrderReceivableResolver.totalOpenCents(order)

        assertEquals(1000L, open)
    }

    @Test
    fun subtractsNetPaidWhenReceivableTermsAreMissing() {
        val order = baseOrder(
            totalAmount = "10.00",
            orderPayments = listOf(
                OrderPaymentDto(charged = "6.00", refunded = "0.00")
            )
        )

        val open = OrderReceivableResolver.totalOpenCents(order)

        assertEquals(400L, open)
    }

    @Test
    fun ignoresVoidedPaymentsInFallbackComputation() {
        val order = baseOrder(
            totalAmount = "10.00",
            orderPayments = listOf(
                OrderPaymentDto(charged = "10.00", refunded = "0.00", voidedAt = "2026-04-23T22:40:00Z")
            )
        )

        val open = OrderReceivableResolver.totalOpenCents(order)

        assertEquals(1000L, open)
    }

    @Test
    fun prefersReceivableTermsWhenPresent() {
        val order = baseOrder(
            totalAmount = "10.00",
            receivableTerms = listOf(
                ReceivableTermDto(
                    id = 1L,
                    termNumber = 1,
                    dueDateUnixSeconds = 1_776_000_000,
                    originalAmount = "10.00",
                    openAmount = "3.00",
                    status = 1,
                    termKind = "declared"
                )
            ),
            orderPayments = listOf(
                OrderPaymentDto(charged = "10.00", refunded = "0.00")
            )
        )

        val open = OrderReceivableResolver.totalOpenCents(order)

        assertEquals(300L, open)
    }

    private fun baseOrder(
        totalAmount: String,
        receivableTerms: List<ReceivableTermDto> = emptyList(),
        orderPayments: List<OrderPaymentDto> = emptyList(),
    ): Order {
        return Order(
            id = 3796,
            orderType = "01",
            businessId = 4,
            internalNumber = "ORD-4-0000-865-0000000475",
            currencyCode = "USD",
            subtotal = totalAmount,
            discountTotal = "0.00",
            taxableBase = totalAmount,
            taxTotal = "0.00",
            tipsTotal = "0.00",
            totalAmount = totalAmount,
            status = 1,
            receivableTerms = receivableTerms,
            orderPayments = orderPayments,
            createdAt = "2026-04-23T22:36:45Z"
        )
    }
}
