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
    return when (keyboardType) {
        KeyboardType.Text -> KeyboardOptions(
            keyboardType = keyboardType,
            autoCorrectEnabled = true,
            capitalization = capitalization ?: KeyboardCapitalization.Sentences,
            imeAction = imeAction
        )
        KeyboardType.Number -> KeyboardOptions(
            keyboardType = keyboardType,
            autoCorrectEnabled = true,
            capitalization = capitalization ?: KeyboardCapitalization.None,
            imeAction = imeAction
        )
        KeyboardType.Password -> KeyboardOptions(
            keyboardType = keyboardType,
            autoCorrectEnabled = false,
            capitalization = capitalization ?: KeyboardCapitalization.None,
            imeAction = imeAction
        )
        KeyboardType.Phone -> KeyboardOptions(
            keyboardType = keyboardType,
            autoCorrectEnabled = true,
            capitalization = capitalization ?: KeyboardCapitalization.None,
            imeAction = imeAction
        )
        KeyboardType.Email -> KeyboardOptions(
            keyboardType = keyboardType,
            autoCorrectEnabled = true,
            capitalization = capitalization ?: KeyboardCapitalization.None,
            imeAction = imeAction
        )
        KeyboardType.Uri -> KeyboardOptions(
            capitalization = capitalization ?: KeyboardCapitalization.None,
            autoCorrectEnabled = true,
            keyboardType = keyboardType,
            imeAction = imeAction
        )
        else -> KeyboardOptions(keyboardType = keyboardType, autoCorrectEnabled = true)
    }
}

