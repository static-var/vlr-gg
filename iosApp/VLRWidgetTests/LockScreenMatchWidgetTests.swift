import XCTest
import SwiftUI
import WidgetKit

final class LockScreenMatchWidgetTests: XCTestCase {
    private let now = Date(timeIntervalSince1970: 1_792_497_600)
    private let locale = Locale(identifier: "en_US")
    private let gmt = TimeZone(secondsFromGMT: 0)!

    func testStateNeedsAppWithoutSnapshotAndNoFavoritesWhenNothingIsFavorited() {
        XCTAssertEqual(LockScreenMatchState.make(snapshot: nil, now: now), .needsApp)
        var source = snapshot(matches: [upcoming(in: 3_600)])
        source.hasFavorites = false
        XCTAssertEqual(LockScreenMatchState.make(snapshot: source, now: now), .noFavorites)
    }

    func testStateLiveBeatsRecentResult() {
        let source = snapshot(matches: [upcoming(in: 3_600), live()], results: [completed(hoursAgo: 2)])
        XCTAssertEqual(LockScreenMatchState.make(snapshot: source, now: now), .live(live()))
    }

    func testStateShowsMostRecentResultWithinADayWithAndWithoutNextMatch() {
        let recent = completed(id: "recent", hoursAgo: 3)
        let older = completed(id: "older", hoursAgo: 20)
        let next = upcoming(in: 3_600)
        XCTAssertEqual(
            LockScreenMatchState.make(snapshot: snapshot(matches: [next], results: [older, recent]), now: now),
            .completed(recent, next: next)
        )
        XCTAssertEqual(
            LockScreenMatchState.make(snapshot: snapshot(results: [recent]), now: now),
            .completed(recent, next: nil)
        )
    }

    func testStateSkipsResultsOlderThanADayOrInTheFuture() {
        let next = upcoming(in: 3_600)
        let source = snapshot(matches: [next], results: [completed(id: "future", hoursAgo: -1), completed(id: "old", hoursAgo: 25)])
        XCTAssertEqual(LockScreenMatchState.make(snapshot: source, now: now), .upcoming(next))
        XCTAssertEqual(LockScreenMatchState.make(snapshot: snapshot(results: [completed(hoursAgo: 25)]), now: now), .noMatches)
    }

    func testStateUpcomingOnlyAndNoMatches() {
        let next = upcoming(in: 7_200)
        XCTAssertEqual(LockScreenMatchState.make(snapshot: snapshot(matches: [next]), now: now), .upcoming(next))
        XCTAssertEqual(LockScreenMatchState.make(snapshot: snapshot(), now: now), .noMatches)
    }

    func testExpiryIsResultDeadlineOrKickoff() {
        XCTAssertEqual(
            LockScreenMatchState.completed(completed(hoursAgo: 3), next: nil).expiry(now: now),
            now.addingTimeInterval(21 * 3_600)
        )
        XCTAssertEqual(LockScreenMatchState.upcoming(upcoming(in: 600)).expiry(now: now), now.addingTimeInterval(600))
        XCTAssertNil(LockScreenMatchState.live(live()).expiry(now: now))
    }

    func testInlineTextForEveryMatchState() {
        let cases: [(LockScreenMatchState, Bool, String)] = [
            (.live(live()), false, "SEN 2–1 G2 · Live"),
            (.live(live()), true, "SEN vs G2 · Live"),
            (.upcoming(upcoming(in: 3 * 3_600)), false, "PRX vs FNC · 3:00\u{202F}PM"),
            (.upcoming(upcoming(in: 2 * 86_400 + 9 * 3_600)), false, "PRX vs FNC · Thu 9\u{202F}PM"),
            (.completed(completed(hoursAgo: 3), next: nil), false, "SEN 2–1 G2 · Final"),
            (.completed(completed(hoursAgo: 3), next: nil), true, "SEN vs G2 · Final"),
            (.noFavorites, false, "Pick favorites"),
        ]
        for (state, hidden, expected) in cases {
            XCTAssertEqual(text(state, spoilersHidden: hidden).inline, expected)
        }
    }

    func testRectangularLinesForLiveUpcomingAndCompleted() {
        XCTAssertEqual(
            text(.live(live())).rectangular,
            .live(.init(team1: "SEN", center: "2–1", team2: "G2", winner: .team1), caption: "● Live · Map 4 of 5 · Champions")
        )
        XCTAssertEqual(
            text(.live(live()), spoilersHidden: true).rectangular,
            .live(.init(team1: "SEN", center: "vs", team2: "G2", winner: nil), caption: "● Live · Champions")
        )
        let soon = upcoming(in: 3_600)
        XCTAssertEqual(
            text(.upcoming(soon)).rectangular,
            .upcoming(matchup: "PRX vs FNC", kickoff: .countdown(to: now.addingTimeInterval(3_600)), caption: "Upper Final · Masters")
        )
        XCTAssertEqual(
            text(.completed(completed(hoursAgo: 3), next: upcoming(in: 2 * 86_400 + 9 * 3_600))).rectangular,
            .completed(
                .init(team1: "SEN", center: "2–1", team2: "G2", winner: .team1),
                status: "Final",
                caption: "Champions · Grand Final",
                next: "Next: PRX vs FNC · Thu 9\u{202F}PM"
            )
        )
    }

    func testTeamCodePrefersTagThenCachedCodeThenName() {
        let lookup: (String?) -> String? = { $0 == "624" ? "PRX" : nil }
        XCTAssertEqual(LockScreenMatchText.teamCode(name: "Gen.G", tag: " g2 ", id: "624", lookup: lookup), "G2")
        XCTAssertEqual(LockScreenMatchText.teamCode(name: "Paper Rex", tag: nil, id: "624", lookup: lookup), "PRX")
        XCTAssertEqual(LockScreenMatchText.teamCode(name: "Fnatic", tag: "", id: "2593", lookup: lookup), "FNA")
    }

    func testLegacySnapshotDecodesWithoutResultsOrTeamFields() throws {
        let source = snapshot(matches: [upcoming(in: 3_600)], results: [completed(hoursAgo: 3)])
        var json = try XCTUnwrap(JSONSerialization.jsonObject(with: JSONEncoder().encode(source)) as? [String: Any])
        json.removeValue(forKey: "results")
        var matches = try XCTUnwrap(json["matches"] as? [[String: Any]])
        for key in ["team1Id", "team2Id", "team1Tag", "team2Tag"] { matches[0].removeValue(forKey: key) }
        json["matches"] = matches
        let decoded = try JSONDecoder().decode(UpcomingMatchesSnapshot.self, from: JSONSerialization.data(withJSONObject: json))
        XCTAssertEqual(decoded.results, [])
        XCTAssertEqual(decoded.matches.first?.id, "200")
        XCTAssertNil(decoded.matches.first?.team1Tag)
        XCTAssertNil(decoded.matches.first?.team2Id)
    }

    func testRefreshedResultsFollowSpoilerSettingAndSurviveConfiguration() {
        let result = completed(hoursAgo: 3)
        let shown = snapshot().withRefreshedMatches([], results: [result], at: now)
        XCTAssertEqual(shown.results.first?.score1, 2)
        XCTAssertEqual(shown.results.first?.team1Tag, "SEN")
        var hiddenSource = snapshot()
        hiddenSource.spoilersHidden = true
        let hidden = hiddenSource.withRefreshedMatches([], results: [result], at: now)
        XCTAssertNil(hidden.results.first?.score1)
        XCTAssertEqual(hidden.results.first?.id, "300")
        XCTAssertEqual(shown.usingConfiguration(from: snapshot()).results, [result])
        XCTAssertNil(shown.usingConfiguration(from: hiddenSource).results.first?.score2)
    }

    @MainActor
    func testLockScreenLayoutsAttachInlineAndRectangularRenders() throws {
        let scenarios: [(String, UpcomingMatchesSnapshot)] = [
            ("live", snapshot(matches: [live()])),
            ("upcoming", snapshot(matches: [upcoming(in: 3_600)])),
            ("upcoming-later", snapshot(matches: [upcoming(in: 2 * 86_400)])),
            ("completed", snapshot(results: [completed(hoursAgo: 3)])),
            ("completed-next", snapshot(matches: [upcoming(in: 86_400)], results: [completed(hoursAgo: 3)])),
            ("hidden", snapshot(spoilersHidden: true, matches: [live().withoutScores()])),
            ("no-favorites", { var value = snapshot(); value.hasFavorites = false; return value }()),
        ]
        let layouts: [(String, WidgetFamily, CGSize)] = [
            ("rectangular", .accessoryRectangular, CGSize(width: 160, height: 72)),
            ("inline", .accessoryInline, CGSize(width: 234, height: 26)),
        ]
        for (scenario, source) in scenarios {
            for (name, family, size) in layouts {
                let content = LockScreenMatchWidgetView(entry: LockScreenMatchEntry(date: now, snapshot: source), familyOverride: family)
                    .frame(width: size.width, height: size.height)
                    .foregroundStyle(.white)
                    .background(Color.black)
                    .environment(\.colorScheme, .dark)
                    .environment(\.locale, locale)
                    .environment(\.timeZone, gmt)
                let renderer = ImageRenderer(content: content)
                renderer.scale = 2
                let image = try XCTUnwrap(renderer.uiImage)
                XCTAssertEqual(image.size, size)
                let attachment = XCTAttachment(image: image)
                attachment.name = "lock-\(scenario)-\(name)"
                attachment.lifetime = .keepAlways
                add(attachment)
            }
        }
    }

    private func text(_ state: LockScreenMatchState, spoilersHidden: Bool = false) -> LockScreenMatchText {
        LockScreenMatchText(state: state, now: now, locale: locale, timeZone: gmt, spoilersHidden: spoilersHidden) { _ in nil }
    }

    private func snapshot(
        spoilersHidden: Bool = false,
        matches: [UpcomingMatch] = [],
        results: [UpcomingMatch] = []
    ) -> UpcomingMatchesSnapshot {
        UpcomingMatchesSnapshot(
            savedAtEpochMillis: Int64(now.timeIntervalSince1970 * 1_000),
            clientId: "client",
            hasFavorites: true,
            favorites: WidgetFavoriteIDs(matchIds: [], teamIds: ["2"], eventIds: [], playerIds: []),
            spoilersHidden: spoilersHidden,
            matches: matches,
            results: results,
            theme: .preview
        )
    }

    private func live() -> UpcomingMatch {
        UpcomingMatch(
            id: "100", event: "Champions", team1: "Sentinels", team2: "G2 Esports",
            startTimeEpochMillis: epochMillis(now.addingTimeInterval(-3_600)), status: .live,
            score1: 2, score2: 1, format: "BO5", stage: "",
            team1Id: "2", team2Id: "11058", team1Tag: "SEN", team2Tag: "G2"
        )
    }

    private func upcoming(in seconds: TimeInterval) -> UpcomingMatch {
        UpcomingMatch(
            id: "200", event: "Masters", team1: "Paper Rex", team2: "Fnatic",
            startTimeEpochMillis: epochMillis(now.addingTimeInterval(seconds)), status: .upcoming,
            score1: nil, score2: nil, format: "BO3", stage: "Upper Final",
            team1Id: "624", team2Id: "2593", team1Tag: "PRX", team2Tag: "FNC"
        )
    }

    private func completed(id: String = "300", hoursAgo: Double) -> UpcomingMatch {
        UpcomingMatch(
            id: id, event: "Champions", team1: "Sentinels", team2: "G2 Esports",
            startTimeEpochMillis: epochMillis(now.addingTimeInterval(-hoursAgo * 3_600)), status: .completed,
            score1: 2, score2: 1, format: "BO3", stage: "Grand Final",
            team1Id: "2", team2Id: "11058", team1Tag: "SEN", team2Tag: "G2"
        )
    }

    private func epochMillis(_ date: Date) -> Int64 {
        Int64(date.timeIntervalSince1970 * 1_000)
    }
}
