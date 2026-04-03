import SwiftUI
import Firebase
import FirebaseAuth
import FirebaseFirestore
import FirebaseMessaging
import UIKit
import UserNotifications
import ComposeApp

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
    private var latestFcmToken: String?
    private var authStateHandle: AuthStateDidChangeListenerHandle?

    // Cold start: still do your setup
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        FirebaseApp.configure()
        ComposeApp.AppModuleKt.doInitKoinIOS()
        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self

        authStateHandle = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            guard let self = self, let token = self.latestFcmToken, user != nil else { return }
            self.saveTokenToFirestore(token: token)
        }

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

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let fcmToken else { return }
        latestFcmToken = fcmToken
        saveTokenToFirestore(token: fcmToken)
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.banner, .sound, .badge])
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

    private func saveTokenToFirestore(token: String) {
        guard let uid = Auth.auth().currentUser?.uid else { return }
        let reference = Firestore.firestore().collection("device_tokens").document(uid)
        reference.setData(["tokens": FieldValue.arrayUnion([token])], merge: true)
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
