import Foundation

/// Localized copy for the Lock Screen widget, built without I/O so tests can pin every string.
struct LockScreenMatchText: Equatable {
    enum Side: Equatable {
        case team1
        case team2
    }

    struct Scoreline: Equatable {
        let team1: String
        let center: String
        let team2: String
        let winner: Side?
    }

    enum Kickoff: Equatable {
        case countdown(to: Date)
        case text(String)
    }

    enum Rectangular: Equatable {
        case live(Scoreline, caption: String)
        case upcoming(matchup: String, kickoff: Kickoff, caption: String)
        case completed(Scoreline, status: String, caption: String, next: String?)
        case message(title: String, caption: String)
    }

    let inline: String
    let rectangular: Rectangular

    init(
        state: LockScreenMatchState,
        now: Date,
        locale: Locale,
        timeZone: TimeZone,
        spoilersHidden: Bool,
        teamCode: @escaping (_ teamID: String?) -> String? = { MatchActivityTeamCodes.code(for: $0) }
    ) {
        let copy = LockScreenCopy(now: now, locale: locale, timeZone: timeZone, spoilersHidden: spoilersHidden, teamCode: teamCode)
        switch state {
        case .live(let match):
            let live = widgetString("Live", locale: locale)
            inline = "\(copy.inlineScore(match)) · \(live)"
            let caption = ["● \(live)", copy.mapProgress(match), match.event].filter { !$0.isEmpty }
            rectangular = .live(copy.scoreline(match), caption: caption.joined(separator: " · "))
        case .upcoming(let match):
            inline = "\(copy.matchup(match)) · \(copy.kickoff(match.startTime))"
            let caption = [match.stage, match.event].filter { !$0.isEmpty }.joined(separator: " · ")
            rectangular = .upcoming(matchup: copy.matchup(match), kickoff: copy.countdown(match.startTime), caption: caption)
        case .completed(let match, let next):
            let final = widgetString("Final", locale: locale)
            inline = "\(copy.inlineScore(match)) · \(final)"
            let caption = [match.event, match.stage].filter { !$0.isEmpty }.joined(separator: " · ")
            let nextLine = next.map {
                widgetString("Next: %@", locale: locale, "\(copy.matchup($0)) · \(copy.kickoff($0.startTime))")
            }
            rectangular = .completed(copy.scoreline(match), status: final, caption: caption, next: nextLine)
        case .noFavorites:
            inline = widgetString("Pick favorites", locale: locale)
            rectangular = .message(
                title: widgetString("No favorites.", locale: locale),
                caption: widgetString("Favorite a team, player, event, or match in Val Esports.", locale: locale)
            )
        case .noMatches:
            inline = widgetString("No matches", locale: locale)
            rectangular = .message(
                title: widgetString("No matches scheduled.", locale: locale),
                caption: widgetString("Your favorites return here when they play.", locale: locale)
            )
        case .needsApp:
            inline = widgetString("Open Val Esports", locale: locale)
            rectangular = .message(
                title: widgetString("Open the app.", locale: locale),
                caption: widgetString("Open Val Esports once to connect your favorites.", locale: locale)
            )
        }
    }

    static func teamCode(name: String, tag: String?, id: String?, lookup: (String?) -> String?) -> String {
        if let tag = tag?.trimmingCharacters(in: .whitespacesAndNewlines), !tag.isEmpty { return tag.uppercased() }
        if let code = lookup(id) { return code }
        return String(name.trimmingCharacters(in: .whitespacesAndNewlines).prefix(3)).uppercased()
    }
}

private struct LockScreenCopy {
    let now: Date
    let locale: Locale
    let timeZone: TimeZone
    let spoilersHidden: Bool
    let teamCode: (String?) -> String?

    func codes(_ match: UpcomingMatch) -> (String, String) {
        (
            LockScreenMatchText.teamCode(name: match.team1, tag: match.team1Tag, id: match.team1Id, lookup: teamCode),
            LockScreenMatchText.teamCode(name: match.team2, tag: match.team2Tag, id: match.team2Id, lookup: teamCode)
        )
    }

    func scores(_ match: UpcomingMatch) -> (Int, Int)? {
        guard !spoilersHidden, let score1 = match.score1, let score2 = match.score2 else { return nil }
        return (score1, score2)
    }

    func matchup(_ match: UpcomingMatch) -> String {
        let (team1, team2) = codes(match)
        return widgetString("%1$@ vs %2$@", locale: locale, team1, team2)
    }

    func inlineScore(_ match: UpcomingMatch) -> String {
        guard let (score1, score2) = scores(match) else { return matchup(match) }
        let (team1, team2) = codes(match)
        return "\(team1) \(score1)–\(score2) \(team2)"
    }

    func scoreline(_ match: UpcomingMatch) -> LockScreenMatchText.Scoreline {
        let (team1, team2) = codes(match)
        guard let (score1, score2) = scores(match) else {
            return .init(team1: team1, center: widgetString("vs", locale: locale), team2: team2, winner: nil)
        }
        let winner: LockScreenMatchText.Side? = score1 == score2 ? nil : score1 > score2 ? .team1 : .team2
        return .init(team1: team1, center: "\(score1)–\(score2)", team2: team2, winner: winner)
    }

    /// The map in play, counted from the series score; empty when scores are hidden or the format is unknown.
    func mapProgress(_ match: UpcomingMatch) -> String {
        guard let (score1, score2) = scores(match),
              match.format.hasPrefix("BO"), let total = Int(match.format.dropFirst(2)), total > 1 else { return "" }
        return widgetString("Map %1$lld of %2$lld", locale: locale, Int64(min(score1 + score2 + 1, total)), Int64(total))
    }

    func kickoff(_ start: Date?) -> String {
        guard let start else { return widgetString("TBD", locale: locale) }
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone
        guard start > now, !calendar.isDate(start, inSameDayAs: now) else {
            return WidgetKickoff.make(start: start, now: now, locale: locale, timeZone: timeZone).hero(locale: locale)
        }
        let row = WidgetKickoff.row(start: start, now: now, locale: locale, timeZone: timeZone)
        return "\(row.day) \(row.time)"
    }

    func countdown(_ start: Date?) -> LockScreenMatchText.Kickoff {
        guard let start, start > now, start.timeIntervalSince(now) <= 24 * 60 * 60 else { return .text(kickoff(start)) }
        return .countdown(to: start)
    }
}
