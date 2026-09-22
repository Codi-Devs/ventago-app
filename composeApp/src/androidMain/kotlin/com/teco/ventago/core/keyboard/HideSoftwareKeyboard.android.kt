package com.teco.ventago.core.keyboard

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView

@Composable
actual fun rememberHideSoftwareKeyboard(): () -> Unit {
    val view = LocalView.current
    val controller = LocalSoftwareKeyboardController.current
    return remember(view, controller) {
        {
            controller?.hide()
            val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}
