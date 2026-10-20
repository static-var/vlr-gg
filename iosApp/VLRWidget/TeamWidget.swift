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
        guard let id = configuration.team?.id, source?.favoriteTeams.contains(where: { $0.id == id }) == true,
              let source else { return TeamWidgetEntry(date: .now, content: .needsTeam) }
        return TeamWidgetEntry(date: .now, content: .make(TeamWidgetCache().load(teamID: id, source: source), teamID: id, clientID: source.clientId, source: source))
    }

    func timeline(for configuration: TeamWidgetIntent, in context: Context) async -> Timeline<TeamWidgetEntry> {
        let now = Date.now
        let repository = WidgetSnapshotRepository()
        guard let source = repository.loadSource(), let id = configuration.team?.id,
              source.favoriteTeams.contains(where: { $0.id == id }) else {
            return Timeline(entries: [TeamWidgetEntry(date: now, content: .needsTeam)], policy: .never)
        }
        let cache = TeamWidgetCache()
        let content = await TeamWidgetRefreshCoordinator.shared.refresh(teamID: id, source: source, cache: cache)
        if let content {
            let visibleContent = repository.loadSource()?.spoilersHidden == true ? content.redacted() : content
            await TeamWidgetLogoPrefetch.run(visibleContent.logoSources(large: context.family == .systemLarge))
            let entryContent = TeamWidgetEntryContent.make(content, teamID: id, clientID: source.clientId, source: repository.loadSource())
            let refresh = TeamWidgetRefreshPolicy.date(for: entryContent, now: now)
            let start: Date?
            if case .upcoming(let fixture) = content.state { start = fixture.start } else { start = nil }
            let dates = TeamWidgetCountdown.timelineDates(start: start, now: now, refresh: refresh ?? now)
            return Timeline(entries: dates.map { TeamWidgetEntry(date: $0, content: entryContent) }, policy: refresh.map { .after($0) } ?? .never)
        }
        let entryContent = TeamWidgetEntryContent.make(nil, teamID: id, clientID: source.clientId, source: repository.loadSource())
        let refresh = TeamWidgetRefreshPolicy.date(for: entryContent, now: now)
        return Timeline(entries: [TeamWidgetEntry(date: now, content: entryContent)], policy: refresh.map { .after($0) } ?? .never)
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
            case .needsTeam: needsTeam
            case .content(let content): body(content)
            case .placeholder:
                body(.sample(at: entry.date)).redacted(reason: .placeholder)
            }
        }
        .foregroundStyle(palette.ink)
        .padding(16)
        .widgetURL(widgetURL)
        .teamWidgetBackground(PrismWidgetPalette.background(for: colorScheme))
    }

    private var widgetURL: URL {
        if case .content(let content) = entry.content, let fixture = content.fixture {
            return VLRWidgetContract.matchURL(id: fixture.id)
        }
        return VLRWidgetContract.appURL
    }

    private var needsTeam: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 8) {
                RoundedRectangle(cornerRadius: 7).strokeBorder(palette.border, style: StrokeStyle(lineWidth: 1.5, dash: [3, 3]))
                    .frame(width: large ? 40 : 28, height: large ? 40 : 28)
                VStack(alignment: .leading, spacing: 1) {
                    Text("Choose a team").font(.system(size: 15, weight: .semibold))
                    Text(verbatim: "Val Esports").font(.system(size: 12)).foregroundStyle(palette.secondary)
                }
            }
            Spacer(minLength: 18)
            Text("Touch and hold, then").font(.system(size: 13))
            Text("Edit Widget").font(.system(size: 34)).minimumScaleFactor(0.7).lineLimit(1)
            Text("Pick a team from your favorites").font(.system(size: 12)).foregroundStyle(palette.secondary)
        }.frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
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
            logo(content.header.team, size: large ? 40 : 28)
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
        if let rank = header.rank { parts.append(teamWidgetString("#%lld worldwide", locale: locale, Int64(rank))) }
        if large, let rank = header.regionRank, let region = header.region {
            parts.append(teamWidgetString("#%lld %@", locale: locale, Int64(rank), region))
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
                if !large, !hero.maps.isEmpty { mapPills(hero.maps) }
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
            if let opponent = hero.opponent { logo(opponent, size: 44) }
            else if case .content(let content) = entry.content, case .upcoming = content.state {
                ZStack {
                    Circle().strokeBorder(palette.border, style: StrokeStyle(lineWidth: 1.5, dash: [3, 3]))
                    Text(verbatim: "?").font(.system(size: 18, weight: .medium)).foregroundStyle(palette.secondary)
                }.frame(width: 44, height: 44)
            }
        }
    }

    private func mapPills(_ maps: [TeamWidgetMap]) -> some View {
        let maps = Self.mediumMaps(maps)
        return HStack(spacing: 5) {
            ForEach(Array(maps.enumerated()), id: \.offset) { _, map in
                let live = map.status == .live
                Text(verbatim: pillText(map, compact: maps.count >= 4))
                    .font(.system(size: 11)).monospacedDigit().lineLimit(1).minimumScaleFactor(0.6)
                    .padding(.horizontal, 6).frame(height: 18)
                    .foregroundStyle(live ? palette.accent : map.status == .won ? palette.ink : palette.secondary)
                    .background(RoundedRectangle(cornerRadius: 5).fill(map.status == .won || map.status == .lost ? palette.ink.opacity(0.1) : .clear))
                    .overlay(RoundedRectangle(cornerRadius: 5).strokeBorder(live ? palette.accent : map.status == .next ? palette.border : .clear, lineWidth: live ? 1.5 : 1))
                    .widgetAccentable(live)
            }
        }.padding(.top, 5)
    }

    static func mediumMaps(_ maps: [TeamWidgetMap]) -> [TeamWidgetMap] {
        let firstNext = maps.firstIndex { $0.status == .next }
        return maps.enumerated().filter { $0.element.status != .next || $0.offset == firstNext }.map(\.element)
    }

    private func pillText(_ map: TeamWidgetMap, compact: Bool) -> String {
        if map.status == .next { return teamWidgetString("%@ next", locale: locale, map.name) }
        if compact && map.status != .live { return map.score?.text ?? "–" }
        return map.name + " " + (map.score?.text ?? "–")
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
        } else {
            let rows = content.historyRows
            if !rows.isEmpty {
                VStack(alignment: .leading, spacing: 0) {
                    Text(verbatim: historyLabel(content)).font(.system(size: 12)).foregroundStyle(palette.secondary).padding(.bottom, 2)
                    ForEach(Array(rows.enumerated()), id: \.offset) { index, result in
                        historyRow(result).overlay(alignment: .top) {
                            if index > 0 { Rectangle().fill(palette.border.opacity(0.35)).frame(height: 0.5) }
                        }
                    }
                }
            }
        }
    }

    private func historyLabel(_ content: TeamWidgetContent) -> String {
        switch content.state {
        case .idle, .idleHidden: teamWidgetString("Before that", locale: locale)
        default: teamWidgetString("Recent", locale: locale)
        }
    }

    private func historyRow(_ row: TeamWidgetRecentResult) -> some View {
        HStack(spacing: 10) {
            Text(verbatim: resultLetter(row.result)).font(.system(size: 12, weight: .semibold)).frame(width: 18)
            logo(row.opponent, size: 22)
            Text(verbatim: row.opponent.name).foregroundStyle(palette.ink).font(.system(size: 15)).lineLimit(1).minimumScaleFactor(0.65)
            Text(verbatim: teamWidgetDate(row.date, locale: locale, timeZone: timeZone)).font(.system(size: 12)).foregroundStyle(palette.secondary).lineLimit(1)
            Spacer(minLength: 0)
            Text(verbatim: row.result.score).font(.system(size: 17)).monospacedDigit()
        }.foregroundStyle(resultColor(row.result)).frame(height: 38)
    }

    private func resultLetter(_ result: TeamWidgetResult) -> String {
        switch result { case .win: teamWidgetString("W", locale: locale); case .loss: teamWidgetString("L", locale: locale); case .hidden: "" }
    }

    private func resultColor(_ result: TeamWidgetResult) -> Color {
        switch result { case .win: palette.ink; case .loss, .hidden: palette.secondary }
    }

    private func mapRow(_ map: TeamWidgetMap, compact: Bool) -> some View {
        HStack(spacing: 8) {
            VStack(alignment: .leading, spacing: 1) {
                Text(verbatim: map.name).font(.system(size: 15))
                if !compact { Text(verbatim: mapSubtitle(map)).font(.system(size: 12)).foregroundStyle(palette.secondary).lineLimit(1) }
            }
            Spacer(minLength: 0)
            Text(verbatim: map.score?.text ?? teamWidgetString("Next", locale: locale)).font(.system(size: 17)).monospacedDigit()
                .foregroundStyle(map.status == .live ? palette.accent : map.status == .won ? palette.ink : palette.secondary)
                .widgetAccentable(map.status == .live)
        }.frame(height: compact ? 30 : 52)
    }

    private func mapSubtitle(_ map: TeamWidgetMap) -> String {
        let pick: String
        switch map.pick {
        case .team(let name), .opponent(let name): pick = teamWidgetString("%@ pick", locale: locale, name)
        case .decider: pick = teamWidgetString("Decider", locale: locale)
        }
        let status: String
        switch map.status {
        case .won: status = teamWidgetString("Won", locale: locale)
        case .lost: status = teamWidgetString("Lost", locale: locale)
        case .live: status = teamWidgetString("Playing now", locale: locale)
        case .next: status = teamWidgetString("Next", locale: locale)
        }
        return pick + " · " + status
    }

    @ViewBuilder
    private func logo(_ team: TeamWidgetTeam, size: CGFloat) -> some View {
        let image = tinted ? MatchActivityLogoCache.tintedImage(for: team.logo, size: .expanded)
            : MatchActivityLogoCache.image(for: team.logo, appearance: colorScheme == .dark ? .dark : .light, size: .expanded)
        if let image {
            if #available(iOS 18.0, *), tinted {
                Image(uiImage: image).resizable().widgetAccentedRenderingMode(.fullColor).scaledToFit().frame(width: size, height: size)
            } else {
                Image(uiImage: image).resizable().scaledToFit().frame(width: size, height: size)
            }
        } else {
            RoundedRectangle(cornerRadius: size / 4).fill(palette.ink.opacity(0.08))
                .overlay {
                    Text(verbatim: (team.tag ?? String(team.name.prefix(3))).uppercased())
                        .font(.system(size: size * 0.32, weight: .semibold))
                        .foregroundStyle(palette.secondary).lineLimit(1).minimumScaleFactor(0.5)
                }
                .frame(width: size, height: size)
        }
    }
}

private extension View {
    @ViewBuilder
    func teamWidgetBackground(_ color: Color) -> some View {
        if #available(iOS 17.0, *) { containerBackground(color, for: .widget) }
        else { background(color) }
    }
}
