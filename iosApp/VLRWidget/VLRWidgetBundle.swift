import SwiftUI
import WidgetKit

@main
struct VLRWidgetBundle: WidgetBundle {
    var body: some Widget {
        UpcomingMatchesWidget()
    }
}

private struct UpcomingMatchesWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: VLRWidgetContract.widgetKind, provider: UpcomingMatchesProvider()) { entry in
            UpcomingMatchesWidgetView(entry: entry)
        }
        .configurationDisplayName("Favorite matches")
        .description("Live and upcoming matches connected to your Valorant Esports favorites.")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}
