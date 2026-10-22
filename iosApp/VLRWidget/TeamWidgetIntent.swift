import AppIntents
import WidgetKit

@available(iOS 17.0, *)
struct FavoriteTeamEntity: AppEntity {
    static var typeDisplayRepresentation: TypeDisplayRepresentation = "Team"
    static var defaultQuery = FavoriteTeamQuery()

    let id: String
    let name: String
    var tag: String? = nil
    var imageData: Data? = nil
    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(name)", subtitle: tag.flatMap { value in
            let value = value.trimmingCharacters(in: .whitespacesAndNewlines)
            return value.isEmpty || value.caseInsensitiveCompare(name) == .orderedSame ? nil : "\(value)"
        }, image: imageData.map { .init(data: $0, isTemplate: false) })
    }
}

@available(iOS 17.0, *)
struct FavoriteTeamQuery: EntityQuery {
    var loadTeams: @Sendable () -> [WidgetFavoriteTeam] = {
        WidgetSnapshotRepository().loadSource()?.favoriteTeams ?? []
    }

    func entities(for identifiers: [String]) async throws -> [FavoriteTeamEntity] {
        let teams = loadTeams()
        return await entities(identifiers.compactMap { id in teams.first { $0.id == id } })
    }

    func suggestedEntities() async throws -> [FavoriteTeamEntity] {
        await entities(loadTeams().sorted { $0.name.localizedStandardCompare($1.name) == .orderedAscending })
    }

    private func entities(_ values: [WidgetFavoriteTeam]) async -> [FavoriteTeamEntity] {
        await WidgetLogoPrefetch.run(values.compactMap(\.logo), timeout: .seconds(1))
        return values.map { FavoriteTeamEntity(id: $0.id, name: $0.name, tag: $0.tag, imageData: MatchActivityLogoCache.cachedImageData(for: $0.logo)) }
    }
}

@available(iOS 17.0, *)
struct TeamWidgetIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource = "Team"
    static var description = IntentDescription("Your favorite team's next match, live score and recent form.")

    @Parameter(title: "Team") var team: FavoriteTeamEntity?
}
