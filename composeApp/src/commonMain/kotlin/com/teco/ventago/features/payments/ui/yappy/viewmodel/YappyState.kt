package com.teco.ventago.features.payments.ui.yappy.viewmodel

import com.teco.ventago.design_system.organism.LoadingBottomSheetState

data class YappyUiState(
    val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
    val transactionFeePercent: Double = 1.0,
    val loadingSummaryData: Boolean = true,
    val linkedYappyAccount: Boolean = false,
    val merchantID: String = "",
    val domain: String = "",
    val secretKey: String = "",
    val showIntroDialog: Boolean = false,
)

sealed class YappyUIEvents {
    data object UnlinkedYappyAccount: YappyUIEvents()
    data object LinkedYappyAccount: YappyUIEvents()
    data object ErrorLoadingSummary : YappyUIEvents()
}