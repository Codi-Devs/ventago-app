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
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.loaders.shimmerBrush
import com.teco.ventago.design_system.molecules.InstallmentDueDateFieldKmp
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.features.expenses.domain.ExpensesSelectionStore
import com.teco.ventago.features.expenses.domain.models.Expense
import com.teco.ventago.features.expenses.domain.models.ExpenseItem
import com.teco.ventago.features.expenses.domain.models.ExpenseParty
import com.teco.ventago.features.expenses.domain.models.ExpensePayment
import com.teco.ventago.features.expenses.domain.models.PaymentSummary
import com.teco.ventago.features.expenses.domain.models.requests.ExpenseProofFile
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.textfields.helpers.DMDropDownField
import com.teco.ventago.features.expenses.domain.models.PaymentMethod
import com.teco.ventago.utils.randomUUID
import com.teco.ventago.navigation.PosScreens
import com.teco.ventago.utils.DateFormat.getFormattedDate
import com.teco.ventago.utils.formatNumberToMoney
import org.jetbrains.compose.resources.stringResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.expense_details

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailsScreen(
    viewModel: ExpenseDetailsViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit = {},
    onDuplicate: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(confirmValueChange = { false })
    val uriHandler = LocalUriHandler.current
    var showPaymentSheet by remember { mutableStateOf(false) }
    var showOverpaymentDialog by remember { mutableStateOf(false) }
    var pendingPaymentData by remember { mutableStateOf<PaymentSubmitData?>(null) }

    LaunchedEffect(Unit) {
        val selected = ExpensesSelectionStore.selected
        viewModel.loadExpense(selected?.id)
    }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onBack()
    }

    LaunchedEffect(uiState.paymentSuccess) {
        if (uiState.paymentSuccess) {
            showPaymentSheet = false
            viewModel.resetPaymentState()
        }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header card
        ExpenseHeaderCard(expense)

        // Items card
        if (!expense.items.isNullOrEmpty()) {
            ItemsCard(expense.items)
        }

        // Parties card
        PartiesCard(expense.issuer, expense.receiver)

        // Payment summary card
        PaymentSummaryCard(
            expense = expense,
            isPaid = expense.paymentStatus == "paid",
            hasCreditLock = viewModel.hasCreditLock(),
            onRegisterPayment = { showPaymentSheet = true }
        )

        // Payments list card
        if (!expense.payments.isNullOrEmpty()) {
            PaymentsListCard(
                payments = expense.payments,
                onMarkPaid = { viewModel.markPaymentAsPaid(it) },
                onEdit = { p ->
                    viewModel.setEditingPayment(p)
                    showPaymentSheet = true
                },
                onDelete = { viewModel.deletePayment(it.id ?: return@PaymentsListCard) },
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
                            tint = MaterialTheme.colorScheme.primary,
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
            onEdit = onEdit,
            onDuplicate = onDuplicate,
            onDelete = { viewModel.deleteExpense() },
            onOpenDgi = { cufe ->
                uriHandler.openUri("https://dgi-fep.mef.gob.pa/FacturasPorCUFE/$cufe")
            },
            onDownloadFile = { url ->
                uriHandler.openUri(url)
            },
            onGeneratePdf = { viewModel.generateAndOpenPdf() }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Payment registration/edit bottom sheet
    if (showPaymentSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showPaymentSheet = false
                viewModel.setEditingPayment(null)
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            PaymentRegistrationSheet(
                isSubmitting = uiState.isSubmittingPayment,
                error = uiState.paymentError,
                editingPayment = uiState.editingPayment,
                expenseTotalAmount = expense.totalAmount ?: 0.0,
                onSubmit = { method, amount, reference, notes, paymentDate, dueDate, proofFileUrl, proofFile ->
                    // Check for overpayment
                    val totalAmount = expense.totalAmount ?: 0.0
                    val totalPaid = expense.paymentSummary?.totalPaid ?: expense.totalPaid ?: 0.0
                    val editingAmount = uiState.editingPayment?.amountPaid ?: 0.0
                    val effectivePaid = totalPaid - editingAmount + amount
                    if (effectivePaid > totalAmount) {
                        pendingPaymentData = PaymentSubmitData(
                            method,
                            amount,
                            reference,
                            notes,
                            paymentDate,
                            dueDate,
                            proofFileUrl,
                            proofFile
                        )
                        showOverpaymentDialog = true
                    } else {
                        submitPayment(
                            viewModel,
                            uiState.editingPayment,
                            method,
                            amount,
                            reference,
                            notes,
                            paymentDate,
                            dueDate,
                            proofFileUrl,
                            proofFile
                        )
                    }
                },
                onDismiss = {
                    showPaymentSheet = false
                    viewModel.setEditingPayment(null)
                }
            )
        }
    }

    // Overpayment confirmation dialog
    if (showOverpaymentDialog && pendingPaymentData != null) {
        AlertDialog(
            onDismissRequest = { showOverpaymentDialog = false },
            title = { Text("Sobrepago") },
            text = { Text("El monto total de los pagos excede el total del gasto. ¿Desea continuar?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showOverpaymentDialog = false
                        pendingPaymentData?.let { data ->
                            submitPayment(
                                viewModel,
                                uiState.editingPayment,
                                data.method,
                                data.amount,
                                data.reference,
                                data.notes,
                                data.paymentDate,
                                data.dueDate,
                                data.proofFileUrl,
                                data.proofFile
                            )
                        }
                        pendingPaymentData = null
                    }
                ) {
                    Text("Continuar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showOverpaymentDialog = false
                    pendingPaymentData = null
                }) {
                    Text("Cancelar")
                }
            }
        )
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

private data class PaymentSubmitData(
    val method: String,
    val amount: Double,
    val reference: String,
    val notes: String,
    val paymentDate: String?,
    val dueDate: String?,
    val proofFileUrl: String? = null,
    val proofFile: ExpenseProofFile? = null
)

private fun submitPayment(
    viewModel: ExpenseDetailsViewModel,
    editingPayment: ExpensePayment?,
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
    if (editId != null) {
        viewModel.updatePayment(editId, method, amount, reference, notes, paymentDate, dueDate, proofFileUrl, proofFile)
    } else {
        viewModel.createPayment(method, amount, reference, notes, paymentDate, dueDate, proofFileUrl, proofFile)
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
                        tint = MaterialTheme.colorScheme.primary,
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
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Artículos (${items.size})", style = bodyMediumBold())
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
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${item.quantity ?: 0}x",
            style = bodyMediumBold()
        )
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
                    tint = MaterialTheme.colorScheme.primary,
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
                    party.dv?.let { append("-$it") }
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
                    tint = MaterialTheme.colorScheme.primary,
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
                    party.dv?.let { append("-$it") }
                }
                InfoRow("RUC", rucDisplay)
            } ?: Text("-", style = bodyMedium())
        }
    }
}

@Composable
private fun PaymentSummaryCard(
    expense: Expense,
    isPaid: Boolean,
    hasCreditLock: Boolean,
    onRegisterPayment: () -> Unit
) {
    val totalAmount = expense.totalAmount ?: 0.0
    val totalPaid = expense.paymentSummary?.totalPaid ?: expense.totalPaid ?: 0.0
    val remaining = expense.paymentSummary?.remaining ?: (totalAmount - totalPaid)

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
                    tint = MaterialTheme.colorScheme.primary,
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

            if (!isPaid) {
                if (hasCreditLock) {
                    Text(
                        text = "Existe un crédito pendiente por el total. Márquelo como pagado o elimínelo para registrar un nuevo pago.",
                        style = labelSmall(color = MaterialTheme.colorScheme.error),
                        modifier = Modifier.padding(top = 4.dp)
                    )
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
                    tint = MaterialTheme.colorScheme.primary,
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
                PaymentRow(payment, onMarkPaid, onEdit, onDelete, onDownloadProof)
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
                    tint = MaterialTheme.colorScheme.primary,
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
    onMarkPaid: (ExpensePayment) -> Unit,
    onEdit: (ExpensePayment) -> Unit,
    onDelete: (ExpensePayment) -> Unit,
    onDownloadProof: (String) -> Unit
) {
    val dateDisplay = payment.paymentDate?.let {
        runCatching {
            getFormattedDate(it, "yyyy-MM-dd'T'HH:mm:ss", "dd/MM/yyyy HH:mm")
        }.getOrDefault(it.take(10))
    } ?: payment.dueDate?.let {
        runCatching {
            "Vence: ${getFormattedDate(it, "yyyy-MM-dd'T'HH:mm:ss", "dd/MM/yyyy HH:mm")}"
        }.getOrDefault("Vence: ${it.take(10)}")
    } ?: ""

    val statusColor = when (payment.paymentStatus) {
        "paid" -> Color(0xFF4CAF50)
        "pending" -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    val statusLabel = when (payment.paymentStatus) {
        "paid" -> "Pagado"
        "pending" -> "Pendiente"
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
                if (payment.paymentStatus == "pending") {
                    IconButton(onClick = { onMarkPaid(payment) }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Marcar pagado",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(onClick = { onEdit(payment) }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
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

@Composable
private fun ActionsSection(
    expense: Expense,
    isDeleting: Boolean,
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
        if (expense.isManual) {
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

        // Duplicate button
        OutlinedButtonM(onClick = onDuplicate) {
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Duplicar Gasto")
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
                Text("Descargar Archivo")
            }
        } else {
            OutlinedButtonM(onClick = onGeneratePdf) {
                Icon(
                    imageVector = Icons.Rounded.Description,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generar PDF")
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
    isSubmitting: Boolean,
    error: String?,
    editingPayment: ExpensePayment? = null,
    expenseTotalAmount: Double = 0.0,
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
    val isEditMode = editingPayment != null
    val allMethods = remember { PaymentMethod.getAllMethods() }
    var selectedMethodIndex by remember {
        mutableStateOf(
            allMethods.indexOfFirst { it.value == editingPayment?.paymentMethod }.takeIf { it >= 0 } ?: 0
        )
    }
    val selectedMethod = allMethods[selectedMethodIndex]

    var amount by remember { mutableStateOf(editingPayment?.amountPaid?.let { "$it" } ?: "") }
    var reference by remember { mutableStateOf(editingPayment?.reference ?: "") }
    var notes by remember { mutableStateOf(editingPayment?.notes ?: "") }
    var paymentDate by remember { mutableStateOf(editingPayment?.paymentDate?.take(10) ?: "") }
    var dueDate by remember { mutableStateOf(editingPayment?.dueDate?.take(10) ?: "") }
    var proofFileUrl by remember { mutableStateOf(editingPayment?.proofFileUrl) }
    var proofFile by remember { mutableStateOf<ExpenseProofFile?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(if (isEditMode) "Editar Pago" else "Registrar Pago", style = titleMediumBold())

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

        DMOutlinedTextField(
            text = amount,
            label = "Monto",
            modifier = Modifier,
            onChange = { newValue ->
                val parsedAmount = newValue.toDoubleOrNull()
                amount = if (parsedAmount != null && parsedAmount > expenseTotalAmount) {
                    expenseTotalAmount.toString()
                } else {
                    newValue
                }
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

        if (selectedMethod == PaymentMethod.CREDIT) {
            InstallmentDueDateFieldKmp(
                valueIso = dueDate,
                onDatePickedIso = { dueDate = it },
                modifier = Modifier,
                label = "Fecha de vencimiento"
            )
        } else {
            InstallmentDueDateFieldKmp(
                valueIso = paymentDate,
                onDatePickedIso = { paymentDate = it },
                modifier = Modifier,
                label = "Fecha de pago"
            )
        }

        // Proof file
        if (selectedMethod != PaymentMethod.CREDIT) {
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
                    val amountVal = amount.toDoubleOrNull() ?: return@ButtonM
                    onSubmit(
                        selectedMethod.value,
                        amountVal,
                        reference,
                        notes,
                        paymentDate.ifBlank { null },
                        dueDate.ifBlank { null },
                        proofFileUrl,
                        proofFile
                    )
                },
                enabled = !isSubmitting && amount.toDoubleOrNull() != null,
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
                Text(if (isEditMode) "Actualizar" else "Registrar")
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

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Comprobante de pago (opcional)",
            style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )

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
