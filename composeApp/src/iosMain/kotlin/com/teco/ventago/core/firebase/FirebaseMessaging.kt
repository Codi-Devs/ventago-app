package com.teco.ventago.core.firebase

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIApplication
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import platform.Foundation.NSSelectorFromString
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
actual fun getToken() {
    UNUserNotificationCenter.currentNotificationCenter()
        .getNotificationSettingsWithCompletionHandler { settings ->
            val status = settings?.authorizationStatus ?: return@getNotificationSettingsWithCompletionHandler
            if (status == UNAuthorizationStatusAuthorized || status == UNAuthorizationStatusProvisional) {
                registerForRemoteNotifications()
            }
        }
}

@OptIn(ExperimentalForeignApi::class)
private fun registerForRemoteNotifications() {
    dispatch_async(dispatch_get_main_queue()) {
        val app = UIApplication.sharedApplication
        val selector = NSSelectorFromString("registerForRemoteNotifications")
        if (app.respondsToSelector(selector)) {
            app.performSelector(selector)
        }
    }
}
