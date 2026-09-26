import ActivityKit
import SwiftUI
import WidgetKit

/// Shows match updates on the Lock Screen and Dynamic Island.
@available(iOS 16.1, *)
struct MatchLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: MatchActivityAttributes.self) { context in
            MatchLiveActivityLockScreen(
                state: context.state,
                spoilersHidden: MatchLiveActivitySpoilerPreference.isHidden
            )
            .environment(\.colorScheme, .dark)
            .activityBackgroundTint(.black)
            .activitySystemActionForegroundColor(.white)
            .widgetURL(VLRWidgetContract.matchURL(id: context.attributes.match_id))
        } dynamicIsland: { context in
            let hidden = MatchLiveActivitySpoilerPreference.isHidden
            let scores = MatchLiveActivityScores(state: context.state, hidden: hidden)
            return DynamicIsland {
                DynamicIslandExpandedRegion(.bottom) {
                    MatchLiveActivityLockScreen(state: context.state, spoilersHidden: hidden)
                        .environment(\.colorScheme, .dark)
                }
            } compactLeading: {
                HStack(spacing: 4) {
                    MatchLiveActivityLogo(team: context.state.teams.first, size: 20)
                    MatchLiveActivityRollingScore(value: scores.primaryLeft, height: 20)
                }
                .font(PrismWidgetFont.regular(13, relativeTo: .caption))
                .foregroundStyle(PrismWidgetPalette.dark.accent)
                .monospacedDigit()
                .accessibilityElement(children: .combine)
                .environment(\.colorScheme, .dark)
            } compactTrailing: {
                HStack(spacing: 4) {
                    MatchLiveActivityRollingScore(value: scores.primaryRight, height: 20)
                    MatchLiveActivityLogo(team: context.state.teams.dropFirst().first, size: 20)
                }
                .font(PrismWidgetFont.regular(13, relativeTo: .caption))
                .foregroundStyle(PrismWidgetPalette.dark.accent)
                .monospacedDigit()
                .accessibilityElement(children: .combine)
                .environment(\.colorScheme, .dark)
            } minimal: {
                MatchLiveActivityLogo(team: context.state.teams.first, size: 20)
                    .environment(\.colorScheme, .dark)
            }
            .keylineTint(PrismWidgetPalette.dark.accent)
            .widgetURL(VLRWidgetContract.matchURL(id: context.attributes.match_id))
        }
    }
}

/// Displays teams and scores for a live or finished match.
@available(iOS 16.1, *)
struct MatchLiveActivityLockScreen: View {
    let state: MatchActivityAttributes.ContentState
    let spoilersHidden: Bool
    @Environment(\.colorScheme) private var colorScheme

    private var palette: PrismWidgetPalette { colorScheme == .dark ? .dark : .light }
    private var scores: MatchLiveActivityScores {
        MatchLiveActivityScores(state: state, hidden: spoilersHidden)
    }
    private var mapProgress: MatchLiveActivityMapProgress? {
        MatchLiveActivityMapProgress(state: state, hidden: spoilersHidden)
    }

    var body: some View {
        VStack(spacing: mapProgress == nil ? 10 : 7) {
            HStack(spacing: 6) {
                if !state.terminal {
                    Circle().fill(palette.accent).frame(width: 5, height: 5)
                        .accessibilityHidden(true)
                }
                Text(state.terminal ? String(localized: "FINAL") : String(localized: "LIVE"))
                    .foregroundStyle(palette.accent)
                Spacer()
                Text(verbatim: "VAL ESPORTS")
                    .foregroundStyle(palette.secondary)
            }
            .font(PrismWidgetFont.regular(11, relativeTo: .caption2))
            .lineLimit(1)

            HStack(alignment: state.terminal ? .top : .center, spacing: 8) {
                team(at: 0)
                VStack(spacing: 3) {
                    if !state.terminal, let map = state.current_map {
                        Text(map.name)
                            .font(PrismWidgetFont.regular(12, relativeTo: .caption))
                            .foregroundStyle(palette.secondary)
                            .lineLimit(1)
                            .minimumScaleFactor(0.7)
                    }
                    MatchLiveActivityScorePair(
                        left: scores.primaryLeft,
                        right: scores.primaryRight,
                        size: state.terminal ? 58 : 43,
                        color: palette.ink
                    )
                    .frame(height: state.terminal ? 58 : 48)
                    if !state.terminal, state.current_map != nil {
                        Text(scores.series)
                            .font(PrismWidgetFont.regular(14, relativeTo: .caption))
                            .foregroundStyle(palette.secondary)
                            .monospacedDigit()
                    }
                }
                .frame(width: state.terminal ? 112 : 100)
                .accessibilityElement(children: .combine)
                team(at: 1)
            }
            .frame(height: mapProgress == nil ? 100 : 88)
            if let mapProgress {
                MatchLiveActivityMapProgressView(progress: mapProgress, state: state, palette: palette)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, mapProgress == nil ? 14 : 11)
        .foregroundStyle(palette.ink)
        .accessibilityElement(children: .contain)
        .accessibilityHint(String(localized: "Opens match details"))
    }

    private func team(at index: Int) -> some View {
        let value = state.teams.indices.contains(index) ? state.teams[index] : nil
        return VStack(spacing: 7) {
            if let image = MatchActivityLogoCache.image(
                for: value?.img,
                appearance: colorScheme == .dark ? .dark : .light,
                size: .expanded
            ) {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 58, height: 58)
                    .accessibilityHidden(true)
            }
            Text(value?.visibleName ?? "—")
                .accessibilityLabel(value?.name ?? "—")
                .font(PrismWidgetFont.regular(13, relativeTo: .caption))
                .foregroundStyle(MatchLiveActivityTeamColors.color(for: index, scheme: colorScheme))
                .lineLimit(2)
                .minimumScaleFactor(0.75)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

@available(iOS 16.1, *)
struct MatchLiveActivityMapProgress {
    enum Segment: Equatable {
        case wonBy(Int)
        case active
        case pending
    }

    let segments: [Segment]
    let hidden: Bool

    init?(state: MatchActivityAttributes.ContentState, hidden: Bool) {
        guard let total = state.total_maps, (1...9).contains(total) else { return nil }
        self.hidden = hidden
        segments = (0..<total).map { index in
            if hidden { return .pending }
            if state.map_winners.indices.contains(index), let winner = state.map_winners[index] {
                let matchingTeams = state.teams.indices.filter { state.teams[$0].id == winner }
                if matchingTeams.count == 1 { return .wonBy(matchingTeams[0]) }
            }
            if !state.terminal, state.current_map?.number == index + 1 { return .active }
            return .pending
        }
    }
}

@available(iOS 16.1, *)
private struct MatchLiveActivityMapProgressView: View {
    let progress: MatchLiveActivityMapProgress
    let state: MatchActivityAttributes.ContentState
    let palette: PrismWidgetPalette
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        HStack(spacing: 5) {
            ForEach(progress.segments.indices, id: \.self) { index in
                let segment = progress.segments[index]
                Capsule()
                    .fill(fill(for: segment))
                    .overlay {
                        if segment == .active {
                            Capsule().strokeBorder(palette.accent, lineWidth: 1.2)
                            Circle().fill(palette.accent).frame(width: 4, height: 4)
                        }
                    }
                    .frame(height: 7)
                    .accessibilityLabel(label(for: segment, map: index + 1))
            }
        }
        .accessibilityElement(children: .contain)
    }

    private func fill(for segment: MatchLiveActivityMapProgress.Segment) -> Color {
        if case .wonBy(let index) = segment {
            return MatchLiveActivityTeamColors.color(for: index, scheme: colorScheme)
        }
        return palette.border.opacity(0.55)
    }

    private func label(for segment: MatchLiveActivityMapProgress.Segment, map: Int) -> String {
        switch segment {
        case .wonBy(let index):
            return "Map \(map), \(state.teams[index].name) won"
        case .active:
            return "Map \(map) in progress"
        case .pending:
            return progress.hidden ? "Map \(map), result hidden" : "Map \(map), result unavailable"
        }
    }
}

private enum MatchLiveActivityTeamColors {
    static func color(for index: Int, scheme: ColorScheme) -> Color {
        if scheme == .dark {
            return index == 0
                ? Color(red: 207.0 / 255.0, green: 178.0 / 255.0, blue: 1)
                : Color(red: 100.0 / 255.0, green: 218.0 / 255.0, blue: 199.0 / 255.0)
        }
        return index == 0
            ? Color(red: 103.0 / 255.0, green: 58.0 / 255.0, blue: 183.0 / 255.0)
            : Color(red: 0, green: 105.0 / 255.0, blue: 92.0 / 255.0)
    }
}

/// Displays a cached team logo or falls back to team initials.
@available(iOS 16.1, *)
private struct MatchLiveActivityLogo: View {
    let team: MatchActivityAttributes.ContentState.Team?
    let size: CGFloat
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        Group {
            if let image = MatchActivityLogoCache.image(
                for: team?.img,
                appearance: colorScheme == .dark ? .dark : .light,
                size: size <= 20 ? .compact : .expanded
            ) {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFit()
            } else {
                Text(team?.logoInitials ?? "—")
                    .font(PrismWidgetFont.regular(size * 0.45, relativeTo: .headline))
                    .foregroundStyle(colorScheme == .dark ? PrismWidgetPalette.dark.accent : PrismWidgetPalette.light.accent)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
            }
        }
        .frame(width: size, height: size)
        .accessibilityLabel(team?.name ?? "—")
    }
}

/// Displays two scores with a shared accessibility label.
private struct MatchLiveActivityScorePair: View {
    let left: String
    let right: String
    let size: CGFloat
    let color: Color

    var body: some View {
        HStack(spacing: 4) {
            MatchLiveActivityRollingScore(value: left, height: size * 1.15)
                .frame(maxWidth: .infinity, alignment: .trailing)
            VStack(spacing: 5) {
                Circle().frame(width: 2, height: 2)
                Circle().frame(width: 2, height: 2)
            }
            .frame(width: 5)
            .accessibilityHidden(true)
            MatchLiveActivityRollingScore(value: right, height: size * 1.15)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .font(PrismWidgetFont.regular(size, relativeTo: .largeTitle))
        .foregroundStyle(color)
        .monospacedDigit()
        .lineLimit(1)
        .minimumScaleFactor(0.6)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(left) – \(right)")
    }
}

private struct MatchLiveActivityRollingScore: View {
    let value: String
    let height: CGFloat

    var body: some View {
        ZStack {
            Text(value)
                .id(value)
                .transition(.push(from: .bottom))
        }
        .frame(height: height)
        .clipped()
        .animation(.easeInOut(duration: 0.45), value: value)
    }
}

/// Formats match scores according to the match phase and spoiler preference.
@available(iOS 16.1, *)
struct MatchLiveActivityScores {
    let state: MatchActivityAttributes.ContentState
    let hidden: Bool

    var primaryLeft: String { display(primary, at: 0) }
    var primaryRight: String { display(primary, at: 1) }
    var series: String { "\(display(seriesScores, at: 0)) – \(display(seriesScores, at: 1))" }

    private var seriesScores: [Int?] { state.teams.map(\.score) }
    /// Uses map scores during play and series scores once the match finishes.
    /// Falls back to series scores when current map data is unavailable.
    private var primary: [Int?] {
        state.terminal ? seriesScores : state.current_map?.scores ?? seriesScores
    }

    private func display(_ values: [Int?], at index: Int) -> String {
        guard !hidden, values.indices.contains(index), let value = values[index] else { return "—" }
        return String(value)
    }
}

/// Reads the spoiler preference shared with the app.
private enum MatchLiveActivitySpoilerPreference {
    static var isHidden: Bool {
        WidgetSnapshotRepository().loadSource()?.spoilersHidden ?? true
    }
}
