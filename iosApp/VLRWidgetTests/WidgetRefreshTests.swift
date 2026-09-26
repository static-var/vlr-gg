import XCTest
import SwiftUI
import WidgetKit
import CoreText

final class WidgetRefreshTests: XCTestCase {
    @MainActor
    func testWidgetLayoutsAttachSmallMediumAndLargeRenders() throws {
        let fontURL = try XCTUnwrap(Bundle(for: Self.self).url(forResource: "chakra_petch_regular", withExtension: "ttf"))
        CTFontManagerRegisterFontsForURL(fontURL as CFURL, .process, nil)
        XCTAssertNotNil(UIFont(name: "ChakraPetch-Regular", size: 16))
        let date = Date(timeIntervalSince1970: 1_789_652_400)
        let live = UpcomingMatch(
            id: "748633", event: "VCT Pacific", team1: "Team Liquid", team2: "Paper Rex",
            startTimeEpochMillis: Int64(date.timeIntervalSince1970 * 1_000), status: .live,
            score1: 1, score2: 2, format: "BO3", stage: "Playoffs"
        )
        let upcoming = UpcomingMatch(
            id: "748634", event: "Champions", team1: "Fnatic", team2: "Sentinels",
            startTimeEpochMillis: Int64(date.addingTimeInterval(86_400).timeIntervalSince1970 * 1_000), status: .upcoming,
            score1: nil, score2: nil, format: "BO3", stage: "Group stage"
        )
        let longNames = UpcomingMatch(
            id: "748635", event: "Game Changers North America", team1: "Shopify Rebellion Gold", team2: "Twisted Minds Esports",
            startTimeEpochMillis: nil, status: .live, score1: 1, score2: 2, format: "BO5", stage: "Grand Final"
        )
        let scenarios: [(String, [UpcomingMatch], Bool)] = [
            ("live", [live, upcoming], false),
            ("hidden", [live, upcoming], true),
            ("upcoming", [upcoming], false),
            ("single", [live], false),
            ("long-names", [longNames, upcoming], true),
            ("empty", [], false),
        ]
        let layouts: [(String, WidgetFamily, CGSize)] = [
            ("small", .systemSmall, CGSize(width: 158, height: 158)),
            ("medium", .systemMedium, CGSize(width: 338, height: 158)),
            ("large", .systemLarge, CGSize(width: 338, height: 354)),
        ]
        let hostPadding: CGFloat
        if #available(iOS 17.0, *) { hostPadding = 16 } else { hostPadding = 0 }
        for (scenario, matches, hidden) in scenarios {
            let entry = UpcomingMatchesEntry(date: date, snapshot: snapshot(
                savedAtEpochMillis: Int64(date.timeIntervalSince1970 * 1_000),
                favorites: WidgetFavoriteIDs(matchIds: ["748633"], teamIds: [], eventIds: [], playerIds: []),
                spoilersHidden: hidden, matches: matches
            ))
            for colorScheme in [ColorScheme.light, .dark] {
                for (name, family, size) in layouts {
                    let content = UpcomingMatchesWidgetView(entry: entry, familyOverride: family)
                        .padding(hostPadding)
                        .frame(width: size.width, height: size.height)
                        .background(PrismWidgetPalette.background(for: colorScheme))
                        .clipShape(RoundedRectangle(cornerRadius: 24))
                        .environment(\.colorScheme, colorScheme)
                        .environment(\.locale, Locale(identifier: "en_US"))
                        .environment(\.timeZone, TimeZone(secondsFromGMT: 0)!)
                    let renderer = ImageRenderer(content: content)
                    renderer.scale = 2
                    let image = try XCTUnwrap(renderer.uiImage)
                    XCTAssertEqual(image.size, size)
                    let attachment = XCTAttachment(image: image)
                    attachment.name = "prism-\(scenario)-\(colorScheme)-\(name)"
                    attachment.lifetime = .keepAlways
                    add(attachment)
                }
            }
        }
    }

    func testRefreshUsesFavoriteMatchesEndpointWithoutResultsAndPreservesServerOrder() async throws {
        let recorder = RequestRecorder(responses: [
            favoriteMatchesPath: .ok("""
                [
                  {"id":"100","event":"Masters","series":"Bo3","status":"live","team1":{"id":"88","name":"Alpha","score":1},"team2":{"id":"2","name":"Beta","score":0},"time":"2026-09-12T18:00:00Z","event_id":"5"},
                  {"id":"200","event":"Champions","series":"Bo5","status":"upcoming","team1":{"id":"7","name":"Delta"},"team2":{"id":"8","name":"Epsilon"},"time":"2026-09-13T18:00:00Z","event_id":"6"},
                  {"id":"300","event":"Champions","series":"Bo3","status":"completed","team1":{"id":"7","name":"Delta"},"team2":{"id":"8","name":"Epsilon"},"time":"2026-09-11T18:00:00Z","event_id":"6"}
                ]
                """),
        ])
        let source = snapshot(
            favorites: WidgetFavoriteIDs(matchIds: ["100"], teamIds: [], eventIds: [], playerIds: [])
        )
        let refreshed = try await WidgetRefreshService(api: recorder.client(authorization: "Bearer key")).refresh(source: source)
        XCTAssertEqual(refreshed.matches.map(\.id), ["100", "200"])
        XCTAssertEqual(recorder.requestPaths, [favoriteMatchesPath])
        XCTAssertEqual(recorder.requestQueries, ["include_results=false"])
        XCTAssertEqual(recorder.authorizationValues, ["Bearer key"])
        XCTAssertEqual(recorder.appNameValues, ["dev.staticvar.vlr"])
    }

    func testServerEmptyListClearsStaleWidgetMatches() async throws {
        let recorder = RequestRecorder(responses: [favoriteMatchesPath: .ok("[]")])
        let source = snapshot(
            favorites: WidgetFavoriteIDs(matchIds: ["42"], teamIds: [], eventIds: [], playerIds: []),
            matches: [liveMatch(score1: 1, score2: 1)]
        )
        let refreshed = try await WidgetRefreshService(api: recorder.client()).refresh(source: source)
        XCTAssertTrue(refreshed.matches.isEmpty)
    }

    func testFreshVerifiedSnapshotAvoidsAllNetworkRequests() async throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let now = Date.now
        let source = snapshot(
            savedAtEpochMillis: Int64(now.timeIntervalSince1970 * 1000),
            favorites: WidgetFavoriteIDs(matchIds: ["42"], teamIds: [], eventIds: [], playerIds: []),
            matches: [liveMatch(score1: 1, score2: 1)]
        )
        let sourceURL = directory.appendingPathComponent("source.json")
        try JSONEncoder().encode(source).write(to: sourceURL)
        let repository = WidgetSnapshotRepository(sourceURL: sourceURL, refreshedURL: directory.appendingPathComponent("refresh.json"))
        XCTAssertTrue(repository.store(source, for: source))
        let recorder = RequestRecorder(responses: [:])
        let result = await WidgetRefreshCoordinator().refresh(
            repository: repository,
            service: WidgetRefreshService(api: recorder.client()), now: now.addingTimeInterval(60)
        )
        XCTAssertEqual(result.snapshot, source)
        XCTAssertEqual(result.refreshDate, source.refreshDate)
        XCTAssertTrue(recorder.requestPaths.isEmpty)
        XCTAssertEqual(source.refreshDate.timeIntervalSince(source.savedAt), 900)
    }

    func testOverlappingTimelinesShareOneRefreshAndPersistIt() async throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let source = snapshot(
            favorites: WidgetFavoriteIDs(matchIds: [], teamIds: ["88"], eventIds: [], playerIds: [])
        )
        let sourceURL = directory.appendingPathComponent("source.json")
        try JSONEncoder().encode(source).write(to: sourceURL)
        let repository = WidgetSnapshotRepository(sourceURL: sourceURL, refreshedURL: directory.appendingPathComponent("refresh.json"))
        let recorder = RequestRecorder(responses: [favoriteMatchesPath: .ok("[]")], delayNanoseconds: 50_000_000)
        let coordinator = WidgetRefreshCoordinator()
        let service = WidgetRefreshService(api: recorder.client())
        let now = Date.now
        async let first = coordinator.refresh(repository: repository, service: service, now: now)
        async let second = coordinator.refresh(repository: repository, service: service, now: now)
        let results = await (first, second)
        XCTAssertEqual(results.0, results.1)
        let cached = await coordinator.refresh(repository: repository, service: service, now: now.addingTimeInterval(60))
        XCTAssertEqual(cached, results.0)
        XCTAssertEqual(recorder.requestPaths.count, 1)
    }

    func testRecentlySerializedAppSnapshotStillFetchesAndFailuresAreThrottled() async throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let now = Date.now
        let source = snapshot(
            savedAtEpochMillis: Int64(now.timeIntervalSince1970 * 1000),
            favorites: WidgetFavoriteIDs(matchIds: ["42"], teamIds: [], eventIds: [], playerIds: []),
            matches: [liveMatch(score1: 1, score2: 1)]
        )
        let sourceURL = directory.appendingPathComponent("source.json")
        try JSONEncoder().encode(source).write(to: sourceURL)
        let repository = WidgetSnapshotRepository(sourceURL: sourceURL, refreshedURL: directory.appendingPathComponent("refresh.json"))
        let recorder = RequestRecorder(responses: [favoriteMatchesPath: Response(statusCode: 503, body: Data())])
        let service = WidgetRefreshService(api: recorder.client())
        let coordinator = WidgetRefreshCoordinator()
        let retryAfter = now.addingTimeInterval(5 * 60)
        let first = await coordinator.refresh(repository: repository, service: service, now: now)
        XCTAssertEqual(first.snapshot, source)
        XCTAssertEqual(first.refreshDate, retryAfter)
        assertTimeline(first, at: now)
        XCTAssertEqual(recorder.requestPaths.count, 1)
        let second = await coordinator.refresh(repository: repository, service: service, now: now.addingTimeInterval(60))
        XCTAssertEqual(second.snapshot, source)
        XCTAssertEqual(second.refreshDate, retryAfter)
        XCTAssertEqual(recorder.requestPaths.count, 1)
        _ = await coordinator.refresh(repository: repository, service: service, now: now.addingTimeInterval(301))
        XCTAssertEqual(recorder.requestPaths.count, 2)
    }

    func testSuccessfulRefreshUsesFetchedDataAndSoonMatchDeadlineWhenCacheWriteFails() async throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let now = Date(timeIntervalSince1970: 1_800_000_000)
        let matchStart = now.addingTimeInterval(2 * 60)
        let source = snapshot(
            savedAtEpochMillis: Int64(now.addingTimeInterval(-31 * 60).timeIntervalSince1970 * 1_000),
            favorites: WidgetFavoriteIDs(matchIds: ["42"], teamIds: [], eventIds: [], playerIds: [])
        )
        let sourceURL = directory.appendingPathComponent("source.json")
        try JSONEncoder().encode(source).write(to: sourceURL)
        let refreshedURL = directory.appendingPathComponent("refresh.json", isDirectory: true)
        try FileManager.default.createDirectory(at: refreshedURL, withIntermediateDirectories: true)
        let repository = WidgetSnapshotRepository(sourceURL: sourceURL, refreshedURL: refreshedURL)
        let start = ISO8601DateFormatter().string(from: matchStart)
        let recorder = RequestRecorder(responses: [
            favoriteMatchesPath: .ok("""
                [{"id":"42","event":"Champions","series":"Bo3","status":"upcoming","team1":{"name":"Alpha"},"team2":{"name":"Beta"},"time":"\(start)","event_id":"5"}]
                """),
        ])

        let result = await WidgetRefreshCoordinator().refresh(
            repository: repository,
            service: WidgetRefreshService(api: recorder.client()),
            now: now
        )

        XCTAssertEqual(result.snapshot?.matches.first?.startTime, matchStart)
        XCTAssertEqual(result.refreshDate, matchStart)
        assertTimeline(result, at: now)
        XCTAssertEqual(recorder.requestPaths, [favoriteMatchesPath])
    }

    func testSourceChangeDuringRefreshRejectsOldConfigurationResult() async throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let now = Date(timeIntervalSince1970: 1_800_000_000)
        let source = snapshot(
            savedAtEpochMillis: Int64(now.addingTimeInterval(-31 * 60).timeIntervalSince1970 * 1_000),
            favorites: WidgetFavoriteIDs(matchIds: ["42"], teamIds: [], eventIds: [], playerIds: [])
        )
        let replacement = snapshot(
            savedAtEpochMillis: Int64(now.timeIntervalSince1970 * 1_000),
            favorites: WidgetFavoriteIDs(matchIds: ["99"], teamIds: [], eventIds: [], playerIds: [])
        )
        let sourceURL = directory.appendingPathComponent("source.json")
        try JSONEncoder().encode(source).write(to: sourceURL)
        let repository = WidgetSnapshotRepository(
            sourceURL: sourceURL,
            refreshedURL: directory.appendingPathComponent("refresh.json")
        )
        let responseBody = Data("""
            [{"id":"42","event":"Champions","series":"Bo3","status":"live","team1":{"name":"Alpha"},"team2":{"name":"Beta"},"time":null,"event_id":"5"}]
            """.utf8)
        let client = WidgetAPIClient(
            baseURL: URL(string: "https://fixture.invalid")!,
            authorization: nil
        ) { request in
            try JSONEncoder().encode(replacement).write(to: sourceURL, options: .atomic)
            return (
                responseBody,
                HTTPURLResponse(
                    url: try XCTUnwrap(request.url),
                    statusCode: 200,
                    httpVersion: nil,
                    headerFields: nil
                )!
            )
        }

        let result = await WidgetRefreshCoordinator().refresh(
            repository: repository,
            service: WidgetRefreshService(api: client),
            now: now
        )

        XCTAssertEqual(
            result,
            .retry(snapshot: replacement, refreshAfter: now.addingTimeInterval(5 * 60))
        )
        assertTimeline(result, at: now)
    }

    func testUpcomingStartExpiresCacheBeforeNormalInterval() {
        let now = Date.now
        let match = UpcomingMatch(
            id: "42", event: "Champions", team1: "Alpha", team2: "Beta",
            startTimeEpochMillis: Int64(now.addingTimeInterval(120).timeIntervalSince1970 * 1000),
            status: .upcoming, score1: nil, score2: nil, format: "BO3", stage: "Final"
        )
        let source = snapshot(
            savedAtEpochMillis: Int64(now.timeIntervalSince1970 * 1000),
            favorites: WidgetFavoriteIDs(matchIds: ["42"], teamIds: [], eventIds: [], playerIds: []),
            matches: [match]
        )
        XCTAssertEqual(source.refreshDate, match.startTime)
    }

    func testUnregisteredAndUnavailableResponsesKeepRefreshRetryable() async {
        for status in [404, 503] {
            let recorder = RequestRecorder(responses: [
                favoriteMatchesPath: Response(statusCode: status, body: Data()),
            ])
            let service = WidgetRefreshService(api: recorder.client())

            do {
                _ = try await service.refresh(
                    source: snapshot(
                        favorites: WidgetFavoriteIDs(matchIds: ["42"], teamIds: [], eventIds: [], playerIds: [])
                    )
                )
                XCTFail("Expected the refresh to fail")
            } catch {
                XCTAssertEqual(error as? WidgetRefreshError, .httpStatus(status))
            }
        }
    }

    func testCacheRejectsOlderResultAndConfigurationRollback() throws {
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString, isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }

        let sourceURL = directory.appendingPathComponent("source.json")
        let refreshedURL = directory.appendingPathComponent("refreshed.json")
        let source = snapshot(
            savedAtEpochMillis: 200,
            favorites: WidgetFavoriteIDs(matchIds: ["42"], teamIds: [], eventIds: [], playerIds: []),
            spoilersHidden: true
        )
        try JSONEncoder().encode(source).write(to: sourceURL)
        let older = snapshot(
            savedAtEpochMillis: 100,
            favorites: source.favorites,
            spoilersHidden: false,
            matches: [liveMatch(score1: 2, score2: 1)]
        )
        try JSONEncoder().encode(
            RefreshedSnapshotEnvelope(sourceSignature: source.sourceSignature, snapshot: older)
        ).write(to: refreshedURL)
        let repository = WidgetSnapshotRepository(sourceURL: sourceURL, refreshedURL: refreshedURL)

        XCTAssertEqual(repository.loadBestSnapshot(), source)

        let newer = snapshot(
            savedAtEpochMillis: 300,
            favorites: source.favorites,
            spoilersHidden: false,
            matches: [liveMatch(score1: 2, score2: 1)]
        )
        XCTAssertTrue(repository.store(newer, for: source))
        let loaded = try XCTUnwrap(repository.loadBestSnapshot())
        XCTAssertTrue(loaded.spoilersHidden)
        XCTAssertEqual(loaded.theme, source.theme)
        XCTAssertNil(loaded.matches[0].score1)
        XCTAssertNil(loaded.matches[0].score2)
    }

    private func snapshot(
        savedAtEpochMillis: Int64 = 1,
        favorites: WidgetFavoriteIDs,
        spoilersHidden: Bool = false,
        matches: [UpcomingMatch] = []
    ) -> UpcomingMatchesSnapshot {
        UpcomingMatchesSnapshot(
            savedAtEpochMillis: savedAtEpochMillis,
            clientId: testClientId,
            hasFavorites: true,
            favorites: favorites,
            spoilersHidden: spoilersHidden,
            matches: matches,
            theme: .preview
        )
    }

    private func liveMatch(score1: Int?, score2: Int?) -> UpcomingMatch {
        UpcomingMatch(
            id: "42",
            event: "Champions",
            team1: "Alpha",
            team2: "Beta",
            startTimeEpochMillis: nil,
            status: .live,
            score1: score1,
            score2: score2,
            format: "BO3",
            stage: "Final"
        )
    }

    private func assertTimeline(
        _ outcome: WidgetRefreshOutcome,
        at date: Date,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        let plan = UpcomingMatchesProvider.timelinePlan(for: outcome, at: date)
        XCTAssertEqual(plan.entry.date, date, file: file, line: line)
        XCTAssertEqual(plan.entry.snapshot, outcome.snapshot, file: file, line: line)
        XCTAssertEqual(plan.refreshDate, outcome.refreshDate, file: file, line: line)
    }
}

private struct Response {
    let statusCode: Int
    let body: Data

    static func ok(_ body: String) -> Response {
        Response(statusCode: 200, body: Data(body.utf8))
    }
}

private final class RequestRecorder {
    private let lock = NSLock()
    private let responses: [String: Response]
    private let delayNanoseconds: UInt64
    private(set) var authorizationValues = Set<String>()
    private(set) var appNameValues = Set<String>()
    private(set) var requestPaths: [String] = []
    private(set) var requestQueries: [String?] = []

    init(responses: [String: Response], delayNanoseconds: UInt64 = 0) {
        self.responses = responses
        self.delayNanoseconds = delayNanoseconds
    }

    func client(authorization: String? = nil) -> WidgetAPIClient {
        WidgetAPIClient(
            baseURL: URL(string: "https://fixture.invalid")!,
            authorization: authorization
        ) { [self] request in
            if delayNanoseconds > 0 { try await Task.sleep(nanoseconds: delayNanoseconds) }
            let response = response(for: request)
            guard let response, let url = request.url else {
                throw WidgetRefreshError.invalidResponse
            }
            return (
                response.body,
                HTTPURLResponse(
                    url: url,
                    statusCode: response.statusCode,
                    httpVersion: nil,
                    headerFields: nil
                )!
            )
        }
    }

    private func response(for request: URLRequest) -> Response? {
        lock.lock()
        defer { lock.unlock() }
        if let value = request.value(forHTTPHeaderField: "Authorization") {
            authorizationValues.insert(value)
        }
        if let value = request.value(forHTTPHeaderField: "app-name") {
            appNameValues.insert(value)
        }
        let path = request.url?.path ?? ""
        requestPaths.append(path)
        requestQueries.append(request.url?.query)
        if let response = responses[path] {
            return response
        }
        return responses[path.hasSuffix("/") ? String(path.dropLast()) : path + "/"]
    }
}

private let testClientId = "01996ff9-3000-7000-8000-000000000001"
private let favoriteMatchesPath = "/api/v1/favorites/\(testClientId)/matches"
