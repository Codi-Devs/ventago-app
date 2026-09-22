package com.teco.ventago.core.keyboard

import androidx.compose.runtime.Composable

@Composable
expect fun rememberHideSoftwareKeyboard(): () -> Unit
