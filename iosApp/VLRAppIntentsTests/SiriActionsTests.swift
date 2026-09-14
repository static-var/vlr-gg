import AppIntents
import XCTest
import shared
@testable import VLR

@MainActor
final class SiriActionsTests: XCTestCase {
    func testFreshUpcomingMatchIncludesTeamsEventAndLocalStartTime() {
        let timestamp: Int64 = 1_900_000_000_000
        let response = summary(match(timestamp: timestamp))
        XCTAssertTrue(response.contains("Fnatic versus Sentinels in Champions starts"))
        let formatter = DateFormatter()
        formatter.dateStyle = .full
        formatter.timeStyle = .short
        XCTAssertTrue(response.contains(formatter.string(from: Date(timeIntervalSince1970: Double(timestamp) / 1_000))))
        XCTAssertFalse(response.contains("saved"))
    }

    func testLiveMatchNeverSpeaksScores() {
        let response = summary(match(status: "LIVE"))
        XCTAssertEqual(response, "Fnatic versus Sentinels in Champions is live.")
    }

    func testStaleMatchDisclosesSavedSchedule() {
        let response = summary(match(), refreshed: false)
        XCTAssertTrue(response.hasPrefix("From saved matches:"))
        XCTAssertTrue(response.hasSuffix("The schedule may have changed."))
    }

    func testMissingStartTimeIsExplicit() {
        XCTAssertEqual(summary(match()), "Fnatic versus Sentinels in Champions is upcoming. The start time has not been announced.")
    }

    func testNoFavoritesExplainsHowToStart() {
        let result = SiriMatchResult(match: nil, hasFavorites: false, refreshed: true)
        XCTAssertEqual(SiriMatchResponse.summary(result), "Add favorite teams, players, matches, or events in Valorant Esports first.")
    }

    func testSuccessfulEmptyScheduleSaysNoMatches() {
        XCTAssertEqual(summary(nil), "There are no live or upcoming matches from your favorites right now.")
    }

    func testFailedRefreshWithoutCachedMatchDoesNotClaimNoMatches() {
        XCTAssertEqual(summary(nil, refreshed: false), SiriMatchResponse.unavailable)
    }

    func testNextMatchIntentRunsAgainstAppServices() async throws {
        let result = try await NextFavoriteMatchIntent().perform()
        XCTAssertFalse(try XCTUnwrap(result.value).isEmpty)
    }

    func testOpenNextMatchIntentRunsExistingDeepLinkHandoff() async throws {
        let result = try await OpenNextFavoriteMatchIntent().perform()
        let url = try XCTUnwrap(result.value)
        XCTAssertEqual(url.scheme, "vlr")
        XCTAssertTrue(["match", "home"].contains(url.host ?? ""))
    }

    func testSpoilerCycleRestoresWidgetScoresWithoutChangingScheduleFreshness() async throws {
        let url = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: url) }
        let data = Data("""
        {"savedAtEpochMillis":123,"hasFavorites":true,"spoilersHidden":false,
         "favorites":{"matchIds":["42"],"teamIds":[],"eventIds":[],"playerIds":[]},
         "matches":[{"id":"42","event":"Final","team1":"Alpha","team2":"Bravo",
         "startTimeEpochMillis":null,"status":"LIVE","score1":13,"score2":7,"format":"BO3","stage":""}],
         "theme":{"background":0,"surface":0,"accent":0,"content":0,"secondary":0,"border":0,"monospace":false}}
        """.utf8)
        let source = try JSONDecoder().decode(VLR.UpcomingMatchesSnapshot.self, from: data)
        let matchesJSON = String(decoding: try JSONEncoder().encode(source.matches), as: UTF8.self)
        try data.write(to: url)
        try await WidgetSnapshotStore.setSpoilersHidden(true, matchesJSON: matchesJSON, snapshotURL: url)
        let hidden = try JSONDecoder().decode(VLR.UpcomingMatchesSnapshot.self, from: Data(contentsOf: url))
        XCTAssertTrue(hidden.spoilersHidden)
        XCTAssertNil(hidden.matches.first?.score1)
        XCTAssertNil(hidden.matches.first?.score2)
        XCTAssertEqual(hidden.savedAtEpochMillis, 123)
        XCTAssertEqual(hidden.favorites.matchIds, ["42"])
        try await WidgetSnapshotStore.setSpoilersHidden(false, matchesJSON: matchesJSON, snapshotURL: url)
        let visible = try JSONDecoder().decode(VLR.UpcomingMatchesSnapshot.self, from: Data(contentsOf: url))
        XCTAssertFalse(visible.spoilersHidden)
        XCTAssertEqual(visible.matches.first?.id, "42")
        XCTAssertEqual(visible.matches.first?.score1, 13)
        XCTAssertEqual(visible.matches.first?.score2, 7)
        XCTAssertEqual(visible.savedAtEpochMillis, 123)
    }

    private func summary(_ match: UpcomingWidgetMatch?, refreshed: Bool = true) -> String {
        SiriMatchResponse.summary(SiriMatchResult(match: match, hasFavorites: true, refreshed: refreshed))
    }

    private func match(status: String = "UPCOMING", timestamp: Int64? = nil) -> UpcomingWidgetMatch {
        UpcomingWidgetMatch(
            id: "test-match",
            event: "Champions",
            team1: "Fnatic",
            team2: "Sentinels",
            startTimeEpochMillis: timestamp.map { KotlinLong(value: $0) },
            status: status,
            score1: KotlinInt(value: 13),
            score2: KotlinInt(value: 7),
            format: "BO3",
            stage: "Final"
        )
    }
}
