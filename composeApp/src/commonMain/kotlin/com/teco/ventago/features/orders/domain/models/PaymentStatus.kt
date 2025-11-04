package com.teco.ventago.features.orders.domain.models

enum class PaymentStatus(val id: Int) {
    UNPAID(0),
    PARTIAL(1),
    PAID(2),
    REFUNDED(3),
    CANCELLED(4)
}