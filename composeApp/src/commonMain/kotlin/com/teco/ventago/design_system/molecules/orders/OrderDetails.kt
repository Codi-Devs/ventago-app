package com.teco.ventago.design_system.molecules.orders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teco.ventago.design_system.buttons.TextButtonS
import com.teco.ventago.design_system.theme.Gray80
import com.teco.ventago.design_system.theme.bodyMedium
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.bodySmall
import com.teco.ventago.design_system.theme.latoFontFamily
import com.teco.ventago.features.orders.domain.models.Order
import com.teco.ventago.features.orders.domain.models.OrderLineDto
import com.teco.ventago.utils.DateFormat
import com.teco.ventago.utils.formatNumberToMoney
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.call_customer
import ventago.composeapp.generated.resources.created
import ventago.composeapp.generated.resources.customer
import ventago.composeapp.generated.resources.delivery
import ventago.composeapp.generated.resources.email
import ventago.composeapp.generated.resources.name
import ventago.composeapp.generated.resources.order_call_customer
import ventago.composeapp.generated.resources.order_id
import ventago.composeapp.generated.resources.order_see_on_map
import ventago.composeapp.generated.resources.order_summary
import ventago.composeapp.generated.resources.payment_and_shipping
import ventago.composeapp.generated.resources.payment_mode
import ventago.composeapp.generated.resources.phone
import ventago.composeapp.generated.resources.pick_up_desc
import ventago.composeapp.generated.resources.see_on_map
import ventago.composeapp.generated.resources.shipping
import ventago.composeapp.generated.resources.status
import ventago.composeapp.generated.resources.total


@Composable
fun OrderDetailsHeader(order: Order, statusOnClick: () -> Unit) {
    return Column(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
    ) {
        Row {
            Column(
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    maxLines = 1,
                    text = stringResource(Res.string.order_id),
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontFamily = latoFontFamily(),
                        fontWeight = FontWeight(600),
                        color = Color(0xFF7C8988),
                    )
                )

                Text(
                    maxLines = 1,
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(Res.string.created),
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontFamily = latoFontFamily(),
                        fontWeight = FontWeight(600),
                        color = Color(0xFF7C8988),
                    )
                )

                Text(
                    maxLines = 1,
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(Res.string.total),
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontFamily = latoFontFamily(),
                        fontWeight = FontWeight(600),
                        color = Color(0xFF7C8988),
                    )
                )

                Text(
                    maxLines = 1,
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(Res.string.status),
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontFamily = latoFontFamily(),
                        fontWeight = FontWeight(600),
                        color = Color(0xFF7C8988),
                    )
                )

            }
            Column {
                Text(
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    text = "#${order.internalNumber}",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontFamily = latoFontFamily(),
                        fontWeight = FontWeight(600),
                    )
                )

                Text(
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                    text = DateFormat.getOrdersFormattedDate(order.createdAt),
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontFamily = latoFontFamily(),
                        fontWeight = FontWeight(600),
                    )
                )

                Text(
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    text = formatNumberToMoney(order.totalAmount),
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontFamily = latoFontFamily(),
                        fontWeight = FontWeight(600),
                    )
                )

                OrderStatusChip(status = order.status) {
                    statusOnClick()
                }

            }


        }
        Divider(
            modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp),
            color = Gray80,
            thickness = 1.dp,
        )
    }

}

@Composable
fun OrderDetailsItem(orderItem: OrderLineDto) {
    return Column(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween

        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    text = orderItem.itemName,
                    style = bodyMedium()
                )

                Text(
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    text = formatNumberToMoney(orderItem.baseUnitPrice),
                    style = bodySmall()
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                modifier = Modifier.padding(bottom = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                text = "${orderItem.quantity}x",
                style = bodyMediumBold()
            )
        }
    }


}

@Composable
fun OrderDetailsPaymentShipping(
    paymentMethod: String,
    isPickup: Boolean,
    address: String,
    seeOnMapClick: () -> Unit,
) {
    return Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            modifier = Modifier.padding(start = 8.dp, bottom = 16.dp),
            text = stringResource(Res.string.payment_and_shipping),
            style = TextStyle(
                fontSize = 12.sp,
                fontFamily = latoFontFamily(),
                fontWeight = FontWeight(700),
                color = Color(0xFF7C8988),
                textAlign = TextAlign.Center,
            )
        )

        ElevatedCard(

            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 2.dp
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(
                modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp)
            ) {
                Text(
                    text = stringResource(Res.string.payment_mode),
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontFamily = latoFontFamily(),
                        fontWeight = FontWeight(700),
                        color = Color(0xFF2F4446),
                    )
                )
                Text(
                    text = paymentMethod,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontFamily = latoFontFamily(),
                        fontWeight = FontWeight(500),
                        color = Color(0xFF7C8988),
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.shipping),
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontFamily = latoFontFamily(),
                        fontWeight = FontWeight(700),
                        color = Color(0xFF2F4446),
                    )
                )
                if (isPickup) {
                    Text(
                        modifier = Modifier.padding(bottom = 8.dp),
                        text = stringResource(Res.string.pick_up_desc),
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontFamily = latoFontFamily(),
                            fontWeight = FontWeight(500),
                            color = Color(0xFF7C8988),
                        )
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.delivery),
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontFamily = latoFontFamily(),
                            fontWeight = FontWeight(500),
                            color = Color(0xFF7C8988),
                        )
                    )

                    Text(
                        modifier = Modifier.padding(vertical = 8.dp),
                        text = address,
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontFamily = latoFontFamily(),
                            fontWeight = FontWeight(500),
                            color = Color(0xFF7C8988),
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButtonS(
                            label = stringResource(Res.string.see_on_map),
                            onClick = seeOnMapClick,
                            prefixIcon = painterResource(Res.drawable.order_see_on_map)
                        )
                    }

                }
            }
        }
    }
}

//@Composable
//fun OrderDetailsCustomer(customer: Customer, callOnClick: () -> Unit, emailOnClick: () -> Unit) {
//    return Column(
//        modifier = Modifier.fillMaxWidth(),
//    ) {
//        Text(
//            modifier = Modifier.padding(start = 8.dp, bottom = 16.dp),
//            text = stringResource(Res.string.customer),
//            style = TextStyle(
//                fontSize = 12.sp,
//                fontFamily = latoFontFamily(),
//                fontWeight = FontWeight(700),
//                color = Color(0xFF7C8988),
//                textAlign = TextAlign.Center,
//            )
//        )
//
//        ElevatedCard(
//            modifier = Modifier.fillMaxWidth(),
//            colors = CardDefaults.elevatedCardColors(
//                containerColor = Color.White
//            ),
//            elevation = CardDefaults.elevatedCardElevation(
//                defaultElevation = 2.dp
//            ),
//            shape = RoundedCornerShape(10.dp)
//        ) {
//            Column(
//                modifier = Modifier.padding(top = 8.dp)
//            ) {
//                Column {
//                    Row(
//                        modifier = Modifier.padding(bottom = 8.dp, start = 8.dp, end = 8.dp),
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Text(
//                            modifier = Modifier.width(60.dp),
//                            text = stringResource(Res.string.name),
//                            style = MaterialTheme.typography.titleSmall.merge(
//                                TextStyle(
//                                    fontWeight = FontWeight.W700,
//                                    fontSize = 14.sp,
//                                    lineHeight = 20.sp,
//                                    fontFamily = latoFontFamily(),
//                                    letterSpacing = 0.1.sp
//                                )
//                            ),
//                        )
//
//                        Text(
//                            text = customer.name,
//                            style = TextStyle(
//                                fontSize = 12.sp,
//                                fontFamily = latoFontFamily(),
//                                fontWeight = FontWeight(600),
//                                color = Color(0xFF7C8988),
//                            )
//                        )
//                    }
//                    if (customer.email.isNotEmpty() && !customer.email.contains("pos.com")) {
//                        Row(
//                            modifier = Modifier.padding(bottom = 8.dp, start = 8.dp, end = 8.dp),
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//                            Text(
//                                modifier = Modifier.width(60.dp),
//                                text = stringResource(Res.string.email),
//                                style = MaterialTheme.typography.titleSmall.merge(
//                                    TextStyle(
//                                        fontWeight = FontWeight.W700,
//                                        fontSize = 14.sp,
//                                        lineHeight = 20.sp,
//                                        fontFamily = latoFontFamily(),
//                                        letterSpacing = 0.1.sp
//                                    )
//                                ),
//                            )
//
//                            Text(
//                                modifier = Modifier.clickable(true) {
//                                    emailOnClick()
//                                },
//                                text = customer.email,
//                                style = TextStyle(
//                                    fontSize = 12.sp,
//                                    fontFamily = latoFontFamily(),
//                                    fontWeight = FontWeight(600),
//                                    color = Color(0xFF7C8988),
//                                    textDecoration = TextDecoration.Underline,
//                                )
//                            )
//                        }
//                    }
//
//                    if (customer.phone.isNotEmpty() && customer.phone != customer.id.toString()) {
//                        Row(
//                            modifier = Modifier.padding(start = 8.dp, end = 8.dp),
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//                            Text(
//                                modifier = Modifier.width(60.dp),
//                                text = stringResource(Res.string.phone),
//                                style = MaterialTheme.typography.titleSmall.merge(
//                                    TextStyle(
//                                        fontWeight = FontWeight.W700,
//                                        fontSize = 14.sp,
//                                        lineHeight = 20.sp,
//                                        fontFamily = latoFontFamily(),
//                                        letterSpacing = 0.1.sp
//                                    )
//                                ),
//                            )
//
//                            Text(
//                                text = customer.phone,
//                                style = TextStyle(
//                                    fontSize = 12.sp,
//                                    fontFamily = latoFontFamily(),
//                                    fontWeight = FontWeight(600),
//                                    color = Color(0xFF7C8988),
//                                )
//                            )
//                        }
//                    }
//
//                }
//                if (customer.phone.isNotEmpty() && customer.phone != customer.id.toString()) {
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.Center
//                    ) {
//                        TextButtonS(
//                            label = stringResource(Res.string.call_customer),
//                            onClick = callOnClick,
//                            prefixIcon = painterResource(Res.drawable.order_call_customer)
//                        )
//                    }
//                }
//
//
//            }
//        }
//    }
//}

@Composable
fun OrderDetailsSummary(items: List<Pair<String, String>>, total: String) {
    return Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            modifier = Modifier.padding(start = 8.dp, bottom = 16.dp),
            text = stringResource(Res.string.order_summary),
            style = TextStyle(
                fontSize = 12.sp,
                fontFamily = latoFontFamily(),
                fontWeight = FontWeight(700),
                color = Color(0xFF7C8988),
                textAlign = TextAlign.Center,
            )
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 2.dp
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(
                modifier = Modifier.padding(all = 8.dp)
            ) {
                for (item in items) {
                    Row(
                        modifier = Modifier.padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            modifier = Modifier.weight(1f, fill = true),
                            text = item.first,
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontFamily = latoFontFamily(),
                                fontWeight = FontWeight(700),
                                color = Color(0xFF7C8988),
                            )
                        )

                        Spacer(modifier = Modifier)

                        Text(
                            text = formatNumberToMoney(item.second),
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontFamily = latoFontFamily(),
                                fontWeight = FontWeight(700),
                                color = Color(0xFF7C8988),
                            )
                        )
                    }
                }

                Divider(
                    modifier = Modifier.padding(top = 4.dp),
                    color = Gray80,
                    thickness = 1.dp,
                )

                Row(
                    modifier = Modifier.padding(bottom = 4.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        modifier = Modifier.weight(1f, fill = true),
                        text = stringResource(Res.string.total),
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontFamily = latoFontFamily(),
                            fontWeight = FontWeight(700),
                            color = Color(0xFF2F4446),
                        )
                    )

                    Spacer(modifier = Modifier)

                    Text(
                        text = formatNumberToMoney(total),
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontFamily = latoFontFamily(),
                            fontWeight = FontWeight(700),
                            color = Color(0xFF2F4446),
                        )
                    )
                }

            }
        }
    }
}