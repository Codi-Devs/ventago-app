package com.teco.ventago.features.pos.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import com.teco.ventago.core.keyboard.dismissKeyboardOnOutsideTap as dismissKeyboardOnOutsideTapCore

internal fun Modifier.dismissKeyboardOnOutsideTap(
    @Suppress("UNUSED_PARAMETER") focusManager: FocusManager
): Modifier = dismissKeyboardOnOutsideTapCore()
