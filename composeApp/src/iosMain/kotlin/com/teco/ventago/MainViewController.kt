package com.teco.ventago

import androidx.compose.ui.window.ComposeUIViewController
import platform.Foundation.NSUserDefaults

fun MainViewController() = run {
    NSUserDefaults.standardUserDefaults.setObject(listOf("es"), forKey = "AppleLanguages")
    ComposeUIViewController { App() }
}