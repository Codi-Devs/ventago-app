package com.teco.ventago.features.orders.ui.order_details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Whatsapp
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
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
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavOptionsBuilder
import coil3.compose.AsyncImage
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.buttons.TextButtonM
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.molecules.orders.OrderStatusChip
import com.teco.ventago.design_system.loaders.shimmerBrush
import com.teco.ventago.design_system.molecules.InstallmentDueDateFieldKmp
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.textfields.DMMoneyOutlinedTextField
import com.teco.ventago.design_system.textfields.DMOutlinedTextField
import com.teco.ventago.design_system.theme.AcceptedContainer
import com.teco.ventago.design_system.theme.AcceptedLabel
import com.teco.ventago.design_system.theme.CancelledContainer
import com.teco.ventago.design_system.theme.CancelledLabel
import com.teco.ventago.design_system.theme.CompletedContainer
import com.teco.ventago.design_system.theme.CompletedLabel
import com.teco.ventago.design_system.theme.NewContainer
import com.teco.ventago.design_system.theme.NewLabel
import com.teco.ventago.design_system.theme.ProcessingContainer
import com.teco.ventago.design_system.theme.ProcessingLabel
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.bodySmall
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.labelLarge
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.titleLarge
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.design_system.theme.vanishedBackgroundColor
import com.teco.ventago.features.invoicing.domain.models.FEDocumentType
import com.teco.ventago.features.invoicing.domain.models.InvoiceStatus
import com.teco.ventago.features.orders.domain.PaymentLinkResolver
import com.teco.ventago.features.orders.domain.models.ManualPaymentMethodOption
import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.orders.domain.models.OrderPaymentDto
import com.teco.ventago.features.orders.domain.models.OrderStatus
import com.teco.ventago.features.orders.domain.models.PaymentStatus
import com.teco.ventago.features.orders.domain.models.ReceivableTermDto
import com.teco.ventago.features.orders.domain.models.RelatedDocument
import com.teco.ventago.features.orders.ui.order_details.viewModel.OrderDetailsState
import com.teco.ventago.features.orders.ui.order_details.viewModel.OrderDetailsUiEvent
import com.teco.ventago.features.orders.ui.order_details.viewModel.OrdersDetailsViewModel
import com.teco.ventago.features.orders.ui.order_details.viewModel.OrderCxcValidators
import com.teco.ventago.features.orders.ui.order_details.viewModel.MIN_CANCEL_ORDER_REASON_LENGTH
import com.teco.ventago.features.orders.ui.order_details.viewModel.RegisterPaymentMode
import com.teco.ventago.features.orders.ui.order_details.viewModel.RegisterPaymentState
import com.teco.ventago.features.orders.ui.order_details.viewModel.RescheduleState
import com.teco.ventago.features.orders.ui.order_details.viewModel.VoidPaymentState
import com.teco.ventago.features.quotes.ui.preview.PdfPreview
import com.teco.ventago.navigation.AchPaymentDetailsRoute
import com.teco.ventago.navigation.OrdersScreenRoute
import com.teco.ventago.navigation.PosNoteRoute
import com.teco.ventago.navigation.PosScreens
import com.teco.ventago.utils.DateFormat
import com.teco.ventago.utils.copyToClipboard
import com.teco.ventago.utils.formatNumberToMoney
import com.teco.ventago.utils.generateQR
import com.teco.ventago.utils.openCustomTab
import com.teco.ventago.utils.openDialer
import com.teco.ventago.utils.openMapUrl
import com.teco.ventago.utils.openSms
import com.teco.ventago.utils.openWhatsappMessage
import com.teco.ventago.utils.shareLink
import com.teco.ventago.utils.toLongCents
import com.teco.ventago.utils.toQuantityUiString
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.change_order_status
import ventago.composeapp.generated.resources.customer_note
import ventago.composeapp.generated.resources.delivery
import ventago.composeapp.generated.resources.description
import ventago.composeapp.generated.resources.email
import ventago.composeapp.generated.resources.enter_transfer_description
import ventago.composeapp.generated.resources.enter_transfer_reference
import ventago.composeapp.generated.resources.generate_invoice
import ventago.composeapp.generated.resources.invoice_i_client
import ventago.composeapp.generated.resources.invoice_i_name
import ventago.composeapp.generated.resources.items
import ventago.composeapp.generated.resources.manual_payment_info
import ventago.composeapp.generated.resources.manual_transference
import ventago.composeapp.generated.resources.mark_paid
import ventago.composeapp.generated.resources.order_call_customer
import ventago.composeapp.generated.resources.paid
import ventago.composeapp.generated.resources.payment_link
import ventago.composeapp.generated.resources.payment_mode
import ventago.composeapp.generated.resources.payment_register
import ventago.composeapp.generated.resources.payment_status
import ventago.composeapp.generated.resources.paypal
import ventago.composeapp.generated.resources.phone
import ventago.composeapp.generated.resources.pick_up_desc
import ventago.composeapp.generated.resources.pos_card
import ventago.composeapp.generated.resources.pos_cash
import ventago.composeapp.generated.resources.pos_mixed
import ventago.composeapp.generated.resources.pos_other
import ventago.composeapp.generated.resources.record_transfer
import ventago.composeapp.generated.resources.reference
import ventago.composeapp.generated.resources.share_by_message
import ventago.composeapp.generated.resources.share_by_whatsapp
import ventago.composeapp.generated.resources.share_payment_link
import ventago.composeapp.generated.resources.total
import ventago.composeapp.generated.resources.unpaid
import ventago.composeapp.generated.resources.yappy
import kotlin.time.Duration.Companion.days
import kotlin.math.abs


@Composable
fun OrderDetailsActions(backStackEntry: NavBackStackEntry?,  navigateAny: (Any) -> Unit) {
    val viewModel: OrdersDetailsViewModel = backStackEntry?.let {
        koinViewModel(viewModelStoreOwner = it)
    } ?: koinViewModel()

    val uiState by viewModel.uiState.collectAsState()
    val order = uiState.order

    val phoneNumber = uiState.order?.displayCustomerPhone()

    IconButton(onClick = {
        copyToClipboard("Order Details", viewModel.getOrderDetailsMessage())
    }) {
        Icon(
            imageVector = Icons.Rounded.ContentCopy,
            contentDescription = "",
            tint = MaterialTheme.colorScheme.primary
        )
    }

    IconButton(onClick = {
        phoneNumber?.let {
            viewModel.showShareSheet(true)
        } ?: run {
            shareLink(viewModel.getOrderDetailsMessage())
        }

    }) {
        Icon(
            imageVector = Icons.Rounded.Share,
            contentDescription = "",
            tint = MaterialTheme.colorScheme.primary
        )
    }


    if (order?.invoiceStatus == InvoiceStatus.ISSUED.id) {
        val documentType = order.orderType.let { FEDocumentType.fromCode(it) }
        val isCreditOrDebitNote = documentType in listOf(
            FEDocumentType.CREDIT_NOTE_REFERENCING_FE,
            FEDocumentType.DEBIT_NOTE_REFERENCING_FE,
            FEDocumentType.GENERIC_CREDIT_NOTE,
            FEDocumentType.GENERIC_DEBIT_NOTE
        )

        if (!isCreditOrDebitNote) {
            var menuExpanded by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Más opciones",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    val remainingCreditNoteCents = order.remainingCreditNoteCapacityCents()
                    val canCreateCreditNote = remainingCreditNoteCents > 0L
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (canCreateCreditNote) {
                                    "Generar nota de crédito"
                                } else {
                                    "Límite de notas de crédito alcanzado"
                                }
                            )
                        },
                        enabled = canCreateCreditNote,
                        onClick = {
                            menuExpanded = false
                            val cufe = order.externalInvoiceNumber ?: return@DropdownMenuItem

                            navigateAny(
                                PosNoteRoute(
                                    op = FEDocumentType.CREDIT_NOTE_REFERENCING_FE.code,
                                    cufe = cufe,
                                    createdAt = order.createdAt,
                                    customerId = order.customer?.id,
                                    customerName = order.displayCustomerName(),
                                    customerEmail = order.customer?.email,
                                    customerphone = order.displayCustomerPhone(),
                                    customerRuc = order.customer?.ruc,
                                    customerStatus = order.customer?.status ?: 1,
                                    customerInvoiceID = order.customer?.customerInvoiceID,
                                    orderLinesJson = Json.encodeToString(order.lines),
                                    maxCreditNoteAmountCents = remainingCreditNoteCents,
                                    sourceOrderNumber = order.internalNumber
                                )
                            )
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Generar nota de débito") },
                        onClick = {
                            menuExpanded = false
                            val cufe = order.externalInvoiceNumber ?: return@DropdownMenuItem
                            navigateAny(
                                PosNoteRoute(
                                    op = FEDocumentType.DEBIT_NOTE_REFERENCING_FE.code,
                                    cufe = cufe,
                                    createdAt = order.createdAt,
                                    customerId = order.customer?.id,
                                    customerName = order.displayCustomerName(),
                                    customerEmail = order.customer?.email,
                                    customerphone = order.displayCustomerPhone(),
                                    customerRuc = order.customer?.ruc,
                                    customerStatus = order.customer?.status ?: 1,
                                    customerInvoiceID = order.customer?.customerInvoiceID,
                                    orderLinesJson = Json.encodeToString(order.lines)
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsScreen(
    viewModel: OrdersDetailsViewModel,
    navigate: (Any, (NavOptionsBuilder.() -> Unit)?) -> Unit
) {

    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteReason by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val linkSheetState = rememberModalBottomSheetState()
    val shareSheetState = rememberModalBottomSheetState()
    val registerPaymentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val rescheduleSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val voidPaymentSheetState = rememberModalBottomSheetState()
    val cancelOrderSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is OrderDetailsUiEvent.OrderCancelled -> {
                    showCancelDialog = false
                    cancelReason = ""
                }
                is OrderDetailsUiEvent.OrderDeleted -> {
                    navigate(PosScreens.OrdersScreen) {
                        popUpTo(PosScreens.OrdersScreen.name) { inclusive = false }
                        launchSingleTop = true
                    }
                }
                is OrderDetailsUiEvent.OpenExternalUrl -> {
                    openCustomTab(event.url)
                }

                else -> Unit
            }
        }
    }

    val phoneNumber = uiState.order?.displayCustomerPhone()

    val order = uiState.order ?: run {
        OrderDetailsLoadingSkeleton()
        if (uiState.loadingBottomSheet.isLoading()) {
            LoadingSheet(
                state = uiState.loadingBottomSheet,
                sheetState = loadingSheetState
            ) {
                viewModel.hideLoading()
            }
        }
        return
    }
    LaunchedEffect(order.id) {
        viewModel.hydrateSelectedOrderDetailsIfNeeded()
    }

    val canShowCancelButton = order.invoiceStatus != InvoiceStatus.ISSUED.id &&
            viewModel.isOrderCancellable(order.status)
    val canShowDeleteButton = canDeleteOrder(order)
    val canShowReprintButton = viewModel.canShowReprintAction(order)
    val pendingCents = viewModel.totalOpenReceivableCents(order)
    val resolvedPaymentLink = PaymentLinkResolver.resolveCurrent(order)
    val canGeneratePaymentLink = viewModel.canGeneratePaymentLink(order)
    val canCopyOrSharePaymentLink = viewModel.canCopyOrSharePaymentLink(order)
    val hasActiveYappyOnsiteIntent = (
        order.paymentFlowType.equals("yappy_onsite", ignoreCase = true) ||
            order.paymentFlowType.equals("in_place", ignoreCase = true)
        ) &&
        order.paymentStatus != PaymentStatus.PAID.id &&
        order.paymentStatus != PaymentStatus.CANCELLED.id
    val canManageReceivables = order.supportsReceivableActions()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header card
        OrderHeaderCard(order = order, viewModel = viewModel)

        // Items card
        OrderItemsCard(order = order)

        // Receivables card
        if (order.status != OrderStatus.CANCELLED && canManageReceivables) {
            OrderReceivablesCard(
                order = order,
                pendingCents = pendingCents,
                overdueCents = viewModel.totalOverdueReceivableCents(order),
                terms = viewModel.nonCancelledReceivableTerms(order),
                onReschedule = { viewModel.openRescheduleSheet() }
            )
        }

        // Registered payments must be visible even for cancelled orders.
        RegisteredPaymentsCard(
            order = order,
            viewModel = viewModel,
            onVoidPayment = { paymentId -> viewModel.openVoidPaymentSheet(paymentId) },
            onNavigateAchReview = { paymentIntentId ->
                navigate(AchPaymentDetailsRoute(paymentUid = paymentIntentId), null)
            }
        )

        // Invoicing card
        if (order.invoiceStatus != 0 && order.externalInvoiceNumber != null) {
            OrderInvoicingCard(
                order = order,
                onShowCancelDialog = {
                    viewModel.clearCancelOrderError()
                    showCancelDialog = true
                }
            )
        }

        OrderRelatedDocumentsCard(
            relatedDocuments = order.relatedDocuments,
            onOpenOrder = { orderNumber ->
                navigate(OrdersScreenRoute(orderNumber = orderNumber), null)
            }
        )

        // Customer card
        order.displayCustomerSnapshot()?.let { customer ->
            OrderCustomerCard(customer = customer, phoneNumber = phoneNumber)
        }

        // Action buttons
        if (order.status != OrderStatus.CANCELLED) {
            when (order.invoiceStatus) {
                InvoiceStatus.ISSUED.id -> {
                    ButtonM(
                        onClick = { viewModel.getDocumentByCufe() },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ) {
                        OrderActionButtonContent(
                            icon = Icons.Rounded.Visibility,
                            label = "Ver factura PDF"
                        )
                    }
                }

                InvoiceStatus.FAILED.id -> {
                    // No retry button and no cancel-order action for failed invoice orders.
                }

                InvoiceStatus.NONE.id, InvoiceStatus.PENDING.id -> {
                    if (viewModel.canInvoiceDraftOrder(order)) {
                        ButtonM(
                            onClick = { viewModel.showManualPaymentSheet(true) },
                            containerColor = Color(0xFF2E7D32),
                            contentColor = Color.White
                        ) {
                            OrderActionButtonContent(
                                icon = Icons.Rounded.Description,
                                label = "Facturar"
                            )
                        }
                    }
                }

                InvoiceStatus.CANCELLED.id -> {
                    // Intentionally no actions
                }
            }

            if (viewModel.canShowRetryInvoiceButton(order)) {
                ButtonM(onClick = { viewModel.retryElectronicInvoice() }) {
                    OrderActionButtonContent(
                        icon = Icons.Rounded.Receipt,
                        label = "Reintentar facturación"
                    )
                }
            }

            val hasIssuedAdditionalActions = order.invoiceStatus == InvoiceStatus.ISSUED.id &&
                    ((canShowReprintButton || uiState.reprintInFlight) ||
                            (canManageReceivables && uiState.canMarkPaid && viewModel.totalOpenReceivableCents(order) > 0L))
            val hasDraftAdditionalActions = (order.invoiceStatus == InvoiceStatus.NONE.id ||
                    order.invoiceStatus == InvoiceStatus.PENDING.id) && canShowCancelButton
            val hasPaymentLinkActions = canManageReceivables &&
                    uiState.canCreatePaymentLink &&
                    (canGeneratePaymentLink || canCopyOrSharePaymentLink || hasActiveYappyOnsiteIntent)
            val hasAdditionalActions = hasIssuedAdditionalActions ||
                    hasDraftAdditionalActions ||
                    hasPaymentLinkActions ||
                    canShowDeleteButton

            if (hasAdditionalActions) {
                OrderActionsDivider()
            }

            if (order.invoiceStatus == InvoiceStatus.ISSUED.id) {
                if (canShowReprintButton || uiState.reprintInFlight) {
                    OrderOutlinedActionButton(
                        label = if (uiState.reprintInFlight) "Reimprimiendo..." else "Reimprimir ticket",
                        icon = Icons.Rounded.Receipt,
                        color = MaterialTheme.colorScheme.primary,
                        enabled = !uiState.reprintInFlight,
                        onClick = { viewModel.reprintTicket() }
                    )
                }
                if (canManageReceivables && uiState.canMarkPaid && viewModel.totalOpenReceivableCents(order) > 0L) {
                    OrderOutlinedActionButton(
                        label = "Registrar pago",
                        icon = Icons.Rounded.Payment,
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = { viewModel.openRegisterPaymentSheet() }
                    )
                }
            }

            if ((order.invoiceStatus == InvoiceStatus.NONE.id ||
                        order.invoiceStatus == InvoiceStatus.PENDING.id) && canShowCancelButton
            ) {
                OrderOutlinedActionButton(
                    label = "Anular pedido",
                    icon = Icons.Rounded.Close,
                    color = MaterialTheme.colorScheme.error,
                    onClick = {
                        viewModel.clearCancelOrderError()
                        showCancelDialog = true
                    }
                )
            }

            if (canManageReceivables && uiState.canCreatePaymentLink) {
                if (canGeneratePaymentLink) {
                    OrderOutlinedActionButton(
                        label = "Generar Link de Pago",
                        icon = Icons.Rounded.Link,
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = { viewModel.openGeneratePaymentLinkSheet() }
                    )
                }
                if (hasActiveYappyOnsiteIntent && !canCopyOrSharePaymentLink) {
                    OrderOutlinedActionButton(
                        label = "Cambiar a pago manual",
                        icon = Icons.Rounded.Payment,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { viewModel.showManualPaymentSheet(true) }
                    )
                    OrderOutlinedActionButton(
                        label = "Crear link de pago",
                        icon = Icons.Rounded.Link,
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = { viewModel.createReplacementPaymentLink() }
                    )
                }
                if (canCopyOrSharePaymentLink) {
                    val linkUrl = resolvedPaymentLink?.url.orEmpty()
                    OrderOutlinedActionButton(
                        label = "Cambiar a pago manual",
                        icon = Icons.Rounded.Payment,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { viewModel.showManualPaymentSheet(true) }
                    )
                    OrderOutlinedActionButton(
                        label = "Crear nuevo link de pago",
                        icon = Icons.Rounded.Link,
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = { viewModel.createReplacementPaymentLink() }
                    )
                    OrderOutlinedActionButton(
                        label = "Cambiar a Yappy en caja",
                        icon = Icons.Rounded.Payment,
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = { viewModel.createReplacementYappyOnsite() }
                    )
                    OrderOutlinedActionButton(
                        label = "Copiar Link de Pago",
                        icon = Icons.Rounded.ContentCopy,
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = {
                            if (linkUrl.isNotBlank()) {
                                viewModel.trackOrderPaymentLinkAction("copy")
                                copyToClipboard("Link de pago", linkUrl)
                            }
                        }
                    )
                    OrderOutlinedActionButton(
                        label = stringResource(Res.string.share_payment_link),
                        icon = Icons.Rounded.Share,
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = {
                            if (linkUrl.isNotBlank()) {
                                viewModel.trackOrderPaymentLinkAction("share")
                                shareLink(linkUrl)
                            }
                        }
                    )
                }
            }

            if (canShowDeleteButton) {
                OrderOutlinedActionButton(
                    label = "Eliminar pedido",
                    icon = Icons.Rounded.Delete,
                    color = MaterialTheme.colorScheme.error,
                    onClick = {
                        deleteReason = ""
                        showDeleteDialog = true
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- Bottom sheets and dialogs (unchanged) ---

        if (!uiState.paymentLink.isNullOrBlank()) {
            ModalBottomSheet(
                containerColor = MaterialTheme.colorScheme.background,
                onDismissRequest = {
                    viewModel.resetPaymentLink()
                },
                sheetState = linkSheetState,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(Res.string.payment_link),
                        modifier = Modifier.padding(bottom = 16.dp),
                        style = titleLarge()
                    )

                    uiState.paymentLink?.let { paymentLink ->
                        val bitmap = remember(paymentLink) {
                            generateQR(500, 500, paymentLink)
                        }

                        bitmap.toImageBitmap()?.let {
                            Image(
                                painter = BitmapPainter(it),
                                contentDescription = "QR Code for Table",
                                modifier = Modifier.fillMaxWidth().height(300.dp)
                            )
                        }

                        TextButtonM(
                            label = stringResource(Res.string.share_payment_link),
                            icon = Icons.Filled.Share
                        ) {
                            shareLink(paymentLink)
                        }
                    }
                }
            }
        }

        uiState.replacementYappyOnsite?.let { onsite ->
            val payload = uiState.replacementYappyOnsitePayload
            val transaction = payload?.transaction
            val invoice = payload?.invoice
            val status = transaction?.status?.ifBlank { onsite.status } ?: onsite.status.ifBlank { "created" }
            val invoiceStatus = invoice?.status?.takeIf { it != 0 } ?: payload?.order?.invoiceStatus ?: 0
            val terminal = status.lowercase() in setOf("cancelled", "canceled", "expired", "returned") ||
                invoiceStatus == 2 ||
                invoiceStatus == 3

            LaunchedEffect(onsite.transactionId) {
                viewModel.startReplacementYappyOnsitePolling(onsite.transactionId)
            }

            ModalBottomSheet(
                containerColor = MaterialTheme.colorScheme.background,
                onDismissRequest = viewModel::dismissReplacementYappyOnsite,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Yappy en caja", style = titleMediumBold())
                    Text(
                        "Muestra este QR al cliente para completar el pago.",
                        style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        textAlign = TextAlign.Center,
                    )
                    val qrHash = transaction?.qrHash?.ifBlank { onsite.qrHash } ?: onsite.qrHash
                    val bitmap = remember(qrHash) {
                        generateQR(500, 500, qrHash)
                    }
                    bitmap.toImageBitmap()?.let {
                        Image(
                            painter = BitmapPainter(it),
                            contentDescription = "QR Yappy en caja",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        )
                    }
                    InfoRow("Transacción", transaction?.transactionId?.ifBlank { onsite.transactionId } ?: onsite.transactionId.ifBlank { "-" })
                    InfoRow("Monto", "${transaction?.currency?.ifBlank { onsite.currency } ?: onsite.currency.ifBlank { "USD" }} ${transaction?.amount?.ifBlank { onsite.amount } ?: onsite.amount}")
                    InfoRow("Estado del pago", status)
                    InfoRow("Factura", replacementInvoiceLabel(invoiceStatus))
                    InfoRow("Factura POS", payload?.order?.orderNumber?.ifBlank { uiState.order?.internalNumber.orEmpty() } ?: uiState.order?.internalNumber.orEmpty().ifBlank { "-" })
                    if (uiState.replacementYappyOnsitePolling) {
                        Text(
                            "Actualizando estado...",
                            style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        )
                    }
                    if (!terminal) {
                        OutlinedButtonM(onClick = viewModel::requestCancelReplacementYappyOnsite) {
                            Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Cancelar cobro")
                        }
                    }
                    ButtonM(onClick = viewModel::dismissReplacementYappyOnsite) {
                        Text("Cerrar")
                    }
                }
            }
        }

        if (uiState.showReplacementYappyCancelDialog) {
            AlertDialog(
                onDismissRequest = viewModel::dismissCancelReplacementYappyOnsiteDialog,
                title = { Text("Cancelar cobro") },
                text = { Text("El QR quedará inactivo y el cliente ya no podrá completar este pago.") },
                confirmButton = {
                    TextButton(onClick = viewModel::cancelReplacementYappyOnsite) {
                        Text("Cancelar cobro")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissCancelReplacementYappyOnsiteDialog) {
                        Text("Mantener QR")
                    }
                },
            )
        }

        if (uiState.generatePaymentLinkState.showSheet) {
            val generationState = uiState.generatePaymentLinkState
            val expiryPresets = listOf(
                60 to "1 hora",
                720 to "12 horas",
                1440 to "24 horas",
                4320 to "3 días",
            )
            ModalBottomSheet(
                containerColor = MaterialTheme.colorScheme.background,
                onDismissRequest = { viewModel.closeGeneratePaymentLinkSheet() },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Generar Link de Pago", style = titleMediumBold())
                    Text(
                        "Saldo pendiente por cobrar: ${formatDollarFromCents(pendingCents)}",
                        style = bodyMediumBold()
                    )

                    DMOutlinedTextField(
                        text = generationState.amountInput,
                        label = "Monto a cobrar (opcional)",
                        onChange = { viewModel.updateGeneratePaymentAmountInput(it) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardType = KeyboardType.Decimal
                    )

                    Text("Expiración del link", style = bodyMediumBold())
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        expiryPresets.forEach { (minutes, label) ->
                            FilterChip(
                                selected = !generationState.useCustomExpiry && generationState.selectedExpiryPresetMinutes == minutes,
                                onClick = { viewModel.selectGenerateExpiryPreset(minutes) },
                                label = { Text(label) }
                            )
                        }
                        FilterChip(
                            selected = generationState.useCustomExpiry,
                            onClick = { viewModel.setGenerateCustomExpiryEnabled(true) },
                            label = { Text("Personalizado") }
                        )
                    }

                    if (generationState.useCustomExpiry) {
                        DMOutlinedTextField(
                            text = generationState.customExpiryMinutesInput,
                            label = "Minutos personalizados",
                            onChange = { viewModel.updateGenerateCustomExpiryInput(it) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardType = KeyboardType.Number
                        )
                    }

                    generationState.errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = bodySmall())
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButtonM(
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.closeGeneratePaymentLinkSheet() }
                        ) {
                            Text("Cancelar")
                        }
                        ButtonM(
                            modifier = Modifier.weight(1f),
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                            onClick = { viewModel.generatePaymentLink() }
                        ) {
                            Text("Generar")
                        }
                    }
                }
            }
        }

        if (uiState.invoiceRetryState.showSuccessDialog) {
            val invoiceOrder = uiState.order
            val invoiceTotal = invoiceOrder?.totalAmount?.let { formatNumberToMoney(it) }.orEmpty()
            val invoiceCufe = invoiceOrder?.externalInvoiceNumber.orEmpty()
            AlertDialog(
                onDismissRequest = { viewModel.dismissRetryInvoiceDialogs() },
                title = { Text("Factura emitida") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Receipt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("La facturación del pedido fue completada exitosamente.")
                        }
                        if (invoiceOrder != null) {
                            Divider()
                            if (invoiceTotal.isNotBlank()) {
                                InfoRow("Total facturado", invoiceTotal)
                            }
                            InfoRow(
                                "CUFE",
                                value = invoiceCufe.ifBlank { "No disponible" },
                                maxLines = 2
                            )
                        }
                    }
                },
                confirmButton = {
                    ButtonM(onClick = {
                        viewModel.dismissRetryInvoiceDialogs()
                        viewModel.getDocumentByCufe()
                    }) {
                        Text("Descargar factura")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        val shareMessage = viewModel.buildInvoiceShareMessage().ifBlank {
                            "Factura emitida para el pedido #${invoiceOrder?.internalNumber.orEmpty()}"
                        }
                        if (shareMessage.isNotBlank()) {
                            shareLink(shareMessage)
                        }
                        viewModel.dismissRetryInvoiceDialogs()
                    }) {
                        Text("Compartir factura")
                    }
                }
            )
        }

        if (uiState.invoiceRetryState.showWarningDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissRetryInvoiceDialogs() },
                title = { Text("Facturación en proceso") },
                text = {
                    Text(uiState.invoiceRetryState.warningMessage.orEmpty())
                },
                confirmButton = {
                    ButtonM(onClick = { viewModel.dismissRetryInvoiceDialogs() }) {
                        Text("Entendido")
                    }
                }
            )
        }

        if (uiState.achApproveDialog.show) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissAchApproveDialog() },
                title = { Text("Aprobar pago ACH") },
                text = {
                    Text(
                        if (uiState.achApproveDialog.highRisk) {
                            "Este pago ACH tiene indicadores de riesgo alto. ¿Deseas aprobarlo de todas formas?"
                        } else {
                            "¿Confirmas que deseas aprobar este pago ACH?"
                        }
                    )
                },
                confirmButton = {
                    ButtonM(onClick = { viewModel.confirmApproveAchPayment() }) {
                        Text("Aprobar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissAchApproveDialog() }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (uiState.achRejectDialog.show) {
            val rejectDialog = uiState.achRejectDialog
            val reasonOptions = remember { viewModel.achRejectReasonOptions() }
            AlertDialog(
                onDismissRequest = { viewModel.dismissAchRejectDialog() },
                title = { Text("Rechazar pago ACH") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Selecciona la razón del rechazo:")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            reasonOptions.forEach { (code, label) ->
                                FilterChip(
                                    selected = rejectDialog.reasonCode == code,
                                    onClick = { viewModel.updateAchRejectReasonCode(code) },
                                    label = { Text(label) }
                                )
                            }
                        }
                        if (rejectDialog.reasonCode == "other") {
                            OutlinedTextField(
                                value = rejectDialog.customReasonText,
                                onValueChange = { viewModel.updateAchRejectCustomReasonText(it) },
                                label = { Text("Motivo personalizado") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3
                            )
                        }
                        rejectDialog.errorMessage?.let {
                            Text(
                                text = it,
                                style = bodySmall(),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                confirmButton = {
                    ButtonM(onClick = { viewModel.confirmRejectAchPayment() }) {
                        Text("Rechazar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissAchRejectDialog() }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (uiState.achProofPreviewState.show) {
            val preview = uiState.achProofPreviewState
            val previewDetail = preview.paymentIntentId?.let { viewModel.achDetailState(it).detail }
            val canDownloadProof = viewModel.canDownloadAchProof(previewDetail)
            val proofSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                containerColor = MaterialTheme.colorScheme.background,
                onDismissRequest = { viewModel.closeAchProofPreview() },
                sheetState = proofSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Comprobante ACH", style = titleMediumBold())

                    when {
                        preview.isLoading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(340.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(shimmerBrush())
                            )
                        }
                        !preview.imageDataUri.isNullOrBlank() -> {
                            AsyncImage(
                                model = preview.imageDataUri,
                                contentDescription = "Comprobante ACH",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 220.dp, max = 460.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                        !preview.previewUrl.isNullOrBlank() &&
                            (preview.contentType.orEmpty().contains("pdf", ignoreCase = true) ||
                                preview.previewUrl.endsWith(".pdf", ignoreCase = true)) -> {
                            PdfPreview(
                                url = preview.previewUrl,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(420.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                        !preview.previewUrl.isNullOrBlank() -> {
                            AsyncImage(
                                model = preview.previewUrl,
                                contentDescription = "Comprobante ACH",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 220.dp, max = 460.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                        else -> {
                            Text(
                                text = preview.errorMessage ?: "No hay vista previa disponible para este comprobante.",
                                style = bodySmall(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!preview.previewUrl.isNullOrBlank() &&
                            preview.previewUrl.startsWith("http", ignoreCase = true)
                        ) {
                            OutlinedButtonM(
                                modifier = Modifier.weight(1f),
                                onClick = { openCustomTab(preview.previewUrl) }
                            ) {
                                Text("Abrir enlace")
                            }
                        }
                        if (canDownloadProof) {
                            preview.paymentIntentId?.let { paymentIntentId ->
                                ButtonM(
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.openAchProofDocumentForDownload(paymentIntentId) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Descargar")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (uiState.showAchScoreInfoDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.setAchScoreInfoDialog(false) },
                title = { Text("¿Qué significa el score ACH?") },
                text = {
                    Text(
                        "El score ACH combina señales del comprobante, el banco y los datos detectados para sugerir una revisión. " +
                            "Un score más alto implica más riesgo potencial y requiere una validación más estricta."
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.setAchScoreInfoDialog(false) }) {
                        Text("Entendido")
                    }
                }
            )
        }

        if (uiState.showShareSheet) {
            ModalBottomSheet(
                containerColor = MaterialTheme.colorScheme.background,
                onDismissRequest = {
                    viewModel.showShareSheet(false)
                },
                sheetState = shareSheetState,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextButtonS(
                        label = stringResource(Res.string.share_by_whatsapp),
                        prefixIcon = rememberVectorPainter(
                            Icons.Filled.Whatsapp
                        )
                    ) {
                        openWhatsappMessage(phoneNumber, viewModel.getOrderDetailsMessage())
                    }

                    phoneNumber?.let {
                        TextButtonS(
                            label = stringResource(Res.string.share_by_message),
                            prefixIcon = rememberVectorPainter(
                                Icons.AutoMirrored.Filled.Message
                            )
                        ) {
                            openSms(it, viewModel.getOrderDetailsMessage())
                        }
                    }

                }

            }
        }

        if (uiState.showPrinterSelectionSheet) {
            ModalBottomSheet(
                containerColor = MaterialTheme.colorScheme.background,
                onDismissRequest = { viewModel.dismissPrinterSelectionSheet() },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Selecciona una impresora",
                        style = titleMediumBold()
                    )
                    uiState.printerSelectionOptions.forEachIndexed { index, option ->
                        OutlinedButtonM(
                            onClick = { viewModel.printSelectedPrinter(index) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(option.displayLabel)
                        }
                    }
                }
            }
        }

        if (uiState.manualPayment.showSheet) {
            OrderScreenManualPaymentBottomSheetHost(
                uiState = uiState,
                order = order,
                showSheet = uiState.manualPayment.showSheet,
                onDismissSheet = {
                    viewModel.resetManualPaymentFields()
                    viewModel.showManualPaymentSheet(false)
                },
                methodOptions = ManualPaymentMethodOption.getAllOptionsPairs(),
                charged = uiState.manualPayment.charged,
                otherPaymentDescription = uiState.manualPayment.otherPaymentDescription,
                onToggleMethod = { code, selected ->
                    viewModel.onToggleMethod(code, selected)
                },
                onAmountChange = { code, amount ->
                    viewModel.onAmountChange(code, amount)
                },
                onOtherDesc = { desc ->
                    viewModel.onOtherDescription(desc)
                },
                onConfirmManualPayment = {
                    viewModel.onConfirmManualPayment()
                }
            )
        }

        if (showCancelDialog) {
            ModalBottomSheet(
                containerColor = MaterialTheme.colorScheme.background,
                onDismissRequest = {
                    showCancelDialog = false
                    viewModel.clearCancelOrderError()
                },
                sheetState = cancelOrderSheetState
            ) {
                CancelOrderBottomSheet(
                    reason = cancelReason,
                    apiErrorMessage = uiState.cancelOrderErrorMessage,
                    onReasonChange = {
                        cancelReason = it
                        viewModel.clearCancelOrderError()
                    },
                    onDismiss = {
                        showCancelDialog = false
                        viewModel.clearCancelOrderError()
                    },
                    onConfirm = {
                        viewModel.cancelOrder(cancelReason.trim())
                    }
                )
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Eliminar pedido") },
                text = {
                    Column {
                        Text("Ingresa el motivo de eliminación del pedido.")
                        Spacer(Modifier.height(8.dp))
                        DMOutlinedTextField(
                            text = deleteReason,
                            label = "Motivo",
                            onChange = { deleteReason = it },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                    }
                },
                confirmButton = {
                    ButtonM(
                        onClick = {
                            showDeleteDialog = false
                            viewModel.deleteOrder(deleteReason.trim())
                        },
                        enabled = deleteReason.isNotBlank(),
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ) { Text("Eliminar") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
                }
            )
        }

        if (uiState.registerPaymentState.showSheet && canManageReceivables) {
            ModalBottomSheet(
                containerColor = MaterialTheme.colorScheme.background,
                onDismissRequest = {
                    viewModel.closeRegisterPaymentSheet()
                },
                sheetState = registerPaymentSheetState,
            ) {
                RegisterPaymentsBottomSheet(
                    state = uiState.registerPaymentState,
                    receivableTerms = viewModel.openReceivableTerms(order),
                    openBalanceCents = viewModel.totalOpenReceivableCents(order),
                    onDismiss = { viewModel.closeRegisterPaymentSheet() },
                    onModeSelected = { viewModel.setRegisterPaymentMode(it) },
                    onAddPayment = { viewModel.addRegisterPaymentRow() },
                    onRemovePayment = { viewModel.removeRegisterPaymentRow(it) },
                    onPaymentMethodChanged = { rowId, methodCode ->
                        viewModel.updateRegisterPaymentMethod(rowId, methodCode)
                    },
                    onPaymentAmountChanged = { rowId, amount ->
                        viewModel.updateRegisterPaymentAmount(rowId, amount)
                    },
                    onPaymentDateChanged = { rowId, dateIso ->
                        viewModel.updateRegisterPaymentDate(rowId, dateIso)
                    },
                    onAddApplication = { rowId -> viewModel.addRegisterPaymentApplication(rowId) },
                    onRemoveApplication = { rowId, appId ->
                        viewModel.removeRegisterPaymentApplication(rowId, appId)
                    },
                    onApplicationTermChanged = { rowId, appId, termId ->
                        viewModel.updateRegisterPaymentApplicationTerm(rowId, appId, termId)
                    },
                    onApplicationAmountChanged = { rowId, appId, amount ->
                        viewModel.updateRegisterPaymentApplicationAmount(rowId, appId, amount)
                    },
                    onSubmit = { viewModel.submitRegisterPayments() }
                )
            }
        }

        if (uiState.rescheduleState.showSheet && canManageReceivables) {
            ModalBottomSheet(
                containerColor = MaterialTheme.colorScheme.background,
                onDismissRequest = { viewModel.closeRescheduleSheet() },
                sheetState = rescheduleSheetState,
            ) {
                RescheduleTermsBottomSheet(
                    state = uiState.rescheduleState,
                    totalOpenCents = viewModel.totalOpenReceivableCents(order),
                    onDismiss = { viewModel.closeRescheduleSheet() },
                    onAddTerm = { viewModel.addRescheduleTerm() },
                    onRemoveTerm = { termId -> viewModel.removeRescheduleTerm(termId) },
                    onTermDateChanged = { termId, dateIso -> viewModel.updateRescheduleTermDate(termId, dateIso) },
                    onTermAmountChanged = { termId, amount -> viewModel.updateRescheduleTermAmount(termId, amount) },
                    onConfirmRequest = { viewModel.requestRescheduleConfirmation() },
                    onDismissConfirmDialog = { viewModel.dismissRescheduleConfirmation() },
                    onConfirmReschedule = { viewModel.confirmReschedule() }
                )
            }
        }

        if (uiState.voidPaymentState.showSheet) {
            ModalBottomSheet(
                containerColor = MaterialTheme.colorScheme.background,
                onDismissRequest = { viewModel.closeVoidPaymentSheet() },
                sheetState = voidPaymentSheetState,
            ) {
                VoidPaymentBottomSheet(
                    state = uiState.voidPaymentState,
                    onReasonChange = { viewModel.onVoidReasonChanged(it) },
                    onDismiss = { viewModel.closeVoidPaymentSheet() },
                    onSubmit = { viewModel.confirmVoidPayment() }
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
}

@Composable
private fun OrderDetailsLoadingSkeleton() {
    val brush = shimmerBrush()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (index == 0) 210.dp else 140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brush)
            )
        }
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(brush)
            )
        }
    }
}

@Composable
private fun OrderHeaderCard(order: Order, viewModel: OrdersDetailsViewModel) {
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
                    CardSectionIcon(imageVector = Icons.Rounded.Receipt)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "#${order.internalNumber}",
                        style = titleMediumBold()
                    )
                }
                OrderStatusChip(status = order.status) { }
            }
            Spacer(modifier = Modifier.height(12.dp))
            InfoRow("Fecha", DateFormat.getOrdersFormattedDate(order.createdAt))
            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            for (item in viewModel.getSummaryFromOrder()) {
                InfoRow(item.first, formatNumberToMoney(item.second))
                Spacer(modifier = Modifier.height(4.dp))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.total),
                    style = bodyMediumBold()
                )
                Text(
                    text = formatNumberToMoney(order.totalAmount),
                    style = bodyMediumBold(color = MaterialTheme.colorScheme.secondary)
                )
            }
        }
    }
}

@Composable
private fun OrderItemsCard(order: Order) {
    var itemsExpanded by remember(order.internalNumber) { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { itemsExpanded = !itemsExpanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                CardSectionIcon(imageVector = Icons.Rounded.Inventory2)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.items) + " (${order.lines.size})",
                    style = bodyMediumBold(),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (itemsExpanded) {
                        Icons.Rounded.KeyboardArrowUp
                    } else {
                        Icons.Rounded.KeyboardArrowDown
                    },
                    contentDescription = if (itemsExpanded) "Colapsar" else "Expandir",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp)
                )
            }
            AnimatedVisibility(
                visible = itemsExpanded,
                enter = fadeIn(animationSpec = tween(160)) + expandVertically(animationSpec = tween(160)),
                exit = fadeOut(animationSpec = tween(120)) + shrinkVertically(animationSpec = tween(120))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    order.lines.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    text = item.itemName,
                                    style = bodyMedium()
                                )
                                Text(
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    text = formatNumberToMoney(item.baseUnitPrice),
                                    style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                text = "${item.quantity.toQuantityUiString()}x",
                                style = bodyMediumBold()
                            )
                        }
                        if (index < order.lines.lastIndex) {
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderRelatedDocumentsCard(
    relatedDocuments: List<RelatedDocument>,
    onOpenOrder: (String) -> Unit
) {
    if (relatedDocuments.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CardSectionIcon(imageVector = Icons.Rounded.Receipt)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Documentos relacionados",
                    style = bodyMediumBold()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            RelatedDocumentsTableHeader()
            Spacer(modifier = Modifier.height(6.dp))

            relatedDocuments.forEachIndexed { index, document ->
                RelatedDocumentRow(
                    document = document,
                    onOpenOrder = onOpenOrder
                )
                if (index < relatedDocuments.lastIndex) {
                    Divider(modifier = Modifier.padding(vertical = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun RelatedDocumentsTableHeader() {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Pedido",
            style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.weight(1.4f)
        )
        Text(
            text = "Tipo",
            style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Monto",
            style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.9f)
        )
    }
}

@Composable
private fun RelatedDocumentRow(
    document: RelatedDocument,
    onOpenOrder: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = document.orderNumber,
            style = bodyMedium(color = MaterialTheme.colorScheme.secondary).copy(
                textDecoration = TextDecoration.Underline
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1.4f)
                .clickable { onOpenOrder(document.orderNumber) }
        )
        Text(
            text = document.displayOrderType(),
            style = bodyMedium(),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp)
        )
        Text(
            text = formatNumberToMoney(document.totalAmount),
            style = bodyMediumBold(),
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.9f)
        )
    }
}

@Composable
private fun CardSectionIcon(imageVector: ImageVector) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(vanishedBackgroundColor(), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun OrderActionsDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Divider(modifier = Modifier.weight(1f))
        Text(
            text = "Acciones del pedido",
            style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        Divider(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun OrderActionButtonContent(icon: ImageVector, label: String) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(18.dp)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(label)
}

@Composable
private fun OrderOutlinedActionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val actionColor = if (enabled) color else MaterialTheme.colorScheme.outline
    OutlinedButtonM(
        onClick = onClick,
        enabled = enabled,
        contentColor = actionColor,
        containerColor = actionColor.copy(alpha = if (enabled) 0.04f else 0.02f),
        border = BorderStroke(1.dp, actionColor.copy(alpha = if (enabled) 0.55f else 0.35f))
    ) {
        OrderActionButtonContent(icon = icon, label = label)
    }
}

@Composable
private fun OrderReceivablesCard(
    order: Order,
    pendingCents: Long,
    overdueCents: Long,
    terms: List<ReceivableTermDto>,
    onReschedule: () -> Unit
) {
    if (terms.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CardSectionIcon(imageVector = Icons.Rounded.Payment)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Cuotas de pago", style = bodyMediumBold())
            }

            Spacer(modifier = Modifier.height(8.dp))
            InfoRow("Pendiente", formatDollarFromCents(pendingCents))
            Spacer(modifier = Modifier.height(4.dp))
            InfoRow("Vencido", formatDollarFromCents(overdueCents))
            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            TermsTableHeader()
            Spacer(modifier = Modifier.height(6.dp))
            terms.forEachIndexed { index, term ->
                TermTableRow(term = term)
                if (index < terms.lastIndex) {
                    Divider(modifier = Modifier.padding(vertical = 6.dp))
                }
            }

            if (pendingCents > 0L && order.invoiceStatus == InvoiceStatus.ISSUED.id) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButtonM(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onReschedule
                ) {
                    Text("Reprogramar cuotas")
                }
            }
        }
    }
}

@Composable
private fun TermsTableHeader() {
    var showStatusHelp by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth()) {
        Text("#", style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant), modifier = Modifier.weight(0.6f))
        Text("Vence", style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant), modifier = Modifier.weight(1.3f))
        Text("Monto", style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant), modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
        Text("Adeudado", style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant), modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
        Text("Pagado", style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant), modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
        Box(modifier = Modifier.weight(0.7f), contentAlignment = Alignment.CenterEnd) {
            IconButton(
                onClick = { showStatusHelp = true },
                modifier = Modifier.size(18.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.HelpOutline,
                    contentDescription = "Estados de cuota",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }

    if (showStatusHelp) {
        AlertDialog(
            onDismissRequest = { showStatusHelp = false },
            title = { Text("Estado de cuota") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Verde: Pagado")
                    Text("Azul: Parcial")
                    Text("Naranja: Pendiente")
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatusHelp = false }) { Text("Entendido") }
            }
        )
    }
}

@Composable
private fun TermTableRow(term: ReceivableTermDto) {
    val originalCents = term.originalAmount.toLongCents()
    val openCents = term.openAmount.toLongCents()
    val paidCents = (originalCents - openCents).coerceAtLeast(0L)
    val statusColor = when {
        openCents <= 0L -> Color(0xFF2E7D32)
        openCents < originalCents -> Color(0xFF1976D2)
        else -> Color(0xFFFF9800)
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(term.termNumber.toString(), style = bodyMedium(), modifier = Modifier.weight(0.6f))
        Text(formatUnixSecondsDate(term.dueDateUnixSeconds), style = bodyMedium(), modifier = Modifier.weight(1.3f))
        Text(formatDollarFromCents(originalCents), style = bodyMedium(), modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
        Text(formatDollarFromCents(openCents), style = bodyMedium(), modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
        Text(formatDollarFromCents(paidCents), style = bodyMedium(), modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
        Box(modifier = Modifier.weight(0.7f), contentAlignment = Alignment.CenterEnd) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(statusColor, CircleShape)
            )
        }
    }
}

@Composable
private fun RegisteredPaymentsCard(
    order: Order,
    viewModel: OrdersDetailsViewModel,
    onNavigateAchReview: (String) -> Unit,
    onVoidPayment: (Long) -> Unit
) {
    if (order.orderPayments.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CardSectionIcon(imageVector = Icons.Rounded.Payment)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Pagos registrados", style = bodyMediumBold())
            }
            Spacer(modifier = Modifier.height(8.dp))

            order.orderPayments.forEachIndexed { index, payment ->
                val paymentDate = formatRfc3339DateOnly(payment.paymentDate)
                val isVoided = !payment.voidedAt.isNullOrBlank()
                val isAchAutomatic = viewModel.isAutomaticAchPayment(payment) && payment.paymentIntentId.isNotBlank()
                val yappyPaymentId = payment.paymentIntentId.takeIf {
                    it.isNotBlank() && payment.isYappyPayment()
                }
                val achIntentId = payment.paymentIntentId
                val achState = if (isAchAutomatic) viewModel.achDetailState(achIntentId) else null
                val achDetail = achState?.detail
                val achStatusRaw = achDetail?.paymentStatus ?: payment.paymentStatusStr
                val achStatus = viewModel.achStatusLabel(achStatusRaw)
                val canApproveAch = isAchAutomatic && !isVoided && viewModel.canShowAchApproveAction(achStatusRaw)
                val canRejectAch = isAchAutomatic && !isVoided && viewModel.canShowAchRejectAction(achStatusRaw)
                val canOpenProof = isAchAutomatic && !isVoided && viewModel.canShowAchProofAction(achDetail)

                val badgeLabel = when {
                    isVoided -> "Anulado"
                    isAchAutomatic -> achStatus
                    else -> "Aplicado"
                }
                val statusColorPair = when {
                    isVoided -> Pair(Color(0xFFFFEBEE), Color(0xFFD32F2F))
                    normalizeStatusToken(achStatusRaw) in setOf("approved", "paid", "succeeded", "completed") ->
                        Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
                    normalizeStatusToken(achStatusRaw) in setOf("rejected", "declined", "cancelled") ->
                        Pair(Color(0xFFFFEBEE), Color(0xFFD32F2F))
                    else -> Pair(Color(0xFFFFF8E1), Color(0xFFF57F17))
                }
                val badgeBg = statusColorPair.first
                val badgeFg = statusColorPair.second

                if (isAchAutomatic) {
                    LaunchedEffect(achIntentId) {
                        viewModel.loadAchDetailIfNeeded(achIntentId)
                    }
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(payment.paymentMethod.name.ifBlank { "N/A" }, style = bodyMediumBold())
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = badgeLabel,
                                style = labelSmall(color = badgeFg),
                                modifier = Modifier
                                    .background(badgeBg, RoundedCornerShape(50))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(formatDollarFromCents(payment.totalAmount.toLongCents()), style = bodyMediumBold())
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = paymentDate,
                        style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    yappyPaymentId?.let { paymentId ->
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "ID de pago: $paymentId",
                            style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    if (isAchAutomatic) {
                        Spacer(modifier = Modifier.height(4.dp))
                        if (achState?.isLoading == true && achDetail == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(shimmerBrush())
                            )
                        } else if (!achState?.errorMessage.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = achState?.errorMessage.orEmpty(),
                                    style = labelSmall(color = MaterialTheme.colorScheme.error)
                                )
                            }
                        } else {
                            achDetail?.let { detail ->
                                if (detail.bankName.isNotBlank()) {
                                    Text(
                                        text = "Banco: ${detail.bankName}",
                                        style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                                if (detail.destinationAccount.isNotBlank()) {
                                    Text(
                                        text = "Cuenta destino: ${detail.destinationAccount}",
                                        style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                                if (detail.reference.isNotBlank()) {
                                    Text(
                                        text = "Referencia: ${detail.reference}",
                                        style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TextButtonS(
                                label = "Ver detalles",
                                color = MaterialTheme.colorScheme.primary,
                                prefixIcon = rememberVectorPainter(Icons.Rounded.OpenInNew)
                            ) {
                                onNavigateAchReview(achIntentId)
                            }
                            if (canOpenProof) {
                                TextButtonS(
                                    label = "Ver comprobante",
                                    color = MaterialTheme.colorScheme.secondary,
                                    prefixIcon = rememberVectorPainter(Icons.Rounded.Visibility)
                                ) {
                                    viewModel.openAchProofPreview(achIntentId)
                                }
                            }
                        }

                        if (canApproveAch || canRejectAch) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (canApproveAch) {
                                    OutlinedButtonM(
                                        modifier = Modifier.weight(1f),
                                        onClick = { viewModel.openAchApproveDialog(achIntentId) }
                                    ) {
                                        Text("Aprobar")
                                    }
                                }
                                if (canRejectAch) {
                                    OutlinedButtonM(
                                        modifier = Modifier.weight(1f),
                                        onClick = { viewModel.openAchRejectDialog(achIntentId) },
                                        contentColor = MaterialTheme.colorScheme.error,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("Rechazar")
                                    }
                                }
                            }
                        }
                    }
                    if (!isVoided &&
                        !payment.isAutomatic &&
                        payment.id != null &&
                        order.invoiceStatus == InvoiceStatus.ISSUED.id
                    ) {
                        TextButtonS(
                            label = "Anular pago",
                            color = MaterialTheme.colorScheme.error
                        ) {
                            onVoidPayment(payment.id)
                        }
                    }
                }
                if (index < order.orderPayments.lastIndex) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

private fun OrderPaymentDto.isYappyPayment(): Boolean {
    val methodName = paymentMethod.name.lowercase()
    val methodDescription = paymentMethod.description.lowercase()
    return methodName.contains("yappy") ||
        methodDescription.contains("yappy") ||
        paymentMethod.id == 12
}

private fun normalizeStatusToken(raw: String?): String {
    return raw.orEmpty()
        .trim()
        .lowercase()
        .replace('-', '_')
        .replace(' ', '_')
}

@Composable
private fun OrderInvoicingCard(order: Order, onShowCancelDialog: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CardSectionIcon(imageVector = Icons.Rounded.Receipt)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = FEDocumentType.fromCode(order.orderType).description,
                    style = bodyMediumBold()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Estado",
                    style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.weight(0.4f)
                )
                val shipColors = invoiceStatusColors(order.invoiceStatus ?: 0)
                SuggestionChip(
                    modifier = Modifier.height(23.dp),
                    onClick = { },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = shipColors.first,
                        labelColor = shipColors.second,
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = shipColors.first,
                        disabledBorderColor = shipColors.first,
                        borderWidth = 1.dp
                    ),
                    label = {
                        Text(
                            text = invoiceStatusLabel(order.invoiceStatus ?: 0),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            InfoRow("CUFE", order.externalInvoiceNumber ?: "")
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val canCancel =
                    canCancelInvoice(order.invoiceStatus ?: 0, order.createdAt)
                var showCancelConfirmDialog by remember { mutableStateOf(false) }
                if (canCancel) {
                    TextButtonS(
                        label = "Anular factura",
                        color = if (canCancel) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        prefixIcon = rememberVectorPainter(Icons.Rounded.Cancel),
                    ) {
                        showCancelConfirmDialog = true
                    }
                    if (showCancelConfirmDialog) {
                        AlertDialog(
                            onDismissRequest = { showCancelConfirmDialog = false },
                            title = { Text("Confirmar anulación") },
                            text = {
                                Text(
                                    "¿Deseas anular esta factura?\n" +
                                            "• Solo es posible dentro de los 7 días de emitida.\n" +
                                            "• Esta acción no se puede deshacer."
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showCancelConfirmDialog = false
                                        onShowCancelDialog()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    )
                                ) { Text("Anular") }
                            },
                            dismissButton = {
                                Button(
                                    onClick = { showCancelConfirmDialog = false }
                                ) { Text("Cancelar") }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f, fill = true))
                TextButtonS(
                    label = "Ver en la DGI",
                    color = MaterialTheme.colorScheme.secondary,
                    prefixIcon = rememberVectorPainter(Icons.Rounded.Search),
                ) {
                    openCustomTab("https://dgi-fep.mef.gob.pa/Consultas/FacturasPorCUFE/" + order.externalInvoiceNumber)
                }
            }
        }
    }
}

@Composable
private fun OrderCustomerCard(customer: com.teco.ventago.features.orders.domain.models.CustomerSnapshot, phoneNumber: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CardSectionIcon(imageVector = Icons.Rounded.Business)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.invoice_i_client),
                    style = bodyMediumBold()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(stringResource(Res.string.invoice_i_name), customer.name)

            customer.ruc?.let { ruc ->
                Spacer(modifier = Modifier.height(4.dp))
                InfoRow("RUC", ruc)
            }

            val emailValue = customer.email
            if (emailValue != null && emailValue.isNotEmpty() && !emailValue.contains("pos.com")) {
                Spacer(modifier = Modifier.height(4.dp))
                InfoRow(stringResource(Res.string.email), emailValue)
            }

            phoneNumber?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = {
                            try {
                                openDialer(phoneNumber)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.phone),
                        style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.weight(0.4f)
                    )
                    Row(
                        modifier = Modifier.weight(0.6f),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = phoneNumber,
                            style = bodyMedium(),
                            textAlign = TextAlign.End
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(Res.drawable.order_call_customer),
                            contentDescription = "Prefix Icon",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

private fun replacementInvoiceLabel(status: Int): String {
    return when (status) {
        1 -> "Pendiente"
        2 -> "Emitida"
        3 -> "Fallida"
        4 -> "Anulada"
        else -> "Sin emitir"
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
private fun CancelOrderBottomSheet(
    reason: String,
    apiErrorMessage: String?,
    onReasonChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val reasonValidation = OrderCxcValidators.validateCancelOrderReason(reason)
    val showReasonError = reason.isNotBlank() && reasonValidation != null
    val supportingText = if (showReasonError) {
        reasonValidation.orEmpty()
    } else {
        "Mínimo $MIN_CANCEL_ORDER_REASON_LENGTH caracteres."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .navigationBarsPadding()
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.errorContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Cancel,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Anular pedido", style = titleMediumBold())
                Text(
                    "Esta acción requiere un motivo para continuar.",
                    style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "El pedido quedará marcado como anulado y no se podrá procesar como una venta activa.",
                style = bodySmall(),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        DMOutlinedTextField(
            text = reason,
            label = "Motivo de anulación",
            onChange = onReasonChange,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 4,
            supportingText = supportingText,
            isError = showReasonError
        )

        if (!apiErrorMessage.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = apiErrorMessage,
                    style = bodySmall(),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButtonM(
                modifier = Modifier.weight(1f),
                onClick = onDismiss
            ) {
                Text("Cancelar")
            }
            ButtonM(
                modifier = Modifier.weight(1f),
                onClick = {
                    if (reasonValidation == null) {
                        onConfirm()
                    }
                },
                enabled = reasonValidation == null,
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ) {
                Text("Anular")
            }
        }
    }
}

@Composable
private fun RegisterPaymentsBottomSheet(
    state: RegisterPaymentState,
    receivableTerms: List<ReceivableTermDto>,
    openBalanceCents: Long,
    onDismiss: () -> Unit,
    onModeSelected: (RegisterPaymentMode) -> Unit,
    onAddPayment: () -> Unit,
    onRemovePayment: (Int) -> Unit,
    onPaymentMethodChanged: (Int, Int) -> Unit,
    onPaymentAmountChanged: (Int, String) -> Unit,
    onPaymentDateChanged: (Int, String) -> Unit,
    onAddApplication: (Int) -> Unit,
    onRemoveApplication: (Int, Int) -> Unit,
    onApplicationTermChanged: (Int, Int, Long?) -> Unit,
    onApplicationAmountChanged: (Int, Int, String) -> Unit,
    onSubmit: () -> Unit
) {
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    val methods = remember { ManualPaymentMethodOption.getAllOptions() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Registrar pago", style = titleMediumBold())
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = "Cerrar")
            }
        }

        Text("Saldo a cobrar: ${formatDollarFromCents(openBalanceCents)}", style = bodyMediumBold())

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.mode == RegisterPaymentMode.AUTOMATIC,
                onClick = { onModeSelected(RegisterPaymentMode.AUTOMATIC) },
                label = { Text("Aplicar automáticamente") }
            )
            FilterChip(
                selected = state.mode == RegisterPaymentMode.MANUAL,
                onClick = { onModeSelected(RegisterPaymentMode.MANUAL) },
                label = { Text("Aplicar manualmente") }
            )
        }

        Text(
            if (state.mode == RegisterPaymentMode.AUTOMATIC) {
                "El sistema distribuye por orden de vencimiento hasta cubrir el monto."
            } else {
                "Reparte cada pago entre cuotas."
            },
            style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )

        state.rows.forEachIndexed { index, row ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Pago ${index + 1}", style = bodyMediumBold())
                        Spacer(Modifier.weight(1f))
                        if (state.rows.size > 1) {
                            TextButtonS(label = "Eliminar", color = MaterialTheme.colorScheme.error) {
                                onRemovePayment(row.id)
                            }
                        }
                    }

                    val selectedMethodIndex = methods.indexOfFirst { it.id == row.paymentMethodCode }
                        .takeIf { it >= 0 } ?: 0
                    com.teco.ventago.design_system.textfields.helpers.DMDropDownField(
                        label = "Método",
                        items = methods,
                        selectedIndex = selectedMethodIndex,
                        onItemSelected = { _, method ->
                            onPaymentMethodChanged(row.id, method.id)
                        },
                        selectedItemToString = { it.displayName },
                        modifier = Modifier.fillMaxWidth()
                    )

                    InstallmentDueDateFieldKmp(
                        valueIso = row.paymentDateIso,
                        onDatePickedIso = { onPaymentDateChanged(row.id, it) },
                        label = "Fecha de pago",
                        modifier = Modifier.fillMaxWidth(),
                        maxSelectableDate = today
                    )

                    DMMoneyOutlinedTextField(
                        text = row.amountInput,
                        label = "Monto",
                        onChange = { onPaymentAmountChanged(row.id, it) },
                        leadingIcon = null,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        imeAction = ImeAction.Done
                    )

                    if (state.mode == RegisterPaymentMode.MANUAL) {
                        Text("Aplicaciones", style = bodyMediumBold())
                        row.applications.forEach { app ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val selectedIndex = receivableTerms.indexOfFirst { it.id == app.receivableTermId }
                                    com.teco.ventago.design_system.textfields.helpers.DMDropDownField(
                                        label = "Cuota",
                                        items = receivableTerms,
                                        selectedIndex = if (selectedIndex >= 0) selectedIndex else -1,
                                        onItemSelected = { _, term ->
                                            onApplicationTermChanged(row.id, app.id, term.id)
                                        },
                                        selectedItemToString = {
                                            "Cuota ${it.termNumber} - Adeudado ${formatDollarFromCents(it.openAmount.toLongCents())}"
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    DMMoneyOutlinedTextField(
                                        text = app.amountInput,
                                        label = "Monto aplicado",
                                        onChange = { onApplicationAmountChanged(row.id, app.id, it) },
                                        leadingIcon = null,
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = 1,
                                        imeAction = ImeAction.Done
                                    )

                                    TextButtonS(label = "Eliminar cuota", color = MaterialTheme.colorScheme.error) {
                                        onRemoveApplication(row.id, app.id)
                                    }
                                }
                            }
                        }
                        TextButtonS(label = "Agregar cuota", color = MaterialTheme.colorScheme.primary) {
                            onAddApplication(row.id)
                        }
                    }
                }
            }
        }

        TextButtonS(label = "Agregar pago", color = MaterialTheme.colorScheme.primary) {
            onAddPayment()
        }

        val allocated = state.rows.sumOf { OrderCxcValidators.parseCents(it.amountInput) }
        val remaining = (openBalanceCents - allocated).coerceAtLeast(0L)
        InfoRow("Total asignado", formatDollarFromCents(allocated))
        InfoRow("Total restante", formatDollarFromCents(remaining))

        state.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = bodySmall())
        }

        ButtonM(
            modifier = Modifier.fillMaxWidth(),
            onClick = onSubmit
        ) {
            Text("Registrar pago")
        }
    }
}

@Composable
private fun RescheduleTermsBottomSheet(
    state: RescheduleState,
    totalOpenCents: Long,
    onDismiss: () -> Unit,
    onAddTerm: () -> Unit,
    onRemoveTerm: (Int) -> Unit,
    onTermDateChanged: (Int, String) -> Unit,
    onTermAmountChanged: (Int, String) -> Unit,
    onConfirmRequest: () -> Unit,
    onDismissConfirmDialog: () -> Unit,
    onConfirmReschedule: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Reprogramar cuotas de pago", style = titleMediumBold())
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = "Cerrar")
            }
        }

        Text("Saldo adeudado total: ${formatDollarFromCents(totalOpenCents)}", style = bodyMediumBold())
        Text("Nuevos vencimientos", style = bodyMediumBold())

        state.terms.forEachIndexed { index, term ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Cuota ${index + 1}", style = bodyMediumBold())
                        Spacer(Modifier.weight(1f))
                        if (state.terms.size > 1) {
                            TextButtonS(label = "Eliminar", color = MaterialTheme.colorScheme.error) {
                                onRemoveTerm(term.id)
                            }
                        }
                    }
                    InstallmentDueDateFieldKmp(
                        valueIso = term.dueDateIso,
                        onDatePickedIso = { onTermDateChanged(term.id, it) },
                        label = "Fecha de vencimiento",
                        modifier = Modifier.fillMaxWidth()
                    )
                    DMMoneyOutlinedTextField(
                        text = term.amountInput,
                        label = "Monto",
                        onChange = { onTermAmountChanged(term.id, it) },
                        leadingIcon = null,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        imeAction = ImeAction.Done
                    )
                }
            }
        }

        TextButtonS(label = "Agregar cuota", color = MaterialTheme.colorScheme.primary) {
            onAddTerm()
        }

        val assigned = state.terms.sumOf { OrderCxcValidators.parseCents(it.amountInput) }
        InfoRow("Total adeudado por asignar a cuotas", formatDollarFromCents((totalOpenCents - assigned).coerceAtLeast(0L)))

        state.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = bodySmall())
        }

        TextButtonS(
            modifier = Modifier.fillMaxWidth(),
            label = "Reprogramar cuotas",
            color = MaterialTheme.colorScheme.secondary
        ) {
            onConfirmRequest()
        }
    }

    if (state.showConfirmDialog) {
        AlertDialog(
            onDismissRequest = onDismissConfirmDialog,
            title = { Text("Confirmar reprogramación") },
            text = {
                Text("Esta acción cancela todas las cuotas existentes y crea una nueva programación de pago.")
            },
            confirmButton = {
                ButtonM(onClick = onConfirmReschedule) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = onDismissConfirmDialog) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun VoidPaymentBottomSheet(
    state: VoidPaymentState,
    onReasonChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding()
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Anular pago", style = titleMediumBold())
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = "Cerrar")
            }
        }

        DMOutlinedTextField(
            text = state.reason,
            label = "Razón",
            onChange = onReasonChange,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 4
        )

        state.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = bodySmall())
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButtonM(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text("Cancelar")
            }
            ButtonM(onClick = onSubmit, modifier = Modifier.weight(1f)) {
                Text("Anular")
            }
        }
    }
}

@Composable
fun OrderScreenManualPaymentBottomSheetHost(
    uiState: OrderDetailsState,
    order: Order,
    showSheet: Boolean,
    onDismissSheet: () -> Unit,
    methodOptions: List<Pair<Int, String>>,
    charged: Map<Int, Long>,
    otherPaymentDescription: String,
    onToggleMethod: (Int, Boolean) -> Unit,
    onAmountChange: (Int, Long) -> Unit,
    onOtherDesc: (String) -> Unit,
    onConfirmManualPayment: () -> Unit
) {
    val totalToCharge = uiState.manualPayment.totalToChargeCents
        .takeIf { it > 0L }
        ?: order.totalAmount.toLongCents()
    val allocated = charged.values.sum()
    val remaining = (totalToCharge - allocated).coerceAtLeast(0L)

    ManualPaymentBottomSheet(
        open = showSheet,
        onDismiss = onDismissSheet,
        methodOptions = methodOptions,
        charged = charged,
        otherPaymentDescription = otherPaymentDescription,
        totalToCharge = totalToCharge,
        remaining = remaining,
        onToggleMethod = onToggleMethod,
        onAmountChange = onAmountChange,
        onOtherDesc = onOtherDesc,
        onConfirm = onConfirmManualPayment,
        uiState = uiState
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualPaymentBottomSheet(
    uiState: OrderDetailsState,
    open: Boolean,
    onDismiss: () -> Unit,
    methodOptions: List<Pair<Int, String>>,
    charged: Map<Int, Long>,
    otherPaymentDescription: String,
    totalToCharge: Long,
    remaining: Long,
    onToggleMethod: (Int, Boolean) -> Unit,
    onAmountChange: (Int, Long) -> Unit,
    onOtherDesc: (String) -> Unit,
    onConfirm: () -> Unit
) {
    if (!open) return

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Registrar pago manual",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onDismiss) {
                Icon(rememberVectorPainter(Icons.Rounded.Close), contentDescription = "Cerrar")
            }
        }

        Divider()

        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Methods
            Text("Métodos de pago", style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                methodOptions.forEach { (code, label) ->
                    val selected = charged.containsKey(code)
                    FilterChip(
                        selected = selected,
                        onClick = { onToggleMethod(code, !selected) },
                        label = { Text(label) }
                    )
                }
            }

            // Amount inputs (for selected methods)
            charged.keys.sorted().forEach { code ->
                val label = methodOptions.find { it.first == code }?.second ?: code.toString()

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label)
                    Spacer(Modifier.weight(1f))
                    DMMoneyOutlinedTextField(
                        text = (charged[code] ?: 0L).toString(),
                        label = "Monto",
                        onChange = { raw ->
                            val cents = raw.filter(Char::isDigit).toLongOrNull() ?: 0L
                            onAmountChange(code, cents)
                        },
                        leadingIcon = null,
                        modifier = Modifier.widthIn(min = 160.dp),
                        maxLines = 1,
                        imeAction = ImeAction.Done
                    )
                }

                // "Otro" description when code == 11 (match your POS behavior)
                if (code == 11) {
                    DMOutlinedTextField(
                        text = otherPaymentDescription,
                        label = "Descripción (requerida para 'Otro')",
                        onChange = onOtherDesc,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                } else if (code == 2) {
                    // Cash note, same as POS
                    Text(
                        "Se permite exceso de pago (se calculará cambio)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Divider()

            // Allocation summary (NO installments here)
            val allocated = remember(charged) { charged.values.sum() }
            val change = (allocated - totalToCharge).coerceAtLeast(0L)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Asignado")
                    Text(formatNumberToMoney((allocated / 100.0).toString()))
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Restante")
                    Text(formatNumberToMoney((remaining / 100.0).toString()))
                }

                if (change > 0) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cambio", color = MaterialTheme.colorScheme.secondary)
                        Text(
                            formatNumberToMoney((change / 100.0).toString()),
                            style = bodyMediumBold(color = MaterialTheme.colorScheme.secondary)
                        )
                    }
                }
            }

            // Confirm button (enabled only if fully covered)
            ButtonM(
                onClick = onConfirm,
                enabled = allocated >= totalToCharge
            ) {
                Text(
                    if (uiState.invoicingEnabled) "Confirmar cobro y facturar" else "Confirmar cobro",
                    style = labelLarge().copy(color = MaterialTheme.colorScheme.onPrimary)
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun formatDollarFromCents(cents: Long): String {
    val sign = if (cents < 0) "-" else ""
    val absolute = abs(cents)
    val units = absolute / 100
    val decimals = (absolute % 100).toString().padStart(2, '0')
    return "$sign$$units.$decimals"
}

private fun formatUnixSecondsDate(unixSeconds: Long): String {
    return runCatching {
        val date = Instant.fromEpochSeconds(unixSeconds).toLocalDateTime(TimeZone.of("America/Panama")).date
        "${date.dayOfMonth.toString().padStart(2, '0')}/${date.monthNumber.toString().padStart(2, '0')}/${date.year}"
    }.getOrElse { "--/--/----" }
}

private fun formatRfc3339DateOnly(raw: String?): String {
    if (raw.isNullOrBlank()) return "--/--/----"
    return runCatching {
        val instant = Instant.parse(raw)
        val date = instant.toLocalDateTime(TimeZone.of("America/Panama")).date
        "${date.dayOfMonth.toString().padStart(2, '0')}/${date.monthNumber.toString().padStart(2, '0')}/${date.year}"
    }.getOrElse {
        val prefix = raw.take(10)
        if (prefix.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            "${prefix.substring(8, 10)}/${prefix.substring(5, 7)}/${prefix.substring(0, 4)}"
        } else {
            "--/--/----"
        }
    }
}

fun paymentStatusLabel(paymentStatus: Int): String = when (paymentStatus) {
    0 -> "Pendiente"
    1 -> "Pagado parcialmente"
    2 -> "Pagado"
    3 -> "Reembolsado"
    4 -> "Cancelado"
    else -> "Sin pagar"
}

private fun canDeleteOrder(order: Order): Boolean {
    val invoiceStatus = order.invoiceStatus ?: InvoiceStatus.NONE.id
    val hasFailedOrNotInvoicedStatus = invoiceStatus == InvoiceStatus.FAILED.id ||
            invoiceStatus == InvoiceStatus.NONE.id

    val hasAutomaticPaymentRegistered = order.orderPayments.any { payment ->
        payment.isAutomatic &&
            payment.voidedAt.isNullOrBlank() &&
            (payment.charged.toLongCents() - payment.refunded.toLongCents()) > 0L
    }

    if (hasAutomaticPaymentRegistered) return false

    return order.status == OrderStatus.DRAFT || hasFailedOrNotInvoicedStatus
}

fun paymentStatusChipColors(paymentStatus: Int): Pair<Color, Color> = when (paymentStatus) {
    // Pending
    0 -> Pair(Color(0xFFFFA000), Color(0xFFFFF3E0))   // Amber text, light amber bg
    // Partially Paid
    1 -> Pair(Color(0xFF1976D2), Color(0xFFE3F2FD))   // Blue text, light blue bg
    // Paid
    2 -> Pair(Color(0xFF2E7D32), Color(0xFFE8F5E9))   // Green text, light green bg
    // Refunded
    3 -> Pair(Color(0xFF6A1B9A), Color(0xFFF3E5F5))   // Purple text, light purple bg
    // Cancelled
    4 -> Pair(Color(0xFFD32F2F), Color(0xFFFFEBEE))   // Red text, light red bg
    // Default / Unpaid
    else -> Pair(Color(0xFF757575), Color(0xFFF5F5F5)) // Gray text, light gray bg
}

fun invoiceStatusLabel(invoiceStatus: Int): String = when (invoiceStatus) {
    0 -> "Sin factura"       // InvNone
    1 -> "Pendiente"         // InvPending
    2 -> "Emitida"           // InvIssued
    3 -> "Fallida"           // InvFailed
    4 -> "Anulada"           // InvCancelled
    else -> "Desconocida"
}

// --- Helper (KMP-safe): can this invoice be canceled? ---
fun canCancelInvoice(invoiceStatus: Int, createdAtIso8601: String?): Boolean {
    if (invoiceStatus != 2 || createdAtIso8601.isNullOrBlank()) return false
    return runCatching {
        val created = Instant.parse(createdAtIso8601)         // expects ISO-8601
        val now = Clock.System.now()
        created >= (now - 7.days)
    }.getOrDefault(false)
}

// Overload in case you already have Instant on your model
fun canCancelInvoice(invoiceStatus: Int, createdAt: Instant): Boolean {
    if (invoiceStatus != 2) return false
    val now = Clock.System.now()
    return createdAt >= (now - 7.days)
}

fun invoiceStatusColors(invoiceStatus: Int): Pair<Color, Color> = when (invoiceStatus) {
    // 0 = None
    0 -> Pair(Color(0xFF757575), Color(0xFFF5F5F5))   // Gray text / light gray background

    // 1 = Pending
    1 -> Pair(Color(0xFFFFA000), Color(0xFFFFF3E0))   // Amber text / light amber background

    // 2 = Issued
    2 -> Pair(Color(0xFF2E7D32), Color(0xFFE8F5E9))   // Green text / light green background

    // 3 = Failed
    3 -> Pair(Color(0xFFD32F2F), Color(0xFFFFEBEE))   // Red text / light red background

    // 4 = Cancelled
    4 -> Pair(Color(0xFF6A1B9A), Color(0xFFF3E5F5))   // Purple text / light purple background

    // Default / Unknown
    else -> Pair(Color(0xFF757575), Color(0xFFF5F5F5))
}
