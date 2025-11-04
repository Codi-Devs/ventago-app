package com.teco.ventago.features.payments.ui.home.viewmodel

import com.teco.ventago.core.LoadableState
import com.teco.ventago.design_system.organism.LoadingBottomSheetState


data class PaymentUiState(
    override val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
    val availablePaymentMethods: Map<String, PaymentMethodItem> = emptyMap(),
    val loadingSummaryData: Boolean = true,
    val errorLoadingSummaryData: Boolean = false,
    val showOnboarding: Boolean = true,
    val linkedBillingAgreement: Boolean = false,
    val pendingFees: Long = 0L,
    val pendingFeesCurrency: String = "",
    val pendingFeesCurrencySymbol: String = "",
    val nextBillingDate: String = "", // Date to show when going to charge the pending fees
    val showBottomBar: Boolean = false,
    val tab: Int = 1,
    val transferenceInstructions: String = "",
) : LoadableState<PaymentUiState> {

    override fun withLoading(state: LoadingBottomSheetState): PaymentUiState {
        return copy(loadingBottomSheet = state)
    }
}

data class PaymentMethodItem(val id: String, val visible:Boolean, val enabled: Boolean, val label: String?)


sealed class PaymentUiEvent {
    data class OpenBillingAgreementUrl(val url: String): PaymentUiEvent()
    data object OpenPaypalOnboarding: PaymentUiEvent()
    data object OpenPaypalScreen: PaymentUiEvent()
    data object ErrorLoadingSummary : PaymentUiEvent()
}