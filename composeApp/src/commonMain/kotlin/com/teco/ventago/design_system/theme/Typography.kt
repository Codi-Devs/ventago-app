package com.teco.ventago.design_system.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import ventago.composeapp.generated.resources.Lato_Black
import ventago.composeapp.generated.resources.Lato_Bold
import ventago.composeapp.generated.resources.Lato_Light
import ventago.composeapp.generated.resources.Lato_Regular
import ventago.composeapp.generated.resources.Lato_Thin
import ventago.composeapp.generated.resources.Res

@Composable
fun latoFontFamily() = FontFamily(
    Font(Res.font.Lato_Thin, weight = FontWeight.Thin),
    Font(Res.font.Lato_Light, weight = FontWeight.Light),
    Font(Res.font.Lato_Regular, weight = FontWeight.Normal),
    Font(Res.font.Lato_Regular, weight = FontWeight.Medium),
    Font(Res.font.Lato_Bold, weight = FontWeight.SemiBold),
    Font(Res.font.Lato_Black, weight = FontWeight.Black)
)


@Composable
fun LatoTypography() = Typography().run {

    val fontFamily = latoFontFamily()
    copy(
        displayLarge = displayLarge.copy(fontFamily = fontFamily),
        displayMedium = displayMedium.copy(fontFamily = fontFamily),
        displaySmall = displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = titleLarge.copy(fontFamily = fontFamily),
        titleMedium = titleMedium.copy(fontFamily = fontFamily),
        titleSmall = titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = bodySmall.copy(fontFamily = fontFamily),
        labelLarge = labelLarge.copy(fontFamily = fontFamily),
        labelMedium = labelMedium.copy(fontFamily = fontFamily),
        labelSmall = labelSmall.copy(fontFamily = fontFamily)
    )
}

@Composable
fun headlineSmall(): TextStyle {
    return MaterialTheme.typography.headlineSmall
}

@Composable
fun headlineMediumBold(color: Color? = null): TextStyle {
    return MaterialTheme.typography.headlineMedium.copy(
        color = color ?: MaterialTheme.typography.headlineMedium.color,
        fontWeight = FontWeight.W700,
    )
}

@Composable
fun headlineLarge(): TextStyle {
    return MaterialTheme.typography.headlineLarge
}

@Composable
fun titleLarge(color: Color? = null): TextStyle {
    return MaterialTheme.typography.titleLarge.copy(
        color = color ?: MaterialTheme.typography.titleLarge.color,
    )
}

@Composable
fun titleMedium(): TextStyle {
    return MaterialTheme.typography.titleMedium
}

@Composable
fun titleMediumBold(color: Color? = null): TextStyle {
    return MaterialTheme.typography.titleMedium.copy(
        fontWeight = FontWeight.W700,
        color = color ?: MaterialTheme.typography.titleMedium.color,
    )
}

@Composable
fun titleSmallBold(): TextStyle {
    return MaterialTheme.typography.titleSmall.copy(
        fontWeight = FontWeight.W700,
    )
}

// Label
@Composable
fun labelLarge(): TextStyle {
    return MaterialTheme.typography.labelLarge
}

@Composable
fun labelMedium(color: Color? = null): TextStyle {
    return MaterialTheme.typography.labelMedium.copy(
        color = color ?: MaterialTheme.typography.labelMedium.color,
    )
}

@Composable
fun labelMediumBold(): TextStyle {
    return MaterialTheme.typography.labelMedium.copy(
        fontWeight = FontWeight.W700,
    )
}

@Composable
fun labelSmall(color: Color? = null): TextStyle {
    return MaterialTheme.typography.labelSmall.copy(
        color = color ?: MaterialTheme.typography.labelSmall.color,
    )
}

@Composable
fun buttonMBold(): TextStyle {
    return MaterialTheme.typography.labelLarge.copy(
        fontWeight = FontWeight.ExtraBold,
    )
}

@Composable
fun bodyLarge(color: Color? = null): TextStyle {
    return MaterialTheme.typography.bodyLarge.copy(
        color = color ?: MaterialTheme.typography.bodyLarge.color,
    )
}

@Composable
fun bodyLargeBold(color: Color? = null): TextStyle {
    return MaterialTheme.typography.bodyLarge.copy(
        fontWeight = FontWeight.W700,
        color = color ?: MaterialTheme.typography.bodyLarge.color,
    )
}

@Composable
fun bodyMedium(color: Color? = null): TextStyle {
    return MaterialTheme.typography.bodyMedium.copy(
        color = color ?: MaterialTheme.typography.bodyMedium.color,
    )
}

@Composable
fun bodySmall(color: Color? = null): TextStyle {
    return MaterialTheme.typography.bodySmall.copy(
        color = color ?: MaterialTheme.typography.bodySmall.color,
    )
}

@Composable
fun headlineLargeBold(): TextStyle {
    return MaterialTheme.typography.headlineLarge.copy(
        fontWeight = FontWeight.W700,
    )
}

@Composable
fun titleSmall(color: Color? = null): TextStyle {
    return MaterialTheme.typography.titleSmall.copy(
        color = color ?: MaterialTheme.typography.titleSmall.color,
    )
}

@Composable
fun bodyMediumBold(color: Color? = null): TextStyle {
    return MaterialTheme.typography.bodyMedium.copy(
        fontWeight = FontWeight.W700,
        color = color ?: MaterialTheme.typography.bodyMedium.color,
    )
}