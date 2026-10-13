import CoreText
import SwiftUI
import Vision
import XCTest

@available(iOS 16.1, *)
final class MatchLiveActivityRoundsTests: XCTestCase {
    typealias State = MatchActivityAttributes.ContentState

    private func state(
        map: Int = 1, scores: [Int?] = [2, 2], total: Int = 3,
        history: [State.MapRounds] = [], winners: [String?] = [],
        pause: State.Pause? = nil, terminal: Bool = false,
        seriesScores: [Int] = [2, 1],
        stage: String? = "Playoffs: Grand Final"
    ) -> State {
        State(match_id: "123", observed_at: 0, terminal: terminal,
              teams: [.init(name: "FNATIC", img: nil, score: seriesScores[0], id: "1"),
                      .init(name: "PRX", img: nil, score: seriesScores[1], id: "2")],
              current_map: .init(name: "Lotus", scores: scores, number: map),
              total_maps: total, map_winners: winners, pause: pause,
              stage: stage, map_round_winners: history)
    }

    func testBaselineAndGrowingRoundsUseIndependentMapCounts() throws {
        let progress = try XCTUnwrap(MatchLiveActivityMapProgress(state: state(
            map: 2, scores: [14, 12],
            history: [.init(map_number: 1, winners: Array(repeating: 0, count: 13)),
                      .init(map_number: 2, winners: Array(repeating: 1, count: 26))],
            winners: ["1", nil, nil]
        ), hidden: false))
        XCTAssertEqual(progress.maps.map { $0.rounds.count }, [13, 26, 12])
        XCTAssertEqual(progress.maps.map(\.scoreText), ["13 : 0", "14 : 12", "—"])
        XCTAssertEqual(progress.maps.map(\.segment), [.wonBy(0), .active, .pending])
        XCTAssertTrue(progress.maps[2].rounds.allSatisfy { $0 == .pending })
    }

    func testHistoryCanLagAuthoritativeCurrentScoreWithoutInventingWinners() throws {
        let progress = try XCTUnwrap(MatchLiveActivityMapProgress(state: state(
            scores: [8, 6], history: [.init(map_number: 1, winners: [1, nil, 0])]
        ), hidden: false))
        XCTAssertEqual(progress.maps[0].scoreText, "8 : 6")
        XCTAssertEqual(progress.maps[0].rounds.count, 14)
        XCTAssertEqual(Array(progress.maps[0].rounds.prefix(3)), [.wonBy(1), .unknown, .wonBy(0)])
        XCTAssertEqual(progress.maps[0].rounds.filter { $0 == .unknown }.count, 12)
    }

    func testIssue449PayloadUsesMapScoresAndRecoversSingleUnknownWinners() throws {
        let value = try issue449State()
        let progress = try XCTUnwrap(MatchLiveActivityMapProgress(state: value, hidden: false))
        XCTAssertEqual(progress.maps.map(\.scoreText), ["13 : 5", "9 : 13", "13 : 1"])
        XCTAssertEqual(progress.maps.map(\.segment), [.wonBy(0), .wonBy(1), .wonBy(0)])
        XCTAssertEqual(progress.maps.map { $0.rounds.count }, [18, 22, 14])
        XCTAssertEqual(progress.maps[0].rounds[7], .wonBy(1))
        XCTAssertEqual(progress.maps[2].rounds.last, .wonBy(0))
        XCTAssertTrue(progress.maps[1].rounds.allSatisfy { $0 == .unknown })
        XCTAssertNil(value.map_round_winners[0].winners[7])
    }

    private func issue449State() throws -> State {
        let json = """
        {"match_id":"754738","observed_at":0,"terminal":false,"total_maps":3,
         "stage":"Playoffs: Lower Round 1",
         "teams":[{"id":"120","name":"100 Thieves","score":2},{"id":"11060","name":"Nongshim RedForce","score":1}],
         "current_map":{"name":"Ascent","number":3,"scores":[13,1]},
         "map_winners":["120","11060","120"],
         "map_round_winners":[
           {"map_number":1,"scores":[13,5],"winners":[0,0,0,1,0,0,1,null,0,0,0,0,1,1,0,0,0,0]},
           {"map_number":2,"scores":[9,13],"winners":[]},
           {"map_number":3,"scores":[13,1],"winners":[0,0,0,0,0,0,0,0,0,0,0,0,1,null]}]}
        """
        return try JSONDecoder().decode(State.self, from: Data(json.utf8))
    }

    func testSingleUnknownRoundUsesCurrentScoresInEitherTeamOrder() throws {
        for (history, winner) in [([0, nil, 0, 1] as [Int?], 1), ([1, nil, 1, 0], 0)] {
            let progress = try XCTUnwrap(MatchLiveActivityMapProgress(state: state(
                scores: [2, 2], history: [.init(map_number: 1, winners: history, scores: [1, 1])]
            ), hidden: false))
            XCTAssertEqual(progress.maps[0].rounds[1], .wonBy(winner))
            XCTAssertEqual(progress.maps[0].scoreText, "2 : 2")
        }
    }

    func testMultipleUnknownRoundsResolveWhenOnlyOneTeamIsShort() throws {
        for (history, scores, winner) in [
            ([0, nil, 0, nil] as [Int?], [2, 2] as [Int?], 1),
            ([nil, 1, nil, 1], [2, 2], 0),
            ([nil, nil, nil], [0, 3], 1),
        ] {
            let progress = try XCTUnwrap(MatchLiveActivityMapProgress(state: state(
                map: 2, history: [.init(map_number: 1, winners: history, scores: scores)]
            ), hidden: false))
            for index in history.indices where history[index] == nil {
                XCTAssertEqual(progress.maps[0].rounds[index], .wonBy(winner))
            }
        }
    }

    func testAmbiguousOrInconsistentRoundHistoryStaysUnknown() throws {
        let cases: [([Int?], [Int?])] = [
            ([0, nil, nil, 1], [2, 2]),
            ([0, nil, 1], [2, 2]),
            ([0, nil, nil, 1], [3, 2]),
            ([0, 0, nil, 0], [2, 2]),
            ([0, nil, 1], [1, nil]),
            ([0, nil, 1], [-1, 4]),
            ([0, nil, 2], [2, 1]),
        ]
        for (history, scores) in cases {
            let progress = try XCTUnwrap(MatchLiveActivityMapProgress(state: state(
                scores: scores, history: [.init(map_number: 1, winners: history)]
            ), hidden: false))
            for index in history.indices where history[index] == nil {
                XCTAssertEqual(progress.maps[0].rounds[index], .unknown, "\(history), \(scores)")
            }
        }
    }

    func testCompletedMapUsesBackendScoreWhenHistoryLagsOrHasNoRounds() throws {
        for history in [Array(repeating: 0, count: 12) + Array(repeating: 1, count: 9), []] as [[Int?]] {
            let progress = try XCTUnwrap(MatchLiveActivityMapProgress(state: state(
                map: 2, history: [.init(map_number: 1, winners: history, scores: [13, 9])],
                winners: ["1", nil, nil]
            ), hidden: false))
            XCTAssertEqual(progress.maps[0].scoreText, "13 : 9")
            XCTAssertEqual(progress.maps[0].rounds.count, 22)
            XCTAssertEqual(progress.maps[0].rounds.last, .unknown)
        }
    }

    func testPartialScoresUseOnlyCompletedHistoryThatMatchesKnownBackendValues() throws {
        let history: [Int?] = Array(repeating: 0, count: 12) + Array(repeating: 1, count: 9) + [0]
        for (scores, expected) in [([13, nil] as [Int?], "13 : 9"), ([14, nil], "14 : —")] {
            let progress = try XCTUnwrap(MatchLiveActivityMapProgress(state: state(
                map: 2, history: [.init(map_number: 1, winners: history, scores: scores)], winners: ["1"]
            ), hidden: false))
            XCTAssertEqual(progress.maps[0].scoreText, expected)
        }
        let lagging = try XCTUnwrap(MatchLiveActivityMapProgress(state: state(
            map: 2, history: [.init(map_number: 1, winners: Array(history.dropLast()))], winners: ["1"]
        ), hidden: false))
        XCTAssertEqual(lagging.maps[0].scoreText, "—")
        let current = try XCTUnwrap(MatchLiveActivityMapProgress(state: state(
            scores: [], history: [.init(map_number: 1, winners: [0, nil, 1], scores: [2, 1])]
        ), hidden: false))
        XCTAssertEqual(current.maps[0].scoreText, "2 : 1")
        XCTAssertEqual(current.maps[0].rounds[1], .wonBy(0))
    }

    func testFiveMapWindowMovesAfterThirdMapAndEndsAtLastPlayedMap() throws {
        for (current, expected) in [(3, [1, 2, 3]), (4, [2, 3, 4]), (5, [3, 4, 5])] {
            let progress = try XCTUnwrap(MatchLiveActivityMapProgress(state: state(map: current, total: 5), hidden: false))
            XCTAssertEqual(progress.visibleMaps.map(\.number), expected)
        }
        let final = try XCTUnwrap(MatchLiveActivityMapProgress(
            state: state(map: 3, total: 5, winners: ["1", "1", "1", nil, nil], terminal: true), hidden: false
        ))
        XCTAssertEqual(final.visibleMaps.map(\.number), [1, 2, 3])
        let extraMap = try XCTUnwrap(MatchLiveActivityMapProgress(state: state(
            map: 4, total: 3, history: [.init(map_number: 4, winners: [0, 1])]
        ), hidden: false))
        XCTAssertEqual(extraMap.visibleMaps.map(\.number), [2, 3, 4])
    }

    func testTerminalMapKeepsBackendScoreWhenHistoryLags() throws {
        let history: [Int?] = Array(repeating: 0, count: 12) + Array(repeating: 1, count: 9)
        for terminal in [false, true] {
            let progress = try XCTUnwrap(MatchLiveActivityMapProgress(state: state(
                scores: [13, 9], history: [.init(map_number: 1, winners: history)],
                winners: ["1", nil, nil], terminal: terminal
            ), hidden: false))
            XCTAssertEqual(progress.maps[0].scoreText, "13 : 9")
            XCTAssertEqual(progress.maps[0].rounds.count, 22)
            XCTAssertEqual(progress.maps[0].rounds.last, .unknown)
            XCTAssertEqual(progress.maps[0].segment, .wonBy(0))
        }
    }

    func testTwoMapSweepOnlyHidesUnplayedMapAfterEndingWithSpoilersVisible() throws {
        let history: [State.MapRounds] = [
            .init(map_number: 1, winners: Array(repeating: 0, count: 13) + Array(repeating: 1, count: 9)),
            .init(map_number: 2, winners: Array(repeating: 0, count: 13) + Array(repeating: 1, count: 6)),
            .init(map_number: 3, winners: []),
        ]
        for (terminal, hidden, expected) in [
            (true, false, [1, 2]),
            (false, false, [1, 2, 3]),
            (true, true, [1, 2, 3]),
        ] {
            let progress = try XCTUnwrap(MatchLiveActivityMapProgress(state: state(
                map: 2, scores: [13, 6], history: history, winners: ["1", "1", nil],
                terminal: terminal, seriesScores: [2, 0]
            ), hidden: hidden))
            XCTAssertEqual(progress.visibleMaps.map(\.number), expected, "terminal: \(terminal), hidden: \(hidden)")
            if terminal && !hidden {
                XCTAssertEqual(progress.visibleMaps.map(\.scoreText), ["13 : 9", "13 : 6"])
                XCTAssertTrue(progress.visibleMaps.allSatisfy { $0.segment == .wonBy(0) })
            }
        }
    }

    func testSpoilersHideTallyWinnersAndRoundCount() throws {
        let progress = try XCTUnwrap(MatchLiveActivityMapProgress(state: state(
            scores: [14, 12], history: [.init(map_number: 1, winners: Array(repeating: 1, count: 25) + [nil], scores: [14, 12])],
            winners: ["2", nil, nil]
        ), hidden: true))
        XCTAssertEqual(progress.maps.map(\.scoreText), ["—", "—", "—"])
        XCTAssertTrue(progress.maps.allSatisfy { map in
            map.segment == .pending && map.rounds.count == 12 && map.rounds.allSatisfy { $0 == .pending }
        })
    }

    func testPauseSuppressesStageAndFinalUsesStage() {
        XCTAssertEqual(MatchLiveActivityDetailContent(state: state()).value, .stage("Playoffs: Grand Final"))
        for kind in [State.Pause.Kind.techPause, .timeout, .halftime, .paused] {
            XCTAssertEqual(MatchLiveActivityDetailContent(state: state(pause: .init(kind: kind))).value, .branding)
            XCTAssertEqual(MatchLiveActivityDetailContent(state: state(pause: .init(kind: kind, reason: "Equipment issue"))).value,
                           .pauseReason("Equipment issue"))
        }
        XCTAssertEqual(MatchLiveActivityDetailContent(state: state(pause: .init(kind: .techPause, reason: "Line one\nLine two"))).value,
                       .branding)
        XCTAssertEqual(MatchLiveActivityDetailContent(state: state(pause: .init(kind: .timeout), terminal: true)).value,
                       .stage("Playoffs: Grand Final"))
        XCTAssertEqual(MatchLiveActivityDetailContent(state: state(stage: "  ")).value, .branding)
    }

    @MainActor
    func testRoundLayoutsStayBelowHeightLimitAndPauseReasonReplacesStage() throws {
        let font = try XCTUnwrap(Bundle(for: Self.self).url(forResource: "chakra_petch_regular", withExtension: "ttf"))
        CTFontManagerRegisterFontsForURL(font as CFURL, .process, nil)
        let completed: [State.MapRounds] = (1...4).map { map in
            let winner = map.isMultiple(of: 2) ? 1 : 0
            return .init(map_number: map, winners: Array(repeating: winner, count: 13) + Array(repeating: 1 - winner, count: 9))
        }
        let fourth = Array(completed.prefix(3)) + [State.MapRounds(map_number: 4, winners: (0..<12).map { $0 % 2 })]
        let fifth = completed + [State.MapRounds(map_number: 5, winners: Array(repeating: 0, count: 14) + Array(repeating: 1, count: 12))]
        let sweep: [State.MapRounds] = [
            .init(map_number: 1, winners: Array(repeating: 0, count: 13) + Array(repeating: 1, count: 9)),
            .init(map_number: 2, winners: Array(repeating: 0, count: 13) + Array(repeating: 1, count: 6)),
            .init(map_number: 3, winners: []),
        ]
        let scenarios: [(String, State, String)] = [
            ("issue-449", try issue449State(), "Playoffs: Lower Round 1"),
            ("ended-sweep", state(map: 2, scores: [13, 6], history: sweep, winners: ["1", "1", nil], terminal: true, seriesScores: [2, 0]), "Playoffs: Grand Final"),
            ("ongoing-sweep", state(map: 2, scores: [13, 6], history: sweep, winners: ["1", "1", nil], seriesScores: [2, 0]), "Playoffs: Grand Final"),
            ("fourth-map", state(map: 4, scores: [6, 6], total: 5, history: fourth, winners: ["1", "2", "1", nil, nil]), "Playoffs: Grand Final"),
            ("fifth-map", state(map: 5, scores: [14, 12], total: 5, history: fifth, winners: ["1", "2", "1", "2", nil]), "Playoffs: Grand Final"),
            ("long-overtime", state(map: 3, scores: [30, 30], history: Array(completed.prefix(2)) + [
                .init(map_number: 3, winners: (0..<60).map { $0 % 2 }),
            ], winners: ["1", "2", nil], seriesScores: [1, 1]), "Playoffs: Grand Final"),
            ("pause", state(pause: .init(kind: .techPause, reason: "Player disconnected")), "Player disconnected"),
            ("long-stage", state(stage: "Playoffs: Grand Final International Championship Decider"), "Playoffs: Grand Final"),
        ]
        for (name, value, detail) in scenarios {
            for width in [320.0, 370.0] {
                let renderer = ImageRenderer(content: MatchLiveActivityLockScreen(state: value, spoilersHidden: false)
                    .frame(width: width).background(Color.black).environment(\.colorScheme, .dark)
                    .environment(\.locale, Locale(identifier: "en_US")))
                renderer.scale = 3
                let image = try XCTUnwrap(renderer.uiImage)
                XCTAssertLessThanOrEqual(image.size.height, 160, "\(name), \(width)")
                let request = VNRecognizeTextRequest()
                request.recognitionLevel = .accurate
                request.recognitionLanguages = ["en-US"]
                request.usesLanguageCorrection = false
                try VNImageRequestHandler(cgImage: try XCTUnwrap(image.cgImage)).perform([request])
                let recognized = request.results?.compactMap { $0.topCandidates(1).first?.string }.joined(separator: " ").uppercased() ?? ""
                XCTAssertTrue(recognized.contains(detail.uppercased()), recognized)
                XCTAssertFalse(recognized.split(whereSeparator: \.isWhitespace).contains("M1"), recognized)
                if value.pause != nil { XCTAssertFalse(recognized.contains("PLAYOFFS"), recognized) }
                if name == "long-stage" {
                    XCTAssertTrue(recognized.contains("LIVE"), recognized)
                    XCTAssertFalse(recognized.contains("DECIDER"), recognized)
                }
                let attachment = XCTAttachment(image: image)
                attachment.name = "rounds-\(name)-\(Int(width))"
                attachment.lifetime = .keepAlways
                add(attachment)
            }
        }
    }
}
