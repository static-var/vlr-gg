import ActivityKit
import SwiftUI
import WidgetKit

@available(iOS 16.1, *)
struct MatchLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: MatchActivityAttributes.self) { context in
            let spoilersHidden = MatchLiveActivitySpoilerPreference.isHidden
            MatchLiveActivityView(state: context.state, spoilersHidden: spoilersHidden)
                .widgetURL(VLRWidgetContract.matchURL(id: context.attributes.match_id))
                .activityBackgroundTint(PrismWidgetPalette.dark.background)
                .activitySystemActionForegroundColor(PrismWidgetPalette.dark.accent)
        } dynamicIsland: { context in
            let spoilersHidden = MatchLiveActivitySpoilerPreference.isHidden
            return DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    MatchLiveActivityTeamName(team: context.state.teams.first)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    MatchLiveActivityScores(state: context.state, spoilersHidden: spoilersHidden)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    MatchLiveActivityMapSummary(state: context.state, spoilersHidden: spoilersHidden)
                }
            } compactLeading: {
                MatchLiveActivityTeamName(team: context.state.teams.first)
            } compactTrailing: {
                MatchLiveActivityScores(state: context.state, spoilersHidden: spoilersHidden)
            } minimal: {
                Image(systemName: context.state.terminal ? "checkmark" : "dot.radiowaves.left.and.right")
                    .foregroundStyle(PrismWidgetPalette.dark.accent)
            }
            .keylineTint(PrismWidgetPalette.dark.accent)
            .widgetURL(VLRWidgetContract.matchURL(id: context.attributes.match_id))
        }
    }
}

@available(iOS 16.1, *)
private struct MatchLiveActivityView: View {
    let state: MatchActivityAttributes.ContentState
    let spoilersHidden: Bool

    private let palette = PrismWidgetPalette.dark

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                Image(systemName: state.terminal ? "checkmark" : "dot.radiowaves.left.and.right")
                    .foregroundStyle(palette.accent)
                MatchLiveActivityMapSummary(state: state, spoilersHidden: spoilersHidden)
                Spacer(minLength: 8)
                MatchLiveActivityScores(state: state, spoilersHidden: spoilersHidden)
            }

            ForEach(Array(state.teams.prefix(2).enumerated()), id: \.offset) { _, team in
                HStack(spacing: 8) {
                    Text(team.name)
                        .font(PrismWidgetFont.regular(16, relativeTo: .headline))
                        .foregroundStyle(palette.ink)
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                    Spacer(minLength: 8)
                    Text(spoilersHidden ? "–" : team.score.map(String.init) ?? "–")
                        .font(PrismWidgetFont.regular(20, relativeTo: .title2))
                        .foregroundStyle(palette.accent)
                        .monospacedDigit()
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }
}

@available(iOS 16.1, *)
private struct MatchLiveActivityTeamName: View {
    let team: MatchActivityAttributes.ContentState.Team?

    @ViewBuilder
    var body: some View {
        if let team {
            Text(team.name)
                .font(PrismWidgetFont.regular(13, relativeTo: .caption))
                .lineLimit(1)
                .minimumScaleFactor(0.65)
        }
    }
}

@available(iOS 16.1, *)
private struct MatchLiveActivityScores: View {
    let state: MatchActivityAttributes.ContentState
    let spoilersHidden: Bool

    var body: some View {
        Text(scoreText)
            .font(PrismWidgetFont.regular(15, relativeTo: .headline))
            .monospacedDigit()
            .lineLimit(1)
    }

    private var scoreText: String {
        guard !spoilersHidden else { return "–:–" }
        return state.teams.prefix(2).map { $0.score.map(String.init) ?? "–" }.joined(separator: ":")
    }
}

@available(iOS 16.1, *)
private struct MatchLiveActivityMapSummary: View {
    let state: MatchActivityAttributes.ContentState
    let spoilersHidden: Bool

    var body: some View {
        HStack(spacing: 6) {
            if let map = state.current_map {
                Text(map.name)
                    .lineLimit(1)
                if !spoilersHidden, !map.scores.isEmpty {
                    Text(map.scores.map { $0.map(String.init) ?? "–" }.joined(separator: ":"))
                        .monospacedDigit()
                        .lineLimit(1)
                }
            }
        }
        .font(PrismWidgetFont.regular(12, relativeTo: .caption))
    }
}

private enum MatchLiveActivitySpoilerPreference {
    static var isHidden: Bool {
        WidgetSnapshotRepository().loadSource()?.spoilersHidden ?? true
    }
}
