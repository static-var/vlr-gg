import XCTest
import SwiftUI
import WidgetKit

final class WidgetRefreshTests: XCTestCase {
    @MainActor
    func testWidgetLayoutsAttachSmallMediumAndLargeRenders() throws {
        let renderSnapshot = snapshot(
            savedAtEpochMillis: Int64(Date.now.timeIntervalSince1970 * 1_000),
            favorites: WidgetFavoriteIDs(matchIds: ["748633"], teamIds: [], eventIds: [], playerIds: []),
            matches: [
                UpcomingMatch(
                    id: "748633",
                    event: "Valorant Champions Tour 2026",
                    team1: "Paper Rex",
                    team2: "FNATIC",
                    startTimeEpochMillis: Int64(Date.now.addingTimeInterval(1_800).timeIntervalSince1970 * 1_000),
                    status: .live,
                    score1: 1,
                    score2: 0,
                    format: "BO3",
                    stage: "Upper Final"
                ),
            ]
        )
        let entry = UpcomingMatchesEntry(date: .now, snapshot: renderSnapshot)
        let layouts: [(String, WidgetFamily, CGSize)] = [
            ("small", .systemSmall, CGSize(width: 158, height: 158)),
            ("medium", .systemMedium, CGSize(width: 338, height: 158)),
            ("large", .systemLarge, CGSize(width: 338, height: 354)),
        ]

        for (name, family, size) in layouts {
            let content = UpcomingMatchesWidgetView(entry: entry, familyOverride: family)
                .frame(width: size.width, height: size.height)
                .background(Color.white)
            let renderer = ImageRenderer(content: content)
            renderer.scale = 2
            let image = try XCTUnwrap(renderer.uiImage)
            let attachment = XCTAttachment(image: image)
            attachment.name = "widget-\(name)"
            attachment.lifetime = XCTAttachment.Lifetime.keepAlways
            add(attachment)
        }
    }

    func testRefreshDiscoversEveryFavoriteRouteAndMasksScores() async throws {
        let recorder = RequestRecorder(responses: [
            "/api/v1/matches/": .ok("""
                [{"id":"100","event":"Masters","series":"Bo3","status":"live","team1":{"id":"88","name":"Alpha","score":1},"team2":{"id":"2","name":"Beta","score":0},"time":"2026-09-12T18:00:00Z","event_id":"5"}]
                """),
            "/api/v1/player/77": .ok("{\"current_team\":{\"id\":\"88\"}}"),
            "/api/v1/team/88": .ok("""
                {"name":"Alpha","upcoming":[{"id":"200","event":"Challengers","stage":"Playoffs ⋅ Final","opponent":"Gamma","date":"2026-09-14T18:00:00Z"}]}
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

        XCTAssertEqual(Set(refreshed.matches.map(\.id)), Set(["42", "100", "200", "300"]))
        XCTAssertTrue(refreshed.matches.allSatisfy { $0.score1 == nil && $0.score2 == nil })
        XCTAssertEqual(refreshed.matches.first(where: { $0.id == "100" })?.format, "BO3")
        XCTAssertEqual(refreshed.matches.first(where: { $0.id == "100" })?.stage, "Swiss Round 2")
        XCTAssertEqual(recorder.authorizationValues, Set(["fixture-token"]))
        XCTAssertEqual(recorder.appNameValues, Set(["dev.staticvar.vlr"]))
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

    func testDetailEnrichmentKeepsScoresWithReversedTeams() async throws {
        let recorder = RequestRecorder(responses: [
            "/api/v1/matches/": .ok("""
                [{"id":"100","event":"Masters","series":"Bo3","status":"live","team1":{"name":"Alpha","score":1},"team2":{"name":"Beta","score":0},"time":null,"event_id":"5"}]
                """),
            "/api/v1/matches/100": .ok("""
                {"event":{"name":"Masters","series":"Bo3","stage":"Final","date":null,"status":"live"},"teams":[{"name":"Beta","score":null},{"name":"Alpha","score":null}]}
                """),
        ])
        let service = WidgetRefreshService(api: recorder.client())
        let source = snapshot(
            favorites: WidgetFavoriteIDs(matchIds: ["100"], teamIds: [], eventIds: [], playerIds: [])
        )

        let refreshed = try await service.refresh(source: source)
        let match = try XCTUnwrap(refreshed.matches.first)

        XCTAssertEqual(match.team1, "Beta")
        XCTAssertEqual(match.team2, "Alpha")
        XCTAssertEqual(match.score1, 0)
        XCTAssertEqual(match.score2, 1)
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
    private(set) var authorizationValues = Set<String>()
    private(set) var appNameValues = Set<String>()

    init(responses: [String: Response]) {
        self.responses = responses
    }

    func client(authorization: String? = nil) -> WidgetAPIClient {
        WidgetAPIClient(
            baseURL: URL(string: "https://fixture.invalid")!,
            authorization: authorization
        ) { [self] request in
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
        if let response = responses[path] {
            return response
        }
        return responses[path.hasSuffix("/") ? String(path.dropLast()) : path + "/"]
    }
}
