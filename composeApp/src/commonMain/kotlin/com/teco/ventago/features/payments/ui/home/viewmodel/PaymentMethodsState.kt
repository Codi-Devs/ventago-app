package com.teco.ventago.features.payments.ui.home.viewmodel

import com.teco.ventago.core.LoadableState
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.features.branches.domain.model.Branch
import com.teco.ventago.features.financialProfile.domain.model.PaymentSummary
import com.teco.ventago.features.payments.domain.models.AchStatus
import com.teco.ventago.features.payments.domain.models.FeeBatchItem
import com.teco.ventago.features.payments.domain.models.FeeSummary
import com.teco.ventago.features.payments.domain.models.FeeTransactionItem
import com.teco.ventago.features.payments.domain.models.TiloPayStatus
import com.teco.ventago.features.payments.domain.models.YappyOnsiteDevice
import com.teco.ventago.features.payments.domain.models.YappyOnsiteGroup


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
    YappyOnsite,
    Ach,
    Paypal,
    CardTilopay,
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
    val transactionsStatus: String = "",
    val transactionsMethod: String? = null,
    val selectedBatchId: Long? = null,
    val batchesStatus: String = "",
)

data class FeeTransactionsState(
    val items: List<FeeTransactionItem> = emptyList(),
    val page: Int = 1,
    val size: Int = 10,
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

data class TiloPayFormState(
    val apiUser: String = "",
    val password: String = "",
    val apiKey: String = "",
)

data class YappyOnsiteGroupFormState(
    val groupId: String = "",
    val name: String = "",
    val apiKey: String = "",
    val secretKey: String = "",
    val branchCode: String = "",
)

data class YappyOnsiteDeviceFormState(
    val groupId: String = "",
    val deviceId: String = "",
    val name: String = "",
    val userCode: String = "",
    val branchCode: String = "",
    val billingPoint: String = "",
)

data class YappyOnsiteGroupDraftState(
    val localId: Int,
    val groupId: String = "",
    val branchCode: String = "",
    val apiKey: String = "",
    val secretKey: String = "",
    val collapsed: Boolean = false,
)

data class YappyOnsiteDeviceDraftState(
    val localId: Int,
    val groupId: String = "",
    val branchCode: String = "",
    val billingPoint: String = "",
    val deviceId: String = "",
    val collapsed: Boolean = false,
)

data class PaymentUiState(
    override val loadingBottomSheet: LoadingBottomSheetState = LoadingBottomSheetState(),
    val loadingSummaryData: Boolean = true,
    val errorLoadingSummaryData: Boolean = false,
    val screenMode: PaymentScreenMode = PaymentScreenMode.Loading,
    val canViewPayments: Boolean = false,
    val canConfigurePayments: Boolean = false,
    val canPayFees: Boolean = false,
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
    val branches: List<Branch> = emptyList(),
    val yappyOnsiteGroups: List<YappyOnsiteGroup> = emptyList(),
    val yappyOnsiteDevices: List<YappyOnsiteDevice> = emptyList(),
    val yappyOnsiteLoading: Boolean = false,
    val yappyOnsiteGroupForm: YappyOnsiteGroupFormState = YappyOnsiteGroupFormState(),
    val yappyOnsiteDeviceForm: YappyOnsiteDeviceFormState = YappyOnsiteDeviceFormState(),
    val yappyOnsiteGroupDrafts: List<YappyOnsiteGroupDraftState> = emptyList(),
    val yappyOnsiteDeviceDrafts: List<YappyOnsiteDeviceDraftState> = emptyList(),
    val showYappyOnsiteGroupSheet: Boolean = false,
    val showYappyOnsiteDeviceSheet: Boolean = false,
    val editingYappyOnsiteGroupId: String? = null,
    val editingYappyOnsiteDeviceOriginalGroupId: String? = null,
    val editingYappyOnsiteDeviceId: String? = null,
    val confirmDeleteYappyOnsiteGroupId: String? = null,
    val confirmDeleteYappyOnsiteDevice: YappyOnsiteDevice? = null,
    val achForm: PaymentAchFormState = PaymentAchFormState(),
    val achAccountNumberMasked: String = "",
    val tiloPayStatus: TiloPayStatus? = null,
    val tiloPayForm: TiloPayFormState = TiloPayFormState(),
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
    data object NavigateToPaymentMethodsHome : PaymentUiEvent()
}

internal object YappyOnsiteOnboardingPolicy {
    fun exceedsGroupLimit(
        savedGroups: List<YappyOnsiteGroup>,
        drafts: List<YappyOnsiteGroupDraftState>,
        branches: List<Branch>,
    ): Boolean = savedGroups.size + drafts.size > branches.size

    fun hasDuplicateGroupBranches(
        savedGroups: List<YappyOnsiteGroup>,
        drafts: List<YappyOnsiteGroupDraftState>,
    ): Boolean {
        val draftBranches = drafts.map { it.branchCode.trim() }
        return draftBranches.distinct().size != draftBranches.size ||
            drafts.any { draft -> savedGroups.any { it.branchCode == draft.branchCode.trim() } }
    }

    fun hasDuplicateGroupIds(
        savedGroups: List<YappyOnsiteGroup>,
        drafts: List<YappyOnsiteGroupDraftState>,
    ): Boolean {
        val draftGroupIds = drafts.map { it.groupId.trim().lowercase() }
        return draftGroupIds.distinct().size != draftGroupIds.size ||
            drafts.any { draft -> savedGroups.any { it.groupId.equals(draft.groupId.trim(), ignoreCase = true) } }
    }

    fun groupNameForBranch(branchCode: String, branches: List<Branch>): String {
        return branches.firstOrNull { it.branchCode == branchCode.trim() }
            ?.name
            ?.trim()
            .orEmpty()
            .ifBlank { "Sucursal ${branchCode.trim()}" }
    }

    fun totalBillingPoints(branches: List<Branch>): Int {
        return branches.sumOf { it.fiscalBillingPoints.size }
    }

    fun exceedsDeviceLimit(
        savedDevices: List<YappyOnsiteDevice>,
        drafts: List<YappyOnsiteDeviceDraftState>,
        branches: List<Branch>,
    ): Boolean = savedDevices.size + drafts.size > totalBillingPoints(branches)

    fun hasDuplicateDeviceIds(
        savedDevices: List<YappyOnsiteDevice>,
        drafts: List<YappyOnsiteDeviceDraftState>,
    ): Boolean {
        val draftKeys = drafts.map { "${it.groupId.trim().lowercase()}|${it.deviceId.trim().lowercase()}" }
        return draftKeys.distinct().size != draftKeys.size ||
            drafts.any { draft ->
                savedDevices.any {
                    it.groupId.equals(draft.groupId.trim(), ignoreCase = true) &&
                        it.deviceId.equals(draft.deviceId.trim(), ignoreCase = true)
                }
            }
    }

    fun hasDuplicateDeviceBillingPoints(
        savedDevices: List<YappyOnsiteDevice>,
        drafts: List<YappyOnsiteDeviceDraftState>,
    ): Boolean {
        val draftKeys = drafts.map { "${it.groupId.trim().lowercase()}|${it.billingPoint.trim()}" }
        return draftKeys.distinct().size != draftKeys.size ||
            drafts.any { draft ->
                savedDevices.any {
                    it.groupId.equals(draft.groupId.trim(), ignoreCase = true) &&
                        it.billingPoint == draft.billingPoint.trim()
                }
            }
    }

    fun deviceNameForBillingPoint(
        branchCode: String,
        billingPoint: String,
        branches: List<Branch>,
    ): String {
        val point = branches
            .firstOrNull { it.branchCode == branchCode.trim() }
            ?.fiscalBillingPoints
            ?.firstOrNull { it.billingPoint == billingPoint.trim() }

        return point?.description?.trim().orEmpty().ifBlank { "Punto ${billingPoint.trim()}" }
    }
}
