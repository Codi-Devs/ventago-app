package com.teco.ventago

import androidx.compose.runtime.Composable
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun httpClient(config: HttpClientConfig<*>.() -> Unit = {}): HttpClient
expect fun buildVariant(): BuildVariant

@Composable
expect fun isTablet(): Boolean
expect fun isAndroid(): Boolean
expect fun isIOS(): Boolean

object AppInfo {
    const val APP_NAME = "VentaGo"
    const val VERSION = "1.3.0"
}



interface PlatformState {
    fun launchWindow(route: String)
    fun openEmailIntent(email: String)
    fun requestNotificationPermission()
}

@Composable
expect fun rememberPlatformState(): PlatformState


enum class BuildVariant {
    RELEASE,
    SANDBOX,
}

object Configs {
    val serverBasePath: String
    val ordersBasePath: String

    init {
        if (buildVariant() == BuildVariant.SANDBOX) {
            serverBasePath = SandboxConfigs.serverBasePath
            ordersBasePath = SandboxConfigs.ordersBasePath
        } else {
            serverBasePath = ReleaseConfigs.serverBasePath
            ordersBasePath = ReleaseConfigs.ordersBasePath
        }
    }
}

object ReleaseConfigs {
//    const val serverBasePath: String = "http://10.0.2.2:8080/index.php?r="
    const val serverBasePath: String = "https://business-vg.tecodigi.com/index.php?r="

//       const val ordersBasePath: String = "http://192.168.40.165:5001"
     const val ordersBasePath: String = "https://invoice-vg.tecodigi.com"
}

object SandboxConfigs {
//    const val serverBasePath: String = "http://10.0.2.2:8080/index.php?r="
    const val serverBasePath: String = "https://business-vg.tecodigi.com/index.php?r="

//       const val ordersBasePath: String = "http://192.168.40.165:5001"
     const val ordersBasePath: String = "https://invoice-vg.tecodigi.com"
}
