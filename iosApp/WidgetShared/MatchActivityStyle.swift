import Foundation

/// Selects the Live Activity layout. Raw values match the shared Kotlin `LiveActivityStyle` storage values.
enum MatchActivityStyle: String, CaseIterable, Codable {
    case classic
    case glance

    init(storageValue: String?) {
        self = storageValue.flatMap(MatchActivityStyle.init(rawValue:)) ?? .classic
    }
}

/// Shares the selected Live Activity style between the app and the widget extension.
/// Shared Kotlin settings write `appKey` to standard defaults; the app mirrors it into the app group.
enum MatchActivityStylePreference {
    static let appKey = "notifications.liveActivityStyle"
    static let sharedKey = "liveActivity.style"

    static var sharedDefaults: UserDefaults? {
        UserDefaults(suiteName: VLRWidgetContract.appGroupIdentifier)
    }

    static func current(in defaults: UserDefaults? = sharedDefaults) -> MatchActivityStyle {
        MatchActivityStyle(storageValue: defaults?.string(forKey: sharedKey))
    }

    /// Copies the app's choice into the app group.
    /// Returns the new style when it differs from the style Live Activities last rendered.
    @discardableResult
    static func mirror(from app: UserDefaults = .standard, to shared: UserDefaults? = sharedDefaults) -> MatchActivityStyle? {
        guard let shared else { return nil }
        let style = MatchActivityStyle(storageValue: app.string(forKey: appKey))
        let stored = shared.string(forKey: sharedKey)
        guard stored != style.rawValue else { return nil }
        shared.set(style.rawValue, forKey: sharedKey)
        return MatchActivityStyle(storageValue: stored) == style ? nil : style
    }
}
