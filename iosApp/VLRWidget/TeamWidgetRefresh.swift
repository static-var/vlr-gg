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
        return TeamWidgetMapper.content(profile: ranking, match: match, details: details, spoilersHidden: source.spoilersHidden)
    }
}

struct TeamWidgetCache {
    let directory: URL?

    init(directory: URL? = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: VLRWidgetContract.appGroupIdentifier)) {
        self.directory = directory
    }

    private struct Saved: Codable {
        let teamID: String
        let signature: String
        let content: TeamWidgetContent
    }

    func load(teamID: String, source: UpcomingMatchesSnapshot) -> TeamWidgetContent? {
        guard let url = url(teamID: teamID), let data = try? Data(contentsOf: url),
              let saved = try? JSONDecoder().decode(Saved.self, from: data),
              saved.teamID == teamID, saved.signature == source.sourceSignature else { return nil }
        return saved.content
    }

    func store(_ content: TeamWidgetContent, teamID: String, source: UpcomingMatchesSnapshot) {
        guard let url = url(teamID: teamID),
              let data = try? JSONEncoder().encode(Saved(teamID: teamID, signature: source.sourceSignature, content: content)) else { return }
        try? data.write(to: url, options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication])
    }

    private func url(teamID: String) -> URL? {
        let name = Data(teamID.utf8).base64EncodedString().replacingOccurrences(of: "/", with: "_")
        return directory?.appendingPathComponent("team-widget-\(name).json")
    }
}
