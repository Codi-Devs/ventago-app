package com.teco.ventago.design_system.organism//package com.teco.ventago.design_system.organism
//
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.outlined.Cancel
//import androidx.compose.material.icons.outlined.WatchLater
//import androidx.compose.material.icons.rounded.CalendarMonth
//import androidx.compose.material3.Card
//import androidx.compose.material3.CardDefaults
//import androidx.compose.material3.Icon
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.OutlinedButton
//import androidx.compose.material3.SuggestionChip
//import androidx.compose.material3.SuggestionChipDefaults
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalUriHandler
//import androidx.compose.ui.text.TextStyle
//import androidx.compose.ui.text.buildAnnotatedString
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.text.withStyle
//import androidx.compose.ui.unit.dp
//import com.teco.ventago.design_system.buttons.TextButtonS
//import com.teco.ventago.design_system.theme.bodyMediumBold
//import org.jetbrains.compose.resources.painterResource
//import org.jetbrains.compose.resources.stringResource
//import ventago.composeapp.generated.resources.Res
//
//@Composable
//fun ManageSubsCard(modifier: Modifier = Modifier) {
//    val uriHandler = LocalUriHandler.current
//    Card(
//        modifier = modifier,
//        elevation = CardDefaults.cardElevation(4.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.background
//        ),
//    ) {
//        Row(
//            verticalAlignment = Alignment.CenterVertically,
//        ) {
//            Image(
//                modifier = Modifier.padding(start = 16.dp, end = 8.dp),
//                painter = painterResource(Res.drawable.colored_play_icon),
//                contentDescription = "",
//            )
//            Column {
//                Text(
//                    text = stringResource(Res.string.manage_subscriptions_on_google),
//                    modifier = Modifier.padding(top = 8.dp, end = 8.dp),
//                    style = bodyMediumBold().merge(TextStyle(textAlign = TextAlign.Start))
//                )
//
//                Row(
//                    modifier = Modifier
//                        .padding(end = 16.dp, bottom = 0.dp)
//                        .fillMaxWidth(),
//                    horizontalArrangement = Arrangement.End,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    TextButtonS(label = stringResource(Res.string.manage)) {
//                        uriHandler.openUri("https://play.google.com/store/account/subscriptions?sku=vip_premium&package=com.tecomenu.digitalmenu")
//                    }
//                }
//            }
//        }
//    }
//}
//
//
//@Composable
//fun SubsInformationCard(
//    modifier: Modifier = Modifier,
//    status: SubscriptionStatus,
//    formattedPrice: String,
//    formattedPayDate: String,
//    formattedExpiredDate: String,
//    formattedCancelDate: String?,
//) {
//    val context = LocalContext.current
//    val uriHandler = LocalUriHandler.current
//    Card(
//        modifier = modifier.fillMaxWidth(),
//        colors = CardDefaults.cardColors(
//            containerColor = Color(0xFFF3F3F3),
//        ),
//        elevation = CardDefaults.cardElevation(0.dp),
//    ) {
//
//        Text(
//            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
//            text = stringResource(id = Res.string.actual_subs),
//            style = titleSmallBold(),
//        )
//        Text(
//            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
//            text = formattedPrice,
//            style = headlineSmall()
//        )
//
//
//        SubscriptionStatusChip(
//            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
//            status = status
//        )
//
//
//        Row(
//            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Icon(
//                modifier = Modifier.size(20.dp),
//                imageVector = Icons.Rounded.CalendarMonth,
//                contentDescription = ""
//            )
//            Text(
//                buildAnnotatedString {
//                    withStyle(style = bodySmall().toSpanStyle()) {
//                        append("${stringResource(id = Res.string.payment_date)} ")
//                    }
//
//                    withStyle(style = labelMediumBold().toSpanStyle()) {
//                        append(formattedPayDate)
//                    }
//                },
//                modifier = Modifier.padding(start = 8.dp),
//            )
//        }
//
//
//        Row(
//            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Icon(
//                modifier = Modifier.size(20.dp),
//                imageVector = Icons.Outlined.WatchLater,
//                contentDescription = ""
//            )
//
//            if (status == SubscriptionStatus.ACTIVE) {
//                Text(
//                    buildAnnotatedString {
//                        withStyle(style = bodySmall().toSpanStyle()) {
//                            append("${stringResource(id = Res.string.next_payment)} ")
//                        }
//
//                        withStyle(style = labelMediumBold().toSpanStyle()) {
//                            append(formattedExpiredDate)
//                        }
//                    },
//                    modifier = Modifier.padding(start = 8.dp),
//                )
//            } else {
//                Text(
//                    buildAnnotatedString {
//                        withStyle(style = bodySmall().toSpanStyle()) {
//                            append("${stringResource(id = Res.string.expires_on)} ")
//                        }
//
//                        withStyle(style = labelMediumBold().toSpanStyle()) {
//                            append(formattedExpiredDate)
//                        }
//                    },
//                    modifier = Modifier.padding(start = 8.dp),
//                )
//            }
//
//        }
//
//        if (!formattedCancelDate.isNullOrBlank()) {
//            Row(
//                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Icon(
//                    modifier = Modifier.size(20.dp),
//                    imageVector = Icons.Outlined.Cancel,
//                    contentDescription = ""
//                )
//                Text(
//                    buildAnnotatedString {
//                        withStyle(style = bodySmall().toSpanStyle()) {
//                            append("${stringResource(id = Res.string.cancelled_on)} ")
//                        }
//
//                        withStyle(style = labelMediumBold().toSpanStyle()) {
//                            append(formattedCancelDate)
//                        }
//                    },
//                    modifier = Modifier.padding(start = 8.dp),
//                )
//            }
//        }
//
//        OutlinedButton(modifier = Modifier
//            .fillMaxWidth()
//            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
//            shape = RoundedCornerShape(10.dp),
//            onClick = {
//                uriHandler.openUri("https://play.google.com/store/account/subscriptions?sku=vip_premium&package=com.tecomenu.digitalmenu")
//            }) {
//            Text(text = stringResource(id = Res.string.manage), style = labelLarge())
//        }
//
//    }
//}
//
//@Composable
//fun SubscriptionStatusChip(status: SubscriptionStatus, modifier: Modifier = Modifier) {
//    val colors = getSubscriptionStatusColors(status)
//    val label = stringResource(id = getSubscriptionLabelStringID(status))
//    SuggestionChip(
//        modifier = modifier.height(23.dp),
//        onClick = { },
//
//        colors = SuggestionChipDefaults.suggestionChipColors(
//            containerColor = colors.first,
//            labelColor = colors.second,
//        ),
//        border = SuggestionChipDefaults.suggestionChipBorder(
//            enabled = true,
//            borderColor = colors.first,
//            disabledBorderColor= colors.first,
//            borderWidth = 1.dp
//        ),
//        label = {
//            Text(
//                text = label, maxLines = 2, overflow = TextOverflow.Ellipsis
//            )
//        },
//    )
//}
//
//private fun getSubscriptionStatusColors(status: SubscriptionStatus): Pair<Color, Color> {
//    return when (status) {
//        SubscriptionStatus.ACTIVE -> Pair(NewContainer, NewLabel)
//        SubscriptionStatus.PENDING -> Pair(ProcessingContainer, ProcessingLabel)
//        SubscriptionStatus.CANCELLED -> Pair(CancelledContainer, CancelledLabel)
//        else -> Pair(NewContainer, NewLabel)
//    }
//}
//
//
//private fun getSubscriptionLabelStringID(status: SubscriptionStatus): Int {
//    return when (status) {
//        SubscriptionStatus.ACTIVE -> Res.string.active
//        SubscriptionStatus.PENDING -> Res.string.pending
//        SubscriptionStatus.CANCELLED -> Res.string.cancelled
//        else -> Res.string.active
//    }
//}