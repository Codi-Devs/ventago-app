package com.teco.ventago.design_system.textfields.helpers


import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType

fun getKeyboardType(
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    capitalization: KeyboardCapitalization? = null,
): KeyboardOptions {
    val resolvedImeAction = if (
        imeAction == ImeAction.Default &&
        keyboardType in numericKeyboardTypes
    ) {
        ImeAction.Done
    } else {
        imeAction
    }
    return when (keyboardType) {
        KeyboardType.Text -> KeyboardOptions(
            keyboardType = keyboardType,
            autoCorrectEnabled = true,
            capitalization = capitalization ?: KeyboardCapitalization.Sentences,
            imeAction = resolvedImeAction
        )
        KeyboardType.Number -> KeyboardOptions(
            keyboardType = keyboardType,
            autoCorrectEnabled = true,
            capitalization = capitalization ?: KeyboardCapitalization.None,
            imeAction = resolvedImeAction
        )
        KeyboardType.Password -> KeyboardOptions(
            keyboardType = keyboardType,
            autoCorrectEnabled = false,
            capitalization = capitalization ?: KeyboardCapitalization.None,
            imeAction = resolvedImeAction
        )
        KeyboardType.Phone -> KeyboardOptions(
            keyboardType = keyboardType,
            autoCorrectEnabled = true,
            capitalization = capitalization ?: KeyboardCapitalization.None,
            imeAction = resolvedImeAction
        )
        KeyboardType.Email -> KeyboardOptions(
            keyboardType = keyboardType,
            autoCorrectEnabled = true,
            capitalization = capitalization ?: KeyboardCapitalization.None,
            imeAction = resolvedImeAction
        )
        KeyboardType.Uri -> KeyboardOptions(
            capitalization = capitalization ?: KeyboardCapitalization.None,
            autoCorrectEnabled = true,
            keyboardType = keyboardType,
            imeAction = resolvedImeAction
        )
        else -> KeyboardOptions(
            keyboardType = keyboardType,
            autoCorrectEnabled = true,
            imeAction = resolvedImeAction
        )
    }
}

private val numericKeyboardTypes = setOf(
    KeyboardType.Number,
    KeyboardType.Decimal,
    KeyboardType.NumberPassword,
    KeyboardType.Phone
)

