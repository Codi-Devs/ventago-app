package com.teco.ventago.features.quotes.domain

import com.teco.ventago.features.quotes.domain.models.Quote

object QuoteSelectionStore {
    var selected: Quote? = null
    var startQuoteFlow: Boolean = false
    var startOrderFlowFromQuote: Boolean = false
}
