package com.teco.ventago.features.quotes.ui.details

import com.teco.ventago.features.quotes.domain.models.Quote

data class QuoteDetailsState(
    val quote: Quote? = null,
    val isLoading: Boolean = false,
    val isCancelling: Boolean = false,
    val isSendingEmail: Boolean = false,
    val isDownloadingPdf: Boolean = false,
    val error: String? = null
)
