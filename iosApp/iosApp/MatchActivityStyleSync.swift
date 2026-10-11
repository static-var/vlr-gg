import ActivityKit
import Foundation

/// Mirrors the Live Activity style chosen in shared settings and redraws running activities.
/// The widget reads the style at render time; a local update with the current content makes
/// ActivityKit render again, so an activity switches layout without restarting.
@available(iOS 16.2, *)
@MainActor
final class MatchActivityStyleSync {
    static let shared = MatchActivityStyleSync()

    private var observer: NSObjectProtocol?
    private var assetTask: Task<Void, Never>?

    func start() {
        guard observer == nil else { return }
        observer = NotificationCenter.default.addObserver(
            forName: UserDefaults.didChangeNotification, object: nil, queue: .main
        ) { _ in
            Task { @MainActor in MatchActivityStyleSync.shared.sync() }
        }
        sync()
    }

    func sync() {
        guard MatchActivityStylePreference.mirror() != nil else { return }
        redraw()
        assetTask?.cancel()
        assetTask = Task {
            let states = Self.runningActivities.map(\.content.state)
            guard await MatchActivityStyleAssets.prefetch(for: states), !Task.isCancelled else { return }
            redraw()
        }
    }

    private static var runningActivities: [Activity<MatchActivityAttributes>] {
        Activity<MatchActivityAttributes>.activities.filter { $0.activityState == .active || $0.activityState == .stale }
    }

    /// Re-sends each running activity's current content with a new local revision.
    func redraw() {
        let revision = Int(Date().timeIntervalSince1970 * 1000)
        for activity in Self.runningActivities {
            let content = activity.content
            Task {
                await activity.update(ActivityContent(
                    state: content.state.withLocalRevision(revision),
                    staleDate: content.staleDate,
                    relevanceScore: content.relevanceScore
                ))
            }
        }
    }
}



