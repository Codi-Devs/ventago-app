package com.teco.ventago.features.expenses.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.PdfSharer
import com.teco.ventago.features.expenses.domain.ExpensesSelectionStore
import com.teco.ventago.features.expenses.domain.ExpensesService
import com.teco.ventago.features.expenses.domain.models.ExpensePayment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ExpenseDetailsViewModel(
    private val expensesService: ExpensesService,
    private val pdfSharer: PdfSharer
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseDetailsState())
    val uiState: StateFlow<ExpenseDetailsState> = _uiState.asStateFlow()

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
            try {
                withContext(Dispatchers.IO) {
                    expensesService.deleteExpense(expenseId)
                }
                _uiState.value = _uiState.value.copy(isDeleting = false, isDeleted = true)
            } catch (e: Exception) {
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
        proofFileUrl: String? = null
    ) {
        val expenseId = _uiState.value.expense?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingPayment = true, paymentError = null)
            try {
                val payload = buildJsonObject {
                    put("payment_method", paymentMethod)
                    put("amount_paid", amountPaid)
                    if (reference.isNotBlank()) put("reference", reference)
                    if (notes.isNotBlank()) put("notes", notes)
                    if (paymentDate != null) put("payment_date", "${paymentDate}T00:00:00-05:00")
                    if (dueDate != null) put("due_date", "${dueDate}T23:59:59-05:00")
                    if (proofFileUrl != null) put("proof_file_url", proofFileUrl)
                }.toString()

                withContext(Dispatchers.IO) {
                    expensesService.createPayment(expenseId, payload)
                }
                _uiState.value = _uiState.value.copy(isSubmittingPayment = false, paymentSuccess = true)
                // Reload expense to get updated payment summary
                loadExpense(expenseId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSubmittingPayment = false, paymentError = e.message)
            }
        }
    }

    fun deletePayment(paymentId: Long) {
        val expenseId = _uiState.value.expense?.id ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    expensesService.deletePayment(expenseId, paymentId)
                }
                loadExpense(expenseId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun markPaymentAsPaid(payment: ExpensePayment) {
        val expenseId = _uiState.value.expense?.id ?: return
        val paymentId = payment.id ?: return
        viewModelScope.launch {
            try {
                val payload = buildJsonObject {
                    put("payment_status", "paid")
                    put("payment_method", payment.paymentMethod ?: "cash")
                    put("amount_paid", payment.amountPaid ?: 0.0)
                }.toString()
                withContext(Dispatchers.IO) {
                    expensesService.updatePayment(expenseId, paymentId, payload)
                }
                loadExpense(expenseId)
            } catch (e: Exception) {
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
        proofFileUrl: String? = null
    ) {
        val expenseId = _uiState.value.expense?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingPayment = true, paymentError = null)
            try {
                val payload = buildJsonObject {
                    put("payment_method", paymentMethod)
                    put("amount_paid", amountPaid)
                    if (reference.isNotBlank()) put("reference", reference)
                    if (notes.isNotBlank()) put("notes", notes)
                    if (paymentDate != null) put("payment_date", "${paymentDate}T00:00:00-05:00")
                    if (dueDate != null) put("due_date", "${dueDate}T23:59:59-05:00")
                    if (proofFileUrl != null) put("proof_file_url", proofFileUrl)
                }.toString()

                withContext(Dispatchers.IO) {
                    expensesService.updatePayment(expenseId, paymentId, payload)
                }
                _uiState.value = _uiState.value.copy(
                    isSubmittingPayment = false,
                    paymentSuccess = true,
                    editingPayment = null
                )
                loadExpense(expenseId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSubmittingPayment = false, paymentError = e.message)
            }
        }
    }

    fun resetPaymentState() {
        _uiState.value = _uiState.value.copy(paymentSuccess = false, paymentError = null, editingPayment = null)
    }

    /**
     * Check if a credit lock exists - if there is a pending credit payment
     * with amount >= total expense, block new payment registration.
     */
    fun hasCreditLock(): Boolean {
        val expense = _uiState.value.expense ?: return false
        val totalAmount = expense.totalAmount ?: return false
        return expense.payments?.any { payment ->
            payment.paymentMethod == "credit" &&
                payment.paymentStatus == "pending" &&
                (payment.amountPaid ?: 0.0) >= totalAmount
        } == true
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
}
