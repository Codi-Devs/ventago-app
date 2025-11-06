package com.teco.ventago.design_system.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.features.product.domain.model.Category
import org.jetbrains.compose.resources.stringResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.items


@Composable
fun CategoryItem(
    modifier: Modifier = Modifier,
    category: Category,
    reordering: Boolean = false,
    onClick: (Int) -> Unit = {},
    onOptionsClick: (Int) -> Unit = {},
) {

    ListRowCard(
        modifier = modifier,
        onClick = { onClick(category.id) },
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
                    CountBadge(category.items.size, stringResource(Res.string.items))
                }
            }
        },
        contentSlot = {
            Text(
                text = category.name,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = bodyMediumBold()
            )
            StatusChip(active = category.active)
        },
        trailingSlot = {
            if (!reordering) {
                IconButton(onClick = { onOptionsClick(category.id) }) {
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