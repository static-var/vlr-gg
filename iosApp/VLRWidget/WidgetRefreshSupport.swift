import Foundation

enum WidgetLogoPrefetch {
    static func run(_ sources: [String], timeout: Duration = .seconds(4),
                    prefetch: @escaping ([String]) async -> Void = MatchActivityLogoCache.prefetch) async {
        let sources = Array(Set(sources)).sorted()
        await withTaskGroup(of: Void.self) { group in
            group.addTask { await prefetch(sources) }
            group.addTask { try? await Task.sleep(for: timeout) }
            await group.next()
            group.cancelAll()
        }
    }
}

enum FavoriteWidgetRefreshPolicy {
    static func scheduled(start: Date?, now: Date) -> Date {
        guard let start else { return now.addingTimeInterval(7_200) }
        let remaining = start.timeIntervalSince(now)
        let interval: TimeInterval = remaining <= 0 ? 300 : remaining <= 86_400 ? 1_800 : 7_200
        return min(now.addingTimeInterval(interval), start > now ? start : .distantFuture)
    }
}
