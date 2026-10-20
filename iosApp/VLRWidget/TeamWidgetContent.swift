import Foundation

extension FavoriteWidgetRecord where Value == TeamWidgetContent {
    func teamContent(at date: Date) -> TeamWidgetContent {
        switch origin {
        case .fresh: value
        case .cachedFallback: value.expired(fetchedAt: fetchedAt, now: date)
        }
    }
}

struct TeamWidgetHeader: Codable, Equatable, Sendable {
    let team: WidgetTeam
    let rank: Int?
    let region: String?
    let regionRank: Int?
}

enum TeamWidgetFormSlot: String, Codable, Sendable {
    case win, loss, empty, neutral, live
}

enum TeamWidgetResult: Codable, Equatable, Sendable {
    case win(WidgetScore?)
    case loss(WidgetScore?)
    case hidden

    var score: String {
        switch self {
        case .win(let score), .loss(let score): score?.text ?? "–"
        case .hidden: ""
        }
    }
}

struct TeamWidgetRecentResult: Codable, Equatable, Sendable, Identifiable {
    let id: String
    let date: WidgetDay?
    let opponent: WidgetTeam
    var result: TeamWidgetResult
}

struct TeamWidgetFixture: Codable, Equatable, Sendable {
    let id: String
    let opponent: WidgetTeam?
    let start: Date?
    let event: String
    let stage: String
    let format: String

    var context: String { [event, stage, format].filter { !$0.isEmpty }.joined(separator: " · ") }
}

struct TeamWidgetLiveSeries: Codable, Equatable, Sendable {
    let fixture: TeamWidgetFixture
    let score: WidgetScore?
    let maps: [WidgetMap]
}

enum TeamWidgetState: Codable, Equatable, Sendable {
    case upcoming(TeamWidgetFixture)
    case live(TeamWidgetLiveSeries)
    case liveHidden(TeamWidgetFixture)
    case idle(TeamWidgetRecentResult?)
    case idleHidden(TeamWidgetRecentResult)
}

struct TeamWidgetContent: Codable, Equatable, Sendable {
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
        var content = TeamWidgetMapper.content(profile: sampleProfile, match: nil, details: nil)
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

    func expired(fetchedAt: Date, now: Date) -> Self {
        let expired: Bool
        switch state {
        case .live: expired = now.timeIntervalSince(fetchedAt) > 3 * 60 * 60
        case .upcoming(let fixture): expired = fixture.start.map { now.timeIntervalSince($0) > 6 * 60 * 60 } ?? false
        default: expired = false
        }
        guard expired else { return self }
        var content = self
        content.state = .idle(history.first)
        if content.form.last == .live { content.form.removeLast() }
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
        let opponent: WidgetTeam?
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

enum TeamWidgetSetup: Equatable {
    case chooseTeam, noFavorites
}

enum TeamWidgetEntryContent: Equatable {
    case needsTeam(TeamWidgetSetup)
    case content(TeamWidgetContent)
    case placeholder

    static func make(_ content: TeamWidgetContent?, teamID: String?, clientID: String, source: UpcomingMatchesSnapshot?) -> Self {
        guard let source, !source.favoriteTeams.isEmpty else { return .needsTeam(.noFavorites) }
        guard let teamID, source.clientId == clientID,
              source.favoriteTeams.contains(where: { $0.id == teamID }) else { return .needsTeam(.chooseTeam) }
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
                return FavoriteWidgetRefreshPolicy.scheduled(start: fixture.start, now: now)
            case .idle, .idleHidden: interval = 2 * 60 * 60
            }
        }
        return now.addingTimeInterval(interval)
    }
}

struct TeamWidgetHero: Equatable {
    let lead: String
    let value: String
    let context: String
    let opponent: WidgetTeam?
    var liveLabel = false
    var liveContext = false
    var maps: [WidgetMap] = []

    static func make(_ content: TeamWidgetContent, now: Date, locale: Locale = .current, timeZone: TimeZone = .current) -> Self {
        func localized(_ key: String, _ argument: String) -> String { widgetString(key, locale: locale, argument) }
        switch content.state {
        case .upcoming(let fixture):
            let opponent = fixture.opponent?.name ?? widgetString("TBD", locale: locale)
            let kickoff = WidgetKickoff.make(start: fixture.start, now: now, locale: locale, timeZone: timeZone)
            let lead: String
            let context: String
            switch kickoff {
            case .scheduled(let day, _): lead = localized("Plays %@", opponent) + " · " + day; context = fixture.context
            case .delayed(let time):
                lead = localized("Plays %@", opponent)
                context = [localized("Was due %@", time), fixture.event].filter { !$0.isEmpty }.joined(separator: " · ")
            case .unknown: lead = localized("Plays %@", opponent); context = fixture.context
            }
            return Self(lead: lead, value: kickoff.hero(locale: locale), context: context, opponent: fixture.opponent)
        case .live(let series):
            return Self(lead: localized("Playing %@", series.fixture.opponent?.name ?? widgetString("TBD", locale: locale)),
                value: series.score?.text ?? "–", context: series.fixture.context, opponent: series.fixture.opponent,
                liveLabel: true, maps: series.maps)
        case .liveHidden(let fixture):
            return Self(lead: localized("Playing %@", fixture.opponent?.name ?? widgetString("TBD", locale: locale)),
                value: widgetString("Live now", locale: locale),
                context: [widgetString("Scores hidden", locale: locale), fixture.format].filter { !$0.isEmpty }.joined(separator: " · "),
                opponent: fixture.opponent, liveContext: true)
        case .idleHidden(let last):
            return Self(lead: localized("Last played %@", last.opponent.name), value: widgetDate(last.date, locale: locale, timeZone: timeZone),
                context: widgetString("Scores hidden · No match scheduled", locale: locale), opponent: last.opponent)
        case .idle(.some(let last)):
            let key: String
            switch last.result { case .win: key = "Beat %@"; case .loss: key = "Lost to %@"; case .hidden: key = "Last played %@" }
            return Self(lead: localized(key, last.opponent.name), value: last.result.score,
                context: widgetDate(last.date, locale: locale, timeZone: timeZone) + " · " + widgetString("No match scheduled", locale: locale), opponent: last.opponent)
        case .idle(nil):
            return Self(lead: widgetString("Season starts soon", locale: locale), value: widgetString("No matches", locale: locale),
                context: widgetString("VLR has not listed a match yet", locale: locale), opponent: nil)
        }
    }
}
