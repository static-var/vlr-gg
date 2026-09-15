import CoreSpotlight
import XCTest
@testable import VLR

@MainActor
final class FavoriteSearchTests: XCTestCase {
    func testDecodeRejectsMismatchedDuplicateAndUnsafeIdentifiers() throws {
        let valid = #"[{"id":"team:12","kind":"team","sourceId":"12","title":"Team Liquid"}]"#
        XCTAssertEqual(try SearchFavorite.decode(Data(valid.utf8)).first?.url.absoluteString, "vlr://team/12")
        for invalid in [
            valid.replacingOccurrences(of: "team:12", with: "event:12"),
            valid.replacingOccurrences(of: "\"sourceId\":\"12\"", with: "\"sourceId\":\"../12\""),
            "[" + valid.dropFirst().dropLast() + "," + valid.dropFirst().dropLast() + "]",
        ] {
            XCTAssertThrowsError(try SearchFavorite.decode(Data(invalid.utf8)))
        }
    }

    func testEveryKindHasDistinctIdentityAndDestination() {
        let records = [SearchFavorite.Kind.team, .event, .match, .player].map {
            SearchFavorite(id: "\($0.rawValue):12", kind: $0, sourceId: "12", title: "Favorite")
        }
        XCTAssertEqual(Set(records.map(\.id)).count, 4)
        XCTAssertEqual(records.map { $0.url.absoluteString }, ["vlr://team/12", "vlr://event/12", "vlr://match/12", "vlr://player/12"])
    }

    func testCoreSpotlightUpdatesRemovesAndClearsOnlyFavoritesDomain() async throws {
        guard CSSearchableIndex.isIndexingAvailable() else { throw XCTSkip("CoreSpotlight unavailable on this device") }
        let identifier = UUID().uuidString
        let domain = "dev.staticvar.vlr.test.\(identifier)"
        let unrelatedDomain = "dev.staticvar.vlr.independent-test.\(identifier)"
        let index = CSSearchableIndex(name: domain)
        let file = FileManager.default.temporaryDirectory.appendingPathComponent(identifier + ".json")
        let store = FavoriteSearchStore(file: file, index: index, domain: domain)
        let teamId = identifier.utf8.map { String($0) }.joined()
        let eventId = teamId + "1"
        let team = SearchFavorite(id: "team:\(teamId)", kind: .team, sourceId: teamId, title: "Spotlight Test Team")
        let event = SearchFavorite(id: "event:\(eventId)", kind: .event, sourceId: eventId, title: "Spotlight Test Event")
        let unrelatedAttributes = CSSearchableItemAttributeSet(contentType: .text)
        unrelatedAttributes.title = "Independent saved article"
        unrelatedAttributes.contentDescription = "Unrelated test content"
        unrelatedAttributes.url = URL(string: "vlr://news/\(teamId)")
        let unrelated = CSSearchableItem(uniqueIdentifier: identifier, domainIdentifier: unrelatedDomain, attributeSet: unrelatedAttributes)
        do {
            try await index.indexSearchableItems([unrelated])
            try await assertIndexed(domain: unrelatedDomain, titles: ["Independent saved article"])
            try await store.sync([team, event])
            try await assertIndexed(domain: domain, titles: [team.title, event.title])
            let renamed = SearchFavorite(id: team.id, kind: team.kind, sourceId: team.sourceId, title: "Renamed Team")
            try await store.sync([renamed])
            try await assertIndexed(domain: domain, titles: [renamed.title])
            XCTAssertEqual(try store.records(), [renamed])
            XCTAssertNil(store.url(for: event.id))
            try await store.sync([])
            try await assertIndexed(domain: domain, titles: [])
            try await assertIndexed(domain: unrelatedDomain, titles: ["Independent saved article"])
            XCTAssertEqual(try store.records(), [])
        } catch {
            try? await index.deleteSearchableItems(withDomainIdentifiers: [domain, unrelatedDomain])
            try? FileManager.default.removeItem(at: file)
            throw error
        }
        try await index.deleteSearchableItems(withDomainIdentifiers: [domain, unrelatedDomain])
        try FileManager.default.removeItem(at: file)
    }

    private func assertIndexed(domain: String, titles: [String]) async throws {
        var found: [String] = []
        for _ in 0..<30 {
            found = try await query(domain: domain)
            if found.sorted() == titles.sorted() { return }
            try await Task.sleep(nanoseconds: 200_000_000)
        }
        XCTAssertEqual(found.sorted(), titles.sorted())
    }

    private func query(domain: String) async throws -> [String] {
        try await withCheckedThrowingContinuation { continuation in
            let context = CSSearchQueryContext()
            context.fetchAttributes = ["title"]
            let query = CSSearchQuery(queryString: "domainIdentifier == '\(domain)'", queryContext: context)
            let results = SearchQueryResults()
            query.foundItemsHandler = { items in results.append(items.compactMap { $0.attributeSet.title }) }
            query.completionHandler = { error in
                if let error { continuation.resume(throwing: error) }
                else { continuation.resume(returning: results.values) }
            }
            query.start()
        }
    }
}

private final class SearchQueryResults: @unchecked Sendable {
    private let lock = NSLock()
    private var storage: [String] = []
    func append(_ values: [String]) {
        lock.lock()
        defer { lock.unlock() }
        storage.append(contentsOf: values)
    }
    var values: [String] {
        lock.lock()
        defer { lock.unlock() }
        return storage
    }
}
