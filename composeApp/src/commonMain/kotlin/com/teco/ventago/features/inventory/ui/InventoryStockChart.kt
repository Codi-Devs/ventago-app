package com.teco.ventago.features.inventory.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.features.inventory.domain.InventoryKardexSupport
import com.teco.ventago.features.inventory.domain.StockChartPoint
import kotlin.math.abs

@Composable
fun InventoryStockChart(
    points: List<StockChartPoint>,
    minQty: String?,
    modifier: Modifier = Modifier,
) {
    if (points.size < 2) {
        Text(
            text = "Sin histórico de stock todavía.",
            style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = modifier,
        )
        return
    }

    var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }
    val accent = MaterialTheme.colorScheme.secondary
    val grid = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val minLine = MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
    val minValue = minQty?.trim()?.toDoubleOrNull()?.takeIf { it > 0.0 }

    val values = points.map { it.qty }
    var minY = values.minOrNull() ?: 0.0
    var maxY = values.maxOrNull() ?: 0.0
    minValue?.let {
        minY = minOf(minY, it)
        maxY = maxOf(maxY, it)
    }
    if (minY == maxY) {
        minY -= 1.0
        maxY += 1.0
    }
    val range = (maxY - minY).coerceAtLeast(0.001)
    val yTicks = (0..4).map { step ->
        maxY - (range * step / 4.0)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(188.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .width(40.dp)
                    .height(180.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                yTicks.forEach { tick ->
                    Text(
                        InventoryKardexSupport.formatQty(tick),
                        style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant).copy(
                            fontFeatureSettings = "tnum",
                        ),
                        textAlign = TextAlign.End,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp)
                    .pointerInput(points, minY, maxY, range) {
                        detectTapGestures { offset ->
                            val stepX = size.width / (points.size - 1).coerceAtLeast(1)
                            fun yFor(value: Double): Float {
                                val ratio = ((value - minY) / range).toFloat().coerceIn(0f, 1f)
                                return size.height - (ratio * size.height)
                            }
                            var nearest = 0
                            var nearestDist = Float.MAX_VALUE
                            points.forEachIndexed { index, point ->
                                val x = stepX * index
                                val y = yFor(point.qty)
                                val dist = abs(offset.x - x) + abs(offset.y - y)
                                if (dist < nearestDist) {
                                    nearestDist = dist
                                    nearest = index
                                }
                            }
                            selectedIndex = if (nearestDist <= 48f) nearest else null
                        }
                    },
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val stepX = size.width / (points.size - 1).coerceAtLeast(1)
                    fun yFor(value: Double): Float {
                        val ratio = ((value - minY) / range).toFloat().coerceIn(0f, 1f)
                        return size.height - (ratio * size.height)
                    }

                    for (i in 0..4) {
                        val y = size.height * i / 4f
                        drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    }

                    minValue?.let {
                        val y = yFor(it)
                        drawLine(minLine, Offset(0f, y), Offset(size.width, y), strokeWidth = 2f)
                    }

                    val path = Path()
                    points.forEachIndexed { index, point ->
                        val x = stepX * index
                        val y = yFor(point.qty)
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = accent,
                        style = Stroke(width = 3f, cap = StrokeCap.Round),
                    )

                    points.forEachIndexed { index, point ->
                        val x = stepX * index
                        val y = yFor(point.qty)
                        val selected = selectedIndex == index
                        if (selected) {
                            drawCircle(accent.copy(alpha = 0.2f), radius = 10f, center = Offset(x, y))
                        }
                        drawCircle(
                            color = accent,
                            radius = if (selected) 6f else 4f,
                            center = Offset(x, y),
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 40.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                points.first().label,
                style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
                maxLines = 1,
            )
            Text(
                points.last().label,
                style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
                maxLines = 1,
                textAlign = TextAlign.End,
            )
        }

        selectedIndex?.let { index ->
            val point = points[index]
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accent.copy(alpha = 0.12f),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        point.label,
                        style = labelSmall(accent),
                    )
                    Text(
                        "${InventoryKardexSupport.formatQty(point.qty)} und",
                        style = bodyMediumBold(accent),
                    )
                }
            }
        } ?: Text(
            "Toca un punto para ver fecha y cantidad.",
            style = labelSmall(MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.padding(start = 40.dp),
        )
    }
}
