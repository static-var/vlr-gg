import Foundation
import XCTest

final class MatchActivityAttributesTests: XCTestCase {
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
        XCTAssertNil(state.teams[1].score)
        XCTAssertEqual(state.current_map?.name, "Ascent")
        XCTAssertEqual(state.current_map?.scores, [12, nil])

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
