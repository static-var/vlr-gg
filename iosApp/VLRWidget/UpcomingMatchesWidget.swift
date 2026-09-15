import SwiftUI
import WidgetKit

struct UpcomingMatchesEntry: TimelineEntry {
    let date: Date
    let snapshot: UpcomingMatchesSnapshot?
}

struct UpcomingMatchesProvider: TimelineProvider {
    private let repository: WidgetSnapshotRepository
    private let refreshService: WidgetRefreshService

    init(
        repository: WidgetSnapshotRepository = WidgetSnapshotRepository(),
        refreshService: WidgetRefreshService = WidgetRefreshService()
    ) {
        self.repository = repository
        self.refreshService = refreshService
    }

    func placeholder(in context: Context) -> UpcomingMatchesEntry {
        UpcomingMatchesEntry(date: .now, snapshot: .preview)
    }

    func getSnapshot(in context: Context, completion: @escaping (UpcomingMatchesEntry) -> Void) {
        let snapshot = repository.loadBestSnapshot() ?? (context.isPreview ? .preview : nil)
        completion(UpcomingMatchesEntry(date: .now, snapshot: snapshot))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<UpcomingMatchesEntry>) -> Void) {
        Task {
            let now = Date.now
            let source = repository.loadSource()
            var snapshot = repository.loadBestSnapshot()

            if let source, source.hasFavorites {
                do {
                    let refreshed = try await refreshService.refresh(source: source, now: now)
                    if repository.store(refreshed, for: source) {
                        snapshot = refreshed.usingConfiguration(from: source)
                    } else {
                        snapshot = repository.loadBestSnapshot()
                    }
                } catch {
                    snapshot = repository.loadBestSnapshot() ?? snapshot
                }
            }

            let refreshDate = nextRefreshDate(for: snapshot, now: now)
            completion(
                Timeline(
                    entries: [UpcomingMatchesEntry(date: now, snapshot: snapshot)],
                    policy: .after(refreshDate)
                )
            )
        }
    }

    private func nextRefreshDate(for snapshot: UpcomingMatchesSnapshot?, now: Date) -> Date {
        let hasLiveMatch = snapshot?.matches.contains { $0.status == .live } == true
        let requestedInterval: TimeInterval = hasLiveMatch ? 15 * 60 : 30 * 60
        var requestedDate = now.addingTimeInterval(requestedInterval)

        if let nextStart = snapshot?.matches
            .filter({ $0.status == .upcoming })
            .compactMap(\.startTime)
            .filter({ $0 > now.addingTimeInterval(5 * 60) })
            .min() {
            requestedDate = min(requestedDate, nextStart)
        }
        return requestedDate
    }
}

struct UpcomingMatchesWidgetView: View {
    @Environment(\.widgetFamily) private var environmentFamily
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.widgetRenderingMode) private var renderingMode

    let entry: UpcomingMatchesEntry
    let familyOverride: WidgetFamily?

    init(entry: UpcomingMatchesEntry, familyOverride: WidgetFamily? = nil) {
        self.entry = entry
        self.familyOverride = familyOverride
    }

    private var family: WidgetFamily { familyOverride ?? environmentFamily }

    private var palette: PrismWidgetPalette {
        let palette = colorScheme == .dark ? PrismWidgetPalette.dark : .light
        return renderingMode == .fullColor ? palette : .systemRendered
    }

    private var visibleMatches: [UpcomingMatch] {
        guard let snapshot = entry.snapshot else { return [] }
        let limit = family == .systemLarge ? 2 : 1
        return Array(snapshot.matches.prefix(limit))
    }

    private var widgetURL: URL {
        guard let first = visibleMatches.first,
              family == .systemSmall || visibleMatches.count == 1 else {
            return VLRWidgetContract.appURL
        }
        return VLRWidgetContract.matchURL(id: first.id)
    }

    private var outerPadding: CGFloat {
        if #available(iOS 17.0, *) {
            return 0
        }
        return 14
    }

    var body: some View {
        Group {
            if let snapshot = entry.snapshot {
                if !snapshot.hasFavorites {
                    EmptyWidgetState(
                        family: family,
                        title: "No favorites.",
                        message: "Favorite a team, player, event, or match in Val Esports.",
                        palette: palette
                    )
                } else if visibleMatches.isEmpty {
                    EmptyWidgetState(
                        family: family,
                        title: "No matches scheduled.",
                        message: "Your favorites return here when they play.",
                        palette: palette
                    )
                } else {
                    content(snapshot: snapshot)
                }
            } else {
                EmptyWidgetState(
                    family: family,
                    title: "Open the app.",
                    message: "Open Val Esports once to connect your favorites.",
                    palette: palette
                )
            }
        }
        .padding(outerPadding)
        .widgetURL(widgetURL)
        .widgetBackground(PrismWidgetPalette.background(for: colorScheme))
    }

    @ViewBuilder
    private func content(snapshot: UpcomingMatchesSnapshot) -> some View {
        switch family {
        case .systemSmall:
            SmallMatchWidget(
                match: visibleMatches[0],
                spoilersHidden: snapshot.spoilersHidden,
                palette: palette
            )
        case .systemLarge:
            LargeMatchesWidget(
                matches: visibleMatches,
                spoilersHidden: snapshot.spoilersHidden,
                palette: palette
            )
        default:
            MediumMatchWidget(
                match: visibleMatches[0],
                spoilersHidden: snapshot.spoilersHidden,
                palette: palette
            )
        }
    }
}

struct PrismWidgetPalette {
    let background: Color
    let ink: Color
    let secondary: Color
    let accent: Color
    let border: Color

    static let light = PrismWidgetPalette(
        background: Color(hex: 0xE2D9FA),
        ink: Color(hex: 0x241A35),
        secondary: Color(hex: 0x635174),
        accent: Color(hex: 0x63339C),
        border: Color(hex: 0xB7A5CD)
    )

    static let dark = PrismWidgetPalette(
        background: Color(hex: 0x292136),
        ink: Color(hex: 0xF0E7FF),
        secondary: Color(hex: 0xC3B2D5),
        accent: Color(hex: 0xC49AFD),
        border: Color(hex: 0x594567)
    )

    static let systemRendered = PrismWidgetPalette(
        background: .clear,
        ink: .white,
        secondary: .white.opacity(0.72),
        accent: .white,
        border: .white.opacity(0.32)
    )

    static func background(for colorScheme: ColorScheme) -> Color {
        colorScheme == .dark ? dark.background : light.background
    }
}

private enum PrismWidgetFont {
    static func regular(_ size: CGFloat, relativeTo style: Font.TextStyle) -> Font {
        .custom("ChakraPetch-Regular", size: size, relativeTo: style)
    }
}

private struct SmallMatchWidget: View {
    let match: UpcomingMatch
    let spoilersHidden: Bool
    let palette: PrismWidgetPalette

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            MatchStatusRow(
                match: match,
                trailingText: match.status == .upcoming ? match.startWeekday : match.displayFormat,
                palette: palette,
                trailingStyle: .secondary
            )
            Spacer(minLength: 8)
            MatchTeams(
                match: match,
                spoilersHidden: spoilersHidden,
                style: .small,
                palette: palette
            )

            if match.status == .upcoming {
                Spacer(minLength: 6)
                Text(match.startClockTime)
                    .font(PrismWidgetFont.regular(12, relativeTo: .caption))
                    .foregroundStyle(palette.secondary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.75)
            } else if spoilersHidden {
                Spacer(minLength: 6)
                Text("Scores hidden")
                    .font(PrismWidgetFont.regular(10, relativeTo: .caption2))
                    .foregroundStyle(palette.secondary)
                    .lineLimit(1)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .accessibilityElement(children: .combine)
        .accessibilityHint("Opens match details")
    }
}

private struct MediumMatchWidget: View {
    let match: UpcomingMatch
    let spoilersHidden: Bool
    let palette: PrismWidgetPalette

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            MatchStatusRow(
                match: match,
                trailingText: match.status == .live ? match.displayFormat : match.compactStartTime,
                palette: palette,
                trailingStyle: match.status == .live ? .secondary : .primary
            )
            Spacer(minLength: 8)
            MatchTeams(
                match: match,
                spoilersHidden: spoilersHidden,
                style: .standard,
                palette: palette
            )
            Spacer(minLength: 6)
            Text(match.details)
                .font(PrismWidgetFont.regular(11, relativeTo: .caption2))
                .foregroundStyle(palette.secondary)
                .lineLimit(1)
                .minimumScaleFactor(0.72)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .accessibilityElement(children: .combine)
        .accessibilityHint("Opens match details")
    }
}

private struct LargeMatchesWidget: View {
    let matches: [UpcomingMatch]
    let spoilersHidden: Bool
    let palette: PrismWidgetPalette

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            FavoritesHeader(count: matches.count, palette: palette)
                .padding(.bottom, 14)

            ForEach(matches) { match in
                Link(destination: VLRWidgetContract.matchURL(id: match.id)) {
                    LargeMatchCard(
                        match: match,
                        spoilersHidden: spoilersHidden,
                        expanded: matches.count == 1,
                        palette: palette
                    )
                }
                .buttonStyle(.plain)
                .frame(maxHeight: .infinity)
            }

            if matches.count == 1 {
                Text("You're caught up on your favorites.")
                    .font(PrismWidgetFont.regular(12, relativeTo: .caption))
                    .foregroundStyle(palette.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.top, 13)
                    .overlay(alignment: .top) {
                        Rectangle()
                            .fill(palette.border)
                            .frame(height: 1)
                    }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }
}

private struct LargeMatchCard: View {
    let match: UpcomingMatch
    let spoilersHidden: Bool
    let expanded: Bool
    let palette: PrismWidgetPalette

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Rectangle()
                .fill(palette.border)
                .frame(height: 1)
            MatchStatusRow(
                match: match,
                trailingText: match.compactStartTime,
                palette: palette,
                trailingStyle: .primary
            )
            .padding(.top, 11)

            Spacer(minLength: expanded ? 16 : 7)
            MatchTeams(
                match: match,
                spoilersHidden: spoilersHidden,
                style: expanded ? .expanded : .standard,
                palette: palette
            )
            Spacer(minLength: expanded ? 16 : 7)
            Text(match.details)
                .font(PrismWidgetFont.regular(expanded ? 12 : 11, relativeTo: .caption))
                .foregroundStyle(palette.secondary)
                .lineLimit(expanded ? 2 : 1)
                .minimumScaleFactor(0.72)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .padding(.bottom, expanded ? 0 : 10)
        .contentShape(Rectangle())
        .accessibilityElement(children: .combine)
        .accessibilityHint("Opens match details")
    }
}

private struct FavoritesHeader: View {
    let count: Int
    let palette: PrismWidgetPalette

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 8) {
            Text("FAVORITES")
                .font(PrismWidgetFont.regular(15, relativeTo: .subheadline))
                .tracking(0.8)
                .foregroundStyle(palette.ink)
            Spacer(minLength: 4)
            Text(count == 0 ? "NO MATCHES" : count == 1 ? "1 MATCH" : "\(count) MATCHES")
                .font(PrismWidgetFont.regular(11, relativeTo: .caption2))
                .foregroundStyle(palette.secondary)
                .lineLimit(1)
        }
    }
}

private enum MatchTeamsStyle {
    case small
    case standard
    case expanded

    var nameSize: CGFloat {
        switch self {
        case .small: 16
        case .standard: 20
        case .expanded: 27
        }
    }

    var scoreSize: CGFloat {
        switch self {
        case .small: 27
        case .standard: 28
        case .expanded: 39
        }
    }

    var rowSpacing: CGFloat {
        switch self {
        case .small: 7
        case .standard: 5
        case .expanded: 17
        }
    }

    var minimumRowHeight: CGFloat {
        switch self {
        case .small, .standard: 28
        case .expanded: 39
        }
    }

    var scoreWidth: CGFloat {
        switch self {
        case .small: 24
        case .standard: 28
        case .expanded: 40
        }
    }
}

private struct MatchTeams: View {
    let match: UpcomingMatch
    let spoilersHidden: Bool
    let style: MatchTeamsStyle
    let palette: PrismWidgetPalette

    var body: some View {
        VStack(spacing: style.rowSpacing) {
            teamRow(name: match.team1, score: match.score1)
            teamRow(name: match.team2, score: match.score2)
        }
    }

    private func teamRow(name: String, score: Int?) -> some View {
        HStack(alignment: .center, spacing: 8) {
            Text(name)
                .font(PrismWidgetFont.regular(style.nameSize, relativeTo: .headline))
                .foregroundStyle(palette.ink)
                .lineLimit(2)
                .minimumScaleFactor(0.72)
                .multilineTextAlignment(.leading)
                .frame(maxWidth: .infinity, alignment: .leading)

            Text(displayScore(score))
                .font(PrismWidgetFont.regular(style.scoreSize, relativeTo: .title))
                .foregroundStyle(palette.accent)
                .monospacedDigit()
                .lineLimit(1)
                .minimumScaleFactor(0.72)
                .frame(minWidth: style.scoreWidth, alignment: .trailing)
                .widgetAccentable()
                .accessibilityLabel(scoreAccessibilityLabel(score))
        }
        .frame(minHeight: style.minimumRowHeight)
    }

    private func displayScore(_ score: Int?) -> String {
        guard match.status == .live, !spoilersHidden, let score else { return "—" }
        return String(score)
    }

    private func scoreAccessibilityLabel(_ score: Int?) -> String {
        guard match.status == .live else { return "Not started" }
        guard !spoilersHidden else { return "Score hidden" }
        guard let score else { return "Score unavailable" }
        return "Score \(score)"
    }
}

private struct MatchStatusRow: View {
    enum TrailingStyle {
        case primary
        case secondary
    }

    let match: UpcomingMatch
    let trailingText: String
    let palette: PrismWidgetPalette
    let trailingStyle: TrailingStyle

    var body: some View {
        HStack(alignment: .center, spacing: 8) {
            HStack(spacing: 5) {
                if match.status == .live {
                    Rectangle()
                        .fill(palette.accent)
                        .frame(width: 5, height: 5)
                        .accessibilityHidden(true)
                }
                Text(match.status.rawValue)
                    .font(PrismWidgetFont.regular(11, relativeTo: .caption2))
                    .tracking(0.7)
                    .lineLimit(1)
            }
            .foregroundStyle(palette.accent)
            .widgetAccentable()

            Spacer(minLength: 4)

            Text(trailingText)
                .font(PrismWidgetFont.regular(12, relativeTo: .caption))
                .foregroundStyle(trailingStyle == .primary ? palette.ink : palette.secondary)
                .lineLimit(1)
                .minimumScaleFactor(0.72)
        }
        .frame(minHeight: 15)
    }
}

private struct EmptyWidgetState: View {
    let family: WidgetFamily
    let title: String
    let message: String
    let palette: PrismWidgetPalette

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if family == .systemLarge {
                FavoritesHeader(count: 0, palette: palette)
                    .padding(.bottom, 14)
            }

            Group {
                if family == .systemMedium {
                    HStack(alignment: .center, spacing: 18) {
                        emptySymbol
                        emptyCopy
                    }
                } else {
                    VStack(alignment: .leading, spacing: family == .systemSmall ? 6 : 12) {
                        emptySymbol
                        emptyCopy
                    }
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var emptySymbol: some View {
        Text("/ /")
            .font(PrismWidgetFont.regular(family == .systemSmall ? 23 : 34, relativeTo: .title))
            .foregroundStyle(palette.accent)
            .widgetAccentable()
            .accessibilityHidden(true)
    }

    private var emptyCopy: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(PrismWidgetFont.regular(family == .systemSmall ? 15 : 19, relativeTo: .headline))
                .foregroundStyle(palette.ink)
                .lineLimit(2)
                .minimumScaleFactor(0.75)
            Text(message)
                .font(PrismWidgetFont.regular(family == .systemSmall ? 11 : 12, relativeTo: .caption))
                .foregroundStyle(palette.secondary)
                .lineLimit(family == .systemLarge ? 3 : 2)
                .minimumScaleFactor(0.75)
        }
    }
}

private extension UpcomingMatch {
    var displayFormat: String {
        format.isEmpty ? "MATCH" : format.uppercased()
    }

    var startWeekday: String {
        startTime?.formatted(.dateTime.weekday(.abbreviated)) ?? "TBD"
    }

    var startClockTime: String {
        startTime?.formatted(date: .omitted, time: .shortened) ?? "TBD"
    }

    var compactStartTime: String {
        guard let startTime else { return "TBD" }
        let weekday = startTime.formatted(.dateTime.weekday(.abbreviated))
        let time = startTime.formatted(date: .omitted, time: .shortened)
        return "\(weekday) · \(time)"
    }

    var details: String {
        let values = [event, format, stage].filter { !$0.isEmpty }
        return values.isEmpty ? "Favorite match" : values.joined(separator: " · ")
    }
}

private extension View {
    @ViewBuilder
    func widgetBackground(_ color: Color) -> some View {
        if #available(iOS 17.0, *) {
            containerBackground(color, for: .widget)
        } else {
            background(color)
        }
    }
}

private extension Color {
    init(hex: UInt32) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
            opacity: 1
        )
    }
}

extension UpcomingMatchesSnapshot {
    static let preview = UpcomingMatchesSnapshot(
        savedAtEpochMillis: Int64(Date.now.timeIntervalSince1970 * 1_000),
        hasFavorites: true,
        favorites: WidgetFavoriteIDs(matchIds: ["preview-1"], teamIds: [], eventIds: [], playerIds: []),
        spoilersHidden: false,
        matches: [
            UpcomingMatch(
                id: "preview-1",
                event: "Valorant Champions",
                team1: "Sentinels",
                team2: "Paper Rex",
                startTimeEpochMillis: Int64(Date.now.addingTimeInterval(3_600).timeIntervalSince1970 * 1_000),
                status: .upcoming,
                score1: nil,
                score2: nil,
                format: "BO5",
                stage: "Upper Final"
            ),
            UpcomingMatch(
                id: "preview-2",
                event: "VCT Pacific",
                team1: "DRX",
                team2: "Gen.G",
                startTimeEpochMillis: nil,
                status: .live,
                score1: 1,
                score2: 0,
                format: "BO3",
                stage: "Group Stage"
            ),
        ],
        theme: .preview
    )
}

struct UpcomingMatchesWidgetPreviews: PreviewProvider {
    static var previews: some View {
        Group {
            UpcomingMatchesWidgetView(entry: UpcomingMatchesEntry(date: .now, snapshot: .preview))
                .previewContext(WidgetPreviewContext(family: .systemSmall))
                .previewDisplayName("Small")
            UpcomingMatchesWidgetView(entry: UpcomingMatchesEntry(date: .now, snapshot: .preview))
                .previewContext(WidgetPreviewContext(family: .systemMedium))
                .previewDisplayName("Medium")
            UpcomingMatchesWidgetView(
                entry: UpcomingMatchesEntry(
                    date: .now,
                    snapshot: UpcomingMatchesSnapshot(
                        savedAtEpochMillis: Int64(Date.now.timeIntervalSince1970 * 1_000),
                        hasFavorites: true,
                        favorites: .empty,
                        spoilersHidden: true,
                        matches: [UpcomingMatchesSnapshot.preview.matches[1]],
                        theme: .preview
                    )
                )
            )
            .previewContext(WidgetPreviewContext(family: .systemLarge))
            .previewDisplayName("Large featured")
        }
    }
}
