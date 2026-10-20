import SwiftUI
import WidgetKit

enum WidgetLogoMark {
    case team(WidgetTeam)
    case event(name: String, source: String?)
    case undecided
}

@available(iOS 17.0, *)
struct WidgetLogo: View {
    let mark: WidgetLogoMark
    let size: CGFloat
    let palette: PrismWidgetPalette
    let colorScheme: ColorScheme
    let tinted: Bool
    @ViewBuilder
    var body: some View {
        switch mark {
        case .undecided:
            ZStack {
                Circle().strokeBorder(palette.border, style: StrokeStyle(lineWidth: 1.5, dash: [3, 3]))
                Text(verbatim: "?").font(.system(size: size * 0.4, weight: .medium)).foregroundStyle(palette.secondary)
            }.frame(width: size, height: size)
        case .team(let team): image(team)
        case .event(let name, let source): image(.init(id: nil, name: name, logo: source))
        }
    }

    @ViewBuilder
    private func image(_ team: WidgetTeam) -> some View {
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

@available(iOS 17.0, *)
struct WidgetMapPills: View {
    let maps: [WidgetMap]
    let palette: PrismWidgetPalette
    let locale: Locale
    var compactSpacing = false
    var body: some View {
        let maps = Self.visible(maps)
        return HStack(spacing: compactSpacing && maps.count >= 4 ? 3 : 5) {
            ForEach(Array(maps.enumerated()), id: \.offset) { _, map in
                let live = map.status == .live
                label(map, compact: maps.count >= 4)
                    .font(.system(size: 11)).monospacedDigit().lineLimit(1).minimumScaleFactor(0.6)
                    .padding(.horizontal, compactSpacing && maps.count >= 4 ? 3 : 6).frame(height: 18)
                    .foregroundStyle(live ? palette.accent : map.status == .won ? palette.ink : palette.secondary)
                    .background(RoundedRectangle(cornerRadius: 5).fill(map.status == .won || map.status == .lost ? palette.ink.opacity(0.1) : .clear))
                    .overlay(RoundedRectangle(cornerRadius: 5).strokeBorder(live ? palette.accent : map.status == .next ? palette.border : .clear, lineWidth: live ? 1.5 : 1))
                    .widgetAccentable(live)
            }
        }.padding(.top, 5)
    }

    @ViewBuilder
    private func label(_ map: WidgetMap, compact: Bool) -> some View {
        if compactSpacing && map.status == .live {
            HStack(spacing: 3) {
                Text(verbatim: map.name).lineLimit(1).minimumScaleFactor(0.6)
                Text(verbatim: map.score?.text ?? "–").fixedSize()
            }
        } else {
            Text(verbatim: Self.text(map, compact: compact, locale: locale))
        }
    }

    static func visible(_ maps: [WidgetMap]) -> [WidgetMap] {
        let firstNext = maps.firstIndex { $0.status == .next }
        return maps.enumerated().filter { $0.element.status != .next || $0.offset == firstNext }.map(\.element)
    }

    static func text(_ map: WidgetMap, compact: Bool, locale: Locale = .current) -> String {
        if map.status == .next { return widgetString("%@ next", locale: locale, map.name) }
        if compact && map.status != .live { return map.score?.text ?? "–" }
        return map.name + " " + (map.score?.text ?? "–")
    }

}
