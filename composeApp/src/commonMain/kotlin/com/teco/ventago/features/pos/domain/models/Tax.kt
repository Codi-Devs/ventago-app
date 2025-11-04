package com.teco.ventago.features.pos.domain.models


data class Tax(
    val id: Int,
    val name: String,
    /** basis points: 7% = 700, 10% = 1000 */
    val rateBps: Int
)