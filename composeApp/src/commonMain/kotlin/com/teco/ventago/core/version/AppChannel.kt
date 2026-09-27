package com.teco.ventago.core.version

enum class AppChannel {
    ANDROID_PUBLIC,
    ANDROID_POS,
    IOS_PUBLIC,
    ;

    val minUsableKey: String
        get() = when (this) {
            ANDROID_PUBLIC -> "app_min_usable_version_android_public"
            ANDROID_POS -> "app_min_usable_version_android_pos"
            IOS_PUBLIC -> "app_min_usable_version_ios_public"
        }

    val minRecommendedKey: String
        get() = when (this) {
            ANDROID_PUBLIC -> "app_min_recommended_version_android_public"
            ANDROID_POS -> "app_min_recommended_version_android_pos"
            IOS_PUBLIC -> "app_min_recommended_version_ios_public"
        }

    companion object {
        const val ANDROID_PUBLIC_PACKAGE = "com.teco.ventago"
        const val ANDROID_POS_PACKAGE = "com.teco.ventago.pos"
        const val IOS_PUBLIC_BUNDLE = "com.tecodigi.ventago.app"
        const val IOS_APP_STORE_ID = "6779462594"

        fun resolve(
            packageName: String,
            isPosBuild: Boolean,
            isIos: Boolean,
        ): AppChannel {
            if (isIos) return IOS_PUBLIC
            if (isPosBuild || packageName == ANDROID_POS_PACKAGE) return ANDROID_POS
            return ANDROID_PUBLIC
        }
    }

    fun marketUri(): String = when (this) {
        ANDROID_PUBLIC -> "market://details?id=$ANDROID_PUBLIC_PACKAGE"
        ANDROID_POS -> "market://details?id=$ANDROID_POS_PACKAGE"
        IOS_PUBLIC -> "itms-apps://itunes.apple.com/app/id$IOS_APP_STORE_ID"
    }

    fun httpsListingUrl(): String = when (this) {
        ANDROID_PUBLIC -> "https://play.google.com/store/apps/details?id=$ANDROID_PUBLIC_PACKAGE"
        ANDROID_POS -> "https://play.google.com/store/apps/details?id=$ANDROID_POS_PACKAGE"
        IOS_PUBLIC -> "https://apps.apple.com/app/id$IOS_APP_STORE_ID"
    }
}
