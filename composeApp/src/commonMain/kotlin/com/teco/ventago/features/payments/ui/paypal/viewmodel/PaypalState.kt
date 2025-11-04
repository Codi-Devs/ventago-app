package com.teco.ventago.features.payments.ui.paypal.viewmodel

import com.teco.ventago.design_system.organism.LoadingBottomSheetState

data class PaypalUiState(
    val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
    val transactionFeePercent: Double = 2.0,
    val loadingSummaryData: Boolean = true,
    val linkedPaypalAccount: Boolean = false,
    val linkedBillingAgreement: Boolean = false,
    val linkedEmail: String = "",
)

sealed class PaypalUIEvents {
    data class OpenConnectUrl(val url: String) : PaypalUIEvents()
    data class OpenBillingAgreementUrl(val url: String) : PaypalUIEvents()
    data object UnlinkedPaypalAccount : PaypalUIEvents()
    data object ErrorLoadingSummary : PaypalUIEvents()
}