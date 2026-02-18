package com.teco.ventago.design_system.organism

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
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
import com.teco.ventago.design_system.theme.StatusPending
import com.teco.ventago.design_system.theme.graphicNormalColor
import com.teco.ventago.design_system.theme.graphicSelectedColor
import com.teco.ventago.design_system.theme.latoFontFamily
import com.teco.ventago.design_system.theme.onSecondaryLight
import com.teco.ventago.design_system.theme.secondaryContainerLight
import com.teco.ventago.design_system.theme.secondaryLight
import com.teco.ventago.utils.formatNumberToMoney
import com.teco.ventago.utils.toDecimalString
import com.teco.ventago.utils.toLongCents
import org.jetbrains.compose.resources.stringResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.no_data
import kotlin.math.abs

@Composable
fun BarGraphic(
    modifier: Modifier = Modifier,
    data: List<Pair<String, Double>>,
    selectedIndex: Int = 2,
    barGraphicHeight: Double = 140.0,
    itemsToShow: Int = 6,
    selectedColor: Color = MaterialTheme.colorScheme.secondary,
    normalColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    onItemClick: (Int) -> Unit = {}
) {
    val displayItems = if (itemsToShow <= 0) emptyList() else data.take(itemsToShow)

    if (displayItems.isEmpty()) {
        Row(
            Modifier
                .height(barGraphicHeight.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
             verticalAlignment = Alignment.CenterVertically,
            ) {
            Icon(
                modifier = Modifier
                    .padding(start = 16.dp),
                imageVector = Icons.Outlined.WarningAmber,
                tint = StatusPending,
                contentDescription = "",
                )
            Text(
                modifier = Modifier
                    .padding(start = 8.dp, end = 16.dp),
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
    } else {
        val barWidth = if (displayItems.size > 18) 24.dp else 34.dp
        val biggestValue = displayItems.maxOfOrNull { abs(it.second) } ?: 0.0
        Row (
            modifier = modifier
                .height((barGraphicHeight + 40.0).dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            displayItems.forEachIndexed { index, pair ->
                val color = if (index == selectedIndex) selectedColor else normalColor
                val height = if (pair.second == 0.0 || biggestValue == 0.0) {
                    1.0
                } else {
                    getBarHeight(abs(pair.second), biggestValue, barGraphicHeight)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (index == selectedIndex) {
                        Box(
                            Modifier
                                .background(color, RoundedCornerShape(4.dp))
                                .padding(2.dp)
                                .clickable {

                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = formatNumberToMoney(pair.second.toLongCents().toDecimalString()),
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    fontFamily = latoFontFamily(),
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    fontWeight = FontWeight(500),
                                    letterSpacing = 0.15.sp,
                                )
                            )
                        }

                        Spacer(modifier = Modifier.size(1.dp, 8.dp))
                    }

                    Box(
                        Modifier
                            .size(barWidth, height.dp)
                            .background(color, RoundedCornerShape(4.dp))
                            .clickable {
                                onItemClick(index)
                                       },
                        contentAlignment = Alignment.Center
                    ) {

                    }
                    Text(
                        text = pair.first,
                        modifier = Modifier.padding(top = 8.dp),
                        style = TextStyle(
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            fontFamily = latoFontFamily(),
                            fontWeight = FontWeight(500),
                            letterSpacing = 0.5.sp,
                        )
                    )
                }
            }
        }
    }
}

private fun getBarHeight(value: Double, biggestValue: Double, barGraphicHeight: Double): Double {
    return ((value * barGraphicHeight) / biggestValue)
}
