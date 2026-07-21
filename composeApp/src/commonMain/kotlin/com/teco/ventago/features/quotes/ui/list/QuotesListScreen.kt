package com.teco.ventago.features.quotes.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teco.ventago.core.LocalStorage
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.loaders.shimmerBrush
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.textfields.helpers.DMDropDownField
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.features.branches.domain.model.Branch
import com.teco.ventago.features.quotes.domain.QuotesOnboarding
import com.teco.ventago.features.quotes.domain.models.Quote
import com.teco.ventago.features.quotes.domain.models.QuoteStatus
import com.teco.ventago.features.quotes.domain.QuoteSelectionStore
import com.teco.ventago.features.quotes.domain.models.compactDisplayNumber
import com.teco.ventago.navigation.PosScreens
import com.teco.ventago.navigation.LocalNavController
import com.teco.ventago.utils.DateFormat.getFormattedDate
import com.teco.ventago.utils.formatNumberToMoney
import androidx.navigation.NavBackStackEntry
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.action_continue
import ventago.composeapp.generated.resources.apply_filters
import ventago.composeapp.generated.resources.clear_filters
import ventago.composeapp.generated.resources.customer_name_label
import ventago.composeapp.generated.resources.customer_ruc
import ventago.composeapp.generated.resources.filter_all
import ventago.composeapp.generated.resources.filters
import ventago.composeapp.generated.resources.quote_number
import ventago.composeapp.generated.resources.quote_status_accepted
import ventago.composeapp.generated.resources.quote_status_cancelled
import ventago.composeapp.generated.resources.quote_status_created
import ventago.composeapp.generated.resources.quote_status_draft
import ventago.composeapp.generated.resources.quote_status_rejected
import ventago.composeapp.generated.resources.quotes
import ventago.composeapp.generated.resources.quotes_welcome_message
import ventago.composeapp.generated.resources.quotes_welcome_message_small
import ventago.composeapp.generated.resources.quotes_welcome_title
import ventago.composeapp.generated.resources.see_more
import ventago.composeapp.generated.resources.status
import ventago.composeapp.generated.resources.items

private const val QUOTE_SEARCH_START_YEAR = 2023
private const val QUOTE_SEARCH_DEFAULT_YEAR = 2026

@Composable
fun QuotesListScreenActions(backStackEntry: NavBackStackEntry?) {
    val navController = LocalNavController.current
    val quotesOwner = remember(navController) {
        navController.getBackStackEntry(PosScreens.Quotes.name)
    }
    val viewModel: QuotesListViewModel = koinViewModel(viewModelStoreOwner = quotesOwner)
    val uiState by viewModel.uiState.collectAsState()

    IconButton(onClick = { viewModel.showQuoteSearchSheet(true) }) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = "Buscar cotización",
            tint = MaterialTheme.colorScheme.primary
        )
    }

    Box {
        IconButton(onClick = { viewModel.showFiltersSheet(true) }) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = stringResource(Res.string.filters),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        val activeFilterCount = viewModel.activeFilterCount()
        if (activeFilterCount > 0) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = activeFilterCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QuotesListScreen(
    viewModel: QuotesListViewModel,
    navigate: (PosScreens) -> Unit,
    onBack: () -> Unit
) {
    val storage: LocalStorage = koinInject()
    val uiState by viewModel.uiState.collectAsState()
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val quoteSearchSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showWelcomeSheet by remember { mutableStateOf(false) }
    val welcomeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val hasShownWelcome = storage.bool(QuotesOnboarding.KEY_WELCOME_SHEET_SHOWN) == true

    LaunchedEffect(Unit) {
        storage.set(QuotesOnboarding.KEY_HAS_ENTERED_QUOTES, true)
        if (!hasShownWelcome) {
            showWelcomeSheet = true
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.refreshing,
        onRefresh = { viewModel.loadQuotes(refresh = true) }
    )

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            if (uiState.isLoading && uiState.quotes.isEmpty()) {
                LoadingQuotesView()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val dataGroup = uiState.quotes.groupBy { formatQuoteDate(it.createdAt) }
                    dataGroup.forEach { (date, quotes) ->
                        if (date.isNotBlank()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 4.dp, top = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Text(
                                        text = date,
                                        style = TextStyle(
                                            fontSize = 12.sp,
                                            lineHeight = 20.sp,
                                            fontWeight = FontWeight(400),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            letterSpacing = 0.08.sp,
                                        )
                                    )
                                }
                            }
                        }

                        itemsIndexed(
                            items = quotes,
                            key = { index, quote -> quoteLazyKey(quote, index) }
                        ) { _, quote ->
                            QuoteListItem(quote) {
                                QuoteSelectionStore.selected = quote
                                navigate(PosScreens.QuoteDetailsScreen)
                            }
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
                                else -> TextButtonS(label = stringResource(Res.string.see_more)) { viewModel.loadQuotes() }
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
        }
    }

    if (showWelcomeSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                storage.set(QuotesOnboarding.KEY_WELCOME_SHEET_SHOWN, true)
                showWelcomeSheet = false
            },
            sheetState = welcomeSheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.Redeem,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.quotes_welcome_title),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.quotes_welcome_message),
                    style = bodyMedium(),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.quotes_welcome_message_small),
                    style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                ButtonM(
                    onClick = {
                        storage.set(QuotesOnboarding.KEY_WELCOME_SHEET_SHOWN, true)
                        showWelcomeSheet = false
                    },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ) {
                    Text(
                        stringResource(Res.string.action_continue),
                        style = bodyMediumBold(color = MaterialTheme.colorScheme.onSecondary)
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (uiState.showFiltersSheet) {
        QuotesFilterSheet(
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { viewModel.showFiltersSheet(false) },
            sheetState = filterSheetState
        )
    }

    if (uiState.showQuoteSearchSheet) {
        QuoteNumberSearchSheet(
            branches = uiState.branches,
            quotePrefix = uiState.quotePrefix,
            onDismiss = { viewModel.showQuoteSearchSheet(false) },
            onSearch = { quoteNumber ->
                viewModel.setQuoteNumber(quoteNumber)
                viewModel.showQuoteSearchSheet(false)
                viewModel.applyFilters()
            },
            sheetState = quoteSearchSheetState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuotesFilterSheet(
    uiState: QuotesListState,
    viewModel: QuotesListViewModel,
    onDismiss: () -> Unit,
    sheetState: SheetState,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = stringResource(Res.string.filters), style = MaterialTheme.typography.titleMedium)

            QuoteStatusFilterChips(
                selectedStatus = uiState.status,
                onStatusSelected = viewModel::setStatus
            )

            DMOutlinedTextField(
                label = stringResource(Res.string.customer_name_label),
                modifier = Modifier.fillMaxWidth(),
                text = uiState.customerName,
                onChange = { viewModel.setCustomerName(it) }
            )
            DMOutlinedTextField(
                label = stringResource(Res.string.customer_ruc),
                modifier = Modifier.fillMaxWidth(),
                text = uiState.customerRuc,
                onChange = { viewModel.setCustomerRuc(it) }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButtonM(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.clearFilters()
                        onDismiss()
                    }
                ) {
                    Text(text = stringResource(Res.string.clear_filters))
                }
                ButtonM(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.applyFilters()
                        onDismiss()
                    },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ) {
                    Text(text = stringResource(Res.string.apply_filters))
                }
            }
        }
    }
}

@Composable
private fun QuoteStatusFilterChips(
    selectedStatus: Int?,
    onStatusSelected: (Int?) -> Unit,
) {
    val statusOptions = listOf(
        null to stringResource(Res.string.filter_all),
        QuoteStatus.DRAFT to stringResource(Res.string.quote_status_draft),
        QuoteStatus.CREATED to stringResource(Res.string.quote_status_created),
        QuoteStatus.ACCEPTED to stringResource(Res.string.quote_status_accepted),
        QuoteStatus.REJECTED to stringResource(Res.string.quote_status_rejected),
        QuoteStatus.CANCELLED to stringResource(Res.string.quote_status_cancelled)
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.status),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            statusOptions.forEach { (value, label) ->
                FilterChip(
                    selected = selectedStatus == value,
                    onClick = { onStatusSelected(value) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuoteNumberSearchSheet(
    branches: List<Branch>,
    quotePrefix: String,
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit,
    sheetState: SheetState,
) {
    val selectableBranches = branches.filter { it.branchCode.isNotBlank() }
    var selectedBranchIndex by remember(selectableBranches) {
        val mainBranchIndex = selectableBranches.indexOfFirst { it.branchCode == "0000" }
        mutableStateOf(if (mainBranchIndex >= 0) mainBranchIndex else 0)
    }
    val years = remember { (QUOTE_SEARCH_START_YEAR..QUOTE_SEARCH_DEFAULT_YEAR).toList() }
    var selectedYearIndex by remember(years) {
        val defaultYearIndex = years.indexOf(QUOTE_SEARCH_DEFAULT_YEAR)
            .takeIf { it >= 0 } ?: years.lastIndex.coerceAtLeast(0)
        mutableStateOf(defaultYearIndex)
    }
    var quoteSequenceInput by remember { mutableStateOf("") }

    val selectedBranch = selectableBranches.getOrNull(selectedBranchIndex)
    val selectedYear = years.getOrNull(selectedYearIndex) ?: QUOTE_SEARCH_DEFAULT_YEAR
    val sequenceDigits = quoteSequenceInput.filter(Char::isDigit)
    val fullQuoteNumber = buildInternalQuoteNumber(
        prefix = quotePrefix,
        branchCode = selectedBranch?.branchCode.orEmpty(),
        year = selectedYear,
        sequenceDigits = sequenceDigits
    )
    val canSearch = selectedBranch != null && sequenceDigits.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Buscar cotización", style = MaterialTheme.typography.titleMedium)

            if (selectableBranches.size > 1) {
                DMDropDownField(
                    label = "Sucursal",
                    items = selectableBranches,
                    selectedIndex = selectedBranchIndex,
                    onItemSelected = { index, _ -> selectedBranchIndex = index },
                    selectedItemToString = ::branchSearchLabel
                )
            }

            DMDropDownField(
                label = "Año",
                items = years,
                selectedIndex = selectedYearIndex,
                onItemSelected = { index, _ -> selectedYearIndex = index },
                selectedItemToString = { it.toString() }
            )

            DMOutlinedTextField(
                text = quoteSequenceInput,
                label = stringResource(Res.string.quote_number),
                onChange = { quoteSequenceInput = it.filter(Char::isDigit) },
                modifier = Modifier.fillMaxWidth(),
                keyboardType = KeyboardType.Number
            )

            if (fullQuoteNumber.isNotBlank()) {
                Text(
                    text = fullQuoteNumber,
                    style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButtonM(
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss
                ) {
                    Text("Cancelar")
                }
                ButtonM(
                    modifier = Modifier.weight(1f),
                    enabled = canSearch,
                    onClick = { onSearch(fullQuoteNumber) },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ) {
                    Text("Buscar")
                }
            }
        }
    }
}

private fun buildInternalQuoteNumber(
    prefix: String,
    branchCode: String,
    year: Int,
    sequenceDigits: String
): String {
    if (branchCode.isBlank() || sequenceDigits.isBlank()) return ""
    val normalizedPrefix = prefix.trim().ifBlank { "COT" }
    return "$normalizedPrefix-$branchCode-$year-${sequenceDigits.padStart(6, '0')}"
}

private fun branchSearchLabel(branch: Branch): String {
    return branch.name.takeIf { it.isNotBlank() }?.let { "${branch.branchCode} - $it" }
        ?: branch.branchCode
}

private fun quoteLazyKey(quote: Quote, index: Int): String {
    return when {
        quote.id != null -> "quote-id-${quote.id}"
        !quote.quoteNumber.isNullOrBlank() -> "quote-number-${quote.quoteNumber}"
        !quote.displayNumber.isNullOrBlank() -> "quote-display-${quote.displayNumber}"
        else -> "quote-fallback-${quote.createdAt ?: "unknown"}-$index-${quote.hashCode()}"
    }
}

@Composable
private fun QuoteListItem(quote: Quote, onClick: () -> Unit) {
    val totalItems = quote.lines?.sumOf { it.quantity ?: 0.0 } ?: 0.0
    val statusLabel = quoteStatusLabel(quote.status)
    val (statusText, statusBg) = quoteStatusColors(quote.status)
    val totalAmount = formatNumberToMoney((quote.totals?.total ?: 0.0).toString())
    val businessName = quoteBusinessDisplayName(quote)

    Row(
        modifier = Modifier
            .height(IntrinsicSize.Max)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Surface(
            modifier = Modifier.size(50.dp),
            shape = RoundedCornerShape(10),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                contentDescription = "Quote",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(10.dp)
                    .size(30.dp)
            )
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .weight(1f, fill = true)
                .fillMaxHeight()
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                text = quote.compactDisplayNumber(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                ),
            )
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = businessName.ifBlank { "${totalItems.toInt()} ${stringResource(Res.string.items)}" },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
            )
        }

        Column(
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            SuggestionChip(
                modifier = Modifier.height(23.dp),
                onClick = {},
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = statusBg,
                    labelColor = statusText,
                ),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    enabled = true,
                    borderColor = statusBg,
                    disabledBorderColor = statusBg,
                    borderWidth = 1.dp
                ),
                label = {
                    Text(
                        text = statusLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
            )
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = totalAmount,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

private fun quoteBusinessDisplayName(quote: Quote): String {
    return quote.finalCustomerInfo?.name?.takeIf { it.isNotBlank() }
        ?: quote.customerName?.takeIf { it.isNotBlank() }
        ?: ""
}

@Composable
private fun quoteStatusLabel(status: Int?): String = when (status) {
    QuoteStatus.DRAFT -> stringResource(Res.string.quote_status_draft)
    QuoteStatus.CREATED -> stringResource(Res.string.quote_status_created)
    QuoteStatus.ACCEPTED -> stringResource(Res.string.quote_status_accepted)
    QuoteStatus.REJECTED -> stringResource(Res.string.quote_status_rejected)
    QuoteStatus.CANCELLED -> stringResource(Res.string.quote_status_cancelled)
    else -> QuoteStatus.label(status)
}

@Composable
private fun quoteStatusColors(status: Int?): Pair<Color, Color> = when (status) {
    QuoteStatus.DRAFT -> Color(0xFF6C757D) to Color(0xFFE9ECEF)
    QuoteStatus.CREATED -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    QuoteStatus.ACCEPTED -> Color(0xFF2E7D32) to Color(0xFFE8F5E9)
    QuoteStatus.REJECTED -> Color(0xFFD32F2F) to Color(0xFFFFEBEE)
    QuoteStatus.CANCELLED -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.errorContainer
    else -> MaterialTheme.colorScheme.onSurfaceVariant to MaterialTheme.colorScheme.surfaceVariant
}

@Composable
private fun LoadingQuotesView() {
    val brush = shimmerBrush()
    LazyColumn(modifier = Modifier.padding(16.dp)) {
        repeat(5) {
            item {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(top = 8.dp)
                        .clip(shape = RoundedCornerShape(6.dp))
                        .background(brush = brush)
                )
            }
        }
    }
}

private fun formatQuoteDate(date: String?): String {
    if (date.isNullOrBlank()) return ""
    val sanitized = date.trim()
        .removeSuffix("Z")
        .split(".")
        .firstOrNull()
        ?: date
    return getFormattedDate(sanitized, "yyyy-MM-dd'T'HH:mm:ss", "dd MMMM yyyy")
}
