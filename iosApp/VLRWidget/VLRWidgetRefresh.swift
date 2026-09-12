import Foundation
import os

enum WidgetRefreshError: Error, Equatable {
    case invalidResponse
    case httpStatus(Int)
}

struct WidgetAPIClient {
    typealias Loader = (URLRequest) async throws -> (Data, HTTPURLResponse)

    private let baseURL: URL
    private let loader: Loader
    private let authorization: String?

    init(baseURL: URL, authorization: String?, loader: @escaping Loader) {
        self.baseURL = baseURL
        self.authorization = authorization?.trimmingCharacters(in: .whitespacesAndNewlines)
        self.loader = loader
    }

    static func live(authorization: String?) -> WidgetAPIClient {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.timeoutIntervalForRequest = 8
        configuration.timeoutIntervalForResource = 12
        configuration.waitsForConnectivity = false
        let session = URLSession(configuration: configuration)
        return WidgetAPIClient(
            baseURL: URL(string: "https://vlr-scraper.akhilnarang.dev")!,
            authorization: authorization
        ) { request in
            let (data, response) = try await session.data(for: request)
            guard let response = response as? HTTPURLResponse else {
                throw WidgetRefreshError.invalidResponse
            }
            return (data, response)
        }
    }

    func get<Value: Decodable>(_ path: String, as type: Value.Type = Value.self) async throws -> Value {
        guard let url = URL(string: path, relativeTo: baseURL) else {
            throw WidgetRefreshError.invalidResponse
        }
        var request = URLRequest(url: url)
        request.timeoutInterval = 8
        request.setValue("dev.staticvar.vlr", forHTTPHeaderField: "app-name")
        if let authorization, !authorization.isEmpty {
            request.setValue(authorization, forHTTPHeaderField: "Authorization")
        }

        let (data, response) = try await loader(request)
        guard (200..<300).contains(response.statusCode) else {
            throw WidgetRefreshError.httpStatus(response.statusCode)
        }
        return try JSONDecoder().decode(Value.self, from: data)
    }
}

struct WidgetSnapshotRepository {
    private static let logger = Logger(subsystem: "dev.staticvar.vlr.ios.widget", category: "Snapshot")

    let sourceURL: URL?
    let refreshedURL: URL?

    init(
        sourceURL: URL? = VLRWidgetContract.snapshotURL,
        refreshedURL: URL? = VLRWidgetContract.refreshedSnapshotURL
    ) {
        self.sourceURL = sourceURL
        self.refreshedURL = refreshedURL
    }

    func loadSource() -> UpcomingMatchesSnapshot? {
        decode(UpcomingMatchesSnapshot.self, from: sourceURL)
    }

    func loadBestSnapshot() -> UpcomingMatchesSnapshot? {
        guard let source = loadSource() else { return nil }
        guard
            let envelope = decode(RefreshedSnapshotEnvelope.self, from: refreshedURL),
            envelope.sourceSignature == source.sourceSignature,
            envelope.snapshot.savedAtEpochMillis >= source.savedAtEpochMillis
        else {
            return source
        }
        return envelope.snapshot.usingConfiguration(from: source)
    }

    @discardableResult
    func store(_ refreshed: UpcomingMatchesSnapshot, for source: UpcomingMatchesSnapshot) -> Bool {
        guard
            let refreshedURL,
            loadSource()?.sourceSignature == source.sourceSignature
        else {
            return false
        }

        do {
            let envelope = RefreshedSnapshotEnvelope(
                sourceSignature: source.sourceSignature,
                snapshot: refreshed.usingConfiguration(from: source)
            )
            let encoder = JSONEncoder()
            encoder.outputFormatting = [.sortedKeys]
            try encoder.encode(envelope).write(to: refreshedURL, options: .atomic)
            return true
        } catch {
            Self.logger.error("Unable to store refreshed widget data: \(error.localizedDescription, privacy: .public)")
            return false
        }
    }

    private func decode<Value: Decodable>(_ type: Value.Type, from url: URL?) -> Value? {
        guard let url, let data = try? Data(contentsOf: url) else { return nil }
        return try? JSONDecoder().decode(Value.self, from: data)
    }
}

struct WidgetRefreshService {
    private static let maximumFavoriteIDsPerKind = 12
    private static let maximumMatches = 12
    private static let requestBatchSize = 4

    private let api: WidgetAPIClient

    init(api: WidgetAPIClient = .live(authorization: GeneratedBuildConfig.authToken)) {
        self.api = api
    }

    func refresh(source: UpcomingMatchesSnapshot, now: Date = .now) async throws -> UpcomingMatchesSnapshot {
        guard source.hasFavorites else {
            return source.withRefreshedMatches([], at: now)
        }

        let favoriteMatchIDs = source.favorites.matchIds.filter { !$0.isEmpty }.unique()
        let directTeamIDs = source.favorites.teamIds.filter { !$0.isEmpty }.unique()
        let favoriteEventIDs = source.favorites.eventIds.filter { !$0.isEmpty }.unique()
        let favoritePlayerIDs = source.favorites.playerIds.filter { !$0.isEmpty }.unique()
        let playerLookupIDs = bounded(favoritePlayerIDs)

        async let overviewRequest: [RemoteMatchPreview] = api.get("/api/v1/matches/")
        async let playerRequest: [(String, RemotePlayerDetails)] = fetchMany(
            playerLookupIDs,
            path: { "/api/v1/player/\($0)" },
            as: RemotePlayerDetails.self
        )
        let (overview, players) = try await (overviewRequest, playerRequest)

        let playerTeamIDs = players.compactMap(\.1.currentTeam?.id)
        let allResolvedTeamIDs = (directTeamIDs + playerTeamIDs).unique()
        let teamLookupIDs = bounded(allResolvedTeamIDs)
        let favoriteTeamIDSet = Set(directTeamIDs + playerTeamIDs)
        let favoriteMatchIDSet = Set(favoriteMatchIDs)
        let favoriteEventIDSet = Set(favoriteEventIDs)

        var freshMatches: [UpcomingMatch] = []
        var knownOverviewIDs = Set<String>()
        var unresolvedStatusIDs = Set<String>()
        var terminalMatchIDs = Set<String>()

        for preview in overview {
            let isFavorite = favoriteMatchIDSet.contains(preview.id)
                || preview.teamIDs.contains(where: favoriteTeamIDSet.contains)
                || favoriteEventIDSet.contains(preview.eventId)
            guard isFavorite else { continue }

            switch preview.remoteStatus {
            case .upcoming, .live:
                knownOverviewIDs.insert(preview.id)
                if let match = preview.widgetMatch {
                    freshMatches.append(match)
                }
            case .completed:
                knownOverviewIDs.insert(preview.id)
                terminalMatchIDs.insert(preview.id)
            case .unknown:
                unresolvedStatusIDs.insert(preview.id)
            }
        }

        let missingDirectMatchIDs = favoriteMatchIDs.filter { !knownOverviewIDs.contains($0) }
        let overviewEnrichmentIDs = freshMatches.map(\.id)
        let matchLookupIDs = bounded(missingDirectMatchIDs + overviewEnrichmentIDs)
        async let teamRequest: [(String, RemoteTeamDetails)] = fetchMany(
            teamLookupIDs,
            path: { "/api/v1/team/\($0)" },
            as: RemoteTeamDetails.self
        )
        async let eventRequest: [(String, RemoteEventDetails)] = fetchMany(
            bounded(favoriteEventIDs),
            path: { "/api/v1/events/\($0)" },
            as: RemoteEventDetails.self
        )
        async let matchRequest: [(String, RemoteMatchDetails)] = fetchMany(
            matchLookupIDs,
            path: { "/api/v1/matches/\($0)" },
            as: RemoteMatchDetails.self
        )

        let (teams, events, directMatches) = try await (teamRequest, eventRequest, matchRequest)

        for (_, details) in teams {
            freshMatches.append(contentsOf: details.upcoming.compactMap { $0.widgetMatch(teamName: details.name) })
        }

        for (_, details) in events {
            for match in details.matches {
                switch match.remoteStatus {
                case .upcoming, .live:
                    if let candidate = match.widgetMatch(event: details.title) {
                        freshMatches.append(candidate)
                    }
                case .unknown:
                    unresolvedStatusIDs.insert(match.id)
                case .completed:
                    terminalMatchIDs.insert(match.id)
                    break
                }
            }
        }

        for (requestedID, details) in directMatches {
            switch details.event.remoteStatus {
            case .upcoming, .live:
                if let match = details.widgetMatch(id: requestedID) {
                    freshMatches.append(match)
                }
            case .unknown:
                unresolvedStatusIDs.insert(requestedID)
            case .completed:
                terminalMatchIDs.insert(requestedID)
                break
            }
        }

        let lookupsWereCapped = playerLookupIDs.count < favoritePlayerIDs.count
            || teamLookupIDs.count < allResolvedTeamIDs.count
            || bounded(favoriteEventIDs).count < favoriteEventIDs.count
            || matchLookupIDs.count < (missingDirectMatchIDs + overviewEnrichmentIDs).unique().count
        freshMatches.append(contentsOf: source.matches.filter { match in
            unresolvedStatusIDs.contains(match.id)
                || (lookupsWereCapped && !terminalMatchIDs.contains(match.id))
        })
        let normalized = normalize(freshMatches.filter { !terminalMatchIDs.contains($0.id) })
        return source.withRefreshedMatches(normalized, at: now)
    }

    private func bounded(_ ids: [String]) -> [String] {
        Array(ids.lazy.filter { !$0.isEmpty }.unique().prefix(Self.maximumFavoriteIDsPerKind))
    }

    private func fetchMany<Value: Decodable>(
        _ ids: [String],
        path: @escaping (String) -> String,
        as type: Value.Type
    ) async throws -> [(String, Value)] {
        var results: [(String, Value)] = []
        var start = 0
        while start < ids.count {
            let end = min(start + Self.requestBatchSize, ids.count)
            let batch = Array(ids[start..<end])
            let values = try await withThrowingTaskGroup(of: (String, Value).self) { group in
                for id in batch {
                    group.addTask {
                        (id, try await api.get(path(id), as: type))
                    }
                }
                var batchResults: [(String, Value)] = []
                for try await value in group {
                    batchResults.append(value)
                }
                return batchResults
            }
            results.append(contentsOf: values)
            start = end
        }
        return results
    }

    private func normalize(_ matches: [UpcomingMatch]) -> [UpcomingMatch] {
        var byID: [String: UpcomingMatch] = [:]
        for match in matches where !match.id.isEmpty {
            if let existing = byID[match.id] {
                byID[match.id] = preferred(existing, match)
            } else {
                byID[match.id] = match
            }
        }
        return Array(byID.values)
            .sorted { left, right in
                if left.status != right.status {
                    return left.status == .live
                }
                let leftTime = left.startTimeEpochMillis ?? Int64.max
                let rightTime = right.startTimeEpochMillis ?? Int64.max
                return leftTime == rightTime ? left.id < right.id : leftTime < rightTime
            }
            .prefix(Self.maximumMatches)
            .map { $0 }
    }

    private func preferred(_ left: UpcomingMatch, _ right: UpcomingMatch) -> UpcomingMatch {
        if left.status != right.status {
            return right.status == .live ? right : left
        }
        let rightHasTeamPair = !right.team1.isEmpty && !right.team2.isEmpty
        var selectedScore1 = rightHasTeamPair ? right.score1 : left.score1
        var selectedScore2 = rightHasTeamPair ? right.score2 : left.score2
        if rightHasTeamPair, right.score1 == nil, right.score2 == nil {
            if right.team1 == left.team1, right.team2 == left.team2 {
                selectedScore1 = left.score1
                selectedScore2 = left.score2
            } else if right.team1 == left.team2, right.team2 == left.team1 {
                selectedScore1 = left.score2
                selectedScore2 = left.score1
            }
        }
        return UpcomingMatch(
            id: left.id,
            event: right.event.isEmpty ? left.event : right.event,
            team1: rightHasTeamPair ? right.team1 : left.team1,
            team2: rightHasTeamPair ? right.team2 : left.team2,
            startTimeEpochMillis: right.startTimeEpochMillis ?? left.startTimeEpochMillis,
            status: right.status,
            score1: selectedScore1,
            score2: selectedScore2,
            format: right.format.isEmpty ? left.format : right.format,
            stage: right.stage.isEmpty ? left.stage : right.stage
        )
    }
}

private enum RemoteMatchStatus {
    case upcoming
    case live
    case completed
    case unknown

    init(_ value: String?) {
        switch value?.lowercased() {
        case "upcoming": self = .upcoming
        case "live", "ongoing": self = .live
        case "completed", "final": self = .completed
        default: self = .unknown
        }
    }

    var widgetStatus: WidgetMatchStatus? {
        switch self {
        case .upcoming: return .upcoming
        case .live: return .live
        case .completed, .unknown: return nil
        }
    }
}

private struct RemoteTeam: Decodable {
    let id: String?
    let name: String
    let score: Int?
}

private struct RemoteMatchPreview: Decodable {
    let id: String
    let event: String
    let series: String
    let status: String?
    let team1: RemoteTeam
    let team2: RemoteTeam
    let time: String?
    let eventId: String

    enum CodingKeys: String, CodingKey {
        case id, event, series, status, team1, team2, time
        case eventId = "event_id"
    }

    var teamIDs: [String] { [team1.id, team2.id].compactMap { $0 } }
    var remoteStatus: RemoteMatchStatus { RemoteMatchStatus(status) }

    var widgetMatch: UpcomingMatch? {
        guard let status = remoteStatus.widgetStatus else { return nil }
        return UpcomingMatch(
            id: id,
            event: event,
            team1: team1.name,
            team2: team2.name,
            startTimeEpochMillis: epochMillis(time),
            status: status,
            score1: team1.score,
            score2: team2.score,
            format: normalizedMatchFormat(series),
            stage: ""
        )
    }
}

private struct RemotePlayerTeam: Decodable {
    let id: String?
}

private struct RemotePlayerDetails: Decodable {
    let currentTeam: RemotePlayerTeam?

    enum CodingKeys: String, CodingKey {
        case currentTeam = "current_team"
    }
}

private struct RemoteTeamMatch: Decodable {
    let id: String
    let event: String
    let stage: String
    let opponent: String
    let date: String?

    func widgetMatch(teamName: String) -> UpcomingMatch? {
        guard !id.isEmpty else { return nil }
        let detail = splitFormatAndStage(stage)
        return UpcomingMatch(
            id: id,
            event: event,
            team1: teamName,
            team2: opponent,
            startTimeEpochMillis: epochMillis(date),
            status: .upcoming,
            score1: nil,
            score2: nil,
            format: detail.format,
            stage: detail.stage
        )
    }
}

private struct RemoteTeamDetails: Decodable {
    let name: String
    let upcoming: [RemoteTeamMatch]
}

private struct RemoteEventTeam: Decodable {
    let name: String
    let score: Int?
}

private struct RemoteEventMatch: Decodable {
    let id: String
    let time: String
    let date: String
    let status: String?
    let teams: [RemoteEventTeam]
    let round: String
    let stage: String

    var remoteStatus: RemoteMatchStatus { RemoteMatchStatus(status) }

    func widgetMatch(event: String) -> UpcomingMatch? {
        guard let status = remoteStatus.widgetStatus, teams.count >= 2 else { return nil }
        return UpcomingMatch(
            id: id,
            event: event,
            team1: teams[0].name,
            team2: teams[1].name,
            startTimeEpochMillis: epochMillis(date: date, time: time),
            status: status,
            score1: teams[0].score,
            score2: teams[1].score,
            format: stage,
            stage: round
        )
    }
}

private struct RemoteEventDetails: Decodable {
    let title: String
    let matches: [RemoteEventMatch]
}

private struct RemoteMatchEvent: Decodable {
    let name: String?
    let series: String
    let stage: String
    let date: String?
    let status: String?

    var remoteStatus: RemoteMatchStatus { RemoteMatchStatus(status) }
}

private struct RemoteMatchDetails: Decodable {
    let event: RemoteMatchEvent
    let teams: [RemoteTeam]
    let score: String?
    let note: String?
    let mapCount: Int?

    enum CodingKeys: String, CodingKey {
        case event, teams, score, note
        case mapCount = "map_count"
    }

    func widgetMatch(id: String) -> UpcomingMatch? {
        guard let status = event.remoteStatus.widgetStatus, teams.count >= 2 else { return nil }
        let parsedScore = parseScore(score)
        return UpcomingMatch(
            id: id,
            event: event.name ?? "",
            team1: teams[0].name,
            team2: teams[1].name,
            startTimeEpochMillis: epochMillis(event.date),
            status: status,
            score1: parsedScore?.0 ?? teams[0].score,
            score2: parsedScore?.1 ?? teams[1].score,
            format: plannedMatchFormat(
                status: event.remoteStatus,
                note: note,
                mapCount: mapCount,
                series: event.series
            ),
            stage: event.stage
        )
    }
}

private func epochMillis(_ value: String?) -> Int64? {
    guard let value, !value.isEmpty else { return nil }
    let formatter = ISO8601DateFormatter()
    formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    let date = formatter.date(from: value) ?? ISO8601DateFormatter().date(from: value)
    return date.map { Int64($0.timeIntervalSince1970 * 1_000) }
}

private func epochMillis(date: String, time: String) -> Int64? {
    epochMillis("\(date)T\(time)Z")
}

private func splitFormatAndStage(_ value: String) -> (format: String, stage: String) {
    for separator in ["–", "⋅", ":"] {
        let components = value.components(separatedBy: separator)
        if components.count > 1 {
            return (
                components[0].trimmingCharacters(in: .whitespacesAndNewlines),
                components.dropFirst().joined(separator: separator).trimmingCharacters(in: .whitespacesAndNewlines)
            )
        }
    }
    return (value, "")
}

private func normalizedMatchFormat(_ value: String) -> String {
    let compact = value.replacingOccurrences(of: " ", with: "").uppercased()
    guard compact.hasPrefix("BO"), Int(compact.dropFirst(2)) != nil else { return "" }
    return compact
}

private func plannedMatchFormat(
    status: RemoteMatchStatus,
    note: String?,
    mapCount: Int?,
    series: String
) -> String {
    if let bestOf = explicitBestOf(note) {
        return "BO\(bestOf)"
    }
    if status == .upcoming || status == .live,
       let mapCount,
       [1, 3, 5].contains(mapCount) {
        return "BO\(mapCount)"
    }
    return normalizedMatchFormat(series)
}

private func explicitBestOf(_ value: String?) -> Int? {
    guard let value else { return nil }
    let pattern = #"\b(?:bo|best(?:\s*-\s*|\s+)of)\s*[-:]?\s*(\d+)\b"#
    guard
        let expression = try? NSRegularExpression(pattern: pattern, options: .caseInsensitive),
        let match = expression.firstMatch(in: value, range: NSRange(value.startIndex..., in: value)),
        let range = Range(match.range(at: 1), in: value),
        let count = Int(value[range]),
        count > 0,
        count.isMultiple(of: 2) == false
    else {
        return nil
    }
    return count
}

private func parseScore(_ value: String?) -> (Int, Int)? {
    guard let value else { return nil }
    let parts = value.split(whereSeparator: { $0 == ":" || $0 == "-" })
    guard parts.count == 2, let first = Int(parts[0]), let second = Int(parts[1]) else { return nil }
    return (first, second)
}

private extension Sequence where Element: Hashable {
    func unique() -> [Element] {
        var seen = Set<Element>()
        return filter { seen.insert($0).inserted }
    }
}
