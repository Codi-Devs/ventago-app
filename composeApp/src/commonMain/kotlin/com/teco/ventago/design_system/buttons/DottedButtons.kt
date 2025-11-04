package com.teco.ventago.design_system.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.theme.primaryLight
import com.teco.ventago.design_system.theme.vanishedBackgroundColor

@Composable
fun DottedButton(
    modifier: Modifier = Modifier,
    corner: Int = 8,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .background(vanishedBackgroundColor(), RoundedCornerShape(corner.dp ))
            .dashedBorder(1.5.dp, MaterialTheme.colorScheme.secondary, corner.dp)
            .clickable { onClick()  },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.
            border( width = 1.dp,
                color = MaterialTheme.colorScheme.secondary,
                shape = RoundedCornerShape(50.dp)),
            imageVector = Icons.Rounded.Add,
            tint = MaterialTheme.colorScheme.secondary,
            contentDescription = "Localized description"
        )
    }
}

fun Modifier.dashedBorder(strokeWidth: Dp, color: Color, cornerRadiusDp: Dp) = composed(
    factory = {
        val density = LocalDensity.current
        val strokeWidthPx = density.run { strokeWidth.toPx() }
        val cornerRadiusPx = density.run { cornerRadiusDp.toPx() }

        this.then(
            Modifier.drawWithCache {
                onDrawBehind {
                    val stroke = Stroke(
                        width = strokeWidthPx,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
                    )

                    drawRoundRect(
                        color = color,
                        style = stroke,
                        cornerRadius = CornerRadius(cornerRadiusPx)
                    )
                }
            }
        )
    }
)