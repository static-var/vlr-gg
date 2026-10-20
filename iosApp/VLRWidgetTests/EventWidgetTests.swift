import XCTest
import SwiftUI
import WidgetKit

final class EventWidgetTests: XCTestCase {
    let now = widgetInstant("2026-10-20T12:00:00Z")!
    let zone = TimeZone(secondsFromGMT: 0)!
    let locale = Locale(identifier: "en_US")

    func fixtureBytes(_ name: String) throws -> Data {
        let bundle = Bundle(for: Self.self)
        let url = bundle.url(forResource: name, withExtension: "json", subdirectory: "EventWidget")
            ?? bundle.url(forResource: name, withExtension: "json")
        return try Data(contentsOf: XCTUnwrap(url))
    }

    func data(_ id: String, edit: ((inout [String: Any]) -> Void)? = nil) throws -> EventWidgetData {
        var json = try XCTUnwrap(JSONSerialization.jsonObject(with: fixtureBytes("event-" + id)) as? [String: Any])
        edit?(&json)
        return try EventWidgetMapper.decode(JSONSerialization.data(withJSONObject: json), expectedEventID: id, at: now)
    }

    func source(_ id: String, hidden: Bool = false, teams: [String] = []) -> UpcomingMatchesSnapshot {
        .init(savedAtEpochMillis: 0, clientId: "client", hasFavorites: true,
              favorites: .init(matchIds: [], teamIds: teams, eventIds: [id], playerIds: []),
              spoilersHidden: hidden, matches: [], theme: .preview, favoriteEvents: [.init(id: id, name: "Event")])
    }

    func plan(_ data: EventWidgetData, at date: Date? = nil, hidden: Bool = false, teams: [String] = [], fetchedAt: Date? = nil) -> EventWidgetPlan {
        let date = date ?? now
        return EventWidgetEngine.plan(.init(key: .init(entityID: data.event.id, clientID: "client"), fetchedAt: fetchedAt ?? date, value: data),
            selectedEventID: data.event.id, source: source(data.event.id, hidden: hidden, teams: teams), at: date, timeZone: zone)
    }

    func content(_ plan: EventWidgetPlan) throws -> EventWidgetContent {
        guard case .content(let value) = plan.entries[0].content else { throw WidgetRefreshError.invalidResponse }
        return value
    }

    func testRealFixturesNormalizeAndFinishedChampionUsesMatchingFinal() throws {
        let champions = try data("2766")
        XCTAssertEqual(champions.matches.count, 34)
        XCTAssertEqual(champions.groups.count, 0)
        XCTAssertEqual(champions.prizes.count, 8)
        guard case .visible(.finished(.champion(let team, let final), .prizes(let prizes))) = try content(plan(champions)).body else { return XCTFail() }
        XCTAssertEqual(team.name, "LOUD")
        XCTAssertEqual(final?.matchID, "754737")
        XCTAssertEqual(final?.loser.name, "Team Vitality")
        XCTAssertEqual(final?.score.text, "3–1")
        XCTAssertEqual(prizes.map(\.team.name), ["Team Vitality", "100 Thieves", "G2 Esports", "NRG"])
        let noFirst = EventWidgetData(event: champions.event, matches: champions.matches, groups: [], prizes: [], detail: nil)
        guard case .visible(.finished(.champion(let fallback, _), _)) = try content(plan(noFirst)).body else { return XCTFail() }
        XCTAssertEqual(fallback.name, "LOUD")
        let mismatched = EventWidgetData(event: champions.event, matches: champions.matches, groups: [],
            prizes: [.init(position: "1st", amount: "", team: .init(id: "other", name: "Other"))], detail: nil)
        guard case .visible(.finished(.champion(_, let result), _)) = try content(plan(mismatched)).body else { return XCTFail() }
        XCTAssertNil(result)
        let americas = try data("2977")
        XCTAssertEqual(americas.groups.map(\.standings.count), [6, 6])
        guard case .visible(.finished(_, .prizes)) = try content(plan(americas)).body else { return XCTFail() }
    }

    func testUpcomingIgnoresReportedStatusAndFavoritesChooseTheGroup() throws {
        let event = try data("3122")
        XCTAssertEqual(event.matches.filter { if case .completed = $0.phase { return true }; return false }.count, 63)
        XCTAssertEqual(event.matches.filter { $0.phase == .upcoming }.count, 15)
        XCTAssertEqual(event.groups.map(\.standings.count), [4, 4])
        for (favorites, expectedID, groupName) in [([], "777919", "Group A"), (["4915"], "777925", "Group B")] {
            guard case .visible(.scheduled(.next(let fixture), .group(let group))) = try content(plan(event, teams: favorites)).body else { return XCTFail() }
            XCTAssertEqual(fixture.id, expectedID)
            XCTAssertEqual(group.name, groupName)
        }
        let before = widgetInstant("2026-10-01T12:00:00Z")!
        let clean = EventWidgetData(event: event.event, matches: event.matches.map { .init(fixture: $0.fixture, phase: .upcoming) }, groups: event.groups, prizes: [], detail: nil)
        guard case .scheduled(.next(let selected)) = EventWidgetEngine.selection(in: clean, fetchedAt: before, favoriteTeamIDs: ["4915"], spoilersHidden: false, at: before, timeZone: zone) else { return XCTFail() }
        XCTAssertEqual(selected.id, "777925")
    }

    func testLiveAlwaysWinsAndFavoriteLiveKeepsOriginalScoreOrder() throws {
        let event = try data("3122")
        let selected = event.matches.filter { ["777919", "777925"].contains($0.fixture.id) }
        let live = EventWidgetData(event: event.event, matches: selected.map { .init(fixture: $0.fixture, phase: .live(.init(team: 0, opponent: 1))) }, groups: event.groups, prizes: [], detail: nil)
        guard case .visible(.live(let hero, _)) = try content(plan(live, teams: ["4915"])).body else { return XCTFail() }
        XCTAssertEqual(hero.fixture.id, "777925")
        XCTAssertEqual(hero.score?.text, "0–1")
        XCTAssertEqual(hero.otherLiveCount, 1)
        let oneLive = EventWidgetData(event: event.event, matches: event.matches.map { .init(fixture: $0.fixture, phase: $0.fixture.id == "777919" ? .live(nil) : $0.phase) }, groups: [], prizes: [], detail: nil)
        guard case .visible(.live(let nonfavorite, _)) = try content(plan(oneLive, teams: ["4915"])).body else { return XCTFail() }
        XCTAssertEqual(nonfavorite.fixture.id, "777919")
    }

    func testDateBoundsStrictDaysUTCStartsAndFloatingLabels() throws {
        let cases = [
            ("Sep 24 – Oct 18, 2026", "2026-09-24", "2026-10-18"),
            ("Dec 11–14, 2026", "2026-12-11", "2026-12-14"),
            ("Sep 12 – TBD", "2026-09-12", nil), ("Nov 1 – TBD", "2026-11-01", nil),
            ("Feb 20 – Mar 3, 2027", "2027-02-20", "2027-03-03"),
            ("Dec 28 – Jan 4, 2027", "2026-12-28", "2027-01-04"),
            ("Oct 1 - Oct 4, 2026", "2026-10-01", "2026-10-04"),
            ("Feb 29 - Mar 1, 2028", "2028-02-29", "2028-03-01"),
        ]
        for (text, start, end) in cases {
            let bounds = EventWidgetDates.parse(text, matchDays: [], reference: now)
            XCTAssertEqual(bounds, EventDateBounds(start: WidgetDay(start), endInclusive: end.flatMap(WidgetDay.init)), text)
        }
        for text in ["Feb 29 - Mar 1, 2027", "Oct 30 - Oct 1, 2026", "Feb 31 - Mar 2, 2026", "Oct 1 - Oct 2, 26", "bad"] {
            XCTAssertEqual(EventWidgetDates.parse(text, matchDays: [], reference: now), .init(start: nil, endInclusive: nil), text)
        }
        XCTAssertEqual(EventWidgetDates.parse("bad", matchDays: [WidgetDay("2027-02-20")!, WidgetDay("2027-03-03")!], reference: now),
                       .init(start: WidgetDay("2027-02-20"), endInclusive: WidgetDay("2027-03-03")))
        let instant = widgetInstant("2026-10-18T06:00:00Z")
        XCTAssertEqual(EventWidgetDates.matchStart(date: "2026-10-18", time: "06:00"), instant)
        XCTAssertEqual(EventWidgetDates.matchStart(date: "2026-10-18", time: "06:00:00"), instant)
        XCTAssertEqual(EventWidgetDates.matchStart(date: "", time: "2026-10-18T06:00:00.125Z"), instant?.addingTimeInterval(0.125))
        for time in ["", "TBD", "25:00", "06:70", "6:00"] { XCTAssertNil(EventWidgetDates.matchStart(date: "2026-10-18", time: time)) }
        XCTAssertNil(EventWidgetDates.matchStart(date: "2026-02-30", time: "06:00"))
        XCTAssertNil(WidgetDay("26-10-18"))
        for name in ["America/Los_Angeles", "Asia/Kolkata"] {
            XCTAssertEqual(widgetDate(WidgetDay("2026-10-18"), locale: locale, timeZone: TimeZone(identifier: name)!), "Oct 18")
        }
    }

    func testNoMatchesAndProgressUseInclusiveLocalDaysAcrossDST() throws {
        let event = try data("3161")
        let initial = try content(plan(event))
        guard case .visible(.gap(.notListed(let start), let list)) = initial.body else { return XCTFail() }
        XCTAssertEqual(start, WidgetDay("2027-02-20"))
        XCTAssertEqual(initial.header.progress, .empty)
        XCTAssertTrue(list.rows.isEmpty)
        XCTAssertEqual(plan(event).refreshAfter, now.addingTimeInterval(21_600))
        let during = widgetInstant("2027-02-21T12:00:00Z")!
        guard case .visible(.gap(.nextUnlisted, _)) = try content(plan(event, at: during)).body else { return XCTFail() }
        XCTAssertEqual(plan(event, at: during).refreshAfter, during.addingTimeInterval(7_200))
        let bounds = EventDateBounds(start: WidgetDay("2026-11-01"), endInclusive: WidgetDay("2026-11-01"))
        let la = TimeZone(identifier: "America/Los_Angeles")!
        XCTAssertEqual(EventWidgetDates.progress(bounds, phase: .inProgress, at: widgetInstant("2026-11-01T19:30:00Z")!, timeZone: la), .fraction(0.5))
        XCTAssertEqual(EventWidgetDates.progress(bounds, phase: .finished, at: now, timeZone: la), .complete)
        XCTAssertEqual(EventWidgetDates.progress(bounds, phase: .notStarted, at: during, timeZone: zone), .empty)
    }

    func testRosterSourcesUnknownSlotsAndMalformedOptionalFacts() throws {
        let event = try data("2977")
        let sentinels = event.groups.flatMap(\.standings).first { $0.team.name == "Sentinels" }?.team
        XCTAssertNil(sentinels?.id)
        XCTAssertNotNil(sentinels?.logo)
        XCTAssertEqual(event.matches.flatMap { $0.fixture.participants.teams }.first { $0.name == "LOUD" }?.id, "6961")
        let json = #"{"id":"test","title":"Title","teams":[{"id":"1","name":"A"},{"id":"2","name":" a "}],"prizes":[{"position":"1st","team":{"id":"3","name":"B","img":"b"}}],"matches":[{"id":"m","status":"live","teams":[{"name":"A","score":-1},{"name":"B","score":2}]},{"id":"m","teams":[]},{"id":"other","status":"tbd","teams":[{"name":""},{"name":"Stranger"}]},{"id":"blank","teams":[{},{},{}]},{"teams":[]}]}"#
        let normalized = try EventWidgetMapper.decode(Data(json.utf8), expectedEventID: "test", at: now)
        XCTAssertEqual(normalized.matches.count, 2)
        XCTAssertNil(normalized.matches[0].fixture.participants.left.team?.id)
        XCTAssertEqual(normalized.matches[0].fixture.participants.right.team?.id, "3")
        XCTAssertEqual(normalized.matches[0].phase, .live(nil))
        XCTAssertEqual(normalized.matches[1].fixture.participants.left, .undecided)
        XCTAssertEqual(normalized.matches[1].fixture.participants.right, .team(.init(id: nil, name: "Stranger")))
        XCTAssertThrowsError(try EventWidgetMapper.decode(Data(json.utf8), expectedEventID: "wrong", at: now))
        let minimal = try EventWidgetMapper.decode(Data(#"{"id":"test","title":"Title","matches":null,"prize":3}"#.utf8), expectedEventID: "test", at: now)
        XCTAssertEqual(minimal.matches, [])
        XCTAssertNil(minimal.event.prizePool)
    }

    func testGroupFallbackPreservesRankAndDoesNotUsePlayoffPairs() throws {
        let base = try data("2977")
        let group = try XCTUnwrap(base.groups.first { $0.name == "Group Omega" })
        let pair = WidgetMatchPair(left: .team(group.standings[0].team), right: .team(group.standings[1].team))
        func event(_ round: String, _ stage: String) -> EventWidgetData {
            let fixture = EventWidgetFixture(id: "next", participants: pair, start: now.addingTimeInterval(3_600), day: WidgetDay("2026-10-20"), round: round, stage: stage, format: nil)
            let meta = EventWidgetMetadata(id: "2977", title: "Event", logo: nil, location: "", prizePool: nil, dates: base.event.dates, reportedStatus: .ongoing)
            return .init(event: meta, matches: [.init(fixture: fixture, phase: .upcoming)], groups: base.groups, prizes: [], detail: nil)
        }
        guard case .visible(.scheduled(_, .group(let table))) = try content(plan(event("Week 3", "Group Stage"))).body else { return XCTFail() }
        XCTAssertEqual(table.rows.map(\.standing.team.name), Array(group.standings.prefix(5).map(\.team.name)))
        XCTAssertEqual(table.rows.map(\.rank), [1, 2, 3, 4, 5])
        guard case .visible(.scheduled(_, .matches)) = try content(plan(event("Grand Final", "Playoffs"))).body else { return XCTFail() }
        let exact = event("gRoUp OmEgA", "Group Stage")
        guard case .visible(.scheduled(_, .group)) = try content(plan(exact)).body else { return XCTFail() }
    }

    func testExpiryAndResultCutoffReprojectAtExactBoundary() throws {
        let event = try data("3122")
        let fixture = event.matches.first { $0.fixture.id == "777919" }!.fixture
        let start = try XCTUnwrap(fixture.start)
        let upcoming = EventWidgetData(event: event.event, matches: [.init(fixture: fixture, phase: .upcoming)], groups: [], prizes: [], detail: nil)
        guard case .visible(.scheduled) = try content(plan(upcoming, at: start.addingTimeInterval(21_600))).body else { return XCTFail() }
        guard case .visible(.gap) = try content(plan(upcoming, at: start.addingTimeInterval(21_601))).body else { return XCTFail() }
        XCTAssertEqual(plan(upcoming, at: start.addingTimeInterval(21_601)).refreshAfter, start.addingTimeInterval(22_501))
        let live = EventWidgetData(event: event.event, matches: [.init(fixture: fixture, phase: .live(nil))], groups: [], prizes: [], detail: nil)
        let livePlan = plan(live, at: now, fetchedAt: now)
        XCTAssertTrue(livePlan.entries.contains { $0.date == now.addingTimeInterval(10_801) })
        guard case .visible(.live) = try content(plan(live, at: now.addingTimeInterval(10_800), fetchedAt: now)).body else { return XCTFail() }
        guard case .visible(.gap) = try content(plan(live, at: now.addingTimeInterval(10_801), fetchedAt: now)).body else { return XCTFail() }
        let lastFixture = EventWidgetFixture(id: "last", participants: fixture.participants, start: now.addingTimeInterval(-43_199), day: WidgetDay("2026-10-20"), round: "Final", stage: "Playoffs", format: nil)
        let result = EventWidgetData(event: event.event, matches: [.init(fixture: lastFixture, phase: .completed(.init(team: 1, opponent: 2))), .init(fixture: fixture, phase: .upcoming)], groups: [], prizes: [], detail: nil)
        let resultPlan = plan(result)
        guard case .visible(.between(let hero, _)) = try content(resultPlan).body else { return XCTFail() }
        XCTAssertEqual(hero.last.score.text, "2–1")
        XCTAssertEqual(resultPlan.refreshAfter, now.addingTimeInterval(1))
        let cutoff = try XCTUnwrap(resultPlan.entries.first { $0.date == now.addingTimeInterval(1) })
        guard case .content(let after) = cutoff.content, case .visible(.scheduled) = after.body else { return XCTFail() }
        guard case .hidden(.scheduled) = try content(plan(result, hidden: true)).body else { return XCTFail() }
    }

    func testHiddenFinishedListsAreNeutralAndCurrentConfigurationWins() throws {
        let event = try data("2766")
        let hidden = try content(plan(event, hidden: true))
        guard case .hidden(.finished(let ended, let list)) = hidden.body else { return XCTFail() }
        XCTAssertEqual(ended.prizePool, "$2,250,000")
        XCTAssertEqual(list.rows.count, 3)
        XCTAssertTrue(list.rows.allSatisfy { $0.phase == .ended })
        let mediumLogos = hidden.logoSources(large: false)
        XCTAssertEqual(mediumLogos, [event.event.logo!])
        let record = FavoriteWidgetRecord(key: .init(entityID: "2766", clientID: "old"), fetchedAt: now, value: event)
        let wrongClient = EventWidgetEngine.plan(record, selectedEventID: "2766", source: source("2766"), at: now, timeZone: zone)
        XCTAssertEqual(wrongClient.entries[0].content, .placeholder(eventID: "2766"))
        let removed = EventWidgetEngine.plan(record, selectedEventID: "2766", source: source("3122"), at: now, timeZone: zone)
        XCTAssertEqual(removed.entries[0].content, .setup(.chooseEvent))
        XCTAssertNil(removed.refreshAfter)
        XCTAssertEqual(EventWidgetEngine.plan(nil, selectedEventID: nil, source: nil, at: now, timeZone: zone).entries[0].content, .setup(.noFavorites))
    }

    func testScheduleBoundariesAndRefreshCapsUseEveryEligibleStart() throws {
        let event = try data("3122")
        let start = widgetInstant("2026-10-21T12:00:00Z")!
        let first = EventWidgetFixture(id: "a", participants: .init(left: .team(.init(id: "a", name: "Alpha")), right: .undecided),
            start: start, day: WidgetDay("2026-10-21"), round: "Round", stage: "Group Stage", format: nil)
        let later = EventWidgetFixture(id: "b", participants: .init(left: .team(.init(id: "b", name: "Beta")), right: .undecided),
            start: start.addingTimeInterval(7_200), day: first.day, round: "Round", stage: "Group Stage", format: nil)
        let scheduled = EventWidgetData(event: event.event, matches: [.init(fixture: first, phase: .upcoming), .init(fixture: later, phase: .upcoming)], groups: [], prizes: [], detail: nil)
        let before = start.addingTimeInterval(-1)
        let result = plan(scheduled, at: before, teams: ["b"])
        XCTAssertEqual(result.refreshAfter, start)
        guard case .content(let kickoff) = result.entries.first(where: { $0.date == start.addingTimeInterval(7_200) })?.content,
              case .visible(.scheduled(let hero, _)) = kickoff.body else { return XCTFail() }
        XCTAssertEqual(hero.fixture.id, "b")
        let regular = plan(scheduled, at: before)
        guard case .content(let due) = regular.entries.first(where: { $0.date == start })?.content,
              case .visible(.scheduled(let next, _)) = due.body else { return XCTFail() }
        XCTAssertEqual(WidgetKickoff.make(start: next.fixture.start, now: start, locale: locale, timeZone: zone), .delayed("12:00\u{202F}PM"))
        XCTAssertEqual(plan(scheduled, at: start).refreshAfter, start.addingTimeInterval(300))
        XCTAssertEqual(plan(scheduled, at: start.addingTimeInterval(-86_400)).refreshAfter, start.addingTimeInterval(-84_600))
        XCTAssertEqual(plan(scheduled, at: start.addingTimeInterval(-86_401)).refreshAfter, start.addingTimeInterval(-79_201))
        let hidden = try content(plan(scheduled, at: before, hidden: true, teams: ["b"]))
        guard case .hidden(.scheduled(_, let rows)) = hidden.body else { return XCTFail() }
        XCTAssertEqual(rows.rows, [])
    }

    func testGroupWindowIncludesSixthFavoriteAndAmbiguousFallbackHasNoTable() throws {
        let base = try data("2977")
        var groups = base.groups
        let first = groups[0]
        let rows = first.standings.enumerated().map { index, row in
            EventWidgetStanding(team: .init(id: "row-\(index)", name: row.team.name, logo: row.team.logo),
                wins: row.wins, losses: row.losses, ties: row.ties, mapDifference: row.mapDifference)
        }
        groups[0] = .init(name: first.name, standings: rows)
        let fixture = EventWidgetFixture(id: "next", participants: .init(left: .team(rows[0].team), right: .team(rows[1].team)),
            start: now.addingTimeInterval(3_600), day: nil, round: first.name, stage: "Group Stage", format: nil)
        let event = EventWidgetMetadata(id: "2977", title: "Event", logo: nil, location: "", prizePool: nil, dates: base.event.dates, reportedStatus: .ongoing)
        let facts = EventWidgetData(event: event, matches: [.init(fixture: fixture, phase: .upcoming)], groups: groups, prizes: [], detail: nil)
        guard case .visible(.scheduled(_, .group(let table))) = try content(plan(facts, teams: ["row-5"])).body else { return XCTFail() }
        XCTAssertEqual(table.rows.map(\.rank), [2, 3, 4, 5, 6])
        XCTAssertEqual(table.rows.last?.isFavorite, true)
        let otherRound = EventWidgetFixture(id: fixture.id, participants: fixture.participants, start: fixture.start, day: nil, round: "Week 3", stage: "Group Stage", format: nil)
        let ambiguous = EventWidgetData(event: event, matches: [.init(fixture: otherRound, phase: .upcoming)],
            groups: [groups[0], .init(name: "Other", standings: rows)], prizes: [], detail: nil)
        guard case .visible(.scheduled(_, .matches)) = try content(plan(ambiguous)).body else { return XCTFail() }
    }

    func testSharedDetailsUseBo5AndOrientMapsByIdentity() throws {
        let event = try data("2766")
        let details = try JSONDecoder().decode(WidgetMatchDetailsResponse.self, from: fixtureBytes("match-754737"))
        let enriched = EventWidgetMapper.enrich(event, matchID: "754737", details: details)
        XCTAssertEqual(enriched.detail?.series.format, .bo5)
        XCTAssertEqual(enriched.detail?.series.maps.prefix(4).map(\.score?.text), ["13–11", "8–13", "13–5", "13–10"])
        XCTAssertEqual(enriched.detail?.series.maps.prefix(4).map(\.status), [.won, .lost, .won, .won])
        let reversed = WidgetMatchDetailsResponse(veto: details.veto, data: details.data, teams: details.teams.reversed(), event: details.event,
            current_map: details.current_map, total_maps: details.total_maps, map_count: details.map_count)
        XCTAssertEqual(EventWidgetMapper.enrich(event, matchID: "754737", details: reversed).detail, enriched.detail)
    }

    func testRefreshUsesOneOptionalDetailRequestAndBaseFallback() async throws {
        let bytes = try fixtureBytes("event-3122")
        let recorder = EventRequestRecorder(bytes: bytes)
        let api = WidgetAPIClient(baseURL: URL(string: "https://fixture.invalid")!, authorization: "fixture-token") { request in
            await recorder.load(request)
        }
        let store = FavoriteWidgetStore<EventWidgetData>(namespace: .event, directory: nil, now: { self.now })
        let service = EventWidgetRefreshService(api: api, store: store)
        let fetched = await service.refresh(eventID: "3122", source: source("3122"), at: now, timeZone: zone)
        XCTAssertEqual(fetched?.value.matches.count, 78)
        XCTAssertNil(fetched?.value.detail)
        let paths = await recorder.paths
        XCTAssertEqual(paths, ["/api/v1/events/3122", "/api/v1/matches/777919"])
        let headers = await recorder.headers
        XCTAssertEqual(headers, ["fixture-token|dev.staticvar.vlr|true", "fixture-token|dev.staticvar.vlr|true"])
        let invalid = await service.refresh(eventID: "removed", source: source("3122"), at: now, timeZone: zone)
        XCTAssertNil(invalid)
    }
}

private actor EventRequestRecorder {
    let bytes: Data
    var paths: [String] = []
    var headers: [String] = []
    init(bytes: Data) { self.bytes = bytes }
    func load(_ request: URLRequest) -> (Data, HTTPURLResponse) {
        let url = request.url!
        paths.append(url.path)
        headers.append((request.value(forHTTPHeaderField: "Authorization") ?? "") + "|" + (request.value(forHTTPHeaderField: "app-name") ?? "") + "|" + String(request.value(forHTTPHeaderField: "Accept-Language") != nil))
        return (bytes, HTTPURLResponse(url: url, statusCode: url.path.contains("/matches/") ? 503 : 200, httpVersion: nil, headerFields: nil)!)
    }
}
