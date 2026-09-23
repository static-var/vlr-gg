import ActivityKit
import SwiftUI
import WidgetKit

@available(iOS 16.1, *)
struct MatchLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: MatchActivityAttributes.self) { context in
            let spoilersHidden = MatchLiveActivitySpoilerPreference.isHidden
            MatchLiveActivityLockScreen(
                state: context.state,
                spoilersHidden: spoilersHidden
            )
            .widgetURL(VLRWidgetContract.matchURL(id: context.attributes.match_id))
            .activityBackgroundTint(PrismWidgetPalette.dark.background)
            .activitySystemActionForegroundColor(PrismWidgetPalette.dark.accent)
        } dynamicIsland: { context in
            let spoilersHidden = MatchLiveActivitySpoilerPreference.isHidden
            return DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    MatchLiveActivityStatus(terminal: context.state.terminal)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    MatchLiveActivityMapSummary(
                        map: context.state.current_map,
                        spoilersHidden: spoilersHidden
                    )
                }
                DynamicIslandExpandedRegion(.bottom) {
                    MatchLiveActivityTeams(
                        state: context.state,
                        spoilersHidden: spoilersHidden,
                        presentation: .island
                    )
                    .padding(.top, 4)
                }
            } compactLeading: {
                MatchLiveActivityCompactTeams(state: context.state)
            } compactTrailing: {
                MatchLiveActivitySeriesScore(
                    state: context.state,
                    spoilersHidden: spoilersHidden
                )
            } minimal: {
                Image(systemName: context.state.terminal ? "checkmark" : "circle.fill")
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundStyle(PrismWidgetPalette.dark.accent)
                    .accessibilityLabel(
                        context.state.terminal
                            ? String(localized: "FINAL")
                            : String(localized: "LIVE")
                    )
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

    private let palette = PrismWidgetPalette.dark

    var body: some View {
        VStack(spacing: 9) {
            HStack(spacing: 10) {
                MatchLiveActivityStatus(terminal: state.terminal)
                Spacer(minLength: 8)
                MatchLiveActivityMapSummary(
                    map: state.current_map,
                    spoilersHidden: spoilersHidden
                )
            }

            Rectangle()
                .fill(palette.border)
                .frame(height: 1)

            MatchLiveActivityTeams(
                state: state,
                spoilersHidden: spoilersHidden,
                presentation: .lockScreen
            )
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .foregroundStyle(palette.ink)
        .accessibilityElement(children: .contain)
        .accessibilityHint(String(localized: "Opens match details"))
    }
}

@available(iOS 16.1, *)
private struct MatchLiveActivityStatus: View {
    let terminal: Bool

    private let palette = PrismWidgetPalette.dark

    var body: some View {
        HStack(spacing: 6) {
            if terminal {
                Image(systemName: "checkmark")
                    .font(.system(size: 9, weight: .bold))
            } else {
                Rectangle()
                    .frame(width: 5, height: 5)
                    .accessibilityHidden(true)
            }

            Text(statusText)
                .font(PrismWidgetFont.regular(11, relativeTo: .caption2))
                .tracking(0.8)
                .lineLimit(1)
                .minimumScaleFactor(0.72)
        }
        .foregroundStyle(palette.accent)
        .widgetAccentable()
    }

    private var statusText: String {
        terminal ? String(localized: "FINAL") : String(localized: "LIVE")
    }
}

@available(iOS 16.1, *)
private struct MatchLiveActivityMapSummary: View {
    let map: MatchActivityAttributes.ContentState.CurrentMap?
    let spoilersHidden: Bool

    private let palette = PrismWidgetPalette.dark

    var body: some View {
        if let map {
            HStack(spacing: 7) {
                Text(map.name)
                    .font(PrismWidgetFont.regular(12, relativeTo: .caption))
                    .foregroundStyle(palette.secondary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)

                Text(roundScore(map.scores))
                    .font(PrismWidgetFont.regular(12, relativeTo: .caption))
                    .foregroundStyle(palette.ink)
                    .monospacedDigit()
                    .lineLimit(1)
                    .padding(.horizontal, 7)
                    .padding(.vertical, 3)
                    .background {
                        Capsule()
                            .fill(palette.border.opacity(0.55))
                    }
            }
            .accessibilityElement(children: .combine)
        }
    }

    private func roundScore(_ scores: [Int?]) -> String {
        guard !spoilersHidden else { return "—–—" }
        return [score(at: 0, in: scores), score(at: 1, in: scores)]
            .joined(separator: "–")
    }

    private func score(at index: Int, in scores: [Int?]) -> String {
        guard scores.indices.contains(index), let score = scores[index] else { return "—" }
        return String(score)
    }
}

@available(iOS 16.1, *)
private struct MatchLiveActivityTeams: View {
    enum Presentation {
        case lockScreen
        case island

        var nameSize: CGFloat {
            switch self {
            case .lockScreen: 17
            case .island: 15
            }
        }

        var scoreSize: CGFloat {
            switch self {
            case .lockScreen: 23
            case .island: 20
            }
        }

        var rowHeight: CGFloat {
            switch self {
            case .lockScreen: 27
            case .island: 23
            }
        }
    }

    let state: MatchActivityAttributes.ContentState
    let spoilersHidden: Bool
    let presentation: Presentation

    private let palette = PrismWidgetPalette.dark

    var body: some View {
        VStack(spacing: presentation == .lockScreen ? 5 : 3) {
            teamRow(at: 0)
            teamRow(at: 1)
        }
    }

    private func teamRow(at index: Int) -> some View {
        let team = state.teams.indices.contains(index) ? state.teams[index] : nil
        return HStack(spacing: 10) {
            Text(team?.name ?? "—")
                .font(PrismWidgetFont.regular(presentation.nameSize, relativeTo: .headline))
                .foregroundStyle(palette.ink)
                .lineLimit(1)
                .minimumScaleFactor(0.65)
                .frame(maxWidth: .infinity, alignment: .leading)

            Text(displayScore(team?.score))
                .font(PrismWidgetFont.regular(presentation.scoreSize, relativeTo: .title2))
                .foregroundStyle(palette.accent)
                .monospacedDigit()
                .lineLimit(1)
                .frame(minWidth: 28, alignment: .trailing)
                .widgetAccentable()
                .accessibilityLabel(scoreAccessibilityLabel(team?.score))
        }
        .frame(minHeight: presentation.rowHeight)
    }

    private func displayScore(_ score: Int?) -> String {
        guard !spoilersHidden, let score else { return "—" }
        return String(score)
    }

    private func scoreAccessibilityLabel(_ score: Int?) -> String {
        guard !spoilersHidden else { return String(localized: "Score hidden") }
        guard let score else { return String(localized: "Score unavailable") }
        return String(
            format: String(localized: "widget.score_format"),
            locale: .current,
            Int64(score)
        )
    }
}

@available(iOS 16.1, *)
private struct MatchLiveActivityCompactTeams: View {
    let state: MatchActivityAttributes.ContentState

    var body: some View {
        HStack(spacing: 5) {
            if !state.terminal {
                Rectangle()
                    .fill(PrismWidgetPalette.dark.accent)
                    .frame(width: 4, height: 4)
                    .accessibilityHidden(true)
            }

            Text(compactTeamNames)
                .font(PrismWidgetFont.regular(12, relativeTo: .caption))
                .foregroundStyle(PrismWidgetPalette.dark.ink)
                .lineLimit(1)
        }
        .accessibilityLabel(fullTeamNames)
    }

    private var compactTeamNames: String {
        [compactTeamName(at: 0), compactTeamName(at: 1)].joined(separator: "·")
    }

    private var fullTeamNames: String {
        [teamName(at: 0), teamName(at: 1)].joined(separator: ", ")
    }

    private func compactTeamName(at index: Int) -> String {
        let name = teamName(at: index)
        let words = name.split(whereSeparator: { $0.isWhitespace })
        if words.count > 1 {
            return words.prefix(2)
                .map { String($0.prefix(1)) }
                .joined()
                .uppercased()
        }
        return String(name.prefix(2)).uppercased()
    }

    private func teamName(at index: Int) -> String {
        guard state.teams.indices.contains(index) else { return "—" }
        return state.teams[index].name
    }
}

@available(iOS 16.1, *)
private struct MatchLiveActivitySeriesScore: View {
    let state: MatchActivityAttributes.ContentState
    let spoilersHidden: Bool

    var body: some View {
        Text(scoreText)
            .font(PrismWidgetFont.regular(13, relativeTo: .headline))
            .foregroundStyle(PrismWidgetPalette.dark.accent)
            .monospacedDigit()
            .lineLimit(1)
            .widgetAccentable()
    }

    private var scoreText: String {
        guard !spoilersHidden else { return "—–—" }
        return [teamScore(at: 0), teamScore(at: 1)].joined(separator: "–")
    }

    private func teamScore(at index: Int) -> String {
        guard state.teams.indices.contains(index), let score = state.teams[index].score else {
            return "—"
        }
        return String(score)
    }
}

private enum MatchLiveActivitySpoilerPreference {
    static var isHidden: Bool {
        WidgetSnapshotRepository().loadSource()?.spoilersHidden ?? true
    }
}
