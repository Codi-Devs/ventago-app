package com.teco.ventago.design_system.molecules.orders

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teco.ventago.features.orders.domain.models.OrderStatus
import com.teco.ventago.design_system.theme.primaryLight
import com.teco.ventago.features.orders.domain.models.OrderHistoryDto
import com.teco.ventago.utils.DateFormat
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.order_history_cancelled
import ventago.composeapp.generated.resources.order_status_accepted
import ventago.composeapp.generated.resources.order_status_cancelled
import ventago.composeapp.generated.resources.order_status_check
import ventago.composeapp.generated.resources.order_status_clock
import ventago.composeapp.generated.resources.order_status_completed
import ventago.composeapp.generated.resources.order_status_in_delivery
import ventago.composeapp.generated.resources.order_status_new
import ventago.composeapp.generated.resources.order_status_payment_failed
import ventago.composeapp.generated.resources.order_status_processing
import ventago.composeapp.generated.resources.order_status_ready
import ventago.composeapp.generated.resources.order_status_ready_pickup
import ventago.composeapp.generated.resources.order_status_rejected
import ventago.composeapp.generated.resources.order_time
import ventago.composeapp.generated.resources.reason


@Composable
fun OrderHistoryList(data: List<OrderHistoryDto>, reasonClick: () -> Unit = {}) {
     return Column {
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)

        for (item in data) {
            OrderHistoryItem(false, getLabel(item.statusId), item.createdAt,data.last().statusId == item.statusId, reasonClick)
            if (!false && !false && data.last().statusId != item.statusId) {
                Canvas(
                    Modifier
                        .height(LocalDensity.current.run {
                            150f
                                .toInt()
                                .toDp()
                        })
                        .padding(start = 18.dp)
                ) {
                    drawLine(
                        color = primaryLight,
                        start = Offset(0f, 0f),
                        end = Offset(0f, 150f),
                        pathEffect = pathEffect,
                        strokeWidth = 1f
                    )
                }
            }
        }
    }
}

@Composable
fun OrderHistoryItem(isCancelled: Boolean, label: String, date: String, last: Boolean, onClick: () -> Unit) {
    val drawable = if (isCancelled)
        Res.drawable.order_history_cancelled
    else
        Res.drawable.order_status_check


    return Row(
        modifier = Modifier.padding(horizontal = 8.dp),
        verticalAlignment = if (isCancelled) Alignment.Top else Alignment.CenterVertically
    ) {
        Column {
            if (isCancelled) {
                Spacer(modifier = Modifier.height(10.dp))
            }
            Image(
                modifier = Modifier.size(18.dp),
                painter = painterResource(drawable),
                contentDescription = "Order status"
            )

        }


        Column(
            Modifier.padding(start = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier,
                text = label,
                style = MaterialTheme.typography.titleSmall.merge(
                    TextStyle(
                        fontWeight = FontWeight.W700,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        letterSpacing = 0.1.sp
                    )
                ),
            )

            Row(
                modifier = Modifier.padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    modifier = Modifier.size(12.dp),
                    painter = painterResource(Res.drawable.order_status_clock),
                    contentDescription = stringResource(Res.string.order_time),
                )

                Text(
                    modifier = Modifier.padding(start = 4.dp),
                    text = DateFormat.getOrdersFormattedDate(date),
                    style = MaterialTheme.typography.bodyMedium.merge(
                        TextStyle(
                            fontWeight = FontWeight.W400,
                            fontSize = 12.sp,
                            lineHeight = 20.sp,
                            letterSpacing = 0.08.sp,
                            color = Color(0xFF7C8988),
                            textAlign = TextAlign.Center,
                        )
                    ),
                )
            }


            if (isCancelled) {
                Row(
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick,
                        modifier = Modifier.height(48.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.reason),
                            style = MaterialTheme.typography.bodyMedium.merge(
                                TextStyle(
                                    fontWeight = FontWeight.W700,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    letterSpacing = 0.1.sp
                                )
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

}

@Composable
private fun getLabel(statusId: Int): String {
    return when (statusId) {
        OrderStatus.DRAFT -> stringResource(Res.string.order_status_new)
        OrderStatus.CANCELLED -> stringResource(Res.string.order_status_cancelled)
        OrderStatus.CONFIRMED -> stringResource(Res.string.order_status_accepted)
        OrderStatus.COMPLETED -> stringResource(Res.string.order_status_completed)
        OrderStatus.REJECT -> stringResource(Res.string.order_status_rejected)
        OrderStatus.PROCESSING -> stringResource(Res.string.order_status_processing)
        OrderStatus.REFUNDED -> stringResource(Res.string.order_status_in_delivery)
        OrderStatus.READY -> stringResource(Res.string.order_status_ready)
        else -> stringResource(Res.string.order_status_new)
    }
}
