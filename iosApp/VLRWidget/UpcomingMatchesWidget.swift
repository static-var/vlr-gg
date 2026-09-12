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

    let entry: UpcomingMatchesEntry
    let familyOverride: WidgetFamily?

    init(entry: UpcomingMatchesEntry, familyOverride: WidgetFamily? = nil) {
        self.entry = entry
        self.familyOverride = familyOverride
    }

    private var family: WidgetFamily { familyOverride ?? environmentFamily }

    private var theme: WidgetTheme {
        (entry.snapshot?.theme ?? .preview).adapted(to: colorScheme)
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
                    status(title: "No favorites", message: "Favorite a team, player, event, or match in VLR.")
                } else if visibleMatches.isEmpty {
                    status(title: "No matches", message: "There are no live or upcoming matches for your favorites.")
                } else {
                    content(snapshot: snapshot)
                }
            } else {
                status(title: "Open VLR", message: "Open the app once to connect your favorites.")
            }
        }
        .padding(outerPadding)
        .widgetURL(widgetURL)
        .widgetBackground(theme.widgetBackgroundColor)
    }

    @ViewBuilder
    private func content(snapshot: UpcomingMatchesSnapshot) -> some View {
        switch family {
        case .systemSmall:
            smallLayout(match: visibleMatches[0], snapshot: snapshot)
        case .systemLarge:
            largeLayout(matches: visibleMatches, snapshot: snapshot)
        default:
            mediumLayout(matches: visibleMatches, snapshot: snapshot)
        }
    }

    private func smallLayout(match: UpcomingMatch, snapshot: UpcomingMatchesSnapshot) -> some View {
        VStack(alignment: .center, spacing: 8) {
            if match.status == .live {
                Text("LIVE")
                    .font(textFont(.caption2).weight(.bold))
                    .foregroundStyle(theme.accentColor)
            }
            Spacer(minLength: 0)
            VStack(spacing: 3) {
                smallTeamName(match.team1)
                Group {
                    if match.status == .live, snapshot.spoilersHidden {
                        Image(systemName: "eye.slash")
                            .accessibilityLabel("Scores hidden")
                    } else if match.status == .live,
                              let score1 = match.score1, let score2 = match.score2 {
                        Text("\(score1) : \(score2)")
                    } else {
                        Text("vs")
                    }
                }
                .font(textFont(.caption))
                .foregroundStyle(theme.secondaryColor)
                smallTeamName(match.team2)
            }
            Spacer(minLength: 0)
            if match.status == .upcoming {
                Text(match.startTime.map {
                    "\($0.formatted(.dateTime.weekday(.abbreviated))), \($0.formatted(date: .omitted, time: .shortened))"
                } ?? "TBD")
                    .font(textFont(.subheadline).weight(.semibold))
                    .foregroundStyle(theme.contentColor)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
                    .lineLimit(1)
                    .minimumScaleFactor(0.75)
            }
        }
    }

    private func smallTeamName(_ name: String) -> some View {
        Text(name)
            .font(textFont(.subheadline).weight(.semibold))
            .foregroundStyle(theme.contentColor)
            .multilineTextAlignment(.center)
            .frame(maxWidth: .infinity)
            .lineLimit(2)
            .minimumScaleFactor(0.75)
    }

    private func mediumLayout(matches: [UpcomingMatch], snapshot: UpcomingMatchesSnapshot) -> some View {
        scoreboard(matches[0], snapshot: snapshot)
    }

    private func largeLayout(matches: [UpcomingMatch], snapshot: UpcomingMatchesSnapshot) -> some View {
        VStack(spacing: 16) {
            ForEach(Array(matches.enumerated()), id: \.element.id) { index, match in
                if index > 0 {
                    Rectangle().fill(theme.borderColor).frame(height: 1)
                }
                Link(destination: VLRWidgetContract.matchURL(id: match.id)) {
                    scoreboard(match, snapshot: snapshot)
                }
            }
        }
    }

    private func scoreboard(_ match: UpcomingMatch, snapshot: UpcomingMatchesSnapshot) -> some View {
        VStack(spacing: 8) {
            Text(match.event.isEmpty ? "Favorite match" : match.event)
                .font(textFont(.caption))
                .foregroundStyle(theme.accentColor)
                .lineLimit(1)
                .frame(maxWidth: .infinity, alignment: .leading)
            Rectangle().fill(theme.borderColor).frame(height: 1)
            Spacer(minLength: 0)
            HStack(alignment: .center, spacing: 8) {
                scoreboardTeam(match.team1, score: match.score1, match: match, spoilersHidden: snapshot.spoilersHidden)
                VStack(spacing: 4) {
                    if match.status == .live {
                        Text("LIVE")
                            .foregroundStyle(theme.accentColor)
                        if snapshot.spoilersHidden {
                            Image(systemName: "eye.slash")
                                .accessibilityLabel("Scores hidden")
                        }
                    } else if let time = match.startTime {
                        Text(time.formatted(.dateTime.weekday(.abbreviated)))
                        Text(time.formatted(date: .omitted, time: .shortened))
                    } else {
                        Text("TBD")
                    }
                }
                .font(textFont(.caption))
                .foregroundStyle(theme.secondaryColor)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: true, vertical: false)
                scoreboardTeam(match.team2, score: match.score2, match: match, spoilersHidden: snapshot.spoilersHidden)
            }
            Spacer(minLength: 0)
            Text([match.format, match.stage].filter { !$0.isEmpty }.joined(separator: " · "))
                .font(textFont(.caption2))
                .foregroundStyle(theme.secondaryColor)
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .accessibilityElement(children: .combine)
        .accessibilityHint("Opens match details")
    }

    private func scoreboardTeam(_ name: String, score: Int?, match: UpcomingMatch, spoilersHidden: Bool) -> some View {
        VStack(spacing: 5) {
            Text(match.status == .live && !spoilersHidden ? score.map(String.init) ?? "-" : "-")
                .font(.custom("SpaceGrotesk-Regular", size: 34, relativeTo: .largeTitle))
                .foregroundStyle(theme.contentColor)
            Text(name)
                .font(textFont(.subheadline))
                .foregroundStyle(theme.contentColor)
                .lineLimit(2)
                .minimumScaleFactor(0.75)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
    }

    private func status(title: String, message: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            if family != .systemSmall {
                Text("VLR")
                    .font(textFont(.caption).weight(.black))
                    .tracking(1.1)
                    .foregroundStyle(theme.accentColor)
            }
            Spacer(minLength: 0)
            Text(title)
                .font(textFont(.headline).weight(.semibold))
                .foregroundStyle(theme.contentColor)
            Text(message)
                .font(textFont(.caption))
                .foregroundStyle(theme.secondaryColor)
                .lineLimit(family == .systemLarge ? 3 : 2)
            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
    }

    private func textFont(_ style: Font.TextStyle) -> Font {
        let size: CGFloat
        switch style {
        case .caption2: size = 11
        case .caption: size = 12
        case .subheadline: size = 15
        case .headline: size = 17
        default: size = 15
        }
        return .custom("SpaceGrotesk-Regular", size: size, relativeTo: style)
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
    init(argb: Int64) {
        let value = UInt32(truncatingIfNeeded: argb)
        self.init(
            .sRGB,
            red: Double((value >> 16) & 0xFF) / 255,
            green: Double((value >> 8) & 0xFF) / 255,
            blue: Double(value & 0xFF) / 255,
            opacity: Double((value >> 24) & 0xFF) / 255
        )
    }
}

private extension WidgetTheme {
    func adapted(to colorScheme: ColorScheme) -> WidgetTheme {
        let base = UInt32(truncatingIfNeeded: background)
        let brightness = 0.2126 * Double((base >> 16) & 0xFF)
            + 0.7152 * Double((base >> 8) & 0xFF)
            + 0.0722 * Double(base & 0xFF)
        let dark = colorScheme == .dark
        guard dark != (brightness < 128) else { return self }
        let accentValue = UInt32(truncatingIfNeeded: accent)
        func adjustedAccentChannel(_ shift: UInt32) -> UInt32 {
            let value = Double((accentValue >> shift) & 0xFF)
            return UInt32(dark ? value * 0.65 + 255 * 0.35 : value * 0.7)
        }
        let adjustedAccent = 0xFF000000
            | (adjustedAccentChannel(16) << 16)
            | (adjustedAccentChannel(8) << 8)
            | adjustedAccentChannel(0)
        return WidgetTheme(
            background: dark ? 0xFF19171F : 0xFFFAF9FC,
            surface: dark ? 0xFF292531 : 0xFFF0EDF5,
            accent: Int64(adjustedAccent),
            content: dark ? 0xFFF5F1FA : 0xFF211C2B,
            secondary: dark ? 0xFFBDB5CA : 0xFF655D71,
            border: dark ? 0xFF3D3648 : 0xFFDED8E7,
            monospace: monospace
        )
    }

    var widgetBackgroundColor: Color {
        let base = UInt32(truncatingIfNeeded: background)
        let tint = UInt32(truncatingIfNeeded: accent)
        let red = Double((base >> 16) & 0xFF)
        let green = Double((base >> 8) & 0xFF)
        let blue = Double(base & 0xFF)
        let isDark = (0.2126 * red + 0.7152 * green + 0.0722 * blue) < 128
        let amount = isDark ? 0.10 : 0.06
        func channel(_ value: Double, shift: UInt32) -> Double {
            (value * (1 - amount) + Double((tint >> shift) & 0xFF) * amount) / 255
        }
        return Color(.sRGB, red: channel(red, shift: 16), green: channel(green, shift: 8), blue: channel(blue, shift: 0), opacity: 1)
    }
    var backgroundColor: Color { Color(argb: background) }
    var surfaceColor: Color { Color(argb: surface) }
    var accentColor: Color { Color(argb: accent) }
    var contentColor: Color { Color(argb: content) }
    var secondaryColor: Color { Color(argb: secondary) }
    var borderColor: Color { Color(argb: border) }
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
                format: "Playoffs",
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
                format: "Group Stage",
                stage: "Week 3"
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
