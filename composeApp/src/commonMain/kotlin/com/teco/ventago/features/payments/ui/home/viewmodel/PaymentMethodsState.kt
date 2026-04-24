package com.teco.ventago.features.payments.ui.home.viewmodel

import com.teco.ventago.core.LoadableState
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.features.financialProfile.domain.model.PaymentSummary
import com.teco.ventago.features.payments.domain.models.AchStatus
import com.teco.ventago.features.payments.domain.models.FeeBatchItem
import com.teco.ventago.features.payments.domain.models.FeeSummary
import com.teco.ventago.features.payments.domain.models.FeeTransactionItem


enum class PaymentScreenMode {
    Loading,
    BlockedNoPaymentsAccess,
    GlobalOnboarding,
    ConfiguredList,
    MethodDetailOnboarding,
    MethodDetailConfigured,
}

enum class PaymentViewMode {
    Transactions,
    BillingCycles,
}

enum class PaymentMethodType {
    Yappy,
    Ach,
    Paypal,
}

enum class FeeTransactionStatusUi(val api: String, val label: String) {
    Pending("unpaid", "Pendiente"),
    Paid("paid", "Pagado"),
    Overdue("overdue", "Vencido"),
    PendingDue("pending_due", "Por pagar"),
    CurrentPeriodAccrued("current_period_accrued", "Acumulado"),
    Issued("issued", "Emitido"),
    Unknown("", "Sin estado");

    companion object {
        fun fromApi(api: String?): FeeTransactionStatusUi {
            val normalized = api.orEmpty().trim().lowercase()
            return entries.firstOrNull { it.api == normalized } ?: Unknown
        }
    }
}

enum class FeeBatchStatusUi(val api: String, val label: String) {
    Issued("issued", "Emitido"),
    Paid("paid", "Pagado"),
    Unpaid("unpaid", "Pendiente"),
    Overdue("overdue", "Vencido"),
    PendingDue("pending_due", "Por pagar"),
    CurrentPeriodAccrued("current_period_accrued", "Acumulado"),
    Unknown("", "Sin estado");

    companion object {
        fun fromApi(api: String?): FeeBatchStatusUi {
            val normalized = api.orEmpty().trim().lowercase()
            return entries.firstOrNull { it.api == normalized } ?: Unknown
        }
    }
}

data class PaymentMethodItem(
    val id: String,
    val visible: Boolean,
    val enabled: Boolean,
    val label: String?,
)

data class FeeFilters(
    val transactionsStatus: String = "unpaid",
    val transactionsMethod: String? = null,
    val batchesStatus: String = "issued",
)

data class FeeTransactionsState(
    val items: List<FeeTransactionItem> = emptyList(),
    val page: Int = 1,
    val size: Int = 20,
    val total: Int = 0,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
)

data class FeeBatchesState(
    val items: List<FeeBatchItem> = emptyList(),
    val page: Int = 1,
    val size: Int = 20,
    val total: Int = 0,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
)

data class PaymentAchFormState(
    val bankCode: String = "",
    val bankName: String = "",
    val accountType: String = "checking",
    val accountNumber: String = "",
    val accountHolderName: String = "",
    val instructionsText: String = "Transferir y subir comprobante de pago",
)

data class PaymentUiState(
    override val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
    val loadingSummaryData: Boolean = true,
    val errorLoadingSummaryData: Boolean = false,
    val screenMode: PaymentScreenMode = PaymentScreenMode.Loading,
    val viewMode: PaymentViewMode = PaymentViewMode.Transactions,
    val activeMethod: PaymentMethodType? = null,
    val activeStep: Int = 1,
    val availablePaymentMethods: Map<String, PaymentMethodItem> = emptyMap(),
    val paymentSummary: PaymentSummary? = null,
    val achStatus: AchStatus? = null,
    val feeSummary: FeeSummary = FeeSummary(),
    val filters: FeeFilters = FeeFilters(),
    val feeTransactions: FeeTransactionsState = FeeTransactionsState(),
    val feeBatches: FeeBatchesState = FeeBatchesState(),
    val autoInvoiceEnabled: Boolean = false,
    val yappyMerchantId: String = "",
    val yappySecretKey: String = "",
    val achForm: PaymentAchFormState = PaymentAchFormState(),
    val achAccountNumberMasked: String = "",
    val showAddressRequiredDialog: Boolean = false,
    val confirmUnlinkMethod: PaymentMethodType? = null,
    val confirmDisableAch: Boolean = false,
) : LoadableState<PaymentUiState> {

    override fun withLoading(state: LoadingBottomSheetState): PaymentUiState {
        return copy(loadingBottomSheet = state)
    }
}

sealed class PaymentUiEvent {
    data class OpenExternalUrl(val url: String) : PaymentUiEvent()
    data class ShowWarning(val message: String) : PaymentUiEvent()
    data object NavigateToSettingsRoot : PaymentUiEvent()
}
