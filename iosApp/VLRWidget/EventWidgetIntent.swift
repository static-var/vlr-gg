import AppIntents
import WidgetKit

@available(iOS 17.0, *)
struct FavoriteEventEntity: AppEntity {
    static var typeDisplayRepresentation: TypeDisplayRepresentation = "Event"
    static var defaultQuery = FavoriteEventQuery()

    let id: String
    let name: String
    var displayRepresentation: DisplayRepresentation { DisplayRepresentation(title: "\(name)") }
}

@available(iOS 17.0, *)
struct FavoriteEventQuery: EntityQuery {
    var loadEvents: @Sendable () -> [WidgetFavoriteEvent] = {
        WidgetSnapshotRepository().loadSource()?.favoriteEvents ?? []
    }

    func entities(for identifiers: [String]) async throws -> [FavoriteEventEntity] {
        let events = loadEvents()
        return identifiers.compactMap { id in
            events.first { $0.id == id }.map { FavoriteEventEntity(id: $0.id, name: $0.name) }
        }
    }

    func suggestedEntities() async throws -> [FavoriteEventEntity] {
        loadEvents()
            .sorted { $0.name.localizedStandardCompare($1.name) == .orderedAscending }
            .map { FavoriteEventEntity(id: $0.id, name: $0.name) }
    }
}

@available(iOS 17.0, *)
struct EventWidgetIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource = "Event"
    static var description = IntentDescription("Your favorite event's next match, live scores and standings.")

    @Parameter(title: "Event") var event: FavoriteEventEntity?
}
