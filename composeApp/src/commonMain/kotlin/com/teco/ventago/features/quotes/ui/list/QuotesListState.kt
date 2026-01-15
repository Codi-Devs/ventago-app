package com.teco.ventago.features.quotes.ui.list

import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.features.quotes.domain.models.PagedQuotes
import com.teco.ventago.features.quotes.domain.models.Quote

data class QuotesListState(
    val quotes: List<Quote> = emptyList(),
    val page: Int = 0,
    val isLoading: Boolean = false,
    val refreshing: Boolean = false,
    val noMore: Boolean = false,
    val error: String? = null,
    val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
    val customerName: String = "",
    val customerRuc: String = "",
    val quoteNumber: String = "",
    val status: Int? = null
)
