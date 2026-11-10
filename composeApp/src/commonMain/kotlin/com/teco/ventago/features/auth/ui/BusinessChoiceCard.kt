package com.teco.ventago.features.auth.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.latoFontFamily
import org.jetbrains.compose.resources.painterResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.ic_arrow_forward_ios

fun businessRoleLabel(role: String): String {
    return when (role.trim().lowercase()) {
        "owner", "dueño", "dueno" -> "Dueño"
        "staff", "colaborador" -> "Colaborador"
        else -> role.trim()
    }
}

@Composable
fun BusinessChoiceCard(
    name: String,
    role: String,
    logoUrl: String,
    current: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val secondary = MaterialTheme.colorScheme.secondary
    OutlinedCard(
        onClick = { if (!current) onClick() },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (current) secondary.copy(alpha = 0.08f) else cardContainerColor(),
            disabledContainerColor = if (current) secondary.copy(alpha = 0.08f) else cardContainerColor(),
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (current) secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        ),
        elevation = CardDefaults.outlinedCardElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BusinessMark(name = name, logoUrl = logoUrl)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name.ifBlank { "Negocio" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = latoFontFamily(),
                    fontWeight = FontWeight.W700,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val roleLabel = businessRoleLabel(role)
                if (roleLabel.isNotEmpty()) {
                    Text(
                        text = roleLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = latoFontFamily(),
                        fontWeight = FontWeight.W600,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (current) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Negocio actual",
                    tint = secondary,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Icon(
                    painter = painterResource(Res.drawable.ic_arrow_forward_ios),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun BusinessMark(name: String, logoUrl: String) {
    val shape = RoundedCornerShape(4.dp)
    val logo = logoUrl.trim().takeIf { it.isNotEmpty() && it != "null" }
    var failed by remember(logo) { mutableStateOf(false) }
    if (logo != null && !failed) {
        AsyncImage(
            model = logo,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            onError = { failed = true },
            modifier = Modifier
                .size(40.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
    } else {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.trim().firstOrNull()?.uppercase() ?: "N",
                fontFamily = latoFontFamily(),
                fontWeight = FontWeight.W700,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
