package com.teco.ventago.features.expenses.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.PdfSharer
import com.teco.ventago.core.beta.BetaFeature
import com.teco.ventago.core.beta.BetaService
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.design_system.organism.LoadingState
import com.teco.ventago.features.expenses.domain.ExpensesSelectionStore
import com.teco.ventago.features.expenses.domain.ExpensesService
import com.teco.ventago.features.expenses.domain.models.Expense
import com.teco.ventago.features.expenses.domain.models.ExpensePayment
import com.teco.ventago.features.expenses.domain.models.PaymentSummary
import com.teco.ventago.features.expenses.domain.models.requests.ExpenseProofFile
import com.teco.ventago.features.expenses.domain.models.requests.UpsertExpensePaymentRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExpenseDetailsViewModel(
    private val expensesService: ExpensesService,
    private val pdfSharer: PdfSharer,
    private val betaService: BetaService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseDetailsState())
    val uiState: StateFlow<ExpenseDetailsState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            betaService.accessFlow(BetaFeature.EXPENSES_QR)
                .onEach { hasAccess ->
                    _uiState.value = _uiState.value.copy(hasExpensesQr = hasAccess)
                }
                .launchIn(this)
        }
    }

    fun loadExpense(expenseId: Long? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val expense = withContext(Dispatchers.IO) {
                    val selected = ExpensesSelectionStore.selected
                    val idToLoad = expenseId ?: selected?.id
                        ?: throw IllegalStateException("No expense selected")
                    expensesService.getExpense(idToLoad)
                }
                _uiState.value = _uiState.value.copy(expense = expense, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun deleteExpense() {
        val expenseId = _uiState.value.expense?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true, error = null)
            showLoading("Eliminando gasto...")
            try {
                withContext(Dispatchers.IO) {
                    expensesService.deleteExpense(expenseId)
                }
                showSuccess("Gasto eliminado correctamente")
                delay(1200)
                _uiState.value = _uiState.value.copy(isDeleting = false, isDeleted = true)
            } catch (e: Exception) {
                showError("No se pudo eliminar el gasto")
                _uiState.value = _uiState.value.copy(isDeleting = false, error = e.message)
            }
        }
    }

    // Payments

    fun createPayment(
        paymentMethod: String,
        amountPaid: Double,
        reference: String,
        notes: String,
        paymentDate: String?,
        dueDate: String?,
        proofFileUrl: String? = null,
        proofFile: ExpenseProofFile? = null
    ) {
        val expenseId = _uiState.value.expense?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingPayment = true, paymentError = null)
            try {
                val requestResult = buildPaymentRequest(
                    paymentMethod = paymentMethod,
                    amountPaid = amountPaid,
                    reference = reference,
                    notes = notes,
                    paymentDate = paymentDate,
                    dueDate = dueDate,
                    proofFileUrl = proofFileUrl
                )
                if (requestResult.request == null) {
                    _uiState.value = _uiState.value.copy(
                        isSubmittingPayment = false,
                        paymentError = requestResult.error ?: "Formato de fecha inválido."
                    )
                    return@launch
                }
                showLoading("Registrando pago...")
                val createdPayment = withContext(Dispatchers.IO) {
                    expensesService.createPayment(expenseId, requestResult.request, proofFile)
                }
                applyCreatedPaymentLocally(createdPayment)
                showSuccess("Pago registrado correctamente")
                _uiState.value = _uiState.value.copy(isSubmittingPayment = false, paymentSuccess = true)
            } catch (e: Exception) {
                showError("No se pudo registrar el pago")
                _uiState.value = _uiState.value.copy(isSubmittingPayment = false, paymentError = e.message)
            }
        }
    }

    fun deletePayment(paymentId: Long) {
        val expenseId = _uiState.value.expense?.id ?: return
        viewModelScope.launch {
            try {
                showLoading("Eliminando pago...")
                withContext(Dispatchers.IO) {
                    expensesService.deletePayment(expenseId, paymentId)
                }
                applyDeletedPaymentLocally(paymentId)
                showSuccess("Pago eliminado correctamente")
            } catch (e: Exception) {
                showError("No se pudo eliminar el pago")
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun markPaymentAsPaid(payment: ExpensePayment) {
        val expenseId = _uiState.value.expense?.id ?: return
        val paymentId = payment.id ?: return
        viewModelScope.launch {
            try {
                val request = UpsertExpensePaymentRequest(
                    paymentStatus = "paid",
                    paymentMethod = payment.paymentMethod ?: "cash",
                    amountPaid = payment.amountPaid ?: 0.0
                )
                showLoading("Actualizando pago...")
                val updatedPayment = withContext(Dispatchers.IO) {
                    expensesService.updatePayment(expenseId, paymentId, request)
                }
                val optimisticPayment = mergePaymentWithFallbacks(
                    paymentId = paymentId,
                    backend = updatedPayment,
                    request = request,
                    fallback = payment
                )
                applyUpsertedPaymentLocally(optimisticPayment)
                showSuccess("Pago actualizado correctamente")
            } catch (e: Exception) {
                showError("No se pudo actualizar el pago")
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun setEditingPayment(payment: ExpensePayment?) {
        _uiState.value = _uiState.value.copy(editingPayment = payment)
    }

    fun updatePayment(
        paymentId: Long,
        paymentMethod: String,
        amountPaid: Double,
        reference: String,
        notes: String,
        paymentDate: String?,
        dueDate: String?,
        paymentStatus: String? = null,
        proofFileUrl: String? = null,
        proofFile: ExpenseProofFile? = null
    ) {
        val expenseId = _uiState.value.expense?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingPayment = true, paymentError = null)
            try {
                val requestResult = buildPaymentRequest(
                    paymentMethod = paymentMethod,
                    amountPaid = amountPaid,
                    reference = reference,
                    notes = notes,
                    paymentDate = paymentDate,
                    dueDate = dueDate,
                    paymentStatus = paymentStatus,
                    proofFileUrl = proofFileUrl
                )
                if (requestResult.request == null) {
                    _uiState.value = _uiState.value.copy(
                        isSubmittingPayment = false,
                        paymentError = requestResult.error ?: "Formato de fecha inválido."
                    )
                    return@launch
                }
                showLoading("Actualizando pago...")
                val updatedPayment = withContext(Dispatchers.IO) {
                    expensesService.updatePayment(expenseId, paymentId, requestResult.request, proofFile)
                }
                val fallback = _uiState.value.expense?.payments
                    ?.firstOrNull { it.id == paymentId }
                val optimisticPayment = mergePaymentWithFallbacks(
                    paymentId = paymentId,
                    backend = updatedPayment,
                    request = requestResult.request,
                    fallback = fallback
                )
                applyUpsertedPaymentLocally(optimisticPayment)
                showSuccess("Pago actualizado correctamente")
                _uiState.value = _uiState.value.copy(
                    isSubmittingPayment = false,
                    paymentSuccess = true,
                    editingPayment = null
                )
            } catch (e: Exception) {
                showError("No se pudo actualizar el pago")
                _uiState.value = _uiState.value.copy(isSubmittingPayment = false, paymentError = e.message)
            }
        }
    }

    fun resetPaymentState() {
        _uiState.value = _uiState.value.copy(paymentSuccess = false, paymentError = null, editingPayment = null)
    }

    fun hideLoading() {
        _uiState.value = _uiState.value.copy(
            loadingBottomSheet = LoadingBottomSheetState(LoadingState.HIDDEN)
        )
    }

    /**
     * Check if a credit lock exists: any unpaid credit payment should be
     * completed via "Marcar como pagado" instead of creating a new payment.
     */
    fun hasCreditLock(): Boolean {
        return getCreditLockPayment() != null
    }

    fun getCreditLockPayment(): ExpensePayment? {
        val expense = _uiState.value.expense ?: return null
        return expense.payments?.firstOrNull { payment ->
            payment.paymentMethod == "credit" &&
                payment.paymentStatus != "paid"
        }
    }

    fun generateAndOpenPdf() {
        val expense = _uiState.value.expense ?: return
        viewModelScope.launch {
            try {
                val pdfBytes = withContext(Dispatchers.Default) {
                    ExpensePdfGenerator.generate(expense)
                }
                val filename = "gasto_${expense.invoiceNumber ?: expense.id}.pdf"
                pdfSharer.openPdf(filename, pdfBytes)
            } catch (_: Exception) {}
        }
    }

    private fun normalizeDateForApi(rawDate: String?): String? {
        if (rawDate == null) return null
        val datePart = rawDate.trim().substringBefore("T")
        val parts = datePart.split("-")
        if (parts.size != 3) return null

        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].toIntOrNull() ?: return null
        if (month !in 1..12 || day !in 1..31) return null

        return "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
    }

    private fun buildPaymentRequest(
        paymentMethod: String,
        amountPaid: Double,
        reference: String,
        notes: String,
        paymentDate: String?,
        dueDate: String?,
        paymentStatus: String? = null,
        proofFileUrl: String?
    ): PaymentRequestResult {
        val normalizedPaymentDate = normalizeDateForApi(paymentDate)
        if (paymentDate != null && normalizedPaymentDate == null) {
            return PaymentRequestResult(error = "Formato de fecha de pago inválido.")
        }
        val normalizedDueDate = normalizeDateForApi(dueDate)
        if (dueDate != null && normalizedDueDate == null) {
            return PaymentRequestResult(error = "Formato de fecha de vencimiento inválido.")
        }

        return PaymentRequestResult(
            request = UpsertExpensePaymentRequest(
                paymentMethod = paymentMethod,
                amountPaid = amountPaid,
                reference = reference.ifBlank { null },
                notes = notes.ifBlank { null },
                paymentDate = normalizedPaymentDate?.let { "${it}T00:00:00-05:00" },
                dueDate = normalizedDueDate?.let { "${it}T23:59:59-05:00" },
                proofFileUrl = proofFileUrl,
                paymentStatus = paymentStatus
            )
        )
    }

    private fun showLoading(title: String) {
        _uiState.value = _uiState.value.copy(
            loadingBottomSheet = LoadingBottomSheetState(LoadingState.LOADING, title)
        )
    }

    private fun showSuccess(title: String) {
        _uiState.value = _uiState.value.copy(
            loadingBottomSheet = LoadingBottomSheetState(LoadingState.SUCCESS, title)
        )
    }

    private fun showError(title: String) {
        _uiState.value = _uiState.value.copy(
            loadingBottomSheet = LoadingBottomSheetState(LoadingState.ERROR, title)
        )
    }

    private data class PaymentRequestResult(
        val request: UpsertExpensePaymentRequest? = null,
        val error: String? = null
    )

    private fun applyCreatedPaymentLocally(createdPayment: ExpensePayment) {
        applyUpsertedPaymentLocally(createdPayment)
    }

    private fun applyUpsertedPaymentLocally(updatedPayment: ExpensePayment) {
        val currentExpense = _uiState.value.expense ?: return
        val payments = currentExpense.payments.orEmpty().toMutableList()
        val updateIndex = updatedPayment.id?.let { id ->
            payments.indexOfFirst { it.id == id }
        } ?: -1
        if (updateIndex >= 0) {
            payments[updateIndex] = updatedPayment
        } else {
            payments.add(updatedPayment)
        }
        updateExpensePaymentSummary(currentExpense, payments)
    }

    private fun applyDeletedPaymentLocally(paymentId: Long) {
        val currentExpense = _uiState.value.expense ?: return
        val updatedPayments = currentExpense.payments.orEmpty().filterNot { it.id == paymentId }
        updateExpensePaymentSummary(currentExpense, updatedPayments)
    }

    private fun updateExpensePaymentSummary(expense: Expense, payments: List<ExpensePayment>) {
        val paidTotal = payments
            .filter { it.paymentStatus == "paid" }
            .sumOf { it.amountPaid ?: 0.0 }
        val totalAmount = expense.totalAmount ?: 0.0
        val remaining = (totalAmount - paidTotal).coerceAtLeast(0.0)
        val status = when {
            paidTotal <= 0.0 -> "not_paid"
            remaining <= 0.0001 -> "paid"
            else -> "partial"
        }

        _uiState.value = _uiState.value.copy(
            expense = expense.copy(
                payments = payments,
                totalPaid = paidTotal,
                paymentStatus = status,
                paymentSummary = PaymentSummary(
                    totalPaid = paidTotal,
                    remaining = remaining,
                    status = status
                )
            )
        )
    }

    private fun mergePaymentWithFallbacks(
        paymentId: Long,
        backend: ExpensePayment?,
        request: UpsertExpensePaymentRequest,
        fallback: ExpensePayment?
    ): ExpensePayment {
        val currentExpenseId = _uiState.value.expense?.id
        return ExpensePayment(
            id = paymentId,
            expenseId = backend?.expenseId ?: fallback?.expenseId ?: currentExpenseId,
            paymentMethod = backend?.paymentMethod ?: request.paymentMethod.ifBlank { fallback?.paymentMethod ?: "cash" },
            paymentStatus = backend?.paymentStatus ?: request.paymentStatus ?: fallback?.paymentStatus,
            amountPaid = backend?.amountPaid ?: request.amountPaid,
            reference = backend?.reference ?: request.reference ?: fallback?.reference,
            proofFileUrl = backend?.proofFileUrl ?: request.proofFileUrl ?: fallback?.proofFileUrl,
            proofFileName = backend?.proofFileName ?: fallback?.proofFileName,
            notes = backend?.notes ?: request.notes ?: fallback?.notes,
            paymentDate = backend?.paymentDate ?: request.paymentDate ?: fallback?.paymentDate,
            dueDate = backend?.dueDate ?: request.dueDate ?: fallback?.dueDate,
            isOverdue = backend?.isOverdue ?: fallback?.isOverdue,
            daysOverdue = backend?.daysOverdue ?: fallback?.daysOverdue,
            createdAt = backend?.createdAt ?: fallback?.createdAt,
            updatedAt = backend?.updatedAt ?: fallback?.updatedAt
        )
    }
}
