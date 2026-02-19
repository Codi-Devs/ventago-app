package com.teco.ventago.design_system.organism

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teco.ventago.design_system.theme.StatusPending
import com.teco.ventago.design_system.theme.labelSmall
import com.teco.ventago.design_system.theme.latoFontFamily
import com.teco.ventago.utils.formatNumberToMoney
import com.teco.ventago.utils.toDecimalString
import com.teco.ventago.utils.toLongCents
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.no_data

@Composable
fun SalesLineGraphic(
    modifier: Modifier = Modifier,
    data: List<Pair<String, Double>>,
    selectedIndex: Int,
    chartHeight: Dp = 220.dp,
    onItemClick: (Int) -> Unit = {}
) {
    if (data.isEmpty()) {
        Row(
            modifier = modifier
                .height(chartHeight)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.padding(start = 16.dp),
                imageVector = Icons.Outlined.WarningAmber,
                tint = StatusPending,
                contentDescription = ""
            )
            Text(
                modifier = Modifier.padding(start = 8.dp, end = 16.dp),
                text = stringResource(Res.string.no_data),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontFamily = latoFontFamily(),
                    fontWeight = FontWeight.W400,
                    textAlign = TextAlign.Start,
                    letterSpacing = 0.5.sp
                )
            )
        }
        return
    }

    val selected = selectedIndex.coerceIn(0, data.lastIndex)
    val values = data.map { it.second }
    var minValue = min(values.minOrNull() ?: 0.0, 0.0)
    var maxValue = max(values.maxOrNull() ?: 0.0, 0.0)
    if ((maxValue - minValue).absoluteValue < 0.0001) {
        val padding = if (maxValue.absoluteValue < 1.0) 1.0 else maxValue.absoluteValue * 0.1
        minValue -= padding
        maxValue += padding
    }

    val density = LocalDensity.current
    val gridTicks = remember(minValue, maxValue) {
        buildTickValues(min = minValue, max = maxValue, tickCount = 6)
    }
    val scrollState = rememberScrollState()
    val lineColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.17f)
    val markerCenterColor = MaterialTheme.colorScheme.surface
    val tooltipBackground = MaterialTheme.colorScheme.secondary
    val tooltipContentColor = MaterialTheme.colorScheme.onSecondary

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val viewportWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val horizontalPaddingPx = with(density) { 10.dp.toPx() }
        val minStepPx = with(density) { if (data.size <= 12) 24.dp.toPx() else 34.dp.toPx() }
        val fitStepPx = if (data.size <= 1) {
            0f
        } else {
            ((viewportWidthPx - (2f * horizontalPaddingPx)) / (data.size - 1).toFloat()).coerceAtLeast(0f)
        }
        val pointStepPx = max(minStepPx, fitStepPx)
        val contentWidthPx = max(
            viewportWidthPx,
            (2f * horizontalPaddingPx) + (pointStepPx * (data.size - 1).coerceAtLeast(0))
        )
        val chartHeightPx = with(density) { chartHeight.toPx() }
        val contentWidthDp = with(density) { contentWidthPx.toDp() }
        val points = remember(values, minValue, maxValue, contentWidthPx, pointStepPx, horizontalPaddingPx, chartHeightPx) {
            buildChartPoints(
                values = values,
                minValue = minValue,
                maxValue = maxValue,
                widthPx = contentWidthPx,
                heightPx = chartHeightPx,
                horizontalPaddingPx = horizontalPaddingPx,
                pointStepPx = pointStepPx
            )
        }
        val selectedPoint = points[selected]

        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
                    .horizontalScroll(scrollState)
            ) {
                Box(
                    modifier = Modifier
                        .width(contentWidthDp)
                        .fillMaxHeight()
                ) {
                    var tooltipSize by remember { mutableStateOf(IntSize.Zero) }
                    val tooltipMarginPx = with(density) { 8.dp.roundToPx() }
                    val rawTooltipX = selectedPoint.x.roundToInt() - (tooltipSize.width / 2)
                    val tooltipX = rawTooltipX.coerceIn(
                        0,
                        (contentWidthPx.roundToInt() - tooltipSize.width).coerceAtLeast(0)
                    )
                    val tooltipAboveY = selectedPoint.y.roundToInt() - tooltipSize.height - tooltipMarginPx
                    val tooltipBelowY = selectedPoint.y.roundToInt() + tooltipMarginPx
                    val tooltipY = if (tooltipAboveY >= 0) {
                        tooltipAboveY
                    } else {
                        tooltipBelowY.coerceAtMost(
                            (chartHeightPx.roundToInt() - tooltipSize.height).coerceAtLeast(0)
                        )
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(points) {
                                detectTapGestures { tapOffset ->
                                    onItemClick(nearestIndexForX(tapOffset.x, points))
                                }
                            }
                    ) {
                        val strokeWidth = 1.dp.toPx()
                        val chartHeightInner = size.height

                        gridTicks.forEach { tick ->
                            val y = valueToY(
                                value = tick,
                                minValue = minValue,
                                maxValue = maxValue,
                                heightPx = chartHeightInner
                            )
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = strokeWidth
                            )
                        }

                        drawPath(
                            path = buildFillPath(points = points, heightPx = chartHeightInner),
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    lineColor.copy(alpha = 0.25f),
                                    lineColor.copy(alpha = 0.08f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = chartHeightInner
                            )
                        )

                        drawPath(
                            path = buildSmoothLinePath(points),
                            color = lineColor,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        drawLine(
                            color = lineColor.copy(alpha = 0.22f),
                            start = Offset(selectedPoint.x, selectedPoint.y),
                            end = Offset(selectedPoint.x, 0f),
                            strokeWidth = 1.dp.toPx()
                        )

                        drawCircle(
                            color = lineColor,
                            radius = 6.dp.toPx(),
                            center = selectedPoint
                        )
                        drawCircle(
                            color = markerCenterColor,
                            radius = 3.dp.toPx(),
                            center = selectedPoint
                        )
                    }

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(tooltipX, tooltipY) }
                            .onSizeChanged { tooltipSize = it }
                            .clip(RoundedCornerShape(6.dp))
                            .background(tooltipBackground)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = toMoney(data[selected].second),
                            style = labelSmall(color = tooltipContentColor),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .horizontalScroll(scrollState)
            ) {
                Box(
                    modifier = Modifier
                        .width(contentWidthDp)
                        .fillMaxHeight()
                ) {
                    val labelWidth = if (data.size <= 12) 30.dp else 26.dp
                    val labelWidthPx = with(density) { labelWidth.toPx() }
                    data.indices.forEach { index ->
                        val x = points[index].x
                        Text(
                            text = data[index].first,
                            style = labelSmall(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier
                                .width(labelWidth)
                                .offset {
                                    IntOffset(
                                        x = (x - (labelWidthPx / 2f)).roundToInt().coerceIn(
                                            0,
                                            (contentWidthPx - labelWidthPx).roundToInt().coerceAtLeast(0)
                                        ),
                                        y = 0
                                    )
                                }
                        )
                    }
                }
            }
        }
    }
}

private fun buildTickValues(min: Double, max: Double, tickCount: Int): List<Double> {
    if (tickCount <= 1) return listOf(max)
    val step = (max - min) / (tickCount - 1)
    return (0 until tickCount).map { index -> max - (step * index) }
}

private fun buildChartPoints(
    values: List<Double>,
    minValue: Double,
    maxValue: Double,
    widthPx: Float,
    heightPx: Float,
    horizontalPaddingPx: Float,
    pointStepPx: Float
): List<Offset> {
    if (values.isEmpty()) return emptyList()
    return values.mapIndexed { index, value ->
        Offset(
            x = xForIndex(index = index, widthPx = widthPx, horizontalPaddingPx = horizontalPaddingPx, pointStepPx = pointStepPx),
            y = valueToY(
                value = value,
                minValue = minValue,
                maxValue = maxValue,
                heightPx = heightPx
            )
        )
    }
}

private fun xForIndex(index: Int, widthPx: Float, horizontalPaddingPx: Float, pointStepPx: Float): Float {
    if (widthPx <= 0f) return 0f
    return horizontalPaddingPx + (index * pointStepPx)
}

private fun valueToY(value: Double, minValue: Double, maxValue: Double, heightPx: Float): Float {
    val range = (maxValue - minValue).toFloat().coerceAtLeast(0.0001f)
    val normalized = ((value - minValue).toFloat() / range).coerceIn(0f, 1f)
    return heightPx - (normalized * heightPx)
}

private fun nearestIndexForX(x: Float, points: List<Offset>): Int {
    var nearestIndex = 0
    var nearestDistance = Float.MAX_VALUE
    points.forEachIndexed { index, point ->
        val distance = abs(point.x - x)
        if (distance < nearestDistance) {
            nearestDistance = distance
            nearestIndex = index
        }
    }
    return nearestIndex
}

private fun buildSmoothLinePath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points.first().x, points.first().y)

    if (points.size == 1) {
        return path
    }

    for (index in 1 until points.size) {
        val previous = points[index - 1]
        val current = points[index]
        val controlX = (previous.x + current.x) / 2f
        path.quadraticTo(controlX, previous.y, current.x, current.y)
    }
    return path
}

private fun buildFillPath(points: List<Offset>, heightPx: Float): Path {
    val path = Path()
    if (points.isEmpty()) return path

    path.moveTo(points.first().x, heightPx)
    path.lineTo(points.first().x, points.first().y)
    for (index in 1 until points.size) {
        val previous = points[index - 1]
        val current = points[index]
        val controlX = (previous.x + current.x) / 2f
        path.quadraticTo(controlX, previous.y, current.x, current.y)
    }
    path.lineTo(points.last().x, heightPx)
    path.close()
    return path
}

private fun toMoney(value: Double): String {
    return formatNumberToMoney(value.toLongCents().toDecimalString())
}
