import Foundation

struct WidgetFavoriteIDs: Codable, Equatable {
    let matchIds: [String]
    let teamIds: [String]
    let eventIds: [String]
    let playerIds: [String]

    static let empty = WidgetFavoriteIDs(matchIds: [], teamIds: [], eventIds: [], playerIds: [])

    init(matchIds: [String], teamIds: [String], eventIds: [String], playerIds: [String]) {
        self.matchIds = matchIds
        self.teamIds = teamIds
        self.eventIds = eventIds
        self.playerIds = playerIds
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        matchIds = try container.decodeIfPresent([String].self, forKey: .matchIds) ?? []
        teamIds = try container.decodeIfPresent([String].self, forKey: .teamIds) ?? []
        eventIds = try container.decodeIfPresent([String].self, forKey: .eventIds) ?? []
        playerIds = try container.decodeIfPresent([String].self, forKey: .playerIds) ?? []
    }
}

enum WidgetMatchStatus: String, Codable {
    case upcoming = "UPCOMING"
    case live = "LIVE"
}

struct UpcomingMatchesSnapshot: Codable, Equatable {
    let savedAtEpochMillis: Int64
    let hasFavorites: Bool
    let favorites: WidgetFavoriteIDs
    let spoilersHidden: Bool
    let matches: [UpcomingMatch]
    let theme: WidgetTheme

    var savedAt: Date {
        Date(timeIntervalSince1970: TimeInterval(savedAtEpochMillis) / 1_000)
    }

    init(
        savedAtEpochMillis: Int64,
        hasFavorites: Bool,
        favorites: WidgetFavoriteIDs,
        spoilersHidden: Bool,
        matches: [UpcomingMatch],
        theme: WidgetTheme
    ) {
        self.savedAtEpochMillis = savedAtEpochMillis
        self.hasFavorites = hasFavorites
        self.favorites = favorites
        self.spoilersHidden = spoilersHidden
        self.matches = matches
        self.theme = theme
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        savedAtEpochMillis = try container.decode(Int64.self, forKey: .savedAtEpochMillis)
        hasFavorites = try container.decode(Bool.self, forKey: .hasFavorites)
        favorites = try container.decodeIfPresent(WidgetFavoriteIDs.self, forKey: .favorites) ?? .empty
        spoilersHidden = try container.decodeIfPresent(Bool.self, forKey: .spoilersHidden) ?? false
        matches = try container.decode([UpcomingMatch].self, forKey: .matches)
        theme = try container.decode(WidgetTheme.self, forKey: .theme)
    }

    func withRefreshedMatches(_ matches: [UpcomingMatch], at date: Date) -> UpcomingMatchesSnapshot {
        UpcomingMatchesSnapshot(
            savedAtEpochMillis: Int64(date.timeIntervalSince1970 * 1_000),
            hasFavorites: hasFavorites,
            favorites: favorites,
            spoilersHidden: spoilersHidden,
            matches: matches.map { match in
                guard spoilersHidden else { return match }
                return match.withoutScores()
            },
            theme: theme
        )
    }

    func usingConfiguration(from source: UpcomingMatchesSnapshot) -> UpcomingMatchesSnapshot {
        UpcomingMatchesSnapshot(
            savedAtEpochMillis: savedAtEpochMillis,
            hasFavorites: source.hasFavorites,
            favorites: source.favorites,
            spoilersHidden: source.spoilersHidden,
            matches: source.spoilersHidden ? matches.map { $0.withoutScores() } : matches,
            theme: source.theme
        )
    }

    var sourceSignature: String {
        let fields = [
            String(savedAtEpochMillis),
            String(hasFavorites),
            String(spoilersHidden),
            favorites.matchIds.sorted().joined(separator: ","),
            favorites.teamIds.sorted().joined(separator: ","),
            favorites.eventIds.sorted().joined(separator: ","),
            favorites.playerIds.sorted().joined(separator: ","),
            String(theme.background),
            String(theme.surface),
            String(theme.accent),
            String(theme.content),
            String(theme.secondary),
            String(theme.border),
            String(theme.monospace),
        ]
        var hash: UInt64 = 14_695_981_039_346_656_037
        for byte in fields.joined(separator: "|").utf8 {
            hash ^= UInt64(byte)
            hash &*= 1_099_511_628_211
        }
        return String(hash, radix: 16)
    }
}

struct UpcomingMatch: Codable, Equatable, Identifiable {
    let id: String
    let event: String
    let team1: String
    let team2: String
    let startTimeEpochMillis: Int64?
    let status: WidgetMatchStatus
    let score1: Int?
    let score2: Int?
    let format: String
    let stage: String

    var startTime: Date? {
        startTimeEpochMillis.map { Date(timeIntervalSince1970: TimeInterval($0) / 1_000) }
    }

    init(
        id: String,
        event: String,
        team1: String,
        team2: String,
        startTimeEpochMillis: Int64?,
        status: WidgetMatchStatus,
        score1: Int?,
        score2: Int?,
        format: String,
        stage: String
    ) {
        self.id = id
        self.event = event
        self.team1 = team1
        self.team2 = team2
        self.startTimeEpochMillis = startTimeEpochMillis
        self.status = status
        self.score1 = score1
        self.score2 = score2
        self.format = format
        self.stage = stage
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(String.self, forKey: .id)
        event = try container.decode(String.self, forKey: .event)
        team1 = try container.decode(String.self, forKey: .team1)
        team2 = try container.decode(String.self, forKey: .team2)
        startTimeEpochMillis = try container.decodeIfPresent(Int64.self, forKey: .startTimeEpochMillis)
        status = try container.decodeIfPresent(WidgetMatchStatus.self, forKey: .status) ?? .upcoming
        score1 = try container.decodeIfPresent(Int.self, forKey: .score1)
        score2 = try container.decodeIfPresent(Int.self, forKey: .score2)
        format = try container.decodeIfPresent(String.self, forKey: .format) ?? ""
        stage = try container.decodeIfPresent(String.self, forKey: .stage) ?? ""
    }

    func withoutScores() -> UpcomingMatch {
        UpcomingMatch(
            id: id,
            event: event,
            team1: team1,
            team2: team2,
            startTimeEpochMillis: startTimeEpochMillis,
            status: status,
            score1: nil,
            score2: nil,
            format: format,
            stage: stage
        )
    }
}

struct WidgetTheme: Codable, Equatable {
    let background: Int64
    let surface: Int64
    let accent: Int64
    let content: Int64
    let secondary: Int64
    let border: Int64
    let monospace: Bool

    static let preview = WidgetTheme(
        background: 0xFFFFFFFF,
        surface: 0xFFF5F5F5,
        accent: 0xFF7C3AED,
        content: 0xFF000000,
        secondary: 0xFF404040,
        border: 0xFFE5E5E5,
        monospace: false
    )
}

struct RefreshedSnapshotEnvelope: Codable, Equatable {
    let sourceSignature: String
    let snapshot: UpcomingMatchesSnapshot
}
