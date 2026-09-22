package com.teco.ventago.core.keyboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import platform.UIKit.UIApplication
import platform.UIKit.UIView
import platform.UIKit.UIWindow
import platform.UIKit.endEditing

@Composable
actual fun rememberHideSoftwareKeyboard(): () -> Unit {
    val controller = LocalSoftwareKeyboardController.current
    return remember(controller) {
        {
            controller?.hide()
            hideIosKeyboard()
        }
    }
}

private fun hideIosKeyboard() {
    val application = UIApplication.sharedApplication
    val keyWindow = application.keyWindow
        ?: (application.windows.firstOrNull() as? UIWindow)
    (keyWindow as? UIView)?.endEditing(force = true)
    application.windows.forEach { candidate ->
        (candidate as? UIView)?.endEditing(force = true)
    }
}
