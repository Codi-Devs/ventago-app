package com.teco.ventago.features.pos.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.pointer.pointerInput

internal fun Modifier.dismissKeyboardOnOutsideTap(focusManager: FocusManager): Modifier =
    pointerInput(focusManager) {
        detectTapGestures(onTap = {
            focusManager.clearFocus()
        })
    }
