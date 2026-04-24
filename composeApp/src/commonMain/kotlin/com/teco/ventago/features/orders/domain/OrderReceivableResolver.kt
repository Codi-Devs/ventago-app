package com.teco.ventago.features.orders.domain

import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.utils.toLongCents

object OrderReceivableResolver {

    fun totalOpenCents(order: Order): Long {
        val nonCancelledTerms = order.receivableTerms.filter { it.status != 4 }
        val openFromTerms = nonCancelledTerms.sumOf { it.openAmount.toLongCents() }.coerceAtLeast(0L)

        if (order.receivableTerms.isNotEmpty()) {
            return openFromTerms
        }

        val totalOrderCents = order.totalAmount.toLongCents().coerceAtLeast(0L)
        val paidNetCents = order.orderPayments
            .asSequence()
            .filter { it.voidedAt.isNullOrBlank() }
            .sumOf { payment ->
                (payment.charged.toLongCents() - payment.refunded.toLongCents()).coerceAtLeast(0L)
            }

        return (totalOrderCents - paidNetCents).coerceAtLeast(0L)
    }
}
