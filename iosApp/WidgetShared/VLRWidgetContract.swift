import Foundation

enum VLRWidgetContract {
    static let appGroupIdentifier = "group.dev.staticvar.vlr.ios"
    static let snapshotFilename = "upcoming-matches.json"
    static let refreshedSnapshotFilename = "upcoming-matches-refreshed.json"
    static let widgetKind = "dev.staticvar.vlr.upcoming-matches"
    static let appURL = URL(string: "vlr://home")!

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
        var components = URLComponents()
        components.scheme = "vlr"
        components.host = "match"
        components.path = "/\(id)"
        return components.url ?? appURL
    }
}
