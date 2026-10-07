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
            let teamColors = MatchActivityLogoCache.teamColors(for: context.state.teams.map(\.img), appearance: .dark)
            return DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    MatchLiveActivityStatus(state: context.state)
                        .padding(.leading, 8)
                        .environment(\.colorScheme, .dark)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    MatchLiveActivityDetail(state: context.state)
                        .padding(.trailing, 8)
                        .environment(\.colorScheme, .dark)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    MatchLiveActivityScoreContent(state: context.state, spoilersHidden: hidden, isExpanded: true)
                        .padding(.horizontal, 16)
                        .padding(.top, 4)
                        .environment(\.colorScheme, .dark)
                }
            } compactLeading: {
                HStack(spacing: 4) {
                    MatchLiveActivityLogo(team: context.state.teams.first, size: 20)
                    MatchLiveActivityRollingScore(value: scores.primaryLeft, height: 20)
                }
                .font(PrismWidgetFont.regular(13, relativeTo: .caption))
                .foregroundStyle(Color(uiColor: teamColors.color(for: 0)))
                .monospacedDigit()
                .accessibilityElement(children: .combine)
                .environment(\.colorScheme, .dark)
            } compactTrailing: {
                HStack(spacing: 4) {
                    MatchLiveActivityRollingScore(value: scores.primaryRight, height: 20)
                    MatchLiveActivityLogo(team: context.state.teams.dropFirst().first, size: 20)
                }
                .font(PrismWidgetFont.regular(13, relativeTo: .caption))
                .foregroundStyle(Color(uiColor: teamColors.color(for: 1)))
                .monospacedDigit()
                .accessibilityElement(children: .combine)
                .environment(\.colorScheme, .dark)
            } minimal: {
                Image("LiveActivityAppLogo")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 20, height: 20)
                    .accessibilityLabel("VAL ESPORTS")
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
    private var mapProgress: MatchLiveActivityMapProgress? {
        MatchLiveActivityMapProgress(state: state, hidden: spoilersHidden)
    }

    var body: some View {
        VStack(spacing: mapProgress == nil ? 10 : 6) {
            HStack(spacing: 6) {
                MatchLiveActivityStatus(state: state)
                Spacer()
                MatchLiveActivityDetail(state: state)
            }
            .font(PrismWidgetFont.regular(11, relativeTo: .caption2))
            .lineLimit(1)

            MatchLiveActivityScoreContent(state: state, spoilersHidden: spoilersHidden, isExpanded: false)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, mapProgress == nil ? 14 : 10)
        .foregroundStyle(palette.ink)
        .accessibilityElement(children: .contain)
        .accessibilityHint(String(localized: "Opens match details"))
    }
}

@available(iOS 16.1, *)
private struct MatchLiveActivityStatus: View {
    let state: MatchActivityAttributes.ContentState
    @Environment(\.colorScheme) private var colorScheme

    private var statusLabel: String {
        if state.terminal { return String(localized: "FINAL") }
        switch state.pause?.kind {
        case .techPause: return String(localized: "TECHNICAL PAUSE")
        case .timeout: return String(localized: "TIMEOUT")
        case .halftime: return String(localized: "HALFTIME")
        case .paused: return String(localized: "PAUSED")
        case nil: return String(localized: "LIVE")
        }
    }

    var body: some View {
        HStack(spacing: 6) {
            if !state.terminal {
                if state.pause != nil {
                    Image(systemName: "pause.fill")
                        .accessibilityHidden(true)
                } else {
                    Circle().frame(width: 5, height: 5)
                        .accessibilityHidden(true)
                }
            }
            Text(verbatim: statusLabel)
                .minimumScaleFactor(0.7)
        }
        .font(PrismWidgetFont.regular(11, relativeTo: .caption2))
        .foregroundStyle(colorScheme == .dark ? PrismWidgetPalette.dark.accent : PrismWidgetPalette.light.accent)
        .lineLimit(1)
    }
}

@available(iOS 16.1, *)
struct MatchLiveActivityDetailContent: Equatable {
    enum Value: Equatable {
        case branding
        case stage(String)
        case pauseReason(String)
    }

    let value: Value

    init(state: MatchActivityAttributes.ContentState) {
        if !state.terminal, let pause = state.pause {
            if let reason = pause.reason, !reason.contains(where: \.isNewline) {
                value = .pauseReason(reason)
            } else {
                value = .branding
            }
        } else if let stage = state.stage?.trimmingCharacters(in: .whitespacesAndNewlines), !stage.isEmpty {
            value = .stage(stage)
        } else {
            value = .branding
        }
    }
}

@available(iOS 16.1, *)
private struct MatchLiveActivityDetail: View {
    let state: MatchActivityAttributes.ContentState
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        Group {
            switch MatchLiveActivityDetailContent(state: state).value {
            case .pauseReason(let reason):
                ViewThatFits(in: .horizontal) {
                    Text(verbatim: reason)
                        .fixedSize(horizontal: true, vertical: false)
                    Text(verbatim: "VAL ESPORTS")
                        .fixedSize(horizontal: true, vertical: false)
                }
            case .stage(let stage):
                Text(verbatim: stage)
                    .truncationMode(.tail)
            case .branding:
                Text(verbatim: "VAL ESPORTS")
                    .fixedSize(horizontal: true, vertical: false)
            }
        }
        .font(PrismWidgetFont.regular(11, relativeTo: .caption2))
        .foregroundStyle(colorScheme == .dark ? PrismWidgetPalette.dark.secondary : PrismWidgetPalette.light.secondary)
        .lineLimit(1)
    }
}

@available(iOS 16.1, *)
private struct MatchLiveActivityScoreContent: View {
    let state: MatchActivityAttributes.ContentState
    let spoilersHidden: Bool
    let isExpanded: Bool
    @Environment(\.colorScheme) private var colorScheme

    private var palette: PrismWidgetPalette { colorScheme == .dark ? .dark : .light }
    private var scores: MatchLiveActivityScores {
        MatchLiveActivityScores(state: state, hidden: spoilersHidden)
    }
    private var mapProgress: MatchLiveActivityMapProgress? {
        MatchLiveActivityMapProgress(state: state, hidden: spoilersHidden)
    }
    private var teamColors: MatchActivityTeamColors {
        MatchActivityLogoCache.teamColors(for: state.teams.map(\.img), appearance: colorScheme == .dark ? .dark : .light)
    }

    var body: some View {
        VStack(spacing: mapProgress == nil ? 10 : 6) {
            HStack(alignment: state.terminal ? .top : .center, spacing: 8) {
                team(at: 0)
                VStack(spacing: mapProgress == nil ? 3 : 1) {
                    if !state.terminal, let map = state.current_map {
                        Text(map.name)
                            .font(PrismWidgetFont.regular(mapProgress == nil ? 12 : 11, relativeTo: .caption))
                            .foregroundStyle(palette.secondary)
                            .lineLimit(1)
                            .minimumScaleFactor(0.7)
                    }
                    MatchLiveActivityScorePair(
                        left: scores.primaryLeft,
                        right: scores.primaryRight,
                        size: mapProgress != nil ? 36 : state.terminal ? (isExpanded ? 48 : 58) : (isExpanded ? 36 : 43),
                        color: palette.ink
                    )
                    .frame(height: mapProgress != nil ? 41 : state.terminal ? (isExpanded ? 52 : 58) : (isExpanded ? 41 : 48))
                    if !state.terminal, state.current_map != nil {
                        Text(scores.series)
                            .font(PrismWidgetFont.regular(mapProgress != nil ? 11 : isExpanded ? 12 : 14, relativeTo: .caption))
                            .foregroundStyle(palette.secondary)
                            .monospacedDigit()
                    }
                }
                .frame(width: state.terminal ? 112 : 100)
                .accessibilityElement(children: .combine)
                team(at: 1)
            }
            .frame(height: isExpanded ? (mapProgress == nil ? 88 : 72) : (mapProgress == nil ? 100 : 72))
            if let mapProgress {
                MatchLiveActivityMapProgressView(progress: mapProgress, state: state, palette: palette, teamColors: teamColors)
            }
        }
        .foregroundStyle(palette.ink)
        .accessibilityElement(children: .contain)
        .accessibilityHint(String(localized: "Opens match details"))
    }

    private func team(at index: Int) -> some View {
        let value = state.teams.indices.contains(index) ? state.teams[index] : nil
        return VStack(spacing: mapProgress == nil ? 7 : 4) {
            if let image = MatchActivityLogoCache.image(
                for: value?.img,
                appearance: colorScheme == .dark ? .dark : .light,
                size: .expanded
            ) {
                Image(uiImage: image)
                    .resizable()
                    .scaledToFit()
                    .frame(width: mapProgress != nil ? 38 : isExpanded ? 40 : 58, height: mapProgress != nil ? 38 : isExpanded ? 40 : 58)
                    .accessibilityHidden(true)
            }
            Text(value?.visibleName ?? "—")
                .accessibilityLabel(value?.name ?? "—")
                .font(PrismWidgetFont.regular(mapProgress == nil ? 13 : 12, relativeTo: .caption))
                .foregroundStyle(Color(uiColor: teamColors.color(for: index)))
                .lineLimit(mapProgress == nil ? 2 : 1)
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

    enum Round: Equatable {
        case wonBy(Int)
        case unknown
        case pending
    }

    struct Map: Identifiable, Equatable {
        let number: Int
        let segment: Segment
        let rounds: [Round]
        let scores: [Int?]

        var id: Int { number }
        var scoreText: String {
            if scores.allSatisfy({ $0 == nil }) { return "—" }
            return scores.map { $0.map(String.init) ?? "—" }.joined(separator: " : ")
        }
    }

    let maps: [Map]
    let firstVisibleIndex: Int
    let hidden: Bool

    var visibleMaps: ArraySlice<Map> { maps.dropFirst(firstVisibleIndex).prefix(3) }

    init?(state: MatchActivityAttributes.ContentState, hidden: Bool) {
        let total = max(state.total_maps ?? 0, state.map_winners.count,
                        state.map_round_winners.map(\.map_number).max() ?? 0,
                        state.current_map?.number ?? 0)
        guard (1...9).contains(total) else { return nil }
        self.hidden = hidden
        maps = (0..<total).map { index in
            let number = index + 1
            let current = state.current_map?.number == number
            let active = !state.terminal && current
            let history = state.map_round_winners.first { $0.map_number == number }?.winners ?? []
            var segment = Segment.pending
            if state.map_winners.indices.contains(index), let winner = state.map_winners[index] {
                let matchingTeams = state.teams.indices.prefix(2).filter { state.teams[$0].id == winner }
                if matchingTeams.count == 1 { segment = .wonBy(matchingTeams[0]) }
            }
            if segment == .pending && active { segment = .active }
            if hidden {
                return Map(number: number, segment: .pending,
                           rounds: Array(repeating: .pending, count: 12), scores: [nil, nil])
            }
            let knownHistory = !history.isEmpty && history.allSatisfy { $0 == 0 || $0 == 1 }
            let historyScores: [Int?] = knownHistory
                ? [history.filter { $0 == 0 }.count, history.filter { $0 == 1 }.count] : [nil, nil]
            let scores = current ? (0..<2).map { team in
                let values = state.current_map?.scores ?? []
                return values.indices.contains(team) ? values[team] : nil
            } : historyScores
            let currentRoundCount = current && scores.allSatisfy({ $0 != nil && $0! >= 0 })
                ? scores.compactMap { $0 }.reduce(0, +) : 0
            let played = max(history.count, currentRoundCount)
            let rounds: [Round] = (0..<max(12, played)).map { round in
                if round < history.count, let winner = history[round], (0...1).contains(winner) {
                    return .wonBy(winner)
                }
                return round < played ? .unknown : .pending
            }
            return Map(number: number, segment: segment, rounds: rounds, scores: scores)
        }
        let lastPlayed = maps.last { map in
            map.segment != .pending || map.rounds.contains { $0 != .pending }
        }?.number ?? 1
        let focus = state.terminal ? lastPlayed : state.current_map?.number ?? lastPlayed
        firstVisibleIndex = min(max(0, focus - 3), max(0, total - 3))
    }
}

@available(iOS 16.1, *)
private struct MatchLiveActivityMapProgressView: View {
    let progress: MatchLiveActivityMapProgress
    let state: MatchActivityAttributes.ContentState
    let palette: PrismWidgetPalette
    let teamColors: MatchActivityTeamColors

    var body: some View {
        HStack(spacing: 7) {
            ForEach(progress.visibleMaps) { map in
                VStack(spacing: 3) {
                    Text(verbatim: map.scoreText)
                        .font(PrismWidgetFont.regular(11, relativeTo: .caption2))
                        .foregroundStyle(palette.secondary)
                        .monospacedDigit()
                        .lineLimit(1)
                    GeometryReader { geometry in
                        HStack(spacing: min(3, geometry.size.width / CGFloat(map.rounds.count * 3))) {
                            ForEach(map.rounds.indices, id: \.self) { round in
                                RoundedRectangle(cornerRadius: 1.5)
                                    .fill(fill(for: map.rounds[round]))
                                    .frame(maxWidth: 6)
                                    .frame(maxWidth: .infinity)
                            }
                        }
                    }
                    .frame(height: 12)
                    .padding(.horizontal, 5)
                    .padding(.vertical, 4)
                    .background {
                        RoundedRectangle(cornerRadius: 6)
                            .strokeBorder(outline(for: map.segment), lineWidth: 1.4)
                    }
                }
                .frame(maxWidth: .infinity)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(label(for: map))
                .transition(.asymmetric(insertion: .move(edge: .trailing), removal: .move(edge: .leading)))
            }
        }
        .clipped()
        .animation(.easeInOut(duration: 0.3), value: progress.firstVisibleIndex)
        .accessibilityElement(children: .contain)
    }

    private func fill(for round: MatchLiveActivityMapProgress.Round) -> Color {
        switch round {
        case .wonBy(let index): return Color(uiColor: teamColors.color(for: index))
        case .unknown: return palette.secondary.opacity(0.6)
        case .pending: return palette.border.opacity(0.55)
        }
    }

    private func outline(for segment: MatchLiveActivityMapProgress.Segment) -> Color {
        if case .wonBy(let index) = segment { return Color(uiColor: teamColors.color(for: index)) }
        return palette.border.opacity(0.7)
    }

    private func label(for map: MatchLiveActivityMapProgress.Map) -> String {
        if progress.hidden { return "Map \(map.number), result hidden" }
        let status: String
        switch map.segment {
        case .wonBy(let index):
            status = "\(state.teams[index].name) won"
        case .active:
            status = "in progress"
        case .pending:
            status = "result unavailable"
        }
        let rounds = map.rounds.enumerated().compactMap { index, round -> String? in
            switch round {
            case .wonBy(let team):
                return "Round \(index + 1), \(state.teams.indices.contains(team) ? state.teams[team].name : "unknown team")"
            case .unknown: return "Round \(index + 1), winner unavailable"
            case .pending: return nil
            }
        }.joined(separator: ". ")
        return "Map \(map.number), \(status), \(map.scoreText). \(rounds)"
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
