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
        stage: String? = "Playoffs: Grand Final"
    ) -> State {
        State(match_id: "123", observed_at: 0, terminal: terminal,
              teams: [.init(name: "FNATIC", img: nil, score: 2, id: "1"),
                      .init(name: "PRX", img: nil, score: 1, id: "2")],
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

    func testSpoilersHideTallyWinnersAndRoundCount() throws {
        let progress = try XCTUnwrap(MatchLiveActivityMapProgress(state: state(
            scores: [14, 12], history: [.init(map_number: 1, winners: Array(repeating: 1, count: 26))],
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
        let scenarios: [(String, State, String)] = [
            ("fourth-map", state(map: 4, scores: [6, 6], total: 5, history: fourth, winners: ["1", "2", "1", nil, nil]), "Playoffs: Grand Final"),
            ("fifth-map", state(map: 5, scores: [14, 12], total: 5, history: fifth, winners: ["1", "2", "1", "2", nil]), "Playoffs: Grand Final"),
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
