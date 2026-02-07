package com.teco.ventago.features.quotes.ui.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.loaders.shimmerBrush
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.bodySmall
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.features.quotes.domain.QuoteSelectionStore
import com.teco.ventago.features.quotes.domain.models.Quote
import com.teco.ventago.features.quotes.domain.models.QuoteLine
import com.teco.ventago.features.quotes.domain.models.QuoteStatus
import com.teco.ventago.utils.DateFormat
import com.teco.ventago.utils.formatNumberToMoney
import org.jetbrains.compose.resources.stringResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.download_pdf
import ventago.composeapp.generated.resources.send_by_email
import ventago.composeapp.generated.resources.cancel_quote
import ventago.composeapp.generated.resources.modify_quote
import ventago.composeapp.generated.resources.create_order
import ventago.composeapp.generated.resources.additional_info
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import ventago.composeapp.generated.resources.cancel
import ventago.composeapp.generated.resources.email
import ventago.composeapp.generated.resources.reason
import ventago.composeapp.generated.resources.cancel_reason_min
import ventago.composeapp.generated.resources.customer
import ventago.composeapp.generated.resources.invalid_email
import ventago.composeapp.generated.resources.quote_number
import ventago.composeapp.generated.resources.total
import ventago.composeapp.generated.resources.quote_status_accepted
import ventago.composeapp.generated.resources.quote_status_cancelled
import ventago.composeapp.generated.resources.quote_status_created
import ventago.composeapp.generated.resources.quote_status_draft
import ventago.composeapp.generated.resources.quote_status_rejected
import ventago.composeapp.generated.resources.items
import ventago.composeapp.generated.resources.created
import ventago.composeapp.generated.resources.quote_subtotal_label
import ventago.composeapp.generated.resources.quote_taxes_label
import ventago.composeapp.generated.resources.quote_total_label
import ventago.composeapp.generated.resources.pos_discount
import ventago.composeapp.generated.resources.phone
import ventago.composeapp.generated.resources.name
import com.teco.ventago.navigation.LocalNavController
import com.teco.ventago.navigation.PosScreens
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun QuoteDetailsScreen(
    viewModel: QuoteDetailsViewModel,
    onBack: () -> Unit,
    onModify: () -> Unit,
    onCreateOrder: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedQuote = QuoteSelectionStore.selected

    LaunchedEffect(selectedQuote?.id, selectedQuote?.quoteNumber, selectedQuote?.displayNumber) {
        if (selectedQuote != null) {
            viewModel.loadQuote(
                quoteId = selectedQuote.id,
                quoteNumber = selectedQuote.displayNumber ?: selectedQuote.quoteNumber
            )
        } else if (uiState.quote == null) {
            viewModel.loadQuote()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                QuoteDetailsSkeleton()
            }
            uiState.quote == null -> {
                Text("No quote loaded", modifier = Modifier.padding(16.dp))
            }
            else -> QuoteDetailsContent(
                quote = uiState.quote!!,
                isCancelling = uiState.isCancelling,
                errorMessage = uiState.error,
                onCancel = { reason -> viewModel.cancel(reason) },
                onModify = onModify,
                onCreateOrder = onCreateOrder
            )
        }

        if (uiState.isDownloadingPdf) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun QuoteDetailsSkeleton() {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuoteDetailsContent(
    quote: Quote,
    isCancelling: Boolean,
    errorMessage: String?,
    onCancel: (String) -> Unit,
    onModify: () -> Unit,
    onCreateOrder: () -> Unit
) {
    val statusLabel = quoteStatusLabel(quote.status)
    val (statusTextColor, statusBgColor) = quoteStatusColors(quote.status)
    val cancelReasonMinText = stringResource(Res.string.cancel_reason_min)
    val additionalInfoLabel = stringResource(Res.string.additional_info)

    var showCancelSheet by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }
    var cancelError by remember { mutableStateOf<String?>(null) }
    var pendingCancel by remember { mutableStateOf(false) }
    val cancelSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val showLimitedActions = quote.status == QuoteStatus.CANCELLED || quote.status == QuoteStatus.ACCEPTED

    val items = quote.lines.orEmpty()
    val totals = quote.totals
    val customerName = quote.finalCustomerInfo?.name ?: quote.customerName.orEmpty()
    val customerEmail = quote.finalCustomerInfo?.email ?: quote.customerEmail.orEmpty()
    val customerPhone = quote.finalCustomerInfo?.phone ?: quote.customerPhone.orEmpty()
    val customerRuc = quote.customerRuc.orEmpty()
    val hasCustomerInfo = listOf(customerName, customerEmail, customerPhone, customerRuc)
        .any { it.isNotBlank() }
    val additionalInfo = formatAdditionalInfo(quote.additionalInfo)
    val hasAdditionalInfo = additionalInfo.isNotBlank() && !isBlankHtml(quote.additionalInfo)

    LaunchedEffect(isCancelling, errorMessage) {
        if (pendingCancel && !isCancelling) {
            if (errorMessage.isNullOrBlank()) {
                showCancelSheet = false
                cancelReason = ""
                cancelError = null
            } else {
                cancelError = errorMessage
            }
            pendingCancel = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header card
        QuoteHeaderCard(
            quote = quote,
            statusLabel = statusLabel,
            statusTextColor = statusTextColor,
            statusBackgroundColor = statusBgColor,
            totals = totals
        )

        // Items card
        if (items.isNotEmpty()) {
            QuoteItemsCard(items = items)
        }

        // Customer card
        if (hasCustomerInfo) {
            QuoteCustomerCard(
                customerName = customerName,
                customerRuc = customerRuc,
                customerEmail = customerEmail,
                customerPhone = customerPhone
            )
        }

        // Additional info card
        if (hasAdditionalInfo) {
            QuoteAdditionalInfoCard(
                label = additionalInfoLabel,
                info = additionalInfo
            )
        }

        // Action buttons
        if (!showLimitedActions) {
            ButtonM(
                onClick = onCreateOrder,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Text(text = stringResource(Res.string.create_order))
            }
            OutlinedButtonM(
                onClick = onModify,
                contentColor = MaterialTheme.colorScheme.secondary,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
            ) {
                Text(text = stringResource(Res.string.modify_quote))
            }
            OutlinedButtonM(
                onClick = { showCancelSheet = true },
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.error,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Text(text = stringResource(Res.string.cancel_quote))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    if (showCancelSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCancelSheet = false },
            sheetState = cancelSheetState,
            containerColor = MaterialTheme.colorScheme.background,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Por favor proporcione una razón para cancelar esta cotización (mínimo 10 caracteres).",
                    style = bodySmall(),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                DMOutlinedTextField(
                    label = stringResource(Res.string.reason),
                    modifier = Modifier.fillMaxWidth(),
                    text = cancelReason,
                    onChange = {
                        cancelReason = it
                        cancelError = null
                    },
                    enabled = !isCancelling,
                    isError = cancelError != null
                )
                cancelError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Spacer(modifier = Modifier.height(16.dp))
                ButtonM(
                    onClick = {
                        if (cancelReason.length < 10) {
                            cancelError = cancelReasonMinText
                            return@ButtonM
                        }
                        pendingCancel = true
                        onCancel(cancelReason)
                    },
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    enabled = !isCancelling
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isCancelling) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onError
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cancelando...")
                        } else {
                            Text(stringResource(Res.string.cancel_quote))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButtonM(
                    onClick = { showCancelSheet = false },
                    contentColor = MaterialTheme.colorScheme.secondary,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                    enabled = !isCancelling
                ) {
                    Text(stringResource(Res.string.cancel))
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun QuoteHeaderCard(
    quote: Quote,
    statusLabel: String,
    statusTextColor: Color,
    statusBackgroundColor: Color,
    totals: com.teco.ventago.features.quotes.domain.models.QuoteTotals?
) {
    val createdAt = quote.createdAt?.let { DateFormat.getOrdersFormattedDate(it) }.orEmpty()

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
                        text = quote.displayNumberOrQuoteNumber,
                        style = titleMediumBold()
                    )
                }
                QuoteStatusBadge(
                    label = statusLabel,
                    textColor = statusTextColor,
                    backgroundColor = statusBackgroundColor
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            InfoRow(stringResource(Res.string.created), if (createdAt.isBlank()) "-" else createdAt)
            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(stringResource(Res.string.quote_subtotal_label), formatMoney(totals?.subtotal))
            Spacer(modifier = Modifier.height(4.dp))
            InfoRow(stringResource(Res.string.pos_discount), formatMoney(totals?.discount))
            Spacer(modifier = Modifier.height(4.dp))
            InfoRow(stringResource(Res.string.quote_taxes_label), formatMoney(totals?.taxes))
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(Res.string.quote_total_label),
                    style = bodyMediumBold()
                )
                Text(
                    text = formatMoney(totals?.total),
                    style = bodyMediumBold(color = MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun QuoteItemsCard(items: List<QuoteLine>) {
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
                Text(
                    text = stringResource(Res.string.items) + " (${items.size})",
                    style = bodyMediumBold()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            items.forEachIndexed { index, item ->
                QuoteDetailsItem(quoteLine = item)
                if (index < items.lastIndex) {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun QuoteCustomerCard(
    customerName: String,
    customerRuc: String,
    customerEmail: String,
    customerPhone: String
) {
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
                Text(
                    text = stringResource(Res.string.customer),
                    style = bodyMediumBold()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (customerName.isNotBlank()) {
                InfoRow(stringResource(Res.string.name), customerName)
                Spacer(modifier = Modifier.height(4.dp))
            }
            if (customerRuc.isNotBlank()) {
                InfoRow("RUC", customerRuc)
                Spacer(modifier = Modifier.height(4.dp))
            }
            if (customerEmail.isNotBlank()) {
                InfoRow(stringResource(Res.string.email), customerEmail)
                Spacer(modifier = Modifier.height(4.dp))
            }
            if (customerPhone.isNotBlank()) {
                InfoRow(stringResource(Res.string.phone), customerPhone)
            }
        }
    }
}

@Composable
private fun QuoteAdditionalInfoCard(label: String, info: String) {
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
                Text(text = label, style = bodyMediumBold())
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = info, style = bodySmall())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteDetailsActions(
    backStackEntry: NavBackStackEntry?,
    navigate: (PosScreens) -> Unit,
    navigateAny: (Any) -> Unit
) {
    val navController = LocalNavController.current
    val owner = remember(navController, backStackEntry) {
        runCatching { navController.getBackStackEntry(PosScreens.Quotes.name) }.getOrNull() ?: backStackEntry
    } ?: return

    val viewModel: QuoteDetailsViewModel = koinViewModel(viewModelStoreOwner = owner)
    val uiState by viewModel.uiState.collectAsState()
    val quote = uiState.quote
    val showLimitedActions = quote?.status == QuoteStatus.CANCELLED || quote?.status == QuoteStatus.ACCEPTED

    var menuExpanded by remember { mutableStateOf(false) }
    var showEmailSheet by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var pendingEmail by remember { mutableStateOf(false) }
    val emailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val invalidEmailText = stringResource(Res.string.invalid_email)

    LaunchedEffect(showEmailSheet, quote?.customerEmail, quote?.finalCustomerInfo?.email) {
        if (showEmailSheet && emailInput.isBlank()) {
            emailInput = (quote?.finalCustomerInfo?.email ?: quote?.customerEmail).orEmpty()
        }
    }

    LaunchedEffect(uiState.isSendingEmail, uiState.error) {
        if (pendingEmail && !uiState.isSendingEmail) {
            if (uiState.error.isNullOrBlank()) {
                showEmailSheet = false
                emailError = null
            } else {
                emailError = uiState.error
            }
            pendingEmail = false
        }
    }

    IconButton(
        onClick = { menuExpanded = true },
        enabled = quote != null && !uiState.isDownloadingPdf
    ) {
        if (uiState.isDownloadingPdf) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }

    DropdownMenu(
        expanded = menuExpanded,
        onDismissRequest = { menuExpanded = false }
    ) {
        if (!showLimitedActions) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.send_by_email)) },
                onClick = {
                    menuExpanded = false
                    emailError = null
                    showEmailSheet = true
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Email, contentDescription = null)
                }
            )
        }
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.download_pdf)) },
            onClick = {
                menuExpanded = false
                viewModel.downloadPdf()
            },
            enabled = !uiState.isDownloadingPdf,
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.FileDownload, contentDescription = null)
            }
        )
    }

    if (showEmailSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                if (!uiState.isSendingEmail) {
                    showEmailSheet = false
                }
            },
            sheetState = emailSheetState,
            containerColor = MaterialTheme.colorScheme.background,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(Res.string.send_by_email),
                    style = bodyMediumBold()
                )
                Spacer(modifier = Modifier.height(12.dp))
                DMOutlinedTextField(
                    label = stringResource(Res.string.email),
                    modifier = Modifier.fillMaxWidth(),
                    text = emailInput,
                    onChange = {
                        emailInput = it
                        emailError = null
                    },
                    enabled = !uiState.isSendingEmail,
                    isError = emailError != null
                )
                emailError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Spacer(modifier = Modifier.height(16.dp))
                ButtonM(
                    onClick = {
                        if (emailInput.isBlank() || !emailInput.contains("@")) {
                            emailError = invalidEmailText
                            return@ButtonM
                        }
                        pendingEmail = true
                        viewModel.sendEmail(emailInput)
                    },
                    enabled = !uiState.isSendingEmail
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.isSendingEmail) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enviando...")
                        } else {
                            Text(stringResource(Res.string.send_by_email))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButtonM(
                    onClick = { showEmailSheet = false },
                    contentColor = MaterialTheme.colorScheme.secondary,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                    enabled = !uiState.isSendingEmail
                ) {
                    Text(stringResource(Res.string.cancel))
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
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
    QuoteStatus.CANCELLED -> Color(0xFF5C6B73) to Color(0xFFE0E0E0)
    else -> MaterialTheme.colorScheme.onSurfaceVariant to MaterialTheme.colorScheme.surfaceVariant
}

@Composable
private fun QuoteStatusBadge(
    label: String,
    textColor: Color,
    backgroundColor: Color
) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight(600),
                color = textColor
            )
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
private fun QuoteDetailsItem(quoteLine: QuoteLine) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                text = quoteLine.itemName.orEmpty(),
                style = bodyMedium()
            )
            Text(
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                text = formatMoney(quoteLine.unitPrice),
                style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            text = "${formatQuantity(quoteLine.quantity)}x",
            style = bodyMediumBold()
        )
    }
}

private fun formatMoney(value: Double?): String {
    return formatNumberToMoney((value ?: 0.0).toString())
}

private fun formatAdditionalInfo(value: String?): String {
    val raw = value.orEmpty().trim()
    if (raw.isBlank()) return ""
    return raw
        .replace("<br>", "\n", ignoreCase = true)
        .replace("<br/>", "\n", ignoreCase = true)
        .replace("<br />", "\n", ignoreCase = true)
        .replace("</p>", "\n", ignoreCase = true)
        .replace(Regex("<[^>]*>"), "")
        .replace("&nbsp;", " ")
        .replace("\n\n", "\n")
        .trim()
}

private fun normalizeHtml(value: String?): String {
    val raw = value.orEmpty().trim()
    if (raw.isBlank()) return ""
    return raw.replace("\\s".toRegex(), "")
}

private fun isBlankHtml(value: String?): Boolean {
    val normalized = normalizeHtml(value)
    return normalized.isEmpty() ||
        normalized == "<p></p>" ||
        normalized == "<p><br></p>" ||
        normalized == "<p><br/></p>"
}

private fun formatQuantity(value: Double?): String {
    val safe = value ?: 0.0
    return if (safe % 1.0 == 0.0) {
        safe.toInt().toString()
    } else {
        safe.toString()
    }
}
