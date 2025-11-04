package com.teco.ventago.design_system.organism

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.sp
import com.teco.ventago.design_system.theme.latoFontFamily
import com.teco.ventago.utils.formatNumberToMoney

@Composable
fun PriceElevatedDecimalText(amount: Double) {
    val amountString = formatNumberToMoney(amount.toString())
    val integerPart = amountString.split(".")[0]
    val decimalPart = amountString.split(".")[1]
    val integerLength = integerPart.length
    Box {
        Text(
            text = buildAnnotatedString {
                append("$integerPart.")
                addStyle(
                    style = SpanStyle(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Black,
                        fontFamily = latoFontFamily(),
                    ),
                    start = 0,
                    end = integerLength + 1 // Adjust to include the text before "65"
                )
                append(decimalPart)
                addStyle(
                    style = SpanStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Black,
                        fontFamily = latoFontFamily(),
                        baselineShift = BaselineShift(0.4f) // Adjusts the vertical elevation
                    ),
                    start = integerLength + 1,
                    end = integerLength + 1 + decimalPart.length // Adjust to the position of "65"
                )
            }
        )
    }
}