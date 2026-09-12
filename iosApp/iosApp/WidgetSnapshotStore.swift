import Foundation
import WidgetKit
import os

enum WidgetSnapshotStore {
    private static let writeQueue = DispatchQueue(label: "dev.staticvar.vlr.widget-snapshot")
    private static let logger = Logger(subsystem: "dev.staticvar.vlr.ios", category: "WidgetSnapshot")

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
