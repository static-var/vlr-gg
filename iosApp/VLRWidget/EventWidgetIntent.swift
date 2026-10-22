import AppIntents
import WidgetKit

@available(iOS 17.0, *)
struct FavoriteEventEntity: AppEntity {
    static var typeDisplayRepresentation: TypeDisplayRepresentation = "Event"
    static var defaultQuery = FavoriteEventQuery()

    let id: String
    let name: String
    var imageData: Data? = nil
    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(name)", image: imageData.map { .init(data: $0, isTemplate: false) })
    }
}

@available(iOS 17.0, *)
struct FavoriteEventQuery: EntityQuery {
    var loadEvents: @Sendable () -> [WidgetFavoriteEvent] = {
        WidgetSnapshotRepository().loadSource()?.favoriteEvents ?? []
    }

    func entities(for identifiers: [String]) async throws -> [FavoriteEventEntity] {
        let events = loadEvents()
        return await entities(identifiers.compactMap { id in events.first { $0.id == id } })
    }

    func suggestedEntities() async throws -> [FavoriteEventEntity] {
        await entities(loadEvents().sorted { $0.name.localizedStandardCompare($1.name) == .orderedAscending })
    }

    private func entities(_ values: [WidgetFavoriteEvent]) async -> [FavoriteEventEntity] {
        await WidgetLogoPrefetch.run(values.compactMap(\.logo), timeout: .seconds(1))
        return values.map { FavoriteEventEntity(id: $0.id, name: $0.name, imageData: MatchActivityLogoCache.cachedImageData(for: $0.logo)) }
    }
}

@available(iOS 17.0, *)
struct EventWidgetIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource = "Event"
    static var description = IntentDescription("Your favorite event's next match, live scores and standings.")

    @Parameter(title: "Event") var event: FavoriteEventEntity?
}
