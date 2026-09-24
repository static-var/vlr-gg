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

    func testRefreshUsesCachedPlayerTeamIDWithoutPlayerOrTeamRequestsAndKeepsOtherRoutes() async throws {
        let recorder = RequestRecorder(responses: [
            "/api/v1/matches/": .ok("""
                [{"id":"100","event":"Masters","series":"Bo3","status":"live","team1":{"id":"88","name":"Alpha","score":1},"team2":{"id":"2","name":"Beta","score":0},"time":"2026-09-12T18:00:00Z","event_id":"5"}]
                """),
            "/api/v1/events/99": .ok("""
                {"title":"Game Changers","matches":[{"id":"300","time":"20:00:00","date":"2026-09-15","status":"upcoming","teams":[{"name":"Delta","score":null},{"name":"Epsilon","score":null}],"round":"Upper Final","stage":"Playoffs"}]}
                """),
            "/api/v1/matches/42": .ok("""
                {"event":{"name":"Champions","series":"Bo5","stage":"Grand Final","date":"2026-09-16T18:00:00Z","status":"upcoming"},"score":"","teams":[{"name":"Zeta","score":null},{"name":"Eta","score":null}]}
                """),
            "/api/v1/matches/100": .ok("""
                {"event":{"name":"Masters","series":"Masters 2026","stage":"Swiss Round 2","date":"2026-09-12T18:00:00Z","status":"ongoing"},"score":"1:0","teams":[{"name":"Alpha","score":1},{"name":"Beta","score":0}],"map_count":3}
                """),
        ])
        let service = WidgetRefreshService(api: recorder.client(authorization: "fixture-token"))
        let source = snapshot(
            favorites: WidgetFavoriteIDs(
                matchIds: ["42"],
                teamIds: ["88"],
                eventIds: ["99"],
                playerIds: ["77"]
            ),
            spoilersHidden: true
        )

        let refreshed = try await service.refresh(
            source: source,
            now: Date(timeIntervalSince1970: 1_788_800_000)
        )

        XCTAssertEqual(Set(refreshed.matches.map(\.id)), Set(["42", "100", "300"]))
        XCTAssertTrue(refreshed.matches.allSatisfy { $0.score1 == nil && $0.score2 == nil })
        XCTAssertEqual(refreshed.matches.first(where: { $0.id == "100" })?.format, "BO3")
        XCTAssertEqual(refreshed.matches.first(where: { $0.id == "100" })?.stage, "")
        XCTAssertEqual(
            Set(recorder.requestPaths),
            Set(["/api/v1/matches", "/api/v1/matches/42", "/api/v1/events/99"])
        )
        XCTAssertEqual(recorder.requestPaths.count, 3)
        XCTAssertFalse(recorder.requestPaths.contains { $0.hasPrefix("/api/v1/player/") })
        XCTAssertFalse(recorder.requestPaths.contains { $0.hasPrefix("/api/v1/team/") })
        XCTAssertEqual(recorder.authorizationValues, Set(["fixture-token"]))
        XCTAssertEqual(recorder.appNameValues, Set(["dev.staticvar.vlr"]))
    }

    func testRefreshRetainsCachedTeamScheduleMatchMissingFromOverview() async throws {
        let recorder = RequestRecorder(responses: [
            "/api/v1/matches/": .ok("[]"),
            "/api/v1/matches/200": .ok("""
                {"event":{"name":"Challengers","series":"Bo3","stage":"Final","date":"2026-09-14T18:00:00Z","status":"upcoming"},"teams":[{"name":"Alpha","score":null},{"name":"Gamma","score":null}]}
                """),
        ])
        let service = WidgetRefreshService(api: recorder.client())
        let cachedMatch = UpcomingMatch(
            id: "200",
            event: "Challengers",
            team1: "Alpha",
            team2: "Gamma",
            startTimeEpochMillis: nil,
            status: .upcoming,
            score1: nil,
            score2: nil,
            format: "BO3",
            stage: "Final"
        )
        let source = snapshot(
            favorites: WidgetFavoriteIDs(matchIds: [], teamIds: ["88"], eventIds: [], playerIds: ["77"]),
            matches: [cachedMatch]
        )

        let refreshed = try await service.refresh(source: source)

        XCTAssertEqual(refreshed.matches.map(\.id), ["200"])
        XCTAssertEqual(Set(recorder.requestPaths), Set(["/api/v1/matches", "/api/v1/matches/200"]))
        XCTAssertEqual(recorder.requestPaths.count, 2)
        XCTAssertFalse(recorder.requestPaths.contains { $0.hasPrefix("/api/v1/player/") })
        XCTAssertFalse(recorder.requestPaths.contains { $0.hasPrefix("/api/v1/team/") })
    }

    func testUnknownStatusKeepsPreviousMatchWithoutTreatingItAsLive() async throws {
        let previous = UpcomingMatch(
            id: "42",
            event: "Champions",
            team1: "Alpha",
            team2: "Beta",
            startTimeEpochMillis: nil,
            status: .upcoming,
            score1: nil,
            score2: nil,
            format: "BO3",
            stage: "Final"
        )
        let recorder = RequestRecorder(responses: [
            "/api/v1/matches/": .ok("""
                [{"id":"42","event":"Champions","series":"Bo3","status":"tbd","team1":{"name":"Alpha"},"team2":{"name":"Beta"},"time":null,"event_id":"9"}]
                """),
            "/api/v1/matches/42": .ok("""
                {"event":{"name":"Champions","series":"Bo3","stage":"Final","date":null,"status":"tbd"},"teams":[{"name":"Alpha"},{"name":"Beta"}]}
                """),
        ])
        let service = WidgetRefreshService(api: recorder.client())
        let source = snapshot(
            favorites: WidgetFavoriteIDs(matchIds: ["42"], teamIds: [], eventIds: [], playerIds: []),
            matches: [previous]
        )

        let refreshed = try await service.refresh(source: source)

        XCTAssertEqual(refreshed.matches, [previous])
        XCTAssertEqual(refreshed.matches[0].status, .upcoming)
    }

    func testOverviewKeepsCachedMetadataWithoutFetchingDetails() async throws {
        let recorder = RequestRecorder(responses: [
            "/api/v1/matches/": .ok("""
                [{"id":"42","event":"Champions","series":"","status":"live","team1":{"name":"Beta","score":2},"team2":{"name":"Alpha","score":1},"time":null,"event_id":"5"}]
                """),
        ])
        let source = snapshot(
            favorites: WidgetFavoriteIDs(matchIds: ["42"], teamIds: [], eventIds: [], playerIds: []),
            matches: [liveMatch(score1: 0, score2: 0)]
        )
        let refreshed = try await WidgetRefreshService(api: recorder.client()).refresh(source: source)
        let match = try XCTUnwrap(refreshed.matches.first)
        XCTAssertEqual(match.team1, "Beta")
        XCTAssertEqual(match.score1, 2)
        XCTAssertEqual(match.score2, 1)
        XCTAssertEqual(match.format, "BO3")
        XCTAssertEqual(match.stage, "Final")
        XCTAssertEqual(recorder.requestPaths, ["/api/v1/matches"])
    }

    func testCompletedOverviewRemovesCachedMatchWithoutDetails() async throws {
        let recorder = RequestRecorder(responses: [
            "/api/v1/matches/": .ok("""
                [{"id":"42","event":"Champions","series":"Bo3","status":"completed","team1":{"name":"Alpha","score":2},"team2":{"name":"Beta","score":1},"time":null,"event_id":"5"}]
                """),
        ])
        let source = snapshot(
            favorites: WidgetFavoriteIDs(matchIds: ["42"], teamIds: [], eventIds: [], playerIds: []),
            matches: [liveMatch(score1: 1, score2: 1)]
        )
        let refreshed = try await WidgetRefreshService(api: recorder.client()).refresh(source: source)
        XCTAssertTrue(refreshed.matches.isEmpty)
        XCTAssertEqual(recorder.requestPaths.count, 1)
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
        let result = await WidgetRefreshCoordinator().snapshot(
            repository: repository,
            service: WidgetRefreshService(api: recorder.client()), now: now.addingTimeInterval(60)
        )
        XCTAssertEqual(result, source)
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
        let recorder = RequestRecorder(responses: ["/api/v1/matches/": .ok("[]")], delayNanoseconds: 50_000_000)
        let coordinator = WidgetRefreshCoordinator()
        let service = WidgetRefreshService(api: recorder.client())
        let now = Date.now
        async let first = coordinator.snapshot(repository: repository, service: service, now: now)
        async let second = coordinator.snapshot(repository: repository, service: service, now: now)
        let results = await (first, second)
        XCTAssertEqual(results.0, results.1)
        let cached = await coordinator.snapshot(repository: repository, service: service, now: now.addingTimeInterval(60))
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
        let recorder = RequestRecorder(responses: ["/api/v1/matches/": Response(statusCode: 503, body: Data())])
        let service = WidgetRefreshService(api: recorder.client())
        let coordinator = WidgetRefreshCoordinator()
        let first = await coordinator.snapshot(repository: repository, service: service, now: now)
        XCTAssertEqual(first, source)
        XCTAssertEqual(recorder.requestPaths.count, 1)
        let second = await coordinator.snapshot(repository: repository, service: service, now: now.addingTimeInterval(60))
        XCTAssertEqual(second, source)
        XCTAssertEqual(recorder.requestPaths.count, 1)
        _ = await coordinator.snapshot(repository: repository, service: service, now: now.addingTimeInterval(301))
        XCTAssertEqual(recorder.requestPaths.count, 2)
    }

    func testIncompleteEventMatchFallsBackToMatchDetails() async throws {
        for teams in ["[]", "[{\"name\":\"Alpha\"}]"] {
            let recorder = RequestRecorder(responses: [
                "/api/v1/matches/": .ok("[]"),
                "/api/v1/events/5": .ok("""
                    {"title":"Champions","matches":[{"id":"42","time":"18:00:00","date":"2026-09-24","status":"live","teams":\(teams),"round":"Final","stage":"Playoffs"}]}
                    """),
                "/api/v1/matches/42": .ok("""
                    {"event":{"name":"Champions","series":"Bo3","stage":"Final","date":null,"status":"live"},"score":"2:1","teams":[{"name":"Alpha"},{"name":"Beta"}]}
                    """),
            ])
            let source = snapshot(
                favorites: WidgetFavoriteIDs(matchIds: [], teamIds: [], eventIds: ["5"], playerIds: []),
                matches: [liveMatch(score1: 1, score2: 1)]
            )
            let refreshed = try await WidgetRefreshService(api: recorder.client()).refresh(source: source)
            XCTAssertEqual(refreshed.matches.first?.score1, 2)
            XCTAssertEqual(Set(recorder.requestPaths), Set(["/api/v1/matches", "/api/v1/events/5", "/api/v1/matches/42"]))
        }
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

    func testNonSuccessResponseFailsRefresh() async {
        let recorder = RequestRecorder(responses: [
            "/api/v1/matches/": Response(statusCode: 503, body: Data()),
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
            XCTAssertEqual(error as? WidgetRefreshError, .httpStatus(503))
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
        if let response = responses[path] {
            return response
        }
        return responses[path.hasSuffix("/") ? String(path.dropLast()) : path + "/"]
    }
}
