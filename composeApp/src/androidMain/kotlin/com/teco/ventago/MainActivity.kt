package com.teco.ventago

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.libraries.places.widget.Autocomplete
import com.teco.ventago.core.deeplink.ExternalUriHandler
import com.teco.ventago.features.business.domain.model.BusinessAddress
import com.teco.ventago.utils.AutocompleteLauncher
import org.koin.mp.KoinPlatform.getKoin


class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
        } else {
            // TODO: Inform user that that your app will not show notifications.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        getKoin().get<MainActivityHolder>().activity = this
        super.onCreate(savedInstanceState)

        intent?.data?.let { uri: Uri ->
            ExternalUriHandler.onNewUri(uri.toString())
        }

        AutocompleteLauncher.launcher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val intent = result.data
            if (result.resultCode == RESULT_OK && intent != null) {
                val place = Autocomplete.getPlaceFromIntent(intent)
                AutocompleteLauncher.onResult?.invoke(BusinessAddress(
                    place.id ?: "",
                    place.formattedAddress ?: "",
                    place.location?.latitude ?: -1.0,
                    place.location?.longitude ?: -1.0
                ))
            } else {
                AutocompleteLauncher.onResult?.invoke(null)
            }
        }

        setContent {
            val darkTheme = isSystemInDarkTheme()
            val view = LocalView.current
            SideEffect {
                val window = (view.context as Activity).window
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.isAppearanceLightStatusBars = !darkTheme
            }

            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { uri: Uri ->
            ExternalUriHandler.onNewUri(uri.toString())
        }
    }

    fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                AlertDialog.Builder(this)
                    .setTitle("Notification Permission")
                    .setMessage("This app requires notification permission to show important updates, or notifying payment status changes.")
                    .setPositiveButton("OK") { _, _ ->
                        // Directly ask for the permission
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    .setNegativeButton("No thanks") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}


@Composable
fun AndroidApp() {
    App()
}
