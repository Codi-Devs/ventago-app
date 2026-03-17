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
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.SmallFloatingActionButton
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.loaders.shimmerBrush
import com.teco.ventago.design_system.molecules.InstallmentDueDateFieldKmp
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.features.expenses.domain.models.CrawlJob
import com.teco.ventago.features.expenses.domain.models.Expense
import com.teco.ventago.features.expenses.domain.models.ExpenseMerchant
import com.teco.ventago.features.expenses.domain.models.PaymentMethod
import com.teco.ventago.navigation.PosScreens
import com.teco.ventago.utils.DateFormat.getFormattedDate
import com.teco.ventago.utils.formatNumberToMoney
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
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

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExpensesListScreen(
    viewModel: ExpensesListViewModel,
    navigate: (PosScreens) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilters by remember { mutableStateOf(false) }
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.refreshing,
        onRefresh = { viewModel.loadExpenses(refresh = true) }
    )

    val sourceOptions = listOf(
        null to stringResource(Res.string.expenses_source_all),
        "manual" to stringResource(Res.string.expenses_source_manual),
        "crawled" to stringResource(Res.string.expenses_source_crawled)
    )

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
        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DMOutlinedTextField(
                text = uiState.searchQuery,
                label = stringResource(Res.string.expenses_search_placeholder),
                modifier = Modifier.weight(1f),
                onChange = { viewModel.setSearchQuery(it) },
                leadingIcon = Icons.Rounded.Search,
                trailingIcon = Icons.Rounded.Tune,
                trailingIconClick = { showFilters = true }
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { viewModel.search() }) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

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
                isLoading = uiState.isLoadingCrawlJobs
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                if (uiState.hasExpensesQr) {
                    SmallFloatingActionButton(
                        onClick = { navigate(PosScreens.CufeImportScreen) },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QrCodeScanner,
                            contentDescription = "Importar CUFE",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                if (uiState.canCreateExpense) {
                    FloatingActionButton(
                        onClick = { navigate(PosScreens.NewExpenseScreen) },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Nuevo gasto",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }

    // Filters bottom sheet
    if (showFilters) {
        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
            sheetState = filterSheetState
        ) {
            ExpensesFilterSheet(
                startDate = uiState.startDate ?: "",
                onStartDateChange = { viewModel.setStartDate(it.ifBlank { null }) },
                endDate = uiState.endDate ?: "",
                onEndDateChange = { viewModel.setEndDate(it.ifBlank { null }) },
                issuerName = uiState.issuerName,
                onIssuerNameChange = { viewModel.setIssuerName(it) },
                merchantSuggestions = uiState.merchantSuggestions,
                onMerchantSelected = { viewModel.selectFilterMerchant(it) },
                onDismissMerchantSuggestions = { viewModel.dismissFilterMerchantSuggestions() },
                issuerRuc = uiState.issuerRuc,
                onIssuerRucChange = { viewModel.setIssuerRuc(it) },
                invoiceNumber = uiState.invoiceNumber,
                onInvoiceNumberChange = { viewModel.setInvoiceNumber(it) },
                onApply = {
                    showFilters = false
                    viewModel.applyFilters()
                },
                onClear = {
                    showFilters = false
                    viewModel.clearFilters()
                }
            )
        }
    }
}

@Composable
private fun ExpensesFilterSheet(
    startDate: String,
    onStartDateChange: (String) -> Unit,
    endDate: String,
    onEndDateChange: (String) -> Unit,
    issuerName: String,
    onIssuerNameChange: (String) -> Unit,
    merchantSuggestions: List<ExpenseMerchant>,
    onMerchantSelected: (ExpenseMerchant) -> Unit,
    onDismissMerchantSuggestions: () -> Unit,
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

        Column {
            DMOutlinedTextField(
                text = issuerName,
                label = stringResource(Res.string.expenses_issuer_name),
                modifier = Modifier,
                onChange = onIssuerNameChange
            )
            if (merchantSuggestions.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        merchantSuggestions.forEachIndexed { index, merchant ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onMerchantSelected(merchant) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Store,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = merchant.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!merchant.ruc.isNullOrBlank()) {
                                        Text(
                                            text = "RUC: ${merchant.ruc}${if (!merchant.dv.isNullOrBlank()) "-${merchant.dv}" else ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            if (index < merchantSuggestions.lastIndex) {
                                Divider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }

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
    val emissionDate = expense.emissionDate?.let {
        runCatching {
            getFormattedDate(it, "yyyy-MM-dd'T'HH:mm:ss", "dd/MM/yyyy")
        }.getOrDefault(it.take(10))
    } ?: ""

    val totalFormatted = formatNumberToMoney("${expense.totalAmount ?: 0.0}")
    val statusColor = when (expense.paymentStatus) {
        "paid" -> Color(0xFF4CAF50)
        "partial" -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    val statusLabel = when (expense.paymentStatus) {
        "paid" -> "Pagado"
        "partial" -> "Parcial"
        "not_paid" -> "No pagado"
        else -> expense.paymentStatus ?: ""
    }
    val creditDueBadge = remember(expense) { buildCreditDueBadge(expense) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Receipt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.issuer?.name ?: "Sin emisor",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = expense.invoiceNumber ?: "Sin factura",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = emissionDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Concepto: $conceptLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = totalFormatted,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = statusLabel,
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    ),
                    modifier = Modifier
                        .background(statusColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                creditDueBadge?.let { badge ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = badge.text,
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = badge.textColor
                        ),
                        modifier = Modifier
                            .background(badge.containerColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
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
                    .clip(RoundedCornerShape(12.dp))
                    .background(shimmerBrush()),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 1.dp
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
    isLoading: Boolean
) {
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
                val failed = jobs.count { it.status == "failed" }

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
                            text = "$failed fallidos",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFF44336)
                        )
                    }
                }
            }
        }
    }
}
