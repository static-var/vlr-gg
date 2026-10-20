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

struct TeamWidgetDay: Codable, Equatable {
    let year: Int
    let month: Int
    let day: Int

    init?(_ value: String) {
        let parts = value.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3 else { return nil }
        let components = DateComponents(year: parts[0], month: parts[1], day: parts[2])
        guard components.isValidDate(in: Calendar(identifier: .gregorian)) else { return nil }
        year = parts[0]
        month = parts[1]
        day = parts[2]
    }
}

struct TeamWidgetRecentResult: Codable, Equatable, Identifiable {
    let id: String
    let date: TeamWidgetDay?
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
            .init(match_id: "2", played_on: "2026-10-12", opponent: .init(id: "8185", name: "DRX", tag: "DRX"), team_score: 2, opponent_score: 0, won: true),
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
        content.form = form.map { $0 == .live ? .live : .neutral }
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

    var historyRows: [TeamWidgetRecentResult] {
        switch state {
        case .live(let series) where !series.maps.isEmpty: []
        case .idle, .idleHidden: Array(history.dropFirst().prefix(4))
        default: Array(history.prefix(4))
        }
    }

    func logoSources(large: Bool) -> [String] {
        let opponent: TeamWidgetTeam?
        switch state {
        case .idle(let last): opponent = last?.opponent
        case .idleHidden(let last): opponent = last.opponent
        default: opponent = fixture?.opponent
        }
        return ([header.team.logo, opponent?.logo] + (large ? historyRows.map { $0.opponent.logo } : [])).compactMap { $0 }
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

    static func make(_ content: TeamWidgetContent?, teamID: String, clientID: String, source: UpcomingMatchesSnapshot?) -> Self {
        guard let source, source.clientId == clientID,
              source.favoriteTeams.contains(where: { $0.id == teamID }) else { return .needsTeam }
        guard let content else { return .placeholder }
        return .content(source.spoilersHidden ? content.redacted() : content)
    }
}

enum TeamWidgetRefreshPolicy {
    static func date(for entry: TeamWidgetEntryContent, now: Date) -> Date? {
        let interval: TimeInterval
        switch entry {
        case .needsTeam: return nil
        case .placeholder: interval = 15 * 60
        case .content(let content):
            switch content.state {
            case .live, .liveHidden: interval = 15 * 60
            case .upcoming(let fixture):
                if let start = fixture.start {
                    let remaining = start.timeIntervalSince(now)
                    interval = remaining <= 0 ? 5 * 60 : remaining <= 86_400 ? 30 * 60 : 2 * 60 * 60
                    if start > now { return min(now.addingTimeInterval(interval), start) }
                } else { interval = 2 * 60 * 60 }
            case .idle, .idleHidden: interval = 2 * 60 * 60
            }
        }
        return now.addingTimeInterval(interval)
    }
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
        if minutes < 60 {
            return .countdown(teamWidgetString("%lld min", locale: locale, Int64(minutes)))
        }
        return .countdown(teamWidgetString("%lldh %02lldm", locale: locale, Int64(minutes / 60), Int64(minutes % 60)))
    }

    func hero(locale: Locale = .current) -> String {
        switch self {
        case .scheduled(let text), .countdown(let text): text
        case .delayed: teamWidgetString("Any minute", locale: locale)
        case .unknown: teamWidgetString("TBD", locale: locale)
        }
    }

    static func timelineDates(start: Date?, now: Date) -> [Date] {
        guard let start, start > now else { return [now] }
        let end = min(start, now.addingTimeInterval(6 * 60 * 60))
        var dates = [now]
        var date = Date(timeIntervalSince1970: (floor(now.timeIntervalSince1970 / 60) + 1) * 60)
        while date <= end && dates.count < 360 {
            dates.append(date)
            date = date.addingTimeInterval(60)
        }
        if start <= end, dates.last != start {
            if dates.count == 360 { dates.removeLast() }
            dates.append(start)
        }
        return dates
    }

    private static func format(_ date: Date, template: String, locale: Locale, timeZone: TimeZone) -> String {
        let formatter = DateFormatter()
        formatter.locale = locale
        formatter.timeZone = timeZone
        formatter.setLocalizedDateFormatFromTemplate(template)
        if template == "EEEj", locale.language.languageCode?.identifier == "en" {
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

private final class TeamWidgetResources {}

func teamWidgetString(_ key: String, locale: Locale = .current, _ arguments: CVarArg...) -> String {
    let resources = Bundle(for: TeamWidgetResources.self)
    let identifier = locale.identifier
    let language = locale.language.languageCode?.identifier ?? identifier
    let candidates = [identifier, identifier.replacingOccurrences(of: "_", with: "-"), language]
        + Bundle.preferredLocalizations(from: resources.localizations, forPreferences: [identifier])
    let path = candidates.lazy.compactMap { resources.path(forResource: $0, ofType: "lproj") }.first
    let bundle = path.flatMap(Bundle.init(path:)) ?? resources
    let format = bundle.localizedString(forKey: key, value: key, table: "Localizable")
    return String(format: format, locale: locale, arguments: arguments)
}

func teamWidgetDate(_ day: TeamWidgetDay?, locale: Locale = .current, timeZone: TimeZone = .current) -> String {
    guard let day else { return teamWidgetString("TBD", locale: locale) }
    var calendar = Calendar(identifier: .gregorian)
    calendar.timeZone = timeZone
    guard let date = calendar.date(from: DateComponents(year: day.year, month: day.month, day: day.day, hour: 12)) else {
        return teamWidgetString("TBD", locale: locale)
    }
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
            return Self(lead: lead, value: countdown.hero(locale: locale), context: context, opponent: fixture.opponent)
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
