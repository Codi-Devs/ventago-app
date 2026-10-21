package com.teco.ventago.features.printers.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
actual fun BluetoothPermissionEffect(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
) {
    LaunchedEffect(Unit) {
        onGranted()
    }
}
