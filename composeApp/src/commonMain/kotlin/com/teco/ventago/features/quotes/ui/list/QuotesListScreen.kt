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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
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
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.loaders.shimmerBrush
import com.teco.ventago.design_system.molecules.DMTopAppBar
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.features.quotes.domain.models.Quote
import com.teco.ventago.features.quotes.domain.models.QuoteStatus
import com.teco.ventago.features.quotes.domain.QuoteSelectionStore
import com.teco.ventago.navigation.PosScreens
import com.teco.ventago.utils.DateFormat.getFormattedDate
import com.teco.ventago.utils.formatNumberToMoney
import org.jetbrains.compose.resources.stringResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.apply_filters
import ventago.composeapp.generated.resources.clear_filters
import ventago.composeapp.generated.resources.customer_name_label
import ventago.composeapp.generated.resources.customer_ruc
import ventago.composeapp.generated.resources.filter_all
import ventago.composeapp.generated.resources.quote_number
import ventago.composeapp.generated.resources.quote_status_accepted
import ventago.composeapp.generated.resources.quote_status_cancelled
import ventago.composeapp.generated.resources.quote_status_created
import ventago.composeapp.generated.resources.quote_status_draft
import ventago.composeapp.generated.resources.quote_status_rejected
import ventago.composeapp.generated.resources.quotes
import ventago.composeapp.generated.resources.see_more
import ventago.composeapp.generated.resources.status
import ventago.composeapp.generated.resources.items

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QuotesListScreen(
    viewModel: QuotesListViewModel,
    navigate: (PosScreens) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilters by remember { mutableStateOf(false) }
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.refreshing,
        onRefresh = { viewModel.loadQuotes(refresh = true) }
    )
    val statusOptions = listOf(
        null to stringResource(Res.string.filter_all),
        QuoteStatus.DRAFT to stringResource(Res.string.quote_status_draft),
        QuoteStatus.CREATED to stringResource(Res.string.quote_status_created),
        QuoteStatus.ACCEPTED to stringResource(Res.string.quote_status_accepted),
        QuoteStatus.REJECTED to stringResource(Res.string.quote_status_rejected),
        QuoteStatus.CANCELLED to stringResource(Res.string.quote_status_cancelled)
    )

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(Res.string.status),
                style = MaterialTheme.typography.titleSmall
            )
            IconButton(onClick = { showFilters = true }) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = "Filters"
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            statusOptions.forEach { (value, label) ->
                FilterChip(
                    selected = uiState.status == value,
                    onClick = {
                        viewModel.setStatus(value)
                        viewModel.applyFilters()
                    },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors()
                )
            }
        }

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

                        items(quotes, key = { it.id ?: it.hashCode() }) { quote ->
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

    if (showFilters) {
        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
            sheetState = filterSheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = stringResource(Res.string.apply_filters), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                DMOutlinedTextField(
                    label = stringResource(Res.string.customer_name_label),
                    modifier = Modifier.fillMaxWidth(),
                    text = uiState.customerName,
                    onChange = { viewModel.setCustomerName(it) }
                )
                Spacer(Modifier.height(8.dp))
                DMOutlinedTextField(
                    label = stringResource(Res.string.customer_ruc),
                    modifier = Modifier.fillMaxWidth(),
                    text = uiState.customerRuc,
                    onChange = { viewModel.setCustomerRuc(it) }
                )
                Spacer(Modifier.height(8.dp))
                DMOutlinedTextField(
                    label = stringResource(Res.string.quote_number),
                    modifier = Modifier.fillMaxWidth(),
                    text = uiState.quoteNumber,
                    onChange = { viewModel.setQuoteNumber(it) }
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButtonM(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.clearFilters()
                            showFilters = false
                        }
                    ) {
                        Text(text = stringResource(Res.string.clear_filters))
                    }
                    ButtonM(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.applyFilters()
                            showFilters = false
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
}

@Composable
private fun QuoteListItem(quote: Quote, onClick: () -> Unit) {
    val totalItems = quote.lines?.sumOf { it.quantity ?: 0.0 } ?: 0.0
    val statusLabel = quoteStatusLabel(quote.status)
    val (statusText, statusBg) = quoteStatusColors(quote.status)
    val totalAmount = formatNumberToMoney((quote.totals?.total ?: 0.0).toString())

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
                text = quote.displayNumberOrQuoteNumber,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                ),
            )
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = "${totalItems.toInt()} ${stringResource(Res.string.items)}",
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
