package com.teco.ventago.features.orders.domain.models

object OrderStatus {
    const val DRAFT = 1
    const val CONFIRMED = 2
    const val PROCESSING = 3
    const val READY = 4
    const val COMPLETED = 5
    const val CANCELLED = 6
    const val REJECT = 7
    const val REFUNDED = 8
}