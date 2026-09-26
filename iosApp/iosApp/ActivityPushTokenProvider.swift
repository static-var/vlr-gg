import ActivityKit
import Foundation
import shared
import UIKit

/// Watches Live Activities and caches logos when their teams change.
@available(iOS 16.2, *)
@MainActor
final class MatchActivityLogoObserver {
    static let shared = MatchActivityLogoObserver()

    private var activityTask: Task<Void, Never>?
    private var contentTasks: [String: Task<Void, Never>] = [:]

    /// Observes existing activities and any activities started later.
    /// Repeated calls reuse the active observation task.
    func start() {
        guard activityTask == nil else { return }
        activityTask = Task {
            for activity in Activity<MatchActivityAttributes>.activities {
                observe(activity)
            }
            for await activity in Activity<MatchActivityAttributes>.activityUpdates {
                observe(activity)
            }
        }
    }

    /// Records the match and caches logos as its activity content changes.
    /// Stops watching when the activity ends, is dismissed, or receives a final score.
    private func observe(_ activity: Activity<MatchActivityAttributes>) {
        ObservedMatchActivities.record(activity.attributes.match_id)
        guard contentTasks[activity.id] == nil,
              activity.activityState != .ended,
              activity.activityState != .dismissed else { return }
        contentTasks[activity.id] = Task {
            let stateTask = Task {
                for await state in activity.activityStateUpdates {
                    if state == .ended || state == .dismissed {
                        contentTasks[activity.id]?.cancel()
                        break
                    }
                }
            }
            defer {
                stateTask.cancel()
                contentTasks[activity.id] = nil
            }
            guard activity.activityState != .ended,
                  activity.activityState != .dismissed else { return }
            await prefetch(activity.content.state)
            guard !Task.isCancelled, !activity.content.state.terminal else { return }
            for await content in activity.contentUpdates {
                guard !Task.isCancelled else { return }
                await prefetch(content.state)
                if content.state.terminal { break }
            }
        }
    }

    private func prefetch(_ state: MatchActivityAttributes.ContentState) async {
        await MatchActivityLogoCache.prefetch(state.teams.compactMap(\.img))
    }
}

/// Remembers matches seen in current and recent Live Activities.
@available(iOS 16.2, *)
enum ObservedMatchActivities {
    private static let key = "notifications.observedLiveActivityMatchIds"
    private static let endingKey = "notifications.endingLiveActivityIds"
    private static let limit = 256

    static func record(_ matchID: String) {
        let previous = UserDefaults.standard.stringArray(forKey: key) ?? []
        UserDefaults.standard.set(Array((previous.filter { $0 != matchID } + [matchID]).suffix(limit)), forKey: key)
    }

    static func all() -> [String] {
        let stored = UserDefaults.standard.stringArray(forKey: key) ?? []
        let ending = Set(UserDefaults.standard.stringArray(forKey: endingKey) ?? [])
        let current = Activity<MatchActivityAttributes>.activities
            .filter { $0.activityState != .ended && $0.activityState != .dismissed && !ending.contains($0.id) }
            .map { $0.attributes.match_id }
        for matchID in current where !stored.contains(matchID) {
            record(matchID)
        }
        return Array(Set(stored + current))
    }

    static func forget(_ matchID: String) {
        let stored = UserDefaults.standard.stringArray(forKey: key) ?? []
        if stored.contains(matchID) {
            UserDefaults.standard.set(stored.filter { $0 != matchID }, forKey: key)
        }
    }

    static func clear() {
        if !(UserDefaults.standard.stringArray(forKey: key) ?? []).isEmpty {
            UserDefaults.standard.set([], forKey: key)
        }
    }

    static func markEnding(_ activityID: String) {
        let stored = UserDefaults.standard.stringArray(forKey: endingKey) ?? []
        if !stored.contains(activityID) {
            UserDefaults.standard.set(Array((stored + [activityID]).suffix(limit)), forKey: endingKey)
        }
    }

    static func finishEnding(_ activityID: String) {
        let stored = UserDefaults.standard.stringArray(forKey: endingKey) ?? []
        if stored.contains(activityID) {
            UserDefaults.standard.set(stored.filter { $0 != activityID }, forKey: endingKey)
        }
    }

    static func clearEnding() {
        if !(UserDefaults.standard.stringArray(forKey: endingKey) ?? []).isEmpty {
            UserDefaults.standard.set([], forKey: endingKey)
        }
    }
}

/// Exposes iOS Live Activity availability and observed matches to shared code.
final class IosLiveUpdateStateProvider: NSObject, LiveUpdateStateProvider {
    var platform: PushPlatform { .ios }

    func canRequestStart() -> Bool { true }

    func observedMatchIds() -> [String] {
        guard #available(iOS 16.2, *) else { return [] }
        return ObservedMatchActivities.all()
    }
}

/// Forwards new iOS push-to-start tokens to shared code.
final class IosActivityPushTokenProvider: NSObject, PushTokenProvider {
    var platform: PushPlatform { .ios }

    private var observationTask: Task<Void, Never>?

    /// Emits the current push-to-start token, then watches for replacements.
    /// Suppresses duplicate tokens and keeps only one observation task running.
    func start(onToken: @escaping (String) -> Void) {
        guard observationTask == nil else { return }
        guard #available(iOS 18.0, *) else { return }

        observationTask = Task { @MainActor in
            var lastToken: String?
            guard !Task.isCancelled else { return }

            if let tokenData = Activity<MatchActivityAttributes>.pushToStartToken {
                guard !Task.isCancelled else { return }
                let token = tokenData.hexadecimalString
                lastToken = token
                onToken(token)
            }

            for await tokenData in Activity<MatchActivityAttributes>.pushToStartTokenUpdates {
                guard !Task.isCancelled else { return }
                let token = tokenData.hexadecimalString
                guard token != lastToken else { continue }
                lastToken = token
                onToken(token)
            }
        }
    }

    func stop() {
        observationTask?.cancel()
        observationTask = nil
    }

    deinit {
        observationTask?.cancel()
    }
}

private extension Data {
    var hexadecimalString: String {
        map { String(format: "%02x", $0) }.joined()
    }
}

@available(iOS 16.2, *)
@MainActor
final class IosMatchActivityOptOut {
    static let shared = IosMatchActivityOptOut()

    private let preferenceKey = "notifications.favorites"
    private var observationTask: Task<Void, Never>?
    private var notificationObservers: [NSObjectProtocol] = []
    private var endingActivityIDs: Set<String> = []

    func start() {
        guard observationTask == nil else { return }
        if UserDefaults.standard.bool(forKey: preferenceKey) {
            ObservedMatchActivities.clearEnding()
        }
        let center = NotificationCenter.default
        for name in [UserDefaults.didChangeNotification, UIApplication.didBecomeActiveNotification] {
            notificationObservers.append(center.addObserver(forName: name, object: nil, queue: .main) { [weak self] _ in
                Task { @MainActor in self?.reconcile() }
            })
        }
        reconcile()
        observationTask = Task {
            for await activity in Activity<MatchActivityAttributes>.activityUpdates {
                endIfDisabled(activity)
            }
        }
    }

    func preferenceChanged(_ enabled: Bool) {
        if !enabled { reconcile() }
    }

    func reconcile() {
        guard !UserDefaults.standard.bool(forKey: preferenceKey) else { return }
        ObservedMatchActivities.clear()
        for activity in Activity<MatchActivityAttributes>.activities {
            endIfDisabled(activity)
        }
    }

    private func endIfDisabled(_ activity: Activity<MatchActivityAttributes>) {
        guard !UserDefaults.standard.bool(forKey: preferenceKey),
              activity.activityState != .ended,
              activity.activityState != .dismissed,
              endingActivityIDs.insert(activity.id).inserted else { return }
        ObservedMatchActivities.markEnding(activity.id)
        ObservedMatchActivities.forget(activity.attributes.match_id)
        Task {
            await activity.end(nil, dismissalPolicy: .immediate)
            endingActivityIDs.remove(activity.id)
            ObservedMatchActivities.finishEnding(activity.id)
            ObservedMatchActivities.forget(activity.attributes.match_id)
        }
    }
}
