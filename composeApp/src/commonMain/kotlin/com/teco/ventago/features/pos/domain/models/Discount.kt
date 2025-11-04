package com.teco.ventago.features.pos.domain.models

sealed class Discount {
    data class Amount(val value: Money) : Discount()          // e.g. -$1.50
    data class Percent(val bps: Int) : Discount()             // e.g. 15% = 1500
}