package com.teco.ventago.features.quotes.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PagedQuotes(
    val items: List<Quote> = emptyList(),
    val total: Long? = null,
    val size: Int? = null,
    val page: Int? = null
)
