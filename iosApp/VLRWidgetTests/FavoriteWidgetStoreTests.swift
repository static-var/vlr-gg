import XCTest

final class FavoriteWidgetStoreTests: XCTestCase {
    func testSharedRefreshRoundTripFallbackAndLatestValue() async throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let now = Date(timeIntervalSince1970: 1_792_152_000)
        let store = FavoriteWidgetStore<TeamWidgetContent>(namespace: .team, directory: directory, now: { now })
        let key = FavoriteWidgetKey(entityID: "2", clientID: "client")
        let counter = FetchCounter()
        let initial = TeamWidgetContent.sample(at: now)
        let fetch: @Sendable () async throws -> TeamWidgetContent = {
            await counter.increment()
            try await Task.sleep(for: .milliseconds(20))
            return initial
        }
        async let first = store.refresh(for: key, fetch: fetch)
        async let second = store.refresh(for: key, fetch: fetch)
        let values = await (first, second)
        XCTAssertEqual(values.0, values.1)
        let count = await counter.count
        XCTAssertEqual(count, 1)
        let loaded = await store.load(for: key)
        XCTAssertEqual(loaded?.value, initial)
        XCTAssertEqual(loaded?.fetchedAt, now)
        var next = initial
        next.state = .idle(nil)
        let latest = next
        _ = await store.refresh(for: key) { latest }
        let fallback = await store.refresh(for: key) { throw WidgetRefreshError.httpStatus(503) }
        XCTAssertEqual(fallback?.value, latest)
        let reopened = FavoriteWidgetStore<TeamWidgetContent>(namespace: .team, directory: directory)
        let disk = await reopened.load(for: key)
        XCTAssertEqual(disk?.value, latest)
        let otherClient = await store.load(for: .init(entityID: "2", clientID: "other"))
        XCTAssertNil(otherClient)
    }

    func testNamespacesStructuredKeysAndBadRecordsStaySeparate() async throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let team = FavoriteWidgetStore<String>(namespace: .team, directory: directory)
        let event = FavoriteWidgetStore<String>(namespace: .event, directory: directory)
        let keys = [FavoriteWidgetKey(entityID: "a:b/c", clientID: "d"), .init(entityID: "a/b:c", clientID: "d")]
        _ = await team.refresh(for: keys[0]) { "team" }
        _ = await event.refresh(for: keys[0]) { "event" }
        _ = await event.refresh(for: keys[1]) { "other" }
        let teamValue = await team.load(for: keys[0])
        let eventValue = await event.load(for: keys[0])
        let otherValue = await event.load(for: keys[1])
        XCTAssertEqual(teamValue?.value, "team")
        XCTAssertEqual(eventValue?.value, "event")
        XCTAssertEqual(otherValue?.value, "other")
        let files = try FileManager.default.contentsOfDirectory(at: directory, includingPropertiesForKeys: nil)
        XCTAssertEqual(files.count, 3)
        for file in files {
            var json = try XCTUnwrap(JSONSerialization.jsonObject(with: Data(contentsOf: file)) as? [String: Any])
            json["version"] = 99
            try JSONSerialization.data(withJSONObject: json).write(to: file)
        }
        let invalid = await team.load(for: keys[0])
        XCTAssertNil(invalid)
        for file in files { try Data("bad".utf8).write(to: file) }
        let corrupt = await event.load(for: keys[1])
        XCTAssertNil(corrupt)
        let unavailable = FavoriteWidgetStore<String>(namespace: .event, directory: directory.appendingPathComponent("absent"))
        let fresh = await unavailable.refresh(for: keys[0]) { "fresh" }
        XCTAssertEqual(fresh?.value, "fresh")
    }
}

private actor FetchCounter {
    var count = 0
    func increment() { count += 1 }
}
