import Foundation

/// The single favorite match the Lock Screen widget shows, in priority order.
enum LockScreenMatchState: Equatable {
    case live(UpcomingMatch)
    case upcoming(UpcomingMatch)
    case completed(UpcomingMatch, next: UpcomingMatch?)
    case noMatches
    case noFavorites
    case needsApp

    static let completedWindow: TimeInterval = 24 * 60 * 60

    static func make(snapshot: UpcomingMatchesSnapshot?, now: Date) -> LockScreenMatchState {
        guard let snapshot else { return .needsApp }
        guard snapshot.hasFavorites else { return .noFavorites }
        if let live = snapshot.matches.first(where: { $0.status == .live }) { return .live(live) }
        let upcoming = snapshot.matches.first { $0.status == .upcoming }
        let recent = snapshot.results
            .filter { result in
                guard let start = result.startTime else { return false }
                return start <= now && now.timeIntervalSince(start) < completedWindow
            }
            .max { ($0.startTime ?? .distantPast) < ($1.startTime ?? .distantPast) }
        if let recent { return .completed(recent, next: upcoming) }
        if let upcoming { return .upcoming(upcoming) }
        return .noMatches
    }

    /// The next date at which the state itself changes without new data: a completed result expiring, or an upcoming kickoff.
    func expiry(now: Date) -> Date? {
        switch self {
        case .completed(let result, _):
            return result.startTime?.addingTimeInterval(Self.completedWindow)
        case .upcoming(let match):
            guard let start = match.startTime, start > now else { return nil }
            return start
        case .live, .noMatches, .noFavorites, .needsApp:
            return nil
        }
    }

    var url: URL {
        switch self {
        case .live(let match), .upcoming(let match), .completed(let match, _):
            VLRWidgetContract.matchURL(id: match.id)
        case .noMatches, .noFavorites, .needsApp:
            VLRWidgetContract.appURL
        }
    }

    var symbol: String {
        switch self {
        case .live: "bolt.fill"
        case .upcoming: "clock"
        case .completed: "flag.checkered"
        case .noFavorites: "star"
        case .noMatches: "calendar"
        case .needsApp: "gamecontroller"
        }
    }
}
