import Foundation
import WidgetKit
import os

enum WidgetSnapshotStore {
    private static let writeQueue = DispatchQueue(label: "dev.staticvar.vlr.widget-snapshot")
    private static let logger = Logger(subsystem: "dev.staticvar.vlr.ios", category: "WidgetSnapshot")

    static func setSpoilersHidden(_ enabled: Bool, matchesJSON: String, snapshotURL: URL? = VLRWidgetContract.snapshotURL) async throws {
        let matches = try JSONDecoder().decode([UpcomingMatch].self, from: Data(matchesJSON.utf8))
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            writeQueue.async {
                do {
                    if let url = snapshotURL,
                       FileManager.default.fileExists(atPath: url.path) {
                        let data = try Data(contentsOf: url)
                        let snapshot = try JSONDecoder().decode(UpcomingMatchesSnapshot.self, from: data)
                        var updated = snapshot
                        updated.spoilersHidden = enabled
                        updated.matches = enabled ? matches.map { $0.withoutScores() } : matches
                        try JSONEncoder().encode(updated).write(to: url, options: .atomic)
                    }
                    reloadHomeWidgets()
                    continuation.resume()
                } catch {
                    continuation.resume(throwing: error)
                }
            }
        }
    }

    private static func reloadHomeWidgets() {
        for kind in VLRWidgetContract.homeWidgetKinds + VLRWidgetContract.lockScreenWidgetKinds {
            WidgetCenter.shared.reloadTimelines(ofKind: kind)
        }
    }

    static func publish(_ snapshotJSON: String) {
        guard let data = snapshotJSON.data(using: .utf8) else { return }

        writeQueue.async {
            guard let snapshotURL = VLRWidgetContract.snapshotURL else { return }

            if (try? Data(contentsOf: snapshotURL)) == data {
                return
            }

            do {
                try data.write(to: snapshotURL, options: .atomic)
                reloadHomeWidgets()
            } catch {
                logger.error("Unable to publish widget snapshot: \(error.localizedDescription, privacy: .public)")
            }
        }
    }
}
