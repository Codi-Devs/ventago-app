package com.teco.ventago.features.pos.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.molecules.DMAlertDialog
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.design_system.theme.vanishedBackgroundColor
import com.teco.ventago.features.pos.ui.viewmodel.PendingPaymentChangeExitAction
import com.teco.ventago.features.pos.ui.viewmodel.PendingPaymentIntentMethod
import com.teco.ventago.features.pos.ui.viewmodel.PosViewModel
import com.teco.ventago.navigation.PosScreens
import com.teco.ventago.utils.KmpBarcodeFormat
import com.teco.ventago.utils.generateBarcodeImage
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import androidx.navigation.NavOptionsBuilder
import org.jetbrains.compose.resources.painterResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.yappy_logo

private const val InvoicePendingPollMs = 1_500L
private const val InvoicePendingFastWindowMs = 60_000L

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun YappyOnsitePaymentScreen(
    viewModel: PosViewModel,
    onNewSale: () -> Unit,
    onReturnToCheckout: () -> Unit,
    onOpenOrder: (String) -> Unit,
    navigate: (PosScreens, (NavOptionsBuilder.() -> Unit)?) -> Unit,
) {
    val ui by viewModel.uiState.collectAsState()
    val onsite = ui.onsitePayment
    val payload = ui.yappyOnsiteTransaction
    val transaction = payload?.transaction
    val invoice = payload?.invoice
    val status = transaction?.status ?: onsite?.status.orEmpty()
    val invoiceStatus = invoice?.status ?: 0
    val isPendingQrView = onsite != null &&
        !status.equals("expired", ignoreCase = true) &&
        !status.equals("cancelled", ignoreCase = true) &&
        !status.equals("canceled", ignoreCase = true) &&
        !status.equals("returned", ignoreCase = true) &&
        !status.equals("succeeded", ignoreCase = true)
    var firstSucceededAtMs by remember { mutableLongStateOf(0L) }
    var showCancelConfirmation by remember { mutableStateOf(false) }
    var showChangePaymentConfirmation by remember { mutableStateOf(false) }
    val loadingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun requestExit() {
        if (!viewModel.requestPendingPaymentChangeExit(PendingPaymentChangeExitAction.POS_START)) {
            onNewSale()
        }
    }

    BackHandler(enabled = ui.pendingPaymentChangeOpen || isPendingQrView) {
        if (ui.pendingPaymentChangeOpen) {
            requestExit()
        } else {
            viewModel.requestYappyOnsiteQrExit()
        }
    }

    LaunchedEffect(onsite?.transactionId, ui.yappyOnsitePollingSuppressed) {
        if (onsite?.transactionId?.isNotBlank() == true && !ui.yappyOnsitePollingSuppressed) {
            viewModel.pollYappyOnsiteTransaction()
        }
    }

    LaunchedEffect(status, invoiceStatus, onsite?.transactionId, ui.yappyOnsitePollingSuppressed) {
        if (onsite?.transactionId.isNullOrBlank() || ui.yappyOnsitePollingSuppressed) return@LaunchedEffect
        while (
            !ui.yappyOnsitePollingSuppressed &&
            !yappyOnsiteShouldStopPolling(status = status, invoiceStatus = invoiceStatus)
        ) {
            val now = Clock.System.now().toEpochMilliseconds()
            val succeeded = status.equals("succeeded", ignoreCase = true)
            if (succeeded && firstSucceededAtMs == 0L) {
                firstSucceededAtMs = now
            }
            val timedOut = succeeded &&
                invoiceStatus == 1 &&
                firstSucceededAtMs > 0L &&
                now - firstSucceededAtMs >= InvoicePendingFastWindowMs
            delay(InvoicePendingPollMs)
            if (!ui.yappyOnsitePollingSuppressed) {
                viewModel.pollYappyOnsiteTransaction(markInvoiceProcessingTimedOut = timedOut)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(vanishedBackgroundColor()),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (ui.pendingPaymentChangeOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 560.dp),
            ) {
                PaymentScreenContent(
                    viewModel = viewModel,
                    navigate = navigate,
                    replacementConfig = PendingPaymentReplacementUiConfig(
                        sourceMethod = ui.pendingPaymentChangeSourceMethod
                            ?: PendingPaymentIntentMethod.YAPPY_ONSITE,
                        onConfirmManual = {
                            viewModel.confirmPendingPaymentChangeManual {
                                navigate(PosScreens.SuccessScreen, null)
                            }
                        },
                        onConfirmPaymentLink = {
                            viewModel.confirmPendingPaymentChangePaymentLink {
                                navigate(PosScreens.SuccessScreen, null)
                            }
                        },
                        onConfirmYappyOnsite = {},
                    ),
                    showLoadingSheet = false,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = if (isPendingQrView) 0.dp else 72.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
            ) {
                when {
                    onsite == null -> MissingQrState(onReturnToCheckout)
                    status.equals("expired", ignoreCase = true) -> ExpiredState(onReturnToCheckout)
                    status.equals("cancelled", ignoreCase = true) ||
                        status.equals("canceled", ignoreCase = true) -> CancelledState(onReturnToCheckout)
                    status.equals("returned", ignoreCase = true) -> ReturnedState(onReturnToCheckout)
                    status.equals("succeeded", ignoreCase = true) && invoiceStatus == 3 -> InvoiceFailedState(
                        warning = invoice?.warningMessage,
                        onRetry = viewModel::retryYappyOnsiteInvoice,
                        onOpenOrder = { (payload?.order?.orderNumber ?: ui.orderNumber).takeIf { it.isNotBlank() }?.let(onOpenOrder) },
                        onNewSale = onNewSale,
                    )
                    status.equals("succeeded", ignoreCase = true) && invoiceStatus == 2 -> SuccessState(
                        orderNumber = payload?.order?.orderNumber ?: ui.orderNumber,
                        amount = transaction?.amount?.ifBlank { onsite?.amount } ?: onsite?.amount.orEmpty(),
                        cufe = invoice?.cufe,
                        authNumber = invoice?.authNumber,
                        ticketReady = invoice?.ticket != null,
                        onDownloadPdf = viewModel::openYappyOnsiteInvoicePdf,
                        onSharePdf = viewModel::shareYappyOnsiteInvoicePdf,
                        onOpenOrder = { (payload?.order?.orderNumber ?: ui.orderNumber).takeIf { it.isNotBlank() }?.let(onOpenOrder) },
                        onNewSale = onNewSale,
                    )
                    status.equals("succeeded", ignoreCase = true) -> InvoicePendingState(
                        slowMode = ui.yappyOnsiteInvoiceProcessingTimedOut,
                    )
                    else -> PendingQrState(
                        qrHash = transaction?.qrHash?.ifBlank { onsite.qrHash } ?: onsite.qrHash,
                        transactionId = transaction?.transactionId?.ifBlank { onsite.transactionId } ?: onsite.transactionId,
                        status = status,
                        invoiceStatus = invoiceStatus,
                        orderNumber = payload?.order?.orderNumber ?: ui.orderNumber,
                        amount = transaction?.amount?.ifBlank { onsite.amount } ?: onsite.amount,
                        expiresAt = transaction?.expiresAt ?: onsite.expiresAt,
                        onCancel = { showCancelConfirmation = true },
                        onChangePaymentMethod = {
                            showChangePaymentConfirmation = true
                        },
                    )
                }
            }
        }
    }

    if (showCancelConfirmation) {
        DMAlertDialog(
            title = "Cancelar cobro",
            message = "El QR quedará inactivo y el cliente ya no podrá completar este pago.",
            show = true,
            confirmText = "Cancelar cobro",
            dismissText = "Mantener QR",
            onDismiss = { showCancelConfirmation = false },
            onConfirm = {
                showCancelConfirmation = false
                viewModel.cancelYappyOnsiteTransaction()
            },
        )
    }

    if (showChangePaymentConfirmation) {
        DMAlertDialog(
            title = "Seleccionar otro método",
            message = "El QR de Yappy quedará inactivo y el cliente ya no podrá completar este pago. Luego podrás seleccionar otro método de cobro para esta orden.",
            show = true,
            confirmText = "Continuar",
            dismissText = "Mantener QR",
            onDismiss = { showChangePaymentConfirmation = false },
            onConfirm = {
                showChangePaymentConfirmation = false
                viewModel.confirmOpenYappyOnsitePaymentMethodChange()
            },
        )
    }

    if (ui.yappyOnsiteExitCancelDialogVisible) {
        DMAlertDialog(
            title = "Cancelar cobro pendiente",
            message = "Para salir de esta pantalla debemos cancelar el QR activo. El cliente ya no podrá completar este pago.",
            show = true,
            confirmText = "Cancelar y salir",
            dismissText = "Mantener QR",
            onDismiss = viewModel::dismissYappyOnsiteQrExit,
            onConfirm = {
                viewModel.confirmYappyOnsiteQrExitCancellation {
                    onReturnToCheckout()
                }
            },
        )
    }

    if (ui.loadingBottomSheet.isLoading()) {
        LoadingSheet(
            state = ui.loadingBottomSheet,
            sheetState = loadingSheetState,
            onDismissRequest = viewModel::hideLoading,
        )
    }

    if (ui.pendingPaymentChangeCancelDialogVisible) {
        val errorMessage = ui.pendingPaymentChangeCancelErrorMessage?.takeIf { it.isNotBlank() }
        DMAlertDialog(
            title = "Cancelar orden",
            message = buildString {
                append("Si sales sin seleccionar otro método de pago, la orden será cancelada.")
                if (errorMessage != null) {
                    append("\n\n")
                    append(errorMessage)
                }
            },
            show = true,
            confirmText = "Cancelar orden",
            dismissText = "Seguir cobrando",
            onDismiss = viewModel::dismissPendingPaymentChangeCancelDialog,
            onConfirm = {
                viewModel.confirmPendingPaymentChangeOrderCancellation {
                    onNewSale()
                }
            },
        )
    }
}

@Composable
private fun PendingQrState(
    qrHash: String,
    transactionId: String,
    status: String,
    invoiceStatus: Int,
    orderNumber: String,
    amount: String,
    expiresAt: String?,
    onCancel: () -> Unit,
    onChangePaymentMethod: () -> Unit,
) {
    val qrBitmap = remember(qrHash) {
        generateBarcodeImage(
            data = qrHash,
            format = KmpBarcodeFormat.QR_CODE,
            width = 1200,
            height = 1200,
            margin = 0,
        )
    }
    SuccessCard(
        contentPadding = 22.dp,
    ) {
        Image(
            painter = painterResource(Res.drawable.yappy_logo),
            contentDescription = "Yappy",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .height(56.dp)
                .width(210.dp),
        )
        Spacer(Modifier.height(22.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp),
        ) {
            Image(
                bitmap = qrBitmap,
                contentDescription = "QR Yappy en caja",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(8.dp),
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(
            amountLabel(amount),
            style = titleMediumBold(color = Color(0xFF111827)).copy(fontSize = 56.sp, lineHeight = 64.sp),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        expiresAt?.let {
            Spacer(Modifier.height(18.dp))
            CountdownPill(expiresAt = it)
        }
        Spacer(Modifier.height(22.dp))
        DetailRow("Estado del pago", paymentStatusLabel(status))
        DetailRow("Factura", invoiceLabel(invoiceStatus))
        DetailRow("Factura POS", orderNumber.ifBlank { "-" })
        DetailRow("Transacción", transactionId.ifBlank { "-" })
        Spacer(Modifier.height(16.dp))
        OutlinedButtonM(
            onClick = onChangePaymentMethod,
            contentColor = MaterialTheme.colorScheme.secondary,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
            modifier = Modifier.widthIn(max = 520.dp),
        ) {
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Seleccionar otro método de pago", style = bodyMediumBold(color = MaterialTheme.colorScheme.secondary))
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButtonM(
            onClick = onCancel,
            contentColor = MaterialTheme.colorScheme.secondary,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
            modifier = Modifier.widthIn(max = 520.dp),
        ) {
            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Cancelar cobro", style = bodyMediumBold(color = MaterialTheme.colorScheme.secondary))
        }
    }
}

@Composable
private fun InvoicePendingState(slowMode: Boolean) {
    SuccessCard {
        AnimatedStatusIcon(
            iconColor = Color(0xFF087A16),
            circleColor = Color(0xFFD7F2D1),
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Pago recibido",
            style = titleMediumBold(color = Color(0xFF087A16)),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (slowMode) {
                "Gracias. La factura sigue procesándose; mantén esta pantalla abierta mientras actualizamos el estado."
            } else {
                "Gracias. Estamos emitiendo la factura."
            },
            style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Generando factura",
                style = bodyMediumBold(color = MaterialTheme.colorScheme.secondary),
            )
        }
    }
}

@Composable
private fun SuccessState(
    orderNumber: String,
    amount: String,
    cufe: String?,
    authNumber: String?,
    ticketReady: Boolean,
    onDownloadPdf: () -> Unit,
    onSharePdf: () -> Unit,
    onOpenOrder: () -> Unit,
    onNewSale: () -> Unit,
) {
    val secondary = MaterialTheme.colorScheme.secondary

    AnimatedStatusIcon(
        iconColor = Color(0xFF087A16),
        circleColor = Color(0xFFD7F2D1),
    )
    Spacer(Modifier.height(20.dp))
    Text(
        text = "¡Factura completada!",
        style = titleMediumBold(color = Color(0xFF087A16)),
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = "Tu orden fue procesada correctamente.",
        style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
        textAlign = TextAlign.Center,
    )

    Spacer(Modifier.height(28.dp))

    SuccessCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Orden ${orderNumber.toCompactOrderNumber()}",
                style = titleMediumBold(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = amountLabel(amount),
                style = bodyMediumBold(),
                textAlign = TextAlign.End,
            )
        }
        Spacer(Modifier.height(18.dp))
        DetailRow("Método de pago", "Yappy en caja")
        DetailRow("CUFE", cufe.orEmpty().ifBlank { "-" })
        DetailRow("Autorización", authNumber.orEmpty().ifBlank { "-" })
        if (ticketReady) {
            Spacer(Modifier.height(4.dp))
            Divider()
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Ticket enviado a impresora configurada", style = bodyMedium())
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    SuccessCard(
        modifier = Modifier.clickable(onClick = onDownloadPdf),
        contentPadding = 18.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Filled.Description,
                contentDescription = null,
                tint = secondary,
                modifier = Modifier.size(34.dp),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Descargar factura",
                    style = bodyMediumBold(color = secondary),
                )
                Text(
                    text = "Guarda tu factura en PDF",
                    style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
        }
    }

    Spacer(Modifier.height(22.dp))

    ButtonM(
        onClick = onNewSale,
        containerColor = secondary,
        modifier = Modifier.widthIn(max = 520.dp),
    ) {
        Text(
            text = "Hacer otra orden",
            style = bodyMediumBold(color = MaterialTheme.colorScheme.onSecondary),
        )
    }

    Spacer(Modifier.height(12.dp))

    OutlinedButtonM(
        onClick = onSharePdf,
        contentColor = secondary,
        border = BorderStroke(1.dp, secondary),
        modifier = Modifier.widthIn(max = 520.dp),
    ) {
        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("Compartir factura", style = bodyMediumBold(color = secondary))
    }

    Spacer(Modifier.height(12.dp))

    OutlinedButtonM(
        onClick = onOpenOrder,
        contentColor = secondary,
        border = BorderStroke(1.dp, secondary),
        modifier = Modifier.widthIn(max = 520.dp),
    ) {
        Text("Ver detalle de orden", style = bodyMediumBold(color = secondary))
    }
}

@Composable
private fun InvoiceFailedState(
    warning: String?,
    onRetry: () -> Unit,
    onOpenOrder: () -> Unit,
    onNewSale: () -> Unit,
) {
    PaymentStatusCard(
        icon = Icons.Filled.Warning,
        iconColor = MaterialTheme.colorScheme.error,
        title = "Pago completado",
        subtitle = "La factura no pudo generarse automáticamente.",
    ) {
        Text(
            warning.orEmpty().ifBlank { "Revisa la orden o contacta soporte para completar la facturación." },
            style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
            textAlign = TextAlign.Center,
        )
        ButtonM(onClick = onRetry) {
            Text("Reintentar factura")
        }
        OutlinedButtonM(onClick = onOpenOrder) {
            Text("Abrir orden")
        }
        TextButtonS(label = "Nueva venta", onClick = onNewSale)
    }
}

@Composable
private fun ExpiredState(onReturnToCheckout: () -> Unit) {
    PaymentStatusCard(
        icon = Icons.Filled.Warning,
        iconColor = Color(0xFFB45309),
        title = "QR expirado",
        subtitle = "Crea un nuevo QR para que el cliente pueda pagar.",
    ) {
        ButtonM(onClick = onReturnToCheckout) {
            Text("Crear nuevo pago")
        }
    }
}

@Composable
private fun CancelledState(onReturnToCheckout: () -> Unit) {
    PaymentStatusCard(
        icon = Icons.Filled.Close,
        iconColor = MaterialTheme.colorScheme.error,
        title = "Pago cancelado",
        subtitle = "Puedes volver al checkout y elegir otro método.",
    ) {
        ButtonM(onClick = onReturnToCheckout) {
            Text("Volver al checkout")
        }
    }
}

@Composable
private fun ReturnedState(onReturnToCheckout: () -> Unit) {
    PaymentStatusCard(
        icon = Icons.Filled.Warning,
        iconColor = Color(0xFFB45309),
        title = "Pago devuelto",
        subtitle = "El pago fue devuelto. Puedes volver al checkout y elegir otro método.",
    ) {
        ButtonM(onClick = onReturnToCheckout) {
            Text("Volver al checkout")
        }
    }
}

@Composable
private fun MissingQrState(onReturnToCheckout: () -> Unit) {
    PaymentStatusCard(
        icon = Icons.Filled.Warning,
        iconColor = MaterialTheme.colorScheme.error,
        title = "QR no disponible",
        subtitle = "No encontramos una transacción activa para mostrar.",
    ) {
        ButtonM(onClick = onReturnToCheckout) {
            Text("Volver al checkout")
        }
    }
}

@Composable
private fun AnimatedStatusIcon(
    iconColor: Color,
    circleColor: Color,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.72f,
        animationSpec = tween(durationMillis = 420),
    )

    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(circleColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(48.dp),
            )
        }
    }
}

@Composable
private fun CountdownPill(expiresAt: String) {
    var remaining by remember(expiresAt) { mutableStateOf(remainingLabel(expiresAt)) }

    LaunchedEffect(expiresAt) {
        while (true) {
            remaining = remainingLabel(expiresAt)
            if (remaining == "0:00" || remaining == "-") break
            delay(1_000L)
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xFFE2F4FB),
            modifier = Modifier.border(1.dp, Color(0xFFCFEAF6), RoundedCornerShape(50)),
        ) {
            Text(
                text = remaining,
                style = titleMediumBold(color = Color(0xFF156082)).copy(fontSize = 34.sp, lineHeight = 40.sp),
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun SuccessCard(
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.ui.unit.Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

@Composable
private fun PaymentStatusCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(29.dp))
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconColor)
            }
            Text(title, style = titleMediumBold(), textAlign = TextAlign.Center)
            Text(
                subtitle,
                style = bodyMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
                textAlign = TextAlign.Center,
            )
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(label, style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant), modifier = Modifier.weight(0.35f))
        Text(
            value,
            style = bodyMediumBold(),
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.65f),
        )
    }
}

internal fun yappyOnsiteShouldStopPolling(status: String, invoiceStatus: Int): Boolean {
    return status.equals("cancelled", ignoreCase = true) ||
        status.equals("canceled", ignoreCase = true) ||
        status.equals("expired", ignoreCase = true) ||
        status.equals("returned", ignoreCase = true) ||
        invoiceStatus == 2 ||
        invoiceStatus == 3
}

private fun invoiceLabel(status: Int): String {
    return when (status) {
        0 -> "Pendiente"
        1 -> "Generando"
        2 -> "Emitida"
        3 -> "Fallida"
        else -> "Sin estado"
    }
}

private fun paymentStatusLabel(status: String): String {
    return when (status.lowercase()) {
        "created", "pending" -> "Pendiente"
        "processing" -> "Procesando"
        "succeeded", "success", "paid" -> "Pagado"
        "expired" -> "Expirado"
        "cancelled", "canceled" -> "Cancelado"
        "returned" -> "Devuelto"
        else -> status.ifBlank { "Pendiente" }
    }
}

private fun amountLabel(amount: String): String {
    val cleaned = amount
        .replace("USD", "", ignoreCase = true)
        .replace("$", "")
        .trim()
    if (cleaned.isBlank()) return "$0.00"

    val normalized = cleaned.replace(",", "")
    val parts = normalized.split(".", limit = 2)
    val whole = parts.firstOrNull().orEmpty().filter { it.isDigit() }.ifBlank { "0" }
    val cents = parts.getOrNull(1)
        .orEmpty()
        .filter { it.isDigit() }
        .padEnd(2, '0')
        .take(2)
    return "\$$whole.$cents"
}

private fun String.toCompactOrderNumber(): String {
    if (isBlank()) return "#-"
    val normalized = removePrefix("#")
    return "#${normalized.substringAfterLast("-")}"
}

private fun remainingLabel(expiresAt: String): String {
    val expires = runCatching { Instant.parse(expiresAt) }.getOrNull() ?: return "-"
    val seconds = ((expires.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds()) / 1000L)
        .coerceAtLeast(0L)
    val minutes = seconds / 60L
    val remainder = seconds % 60L
    return "${minutes}:${remainder.toString().padStart(2, '0')}"
}
