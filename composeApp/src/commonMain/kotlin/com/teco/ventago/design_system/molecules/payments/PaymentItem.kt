package com.teco.ventago.design_system.molecules.payments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Badge
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.labelSmall
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.enabled
import ventago.composeapp.generated.resources.ic_arrow_forward_ios

@Composable
fun PaymentItem(
    modifier: Modifier = Modifier,
    itemId: String,
    title: @Composable (RowScope.() -> Unit),
    enabled: Boolean,
    onClick: (String) -> Unit = {}
) {

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .height(74.dp)
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor(),
        ),
        shape = RoundedCornerShape(8.dp),
        onClick = {
            onClick(itemId)
        }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            title()
            Spacer(modifier = Modifier.weight(1f))
            if (enabled) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = stringResource(Res.string.enabled),
                        style = labelSmall(color = MaterialTheme.colorScheme.onPrimary),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
            IconButton(
                onClick = {
                    onClick(itemId)
                },
                enabled = true,
            ) {
                Icon(
                    modifier = Modifier
                        .size(width = 20.dp, height = 20.dp)
                        .padding(end = 8.dp),
                    painter = painterResource(Res.drawable.ic_arrow_forward_ios),
                    contentDescription = "Action",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

}


@Composable
fun CheckMarkItem(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            modifier = Modifier
                .padding(end = 8.dp)
                .size(16.dp),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text,
            style = bodyMedium(),
            textAlign = TextAlign.Start,
        )
    }
}