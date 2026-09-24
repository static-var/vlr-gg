import ActivityKit
import UIKit
import shared

/// Reports iOS Live Activity support and opens the app’s system settings.
final class IosLiveActivityPermissionProvider: NSObject, NotificationPermissionProvider {
    func supportsLiveUpdates() -> Bool {
        if #available(iOS 17.2, *) {
            return true
        }
        return false
    }

    func requiresNotificationPermission() -> Bool {
        false
    }

    func areLiveActivitiesEnabled() -> KotlinBoolean? {
        guard #available(iOS 17.2, *) else { return KotlinBoolean(bool: false) }
        return KotlinBoolean(bool: ActivityAuthorizationInfo().areActivitiesEnabled)
    }

    func canPromoteNotifications() -> KotlinBoolean? {
        nil
    }

    func openPromotionSettings() {
        openSettings()
    }

    func readNotificationAuthorization(onResult: @escaping (NotificationAuthorization) -> Void) {
        onResult(.error)
    }

    func requestNotificationAuthorization(onResult: @escaping (NotificationAuthorization) -> Void) {
        onResult(.error)
    }

    func openSettings() {
        guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
        DispatchQueue.main.async {
            UIApplication.shared.open(url)
        }
    }
}
