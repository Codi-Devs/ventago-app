package com.teco.ventago.features.orders.ui.order_details.viewModel

import com.teco.ventago.core.LoadableState
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.features.financialProfile.domain.model.PaymentMethods
import com.teco.ventago.features.orders.domain.models.ManualPaymentMethodOption
import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.utils.toLongCents

data class OrderDetailsState(
    val order: Order? = null,
    val showShareSheet: Boolean = false,

    val showMarkAsPaidAlertDialog: Boolean = false,

    val manualPaymentReference: String = "",
    val manualPaymentDescription: String = "",

    val showPaymentLinkSheet: Boolean = false,
    val havePaymentsConfigured: Boolean = false,
    val invoicingEnabled: Boolean = false,
    val canMarkPaid: Boolean = false,
    val loadingPaymentLink: Boolean = false,
    val paymentLink: String? = null,
    val errorLoadingPaymentLink: Boolean = false,

    val manualPayment: ManualPaymentState = ManualPaymentState(),

    override val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
): LoadableState<OrderDetailsState> {
    override fun withLoading(state: LoadingBottomSheetState): OrderDetailsState {
        return copy(loadingBottomSheet = state)
    }
}

sealed class OrderDetailsUiEvent {
    data class ShowPaymentLinkSheet(val url: String) : OrderDetailsUiEvent()
    data object OrderDeleted : OrderDetailsUiEvent()
}


data class ManualPaymentState(
    val showSheet: Boolean = false,

    // options shown as chips
    val methodOptions: List<ManualPaymentMethodOption> = ManualPaymentMethodOption.getAllOptions(),

    // selected methods with amounts in cents (e.g., 2->1500)
    val charged: Map<Int, Long> = emptyMap(),

    // required if OTHER is selected
    val otherPaymentDescription: String = "",

    // total to cover (cents) for the current order
    val totalToChargeCents: Long = 0L,

    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
) {
    val allocated: Long get() = charged.values.sum()
    val remaining: Long get() = (totalToChargeCents - allocated).coerceAtLeast(0L)
    val change: Long get() = (allocated - totalToChargeCents).coerceAtLeast(0L)

    val requiresOtherDesc: Boolean get() = charged.containsKey(11)
    val isConfirmEnabled: Boolean
        get() = allocated >= totalToChargeCents && (!requiresOtherDesc || otherPaymentDescription.isNotBlank())
}
