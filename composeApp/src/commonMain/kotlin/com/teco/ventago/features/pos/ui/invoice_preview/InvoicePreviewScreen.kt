package com.teco.ventago.features.pos.ui.invoice_preview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.bodySmall
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.features.pos.ui.viewmodel.PosViewModel
import com.teco.ventago.utils.formatNumberToMoney
import com.teco.ventago.utils.getImageRequest
import kotlin.math.abs

@Composable
fun InvoicePreviewScreen(
    viewModel: PosViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val business = viewModel.business
    val preview = remember(uiState, business) {
        InvoicePreviewBuilder.build(uiState, business)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PreviewWarningBanner()
        HeaderCard(preview)
        ReceptorAndMetaCard(preview)
        ItemsCard(preview.items)
        TaxAndPaymentCard(preview)
        TotalsCard(preview.totals)
        OutlinedButtonM(
            onClick = onBack,
            modifier = Modifier.padding(bottom = 8.dp),
            contentColor = MaterialTheme.colorScheme.secondary,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
        ) {
            Text("Volver al pago")
        }
    }
}

@Composable
private fun PreviewWarningBanner() {
    val amber = Color(0xFFFFB300)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF3CD), RoundedCornerShape(4.dp))
            .dashedBorder(color = amber, radius = 4.dp.value)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "VISTA PREVIA - Este documento no es una factura oficial",
            style = bodyMediumBold(color = Color(0xFF856404)),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HeaderCard(preview: InvoicePreview) {
    PreviewCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IssuerLogo(preview.issuer)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("DGI", style = titleMediumBold(), textAlign = TextAlign.Center)
                Text(
                    "Comprobante auxiliar de factura electronica",
                    style = bodyMediumBold(),
                    textAlign = TextAlign.Center,
                )
                Text(
                    preview.meta.invoiceTypeTitle,
                    style = bodyMediumBold(),
                    textAlign = TextAlign.Center,
                )
            }
            QrPlaceholder()
        }
        Divider(Modifier.padding(vertical = 12.dp))
        SectionTitle("Emisor", Icons.Outlined.Business)
        InfoRow("Emisor", preview.issuer.name.ifBlank { "N/A" })
        InfoRow("RUC", preview.issuer.ruc.ifBlank { "N/A" })
        InfoRow("Direccion", preview.issuer.address, maxLines = 2)
    }
}

@Composable
private fun IssuerLogo(issuer: InvoicePreviewIssuer) {
    Box(
        modifier = Modifier
            .size(width = 74.dp, height = 58.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (issuer.logoUrl.isNotBlank() && !issuer.logoUrl.equals("null", ignoreCase = true)) {
            AsyncImage(
                model = getImageRequest(LocalPlatformContext.current, issuer.logoUrl),
                contentDescription = "Logo del negocio",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(6.dp)),
            )
        } else {
            Text(
                text = issuer.name.ifBlank { "DGI" },
                style = bodyMediumBold(color = MaterialTheme.colorScheme.primary),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun QrPlaceholder() {
    Box(
        modifier = Modifier
            .size(74.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Vista previa\nSin codigo QR",
            style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ReceptorAndMetaCard(preview: InvoicePreview) {
    PreviewCard {
        SectionTitle("Receptor", Icons.Outlined.Person)
        InfoRow("Tipo de Receptor", preview.receptor.type)
        InfoRow("Cliente", preview.receptor.name.ifBlank { "N/A" }, maxLines = 2)
        InfoRow("Identificacion", preview.receptor.identification.ifBlank { "N/A" })

        Divider(Modifier.padding(vertical = 12.dp))
        SectionTitle("Datos fiscales", Icons.Outlined.Info)
        InfoRow("Numero", preview.meta.number)
        InfoRow("Fecha de emision", preview.meta.issuedAt)
        InfoRow("Punto de Facturacion", preview.meta.billingPoint.ifBlank { "N/A" })
        InfoRow("Consulta", preview.meta.consultationUrl, maxLines = 2)
        InfoRow("CUFE", preview.meta.cufe, maxLines = 2, italicValue = true)
    }
}

@Composable
private fun ItemsCard(items: List<InvoicePreviewItem>) {
    PreviewCard {
        SectionTitle("Items", Icons.Outlined.Info)
        if (items.isEmpty()) {
            EmptyText("Sin productos")
            return@PreviewCard
        }

        items.forEachIndexed { index, item ->
            if (index > 0) Divider(Modifier.padding(vertical = 10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = item.number.toString(),
                    style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.width(24.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.description, style = bodyMedium())
                    if (item.detail.isNotBlank()) {
                        Text(
                            item.detail,
                            style = bodySmall(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${item.quantity} ${item.unit} x ${money(item.unitPriceCents)}",
                        style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                    Text(
                        "Desc. unit. ${money(item.discountPerUnitCents)} - ITBMS ${money(item.itbmsAmountCents)}",
                        style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                }
                Text(
                    money(item.totalCents),
                    style = bodyMediumBold(),
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun TaxAndPaymentCard(preview: InvoicePreview) {
    PreviewCard {
        SectionTitle("Desglose ITBMS", Icons.Outlined.Info)
        if (preview.itbmsBreakdown.isEmpty()) {
            EmptyText("Sin desglose")
        } else {
            preview.itbmsBreakdown.forEach { row ->
                TripleRow(
                    first = money(row.baseCents),
                    second = row.rateLabel,
                    third = money(row.taxCents),
                )
            }
            Divider(Modifier.padding(vertical = 8.dp))
            TripleRow(
                first = "",
                second = "Total",
                third = money(preview.totals.itbmsCents),
                bold = true,
            )
        }

        Divider(Modifier.padding(vertical = 12.dp))
        SectionTitle("Forma de Pago", Icons.Outlined.Payments)
        if (preview.payments.isEmpty()) {
            EmptyText("Sin pagos registrados")
        } else {
            preview.payments.forEach { payment ->
                InfoRow(payment.label, money(payment.amountCents))
            }
        }
    }
}

@Composable
private fun TotalsCard(totals: InvoicePreviewTotals) {
    PreviewCard {
        SectionTitle("Totales", Icons.Outlined.Info)
        TotalRow("Total Neto", totals.netTotalCents)
        TotalRow("Monto Exento ITBMS", totals.exemptItbmsCents)
        TotalRow("Monto Gravado ITBMS", totals.taxableItbmsCents)
        TotalRow("ITBMS", totals.itbmsCents)
        if (totals.iscCents > 0) TotalRow("ISC", totals.iscCents)
        if (totals.otiCents > 0) TotalRow("OTI", totals.otiCents)
        TotalRow("Total Impuesto", totals.totalTaxCents)
        if (totals.discountCents > 0) TotalRow("Descuento", -totals.discountCents)
        if (totals.freightCents > 0) TotalRow("Acarreos", totals.freightCents)
        if (totals.insuranceCents > 0) TotalRow("Seguros", totals.insuranceCents)
        if (totals.otherChargesCents > 0) TotalRow("Otros Cargos", totals.otherChargesCents)
        Divider(Modifier.padding(vertical = 10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Total", style = bodyMediumBold(color = MaterialTheme.colorScheme.secondary))
            Text(
                money(totals.totalCents),
                style = bodyMediumBold(color = MaterialTheme.colorScheme.secondary),
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun PreviewCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content,
        )
    }
}

@Composable
private fun SectionTitle(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val sectionColor = MaterialTheme.colorScheme.secondary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = sectionColor,
            modifier = Modifier.size(20.dp),
        )
        Text(title, style = bodyMediumBold(color = sectionColor))
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    maxLines: Int = 1,
    italicValue: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.weight(0.42f),
        )
        Text(
            value,
            style = bodyMedium().copy(
                fontStyle = if (italicValue) FontStyle.Italic else FontStyle.Normal,
                color = if (italicValue) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            ),
            textAlign = TextAlign.End,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.58f),
        )
    }
}

@Composable
private fun TripleRow(
    first: String,
    second: String,
    third: String,
    bold: Boolean = false,
) {
    val style = if (bold) {
        bodyMediumBold(color = MaterialTheme.colorScheme.secondary)
    } else {
        bodyMedium()
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(first, style = style, modifier = Modifier.weight(1f))
        Text(second, style = style, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
        Text(third, style = style, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
    }
}

@Composable
private fun TotalRow(label: String, amountCents: Long) {
    InfoRow(label, money(amountCents))
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text,
        style = bodySmall(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun money(cents: Long): String {
    val formatted = formatNumberToMoney((abs(cents) / 100.0).toString())
    return if (cents < 0) "-$formatted" else formatted
}

private fun Modifier.dashedBorder(color: Color, radius: Float): Modifier {
    return drawBehind {
        val strokeWidth = 2.dp.toPx()
        drawRoundRect(
            color = color,
            style = Stroke(
                width = strokeWidth,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f),
            ),
            cornerRadius = CornerRadius(radius.dp.toPx(), radius.dp.toPx()),
        )
    }
}
