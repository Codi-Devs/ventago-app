package com.teco.ventago

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.teco.ventago.core.FingerPrintService

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun buildVariant(): BuildVariant {
//    if (BuildConfig.DEBUG) {
//        return BuildVariant.SANDBOX
//    }

    return BuildVariant.RELEASE
}

@Composable
actual fun isTablet(): Boolean {
    return LocalConfiguration.current.screenWidthDp >= 600
}
actual fun isAndroid(): Boolean = true
actual fun isIOS(): Boolean = false

@Composable
actual fun rememberPlatformState(): PlatformState {
    val context = LocalContext.current
    val state = remember(context) {
        AndroidPlatformState(context)
    }
    return state
}

// Android side platform state implementation
@Stable
internal class AndroidPlatformState(
    private val context: Context
) : PlatformState {
    override fun launchWindow(route: String) {
        context.startActivity(context.packageManager.getLaunchIntentForPackage(route))
    }

    override fun openEmailIntent(email: String) {
        com.teco.ventago.utils.openEmailIntent(email, context)
    }

    override fun requestNotificationPermission() {
        val activity = context as? MainActivity
        activity?.askNotificationPermission()
    }
}


