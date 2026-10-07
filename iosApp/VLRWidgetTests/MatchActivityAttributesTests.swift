import Foundation
import XCTest
import SwiftUI
import CoreText
import Vision

/// Checks Live Activity payloads, score display, and layouts.
final class MatchActivityAttributesTests: XCTestCase {
    func testLogoURLValidationAcceptsKnownHosts() {
        for source in [
            "https://owcdn.net/img/team.png",
            "https://www.vlr.gg/img/team.png",
            "https://owcdn.net:443/img/team.png",
        ] {
            XCTAssertNotNil(MatchActivityLogoCache.allowedRemoteURL(for: source), source)
        }
    }

    func testLogoURLValidationRejectsUntrustedAuthorities() {
        for source in [
            "http://owcdn.net/img/team.png",
            "https://owcdn.net.evil.example/img/team.png",
            "https://evil-owcdn.net/img/team.png",
            "https://www.vlr.gg.evil.example/img/team.png",
            "https://owcdn%2Enet/img/team.png",
            "https://owcdn.net./img/team.png",
            "https://127.0.0.1/img/team.png",
            "https://[::1]/img/team.png",
            "https://user@owcdn.net/img/team.png",
            "https://user:password@www.vlr.gg/img/team.png",
            "https://owcdn.net:444/img/team.png",
            "file:///tmp/team.png",
        ] {
            XCTAssertNil(MatchActivityLogoCache.allowedRemoteURL(for: source), source)
        }
    }

    func testLogoRedirectGuardRejectsUntrustedDestinations() throws {
        let original = try XCTUnwrap(URL(string: "https://owcdn.net/img/team.png"))
        let session = URLSession(configuration: .ephemeral)
        let task = session.dataTask(with: original)
        let response = try XCTUnwrap(HTTPURLResponse(
            url: original, statusCode: 302, httpVersion: nil, headerFields: nil
        ))
        let guardDelegate = MatchActivityLogoCache.RedirectGuard()

        for (destination, allowed) in [
            ("https://www.vlr.gg/img/team.png", true),
            ("https://owcdn.net/img/other.png", true),
            ("https://owcdn.net.evil.example/img/team.png", false),
            ("https://127.0.0.1/img/team.png", false),
            ("http://owcdn.net/img/team.png", false),
            ("https://user@owcdn.net/img/team.png", false),
            ("https://owcdn.net:444/img/team.png", false),
        ] {
            let request = URLRequest(url: try XCTUnwrap(URL(string: destination)))
            var accepted: URLRequest?
            guardDelegate.urlSession(
                session, task: task, willPerformHTTPRedirection: response, newRequest: request
            ) { accepted = $0 }
            XCTAssertEqual(accepted?.url, allowed ? request.url : nil, destination)
        }
        task.cancel()
        session.invalidateAndCancel()
    }

    @available(iOS 16.1, *)
    @MainActor
    func testLiveActivityLayouts() throws {
        let fontURL = try XCTUnwrap(Bundle(for: Self.self).url(forResource: "chakra_petch_regular", withExtension: "ttf"))
        CTFontManagerRegisterFontsForURL(fontURL as CFURL, .process, nil)
        typealias Pause = MatchActivityAttributes.ContentState.Pause
        let scenarios: [(String, Bool, Bool, Pause?, String, String)] = [
            ("live", false, false, nil, "LIVE", "VAL ESPORTS"),
            ("hidden", false, true, nil, "LIVE", "VAL ESPORTS"),
            ("final", true, false, nil, "FINAL", "VAL ESPORTS"),
            ("technical", false, false, .init(kind: .techPause, reason: "Player disconnected"), "TECHNICAL PAUSE", "Player disconnected"),
            ("timeout", false, false, .init(kind: .timeout, reason: "Team timeout"), "TIMEOUT", "Team timeout"),
            ("halftime", false, false, .init(kind: .halftime), "HALFTIME", "VAL ESPORTS"),
            ("paused", false, false, .init(kind: .paused), "PAUSED", "VAL ESPORTS"),
            ("long-reason", false, false, .init(kind: .techPause, reason: "Player disconnected; officials are resolving an equipment issue"), "TECHNICAL PAUSE", "VAL ESPORTS"),
            ("multiline-reason", false, false, .init(kind: .techPause, reason: "Player disconnected\nEquipment issue"), "TECHNICAL PAUSE", "VAL ESPORTS"),
            ("paused-hidden", false, true, .init(kind: .techPause, reason: "Player disconnected"), "TECHNICAL PAUSE", "Player disconnected"),
            ("final-with-pause", true, false, .init(kind: .techPause, reason: "Player disconnected"), "FINAL", "VAL ESPORTS"),
        ]
        for (name, terminal, hidden, pause, status, detail) in scenarios {
            let state = MatchActivityAttributes.ContentState(
                match_id: "3141592653", observed_at: 1790000000, terminal: terminal,
                teams: [
                    .init(name: "TL", img: nil, score: terminal ? 6 : 1, id: "474"),
                    .init(name: "PRX", img: nil, score: terminal ? 5 : 0, id: "624"),
                ],
                current_map: .init(name: "Lotus", scores: terminal ? [6, 5] : [1, 0], number: 3),
                total_maps: 3, map_winners: terminal ? [] : ["474", "624", nil], pause: pause
            )
            for width in [320.0, 370.0] {
                let content = MatchLiveActivityLockScreen(state: state, spoilersHidden: hidden)
                    .frame(width: width)
                    .background(Color.black)
                    .environment(\.colorScheme, .dark)
                    .environment(\.locale, Locale(identifier: "en_US"))
                let renderer = ImageRenderer(content: content)
                renderer.scale = 3
                let image = try XCTUnwrap(renderer.uiImage)
                XCTAssertLessThanOrEqual(image.size.height, 160, name)
                XCTAssertEqual(image.size.width, width, name)
                let request = VNRecognizeTextRequest()
                request.recognitionLevel = .accurate
                request.recognitionLanguages = ["en-US"]
                request.usesLanguageCorrection = false
                try VNImageRequestHandler(cgImage: try XCTUnwrap(image.cgImage)).perform([request])
                let recognized = request.results?.compactMap { $0.topCandidates(1).first?.string }.joined(separator: " ").uppercased() ?? ""
                XCTAssertTrue(recognized.contains(status), "\(name), \(width): \(recognized)")
                XCTAssertTrue(recognized.contains(detail.uppercased()), "\(name), \(width): \(recognized)")
                if detail == "VAL ESPORTS", pause?.reason != nil {
                    XCTAssertFalse(recognized.contains("PLAYER DISCONNECTED"), name)
                }
                let attachment = XCTAttachment(image: image)
                attachment.name = "live-activity-\(name)-\(Int(width))-dark"
                attachment.lifetime = .keepAlways
                add(attachment)
            }
        }
    }

    @available(iOS 16.1, *)
    @MainActor
    func testPauseHeaderAtLargerTextSize() throws {
        let fontURL = try XCTUnwrap(Bundle(for: Self.self).url(forResource: "chakra_petch_regular", withExtension: "ttf"))
        CTFontManagerRegisterFontsForURL(fontURL as CFURL, .process, nil)
        let state = try decodePauseState(pauseJSON: #"{"kind":"tech_pause"}"#)
        let renderer = ImageRenderer(content: MatchLiveActivityLockScreen(state: state, spoilersHidden: false)
            .frame(width: 320)
            .background(Color.black)
            .environment(\.colorScheme, .dark)
            .dynamicTypeSize(.xxxLarge))
        renderer.scale = 3
        let image = try XCTUnwrap(renderer.uiImage)
        let request = VNRecognizeTextRequest()
        request.recognitionLevel = .accurate
        request.recognitionLanguages = ["en-US"]
        request.usesLanguageCorrection = false
        try VNImageRequestHandler(cgImage: try XCTUnwrap(image.cgImage)).perform([request])
        let recognized = request.results?.compactMap { $0.topCandidates(1).first?.string }.joined(separator: " ").uppercased() ?? ""
        XCTAssertTrue(recognized.contains("TECHNICAL PAUSE"), recognized)
        XCTAssertTrue(recognized.contains("VAL ESPORTS"), recognized)
        let attachment = XCTAttachment(image: image)
        attachment.name = "live-activity-larger-text"
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    @available(iOS 16.1, *)
    func testDecodesPauseKindsAndOptionalReasons() throws {
        typealias Pause = MatchActivityAttributes.ContentState.Pause
        let cases: [(String, Pause.Kind, String?)] = [
            (#"{"kind":"tech_pause","reason":" Player disconnected "}"#, .techPause, "Player disconnected"),
            (#"{"kind":"timeout","reason":null}"#, .timeout, nil),
            (#"{"kind":"halftime"}"#, .halftime, nil),
            (#"{"kind":"pause","reason":"  "}"#, .paused, nil),
            (#"{"kind":"future_kind","reason":"Broadcast paused"}"#, .paused, "Broadcast paused"),
            (#"{"kind":"timeout","reason":42}"#, .timeout, nil),
        ]
        for (json, kind, reason) in cases {
            let state = try decodePauseState(pauseJSON: json)
            XCTAssertEqual(state.pause?.kind, kind, json)
            XCTAssertEqual(state.pause?.reason, reason, json)
            let encoded = try JSONEncoder().encode(state)
            XCTAssertEqual(try JSONDecoder().decode(MatchActivityAttributes.ContentState.self, from: encoded), state)
        }
    }

    @available(iOS 16.1, *)
    func testMalformedPauseDoesNotDiscardMatchAndResumeClearsPause() throws {
        for pauseJSON in [nil, "null", "42", #""paused""#, "[]", "{}", #"{"kind":null}"#, #"{"kind":42}"#] as [String?] {
            let state = try decodePauseState(pauseJSON: pauseJSON)
            XCTAssertNil(state.pause)
            XCTAssertEqual(state.current_map?.scores, [9, 7])
        }
        let paused = try decodePauseState(pauseJSON: #"{"kind":"tech_pause","reason":"Player disconnected"}"#)
        let resumed = try decodePauseState(pauseJSON: nil)
        XCTAssertNotNil(paused.pause)
        XCTAssertNil(resumed.pause)
        XCTAssertEqual(MatchLiveActivityScores(state: paused, hidden: false).primaryLeft,
                       MatchLiveActivityScores(state: resumed, hidden: false).primaryLeft)
        XCTAssertEqual(MatchLiveActivityMapProgress(state: paused, hidden: false)?.maps.map(\.segment),
                       MatchLiveActivityMapProgress(state: resumed, hidden: false)?.maps.map(\.segment))
    }

    @available(iOS 16.1, *)
    private func decodePauseState(pauseJSON: String?) throws -> MatchActivityAttributes.ContentState {
        let pauseField = pauseJSON.map { #", "pause": "# + $0 } ?? ""
        let json = """
        {"match_id":"734308","observed_at":1788789340,"terminal":false,"teams":[{"id":"474","name":"TL","score":1},{"id":"624","name":"PRX","score":1}],"current_map":{"name":"Lotus","number":3,"scores":[9,7]},"total_maps":3,"map_winners":["474","624",null]\(pauseField)}
        """
        return try JSONDecoder().decode(MatchActivityAttributes.ContentState.self, from: Data(json.utf8))
    }

    @available(iOS 16.1, *)
    func testDisplayedScoresFollowMatchPhaseAndSpoilerPreference() {
        func state(terminal: Bool, map: MatchActivityAttributes.ContentState.CurrentMap?) -> MatchActivityAttributes.ContentState {
            .init(match_id: "3141592653", observed_at: 0, terminal: terminal,
                  teams: [.init(name: "Alpha", img: nil, score: 2), .init(name: "Beta", img: nil, score: 1)],
                  current_map: map)
        }
        let map = MatchActivityAttributes.ContentState.CurrentMap(name: "Ascent", scores: [13, 9])
        let live = MatchLiveActivityScores(state: state(terminal: false, map: map), hidden: false)
        XCTAssertEqual(live.primaryLeft, "13")
        XCTAssertEqual(live.primaryRight, "9")
        XCTAssertEqual(live.series, "2 – 1")
        let final = MatchLiveActivityScores(state: state(terminal: true, map: map), hidden: false)
        XCTAssertEqual(final.primaryLeft, "2")
        XCTAssertEqual(final.primaryRight, "1")
        for terminal in [false, true] {
            let hidden = MatchLiveActivityScores(state: state(terminal: terminal, map: map), hidden: true)
            XCTAssertEqual(hidden.primaryLeft, "—")
            XCTAssertEqual(hidden.primaryRight, "—")
            XCTAssertEqual(hidden.series, "— – —")
        }
        XCTAssertEqual(MatchLiveActivityScores(state: state(terminal: false, map: nil), hidden: false).primaryLeft, "2")
        let incomplete = MatchLiveActivityScores(state: state(terminal: false, map: .init(name: "Ascent", scores: [nil])), hidden: false)
        XCTAssertEqual(incomplete.primaryLeft, "—")
        XCTAssertEqual(incomplete.primaryRight, "—")
    }

    @available(iOS 16.1, *)
    func testSyntheticMatchPayloadAdvancesToFinal() throws {
        for tick in 1...6 {
            let json = """
            {"match_id":"3141592653","observed_at":1790000000,"terminal":\(tick == 6),"teams":[{"name":"Test Alpha","tag":"TA","img":null,"score":\(tick)},{"name":"Test Beta","tag":"TB","img":null,"score":\(tick - 1)}],"current_map":{"name":"Test Range","number":2,"scores":[\(tick),\(tick - 1)]},"total_maps":3}
            """
            let state = try JSONDecoder().decode(
                MatchActivityAttributes.ContentState.self,
                from: Data(json.utf8)
            )
            XCTAssertEqual(state.match_id, "3141592653")
            XCTAssertEqual(state.teams.map(\.name), ["Test Alpha", "Test Beta"])
            XCTAssertEqual(state.teams.map(\.visibleName), ["TA", "TB"])
            XCTAssertEqual(state.teams.map(\.logoInitials), ["TA", "TB"])
            XCTAssertEqual(state.teams.map(\.score), [tick, tick - 1])
            XCTAssertEqual(state.current_map?.scores, [tick, tick - 1])
            XCTAssertEqual(state.current_map?.number, 2)
            XCTAssertEqual(state.total_maps, 3)
            XCTAssertEqual(state.terminal, tick == 6)
        }
    }

    @available(iOS 16.1, *)
    func testMapWinnersKeepLegacyTeamIDs() throws {
        let json = #"{"match_id":"734308","observed_at":1788789340,"terminal":false,"teams":[{"id":474,"name":"TL","img":null,"score":1},{"id":"624","name":"PRX","img":null,"score":1}],"current_map":{"name":"Lotus","number":3,"scores":[5,0]},"total_maps":3,"team_0":"474","team_1":"624","map_winners":["474","624",null]}"#
        let state = try JSONDecoder().decode(
            MatchActivityAttributes.ContentState.self,
            from: Data(json.utf8)
        )
        XCTAssertEqual(state.teams.map(\.id), ["474", "624"])
        XCTAssertEqual([state.team_0, state.team_1], ["474", "624"])
        XCTAssertEqual(state.map_winners, ["474", "624", nil])
        XCTAssertEqual(MatchLiveActivityMapProgress(state: state, hidden: false)?.maps.map(\.segment),
                       [.wonBy(0), .wonBy(1), .active])
        XCTAssertEqual(MatchLiveActivityMapProgress(state: state, hidden: true)?.maps.map(\.segment),
                       [.pending, .pending, .pending])
    }

    @available(iOS 16.1, *)
    func testMapProgressHandlesIncompleteAndFinalPayloads() throws {
        func state(terminal: Bool, winners: [String?]) -> MatchActivityAttributes.ContentState {
            .init(match_id: "734308", observed_at: 0, terminal: terminal,
                  teams: [.init(name: "TL", img: nil, score: 1, id: "474"),
                          .init(name: "PRX", img: nil, score: 1, id: "624")],
                  current_map: .init(name: "Lotus", scores: [5, 0], number: 3),
                  total_maps: 3, map_winners: winners)
        }
        XCTAssertEqual(MatchLiveActivityMapProgress(state: state(terminal: false, winners: ["474"]), hidden: false)?.maps.map(\.segment),
                       [.wonBy(0), .pending, .active])
        XCTAssertEqual(MatchLiveActivityMapProgress(state: state(terminal: true, winners: []), hidden: false)?.maps.map(\.segment),
                       [.pending, .pending, .pending])
        XCTAssertEqual(MatchLiveActivityMapProgress(state: state(terminal: true, winners: ["474", "624"]), hidden: false)?.maps.map(\.segment),
                       [.wonBy(0), .wonBy(1), .pending])
    }

    @available(iOS 16.1, *)
    func testTeamTagFallbackAfterDecoding() throws {
        let json = #"[{"name":"Alpha Wolves","img":null,"score":0},{"name":"Beta Squad","tag":null,"img":null,"score":0},{"name":"Gamma Group","tag":"  \t  ","img":null,"score":0},{"name":"Delta Force","tag":" DF ","img":null,"score":0}]"#
        let teams = try JSONDecoder().decode(
            [MatchActivityAttributes.ContentState.Team].self,
            from: Data(json.utf8)
        )

        XCTAssertEqual(teams.map(\.visibleName), ["Alpha Wolves", "Beta Squad", "Gamma Group", "DF"])
        XCTAssertEqual(teams.map(\.logoInitials), ["AW", "BS", "GG", "DF"])
    }

    @available(iOS 16.1, *)
    func testDecodesStageAndRoundWinnersInPayloadTeamOrder() throws {
        let json = #"{"match_id":"734308","observed_at":1788789340,"terminal":false,"stage":"Playoffs: Grand Final","teams":[{"id":"474","name":"TL","score":1},{"id":"624","name":"PRX","score":1}],"current_map":{"name":"Lotus","number":3,"scores":[5,0]},"total_maps":3,"team_0":"474","team_1":"624","map_winners":["624","474",null],"map_rounds":["101","01",""]}"#
        let state = try JSONDecoder().decode(
            MatchActivityAttributes.ContentState.self,
            from: Data(json.utf8)
        )

        XCTAssertEqual(state.stage, "Playoffs: Grand Final")
        XCTAssertEqual(state.map_round_winners, ["101", "01", ""])
        XCTAssertEqual([state.team_0, state.team_1], ["474", "624"])
        XCTAssertEqual(state.map_winners, ["624", "474", nil])
        XCTAssertEqual(MatchLiveActivityMapProgress(state: state, hidden: false)?.maps.map(\.segment),
                       [.wonBy(1), .wonBy(0), .active])
        XCTAssertEqual(
            try JSONDecoder().decode(MatchActivityAttributes.ContentState.self, from: JSONEncoder().encode(state)),
            state
        )
        let encoded = try XCTUnwrap(JSONSerialization.jsonObject(with: JSONEncoder().encode(state)) as? [String: Any])
        XCTAssertEqual(encoded["map_rounds"] as? [String], ["101", "01", ""])
        XCTAssertNil(encoded["map_winner_indexes"])
    }

    @available(iOS 16.1, *)
    func testLegacyBackendPayloadStillDecodesWithoutCompactFields() throws {
        let json = #"{"match_id":"734308","observed_at":1788789340,"terminal":false,"teams":[{"id":"474","name":"TL"},{"id":"624","name":"PRX"}],"current_map":{"name":"Lotus","number":3,"scores":[5,0]},"total_maps":3,"map_winners":["624",474,null],"map_round_winners":[{"map_number":1,"winners":[1,0,1]},{"map_number":2,"winners":[0,null,1]},{"map_number":3,"winners":[]}]}"#
        let state = try JSONDecoder().decode(MatchActivityAttributes.ContentState.self, from: Data(json.utf8))
        XCTAssertEqual([state.team_0, state.team_1], ["474", "624"])
        XCTAssertEqual(state.map_winners, ["624", "474", nil])
        XCTAssertEqual(state.map_round_winners, ["101", "", ""])
        XCTAssertEqual(MatchLiveActivityMapProgress(state: state, hidden: false)?.maps.map(\.segment),
                       [.wonBy(1), .wonBy(0), .active])
    }

    @available(iOS 16.1, *)
    func testEmptyCompactHistoryTakesPriorityOverLegacyHistory() throws {
        let json = #"{"match_id":"734308","observed_at":1788789340,"terminal":false,"teams":[{"id":"474","name":"TL"},{"id":"624","name":"PRX"}],"team_0":"474","team_1":"624","map_winners":["474","624",null],"map_rounds":["","01",""],"map_round_winners":[{"map_number":1,"winners":[1,0,1]}]}"#
        let state = try JSONDecoder().decode(MatchActivityAttributes.ContentState.self, from: Data(json.utf8))
        XCTAssertEqual([state.team_0, state.team_1], ["474", "624"])
        XCTAssertEqual(state.map_winners, ["474", "624", nil])
        XCTAssertEqual(state.map_round_winners, ["", "01", ""])
    }

    @available(iOS 16.1, *)
    func testDecodesNullStageAndEmptyRoundHistory() throws {
        let json = #"{"match_id":"734308","observed_at":1788789340,"terminal":false,"stage":null,"teams":[{"name":"TL"},{"name":"PRX"}],"map_round_winners":[]}"#
        let state = try JSONDecoder().decode(
            MatchActivityAttributes.ContentState.self,
            from: Data(json.utf8)
        )

        XCTAssertNil(state.stage)
        XCTAssertTrue(state.map_round_winners.isEmpty)
    }

    @available(iOS 16.1, *)
    func testRoundHistoryRejectsUnknownCharactersWithoutRemovingPositions() throws {
        let json = #"{"match_id":"734308","observed_at":1788789340,"terminal":false,"teams":[{"name":"TL"},{"name":"PRX"}],"map_rounds":["10?1"]}"#
        XCTAssertThrowsError(try JSONDecoder().decode(MatchActivityAttributes.ContentState.self, from: Data(json.utf8)))
    }

    @available(iOS 16.1, *)
    func testDecodesCompactBackendPayload() throws {
        let attributesJSON = #"{"match_id":"734308"}"#
        let contentStateJSON = #"{"match_id":"734308","observed_at":1788789340,"terminal":false,"teams":[{"name":"FNATIC","img":null,"score":1},{"name":"NRG","img":"https://example.com/nrg.png","score":null}],"current_map":{"name":"Ascent","scores":[12,null]}}"#

        let attributes = try JSONDecoder().decode(
            MatchActivityAttributes.self,
            from: Data(attributesJSON.utf8)
        )
        let state = try JSONDecoder().decode(
            MatchActivityAttributes.ContentState.self,
            from: Data(contentStateJSON.utf8)
        )

        XCTAssertEqual(attributes.match_id, "734308")
        XCTAssertEqual(state.match_id, attributes.match_id)
        XCTAssertEqual(state.observed_at, 1_788_789_340)
        XCTAssertFalse(state.terminal)
        XCTAssertNil(state.teams[0].img)
        XCTAssertNil(state.teams[0].tag)
        XCTAssertEqual(state.teams[0].visibleName, "FNATIC")
        XCTAssertNil(state.teams[1].score)
        XCTAssertEqual(state.current_map?.name, "Ascent")
        XCTAssertNil(state.current_map?.number)
        XCTAssertEqual(state.current_map?.scores, [12, nil])
        XCTAssertNil(state.total_maps)
        XCTAssertNil(state.stage)
        XCTAssertTrue(state.map_round_winners.isEmpty)

        let stateWithoutMap = try JSONDecoder().decode(
            MatchActivityAttributes.ContentState.self,
            from: Data(contentStateJSON.replacingOccurrences(
                of: #"{"name":"Ascent","scores":[12,null]}"#,
                with: "null"
            ).utf8)
        )
        XCTAssertNil(stateWithoutMap.current_map)
    }
}
