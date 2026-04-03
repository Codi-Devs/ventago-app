package com.teco.ventago.core.firebase

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIApplication
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.Foundation.NSSelectorFromString
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
actual fun getToken() {
    val options = UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound
    UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(options) { granted, _ ->
        if (!granted) return@requestAuthorizationWithOptions
        dispatch_async(dispatch_get_main_queue()) {
            val app = UIApplication.sharedApplication
            val selector = NSSelectorFromString("registerForRemoteNotifications")
            if (app.respondsToSelector(selector)) {
                app.performSelector(selector)
            }
        }
    }
}
