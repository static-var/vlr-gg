import ActivityKit
import Foundation

@available(iOS 16.1, *)
struct MatchActivityAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        struct Team: Codable, Hashable {
            let name: String
            let img: String?
            let score: Int?
            let tag: String?

            init(name: String, img: String?, score: Int?, tag: String? = nil) {
                self.name = name
                self.img = img
                self.score = score
                self.tag = tag
            }

            var visibleName: String {
                let trimmedTag = tag?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
                return trimmedTag.isEmpty ? name : trimmedTag
            }

            var logoInitials: String {
                let words = visibleName.split(whereSeparator: \.isWhitespace)
                guard !words.isEmpty else { return "—" }
                return words.count > 1
                    ? words.prefix(2).compactMap(\.first).map(String.init).joined().uppercased()
                    : String(words[0].prefix(2)).uppercased()
            }
        }

        struct CurrentMap: Codable, Hashable {
            let name: String
            let scores: [Int?]
            let number: Int?

            init(name: String, scores: [Int?], number: Int? = nil) {
                self.name = name
                self.scores = scores
                self.number = number
            }
        }

        let match_id: String
        let observed_at: Int
        let terminal: Bool
        let teams: [Team]
        let current_map: CurrentMap?
        let total_maps: Int?

        init(match_id: String, observed_at: Int, terminal: Bool, teams: [Team], current_map: CurrentMap?, total_maps: Int? = nil) {
            self.match_id = match_id
            self.observed_at = observed_at
            self.terminal = terminal
            self.teams = teams
            self.current_map = current_map
            self.total_maps = total_maps
        }
    }

    let match_id: String
}
