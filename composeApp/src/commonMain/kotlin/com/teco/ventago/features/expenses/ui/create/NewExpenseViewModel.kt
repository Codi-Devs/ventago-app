package com.teco.ventago.features.expenses.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teco.ventago.core.beta.BetaFeature
import com.teco.ventago.core.beta.BetaService
import com.teco.ventago.core.camera.SharedImage
import com.teco.ventago.core.file.SharedFile
import com.teco.ventago.design_system.organism.LoadingBottomSheetState
import com.teco.ventago.design_system.organism.LoadingState
import com.teco.ventago.features.expenses.domain.ExpenseConceptMode
import com.teco.ventago.features.expenses.domain.ExpensesSelectionStore
import com.teco.ventago.features.expenses.domain.ExpensesService
import com.teco.ventago.features.expenses.domain.buildExpenseCategorizationPayload
import com.teco.ventago.features.expenses.domain.buildExpenseCreatePayload
import com.teco.ventago.features.expenses.domain.inferDefaultExpenseAccount
import com.teco.ventago.features.expenses.domain.models.Expense
import com.teco.ventago.features.expenses.domain.models.requests.ExpenseItemRequest
import com.teco.ventago.features.expenses.domain.models.requests.ExpensePartyRequest
import com.teco.ventago.features.expenses.domain.models.requests.ExpenseProofFile
import com.teco.ventago.features.expenses.domain.models.requests.InitialExpensePaymentRequest
import com.teco.ventago.features.expenses.domain.models.requests.UpsertExpenseRequest
import com.teco.ventago.features.expenses.domain.resolveExpenseConceptMode
import com.teco.ventago.features.expenses.domain.toConceptSelections
import com.teco.ventago.features.expenses.domain.models.ExpenseMerchant
import com.teco.ventago.utils.randomUUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val localInitialPaymentProofFiles = mutableMapOf<Int, ExpenseProofFile>()
    private var merchantSearchJob: Job? = null

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
            isSuccess = false,
            receiverName = businessName,
            receiverRuc = businessRuc
        )
        loadExpenseAccounts()
    }

    fun initForEdit(expense: Expense) {
        clearTransientFiles()
        _uiState.value = _uiState.value.copy(isSuccess = false)
        loadExpenseAccounts()
        val expenseId = expense.id ?: run {
            _uiState.value = _uiState.value.copy(error = "No se pudo cargar el gasto para editar.")
            return
        }
        viewModelScope.launch {
            try {
                val detail = withContext(kotlinx.coroutines.Dispatchers.Default) {
                    expensesService.getExpense(expenseId)
                }
                if (!detail.isManual) {
                    _uiState.value = NewExpenseState(
                        hasExpensesQr = _uiState.value.hasExpensesQr,
                        error = "Solo se pueden editar gastos manuales."
                    )
                    return@launch
                }
                populateFromExpense(detail, isEditMode = true, isDuplicateMode = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "No se pudo cargar el gasto.")
            }
        }
    }

    fun initForDuplicate(expense: Expense) {
        clearTransientFiles()
        _uiState.value = _uiState.value.copy(isSuccess = false)
        loadExpenseAccounts()
        val expenseId = expense.id
        if (expenseId == null) {
            populateFromExpense(expense, isEditMode = false, isDuplicateMode = true)
            return
        }

        viewModelScope.launch {
            try {
                val detail = withContext(kotlinx.coroutines.Dispatchers.Default) {
                    expensesService.getExpense(expenseId)
                }
                populateFromExpense(detail, isEditMode = false, isDuplicateMode = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "No se pudo duplicar el gasto.")
            }
        }
    }

    private fun populateFromExpense(expense: Expense, isEditMode: Boolean, isDuplicateMode: Boolean) {
        val items = expense.items
            ?.mapIndexed { index, item ->
                EditableExpenseItem(
                    itemId = if (isDuplicateMode) null else item.id,
                    lineNumber = index + 1,
                    description = item.description ?: "",
                    quantity = formatAmountForInput(item.quantity ?: 1.0),
                    unitPrice = formatAmountForInput(item.unitPrice ?: 0.0),
                    discountAmount = formatAmountForInput(item.discountAmount ?: 0.0),
                    itbmsAmount = formatAmountForInput(item.itbmsAmount ?: 0.0),
                    expenseAccountId = item.expenseAccountId,
                    expenseAccountName = item.expenseAccount?.name
                )
            }
            ?.ifEmpty { listOf(defaultEditableItem()) }
            ?: listOf(defaultEditableItem())

        val inferredDefaultAccountId = inferDefaultExpenseAccount(
            defaultAccountId = expense.defaultAccountId,
            items = expense.toConceptSelections()
        )
        val conceptMode = resolveExpenseConceptMode(
            defaultAccountId = inferredDefaultAccountId,
            items = expense.toConceptSelections()
        )
        val hasExistingConcepts = expense.defaultAccountId != null ||
            expense.items.orEmpty().any { it.expenseAccountId != null }

        _uiState.value = NewExpenseState(
            hasExpensesQr = _uiState.value.hasExpensesQr,
            isEditMode = isEditMode,
            editingExpenseId = if (isEditMode) expense.id else null,
            invoiceNumber = if (isDuplicateMode) "" else (expense.invoiceNumber ?: ""),
            cufe = if (isDuplicateMode) "" else (expense.cufe ?: ""),
            emissionDate = expense.emissionDate?.take(10) ?: "",
            paymentMethod = expense.paymentMethod ?: "",
            notes = expense.notes ?: "",
            issuerName = expense.issuer?.name ?: "",
            issuerRuc = expense.issuer?.ruc ?: "",
            issuerDv = expense.issuer?.dv ?: "",
            issuerAddress = expense.issuer?.address ?: "",
            issuerPhone = expense.issuer?.phone ?: "",
            selectedMerchantId = if (isEditMode) expense.merchant?.id else null,
            selectedMerchantName = if (isEditMode) expense.merchant?.name else null,
            originalMerchantId = if (isEditMode) expense.merchant?.id else null,
            saveMerchant = expense.merchant != null,
            receiverName = expense.receiver?.name ?: "",
            receiverRuc = expense.receiver?.ruc ?: "",
            receiverDv = expense.receiver?.dv ?: "",
            items = items,
            defaultExpenseAccountId = inferredDefaultAccountId,
            defaultExpenseAccountName = expense.defaultAccount?.name
                ?: items.mapNotNull { it.expenseAccountName }.distinct().singleOrNull(),
            applyConceptPerItem = conceptMode == ExpenseConceptMode.PER_ITEM,
            hadExistingConcepts = hasExistingConcepts,
            fileUrl = if (isDuplicateMode) null else expense.fileUrl,
            originalFileUrl = if (isDuplicateMode) null else expense.fileUrl,
            expenseAccounts = _uiState.value.expenseAccounts
        )
    }

    private fun defaultEditableItem(): EditableExpenseItem {
        val state = _uiState.value
        return EditableExpenseItem(
            lineNumber = 1,
            expenseAccountId = state.defaultExpenseAccountId,
            expenseAccountName = state.defaultExpenseAccountName
        )
    }

    private fun loadExpenseAccounts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(expenseAccountsLoading = true)
            try {
                val accounts = withContext(kotlinx.coroutines.Dispatchers.Default) {
                    expensesService.getExpenseAccounts(includeInactive = false)
                }
                _uiState.value = _uiState.value.copy(
                    expenseAccounts = accounts,
                    expenseAccountsLoading = false
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    expenseAccountsLoading = false,
                    error = "No se pudieron cargar los conceptos de gasto."
                )
            }
        }
    }

    fun setInvoiceNumber(value: String) { updateState { copy(invoiceNumber = value) } }
    fun setCufe(value: String) { updateState { copy(cufe = value) } }
    fun setEmissionDate(value: String) { updateState { copy(emissionDate = value) } }
    fun setPaymentMethod(value: String) { updateState { copy(paymentMethod = value) } }
    fun setNotes(value: String) { updateState { copy(notes = value) } }
    fun setIssuerName(value: String) {
        val wasSelected = _uiState.value.selectedMerchantId != null
        updateState {
            copy(
                issuerName = value,
                selectedMerchantId = if (wasSelected) null else selectedMerchantId,
                selectedMerchantName = if (wasSelected) null else selectedMerchantName
            )
        }
        searchMerchants(value)
    }
    fun setIssuerRuc(value: String) { updateState { copy(issuerRuc = value) } }
    fun setIssuerDv(value: String) { updateState { copy(issuerDv = value) } }
    fun setIssuerAddress(value: String) { updateState { copy(issuerAddress = value) } }
    fun setIssuerPhone(value: String) { updateState { copy(issuerPhone = value) } }
    fun setReceiverName(value: String) { updateState { copy(receiverName = value) } }
    fun setReceiverRuc(value: String) { updateState { copy(receiverRuc = value) } }
    fun setReceiverDv(value: String) { updateState { copy(receiverDv = value) } }

    fun setSaveMerchant(value: Boolean) {
        updateState { copy(saveMerchant = value) }
    }

    fun selectMerchant(merchant: ExpenseMerchant) {
        updateState {
            copy(
                issuerName = merchant.name,
                issuerRuc = merchant.ruc ?: "",
                issuerDv = merchant.dv ?: "",
                issuerAddress = merchant.address ?: "",
                issuerPhone = merchant.phone ?: "",
                selectedMerchantId = merchant.id,
                selectedMerchantName = merchant.name,
                merchantSuggestions = emptyList()
            )
        }
    }

    fun dismissMerchantSuggestions() {
        updateState { copy(merchantSuggestions = emptyList()) }
    }

    private fun searchMerchants(query: String) {
        merchantSearchJob?.cancel()
        if (query.length < 2) {
            updateState { copy(merchantSuggestions = emptyList(), isMerchantSearching = false) }
            return
        }
        merchantSearchJob = viewModelScope.launch {
            delay(300)
            updateState { copy(isMerchantSearching = true) }
            try {
                val results = withContext(kotlinx.coroutines.Dispatchers.Default) {
                    expensesService.searchMerchants(query)
                }
                updateState { copy(merchantSuggestions = results, isMerchantSearching = false) }
            } catch (_: Exception) {
                updateState { copy(merchantSuggestions = emptyList(), isMerchantSearching = false) }
            }
        }
    }

    fun setDefaultExpenseAccount(accountId: Long?, accountName: String?) {
        updateState {
            copy(
                defaultExpenseAccountId = accountId,
                defaultExpenseAccountName = accountName,
                conceptValidationError = null
            )
        }
    }

    fun setApplyConceptPerItem(enabled: Boolean) {
        updateState { copy(applyConceptPerItem = enabled, conceptValidationError = null) }
    }

    fun applyDefaultConceptToAllItems() {
        val state = _uiState.value
        val accountId = state.defaultExpenseAccountId
        if (accountId == null) {
            updateState { copy(conceptValidationError = "Selecciona un concepto para aplicar a todos los items.") }
            return
        }

        val items = state.items.mapIndexed { index, item ->
            item.copy(
                lineNumber = index + 1,
                expenseAccountId = accountId,
                expenseAccountName = state.defaultExpenseAccountName
            )
        }
        updateState { copy(items = items, conceptValidationError = null) }
    }

    fun setItemExpenseAccount(index: Int, accountId: Long?, accountName: String?) {
        val items = _uiState.value.items.toMutableList()
        if (index !in items.indices) return
        items[index] = items[index].copy(
            expenseAccountId = accountId,
            expenseAccountName = accountName
        )
        updateState { copy(items = items, conceptValidationError = null) }
    }

    fun uploadFile(image: SharedImage) {
        val imageData = image.toByteArray()
        if (imageData == null) {
            updateState { copy(hasSelectedFile = false) }
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
        localExpenseFile = ExpenseProofFile(
            bytes = file.bytes,
            fileName = normalizeUploadName(file.fileName, file.contentType),
            contentType = file.contentType
        )
        updateState {
            copy(
                localFileName = localExpenseFile?.fileName,
                fileUrl = null,
                hasSelectedFile = true,
                isUploadingFile = false
            )
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
        updateState {
            copy(
                fileUrl = null,
                localFileName = null,
                hasSelectedFile = false
            )
        }
    }

    fun setIncludePayment(value: Boolean) {
        if (!value) {
            localInitialPaymentProofFiles.clear()
            updateState {
                copy(
                    includePayment = false,
                    initialPayments = listOf(EditableInitialPayment()),
                    initialPaymentProofName = null,
                    paymentAmount = "",
                    paymentDate = "",
                    paymentDueDate = ""
                )
            }
            return
        }
        val totalAmount = getTotalAmount()
        val defaultAmount = formatAmountForInput(totalAmount).takeIf { totalAmount > 0.0 } ?: ""
        updateState {
            copy(
                includePayment = true,
                initialPayments = listOf(EditableInitialPayment(amount = defaultAmount)),
                paymentAmount = defaultAmount
            )
        }
    }

    fun updateInitialPayment(index: Int, payment: EditableInitialPayment) {
        val payments = _uiState.value.initialPayments.toMutableList()
        if (index in payments.indices) {
            payments[index] = payment
            updateState { copy(initialPayments = payments) }
        }
    }

    fun addInitialPayment() {
        val payments = _uiState.value.initialPayments + EditableInitialPayment()
        updateState { copy(initialPayments = payments) }
    }

    fun removeInitialPayment(index: Int) {
        val payments = _uiState.value.initialPayments.toMutableList()
        if (payments.size > 1 && index in payments.indices) {
            payments.removeAt(index)
            localInitialPaymentProofFiles.remove(index)
            updateState { copy(initialPayments = payments) }
        }
    }

    fun uploadInitialPaymentProofAt(index: Int, file: SharedFile) {
        val proofFile = ExpenseProofFile(
            bytes = file.bytes,
            fileName = normalizeUploadName(file.fileName, file.contentType),
            contentType = file.contentType
        )
        localInitialPaymentProofFiles[index] = proofFile
        val payments = _uiState.value.initialPayments.toMutableList()
        if (index in payments.indices) {
            payments[index] = payments[index].copy(proofFileName = proofFile.fileName)
            updateState { copy(initialPayments = payments) }
        }
    }

    fun removeInitialPaymentProofAt(index: Int) {
        localInitialPaymentProofFiles.remove(index)
        val payments = _uiState.value.initialPayments.toMutableList()
        if (index in payments.indices) {
            payments[index] = payments[index].copy(proofFileName = null)
            updateState { copy(initialPayments = payments) }
        }
    }

    // Legacy single-payment setters (kept for backward compatibility)
    fun setPaymentAmount(value: String) { updateState { copy(paymentAmount = value) } }
    fun setPaymentMethodForPayment(value: String) {
        if (value == "credit") {
            localInitialPaymentProofFiles.clear()
            updateState { copy(paymentMethodForPayment = value, initialPaymentProofName = null) }
            return
        }
        updateState { copy(paymentMethodForPayment = value) }
    }
    fun setPaymentDate(value: String) { updateState { copy(paymentDate = value) } }
    fun setPaymentDueDate(value: String) { updateState { copy(paymentDueDate = value) } }

    fun uploadInitialPaymentProof(file: SharedFile) {
        val proofFile = ExpenseProofFile(
            bytes = file.bytes,
            fileName = normalizeUploadName(file.fileName, file.contentType),
            contentType = file.contentType
        )
        localInitialPaymentProofFiles[0] = proofFile
        updateState { copy(initialPaymentProofName = proofFile.fileName) }
    }

    fun removeInitialPaymentProof() {
        localInitialPaymentProofFiles.remove(0)
        updateState { copy(initialPaymentProofName = null) }
    }

    fun updateItem(index: Int, item: EditableExpenseItem) {
        val items = _uiState.value.items.toMutableList()
        if (index in items.indices) {
            items[index] = item.copy(lineNumber = index + 1)
            updateState { copy(items = items) }
        }
    }

    fun addItem() {
        val state = _uiState.value
        val newItem = EditableExpenseItem(
            lineNumber = state.items.size + 1,
            expenseAccountId = state.defaultExpenseAccountId,
            expenseAccountName = state.defaultExpenseAccountName
        )
        updateState { copy(items = state.items + newItem) }
    }

    fun removeItem(index: Int) {
        val items = _uiState.value.items.toMutableList()
        if (items.size > 1 && index in items.indices) {
            items.removeAt(index)
            val reIndexed = items.mapIndexed { itemIndex, item -> item.copy(lineNumber = itemIndex + 1) }
            updateState { copy(items = reIndexed) }
        }
    }

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
            updateState { copy(error = validationError) }
            return
        }

        viewModelScope.launch {
            updateState {
                copy(
                    isSubmitting = true,
                    error = null,
                    conceptValidationError = null,
                    loadingBottomSheet = LoadingBottomSheetState(LoadingState.LOADING, "Guardando gasto...")
                )
            }

            try {
                val resultExpense = withContext(kotlinx.coroutines.Dispatchers.Default) {
                    if (state.isEditMode && state.editingExpenseId != null) {
                        saveEditedExpense(state, businessId)
                    } else {
                        saveNewExpense(state, businessId)
                    }
                }

                ExpensesSelectionStore.selected = resultExpense
                expensesService.publishExpenseUpdate(resultExpense)
                updateState {
                    copy(
                        isSubmitting = false,
                        isSuccess = true,
                        loadingBottomSheet = LoadingBottomSheetState(
                            LoadingState.SUCCESS,
                            "Gasto guardado correctamente."
                        )
                    )
                }
            } catch (e: Exception) {
                updateState {
                    copy(
                        isSubmitting = false,
                        error = e.message ?: "No se pudo guardar el gasto.",
                        loadingBottomSheet = LoadingBottomSheetState(
                            LoadingState.ERROR,
                            "No se pudo guardar el gasto."
                        )
                    )
                }
            }
        }
    }

    fun hideLoading() {
        updateState { copy(loadingBottomSheet = LoadingBottomSheetState()) }
    }

    private suspend fun saveNewExpense(state: NewExpenseState, businessId: Int): Expense {
        val request = buildCreateRequest(state, businessId)
        val paymentProofs = if (state.includePayment) {
            state.initialPayments.mapIndexedNotNull { index, payment ->
                if (payment.paymentMethod != "credit") {
                    localInitialPaymentProofFiles[index]
                } else null
            }
        } else {
            emptyList()
        }
        return expensesService.createExpense(
            request = request,
            file = localExpenseFile,
            paymentProofFiles = paymentProofs
        )
    }

    private suspend fun saveEditedExpense(state: NewExpenseState, businessId: Int): Expense {
        val expenseId = state.editingExpenseId ?: throw IllegalStateException("No expense selected")
        val baseRequest = buildUpdateRequest(state, businessId)
        val updatedExpense = expensesService.updateExpense(expenseId, baseRequest, localExpenseFile)
        val hasSelectedConcepts = hasAnySelectedConcept(state)

        if (!hasSelectedConcepts) {
            return updatedExpense
        }

        val mode = if (state.applyConceptPerItem) ExpenseConceptMode.PER_ITEM else ExpenseConceptMode.GLOBAL
        val payload = buildExpenseCategorizationPayload(
            defaultAccountId = state.defaultExpenseAccountId,
            mode = mode,
            items = state.items.map {
                com.teco.ventago.features.expenses.domain.ExpenseItemConceptSelection(
                    itemId = it.itemId,
                    lineNumber = it.lineNumber,
                    accountId = if (mode == ExpenseConceptMode.GLOBAL) state.defaultExpenseAccountId else it.expenseAccountId
                )
            }
        )
        return expensesService.categorizeExpense(expenseId, payload)
    }

    private fun buildCreateRequest(state: NewExpenseState, businessId: Int): UpsertExpenseRequest {
        val baseRequest = buildBaseRequest(state, businessId, includeConcepts = true)
        return buildExpenseCreatePayload(
            baseRequest = baseRequest,
            defaultAccountId = state.defaultExpenseAccountId,
            applyConceptPerItem = state.applyConceptPerItem
        )
    }

    private fun buildUpdateRequest(state: NewExpenseState, businessId: Int): UpsertExpenseRequest {
        return buildBaseRequest(state, businessId, includeConcepts = false)
    }

    private fun buildBaseRequest(
        state: NewExpenseState,
        businessId: Int,
        includeConcepts: Boolean
    ): UpsertExpenseRequest {
        val emissionDateApi = state.emissionDate.takeIf { it.isNotBlank() }?.let { "${it}T00:00:00-05:00" }
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
                total = total,
                expenseAccountId = if (includeConcepts && state.applyConceptPerItem) item.expenseAccountId else null
            )
        }

        val payments = if (!state.isEditMode && state.includePayment) {
            state.initialPayments.mapNotNull { p ->
                val payAmount = parseDecimal(p.amount)
                if (payAmount != null && payAmount > 0) {
                    InitialExpensePaymentRequest(
                        paymentMethod = p.paymentMethod,
                        amountPaid = payAmount,
                        paymentDate = if (p.paymentMethod == "credit") {
                            null
                        } else {
                            p.paymentDate.takeIf { it.isNotBlank() }?.let { "${it}T00:00:00-05:00" }
                        },
                        dueDate = if (p.paymentMethod == "credit") {
                            p.dueDate.takeIf { it.isNotBlank() }?.let { "${it}T23:59:59-05:00" }
                        } else {
                            null
                        }
                    )
                } else null
            }.ifEmpty { null }
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
                state.includePayment -> state.initialPayments.firstOrNull()?.paymentMethod?.takeIf { it.isNotBlank() }
                else -> null
            },
            defaultAccountId = if (includeConcepts) state.defaultExpenseAccountId else null,
            issuer = ExpensePartyRequest(
                name = state.issuerName,
                ruc = state.issuerRuc.takeIf { it.isNotBlank() },
                dv = state.issuerDv.takeIf { it.isNotBlank() },
                address = state.issuerAddress.takeIf { it.isNotBlank() },
                phone = state.issuerPhone.takeIf { it.isNotBlank() }
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
            fileUrl = if (state.isEditMode && localExpenseFile == null) state.fileUrl else null,
            removeFile = if (
                state.isEditMode &&
                state.originalFileUrl != null &&
                state.fileUrl == null &&
                localExpenseFile == null
            ) true else null,
            payments = payments,
            saveMerchant = resolveSaveMerchant(state)
        )
    }

    private fun resolveSaveMerchant(state: NewExpenseState): Boolean? {
        if (!state.isEditMode) {
            // Create mode: only send if checkbox is ON
            return if (state.saveMerchant) true else null
        }
        // Edit mode (tri-state)
        val hadMerchant = state.originalMerchantId != null
        return when {
            hadMerchant && !state.saveMerchant -> false
            state.saveMerchant -> true
            else -> null
        }
    }

    private fun clearTransientFiles() {
        localExpenseFile = null
        localInitialPaymentProofFile = null
        localInitialPaymentProofFiles.clear()
    }

    private fun validateBeforeSubmit(state: NewExpenseState): String? {
        if (state.invoiceNumber.isBlank()) {
            return "El número de factura es requerido"
        }

        if (state.emissionDate.isBlank()) {
            return "La fecha de emisión es requerida"
        }

        if (state.issuerName.isBlank()) {
            return "El nombre del emisor es requerido"
        }

        if (state.receiverName.isBlank()) {
            return "El nombre del receptor es requerido"
        }

        if (state.items.isEmpty()) {
            return "Debe agregar al menos un item"
        }

        state.items.forEachIndexed { index, item ->
            val label = "item ${index + 1}"
            if (item.description.isBlank()) {
                return "La descripcion del $label es requerida"
            }

            val quantity = parseDecimal(item.quantity)
            if (quantity == null || quantity <= 0.0) {
                return "La cantidad del $label debe ser un numero mayor a 0"
            }

            val unitPrice = parseDecimal(item.unitPrice)
            if (unitPrice == null || unitPrice <= 0.0) {
                return "El precio unitario del $label debe ser un numero mayor a 0"
            }

            val discount = parseDecimal(item.discountAmount)
            if (discount == null || discount < 0.0) {
                return "El descuento del $label debe ser un numero valido"
            }

            val itbms = parseDecimal(item.itbmsAmount)
            if (itbms == null || itbms < 0.0) {
                return "El ITBMS del $label debe ser un numero valido"
            }
        }

        val totalAmount = getTotalAmount()
        if (totalAmount <= 0.0) {
            return "El total del gasto debe ser mayor a 0"
        }

        if (!state.isEditMode && state.includePayment) {
            var totalPayments = 0.0
            state.initialPayments.forEachIndexed { index, payment ->
                val label = "pago ${index + 1}"
                val amount = parseDecimal(payment.amount)
                if (amount == null || amount <= 0.0) {
                    return "El monto del $label debe ser un numero valido"
                }
                totalPayments += amount

                if (payment.paymentMethod == "credit") {
                    if (payment.dueDate.isBlank()) {
                        return "La fecha de vencimiento es requerida para el $label (crédito)"
                    }
                } else if (payment.paymentDate.isBlank()) {
                    return "La fecha de pago es requerida para el $label"
                }
            }
            if (totalPayments - totalAmount > 0.0001) {
                return "El monto total de los pagos no puede exceder el total del gasto"
            }
        }

        if (state.isEditMode && state.hadExistingConcepts && !hasAnySelectedConcept(state)) {
            return "Selecciona al menos un concepto para guardar los cambios."
        }

        if (!state.isEditMode && !state.applyConceptPerItem) {
            return null
        }

        return null
    }

    private fun hasAnySelectedConcept(state: NewExpenseState): Boolean {
        return state.defaultExpenseAccountId != null || state.items.any { it.expenseAccountId != null }
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

    private fun updateState(reducer: NewExpenseState.() -> NewExpenseState) {
        _uiState.value = _uiState.value.reducer()
    }
}
