package com.teco.ventago.design_system.textfields.helpers

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class PrefixTransformation(private val prefix: String): VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val result = AnnotatedString(prefix) + text
        return TransformedText(result, OffsetMapping.Identity)
    }
}