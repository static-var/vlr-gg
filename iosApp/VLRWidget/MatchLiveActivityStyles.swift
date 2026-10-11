import ActivityKit
import SwiftUI
import WidgetKit

/// Chooses the Lock Screen layout for the selected Live Activity style.
@available(iOS 16.1, *)
struct MatchLiveActivityStyledLockScreen: View {
    let state: MatchActivityAttributes.ContentState
    let spoilersHidden: Bool
    let style: MatchActivityStyle

    var body: some View {
        switch style {
        case .classic:
            MatchLiveActivityLockScreen(state: state, spoilersHidden: spoilersHidden)
        case .glance:
            MatchLiveActivityGlance(state: state, spoilersHidden: spoilersHidden)
        case .scoreboard:
            MatchLiveActivityScoreboard(state: state, spoilersHidden: spoilersHidden)
        }
    }
}

/// Lists the series score, then each played map, then the live map, like a badminton scoreboard.
@available(iOS 16.1, *)
struct MatchLiveActivityScoreColumns: Equatable {
    enum Kind: Equatable {
        case series
        case map(Int)
        case current(Int)
    }

    struct Column: Identifiable, Equatable {
        let kind: Kind
        let values: [String]
        /// Team index whose value is emphasized, or nil when neither team has won the column.
        let winner: Int?

        var id: String {
            switch kind {
            case .series: return "series"
            case .map(let number): return "map-\(number)"
            case .current(let number): return "current-\(number)"
            }
        }
    }

    let columns: [Column]

    /// - Parameter maps: Only maps with these numbers get a column; nil keeps every played map.
    /// - Parameter limit: Keeps at most this many of the latest map columns.
    init(state: MatchActivityAttributes.ContentState, hidden: Bool, maps: Set<Int>? = nil, limit: Int = 4) {
        let progress = MatchLiveActivityMapProgress(state: state, hidden: hidden)
        let series = state.teams.prefix(2).map(\.score)
        var seriesWinner: Int?
        if !hidden, state.terminal, series.count == 2, let first = series[0], let second = series[1], first != second {
            seriesWinner = first > second ? 0 : 1
        }
        var columns = [Column(kind: .series, values: Self.display(series, hidden: hidden), winner: seriesWinner)]

        let currentNumber = state.terminal ? nil : state.current_map.map { $0.number ?? 0 }
        var mapColumns: [Column] = (progress?.maps ?? []).compactMap { map in
            guard maps?.contains(map.number) ?? true else { return nil }
            if map.number == currentNumber || map.segment == .active {
                return Column(kind: .current(map.number), values: Self.display(map.scores, hidden: hidden), winner: nil)
            }
            switch map.segment {
            case .wonBy(let winner):
                return Column(kind: .map(map.number), values: Self.display(map.scores, hidden: hidden), winner: winner)
            case .pending where !hidden && map.scores.count == 2 && !map.scores.contains(nil):
                return Column(kind: .map(map.number), values: Self.display(map.scores, hidden: hidden), winner: nil)
            default:
                return nil
            }
        }
        if let current = state.current_map, let currentNumber,
           !mapColumns.contains(where: { $0.kind == .current(currentNumber) }) {
            mapColumns.append(Column(kind: .current(currentNumber), values: Self.display(current.scores, hidden: hidden), winner: nil))
        }
        columns += mapColumns.suffix(max(0, limit))
        self.columns = columns
    }

    private static func display(_ values: [Int?], hidden: Bool) -> [String] {
        (0..<2).map { index in
            guard !hidden, values.indices.contains(index), let value = values[index], value >= 0 else { return "—" }
            return String(value)
        }
    }
}

/// Shows both teams one below the other with their series and map scores in aligned columns.
@available(iOS 16.1, *)
struct MatchLiveActivityStackedScores: View {
    enum Layout {
        case glance, scoreboard, dynamicIsland

        var logoSize: CGFloat { self == .glance ? 22 : 20 }
        var fontSize: CGFloat { self == .glance ? 18 : 16 }
        var cellWidth: CGFloat { self == .glance ? 28 : 25 }
        var cellHeight: CGFloat { self == .glance ? 26 : 23 }
        var rowSpacing: CGFloat { self == .glance ? 6 : 5 }
    }

    let state: MatchActivityAttributes.ContentState
    let spoilersHidden: Bool
    let layout: Layout
    var maps: Set<Int>?
    @Environment(\.colorScheme) private var colorScheme

    private var palette: PrismWidgetPalette { colorScheme == .dark ? .dark : .light }
    private var teamColors: MatchActivityTeamColors {
        MatchActivityLogoCache.teamColors(for: state.teams.map(\.img), appearance: colorScheme == .dark ? .dark : .light)
    }
    private var columns: [MatchLiveActivityScoreColumns.Column] {
        MatchLiveActivityScoreColumns(state: state, hidden: spoilersHidden, maps: maps, limit: layout == .glance ? 5 : 3).columns
    }
    /// Short tags share one narrow column; a full-name fallback gets more room unless a long series needs it.
    private func nameWidth(columns: Int) -> CGFloat {
        let longest = state.teams.prefix(2).map(\.shortName.count).max() ?? 0
        if longest <= 5 { return layout == .glance ? 60 : 54 }
        return columns > 4 ? 64 : 92
    }

    var body: some View {
        let columns = columns
        VStack(alignment: .leading, spacing: layout.rowSpacing) {
            row(at: 0, columns: columns)
            row(at: 1, columns: columns)
        }
    }

    private func row(at index: Int, columns: [MatchLiveActivityScoreColumns.Column]) -> some View {
        let team = state.teams.indices.contains(index) ? state.teams[index] : nil
        return HStack(spacing: 8) {
            MatchLiveActivityLogo(team: team, size: layout.logoSize)
            Text(verbatim: team?.shortName ?? "—")
                .font(PrismWidgetFont.regular(layout.fontSize, relativeTo: .body))
                .foregroundStyle(Color(uiColor: teamColors.color(for: index)))
                .lineLimit(1)
                .minimumScaleFactor(0.6)
                .frame(width: nameWidth(columns: columns.count), alignment: .leading)
            HStack(spacing: 3) {
                ForEach(columns) { column in
                    cell(column, team: index)
                }
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityLabel(team: team, index: index, columns: columns))
    }

    @ViewBuilder
    private func cell(_ column: MatchLiveActivityScoreColumns.Column, team index: Int) -> some View {
        let value = column.values[index]
        switch column.kind {
        case .series:
            Text(verbatim: value)
                .foregroundStyle(column.winner == nil || column.winner == index ? palette.ink : palette.secondary)
                .modifier(CellFrame(layout: layout))
                .background(RoundedRectangle(cornerRadius: 5).fill(palette.ink.opacity(0.14)))
        case .map:
            Text(verbatim: value)
                .foregroundStyle(column.winner == index ? palette.ink : palette.secondary.opacity(0.75))
                .modifier(CellFrame(layout: layout))
        case .current:
            MatchLiveActivityRollingScore(value: value, height: layout.cellHeight)
                .foregroundStyle(palette.accent)
                .modifier(CellFrame(layout: layout))
                .background {
                    RoundedRectangle(cornerRadius: 5)
                        .fill(palette.accent.opacity(0.16))
                        .overlay(RoundedRectangle(cornerRadius: 5).strokeBorder(palette.accent.opacity(0.55), lineWidth: 1))
                }
        }
    }

    private func accessibilityLabel(
        team: MatchActivityAttributes.ContentState.Team?,
        index: Int,
        columns: [MatchLiveActivityScoreColumns.Column]
    ) -> String {
        let scores = columns.map { column -> String in
            let value = column.values[index]
            switch column.kind {
            case .series: return "series \(value)"
            case .map(let number): return "map \(number) \(value)"
            case .current(let number): return "map \(number) live \(value)"
            }
        }
        return ([team?.name ?? "—"] + scores).joined(separator: ", ")
    }

    private struct CellFrame: ViewModifier {
        let layout: Layout

        func body(content: Content) -> some View {
            content
                .font(PrismWidgetFont.regular(layout.fontSize, relativeTo: .body))
                .monospacedDigit()
                .lineLimit(1)
                .minimumScaleFactor(0.6)
                .frame(width: layout.cellWidth, height: layout.cellHeight)
        }
    }
}

/// Shows the live map name next to the match status.
@available(iOS 16.1, *)
private struct MatchLiveActivityStatusLine: View {
    let state: MatchActivityAttributes.ContentState
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        HStack(spacing: 6) {
            MatchLiveActivityStatus(state: state)
                .layoutPriority(1)
            if !state.terminal, let map = state.current_map?.name.trimmingCharacters(in: .whitespacesAndNewlines), !map.isEmpty {
                Text(verbatim: "·")
                    .accessibilityHidden(true)
                Text(verbatim: map.uppercased())
                    .lineLimit(1)
            }
        }
        .font(PrismWidgetFont.regular(11, relativeTo: .caption2))
        .foregroundStyle(colorScheme == .dark ? PrismWidgetPalette.dark.secondary : PrismWidgetPalette.light.secondary)
    }
}

/// Shows the stage even during a pause; the status label already names the pause.
@available(iOS 16.1, *)
struct MatchLiveActivityStageLabel: View {
    let state: MatchActivityAttributes.ContentState
    /// Falls back to the app name when the match has no stage.
    var showsBranding = false
    /// Draws the label in a dark pill for use over artwork.
    var isChip = false
    @Environment(\.colorScheme) private var colorScheme

    static func stage(for state: MatchActivityAttributes.ContentState) -> String? {
        guard let stage = state.stage?.trimmingCharacters(in: .whitespacesAndNewlines), !stage.isEmpty else { return nil }
        return stage
    }

    var body: some View {
        if let stage = Self.stage(for: state) {
            Text(verbatim: stage.count > 48 ? String(stage.prefix(47)) + "…" : stage)
                .truncationMode(.tail)
                .accessibilityLabel(Text(verbatim: stage))
                .modifier(Style(colorScheme: colorScheme, isChip: isChip))
        } else if showsBranding {
            Text(verbatim: "VAL ESPORTS")
                .fixedSize(horizontal: true, vertical: false)
                .modifier(Style(colorScheme: colorScheme, isChip: isChip))
        }
    }

    private struct Style: ViewModifier {
        let colorScheme: ColorScheme
        let isChip: Bool

        private var palette: PrismWidgetPalette { colorScheme == .dark ? .dark : .light }

        func body(content: Content) -> some View {
            let label = content
                .font(PrismWidgetFont.regular(11, relativeTo: .caption2))
                .foregroundStyle(isChip ? palette.ink : palette.secondary)
                .lineLimit(1)
            if isChip {
                label
                    .minimumScaleFactor(0.8)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(Capsule().fill(Color.black.opacity(0.72)))
                    .overlay(Capsule().strokeBorder(palette.border.opacity(0.8), lineWidth: 1))
            } else {
                label
            }
        }
    }
}

/// A light layout: status and stage, both teams with their scores, and the live map's artwork.
@available(iOS 16.1, *)
struct MatchLiveActivityGlance: View {
    let state: MatchActivityAttributes.ContentState
    let spoilersHidden: Bool
    @Environment(\.colorScheme) private var colorScheme

    private var palette: PrismWidgetPalette { colorScheme == .dark ? .dark : .light }
    private var artwork: UIImage? {
        state.terminal ? nil : MatchActivityMapArt.image(forMap: state.current_map?.name)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 9) {
            HStack(spacing: 6) {
                MatchLiveActivityStatusLine(state: state)
                    .layoutPriority(1)
                Spacer(minLength: 8)
                // Sits over the artwork, so a dark pill keeps it readable on bright maps.
                MatchLiveActivityStageLabel(state: state, isChip: true)
            }
            MatchLiveActivityStackedScores(state: state, spoilersHidden: spoilersHidden, layout: .glance)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(alignment: .trailing) {
            if let artwork {
                Color.clear
                    .frame(width: 200)
                    .overlay {
                        Image(uiImage: artwork)
                            .resizable()
                            .scaledToFill()
                    }
                    .clipped()
                    .mask(LinearGradient(
                        stops: [
                            .init(color: .clear, location: 0),
                            .init(color: .black.opacity(0.45), location: 0.45),
                            .init(color: .black.opacity(0.9), location: 1),
                        ],
                        startPoint: .leading,
                        endPoint: .trailing
                    ))
                    .accessibilityHidden(true)
            }
        }
        .foregroundStyle(palette.ink)
        .accessibilityElement(children: .contain)
        .accessibilityHint(String(localized: "Opens match details"))
    }
}

/// The classic layout's details in the stacked arrangement, with round progress per map on the right.
@available(iOS 16.1, *)
struct MatchLiveActivityScoreboard: View {
    let state: MatchActivityAttributes.ContentState
    let spoilersHidden: Bool
    @Environment(\.colorScheme) private var colorScheme

    private var palette: PrismWidgetPalette { colorScheme == .dark ? .dark : .light }
    private var teamColors: MatchActivityTeamColors {
        MatchActivityLogoCache.teamColors(for: state.teams.map(\.img), appearance: colorScheme == .dark ? .dark : .light)
    }

    var body: some View {
        let progress = MatchLiveActivityMapProgress(state: state, hidden: spoilersHidden)
        VStack(spacing: 9) {
            HStack(spacing: 6) {
                MatchLiveActivityStatusLine(state: state)
                    .layoutPriority(1)
                Spacer(minLength: 8)
                MatchLiveActivityStageLabel(state: state, showsBranding: true)
            }
            .lineLimit(1)
            HStack(alignment: .center, spacing: 12) {
                MatchLiveActivityStackedScores(
                    state: state,
                    spoilersHidden: spoilersHidden,
                    layout: .scoreboard,
                    maps: progress.map { Set($0.visibleMaps.map(\.number)) }
                )
                .layoutPriority(1)
                if let progress {
                    VStack(alignment: .leading, spacing: 6) {
                        ForEach(progress.visibleMaps) { map in
                            strip(for: map)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .animation(.easeInOut(duration: 0.3), value: progress.firstVisibleIndex)
                } else {
                    Spacer(minLength: 0)
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .foregroundStyle(palette.ink)
        .accessibilityElement(children: .contain)
        .accessibilityHint(String(localized: "Opens match details"))
    }

    private func strip(for map: MatchLiveActivityMapProgress.Map) -> some View {
        HStack(spacing: 6) {
            Text(verbatim: String(map.number))
                .font(PrismWidgetFont.regular(10, relativeTo: .caption2))
                .foregroundStyle(badgeInk(for: map.segment))
                .monospacedDigit()
                .frame(width: 15, height: 15)
                .background(RoundedRectangle(cornerRadius: 4).fill(badgeFill(for: map.segment)))
            MatchLiveActivityRoundStrip(rounds: map.rounds, palette: palette, teamColors: teamColors)
                .frame(height: 11)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(label(for: map))
        .transition(.asymmetric(insertion: .move(edge: .bottom), removal: .move(edge: .top)))
    }

    private func badgeFill(for segment: MatchLiveActivityMapProgress.Segment) -> Color {
        switch segment {
        case .wonBy(let index): return Color(uiColor: teamColors.color(for: index))
        case .active: return palette.accent.opacity(0.25)
        case .pending: return palette.border.opacity(0.45)
        }
    }

    private func badgeInk(for segment: MatchLiveActivityMapProgress.Segment) -> Color {
        switch segment {
        case .wonBy: return .black
        case .active: return palette.accent
        case .pending: return palette.secondary
        }
    }

    private func label(for map: MatchLiveActivityMapProgress.Map) -> String {
        if spoilersHidden { return "Map \(map.number), result hidden" }
        switch map.segment {
        case .wonBy(let index) where state.teams.indices.contains(index):
            return "Map \(map.number), \(state.teams[index].name) won, \(map.scoreText)"
        case .active:
            return "Map \(map.number), in progress, \(map.scoreText)"
        default:
            return "Map \(map.number), \(map.scoreText)"
        }
    }
}
