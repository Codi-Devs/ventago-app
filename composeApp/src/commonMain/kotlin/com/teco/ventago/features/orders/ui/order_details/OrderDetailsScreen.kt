package com.teco.ventago.features.orders.ui.order_details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
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
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.TextButtonM
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.molecules.TicketDivider
import com.teco.ventago.design_system.molecules.orders.OrderDetailsHeader
import com.teco.ventago.design_system.molecules.orders.OrderDetailsItem
import com.teco.ventago.design_system.molecules.orders.OrderStatusChip
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
import com.teco.ventago.design_system.theme.labelLarge
import com.teco.ventago.design_system.theme.latoFontFamily
import com.teco.ventago.design_system.theme.titleLarge
import com.teco.ventago.design_system.theme.vanishedBackgroundColor
import com.teco.ventago.features.invoicing.domain.models.FEDocumentType
import com.teco.ventago.features.invoicing.domain.models.InvoiceStatus
import com.teco.ventago.features.orders.domain.models.ManualPaymentMethodOption
import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.orders.domain.models.OrderStatus
import com.teco.ventago.features.orders.domain.models.PaymentStatus
import com.teco.ventago.features.orders.ui.order_details.viewModel.OrderDetailsState
import com.teco.ventago.features.orders.ui.order_details.viewModel.OrdersDetailsViewModel
import com.teco.ventago.navigation.PosNoteRoute
import com.teco.ventago.navigation.PosScreens
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
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
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


@Composable
fun OrderDetailsActions(backStackEntry: NavBackStackEntry?,  navigateAny: (Any) -> Unit) {
    val viewModel: OrdersDetailsViewModel = backStackEntry?.let {
        koinViewModel(viewModelStoreOwner = it)
    } ?: koinViewModel()

    val uiState by viewModel.uiState.collectAsState()
    val order = uiState.order

    val phoneNumber = when {
        !uiState.order?.customer?.phone.isNullOrBlank() &&
                uiState.order?.customer?.phone != uiState.order?.customer?.id?.toString() -> uiState.order?.customer?.phone

        else -> null
    }

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
                DropdownMenuItem(
                    text = { Text("Generar nota de crédito") },
                    onClick = {
                        menuExpanded = false
                        val cufe = order.externalInvoiceNumber ?: return@DropdownMenuItem

                        navigateAny(
                            PosNoteRoute(
                                op = FEDocumentType.CREDIT_NOTE_REFERENCING_FE.code,
                                cufe = cufe,
                                createdAt = order.createdAt,
                                customerId = order.customer?.id,
                                customerName = order.customer?.name,
                                customerEmail = order.customer?.email,
                                customerphone = order.customer?.phone,
                                customerRuc = order.customer?.ruc,
                                customerStatus = order.customer?.status ?: 1,
                                customerInvoiceID = order.customer?.customerInvoiceID,
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
                                customerName = order.customer?.name,
                                customerEmail = order.customer?.email,
                                customerphone = order.customer?.phone,
                                customerRuc = order.customer?.ruc,
                                customerStatus = order.customer?.status ?: 1,
                                customerInvoiceID = order.customer?.customerInvoiceID,
                            )
                        )
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsScreen(
    viewModel: OrdersDetailsViewModel,
    navigate: (PosScreens, (NavOptionsBuilder.() -> Unit)?) -> Unit
) {

    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }

// button is shown only if invoice is NOT issued and order is cancellable


    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(confirmValueChange = { false })
    val linkSheetState = rememberModalBottomSheetState()
    val shareSheetState = rememberModalBottomSheetState()
    val markAsPaidSheetState = rememberModalBottomSheetState()

    val phoneNumber = when {
        !uiState.order?.customer?.phone.isNullOrBlank() &&
                uiState.order?.customer?.phone != uiState.order?.customer?.id?.toString() -> uiState.order?.customer?.phone

        else -> null
    }

    val order = uiState.order!!

    val canShowCancelButton = order.invoiceStatus != InvoiceStatus.ISSUED.id &&
            viewModel.isOrderCancellable(order.status)



    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .verticalScroll(rememberScrollState())
            .fillMaxSize(),
    ) {
        OrderDetailsHeader(order = uiState.order!!) {}
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            modifier = Modifier.padding(start = 8.dp, bottom = 16.dp),
            text = stringResource(Res.string.items),
            style = TextStyle(
                fontSize = 12.sp,
                fontFamily = latoFontFamily(),
                fontWeight = FontWeight(700),
                color = Color(0xFF7C8988),
                textAlign = TextAlign.Center,
            )
        )

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                .background(vanishedBackgroundColor(), RoundedCornerShape(10.dp)),
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            for (item in uiState.order!!.lines) {
                OrderDetailsItem(orderItem = item)
            }
            TicketDivider()
            Spacer(modifier = Modifier.height(8.dp))
            for (item in viewModel.getSummaryFromOrder()) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(bottom = 4.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        modifier = Modifier,
                        text = item.first,
                        style = bodyMedium()
                    )

                    Spacer(modifier = Modifier.weight(1f, fill = true))

                    Text(
                        text = formatNumberToMoney(item.second),
                        style = bodyMedium()
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(bottom = 4.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    modifier = Modifier,
                    text = stringResource(Res.string.total),
                    style = bodyMediumBold()
                )

                Spacer(modifier = Modifier.weight(1f, fill = true))

                Text(
                    text = formatNumberToMoney(uiState.order!!.totalAmount),
                    style = bodyMediumBold(color = MaterialTheme.colorScheme.primary)
                )
            }
            if (order.status != OrderStatus.CANCELLED) {
                TicketDivider()
                // Payment Info
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(bottom = 4.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier,
                        text = "Métodos de pago",
                        style = bodyMedium()
                    )

                    Spacer(modifier = Modifier.weight(1f, fill = true))
                    
                    val shipColors = paymentStatusChipColors(order.paymentStatus)
                    SuggestionChip(
                        modifier = Modifier.heightIn(min = 23.dp),
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
                                text = if (!order.paymentLink.isNullOrBlank() && order.paymentStatus == PaymentStatus.UNPAID.id) "Esperando pago por enlace" else paymentStatusLabel(
                                    order.paymentStatus
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                    )
                }
                if (order.orderPayments.isNotEmpty()) {

                    for (payment in order.orderPayments) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(bottom = 4.dp, start = 16.dp, end = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                modifier = Modifier,
                                text = payment.paymentMethod.name,
                                style = bodyMedium()
                            )

                            Spacer(modifier = Modifier.weight(1f, fill = true))
                            Text(
                                modifier = Modifier,
                                text = formatNumberToMoney(payment.totalAmount),
                                style = bodyMedium()
                            )
                        }
                    }
                }
            }

            // INVOICING INFO
            if (order.invoiceStatus != 0 && order.externalInvoiceNumber != null) {
                TicketDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(bottom = 4.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                )
                {
                    Text(
                        modifier = Modifier,
                        text = FEDocumentType.fromCode(order.orderType).description,
                        style = bodyMedium()
                    )

                    Spacer(modifier = Modifier.weight(1f, fill = true))
                }
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(bottom = 4.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                )
                {
                    Text(
                        modifier = Modifier,
                        text = "Estado",
                        style = bodyMedium()
                    )

                    Spacer(modifier = Modifier.weight(1f, fill = true))
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
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(bottom = 4.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        modifier = Modifier,
                        text = "CUFE: ",
                        style = bodyMedium()
                    )

                    Spacer(modifier = Modifier.weight(1f, fill = true))
                    Text(
                        modifier = Modifier,
                        text = order.externalInvoiceNumber,
                        style = bodyMedium()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(bottom = 4.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val canCancel =
                        canCancelInvoice(order.invoiceStatus ?: 0, order.createdAt) // or overload
                    var showCancelConfirmDialog by remember { mutableStateOf(false) }
                    if (canCancel) {
                        TextButtonS(
                            label = "Anular factura",
                            color = if (canCancel) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                            prefixIcon = rememberVectorPainter(Icons.Rounded.Cancel),
                        ) {
                            // You can also show a confirm dialog before proceeding
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
                                            showCancelDialog = true
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

                Spacer(modifier = Modifier.height(16.dp))

                // --- Anular factura (only if status=Issued and within 7 days) ---
            }

            order.customer?.let { customer ->
                TicketDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        modifier = Modifier,
                        text = stringResource(Res.string.invoice_i_client),
                        style = bodyMediumBold()
                    )

                    Spacer(modifier = Modifier.weight(1f, fill = true))
                }

                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        modifier = Modifier,
                        text = stringResource(Res.string.invoice_i_name),
                        style = bodyMedium()
                    )

                    Spacer(modifier = Modifier.weight(1f, fill = true))

                    Text(
                        modifier = Modifier,
                        text = customer.name,
                        style = bodyMedium()
                    )
                }

                customer.ruc?.let { ruc ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            modifier = Modifier,
                            text = "RUC:",
                            style = bodyMedium()
                        )

                        Spacer(modifier = Modifier.weight(1f, fill = true))

                        Text(
                            modifier = Modifier,
                            text = ruc,
                            style = bodyMedium()
                        )
                    }
                }

                if (customer.email != null && customer.email.isNotEmpty() && !customer.email.contains(
                        "pos.com"
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            modifier = Modifier,
                            text = stringResource(Res.string.email),
                            style = bodyMedium()
                        )

                        Spacer(modifier = Modifier.weight(1f, fill = true))

                        Text(
                            modifier = Modifier,
                            text = customer.email,
                            style = bodyMedium()
                        )
                    }
                }

                phoneNumber?.let {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                            .clickable(onClick = {
                                try {
                                    openDialer(phoneNumber)
                                } catch (e: Exception) {
                                    // Handle any exceptions that may occur
                                    e.printStackTrace()
                                }
                            }),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            modifier = Modifier,
                            text = stringResource(Res.string.phone),
                            style = bodyMedium()
                        )

                        Spacer(modifier = Modifier.weight(1f, fill = true))

                        Text(
                            modifier = Modifier.padding(end = 8.dp),
                            text = phoneNumber,
                            style = bodyMedium()
                        )

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


        Spacer(modifier = Modifier.height(16.dp))

        if (order.status != OrderStatus.CANCELLED) {
            when (order.invoiceStatus) {

                // 1) Invoice ISSUED → only show "Descargar factura PDF". NO payment link, NO register manual.
                InvoiceStatus.ISSUED.id -> {
                    ButtonM(onClick = {
                        viewModel.getDocumentByCufe()
                    }) {
                        Text("Ver factura PDF")
                    }
                }

                // 4) Invoice FAILED → only show "Reintentar". NO payment link, NO register manual.
                InvoiceStatus.FAILED.id -> {
                    ButtonM(onClick = {
                        viewModel.retryElectronicInvoice()
                    }) {
                        Text("Reintentar factura electrónica")
                    }
                    if (canShowCancelButton) {
                        TextButtonS(
                            modifier = Modifier.fillMaxWidth(),
                            label = "Anular pedido",
                            color = MaterialTheme.colorScheme.error
                        ) { showCancelDialog = true }
                    }
                }

                // 2 & 3) Invoice NOT ISSUED (NONE or PENDING) and NOT CANCELLED
                InvoiceStatus.NONE.id, InvoiceStatus.PENDING.id -> {
                    // (Optional) If already paid and pending, you might prefer a "Generar factura" button
                    // Keep this if you still want that previous behavior.
                    if (order.paymentStatus == PaymentStatus.PAID.id &&
                        order.invoiceStatus == InvoiceStatus.PENDING.id
                    ) {
                        ButtonM(onClick = { }) {
                            Text("Generar factura electrónica")
                        }
                        // You can `return` here if you don't want to show any other buttons when this appears.
                        // return@Column
                    } else {
                        // 3) If paymentLink == null → show only "Registrar pago manual"
                        ButtonM(onClick = {
                            viewModel.showManualPaymentSheet(true)
                        }) {
                            Text("Registrar pago manual")
                        }
                        // 2) If paymentLink != null → show "Esperando pago" + "Registrar pago manual"
                        if (!order.paymentLink.isNullOrBlank()) {
                            if (uiState.loadingPaymentLink) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            } else if (order.paymentStatus != PaymentStatus.PAID.id
                                && uiState.havePaymentsConfigured
                            ) {
                                TextButtonS(
                                    modifier = Modifier.fillMaxWidth(),
                                    label = stringResource(Res.string.payment_link)
                                ) {
                                    viewModel.getOrderPaymentLink()
                                }
                            }

                        }
                    }

                    if (canShowCancelButton) {
                        TextButtonS(
                            modifier = Modifier.fillMaxWidth(),
                            label = "Anular pedido",
                            color = MaterialTheme.colorScheme.error
                        ) { showCancelDialog = true }
                    }
                }

                // CANCELLED → no buttons requested by your rules (you already gate with != CANCELLED in your snippet)
                InvoiceStatus.CANCELLED.id -> {
                    // Intentionally no actions
                }
            }
        }


        Spacer(modifier = Modifier.height(24.dp))

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

        if (showCancelDialog) {
            AlertDialog(
                onDismissRequest = { showCancelDialog = false },
                title = { Text("Confirmar anulación") },
                text = {
                    Column {
                        Text("Ingresa el motivo de anulación del pedido.")
                        Spacer(Modifier.height(8.dp))
                        // Your existing text field component; swap for TextField if you prefer
                        DMOutlinedTextField(
                            text = cancelReason,
                            label = "Motivo",
                            onChange = { cancelReason = it },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                    }
                },
                confirmButton = {
                    ButtonM(
                        onClick = {
                            showCancelDialog = false
                            viewModel.cancelOrder(cancelReason.trim()) // ← ViewModel knows current orderId
                        },
                        enabled = cancelReason.isNotBlank()
                    ) { Text("Anular") }
                },
                dismissButton = {
                    TextButton(onClick = { showCancelDialog = false }) { Text("Cancelar") }
                }
            )
        }

        if (uiState.manualPayment.showSheet) {
            ModalBottomSheet(
                containerColor = MaterialTheme.colorScheme.background,
                onDismissRequest = {
                    viewModel.resetManualPaymentFields()
                    viewModel.showManualPaymentSheet(false)
                },
                sheetState = markAsPaidSheetState,
            ) {
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
                    otherPaymentDescription = uiState.manualPaymentDescription,
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
fun OrderScreenManualPaymentBottomSheetHost(
    uiState: OrderDetailsState,
    order: Order,
    showSheet: Boolean,
    onDismissSheet: () -> Unit,
    // provide these from VM or state holder:
    methodOptions: List<Pair<Int, String>>,
    charged: Map<Int, Long>,
    otherPaymentDescription: String,
    onToggleMethod: (Int, Boolean) -> Unit,
    onAmountChange: (Int, Long) -> Unit,
    onOtherDesc: (String) -> Unit,
    onConfirmManualPayment: () -> Unit
) {
    val totalToCharge = order.totalAmount.toLongCents()
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

    // Payment method options. Same shape as POS: (code, label)
    methodOptions: List<Pair<Int, String>>,

    // Current selection & amounts in cents, e.g. mapOf(2 to 1050L)
    charged: Map<Int, Long>,
    otherPaymentDescription: String,

    // Totals (cents)
    totalToCharge: Long,
    remaining: Long,

    // Callbacks (same spirit as your POS section)
    onToggleMethod: (Int, Boolean) -> Unit,
    onAmountChange: (Int, Long) -> Unit,
    onOtherDesc: (String) -> Unit,

    // Confirm action (called only when allocated ≥ totalToCharge)
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

                // “Otro” description when code == 11 (match your POS behavior)
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
            // If you allow “cash change” logic, derive change here; if you already have a helper, call it.
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

fun paymentStatusLabel(paymentStatus: Int): String = when (paymentStatus) {
    0 -> "Pendiente"
    1 -> "Pagado parcialmente"
    2 -> "Pagado"
    3 -> "Reembolsado"
    4 -> "Cancelado"
    else -> "Sin pagar"
}

//@Composable
//fun paymentStatusChipColors(paymentStatus: Int): Pair<Color, Color> {
//    val c = MaterialTheme.colorScheme
//    return when (paymentStatus) {
//        // 0 = Pending
//        0 -> c.onSurfaceVariant to c.surfaceVariant
//
//        // 1 = Partially Paid
//        1 -> c.onSecondaryContainer to c.secondaryContainer
//
//        // 2 = Paid
//        2 -> c.onPrimaryContainer to c.primaryContainer
//
//        // 3 = Refunded
//        3 -> c.onTertiaryContainer to c.tertiaryContainer
//
//        // 4 = Cancelled
//        4 -> c.onErrorContainer to c.errorContainer
//
//        // Default / Unknown
//        else -> c.onSurfaceVariant to c.surfaceVariant
//    }
//}

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

//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//private fun PaymentOptionsDropdown(
//    value: String, options: Map<PaymentMethodId, String>, onValueChange: (String) -> Unit, modifier: Modifier = Modifier
//) {
//
//    var expanded by remember { mutableStateOf(false) }
//
//    ExposedDropdownMenuBox(
//        expanded = expanded,
//        onExpandedChange = { expanded = it },
//        modifier = modifier,
//    ) {
//        OutlinedTextField(
//            value = value,
//            onValueChange = {},
//            readOnly = true,
//            label = { Text(stringResource(Res.string.payment_mode)) },
//            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
//            modifier = Modifier
//                .menuAnchor()
//                .fillMaxWidth()
//        )
//        ExposedDropdownMenu(
//            expanded = expanded, onDismissRequest = { expanded = false },
//            containerColor = MaterialTheme.colorScheme.background
//        ) {
//            options.forEach { opt ->
//                DropdownMenuItem(
//                    text = { Text(opt.value) },
//                    onClick = { onValueChange(opt.key.name); expanded = false })
//            }
//        }
//    }
//}
