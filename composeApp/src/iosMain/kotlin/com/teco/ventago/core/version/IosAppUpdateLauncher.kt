package com.teco.ventago.core.version

import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

class IosAppUpdateLauncher : IAppUpdateLauncher {
    override suspend fun startForcedUpdate() {
        openStoreListing()
    }

    override suspend fun startRecommendedUpdate() {
        openStoreListing()
    }

    override fun openStoreListing() {
        val url = NSURL.URLWithString(AppChannel.IOS_PUBLIC.httpsListingUrl()) ?: return
        UIApplication.sharedApplication.openURL(url)
    }
}

fun iosAppBuildInfo(): AppBuildInfo {
    val bundle = NSBundle.mainBundle
    val packageName = bundle.bundleIdentifier ?: AppChannel.IOS_PUBLIC_BUNDLE
    val versionCode = (bundle.objectForInfoDictionaryKey("CFBundleVersion") as? String)
        ?.toLongOrNull()
        ?: 0L
    val versionName = bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: ""
    return AppBuildInfo(
        channel = AppChannel.resolve(
            packageName = packageName,
            isPosBuild = false,
            isIos = true,
        ),
        packageName = packageName,
        versionCode = versionCode,
        versionName = versionName,
    )
}
