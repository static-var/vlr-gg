import XCTest
import SwiftUI
import WidgetKit

@available(iOS 17.0, *)
final class TeamWidgetTests: XCTestCase {
    private let now = Date(timeIntervalSince1970: 1_792_152_000)
    private let locale = Locale(identifier: "en_US")
    private let zone = TimeZone(secondsFromGMT: 0)!

    func testKickoffShowsStartTimeWithRelativeDay() {
        let cases: [(TimeInterval?, WidgetKickoff)] = [
            (8_040, .scheduled(day: "Today", time: "2:14\u{202F}PM")),
            (1, .scheduled(day: "Today", time: "12:00\u{202F}PM")),
            (86_400, .scheduled(day: "Tomorrow", time: "12:00\u{202F}PM")),
            (0, .delayed("12:00\u{202F}PM")),
            (nil, .unknown),
        ]
        for (offset, expected) in cases {
            XCTAssertEqual(WidgetKickoff.make(start: offset.map { now.addingTimeInterval($0) }, now: now, locale: locale, timeZone: zone), expected)
        }
        let start = TeamWidgetMapper.date("2026-10-22T20:00:00Z")!
        XCTAssertEqual(WidgetKickoff.make(start: start, now: now, locale: locale, timeZone: zone), .scheduled(day: "Thu, Oct 22", time: "8:00\u{202F}PM"))
        XCTAssertEqual(WidgetKickoff.make(start: start, now: now, locale: Locale(identifier: "de_DE"), timeZone: zone), .scheduled(day: "Do. 22. Okt.", time: "20:00"))
        XCTAssertEqual(WidgetKickoff.make(start: now.addingTimeInterval(86_400), now: now, locale: Locale(identifier: "de_DE"), timeZone: zone),
            .scheduled(day: "Morgen", time: "12:00"))
    }

    func testRefreshPolicyForEveryWidgetState() throws {
        let live = try content(live: true)
        let idle = try content()
        func upcoming(_ offset: TimeInterval?) -> TeamWidgetEntryContent {
            var value = idle
            value.state = .upcoming(.init(id: "100", opponent: nil, start: offset.map { now.addingTimeInterval($0) }, event: "", stage: "", format: ""))
            return .content(value)
        }
        let cases: [(TeamWidgetEntryContent, TimeInterval?)] = [
            (.needsTeam(.chooseTeam), nil), (.needsTeam(.noFavorites), nil), (.placeholder, 900), (.content(live), 900), (.content(live.redacted()), 900),
            (upcoming(-1), 300), (upcoming(0), 300), (upcoming(60), 60), (upcoming(86_400), 1_800),
            (upcoming(86_401), 7_200), (upcoming(nil), 7_200), (.content(idle), 7_200), (.content(idle.redacted()), 7_200),
        ]
        for (entry, interval) in cases {
            XCTAssertEqual(TeamWidgetRefreshPolicy.date(for: entry, now: now), interval.map { now.addingTimeInterval($0) })
        }
    }

    func testHeroUsesEachRequestedLocaleIncludingRegionalIdentifiers() {
        let translations = ["en_US": "Any minute", "de_DE": "Jeden Moment", "es_ES": "En cualquier momento",
            "fr_FR": "D’un instant à l’autre", "hi_IN": "किसी भी पल", "ko_KR": "곧 시작", "pt_BR": "A qualquer momento",
            "pt_PT": "A qualquer momento", "ru_RU": "С минуты на минуту", "tr_TR": "Her an"]
        for (identifier, expected) in translations {
            let locale = Locale(identifier: identifier)
            XCTAssertEqual(WidgetKickoff.delayed("").hero(locale: locale), expected, identifier)
        }
    }

    func testTimelineAddsEntriesOnlyWhenTheDayLabelOrStateChanges() {
        func dates(_ start: Date?) -> [Date] { WidgetKickoff.timelineDates(start: start, now: now, timeZone: zone) }
        let midnight = Date(timeIntervalSince1970: 1_792_195_200)
        let today = now.addingTimeInterval(3_600)
        XCTAssertEqual(dates(today), [now, today])
        let tomorrow = now.addingTimeInterval(86_400)
        XCTAssertEqual(dates(tomorrow), [now, midnight, tomorrow])
        let later = now.addingTimeInterval(3 * 86_400)
        XCTAssertEqual(dates(later), [now, midnight.addingTimeInterval(86_400), midnight.addingTimeInterval(2 * 86_400), later])
        XCTAssertEqual(dates(midnight), [now, midnight])
        XCTAssertEqual(dates(nil), [now])
        XCTAssertEqual(dates(now), [now])
    }

    func testMatchTimesParseInstantsWithAndWithoutFractionalSeconds() throws {
        let whole = try XCTUnwrap(TeamWidgetMapper.date("2026-10-22T20:00:00Z"))
        let fractional = try XCTUnwrap(TeamWidgetMapper.date("2026-10-22T20:00:00.125Z"))
        XCTAssertEqual(fractional.timeIntervalSince(whole), 0.125, accuracy: 0.001)
        XCTAssertNil(TeamWidgetMapper.date("2026-10-15"))
    }

    func testResultDayKeepsItsCalendarDateInEveryTimeZone() throws {
        let day = try XCTUnwrap(WidgetDay("2026-10-15"))
        for identifier in ["America/Los_Angeles", "Asia/Tokyo"] {
            XCTAssertEqual(widgetDate(day, locale: locale, timeZone: try XCTUnwrap(TimeZone(identifier: identifier))), "Oct 15")
        }
    }

    func testLogoPrefetchUsesOnlyVisibleTeamsAndCancelsAtDeadline() async throws {
        var value = TeamWidgetContent.sample(at: now)
        let own = WidgetTeam(id: "2", name: "Sentinels", logo: "own")
        value = TeamWidgetContent(header: .init(team: own, rank: nil, region: nil, regionRank: nil), form: [],
            history: value.history.enumerated().map { index, row in
                .init(id: row.id, date: row.date, opponent: .init(id: row.opponent.id, name: row.opponent.name, logo: "history-\(index)"), result: row.result)
            }, state: .upcoming(.init(id: "100", opponent: .init(id: "1", name: "FNATIC", logo: "opponent"), start: nil, event: "", stage: "", format: "")))
        XCTAssertEqual(value.logoSources(large: false), ["own", "opponent"])
        XCTAssertEqual(value.logoSources(large: true), ["own", "opponent", "history-0", "history-1", "history-2", "history-3"])
        await WidgetLogoPrefetch.run(["own"], timeout: .milliseconds(10)) { sources in
            XCTAssertEqual(sources, ["own"])
            do {
                try await Task.sleep(for: .seconds(10))
                XCTFail("Prefetch must be cancelled")
            } catch { XCTAssertTrue(Task.isCancelled) }
        }
    }

    func testMappingKeepsTeamPerspectiveMapPicksAndOldestToNewestForm() throws {
        let content = try content(live: true, ownSecond: true)
        XCTAssertEqual(content.form, [.win, .win, .loss, .win, .win, .live])
        guard case .live(let series) = content.state else { return XCTFail("Expected live series") }
        XCTAssertEqual(series.score?.text, "1–1")
        XCTAssertEqual(series.fixture.opponent?.name, "FNATIC")
        XCTAssertEqual(series.maps.map(\.status), [.won, .lost, .live, .next])
        XCTAssertEqual(series.maps.map(\.score?.text), ["13–8", "9–13", "7–5", nil])
        XCTAssertEqual(series.maps.map(\.pick), [.team("SEN"), .opponent("FNC"), .decider, .decider])
        XCTAssertEqual(content.history.map(\.id), ["1", "2", "3", "4", "5"])
        XCTAssertEqual(content.header.rank, 4)
        let reversedDetails = try self.content(live: true, detailsOppositeOrder: true)
        guard case .live(let reversed) = reversedDetails.state else { return XCTFail("Expected live series") }
        XCTAssertEqual(reversed.maps.map(\.score?.text), ["13–8", "9–13", "7–5", nil])
    }

    func testRedactionRemovesResultsSeriesScoresAndAllMapProgress() throws {
        let hidden = try content(live: true).redacted()
        XCTAssertEqual(hidden.form, Array(repeating: .neutral, count: 5) + [.live])
        XCTAssertEqual(hidden.historyRows.map(\.id), ["1", "2", "3", "4"])
        XCTAssertEqual(hidden.historyRows.map(\.result), Array(repeating: .hidden, count: 4))
        XCTAssertEqual(hidden.history.map(\.result), Array(repeating: .hidden, count: 5))
        guard case .liveHidden(let fixture) = hidden.state else { return XCTFail("Expected hidden live fixture") }
        XCTAssertEqual(fixture.opponent?.name, "FNATIC")
        let hero = TeamWidgetHero.make(hidden, now: now, locale: locale, timeZone: zone)
        XCTAssertEqual(hero.value, "Live now")
        XCTAssertEqual(hero.context, "Scores hidden · Bo3")
        XCTAssertEqual(hero.maps, [])
        XCTAssertFalse(hero.liveLabel)
        XCTAssertTrue(hero.liveContext)
        let idle = try content().redacted()
        XCTAssertEqual(TeamWidgetHero.make(idle, now: now, locale: locale, timeZone: zone).lead, "Last played Paper Rex")
        XCTAssertEqual(TeamWidgetHero.make(idle, now: now, locale: locale, timeZone: zone).value, "Oct 15")
    }

    func testMediumMapsKeepFinishedAndLiveMapsWithOnlyFirstNext() {
        let maps: [WidgetMap] = [
            .init(name: "Ascent", pick: .team("SEN"), status: .won, score: .init(team: 13, opponent: 10)),
            .init(name: "Haven", pick: .opponent("FNC"), status: .next, score: nil),
            .init(name: "Lotus", pick: .decider, status: .next, score: nil),
        ]
        XCTAssertEqual(WidgetMapPills.visible(maps).map(\.name), ["Ascent", "Haven"])
        XCTAssertEqual(WidgetMapPills.text(maps[1], compact: false, locale: locale), "Haven next")
        XCTAssertEqual(WidgetMapPills.text(maps[0], compact: true, locale: locale), "13–10")
        let bo5 = [
            maps[0],
            WidgetMap(name: "Icebox", pick: .opponent("FNC"), status: .lost, score: .init(team: 8, opponent: 13)),
            WidgetMap(name: "Split", pick: .team("SEN"), status: .live, score: .init(team: 7, opponent: 5)),
            maps[1], maps[2],
        ]
        XCTAssertEqual(WidgetMapPills.visible(bo5).map(\.name), ["Ascent", "Icebox", "Split", "Haven"])
    }

    func testUpcomingDelayedTBDAndEmptySeasonHeroCopy() throws {
        let base = try content()
        let fixture = TeamWidgetFixture(id: "100", opponent: nil, start: now.addingTimeInterval(19_800), event: "Masters London", stage: "Upper final", format: "Bo3")
        var value = base
        value.state = .upcoming(fixture)
        let hero = TeamWidgetHero.make(value, now: now, locale: locale, timeZone: zone)
        XCTAssertEqual(hero.lead, "Plays TBD · Today")
        XCTAssertEqual(hero.value, "5:30\u{202F}PM")
        XCTAssertEqual(hero.context, "Masters London · Upper final · Bo3")
        let delayed = TeamWidgetHero.make(value, now: now.addingTimeInterval(20_000), locale: locale, timeZone: zone)
        XCTAssertEqual(delayed.lead, "Plays TBD")
        XCTAssertEqual(delayed.value, "Any minute")
        XCTAssertEqual(delayed.context, "Was due 5:30\u{202F}PM · Masters London")
        value.state = .idle(nil)
        XCTAssertEqual(TeamWidgetHero.make(value, now: now, locale: locale, timeZone: zone).value, "No matches")
    }

    func testGallerySampleMatchesTheUpcomingDesign() {
        let sample = TeamWidgetContent.sample(at: now)
        let hero = TeamWidgetHero.make(sample, now: now, locale: locale, timeZone: zone)
        XCTAssertEqual(sample.header, TeamWidgetHeader(team: .init(id: "2", name: "Sentinels", tag: "SEN"),
            rank: 4, region: "Americas", regionRank: 1))
        XCTAssertEqual(sample.form, [.win, .win, .loss, .win, .win])
        XCTAssertEqual(hero.lead, "Plays FNATIC · Today")
        XCTAssertEqual(hero.value, "2:14\u{202F}PM")
        XCTAssertEqual(hero.context, "Masters London · Upper final · Bo3")
        XCTAssertEqual(sample.history.map(\.opponent.name), ["Paper Rex", "DRX", "G2 Esports", "FNATIC", "LOUD"])
        XCTAssertEqual(sample.history.map(\.result.score), ["2–1", "2–0", "1–2", "2–1", "2–0"])
    }

    @MainActor
    func testPlaceholderRendersRedactedSampleInBothFamilies() throws {
        let directory = try renderDirectory()
        for (name, family, height) in [("medium", WidgetFamily.systemMedium, CGFloat(158)), ("large", .systemLarge, 354)] {
            let view = TeamWidgetView(entry: .init(date: now, content: .placeholder), familyOverride: family)
                .frame(width: 338, height: height)
                .background(PrismWidgetPalette.background(for: .light))
                .environment(\.colorScheme, .light).environment(\.locale, locale).environment(\.timeZone, zone)
            let renderer = ImageRenderer(content: view)
            renderer.scale = 3
            let image = try XCTUnwrap(renderer.uiImage)
            XCTAssertEqual(image.size, CGSize(width: 338, height: height))
            if let directory {
                try XCTUnwrap(image.pngData()).write(to: directory.appendingPathComponent("\(name)-placeholder-light.png"))
            }
        }
    }

    func testMatchSelectionPrefersLiveThenNextAndIgnoresUnrelatedOrCompleted() throws {
        let decoder = JSONDecoder()
        let matches = try decoder.decode([RemoteMatchPreview].self, from: Data("""
        [
          {"id":"other","event":"Masters","series":"Bo3","status":"live","team1":{"id":"9","name":"Other"},"team2":{"id":"8","name":"Other"}},
          {"id":"later","event":"Masters","series":"Bo3","status":"upcoming","time":"2026-10-23T20:00:00Z","team1":{"id":"2","name":"Sentinels"},"team2":{"id":"1","name":"FNATIC"}},
          {"id":"next","event":"Masters","series":"Bo3","status":"upcoming","time":"2026-10-22T20:00:00Z","team1":{"id":"2","name":"Sentinels"},"team2":{"name":"TBD"}},
          {"id":"final","event":"Masters","series":"Bo3","status":"completed","team1":{"id":"2","name":"Sentinels"},"team2":{"id":"1","name":"FNATIC"}},
          {"id":"live","event":"Masters","series":"Bo3","status":"live","team1":{"id":"1","name":"FNATIC"},"team2":{"id":"2","name":"Sentinels"}}
        ]
        """.utf8))
        XCTAssertEqual(TeamWidgetMapper.match(for: "2", in: matches)?.id, "live")
        XCTAssertEqual(TeamWidgetMapper.match(for: "2", in: Array(matches.dropLast()))?.id, "next")
    }

    func testRefreshUsesAuthenticatedFavoritePreviewAndLoadsDetailsForUpcomingAndLiveTeam() async throws {
        let recorder = TeamWidgetRequestRecorder()
        let client = WidgetAPIClient(baseURL: URL(string: "https://example.test")!, authorization: "test-token") { request in
            await recorder.load(request)
        }
        let source = UpcomingMatchesSnapshot(savedAtEpochMillis: 0, clientId: "saved-client", hasFavorites: true,
            favorites: .empty, spoilersHidden: false, matches: [], theme: .preview,
            favoriteTeams: [.init(id: "2", name: "Sentinels")])
        let service = TeamWidgetRefreshService(api: client)
        let upcoming = try await service.refresh(teamID: "2", source: source)
        guard case .upcoming(let fixture) = upcoming.state else { return XCTFail("Expected upcoming fixture") }
        XCTAssertEqual(fixture, TeamWidgetFixture(id: "100",
            opponent: .init(id: "1", name: "FNATIC", tag: "FNC", logo: "https://example.test/fnatic.png"),
            start: nil, event: "Masters", stage: "Upper final", format: "Bo3"))
        let initialPaths = await recorder.paths
        XCTAssertEqual(Set(initialPaths), ["/api/v2/rankings/teams/2", "/api/v1/favorites/saved-client/matches?include_results=false", "/api/v1/matches/100"])
        await recorder.setLive()
        let live = try await service.refresh(teamID: "2", source: source)
        guard case .live(let series) = live.state else { return XCTFail("Expected live series") }
        XCTAssertEqual(series.score?.text, "1–0")
        XCTAssertEqual(series.maps.map(\.status), [.live])
        XCTAssertEqual(series.maps.first?.score?.text, "7–5")
        let paths = await recorder.paths
        XCTAssertEqual(paths.last, "/api/v1/matches/100")
        let headers = await recorder.headers
        XCTAssertEqual(Set(headers), ["test-token|dev.staticvar.vlr"])
        await recorder.failDetails()
        let fallback = try await service.refresh(teamID: "2", source: source)
        guard case .upcoming(let preview) = fallback.state else { return XCTFail("Expected preview fallback") }
        XCTAssertEqual(preview, TeamWidgetFixture(id: "100", opponent: .init(id: "1", name: "FNATIC"),
            start: nil, event: "Masters", stage: "Week 1–Quarterfinals", format: ""))
    }

    func testDetailsDecodeMissingListsAndNestedDefaults() throws {
        let decoder = JSONDecoder()
        let minimal = try decoder.decode(WidgetMatchDetailsResponse.self, from: Data("{\"event\":{\"stage\":\"Final\"},\"total_maps\":3}".utf8))
        XCTAssertTrue(minimal.veto.isEmpty)
        XCTAssertTrue(minimal.data.isEmpty)
        XCTAssertTrue(minimal.teams.isEmpty)
        XCTAssertEqual(minimal.event?.stage, "Final")
        XCTAssertEqual(minimal.total_maps, 3)
        let nested = try decoder.decode(WidgetMatchDetailsResponse.self, from: Data("{\"veto\":[{}],\"data\":[{}]}".utf8))
        XCTAssertEqual(nested.veto.first?.action, "unknown")
        XCTAssertEqual(nested.veto.first?.map, "")
        XCTAssertEqual(nested.data.first?.map, "")
        XCTAssertEqual(nested.data.first?.teams.count, 0)
    }

    func testUpcomingDetailsMapCountFallbackAndPreviewFormat() throws {
        let profile = try JSONDecoder().decode(TeamWidgetRankingResponse.self, from: Data("""
        {"team":{"id":"2","name":"Sentinels"},"recent":[]}
        """.utf8))
        let match = try JSONDecoder().decode(RemoteMatchPreview.self, from: Data("""
        {"id":"100","event":"Masters","series":"Bo5","status":"upcoming","team1":{"id":"2","name":"Sentinels"},"team2":{"id":"1","name":"FNATIC","img":"https://example.test/preview.png","tag":"FNC"}}
        """.utf8))
        for (counts, expected) in [("\"total_maps\":3,\"map_count\":5", "Bo3"), ("\"map_count\":1", "Bo1"), ("\"total_maps\":2", "Bo5")] {
            let details = try JSONDecoder().decode(WidgetMatchDetailsResponse.self,
                from: Data("{\"veto\":[],\"data\":[],\"teams\":[],\(counts)}".utf8))
            let result = TeamWidgetMapper.content(profile: profile, match: match, details: details)
            XCTAssertEqual(result.fixture, TeamWidgetFixture(id: "100",
                opponent: .init(id: "1", name: "FNATIC", tag: "FNC", logo: "https://example.test/preview.png"),
                start: nil, event: "Masters", stage: "", format: expected))
        }
    }

    func testEntrySetupUsesCurrentFavoriteTeamsAndClient() {
        let empty = UpcomingMatchesSnapshot(savedAtEpochMillis: 0, clientId: "client", hasFavorites: false,
            favorites: .empty, spoilersHidden: false, matches: [], theme: .preview, favoriteTeams: [])
        let favorites = UpcomingMatchesSnapshot(savedAtEpochMillis: 0, clientId: "client", hasFavorites: true,
            favorites: .empty, spoilersHidden: false, matches: [], theme: .preview,
            favoriteTeams: [.init(id: "2", name: "Sentinels")])
        let cases: [(UpcomingMatchesSnapshot?, String?, String, TeamWidgetSetup)] = [
            (nil, "2", "client", .noFavorites),
            (empty, "2", "client", .noFavorites),
            (favorites, nil, "client", .chooseTeam),
            (favorites, "1", "client", .chooseTeam),
            (favorites, "2", "other-client", .chooseTeam),
        ]
        for (source, teamID, clientID, expected) in cases {
            XCTAssertEqual(TeamWidgetEntryContent.make(nil, teamID: teamID, clientID: clientID, source: source), .needsTeam(expected))
        }
    }

    func testCachedContentExpiresLiveAndOverdueUpcomingMatches() {
        let fetchedAt = Date(timeIntervalSince1970: 1_792_152_000)
        let fixture = TeamWidgetFixture(id: "100", opponent: nil, start: fetchedAt, event: "Masters", stage: "Final", format: "Bo3")
        let last = TeamWidgetRecentResult(id: "99", date: WidgetDay("2026-10-15"),
            opponent: .init(id: "1", name: "FNATIC"), result: .win(.init(team: 2, opponent: 1)))
        let live = TeamWidgetContent(header: .init(team: .init(id: "2", name: "Sentinels"), rank: nil, region: nil, regionRank: nil),
            form: [.win, .live], history: [last], state: .live(.init(fixture: fixture, score: .init(team: 1, opponent: 0), maps: [])))
        var idle = live
        idle.state = .idle(last)
        idle.form = [.win]
        XCTAssertEqual(live.expired(fetchedAt: fetchedAt, now: fetchedAt.addingTimeInterval(10_800)), live)
        XCTAssertEqual(live.expired(fetchedAt: fetchedAt, now: fetchedAt.addingTimeInterval(10_801)), idle)
        var upcoming = live
        upcoming.state = .upcoming(fixture)
        upcoming.form = [.win]
        XCTAssertEqual(upcoming.expired(fetchedAt: fetchedAt, now: fetchedAt.addingTimeInterval(21_600)), upcoming)
        XCTAssertEqual(upcoming.expired(fetchedAt: fetchedAt, now: fetchedAt.addingTimeInterval(21_601)), idle)
    }

    @MainActor
    func testTeamWidgetRendersRequiredLayoutsInLightDarkAndTint() throws {
        let directory = try renderDirectory()
        let upcoming = TeamWidgetContent.sample(at: now)
        var live = try content(live: true)
        if case .live(let series) = live.state {
            live.state = .live(.init(fixture: series.fixture, score: series.score, maps: Array(series.maps.prefix(3))))
        }
        var betweenMaps = live
        if case .live(let series) = live.state {
            betweenMaps.state = .live(.init(fixture: series.fixture, score: .init(team: 1, opponent: 0), maps: [
                .init(name: "Ascent", pick: .team("SEN"), status: .won, score: .init(team: 13, opponent: 10)),
                .init(name: "Haven", pick: .opponent("FNC"), status: .next, score: nil),
                .init(name: "Lotus", pick: .decider, status: .next, score: nil),
            ]))
        }
        let scenarios: [(String, TeamWidgetEntryContent, WidgetFamily, CGFloat, String?)] = [
            ("medium-upcoming", .content(upcoming), .systemMedium, 158, "2:14\u{202F}PM"),
            ("large-upcoming", .content(upcoming), .systemLarge, 354, "2:14\u{202F}PM"),
            ("medium-upcoming-monogram", .content(upcoming), .systemMedium, 158, "2:14\u{202F}PM"),
            ("medium-live", .content(live), .systemMedium, 158, "1–1"),
            ("medium-live-bo3-after-map1", .content(betweenMaps), .systemMedium, 158, "1–0"),
            ("medium-needs-team", .needsTeam(.chooseTeam), .systemMedium, 158, nil),
            ("large-choose-team", .needsTeam(.chooseTeam), .systemLarge, 354, nil),
            ("medium-no-favorites", .needsTeam(.noFavorites), .systemMedium, 158, nil),
            ("large-no-favorites", .needsTeam(.noFavorites), .systemLarge, 354, nil),
            ("large-live", .content(live), .systemLarge, 354, "1–1"),
            ("large-live-hidden", .content(live.redacted()), .systemLarge, 354, "Live now"),
            ("large-idle", .content(try content()), .systemLarge, 354, "2–1"),
        ]
        for (name, content, family, height, expected) in scenarios {
            if case .content(let model) = content {
                XCTAssertEqual(TeamWidgetHero.make(model, now: now, locale: locale, timeZone: zone).value, expected, name)
            }
            for mode in ["light", "dark", "tint"] {
                let view = TeamWidgetView(entry: .init(date: now, content: content), familyOverride: family, tintedOverride: mode == "tint")
                    .frame(width: 338, height: height)
                    .background(mode == "tint" ? Color(red: 0.20, green: 0.30, blue: 0.50) : PrismWidgetPalette.background(for: mode == "dark" ? .dark : .light))
                    .environment(\.colorScheme, mode == "dark" ? .dark : .light)
                    .environment(\.locale, locale).environment(\.timeZone, zone)
                let renderer = ImageRenderer(content: view)
                renderer.scale = 3
                let image = try XCTUnwrap(renderer.uiImage)
                XCTAssertEqual(image.size, CGSize(width: 338, height: height))
                let filename = "\(name)-\(mode).png"
                if let directory {
                    try XCTUnwrap(image.pngData()).write(to: directory.appendingPathComponent(filename))
                }
                let attachment = XCTAttachment(image: image)
                attachment.name = filename
                attachment.lifetime = .keepAlways
                add(attachment)
            }
        }
    }

    private func renderDirectory() throws -> URL? {
        guard let path = ProcessInfo.processInfo.environment["VLR_RENDER_DIR"], !path.isEmpty else { return nil }
        let directory = URL(fileURLWithPath: path, isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        return directory
    }

    private func content(live: Bool = false, ownSecond: Bool = false, detailsOppositeOrder: Bool = false) throws -> TeamWidgetContent {
        let profile = TeamWidgetContent.sampleProfile
        guard live else { return TeamWidgetMapper.content(profile: profile, match: nil, details: nil) }
        let own = RemoteMatchPreview.Team(id: "2", name: "Sentinels", img: nil, score: 1, tag: "SEN")
        let opponent = RemoteMatchPreview.Team(id: "1", name: "FNATIC", img: nil, score: 1, tag: "FNC")
        let match = RemoteMatchPreview(id: "100", event: "Masters London", series: "Bo3", status: "live", time: nil,
            team1: ownSecond ? opponent : own, team2: ownSecond ? own : opponent,
            current_map: .init(name: "Ascent", number: 3, scores: ownSecond ? [5, 7] : [7, 5]))
        func map(_ name: String, _ scores: [Int], live: Bool = false) -> WidgetMatchDetailsResponse.Map {
            let teams = [RemoteMatchPreview.Team(id: nil, name: "Sentinels", img: nil, score: scores[0], tag: nil),
                RemoteMatchPreview.Team(id: nil, name: "FNATIC", img: nil, score: scores[1], tag: nil)]
            return .init(map: name, live: live, teams: ownSecond ? teams.reversed() : teams)
        }
        let details = WidgetMatchDetailsResponse(veto: [.init(team: "SEN", action: "pick", map: "Haven"), .init(team: "FNC", action: "pick", map: "Lotus"), .init(team: nil, action: "remains", map: "Ascent"), .init(team: nil, action: "remains", map: "Split")],
            data: [map("Haven", [13, 8]), map("Lotus", [9, 13]), map("Ascent", [7, 5], live: true)],
            teams: ownSecond != detailsOppositeOrder ? [opponent, own] : [own, opponent], event: .init(stage: "Upper final"),
            current_map: .init(name: "Ascent", number: 3, scores: ownSecond != detailsOppositeOrder ? [5, 7] : [7, 5]))
        return TeamWidgetMapper.content(profile: profile, match: match, details: details)
    }
}

private actor TeamWidgetRequestRecorder {
    var paths: [String] = []
    var headers: [String] = []
    private var live = false
    private var detailsFail = false

    func setLive() { live = true }
    func failDetails() { live = false; detailsFail = true }

    func load(_ request: URLRequest) -> (Data, HTTPURLResponse) {
        let url = request.url!
        paths.append(url.path + (url.query.map { "?" + $0 } ?? ""))
        headers.append((request.value(forHTTPHeaderField: "Authorization") ?? "") + "|" + (request.value(forHTTPHeaderField: "app-name") ?? ""))
        let json: String
        if url.path.hasPrefix("/api/v2/rankings") {
            json = """
            {"team":{"id":"2","name":"Sentinels","tag":"SEN"},"rank":4,"region":"Americas","region_rank":1,"recent":[]}
            """
        } else if url.path.hasPrefix("/api/v1/favorites") {
            json = """
            [{"id":"100","event":"Masters","series":"Week 1–Quarterfinals","status":"\(live ? "live" : "upcoming")","team1":{"id":"2","name":"Sentinels","score":1},"team2":{"id":"1","name":"FNATIC","score":0}}]
            """
        } else {
            json = """
            {"total_maps":3,"map_count":5,"teams":[{"id":"1","name":"FNATIC","tag":"FNC","img":"https://example.test/fnatic.png"},{"id":"2","name":"Sentinels"}],"veto":[{"team":"SEN","action":"pick","map":"Ascent"}],"data":[{"map":"Ascent","live":true,"teams":[{"name":"Sentinels","score":7},{"name":"FNATIC","score":5}]}],"current_map":{"name":"Ascent","number":1,"scores":[5,7]},"event":{"stage":"Upper final"}}
            """
        }
        return (Data(json.utf8), HTTPURLResponse(url: url, statusCode: detailsFail && url.path == "/api/v1/matches/100" ? 503 : 200, httpVersion: nil, headerFields: nil)!)
    }
}

@available(iOS 17.0, *)
final class TeamWidgetIntentTests: XCTestCase {
    func testPickerListsAndResolvesOnlyCurrentFavoriteTeams() async throws {
        let query = FavoriteTeamQuery(loadTeams: { [.init(id: "2", name: "Sentinels"), .init(id: "1", name: "FNATIC")] })
        let suggested = try await query.suggestedEntities()
        XCTAssertEqual(suggested.map(\.name), ["FNATIC", "Sentinels"])
        let resolved = try await query.entities(for: ["624", "2"])
        XCTAssertEqual(resolved.map(\.id), ["2"])
        let removed = FavoriteTeamQuery(loadTeams: { [.init(id: "1", name: "FNATIC")] })
        let unresolved = try await removed.entities(for: ["2"])
        XCTAssertEqual(unresolved.map(\.id), [])
    }
}
