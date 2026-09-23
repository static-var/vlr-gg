import ActivityKit
import UIKit
import UserNotifications
import shared

final class IosLiveActivityPermissionProvider: NSObject, NotificationPermissionProvider {
    func areLiveActivitiesEnabled() -> KotlinBoolean? {
        guard #available(iOS 17.2, *) else { return KotlinBoolean(bool: false) }
        return KotlinBoolean(bool: ActivityAuthorizationInfo().areActivitiesEnabled)
    }

    func readNotificationAuthorization(onResult: @escaping (NotificationAuthorization) -> Void) {
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            let authorization: NotificationAuthorization
            switch settings.authorizationStatus {
            case .notDetermined:
                authorization = .notdetermined
            case .authorized, .provisional, .ephemeral:
                authorization = .authorized
            case .denied:
                authorization = .denied
            @unknown default:
                authorization = .error
            }
            DispatchQueue.main.async {
                onResult(authorization)
            }
        }
    }

    func requestNotificationAuthorization(onResult: @escaping (NotificationAuthorization) -> Void) {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound]) { _, error in
            if error != nil {
                DispatchQueue.main.async {
                    onResult(.error)
                }
                return
            }
            self.readNotificationAuthorization(onResult: onResult)
        }
    }

    func openSettings() {
        guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
        DispatchQueue.main.async {
            UIApplication.shared.open(url)
        }
    }
}
