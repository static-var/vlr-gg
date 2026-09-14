import Foundation
import WidgetKit
import os

enum WidgetSnapshotStore {
    private static let writeQueue = DispatchQueue(label: "dev.staticvar.vlr.widget-snapshot")
    private static let logger = Logger(subsystem: "dev.staticvar.vlr.ios", category: "WidgetSnapshot")

    static func setSpoilersHidden(_ enabled: Bool, snapshotURL: URL? = VLRWidgetContract.snapshotURL) async throws {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            writeQueue.async {
                do {
                    if let url = snapshotURL,
                       FileManager.default.fileExists(atPath: url.path) {
                        let data = try Data(contentsOf: url)
                        let snapshot = try JSONDecoder().decode(UpcomingMatchesSnapshot.self, from: data)
                        let updated = UpcomingMatchesSnapshot(
                            savedAtEpochMillis: snapshot.savedAtEpochMillis,
                            hasFavorites: snapshot.hasFavorites,
                            favorites: snapshot.favorites,
                            spoilersHidden: enabled,
                            matches: enabled ? snapshot.matches.map { $0.withoutScores() } : snapshot.matches,
                            theme: snapshot.theme
                        )
                        try JSONEncoder().encode(updated).write(to: url, options: .atomic)
                    }
                    WidgetCenter.shared.reloadTimelines(ofKind: VLRWidgetContract.widgetKind)
                    continuation.resume()
                } catch {
                    continuation.resume(throwing: error)
                }
            }
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
                WidgetCenter.shared.reloadTimelines(ofKind: VLRWidgetContract.widgetKind)
            } catch {
                logger.error("Unable to publish widget snapshot: \(error.localizedDescription, privacy: .public)")
            }
        }
    }
}
