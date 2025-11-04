package com.teco.ventago
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.UIKit.UIScreen

class IOSPlatform: Platform {
    override val name: String = "IOS"
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun buildVariant(): BuildVariant {
    return BuildVariant.RELEASE
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun isTablet(): Boolean {
    val screenWidth = UIScreen.mainScreen.bounds.useContents { size.width }
    return screenWidth >= 768
}
actual fun isAndroid(): Boolean = false
actual fun isIOS(): Boolean = true


@Composable
actual fun rememberPlatformState(): PlatformState {
    val state = remember {
        IOSPlatformState()
    }
    return state
}

@Stable
internal class IOSPlatformState() : PlatformState {

    override fun launchWindow(route: String) {
//        context.startActivity(context.packageManager.getLaunchIntentForPackage(route))
    }

    override fun openEmailIntent(email: String) {
        com.teco.ventago.utils.openEmailIntent(email)
    }

}