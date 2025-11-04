import SwiftUI
import Firebase
import UIKit
import ComposeApp

class AppDelegate: NSObject, UIApplicationDelegate {
    // Cold start: still do your setup
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        FirebaseApp.configure()
        ComposeApp.AppModuleKt.doInitKoinIOS()

        // If the app was launched by a Universal Link, iOS *may* include it here.
        // (Not always; the more reliable path is `application(:continue:)` below.)
        if let activityDict = launchOptions?[.userActivityDictionary] as? [AnyHashable: Any],
           let activities = activityDict["UIApplicationLaunchOptionsUserActivitiesKey"] as? Set<NSUserActivity>,
           let activity = activities.first,
           activity.activityType == NSUserActivityTypeBrowsingWeb,
           let url = activity.webpageURL {
            ExternalUriHandler.shared.onNewUri(uri: url.absoluteString)
        }

        return true
    }

    // Warm/cold path for Universal Links (most reliable entry point)
    func application(_ application: UIApplication,
                     continue userActivity: NSUserActivity,
                     restorationHandler: @escaping ([UIUserActivityRestoring]?) -> Void) -> Bool {
        guard userActivity.activityType == NSUserActivityTypeBrowsingWeb,
              let url = userActivity.webpageURL else { return false }

        ExternalUriHandler.shared.onNewUri(uri: url.absoluteString)
        return true
    }

    // (Optional) Fallback if you also support a custom URL scheme like ventago://...
    func application(_ app: UIApplication,
                     open url: URL,
                     options: [UIApplication.OpenURLOptionsKey : Any] = [:]) -> Bool {
        ExternalUriHandler.shared.onNewUri(uri: url.absoluteString)
        return true
    }
}


@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}