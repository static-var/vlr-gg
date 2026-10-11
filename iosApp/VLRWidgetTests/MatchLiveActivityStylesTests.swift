import CoreText
import SwiftUI
import Vision
import XCTest

@available(iOS 16.1, *)
final class MatchLiveActivityStylesTests: XCTestCase {
    typealias State = MatchActivityAttributes.ContentState
    typealias Columns = MatchLiveActivityScoreColumns

    private func state(
        map: Int = 3, scores: [Int?] = [5, 3], total: Int = 3,
        history: [State.MapRounds]? = nil, winners: [String?] = ["1", "2", nil],
        terminal: Bool = false, seriesScores: [Int] = [1, 1],
        tags: [String?] = ["FNC", "PRX"], pause: State.Pause? = nil,
        stage: String? = "Playoffs: Grand Final"
    ) -> State {
        State(match_id: "123", observed_at: 0, terminal: terminal,
              teams: [.init(name: "FNATIC", img: nil, score: seriesScores[0], tag: tags[0], id: "1"),
                      .init(name: "Paper Rex", img: nil, score: seriesScores[1], tag: tags[1], id: "2")],
              current_map: .init(name: "Lotus", scores: scores, number: map),
              total_maps: total, map_winners: winners, pause: pause, stage: stage,
              map_round_winners: history ?? [
                .init(map_number: 1, winners: Array(repeating: 0, count: 13) + Array(repeating: 1, count: 9)),
                .init(map_number: 2, winners: Array(repeating: 1, count: 13) + Array(repeating: 0, count: 9)),
              ])
    }

    func testColumnsListSeriesThenPlayedMapsThenLiveMap() {
        let columns = Columns(state: state(), hidden: false).columns
        XCTAssertEqual(columns.map(\.kind), [.series, .map(1), .map(2), .current(3)])
        XCTAssertEqual(columns.map(\.values), [["1", "1"], ["13", "9"], ["9", "13"], ["5", "3"]])
        XCTAssertEqual(columns.map(\.winner), [nil, 0, 1, nil])
    }

    func testFinishedMatchDropsLiveMapAndEmphasizesSeriesWinner() {
        let columns = Columns(
            state: state(map: 2, scores: [13, 9], total: 3, winners: ["1", "2", nil], terminal: true, seriesScores: [2, 0]),
            hidden: false
        ).columns
        XCTAssertEqual(columns.first?.winner, 0)
        XCTAssertFalse(columns.contains { if case .current = $0.kind { return true } else { return false } })
    }

    func testHiddenSpoilersKeepOnlySeriesAndLiveMapPlaceholders() {
        let columns = Columns(state: state(), hidden: true).columns
        XCTAssertEqual(columns.map(\.kind), [.series, .current(3)])
        XCTAssertTrue(columns.allSatisfy { $0.values == ["—", "—"] && $0.winner == nil })
    }

    func testGlanceShowsEveryMapOfAFullBestOfFive() {
        let history: [State.MapRounds] = (1...5).map { map in
            .init(map_number: map, winners: Array(repeating: map % 2 == 1 ? 0 : 1, count: 13))
        }
        let final = state(map: 5, scores: [13, 0], total: 5, history: history, winners: ["1", "2", "1", "2", "1"],
                          terminal: true, seriesScores: [3, 2])
        XCTAssertEqual(Columns(state: final, hidden: false, limit: 5).columns.map(\.kind),
                       [.series, .map(1), .map(2), .map(3), .map(4), .map(5)])
        let live = state(map: 5, scores: [6, 4], total: 5, history: Array(history.prefix(4)),
                         winners: ["1", "2", "1", "2", nil], seriesScores: [2, 2])
        XCTAssertEqual(Columns(state: live, hidden: false, limit: 5).columns.map(\.kind),
                       [.series, .map(1), .map(2), .map(3), .map(4), .current(5)])
    }

    func testMapFilterAndLimitKeepLatestMaps() {
        let history: [State.MapRounds] = (1...4).map { map in
            .init(map_number: map, winners: Array(repeating: map % 2 == 1 ? 0 : 1, count: 13))
        }
        let value = state(map: 5, scores: [2, 1], total: 5, history: history, winners: ["1", "2", "1", "2", nil], seriesScores: [2, 2])
        XCTAssertEqual(Columns(state: value, hidden: false, limit: 3).columns.map(\.kind), [.series, .map(3), .map(4), .current(5)])
        XCTAssertEqual(Columns(state: value, hidden: false, maps: [4, 5]).columns.map(\.kind), [.series, .map(4), .current(5)])
    }

    func testShortNamePrefersTagThenCachedCodeThenName() {
        XCTAssertEqual(State.Team(name: "Paper Rex", img: nil, score: nil, tag: " PRX ", id: "624").shortName, "PRX")
        XCTAssertEqual(State.Team(name: "Paper Rex", img: nil, score: nil, tag: nil, id: nil).shortName, "Paper Rex")
    }

    func testTeamCodesKeepShortAlphanumericCodesForValidIDs() throws {
        let codes = try MatchActivityTeamCodes.parse(Data("""
        {"2":{"riot_code":"sen"},"14":{"riot_code":"T1"},"0":{"riot_code":"BAD"},
         "x":{"riot_code":"BAD"},"17":{"riot_code":"TOOLONG"},"18":{"riot_code":"A B"},"19":{}}
        """.utf8))
        XCTAssertEqual(codes, ["2": "SEN", "14": "T1"])
    }

    func testMapArtPrefersCdnSplashThenBannerWithLetterSlugs() throws {
        XCTAssertEqual(MatchActivityMapArt.slug(for: "Ascent"), "ascent")
        XCTAssertEqual(MatchActivityMapArt.slug(for: " Ice-box "), "icebox")
        XCTAssertNil(MatchActivityMapArt.slug(for: "../"))
        XCTAssertNotNil(MatchActivityMapArt.allowedRemoteURL(for: "https://files.akhilnarang.dev/cdn/valorant/ascent-banner.jpg"))
        XCTAssertNil(MatchActivityMapArt.allowedRemoteURL(for: "http://files.akhilnarang.dev/cdn/valorant/ascent-banner.jpg"))
        XCTAssertNil(MatchActivityMapArt.allowedRemoteURL(for: "https://files.akhilnarang.dev.evil.example/cdn/valorant/a.jpg"))
        XCTAssertNil(MatchActivityMapArt.allowedRemoteURL(for: "https://files.akhilnarang.dev/other/a.jpg"))
        let manifest = try JSONDecoder().decode(MatchActivityMapArt.Manifest.self, from: Data("""
        {"ascent":{"name":"Ascent","banner":{"url":"https://files.akhilnarang.dev/cdn/valorant/ascent-banner.jpg"}},
         "lotus":{"splash":{"url":"https://files.akhilnarang.dev/cdn/valorant/lotus-splash.jpg"},
                  "banner":{"url":"https://files.akhilnarang.dev/cdn/valorant/lotus-banner.jpg"}},
         "split":{"splash":{"url":"https://example.com/cdn/valorant/split-splash.jpg"},
                  "banner":{"url":"https://files.akhilnarang.dev/cdn/valorant/split-banner.jpg"}},
         "evil":{"banner":{"url":"https://example.com/cdn/valorant/evil.jpg"}},"bind":{"name":"Bind"}}
        """.utf8))
        let artwork = manifest.artwork
        XCTAssertEqual(Set(artwork.keys), ["ascent", "lotus", "split"])
        XCTAssertEqual(artwork["ascent"]?.kind, .banner)
        XCTAssertEqual(artwork["lotus"]?.kind, .splash)
        XCTAssertEqual(artwork["lotus"]?.url.lastPathComponent, "lotus-splash.jpg")
        XCTAssertEqual(artwork["split"]?.kind, .banner)
    }

    func testStyleMirrorReportsOnlyRealLayoutChanges() throws {
        let suite = "MatchLiveActivityStylesTests-\(UUID().uuidString)"
        let app = try XCTUnwrap(UserDefaults(suiteName: suite + "-app"))
        let shared = try XCTUnwrap(UserDefaults(suiteName: suite + "-shared"))
        defer {
            app.removePersistentDomain(forName: suite + "-app")
            shared.removePersistentDomain(forName: suite + "-shared")
        }
        XCTAssertNil(MatchActivityStylePreference.mirror(from: app, to: shared))
        XCTAssertEqual(MatchActivityStylePreference.current(in: shared), .classic)
        app.set("glance", forKey: MatchActivityStylePreference.appKey)
        XCTAssertEqual(MatchActivityStylePreference.mirror(from: app, to: shared), .glance)
        XCTAssertNil(MatchActivityStylePreference.mirror(from: app, to: shared))
        app.set("retired", forKey: MatchActivityStylePreference.appKey)
        XCTAssertEqual(MatchActivityStylePreference.mirror(from: app, to: shared), .classic)
        XCTAssertEqual(MatchActivityStylePreference.current(in: shared), .classic)
    }

    func testLocalRevisionRoundTripsAndServerPayloadsOmitIt() throws {
        let revised = state().withLocalRevision(42)
        XCTAssertNotEqual(revised, state())
        let decoded = try JSONDecoder().decode(State.self, from: JSONEncoder().encode(revised))
        XCTAssertEqual(decoded.local_revision, 42)
        XCTAssertEqual(decoded.withLocalRevision(7).teams, state().teams)
        XCTAssertNil(try JSONDecoder().decode(State.self, from: JSONEncoder().encode(state())).local_revision)
    }

    @MainActor
    func testStackedLayoutsStayBelowHeightLimitAndShowTags() throws {
        let font = try XCTUnwrap(Bundle(for: Self.self).url(forResource: "chakra_petch_regular", withExtension: "ttf"))
        CTFontManagerRegisterFontsForURL(font as CFURL, .process, nil)
        let fiveMaps: [State.MapRounds] = (1...4).map { map in
            .init(map_number: map, winners: Array(repeating: map % 2 == 1 ? 0 : 1, count: 13) + Array(repeating: map % 2, count: 11))
        } + [.init(map_number: 5, winners: (0..<26).map { $0 % 2 })]
        let scenarios: [(String, State)] = [
            ("map-three", state()),
            ("map-one", state(map: 1, scores: [4, 2], history: [.init(map_number: 1, winners: [0, 0, 1, 0, 1, 0])], winners: [nil, nil, nil], seriesScores: [0, 0])),
            ("fifth-overtime", state(map: 5, scores: [13, 13], total: 5, history: fiveMaps, winners: ["1", "2", "1", "2", nil], seriesScores: [2, 2])),
            ("final", state(map: 2, scores: [13, 9], winners: ["1", "1", nil], terminal: true, seriesScores: [2, 0])),
            ("bo5-final", state(map: 5, scores: [14, 12], total: 5, history: fiveMaps, winners: ["1", "2", "1", "2", "1"],
                                terminal: true, seriesScores: [3, 2])),
            ("bo5-untagged", state(map: 5, scores: [13, 13], total: 5, history: fiveMaps, winners: ["1", "2", "1", "2", nil],
                                   seriesScores: [2, 2], tags: [nil, nil])),
            ("pause", state(pause: .init(kind: .techPause, reason: "Player disconnected"))),
            ("untagged", state(tags: [nil, nil])),
        ]
        for (name, value) in scenarios {
            for style in [MatchActivityStyle.glance, .scoreboard] {
                for width in [320.0, 370.0] {
                    let renderer = ImageRenderer(content: MatchLiveActivityStyledLockScreen(state: value, spoilersHidden: false, style: style)
                        .frame(width: width).background(Color.black).environment(\.colorScheme, .dark)
                        .environment(\.locale, Locale(identifier: "en_US")))
                    renderer.scale = 3
                    let image = try XCTUnwrap(renderer.uiImage)
                    XCTAssertLessThanOrEqual(image.size.height, 160, "\(style) \(name), \(width)")
                    let request = VNRecognizeTextRequest()
                    request.recognitionLevel = .accurate
                    request.usesLanguageCorrection = false
                    try VNImageRequestHandler(cgImage: try XCTUnwrap(image.cgImage)).perform([request])
                    let recognized = request.results?.compactMap { $0.topCandidates(1).first?.string }.joined(separator: " ").uppercased() ?? ""
                    let names = name.hasSuffix("untagged") ? ["FNATIC", "PAPER REX"] : ["FNC", "PRX"]
                    for team in names {
                        XCTAssertTrue(recognized.contains(team), "\(style) \(name), \(width): \(recognized)")
                    }
                    XCTAssertTrue(recognized.contains("GRAND FINAL"), "\(style) \(name), \(width): \(recognized)")
                    if value.pause != nil {
                        XCTAssertTrue(recognized.contains("TECHNICAL PAUSE"), "\(style) \(name), \(width): \(recognized)")
                        XCTAssertFalse(recognized.contains("DISCONNECTED"), "\(style) \(name), \(width): \(recognized)")
                    }
                    let attachment = XCTAttachment(image: image)
                    attachment.name = "\(style.rawValue)-\(name)-\(Int(width))"
                    attachment.lifetime = .keepAlways
                    add(attachment)
                }
            }
        }
    }
}
