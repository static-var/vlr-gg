import ActivityKit
import Foundation

/// Identifies the match tracked by a Live Activity.
@available(iOS 16.1, *)
struct MatchActivityAttributes: ActivityAttributes {
    /// Holds the latest teams, scores, and match status.
    struct ContentState: Codable, Hashable {
        /// Holds a team’s name, logo, tag, and series score.
        struct Team: Codable, Hashable {
            let name: String
            let img: String?
            let score: Int?
            let tag: String?
            let id: String?

            init(name: String, img: String?, score: Int?, tag: String? = nil, id: String? = nil) {
                self.name = name
                self.img = img
                self.score = score
                self.tag = tag
                self.id = id
            }

            private enum CodingKeys: String, CodingKey { case name, img, score, tag, id }

            init(from decoder: Decoder) throws {
                let container = try decoder.container(keyedBy: CodingKeys.self)
                name = try container.decode(String.self, forKey: .name)
                img = try container.decodeIfPresent(String.self, forKey: .img)
                score = try container.decodeIfPresent(Int.self, forKey: .score)
                tag = try container.decodeIfPresent(String.self, forKey: .tag)
                id = try? container.decodeIfPresent(MatchActivityTeamID.self, forKey: .id)?.value
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

        /// Holds the current map’s name, number, and team scores.
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
        let map_winners: [String?]

        init(match_id: String, observed_at: Int, terminal: Bool, teams: [Team], current_map: CurrentMap?, total_maps: Int? = nil, map_winners: [String?] = []) {
            self.match_id = match_id
            self.observed_at = observed_at
            self.terminal = terminal
            self.teams = teams
            self.current_map = current_map
            self.total_maps = total_maps
            self.map_winners = map_winners
        }

        private enum CodingKeys: String, CodingKey {
            case match_id, observed_at, terminal, teams, current_map, total_maps, map_winners
        }

        init(from decoder: Decoder) throws {
            let container = try decoder.container(keyedBy: CodingKeys.self)
            match_id = try container.decode(String.self, forKey: .match_id)
            observed_at = try container.decode(Int.self, forKey: .observed_at)
            terminal = try container.decode(Bool.self, forKey: .terminal)
            teams = try container.decode([Team].self, forKey: .teams)
            current_map = try container.decodeIfPresent(CurrentMap.self, forKey: .current_map)
            total_maps = try container.decodeIfPresent(Int.self, forKey: .total_maps)
            map_winners = (try? container.decodeIfPresent([MatchActivityTeamID?].self, forKey: .map_winners))?
                .map { $0?.value } ?? []
        }
    }

    let match_id: String
}

private struct MatchActivityTeamID: Decodable {
    let value: String?

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        let raw = (try? container.decode(String.self)) ?? (try? container.decode(Int64.self)).map(String.init)
        value = raw.flatMap(Int64.init).flatMap { $0 > 0 ? String($0) : nil }
    }
}
