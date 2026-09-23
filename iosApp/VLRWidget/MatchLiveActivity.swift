import ActivityKit
import SwiftUI
import WidgetKit

@available(iOS 16.1, *)
struct MatchLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: MatchActivityAttributes.self) { context in
            MatchLiveActivityLockScreen(
                state: context.state,
                spoilersHidden: MatchLiveActivitySpoilerPreference.isHidden
            )
            .widgetURL(VLRWidgetContract.matchURL(id: context.attributes.match_id))
        } dynamicIsland: { context in
            let hidden = MatchLiveActivitySpoilerPreference.isHidden
            return DynamicIsland {
                DynamicIslandExpandedRegion(.bottom) {
                    MatchLiveActivityLockScreen(state: context.state, spoilersHidden: hidden)
                        .environment(\.colorScheme, .dark)
                }
            } compactLeading: {
                HStack(spacing: 4) {
                    MatchLiveActivityLogo(team: context.state.teams.first, size: 20)
                    MatchLiveActivityLogo(team: context.state.teams.dropFirst().first, size: 20)
                }
                .environment(\.colorScheme, .dark)
            } compactTrailing: {
                Text(MatchLiveActivityScores(state: context.state, hidden: hidden).series)
                    .font(PrismWidgetFont.regular(13, relativeTo: .caption))
                    .foregroundStyle(PrismWidgetPalette.dark.accent)
                    .monospacedDigit()
            } minimal: {
                MatchLiveActivityLogo(team: context.state.teams.first, size: 20)
                    .environment(\.colorScheme, .dark)
            }
            .keylineTint(PrismWidgetPalette.dark.accent)
            .widgetURL(VLRWidgetContract.matchURL(id: context.attributes.match_id))
        }
    }
}

@available(iOS 16.1, *)
struct MatchLiveActivityLockScreen: View {
    let state: MatchActivityAttributes.ContentState
    let spoilersHidden: Bool
    @Environment(\.colorScheme) private var colorScheme

    private var palette: PrismWidgetPalette { colorScheme == .dark ? .dark : .light }
    private var scores: MatchLiveActivityScores {
        MatchLiveActivityScores(state: state, hidden: spoilersHidden)
    }

    var body: some View {
        VStack(spacing: 10) {
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
            .frame(height: 100)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .foregroundStyle(palette.ink)
        .accessibilityElement(children: .contain)
        .accessibilityHint(String(localized: "Opens match details"))
    }

    private func team(at index: Int) -> some View {
        let value = state.teams.indices.contains(index) ? state.teams[index] : nil
        return VStack(spacing: 7) {
            MatchLiveActivityLogo(team: value, size: 58)
                .accessibilityHidden(true)
            Text(value?.visibleName ?? "—")
                .accessibilityLabel(value?.name ?? "—")
                .font(PrismWidgetFont.regular(13, relativeTo: .caption))
                .lineLimit(2)
                .minimumScaleFactor(0.75)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
    }
}

@available(iOS 16.1, *)
private struct MatchLiveActivityLogo: View {
    let team: MatchActivityAttributes.ContentState.Team?
    let size: CGFloat
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        Group {
            if let image = MatchActivityLogoCache.image(for: team?.img) {
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

private struct MatchLiveActivityScorePair: View {
    let left: String
    let right: String
    let size: CGFloat
    let color: Color

    var body: some View {
        HStack(spacing: 4) {
            Text(left).frame(maxWidth: .infinity, alignment: .trailing)
            VStack(spacing: 5) {
                Circle().frame(width: 2, height: 2)
                Circle().frame(width: 2, height: 2)
            }
            .frame(width: 5)
            .accessibilityHidden(true)
            Text(right).frame(maxWidth: .infinity, alignment: .leading)
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

@available(iOS 16.1, *)
struct MatchLiveActivityScores {
    let state: MatchActivityAttributes.ContentState
    let hidden: Bool

    var primaryLeft: String { display(primary, at: 0) }
    var primaryRight: String { display(primary, at: 1) }
    var series: String { "\(display(seriesScores, at: 0)) – \(display(seriesScores, at: 1))" }

    private var seriesScores: [Int?] { state.teams.map(\.score) }
    private var primary: [Int?] {
        state.terminal ? seriesScores : state.current_map?.scores ?? seriesScores
    }

    private func display(_ values: [Int?], at index: Int) -> String {
        guard !hidden, values.indices.contains(index), let value = values[index] else { return "—" }
        return String(value)
    }
}

private enum MatchLiveActivitySpoilerPreference {
    static var isHidden: Bool {
        WidgetSnapshotRepository().loadSource()?.spoilersHidden ?? true
    }
}
