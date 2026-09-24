import Foundation
import XCTest
import SwiftUI
import CoreText

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
        let scenarios = [("live", false, false), ("hidden", false, true), ("final", true, false)]
        for (name, terminal, hidden) in scenarios {
          for scheme in [ColorScheme.light, .dark] {
            let state = MatchActivityAttributes.ContentState(
                match_id: "3141592653", observed_at: 1790000000, terminal: terminal,
                teams: [
                    .init(name: "Test Alpha", img: nil, score: terminal ? 6 : 1),
                    .init(name: "Test Beta", img: nil, score: terminal ? 5 : 0),
                ],
                current_map: .init(name: "Test Range", scores: terminal ? [6, 5] : [1, 0])
            )
            let content = MatchLiveActivityLockScreen(state: state, spoilersHidden: hidden)
                .frame(width: 370)
                .background(PrismWidgetPalette.background(for: scheme))
                .environment(\.colorScheme, scheme)
                .environment(\.locale, Locale(identifier: "en_US"))
            let renderer = ImageRenderer(content: content)
            renderer.scale = 3
            let image = try XCTUnwrap(renderer.uiImage)
            XCTAssertLessThanOrEqual(image.size.height, 160)
            let attachment = XCTAttachment(image: image)
            attachment.name = "live-activity-\(name)-\(scheme)"
            attachment.lifetime = .keepAlways
            add(attachment)
          }
        }
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
