package com.teco.ventago.features.product.ui.item.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.teco.ventago.design_system.buttons.ButtonM
import com.teco.ventago.design_system.buttons.OutlinedButtonM
import com.teco.ventago.design_system.theme.Online
import com.teco.ventago.design_system.theme.WarningAmber
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.titleMediumBold
import com.teco.ventago.features.inventory.domain.ProductInventoryDetails
import com.teco.ventago.features.inventory.ui.ProductInventoryDetailsSection
import com.teco.ventago.features.product.domain.model.Item
import com.teco.ventago.features.product.domain.model.ProductType
import com.teco.ventago.features.product.ui.item.add.viewmodel.AdditionalEntryUI
import com.teco.ventago.utils.formatNumberToMoney
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.edit

@Composable
fun ProductDetailsScreen(
    viewModel: ProductDetailsViewModel = koinViewModel(),
    onEdit: () -> Unit,
    onOpenKardex: () -> Unit,
) {
    val ui by viewModel.uiState.collectAsState()
    val item = ui.item

    if (ui.loading && item == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Cargando producto…", style = bodyMedium())
        }
        return
    }

    if (item == null) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(ui.errorMessage ?: "Producto no encontrado.", style = bodyMedium(MaterialTheme.colorScheme.error))
            OutlinedButtonM(onClick = { viewModel.loadFromSelection(force = true) }) {
                Text("Reintentar")
            }
        }
        return
    }

    val margin = resolveProductMargin(item.price, item.cost, ui.inventory)
    val additionalEntries = fiscalAdditionalEntries(item.parseAdditionalEntries())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ProductIdentityCard(item = item, categoryName = ui.categoryName)
        ProductPricingCard(item = item, margin = margin, inventory = ui.inventory)
        ProductTaxCard(taxPercent = item.taxPercent)

        if (additionalEntries.isNotEmpty()) {
            ProductFiscalInfoCard(entries = additionalEntries)
        }

        if (item.description.isNotBlank()) {
            ProductInfoCard(title = "Descripción") {
                Text(item.description, style = bodyMedium())
            }
        }

        ProductInfoCard(title = "Identificación") {
            DetailLine("Tipo", item.productType.description)
            item.sku?.takeIf { it.isNotBlank() }?.let { DetailLine("SKU", it) }
            item.barcode?.takeIf { it.isNotBlank() }?.let { DetailLine("Código de barras", it) }
            DetailLine("Unidad de medida", item.unitMeasureCode.uppercase())
        }

        if (item.productType == ProductType.GOOD) {
            ProductInventoryDetailsSection(
                details = ui.inventory,
                loading = ui.inventoryLoading,
                onOpenKardex = onOpenKardex,
            )
        }

        if (ui.canEdit) {
            ButtonM(
                onClick = onEdit,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.edit))
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ProductIdentityCard(
    item: Item,
    categoryName: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            val imageUrl = item.getImgUrl()
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.name, style = titleMediumBold(), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(categoryName, style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant))
                ProductStatusBadge(active = item.active)
            }
        }
    }
}

@Composable
private fun ProductStatusBadge(active: Boolean) {
    val background = if (active) {
        Online.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
    }
    val content = if (active) Online else MaterialTheme.colorScheme.onSurfaceVariant
    val label = if (active) "Activo" else "Inactivo"
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = background,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = labelSmall(content),
        )
    }
}

@Composable
private fun ProductPricingCard(
    item: Item,
    margin: ProductMarginMeta,
    inventory: ProductInventoryDetails,
) {
    val inventoryAvg = inventory.avgCost.trim().toDoubleOrNull()?.takeIf { it > 0.0 }
    val showInventoryCost = inventory.tracked && inventory.visible && inventoryAvg != null

    ProductInfoCard(title = "Precio y margen") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DetailLine(
                label = "Precio de venta",
                value = formatNumberToMoney(item.price.toString()),
                valueColor = MaterialTheme.colorScheme.secondary,
            )
            when {
                showInventoryCost -> {
                    DetailLine(
                        label = "Costo de inventario",
                        value = formatNumberToMoney(inventoryAvg.toString()),
                    )
                }
                item.cost != null && item.cost > 0.0 -> {
                    DetailLine(
                        label = "Costo de catálogo",
                        value = formatNumberToMoney(item.cost.toString()),
                    )
                }
            }
            if (margin.costSourceLabel == "Promedio de inventario") {
                Text(
                    "Margen calculado con promedio de inventario",
                    style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }
            MarginLine(margin = margin)
        }
    }
}

@Composable
private fun MarginLine(margin: ProductMarginMeta) {
    val marginColor = when {
        margin.color == Color.Unspecified -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> margin.color
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Margen", style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant))
        Text(
            "${formatNumberToMoney(margin.amount.toString())} (${formatMarginPercent(margin.percent)})",
            style = bodyMediumBold(marginColor),
            textAlign = TextAlign.End,
        )
    }
    Text(
        "Sobre precio de venta",
        style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
    )
}

@Composable
private fun ProductTaxCard(taxPercent: Int?) {
    ProductInfoCard(title = "Impuestos") {
        DetailLine("ITBMS", formatItbmsLabel(taxPercent))
    }
}

@Composable
private fun ProductFiscalInfoCard(entries: List<AdditionalEntryUI>) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Información fiscal adicional",
                    style = bodyMediumBold(MaterialTheme.colorScheme.secondary),
                )
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .rotate(rotation),
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    entries.forEach { entry ->
                        DetailLine(entry.title, entry.displayValue)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductInfoCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = bodyMediumBold())
            content()
        }
    }
}

@Composable
private fun DetailLine(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant))
        Text(
            value,
            style = bodyMediumBold(valueColor),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}
