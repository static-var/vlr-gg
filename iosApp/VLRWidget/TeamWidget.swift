import SwiftUI
import WidgetKit

struct TeamWidgetEntry: TimelineEntry {
    let date: Date
    let content: TeamWidgetEntryContent
}

@available(iOS 17.0, *)
struct TeamWidgetProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> TeamWidgetEntry {
        TeamWidgetEntry(date: .now, content: .placeholder)
    }

    func snapshot(for configuration: TeamWidgetIntent, in context: Context) async -> TeamWidgetEntry {
        if context.isPreview {
            let now = Date.now
            return TeamWidgetEntry(date: now, content: .content(.sample(at: now)))
        }
        let source = WidgetSnapshotRepository().loadSource()
        let id = configuration.team?.id
        let cached: TeamWidgetContent?
        if let source, let id, let record = await FavoriteWidgetStores.teams.load(for: .init(entityID: id, clientID: source.clientId)) {
            cached = record.value.expired(fetchedAt: record.fetchedAt, now: .now)
        } else { cached = nil }
        return TeamWidgetEntry(date: .now, content: .make(cached, teamID: id, clientID: source?.clientId ?? "", source: source))
    }

    func timeline(for configuration: TeamWidgetIntent, in context: Context) async -> Timeline<TeamWidgetEntry> {
        let now = Date.now
        let repository = WidgetSnapshotRepository()
        let source = repository.loadSource()
        let id = configuration.team?.id
        let initialContent = TeamWidgetEntryContent.make(nil, teamID: id, clientID: source?.clientId ?? "", source: source)
        guard case .placeholder = initialContent, let source, let id else {
            return Timeline(entries: [TeamWidgetEntry(date: now, content: initialContent)], policy: .never)
        }
        let record = await FavoriteWidgetStores.teams.refresh(for: .init(entityID: id, clientID: source.clientId)) {
            try await TeamWidgetRefreshService().refresh(teamID: id, source: source)
        }
        let content = record.map { $0.value.expired(fetchedAt: $0.fetchedAt, now: now) }
        let entryContent = TeamWidgetEntryContent.make(content, teamID: id, clientID: source.clientId, source: repository.loadSource())
        let start: Date?
        if case .content(let visibleContent) = entryContent {
            await WidgetLogoPrefetch.run(visibleContent.logoSources(large: context.family == .systemLarge))
            if case .upcoming(let fixture) = visibleContent.state { start = fixture.start } else { start = nil }
        } else { start = nil }
        let refresh = TeamWidgetRefreshPolicy.date(for: entryContent, now: now)
        let dates = WidgetKickoff.timelineDates(start: start, now: now)
        return Timeline(entries: dates.map { TeamWidgetEntry(date: $0, content: entryContent) }, policy: refresh.map { .after($0) } ?? .never)
    }
}

@available(iOS 17.0, *)
struct TeamWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: VLRWidgetContract.teamWidgetKind, intent: TeamWidgetIntent.self, provider: TeamWidgetProvider()) { entry in
            TeamWidgetView(entry: entry)
        }
        .configurationDisplayName(LocalizedStringResource("Team"))
        .description(LocalizedStringResource("Your favorite team's next match, live score and recent form."))
        .supportedFamilies([.systemMedium, .systemLarge])
        .contentMarginsDisabled()
    }
}

@available(iOS 17.0, *)
struct TeamWidgetView: View {
    @Environment(\.widgetFamily) private var environmentFamily
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.widgetRenderingMode) private var renderingMode
    @Environment(\.locale) private var locale
    @Environment(\.timeZone) private var timeZone

    let entry: TeamWidgetEntry
    var familyOverride: WidgetFamily? = nil
    var tintedOverride: Bool? = nil

    private var large: Bool { (familyOverride ?? environmentFamily) == .systemLarge }
    private var tinted: Bool { tintedOverride ?? (renderingMode == .accented) }
    private var palette: PrismWidgetPalette { tinted ? .systemRendered : colorScheme == .dark ? .dark : .light }

    var body: some View {
        Group {
            switch entry.content {
            case .needsTeam(let setup): needsTeam(setup)
            case .content(let content): body(content)
            case .placeholder:
                body(.sample(at: entry.date)).redacted(reason: .placeholder)
            }
        }
        .foregroundStyle(palette.ink)
        .padding(16)
        .widgetURL(widgetURL)
        .containerBackground(PrismWidgetPalette.background(for: colorScheme), for: .widget)
    }

    private var widgetURL: URL {
        if case .content(let content) = entry.content, let fixture = content.fixture {
            return VLRWidgetContract.matchURL(id: fixture.id)
        }
        return VLRWidgetContract.appURL
    }

    private func needsTeam(_ setup: TeamWidgetSetup) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top, spacing: 8) {
                RoundedRectangle(cornerRadius: 7).strokeBorder(palette.border, style: StrokeStyle(lineWidth: 1.5, dash: [3, 3]))
                    .frame(width: large ? 40 : 28, height: large ? 40 : 28)
                VStack(alignment: .leading, spacing: 1) {
                    Text(verbatim: widgetString(setup == .chooseTeam ? "Choose a team" : "No favorite teams", locale: locale))
                        .font(.system(size: 15, weight: .semibold)).lineLimit(1).minimumScaleFactor(0.7)
                    Text(verbatim: "Val Esports").font(.system(size: 12)).foregroundStyle(palette.secondary)
                }.frame(maxWidth: .infinity, alignment: .leading)
                HStack(spacing: 6) {
                    ForEach(0..<5) { _ in Circle().fill(palette.border).frame(width: 10, height: 10) }
                }.padding(.top, 2)
            }
            if large { Spacer().frame(height: 18) } else { Spacer(minLength: 18) }
            VStack(alignment: .leading, spacing: 0) {
                Text(verbatim: widgetString(setup == .chooseTeam ? "Touch and hold, then" : "To use this widget,", locale: locale))
                    .font(.system(size: 13))
                Text(verbatim: widgetString(setup == .chooseTeam ? "Edit Widget" : "Open Val Esports", locale: locale))
                    .font(.system(size: 34)).minimumScaleFactor(0.7).lineLimit(1)
                Text(verbatim: widgetString(setup == .chooseTeam ? "Pick a team from your favorites" : "Favorite a team, then pick it here", locale: locale))
                    .font(.system(size: 12)).foregroundStyle(palette.secondary)
            }
            if large {
                Spacer(minLength: 8)
                ghostHistory
            }
        }.frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }

    private var ghostHistory: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Recent results show here").font(.system(size: 12)).foregroundStyle(palette.secondary).padding(.bottom, 2)
            ForEach(Array([CGFloat(118), 84, 140, 96].enumerated()), id: \.offset) { index, width in
                HStack(spacing: 10) {
                    Text(verbatim: "–").font(.system(size: 12, weight: .semibold)).frame(width: 18)
                    RoundedRectangle(cornerRadius: 6).fill(palette.border).frame(width: 22, height: 22)
                    Capsule().fill(palette.border).frame(width: width, height: 10)
                    Spacer(minLength: 0)
                    Text(verbatim: "–").font(.system(size: 17)).monospacedDigit()
                }.foregroundStyle(palette.secondary).frame(height: 38)
                    .overlay(alignment: .top) {
                        if index > 0 { Rectangle().fill(palette.border.opacity(0.35)).frame(height: 0.5) }
                    }
            }
        }
    }

    private func body(_ content: TeamWidgetContent) -> some View {
        let hero = TeamWidgetHero.make(content, now: entry.date, locale: locale, timeZone: timeZone)
        return VStack(alignment: .leading, spacing: 0) {
            header(content)
            if large { Spacer().frame(height: 18) } else { Spacer(minLength: 6) }
            heroBlock(hero)
            if large {
                Spacer(minLength: 8)
                history(content, hero: hero)
            }
        }.frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }

    private func header(_ content: TeamWidgetContent) -> some View {
        HStack(alignment: .top, spacing: 8) {
            WidgetLogo(mark: .team(content.header.team), size: large ? 40 : 28, palette: palette, colorScheme: colorScheme, tinted: tinted)
            VStack(alignment: .leading, spacing: 1) {
                Text(verbatim: content.header.team.name).font(.system(size: 15, weight: .semibold)).lineLimit(1).minimumScaleFactor(0.7)
                Text(verbatim: ranking(content.header)).font(.system(size: 12)).foregroundStyle(palette.secondary).lineLimit(1).minimumScaleFactor(0.6)
            }.frame(maxWidth: .infinity, alignment: .leading)
            HStack(spacing: 6) {
                ForEach(Array(content.form.enumerated()), id: \.offset) { _, slot in dot(slot) }
            }.padding(.top, 2)
        }
    }

    private func ranking(_ header: TeamWidgetHeader) -> String {
        var parts: [String] = []
        if let rank = header.rank { parts.append(widgetString("#%lld worldwide", locale: locale, Int64(rank))) }
        if large, let rank = header.regionRank, let region = header.region {
            parts.append(widgetString("#%lld %@", locale: locale, Int64(rank), region))
        }
        return parts.joined(separator: " · ")
    }

    @ViewBuilder
    private func dot(_ slot: TeamWidgetFormSlot) -> some View {
        switch slot {
        case .loss: Circle().strokeBorder(palette.secondary.opacity(0.7), lineWidth: 2).frame(width: 10, height: 10).accessibilityLabel(Text("Loss"))
        case .live:
            Circle().fill(palette.accent).frame(width: 10, height: 10)
                .background(Circle().fill(palette.accent.opacity(0.22)).frame(width: 16, height: 16))
                .widgetAccentable().accessibilityLabel(Text("Live"))
        case .win: Circle().fill(palette.ink).frame(width: 10, height: 10).accessibilityLabel(Text("Win"))
        case .empty: Circle().fill(palette.ink.opacity(0.12)).frame(width: 10, height: 10)
        case .neutral: Circle().fill(palette.secondary.opacity(0.5)).frame(width: 10, height: 10)
        }
    }

    private func heroBlock(_ hero: TeamWidgetHero) -> some View {
        HStack(alignment: .center, spacing: 12) {
            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 0) {
                    Text(verbatim: hero.lead)
                    if hero.liveLabel {
                        Text(verbatim: " · ")
                        Text("Live").foregroundStyle(palette.accent).widgetAccentable()
                    }
                }.font(.system(size: 13)).lineLimit(1).minimumScaleFactor(0.7)
                Text(verbatim: hero.value).font(.system(size: 40, weight: .regular)).tracking(-0.8).monospacedDigit()
                    .lineLimit(1).minimumScaleFactor(0.65)
                if !large, !hero.maps.isEmpty { WidgetMapPills(maps: hero.maps, palette: palette, locale: locale) }
                else {
                    HStack(spacing: 0) {
                        if hero.liveContext {
                            Text("Live").foregroundStyle(palette.accent).widgetAccentable()
                            Text(verbatim: " · ")
                        }
                        Text(verbatim: hero.context)
                    }.font(.system(size: 12)).foregroundStyle(palette.secondary).lineLimit(1).padding(.top, 3)
                }
            }.frame(maxWidth: .infinity, alignment: .leading)
            if let opponent = hero.opponent { WidgetLogo(mark: .team(opponent), size: 44, palette: palette, colorScheme: colorScheme, tinted: tinted) }
            else if case .content(let content) = entry.content, case .upcoming = content.state {
                ZStack {
                    Circle().strokeBorder(palette.border, style: StrokeStyle(lineWidth: 1.5, dash: [3, 3]))
                    Text(verbatim: "?").font(.system(size: 18, weight: .medium)).foregroundStyle(palette.secondary)
                }.frame(width: 44, height: 44)
            }
        }
    }

    @ViewBuilder
    private func history(_ content: TeamWidgetContent, hero: TeamWidgetHero) -> some View {
        if !hero.maps.isEmpty {
            VStack(alignment: .leading, spacing: 0) {
                Text("Maps").font(.system(size: 12)).foregroundStyle(palette.secondary).padding(.bottom, 2)
                ForEach(Array(hero.maps.prefix(5).enumerated()), id: \.offset) { index, map in
                    mapRow(map, compact: hero.maps.count > 3).overlay(alignment: .top) {
                        if index > 0 { Rectangle().fill(palette.border.opacity(0.35)).frame(height: 0.5) }
                    }
                }
            }
        } else if !content.historyRows.isEmpty {
            VStack(alignment: .leading, spacing: 0) {
                Text(verbatim: historyLabel(content)).font(.system(size: 12)).foregroundStyle(palette.secondary).padding(.bottom, 2)
                ForEach(Array(content.historyRows.enumerated()), id: \.offset) { index, result in
                    historyRow(result).overlay(alignment: .top) {
                        if index > 0 { Rectangle().fill(palette.border.opacity(0.35)).frame(height: 0.5) }
                    }
                }
            }
        }
    }

    private func historyLabel(_ content: TeamWidgetContent) -> String {
        switch content.state {
        case .idle, .idleHidden: widgetString("Before that", locale: locale)
        default: widgetString("Recent", locale: locale)
        }
    }

    private func historyRow(_ row: TeamWidgetRecentResult) -> some View {
        HStack(spacing: 10) {
            Text(verbatim: resultLetter(row.result)).font(.system(size: 12, weight: .semibold)).frame(width: 18)
            WidgetLogo(mark: .team(row.opponent), size: 22, palette: palette, colorScheme: colorScheme, tinted: tinted)
            Text(verbatim: row.opponent.name).foregroundStyle(palette.ink).font(.system(size: 15)).lineLimit(1).minimumScaleFactor(0.65)
            Text(verbatim: widgetDate(row.date, locale: locale, timeZone: timeZone)).font(.system(size: 12)).foregroundStyle(palette.secondary).lineLimit(1)
            Spacer(minLength: 0)
            Text(verbatim: row.result.score).font(.system(size: 17)).monospacedDigit()
        }.foregroundStyle(resultColor(row.result)).frame(height: 38)
    }

    private func resultLetter(_ result: TeamWidgetResult) -> String {
        switch result { case .win: widgetString("W", locale: locale); case .loss: widgetString("L", locale: locale); case .hidden: "" }
    }

    private func resultColor(_ result: TeamWidgetResult) -> Color {
        switch result { case .win: palette.ink; case .loss, .hidden: palette.secondary }
    }

    private func mapRow(_ map: WidgetMap, compact: Bool) -> some View {
        HStack(spacing: 8) {
            VStack(alignment: .leading, spacing: 1) {
                Text(verbatim: map.name).font(.system(size: 15))
                if !compact { Text(verbatim: mapSubtitle(map)).font(.system(size: 12)).foregroundStyle(palette.secondary).lineLimit(1) }
            }
            Spacer(minLength: 0)
            Text(verbatim: map.score?.text ?? widgetString("Next", locale: locale)).font(.system(size: 17)).monospacedDigit()
                .foregroundStyle(map.status == .live ? palette.accent : map.status == .won ? palette.ink : palette.secondary)
                .widgetAccentable(map.status == .live)
        }.frame(height: compact ? 30 : 52)
    }

    private func mapSubtitle(_ map: WidgetMap) -> String {
        let pick: String
        switch map.pick {
        case .team(let name), .opponent(let name): pick = widgetString("%@ pick", locale: locale, name)
        case .decider: pick = widgetString("Decider", locale: locale)
        }
        let status: String
        switch map.status {
        case .won: status = widgetString("Won", locale: locale)
        case .lost: status = widgetString("Lost", locale: locale)
        case .live: status = widgetString("Playing now", locale: locale)
        case .next: status = widgetString("Next", locale: locale)
        }
        return pick + " · " + status
    }

}
