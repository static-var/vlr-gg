import ActivityKit
import Foundation
import shared

final class IosActivityPushTokenProvider: NSObject, PushTokenProvider {
    var platform: PushPlatform { .ios }

    private var observationTask: Task<Void, Never>?

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
