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
    let maps: [WidgetMap]
}

struct EventWidgetData: Codable, Equatable, Sendable {
    let event: EventWidgetMetadata
    let matches: [EventWidgetMatch]
    let groups: [EventWidgetGroup]
    let prizes: [EventWidgetPrize]
    let detail: EventWidgetMatchDetail?
    func adding(_ detail: EventWidgetMatchDetail, participants: WidgetMatchPair, format: WidgetSeriesFormat?) -> Self {
        let updated = matches.map { match in
            guard match.fixture.id == detail.matchID else { return match }
            let old = match.fixture
            let fixture = EventWidgetFixture(id: old.id, participants: participants, start: old.start, day: old.day,
                round: old.round, stage: old.stage, format: format)
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

    var widgetURL: URL {
        header.eventID.isEmpty ? VLRWidgetContract.appURL : VLRWidgetContract.detailsURL(kind: "event", id: header.eventID)
    }

    var heroURL: URL {
        switch body {
        case .visible(let state):
            switch state {
            case .scheduled(let hero, _): return VLRWidgetContract.matchURL(id: hero.fixture.id)
            case .live(let hero, _): return VLRWidgetContract.matchURL(id: hero.fixture.id)
            case .between(let hero, _): return VLRWidgetContract.matchURL(id: hero.last.matchID)
            case .finished(.champion(let team, _), _):
                return team.id.map { VLRWidgetContract.detailsURL(kind: "team", id: $0) } ?? widgetURL
            case .gap, .finished(.unconfirmed, _): return widgetURL
            }
        case .hidden(let state):
            switch state {
            case .scheduled(let hero, _): return VLRWidgetContract.matchURL(id: hero.fixture.id)
            case .live(let hero, _): return VLRWidgetContract.matchURL(id: hero.fixture.id)
            case .gap, .finished: return widgetURL
            }
        }
    }
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

