import Foundation

struct WidgetTeam: Codable, Equatable, Sendable {
    let id: String?
    let name: String
    var tag: String? = nil
    var logo: String? = nil
}

struct WidgetScore: Codable, Equatable, Sendable {
    let team: Int
    let opponent: Int
    var text: String { "\(team)–\(opponent)" }
}

enum WidgetMapStatus: String, Codable, Sendable {
    case won, lost, live, next
}

enum WidgetMapPick: Codable, Equatable, Sendable {
    case team(String)
    case opponent(String)
    case decider
}

struct WidgetMap: Codable, Equatable, Sendable {
    let name: String
    let pick: WidgetMapPick
    let status: WidgetMapStatus
    let score: WidgetScore?
}

struct WidgetMatchDetailsResponse: Codable {
    let veto: [Veto]
    let data: [Map]
    let teams: [RemoteMatchPreview.Team]
    let event: Event?
    let current_map: RemoteMatchPreview.CurrentMap?
    var total_maps: Int? = nil
    var map_count: Int? = nil

    struct Event: Codable { let stage: String? }
    struct Veto: Codable { let team: String?; let action: String; let map: String }
    struct Map: Codable {
        let map: String
        let live: Bool?
        let teams: [RemoteMatchPreview.Team]
    }
}

extension WidgetMatchDetailsResponse {
    init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        veto = try values.decodeIfPresent([Veto].self, forKey: .veto) ?? []
        data = try values.decodeIfPresent([Map].self, forKey: .data) ?? []
        teams = try values.decodeIfPresent([RemoteMatchPreview.Team].self, forKey: .teams) ?? []
        event = try values.decodeIfPresent(Event.self, forKey: .event)
        current_map = try values.decodeIfPresent(RemoteMatchPreview.CurrentMap.self, forKey: .current_map)
        total_maps = try values.decodeIfPresent(Int.self, forKey: .total_maps)
        map_count = try values.decodeIfPresent(Int.self, forKey: .map_count)
    }
}

extension WidgetMatchDetailsResponse.Veto {
    init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        team = try values.decodeIfPresent(String.self, forKey: .team)
        action = try values.decodeIfPresent(String.self, forKey: .action) ?? "unknown"
        map = try values.decodeIfPresent(String.self, forKey: .map) ?? ""
    }
}

extension WidgetMatchDetailsResponse.Map {
    init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        map = try values.decodeIfPresent(String.self, forKey: .map) ?? ""
        live = try values.decodeIfPresent(Bool.self, forKey: .live)
        teams = try values.decodeIfPresent([RemoteMatchPreview.Team].self, forKey: .teams) ?? []
    }
}


enum WidgetParticipant: Codable, Equatable, Sendable {
    case team(WidgetTeam)
    case undecided
    var team: WidgetTeam? { if case .team(let team) = self { return team }; return nil }
}

struct WidgetMatchPair: Codable, Equatable, Sendable {
    let left: WidgetParticipant
    let right: WidgetParticipant
    var teams: [WidgetTeam] { [left.team, right.team].compactMap { $0 } }
}

enum WidgetSide: String, Codable, Sendable { case left, right }

enum WidgetSeriesFormat: Int, Codable, Sendable {
    case bo1 = 1, bo3 = 3, bo5 = 5
    var label: String { "Bo\(rawValue)" }
}

struct WidgetSeriesEnrichment: Codable, Equatable, Sendable {
    let format: WidgetSeriesFormat?
    let maps: [WidgetMap]
}

enum WidgetSeriesMapper {
    static func format(details: WidgetMatchDetailsResponse?, preview: String?) -> WidgetSeriesFormat? {
        if let count = details?.total_maps ?? details?.map_count, let format = WidgetSeriesFormat(rawValue: count) {
            return format
        }
        let text = preview?.replacingOccurrences(of: " ", with: "").lowercased() ?? ""
        guard text.hasPrefix("bo"), let count = Int(text.dropFirst(2)) else { return nil }
        return WidgetSeriesFormat(rawValue: count)
    }

    static func make(details: WidgetMatchDetailsResponse?, participants: WidgetMatchPair,
                     perspective: WidgetSide, previewCurrent: RemoteMatchPreview.CurrentMap? = nil,
                     previewFormat: String? = nil, ownAliases: [String] = []) -> WidgetSeriesEnrichment {
        .init(format: format(details: details, preview: previewFormat),
              maps: maps(details: details, participants: participants, perspective: perspective, previewCurrent: previewCurrent, ownAliases: ownAliases))
    }

    private static func maps(details: WidgetMatchDetailsResponse?, participants: WidgetMatchPair,
                             perspective: WidgetSide, previewCurrent: RemoteMatchPreview.CurrentMap?, ownAliases: [String]) -> [WidgetMap] {
        let ownFirst = perspective == .left
        let team = (ownFirst ? participants.left : participants.right).team ?? WidgetTeam(id: nil, name: "TBD")
        let opponent = (ownFirst ? participants.right : participants.left).team

        guard let details else { return [] }
        let veto = details.veto.filter { ["pick", "remains"].contains($0.action.lowercased()) }
        func key(_ value: String) -> String { value.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() }
        func index(in values: [RemoteMatchPreview.Team], fallback: Int) -> Int {
            if let id = team.id, !id.isEmpty, let index = values.firstIndex(where: { $0.id == id }) { return index }
            return values.firstIndex { key($0.name) == key(team.name) } ?? fallback
        }
        var names: [String] = []
        for name in details.data.map(\.map) + veto.map(\.map) {
            guard !key(name).isEmpty, key(name) != "all", !names.contains(where: { key($0) == key(name) }) else { continue }
            names.append(name.trimmingCharacters(in: .whitespacesAndNewlines))
        }
        let current = details.current_map ?? previewCurrent
        let previewIndex = ownFirst ? 0 : 1
        let currentTeamIndex = details.current_map == nil ? previewIndex : index(in: details.teams, fallback: previewIndex)
        if let current, !key(current.name).isEmpty, !names.contains(where: { key($0) == key(current.name) }) {
            names.append(current.name.trimmingCharacters(in: .whitespacesAndNewlines))
        }
        return names.map { name in
            let map = details.data.first { key($0.map) == key(name) }
            let pick = veto.first { key($0.map) == key(name) }
            let choice: WidgetMapPick
            if let pick, pick.action.lowercased() == "pick", let picker = pick.team {
                let ownNames = [team.id, team.name, team.tag].compactMap { $0 } + ownAliases
                let isOwn = ownNames.contains { !$0.isEmpty && $0.caseInsensitiveCompare(picker) == .orderedSame }
                choice = isOwn ? .team(team.tag ?? team.name) : .opponent(opponent?.tag ?? picker)
            } else { choice = .decider }
            let orderedTeams = map?.teams ?? []
            let ownIndex = index(in: orderedTeams, fallback: previewIndex)
            let otherIndex = ownIndex == 0 ? 1 : 0
            var scores: WidgetScore?
            if orderedTeams.indices.contains(ownIndex), orderedTeams.indices.contains(otherIndex),
               let first = orderedTeams[ownIndex].score, let second = orderedTeams[otherIndex].score {
                scores = WidgetScore(team: first, opponent: second)
            }
            let isCurrent = current.map { key($0.name) == key(name) } ?? false
            let live = map?.live == true || (isCurrent && map?.live != false)
            if live, let raw = current?.scores, raw.count == 2, (0...1).contains(currentTeamIndex),
               let first = raw[currentTeamIndex], let second = raw[1 - currentTeamIndex] {
                scores = WidgetScore(team: first, opponent: second)
            }
            let status: WidgetMapStatus
            if live { status = .live }
            else if let scores, scores.team != scores.opponent, max(scores.team, scores.opponent) >= 13 {
                status = scores.team > scores.opponent ? .won : .lost
            } else { status = .next }
            return WidgetMap(name: name, pick: choice, status: status, score: status == .next ? nil : scores)
        }
    }
}
