package com.teco.ventago.design_system.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teco.ventago.design_system.theme.Gray70
import com.teco.ventago.design_system.theme.bodySmall
import com.teco.ventago.design_system.theme.latoFontFamily
import com.teco.ventago.design_system.theme.titleMediumBold
import org.jetbrains.compose.resources.stringResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.active
import ventago.composeapp.generated.resources.inactive

@Composable
fun ListRowCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    startSlot: @Composable RowScope.() -> Unit,
    contentSlot: @Composable ColumnScope.() -> Unit,
    trailingSlot: @Composable RowScope.() -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        elevation = CardDefaults.elevatedCardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        shape = RoundedCornerShape(10.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp)
                .fillMaxWidth()
                .heightIn(min = 30.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT
            startSlot()

            // Divider
            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .width(1.dp)
                    .heightIn(min = 40.dp)
                    .fillMaxHeight()
                    .background(color = Gray70)
            )

            // CENTER
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                contentSlot()
            }

            // RIGHT
            trailingSlot()
        }
    }
}

/* ---------- Small reusable pieces ---------- */

@Composable
fun CountBadge(
    count: Int,
    label: String,
) {
    Text(
        text = count.toString(),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = titleMediumBold()
    )
    Text(
        text = label,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            fontSize = 9.sp,
            lineHeight = 16.sp,
            fontFamily = latoFontFamily(),
            fontWeight = FontWeight.W400,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun PriceBadge(
    amountText: String,
    currency: String,
) {
    Text(
        text = amountText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = titleMediumBold()
    )
    Text(
        text = currency,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            fontSize = 9.sp,
            lineHeight = 16.sp,
            fontFamily = latoFontFamily(),
            fontWeight = FontWeight.W400,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun DragHandleIcon(modifier: Modifier = Modifier) {
    Icon(
        modifier = modifier.padding(vertical = 12.dp, horizontal = 6.dp),
        imageVector = Icons.Rounded.Menu,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun StatusChip(
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val text = if (active) stringResource(Res.string.active) else stringResource(Res.string.inactive)
    val style = if (active) bodySmall(color = MaterialTheme.colorScheme.primary) else bodySmall()
    Text(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis, style = style, modifier = modifier)
}