import SwiftUI
import WidgetKit

@main
struct VLRWidgetBundle: WidgetBundle {
    var body: some Widget {
        UpcomingMatchesWidget()
        if #available(iOS 16.1, *) {
            MatchLiveActivity()
        }
    }
}

private struct UpcomingMatchesWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: VLRWidgetContract.widgetKind, provider: UpcomingMatchesProvider()) { entry in
            UpcomingMatchesWidgetView(entry: entry)
        }
        .configurationDisplayName(LocalizedStringResource("Favorite matches"))
        .description(LocalizedStringResource("Live and upcoming matches connected to your Val Esports favorites."))
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}
