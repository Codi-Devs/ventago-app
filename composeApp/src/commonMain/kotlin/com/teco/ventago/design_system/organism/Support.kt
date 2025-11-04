package com.teco.ventago.design_system.organism

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teco.ventago.design_system.theme.Gray70
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.cardContainerColor
import com.teco.ventago.design_system.theme.latoFontFamily
import com.teco.ventago.rememberPlatformState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.chat_with_us
import ventago.composeapp.generated.resources.contact_support_team
import ventago.composeapp.generated.resources.fi_rr_headset
import ventago.composeapp.generated.resources.need_help

@Composable
fun SupportCard(unreadCount: Int, startChatClick: () -> Unit) {
    val platformState = rememberPlatformState()
    Card(
        modifier = Modifier.padding(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor()
        ),
    ) {
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier.padding(start = 16.dp, end = 8.dp),
                painter = painterResource(Res.drawable.fi_rr_headset),
                contentDescription = "",
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 8.dp, end = 8.dp)
            ) {
                Text(
                    text = stringResource(Res.string.need_help), style = TextStyle(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontFamily = latoFontFamily(),
                        fontWeight = FontWeight(500),
                        letterSpacing = 0.1.sp,
                    )
                )
                Text(
                    text = stringResource(Res.string.contact_support_team), style = TextStyle(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontFamily = latoFontFamily(),
                        fontWeight = FontWeight(500),
                        letterSpacing = 0.1.sp
                    )
                )
            }
        }

//        BadgedBox(
//            badge = {
//                if (unreadCount > 0) {
//                    Badge(
//                        modifier = Modifier.offset {
//                            IntOffset(x = -10, y = 10.dp.roundToPx())
//                        },
//                        content = {
//                            Text(text = unreadCount.toString())
//                        }
//                    )
//                }
//
//            }
//        ) {
//            Button(
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = MaterialTheme.colorScheme.primary,
//                    contentColor = MaterialTheme.colorScheme.onPrimary/* Other colors use values from MaterialTheme */
//                ),
//                onClick = startChatClick,
//                modifier = Modifier
//                    .padding(start = 16.dp, top = 16.dp)
//                    .height(42.dp)
//                    .wrapContentWidth(),
//                content = {
//                    Icon(
//                        modifier = Modifier
//                            .padding(start = 8.dp, end = 8.dp)
//                            .size(20.dp),
//                        tint = MaterialTheme.colorScheme.onPrimary,
//                        imageVector = Icons.AutoMirrored.Rounded.Send,
//                        contentDescription = ""
//                    )
//                    Text(
//                        modifier = Modifier.padding(end = 16.dp),
//                        text = stringResource(Res.string.chat_with_us),
//                        style = bodyMediumBold(color = MaterialTheme.colorScheme.onPrimary)
//                    )
//                },
//                shape = RoundedCornerShape(10.dp),
//                enabled = true
//            )
//        }

        Box(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(color = Gray70)
        )

        ClickableText(modifier = Modifier.padding(
            start = 16.dp,
            top = 12.dp,
            end = 8.dp,
            bottom = 16.dp
        ),
            text = AnnotatedString("support@tecodigi.com"),
            style = TextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontFamily = latoFontFamily(),
                fontWeight = FontWeight(700),
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.04.sp,
            ),
            onClick = {
                platformState.openEmailIntent("support@tecodigi.com")
            })

    }
}


@Composable
fun VIPSupportCard() {
    Card(
        modifier = Modifier.padding(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
    ) {
        Row(
            modifier = Modifier,
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier.padding(start = 16.dp, end = 8.dp),
                painter = painterResource(Res.drawable.fi_rr_headset),
                contentDescription = "",
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 8.dp, end = 8.dp)
            ) {
                Text(
                    text = stringResource(Res.string.need_help), style = TextStyle(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontFamily = latoFontFamily(),
                        fontWeight = FontWeight(500),
                        letterSpacing = 0.1.sp,
                    )
                )
                Text(
                    text = stringResource(Res.string.contact_support_team), style = TextStyle(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontFamily = latoFontFamily(),
                        fontWeight = FontWeight(500),
                        letterSpacing = 0.1.sp
                    )
                )
                Row(
                    modifier = Modifier
                        .padding(top = 16.dp, end = 8.dp, bottom = 16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary/* Other colors use values from MaterialTheme */
                    ), onClick = {}, modifier = Modifier.height(38.dp), content = {
                        Text(
                            text = "Whatsapp", style = TextStyle(
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                fontFamily = latoFontFamily(),
                                fontWeight = FontWeight(700),
                                letterSpacing = 0.04.sp,
                            )
                        )
                    }, shape = RoundedCornerShape(10.dp), enabled = true
                    )

                    Spacer(modifier = Modifier.size(11.dp))

                    Button(colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF34C2FF),
                        contentColor = MaterialTheme.colorScheme.onPrimary/* Other colors use values from MaterialTheme */
                    ), onClick = {}, modifier = Modifier.height(38.dp), content = {
                        Text(
                            text = "Correo", style = TextStyle(
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                fontFamily = latoFontFamily(),
                                fontWeight = FontWeight(700),
                                letterSpacing = 0.04.sp,
                            )
                        )
                    }, shape = RoundedCornerShape(10.dp), enabled = true
                    )
                }

            }
        }

    }
}