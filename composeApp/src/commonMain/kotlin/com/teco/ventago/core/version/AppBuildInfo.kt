package com.teco.ventago.core.version

data class AppBuildInfo(
    val channel: AppChannel,
    val packageName: String,
    val versionCode: Long,
    val versionName: String,
)
