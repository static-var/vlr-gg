import Foundation

struct TeamWidgetRankingResponse: Codable {
    let team: TeamWidgetTeam
    let rank: Int?
    let region: String?
    let region_rank: Int?
    let recent: [Result]

    struct Result: Codable {
        let match_id: String
        let played_on: String
        let opponent: TeamWidgetTeam
        let team_score: Int?
        let opponent_score: Int?
        let won: Bool
    }
}

struct TeamWidgetDetailsResponse: Codable {
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

extension TeamWidgetDetailsResponse {
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

extension TeamWidgetDetailsResponse.Veto {
    init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        team = try values.decodeIfPresent(String.self, forKey: .team)
        action = try values.decodeIfPresent(String.self, forKey: .action) ?? "unknown"
        map = try values.decodeIfPresent(String.self, forKey: .map) ?? ""
    }
}

extension TeamWidgetDetailsResponse.Map {
    init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        map = try values.decodeIfPresent(String.self, forKey: .map) ?? ""
        live = try values.decodeIfPresent(Bool.self, forKey: .live)
        teams = try values.decodeIfPresent([RemoteMatchPreview.Team].self, forKey: .teams) ?? []
    }
}

enum TeamWidgetMapper {
    static func match(for teamID: String, in matches: [RemoteMatchPreview]) -> RemoteMatchPreview? {
        matches.filter { ($0.team1.id == teamID || $0.team2.id == teamID)
            && $0.remoteStatus.widgetStatus != nil }
            .sorted { left, right in
                let leftLive = left.remoteStatus == .live
                let rightLive = right.remoteStatus == .live
                if leftLive != rightLive { return leftLive }
                return (date(left.time) ?? .distantFuture) < (date(right.time) ?? .distantFuture)
            }.first
    }

    static func content(profile: TeamWidgetRankingResponse, match: RemoteMatchPreview?, details: TeamWidgetDetailsResponse?) -> TeamWidgetContent {
        let history = profile.recent.map { row in
            let score = row.team_score.flatMap { first in row.opponent_score.map { TeamWidgetScore(team: first, opponent: $0) } }
            return TeamWidgetRecentResult(id: row.match_id, date: TeamWidgetDay(row.played_on), opponent: row.opponent,
                result: row.won ? .win(score) : .loss(score))
        }
        let results = Array(profile.recent.prefix(5).reversed()).map { $0.won ? TeamWidgetFormSlot.win : .loss }
        var form = Array(repeating: TeamWidgetFormSlot.empty, count: max(0, 5 - results.count)) + results
        let state: TeamWidgetState
        if let match {
            let ownFirst = match.team1.id == profile.team.id
            let opponent = ownFirst ? match.team2 : match.team1
            let own = ownFirst ? match.team1 : match.team2
            let opponentDetails = details?.teams.first { $0.id == opponent.id }
            let format = match.series.replacingOccurrences(of: " ", with: "").lowercased()
            let isFormat = ["bo1", "bo3", "bo5"].contains(format)
            let mapCount = details?.total_maps ?? details?.map_count
            let seriesFormat = mapCount.flatMap { [1, 3, 5].contains($0) ? "Bo\($0)" : nil }
                ?? (isFormat ? "Bo" + format.dropFirst(2) : "")
            let stage = details?.event?.stage.flatMap { $0.isEmpty ? nil : $0 } ?? (isFormat ? "" : match.series)
            let opponentTeam: TeamWidgetTeam?
            if let id = opponent.id, !id.isEmpty,
               opponent.name.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() != "TBD" {
                opponentTeam = TeamWidgetTeam(id: id, name: opponent.name, tag: opponentDetails?.tag ?? opponent.tag, logo: opponentDetails?.img ?? opponent.img)
            } else { opponentTeam = nil }
            let fixture = TeamWidgetFixture(id: match.id, opponent: opponentTeam,
                start: date(match.time), event: match.event, stage: stage, format: seriesFormat)
            if match.remoteStatus == .live {
                form.append(.live)
                let score = own.score.flatMap { first in opponent.score.map { TeamWidgetScore(team: first, opponent: $0) } }
                state = .live(TeamWidgetLiveSeries(fixture: fixture, score: score,
                    maps: maps(details: details, match: match, team: profile.team, ownFirst: ownFirst)))
            } else { state = .upcoming(fixture) }
        } else { state = .idle(history.first) }
        return TeamWidgetContent(header: .init(team: profile.team, rank: profile.rank, region: profile.region, regionRank: profile.region_rank),
            form: form, history: history, state: state)
    }

    static func date(_ value: String?) -> Date? {
        widgetInstant(value)
    }

    private static func maps(details: TeamWidgetDetailsResponse?, match: RemoteMatchPreview, team: TeamWidgetTeam, ownFirst: Bool) -> [TeamWidgetMap] {
        guard let details else { return [] }
        let veto = details.veto.filter { ["pick", "remains"].contains($0.action.lowercased()) }
        var names = details.data.filter { $0.map.lowercased() != "all" }.map(\.map)
        for pick in veto where !names.contains(where: { $0.caseInsensitiveCompare(pick.map) == .orderedSame }) { names.append(pick.map) }
        let current = details.current_map ?? match.current_map
        let previewIndex = ownFirst ? 0 : 1
        let currentTeamIndex = details.current_map == nil ? previewIndex : details.teams.firstIndex {
            $0.id == team.id || $0.name.caseInsensitiveCompare(team.name) == .orderedSame
        } ?? previewIndex
        if let current, !current.name.isEmpty, !names.contains(current.name) { names.append(current.name) }
        return names.map { name in
            let map = details.data.first { $0.map.caseInsensitiveCompare(name) == .orderedSame }
            let pick = veto.first { $0.map.caseInsensitiveCompare(name) == .orderedSame }
            let choice: TeamWidgetMapPick
            if let pick, pick.action.lowercased() == "pick", let picker = pick.team {
                let ownNames = [team.id, team.name, team.tag ?? "", match.team1.id == team.id ? match.team1.tag ?? "" : match.team2.tag ?? ""]
                let isOwn = ownNames.contains { !$0.isEmpty && $0.caseInsensitiveCompare(picker) == .orderedSame }
                choice = isOwn ? .team(team.tag ?? team.name) : .opponent((ownFirst ? match.team2.tag : match.team1.tag) ?? picker)
            } else { choice = .decider }
            let orderedTeams = map?.teams ?? []
            let ownIndex = orderedTeams.firstIndex { $0.id == team.id || $0.name.caseInsensitiveCompare(team.name) == .orderedSame } ?? (ownFirst ? 0 : 1)
            let otherIndex = ownIndex == 0 ? 1 : 0
            var scores: TeamWidgetScore?
            if orderedTeams.indices.contains(ownIndex), orderedTeams.indices.contains(otherIndex),
               let first = orderedTeams[ownIndex].score, let second = orderedTeams[otherIndex].score {
                scores = TeamWidgetScore(team: first, opponent: second)
            }
            let isCurrent = current?.name.caseInsensitiveCompare(name) == .orderedSame
            let live = map?.live == true || (isCurrent && map?.live != false)
            if live, let raw = current?.scores, raw.count == 2, (0...1).contains(currentTeamIndex),
               let first = raw[currentTeamIndex], let second = raw[1 - currentTeamIndex] {
                scores = TeamWidgetScore(team: first, opponent: second)
            }
            let status: TeamWidgetMapStatus
            if live { status = .live }
            else if let scores, scores.team != scores.opponent, max(scores.team, scores.opponent) >= 13 {
                status = scores.team > scores.opponent ? .won : .lost
            } else { status = .next }
            return TeamWidgetMap(name: name, pick: choice, status: status, score: status == .next ? nil : scores)
        }
    }
}
