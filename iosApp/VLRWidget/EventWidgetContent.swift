import Foundation
import WidgetKit

enum EventReportedStatus: String, Codable, Sendable {
    case upcoming, ongoing, paused, completed, unknown
}
enum EventPhase { case notStarted, inProgress, finished }

struct EventDateBounds: Codable, Equatable, Sendable {
    let start: WidgetDay?
    let endInclusive: WidgetDay?
}

struct EventWidgetMetadata: Codable, Equatable, Sendable {
    let id: String
    let title: String
    let logo: String?
    let location: String
    let prizePool: String?
    let dates: EventDateBounds
    let reportedStatus: EventReportedStatus
}
struct EventWidgetFixture: Codable, Equatable, Sendable {
    let id: String
    let participants: WidgetMatchPair
    let start: Date?
    let day: WidgetDay?
    let round: String
    let stage: String
    let format: WidgetSeriesFormat?
}
enum EventMatchPhase: Codable, Equatable, Sendable {
    case upcoming
    case live(WidgetScore?)
    case completed(WidgetScore?)
    case unknown
}
struct EventWidgetMatch: Codable, Equatable, Sendable {
    let fixture: EventWidgetFixture
    let phase: EventMatchPhase
}
struct EventWidgetStanding: Codable, Equatable, Sendable {
    let team: WidgetTeam
    let wins: Int
    let losses: Int
    let ties: Int
    let mapDifference: Int
}
struct EventWidgetGroup: Codable, Equatable, Sendable {
    let name: String
    let standings: [EventWidgetStanding]
}
struct EventWidgetPrize: Codable, Equatable, Sendable {
    let position: String
    let amount: String
    let team: WidgetTeam
}
struct EventWidgetMatchDetail: Codable, Equatable, Sendable {
    let matchID: String
    let series: WidgetSeriesEnrichment
}

struct EventWidgetData: Codable, Equatable, Sendable {
    let event: EventWidgetMetadata
    let matches: [EventWidgetMatch]
    let groups: [EventWidgetGroup]
    let prizes: [EventWidgetPrize]
    let detail: EventWidgetMatchDetail?
    func adding(_ detail: EventWidgetMatchDetail, participants: WidgetMatchPair) -> Self {
        let updated = matches.map { match in
            guard match.fixture.id == detail.matchID else { return match }
            let old = match.fixture
            let fixture = EventWidgetFixture(id: old.id, participants: participants, start: old.start, day: old.day,
                round: old.round, stage: old.stage, format: detail.series.format)
            return EventWidgetMatch(fixture: fixture, phase: match.phase)
        }
        return Self(event: event, matches: updated, groups: groups, prizes: prizes, detail: detail)
    }
}


enum EventWidgetSetup: Equatable { case chooseEvent, noFavorites }
enum EventWidgetEntryContent: Equatable {
    case setup(EventWidgetSetup)
    case placeholder(eventID: String?)
    case content(EventWidgetContent)
}
enum EventWidgetProgress: Equatable {
    case empty
    case fraction(Double)
    case complete
}
enum EventWidgetSubtitle: Equatable {
    case starts(WidgetDay?, location: String)
    case stage(String, location: String)
    case ended(WidgetDay?, location: String)
    case dates(EventDateBounds, location: String)
}
struct EventWidgetHeader: Equatable {
    let eventID: String
    let title: String
    let logo: String?
    let subtitle: EventWidgetSubtitle
    let progress: EventWidgetProgress
    let hasLiveMatch: Bool
}
struct EventWidgetContent: Equatable {
    let header: EventWidgetHeader
    let body: EventWidgetBody
}
enum EventWidgetBody: Equatable {
    case visible(EventVisibleState)
    case hidden(EventHiddenState)
}

enum EventScheduledHero: Equatable {
    case first(EventWidgetFixture)
    case next(EventWidgetFixture)
}
enum EventScheduleGap: Equatable {
    case notListed(start: WidgetDay?)
    case nextUnlisted
}
struct EventLiveHero: Equatable {
    let fixture: EventWidgetFixture
    let score: WidgetScore?
    let maps: [WidgetMap]
    let otherLiveCount: Int
}
struct EventHiddenLiveHero: Equatable { let fixture: EventWidgetFixture }

struct EventWidgetVictory: Equatable {
    let matchID: String
    let winner: WidgetTeam
    let loser: WidgetTeam
    let score: WidgetScore
    let round: String
    let scheduledStart: Date?
    init?(match: EventWidgetMatch) {
        guard case .completed(let value) = match.phase, let value,
              value.team >= 0, value.opponent >= 0, value.team != value.opponent,
              let left = match.fixture.participants.left.team,
              let right = match.fixture.participants.right.team else { return nil }
        let leftWon = value.team > value.opponent
        matchID = match.fixture.id
        winner = leftWon ? left : right
        loser = leftWon ? right : left
        score = .init(team: max(value.team, value.opponent), opponent: min(value.team, value.opponent))
        round = match.fixture.round
        scheduledStart = match.fixture.start
    }
}
struct EventBetweenHero: Equatable {
    let last: EventWidgetVictory
    let next: EventWidgetFixture
}
struct EventEndedHero: Equatable {
    let end: WidgetDay?
    let prizePool: String?
}
enum EventFinishedHero: Equatable {
    case champion(WidgetTeam, final: EventWidgetVictory?)
    case unconfirmed(EventEndedHero)
}

enum EventMatchListHeading: Equatable { case today, upNext, earlier }
struct EventMatchList: Equatable {
    let heading: EventMatchListHeading
    let rows: [EventWidgetMatch]
}
struct EventNeutralMatch: Equatable {
    let fixture: EventWidgetFixture
    let phase: Phase
    enum Phase: Equatable { case upcoming, live, ended }
}
struct EventNeutralList: Equatable {
    let heading: EventMatchListHeading
    let rows: [EventNeutralMatch]
}
struct EventGroupRow: Equatable {
    let rank: Int
    let standing: EventWidgetStanding
    let isFavorite: Bool
}
struct EventGroupTable: Equatable {
    let name: String
    let rows: [EventGroupRow]
}
enum EventUpcomingList: Equatable {
    case matches(EventMatchList)
    case group(EventGroupTable)
}
enum EventFinishedList: Equatable {
    case prizes([EventWidgetPrize])
    case matches(EventMatchList)
}
enum EventVisibleState: Equatable {
    case scheduled(EventScheduledHero, EventUpcomingList)
    case live(EventLiveHero, EventMatchList)
    case between(EventBetweenHero, EventMatchList)
    case gap(EventScheduleGap, EventMatchList)
    case finished(EventFinishedHero, EventFinishedList)
}
enum EventHiddenState: Equatable {
    case scheduled(EventScheduledHero, EventNeutralList)
    case live(EventHiddenLiveHero, EventNeutralList)
    case gap(EventScheduleGap, EventNeutralList)
    case finished(EventEndedHero, EventNeutralList)
}

enum EventWidgetDates {
    private static let months = ["jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"]

    static func parse(_ text: String, matchDays: [WidgetDay], reference: Date) -> EventDateBounds {
        let normalized = text.replacingOccurrences(of: "–", with: "-").replacingOccurrences(of: "—", with: "-")
        let parts = normalized.split(separator: "-", omittingEmptySubsequences: false).map { String($0).trimmingCharacters(in: .whitespacesAndNewlines) }
        let fallback = EventDateBounds(start: matchDays.min(), endInclusive: matchDays.max())
        guard parts.count == 2 else { return fallback }
        func bound(_ value: String, month: Int?) -> (month: Int, day: Int, year: Int?)? {
            let tokens = value.replacingOccurrences(of: ",", with: " ").split(whereSeparator: { $0.isWhitespace }).map(String.init)
            guard !tokens.isEmpty, tokens.count <= 3 else { return nil }
            let hasMonth = months.contains(String(tokens[0].prefix(3)).lowercased())
            let resolvedMonth = hasMonth ? months.firstIndex(of: String(tokens[0].prefix(3)).lowercased()).map { $0 + 1 } : month
            let dayIndex = hasMonth ? 1 : 0
            guard let resolvedMonth, tokens.indices.contains(dayIndex), let day = Int(tokens[dayIndex]),
                  tokens.count <= dayIndex + 2 else { return nil }
            var year: Int?
            if tokens.count == dayIndex + 2 {
                let token = tokens[dayIndex + 1]
                guard token.count == 4, let parsed = Int(token), parsed > 0 else { return nil }
                year = parsed
            }
            return (resolvedMonth, day, year)
        }
        guard let left = bound(parts[0], month: nil) else { return fallback }
        let right = bound(parts[1], month: left.month)
        guard right != nil || parts[1].caseInsensitiveCompare("TBD") == .orderedSame else { return fallback }
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(secondsFromGMT: 0)!
        let anchor = matchDays.min()?.year ?? calendar.component(.year, from: reference)
        let startYear = left.year ?? right?.year.map { $0 - (left.month > (right?.month ?? left.month) ? 1 : 0) } ?? anchor
        guard let start = WidgetDay(year: startYear, month: left.month, day: left.day) else { return fallback }
        var end: WidgetDay?
        if let right {
            let year = right.year ?? (startYear + (right.month < left.month ? 1 : 0))
            guard let parsed = WidgetDay(year: year, month: right.month, day: right.day), parsed >= start else { return fallback }
            end = parsed
        } else { end = matchDays.max() }
        guard end == nil || end! >= start else { return EventDateBounds(start: start, endInclusive: nil) }
        return EventDateBounds(start: start, endInclusive: end)
    }

    static func matchStart(date: String, time: String) -> Date? {
        let time = time.trimmingCharacters(in: .whitespacesAndNewlines)
        if time.contains("T") {
            guard WidgetDay(String(time.prefix(10))) != nil else { return nil }
            return widgetInstant(time)
        }
        guard date.range(of: #"^\d{4}-\d{2}-\d{2}$"#, options: .regularExpression) != nil,
              WidgetDay(date) != nil else { return nil }
        let parts = time.split(separator: ":", omittingEmptySubsequences: false)
        guard (2...3).contains(parts.count), parts.allSatisfy({ $0.count == 2 && Int($0) != nil }),
              let hour = Int(parts[0]), hour < 24, let minute = Int(parts[1]), minute < 60,
              parts.count == 2 || Int(parts[2])! < 60 else { return nil }
        return widgetInstant(date + "T" + time + (parts.count == 2 ? ":00Z" : "Z"))
    }

    static func progress(_ bounds: EventDateBounds, phase: EventPhase, at date: Date, timeZone: TimeZone) -> EventWidgetProgress {
        if phase == .finished { return .complete }
        guard phase == .inProgress, let first = bounds.start, let last = bounds.endInclusive, last >= first else { return .empty }
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone
        let start = first.start(in: timeZone)
        guard let end = calendar.date(byAdding: .day, value: 1, to: last.start(in: timeZone)), end > start else { return .empty }
        let fraction = max(0, min(1, date.timeIntervalSince(start) / end.timeIntervalSince(start)))
        return fraction == 0 ? .empty : fraction == 1 ? .complete : .fraction(fraction)
    }
}

enum EventWidgetSelection {
    case scheduled(EventScheduledHero)
    case live(EventLiveHero)
    case between(EventBetweenHero)
    case gap(EventScheduleGap)
    case finished(EventFinishedHero)

    var detailMatchID: String? {
        switch self {
        case .scheduled(let hero): hero.fixture.id
        case .live(let hero): hero.fixture.id
        case .between(let hero): hero.next.id
        case .gap, .finished: nil
        }
    }
}

extension EventScheduledHero {
    var fixture: EventWidgetFixture {
        switch self { case .first(let fixture), .next(let fixture): fixture }
    }
}

struct EventWidgetEntry: TimelineEntry {
    let date: Date
    let content: EventWidgetEntryContent
}

struct EventWidgetPlan {
    let entries: [EventWidgetEntry]
    let refreshAfter: Date?

    func logoSources(large: Bool) -> [String] {
        Array(Set(entries.flatMap { entry -> [String] in
            guard case .content(let content) = entry.content else { return [] }
            return content.logoSources(large: large)
        })).sorted()
    }
}

enum EventWidgetEngine {
    static func plan(_ record: FavoriteWidgetRecord<EventWidgetData>?, selectedEventID: String?,
                     source: UpcomingMatchesSnapshot?, at date: Date, timeZone: TimeZone) -> EventWidgetPlan {
        func single(_ content: EventWidgetEntryContent, refresh: Date? = nil) -> EventWidgetPlan {
            .init(entries: [.init(date: date, content: content)], refreshAfter: refresh)
        }
        guard let source, !source.favoriteEvents.isEmpty else { return single(.setup(.noFavorites)) }
        guard let id = selectedEventID, source.favoriteEvents.contains(where: { $0.id == id }) else { return single(.setup(.chooseEvent)) }
        guard !source.clientId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
              let record, record.key == FavoriteWidgetKey(entityID: id, clientID: source.clientId),
              record.value.event.id == id else {
            return single(.placeholder(eventID: id), refresh: date.addingTimeInterval(900))
        }
        let content = content(record.value, fetchedAt: record.fetchedAt, source: source, at: date, timeZone: timeZone)
        let entry = EventWidgetEntryContent.content(content)
        let refresh = EventWidgetRefreshPolicy.date(for: entry, data: record.value, fetchedAt: record.fetchedAt, now: date)
        var dates = Set([date])
        for fixture in content.fixtures(large: true) {
            dates.formUnion(WidgetKickoff.timelineDates(start: fixture.start, now: date, timeZone: timeZone))
        }
        if case .visible(.between(let hero, _)) = content.body, let start = hero.last.scheduledStart {
            dates.insert(start.addingTimeInterval(43_200))
        }
        let safety = record.fetchedAt.addingTimeInterval(10_801)
        if record.value.matches.contains(where: { if case .live = $0.phase { return true }; return false }), safety > date {
            dates.insert(safety)
        }
        let entries = dates.filter { $0 >= date }.sorted().map {
            EventWidgetEntry(date: $0, content: .content(self.content(record.value, fetchedAt: record.fetchedAt, source: source, at: $0, timeZone: timeZone)))
        }
        return .init(entries: entries, refreshAfter: refresh)
    }

    static func eligible(_ match: EventWidgetMatch, fetchedAt: Date, at date: Date) -> Bool {
        switch match.phase {
        case .live: date.timeIntervalSince(fetchedAt) <= 10_800
        case .upcoming: match.fixture.start.map { date.timeIntervalSince($0) <= 21_600 } ?? true
        case .completed: true
        case .unknown: false
        }
    }

    static func phase(in data: EventWidgetData, fetchedAt: Date, at date: Date, timeZone: TimeZone) -> EventPhase {
        if data.matches.contains(where: { if case .live = $0.phase { return eligible($0, fetchedAt: fetchedAt, at: date) }; return false }) { return .inProgress }
        if data.event.reportedStatus == .completed { return .finished }
        if data.matches.contains(where: { if case .completed = $0.phase { return true }; if case .live = $0.phase { return true }; return false }) { return .inProgress }
        if data.event.reportedStatus == .ongoing || data.event.reportedStatus == .paused { return .inProgress }
        if let start = data.event.dates.start, date >= start.start(in: timeZone) { return .inProgress }
        return .notStarted
    }

    static func chronological(_ left: EventWidgetMatch, _ right: EventWidgetMatch) -> Bool {
        if left.fixture.start != right.fixture.start { return (left.fixture.start ?? .distantFuture) < (right.fixture.start ?? .distantFuture) }
        if left.fixture.day != right.fixture.day { return (left.fixture.day ?? WidgetDay("9999-12-31")!) < (right.fixture.day ?? WidgetDay("9999-12-31")!) }
        return left.fixture.id < right.fixture.id
    }

    /// The API has no completion time. The 12-hour result window uses the scheduled start and can therefore end early.
    static func selection(in data: EventWidgetData, fetchedAt: Date, favoriteTeamIDs: Set<String>,
                          spoilersHidden: Bool, at date: Date, timeZone: TimeZone) -> EventWidgetSelection {
        let available = data.matches.filter { eligible($0, fetchedAt: fetchedAt, at: date) }
        func priority(_ left: EventWidgetMatch, _ right: EventWidgetMatch) -> Bool {
            let leftFavorite = left.fixture.participants.teams.contains { $0.id.map(favoriteTeamIDs.contains) ?? false }
            let rightFavorite = right.fixture.participants.teams.contains { $0.id.map(favoriteTeamIDs.contains) ?? false }
            if leftFavorite != rightFavorite { return leftFavorite }
            return chronological(left, right)
        }
        func enriched(_ fixture: EventWidgetFixture) -> EventWidgetFixture {
            guard let detail = data.detail, detail.matchID == fixture.id else { return fixture }
            return .init(id: fixture.id, participants: fixture.participants, start: fixture.start, day: fixture.day,
                         round: fixture.round, stage: fixture.stage, format: detail.series.format)
        }
        let live = available.filter { if case .live = $0.phase { return true }; return false }.sorted(by: priority)
        if let first = live.first, case .live(let score) = first.phase {
            return .live(.init(fixture: enriched(first.fixture), score: score,
                maps: data.detail?.matchID == first.fixture.id ? data.detail!.series.maps : [], otherLiveCount: live.count - 1))
        }
        let completed = data.matches.filter { if case .completed = $0.phase { return true }; return false }.sorted(by: chronological)
        let last = completed.last.flatMap(EventWidgetVictory.init)
        let phase = phase(in: data, fetchedAt: fetchedAt, at: date, timeZone: timeZone)
        if phase == .finished {
            let champion = data.prizes.first { $0.position.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() == "1st" }?.team ?? last?.winner
            if let champion {
                let sameWinner = last.map { sameTeam($0.winner, champion) } ?? false
                return .finished(.champion(champion, final: sameWinner ? last : nil))
            }
            return .finished(.unconfirmed(.init(end: data.event.dates.endInclusive, prizePool: data.event.prizePool)))
        }
        let upcoming = available.filter { $0.phase == .upcoming }.sorted(by: priority)
        guard let nextMatch = upcoming.first else {
            return .gap(phase == .notStarted ? .notListed(start: data.event.dates.start) : .nextUnlisted)
        }
        let next = enriched(nextMatch.fixture)
        if !spoilersHidden, let last, let start = last.scheduledStart, let nextStart = next.start {
            var calendar = Calendar(identifier: .gregorian)
            calendar.timeZone = timeZone
            let age = date.timeIntervalSince(start)
            if age >= 0, age < 43_200, calendar.startOfDay(for: nextStart) > calendar.startOfDay(for: date) {
                return .between(.init(last: last, next: next))
            }
        }
        let first = data.matches.filter { $0.phase != .unknown }.sorted(by: chronological).first
        return .scheduled(phase == .notStarted && first?.fixture.id == next.id ? .first(next) : .next(next))
    }

    static func sameTeam(_ left: WidgetTeam, _ right: WidgetTeam) -> Bool {
        if let leftID = left.id, let rightID = right.id { return leftID == rightID }
        return left.name.trimmingCharacters(in: .whitespacesAndNewlines).caseInsensitiveCompare(right.name.trimmingCharacters(in: .whitespacesAndNewlines)) == .orderedSame
    }

    static func sample(at date: Date) -> EventWidgetContent {
        let fixture = EventWidgetFixture(id: "sample", participants: .init(left: .team(.init(id: nil, name: "Alpha")), right: .team(.init(id: nil, name: "Bravo"))),
            start: date.addingTimeInterval(8_040), day: nil, round: "", stage: "", format: nil)
        return .init(header: .init(eventID: "", title: "Val Esports", logo: nil, subtitle: .starts(nil, location: ""), progress: .empty, hasLiveMatch: false),
                     body: .visible(.scheduled(.first(fixture), .matches(.init(heading: .upNext, rows: [])))))
    }

    private static func content(_ data: EventWidgetData, fetchedAt: Date, source: UpcomingMatchesSnapshot,
                                at date: Date, timeZone: TimeZone) -> EventWidgetContent {
        let favorites = Set(source.favorites.teamIds)
        let selection = selection(in: data, fetchedAt: fetchedAt, favoriteTeamIDs: favorites, spoilersHidden: source.spoilersHidden, at: date, timeZone: timeZone)
        let phase = phase(in: data, fetchedAt: fetchedAt, at: date, timeZone: timeZone)
        let subtitle: EventWidgetSubtitle
        let selectedFixture: EventWidgetFixture?
        switch selection {
        case .scheduled(let hero): selectedFixture = hero.fixture
        case .live(let hero): selectedFixture = hero.fixture
        case .between(let hero): selectedFixture = data.matches.first { $0.fixture.id == hero.last.matchID }?.fixture
        default: selectedFixture = data.matches.sorted(by: chronological).last?.fixture
        }
        switch phase {
        case .finished: subtitle = .ended(data.event.dates.endInclusive, location: data.event.location)
        case .notStarted:
            subtitle = data.matches.isEmpty ? .dates(data.event.dates, location: data.event.location) : .starts(data.event.dates.start, location: data.event.location)
        case .inProgress: subtitle = .stage(selectedFixture?.stage ?? "", location: data.event.location)
        }
        let header = EventWidgetHeader(eventID: data.event.id, title: data.event.title, logo: data.event.logo, subtitle: subtitle,
            progress: EventWidgetDates.progress(data.event.dates, phase: phase, at: date, timeZone: timeZone),
            hasLiveMatch: data.matches.contains { if case .live = $0.phase { return eligible($0, fetchedAt: fetchedAt, at: date) }; return false })
        func list(_ heroID: String?, _ preferred: EventMatchListHeading) -> EventMatchList {
            matchList(in: data, heroID: heroID, preferred: preferred, fetchedAt: fetchedAt, at: date, timeZone: timeZone)
        }
        let body: EventWidgetBody
        if source.spoilersHidden {
            switch selection {
            case .scheduled(let hero):
                let later = data.matches.filter { $0.phase == .upcoming && eligible($0, fetchedAt: fetchedAt, at: date)
                    && chronological(.init(fixture: hero.fixture, phase: .upcoming), $0) }.sorted(by: chronological)
                let rows = later.isEmpty ? list(hero.fixture.id, .earlier) : EventMatchList(heading: .upNext, rows: Array(later.prefix(3)))
                body = .hidden(.scheduled(hero, neutral(rows)))
            case .live(let hero): body = .hidden(.live(.init(fixture: hero.fixture), neutral(list(hero.fixture.id, .today))))
            case .gap(let gap): body = .hidden(.gap(gap, neutral(list(nil, .earlier))))
            case .finished: body = .hidden(.finished(.init(end: data.event.dates.endInclusive, prizePool: data.event.prizePool), neutral(list(nil, .earlier))))
            case .between: preconditionFailure("Hidden selection cannot contain a result")
            }
        } else {
            switch selection {
            case .scheduled(let hero):
                body = .visible(.scheduled(hero, upcomingList(in: data, hero: hero.fixture, favorites: favorites, fetchedAt: fetchedAt, at: date, timeZone: timeZone)))
            case .live(let hero): body = .visible(.live(hero, list(hero.fixture.id, .today)))
            case .between(let hero): body = .visible(.between(hero, list(hero.last.matchID, .upNext)))
            case .gap(let gap): body = .visible(.gap(gap, list(nil, .earlier)))
            case .finished(let hero):
                let prizes = Array(data.prizes.filter { $0.position.lowercased() != "1st" }.prefix(4))
                body = .visible(.finished(hero, prizes.isEmpty ? .matches(list(nil, .earlier)) : .prizes(prizes)))
            }
        }
        return .init(header: header, body: body)
    }

    private static func upcomingList(in data: EventWidgetData, hero: EventWidgetFixture, favorites: Set<String>,
                                     fetchedAt: Date, at date: Date, timeZone: TimeZone) -> EventUpcomingList {
        let exact = data.groups.first { $0.name.caseInsensitiveCompare(hero.round) == .orderedSame }
        let playoffWords = ["final", "playoff", "upper", "lower", "quarter", "semi", "opening", "elimination", "decider", "knockout", "round of"]
        let groupStage = hero.stage.lowercased().contains("group") || !playoffWords.contains(where: hero.round.lowercased().contains)
        let containing = groupStage && hero.participants.teams.count == 2 ? data.groups.filter { group in
            hero.participants.teams.allSatisfy { team in group.standings.contains { sameTeam($0.team, team) } }
        } : []
        if let group = exact ?? (containing.count == 1 ? containing[0] : nil) {
            let firstFavorite = group.standings.firstIndex { $0.team.id.map(favorites.contains) ?? false }
            let start = min(max(0, (firstFavorite ?? 0) - 4), max(0, group.standings.count - 5))
            let rows = group.standings.enumerated().dropFirst(start).prefix(5).map {
                EventGroupRow(rank: $0.offset + 1, standing: $0.element, isFavorite: $0.element.team.id.map(favorites.contains) ?? false)
            }
            return .group(.init(name: group.name, rows: rows))
        }
        let later = data.matches.filter { $0.fixture.id != hero.id && $0.phase == .upcoming && eligible($0, fetchedAt: fetchedAt, at: date)
            && chronological(.init(fixture: hero, phase: .upcoming), $0) }.sorted(by: chronological)
        if !later.isEmpty { return .matches(.init(heading: .upNext, rows: Array(later.prefix(3)))) }
        return .matches(matchList(in: data, heroID: hero.id, preferred: .earlier, fetchedAt: fetchedAt, at: date, timeZone: timeZone))
    }

    private static func matchList(in data: EventWidgetData, heroID: String?, preferred: EventMatchListHeading,
                                  fetchedAt: Date, at date: Date, timeZone: TimeZone) -> EventMatchList {
        let available = data.matches.filter { $0.fixture.id != heroID && eligible($0, fetchedAt: fetchedAt, at: date) }
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone
        if preferred == .today {
            let today = available.filter { $0.fixture.start.map { calendar.isDate($0, inSameDayAs: date) } ?? false }.sorted(by: chronological)
            if !today.isEmpty { return .init(heading: .today, rows: Array(today.prefix(3))) }
        }
        if preferred != .earlier {
            let upcoming = available.filter { $0.phase == .upcoming }.sorted(by: chronological)
            if !upcoming.isEmpty { return .init(heading: .upNext, rows: Array(upcoming.prefix(3))) }
        }
        let recent = available.filter { if case .completed = $0.phase { return true }; return false }.sorted { chronological($1, $0) }
        return .init(heading: .earlier, rows: Array(recent.prefix(3)))
    }

    private static func neutral(_ list: EventMatchList, after fixture: EventWidgetFixture? = nil) -> EventNeutralList {
        .init(heading: list.heading, rows: list.rows.filter { row in
            guard let fixture, list.heading == .upNext else { return true }
            return chronological(.init(fixture: fixture, phase: .upcoming), row)
        }.map { row in
            let phase: EventNeutralMatch.Phase
            switch row.phase { case .upcoming: phase = .upcoming; case .live: phase = .live; case .completed, .unknown: phase = .ended }
            return .init(fixture: row.fixture, phase: phase)
        })
    }
}

extension EventWidgetContent {
    func fixtures(large: Bool) -> [EventWidgetFixture] {
        func matchRows(_ list: EventMatchList) -> [EventWidgetFixture] { large ? list.rows.map(\.fixture) : [] }
        func neutralRows(_ list: EventNeutralList) -> [EventWidgetFixture] { large ? list.rows.map(\.fixture) : [] }
        switch body {
        case .visible(let state):
            switch state {
            case .scheduled(let hero, let list):
                if case .matches(let list) = list { return [hero.fixture] + matchRows(list) }
                return [hero.fixture]
            case .live(let hero, let list): return [hero.fixture] + matchRows(list)
            case .between(let hero, let list): return [hero.next] + matchRows(list)
            case .gap(_, let list): return matchRows(list)
            case .finished(_, let list): if case .matches(let list) = list { return matchRows(list) }; return []
            }
        case .hidden(let state):
            switch state {
            case .scheduled(let hero, let list): return [hero.fixture] + neutralRows(list)
            case .live(let hero, let list): return [hero.fixture] + neutralRows(list)
            case .gap(_, let list), .finished(_, let list): return neutralRows(list)
            }
        }
    }

    func logoSources(large: Bool) -> [String] {
        var sources = [header.logo].compactMap { $0 }
        sources += fixtures(large: large).flatMap { $0.participants.teams.compactMap(\.logo) }
        if case .visible(let state) = body {
            switch state {
            case .between(let hero, _): sources += [hero.last.winner.logo, hero.last.loser.logo].compactMap { $0 }
            case .finished(.champion(let team, _), let list):
                sources += [team.logo].compactMap { $0 }
                if large, case .prizes(let prizes) = list { sources += prizes.compactMap(\.team.logo) }
            case .scheduled(_, .group(let group)) where large: sources += group.rows.compactMap(\.standing.team.logo)
            default: break
            }
        }
        return Array(Set(sources)).sorted()
    }
}

enum EventWidgetRefreshPolicy {
    static func date(for content: EventWidgetEntryContent, data: EventWidgetData?, fetchedAt: Date?, now: Date) -> Date? {
        guard case .content(let content) = content else {
            if case .setup = content { return nil }
            return now.addingTimeInterval(900)
        }
        let fixtures = content.fixtures(large: false)
        let scheduled = fixtures.first
        let base: Date
        switch content.body {
        case .visible(.live), .hidden(.live): base = now.addingTimeInterval(900)
        case .visible(.finished), .hidden(.finished): return now.addingTimeInterval(43_200)
        case .visible(.scheduled), .hidden(.scheduled), .visible(.between):
            base = FavoriteWidgetRefreshPolicy.scheduled(start: scheduled?.start, now: now)
        case .visible(.gap(let gap, _)), .hidden(.gap(let gap, _)):
            if case .notListed = gap { base = now.addingTimeInterval(21_600) }
            else { base = now.addingTimeInterval(7_200) }
        }
        var result = base
        if let data, let fetchedAt {
            let expiredOpen = data.matches.contains { match in
                switch match.phase {
                case .upcoming, .live: !EventWidgetEngine.eligible(match, fetchedAt: fetchedAt, at: now)
                default: false
                }
            }
            if expiredOpen { result = min(result, now.addingTimeInterval(900)) }
            let next = data.matches.filter { $0.phase == .upcoming && EventWidgetEngine.eligible($0, fetchedAt: fetchedAt, at: now) }
                .compactMap(\.fixture.start).filter { $0 > now }.min()
            if let next { result = min(result, next) }
        }
        if case .visible(.between(let hero, _)) = content.body, let start = hero.last.scheduledStart {
            let cutoff = start.addingTimeInterval(43_200)
            if cutoff > now { result = min(result, cutoff) }
        }
        return result
    }
}
