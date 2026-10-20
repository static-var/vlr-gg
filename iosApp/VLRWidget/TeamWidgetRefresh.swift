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
        async let matches: [RemoteMatchPreview] = api.get("/api/v1/favorites/\(client)/matches?include_results=false")
        let (ranking, previews) = try await (profile, matches)
        guard let rankingID = ranking.team.id, !rankingID.isEmpty, rankingID == teamID else {
            throw WidgetRefreshError.invalidResponse
        }
        let match = TeamWidgetMapper.match(for: teamID, in: previews)
        let details: WidgetMatchDetailsResponse?
        if let match, let id = match.id.addingPercentEncoding(withAllowedCharacters: pathCharacters) {
            details = try? await api.get("/api/v1/matches/\(id)")
        } else { details = nil }
        return TeamWidgetMapper.content(profile: ranking, match: match, details: details)
    }
}
