import Foundation

struct TeamWidgetTeam: Codable, Equatable {
    let id: String
    let name: String
    var tag: String? = nil
    var logo: String? = nil
}

struct TeamWidgetHeader: Codable, Equatable {
    let team: TeamWidgetTeam
    let rank: Int?
    let region: String?
    let regionRank: Int?
}

enum TeamWidgetFormSlot: String, Codable {
    case win, loss, empty, neutral, live
}

struct TeamWidgetScore: Codable, Equatable {
    let team: Int
    let opponent: Int
    var text: String { "\(team)–\(opponent)" }
}

enum TeamWidgetResult: Codable, Equatable {
    case win(TeamWidgetScore?)
    case loss(TeamWidgetScore?)
    case hidden

    var score: String {
        switch self {
        case .win(let score), .loss(let score): score?.text ?? "–"
        case .hidden: ""
        }
    }
}

struct TeamWidgetRecentResult: Codable, Equatable, Identifiable {
    let id: String
    let date: Date?
    let opponent: TeamWidgetTeam
    var result: TeamWidgetResult
}

struct TeamWidgetFixture: Codable, Equatable {
    let id: String
    let opponent: TeamWidgetTeam?
    let start: Date?
    let event: String
    let stage: String
    let format: String

    var context: String { [event, stage, format].filter { !$0.isEmpty }.joined(separator: " · ") }
}

enum TeamWidgetMapStatus: String, Codable {
    case won, lost, live, next
}

enum TeamWidgetMapPick: Codable, Equatable {
    case team(String)
    case opponent(String)
    case decider
}

struct TeamWidgetMap: Codable, Equatable {
    let name: String
    let pick: TeamWidgetMapPick
    let status: TeamWidgetMapStatus
    let score: TeamWidgetScore?
}

struct TeamWidgetLiveSeries: Codable, Equatable {
    let fixture: TeamWidgetFixture
    let score: TeamWidgetScore?
    let maps: [TeamWidgetMap]
}

enum TeamWidgetState: Codable, Equatable {
    case upcoming(TeamWidgetFixture)
    case live(TeamWidgetLiveSeries)
    case liveHidden(TeamWidgetFixture)
    case idle(TeamWidgetRecentResult?)
    case idleHidden(TeamWidgetRecentResult)
}

struct TeamWidgetContent: Codable, Equatable {
    let header: TeamWidgetHeader
    var form: [TeamWidgetFormSlot]
    var history: [TeamWidgetRecentResult]
    var state: TeamWidgetState

    static let sampleProfile = TeamWidgetRankingResponse(
        team: .init(id: "2", name: "Sentinels", tag: "SEN"),
        rank: 4, region: "Americas", region_rank: 1,
        recent: [
            .init(match_id: "1", played_on: "2026-10-15", opponent: .init(id: "624", name: "Paper Rex", tag: "PRX"), team_score: 2, opponent_score: 1, won: true),
            .init(match_id: "2", played_on: "2026-10-12", opponent: .init(id: "2", name: "DRX", tag: "DRX"), team_score: 2, opponent_score: 0, won: true),
            .init(match_id: "3", played_on: "2026-10-09", opponent: .init(id: "3", name: "G2 Esports", tag: "G2"), team_score: 1, opponent_score: 2, won: false),
            .init(match_id: "4", played_on: "2026-10-05", opponent: .init(id: "1", name: "FNATIC", tag: "FNC"), team_score: 2, opponent_score: 1, won: true),
            .init(match_id: "5", played_on: "2026-09-28", opponent: .init(id: "4", name: "LOUD", tag: "LOUD"), team_score: 2, opponent_score: 0, won: true),
        ])

    static func sample(at date: Date) -> Self {
        var content = TeamWidgetMapper.content(profile: sampleProfile, match: nil, details: nil, spoilersHidden: false)
        content.state = .upcoming(.init(id: "100", opponent: .init(id: "1", name: "FNATIC", tag: "FNC"),
            start: date.addingTimeInterval(8_040), event: "Masters London", stage: "Upper final", format: "Bo3"))
        return content
    }

    func redacted() -> Self {
        var content = self
        content.form = form.map { _ in .neutral }
        content.history = history.map { row in
            var row = row
            row.result = .hidden
            return row
        }
        switch state {
        case .live(let series): content.state = .liveHidden(series.fixture)
        case .idle(.some(var last)):
            last.result = .hidden
            content.state = .idleHidden(last)
        default: break
        }
        return content
    }

    var fixture: TeamWidgetFixture? {
        switch state {
        case .upcoming(let fixture), .liveHidden(let fixture): fixture
        case .live(let series): series.fixture
        case .idle, .idleHidden: nil
        }
    }
}

enum TeamWidgetEntryContent: Equatable {
    case needsTeam
    case content(TeamWidgetContent)
    case placeholder
}

enum TeamWidgetCountdown: Equatable {
    case scheduled(String)
    case countdown(String)
    case delayed(String)
    case unknown

    static func make(start: Date?, now: Date, locale: Locale = .current, timeZone: TimeZone = .current) -> Self {
        guard let start else { return .unknown }
        let seconds = start.timeIntervalSince(now)
        if seconds <= 0 {
            return .delayed(format(start, template: "jm", locale: locale, timeZone: timeZone))
        }
        if seconds > 86_400 {
            return .scheduled(format(start, template: "EEEj", locale: locale, timeZone: timeZone))
        }
        let minutes = Int(ceil(seconds / 60))
        if seconds < 3_600 {
            return .countdown(teamWidgetString("%lld min", locale: locale, Int64(minutes)))
        }
        return .countdown(teamWidgetString("%lldh %02lldm", locale: locale, Int64(minutes / 60), Int64(minutes % 60)))
    }

    var hero: String {
        switch self {
        case .scheduled(let text), .countdown(let text): text
        case .delayed: teamWidgetString("Any minute")
        case .unknown: teamWidgetString("TBD")
        }
    }

    static func timelineDates(start: Date?, now: Date, refresh: Date) -> [Date] {
        guard let start, start > now, refresh > now else { return [now] }
        var dates = [now]
        let window = start.addingTimeInterval(-86_400)
        let remainingMinutes = ceil(start.timeIntervalSince(now) / 60)
        let nextCountdownChange = start.addingTimeInterval(-(remainingMinutes - 1) * 60)
        var date = max(nextCountdownChange, window)
        while date < refresh && date <= start {
            dates.append(date)
            date = date.addingTimeInterval(60)
        }
        if start > now, start <= refresh, dates.last != start { dates.append(start) }
        return dates.sorted()
    }

    private static func format(_ date: Date, template: String, locale: Locale, timeZone: TimeZone) -> String {
        let formatter = DateFormatter()
        formatter.locale = locale
        formatter.timeZone = timeZone
        formatter.setLocalizedDateFormatFromTemplate(template)
        if template == "EEEj", locale.languageCode == "en" {
            let weekday = DateFormatter()
            weekday.locale = locale
            weekday.timeZone = timeZone
            weekday.setLocalizedDateFormatFromTemplate("EEE")
            formatter.setLocalizedDateFormatFromTemplate("j")
            return weekday.string(from: date) + ", " + formatter.string(from: date)
        }
        return formatter.string(from: date)
    }
}

func teamWidgetString(_ key: String, locale: Locale = .current, _ arguments: CVarArg...) -> String {
    let language = locale.identifier.replacingOccurrences(of: "_", with: "-")
    let path = Bundle.main.path(forResource: language, ofType: "lproj")
        ?? Bundle.main.path(forResource: String(language.prefix(2)), ofType: "lproj")
    let bundle = path.flatMap(Bundle.init(path:)) ?? .main
    let format = bundle.localizedString(forKey: key, value: key, table: "Localizable")
    return String(format: format, locale: locale, arguments: arguments)
}

func teamWidgetDate(_ date: Date?, locale: Locale = .current, timeZone: TimeZone = .current) -> String {
    guard let date else { return teamWidgetString("TBD", locale: locale) }
    let formatter = DateFormatter()
    formatter.locale = locale
    formatter.timeZone = timeZone
    formatter.setLocalizedDateFormatFromTemplate("MMMd")
    return formatter.string(from: date)
}

struct TeamWidgetHero: Equatable {
    let lead: String
    let value: String
    let context: String
    let opponent: TeamWidgetTeam?
    var liveLabel = false
    var liveContext = false
    var maps: [TeamWidgetMap] = []

    static func make(_ content: TeamWidgetContent, now: Date, locale: Locale = .current, timeZone: TimeZone = .current) -> Self {
        func localized(_ key: String, _ argument: String) -> String { teamWidgetString(key, locale: locale, argument) }
        switch content.state {
        case .upcoming(let fixture):
            let opponent = fixture.opponent?.name ?? teamWidgetString("TBD", locale: locale)
            let countdown = TeamWidgetCountdown.make(start: fixture.start, now: now, locale: locale, timeZone: timeZone)
            let lead: String
            let context: String
            switch countdown {
            case .scheduled: lead = localized("Plays %@ on", opponent); context = fixture.context
            case .countdown: lead = localized("Plays %@ in", opponent); context = fixture.context
            case .delayed(let time):
                lead = localized("Plays %@", opponent)
                context = [localized("Was due %@", time), fixture.event].filter { !$0.isEmpty }.joined(separator: " · ")
            case .unknown: lead = localized("Plays %@", opponent); context = fixture.context
            }
            return Self(lead: lead, value: countdown.hero, context: context, opponent: fixture.opponent)
        case .live(let series):
            return Self(lead: localized("Playing %@", series.fixture.opponent?.name ?? teamWidgetString("TBD", locale: locale)),
                value: series.score?.text ?? "–", context: series.fixture.context, opponent: series.fixture.opponent,
                liveLabel: true, maps: series.maps)
        case .liveHidden(let fixture):
            return Self(lead: localized("Playing %@", fixture.opponent?.name ?? teamWidgetString("TBD", locale: locale)),
                value: teamWidgetString("Live now", locale: locale),
                context: [teamWidgetString("Scores hidden", locale: locale), fixture.format].filter { !$0.isEmpty }.joined(separator: " · "),
                opponent: fixture.opponent, liveContext: true)
        case .idleHidden(let last):
            return Self(lead: localized("Last played %@", last.opponent.name), value: teamWidgetDate(last.date, locale: locale, timeZone: timeZone),
                context: teamWidgetString("Scores hidden · No match scheduled", locale: locale), opponent: last.opponent)
        case .idle(.some(let last)):
            let key: String
            switch last.result { case .win: key = "Beat %@"; case .loss: key = "Lost to %@"; case .hidden: key = "Last played %@" }
            return Self(lead: localized(key, last.opponent.name), value: last.result.score,
                context: teamWidgetDate(last.date, locale: locale, timeZone: timeZone) + " · " + teamWidgetString("No match scheduled", locale: locale), opponent: last.opponent)
        case .idle(nil):
            return Self(lead: teamWidgetString("Season starts soon", locale: locale), value: teamWidgetString("No matches", locale: locale),
                context: teamWidgetString("VLR has not listed a match yet", locale: locale), opponent: nil)
        }
    }
}

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

struct TeamWidgetMatchResponse: Codable {
    let id: String
    let event: String
    let series: String
    let status: String?
    let time: String?
    let team1: Team
    let team2: Team
    let current_map: CurrentMap?

    struct Team: Codable {
        let id: String?
        let name: String
        let img: String?
        let score: Int?
        let tag: String?
    }

    struct CurrentMap: Codable {
        let name: String
        let number: Int?
        let scores: [Int?]?
    }
}

struct TeamWidgetDetailsResponse: Codable {
    let veto: [Veto]
    let data: [Map]
    let teams: [TeamWidgetMatchResponse.Team]
    let event: Event?
    let current_map: TeamWidgetMatchResponse.CurrentMap?
    var total_maps: Int? = nil
    var map_count: Int? = nil

    struct Event: Codable { let stage: String? }
    struct Veto: Codable { let team: String?; let action: String; let map: String }
    struct Map: Codable {
        let map: String
        let live: Bool?
        let teams: [TeamWidgetMatchResponse.Team]
    }
}

enum TeamWidgetMapper {
    static func match(for teamID: String, in matches: [TeamWidgetMatchResponse]) -> TeamWidgetMatchResponse? {
        matches.filter { ($0.team1.id == teamID || $0.team2.id == teamID)
            && ["live", "ongoing", "upcoming"].contains($0.status?.lowercased() ?? "") }
            .sorted { left, right in
                let leftLive = left.status?.lowercased() != "upcoming"
                let rightLive = right.status?.lowercased() != "upcoming"
                if leftLive != rightLive { return leftLive }
                return (date(left.time) ?? .distantFuture) < (date(right.time) ?? .distantFuture)
            }.first
    }

    static func content(profile: TeamWidgetRankingResponse, match: TeamWidgetMatchResponse?, details: TeamWidgetDetailsResponse?, spoilersHidden: Bool) -> TeamWidgetContent {
        let history = profile.recent.map { row in
            let score = row.team_score.flatMap { first in row.opponent_score.map { TeamWidgetScore(team: first, opponent: $0) } }
            return TeamWidgetRecentResult(id: row.match_id, date: date(row.played_on), opponent: row.opponent,
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
            let fixture = TeamWidgetFixture(id: match.id,
                opponent: opponent.id?.isEmpty != false || opponent.name.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() == "TBD" ? nil : TeamWidgetTeam(id: opponent.id!, name: opponent.name, tag: opponentDetails?.tag ?? opponent.tag, logo: opponentDetails?.img ?? opponent.img),
                start: date(match.time), event: match.event, stage: stage, format: seriesFormat)
            if ["live", "ongoing"].contains(match.status?.lowercased() ?? "") {
                form.append(.live)
                let score = own.score.flatMap { first in opponent.score.map { TeamWidgetScore(team: first, opponent: $0) } }
                state = .live(TeamWidgetLiveSeries(fixture: fixture, score: score,
                    maps: maps(details: details, match: match, team: profile.team, ownFirst: ownFirst)))
            } else { state = .upcoming(fixture) }
        } else { state = .idle(history.first) }
        let content = TeamWidgetContent(header: .init(team: profile.team, rank: profile.rank, region: profile.region, regionRank: profile.region_rank),
            form: form, history: history, state: state)
        return spoilersHidden ? content.redacted() : content
    }

    static func date(_ value: String?) -> Date? {
        guard let value else { return nil }
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let date = formatter.date(from: value) ?? ISO8601DateFormatter().date(from: value) { return date }
        let day = DateFormatter()
        day.locale = Locale(identifier: "en_US_POSIX")
        day.timeZone = TimeZone(secondsFromGMT: 0)
        day.dateFormat = "yyyy-MM-dd"
        return day.date(from: value)
    }

    private static func maps(details: TeamWidgetDetailsResponse?, match: TeamWidgetMatchResponse, team: TeamWidgetTeam, ownFirst: Bool) -> [TeamWidgetMap] {
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
