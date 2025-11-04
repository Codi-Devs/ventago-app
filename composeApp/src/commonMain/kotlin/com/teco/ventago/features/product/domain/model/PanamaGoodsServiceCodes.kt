package com.teco.ventago.features.product.domain.model

// Represents a Segment (category) in the goods/services classification
data class GoodsSegment(
    val code: String,
    val description: String,
    val families: List<GoodsFamily> = emptyList()
)

data class GoodsFamily(
    val code: String,
    val description: String,
)