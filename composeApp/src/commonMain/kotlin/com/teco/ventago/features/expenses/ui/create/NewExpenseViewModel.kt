package com.teco.ventago.features.expenses.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.camera.SharedImage
import com.teco.ventago.core.file.SharedFile
import com.teco.ventago.core.beta.BetaFeature
import com.teco.ventago.core.beta.BetaService
import com.teco.ventago.features.expenses.domain.ExpensesSelectionStore
import com.teco.ventago.features.expenses.domain.ExpensesService
import com.teco.ventago.features.expenses.domain.models.Expense
import com.teco.ventago.features.expenses.domain.models.requests.ExpenseProofFile
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
import kotlin.math.abs

class NewExpenseViewModel(
    private val expensesService: ExpensesService,
    private val betaService: BetaService
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewExpenseState())
    val uiState: StateFlow<NewExpenseState> = _uiState.asStateFlow()
    private var localExpenseFile: ExpenseProofFile? = null
    private var localInitialPaymentProofFile: ExpenseProofFile? = null

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
        clearTransientFiles()
        val businessName = expensesService.getBusinessName() ?: ""
        val businessRuc = expensesService.getBusinessRuc() ?: ""
        _uiState.value = NewExpenseState(
            hasExpensesQr = _uiState.value.hasExpensesQr,
            receiverName = businessName,
            receiverRuc = businessRuc
        )
    }

    fun initForEdit(expense: Expense) {
        clearTransientFiles()
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
        clearTransientFiles()
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
        val imageData = image.toByteArray()
        if (imageData == null) {
            _uiState.value = _uiState.value.copy(hasSelectedFile = false)
            return
        }
        uploadFile(
            SharedFile(
                bytes = imageData,
                fileName = "expense_${randomUUID()}.jpg",
                contentType = "image/jpeg"
            )
        )
    }

    fun uploadFile(file: SharedFile) {
        val state = _uiState.value
        if (!state.isEditMode) {
            localExpenseFile = ExpenseProofFile(
                bytes = file.bytes,
                fileName = normalizeUploadName(file.fileName, file.contentType),
                contentType = file.contentType
            )
            _uiState.value = state.copy(
                localFileName = localExpenseFile?.fileName,
                fileUrl = null,
                hasSelectedFile = true,
                isUploadingFile = false
            )
            return
        }

        val businessId = expensesService.getBusinessId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUploadingFile = true,
                hasSelectedFile = true,
                localFileName = file.fileName
            )
            try {
                val safeFileName = normalizeUploadName(file.fileName, file.contentType)
                val url = withContext(Dispatchers.IO) {
                    uploadImageToBunnyCdn(file.bytes, safeFileName, businessId.toString())
                }
                _uiState.value = _uiState.value.copy(fileUrl = url, isUploadingFile = false, localFileName = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUploadingFile = false,
                    hasSelectedFile = false,
                    localFileName = null,
                    error = e.message
                )
            }
        }
    }

    private fun normalizeUploadName(fileName: String, contentType: String): String {
        val cleaned = fileName.substringAfterLast('/').substringAfterLast('\\').ifBlank {
            "expense_${randomUUID()}"
        }
        if ('.' in cleaned) return cleaned
        val extension = when (contentType.lowercase()) {
            "application/pdf" -> "pdf"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        return "$cleaned.$extension"
    }

    fun removeFile() {
        localExpenseFile = null
        _uiState.value = _uiState.value.copy(
            fileUrl = null,
            localFileName = null,
            hasSelectedFile = false
        )
    }

    // Payment field setters
    fun setIncludePayment(value: Boolean) {
        if (!value) {
            localInitialPaymentProofFile = null
            _uiState.value = _uiState.value.copy(
                includePayment = false,
                initialPaymentProofName = null,
                paymentAmount = "",
                paymentDate = "",
                paymentDueDate = ""
            )
            return
        }
        val totalAmount = getTotalAmount()
        _uiState.value = _uiState.value.copy(
            includePayment = true,
            paymentAmount = formatAmountForInput(totalAmount).takeIf { totalAmount > 0.0 } ?: ""
        )
    }
    fun setPaymentAmount(value: String) { _uiState.value = _uiState.value.copy(paymentAmount = value) }
    fun setPaymentMethodForPayment(value: String) {
        if (value == "credit") {
            localInitialPaymentProofFile = null
            _uiState.value = _uiState.value.copy(paymentMethodForPayment = value, initialPaymentProofName = null)
            return
        }
        _uiState.value = _uiState.value.copy(paymentMethodForPayment = value)
    }
    fun setPaymentDate(value: String) { _uiState.value = _uiState.value.copy(paymentDate = value) }
    fun setPaymentDueDate(value: String) { _uiState.value = _uiState.value.copy(paymentDueDate = value) }
    fun uploadInitialPaymentProof(file: SharedFile) {
        localInitialPaymentProofFile = ExpenseProofFile(
            bytes = file.bytes,
            fileName = normalizeUploadName(file.fileName, file.contentType),
            contentType = file.contentType
        )
        _uiState.value = _uiState.value.copy(initialPaymentProofName = localInitialPaymentProofFile?.fileName)
    }

    fun removeInitialPaymentProof() {
        localInitialPaymentProofFile = null
        _uiState.value = _uiState.value.copy(initialPaymentProofName = null)
    }

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
    fun getSubtotal(): Double = _uiState.value.items.sumOf { item ->
        val qty = parseDecimal(item.quantity) ?: 0.0
        val unitPrice = parseDecimal(item.unitPrice) ?: 0.0
        val discount = parseDecimal(item.discountAmount) ?: 0.0
        (qty * unitPrice) - discount
    }

    fun getItbmsTotal(): Double = _uiState.value.items.sumOf { item ->
        parseDecimal(item.itbmsAmount) ?: 0.0
    }

    fun getTotalAmount(): Double = getSubtotal() + getItbmsTotal()

    fun submit() {
        val state = _uiState.value
        val businessId = expensesService.getBusinessId() ?: return

        val validationError = validateBeforeSubmit(state)
        if (validationError != null) {
            _uiState.value = state.copy(error = validationError)
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSubmitting = true, error = null)
            try {
                val request = buildRequest(state, businessId)
                withContext(Dispatchers.IO) {
                    if (state.isEditMode && state.editingExpenseId != null) {
                        expensesService.updateExpense(state.editingExpenseId, request)
                    } else {
                        val initialProofs = if (state.includePayment && state.paymentMethodForPayment != "credit") {
                            localInitialPaymentProofFile?.let(::listOf).orEmpty()
                        } else {
                            emptyList()
                        }
                        expensesService.createExpense(
                            request = request,
                            file = localExpenseFile,
                            paymentProofFiles = initialProofs
                        )
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
            val quantity = parseDecimal(item.quantity) ?: 0.0
            val unitPrice = parseDecimal(item.unitPrice) ?: 0.0
            val discountAmount = parseDecimal(item.discountAmount) ?: 0.0
            val itbmsAmount = parseDecimal(item.itbmsAmount) ?: 0.0
            val subtotal = (quantity * unitPrice) - discountAmount
            val total = subtotal + itbmsAmount
            ExpenseItemRequest(
                lineNumber = index + 1,
                description = item.description,
                quantity = quantity,
                unitPrice = unitPrice,
                discountAmount = discountAmount,
                subtotal = subtotal,
                itbmsAmount = itbmsAmount,
                total = total
            )
        }

        val payment = if (!state.isEditMode && state.includePayment) {
            val payAmount = parseDecimal(state.paymentAmount)
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
            paymentMethod = when {
                state.isEditMode -> state.paymentMethod.takeIf { it.isNotBlank() }
                state.includePayment -> state.paymentMethodForPayment.takeIf { it.isNotBlank() }
                else -> null
            },
            issuer = ExpensePartyRequest(
                name = state.issuerName,
                ruc = state.issuerRuc.takeIf { it.isNotBlank() },
                dv = if (state.isEditMode) state.issuerDv.takeIf { it.isNotBlank() } else ""
            ),
            receiver = ExpensePartyRequest(
                name = state.receiverName,
                ruc = state.receiverRuc.takeIf { it.isNotBlank() },
                dv = if (state.isEditMode) state.receiverDv.takeIf { it.isNotBlank() } else "",
                type = "business"
            ),
            items = items,
            subtotal = getSubtotal(),
            itbmsTotal = getItbmsTotal(),
            totalAmount = getTotalAmount(),
            notes = state.notes.takeIf { it.isNotBlank() },
            fileUrl = if (state.isEditMode) state.fileUrl else null,
            removeFile = if (state.isEditMode && state.originalFileUrl != null && state.fileUrl == null) true else null,
            payment = payment
        )
    }

    private fun clearTransientFiles() {
        localExpenseFile = null
        localInitialPaymentProofFile = null
    }

    private fun validateBeforeSubmit(state: NewExpenseState): String? {
        if (state.issuerName.isBlank()) {
            return "El nombre del emisor es requerido"
        }

        if (state.items.isEmpty()) {
            return "Debe agregar al menos un artículo"
        }

        state.items.forEachIndexed { index, item ->
            val label = "artículo ${index + 1}"
            if (item.description.isBlank()) {
                return "La descripción del $label es requerida"
            }

            val quantity = parseDecimal(item.quantity)
            if (quantity == null || quantity <= 0.0) {
                return "La cantidad del $label debe ser un número mayor a 0"
            }

            val unitPrice = parseDecimal(item.unitPrice)
            if (unitPrice == null || unitPrice < 0.0) {
                return "El precio unitario del $label debe ser un número válido"
            }

            val discount = parseDecimal(item.discountAmount)
            if (discount == null || discount < 0.0) {
                return "El descuento del $label debe ser un número válido"
            }

            val itbms = parseDecimal(item.itbmsAmount)
            if (itbms == null || itbms < 0.0) {
                return "El ITBMS del $label debe ser un número válido"
            }
        }

        val totalAmount = getTotalAmount()
        if (totalAmount <= 0.0) {
            return "El total del gasto debe ser mayor a 0"
        }

        if (!state.isEditMode && state.includePayment) {
            val amount = parseDecimal(state.paymentAmount)
            if (amount == null || amount <= 0.0) {
                return "El monto del pago inicial debe ser un número válido"
            }
            if (amount - totalAmount > 0.0001) {
                return "El monto del pago inicial no puede exceder el total del gasto"
            }

            if (state.paymentMethodForPayment == "credit") {
                if (state.paymentDueDate.isBlank()) {
                    return "La fecha de vencimiento es requerida para pagos a crédito"
                }
            } else {
                if (state.paymentDate.isBlank()) {
                    return "La fecha de pago es requerida para el pago inicial"
                }
            }
        }

        return null
    }

    private fun parseDecimal(value: String): Double? {
        val normalized = value.trim().replace(",", ".")
        return normalized.toDoubleOrNull()
    }

    private fun formatAmountForInput(value: Double): String {
        val rounded = kotlin.math.round(value * 100.0) / 100.0
        return if (abs(rounded % 1.0) < 0.000001) {
            rounded.toInt().toString()
        } else {
            rounded.toString()
        }
    }
}
