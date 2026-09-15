import AppIntents
import CoreSpotlight
import Foundation
import OSLog
import UIKit
import UniformTypeIdentifiers

struct SearchFavorite: Codable, Equatable, Sendable {
    enum Kind: String, Codable, Sendable {
        case team, event, match, player
    }

    let id: String
    let kind: Kind
    let sourceId: String
    let title: String

    var url: URL { URL(string: "vlr://\(kind.rawValue)/\(sourceId)")! }

    static func decode(_ data: Data) throws -> [SearchFavorite] {
        let records = try JSONDecoder().decode([SearchFavorite].self, from: data)
        guard Set(records.map(\.id)).count == records.count,
              records.allSatisfy({
                  !$0.sourceId.isEmpty && $0.sourceId.utf8.allSatisfy { (48...57).contains($0) }
                      && $0.id == "\($0.kind.rawValue):\($0.sourceId)"
                      && !$0.title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
              }) else { throw SearchError.invalidSnapshot }
        return records
    }

    func attributes() -> CSSearchableItemAttributeSet {
        let attributes = CSSearchableItemAttributeSet(contentType: .content)
        attributes.title = title
        attributes.contentDescription = "Favorite \(kind.rawValue) in Val Esports"
        attributes.keywords = ["VLR", "Val Esports", kind.rawValue, "favorite"]
        attributes.url = url
        return attributes
    }

    func searchableItem(domain: String) -> CSSearchableItem {
        let item = CSSearchableItem(uniqueIdentifier: id, domainIdentifier: domain, attributeSet: attributes())
        item.expirationDate = .distantFuture
        if #available(iOS 18.0, *) {
            item.associateAppEntity(FavoriteEntity(record: self))
        }
        return item
    }
}

enum SearchError: Error {
    case invalidSnapshot
    case unavailableFavorite
    case cannotOpen
}

@MainActor
final class FavoriteSearchStore: NSObject, CSSearchableIndexDelegate {
    static let shared = FavoriteSearchStore()
    nonisolated static let domain = "dev.staticvar.vlr.favorites"
    private static let logger = Logger(subsystem: "dev.staticvar.vlr.ios", category: "FavoriteSearch")
    private let file: URL
    private let index: CSSearchableIndex
    private let domain: String
    private var tail: Task<Void, Error>?

    init(file: URL? = nil, index: CSSearchableIndex = .default(), domain: String = FavoriteSearchStore.domain) {
        self.file = file ?? FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("search-favorites.json")
        self.index = index
        self.domain = domain
        super.init()
        index.indexDelegate = self
    }

    func records() throws -> [SearchFavorite] {
        guard FileManager.default.fileExists(atPath: file.path) else { return [] }
        return try SearchFavorite.decode(Data(contentsOf: file))
    }

    func publish(_ json: String) {
        do {
            let records = try SearchFavorite.decode(Data(json.utf8))
            let work = enqueue(records)
            Task { await report(work) }
        } catch {
            Self.logger.error("Invalid favorites snapshot: \(error.localizedDescription, privacy: .public)")
        }
    }

    func retryIndexing() {
        let work = enqueue(nil)
        Task { await report(work) }
    }

    func sync(_ records: [SearchFavorite]) async throws {
        try await enqueue(records).value
    }

    private func enqueue(_ records: [SearchFavorite]?) -> Task<Void, Error> {
        let previous = tail
        let task = Task { @MainActor in
            _ = await previous?.result
            let desired = try records ?? self.records()
            if records != nil {
                try FileManager.default.createDirectory(at: file.deletingLastPathComponent(), withIntermediateDirectories: true)
                try JSONEncoder().encode(desired).write(to: file, options: .atomic)
            }
            try await index.deleteSearchableItems(withDomainIdentifiers: [domain])
            if !desired.isEmpty {
                try await index.indexSearchableItems(desired.map { $0.searchableItem(domain: domain) })
            }
        }
        tail = task
        return task
    }

    private func report(_ task: Task<Void, Error>) async {
        do { try await task.value }
        catch { Self.logger.error("Favorites indexing failed; next sync will retry: \(error.localizedDescription, privacy: .public)") }
    }

    nonisolated func searchableIndex(_ searchableIndex: CSSearchableIndex, reindexAllSearchableItemsWithAcknowledgementHandler acknowledgementHandler: @escaping () -> Void) {
        Task { @MainActor in
            await report(enqueue(nil))
            acknowledgementHandler()
        }
    }

    nonisolated func searchableIndex(_ searchableIndex: CSSearchableIndex, reindexSearchableItemsWithIdentifiers identifiers: [String], acknowledgementHandler: @escaping () -> Void) {
        self.searchableIndex(searchableIndex, reindexAllSearchableItemsWithAcknowledgementHandler: acknowledgementHandler)
    }

    func url(for identifier: String) -> URL? {
        do { return try records().first { $0.id == identifier }?.url }
        catch {
            Self.logger.error("Could not resolve favorite: \(error.localizedDescription, privacy: .public)")
            return nil
        }
    }
}

struct FavoriteEntity: AppEntity {
    static let typeDisplayRepresentation = TypeDisplayRepresentation(name: "Favorite")
    static let defaultQuery = FavoriteEntityQuery()
    let record: SearchFavorite
    var id: String { record.id }
    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(record.title)", subtitle: "\(record.kind.rawValue.capitalized)")
    }
}

@available(iOS 18.0, *)
extension FavoriteEntity: IndexedEntity {
    var attributeSet: CSSearchableItemAttributeSet { record.attributes() }
}

struct FavoriteEntityQuery: EntityStringQuery {
    @MainActor
    func entities(for identifiers: [String]) async throws -> [FavoriteEntity] {
        let records = try FavoriteSearchStore.shared.records()
        return identifiers.compactMap { id in records.first { $0.id == id }.map(FavoriteEntity.init) }
    }

    @MainActor
    func entities(matching string: String) async throws -> [FavoriteEntity] {
        try FavoriteSearchStore.shared.records()
            .filter { $0.title.localizedStandardContains(string) }
            .map(FavoriteEntity.init)
    }

    @MainActor
    func suggestedEntities() async throws -> [FavoriteEntity] {
        try FavoriteSearchStore.shared.records().map(FavoriteEntity.init)
    }
}

struct OpenFavoriteIntent: OpenIntent {
    static let title: LocalizedStringResource = "Open Favorite"
    static let description = IntentDescription("Open a favorite team, event, match, or player in Val Esports.")
    static let openAppWhenRun = true

    @Parameter(title: "Favorite")
    var target: FavoriteEntity

    @MainActor
    func perform() async throws -> some IntentResult {
        guard let url = FavoriteSearchStore.shared.url(for: target.id) else { throw SearchError.unavailableFavorite }
        guard await UIApplication.shared.open(url) else { throw SearchError.cannotOpen }
        return .result()
    }
}
