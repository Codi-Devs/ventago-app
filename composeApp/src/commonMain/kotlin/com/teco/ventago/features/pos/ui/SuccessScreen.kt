package com.teco.ventago.features.pos.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.molecules.DMDivider
import com.teco.ventago.design_system.organism.PosSuccessScreen
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.latoFontFamily
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.design_system.theme.vanishedBackgroundColor
import com.teco.ventago.core.SnackbarService
import com.teco.ventago.features.business.domain.model.Business
import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.pos.ui.viewmodel.PaymentFlowMode
import com.teco.ventago.features.pos.ui.viewmodel.PosState
import com.teco.ventago.features.pos.ui.viewmodel.PosViewModel
import com.teco.ventago.isTablet
import com.teco.ventago.navigation.OrdersScreenRoute
import com.teco.ventago.navigation.PosScreens
import com.teco.ventago.rememberPlatformState
import com.teco.ventago.utils.copyToClipboard
import com.teco.ventago.utils.DateFormat
import com.teco.ventago.utils.generateQR
import com.teco.ventago.utils.ImageSaverFactory
import com.teco.ventago.utils.KmpBarcodeFormat
import com.teco.ventago.utils.doubleTryParse
import com.teco.ventago.utils.formatNumberToMoney
import com.teco.ventago.utils.generateBarcodeImage
import com.teco.ventago.utils.getImageRequest
import com.teco.ventago.utils.openFileInGallery
import com.teco.ventago.utils.openWhatsappMessage
import com.teco.ventago.utils.shareInvoice
import com.teco.ventago.utils.shareLink
import com.teco.ventago.utils.toDecimalString
import com.teco.ventago.utils.toQuantityUiString
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.invoice_i_date
import ventago.composeapp.generated.resources.invoice_i_items
import ventago.composeapp.generated.resources.invoice_i_order
import ventago.composeapp.generated.resources.invoice_i_subtotal
import ventago.composeapp.generated.resources.invoice_i_thanks
import ventago.composeapp.generated.resources.invoice_i_total
import ventago.composeapp.generated.resources.invoice_saved
import ventago.composeapp.generated.resources.pos_discount
import ventago.composeapp.generated.resources.pos_tips
import ventago.composeapp.generated.resources.save
import ventago.composeapp.generated.resources.share

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SuccessScreen(
    viewModel: PosViewModel,
    navController: NavController,
    navigate: (PosScreens, (NavOptionsBuilder.() -> Unit)?) -> Unit) {
    val platformState = rememberPlatformState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.orderNumber, uiState.paymentLink, uiState.orderCreationFailed) {
        val hasSuccessfulOrder = uiState.orderNumber.isNotBlank() || uiState.paymentLink.isNotBlank()
        if (hasSuccessfulOrder && !uiState.orderCreationFailed) {
            platformState.requestNotificationPermission()
        }
    }

    fun goToStart() {
        viewModel.resetForNewSale()
        val popped = navController.popBackStack(
            route = PosScreens.POSScreen.name,
            inclusive = false,
            saveState = false
        )
        if (!popped) {
            // Fallback: ensure we land on POSScreen
            navigate(PosScreens.POSScreen) {
                popUpTo(PosScreens.POS.name) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    fun goToHome() {
        viewModel.resetForNewSale()
        navController.navigate(PosScreens.HomeScreen.name) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    BackHandler {
        goToStart()
    }

    if (isTablet()) {
        FriendlySuccessScreen(viewModel, navController, newSale = {
            goToStart()
        }, onHome = {
            goToHome()
        }, navigate)
    } else {
        FriendlySuccessScreen(viewModel, navController, newSale = {
            goToStart()
        }, onHome = {
            goToHome()
        }, navigate = navigate)
    }
}

@Composable
private fun FriendlySuccessScreen(
    viewModel: PosViewModel,
    navController: NavController,
    newSale: () -> Unit,
    onHome: () -> Unit,
    navigate: (PosScreens, (NavOptionsBuilder.() -> Unit)?) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    if (uiState.orderCreationFailed) {
        PosSuccessScreen(
            viewModel = viewModel,
            modifier = modifier,
            newSale = newSale,
            navigate = navigate
        )
        return
    }

    val isPaymentLink = uiState.paymentLink.isNotBlank() ||
            uiState.paymentFlowMode == PaymentFlowMode.PAYMENT_LINK
    val invoiceWarningState = uiState.postCreateInvoiceWarning
    val showInvoiceWarning = !isPaymentLink && invoiceWarningState.isWarning
    val canOpenInvoiceActions = !isPaymentLink &&
        invoiceWarningState.invoiceActionsEnabled &&
        uiState.pdfDocument.isNotBlank()
    val successGreen = Color(0xFF087A16)
    val paidGreen = Color(0xFF0A8F22)
    val warningColor = MaterialTheme.colorScheme.error
    val secondary = MaterialTheme.colorScheme.secondary
    val amount = formatNumberToMoney(viewModel.amountToCharge().toDecimalString())
    val snackbarService: SnackbarService = koinInject()
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(vanishedBackgroundColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(top = 72.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            SuccessHero(
                title = when {
                    isPaymentLink -> "¡Link generado!"
                    showInvoiceWarning -> "Orden creada con advertencia"
                    else -> "¡Factura completada!"
                },
                subtitle = when {
                    isPaymentLink -> "Tu orden fue procesada correctamente."
                    showInvoiceWarning -> "La orden fue creada, pero la factura requiere atención."
                    else -> "Tu orden fue procesada correctamente."
                },
                color = if (showInvoiceWarning) warningColor else successGreen,
                icon = if (showInvoiceWarning) Icons.Filled.Warning else Icons.Filled.Check,
                circleColor = if (showInvoiceWarning) warningColor.copy(alpha = 0.14f) else Color(0xFFD7F2D1)
            )

            Spacer(modifier = Modifier.height(28.dp))

            OrderSummaryCard(
                uiState = uiState,
                amount = amount,
                paymentSummary = if (isPaymentLink) null else SuccessPaymentSummary(
                    method = resolvePaymentMethodLabel(viewModel, uiState),
                    amount = amount,
                    paidColor = paidGreen
                ),
                onOpenOrder = {
                    if (uiState.orderNumber.isNotBlank()) {
                        navController.navigate(OrdersScreenRoute(orderNumber = uiState.orderNumber))
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (showInvoiceWarning) {
                InvoiceWarningCard(
                    message = invoiceWarningState.warningMessage.orEmpty()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (isPaymentLink) {
                PaymentLinkCard(
                    link = uiState.paymentLink,
                    onCopy = {
                        if (uiState.paymentLink.isNotBlank()) {
                            copyToClipboard("Link de pago", uiState.paymentLink)
                            coroutineScope.launch { snackbarService.show("Link copiado") }
                        }
                    },
                    onWhatsapp = {
                        if (uiState.paymentLink.isNotBlank()) {
                            openWhatsappMessage(
                                phoneNumber = null,
                                message = "Te comparto el enlace de pago: ${uiState.paymentLink}"
                            )
                        }
                    }
                )
            } else if (canOpenInvoiceActions) {
                InvoiceDownloadCard(
                    enabled = true,
                    onClick = { viewModel.openPdfDocument() }
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            ButtonM(
                onClick = newSale,
                containerColor = secondary,
                modifier = Modifier.widthIn(max = 520.dp)
            ) {
                Text(
                    text = "Hacer otra orden",
                    style = bodyMediumBold(color = MaterialTheme.colorScheme.onSecondary)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButtonM(
                onClick = {
                    if (isPaymentLink && uiState.paymentLink.isNotBlank()) {
                        shareLink(uiState.paymentLink)
                    } else {
                        viewModel.sharePdfDocument()
                    }
                },
                enabled = if (isPaymentLink) uiState.paymentLink.isNotBlank() else canOpenInvoiceActions,
                contentColor = secondary,
                border = BorderStroke(1.dp, secondary),
                modifier = Modifier.widthIn(max = 520.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isPaymentLink) "Compartir link" else "Compartir factura",
                    style = bodyMediumBold(color = secondary)
                )
            }
        }

        IconButton(
            onClick = onHome,
            modifier = Modifier
                .padding(start = 16.dp, top = 18.dp)
                .size(42.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.Outlined.Home,
                contentDescription = "Inicio",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun SuccessHero(
    title: String,
    subtitle: String,
    color: Color,
    icon: ImageVector,
    circleColor: Color
) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(circleColor)
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(48.dp)
        )
    }
    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = title,
        style = titleMediumBold(color = color),
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = subtitle,
        style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun InvoiceWarningCard(
    message: String
) {
    SuccessCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Estado de facturación",
                    style = bodyMediumBold(color = MaterialTheme.colorScheme.error)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message,
                    style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
    }
}

@Composable
private fun OrderSummaryCard(
    uiState: PosState,
    amount: String,
    paymentSummary: SuccessPaymentSummary?,
    onOpenOrder: () -> Unit
) {
    SuccessCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenOrder),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Orden ${uiState.orderNumber.toCompactOrderNumber()}",
                style = titleMediumBold(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = amount,
                style = bodyMediumBold(),
                textAlign = TextAlign.End
            )
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Filled.ArrowForwardIos,
                contentDescription = "Ver detalle",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        CustomerRows(uiState)

        paymentSummary?.let {
            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(18.dp))
            PaymentSummaryContent(it)
        }
    }
}

@Composable
private fun CustomerRows(uiState: PosState) {
    val details = customerDetails(uiState)
    if (details.isEmpty()) return

    Spacer(modifier = Modifier.height(18.dp))
    details.forEach { detail ->
        DetailRow(
            icon = detail.icon,
            label = detail.label,
            value = detail.value
        )
        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
private fun PaymentSummaryContent(
    summary: SuccessPaymentSummary
) {
    Text(
        text = "Método de pago",
        style = bodyMediumBold()
    )
    Spacer(modifier = Modifier.height(14.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Filled.CreditCard,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = summary.method,
            style = bodyMedium(),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = summary.amount,
            style = bodyMediumBold(color = summary.paidColor),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun InvoiceDownloadCard(
    enabled: Boolean,
    onClick: () -> Unit
) {
    SuccessCard(
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick),
        contentPadding = 18.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Filled.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Descargar factura",
                    style = bodyMediumBold(color = MaterialTheme.colorScheme.secondary)
                )
                Text(
                    text = "Guarda tu factura en PDF",
                    style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = null,
                tint = if (enabled) Color(0xFF07A8F0) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun PaymentLinkCard(
    link: String,
    onCopy: () -> Unit,
    onWhatsapp: () -> Unit
) {
    SuccessCard {
        Text(text = "Enlace de pago", style = bodyMediumBold())
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Comparte este enlace para recibir el pago.",
            style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(0.92f)
            ) {
                CompactOutlinedAction(
                    label = "Copiar enlace",
                    icon = Icons.Filled.ContentCopy,
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = onCopy
                )
                CompactOutlinedAction(
                    label = "WhatsApp",
                    icon = Icons.Filled.Share,
                    color = Color(0xFF0A8F22),
                    onClick = onWhatsapp
                )
            }
            PaymentQr(link = link, modifier = Modifier.weight(1.08f))
        }

        Spacer(modifier = Modifier.height(18.dp))

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = vanishedBackgroundColor(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Importante",
                        style = labelSmall(color = MaterialTheme.colorScheme.secondary)
                    )
                    Text(
                        text = "El enlace expirará en 24 horas.",
                        style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentQr(link: String, modifier: Modifier = Modifier) {
    if (link.isBlank()) {
        Box(
            modifier = modifier
                .heightIn(min = 148.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "QR no disponible",
                style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val bitmap = remember(link) {
        generateQR(width = 500, height = 500, url = link).toImageBitmap()
    }
    bitmap?.let {
        Image(
            painter = BitmapPainter(it),
            contentDescription = "QR de enlace de pago",
            modifier = modifier
                .heightIn(min = 150.dp, max = 210.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Fit
        )
    } ?: Box(
        modifier = modifier.heightIn(min = 148.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("QR no disponible", style = bodyMedium())
    }
}

@Composable
private fun CompactOutlinedAction(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, color, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = labelSmall(color = color),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Text(
                text = value,
                style = bodyMedium(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SuccessCard(
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.ui.unit.Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

private data class SuccessDetail(
    val icon: ImageVector,
    val label: String,
    val value: String
)

private data class SuccessPaymentSummary(
    val method: String,
    val amount: String,
    val paidColor: Color
)

private fun customerDetails(uiState: PosState): List<SuccessDetail> {
    val details = mutableListOf<SuccessDetail>()
    uiState.customer?.let { customer ->
        if (customer.name.isNotBlank()) {
            details += SuccessDetail(Icons.Outlined.Person, "Cliente", customer.name)
        }
        customer.ruc?.takeIf { it.isNotBlank() }?.let {
            details += SuccessDetail(Icons.Outlined.Description, "RUC", it)
        }
        customer.email?.takeIf { it.isNotBlank() }?.let {
            details += SuccessDetail(Icons.Outlined.Email, "Correo", it)
        }
    } ?: run {
        uiState.finalName?.takeIf { it.isNotBlank() }?.let {
            details += SuccessDetail(Icons.Outlined.Person, "Cliente", it)
        }
        uiState.finalIdNumber?.takeIf { it.isNotBlank() }?.let {
            details += SuccessDetail(Icons.Outlined.Description, "RUC", it)
        }
        uiState.finalEmail?.takeIf { it.isNotBlank() }?.let {
            details += SuccessDetail(Icons.Outlined.Email, "Correo", it)
        }
    }
    return details
}

private fun resolvePaymentMethodLabel(
    viewModel: PosViewModel,
    uiState: PosState
): String {
    val manualPayments = uiState.charged.filterValues { it > 0L }
    if (manualPayments.isEmpty()) return "Pago registrado"

    val labels = viewModel.manualMethodOptions().toMap()
    return if (manualPayments.size == 1) {
        labels[manualPayments.keys.first()] ?: "Método de pago"
    } else {
        "Múltiples métodos"
    }
}

private fun String.toCompactOrderNumber(): String {
    if (isBlank()) return "#-"
    val normalized = removePrefix("#")
    return "#${normalized.substringAfterLast("-")}"
}

@Composable
private fun TabletSuccessScreen(viewModel: PosViewModel, newSale: () -> Unit, navigate: (PosScreens, (NavOptionsBuilder.() -> Unit)?) -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val invoiceSavedString = stringResource(Res.string.invoice_saved)

    Row(
        modifier = Modifier
            .fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        PosSuccessScreen(
            viewModel = viewModel,
            modifier = Modifier
                .weight(1f),
            newSale = newSale,
            navigate = navigate,
            showSeeInvoice = false
        )
        Column(
            modifier = Modifier
                .padding(horizontal = 0.dp)
                .width(350.dp)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
//            InvoiceContent(
//                modifier = Modifier
//                    .drawWithCache {
//                        val width = this.size.width.toInt()
//                        val height = this.size.height.toInt()
//                        onDrawWithContent {
//                            val bitmap = ImageBitmap(width, height)
//                            val pictureCanvas = Canvas(bitmap)
//
//                            pictureCanvas.drawRect(
//                                0f, 0f, width.toFloat(), height.toFloat(),
//                                Paint().apply { color = Color.White } // Fondo blanco
//                            )
//
//                            draw(this, this.layoutDirection, pictureCanvas, this.size) {
//                                this@onDrawWithContent.drawContent()
//                            }
//
//                            imageBitmap = bitmap
//
//                            drawIntoCanvas { it.drawImage(
//                                bitmap, topLeftOffset = Offset.Zero,
//                                paint = Paint().apply { color = Color.White } // Fondo blanco
//                            ) }
//                        }
//                    },
//                order = viewModel.getOrder(),
//                business = viewModel.business!!
//            )

            ButtonM(
                modifier = Modifier.padding(all = 16.dp),
                onClick = {
                    val image = imageBitmap
                    var path: String? = null
                    if (image != null) {
                        val imageSaver = ImageSaverFactory.create()
//                        path = imageSaver.saveImage(image, "order_${viewModel.getOrder().orderNumber}.png")
                    }

                    coroutineScope.launch {
                        path?.let {
                            val snackResult = snackbarHostState.showSnackbar(
                                message = invoiceSavedString,
                                actionLabel = "Ver"
                            )
                            when (snackResult) {
                                SnackbarResult.Dismissed -> println("SnackbarDemo Dismissed")
                                SnackbarResult.ActionPerformed -> openFileInGallery(it)
                            }
                        }
                    }
                }) {
                Text(
                    text = stringResource(Res.string.save), style = TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontFamily = latoFontFamily(),
                        fontWeight = FontWeight.W700,
                        letterSpacing = 0.02.sp,
                    )
                )
            }

            OutlinedButtonM(modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp), onClick = {
                val image = imageBitmap
                var path: String? = null
                if (image != null) {
                    val imageSaver = ImageSaverFactory.create()
//                    path = imageSaver.saveImage(image, "order_${viewModel.getOrder().orderNumber}.png")
                }
                path?.let {
                    shareInvoice(it)
                }
            }) {
                Text(stringResource(Res.string.share))
            }


        }
    }
}

@Composable
fun InvoiceContent(modifier: Modifier = Modifier, order: Order, business: Business) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        if (business.logo.isNotBlank() && business.logo != "null") {
            AsyncImage(
                model = getImageRequest(LocalPlatformContext.current, business.logo),
                contentDescription = "Image Styles preview",
                placeholder = ColorPainter(Color.LightGray),
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .height(60.dp)
                    .width(60.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp), horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = business.name,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontSize = 24.sp)
        }
        // TODO add view to modify invoice and add this info
//        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
//            Text(text = "RUC 155166-2-2024 DV 1", fontWeight = FontWeight.Normal, fontSize = 14.sp)
//        }
        if (business.address.placeAddress != null && business.address.placeAddress != "null") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(
                    text = business.address.placeAddress,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))



        Row(
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(Res.string.invoice_i_order),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(text = " ${order.internalNumber}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(Res.string.invoice_i_date),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(text = DateFormat.getOrdersFormattedDate(order.createdAt), fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

//        order.customer?.let {
//            DMDivider(
//                label = stringResource(Res.string.invoice_i_client),
//                dividerColor = Color.Gray
//            )
//
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(top = 8.dp, bottom = 8.dp),
//                horizontalAlignment = Alignment.Start
//            ) {
//                Text(
//                    text = "${stringResource(Res.string.invoice_i_name)} ${it.name}",
//                    fontSize = 16.sp
//                )
//                if (it.email.isNotBlank()) {
//                    Text(
//                        text = "${stringResource(Res.string.invoice_i_email)} ${it.email}",
//                        fontSize = 16.sp
//                    )
//                }
//
//                if (it.phone.isNotBlank()) {
//                    Text(
//                        text = "${stringResource(Res.string.invoice_i_phone)} ${it.phone}",
//                        fontSize = 16.sp
//                    )
//                }
//            }
//
//        }

        DMDivider(
            label = stringResource(Res.string.invoice_i_items),
            dividerColor = Color.Gray
        )


        Spacer(modifier = Modifier.height(8.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            order.lines.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = item.itemName, fontSize = 16.sp)
                    Text(
                        text = "${item.quantity.toQuantityUiString()} x ${formatNumberToMoney(item.baseUnitPrice)}",
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        HorizontalDivider(thickness = 1.dp, color = Color.Gray)

        Spacer(modifier = Modifier.height(8.dp))

        Row {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${stringResource(Res.string.invoice_i_subtotal)} ${formatNumberToMoney(order.subtotal)}",
                fontSize = 16.sp
            )
        }


        Spacer(modifier = Modifier.height(4.dp))

        Row {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Impuestos: ${formatNumberToMoney(order.taxTotal)}",
                fontSize = 16.sp
            )
        }


        if (order.discountTotal.doubleTryParse() > 0.00) {
            Spacer(modifier = Modifier.height(4.dp))

            Row {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${stringResource(Res.string.pos_discount)}: ${formatNumberToMoney(order.discountTotal)}",
                    fontSize = 16.sp
                )
            }
        }

        if (order.tipsTotal.doubleTryParse() > 0.00) {
            Spacer(modifier = Modifier.height(4.dp))

            Row {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${stringResource(Res.string.pos_tips)}: ${formatNumberToMoney(order.tipsTotal)}",
                    fontSize = 16.sp
                )
            }
        }




        Spacer(modifier = Modifier.height(8.dp))

        Row {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${stringResource(Res.string.invoice_i_total)} ${formatNumberToMoney(order.totalAmount)}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        HorizontalDivider(thickness = 1.dp, color = Color.Gray)

        Spacer(modifier = Modifier.height(8.dp))

        Barcode(
            data = order.internalNumber,
            format = KmpBarcodeFormat.CODE_128,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(
                text = stringResource(Res.string.invoice_i_thanks),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

//        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
//            Text(
//                text = "${stringResource(Res.string.invoice_i_visit_us)} ${business.domain}.menucodi.com",
//                fontWeight = FontWeight.Normal,
//                fontSize = 14.sp
//            )
//        }
    }
}

@Composable
fun Barcode(
    data: String,
    modifier: Modifier = Modifier,
    format: KmpBarcodeFormat = KmpBarcodeFormat.CODE_128,
    widthPx: Int = 1024,
    heightPx: Int = 300
) {
    // Cache per input string
    val bitmap = remember(data, format, widthPx, heightPx) {
        generateBarcodeImage(
            data = data,
            format = format,
            width = widthPx,
            height = heightPx
        )
    }
    Image(
        bitmap = bitmap,
        contentDescription = "Invoice barcode",
        modifier = modifier,
        contentScale = ContentScale.FillWidth
    )
}
