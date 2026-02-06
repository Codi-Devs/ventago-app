package com.teco.ventago.features.expenses.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.camera.SharedImage
import com.teco.ventago.features.expenses.domain.ExpensesSelectionStore
import com.teco.ventago.features.expenses.domain.ExpensesService
import com.teco.ventago.features.expenses.domain.models.Expense
import com.teco.ventago.utils.randomUUID
import com.teco.ventago.utils.uploadImageToBunnyCdn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class NewExpenseViewModel(
    private val expensesService: ExpensesService
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewExpenseState())
    val uiState: StateFlow<NewExpenseState> = _uiState.asStateFlow()

    fun initForCreate() {
        val businessName = expensesService.getBusinessName() ?: ""
        val businessRuc = expensesService.getBusinessRuc() ?: ""
        _uiState.value = NewExpenseState(
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
                val payload = buildPayload(state, businessId)
                withContext(Dispatchers.IO) {
                    if (state.isEditMode && state.editingExpenseId != null) {
                        expensesService.updateExpense(state.editingExpenseId, payload)
                    } else {
                        expensesService.createExpense(payload)
                    }
                }
                _uiState.value = _uiState.value.copy(isSubmitting = false, isSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSubmitting = false, error = e.message)
            }
        }
    }

    private fun buildPayload(state: NewExpenseState, businessId: Int): String {
        val emissionDateApi = if (state.emissionDate.isNotBlank()) {
            "${state.emissionDate}T00:00:00-05:00"
        } else null

        val itemsArray = buildJsonArray {
            state.items.forEachIndexed { index, item ->
                add(buildJsonObject {
                    put("line_number", index + 1)
                    put("description", item.description)
                    put("quantity", item.quantity.toDoubleOrNull() ?: 0.0)
                    put("unit_price", item.unitPrice.toDoubleOrNull() ?: 0.0)
                    put("discount_amount", item.discountAmount.toDoubleOrNull() ?: 0.0)
                    put("subtotal", item.subtotalValue)
                    put("itbms_amount", item.itbmsAmount.toDoubleOrNull() ?: 0.0)
                    put("total", item.totalValue)
                })
            }
        }

        val json = buildJsonObject {
            put("business_id", businessId)
            if (state.invoiceNumber.isNotBlank()) put("invoice_number", state.invoiceNumber)
            if (state.cufe.isNotBlank()) put("cufe", state.cufe)
            if (emissionDateApi != null) put("emission_date", emissionDateApi)
            if (state.paymentMethod.isNotBlank()) put("payment_method", state.paymentMethod)
            put("issuer", buildJsonObject {
                put("name", state.issuerName)
                if (state.issuerRuc.isNotBlank()) put("ruc", state.issuerRuc)
                if (state.issuerDv.isNotBlank()) put("dv", state.issuerDv)
            })
            put("receiver", buildJsonObject {
                put("name", state.receiverName)
                if (state.receiverRuc.isNotBlank()) put("ruc", state.receiverRuc)
                if (state.receiverDv.isNotBlank()) put("dv", state.receiverDv)
                put("type", "business")
            })
            put("items", itemsArray)
            put("subtotal", getSubtotal())
            put("itbms_total", getItbmsTotal())
            put("total_amount", getTotalAmount())
            if (state.notes.isNotBlank()) put("notes", state.notes)
            state.fileUrl?.let { put("file_url", it) }
            // In edit mode, if original file was removed and no new file uploaded, send remove_file
            if (state.isEditMode && state.originalFileUrl != null && state.fileUrl == null) {
                put("remove_file", true)
            }
            // Optional initial payment (create mode only)
            if (!state.isEditMode && state.includePayment) {
                val payAmount = state.paymentAmount.toDoubleOrNull()
                if (payAmount != null && payAmount > 0) {
                    put("payment", buildJsonObject {
                        put("payment_method", state.paymentMethodForPayment)
                        put("amount_paid", payAmount)
                        if (state.paymentMethodForPayment == "credit" && state.paymentDueDate.isNotBlank()) {
                            put("due_date", "${state.paymentDueDate}T23:59:59-05:00")
                        } else if (state.paymentDate.isNotBlank()) {
                            put("payment_date", "${state.paymentDate}T00:00:00-05:00")
                        }
                    })
                }
            }
        }

        return json.toString()
    }
}
