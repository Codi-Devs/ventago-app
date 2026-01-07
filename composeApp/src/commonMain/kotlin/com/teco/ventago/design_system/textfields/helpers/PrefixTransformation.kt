package com.teco.ventago.design_system.textfields.helpers

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class PrefixTransformation(private val prefix: String): VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val prefixLength = prefix.length
        val result = AnnotatedString(prefix) + text
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return offset + prefixLength
            }

            override fun transformedToOriginal(offset: Int): Int {
                return (offset - prefixLength).coerceIn(0, text.length)
            }
        }
        return TransformedText(result, offsetMapping)
    }
}
