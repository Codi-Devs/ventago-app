package com.teco.ventago.features.expenses.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teco.ventago.core.camera.PermissionCallback
import com.teco.ventago.core.camera.PermissionStatus
import com.teco.ventago.core.camera.PermissionType
import com.teco.ventago.core.camera.createPermissionsManager
import com.teco.ventago.core.camera.rememberCameraManager
import com.teco.ventago.core.camera.rememberGalleryManager
import com.teco.ventago.core.firebase.AnalyticsService
import com.teco.ventago.core.file.rememberDocumentPickerManager
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.loaders.shimmerBrush
import com.teco.ventago.design_system.molecules.InstallmentDueDateFieldKmp
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.features.expenses.domain.ExpensesSelectionStore
import com.teco.ventago.features.expenses.domain.buildExpenseConceptLabel
import com.teco.ventago.features.expenses.domain.remainingToRegister
import com.teco.ventago.features.expenses.domain.models.Expense
import com.teco.ventago.features.expenses.domain.models.ExpenseItem
import com.teco.ventago.features.expenses.domain.models.ExpenseParty
import com.teco.ventago.features.expenses.domain.models.ExpensePayment
import com.teco.ventago.features.expenses.domain.models.requests.ExpenseProofFile
import com.teco.ventago.features.expenses.ui.components.ExpenseAccountSelectorField
import com.teco.ventago.design_system.textfields.DMMoneyOutlinedTextField
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.textfields.helpers.DMDropDownField
import com.teco.ventago.features.expenses.domain.models.PaymentMethod
import com.teco.ventago.utils.randomUUID
import com.teco.ventago.navigation.PosScreens
import com.teco.ventago.utils.DateFormat.getFormattedDate
import com.teco.ventago.utils.formatNumberToMoney
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToLong
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.expense_details

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailsScreen(
    viewModel: ExpenseDetailsViewModel,
    onBack: () -> Unit,
    openCategorization: Boolean = false,
    onEdit: () -> Unit = {},
    onDuplicate: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val analyticsService: AnalyticsService = koinInject()
    val loadingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val conceptSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current
    var showPaymentSheet by remember { mutableStateOf(false) }
    var paymentSheetMode by remember { mutableStateOf(PaymentSheetMode.REGISTER) }

    LaunchedEffect(Unit) {
        val selected = ExpensesSelectionStore.selected
        viewModel.loadExpense(selected?.id, openCategorization = openCategorization)
    }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onBack()
    }

    LaunchedEffect(uiState.paymentSuccess) {
        if (uiState.paymentSuccess) {
            showPaymentSheet = false
            paymentSheetMode = PaymentSheetMode.REGISTER
            viewModel.resetPaymentState()
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeSnackbar()
    }

    if (uiState.isLoading && uiState.expense == null) {
        ExpenseDetailsLoading()
        return
    }

    uiState.error?.let { error ->
        if (uiState.expense == null) {
            ErrorView(error) { viewModel.loadExpense() }
            return
        }
    }

    val expense = uiState.expense ?: return
    val creditLockPayment = viewModel.getCreditLockPayment()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card
            ExpenseHeaderCard(expense)

            ConceptSummaryCard(
                expense = expense,
                canEditConcepts = uiState.canUpdateExpenseAction,
                onEditConcepts = { viewModel.setConceptSheetVisible(true) }
            )

            // Items card
            if (!expense.items.isNullOrEmpty()) {
                ItemsCard(expense.items)
            }

            // Parties card
            PartiesCard(expense.issuer, expense.receiver)

            // Payment summary card
            PaymentSummaryCard(
                expense = expense,
                isFullyRegistered = remainingToRegister(expense.totalAmount ?: 0.0, expense.payments.orEmpty()) <= 0.0001,
                hasCreditLock = creditLockPayment != null,
                canManagePayments = uiState.canUpdateExpenseAction,
                onRegisterPayment = {
                    analyticsService.logExpensePaymentActionOpened(mode = "register")
                    paymentSheetMode = PaymentSheetMode.REGISTER
                    viewModel.setEditingPayment(null)
                    showPaymentSheet = true
                },
                onMarkCreditAsPaid = {
                    if (creditLockPayment != null) {
                        analyticsService.logExpensePaymentActionOpened(mode = "mark_paid")
                        paymentSheetMode = PaymentSheetMode.MARK_AS_PAID
                        viewModel.setEditingPayment(creditLockPayment)
                        showPaymentSheet = true
                    }
                }
            )

            // Payments list card
            if (!expense.payments.isNullOrEmpty()) {
                PaymentsListCard(
                    payments = expense.payments,
                    canUpdatePayments = uiState.canUpdateExpenseAction,
                    canDeletePayments = uiState.canDeleteExpenseAction,
                    onMarkPaid = { payment ->
                        analyticsService.logExpensePaymentActionOpened(mode = "mark_paid")
                        if (payment.paymentMethod == PaymentMethod.CREDIT.value && payment.paymentStatus != "paid") {
                            paymentSheetMode = PaymentSheetMode.MARK_AS_PAID
                            viewModel.setEditingPayment(payment)
                            showPaymentSheet = true
                        } else {
                            viewModel.markPaymentAsPaid(payment)
                        }
                    },
                    onEdit = { p ->
                        analyticsService.logExpensePaymentActionOpened(mode = "edit")
                        paymentSheetMode = PaymentSheetMode.EDIT
                        viewModel.setEditingPayment(p)
                        showPaymentSheet = true
                    },
                    onDelete = {
                        analyticsService.logExpensePaymentActionOpened(mode = "confirm_delete")
                        viewModel.deletePayment(it.id ?: return@PaymentsListCard)
                    },
                    onDownloadProof = { url -> uriHandler.openUri(url) }
                )
            }

            // Notes card
            if (!expense.notes.isNullOrBlank()) {
                NotesCard(expense.notes)
            }

            // CUFE card
            if (!expense.cufe.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = cardContainerColor())
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.QrCode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CUFE", style = bodyMediumBold())
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = expense.cufe,
                            style = bodyMedium(),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Actions section
            ActionsSection(
                expense = expense,
                isDeleting = uiState.isDeleting,
                canEditExpense = uiState.canUpdateExpenseAction,
                canDeleteExpense = uiState.canDeleteExpenseAction,
                onEdit = onEdit,
                onDuplicate = onDuplicate,
                onDelete = { viewModel.deleteExpense() },
                onOpenDgi = { cufe ->
                    uriHandler.openUri("https://dgi-fep.mef.gob.pa/Consultas/FacturasPorCUFE/$cufe")
                },
                onDownloadFile = { url ->
                    uriHandler.openUri(url)
                },
                onGeneratePdf = { viewModel.generateAndOpenPdf() }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }

    // Payment registration/edit bottom sheet
    if (showPaymentSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showPaymentSheet = false
                paymentSheetMode = PaymentSheetMode.REGISTER
                viewModel.setEditingPayment(null)
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            PaymentRegistrationSheet(
                mode = paymentSheetMode,
                isSubmitting = uiState.isSubmittingPayment,
                error = uiState.paymentError,
                editingPayment = uiState.editingPayment,
                pendingLimit = remainingToRegister(
                    totalAmount = expense.totalAmount ?: 0.0,
                    payments = expense.payments.orEmpty(),
                    excludePaymentId = uiState.editingPayment?.id
                ),
                hasExpensesQr = uiState.hasExpensesQr,
                onSubmit = { method, amount, reference, notes, paymentDate, dueDate, proofFileUrl, proofFile ->
                    submitPayment(
                        viewModel,
                        uiState.editingPayment,
                        paymentSheetMode,
                        method,
                        amount,
                        reference,
                        notes,
                        paymentDate,
                        dueDate,
                        proofFileUrl,
                        proofFile
                    )
                },
                onDismiss = {
                    showPaymentSheet = false
                    paymentSheetMode = PaymentSheetMode.REGISTER
                    viewModel.setEditingPayment(null)
                }
            )
        }
    }

    if (uiState.showConceptSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setConceptSheetVisible(false) },
            sheetState = conceptSheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            ExpenseConceptSheet(
                state = uiState,
                onDefaultAccountSelected = viewModel::setConceptDefaultAccount,
                onPerItemChange = viewModel::setConceptPerItem,
                onApplyToAll = viewModel::applyConceptToAllItems,
                onItemAccountSelected = viewModel::setConceptItemAccount,
                onSave = viewModel::saveConcepts,
                onDismiss = { viewModel.setConceptSheetVisible(false) }
            )
        }
    }

    if (uiState.loadingBottomSheet.isLoading()) {
        LoadingSheet(
            state = uiState.loadingBottomSheet,
            sheetState = loadingSheetState
        ) {
            viewModel.hideLoading()
        }
    }
}

@Composable
private fun ConceptSummaryCard(
    expense: Expense,
    canEditConcepts: Boolean,
    onEditConcepts: () -> Unit
) {
    val statusLabel = when (expense.categorizationStatus) {
        "categorized" -> "Con concepto"
        "partial" -> "Parcial"
        else -> "Sin concepto"
    }
    val summary = "${expense.categorizedItemsCount ?: 0}/${expense.totalItemsCount ?: expense.items.orEmpty().size} items"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Concepto", style = bodyMediumBold())
                if (canEditConcepts) {
                    TextButton(onClick = onEditConcepts) {
                        Text(
                            text = "Editar conceptos",
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            InfoRow("Estado", statusLabel)
            Spacer(modifier = Modifier.height(4.dp))
            InfoRow("Items", summary)
            expense.defaultAccount?.name?.let {
                Spacer(modifier = Modifier.height(4.dp))
                InfoRow("Concepto factura", it, maxLines = 4)
            } ?: expense.defaultAccountId?.let {
                Spacer(modifier = Modifier.height(4.dp))
                InfoRow("Concepto factura", "Concepto #$it", maxLines = 2)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = buildExpenseConceptLabel(expense),
                style = bodyMediumBold(color = MaterialTheme.colorScheme.primary)
            )
        }
    }
}

private enum class PaymentSheetMode {
    REGISTER,
    EDIT,
    MARK_AS_PAID
}

private fun submitPayment(
    viewModel: ExpenseDetailsViewModel,
    editingPayment: ExpensePayment?,
    mode: PaymentSheetMode,
    method: String,
    amount: Double,
    reference: String,
    notes: String,
    paymentDate: String?,
    dueDate: String?,
    proofFileUrl: String? = null,
    proofFile: ExpenseProofFile? = null
) {
    val editId = editingPayment?.id
    if (mode == PaymentSheetMode.REGISTER || editId == null) {
        viewModel.createPayment(method, amount, reference, notes, paymentDate, dueDate, proofFileUrl, proofFile)
    } else {
        viewModel.updatePayment(
            paymentId = editId,
            paymentMethod = method,
            amountPaid = amount,
            reference = reference,
            notes = notes,
            paymentDate = paymentDate,
            dueDate = if (mode == PaymentSheetMode.MARK_AS_PAID) null else dueDate,
            paymentStatus = if (mode == PaymentSheetMode.MARK_AS_PAID) "paid" else null,
            proofFileUrl = proofFileUrl,
            proofFile = proofFile
        )
    }
}

@Composable
private fun ExpenseHeaderCard(expense: Expense) {
    val emissionDate = expense.emissionDate?.let {
        runCatching {
            getFormattedDate(it, "yyyy-MM-dd'T'HH:mm:ss", "dd/MM/yyyy")
        }.getOrDefault(it.take(10))
    } ?: "-"

    val sourceLabel = when (expense.source) {
        "manual" -> "Manual"
        "crawled" -> "Importado"
        else -> expense.source ?: "-"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Receipt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = expense.invoiceNumber ?: "Sin número",
                        style = titleMediumBold()
                    )
                }
                PaymentStatusBadge(expense.paymentStatus)
            }
            Spacer(modifier = Modifier.height(12.dp))
            InfoRow("Fecha de emisión", emissionDate)
            Spacer(modifier = Modifier.height(4.dp))
            InfoRow("Fuente", sourceLabel)
            expense.paymentMethod?.let {
                Spacer(modifier = Modifier.height(4.dp))
                InfoRow("Método de pago", paymentMethodLabel(it))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow("Subtotal", formatNumberToMoney("${expense.subtotal ?: 0.0}"))
            Spacer(modifier = Modifier.height(4.dp))
            InfoRow("ITBMS", formatNumberToMoney("${expense.itbmsTotal ?: 0.0}"))
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Total", style = bodyMediumBold())
                Text(
                    text = formatNumberToMoney("${expense.totalAmount ?: 0.0}"),
                    style = bodyMediumBold(color = MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun ItemsCard(items: List<ExpenseItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Inventory2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Items (${items.size})", style = bodyMediumBold())
            }
            Spacer(modifier = Modifier.height(8.dp))
            items.forEachIndexed { index, item ->
                ExpenseItemRow(item)
                if (index < items.lastIndex) {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun ExpenseItemRow(item: ExpenseItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.description ?: "Sin descripción",
                style = bodyMedium(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatNumberToMoney("${item.unitPrice ?: 0.0}"),
                style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            if ((item.discountAmount ?: 0.0) > 0.0) {
                Text(
                    text = "Desc: -${formatNumberToMoney("${item.discountAmount}")}",
                    style = labelSmall(color = MaterialTheme.colorScheme.error)
                )
            }
            if ((item.itbmsAmount ?: 0.0) > 0.0) {
                Text(
                    text = "ITBMS: ${formatNumberToMoney("${item.itbmsAmount}")}",
                    style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
            Text(
                text = "Concepto: ${item.expenseAccount?.name ?: item.expenseAccountId?.let { "Concepto #$it" } ?: "Sin concepto"}",
                style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${item.quantity ?: 0}x",
            style = bodyMediumBold()
        )
    }
}

@Composable
private fun ExpenseConceptSheet(
    state: ExpenseDetailsState,
    onDefaultAccountSelected: (Long?, String?) -> Unit,
    onPerItemChange: (Boolean) -> Unit,
    onApplyToAll: () -> Unit,
    onItemAccountSelected: (Int, Long?, String?) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val editor = state.conceptEditorState
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Conceptos de gasto", style = titleMediumBold())

        ExpenseAccountSelectorField(
            label = if (editor.applyConceptPerItem) {
                "Aplicar mismo concepto a todos los items"
            } else {
                "Concepto de gasto para toda la factura"
            },
            selectedText = editor.defaultAccountName.orEmpty(),
            placeholder = "Sin concepto de gasto",
            accounts = editor.expenseAccounts,
            isLoading = editor.isLoadingAccounts,
            leafOnly = true,
            emptyOptionLabel = "Sin concepto de gasto",
            clickHintLabel = "Toca para asignar el concepto de gasto",
            selectedTextColor = MaterialTheme.colorScheme.secondary,
            placeholderTextColor = MaterialTheme.colorScheme.secondary,
            hint = if (editor.applyConceptPerItem) {
                "Toca el selector para elegir un concepto y luego presiona \"Aplicar a todos\" para asignarlo a cada item."
            } else {
                "Toca el selector para elegir un concepto. Al guardar en este modo, se aplicara el mismo concepto a todos los items."
            },
            onSelected = onDefaultAccountSelected
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = editor.applyConceptPerItem,
                onCheckedChange = onPerItemChange
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Aplicar concepto por item", style = bodyMedium())
        }

        if (editor.applyConceptPerItem) {
            TextButtonS(label = "Aplicar a todos") {
                onApplyToAll()
            }
            editor.items.forEachIndexed { index, item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(1.dp),
                    colors = CardDefaults.cardColors(containerColor = cardContainerColor())
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(item.description, style = bodyMediumBold())
                        Spacer(modifier = Modifier.height(8.dp))
                        ExpenseAccountSelectorField(
                            label = "Concepto",
                            selectedText = item.expenseAccountName.orEmpty(),
                            placeholder = "Sin concepto",
                            accounts = editor.expenseAccounts,
                            isLoading = editor.isLoadingAccounts,
                            leafOnly = true,
                            emptyOptionLabel = "Sin concepto",
                            clickHintLabel = "Toca para asignar concepto",
                            selectedTextColor = MaterialTheme.colorScheme.secondary,
                            placeholderTextColor = MaterialTheme.colorScheme.secondary,
                            onSelected = { accountId, accountName ->
                                onItemAccountSelected(index, accountId, accountName)
                            }
                        )
                    }
                }
            }
        }

        state.conceptError?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = bodyMedium()
            )
        }

        ButtonM(onClick = onSave, enabled = !state.isSavingConcepts) {
            Text("Guardar conceptos")
        }
        OutlinedButtonM(onClick = onDismiss) {
            Text("Cancelar")
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PartiesCard(issuer: ExpenseParty?, receiver: ExpenseParty?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Business,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Emisor", style = bodyMediumBold())
            }
            Spacer(modifier = Modifier.height(4.dp))
            issuer?.let { party ->
                InfoRow("Nombre", party.name ?: "-")
                Spacer(modifier = Modifier.height(4.dp))
                val rucDisplay = buildString {
                    append(party.ruc ?: "-")
                }
                InfoRow("RUC", rucDisplay)
            } ?: Text("-", style = bodyMedium())

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Business,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Receptor", style = bodyMediumBold())
            }
            Spacer(modifier = Modifier.height(4.dp))
            receiver?.let { party ->
                InfoRow("Nombre", party.name ?: "-")
                Spacer(modifier = Modifier.height(4.dp))
                val rucDisplay = buildString {
                    append(party.ruc ?: "-")
                }
                InfoRow("RUC", rucDisplay)
            } ?: Text("-", style = bodyMedium())
        }
    }
}

@Composable
private fun PaymentSummaryCard(
    expense: Expense,
    isFullyRegistered: Boolean,
    hasCreditLock: Boolean,
    canManagePayments: Boolean,
    onRegisterPayment: () -> Unit,
    onMarkCreditAsPaid: () -> Unit
) {
    val totalAmount = expense.totalAmount ?: 0.0
    val totalPaid = expense.paymentSummary?.totalPaid ?: expense.totalPaid ?: 0.0
    val remaining = remainingToRegister(totalAmount, expense.payments.orEmpty())
    val creditBadge = remember(expense) { buildCreditDueBadge(expense) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Payment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Resumen de Pagos", style = bodyMediumBold())
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryItem(
                    label = "Total",
                    value = formatNumberToMoney("$totalAmount"),
                    color = MaterialTheme.colorScheme.onSurface
                )
                SummaryItem(
                    label = "Pagado",
                    value = formatNumberToMoney("$totalPaid"),
                    color = Color(0xFF4CAF50)
                )
                SummaryItem(
                    label = "Pendiente",
                    value = formatNumberToMoney("$remaining"),
                    color = if (remaining > 0) Color(0xFFF44336) else Color(0xFF4CAF50)
                )
            }

            creditBadge?.let { badge ->
                Text(
                    text = badge.text,
                    style = labelSmall(color = badge.textColor),
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .background(badge.containerColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            if (!isFullyRegistered && canManagePayments) {
                if (hasCreditLock) {
                    Text(
                        text = "Existe un crédito pendiente por el total. Debe marcarlo como pagado para cerrar el gasto.",
                        style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    ButtonM(
                        onClick = onMarkCreditAsPaid,
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Marcar como pagado")
                    }
                } else {
                    ButtonM(onClick = onRegisterPayment) {
                        Icon(
                            imageVector = Icons.Rounded.Payment,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Registrar Pago")
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentsListCard(
    payments: List<ExpensePayment>,
    canUpdatePayments: Boolean,
    canDeletePayments: Boolean,
    onMarkPaid: (ExpensePayment) -> Unit,
    onEdit: (ExpensePayment) -> Unit,
    onDelete: (ExpensePayment) -> Unit,
    onDownloadProof: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Historial de Pagos (${payments.size})",
                    style = bodyMediumBold()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            payments.forEachIndexed { index, payment ->
                PaymentRow(
                    payment = payment,
                    canUpdatePayments = canUpdatePayments,
                    canDeletePayments = canDeletePayments,
                    onMarkPaid = onMarkPaid,
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onDownloadProof = onDownloadProof
                )
                if (index < payments.lastIndex) {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun NotesCard(notes: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Notes,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Notas", style = bodyMediumBold())
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = notes, style = bodyMedium())
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = bodyMediumBold().copy(color = color)
        )
    }
}

@Composable
private fun PaymentRow(
    payment: ExpensePayment,
    canUpdatePayments: Boolean,
    canDeletePayments: Boolean,
    onMarkPaid: (ExpensePayment) -> Unit,
    onEdit: (ExpensePayment) -> Unit,
    onDelete: (ExpensePayment) -> Unit,
    onDownloadProof: (String) -> Unit
) {
    val dateDisplay = payment.paymentDate?.let {
        formatPaymentDate(it)
    } ?: payment.dueDate?.let {
        "Vence: ${formatPaymentDate(it)}"
    } ?: ""

    val statusColor = when (payment.paymentStatus) {
        "paid" -> Color(0xFF4CAF50)
        "not_paid" -> if (payment.isOverdue == true) Color(0xFFF44336) else Color(0xFFFF9800)
        "pending" -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    val dueLabel = dueStatusLabel(payment.dueDate)
    val statusLabel = when (payment.paymentStatus) {
        "paid" -> "Pagado"
        "pending" -> dueLabel ?: "Pendiente"
        "not_paid" -> dueLabel ?: "No pagado"
        else -> payment.paymentStatus ?: ""
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = paymentMethodLabel(payment.paymentMethod),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = dateDisplay,
                style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            payment.reference?.let {
                if (it.isNotBlank()) {
                    Text(
                        text = "Ref: $it",
                        style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
            if (payment.isOverdue == true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = Color(0xFFF44336),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Vencido (${payment.daysOverdue ?: 0} días)",
                        style = labelSmall(color = Color(0xFFF44336))
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatNumberToMoney("${payment.amountPaid ?: 0.0}"),
                style = bodyMediumBold()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = statusLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                modifier = Modifier
                    .background(statusColor, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!payment.proofFileUrl.isNullOrBlank()) {
                    IconButton(onClick = { onDownloadProof(payment.proofFileUrl) }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Description,
                            contentDescription = "Ver comprobante",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (canUpdatePayments && payment.paymentStatus != "paid") {
                    IconButton(onClick = { onMarkPaid(payment) }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Marcar pagado",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (canUpdatePayments) {
                    IconButton(onClick = { onEdit(payment) }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Editar",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (canDeletePayments) {
                    IconButton(onClick = { onDelete(payment) }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatPaymentDate(raw: String): String {
    val normalized = raw.trim()
    // Extract date part (YYYY-MM-DD) from ISO datetime strings
    val datePrefix = if (normalized.length >= 10) normalized.take(10) else normalized
    return try {
        val parts = datePrefix.split("-")
        if (parts.size == 3) {
            val year = parts[0]
            val month = parts[1]
            val day = parts[2]
            "$day/$month/$year"
        } else {
            datePrefix
        }
    } catch (_: Exception) {
        datePrefix
    }
}

@Composable
private fun ActionsSection(
    expense: Expense,
    isDeleting: Boolean,
    canEditExpense: Boolean,
    canDeleteExpense: Boolean,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onOpenDgi: (String) -> Unit = {},
    onDownloadFile: (String) -> Unit = {},
    onGeneratePdf: () -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Edit button (manual only)
        if (expense.isManual && canEditExpense) {
            ButtonM(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Editar Gasto")
            }
        }

        // Duplicate button (manual only)
        if (expense.isManual && canEditExpense) {
            OutlinedButtonM(onClick = onDuplicate) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Duplicar Gasto")
            }
        }

        // Download original file or generate non-fiscal PDF
        val fileUrl = expense.fileUrl
        if (!fileUrl.isNullOrBlank()) {
            OutlinedButtonM(onClick = { onDownloadFile(fileUrl) }) {
                Icon(
                    imageVector = Icons.Rounded.Description,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Descargar factura")
            }
        }

        // DGI link (crawled expenses with CUFE)
        if (!expense.isManual && !expense.cufe.isNullOrBlank()) {
            OutlinedButtonM(onClick = { onOpenDgi(expense.cufe) }) {
                Icon(
                    imageVector = Icons.Rounded.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ver en DGI")
            }
        }

        // Delete button
        if (canDeleteExpense) {
            OutlinedButtonM(
                onClick = { showDeleteDialog = true },
                enabled = !isDeleting
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    "Eliminar Gasto",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar Gasto") },
            text = { Text("¿Estás seguro de que deseas eliminar este gasto? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String, maxLines: Int = 1) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = bodyMedium(),
            textAlign = TextAlign.End,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@Composable
private fun PaymentStatusBadge(status: String?) {
    val statusColor = when (status) {
        "paid" -> Color(0xFF4CAF50)
        "partial" -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    val statusLabel = when (status) {
        "paid" -> "Pagado"
        "partial" -> "Parcial"
        "not_paid" -> "No pagado"
        else -> status ?: ""
    }
    Text(
        text = statusLabel,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = Color.White,
        modifier = Modifier
            .background(statusColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

private fun paymentMethodLabel(method: String?): String = PaymentMethod.getLabel(method)

private data class CreditDueBadge(
    val text: String,
    val containerColor: Color,
    val textColor: Color
)

private fun buildCreditDueBadge(expense: Expense): CreditDueBadge? {
    val creditPayment = expense.payments
        .orEmpty()
        .firstOrNull { it.paymentMethod == PaymentMethod.CREDIT.value && it.paymentStatus != "paid" }
        ?: expense.payments
            .orEmpty()
            .firstOrNull { it.paymentMethod == PaymentMethod.CREDIT.value }
        ?: return null

    val dueDate = parseLocalDatePrefix(creditPayment.dueDate) ?: return null
    val today = currentLocalDate()
    val days = today.daysUntil(dueDate)
    val label = when {
        days < 0 -> "Vencido"
        days == 0 -> "Vence hoy"
        days == 1 -> "Vence en 1 día"
        else -> "Vence en $days días"
    }

    return if (days < 0) {
        CreditDueBadge(
            text = label,
            containerColor = Color(0xFFFFEBEE),
            textColor = Color(0xFFD32F2F)
        )
    } else {
        CreditDueBadge(
            text = label,
            containerColor = Color(0xFFFFF8E1),
            textColor = Color(0xFFFF8F00)
        )
    }
}

private fun currentLocalDate(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

private fun LocalDate.toIsoDate(): String =
    "${year.toString().padStart(4, '0')}-${monthNumber.toString().padStart(2, '0')}-${dayOfMonth.toString().padStart(2, '0')}"

private fun amountToRawCents(amount: Double): String =
    (amount * 100.0).roundToLong().coerceAtLeast(0L).toString()

private fun rawCentsToAmountDouble(raw: String): Double =
    (raw.filter(Char::isDigit).toLongOrNull() ?: 0L) / 100.0

private fun dueStatusLabel(rawDueDate: String?): String? {
    val dueDate = parseLocalDatePrefix(rawDueDate) ?: return null
    val today = currentLocalDate()
    val days = today.daysUntil(dueDate)
    return when {
        days < 0 -> "Vencido"
        days == 0 -> "Vence hoy"
        days == 1 -> "Vence en 1 día"
        else -> "Vence en $days días"
    }
}

private fun parseLocalDatePrefix(value: String?): LocalDate? {
    if (value.isNullOrBlank()) return null
    return try {
        val date = value.take(10)
        val parts = date.split("-")
        if (parts.size != 3) return null
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val day = parts[2].toInt()
        LocalDate(year, month, day)
    } catch (_: Exception) {
        null
    }
}

@Composable
private fun ExpenseDetailsLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (it == 0) 200.dp else 120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(shimmerBrush())
            )
        }
    }
}

@Composable
private fun PaymentRegistrationSheet(
    mode: PaymentSheetMode,
    isSubmitting: Boolean,
    error: String?,
    editingPayment: ExpensePayment? = null,
    pendingLimit: Double = 0.0,
    hasExpensesQr: Boolean = false,
    onSubmit: (
        method: String,
        amount: Double,
        reference: String,
        notes: String,
        paymentDate: String?,
        dueDate: String?,
        proofFileUrl: String?,
        proofFile: ExpenseProofFile?
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val isEditMode = mode == PaymentSheetMode.EDIT
    val isMarkAsPaidMode = mode == PaymentSheetMode.MARK_AS_PAID
    val allMethods = remember(mode) {
        if (isMarkAsPaidMode) {
            PaymentMethod.getAllMethods().filter { it != PaymentMethod.CREDIT }
        } else {
            PaymentMethod.getAllMethods()
        }
    }
    val initialMethodValue = when {
        isMarkAsPaidMode && editingPayment?.paymentMethod == PaymentMethod.CREDIT.value -> PaymentMethod.CASH.value
        else -> editingPayment?.paymentMethod
    }
    var selectedMethodIndex by remember {
        mutableStateOf(
            allMethods.indexOfFirst { it.value == initialMethodValue }.takeIf { it >= 0 } ?: 0
        )
    }
    val selectedMethod = allMethods.getOrElse(selectedMethodIndex) { PaymentMethod.CASH }
    val today = remember { currentLocalDate() }
    val todayIso = remember(today) { today.toIsoDate() }
    val tomorrow = remember(today) { today.plus(DatePeriod(days = 1)) }

    var amount by remember {
        mutableStateOf(
            editingPayment?.amountPaid?.let { amountToRawCents(it) }
                ?: amountToRawCents(pendingLimit)
        )
    }
    var reference by remember { mutableStateOf(editingPayment?.reference ?: "") }
    var notes by remember { mutableStateOf(editingPayment?.notes ?: "") }
    var paymentDate by remember {
        mutableStateOf(
            editingPayment?.paymentDate?.take(10)
                ?: if (isMarkAsPaidMode) todayIso else todayIso
        )
    }
    var dueDate by remember {
        mutableStateOf(
            if (isMarkAsPaidMode) "" else editingPayment?.dueDate?.take(10).orEmpty()
        )
    }
    var proofFileUrl by remember { mutableStateOf(editingPayment?.proofFileUrl) }
    var proofFile by remember { mutableStateOf<ExpenseProofFile?>(null) }
    var proofSectionExpanded by remember(proofFileUrl, proofFile) {
        mutableStateOf(proofFileUrl != null || proofFile != null)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val sheetTitle = when (mode) {
            PaymentSheetMode.EDIT -> "Editar Pago"
            PaymentSheetMode.MARK_AS_PAID -> "Marcar como pagado"
            PaymentSheetMode.REGISTER -> "Registrar Pago"
        }
        Text(sheetTitle, style = titleMediumBold())

        // Payment method dropdown
        DMDropDownField(
            modifier = Modifier.fillMaxWidth(),
            label = "Método de pago",
            items = allMethods,
            selectedIndex = selectedMethodIndex,
            onItemSelected = { index, _ ->
                selectedMethodIndex = index
            },
            selectedItemToString = { it.label }
        )

        DMMoneyOutlinedTextField(
            text = amount,
            label = "Monto",
            modifier = Modifier,
            readOnly = isMarkAsPaidMode,
            onChange = { newValue ->
                val cents = newValue.filter(Char::isDigit).toLongOrNull() ?: 0L
                val maxCents = (pendingLimit * 100.0).roundToLong().coerceAtLeast(0L)
                amount = cents.coerceAtMost(maxCents).toString()
            }
        )

        DMOutlinedTextField(
            text = reference,
            label = "Referencia (opcional)",
            modifier = Modifier,
            onChange = { reference = it }
        )

        DMOutlinedTextField(
            text = notes,
            label = "Notas (opcional)",
            modifier = Modifier,
            onChange = { notes = it }
        )

        if (selectedMethod == PaymentMethod.CREDIT && !isMarkAsPaidMode) {
            InstallmentDueDateFieldKmp(
                valueIso = dueDate,
                onDatePickedIso = { dueDate = it },
                modifier = Modifier,
                label = "Fecha de vencimiento",
                minSelectableDate = tomorrow
            )
        } else {
            InstallmentDueDateFieldKmp(
                valueIso = paymentDate,
                onDatePickedIso = { paymentDate = it },
                modifier = Modifier,
                label = "Fecha de pago",
                maxSelectableDate = today
            )
        }

        // Proof file
        if (selectedMethod != PaymentMethod.CREDIT && hasExpensesQr) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                elevation = CardDefaults.cardElevation(1.dp),
                colors = CardDefaults.cardColors(containerColor = cardContainerColor())
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Comprobante de pago (opcional)",
                            style = bodyMediumBold()
                        )
                        IconButton(
                            onClick = { proofSectionExpanded = !proofSectionExpanded },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (proofSectionExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = if (proofSectionExpanded) "Colapsar" else "Expandir",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (proofSectionExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ProofFileSection(
                            proofFileUrl = proofFileUrl,
                            proofFile = proofFile,
                            onFileSelected = { selected ->
                                proofFile = selected
                                proofFileUrl = null
                            },
                            onRemove = {
                                proofFileUrl = null
                                proofFile = null
                            }
                        )
                    }
                }
            }
        }

        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = bodyMedium()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButtonM(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                contentColor = Color(0xFFD32F2F),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color(0xFFD32F2F)
                )
            ) {
                Text("Cancelar")
            }
            ButtonM(
                onClick = {
                    val amountVal = rawCentsToAmountDouble(amount)
                    onSubmit(
                        selectedMethod.value,
                        amountVal,
                        reference,
                        notes,
                        if (selectedMethod == PaymentMethod.CREDIT && !isMarkAsPaidMode) null else paymentDate.ifBlank { null },
                        if (selectedMethod == PaymentMethod.CREDIT && !isMarkAsPaidMode) dueDate.ifBlank { null } else null,
                        proofFileUrl,
                        proofFile
                    )
                },
                enabled = !isSubmitting && rawCentsToAmountDouble(amount) > 0.0,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                val submitLabel = when (mode) {
                    PaymentSheetMode.EDIT -> "Actualizar"
                    PaymentSheetMode.MARK_AS_PAID -> "Marcar pagado"
                    PaymentSheetMode.REGISTER -> "Registrar"
                }
                Text(submitLabel)
            }
        }
    }
}

@Composable
private fun ProofFileSection(
    proofFileUrl: String?,
    proofFile: ExpenseProofFile?,
    onFileSelected: (ExpenseProofFile) -> Unit,
    onRemove: () -> Unit
) {
    var launchCamera by remember { mutableStateOf(false) }
    var launchGallery by remember { mutableStateOf(false) }
    var launchDocument by remember { mutableStateOf(false) }
    var launchSetting by remember { mutableStateOf(false) }

    val permissionsManager = createPermissionsManager(object : PermissionCallback {
        override fun onPermissionStatus(permissionType: PermissionType, status: PermissionStatus) {
            when (status) {
                PermissionStatus.GRANTED -> when (permissionType) {
                    PermissionType.CAMERA -> launchCamera = true
                    PermissionType.GALLERY -> launchGallery = true
                }
                else -> {}
            }
        }
    })

    val cameraManager = rememberCameraManager { image ->
        if (image != null) {
            val imageData = image.toByteArray()
            if (imageData != null) {
                onFileSelected(
                    ExpenseProofFile(
                        bytes = imageData,
                        fileName = "proof_${randomUUID()}.jpg",
                        contentType = "image/jpeg"
                    )
                )
            }
        }
    }
    val galleryManager = rememberGalleryManager { image ->
        if (image != null) {
            val imageData = image.toByteArray()
            if (imageData != null) {
                onFileSelected(
                    ExpenseProofFile(
                        bytes = imageData,
                        fileName = "proof_${randomUUID()}.jpg",
                        contentType = "image/jpeg"
                    )
                )
            }
        }
    }
    val documentManager = rememberDocumentPickerManager(
        acceptedMimeTypes = listOf("application/pdf")
    ) { file ->
        if (file != null) {
            onFileSelected(
                ExpenseProofFile(
                    bytes = file.bytes,
                    fileName = file.fileName,
                    contentType = file.contentType
                )
            )
        }
    }

    if (launchGallery) {
        if (permissionsManager.isPermissionGranted(PermissionType.GALLERY)) {
            galleryManager.launch()
        } else {
            permissionsManager.askPermission(PermissionType.GALLERY)
        }
        launchGallery = false
    }
    if (launchCamera) {
        if (permissionsManager.isPermissionGranted(PermissionType.CAMERA)) {
            cameraManager.launch()
        } else {
            permissionsManager.askPermission(PermissionType.CAMERA)
        }
        launchCamera = false
    }
    if (launchSetting) {
        permissionsManager.launchSettings()
        launchSetting = false
    }
    if (launchDocument) {
        documentManager.launch()
        launchDocument = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (proofFile != null || proofFileUrl != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        proofFile?.fileName ?: "Comprobante adjunto",
                        style = bodyMedium(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButtonM(
                    onClick = { launchCamera = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cámara")
                }
                OutlinedButtonM(
                    onClick = { launchGallery = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Collections,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Galería")
                }
                OutlinedButtonM(
                    onClick = { launchDocument = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Description,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF")
                }
            }
        }
    }
}

@Composable
private fun ErrorView(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Warning,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        ButtonM(onClick = onRetry) {
            Text("Reintentar")
        }
    }
}
