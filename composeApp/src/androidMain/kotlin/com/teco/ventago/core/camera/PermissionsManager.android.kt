package com.teco.ventago.core.camera

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@Composable
actual fun createPermissionsManager(callback: PermissionCallback): PermissionsManager {
    return remember(callback) { PermissionsManager(callback) }
}

actual class PermissionsManager actual constructor(private val callback: PermissionCallback) :
    PermissionHandler {
    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    actual override fun askPermission(permission: PermissionType) {
        when (permission) {
            PermissionType.CAMERA -> {
                val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
                LaunchedEffect(Unit) {
                    if (!cameraPermissionState.status.isGranted) {
                        cameraPermissionState.launchPermissionRequest()
                    }
                }
                LaunchedEffect(cameraPermissionState.status) {
                    val permissionResult = cameraPermissionState.status
                    if (permissionResult.isGranted) {
                        callback.onPermissionStatus(permission, PermissionStatus.GRANTED)
                    } else if (permissionResult.shouldShowRationale) {
                        callback.onPermissionStatus(permission, PermissionStatus.SHOW_RATIONAL)
                    }
                }
            }

            PermissionType.GALLERY -> {
                // Granted by default because in Android GetContent API does not require any
                // runtime permissions, and i am using it to access gallery in my app
                callback.onPermissionStatus(
                    permission, PermissionStatus.GRANTED
                )
            }
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    actual override fun isPermissionGranted(permission: PermissionType): Boolean {
        return when (permission) {
            PermissionType.CAMERA -> {
                val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
                cameraPermissionState.status.isGranted
            }

            PermissionType.GALLERY -> {
                // Granted by default because in Android GetContent API does not require any
                // runtime permissions, and i am using it to access gallery in my app
                true
            }
        }
    }

    @Composable
    actual override fun launchSettings() {
        val context = LocalContext.current
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).also {
            context.startActivity(it)
        }
    }
}
