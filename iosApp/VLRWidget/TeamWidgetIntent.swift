import AppIntents
import WidgetKit

@available(iOS 17.0, *)
struct FavoriteTeamEntity: AppEntity {
    static var typeDisplayRepresentation: TypeDisplayRepresentation = "Team"
    static var defaultQuery = FavoriteTeamQuery()

    let id: String
    let name: String
    var displayRepresentation: DisplayRepresentation { DisplayRepresentation(title: "\(name)") }
}

@available(iOS 17.0, *)
struct FavoriteTeamQuery: EntityQuery {
    var loadTeams: @Sendable () -> [WidgetFavoriteTeam] = {
        WidgetSnapshotRepository().loadSource()?.favoriteTeams ?? []
    }

    func entities(for identifiers: [String]) async throws -> [FavoriteTeamEntity] {
        let teams = loadTeams()
        return identifiers.compactMap { id in
            teams.first { $0.id == id }.map { FavoriteTeamEntity(id: $0.id, name: $0.name) }
        }
    }

    func suggestedEntities() async throws -> [FavoriteTeamEntity] {
        loadTeams()
            .sorted { $0.name.localizedStandardCompare($1.name) == .orderedAscending }
            .map { FavoriteTeamEntity(id: $0.id, name: $0.name) }
    }
}

@available(iOS 17.0, *)
struct TeamWidgetIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource = "Team"
    static var description = IntentDescription("Your favorite team's next match, live score and recent form.")

    @Parameter(title: "Team") var team: FavoriteTeamEntity?
}
