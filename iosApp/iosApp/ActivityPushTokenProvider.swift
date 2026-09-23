import ActivityKit
import Foundation
import shared

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
private enum ObservedMatchActivities {
    private static let key = "notifications.observedLiveActivityMatchIds"
    private static let limit = 256

    static func record(_ matchID: String) {
        let previous = UserDefaults.standard.stringArray(forKey: key) ?? []
        UserDefaults.standard.set(Array((previous.filter { $0 != matchID } + [matchID]).suffix(limit)), forKey: key)
    }

    static func all() -> [String] {
        let stored = UserDefaults.standard.stringArray(forKey: key) ?? []
        let current = Activity<MatchActivityAttributes>.activities.map { $0.attributes.match_id }
        for matchID in current where !stored.contains(matchID) {
            record(matchID)
        }
        return Array(Set(stored + current))
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
        guard #available(iOS 17.2, *) else { return }

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

#if DEBUG && targetEnvironment(simulator)
/// Runs Live Activity test commands supplied through the simulator environment.
@available(iOS 16.2, *)
@MainActor
enum SimulatorMatchActivity {
    private static var hasRun = false

    /// Runs one environment-supplied command against the synthetic test match.
    /// Writes the activity state or failure to a cache file for test inspection.
    static func runIfRequested() async {
        guard !hasRun,
              let json = ProcessInfo.processInfo.environment["VLR_TEST_LIVE_ACTIVITY"] else { return }
        hasRun = true
        do {
            let payload = try JSONDecoder().decode(Payload.self, from: Data(json.utf8))
            guard payload.state.match_id == "3141592653" else {
                throw TestError.unsupportedMatch
            }
            if payload.event != .inspect {
                await MatchActivityLogoCache.prefetch(payload.state.teams.compactMap(\.img))
            }
            let content = ActivityContent(state: payload.state, staleDate: Date().addingTimeInterval(180))
            let activities = Activity<MatchActivityAttributes>.activities.filter {
                $0.attributes.match_id == payload.state.match_id
            }
            switch payload.event {
            case .inspect:
                let states = try JSONEncoder().encode(activities.map { $0.content.state })
                writeResult([
                    "event": "inspect",
                    "status": "succeeded",
                    "states": String(decoding: states, as: UTF8.self),
                    "phases": activities.map { String(describing: $0.activityState) }.joined(separator: ","),
                ])
            case .start:
                for activity in activities {
                    await activity.end(nil, dismissalPolicy: .immediate)
                }
                let activity = try Activity.request(
                    attributes: MatchActivityAttributes(match_id: payload.state.match_id),
                    content: content,
                    pushType: nil
                )
                writeResult(["event": "start", "activity_id": activity.id, "status": "succeeded"])
            case .update, .end:
                guard !activities.isEmpty else { throw TestError.noActivity }
                for activity in activities {
                    if payload.event == .end {
                        await activity.end(content, dismissalPolicy: .after(Date().addingTimeInterval(3600)))
                    } else {
                        await activity.update(content)
                    }
                }
                writeResult(["event": payload.event.rawValue, "status": "succeeded"])
            }
        } catch {
            writeResult(["status": "failed", "error": String(describing: error)])
        }
    }

    private static func writeResult(_ value: [String: String]) {
        guard let directory = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first,
              let data = try? JSONEncoder().encode(value) else { return }
        try? data.write(to: directory.appendingPathComponent("live-activity-test-result.json"), options: .atomic)
    }

    /// Pairs a simulator test command with its match state.
    private struct Payload: Decodable {
        /// Lists the Live Activity commands supported by simulator tests.
        enum Event: String, Decodable { case start, update, end, inspect }
        let event: Event
        let state: MatchActivityAttributes.ContentState
    }

    /// Describes unsupported matches and missing activities in simulator tests.
    private enum TestError: Error { case unsupportedMatch, noActivity }
}
#endif
