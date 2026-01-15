package com.teco.ventago.features.quotes.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.teco.ventago.features.quotes.domain.models.Quote
import com.teco.ventago.features.quotes.domain.models.QuoteStatus
import org.jetbrains.compose.resources.stringResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.download_pdf
import ventago.composeapp.generated.resources.send_by_email
import ventago.composeapp.generated.resources.cancel_quote
import ventago.composeapp.generated.resources.modify_quote
import ventago.composeapp.generated.resources.quote_details
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import ventago.composeapp.generated.resources.cancel
import ventago.composeapp.generated.resources.email
import ventago.composeapp.generated.resources.reason
import ventago.composeapp.generated.resources.cancel_reason_min
import ventago.composeapp.generated.resources.customer
import ventago.composeapp.generated.resources.invalid_email
import ventago.composeapp.generated.resources.quote_number
import ventago.composeapp.generated.resources.status
import ventago.composeapp.generated.resources.total
import ventago.composeapp.generated.resources.quote_number
import ventago.composeapp.generated.resources.quote_status_accepted
import ventago.composeapp.generated.resources.quote_status_cancelled
import ventago.composeapp.generated.resources.quote_status_created
import ventago.composeapp.generated.resources.quote_status_draft
import ventago.composeapp.generated.resources.quote_status_rejected
import ventago.composeapp.generated.resources.status
import ventago.composeapp.generated.resources.total

@Composable
fun QuoteDetailsScreen(
    viewModel: QuoteDetailsViewModel,
    onBack: () -> Unit,
    onModify: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (uiState.quote == null) {
            viewModel.loadQuote()
        }
    }

    when {
        uiState.isLoading -> {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                CircularProgressIndicator()
            }
        }
        uiState.quote == null -> {
            Text("No quote loaded", modifier = Modifier.padding(16.dp))
        }
        else -> QuoteDetailsContent(
            quote = uiState.quote!!,
            onDownload = { viewModel.downloadPdf() },
            onSendEmail = { email -> viewModel.sendEmail(email) },
            onCancel = { reason -> viewModel.cancel(reason) },
            onModify = onModify
        )
    }
}

@Composable
private fun QuoteDetailsContent(
    quote: Quote,
    onDownload: () -> Unit,
    onSendEmail: (String) -> Unit,
    onCancel: (String) -> Unit,
    onModify: () -> Unit
) {
    val statusLabel = quoteStatusLabel(quote.status)
    val invalidEmailText = stringResource(Res.string.invalid_email)
    val cancelReasonMinText = stringResource(Res.string.cancel_reason_min)
    var showEmailDialog by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf(quote.customerEmail.orEmpty()) }
    var emailError by remember { mutableStateOf<String?>(null) }

    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }
    var cancelError by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = stringResource(Res.string.quote_details), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(text = "${stringResource(Res.string.quote_number)}: ${quote.quoteNumber.orEmpty()}")
        Text(text = "${stringResource(Res.string.customer)}: ${quote.customerName.orEmpty()}")
        Text(text = "${stringResource(Res.string.status)}: $statusLabel")
        Text(text = "${stringResource(Res.string.total)}: ${quote.totals?.total ?: 0.0}")

        Spacer(Modifier.height(16.dp))
        Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(Res.string.download_pdf))
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { showEmailDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(Res.string.send_by_email))
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { showCancelDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(Res.string.cancel_quote))
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onModify, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(Res.string.modify_quote))
        }
    }

    if (showEmailDialog) {
        AlertDialog(
            onDismissRequest = { showEmailDialog = false },
            title = { Text(stringResource(Res.string.send_by_email)) },
            text = {
                Column {
                    DMOutlinedTextField(
                        label = stringResource(Res.string.email),
                        modifier = Modifier.fillMaxWidth(),
                        text = emailInput,
                        onChange = {
                            emailInput = it
                            emailError = null
                        },
                        isError = emailError != null
                    )
                    emailError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (emailInput.isBlank() || !emailInput.contains("@")) {
                        emailError = invalidEmailText
                        return@Button
                    }
                    onSendEmail(emailInput)
                    showEmailDialog = false
                }) {
                    Text(stringResource(Res.string.send_by_email))
                }
            },
            dismissButton = {
                Button(onClick = { showEmailDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text(stringResource(Res.string.cancel_quote)) },
            text = {
                Column {
                    DMOutlinedTextField(
                        label = stringResource(Res.string.reason),
                        modifier = Modifier.fillMaxWidth(),
                        text = cancelReason,
                        onChange = {
                            cancelReason = it
                            cancelError = null
                        },
                        isError = cancelError != null
                    )
                    cancelError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (cancelReason.length < 10) {
                        cancelError = cancelReasonMinText
                        return@Button
                    }
                    onCancel(cancelReason)
                    showCancelDialog = false
                }) {
                    Text(stringResource(Res.string.cancel_quote))
                }
            },
            dismissButton = {
                Button(onClick = { showCancelDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
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
