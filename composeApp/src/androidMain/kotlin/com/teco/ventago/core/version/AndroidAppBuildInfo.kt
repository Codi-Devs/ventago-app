package com.teco.ventago.core.version

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.teco.ventago.AppDistribution

fun androidAppBuildInfo(context: Context, distribution: AppDistribution): AppBuildInfo {
    val packageName = context.packageName
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(packageName, 0)
    }
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        info.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        info.versionCode.toLong()
    }
    return AppBuildInfo(
        channel = AppChannel.resolve(
            packageName = packageName,
            isPosBuild = distribution.isPosBuild,
            isIos = false,
        ),
        packageName = packageName,
        versionCode = versionCode,
        versionName = info.versionName.orEmpty(),
    )
}
