package com.teco.ventago.features.printers.ui

import androidx.compose.runtime.Composable

@Composable
expect fun BluetoothPermissionEffect(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
)
