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
            baseURL: URL(string: "https://val-esports-backend.akhilnarang.dev")!,
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
        let preferredLanguages = Locale.preferredLanguages
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
        if !preferredLanguages.isEmpty {
            request.setValue(preferredLanguages.joined(separator: ","), forHTTPHeaderField: "Accept-Language")
        }
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
        return loadRefreshedSnapshot(for: source) ?? source
    }

    func loadRefreshedSnapshot(for source: UpcomingMatchesSnapshot) -> UpcomingMatchesSnapshot? {
        guard
            let envelope = decode(RefreshedSnapshotEnvelope.self, from: refreshedURL),
            envelope.sourceSignature == source.sourceSignature,
            envelope.snapshot.savedAtEpochMillis >= source.savedAtEpochMillis
        else {
            return nil
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

enum WidgetRefreshOutcome: Equatable {
    case available(snapshot: UpcomingMatchesSnapshot, refreshAfter: Date)
    case retry(snapshot: UpcomingMatchesSnapshot?, refreshAfter: Date)

    var snapshot: UpcomingMatchesSnapshot? {
        switch self {
        case .available(let snapshot, _): snapshot
        case .retry(let snapshot, _): snapshot
        }
    }

    var refreshDate: Date {
        switch self {
        case .available(_, let refreshAfter), .retry(_, let refreshAfter): refreshAfter
        }
    }
}

/// Shares an in-flight refresh across widget families without reusing another configuration's result.
actor WidgetRefreshCoordinator {
    static let shared = WidgetRefreshCoordinator()
    private static let retryInterval: TimeInterval = 5 * 60

    private var pending: [String: Task<WidgetRefreshOutcome, Never>] = [:]
    private var retryAfter: [String: Date] = [:]

    func refresh(repository: WidgetSnapshotRepository, service: WidgetRefreshService, now: Date) async -> WidgetRefreshOutcome {
        guard let source = repository.loadSource() else {
            return .retry(snapshot: nil, refreshAfter: now.addingTimeInterval(Self.retryInterval))
        }
        guard source.hasFavorites else {
            return .available(
                snapshot: source,
                refreshAfter: max(source.refreshDate, now.addingTimeInterval(Self.retryInterval))
            )
        }
        let refreshed = repository.loadRefreshedSnapshot(for: source)
        if let refreshed, refreshed.refreshDate > now {
            return .available(snapshot: refreshed, refreshAfter: refreshed.refreshDate)
        }
        return await refresh(repository: repository, service: service, source: source, cached: refreshed ?? source, now: now)
    }

    private func refresh(repository: WidgetSnapshotRepository, service: WidgetRefreshService, source: UpcomingMatchesSnapshot, cached: UpcomingMatchesSnapshot, now: Date) async -> WidgetRefreshOutcome {
        let key = (repository.sourceURL?.absoluteString ?? "") + source.sourceSignature
        if let task = pending[key] { return await task.value }
        retryAfter = retryAfter.filter { $0.value > now }
        if let retryAfter = retryAfter[key] {
            return .retry(snapshot: repository.loadBestSnapshot(), refreshAfter: retryAfter)
        }
        let task = Task<WidgetRefreshOutcome, Never> {
            do {
                let refreshed = try await service.refresh(source: cached, now: now)
                if repository.store(refreshed, for: source) {
                    let snapshot = repository.loadBestSnapshot() ?? refreshed
                    return .available(snapshot: snapshot, refreshAfter: snapshot.refreshDate)
                }
                if repository.loadSource()?.sourceSignature == source.sourceSignature {
                    return .available(snapshot: refreshed, refreshAfter: refreshed.refreshDate)
                }
                let retryAfter = now.addingTimeInterval(Self.retryInterval)
                return .retry(snapshot: repository.loadBestSnapshot(), refreshAfter: retryAfter)
            } catch {
                let retryAfter = now.addingTimeInterval(Self.retryInterval)
                self.retryAfter[key] = retryAfter
                return .retry(snapshot: repository.loadBestSnapshot(), refreshAfter: retryAfter)
            }
        }
        pending[key] = task
        let result = await task.value
        pending[key] = nil
        return result
    }
}

struct WidgetRefreshService {
    private let api: WidgetAPIClient

    init(api: WidgetAPIClient = .live(authorization: GeneratedBuildConfig.authToken)) {
        self.api = api
    }

    func refresh(source: UpcomingMatchesSnapshot, now: Date = .now) async throws -> UpcomingMatchesSnapshot {
        guard source.hasFavorites else {
            return source.withRefreshedMatches([], at: now)
        }
        guard !source.clientId.isEmpty else {
            throw WidgetRefreshError.invalidResponse
        }

        let matches: [RemoteMatchPreview] = try await api.get(
            "/api/v1/favorites/\(source.clientId)/matches?include_results=false"
        )
        var seen = Set<String>()
        let upcoming = matches
            .filter { seen.insert($0.id).inserted }
            .compactMap(\.widgetMatch)
        return source.withRefreshedMatches(upcoming, at: now)
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

private func epochMillis(_ value: String?) -> Int64? {
    guard let value, !value.isEmpty else { return nil }
    let formatter = ISO8601DateFormatter()
    formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    let date = formatter.date(from: value) ?? ISO8601DateFormatter().date(from: value)
    return date.map { Int64($0.timeIntervalSince1970 * 1_000) }
}

private func normalizedMatchFormat(_ value: String) -> String {
    let compact = value.replacingOccurrences(of: " ", with: "").uppercased()
    guard compact.hasPrefix("BO"), Int(compact.dropFirst(2)) != nil else { return "" }
    return compact
}

