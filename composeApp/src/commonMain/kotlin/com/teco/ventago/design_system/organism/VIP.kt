package com.teco.ventago.design_system.organism
//
//import androidx.compose.foundation.BorderStroke
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material3.Button
//import androidx.compose.material3.ButtonDefaults
//import androidx.compose.material3.Card
//import androidx.compose.material3.CardDefaults
//import androidx.compose.material3.Icon
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableIntStateOf
//import androidx.compose.runtime.mutableLongStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.geometry.Size
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.Outline
//import androidx.compose.ui.graphics.Path
//import androidx.compose.ui.graphics.Shape
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.res.stringResource
//import androidx.compose.ui.text.TextStyle
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.Density
//import androidx.compose.ui.unit.LayoutDirection
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import com.airbnb.lottie.LottieComposition
//import com.airbnb.lottie.compose.LottieAnimation
//import com.airbnb.lottie.compose.LottieConstants
//import com.tecomenu.digitalmenu.R
//import com.tecomenu.digitalmenu.billing.SubscriptionProduct
//import com.tecomenu.digitalmenu.design_system.buttons.ButtonM
//import com.tecomenu.digitalmenu.design_system.buttons.TextButtonS
//import com.tecomenu.digitalmenu.design_system.molecules.DMDivider
//import com.tecomenu.digitalmenu.design_system.molecules.VIPBenefitItem
//import com.tecomenu.digitalmenu.design_system.theme.Gray50
//import com.tecomenu.digitalmenu.design_system.theme.VIPColor
//import com.tecomenu.digitalmenu.design_system.theme.bodyMedium
//import com.tecomenu.digitalmenu.design_system.theme.labelMedium
//import com.tecomenu.digitalmenu.design_system.theme.labelSmall
//import com.tecomenu.digitalmenu.design_system.theme.lato
//import com.tecomenu.digitalmenu.design_system.theme.titleMediumBold
//import kotlinx.coroutines.delay
//import kotlinx.datetime.Instant
//import kotlinx.datetime.TimeZone
//import kotlinx.datetime.toInstant
//import kotlinx.datetime.toLocalDateTime
//import utils.DateFormat
//import java.util.Calendar
//
//
//@Composable
//fun VIPBenefits(composition: LottieComposition?, onSubscribeClick: () -> Unit) {
//    Card(
//        modifier = Modifier.padding(16.dp),
//        elevation = CardDefaults.cardElevation(4.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.background
//        ),
//    ) {
//        Row {
//            LottieAnimation(
//                composition = composition,
//                iterations = LottieConstants.IterateForever,
//                modifier = Modifier
//                    .padding(start = 0.dp, top = 8.dp)
//                    .size(height = 57.dp, width = 57.dp)
//            )
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(start = 8.dp, top = 8.dp, end = 8.dp)
//            ) {
//                Text(
//                    text = stringResource(id = R.string.get_full_access_2),
//                    style = TextStyle(
//                        fontSize = 14.sp,
//                        lineHeight = 20.sp,
//                        fontFamily = latoFontFamily(),
//                        fontWeight = FontWeight(500),
//                        letterSpacing = 0.1.sp,
//                    )
//                )
//                Text(
//                    text = "16% OFF", style = TextStyle(
//                        fontSize = 28.sp,
//                        lineHeight = 36.sp,
//                        fontFamily = latoFontFamily(),
//                        fontWeight = FontWeight(700),
//                    )
//                )
//                Text(
//                    text = stringResource(id = R.string.annual_payments), style = TextStyle(
//                        fontSize = 9.sp,
//                        lineHeight = 16.sp,
//                        fontFamily = latoFontFamily(),
//                        fontWeight = FontWeight(500),
//                        color = Color(0xFF8A8E93),
//                        letterSpacing = 0.5.sp,
//                    )
//                )
//
//                Row(
//                    modifier = Modifier
//                        .padding(end = 8.dp, bottom = 16.dp)
//                        .fillMaxWidth(),
//                    horizontalArrangement = Arrangement.End,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Button(
//                        colors = ButtonDefaults.buttonColors(
//                            containerColor = VIPColor,
//                            contentColor = MaterialTheme.colorScheme.onPrimary/* Other colors use values from MaterialTheme */
//                        ),
//                        onClick = onSubscribeClick,
//                        modifier = Modifier.height(38.dp),
//                        content = {
//                            Text(
//                                text = stringResource(id = R.string.know_more), style = TextStyle(
//                                    fontSize = 14.sp,
//                                    lineHeight = 20.sp,
//                                    fontFamily = latoFontFamily(),
//                                    fontWeight = FontWeight(700),
//                                    letterSpacing = 0.04.sp,
//                                )
//                            )
//                        },
//                        shape = RoundedCornerShape(10.dp),
//                        enabled = true
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun VIPBenefitsWithFreeTrial(composition: LottieComposition?, expireDate: String, onSubscribeClick: () -> Unit) {
//    val instant = Instant.parse(expireDate)
//    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
//    val formattedDate = DateFormat.getFormattedDate(expireDate, DateFormat.ORDERS, DateFormat.FEEDBACKS_TIME_COMPLETE)
//
//    val time = (dateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()).minus(Calendar.getInstance().timeInMillis)
//    var timer by remember { mutableLongStateOf(time) }
//    LaunchedEffect(key1 = timer) {
//        if (timer > 0) {
//            delay(1000L)
//            timer -= 1000L
//        }
//    }
//    val secMilSec: Long = 1000
//    val minMilSec = 60 * secMilSec
//    val hourMilSec = 60 * minMilSec
//    val dayMilSec = 24 * hourMilSec
//    val hours = (time / 3600000).toInt()
//    val minutes = (time % dayMilSec % hourMilSec / minMilSec).toInt()
//    val seconds = (time % dayMilSec % hourMilSec % minMilSec / secMilSec).toInt()
//
//
//
//    Card(
//        modifier = Modifier.padding(16.dp),
//        elevation = CardDefaults.cardElevation(4.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.background
//        ),
//        onClick = onSubscribeClick
//    ) {
//        Row {
//            LottieAnimation(
//                composition = composition,
//                iterations = LottieConstants.IterateForever,
//                modifier = Modifier
//                    .padding(start = 0.dp, top = 8.dp)
//                    .size(height = 72.dp, width = 72.dp)
//            )
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(start = 8.dp, top = 8.dp, end = 8.dp)
//            ) {
//                Text(
//                    text = stringResource(id = R.string.get_full_access_offer),
//                    style = TextStyle(
//                        fontSize = 14.sp,
//                        lineHeight = 20.sp,
//                        fontFamily = latoFontFamily(),
//                        fontWeight = FontWeight(500),
//                        letterSpacing = 0.1.sp,
//                    )
//                )
//                Text(
//                    text = String.format(" %02d:%02d:%02d", hours, minutes, seconds), style = TextStyle(
//                        fontSize = 28.sp,
//                        lineHeight = 36.sp,
//                        fontFamily = latoFontFamily(),
//                        fontWeight = FontWeight(700),
//                    ),
//                    modifier = Modifier.padding(top = 8.dp)
//                )
//                Text(
//                    text = stringResource(id = R.string.offer_valid_before) + formattedDate, style = TextStyle(
//                        fontSize = 9.sp,
//                        lineHeight = 16.sp,
//                        fontFamily = latoFontFamily(),
//                        fontWeight = FontWeight(500),
//                        color = Color(0xFF8A8E93),
//                        letterSpacing = 0.5.sp,
//                    )
//                )
//
//                Row(
//                    modifier = Modifier
//                        .padding(end = 8.dp, bottom = 16.dp, top = 8.dp)
//                        .fillMaxWidth(),
//                    horizontalArrangement = Arrangement.End,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Button(
//                        colors = ButtonDefaults.buttonColors(
//                            containerColor = VIPColor,
//                            contentColor = MaterialTheme.colorScheme.onPrimary/* Other colors use values from MaterialTheme */
//                        ),
//                        onClick = onSubscribeClick,
//                        modifier = Modifier.height(38.dp),
//                        content = {
//                            Text(
//                                text = stringResource(id = R.string.five_free_days), style = TextStyle(
//                                    fontSize = 14.sp,
//                                    lineHeight = 20.sp,
//                                    fontFamily = latoFontFamily(),
//                                    fontWeight = FontWeight(700),
//                                    letterSpacing = 0.04.sp,
//                                )
//                            )
//                        },
//                        shape = RoundedCornerShape(10.dp),
//                        enabled = true
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun VIPBenefitsItems(shipColor: Color = VIPColor) {
//    VIPBenefitItem(
//        stringResource(id = R.string.premium_benefits1), Modifier.padding(top = 24.dp), shipColor = shipColor
//    )
//    VIPBenefitItem(
//        stringResource(id = R.string.premium_benefits2), Modifier.padding(top = 24.dp), shipColor = shipColor
//    )
//    VIPBenefitItem(
//        stringResource(id = R.string.premium_benefits3), Modifier.padding(top = 24.dp), shipColor = shipColor
//    )
//    VIPBenefitItem(
//        stringResource(id = R.string.premium_benefits4), Modifier.padding(top = 24.dp), shipColor = shipColor
//    )
//    VIPBenefitItem(
//        stringResource(id = R.string.premium_benefits5), Modifier.padding(top = 24.dp), shipColor = shipColor
//    )
//    VIPBenefitItem(
//        stringResource(id = R.string.premium_benefits6), Modifier.padding(top = 24.dp), shipColor = shipColor
//    )
//}
//
//
//
//@Composable
//fun VIPFreeTimeOver(
//    composition: LottieComposition?,
//
//    onSubscribeClick: (String) -> Unit,
//    onCancelClick: () -> Unit,
//    products: List<SubscriptionProduct>,
//) {
//    var selected by remember { mutableIntStateOf(0) }
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .verticalScroll(rememberScrollState())
//            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        LottieAnimation(
//            composition = composition,
//            iterations = LottieConstants.IterateForever,
//            modifier = Modifier.size(height = 80.dp, width = 80.dp)
//        )
//
//        Text(
//            text = stringResource(id = R.string.get_premium),
//            modifier = Modifier.padding(top = 8.dp),
//            style = titleMediumBold()
//        )
//
//        Text(
//            text = stringResource(id = R.string.free_time_over),
//            modifier = Modifier.padding(top = 8.dp),
//            style = bodyMedium()
//        )
//
//        Text(
//            text = "*De no obtener el plan premium perderá los beneficios que se encuentran fuera de los límites gratuitos. ",
//            modifier = Modifier.padding(top = 8.dp),
//            style = labelSmall().merge(
//                TextStyle(
//                    color = Color(0xFF6C6B6B),
//                )
//            )
//        )
//
//        DMDivider(
//            modifier = Modifier.padding(top = 16.dp),
//            label = "Conocer las funcionalidades",
//            style = labelMedium()
//        )
//
//        VIPBenefitsItems(Gray50)
//
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(top = 24.dp),
//            horizontalArrangement = Arrangement.Center,
//            verticalAlignment = Alignment.CenterVertically
//
//        ) {
//            VIPPriceItem(
//                modifier = Modifier
//                    .weight(1f)
//                    .padding(end = 8.dp),
//                title = "${products[0].formattedPrice}/mes",
//                subTitle = products[0].description,
//                onClick = {
//                    selected = 0
//                },
//                selected = selected == 0
//            )
//            VIPPriceItem(
//                modifier = Modifier
//                    .weight(1f)
//                    .padding(start = 8.dp),
//                title = "${products[1].formattedPrice}/año",
//                subTitle = products[1].description,
//                onClick = {
//                    selected = 1
//                },
//                selected = selected == 1
//            )
//        }
//
//        ButtonM(
//            onClick = { onSubscribeClick(products[selected].basePlanId) },
//            modifier = Modifier.padding(top = 16.dp),
//            containerColor = VIPColor,
//        ) {
//            Text(
//                text = "Subscribirme", style = TextStyle(
//                    fontSize = 16.sp,
//                    lineHeight = 24.sp,
//                    fontFamily = latoFontFamily(),
//                    fontWeight = FontWeight(700),
//                    color = MaterialTheme.colorScheme.onPrimary,
//                    textAlign = TextAlign.Center,
//                    letterSpacing = 0.02.sp,
//                )
//            )
//        }
//
//        TextButtonS(label = "Cancelar") {
//            onCancelClick()
//        }
//
//    }
//}
//
//@Composable
//fun VIPPriceItem(
//    title: String,
//    subTitle: String,
//    onClick: () -> Unit,
//    selected: Boolean,
//    modifier: Modifier = Modifier,
//) {
//    Card(
//        modifier = modifier,
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.background
//        ),
//        elevation = CardDefaults.cardElevation(4.dp),
//        border = if (selected) BorderStroke(1.5.dp, VIPColor) else null,
//        onClick = onClick,
//    ) {
//        Box {
//
//            if (selected) {
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(0.dp),
//                    horizontalArrangement = Arrangement.End,
//                ) {
//                    Card(
//                        elevation = CardDefaults.cardElevation(0.dp),
//                        colors = CardDefaults.cardColors(
//                            containerColor = VIPColor
//                        ),
//                        modifier = Modifier
//                            .size(35.dp)
//                            .clip(shape = TriangleCornerShape())
//                            .background(color = VIPColor)
//                    ) {
//                        Column(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(top = 6.dp, end = 6.dp),
//                            horizontalAlignment = Alignment.End,
//                            verticalArrangement = Arrangement.Top,
//                        ) {
//                            Icon(
//                                modifier = Modifier.size(10.dp),
//                                painter = painterResource(id = R.drawable.fi_sr_star),
//                                contentDescription = "",
//                                tint = Color.White,
//                            )
//                        }
//                    }
//                }
//            }
//
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(16.dp)
//            ) {
//                Text(
//                    text = title, style = TextStyle(
//                        fontSize = 16.sp,
//                        lineHeight = 24.sp,
//                        fontFamily = latoFontFamily(),
//                        fontWeight = FontWeight(700),
//                        color = MaterialTheme.colorScheme.onBackground,
//                        textAlign = TextAlign.Center,
//                        letterSpacing = 0.02.sp,
//                    )
//                )
//
//                Text(
//                    text = subTitle,
//                    style = labelMedium().merge(
//                        TextStyle(
//                            fontWeight = FontWeight(500),
//                            color = MaterialTheme.colorScheme.onBackground,
//                        )
//                    ),
//                )
//            }
//        }
//    }
//}
//
//
//class TriangleCornerShape : Shape {
//    override fun createOutline(
//        size: Size,
//        layoutDirection: LayoutDirection,
//        density: Density,
//    ): Outline {
//        val trianglePath = Path().apply {
//            moveTo(size.height, size.width)
//            lineTo(size.height, 0f)
//            lineTo(0f, 0f)
//        }
//        return Outline.Generic(
//            // Draw your custom path here
//            path = trianglePath
//        )
//    }
//}
//
