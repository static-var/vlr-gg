import CoreSpotlight
import XCTest
@testable import VLR

@MainActor
final class FavoriteSearchTests: XCTestCase {
    func testFailedReplacementPreservesLiveResultsAndCommittedSnapshot() async throws {
        let file = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString + ".json")
        defer { removeSnapshot(file) }
        let index = FailingSearchIndex()
        let store = FavoriteSearchStore(file: file, index: index)
        let old = SearchFavorite(id: "team:1", kind: .team, sourceId: "1", title: "Old team")
        let next = SearchFavorite(id: "event:2", kind: .event, sourceId: "2", title: "New event")
        try await store.sync([old])
        index.failNextAddition = true
        do {
            try await store.sync([next])
            XCTFail("Expected injected indexing failure")
        } catch { XCTAssertEqual((error as NSError).domain, "SpotlightTestFailure") }
        XCTAssertEqual(Set(index.items.keys), [old.id])
        XCTAssertEqual(try SearchFavorite.decode(Data(contentsOf: file)), [old])
        XCTAssertNil(store.url(for: old.id))
        try await store.sync()
        XCTAssertEqual(Set(index.items.keys), [next.id])
        XCTAssertEqual(try SearchFavorite.decode(Data(contentsOf: file)), [next])
    }

    func testPartialAdditionIsRemovedAfterRestartAndDifferentSnapshot() async throws {
        let file = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString + ".json")
        defer { removeSnapshot(file) }
        let index = FailingSearchIndex()
        let store = FavoriteSearchStore(file: file, index: index)
        let old = SearchFavorite(id: "team:1", kind: .team, sourceId: "1", title: "Old team")
        let attempted = SearchFavorite(id: "event:2", kind: .event, sourceId: "2", title: "Attempted event")
        let latest = SearchFavorite(id: "player:3", kind: .player, sourceId: "3", title: "Latest player")
        try await store.sync([old])
        index.failNextAddition = true
        index.insertBeforeFailing = true
        do {
            try await store.sync([attempted])
            XCTFail("Expected partial indexing failure")
        } catch { XCTAssertEqual((error as NSError).domain, "SpotlightTestFailure") }
        XCTAssertEqual(try SearchFavorite.decode(Data(contentsOf: file)), [old])
        let restarted = FavoriteSearchStore(file: file, index: index)
        try await restarted.sync([latest])
        XCTAssertEqual(Set(index.items.keys), [latest.id])
        XCTAssertEqual(try SearchFavorite.decode(Data(contentsOf: file)), [latest])
    }

    func testFailedStaleDeletionIsRetriedAfterRestart() async throws {
        let file = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString + ".json")
        defer { removeSnapshot(file) }
        let index = FailingSearchIndex()
        let store = FavoriteSearchStore(file: file, index: index)
        let old = SearchFavorite(id: "team:1", kind: .team, sourceId: "1", title: "Old team")
        let attempted = SearchFavorite(id: "event:2", kind: .event, sourceId: "2", title: "Attempted event")
        try await store.sync([old])
        index.failNextDeletion = true
        do {
            try await store.sync([attempted])
            XCTFail("Expected stale deletion failure")
        } catch { XCTAssertEqual((error as NSError).domain, "SpotlightTestFailure") }
        XCTAssertEqual(try SearchFavorite.decode(Data(contentsOf: file)), [old])
        let restarted = FavoriteSearchStore(file: file, index: index)
        try await restarted.sync([])
        XCTAssertTrue(index.items.isEmpty)
        XCTAssertEqual(try SearchFavorite.decode(Data(contentsOf: file)), [])
    }

    private func removeSnapshot(_ file: URL) {
        try? FileManager.default.removeItem(at: file)
        try? FileManager.default.removeItem(at: file.appendingPathExtension("tracked-ids"))
    }

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
            removeSnapshot(file)
            throw error
        }
        try await index.deleteSearchableItems(withDomainIdentifiers: [domain, unrelatedDomain])
        removeSnapshot(file)
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

private final class FailingSearchIndex: CSSearchableIndex, @unchecked Sendable {
    var items: [String: CSSearchableItem] = [:]
    var failNextAddition = false
    var insertBeforeFailing = false
    var failNextDeletion = false
    private let failure = NSError(domain: "SpotlightTestFailure", code: 1)

    override func indexSearchableItems(_ items: [CSSearchableItem], completionHandler: (@Sendable (Error?) -> Void)? = nil) {
        if failNextAddition {
            failNextAddition = false
            if insertBeforeFailing, let item = items.first { self.items[item.uniqueIdentifier] = item }
            completionHandler?(failure)
            return
        }
        for item in items { self.items[item.uniqueIdentifier] = item }
        completionHandler?(nil)
    }

    override func deleteSearchableItems(withIdentifiers identifiers: [String], completionHandler: (@Sendable (Error?) -> Void)? = nil) {
        if failNextDeletion {
            failNextDeletion = false
            completionHandler?(failure)
            return
        }
        for identifier in identifiers { items.removeValue(forKey: identifier) }
        completionHandler?(nil)
    }

    override func deleteSearchableItems(withDomainIdentifiers domains: [String], completionHandler: (@Sendable (Error?) -> Void)? = nil) {
        deleteSearchableItems(withIdentifiers: items.values.filter { domains.contains($0.domainIdentifier ?? "") }.map(\.uniqueIdentifier), completionHandler: completionHandler)
    }
}
