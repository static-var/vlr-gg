import Foundation

struct WidgetFavoriteTeam: Codable, Equatable, Identifiable {
    let id: String
    let name: String
    let tag: String?
    let logo: String?

    init(id: String, name: String, tag: String? = nil, logo: String? = nil) {
        self.id = id
        self.name = name
        self.tag = tag
        self.logo = logo
    }
}

struct WidgetFavoriteEvent: Codable, Equatable, Identifiable, Sendable {
    let id: String
    let name: String
    let logo: String?

    init(id: String, name: String, logo: String? = nil) {
        self.id = id
        self.name = name
        self.logo = logo
    }
}

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
    case completed = "COMPLETED"

    var localizedDisplayName: String {
        switch self {
        case .upcoming: String(localized: "UPCOMING")
        case .live: String(localized: "LIVE")
        case .completed: String(localized: "Final").uppercased()
        }
    }
}

struct UpcomingMatchesSnapshot: Codable, Equatable {
    var savedAtEpochMillis: Int64
    var clientId: String
    var hasFavorites: Bool
    var favorites: WidgetFavoriteIDs
    var spoilersHidden: Bool
    var matches: [UpcomingMatch]
    var results: [UpcomingMatch]
    var theme: WidgetTheme
    var favoriteTeams: [WidgetFavoriteTeam]
    var favoriteEvents: [WidgetFavoriteEvent]

    var savedAt: Date {
        Date(timeIntervalSince1970: TimeInterval(savedAtEpochMillis) / 1_000)
    }

    /// Cached results expire at the normal refresh interval or the next scheduled start.
    var refreshDate: Date {
        let interval: TimeInterval = matches.contains { $0.status == .live } ? 15 * 60 : 30 * 60
        let nextStart = matches.filter { $0.status == .upcoming }
            .compactMap(\.startTime).filter { $0 > savedAt }.min()
        return min(savedAt.addingTimeInterval(interval), nextStart ?? .distantFuture)
    }

    init(
        savedAtEpochMillis: Int64,
        clientId: String = "",
        hasFavorites: Bool,
        favorites: WidgetFavoriteIDs,
        spoilersHidden: Bool,
        matches: [UpcomingMatch],
        results: [UpcomingMatch] = [],
        theme: WidgetTheme,
        favoriteTeams: [WidgetFavoriteTeam] = [],
        favoriteEvents: [WidgetFavoriteEvent] = []
    ) {
        self.savedAtEpochMillis = savedAtEpochMillis
        self.clientId = clientId
        self.hasFavorites = hasFavorites
        self.favorites = favorites
        self.spoilersHidden = spoilersHidden
        self.matches = matches
        self.results = results
        self.theme = theme
        self.favoriteTeams = favoriteTeams
        self.favoriteEvents = favoriteEvents
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        savedAtEpochMillis = try container.decode(Int64.self, forKey: .savedAtEpochMillis)
        clientId = try container.decodeIfPresent(String.self, forKey: .clientId) ?? ""
        hasFavorites = try container.decode(Bool.self, forKey: .hasFavorites)
        favorites = try container.decodeIfPresent(WidgetFavoriteIDs.self, forKey: .favorites) ?? .empty
        spoilersHidden = try container.decodeIfPresent(Bool.self, forKey: .spoilersHidden) ?? false
        matches = try container.decode([UpcomingMatch].self, forKey: .matches)
        results = try container.decodeIfPresent([UpcomingMatch].self, forKey: .results) ?? []
        theme = try container.decode(WidgetTheme.self, forKey: .theme)
        favoriteTeams = try container.decodeIfPresent([WidgetFavoriteTeam].self, forKey: .favoriteTeams) ?? []
        favoriteEvents = try container.decodeIfPresent([WidgetFavoriteEvent].self, forKey: .favoriteEvents) ?? []
    }

    func withRefreshedMatches(_ matches: [UpcomingMatch], results: [UpcomingMatch] = [], at date: Date) -> UpcomingMatchesSnapshot {
        var copy = self
        copy.savedAtEpochMillis = Int64(date.timeIntervalSince1970 * 1_000)
        copy.matches = spoilersHidden ? matches.map { $0.withoutScores() } : matches
        copy.results = spoilersHidden ? results.map { $0.withoutScores() } : results
        return copy
    }

    func usingConfiguration(from source: UpcomingMatchesSnapshot) -> UpcomingMatchesSnapshot {
        var copy = source
        copy.savedAtEpochMillis = savedAtEpochMillis
        copy.matches = source.spoilersHidden ? matches.map { $0.withoutScores() } : matches
        copy.results = source.spoilersHidden ? results.map { $0.withoutScores() } : results
        return copy
    }

    var sourceSignature: String {
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.sortedKeys]
        let fields = [
            String(savedAtEpochMillis),
            clientId,
            String(hasFavorites),
            String(spoilersHidden),
            favorites.matchIds.sorted().joined(separator: ","),
            favorites.teamIds.sorted().joined(separator: ","),
            favorites.eventIds.sorted().joined(separator: ","),
            favorites.playerIds.sorted().joined(separator: ","),
            String(data: (try? encoder.encode(favoriteTeams.sorted { $0.id < $1.id })) ?? Data(), encoding: .utf8) ?? "",
            String(data: (try? encoder.encode(favoriteEvents.sorted { $0.id < $1.id })) ?? Data(), encoding: .utf8) ?? "",
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
    let team1Id: String?
    let team2Id: String?
    let team1Tag: String?
    let team2Tag: String?

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
        stage: String,
        team1Id: String? = nil,
        team2Id: String? = nil,
        team1Tag: String? = nil,
        team2Tag: String? = nil
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
        self.team1Id = team1Id
        self.team2Id = team2Id
        self.team1Tag = team1Tag
        self.team2Tag = team2Tag
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
        team1Id = try container.decodeIfPresent(String.self, forKey: .team1Id)
        team2Id = try container.decodeIfPresent(String.self, forKey: .team2Id)
        team1Tag = try container.decodeIfPresent(String.self, forKey: .team1Tag)
        team2Tag = try container.decodeIfPresent(String.self, forKey: .team2Tag)
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
            stage: stage,
            team1Id: team1Id,
            team2Id: team2Id,
            team1Tag: team1Tag,
            team2Tag: team2Tag
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
