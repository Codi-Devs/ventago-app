package com.teco.ventago.features.quotes.ui.list

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.features.quotes.domain.models.Quote
import com.teco.ventago.features.quotes.domain.models.QuoteStatus
import com.teco.ventago.features.quotes.domain.QuoteSelectionStore
import com.teco.ventago.navigation.PosScreens
import org.jetbrains.compose.resources.stringResource
import ventago.composeapp.generated.resources.Res
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
import ventago.composeapp.generated.resources.see_more
import ventago.composeapp.generated.resources.status

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun QuotesListScreen(
    viewModel: QuotesListViewModel,
    navigate: (PosScreens) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.refreshing,
        onRefresh = { viewModel.loadQuotes(refresh = true) }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        Text(
            text = stringResource(Res.string.quotes),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )

        FilterSection(
            customerName = uiState.customerName,
            customerRuc = uiState.customerRuc,
            quoteNumber = uiState.quoteNumber,
            status = uiState.status,
            onCustomerName = { viewModel.setCustomerName(it) },
            onCustomerRuc = { viewModel.setCustomerRuc(it) },
            onQuoteNumber = { viewModel.setQuoteNumber(it) },
            onStatus = { viewModel.setStatus(it) },
            onApply = { viewModel.applyFilters() },
            onClear = { viewModel.clearFilters() }
        )

        if (uiState.isLoading && uiState.quotes.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) { CircularProgressIndicator() }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.quotes) { quote ->
                    QuoteListItem(quote) {
                        QuoteSelectionStore.selected = quote
                        navigate(PosScreens.QuoteDetailsScreen)
                    }
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        when {
                            uiState.noMore -> {}
                            uiState.isLoading -> CircularProgressIndicator()
                            else -> TextButtonS(label = stringResource(Res.string.see_more)) { viewModel.loadQuotes() }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        PullRefreshIndicator(
            refreshing = uiState.refreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun FilterSection(
    customerName: String,
    customerRuc: String,
    quoteNumber: String,
    status: Int?,
    onCustomerName: (String) -> Unit,
    onCustomerRuc: (String) -> Unit,
    onQuoteNumber: (String) -> Unit,
    onStatus: (Int?) -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit
) {
    val statusOptions = listOf(
        null to stringResource(Res.string.filter_all),
        QuoteStatus.DRAFT to stringResource(Res.string.quote_status_draft),
        QuoteStatus.CREATED to stringResource(Res.string.quote_status_created),
        QuoteStatus.ACCEPTED to stringResource(Res.string.quote_status_accepted),
        QuoteStatus.REJECTED to stringResource(Res.string.quote_status_rejected),
        QuoteStatus.CANCELLED to stringResource(Res.string.quote_status_cancelled)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(text = stringResource(Res.string.filters), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        DMOutlinedTextField(
            label = stringResource(Res.string.customer_name_label),
            modifier = Modifier.fillMaxWidth(),
            text = customerName,
            onChange = onCustomerName
        )
        Spacer(Modifier.height(8.dp))
        DMOutlinedTextField(
            label = stringResource(Res.string.customer_ruc),
            modifier = Modifier.fillMaxWidth(),
            text = customerRuc,
            onChange = onCustomerRuc
        )
        Spacer(Modifier.height(8.dp))
        DMOutlinedTextField(
            label = stringResource(Res.string.quote_number),
            modifier = Modifier.fillMaxWidth(),
            text = quoteNumber,
            onChange = onQuoteNumber
        )
        Spacer(Modifier.height(12.dp))
        Text(text = stringResource(Res.string.status), style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            statusOptions.forEach { (value, label) ->
                FilterChip(
                    selected = status == value,
                    onClick = { onStatus(value) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors()
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onClear,
                modifier = Modifier.weight(1f)
            ) { Text(text = stringResource(Res.string.clear_filters)) }
            Button(
                onClick = onApply,
                modifier = Modifier.weight(1f)
            ) { Text(text = stringResource(Res.string.apply_filters)) }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun QuoteListItem(quote: Quote, onClick: () -> Unit) {
    val statusLabel = quoteStatusLabel(quote.status)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text = quote.quoteNumber.orEmpty(), style = MaterialTheme.typography.titleMedium)
        Text(text = quote.customerName.orEmpty(), style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "${stringResource(Res.string.status)}: $statusLabel",
            style = MaterialTheme.typography.bodySmall
        )
        Text(text = "Total: ${quote.totals?.total ?: 0.0}", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(4.dp))
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Ver detalle")
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
