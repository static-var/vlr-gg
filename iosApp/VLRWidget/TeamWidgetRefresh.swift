import Foundation

struct TeamWidgetRefreshService {
    let api: WidgetAPIClient

    init(api: WidgetAPIClient = .live(authorization: GeneratedBuildConfig.authToken)) {
        self.api = api
    }

    func refresh(teamID: String, source: UpcomingMatchesSnapshot) async throws -> TeamWidgetContent {
        let pathCharacters = CharacterSet.urlPathAllowed.subtracting(CharacterSet(charactersIn: "/?#"))
        guard let team = teamID.addingPercentEncoding(withAllowedCharacters: pathCharacters),
              !source.clientId.isEmpty,
              let client = source.clientId.addingPercentEncoding(withAllowedCharacters: pathCharacters) else {
            throw WidgetRefreshError.invalidResponse
        }
        async let profile: TeamWidgetRankingResponse = api.get("/api/v2/rankings/teams/\(team)")
        async let matches: [TeamWidgetMatchResponse] = api.get("/api/v1/favorites/\(client)/matches?include_results=false")
        let (ranking, previews) = try await (profile, matches)
        let match = TeamWidgetMapper.match(for: teamID, in: previews)
        let details: TeamWidgetDetailsResponse?
        if let match, let id = match.id.addingPercentEncoding(withAllowedCharacters: pathCharacters) {
            details = try? await api.get("/api/v1/matches/\(id)")
        } else { details = nil }
        return TeamWidgetMapper.content(profile: ranking, match: match, details: details, spoilersHidden: false)
    }
}

actor TeamWidgetRefreshCoordinator {
    static let shared = TeamWidgetRefreshCoordinator()

    private struct Key: Hashable {
        let teamID: String
        let clientID: String
    }

    private var pending: [Key: Task<TeamWidgetContent?, Never>] = [:]

    func refresh(teamID: String, source: UpcomingMatchesSnapshot, cache: TeamWidgetCache,
                 service: TeamWidgetRefreshService = .init()) async -> TeamWidgetContent? {
        let key = Key(teamID: teamID, clientID: source.clientId)
        if let task = pending[key] { return await task.value }
        let fetchedAt = Date.now
        let task = Task<TeamWidgetContent?, Never> {
            if let content = try? await service.refresh(teamID: teamID, source: source) {
                cache.store(content, teamID: teamID, source: source, fetchedAt: fetchedAt)
                return cache.load(teamID: teamID, source: source) ?? content
            }
            return cache.load(teamID: teamID, source: source)
        }
        pending[key] = task
        let content = await task.value
        pending[key] = nil
        return content
    }
}

struct TeamWidgetCache {
    let directory: URL?

    init(directory: URL? = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: VLRWidgetContract.appGroupIdentifier)) {
        self.directory = directory
    }

    struct Saved: Codable {
        let teamID: String
        let clientID: String
        let fetchedAt: Date
        let content: TeamWidgetContent
    }

    func load(teamID: String, source: UpcomingMatchesSnapshot) -> TeamWidgetContent? {
        loadSaved(teamID: teamID, clientID: source.clientId)?.content
    }

    func loadSaved(teamID: String, clientID: String) -> Saved? {
        guard let url = url(teamID: teamID, clientID: clientID), let data = try? Data(contentsOf: url),
              let saved = try? JSONDecoder().decode(Saved.self, from: data),
              saved.teamID == teamID, saved.clientID == clientID else { return nil }
        return saved
    }

    func store(_ content: TeamWidgetContent, teamID: String, source: UpcomingMatchesSnapshot, fetchedAt: Date = .now) {
        if let saved = loadSaved(teamID: teamID, clientID: source.clientId), saved.fetchedAt >= fetchedAt { return }
        guard let url = url(teamID: teamID, clientID: source.clientId),
              let data = try? JSONEncoder().encode(Saved(teamID: teamID, clientID: source.clientId, fetchedAt: fetchedAt, content: content)) else { return }
        try? data.write(to: url, options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication])
    }

    private func url(teamID: String, clientID: String) -> URL? {
        let name = Data("\(teamID):\(clientID)".utf8).base64EncodedString().replacingOccurrences(of: "/", with: "_")
        return directory?.appendingPathComponent("team-widget-\(name).json")
    }
}

enum TeamWidgetLogoPrefetch {
    static func run(_ sources: [String], timeout: Duration = .seconds(4),
                    prefetch: @escaping ([String]) async -> Void = MatchActivityLogoCache.prefetch) async {
        await withTaskGroup(of: Void.self) { group in
            group.addTask { await prefetch(sources) }
            group.addTask { try? await Task.sleep(for: timeout) }
            await group.next()
            group.cancelAll()
        }
    }
}
