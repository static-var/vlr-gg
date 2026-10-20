import XCTest
import SwiftUI
import WidgetKit
import Vision

final class TeamWidgetTests: XCTestCase {
    private let now = Date(timeIntervalSince1970: 1_792_152_000)
    private let locale = Locale(identifier: "en_US")
    private let zone = TimeZone(secondsFromGMT: 0)!

    func testCountdownUsesLocalDatesHoursMinutesAndDelayedTextAtBoundaries() {
        let cases: [(TimeInterval?, TeamWidgetCountdown)] = [
            (8_040, .countdown("2h 14m")),
            (3_600, .countdown("1h 00m")),
            (2_700, .countdown("45 min")),
            (1, .countdown("1 min")),
            (86_400, .countdown("24h 00m")),
            (0, .delayed("12:00\u{202F}PM")),
            (nil, .unknown),
        ]
        for (offset, expected) in cases {
            XCTAssertEqual(TeamWidgetCountdown.make(start: offset.map { now.addingTimeInterval($0) }, now: now, locale: locale, timeZone: zone), expected)
        }
        let start = TeamWidgetMapper.date("2026-10-22T20:00:00Z")!
        XCTAssertEqual(TeamWidgetCountdown.make(start: start, now: now, locale: locale, timeZone: zone), .scheduled("Thu, 8\u{202F}PM"))
        XCTAssertEqual(TeamWidgetCountdown.make(start: start, now: now, locale: Locale(identifier: "de_DE"), timeZone: zone), .scheduled("Do, 20 Uhr"))
    }

    func testTimelineHasOneEntryPerMinuteThroughStartUntilRefresh() {
        let dates = TeamWidgetCountdown.timelineDates(start: now.addingTimeInterval(180), now: now, refresh: now.addingTimeInterval(300))
        XCTAssertEqual(dates.map { Int($0.timeIntervalSince(now)) }, [0, 60, 120, 180])
        let starting = TeamWidgetCountdown.timelineDates(start: now.addingTimeInterval(180), now: now, refresh: now.addingTimeInterval(180))
        XCTAssertEqual(starting.map { Int($0.timeIntervalSince(now)) }, [0, 60, 120, 180])
        let crossing = TeamWidgetCountdown.timelineDates(start: now.addingTimeInterval(86_520), now: now, refresh: now.addingTimeInterval(301))
        XCTAssertEqual(crossing.map { Int($0.timeIntervalSince(now)) }, [0, 120, 180, 240, 300])
        let boundary = TeamWidgetCountdown.timelineDates(start: now.addingTimeInterval(86_430), now: now, refresh: now.addingTimeInterval(181))
        XCTAssertEqual(boundary.map { Int($0.timeIntervalSince(now)) }, [0, 30, 90, 150])
        XCTAssertEqual(TeamWidgetCountdown.timelineDates(start: now.addingTimeInterval(86_520), now: now, refresh: now.addingTimeInterval(120)), [now])
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
        XCTAssertEqual(hidden.form, Array(repeating: .neutral, count: 6))
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
        let maps: [TeamWidgetMap] = [
            .init(name: "Ascent", pick: .team("SEN"), status: .won, score: .init(team: 13, opponent: 10)),
            .init(name: "Haven", pick: .opponent("FNC"), status: .next, score: nil),
            .init(name: "Lotus", pick: .decider, status: .next, score: nil),
        ]
        XCTAssertEqual(TeamWidgetView.mediumMaps(maps).map(\.name), ["Ascent", "Haven"])
        let bo5 = [
            maps[0],
            TeamWidgetMap(name: "Icebox", pick: .opponent("FNC"), status: .lost, score: .init(team: 8, opponent: 13)),
            TeamWidgetMap(name: "Split", pick: .team("SEN"), status: .live, score: .init(team: 7, opponent: 5)),
            maps[1], maps[2],
        ]
        XCTAssertEqual(TeamWidgetView.mediumMaps(bo5).map(\.name), ["Ascent", "Icebox", "Split", "Haven"])
    }

    func testUpcomingDelayedTBDAndEmptySeasonHeroCopy() throws {
        let base = try content()
        let fixture = TeamWidgetFixture(id: "100", opponent: nil, start: now.addingTimeInterval(19_800), event: "Masters London", stage: "Upper final", format: "Bo3")
        var value = base
        value.state = .upcoming(fixture)
        let hero = TeamWidgetHero.make(value, now: now, locale: locale, timeZone: zone)
        XCTAssertEqual(hero.lead, "Plays TBD in")
        XCTAssertEqual(hero.value, "5h 30m")
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
        XCTAssertEqual(hero.lead, "Plays FNATIC in")
        XCTAssertEqual(hero.value, "2h 14m")
        XCTAssertEqual(hero.context, "Masters London · Upper final · Bo3")
        XCTAssertEqual(sample.history.map(\.opponent.name), ["Paper Rex", "DRX", "G2 Esports", "FNATIC", "LOUD"])
        XCTAssertEqual(sample.history.map(\.result.score), ["2–1", "2–0", "1–2", "2–1", "2–0"])
    }

    @MainActor
    func testPlaceholderRendersRedactedSampleInBothFamilies() throws {
        let directory = URL(fileURLWithPath: "/tmp/vlr-team-widget-renders")
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        for (name, family, height) in [("medium", WidgetFamily.systemMedium, CGFloat(158)), ("large", .systemLarge, 354)] {
            let view = TeamWidgetView(entry: .init(date: now, content: .placeholder), familyOverride: family)
                .frame(width: 338, height: height)
                .background(PrismWidgetPalette.background(for: .light))
                .environment(\.colorScheme, .light).environment(\.locale, locale).environment(\.timeZone, zone)
            let renderer = ImageRenderer(content: view)
            renderer.scale = 3
            let image = try XCTUnwrap(renderer.uiImage)
            try XCTUnwrap(image.pngData()).write(to: directory.appendingPathComponent("\(name)-placeholder-light.png"))
            let request = VNRecognizeTextRequest()
            request.recognitionLevel = .accurate
            try VNImageRequestHandler(cgImage: try XCTUnwrap(image.cgImage)).perform([request])
            let text = request.results?.compactMap { $0.topCandidates(1).first?.string }.joined(separator: " ") ?? ""
            XCTAssertFalse(text.contains("Loading"), text)
            XCTAssertFalse(text.contains("Sentinels"), text)
            XCTAssertFalse(text.contains("FNATIC"), text)
        }
    }

    func testMatchSelectionPrefersLiveThenNextAndIgnoresUnrelatedOrCompleted() throws {
        let decoder = JSONDecoder()
        let matches = try decoder.decode([TeamWidgetMatchResponse].self, from: Data("""
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

    func testUpcomingDetailsMapCountFallbackAndPreviewFormat() throws {
        let profile = try JSONDecoder().decode(TeamWidgetRankingResponse.self, from: Data("""
        {"team":{"id":"2","name":"Sentinels"},"recent":[]}
        """.utf8))
        let match = try JSONDecoder().decode(TeamWidgetMatchResponse.self, from: Data("""
        {"id":"100","event":"Masters","series":"Bo5","status":"upcoming","team1":{"id":"2","name":"Sentinels"},"team2":{"id":"1","name":"FNATIC","img":"https://example.test/preview.png","tag":"FNC"}}
        """.utf8))
        for (counts, expected) in [("\"total_maps\":3,\"map_count\":5", "Bo3"), ("\"map_count\":1", "Bo1"), ("\"total_maps\":2", "Bo5")] {
            let details = try JSONDecoder().decode(TeamWidgetDetailsResponse.self,
                from: Data("{\"veto\":[],\"data\":[],\"teams\":[],\(counts)}".utf8))
            let result = TeamWidgetMapper.content(profile: profile, match: match, details: details, spoilersHidden: false)
            XCTAssertEqual(result.fixture, TeamWidgetFixture(id: "100",
                opponent: .init(id: "1", name: "FNATIC", tag: "FNC", logo: "https://example.test/preview.png"),
                start: nil, event: "Masters", stage: "", format: expected))
        }
    }

    func testCacheSurvivesPublishAndUsesCurrentSpoilerSetting() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let cache = TeamWidgetCache(directory: directory)
        let source = UpcomingMatchesSnapshot(savedAtEpochMillis: 0, clientId: "client", hasFavorites: true,
            favorites: .empty, spoilersHidden: false, matches: [], theme: .preview,
            favoriteTeams: [.init(id: "2", name: "Sentinels")])
        let content = try content(live: true)
        cache.store(content, teamID: "2", source: source)
        XCTAssertEqual(cache.load(teamID: "2", source: source)?.form, [.win, .win, .loss, .win, .win, .live])
        let hidden = UpcomingMatchesSnapshot(savedAtEpochMillis: 0, clientId: "client", hasFavorites: true,
            favorites: .empty, spoilersHidden: true, matches: [], theme: .preview,
            favoriteTeams: source.favoriteTeams)
        XCTAssertEqual(TeamWidgetEntryContent.make(cache.load(teamID: "2", source: hidden), teamID: "2", clientID: "client", source: hidden), .content(content.redacted()))
        let published = UpcomingMatchesSnapshot(savedAtEpochMillis: 123, clientId: "client", hasFavorites: true,
            favorites: .empty, spoilersHidden: false, matches: [], theme: .preview, favoriteTeams: source.favoriteTeams)
        XCTAssertEqual(cache.load(teamID: "2", source: published), content)
        XCTAssertEqual(TeamWidgetEntryContent.make(content, teamID: "removed", clientID: "client", source: published), .needsTeam)
    }

    @MainActor
    func testTeamWidgetRendersRequiredLayoutsInLightDarkAndTint() throws {
        let directory = URL(fileURLWithPath: "/tmp/vlr-team-widget-renders")
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
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
        let scenarios: [(String, TeamWidgetEntryContent, WidgetFamily, CGFloat, String)] = [
            ("medium-upcoming", .content(upcoming), .systemMedium, 158, "2h 14m"),
            ("large-upcoming", .content(upcoming), .systemLarge, 354, "Recent"),
            ("medium-upcoming-monogram", .content(upcoming), .systemMedium, 158, "FNC"),
            ("medium-live", .content(live), .systemMedium, 158, "Ascent"),
            ("medium-live-bo3-after-map1", .content(betweenMaps), .systemMedium, 158, "Haven next"),
            ("medium-needs-team", .needsTeam, .systemMedium, 158, "Edit Widget"),
            ("large-live", .content(live), .systemLarge, 354, "Playing now"),
            ("large-idle", .content(try content()), .systemLarge, 354, "Before that"),
        ]
        for (name, content, family, height, expected) in scenarios {
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
                let path = directory.appendingPathComponent("\(name)-\(mode).png")
                try XCTUnwrap(image.pngData()).write(to: path)
                let attachment = XCTAttachment(image: image)
                attachment.name = path.lastPathComponent
                attachment.lifetime = .keepAlways
                add(attachment)
                let request = VNRecognizeTextRequest()
                request.recognitionLevel = .accurate
                request.usesLanguageCorrection = false
                try VNImageRequestHandler(cgImage: try XCTUnwrap(image.cgImage)).perform([request])
                let text = request.results?.compactMap { $0.topCandidates(1).first?.string }.joined(separator: " ") ?? ""
                XCTAssertTrue(text.localizedCaseInsensitiveContains(expected), "\(name) \(mode): \(text)")
                if name == "medium-upcoming-monogram" {
                    XCTAssertTrue(text.contains("SEN"), text)
                }
                if name == "medium-live-bo3-after-map1" {
                    XCTAssertFalse(text.contains("Lotus"), text)
                    XCTAssertTrue(text.replacingOccurrences(of: "–", with: "-").contains("13-10"), text)
                }
            }
        }
    }

    private func content(live: Bool = false, ownSecond: Bool = false, detailsOppositeOrder: Bool = false) throws -> TeamWidgetContent {
        let profile = TeamWidgetContent.sampleProfile
        guard live else { return TeamWidgetMapper.content(profile: profile, match: nil, details: nil, spoilersHidden: false) }
        let own = TeamWidgetMatchResponse.Team(id: "2", name: "Sentinels", img: nil, score: 1, tag: "SEN")
        let opponent = TeamWidgetMatchResponse.Team(id: "1", name: "FNATIC", img: nil, score: 1, tag: "FNC")
        let match = TeamWidgetMatchResponse(id: "100", event: "Masters London", series: "Bo3", status: "live", time: nil,
            team1: ownSecond ? opponent : own, team2: ownSecond ? own : opponent,
            current_map: .init(name: "Ascent", number: 3, scores: ownSecond ? [5, 7] : [7, 5]))
        func map(_ name: String, _ scores: [Int], live: Bool = false) -> TeamWidgetDetailsResponse.Map {
            let teams = [TeamWidgetMatchResponse.Team(id: nil, name: "Sentinels", img: nil, score: scores[0], tag: nil),
                TeamWidgetMatchResponse.Team(id: nil, name: "FNATIC", img: nil, score: scores[1], tag: nil)]
            return .init(map: name, live: live, teams: ownSecond ? teams.reversed() : teams)
        }
        let details = TeamWidgetDetailsResponse(veto: [.init(team: "SEN", action: "pick", map: "Haven"), .init(team: "FNC", action: "pick", map: "Lotus"), .init(team: nil, action: "remains", map: "Ascent"), .init(team: nil, action: "remains", map: "Split")],
            data: [map("Haven", [13, 8]), map("Lotus", [9, 13]), map("Ascent", [7, 5], live: true)],
            teams: ownSecond != detailsOppositeOrder ? [opponent, own] : [own, opponent], event: .init(stage: "Upper final"),
            current_map: .init(name: "Ascent", number: 3, scores: ownSecond != detailsOppositeOrder ? [5, 7] : [7, 5]))
        return TeamWidgetMapper.content(profile: profile, match: match, details: details, spoilersHidden: false)
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
