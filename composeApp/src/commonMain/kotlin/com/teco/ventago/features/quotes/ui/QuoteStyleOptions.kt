package com.teco.ventago.features.quotes.ui

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import ventago.composeapp.generated.resources.Res
import ventago.composeapp.generated.resources.quote_style_corporate
import ventago.composeapp.generated.resources.quote_style_minimal
import ventago.composeapp.generated.resources.quote_style_premium
import ventago.composeapp.generated.resources.quote_style_tech

internal data class QuoteStyleOption(
    val key: String,
    val label: String,
    val previewUrl: String
)

@Composable
internal fun quoteStyleOptions(): List<QuoteStyleOption> {
    return listOf(
        QuoteStyleOption(
            key = "style1",
            label = stringResource(Res.string.quote_style_minimal),
            previewUrl = "https://ventago.b-cdn.net/app/style1.jpg"
        ),
        QuoteStyleOption(
            key = "style2",
            label = stringResource(Res.string.quote_style_corporate),
            previewUrl = "https://ventago.b-cdn.net/app/style2.jpg"
        ),
        QuoteStyleOption(
            key = "style3",
            label = stringResource(Res.string.quote_style_premium),
            previewUrl = "https://ventago.b-cdn.net/app/style3.jpg"
        ),
        QuoteStyleOption(
            key = "style4",
            label = stringResource(Res.string.quote_style_tech),
            previewUrl = "https://ventago.b-cdn.net/app/stye4.jpg"
        )
    )
}
