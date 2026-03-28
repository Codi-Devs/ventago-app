package com.teco.ventago.features.orders.ui.order_details.viewModel

import com.teco.ventago.core.LoadableState
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.features.orders.domain.models.ManualPaymentMethodOption
import com.teco.ventago.features.orders.domain.models.Order

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
    val registerPaymentState: RegisterPaymentState = RegisterPaymentState(),
    val rescheduleState: RescheduleState = RescheduleState(),
    val voidPaymentState: VoidPaymentState = VoidPaymentState(),

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

enum class RegisterPaymentMode {
    AUTOMATIC,
    MANUAL
}

data class RegisterPaymentApplicationState(
    val id: Int,
    val receivableTermId: Long? = null,
    val amountInput: String = ""
)

data class RegisterPaymentRowState(
    val id: Int,
    val paymentMethodCode: Int = ManualPaymentMethodOption.CASH.id,
    val amountInput: String = "",
    val paymentDateIso: String = "",
    val applications: List<RegisterPaymentApplicationState> = emptyList()
)

data class RegisterPaymentState(
    val showSheet: Boolean = false,
    val mode: RegisterPaymentMode = RegisterPaymentMode.AUTOMATIC,
    val rows: List<RegisterPaymentRowState> = emptyList(),
    val nextRowId: Int = 1,
    val nextApplicationId: Int = 1,
    val errorMessage: String? = null
)

data class RescheduleTermState(
    val id: Int,
    val dueDateIso: String = "",
    val amountInput: String = ""
)

data class RescheduleState(
    val showSheet: Boolean = false,
    val showConfirmDialog: Boolean = false,
    val terms: List<RescheduleTermState> = emptyList(),
    val nextTermId: Int = 1,
    val errorMessage: String? = null
)

data class VoidPaymentState(
    val showSheet: Boolean = false,
    val paymentId: Long? = null,
    val reason: String = "",
    val errorMessage: String? = null
)

internal object OrderCxcValidators {
    fun parseCents(input: String): Long = input.filter(Char::isDigit).toLongOrNull() ?: 0L

    fun validateRegisterPayment(
        mode: RegisterPaymentMode,
        rows: List<RegisterPaymentRowState>,
        openBalanceCents: Long,
        openByTermCents: Map<Long, Long>,
        todayIso: String
    ): String? {
        if (rows.isEmpty()) return "Agrega al menos un pago."

        var totalPayments = 0L
        val termAccumulated = mutableMapOf<Long, Long>()

        rows.forEachIndexed { index, row ->
            val rowAmount = parseCents(row.amountInput)
            if (rowAmount <= 0L) return "El monto del pago ${index + 1} debe ser mayor a 0."
            if (row.paymentDateIso.isBlank()) return "La fecha del pago ${index + 1} es obligatoria."
            if (row.paymentDateIso > todayIso) return "La fecha del pago ${index + 1} no puede ser futura."
            totalPayments += rowAmount

            if (mode == RegisterPaymentMode.MANUAL) {
                if (row.applications.isEmpty()) return "Agrega al menos una cuota para el pago ${index + 1}."

                var rowApplicationsTotal = 0L
                row.applications.forEach { app ->
                    val termId = app.receivableTermId
                        ?: return "Selecciona una cuota para todas las aplicaciones del pago ${index + 1}."
                    val appAmount = parseCents(app.amountInput)
                    if (appAmount <= 0L) {
                        return "Cada aplicación del pago ${index + 1} debe ser mayor a 0."
                    }
                    rowApplicationsTotal += appAmount
                    termAccumulated[termId] = (termAccumulated[termId] ?: 0L) + appAmount
                }

                if (rowApplicationsTotal != rowAmount) {
                    return "La suma de cuotas del pago ${index + 1} debe coincidir con el monto del pago."
                }
            }
        }

        if (totalPayments > openBalanceCents) {
            return "La suma de pagos no puede exceder el saldo a cobrar."
        }

        if (mode == RegisterPaymentMode.MANUAL) {
            termAccumulated.forEach { (termId, assigned) ->
                val openAmount = openByTermCents[termId] ?: 0L
                if (assigned > openAmount) {
                    return "La cuota #$termId excede su saldo adeudado."
                }
            }
        }

        return null
    }

    fun validateReschedule(
        terms: List<RescheduleTermState>,
        totalOpenCents: Long
    ): String? {
        if (terms.isEmpty()) return "Agrega al menos una nueva cuota."
        var total = 0L
        terms.forEachIndexed { index, term ->
            if (term.dueDateIso.isBlank()) return "La fecha de la cuota ${index + 1} es obligatoria."
            val amount = parseCents(term.amountInput)
            if (amount <= 0L) return "El monto de la cuota ${index + 1} debe ser mayor a 0."
            total += amount
        }
        if (total != totalOpenCents) {
            return "La suma de los nuevos vencimientos debe coincidir exactamente con el saldo abierto total."
        }
        return null
    }

    fun validateVoidReason(reason: String): String? {
        if (reason.isBlank()) return "La razón es obligatoria."
        return null
    }
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
