import Foundation

enum EventWidgetArt: Equatable {
    case pair(WidgetMatchPair)
    case winner(WidgetTeam)
    case none
}

struct EventWidgetHeroText: Equatable {
    let lead: String
    let value: String
    var context = ""
    var day: String? = nil
    var liveBadge = false
    var contextLive = false
    var otherLiveCount = 0
    var beside: Beside? = nil
    var maps: [WidgetMap] = []
    var art: EventWidgetArt = .none

    struct Beside: Equatable {
        let round: String
        let format: String?
    }

    static func make(_ state: EventVisibleState, now: Date, locale: Locale, timeZone: TimeZone) -> Self {
        func text(_ key: String) -> String { widgetString(key, locale: locale) }
        switch state {
        case .scheduled(let hero, _): return scheduled(hero, now: now, locale: locale, timeZone: timeZone)
        case .live(let hero, _):
            return .init(lead: EventWidgetText.matchup(hero.fixture.participants, locale: locale), value: hero.score?.text ?? "–",
                liveBadge: true, otherLiveCount: hero.otherLiveCount, beside: .init(round: hero.fixture.round, format: hero.fixture.format?.label),
                maps: hero.maps, art: .pair(hero.fixture.participants))
        case .between(let hero, _):
            let next = hero.next.start.map { WidgetKickoff.row(start: $0, now: now, locale: locale, timeZone: timeZone) }
            let nextText = next.map { widgetString("Next: %@", locale: locale, $0.day + ", " + $0.time) }
            return .init(lead: widgetString("%1$@ beat %2$@", locale: locale, hero.last.winner.name, hero.last.loser.name), value: hero.last.score.text,
                context: EventWidgetText.join([hero.last.round, nextText]),
                art: .pair(.init(left: .team(hero.last.winner), right: .team(hero.last.loser))))
        case .gap(let gap, _): return gapText(gap, locale: locale, timeZone: timeZone)
        case .finished(let hero, _):
            switch hero {
            case .champion(let team, let final):
                let result = final.map { widgetString("Beat %1$@ %2$@", locale: locale, $0.loser.name, $0.score.text) }
                return .init(lead: text("Champions"), value: team.name, context: EventWidgetText.join([result, final?.round]), art: .winner(team))
            case .unconfirmed(let ended): return endedText(ended, hidden: false, locale: locale, timeZone: timeZone)
            }
        }
    }

    static func make(_ state: EventHiddenState, now: Date, locale: Locale, timeZone: TimeZone) -> Self {
        switch state {
        case .scheduled(let hero, _): return scheduled(hero, now: now, locale: locale, timeZone: timeZone)
        case .live(let hero, _):
            return .init(lead: EventWidgetText.matchup(hero.fixture.participants, locale: locale), value: widgetString("Live now", locale: locale),
                context: EventWidgetText.join([widgetString("Scores hidden", locale: locale), hero.fixture.round]), contextLive: true,
                    art: .pair(hero.fixture.participants))
        case .gap(let gap, _): return gapText(gap, locale: locale, timeZone: timeZone)
        case .finished(let ended, _): return endedText(ended, hidden: true, locale: locale, timeZone: timeZone)
        }
    }

    private static func scheduled(_ hero: EventScheduledHero, now: Date, locale: Locale, timeZone: TimeZone) -> Self {
        let fixture = hero.fixture
        let kickoff = WidgetKickoff.make(start: fixture.start, now: now, locale: locale, timeZone: timeZone)
        let matchup = EventWidgetText.matchup(fixture.participants, locale: locale)
        let lead: String
        let context: String
        var leadDay: String?
        switch kickoff {
        case .delayed(let time):
            lead = matchup
            context = EventWidgetText.join([widgetString("Was due %@", locale: locale, time), fixture.round])
        case .scheduled(let day, _):
            leadDay = day
            if case .first = hero {
                lead = widgetString("First match", locale: locale)
                context = EventWidgetText.join([matchup, fixture.round])
            } else if fixture.participants.teams.count < 2 {
                lead = fixture.round
                context = EventWidgetText.join([matchup, fixture.format?.label])
            } else {
                lead = matchup
                context = EventWidgetText.join([fixture.round, fixture.format?.label])
            }
        case .unknown:
            if case .first = hero {
                lead = widgetString("First match", locale: locale)
                context = EventWidgetText.join([matchup, fixture.round])
            } else {
                lead = matchup
                context = EventWidgetText.join([fixture.round, fixture.format?.label])
            }
        }
        return .init(lead: lead, value: kickoff.hero(locale: locale), context: context, day: leadDay, art: .pair(fixture.participants))
    }

    private static func gapText(_ gap: EventScheduleGap, locale: Locale, timeZone: TimeZone) -> Self {
        switch gap {
        case .notListed(let start):
            return .init(lead: widgetString("Starts on", locale: locale), value: widgetDate(start, locale: locale, timeZone: timeZone),
                         context: widgetString("Teams and matches not listed yet", locale: locale))
        case .nextUnlisted:
            return .init(lead: widgetString("Next match", locale: locale), value: widgetString("TBD", locale: locale),
                         context: widgetString("VLR has not listed the next match", locale: locale))
        }
    }

    private static func endedText(_ ended: EventEndedHero, hidden: Bool, locale: Locale, timeZone: TimeZone) -> Self {
        let pool = ended.prizePool.map { widgetString("%@ prize pool", locale: locale, $0) }
        return .init(lead: widgetString("Event ended", locale: locale), value: widgetDate(ended.end, locale: locale, timeZone: timeZone),
                     context: EventWidgetText.join([widgetString(hidden ? "Winner hidden" : "Winner not listed", locale: locale), pool]))
    }
}

struct EventWidgetRowText: Equatable {
    let day: String
    let time: String
    let left: String
    let right: String
    let round: String
    let leftScore: String?
    let rightScore: String?
    let loser: WidgetSide?
    let live: Bool
}

enum EventWidgetText {
    static func join(_ values: [String?]) -> String {
        values.compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: " · ")
    }

    static func name(_ participant: WidgetParticipant, locale: Locale) -> String {
        participant.team?.name ?? widgetString("TBD", locale: locale)
    }

    static func matchup(_ pair: WidgetMatchPair, locale: Locale) -> String {
        widgetString("%1$@ vs %2$@", locale: locale, name(pair.left, locale: locale), name(pair.right, locale: locale))
    }

    static func subtitle(_ value: EventWidgetSubtitle, locale: Locale, timeZone: TimeZone) -> String {
        switch value {
        case .starts(let day, let location): return join([widgetString("Starts %@", locale: locale, widgetDate(day, locale: locale,
            timeZone: timeZone)), location])
        case .ended(let day, let location): return join([widgetString("Ended %@", locale: locale, widgetDate(day, locale: locale,
            timeZone: timeZone)), location])
        case .stage(let stage, let location): return join([stage, location])
        case .dates(let bounds, let location):
            let start = widgetDate(bounds.start, locale: locale, timeZone: timeZone)
            let end = bounds.endInclusive.map { widgetDate($0, locale: locale, timeZone: timeZone) }
            return join([end.map { start + " – " + $0 } ?? start, location])
        }
    }

    static func heading(_ heading: EventMatchListHeading, locale: Locale) -> String {
        switch heading {
        case .today: widgetString("Today", locale: locale)
        case .upNext: widgetString("Up next", locale: locale)
        case .earlier: widgetString("Earlier", locale: locale)
        }
    }

    static func row(_ match: EventWidgetMatch, heading: EventMatchListHeading, now: Date, locale: Locale, timeZone: TimeZone) -> EventWidgetRowText {
        let phase: EventNeutralMatch.Phase
        let score: WidgetScore?
        switch match.phase {
        case .live(let value): phase = .live; score = value
        case .completed(let value): phase = .ended; score = value
        case .upcoming, .unknown: phase = .upcoming; score = nil
        }
        let neutral = row(.init(fixture: match.fixture, phase: phase), heading: heading, now: now, locale: locale, timeZone: timeZone)
        let loser: WidgetSide? = phase == .ended && score?.team != score?.opponent ? score.map { $0.team < $0.opponent ? .left : .right } : nil
        return .init(day: neutral.day, time: neutral.time, left: neutral.left, right: neutral.right, round: neutral.round,
                     leftScore: score.map { integer($0.team, locale: locale) }, rightScore: score.map { integer($0.opponent, locale: locale) },
                         loser: loser, live: phase == .live)
    }

    static func row(_ match: EventNeutralMatch, heading: EventMatchListHeading, now: Date, locale: Locale, timeZone: TimeZone) -> EventWidgetRowText {
        let clock = match.fixture.start.map { WidgetKickoff.row(start: $0, now: now, locale: locale, timeZone: timeZone) }
        let day: String
        if heading == .today {
            switch match.phase {
            case .ended: day = widgetString("Ended", locale: locale)
            case .live: day = widgetString("Live", locale: locale)
            case .upcoming: day = widgetString("Next", locale: locale)
            }
        } else { day = clock?.day ?? widgetDate(match.fixture.day, locale: locale, timeZone: timeZone) }
        return .init(day: day, time: clock?.time ?? widgetString("TBD", locale: locale),
            left: name(match.fixture.participants.left, locale: locale), right: name(match.fixture.participants.right, locale: locale),
                round: match.fixture.round,
            leftScore: nil, rightScore: nil, loser: nil, live: match.phase == .live)
    }

    static func integer(_ value: Int, locale: Locale, signed: Bool = false) -> String {
        let formatter = NumberFormatter()
        formatter.locale = locale
        formatter.numberStyle = .decimal
        formatter.usesGroupingSeparator = false
        if signed {
            formatter.positivePrefix = value > 0 ? "+" : ""
            formatter.negativePrefix = "−"
        }
        return formatter.string(from: NSNumber(value: value)) ?? String(value)
    }

    static func record(_ row: EventWidgetStanding, locale: Locale) -> String {
        integer(row.wins, locale: locale) + "–" + integer(row.losses, locale: locale)
    }
}
