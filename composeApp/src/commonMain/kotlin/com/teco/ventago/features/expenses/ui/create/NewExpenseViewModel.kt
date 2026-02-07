package com.teco.ventago.features.expenses.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.camera.SharedImage
import com.teco.ventago.core.beta.BetaFeature
import com.teco.ventago.core.beta.BetaService
import com.teco.ventago.features.expenses.domain.ExpensesSelectionStore
import com.teco.ventago.features.expenses.domain.ExpensesService
import com.teco.ventago.features.expenses.domain.models.Expense
import com.teco.ventago.features.expenses.domain.models.requests.ExpenseItemRequest
import com.teco.ventago.features.expenses.domain.models.requests.ExpensePartyRequest
import com.teco.ventago.features.expenses.domain.models.requests.InitialExpensePaymentRequest
import com.teco.ventago.features.expenses.domain.models.requests.UpsertExpenseRequest
import com.teco.ventago.utils.randomUUID
import com.teco.ventago.utils.uploadImageToBunnyCdn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NewExpenseViewModel(
    private val expensesService: ExpensesService,
    private val betaService: BetaService
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewExpenseState())
    val uiState: StateFlow<NewExpenseState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            betaService.accessFlow(BetaFeature.EXPENSES_QR)
                .onEach { hasAccess ->
                    _uiState.value = _uiState.value.copy(hasExpensesQr = hasAccess)
                }
                .launchIn(this)
        }
    }

    fun initForCreate() {
        val businessName = expensesService.getBusinessName() ?: ""
        val businessRuc = expensesService.getBusinessRuc() ?: ""
        _uiState.value = NewExpenseState(
            hasExpensesQr = _uiState.value.hasExpensesQr,
            receiverName = businessName,
            receiverRuc = businessRuc
        )
    }

    fun initForEdit(expense: Expense) {
        val items = expense.items?.mapIndexed { _, item ->
            EditableExpenseItem(
                description = item.description ?: "",
                quantity = "${item.quantity ?: 1.0}",
                unitPrice = "${item.unitPrice ?: 0.0}",
                discountAmount = "${item.discountAmount ?: 0.0}",
                itbmsAmount = "${item.itbmsAmount ?: 0.0}"
            )
        } ?: listOf(EditableExpenseItem())

        _uiState.value = NewExpenseState(
            hasExpensesQr = _uiState.value.hasExpensesQr,
            isEditMode = true,
            editingExpenseId = expense.id,
            invoiceNumber = expense.invoiceNumber ?: "",
            cufe = expense.cufe ?: "",
            emissionDate = expense.emissionDate?.take(10) ?: "",
            paymentMethod = expense.paymentMethod ?: "",
            notes = expense.notes ?: "",
            issuerName = expense.issuer?.name ?: "",
            issuerRuc = expense.issuer?.ruc ?: "",
            issuerDv = expense.issuer?.dv ?: "",
            receiverName = expense.receiver?.name ?: "",
            receiverRuc = expense.receiver?.ruc ?: "",
            receiverDv = expense.receiver?.dv ?: "",
            items = items,
            fileUrl = expense.fileUrl,
            originalFileUrl = expense.fileUrl
        )
    }

    fun initForDuplicate(expense: Expense) {
        val items = expense.items?.mapIndexed { _, item ->
            EditableExpenseItem(
                description = item.description ?: "",
                quantity = "${item.quantity ?: 1.0}",
                unitPrice = "${item.unitPrice ?: 0.0}",
                discountAmount = "${item.discountAmount ?: 0.0}",
                itbmsAmount = "${item.itbmsAmount ?: 0.0}"
            )
        } ?: listOf(EditableExpenseItem())

        _uiState.value = NewExpenseState(
            hasExpensesQr = _uiState.value.hasExpensesQr,
            isEditMode = false,
            // Duplicate excludes invoice number and CUFE
            invoiceNumber = "",
            cufe = "",
            emissionDate = expense.emissionDate?.take(10) ?: "",
            paymentMethod = expense.paymentMethod ?: "",
            notes = expense.notes ?: "",
            issuerName = expense.issuer?.name ?: "",
            issuerRuc = expense.issuer?.ruc ?: "",
            issuerDv = expense.issuer?.dv ?: "",
            receiverName = expense.receiver?.name ?: "",
            receiverRuc = expense.receiver?.ruc ?: "",
            receiverDv = expense.receiver?.dv ?: "",
            items = items
        )
    }

    // Field setters
    fun setInvoiceNumber(value: String) { _uiState.value = _uiState.value.copy(invoiceNumber = value) }
    fun setCufe(value: String) { _uiState.value = _uiState.value.copy(cufe = value) }
    fun setEmissionDate(value: String) { _uiState.value = _uiState.value.copy(emissionDate = value) }
    fun setPaymentMethod(value: String) { _uiState.value = _uiState.value.copy(paymentMethod = value) }
    fun setNotes(value: String) { _uiState.value = _uiState.value.copy(notes = value) }
    fun setIssuerName(value: String) { _uiState.value = _uiState.value.copy(issuerName = value) }
    fun setIssuerRuc(value: String) { _uiState.value = _uiState.value.copy(issuerRuc = value) }
    fun setIssuerDv(value: String) { _uiState.value = _uiState.value.copy(issuerDv = value) }
    fun setReceiverName(value: String) { _uiState.value = _uiState.value.copy(receiverName = value) }
    fun setReceiverRuc(value: String) { _uiState.value = _uiState.value.copy(receiverRuc = value) }
    fun setReceiverDv(value: String) { _uiState.value = _uiState.value.copy(receiverDv = value) }

    // File upload
    fun uploadFile(image: SharedImage) {
        val businessId = expensesService.getBusinessId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingFile = true, hasSelectedFile = true)
            try {
                val imageData = withContext(Dispatchers.Default) { image.toByteArray() }
                if (imageData != null) {
                    val fileName = "expense_${randomUUID()}.jpg"
                    val url = withContext(Dispatchers.IO) {
                        uploadImageToBunnyCdn(imageData, fileName, businessId.toString())
                    }
                    _uiState.value = _uiState.value.copy(fileUrl = url, isUploadingFile = false)
                } else {
                    _uiState.value = _uiState.value.copy(isUploadingFile = false, hasSelectedFile = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isUploadingFile = false, hasSelectedFile = false, error = e.message)
            }
        }
    }

    fun removeFile() {
        _uiState.value = _uiState.value.copy(fileUrl = null, hasSelectedFile = false)
    }

    // Payment field setters
    fun setIncludePayment(value: Boolean) { _uiState.value = _uiState.value.copy(includePayment = value) }
    fun setPaymentAmount(value: String) { _uiState.value = _uiState.value.copy(paymentAmount = value) }
    fun setPaymentMethodForPayment(value: String) { _uiState.value = _uiState.value.copy(paymentMethodForPayment = value) }
    fun setPaymentDate(value: String) { _uiState.value = _uiState.value.copy(paymentDate = value) }
    fun setPaymentDueDate(value: String) { _uiState.value = _uiState.value.copy(paymentDueDate = value) }

    // Item management
    fun updateItem(index: Int, item: EditableExpenseItem) {
        val items = _uiState.value.items.toMutableList()
        if (index in items.indices) {
            items[index] = item
            _uiState.value = _uiState.value.copy(items = items)
        }
    }

    fun addItem() {
        val items = _uiState.value.items + EditableExpenseItem()
        _uiState.value = _uiState.value.copy(items = items)
    }

    fun removeItem(index: Int) {
        val items = _uiState.value.items.toMutableList()
        if (items.size > 1 && index in items.indices) {
            items.removeAt(index)
            _uiState.value = _uiState.value.copy(items = items)
        }
    }

    // Computed totals
    fun getSubtotal(): Double = _uiState.value.items.sumOf { it.subtotalValue }
    fun getItbmsTotal(): Double = _uiState.value.items.sumOf { it.itbmsAmount.toDoubleOrNull() ?: 0.0 }
    fun getTotalAmount(): Double = _uiState.value.items.sumOf { it.totalValue }

    fun submit() {
        val state = _uiState.value
        val businessId = expensesService.getBusinessId() ?: return

        viewModelScope.launch {
            _uiState.value = state.copy(isSubmitting = true, error = null)
            try {
                val request = buildRequest(state, businessId)
                withContext(Dispatchers.IO) {
                    if (state.isEditMode && state.editingExpenseId != null) {
                        expensesService.updateExpense(state.editingExpenseId, request)
                    } else {
                        expensesService.createExpense(request)
                    }
                }
                _uiState.value = _uiState.value.copy(isSubmitting = false, isSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSubmitting = false, error = e.message)
            }
        }
    }

    private fun buildRequest(state: NewExpenseState, businessId: Int): UpsertExpenseRequest {
        val emissionDateApi = if (state.emissionDate.isNotBlank()) {
            "${state.emissionDate}T00:00:00-05:00"
        } else null

        val items = state.items.mapIndexed { index, item ->
            ExpenseItemRequest(
                lineNumber = index + 1,
                description = item.description,
                quantity = item.quantity.toDoubleOrNull() ?: 0.0,
                unitPrice = item.unitPrice.toDoubleOrNull() ?: 0.0,
                discountAmount = item.discountAmount.toDoubleOrNull() ?: 0.0,
                subtotal = item.subtotalValue,
                itbmsAmount = item.itbmsAmount.toDoubleOrNull() ?: 0.0,
                total = item.totalValue
            )
        }

        val payment = if (!state.isEditMode && state.includePayment) {
            val payAmount = state.paymentAmount.toDoubleOrNull()
            if (payAmount != null && payAmount > 0) {
                InitialExpensePaymentRequest(
                    paymentMethod = state.paymentMethodForPayment,
                    amountPaid = payAmount,
                    paymentDate = if (state.paymentMethodForPayment == "credit") {
                        null
                    } else {
                        state.paymentDate.takeIf { it.isNotBlank() }?.let { "${it}T00:00:00-05:00" }
                    },
                    dueDate = if (state.paymentMethodForPayment == "credit") {
                        state.paymentDueDate.takeIf { it.isNotBlank() }?.let { "${it}T23:59:59-05:00" }
                    } else {
                        null
                    }
                )
            } else {
                null
            }
        } else {
            null
        }

        return UpsertExpenseRequest(
            businessId = businessId,
            invoiceNumber = state.invoiceNumber.takeIf { it.isNotBlank() },
            cufe = state.cufe.takeIf { it.isNotBlank() },
            emissionDate = emissionDateApi,
            paymentMethod = state.paymentMethod.takeIf { it.isNotBlank() },
            issuer = ExpensePartyRequest(
                name = state.issuerName,
                ruc = state.issuerRuc.takeIf { it.isNotBlank() },
                dv = state.issuerDv.takeIf { it.isNotBlank() }
            ),
            receiver = ExpensePartyRequest(
                name = state.receiverName,
                ruc = state.receiverRuc.takeIf { it.isNotBlank() },
                dv = state.receiverDv.takeIf { it.isNotBlank() },
                type = "business"
            ),
            items = items,
            subtotal = getSubtotal(),
            itbmsTotal = getItbmsTotal(),
            totalAmount = getTotalAmount(),
            notes = state.notes.takeIf { it.isNotBlank() },
            fileUrl = state.fileUrl,
            removeFile = if (state.isEditMode && state.originalFileUrl != null && state.fileUrl == null) true else null,
            payment = payment
        )
    }
}
