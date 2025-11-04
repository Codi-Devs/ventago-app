package com.teco.ventago.design_system.molecules

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.theme.Gray70
import com.teco.ventago.design_system.theme.titleMedium
import com.teco.ventago.design_system.theme.vanishedBackgroundColor

@Composable
fun DMDivider(
    modifier: Modifier = Modifier,
    label: String? = null,
    style: TextStyle = titleMedium(),
    dividerColor: Color = Gray70
) {
    Row (
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .weight(1f)
                .background(color = dividerColor)
        )
        Text(
            text = label ?: "",
            modifier = Modifier
                .wrapContentSize(Alignment.Center)
                .padding(horizontal = 16.dp),
            style = style,
            textAlign = TextAlign.Center,

        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .weight(1f, fill = true)
                .background(color = dividerColor)
        )
    }
}

@Composable
fun InverseTicketDivider(
    modifier: Modifier = Modifier,
    shapeColor: Color = MaterialTheme.colorScheme.background, // Greenish color
    dashColor: Color = MaterialTheme.colorScheme.secondary, // Greenish color
    dashWidth: Float = 10f,
    dashGap: Float = 10f,
    strokeWidth: Float = 3f,
) {
    Row (modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,) {
        Surface(
            modifier = Modifier
                .offset(x = (-15).dp)
                .width(30.dp)
                .height(30.dp),
            shape = CircleShape,
            color = shapeColor
        ) {}
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height((strokeWidth * 2).dp)
                .weight(1f),
        ) {
            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashWidth, dashGap), 0f)
            drawLine(
                color = dashColor,
                start = androidx.compose.ui.geometry.Offset(-15.dp.toPx(), size.height / 2),
                end = androidx.compose.ui.geometry.Offset(size.width + 15.dp.toPx(), size.height / 2),
                strokeWidth = strokeWidth,
                pathEffect = pathEffect
            )
        }
        Surface(
            modifier = Modifier
                .offset(x = 15.dp)
                .width(30.dp)
                .height(30.dp),
            shape = CircleShape,
            color = shapeColor
        ) {}
    }

}

@Composable
fun TicketDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary, // Greenish color
    dashWidth: Float = 10f,
    dashGap: Float = 10f,
    strokeWidth: Float = 3f,
) {
    Row (modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,) {
        Surface(
            modifier = Modifier
                .offset(x = (-15).dp)
                .width(30.dp)
                .height(30.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.background
        ) {}
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height((strokeWidth * 2).dp)
                .weight(1f),
        ) {
            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashWidth, dashGap), 0f)
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(-15.dp.toPx(), size.height / 2),
                end = androidx.compose.ui.geometry.Offset(size.width + 15.dp.toPx(), size.height / 2),
                strokeWidth = strokeWidth,
                pathEffect = pathEffect
            )
        }
        Surface(
            modifier = Modifier
                .offset(x = 15.dp)
                .width(30.dp)
                .height(30.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.background
        ) {}
    }

}

/**
 * Creates a shape with semi-circle cutouts on the left and right.
 */
/**
 * Creates a shape with semi-circle cutouts on the left and right.
 */
fun ticketShape(cutoutRadius: Dp, density: Density) = GenericShape { size, layoutDirection ->
    val radiusPx = with(density) { cutoutRadius.toPx() }

    moveTo(0f, 0f)
    lineTo(0f, size.height / 2 - radiusPx)

    // Left circular cutout
    quadraticBezierTo(
        x1 = -radiusPx, y1 = size.height / 2,
        x2 = 0f, y2 = size.height / 2 + radiusPx
    )

    lineTo(0f, size.height)
    lineTo(size.width, size.height)
    lineTo(size.width, size.height / 2 + radiusPx)

    // Right circular cutout
    quadraticBezierTo(
        x1 = size.width + radiusPx, y1 = size.height / 2,
        x2 = size.width, y2 = size.height / 2 - radiusPx
    )

    lineTo(size.width, 0f)
    close()
}