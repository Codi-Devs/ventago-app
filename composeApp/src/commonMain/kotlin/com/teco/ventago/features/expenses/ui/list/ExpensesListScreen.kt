package com.teco.ventago.features.expenses.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalParking
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.teco.ventago.core.SnackbarService
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.loaders.shimmerBrush
import com.teco.ventago.design_system.molecules.InstallmentDueDateFieldKmp
import com.teco.ventago.design_system.molecules.list.ListAutocompleteSearchField
import com.teco.ventago.design_system.molecules.list.TransactionListCard
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.features.expenses.ui.components.ExpenseSheetOption
import com.teco.ventago.features.expenses.ui.upload.InvoiceUploadCapture
import com.teco.ventago.features.expenses.domain.CrawlErrorCopy
import com.teco.ventago.features.expenses.domain.CufeParser
import com.teco.ventago.features.expenses.domain.ExpenseConceptVisualCategory
import com.teco.ventago.features.expenses.domain.resolveExpenseConceptVisualCategory
import com.teco.ventago.features.expenses.domain.models.CrawlJob
import com.teco.ventago.features.expenses.domain.models.Expense
import com.teco.ventago.features.expenses.domain.models.ExpenseMerchant
import com.teco.ventago.features.expenses.domain.models.PaymentMethod
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import com.teco.ventago.navigation.PosScreens
import com.teco.ventago.navigation.LocalNavController
import com.teco.ventago.utils.formatTransactionListDate
import com.teco.ventago.utils.formatNumberToMoney
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import androidx.navigation.NavBackStackEntry
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.apply_filters
import ventago.composeapp.generated.resources.clear_filters
import ventago.composeapp.generated.resources.expenses
import ventago.composeapp.generated.resources.expenses_empty
import ventago.composeapp.generated.resources.expenses_issuer_name
import ventago.composeapp.generated.resources.expenses_issuer_ruc
import ventago.composeapp.generated.resources.expenses_search_placeholder
import ventago.composeapp.generated.resources.expenses_source_all
import ventago.composeapp.generated.resources.expenses_source_crawled
import ventago.composeapp.generated.resources.expenses_source_manual
import ventago.composeapp.generated.resources.expenses_syncing
import ventago.composeapp.generated.resources.expenses_start_date
import ventago.composeapp.generated.resources.expenses_end_date
import ventago.composeapp.generated.resources.filter_all
import ventago.composeapp.generated.resources.see_more

@Composable
fun ExpensesListScreenActions(backStackEntry: NavBackStackEntry?) {
    val navController = LocalNavController.current
    val expensesOwner = remember(navController) {
        navController.getBackStackEntry(PosScreens.Expenses.name)
    }
    val viewModel: ExpensesListViewModel = koinViewModel(viewModelStoreOwner = expensesOwner)
    Box {
        IconButton(onClick = { viewModel.showFiltersSheet(true) }) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = "Filtros",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        val activeFilterCount = viewModel.activeFilterCount()
        if (activeFilterCount > 0) {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).size(18.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = activeFilterCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExpensesListScreen(
    viewModel: ExpensesListViewModel,
    uploadViewModel: com.teco.ventago.features.expenses.ui.upload.InvoiceUploadViewModel,
    navigate: (PosScreens) -> Unit,
    onImportCufe: (cufe: String?, autoImport: Boolean, openScanner: Boolean) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showRegisterSheet by remember { mutableStateOf(false) }
    var showInvoiceSourceSheet by remember { mutableStateOf(false) }
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val registerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarService: SnackbarService = koinInject()
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.refreshing,
        onRefresh = { viewModel.loadExpenses(refresh = true) }
    )

    Box(modifier = Modifier.fillMaxSize()) {

    val paymentStatusOptions = listOf(
        "not_paid" to "No pagado",
        "partial" to "Parcial",
        "paid" to "Pagado"
    )
    val visibleCrawlJobs = remember(uiState.crawlJobs) {
        uiState.crawlJobs.filter { job ->
            job.status in setOf("pending", "processing", "failed", "error", "cancelled", "timeout")
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ListAutocompleteSearchField(
            query = uiState.issuerName,
            hasSelection = uiState.merchantId != null,
            isSearching = uiState.isMerchantSearching,
            items = uiState.merchantSuggestions.take(4),
            label = "Buscar por emisor",
            searchContentDescription = "Buscar por emisor",
            clearContentDescription = "Quitar emisor",
            onQueryChanged = viewModel::setIssuerName,
            onItemSelected = viewModel::selectMerchantSearch,
            onClear = viewModel::clearMerchantSearch,
            itemTitle = { it.name },
            itemSubtitle = { merchant ->
                merchant.ruc?.takeIf { it.isNotBlank() }?.let { ruc ->
                    "RUC: $ruc${merchant.dv?.takeIf { it.isNotBlank() }?.let { "-$it" }.orEmpty()}"
                }.orEmpty()
            },
            itemIcon = Icons.Rounded.Store,
        )

        // Source filter chips Not showing by now
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .horizontalScroll(rememberScrollState())
//                .padding(horizontal = 16.dp),
//            horizontalArrangement = Arrangement.spacedBy(8.dp)
//        ) {
//            sourceOptions.forEach { (value, label) ->
//                FilterChip(
//                    selected = uiState.source == value,
//                    onClick = {
//                        viewModel.setSource(value)
//                        viewModel.applyFilters()
//                    },
//                    label = { Text(label) },
//                    colors = FilterChipDefaults.filterChipColors()
//                )
//            }
//        }

        // Payment status filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // "Todos" chip
            FilterChip(
                selected = uiState.paymentStatuses.isEmpty(),
                onClick = {
                    viewModel.setPaymentStatuses(emptyList())
                    viewModel.applyFilters()
                },
                label = { Text(stringResource(Res.string.filter_all)) },
                colors = FilterChipDefaults.filterChipColors()
            )
            paymentStatusOptions.forEach { (value, label) ->
                FilterChip(
                    selected = value in uiState.paymentStatuses,
                    onClick = {
                        val isSelected = value in uiState.paymentStatuses
                        viewModel.setPaymentStatuses(if (isSelected) emptyList() else listOf(value))
                        viewModel.applyFilters()
                    },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors()
                )
            }
        }

        // Crawl jobs summary (only when there are pending/failed jobs)
        if (uiState.hasExpensesQr && visibleCrawlJobs.isNotEmpty()) {
            CrawlJobsSummary(
                jobs = visibleCrawlJobs,
                isLoading = uiState.isLoadingCrawlJobs,
                onDismissFailedJob = viewModel::dismissFailedCrawlJob,
                onRetryFailedJob = { job ->
                    viewModel.retryFailedCrawlJob(job) { cufe ->
                        onImportCufe(cufe, true, false)
                    }
                },
                onCopyCufe = { cufe ->
                    clipboard.setText(AnnotatedString(cufe))
                    scope.launch { snackbarService.show("CUFE copiado") }
                }
            )
        }

        // Error message
        uiState.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // Sync indicator
        if (uiState.isSyncing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.expenses_syncing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // List
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            if (uiState.isLoading && uiState.expenses.isEmpty()) {
                LoadingExpensesView()
            } else if (uiState.expenses.isEmpty() && !uiState.isLoading && !uiState.refreshing) {
                EmptyExpensesView()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.expenses, key = { it.id ?: it.hashCode() }) { expense ->
                        ExpenseListItem(expense, viewModel.expenseConceptLabel(expense)) {
                            viewModel.selectExpense(expense)
                            navigate(PosScreens.ExpenseDetailsScreen)
                        }
                    }
                    item {
                        if (uiState.noMore) return@item
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            when {
                                uiState.isLoading -> CircularProgressIndicator()
                                else -> TextButtonS(
                                    label = stringResource(Res.string.see_more)
                                ) { viewModel.loadExpenses() }
                            }
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = uiState.refreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (uiState.canCreateExpense) {
                    FloatingActionButton(
                        onClick = { showRegisterSheet = true },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Registrar gasto",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }

    // Filters bottom sheet
    if (uiState.showFiltersSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.showFiltersSheet(false) },
            sheetState = filterSheetState
        ) {
            ExpensesFilterSheet(
                startDate = uiState.startDate ?: "",
                onStartDateChange = { viewModel.setStartDate(it.ifBlank { null }) },
                endDate = uiState.endDate ?: "",
                onEndDateChange = { viewModel.setEndDate(it.ifBlank { null }) },
                issuerRuc = uiState.issuerRuc,
                onIssuerRucChange = { viewModel.setIssuerRuc(it) },
                invoiceNumber = uiState.invoiceNumber,
                onInvoiceNumberChange = { viewModel.setInvoiceNumber(it) },
                onApply = {
                    viewModel.showFiltersSheet(false)
                    viewModel.applyFilters()
                },
                onClear = {
                    viewModel.clearFilters()
                }
            )
        }
    }

    if (showRegisterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRegisterSheet = false },
            sheetState = registerSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Registrar gasto",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExpenseSheetOption(
                    icon = Icons.Rounded.Edit,
                    title = "Registro manual",
                    subtitle = "Completa los datos del gasto"
                ) {
                    showRegisterSheet = false
                    navigate(PosScreens.NewExpenseScreen)
                }
                if (uiState.hasInvoiceUploadAccess) {
                    ExpenseSheetOption(
                        icon = Icons.Rounded.PhotoCamera,
                        title = "Sube tu factura",
                        subtitle = "Toma una foto o elige una imagen o PDF"
                    ) {
                        showRegisterSheet = false
                        showInvoiceSourceSheet = true
                    }
                }
                if (uiState.hasExpensesQr) {
                    ExpenseSheetOption(
                        icon = Icons.Rounded.QrCodeScanner,
                        title = "Escanear QR",
                        subtitle = "Importa desde DGI con el CUFE"
                    ) {
                        showRegisterSheet = false
                        onImportCufe(null, false, true)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    InvoiceUploadCapture(
        viewModel = uploadViewModel,
        showSourceSheet = showInvoiceSourceSheet,
        onDismissSourceSheet = { showInvoiceSourceSheet = false },
        onCancelSourceSheet = { showInvoiceSourceSheet = false },
        onFinished = { showInvoiceSourceSheet = false },
        onAcceptedAndContinue = {
            showInvoiceSourceSheet = false
            viewModel.loadExpenses(refresh = true)
        },
        onRequestAnotherUpload = { showInvoiceSourceSheet = true }
    )
    }
}

@Composable
private fun ExpensesFilterSheet(
    startDate: String,
    onStartDateChange: (String) -> Unit,
    endDate: String,
    onEndDateChange: (String) -> Unit,
    issuerRuc: String,
    onIssuerRucChange: (String) -> Unit,
    invoiceNumber: String,
    onInvoiceNumberChange: (String) -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit
) {
    val today = remember { currentLocalDate() }
    val startLocalDate = remember(startDate) { parseLocalDatePrefix(startDate) }
    val endLocalDate = remember(endDate) { parseLocalDatePrefix(endDate) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Filtros",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InstallmentDueDateFieldKmp(
                valueIso = startDate,
                onDatePickedIso = onStartDateChange,
                modifier = Modifier.weight(1f),
                label = stringResource(Res.string.expenses_start_date),
                maxSelectableDate = endLocalDate ?: today
            )
            InstallmentDueDateFieldKmp(
                valueIso = endDate,
                onDatePickedIso = onEndDateChange,
                modifier = Modifier.weight(1f),
                label = stringResource(Res.string.expenses_end_date),
                minSelectableDate = startLocalDate,
                maxSelectableDate = today
            )
        }

        Text(
            text = "Rango máximo: 3 meses",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        DMOutlinedTextField(
            text = invoiceNumber,
            label = "Número de factura",
            modifier = Modifier,
            onChange = onInvoiceNumberChange,
        )

        DMOutlinedTextField(
            text = issuerRuc,
            label = stringResource(Res.string.expenses_issuer_ruc),
            modifier = Modifier,
            onChange = onIssuerRucChange
        )

        Spacer(modifier = Modifier.height(8.dp))

        ButtonM(
            onClick = onApply,
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ) {
            Text(stringResource(Res.string.apply_filters))
        }

        OutlinedButtonM(onClick = onClear) {
            Text(stringResource(Res.string.clear_filters))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ExpenseListItem(
    expense: Expense,
    conceptLabel: String,
    onClick: () -> Unit
) {
    val emissionDate = formatTransactionListDate(expense.emissionDate)

    val totalFormatted = formatNumberToMoney("${expense.totalAmount ?: 0.0}")
    val (statusTextColor, statusContainerColor) = when (expense.paymentStatus) {
        "paid" -> Color(0xFF2E7D32) to Color(0xFFE8F5E9)
        "partial" -> Color(0xFFFF8F00) to Color(0xFFFFF8E1)
        else -> Color(0xFFD32F2F) to Color(0xFFFFEBEE)
    }
    val statusLabel = when (expense.paymentStatus) {
        "paid" -> "Pagado"
        "partial" -> "Parcial"
        "not_paid" -> "No pagado"
        else -> expense.paymentStatus ?: ""
    }
    val creditDueBadge = remember(expense) { buildCreditDueBadge(expense) }

    TransactionListCard(
        icon = expenseConceptIcon(expense),
        iconContentDescription = "Concepto de gasto",
        iconTint = MaterialTheme.colorScheme.primary,
        headline = expense.issuer?.name ?: "Sin emisor",
        supportingLines = listOf(
            expense.invoiceNumber ?: "Sin factura",
            emissionDate,
            "Concepto: $conceptLabel",
        ),
        trailingPrimary = totalFormatted,
        onClick = onClick,
    ) {
        SuggestionChip(
            modifier = Modifier.height(20.dp),
            onClick = {},
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = statusContainerColor,
                labelColor = statusTextColor,
            ),
            border = SuggestionChipDefaults.suggestionChipBorder(
                enabled = true,
                borderColor = statusContainerColor,
                disabledBorderColor = statusContainerColor,
                borderWidth = 1.dp,
            ),
            label = {
                Text(
                    text = statusLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = labelSmall(statusTextColor).copy(fontWeight = FontWeight.Bold),
                )
            },
        )
        creditDueBadge?.let { badge ->
            Text(
                text = badge.text,
                style = TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = badge.textColor,
                ),
                modifier = Modifier
                    .background(badge.containerColor, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

private fun expenseConceptIcon(expense: Expense): ImageVector =
    when (resolveExpenseConceptVisualCategory(expense)) {
        ExpenseConceptVisualCategory.INVENTORY -> Icons.Rounded.Inventory2
        ExpenseConceptVisualCategory.PEOPLE -> Icons.Rounded.People
        ExpenseConceptVisualCategory.PROFESSIONAL -> Icons.Rounded.Work
        ExpenseConceptVisualCategory.RENT -> Icons.Rounded.Apartment
        ExpenseConceptVisualCategory.WATER -> Icons.Rounded.WaterDrop
        ExpenseConceptVisualCategory.ENERGY -> Icons.Rounded.Bolt
        ExpenseConceptVisualCategory.CONNECTIVITY -> Icons.Rounded.Wifi
        ExpenseConceptVisualCategory.SECURITY -> Icons.Rounded.Security
        ExpenseConceptVisualCategory.FOOD -> Icons.Rounded.Restaurant
        ExpenseConceptVisualCategory.TRAVEL -> Icons.Rounded.Flight
        ExpenseConceptVisualCategory.OFFICE -> Icons.Rounded.Work
        ExpenseConceptVisualCategory.FUEL -> Icons.Rounded.LocalGasStation
        ExpenseConceptVisualCategory.SHIPPING -> Icons.Rounded.LocalShipping
        ExpenseConceptVisualCategory.PARKING -> Icons.Rounded.LocalParking
        ExpenseConceptVisualCategory.MARKETING -> Icons.Rounded.Campaign
        ExpenseConceptVisualCategory.TRAINING -> Icons.Rounded.School
        ExpenseConceptVisualCategory.INSURANCE -> Icons.Rounded.Shield
        ExpenseConceptVisualCategory.TECHNOLOGY -> Icons.Rounded.Cloud
        ExpenseConceptVisualCategory.LEGAL -> Icons.Rounded.Gavel
        ExpenseConceptVisualCategory.MAINTENANCE -> Icons.Rounded.Build
        ExpenseConceptVisualCategory.FINANCE -> Icons.Rounded.AccountBalance
        ExpenseConceptVisualCategory.TAX -> Icons.Rounded.Receipt
        ExpenseConceptVisualCategory.GENERAL -> Icons.Rounded.Category
        ExpenseConceptVisualCategory.FALLBACK -> Icons.Rounded.Receipt
    }

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
private fun LoadingExpensesView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(5) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(shimmerBrush()),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 2.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(shimmerBrush())
                )
            }
        }
    }
}

@Composable
private fun EmptyExpensesView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Receipt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(Res.string.expenses_empty),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CrawlJobsSummary(
    jobs: List<CrawlJob>,
    isLoading: Boolean,
    onDismissFailedJob: (Long) -> Unit,
    onRetryFailedJob: (CrawlJob) -> Unit,
    onCopyCufe: (String) -> Unit
) {
    var showFailedDetail by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.QrCodeScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Importaciones CUFE",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Text(
                        text = "Cargando...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (jobs.isEmpty()) {
                Text(
                    text = "Sin facturas pendientes de revision",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val pending = jobs.count { it.status == "pending" || it.status == "processing" }
                val succeeded = jobs.count { it.status == "success" }
                val failedJobs = jobs.filter { it.status == "failed" }
                val failed = failedJobs.size

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (pending > 0) {
                        Text(
                            text = "$pending en proceso",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF9800)
                        )
                    }
                    if (succeeded > 0) {
                        Text(
                            text = "$succeeded completados",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50)
                        )
                    }
                    if (failed > 0) {
                        Text(
                            text = if (failed == 1) "1 error" else "$failed errores",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.clickable { showFailedDetail = !showFailedDetail }
                        )
                    }
                }

                if (showFailedDetail && failedJobs.isNotEmpty()) {
                    failedJobs.forEach { job ->
                        val jobId = job.resolvedId
                        val cufe = job.cufe?.trim().orEmpty()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "No se importó la factura",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = CrawlErrorCopy.userMessage(job.errorMessage ?: job.message),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (cufe.isNotEmpty()) {
                                Text(
                                    text = CufeParser.truncate(cufe),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { onCopyCufe(cufe) }
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(
                                    text = "Intentar de nuevo",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { onRetryFailedJob(job) }
                                )
                                if (jobId != null) {
                                    Text(
                                        text = "Ocultar",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.clickable { onDismissFailedJob(jobId) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
