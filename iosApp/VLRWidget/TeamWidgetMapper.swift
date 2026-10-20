import Foundation

struct TeamWidgetRankingResponse: Codable {
    let team: WidgetTeam
    let rank: Int?
    let region: String?
    let region_rank: Int?
    let recent: [Result]

    struct Result: Codable {
        let match_id: String
        let played_on: String
        let opponent: WidgetTeam
        let team_score: Int?
        let opponent_score: Int?
        let won: Bool
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

    static func content(profile: TeamWidgetRankingResponse, match: RemoteMatchPreview?, details: WidgetMatchDetailsResponse?) -> TeamWidgetContent {
        let history = profile.recent.prefix(5).map { row in
            let score = row.team_score.flatMap { first in row.opponent_score.map { WidgetScore(team: first, opponent: $0) } }
            return TeamWidgetRecentResult(id: row.match_id, date: WidgetDay(row.played_on), opponent: row.opponent,
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
            let seriesFormat = WidgetSeriesMapper.format(details: details, preview: match.series)?.label ?? ""
            let stage = details?.event?.stage.flatMap { $0.isEmpty ? nil : $0 } ?? (isFormat ? "" : match.series)
            let opponentTeam: WidgetTeam?
            if let id = opponent.id, !id.isEmpty,
               opponent.name.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() != "TBD" {
                opponentTeam = WidgetTeam(id: id, name: opponent.name, tag: opponentDetails?.tag ?? opponent.tag, logo: opponentDetails?.img ?? opponent.img)
            } else { opponentTeam = nil }
            let fixture = TeamWidgetFixture(id: match.id, opponent: opponentTeam,
                start: date(match.time), event: match.event, stage: stage, format: seriesFormat)
            if match.remoteStatus == .live {
                form.append(.live)
                let score = own.score.flatMap { first in opponent.score.map { WidgetScore(team: first, opponent: $0) } }
                let seriesOpponent = WidgetTeam(id: opponent.id, name: opponent.name, tag: opponent.tag)
                state = .live(TeamWidgetLiveSeries(fixture: fixture, score: score,
                    maps: WidgetSeriesMapper.make(details: details,
                        participants: .init(left: .team(ownFirst ? profile.team : seriesOpponent),
                                            right: .team(ownFirst ? seriesOpponent : profile.team)),
                        perspective: ownFirst ? .left : .right, previewCurrent: match.current_map,
                        previewFormat: match.series, ownAliases: [own.id, own.name, own.tag].compactMap { $0 }).maps))
            } else { state = .upcoming(fixture) }
        } else { state = .idle(history.first) }
        return TeamWidgetContent(header: .init(team: profile.team, rank: profile.rank, region: profile.region, regionRank: profile.region_rank),
            form: form, history: history, state: state)
    }

    static func date(_ value: String?) -> Date? {
        widgetInstant(value)
    }

}
