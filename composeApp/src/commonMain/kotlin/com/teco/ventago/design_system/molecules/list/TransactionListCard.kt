package com.teco.ventago.design_system.molecules.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.theme.Gray80
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.labelMedium

@Composable
fun TransactionListCard(
    icon: ImageVector,
    iconContentDescription: String?,
    iconTint: Color,
    headline: String,
    supportingLines: List<String>,
    trailingPrimary: String,
    onClick: () -> Unit,
    trailingContent: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = Gray80,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = iconContentDescription,
                    tint = iconTint,
                    modifier = Modifier.padding(10.dp),
                )
            }

            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
            ) {
                Text(
                    text = headline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = bodyMediumBold(color = MaterialTheme.colorScheme.onSurface),
                )
                supportingLines.filter { it.isNotBlank() }.forEach { line ->
                    Text(
                        modifier = Modifier.padding(top = 2.dp),
                        text = line,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = labelMedium(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = trailingPrimary,
                    maxLines = 1,
                    style = bodyMediumBold(color = MaterialTheme.colorScheme.onSurface),
                )
                trailingContent()
            }
        }
    }
}
