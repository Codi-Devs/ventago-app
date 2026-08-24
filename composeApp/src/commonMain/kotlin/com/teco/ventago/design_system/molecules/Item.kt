package com.teco.ventago.design_system.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.theme.bodySmall
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.features.product.domain.model.Item

@Composable
fun ItemRow(
    modifier: Modifier = Modifier,
    item: Item,
    reordering: Boolean = false,
    currency: String = "USD",
    onClick: (Int) -> Unit = {},
    onOptionsClick: (Int) -> Unit = {},
) {
    ListRowCard(
        modifier = modifier,
        onClick = { onClick(item.itemId) },
        startSlot = {
            Column(
                modifier = Modifier.padding(end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (reordering) {
                    Icon(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                        imageVector = Icons.Rounded.Menu,
                        contentDescription = "",
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    PriceBadge(item.price.toString(), currency)
                }
            }

        },
        contentSlot = {
            Text(
                text = item.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = bodyMediumBold()
            )
            StatusChip(active = item.active)
        },
        trailingSlot = {
            if (!reordering) {
                IconButton(onClick = { onOptionsClick(item.itemId) }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreHoriz,
                        contentDescription = "",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    )

}

@Composable
fun PosItemRow(
    modifier: Modifier = Modifier,
    item: Item,
    currency: String = "USD",
    availabilityLabel: String? = null,
    onClick: (Int) -> Unit = {},
) {

    ListRowCard(
        modifier = modifier,
        onClick = { onClick(item.itemId) },
        startSlot = {
            Column(
                modifier = Modifier.padding(end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.price.toString(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = bodyMediumBold(color = MaterialTheme.colorScheme.secondary)
                )
                Text(
                    text = currency,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = bodySmall(color = MaterialTheme.colorScheme.secondary)
                )
            }
        },
        contentSlot = {
            Column {
                Text(
                    text = item.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = bodyMediumBold()
                )
                if (!availabilityLabel.isNullOrBlank()) {
                    Text(
                        text = availabilityLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = bodySmall(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    )
                }
            }
        },
        trailingSlot = {}
    )
}

@Composable
fun PosItemGridCard(
    modifier: Modifier = Modifier,
    item: Item,
    currency: String = "USD",
    availabilityLabel: String? = null,
    onClick: (Int) -> Unit = {},
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        onClick = { onClick(item.itemId) },
        elevation = CardDefaults.elevatedCardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = item.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = bodyMediumBold()
            )
            if (!availabilityLabel.isNullOrBlank()) {
                Text(
                    text = availabilityLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = bodySmall(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.price.toString(),
                    style = bodyMediumBold(color = MaterialTheme.colorScheme.secondary)
                )
                Text(
                    text = currency,
                    style = bodySmall(color = MaterialTheme.colorScheme.secondary)
                )
            }
        }
    }
}
