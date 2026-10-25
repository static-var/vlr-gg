import SwiftUI
import WidgetKit

struct LockScreenMatchEntry: TimelineEntry {
    let date: Date
    let snapshot: UpcomingMatchesSnapshot?
}

struct LockScreenMatchProvider: TimelineProvider {
    private let repository: WidgetSnapshotRepository
    private let refreshService: WidgetRefreshService

    init(
        repository: WidgetSnapshotRepository = WidgetSnapshotRepository(),
        refreshService: WidgetRefreshService = WidgetRefreshService()
    ) {
        self.repository = repository
        self.refreshService = refreshService
    }

    func placeholder(in context: Context) -> LockScreenMatchEntry {
        LockScreenMatchEntry(date: .now, snapshot: .preview)
    }

    func getSnapshot(in context: Context, completion: @escaping (LockScreenMatchEntry) -> Void) {
        let snapshot = repository.loadBestSnapshot() ?? (context.isPreview ? .preview : nil)
        completion(LockScreenMatchEntry(date: .now, snapshot: snapshot))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<LockScreenMatchEntry>) -> Void) {
        Task {
            let now = Date.now
            let outcome = await WidgetRefreshCoordinator.shared.refresh(
                repository: repository, service: refreshService, now: now
            )
            await MatchActivityTeamCodes.prefetch()
            let expiry = LockScreenMatchState.make(snapshot: outcome.snapshot, now: now).expiry(now: now)
            let entry = LockScreenMatchEntry(date: now, snapshot: outcome.snapshot)
            completion(Timeline(entries: [entry], policy: .after(min(outcome.refreshDate, expiry ?? .distantFuture))))
        }
    }
}

struct LockScreenMatchWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: VLRWidgetContract.lockScreenWidgetKind, provider: LockScreenMatchProvider()) { entry in
            LockScreenMatchWidgetView(entry: entry)
        }
        .configurationDisplayName(LocalizedStringResource("Favorite match"))
        .description(LocalizedStringResource("Your most recent favorite match: live score, result or kickoff."))
        .supportedFamilies([.accessoryInline, .accessoryRectangular])
    }
}

struct LockScreenMatchWidgetView: View {
    @Environment(\.widgetFamily) private var environmentFamily
    @Environment(\.locale) private var locale
    @Environment(\.timeZone) private var timeZone

    let entry: LockScreenMatchEntry
    let familyOverride: WidgetFamily?

    init(entry: LockScreenMatchEntry, familyOverride: WidgetFamily? = nil) {
        self.entry = entry
        self.familyOverride = familyOverride
    }

    var body: some View {
        let state = LockScreenMatchState.make(snapshot: entry.snapshot, now: entry.date)
        let text = LockScreenMatchText(
            state: state,
            now: entry.date,
            locale: locale,
            timeZone: timeZone,
            spoilersHidden: entry.snapshot?.spoilersHidden ?? false
        )
        Group {
            if (familyOverride ?? environmentFamily) == .accessoryInline {
                Label(text.inline, systemImage: state.symbol)
            } else {
                LockScreenRectangularView(content: text.rectangular, now: entry.date)
            }
        }
        .widgetURL(state.url)
        .lockScreenBackground()
    }
}

private struct LockScreenRectangularView: View {
    let content: LockScreenMatchText.Rectangular
    let now: Date

    var body: some View {
        VStack(alignment: .leading, spacing: 1) {
            switch content {
            case .live(let scoreline, let caption):
                LockScreenScoreline(scoreline: scoreline, dimsLoser: false)
                captionText(caption)
            case .upcoming(let matchup, let kickoff, let caption):
                HStack(spacing: 4) {
                    Image(systemName: "clock")
                    Text(verbatim: matchup)
                }
                .font(.system(size: 15, weight: .semibold))
                switch kickoff {
                case .countdown(let start):
                    Text(timerInterval: now...start, countsDown: true)
                        .font(.system(size: 22, weight: .bold, design: .rounded))
                        .monospacedDigit()
                        .multilineTextAlignment(.leading)
                case .text(let value):
                    Text(verbatim: value).font(.system(size: 15))
                }
                captionText(caption)
            case .completed(let scoreline, let status, let caption, let next):
                HStack(alignment: .firstTextBaseline, spacing: 4) {
                    LockScreenScoreline(scoreline: scoreline, dimsLoser: true)
                    Spacer(minLength: 4)
                    Text(verbatim: status)
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(.secondary)
                }
                captionText(caption)
                if let next {
                    Text(verbatim: next).font(.system(size: 11)).foregroundStyle(.primary)
                }
            case .message(let title, let caption):
                Text(verbatim: title).font(.headline)
                captionText(caption).lineLimit(2)
            }
        }
        .lineLimit(1)
        .minimumScaleFactor(0.7)
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .combine)
    }

    private func captionText(_ value: String) -> some View {
        Text(verbatim: value)
            .font(.system(size: 11))
            .foregroundStyle(.secondary)
    }
}

private struct LockScreenScoreline: View {
    let scoreline: LockScreenMatchText.Scoreline
    let dimsLoser: Bool

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 6) {
            code(scoreline.team1, side: .team1)
            Text(verbatim: scoreline.center)
                .font(.system(size: 24, weight: .bold, design: .rounded))
                .monospacedDigit()
            code(scoreline.team2, side: .team2)
        }
    }

    private func code(_ value: String, side: LockScreenMatchText.Side) -> some View {
        let dimmed = dimsLoser && scoreline.winner.map { $0 != side } == true
        return Text(verbatim: value)
            .font(.system(size: 17, weight: .heavy, design: .rounded))
            .foregroundStyle(dimmed ? .secondary : .primary)
    }
}

private extension View {
    @ViewBuilder
    func lockScreenBackground() -> some View {
        if #available(iOS 17.0, *) {
            containerBackground(for: .widget) { Color.clear }
        } else {
            self
        }
    }
}
