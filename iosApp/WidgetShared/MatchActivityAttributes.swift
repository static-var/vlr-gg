import ActivityKit

@available(iOS 16.1, *)
struct MatchActivityAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        struct Team: Codable, Hashable {
            let name: String
            let img: String?
            let score: Int?
        }

        struct CurrentMap: Codable, Hashable {
            let name: String
            let scores: [Int?]
        }

        let match_id: String
        let observed_at: Int
        let terminal: Bool
        let teams: [Team]
        let current_map: CurrentMap?
    }

    let match_id: String
}
