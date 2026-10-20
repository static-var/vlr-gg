import Foundation

struct FavoriteWidgetKey: Codable, Hashable, Sendable {
    let entityID: String
    let clientID: String
}

enum FavoriteWidgetOrigin: String, Codable, Sendable { case fresh, cachedFallback }

struct FavoriteWidgetRecord<Value: Codable & Sendable>: Codable, Sendable {
    let key: FavoriteWidgetKey
    let fetchedAt: Date
    let value: Value
    var origin: FavoriteWidgetOrigin = .fresh
}
extension FavoriteWidgetRecord: Equatable where Value: Equatable {}

enum FavoriteWidgetNamespace: String, Codable, Sendable { case team, event }

actor FavoriteWidgetStore<Value: Codable & Sendable> {
    private let namespace: FavoriteWidgetNamespace
    private let directory: URL?
    private let now: @Sendable () -> Date
    private var pending: [FavoriteWidgetKey: Task<FavoriteWidgetRecord<Value>?, Never>] = [:]

    init(namespace: FavoriteWidgetNamespace, directory: URL?, now: @escaping @Sendable () -> Date = { .now }) {
        self.namespace = namespace
        self.directory = directory
        self.now = now
    }

    func load(for key: FavoriteWidgetKey) -> FavoriteWidgetRecord<Value>? {
        guard let url = fileURL(for: key), let bytes = try? Data(contentsOf: url),
              let saved = try? JSONDecoder().decode(Saved.self, from: bytes),
              saved.version == 1, saved.record.key == key else { return nil }
        var record = saved.record
        record.origin = .cachedFallback
        return record
    }

    func refresh(for key: FavoriteWidgetKey,
                 fetch: @escaping @Sendable () async throws -> Value) async -> FavoriteWidgetRecord<Value>? {
        if let task = pending[key] { return await task.value }
        let task = Task<FavoriteWidgetRecord<Value>?, Never> {
            do {
                let value = try await fetch()
                let record = FavoriteWidgetRecord(key: key, fetchedAt: now(), value: value)
                store(record)
                return record
            } catch { return load(for: key) }
        }
        pending[key] = task
        defer { pending[key] = nil }
        return await task.value
    }

    private struct Saved: Codable {
        let version: Int
        let record: FavoriteWidgetRecord<Value>
    }

    private func fileURL(for key: FavoriteWidgetKey) -> URL? {
        guard let bytes = try? JSONEncoder().encode([key.entityID, key.clientID]) else { return nil }
        let name = bytes.base64EncodedString().replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "+", with: "-").replacingOccurrences(of: "=", with: "")
        return directory?.appendingPathComponent("favorite-\(namespace.rawValue)-v1-\(name).json")
    }

    private func store(_ record: FavoriteWidgetRecord<Value>) {
        guard let url = fileURL(for: record.key), let bytes = try? JSONEncoder().encode(Saved(version: 1, record: record)) else { return }
        try? bytes.write(to: url, options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication])
    }
}

enum FavoriteWidgetStores {
    static let events = FavoriteWidgetStore<EventWidgetData>(namespace: .event, directory: directory)
    static let teams = FavoriteWidgetStore<TeamWidgetContent>(namespace: .team, directory: directory)
    static let directory = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: VLRWidgetContract.appGroupIdentifier)
}

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
