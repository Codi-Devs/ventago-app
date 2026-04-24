@file:OptIn(ExperimentalMaterial3Api::class)

package com.teco.ventago.features.orders.ui.ach_review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.loaders.shimmerBrush
import com.teco.ventago.design_system.organism.LoadingSheet
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.bodySmall
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.features.orders.domain.models.AchPaymentDetail
import com.teco.ventago.features.orders.ui.order_details.viewModel.OrderDetailsUiEvent
import com.teco.ventago.features.orders.ui.order_details.viewModel.OrdersDetailsViewModel
import com.teco.ventago.features.quotes.ui.preview.PdfPreview
import com.teco.ventago.utils.DateFormat
import com.teco.ventago.utils.formatNumberToMoney
import com.teco.ventago.utils.openCustomTab
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Composable
fun AchPaymentReviewScreen(
    viewModel: OrdersDetailsViewModel,
    paymentUid: String
) {
    val uiState by viewModel.uiState.collectAsState()
    val loadingSheetState = rememberModalBottomSheetState(confirmValueChange = { false })
    val detail = uiState.achIntentStates[paymentUid]?.detail
    val resolvedPaymentIntentId = detail?.paymentUid?.takeIf { it.isNotBlank() } ?: paymentUid
    val reviewState = uiState.achReviewState
    val proofPreview = uiState.achProofPreviewState.takeIf {
        it.paymentIntentId == paymentUid || it.paymentIntentId == resolvedPaymentIntentId
    }

    LaunchedEffect(paymentUid) {
        viewModel.loadAchReview(paymentUid)
    }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is OrderDetailsUiEvent.OpenExternalUrl -> openCustomTab(event.url)
                else -> Unit
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background.copy(alpha = 0.45f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        when {
            reviewState.isLoading && detail == null -> {
                AchReviewLoadingSkeleton()
            }

            !reviewState.errorMessage.isNullOrBlank() && detail == null -> {
                AchReviewError(
                    message = reviewState.errorMessage.orEmpty(),
                    onRetry = { viewModel.retryAchReview() }
                )
            }

            detail != null -> {
                val canOpenProof = viewModel.canShowAchProofAction(detail)
                val canDownloadProof = viewModel.canDownloadAchProof(detail)
                val comparisonRows = remember(detail) { buildExpectedVsDetectedRows(detail) }
                val findings = remember(detail) { buildFindings(detail) }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AchHeroCard(
                        detail = detail,
                        statusLabel = viewModel.achStatusLabel(detail.paymentStatus),
                        onScoreInfo = { viewModel.setAchScoreInfoDialog(true) }
                    )

                    AchCommercialSummaryCard(detail = detail)
                    AchExpectedVsDetectedCard(rows = comparisonRows)
                    AchFindingsCard(findings = findings)
                    AchProofCard(
                        detail = detail,
                        preview = proofPreview,
                        canOpenProof = canOpenProof,
                        onOpenPreview = { viewModel.openAchProofPreview(resolvedPaymentIntentId) },
                        onDownload = { viewModel.openAchProofDocumentForDownload(resolvedPaymentIntentId) },
                        canDownload = canDownloadProof
                    )
                    AchTimelineCard(detail = detail)

                    val canApprove = viewModel.canShowAchApproveAction(detail.paymentStatus)
                    val canReject = viewModel.canShowAchRejectAction(detail.paymentStatus)
                    if (canApprove || canReject) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (canApprove) {
                                OutlinedButtonM(
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.openAchApproveDialog(resolvedPaymentIntentId) }
                                ) {
                                    Text("Aprobar")
                                }
                            }
                            if (canReject) {
                                OutlinedButtonM(
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.openAchRejectDialog(resolvedPaymentIntentId) },
                                    contentColor = MaterialTheme.colorScheme.error
                                ) {
                                    Text("Rechazar")
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
                        "El score ACH resume señales de validación del comprobante y de consistencia de datos. " +
                            "Un score alto requiere más cautela antes de aprobar."
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.setAchScoreInfoDialog(false) }) {
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

        if (uiState.achProofPreviewState.show &&
            (uiState.achProofPreviewState.paymentIntentId == paymentUid ||
                uiState.achProofPreviewState.paymentIntentId == resolvedPaymentIntentId)
        ) {
            val preview = uiState.achProofPreviewState
            val proofSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                containerColor = MaterialTheme.colorScheme.background,
                onDismissRequest = { viewModel.closeAchProofPreview() },
                sheetState = proofSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                        if (detail?.let(viewModel::canDownloadAchProof) == true) {
                            preview.paymentIntentId?.let { intentId ->
                                ButtonM(
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.openAchProofDocumentForDownload(intentId) }
                                ) {
                                    Text("Descargar comprobante")
                                }
                            }
                        }
                    }
                }
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
private fun AchHeroCard(
    detail: AchPaymentDetail,
    statusLabel: String,
    onScoreInfo: () -> Unit
) {
    val (statusBg, statusFg) = statusPalette(detail.paymentStatus)
    val (riskBg, riskFg) = riskPalette(detail.riskLevel)
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Control antifraude",
                        style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text("Revisión ACH", style = titleMediumBold())
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AchHeroChip(
                    label = statusLabel,
                    background = statusBg,
                    contentColor = statusFg
                )
                AchHeroChip(
                    label = riskLabel(detail.riskLevel),
                    background = riskBg,
                    contentColor = riskFg
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f))
                        .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Score ${detail.riskScore?.toString() ?: "N/A"}",
                        style = labelSmall(color = MaterialTheme.colorScheme.secondary)
                    )
                    IconButton(
                        onClick = onScoreInfo,
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.HelpOutline,
                            contentDescription = "Score ACH",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            InfoRow("ID de pago", detail.paymentUid.ifBlank { "No disponible" })
            InfoRow("Referencia", detail.reference.ifBlank { "No disponible" })
            Divider()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Sugerencia:",
                    style = bodyMediumBold()
                )
                Text(
                    text = decisionLabel(detail.decisionSuggested),
                    style = bodyMediumBold(),
                    color = if (normalizeStatus(detail.decisionSuggested.orEmpty()).contains("reject")) {
                        MaterialTheme.colorScheme.error
                    } else {
                        Color(0xFF2E7D32)
                    }
                )
            }
            Text(
                text = "Revisa los hallazgos para decidir si el pago ACH debe aprobarse o rechazarse.",
                style = bodySmall(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AchHeroChip(
    label: String,
    background: Color,
    contentColor: Color
) {
    Text(
        text = label,
        style = labelSmall(color = contentColor),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

@Composable
private fun AchCommercialSummaryCard(detail: AchPaymentDetail) {
    val summaryItems = listOf(
        AchSummaryItem("Cliente", detail.customerName.ifBlank { "No disponible" }, Icons.Rounded.Person),
        AchSummaryItem("Correo", detail.customerEmail.ifBlank { "No disponible" }, Icons.Rounded.Email),
        AchSummaryItem("Pedido", detail.orderNumber.toOrderLabel(), Icons.Rounded.ReceiptLong),
        AchSummaryItem("Monto", formatAmount(detail.currencyCode, detail.amount.toString()), Icons.Rounded.Info),
        AchSummaryItem("Referencia", detail.reference.ifBlank { "No disponible" }, Icons.Rounded.ReceiptLong),
        AchSummaryItem("Fecha de pago", formatAchDateTime(detail.paymentDate), Icons.Rounded.Event),
        AchSummaryItem("Banco Destino", detail.bankName.ifBlank { "No disponible" }, Icons.Rounded.AccountBalance),
        AchSummaryItem("Cuenta destino", detail.destinationAccount.ifBlank { "No disponible" }, Icons.Rounded.CreditCard)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Resumen comercial", style = bodyMediumBold())

            summaryItems.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { item ->
                        AchSummaryTile(
                            modifier = Modifier.weight(1f),
                            item = item
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private data class AchSummaryItem(
    val label: String,
    val value: String,
    val icon: ImageVector
)

@Composable
private fun AchSummaryTile(
    modifier: Modifier,
    item: AchSummaryItem
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = item.label,
                style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Text(
                text = item.value,
                style = bodySmall(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private data class AchComparisonRow(
    val label: String,
    val expected: String,
    val detected: String
)

private data class AchFindings(
    val increaseRisk: List<String>,
    val reduceRisk: List<String>
)

private fun buildExpectedVsDetectedRows(detail: AchPaymentDetail): List<AchComparisonRow> {
    val fraud = parseJsonObject(detail.latestFraud)
    val fraudFeatures = fraud?.get("features") as? JsonObject
    val ocr = parseJsonObject(detail.latestOcr)
    val sources = listOf(ocr, fraudFeatures, fraud)
    val rows = mutableListOf<AchComparisonRow>()

    fun read(keys: List<String>): String? = readFirstString(sources, keys)

    val expectedAmountRaw = read(listOf("expected_amount", "amount_expected", "expected_total"))
        ?: detail.amount.toString()
    val detectedAmountRaw = read(listOf("detected_amount", "amount_detected", "ocr_amount", "detected_total"))
        ?: "-"
    rows += AchComparisonRow(
        "Monto",
        formatAmount(detail.currencyCode, expectedAmountRaw),
        if (detectedAmountRaw == "-") "-" else formatAmount(detail.currencyCode, detectedAmountRaw)
    )

    val expectedReference = read(listOf("expected_reference", "reference_expected")) ?: detail.reference.ifBlank { "-" }
    val detectedReference = read(listOf("detected_reference", "reference_detected", "ocr_reference")) ?: "-"
    rows += AchComparisonRow("Referencia", expectedReference, detectedReference)

    val expectedAccount = read(
        listOf("expected_account", "destination_account_expected", "expected_account_number", "expected_account_last4")
    )
        ?: detail.destinationAccount.ifBlank { "-" }
    val detectedAccount = read(
        listOf(
            "detected_account",
            "destination_account_detected",
            "ocr_account_number",
            "detected_account_last4"
        )
    ) ?: "-"
    rows += AchComparisonRow("Cuenta destino", expectedAccount, detectedAccount)

    val expectedBank = detail.bankName.ifBlank { "-" }
    val detectedBank = read(listOf("detected_bank_name", "bank_detected_name", "ocr_bank_name")) ?: "-"
    rows += AchComparisonRow("Banco", expectedBank, detectedBank)

    val expectedDate = formatAchDateTime(detail.paymentDate)
    val detectedDate = formatAchDateTime(read(listOf("detected_date", "ocr_detected_date")))
    rows += AchComparisonRow("Fecha", expectedDate, detectedDate)

    return rows
}

private fun buildFindings(detail: AchPaymentDetail): AchFindings {
    val fraud = parseJsonObject(detail.latestFraud)
    if (fraud == null) return AchFindings(emptyList(), emptyList())
    val rules = fraud["rules"] as? JsonArray

    if (rules != null) {
        val increaseRisk = rules.mapNotNull { rule ->
            val obj = rule as? JsonObject ?: return@mapNotNull null
            val delta = (obj["delta"] as? JsonPrimitive)?.contentOrNull?.toIntOrNull() ?: 0
            if (delta <= 0) return@mapNotNull null
            val code = (obj["code"] as? JsonPrimitive)?.contentOrNull.orEmpty()
            mapFraudRuleToWebLabel(code)
        }
        val reduceRisk = rules.mapNotNull { rule ->
            val obj = rule as? JsonObject ?: return@mapNotNull null
            val delta = (obj["delta"] as? JsonPrimitive)?.contentOrNull?.toIntOrNull() ?: 0
            if (delta >= 0) return@mapNotNull null
            val code = (obj["code"] as? JsonPrimitive)?.contentOrNull.orEmpty()
            mapFraudRuleToWebLabel(code)
        }
        if (increaseRisk.isNotEmpty() || reduceRisk.isNotEmpty()) {
            return AchFindings(increaseRisk = increaseRisk, reduceRisk = reduceRisk)
        }
    }

    val increaseRisk = extractStringList(
        fraud["increased_risk_factors"]
            ?: fraud["risk_increasing_factors"]
            ?: fraud["high_risk_reasons"]
            ?: fraud["explanations"]
    )
    val reduceRisk = extractStringList(
        fraud["reduced_risk_factors"]
            ?: fraud["risk_reducing_factors"]
            ?: fraud["green_flags"]
    )
    return AchFindings(increaseRisk = increaseRisk, reduceRisk = reduceRisk)
}

private fun mapFraudRuleToWebLabel(code: String): String? {
    return when (normalizeStatus(code)) {
        "amount_mismatch" -> "El monto detectado no coincide con el monto esperado para este pago."
        "reference_missing" -> "No se detectó la referencia esperada en el comprobante."
        "date_outside_window" -> "La fecha del comprobante está fuera de la ventana esperada."
        "destination_account_mismatch" -> "La cuenta detectada no coincide con la cuenta ACH configurada."
        "payer_name_mismatch" -> "Payer Name Mismatch"
        "negative_history" -> "Se detectó historial reciente de eventos fallidos relacionados."
        "bank_detected" -> "Se detectó correctamente un banco en el comprobante."
        "receipt_structure_detected" -> "El documento contiene estructura típica de comprobante bancario."
        "ocr_high_confidence" -> "La información del comprobante fue leída con alta confianza."
        else -> null
    }
}

@Composable
private fun AchExpectedVsDetectedCard(rows: List<AchComparisonRow>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Esperado vs Detectado", style = bodyMediumBold())
            rows.forEach { row ->
                val mismatch = isComparisonMismatch(row.expected, row.detected)
                val statusColor = if (mismatch) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                val statusBackground = if (mismatch) {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                } else {
                    Color(0xFF2E7D32).copy(alpha = 0.12f)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (mismatch) {
                                MaterialTheme.colorScheme.error.copy(alpha = 0.06f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            }
                        )
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = row.label,
                            style = bodyMediumBold(),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (mismatch) "No coincide" else "Coincide",
                            style = labelSmall(color = statusColor),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(statusBackground)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "Esperado",
                                style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = row.expected,
                                style = bodySmall(),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "Detectado",
                                style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = row.detected,
                                style = bodySmall(),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AchFindingsCard(findings: AchFindings) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Hallazgos y sugerencia de decisión", style = bodyMediumBold())
            FindingsBucket(
                modifier = Modifier.fillMaxWidth(),
                title = "Aumentan riesgo",
                items = findings.increaseRisk,
                background = Color(0xFFFFEBEE),
                textColor = Color(0xFFC62828)
            )
            FindingsBucket(
                modifier = Modifier.fillMaxWidth(),
                title = "Reducen riesgo",
                items = findings.reduceRisk,
                background = Color(0xFFE8F5E9),
                textColor = Color(0xFF2E7D32)
            )
        }
    }
}

@Composable
private fun FindingsBucket(
    modifier: Modifier,
    title: String,
    items: List<String>,
    background: Color,
    textColor: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(background.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(title, style = bodyMediumBold(), color = textColor)
        if (items.isEmpty()) {
            Text("Sin señales", style = bodySmall(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            items.take(5).forEach { item ->
                Text("• $item", style = bodySmall(), color = textColor)
            }
        }
    }
}

@Composable
private fun AchProofCard(
    detail: AchPaymentDetail,
    preview: com.teco.ventago.features.orders.ui.order_details.viewModel.AchProofPreviewState?,
    canOpenProof: Boolean,
    onOpenPreview: () -> Unit,
    onDownload: () -> Unit,
    canDownload: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = Icons.Rounded.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text("Comprobante", style = bodyMediumBold())
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .background(MaterialTheme.colorScheme.background)
                    .padding(10.dp)
            ) {
                when {
                    preview?.isLoading == true -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(shimmerBrush())
                        )
                    }
                    !preview?.imageDataUri.isNullOrBlank() -> {
                        AsyncImage(
                            model = preview.imageDataUri,
                            contentDescription = "Comprobante ACH",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 220.dp, max = 420.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                    !preview?.previewUrl.isNullOrBlank() &&
                        (preview.contentType.orEmpty().contains("pdf", ignoreCase = true) ||
                            preview.previewUrl.endsWith(".pdf", ignoreCase = true)) -> {
                        PdfPreview(
                            url = preview.previewUrl,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                    !preview?.previewUrl.isNullOrBlank() -> {
                        AsyncImage(
                            model = preview.previewUrl,
                            contentDescription = "Comprobante ACH",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 220.dp, max = 420.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                    !detail.proofFileUrl.isNullOrBlank() && canOpenProof -> {
                        TextButtonS(
                            label = "Abrir comprobante",
                            prefixIcon = null
                        ) {
                            openCustomTab(detail.proofFileUrl)
                        }
                    }
                    else -> {
                        Text(
                            text = "No hay comprobante disponible para este pago.",
                            style = bodySmall(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!canOpenProof && !canDownload) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "La vista previa y descarga estarán disponibles cuando el pago sea aprobado y tenga comprobante válido.",
                        style = bodySmall(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (canOpenProof) {
                        ButtonM(
                            modifier = Modifier.weight(1f),
                            onClick = onOpenPreview
                        ) {
                            Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ver comprobante")
                        }
                    }
                    if (canDownload) {
                        OutlinedButtonM(
                            modifier = Modifier.weight(1f),
                            onClick = onDownload
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Descargar comprobante")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AchTimelineCard(detail: AchPaymentDetail) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Timeline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Text("Timeline del pago", style = bodyMediumBold())
            }
            if (detail.timeline.isEmpty()) {
                Text(
                    text = "Sin eventos disponibles.",
                    style = bodySmall(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                detail.timeline.forEachIndexed { index, item ->
                    val (dotColor, lineColor) = timelineDotPalette(item.status)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            if (index < detail.timeline.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(52.dp)
                                        .background(lineColor)
                                )
                            }
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.title.ifBlank { item.status.ifBlank { "Evento" } },
                                    style = bodyMediumBold(),
                                    modifier = Modifier.weight(1f)
                                )
                                if (item.status.isNotBlank()) {
                                    TimelineStatusChip(item.status)
                                }
                            }
                            if (item.message.isNotBlank()) {
                                Text(
                                    text = item.message,
                                    style = bodySmall(),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (item.at.isNotBlank()) {
                                Text(
                                    text = item.at,
                                    style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    }
                    if (index < detail.timeline.lastIndex) {
                        Spacer(Modifier.height(2.dp))
                    }
                }
            }
        }
    }
}

private fun timelineDotPalette(status: String?): Pair<Color, Color> {
    return when (normalizeStatus(status.orEmpty())) {
        "approved", "paid", "succeeded", "completed" -> Color(0xFF2E7D32) to Color(0xFF2E7D32).copy(alpha = 0.35f)
        "rejected", "declined", "cancelled" -> Color(0xFFD32F2F) to Color(0xFFD32F2F).copy(alpha = 0.35f)
        "pending_review", "requires_action", "pending", "processing", "created", "proof_uploaded", "ocr_processed" ->
            Color(0xFFF57F17) to Color(0xFFF57F17).copy(alpha = 0.35f)
        else -> Color(0xFF546E7A) to Color(0xFF546E7A).copy(alpha = 0.25f)
    }
}

private fun statusPalette(rawStatus: String?): Pair<Color, Color> {
    return when (normalizeStatus(rawStatus.orEmpty())) {
        "approved", "paid", "succeeded", "completed" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        "rejected", "declined", "cancelled" -> Color(0xFFFFEBEE) to Color(0xFFD32F2F)
        "pending_review", "requires_action", "pending", "processing", "created" -> Color(0xFFFFF8E1) to Color(0xFFF57F17)
        else -> Color(0xFFECEFF1) to Color(0xFF455A64)
    }
}

private fun riskPalette(rawRisk: String?): Pair<Color, Color> {
    return when (normalizeStatus(rawRisk.orEmpty())) {
        "high", "alto" -> Color(0xFFFFEBEE) to Color(0xFFD32F2F)
        "medium", "medio" -> Color(0xFFFFF8E1) to Color(0xFFF57F17)
        "low", "bajo" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        else -> Color(0xFFECEFF1) to Color(0xFF455A64)
    }
}

private fun isComparisonMismatch(expected: String, detected: String): Boolean {
    val normalizedExpected = normalizeCompareToken(expected)
    val normalizedDetected = normalizeCompareToken(detected)
    if (normalizedExpected.isBlank() || normalizedDetected.isBlank()) return false
    return normalizedExpected != normalizedDetected
}

private fun normalizeCompareToken(value: String): String {
    return value.lowercase()
        .replace("usd", "")
        .replace("pab", "")
        .replace("no disponible", "")
        .replace("-", "")
        .replace(" ", "")
        .replace("\u00A0", "")
        .replace(Regex("[^a-z0-9]"), "")
}

@Composable
private fun TimelineStatusChip(rawStatus: String) {
    val normalized = normalizeStatus(rawStatus)
    val (bg, fg, label) = when (normalized) {
        "approved", "paid", "succeeded", "completed" -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Aprobado")
        "rejected", "declined", "cancelled" -> Triple(Color(0xFFFFEBEE), Color(0xFFD32F2F), "Rechazado")
        "pending_review", "requires_action", "pending", "processing", "created", "proof_uploaded", "ocr_processed" ->
            Triple(Color(0xFFFFF8E1), Color(0xFFF57F17), "En revisión")
        "checkout_created" ->
            Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Checkout Created")
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            rawStatus.replace('_', ' ').replaceFirstChar { it.uppercase() }
        )
    }
    Text(
        text = label,
        style = labelSmall(color = fg),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

private fun decisionLabel(rawDecision: String?): String {
    return when (normalizeStatus(rawDecision.orEmpty())) {
        "reject_suspected", "reject" -> "Rechazar"
        "approve_suggested", "approve", "approved" -> "Aprobar"
        else -> "Sin recomendación"
    }
}

private fun riskLabel(rawRisk: String?): String {
    return when (normalizeStatus(rawRisk.orEmpty())) {
        "high", "alto" -> "Riesgo Alto"
        "medium", "medio" -> "Riesgo Medio"
        "low", "bajo" -> "Riesgo Bajo"
        else -> "Sin nivel"
    }
}

private fun String.toOrderLabel(): String {
    if (isBlank()) return "No disponible"
    return if (startsWith("#")) this else "#$this"
}

private fun formatAmount(currencyCode: String, value: String): String {
    if (value.isBlank()) return "-"
    if (value.contains(currencyCode, ignoreCase = true)) return value
    val numeric = value.replace(",", "").trim()
    val formatted = runCatching { formatNumberToMoney(numeric) }.getOrElse { value }
    return "$currencyCode $formatted"
}

private fun formatAchDateTime(rawDate: String?): String {
    if (rawDate.isNullOrBlank()) return "-"
    val normalized = rawDate.trim()
        .removeSuffix("Z")
        .substringBefore(".")
    val formatted = runCatching {
        DateFormat.getFormattedDate(
            normalized,
            "yyyy-MM-dd'T'HH:mm:ss",
            "dd/MM/yyyy HH:mm"
        )
    }.getOrNull().orEmpty()
    return formatted.takeIf { it.isNotBlank() && it != "00-00-0000 00:00" } ?: rawDate
}

private fun normalizeStatus(raw: String): String {
    return raw.trim()
        .lowercase()
        .replace('-', '_')
        .replace(' ', '_')
}

private fun parseJsonObject(raw: String?): JsonObject? {
    if (raw.isNullOrBlank()) return null
    return runCatching {
        Json.parseToJsonElement(raw).let { element ->
            element as? JsonObject
        }
    }.getOrNull()
}

private fun readFirstString(objects: List<JsonObject?>, keys: List<String>): String? {
    return objects.firstNotNullOfOrNull { obj ->
        keys.firstNotNullOfOrNull { key ->
            readStringValue(obj, key)
        }
    }
}

private fun readStringValue(obj: JsonObject?, key: String): String? {
    if (obj == null) return null
    val direct = obj[key] ?: return null
    return when (direct) {
        is JsonPrimitive -> direct.contentOrNull
        is JsonObject -> {
            listOf("value", "label", "title", "name", "text", "message")
                .firstNotNullOfOrNull { nestedKey ->
                    (direct[nestedKey] as? JsonPrimitive)?.contentOrNull
                }
        }
        else -> null
    }?.takeIf { it.isNotBlank() }
}

private fun extractStringList(element: JsonElement?): List<String> {
    return when (element) {
        is JsonArray -> element.mapNotNull { item ->
            when (item) {
                is JsonPrimitive -> item.contentOrNull?.takeIf { it.isNotBlank() }
                is JsonObject -> listOf("label", "title", "name", "factor", "reason", "message", "description", "text")
                    .firstNotNullOfOrNull { key ->
                        (item[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
                    }
                else -> null
            }
        }
        is JsonPrimitive -> listOfNotNull(element.contentOrNull?.takeIf { it.isNotBlank() })
        else -> emptyList()
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = value,
            style = bodyMedium(),
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.65f)
        )
    }
}

@Composable
private fun AchReviewLoadingSkeleton() {
    val brush = shimmerBrush()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(brush)
        )
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(brush)
            )
        }
    }
}

@Composable
private fun AchReviewError(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = bodyMedium(),
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        ButtonM(
            modifier = Modifier.widthIn(min = 180.dp),
            onClick = onRetry
        ) {
            Text("Reintentar")
        }
    }
}
