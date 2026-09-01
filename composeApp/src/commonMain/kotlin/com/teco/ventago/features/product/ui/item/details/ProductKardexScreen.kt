package com.teco.ventago.features.product.ui.item.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.features.inventory.domain.InventoryKardexSupport
import com.teco.ventago.features.inventory.domain.KardexMovementRow
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProductKardexScreen(
    onOpenOrderDetails: (String) -> Unit = {},
    viewModel: ProductKardexViewModel = koinViewModel(),
) {
    val ui by viewModel.uiState.collectAsState()
    var selectedMovement by remember { mutableStateOf<KardexMovementRow?>(null) }
    val selectedDetail = selectedMovement?.let { InventoryKardexSupport.buildMovementDetail(it) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        KardexHeader(
            itemName = ui.itemName,
            totalMovements = ui.totalMovements,
            hasMoreOnServer = ui.hasMoreOnServer,
        )

        when {
            ui.loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            ui.displayRows.isEmpty() -> {
                KardexEmptyState(modifier = Modifier.weight(1f))
            }

            else -> {
                Card(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(ui.pagedRows, key = { "${it.movementId}:${it.lineNo}:${it.locationId}" }) { row ->
                            KardexMovementRowItem(
                                row = row,
                                onClick = { selectedMovement = row },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                        }
                    }
                }

                KardexPaginationBar(
                    ui = ui,
                    onPrevious = { viewModel.goToPage(-1) },
                    onNext = { viewModel.goToPage(1) },
                    onLoadMore = { viewModel.goToPage(1) },
                )
            }
        }

        if (ui.loadingMore) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Cargando movimientos…",
                    style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }
        }

        ui.errorMessage?.let { message ->
            Text(
                message,
                style = bodyMedium(MaterialTheme.colorScheme.error),
            )
        }
    }

    selectedDetail?.let { detail ->
        KardexMovementDetailDialog(
            detail = detail,
            onDismiss = { selectedMovement = null },
            onOpenOrder = onOpenOrderDetails,
        )
    }
}

@Composable
private fun KardexHeader(
    itemName: String,
    totalMovements: Int,
    hasMoreOnServer: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text("Kardex", style = titleMediumBold())
        }
        if (itemName.isNotBlank()) {
            Text(
                itemName,
                style = bodyMedium(MaterialTheme.colorScheme.onSurfaceVariant),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (totalMovements > 0) {
            val suffix = if (hasMoreOnServer) "+" else ""
            Text(
                "$totalMovements$suffix movimientos",
                style = labelSmall(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
private fun KardexEmptyState(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Outlined.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(40.dp),
            )
            Text(
                "Sin movimientos de inventario",
                style = bodyMediumBold(),
                textAlign = TextAlign.Center,
            )
            Text(
                "Los movimientos de entrada, salida y ajustes aparecerán aquí.",
                style = bodyMedium(MaterialTheme.colorScheme.onSurfaceVariant),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun KardexMovementRowItem(
    row: KardexMovementRow,
    onClick: () -> Unit,
) {
    val sense = InventoryKardexSupport.kardexSenseLabel(row)
    val accent = kardexAccentForSense(sense)
    val qtyText = InventoryKardexSupport.kardexQtyDisplay(row)
    val location = row.locationName
        ?: row.locationCode
        ?: row.locationId.takeIf { it > 0 }?.toString().orEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(accent),
        )
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    InventoryKardexSupport.formatMovementDate(row.occurredAt, row.postedAt),
                    style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
                )
                KardexSenseChip(label = sense, accent = accent)
            }
            Text(
                row.displayType ?: InventoryKardexSupport.movementTypeLabel(row.movementType),
                style = bodyMediumBold(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (location.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Place,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        location,
                        style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            qtyText,
            style = kardexNumericStyle(accent.takeIf { sense != "Valuación" }),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun KardexSenseChip(
    label: String,
    accent: Color,
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = accent.copy(alpha = 0.12f),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = labelSmall(accent),
        )
    }
}

@Composable
private fun KardexPaginationBar(
    ui: ProductKardexUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onLoadMore: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                ui.movementRangeText,
                style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = onPrevious,
                    enabled = ui.hasPreviousPage && !ui.loadingMore,
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                        contentDescription = "Página anterior",
                        tint = if (ui.hasPreviousPage) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        },
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                ) {
                    Text(
                        ui.pageIndicatorText,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = kardexNumericStyle(),
                        textAlign = TextAlign.Center,
                    )
                }

                when {
                    ui.showLoadMore -> {
                        TextButton(
                            onClick = onLoadMore,
                            enabled = !ui.loadingMore,
                        ) {
                            Text(
                                "Cargar más",
                                style = bodyMediumBold(MaterialTheme.colorScheme.primary),
                            )
                        }
                    }

                    else -> {
                        IconButton(
                            onClick = onNext,
                            enabled = ui.hasNextLocalPage && !ui.loadingMore,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                contentDescription = "Página siguiente",
                                tint = if (ui.hasNextLocalPage) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun kardexAccentForSense(sense: String): Color {
    return when (sense) {
        "Entrada" -> MaterialTheme.colorScheme.secondary
        "Salida" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
private fun kardexNumericStyle(color: Color? = null): TextStyle {
    return bodyMediumBold(color).copy(fontFeatureSettings = "tnum")
}
