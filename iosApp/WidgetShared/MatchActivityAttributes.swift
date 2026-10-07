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

        private struct LegacyMapRounds: Decodable {
            let map_number: Int
            let winners: [Int?]
        }

        struct Pause: Codable, Hashable {
            enum Kind: String, Codable {
                case techPause = "tech_pause"
                case timeout
                case halftime
                case paused = "pause"

                init(from decoder: Decoder) throws {
                    let rawValue = try decoder.singleValueContainer().decode(String.self)
                    self = Kind(rawValue: rawValue) ?? .paused
                }
            }

            let kind: Kind
            let reason: String?

            init(kind: Kind, reason: String? = nil) {
                self.kind = kind
                let trimmedReason = reason?.trimmingCharacters(in: .whitespacesAndNewlines)
                self.reason = trimmedReason.flatMap { $0.isEmpty ? nil : $0 }
            }

            private enum CodingKeys: String, CodingKey { case kind, reason }

            init(from decoder: Decoder) throws {
                let container = try decoder.container(keyedBy: CodingKeys.self)
                let kind = try container.decode(Kind.self, forKey: .kind)
                let reason = try? container.decodeIfPresent(String.self, forKey: .reason)
                self.init(kind: kind, reason: reason)
            }
        }

        let match_id: String
        let observed_at: Int
        let terminal: Bool
        let teams: [Team]
        let team_0: String?
        let team_1: String?
        let current_map: CurrentMap?
        let total_maps: Int?
        let map_winners: [String?]
        let pause: Pause?
        let stage: String?
        let map_round_winners: [String]

        init(match_id: String, observed_at: Int, terminal: Bool, teams: [Team], current_map: CurrentMap?, total_maps: Int? = nil, map_winners: [String?] = [], pause: Pause? = nil, stage: String? = nil, map_round_winners: [String] = [], team_0: String? = nil, team_1: String? = nil) {
            self.match_id = match_id
            self.observed_at = observed_at
            self.terminal = terminal
            self.teams = teams
            self.team_0 = team_0
            self.team_1 = team_1
            self.current_map = current_map
            self.total_maps = total_maps
            self.map_winners = map_winners
            self.pause = pause
            self.stage = stage
            self.map_round_winners = map_round_winners
        }

        private enum CodingKeys: String, CodingKey {
            case match_id, observed_at, terminal, teams, team_0, team_1, current_map, total_maps, map_winners, pause, stage
            case map_round_winners = "map_rounds"
        }

        private enum LegacyCodingKeys: String, CodingKey {
            case map_round_winners
        }

        init(from decoder: Decoder) throws {
            let container = try decoder.container(keyedBy: CodingKeys.self)
            match_id = try container.decode(String.self, forKey: .match_id)
            observed_at = try container.decode(Int.self, forKey: .observed_at)
            terminal = try container.decode(Bool.self, forKey: .terminal)
            teams = try container.decode([Team].self, forKey: .teams)
            team_0 = try container.decodeIfPresent(MatchActivityTeamID.self, forKey: .team_0)?.value ?? teams.first?.id
            team_1 = try container.decodeIfPresent(MatchActivityTeamID.self, forKey: .team_1)?.value ?? teams.dropFirst().first?.id
            current_map = try container.decodeIfPresent(CurrentMap.self, forKey: .current_map)
            total_maps = try container.decodeIfPresent(Int.self, forKey: .total_maps)
            let legacy = try decoder.container(keyedBy: LegacyCodingKeys.self)
            map_winners = (try? container.decodeIfPresent([MatchActivityTeamID?].self, forKey: .map_winners))?
                .map { $0?.value } ?? []
            pause = try? container.decodeIfPresent(Pause.self, forKey: .pause)
            stage = try container.decodeIfPresent(String.self, forKey: .stage)
            if let rounds = try container.decodeIfPresent([String].self, forKey: .map_round_winners) {
                map_round_winners = rounds
            } else {
                let rounds = try legacy.decodeIfPresent([LegacyMapRounds].self, forKey: .map_round_winners) ?? []
                let slots = rounds.filter { (1...9).contains($0.map_number) }
                map_round_winners = (0..<(slots.map(\.map_number).max() ?? 0)).map { index in
                    let winners = slots.first { $0.map_number == index + 1 }?.winners ?? []
                    guard winners.allSatisfy({ $0 == 0 || $0 == 1 }) else { return "" }
                    return winners.map { $0 == 0 ? "0" : "1" }.joined()
                }
            }
            guard map_round_winners.allSatisfy({ $0.allSatisfy { $0 == "0" || $0 == "1" } }) else {
                throw DecodingError.dataCorruptedError(forKey: .map_round_winners, in: container,
                                                      debugDescription: "Round histories must contain only team indexes 0 and 1")
            }
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
