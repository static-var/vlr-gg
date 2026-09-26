import Foundation

enum VLRWidgetContract {
    static let appGroupIdentifier = "group.dev.staticvar.vlr.ios"
    static let snapshotFilename = "upcoming-matches.json"
    static let refreshedSnapshotFilename = "upcoming-matches-refreshed.json"
    static let widgetKind = "dev.staticvar.vlr.upcoming-matches"
    static let appURL = URL(string: "https://valorantesports.staticvar.dev/")!

    static var snapshotURL: URL? {
        FileManager.default
            .containerURL(forSecurityApplicationGroupIdentifier: appGroupIdentifier)?
            .appendingPathComponent(snapshotFilename, isDirectory: false)
    }

    static var refreshedSnapshotURL: URL? {
        FileManager.default
            .containerURL(forSecurityApplicationGroupIdentifier: appGroupIdentifier)?
            .appendingPathComponent(refreshedSnapshotFilename, isDirectory: false)
    }

    static func matchURL(id: String) -> URL {
        detailsURL(kind: "match", id: id)
    }

    static func detailsURL(kind: String, id: String) -> URL {
        appURL.appendingPathComponent(kind).appendingPathComponent(id)
    }
}
