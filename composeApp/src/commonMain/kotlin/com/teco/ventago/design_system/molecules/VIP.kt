package com.teco.ventago.design_system.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import com.teco.ventago.design_system.theme.VIPColor
import com.teco.ventago.design_system.theme.bodyMediumBold
import com.teco.ventago.design_system.theme.latoFontFamily
import org.jetbrains.compose.resources.painterResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.fi_sr_star

@Composable
fun VIPBenefitItem(title: String, modifier: Modifier = Modifier, shipColor: Color = VIPColor) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Icon(

            painter = painterResource(Res.drawable.fi_sr_star),
            contentDescription = "",
            tint = VIPColor,
        )

        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = bodyMediumBold(),
        )

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .height(21.dp)
                .background(
                    color = shipColor, shape = RoundedCornerShape(size = 10.dp)
                ), contentAlignment = Alignment.Center
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = "Premium", style = TextStyle(
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontFamily = latoFontFamily(),
                    fontWeight = FontWeight(700),
                    color = Color(0xFFFFFFFF),

                    textAlign = TextAlign.Center,
                )
            )
        }


    }
}