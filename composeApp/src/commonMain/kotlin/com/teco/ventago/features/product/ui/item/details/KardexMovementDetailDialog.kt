package com.teco.ventago.features.product.ui.item.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.features.inventory.domain.KardexMovementDetail

@Composable
fun KardexMovementDetailDialog(
    detail: KardexMovementDetail,
    onDismiss: () -> Unit,
    onOpenOrder: (String) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(detail.typeLabel, style = titleMediumBold())

                KardexDetailHero(detail)

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    KardexDetailField("Fecha", detail.dateLabel)
                    KardexDetailField("Tipo", detail.typeLabel)
                    KardexDetailField("Ubicación", detail.locationLabel)
                    detail.valuationNote?.let {
                        KardexDetailField("Efecto", it)
                    }
                    detail.adjustmentReason?.let {
                        KardexDetailField("Motivo del ajuste", it)
                    }
                    detail.comment?.let {
                        KardexDetailField("Comentario", it)
                    }
                    detail.orderRef?.let { order ->
                        KardexDetailField(
                            label = "Orden relacionada",
                            valueContent = {
                                TextButton(
                                    onClick = {
                                        onDismiss()
                                        onOpenOrder(order.orderNumber)
                                    },
                                ) {
                                    Text(
                                        order.label,
                                        style = bodyMediumBold(MaterialTheme.colorScheme.primary),
                                    )
                                }
                            },
                        )
                    }
                    detail.purchaseOrderRef?.let { purchaseOrder ->
                        KardexDetailField(
                            label = "Orden de compra",
                            value = "OC #${purchaseOrder.purchaseOrderId}",
                        )
                    }
                    detail.genericReference?.let {
                        KardexDetailField("Referencia", it)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButtonS(label = "Cerrar", onClick = onDismiss)
                }
            }
        }
    }
}

@Composable
private fun KardexDetailHero(detail: KardexMovementDetail) {
    val accent = when {
        detail.isValuation -> MaterialTheme.colorScheme.onSurfaceVariant
        detail.isInbound -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }
    val chipBg = accent.copy(alpha = 0.12f)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = chipBg,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (detail.isValuation) "Cambio de stock" else "Cantidad",
                    style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
                )
                Text(
                    detail.quantityDisplay,
                    style = kardexDetailNumericStyle(accent),
                )
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
            ) {
                Text(
                    detail.senseLabel,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = labelSmall(accent),
                )
            }
        }
    }
}

@Composable
private fun KardexDetailField(
    label: String,
    value: String? = null,
    valueContent: (@Composable () -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant))
        if (valueContent != null) {
            valueContent()
        } else {
            Text(
                value.orEmpty(),
                style = bodyMedium(),
            )
        }
    }
}

@Composable
private fun kardexDetailNumericStyle(color: Color): TextStyle {
    return bodyMediumBold(color).copy(
        fontFeatureSettings = "tnum",
        textAlign = TextAlign.Start,
    )
}
