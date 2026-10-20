import Foundation

struct WidgetDay: Codable, Equatable, Sendable {
    let year: Int
    let month: Int
    let day: Int

    init?(_ value: String) {
        guard value.range(of: #"^\d{4}-\d{1,2}-\d{1,2}$"#, options: .regularExpression) != nil else { return nil }
        let parts = value.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3 else { return nil }
        self.init(year: parts[0], month: parts[1], day: parts[2])
    }
}

enum WidgetKickoff: Equatable {
    case scheduled(day: String, time: String)
    case delayed(String)
    case unknown

    static func make(start: Date?, now: Date, locale: Locale = .current, timeZone: TimeZone = .current) -> Self {
        guard let start else { return .unknown }
        let time = format(start, template: "jm", locale: locale, timeZone: timeZone)
        guard start > now else { return .delayed(time) }
        return .scheduled(day: day(start, now: now, locale: locale, timeZone: timeZone), time: time)
    }

    func hero(locale: Locale = .current) -> String {
        switch self {
        case .scheduled(_, let time): time
        case .delayed: widgetString("Any minute", locale: locale)
        case .unknown: widgetString("TBD", locale: locale)
        }
    }

    static func timelineDates(start: Date?, now: Date, timeZone: TimeZone = .current) -> [Date] {
        guard let start, start > now else { return [now] }
        let startDay = calendar(timeZone).startOfDay(for: start)
        let changes = [calendar(timeZone).date(byAdding: .day, value: -1, to: startDay), startDay, start]
        return [now] + Set(changes.compactMap { $0 }.filter { $0 > now }).sorted()
    }

    private static func day(_ start: Date, now: Date, locale: Locale, timeZone: TimeZone) -> String {
        let calendar = calendar(timeZone)
        let days = calendar.dateComponents([.day], from: calendar.startOfDay(for: now), to: calendar.startOfDay(for: start)).day ?? 0
        guard days > 1 else {
            let formatter = RelativeDateTimeFormatter()
            formatter.locale = locale
            formatter.dateTimeStyle = .named
            let word = formatter.localizedString(from: DateComponents(day: days))
            return word.prefix(1).uppercased(with: locale) + word.dropFirst()
        }
        return format(start, template: "EEEMMMd", locale: locale, timeZone: timeZone)
    }

    private static func calendar(_ timeZone: TimeZone) -> Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone
        return calendar
    }

    private static func format(_ date: Date, template: String, locale: Locale, timeZone: TimeZone) -> String {
        let formatter = DateFormatter()
        formatter.locale = locale
        formatter.timeZone = timeZone
        formatter.setLocalizedDateFormatFromTemplate(template)
        return formatter.string(from: date)
    }
}

private final class WidgetResources {}

func widgetString(_ key: String, locale: Locale = .current, _ arguments: CVarArg...) -> String {
    let resources = Bundle(for: WidgetResources.self)
    let identifier = locale.identifier
    let language = locale.language.languageCode?.identifier ?? identifier
    let candidates = [identifier, identifier.replacingOccurrences(of: "_", with: "-"), language]
        + Bundle.preferredLocalizations(from: resources.localizations, forPreferences: [identifier])
    let path = candidates.lazy.compactMap { resources.path(forResource: $0, ofType: "lproj") }.first
    let bundle = path.flatMap(Bundle.init(path:)) ?? resources
    let format = bundle.localizedString(forKey: key, value: key, table: "Localizable")
    return String(format: format, locale: locale, arguments: arguments)
}

func widgetDate(_ day: WidgetDay?, locale: Locale = .current, timeZone: TimeZone = .current) -> String {
    guard let day else { return widgetString("TBD", locale: locale) }
    var calendar = Calendar(identifier: .gregorian)
    calendar.timeZone = timeZone
    guard let date = calendar.date(from: DateComponents(year: day.year, month: day.month, day: day.day, hour: 12)) else {
        return widgetString("TBD", locale: locale)
    }
    let formatter = DateFormatter()
    formatter.locale = locale
    formatter.timeZone = timeZone
    formatter.setLocalizedDateFormatFromTemplate("MMMd")
    return formatter.string(from: date)
}


extension WidgetDay: Comparable {
    init?(year: Int, month: Int, day: Int) {
        guard (1...9999).contains(year) else { return nil }
        let parts = DateComponents(year: year, month: month, day: day)
        guard parts.isValidDate(in: Calendar(identifier: .gregorian)) else { return nil }
        self.year = year
        self.month = month
        self.day = day
    }

    func start(in timeZone: TimeZone) -> Date {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone
        return calendar.date(from: DateComponents(year: year, month: month, day: day))!
    }

    static func < (lhs: Self, rhs: Self) -> Bool {
        (lhs.year, lhs.month, lhs.day) < (rhs.year, rhs.month, rhs.day)
    }
}

extension WidgetKickoff {
    static func row(start: Date, now: Date, locale: Locale, timeZone: TimeZone) -> (day: String, time: String) {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone
        let days = calendar.dateComponents([.day], from: calendar.startOfDay(for: now), to: calendar.startOfDay(for: start)).day ?? 0
        let day: String
        if abs(days) <= 1 {
            let formatter = RelativeDateTimeFormatter()
            formatter.locale = locale
            formatter.dateTimeStyle = .named
            let word = formatter.localizedString(from: DateComponents(day: days))
            day = word.prefix(1).uppercased(with: locale) + word.dropFirst()
        } else {
            day = format(start, template: abs(days) <= 6 ? "EEE" : "MMMd", locale: locale, timeZone: timeZone)
        }
        return (day, format(start, template: calendar.component(.minute, from: start) == 0 ? "j" : "jm", locale: locale, timeZone: timeZone))
    }
}
