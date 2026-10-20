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
        let unknownTime = try data("2766") { json in
            var matches = json["matches"] as! [[String: Any]]
            let index = matches.firstIndex { $0["id"] as? String == "753455" }!
            matches[index]["time"] = ""
            json["matches"] = matches
        }
        XCTAssertEqual(unknownTime.matches.first?.fixture.id, "753455")
        XCTAssertEqual(unknownTime.matches.sorted(by: EventWidgetEngine.chronological).prefix(3).map(\.fixture.id),
                       ["753454", "753455", "753460"])
        let noPrizes = EventWidgetData(event: unknownTime.event, matches: unknownTime.matches, groups: [], prizes: [], detail: nil)
        guard case .visible(.finished(.champion(let latestWinner, let latestFinal), _)) = try content(plan(noPrizes)).body else {
            return XCTFail()
        }
        XCTAssertEqual(latestWinner.name, "LOUD")
        XCTAssertEqual(latestFinal?.matchID, "754737")
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
            let hidden = try content(plan(event, hidden: true, teams: favorites))
            guard case .hidden(.scheduled(let hiddenHero, let neutral)) = hidden.body else { return XCTFail() }
            XCTAssertEqual(hiddenHero.fixture.id, expectedID)
            XCTAssertEqual(neutral.heading, .upNext)
            XCTAssertEqual(neutral.rows.count, 3)
            XCTAssertTrue(neutral.rows.allSatisfy { $0.phase == .upcoming && $0.fixture.id != expectedID })

        }
        let betweenHidden = try content(plan(event, at: widgetInstant("2026-10-04T15:00:00Z")!, hidden: true))
        guard case .stage(let stage, _) = betweenHidden.header.subtitle else { return XCTFail() }
        XCTAssertEqual(stage, "Phase 2")
        let before = widgetInstant("2026-10-01T12:00:00Z")!
        let clean = EventWidgetData(event: event.event, matches: event.matches.map { .init(fixture: $0.fixture, phase: .upcoming) }, groups: event.groups, prizes: [], detail: nil)
        guard case .scheduled(.next(let selected)) = EventWidgetEngine.selection(in: clean, fetchedAt: before, favoriteTeamIDs: ["4915"], phase: .inProgress, at: before, timeZone: zone) else { return XCTFail() }
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
        let eventStart = widgetInstant("2027-02-20T00:00:00Z")!
        let startEntry = try XCTUnwrap(plan(event, at: eventStart.addingTimeInterval(-300)).entries.first { $0.date == eventStart })
        guard case .content(let started) = startEntry.content, case .visible(.gap(.nextUnlisted, _)) = started.body else {
            return XCTFail()
        }

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
        XCTAssertEqual(plan(upcoming, at: start.addingTimeInterval(21_601)).refreshAfter, start.addingTimeInterval(28_801))
        let scheduledPlan = plan(event, at: widgetInstant("2026-10-21T15:55:00Z")!)
        let expired = try XCTUnwrap(scheduledPlan.entries.first { $0.date == widgetInstant("2026-10-21T22:00:01Z") })
        guard case .content(let expiredContent) = expired.content, case .visible(.scheduled(let afterExpiry, _)) = expiredContent.body else {
            return XCTFail()
        }
        XCTAssertEqual(afterExpiry.fixture.id, "777925")
        let laterExpiry = try XCTUnwrap(scheduledPlan.entries.first { $0.date == widgetInstant("2026-10-22T00:30:01Z") })
        guard case .content(let laterContent) = laterExpiry.content,
              case .visible(.scheduled(let laterHero, _)) = laterContent.body else { return XCTFail() }
        XCTAssertNotEqual(laterHero.fixture.id, "777925")
        XCTAssertEqual(Set(scheduledPlan.entries.map(\.date)).count, scheduledPlan.entries.count)
        XCTAssertTrue(scheduledPlan.entries.allSatisfy { $0.date <= widgetInstant("2026-10-22T15:55:00Z")! })
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
        let midnight = widgetInstant("2026-10-19T00:00:00Z")!
        let overnight = plan(event, at: widgetInstant("2026-10-18T23:55:00Z")!, hidden: true)
        let boundary = try XCTUnwrap(overnight.entries.first { $0.date == midnight })
        guard case .content(let boundaryContent) = boundary.content,
              case .hidden(.finished(_, let midnightList)) = boundaryContent.body else { return XCTFail() }
        let row = EventWidgetText.row(try XCTUnwrap(midnightList.rows.first), heading: midnightList.heading,
                                     now: midnight, locale: locale, timeZone: zone)
        XCTAssertEqual(row.day, "Yesterday")

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
        XCTAssertEqual(plan(scheduled, at: start).refreshAfter, start.addingTimeInterval(900))
        XCTAssertEqual(plan(scheduled, at: start.addingTimeInterval(-86_400)).refreshAfter, start.addingTimeInterval(-84_600))
        XCTAssertEqual(plan(scheduled, at: start.addingTimeInterval(-86_401)).refreshAfter, start.addingTimeInterval(-79_201))
        let overdue = widgetInstant("2026-10-21T16:01:00Z")!
        XCTAssertEqual(plan(event, at: overdue, teams: ["4915"]).refreshAfter, widgetInstant("2026-10-21T16:16:00Z"))
        let staleDate = widgetInstant("2026-10-21T22:00:01Z")!
        let stale = EventWidgetData(event: event.event, matches: event.matches.filter {
            $0.phase != .upcoming || $0.fixture.id == "777919" || ($0.fixture.start ?? .distantFuture) > staleDate
        }, groups: event.groups, prizes: [], detail: nil)
        XCTAssertEqual(plan(stale, at: staleDate).refreshAfter, widgetInstant("2026-10-21T22:30:01Z"))
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
        XCTAssertEqual(enriched.matches.first { $0.fixture.id == "754737" }?.fixture.format, .bo5)
        XCTAssertEqual(enriched.detail?.maps.prefix(4).map(\.score?.text), ["13–11", "8–13", "13–5", "13–10"])
        XCTAssertEqual(enriched.detail?.maps.prefix(4).map(\.status), [.won, .lost, .won, .won])
        let reversed = WidgetMatchDetailsResponse(veto: details.veto, data: details.data, teams: details.teams.reversed(), event: details.event,
            current_map: details.current_map, total_maps: details.total_maps, map_count: details.map_count)
        XCTAssertEqual(EventWidgetMapper.enrich(event, matchID: "754737", details: reversed).detail, enriched.detail)
    }

    func testSharedMapNamesAreUniqueAndIDsWinOverConflictingNames() throws {
        let json = #"{"total_maps":5,"teams":[{"id":"wrong","name":"Alpha"},{"id":"a","name":"Alpha"}],"data":[{"map":" Split ","teams":[{"id":"wrong","name":"Alpha","score":5},{"id":"a","name":"Alpha","score":13}]}],"veto":[{"action":"remains","map":"split"}],"current_map":{"name":"SPLIT","scores":[5,7]}}"#
        let details = try JSONDecoder().decode(WidgetMatchDetailsResponse.self, from: Data(json.utf8))
        let pair = WidgetMatchPair(left: .team(.init(id: "a", name: "Alpha")), right: .team(.init(id: "b", name: "Beta")))
        let series = WidgetSeriesMapper.make(details: details, participants: pair, perspective: .left)
        XCTAssertEqual(series.maps.count, 1)
        XCTAssertEqual(series.maps[0].name, "Split")
        XCTAssertEqual(series.maps[0].score?.text, "7–5")
        XCTAssertEqual(series.maps[0].status, .live)
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

@available(iOS 17.0, *)
extension EventWidgetTests {
    struct RenderCase {
        let name: String
        let content: EventWidgetEntryContent
        let value: String?
        var lead: String? = nil
        var context: String? = nil
    }

    func renderCases() throws -> [RenderCase] {
        let champions = try data("2766")
        let final = try XCTUnwrap(champions.matches.last)
        let pair = final.fixture.participants
        func fixture(_ id: String, _ offset: TimeInterval?, round: String = "Grand Final", pair: WidgetMatchPair? = nil, format: WidgetSeriesFormat? = .bo5) -> EventWidgetFixture {
            .init(id: id, participants: pair ?? final.fixture.participants, start: offset.map { now.addingTimeInterval($0) },
                  day: WidgetDay("2026-10-20"), round: round, stage: "Playoffs", format: format)
        }
        let next = fixture("next", 21_600, round: "Lower Final")
        let later = [fixture("row-1", 32_400, round: "Lower Round 3"), fixture("row-2", 86_400, round: "Lower Final", pair: .init(left: .undecided, right: pair.right)),
                     fixture("row-3", 172_800, pair: .init(left: .undecided, right: .undecided))]
        let upcoming = EventMatchList(heading: .upNext, rows: later.map { .init(fixture: $0, phase: .upcoming) })
        let today = EventMatchList(heading: .today, rows: [
            .init(fixture: fixture("earlier-1", -14_400, round: "Lower Round 2"), phase: .completed(.init(team: 2, opponent: 1))),
            .init(fixture: fixture("earlier-2", -3_600, round: "Lower Round 2"), phase: .completed(.init(team: 1, opponent: 2))),
            .init(fixture: later[0], phase: .upcoming),
        ])
        let earlier = EventMatchList(heading: .earlier, rows: Array(champions.matches.dropLast().suffix(3).reversed()))
        let neutralToday = EventNeutralList(heading: .today, rows: today.rows.enumerated().map { .init(fixture: $0.element.fixture, phase: $0.offset < 2 ? .ended : .upcoming) })
        let neutralEarlier = EventNeutralList(heading: .earlier, rows: earlier.rows.map { .init(fixture: $0.fixture, phase: .ended) })
        let header = EventWidgetHeader(eventID: "2766", title: "Champions 2026", logo: champions.event.logo,
            subtitle: .stage("Playoffs", location: "Shanghai"), progress: .fraction(0.82), hasLiveMatch: false)
        func visible(_ name: String, _ state: EventVisibleState, _ value: String?, lead: String? = nil, context: String? = nil,
                     live: Bool = false, subtitle: EventWidgetSubtitle? = nil, progress: EventWidgetProgress? = nil) -> RenderCase {
            let header = EventWidgetHeader(eventID: header.eventID, title: header.title, logo: header.logo, subtitle: subtitle ?? header.subtitle,
                                           progress: progress ?? header.progress, hasLiveMatch: live)
            return .init(name: name, content: .content(.init(header: header, body: .visible(state))), value: value, lead: lead, context: context)
        }
        func hidden(_ name: String, _ state: EventHiddenState, _ value: String, live: Bool = false) -> RenderCase {
            let header = EventWidgetHeader(eventID: header.eventID, title: header.title, logo: header.logo,
                subtitle: live ? header.subtitle : .ended(champions.event.dates.endInclusive, location: "Shanghai"), progress: live ? header.progress : .complete, hasLiveMatch: live)
            return .init(name: name, content: .content(.init(header: header, body: .hidden(state))), value: value)
        }
        func maps(_ index: Int?, between: Bool = false, long: Bool = false) -> [WidgetMap] {
            let names = ["Ascent", "Sunset", "Haven", "Lotus", long ? "Extraordinarily Long Map Name" : "Split"]
            return names.enumerated().map { offset, name in
                let live = !between && offset == index
                let completed = offset < (index ?? 0)
                let won = offset % 2 == 0
                return .init(name: name, pick: .decider, status: live ? .live : completed ? (won ? .won : .lost) : .next,
                    score: live ? .init(team: 6, opponent: 4) : completed ? .init(team: won ? 13 : 9, opponent: won ? 9 : 13) : nil)
            }
        }
        let grand = fixture("live", -3_600)
        let live = EventLiveHero(fixture: grand, score: .init(team: 2, opponent: 1), maps: Array(maps(2).prefix(3)), otherLiveCount: 0)
        let lastFixture = fixture("last", -3_600, round: "Lower Round 3")
        let victory = try XCTUnwrap(EventWidgetVictory(match: .init(fixture: lastFixture, phase: .completed(.init(team: 1, opponent: 2)))))
        let between = EventBetweenHero(last: victory, next: fixture("tomorrow", 86_400))
        let finished = try content(plan(champions))
        let unconfirmed = EventFinishedHero.unconfirmed(.init(end: champions.event.dates.endInclusive, prizePool: champions.event.prizePool))
        let empty = EventMatchList(heading: .upNext, rows: [])
        var cases: [RenderCase] = [
            visible("01-first-match", .scheduled(.first(fixture("first", 172_800, round: "Upper Round 1")), .matches(upcoming)), "12:00\u{202F}PM",
                lead: "First match · Thu, Oct 22", context: "LOUD vs Team Vitality · Upper Round 1",
                subtitle: .starts(WidgetDay("2026-10-22"), location: "São Paulo"), progress: .empty),
            visible("01-first-unknown-time", .scheduled(.first(fixture("first-unknown", nil, round: "Upper Round 1")), .matches(upcoming)), "TBD",
                lead: "First match", context: "LOUD vs Team Vitality · Upper Round 1"),
            visible("02-up-next", .scheduled(.next(next), .matches(upcoming)), "6:00\u{202F}PM"),
            visible("03-undecided", .scheduled(.next(fixture("tbd", 21_600, pair: .init(left: pair.left, right: .undecided))), .matches(upcoming)), "6:00\u{202F}PM"),
            visible("04-live-today", .live(live, today), "2–1", lead: "LOUD vs Team Vitality", context: "Grand Final · Bo5", live: true),
            visible("05-two-live", .live(.init(fixture: grand, score: .init(team: 1, opponent: 0), maps: Array(maps(1).prefix(3)), otherLiveCount: 1), today), "1–0", live: true),
            visible("06-result-right-winner", .between(between, upcoming), "2–1", lead: "Team Vitality beat LOUD",
                context: "Lower Round 3 · Next: Tomorrow, 12\u{202F}PM"),
            RenderCase(name: "07-final-standings", content: .content(finished), value: "LOUD"),
            visible("07-winner-fallback", .finished(.champion(victory.winner, final: victory), .matches(earlier)), victory.winner.name),
            visible("07-winner-unavailable", .finished(unconfirmed, .matches(earlier)), "Oct 18", lead: "Event ended",
                context: "Winner not listed · $2,250,000 prize pool"),
            visible("08-not-listed", .gap(.notListed(start: WidgetDay("2027-02-20")), empty), "Feb 20", subtitle: .dates(.init(start: WidgetDay("2027-02-20"), endInclusive: WidgetDay("2027-03-03")), location: "Seoul"), progress: .empty),
            visible("08-active-gap", .gap(.nextUnlisted, earlier), "TBD"),
            visible("09-delayed", .scheduled(.next(fixture("late", -1, round: "Lower Final")), .matches(upcoming)), "Any minute",
                lead: "LOUD vs Team Vitality", context: "Was due 11:59\u{202F}AM · Lower Final"),
            hidden("10-live-hidden-today", .live(.init(fixture: grand), neutralToday), "Live now", live: true),
            hidden("11-finished-hidden-earlier", .finished(.init(end: champions.event.dates.endInclusive, prizePool: champions.event.prizePool), neutralEarlier), "Oct 18"),
            RenderCase(name: "12-choose-event", content: .setup(.chooseEvent), value: nil),
            RenderCase(name: "12-no-favorites", content: .setup(.noFavorites), value: nil),
            RenderCase(name: "12-placeholder", content: .placeholder(eventID: "2766"), value: nil),
            visible("bo5-map2", .live(.init(fixture: grand, score: .init(team: 1, opponent: 0), maps: maps(1), otherLiveCount: 0), earlier), "1–0", live: true),
            visible("bo5-map4", .live(.init(fixture: grand, score: .init(team: 2, opponent: 1), maps: maps(3), otherLiveCount: 0), earlier), "2–1", live: true),
            visible("bo5-map5-decider", .live(.init(fixture: grand, score: .init(team: 2, opponent: 2), maps: maps(4), otherLiveCount: 0), earlier), "2–2", live: true),
            visible("bo5-between-maps", .live(.init(fixture: grand, score: .init(team: 2, opponent: 1), maps: maps(3, between: true), otherLiveCount: 0), earlier), "2–1", live: true),
            visible("live-missing-score-details", .live(.init(fixture: grand, score: nil, maps: [], otherLiveCount: 0), today), "–", live: true),
            visible("tbd-pair", .scheduled(.next(fixture("unknown", nil, pair: .init(left: .undecided, right: .undecided))), .matches(upcoming)), "TBD"),
            visible("long-map-name", .live(.init(fixture: grand, score: .init(team: 2, opponent: 2), maps: maps(4, long: true), otherLiveCount: 0), earlier), "2–2", live: true),
            hidden("hidden-up-next", .scheduled(.next(next), .init(heading: .upNext, rows: later.map { .init(fixture: $0, phase: .upcoming) })), "6:00\u{202F}PM"),
            hidden("hidden-active-gap", .gap(.nextUnlisted, neutralEarlier), "TBD"),
        ]
        let grouped = try content(plan(data("3122"), teams: ["4915"]))
        cases.append(.init(name: "group-table", content: .content(grouped), value: "6:30\u{202F}PM"))
        let activeEvent = EventWidgetMetadata(id: champions.event.id, title: champions.event.title, logo: champions.event.logo,
            location: champions.event.location, prizePool: champions.event.prizePool, dates: champions.event.dates, reportedStatus: .ongoing)
        let stale = EventWidgetData(event: activeEvent, matches: [.init(fixture: grand, phase: .live(nil))], groups: [], prizes: [], detail: nil)
        cases.append(.init(name: "expired-hero", content: .content(try content(plan(stale, fetchedAt: now.addingTimeInterval(-10_801)))), value: "TBD"))
        let longPair = WidgetMatchPair(left: .team(.init(id: nil, name: "Extraordinarily Long Championship Team Name")), right: .team(.init(id: nil, name: "Another Very Long Esports Team Name")))
        cases.append(visible("long-names", .scheduled(.next(fixture("long", 3_600, round: "Extraordinarily Long Lower Bracket Final Round", pair: longPair)), .matches(upcoming)), "1:00\u{202F}PM"))
        return cases
    }

    func testHeroTextAndNeutralRowsHaveLiteralExpectedValues() throws {
        let cases = try renderCases()
        for scenario in cases {
            guard case .content(let content) = scenario.content else { continue }
            let text: EventWidgetHeroText
            switch content.body {
            case .visible(let state): text = .make(state, now: now, locale: locale, timeZone: zone)
            case .hidden(let state): text = .make(state, now: now, locale: locale, timeZone: zone)
            }
            XCTAssertEqual(text.value, scenario.value, scenario.name)
            XCTAssertEqual(scenario.content.widgetURL, VLRWidgetContract.detailsURL(kind: "event", id: content.header.eventID))
            if let lead = scenario.lead {
                XCTAssertEqual(text.day.map { text.lead + " · " + $0 } ?? text.lead, lead, scenario.name)
            }
            if let context = scenario.context {
                XCTAssertEqual(text.beside.map { EventWidgetText.join([$0.round, $0.format]) } ?? text.context, context, scenario.name)
            }
            if scenario.name == "bo5-map5-decider" {
                XCTAssertEqual(WidgetMapPills.visible(text.maps).count, 5)
                XCTAssertEqual(WidgetMapPills.visible(text.maps).map { WidgetMapPills.text($0, compact: true, locale: locale) }, ["13–9", "9–13", "13–9", "9–13", "Split 6–4"])
            }
            if case .hidden(.live(_, let list)) = content.body {
                XCTAssertEqual(text.lead, "LOUD vs Team Vitality")
                XCTAssertEqual(text.context, "Scores hidden · Grand Final")
                XCTAssertEqual(text.maps, [])
                let row = EventWidgetText.row(list.rows[0], heading: list.heading, now: now, locale: locale, timeZone: zone)
                XCTAssertEqual(row.day, "Ended")
                XCTAssertNil(row.leftScore)
                XCTAssertNil(row.rightScore)
                XCTAssertNil(row.loser)
            }
        }
        XCTAssertEqual(EventWidgetText.integer(-3, locale: locale, signed: true), "−3")
        XCTAssertEqual(EventWidgetEntryContent.placeholder(eventID: "2766").widgetURL, VLRWidgetContract.detailsURL(kind: "event", id: "2766"))
        XCTAssertEqual(EventWidgetEntryContent.setup(.chooseEvent).widgetURL, VLRWidgetContract.appURL)
        XCTAssertEqual(EventWidgetEntryContent.placeholder(eventID: nil).widgetURL, VLRWidgetContract.appURL)
    }

    @MainActor
    func testEventWidgetRendersEveryStateInLightDarkAndTint() throws {
        let directory = ProcessInfo.processInfo.environment["VLR_RENDER_DIR"].map { URL(fileURLWithPath: $0).appendingPathComponent("event") }
        if let directory { try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true) }
        for scenario in try renderCases() {
            for (familyName, family, height) in [("medium", WidgetFamily.systemMedium, CGFloat(158)), ("large", .systemLarge, 354)] {
                for mode in ["light", "dark", "tint"] {
                    let view = EventWidgetView(entry: .init(date: now, content: scenario.content), familyOverride: family, tintedOverride: mode == "tint")
                        .frame(width: 338, height: height)
                        .background(mode == "tint" ? Color(red: 0.20, green: 0.30, blue: 0.50) : PrismWidgetPalette.background(for: mode == "dark" ? .dark : .light))
                        .environment(\.colorScheme, mode == "dark" ? .dark : .light).environment(\.locale, locale).environment(\.timeZone, zone)
                    let renderer = ImageRenderer(content: view)
                    renderer.scale = 3
                    let image = try XCTUnwrap(renderer.uiImage)
                    XCTAssertEqual(image.size, CGSize(width: 338, height: height))
                    let filename = "\(familyName)-\(scenario.name)-\(mode).png"
                    if let directory { try XCTUnwrap(image.pngData()).write(to: directory.appendingPathComponent(filename)) }
                    let attachment = XCTAttachment(image: image)
                    attachment.name = filename
                    attachment.lifetime = .keepAlways
                    add(attachment)
                }
            }
        }
    }

    @MainActor
    func testEventWidgetRendersEveryLocaleWithLongNames() throws {
        let scenario = try XCTUnwrap(renderCases().first { $0.name == "long-names" })
        let directory = ProcessInfo.processInfo.environment["VLR_RENDER_DIR"].map { URL(fileURLWithPath: $0).appendingPathComponent("event") }
        if let directory { try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true) }
        for identifier in ["en_US", "de_DE", "es_ES", "fr_FR", "hi_IN", "ko_KR", "pt_BR", "ru_RU", "tr_TR"] {
            for (familyName, family, height) in [("medium", WidgetFamily.systemMedium, CGFloat(158)), ("large", .systemLarge, 354)] {
                let view = EventWidgetView(entry: .init(date: now, content: scenario.content), familyOverride: family)
                    .frame(width: 338, height: height).background(Color.white)
                    .environment(\.colorScheme, .light).environment(\.locale, Locale(identifier: identifier)).environment(\.timeZone, TimeZone(identifier: "Asia/Kolkata")!)
                let renderer = ImageRenderer(content: view)
                renderer.scale = 3
                let image = try XCTUnwrap(renderer.uiImage)
                let filename = "\(familyName)-locale-\(identifier).png"
                if let directory { try XCTUnwrap(image.pngData()).write(to: directory.appendingPathComponent(filename)) }
            }
        }
    }
}

@available(iOS 17.0, *)
final class EventWidgetIntentTests: XCTestCase {
    func testPickerResolvesCurrentDirectFavoritesOnly() async throws {
        let query = FavoriteEventQuery(loadEvents: { [.init(id: "2", name: "Masters"), .init(id: "1", name: "Champions")] })
        let suggested = try await query.suggestedEntities()
        XCTAssertEqual(suggested.map(\.name), ["Champions", "Masters"])
        let resolved = try await query.entities(for: ["gone", "2"])
        XCTAssertEqual(resolved.map(\.id), ["2"])
        let removed = FavoriteEventQuery(loadEvents: { [] })
        let none = try await removed.entities(for: ["2"])
        XCTAssertEqual(none.map(\.id), [])
    }
}
