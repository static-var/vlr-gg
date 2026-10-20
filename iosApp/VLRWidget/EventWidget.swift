import SwiftUI
import WidgetKit

@available(iOS 17.0, *)
struct EventWidgetProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> EventWidgetEntry {
        .init(date: .now, content: .placeholder(eventID: nil))
    }

    func snapshot(for configuration: EventWidgetIntent, in context: Context) async -> EventWidgetEntry {
        let now = Date.now
        if context.isPreview { return .init(date: now, content: .content(EventWidgetEngine.sample(at: now))) }
        let repository = WidgetSnapshotRepository()
        let source = repository.loadSource()
        let record: FavoriteWidgetRecord<EventWidgetData>?
        if let source, let id = configuration.event?.id {
            record = await FavoriteWidgetStores.events.load(for: .init(entityID: id, clientID: source.clientId))
        } else { record = nil }
        return EventWidgetEngine.plan(record, selectedEventID: configuration.event?.id,
            source: repository.loadSource(), at: now, timeZone: .current).entries[0]
    }

    func timeline(for configuration: EventWidgetIntent, in context: Context) async -> Timeline<EventWidgetEntry> {
        let repository = WidgetSnapshotRepository()
        let source = repository.loadSource()
        let id = configuration.event?.id
        let now = Date.now
        let initial = EventWidgetEngine.plan(nil, selectedEventID: id, source: source, at: now, timeZone: .current)
        guard initial.refreshAfter != nil, let source, let id else { return Timeline(entries: initial.entries, policy: .never) }
        let record = await EventWidgetRefreshService().refresh(eventID: id, source: source, at: now, timeZone: .current)
        let preview = EventWidgetEngine.plan(record, selectedEventID: id, source: repository.loadSource(), at: .now, timeZone: .current)
        await WidgetLogoPrefetch.run(preview.logoSources(large: context.family == .systemLarge))
        let plan = EventWidgetEngine.plan(record, selectedEventID: id, source: repository.loadSource(), at: .now, timeZone: .current)
        return Timeline(entries: plan.entries, policy: plan.refreshAfter.map { .after($0) } ?? .never)
    }
}

@available(iOS 17.0, *)
struct EventWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: VLRWidgetContract.eventWidgetKind, intent: EventWidgetIntent.self, provider: EventWidgetProvider()) {
            EventWidgetView(entry: $0)
        }
        .configurationDisplayName(LocalizedStringResource("Event"))
        .description(LocalizedStringResource("Your favorite event\u{0027}s next match, live scores and standings."))
        .supportedFamilies([.systemMedium, .systemLarge])
        .contentMarginsDisabled()
    }
}

extension EventWidgetEntryContent {
    var widgetURL: URL {
        switch self {
        case .content(let content): content.header.eventID.isEmpty ? VLRWidgetContract.appURL : VLRWidgetContract.detailsURL(kind: "event", id: content.header.eventID)
        case .placeholder(let id): id.map { VLRWidgetContract.detailsURL(kind: "event", id: $0) } ?? VLRWidgetContract.appURL
        case .setup: VLRWidgetContract.appURL
        }
    }
}

@available(iOS 17.0, *)
struct EventWidgetView: View {
    @Environment(\.widgetFamily) private var environmentFamily
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.widgetRenderingMode) private var renderingMode
    @Environment(\.locale) private var locale
    @Environment(\.timeZone) private var timeZone

    let entry: EventWidgetEntry
    var familyOverride: WidgetFamily? = nil
    var tintedOverride: Bool? = nil

    private var large: Bool { (familyOverride ?? environmentFamily) == .systemLarge }
    private var tinted: Bool { tintedOverride ?? (renderingMode == .accented) }
    private var palette: PrismWidgetPalette { tinted ? .systemRendered : colorScheme == .dark ? .dark : .light }

    var body: some View {
        Group {
            switch entry.content {
            case .setup(let reason): setup(reason)
            case .placeholder:
                content(EventWidgetEngine.sample(at: entry.date)).redacted(reason: .placeholder)
            case .content(let value): content(value)
            }
        }
        .foregroundStyle(palette.ink)
        .padding(16)
        .widgetURL(entry.content.widgetURL)
        .containerBackground(PrismWidgetPalette.background(for: colorScheme), for: .widget)
    }

    private func content(_ content: EventWidgetContent) -> some View {
        let text: EventWidgetHeroText
        switch content.body {
        case .visible(let state): text = .make(state, now: entry.date, locale: locale, timeZone: timeZone)
        case .hidden(let state): text = .make(state, now: entry.date, locale: locale, timeZone: timeZone)
        }
        return VStack(alignment: .leading, spacing: 0) {
            header(content.header)
            if large { Spacer().frame(height: 18) } else { Spacer(minLength: 6) }
            hero(text)
            if large {
                Spacer(minLength: 12)
                list(content.body)
            }
        }.frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }

    private func header(_ header: EventWidgetHeader) -> some View {
        HStack(alignment: .top, spacing: 8) {
            logo(.event(name: header.title, source: header.logo), size: large ? 40 : 28)
            VStack(alignment: .leading, spacing: 1) {
                Text(verbatim: header.title).font(.system(size: 15, weight: .semibold)).lineLimit(1).minimumScaleFactor(0.7)
                Text(verbatim: EventWidgetText.subtitle(header.subtitle, locale: locale, timeZone: timeZone))
                    .font(.system(size: 12)).foregroundStyle(palette.secondary).lineLimit(1).minimumScaleFactor(0.6)
            }.frame(maxWidth: .infinity, alignment: .leading)
            HStack(spacing: 6) {
                if header.hasLiveMatch {
                    Circle().fill(palette.accent).frame(width: 8, height: 8)
                        .background(Circle().fill(palette.accent.opacity(0.22)).frame(width: 13, height: 13))
                        .widgetAccentable().accessibilityLabel(Text("Live"))
                }
                progress(header.progress)
            }.padding(.top, 5)
        }
    }

    private func progress(_ progress: EventWidgetProgress) -> some View {
        let fraction: Double
        switch progress { case .empty: fraction = 0; case .fraction(let value): fraction = value; case .complete: fraction = 1 }
        return Capsule().fill(palette.border)
            .overlay(alignment: .leading) { Capsule().fill(palette.ink).frame(width: 64 * fraction) }
            .frame(width: 64, height: 4).accessibilityHidden(true)
    }

    private func hero(_ text: EventWidgetHeroText) -> some View {
        HStack(alignment: .center, spacing: 12) {
            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 0) {
                    Text(verbatim: text.lead)
                    if text.liveBadge {
                        Text(verbatim: " · ")
                        Text("Live").foregroundStyle(palette.accent).widgetAccentable()
                    }
                    if text.otherLiveCount > 0 {
                        Text(verbatim: " · " + widgetString("+%lld", locale: locale, Int64(text.otherLiveCount)))
                    }
                }.font(.system(size: 13)).lineLimit(1).minimumScaleFactor(0.7)
                HStack(alignment: .firstTextBaseline, spacing: 7) {
                    Text(verbatim: text.value).font(.system(size: 40)).tracking(-0.8).monospacedDigit()
                        .lineLimit(1).minimumScaleFactor(0.65).layoutPriority(1)
                    if let beside = text.beside {
                        (Text(verbatim: beside.round).fontWeight(.semibold)
                         + Text(verbatim: beside.format.map { " · " + $0 } ?? "").foregroundColor(palette.secondary))
                            .font(.system(size: 12)).lineLimit(1).minimumScaleFactor(0.65)
                    }
                }
                if !text.maps.isEmpty {
                    WidgetMapPills(maps: text.maps, palette: palette, locale: locale, compactSpacing: true)
                } else {
                    HStack(spacing: 0) {
                        if text.contextLive {
                            Text("Live").foregroundStyle(palette.accent).widgetAccentable()
                            Text(verbatim: " · ")
                        }
                        Text(verbatim: text.context)
                    }.font(.system(size: 12)).foregroundStyle(palette.secondary).lineLimit(1).minimumScaleFactor(0.7).padding(.top, 3)
                }
            }.frame(maxWidth: .infinity, alignment: .leading)
            art(text.art)
        }
    }

    @ViewBuilder
    private func art(_ art: EventWidgetArt) -> some View {
        switch art {
        case .pair(let pair):
            ZStack {
                logo(mark(pair.left), size: 32).frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                logo(mark(pair.right), size: 32).frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomTrailing)
            }.frame(width: 58, height: 52)
        case .winner(let team): logo(.team(team), size: 46)
        case .none: EmptyView()
        }
    }

    private func mark(_ participant: WidgetParticipant) -> WidgetLogoMark {
        switch participant { case .team(let team): .team(team); case .undecided: .undecided }
    }

    private func logo(_ mark: WidgetLogoMark, size: CGFloat) -> some View {
        WidgetLogo(mark: mark, size: size, palette: palette, colorScheme: colorScheme, tinted: tinted).accessibilityHidden(true)
    }

    @ViewBuilder
    private func list(_ body: EventWidgetBody) -> some View {
        switch body {
        case .visible(let state):
            switch state {
            case .scheduled(_, let list):
                switch list { case .group(let table): group(table); case .matches(let list): matches(list) }
            case .live(_, let list), .between(_, let list), .gap(_, let list): matches(list)
            case .finished(_, let list):
                switch list { case .prizes(let prizes): placements(prizes); case .matches(let list): matches(list) }
            }
        case .hidden(let state):
            switch state {
            case .scheduled(_, let list), .live(_, let list), .gap(_, let list), .finished(_, let list): matches(list)
            }
        }
    }

    @ViewBuilder
    private func matches(_ list: EventMatchList) -> some View {
        if !list.rows.isEmpty {
            section(EventWidgetText.heading(list.heading, locale: locale), count: list.rows.count) { index in
                let row = list.rows[index]
                matchRow(row.fixture, text: EventWidgetText.row(row, heading: list.heading, now: entry.date, locale: locale, timeZone: timeZone))
            }
        }
    }

    @ViewBuilder
    private func matches(_ list: EventNeutralList) -> some View {
        if !list.rows.isEmpty {
            section(EventWidgetText.heading(list.heading, locale: locale), count: list.rows.count) { index in
                let row = list.rows[index]
                matchRow(row.fixture, text: EventWidgetText.row(row, heading: list.heading, now: entry.date, locale: locale, timeZone: timeZone))
            }
        }
    }

    private func matchRow(_ fixture: EventWidgetFixture, text: EventWidgetRowText) -> some View {
        HStack(spacing: 10) {
            VStack(alignment: .leading, spacing: 2) {
                Text(verbatim: text.day).font(.system(size: 12)).foregroundStyle(text.live ? palette.accent : palette.secondary)
                Text(verbatim: text.time).font(.system(size: 13)).monospacedDigit()
            }.frame(width: 48, alignment: .leading).lineLimit(1).minimumScaleFactor(0.65)
            VStack(alignment: .leading, spacing: 3) {
                participantRow(fixture.participants.left, name: text.left, score: text.leftScore, dimmed: text.loser == .left)
                participantRow(fixture.participants.right, name: text.right, score: text.rightScore, dimmed: text.loser == .right)
            }.frame(maxWidth: .infinity, alignment: .leading)
            Text(verbatim: text.round).font(.system(size: 11)).foregroundStyle(palette.secondary)
                .multilineTextAlignment(.trailing).lineLimit(2).minimumScaleFactor(0.7).frame(width: 70, alignment: .trailing)
        }.frame(height: 49)
    }

    private func participantRow(_ participant: WidgetParticipant, name: String, score: String?, dimmed: Bool) -> some View {
        HStack(spacing: 5) {
            logo(mark(participant), size: 16)
            Text(verbatim: name).lineLimit(1).minimumScaleFactor(0.65)
            Spacer(minLength: 0)
            if let score { Text(verbatim: score).monospacedDigit() }
        }.font(.system(size: 12)).foregroundStyle(dimmed ? palette.secondary : palette.ink)
    }

    private func group(_ table: EventGroupTable) -> some View {
        section(widgetString("%@ · W–L · maps", locale: locale, table.name), count: table.rows.count) { index in
            let row = table.rows[index]
            HStack(spacing: 8) {
                Text(verbatim: EventWidgetText.integer(row.rank, locale: locale)).frame(width: 16, alignment: .leading)
                logo(.team(row.standing.team), size: 16)
                Text(verbatim: row.standing.team.name).lineLimit(1).minimumScaleFactor(0.7)
                Spacer(minLength: 0)
                Text(verbatim: EventWidgetText.record(row.standing, locale: locale)).monospacedDigit()
                Text(verbatim: EventWidgetText.integer(row.standing.mapDifference, locale: locale, signed: true))
                    .foregroundStyle(palette.secondary).frame(width: 30, alignment: .trailing).monospacedDigit()
            }.font(.system(size: 13, weight: row.isFavorite ? .semibold : .regular)).frame(height: 30)
        }
    }

    private func placements(_ values: [EventWidgetPrize]) -> some View {
        section(widgetString("Final standings", locale: locale), count: values.count) { index in
            let row = values[index]
            HStack(spacing: 8) {
                Text(verbatim: row.position).font(.system(size: 12)).foregroundStyle(palette.secondary).frame(width: 42, alignment: .leading)
                logo(.team(row.team), size: 20)
                Text(verbatim: row.team.name).font(.system(size: 13)).lineLimit(1).minimumScaleFactor(0.7)
                Spacer(minLength: 0)
                Text(verbatim: row.amount).font(.system(size: 13)).lineLimit(1).minimumScaleFactor(0.7)
            }.frame(height: 38)
        }
    }

    private func section<Row: View>(_ title: String, count: Int, @ViewBuilder row: @escaping (Int) -> Row) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(verbatim: title).font(.system(size: 12)).foregroundStyle(palette.secondary).lineLimit(1).padding(.bottom, 2)
            ForEach(0..<count, id: \.self) { index in
                row(index).overlay(alignment: .top) { Rectangle().fill(palette.border.opacity(0.5)).frame(height: 0.5) }
            }
        }
    }

    private func setup(_ reason: EventWidgetSetup) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top, spacing: 8) {
                RoundedRectangle(cornerRadius: 7).strokeBorder(palette.border, style: StrokeStyle(lineWidth: 1.5, dash: [3, 3]))
                    .frame(width: large ? 40 : 28, height: large ? 40 : 28)
                VStack(alignment: .leading, spacing: 1) {
                    Text(verbatim: widgetString(reason == .chooseEvent ? "Choose an event" : "No favorite events", locale: locale))
                        .font(.system(size: 15, weight: .semibold)).lineLimit(1).minimumScaleFactor(0.7)
                    Text(verbatim: "Val Esports").font(.system(size: 12)).foregroundStyle(palette.secondary)
                }
            }
            if large { Spacer().frame(height: 18) } else { Spacer(minLength: 18) }
            VStack(alignment: .leading, spacing: 0) {
                Text(verbatim: widgetString(reason == .chooseEvent ? "Touch and hold, then" : "To use this widget,", locale: locale)).font(.system(size: 13))
                Text(verbatim: widgetString(reason == .chooseEvent ? "Edit Widget" : "Open Val Esports", locale: locale))
                    .font(.system(size: 34)).lineLimit(1).minimumScaleFactor(0.7)
                Text(verbatim: widgetString(reason == .chooseEvent ? "Pick an event from your favorites" : "Favorite an event, then pick it here", locale: locale))
                    .font(.system(size: 12)).foregroundStyle(palette.secondary).lineLimit(1).minimumScaleFactor(0.7)
            }
            if large {
                Spacer(minLength: 12)
                section(widgetString("Matches show here", locale: locale), count: 3) { index in
                    HStack(spacing: 12) {
                        Text(verbatim: "–").frame(width: 32)
                        VStack(alignment: .leading, spacing: 6) {
                            ForEach(0..<2) { line in
                                HStack(spacing: 6) {
                                    RoundedRectangle(cornerRadius: 4).fill(palette.border).frame(width: 16, height: 16)
                                    Capsule().fill(palette.border).frame(width: CGFloat(68 + (index + line) * 12), height: 8)
                                }
                            }
                        }
                        Spacer(minLength: 0)
                        Text(verbatim: "–")
                    }.foregroundStyle(palette.secondary).frame(height: 49)
                }
            }
        }.frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }
}
