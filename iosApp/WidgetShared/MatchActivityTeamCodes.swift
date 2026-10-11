import Foundation

/// Provides short team codes when a Live Activity update has no VLR tag yet.
/// VLR shows tags only once a map's round timeline appears, so the app caches the
/// CDN's `riot_code` for partner teams and the widget reads it by VLR team ID.
enum MatchActivityTeamCodes {
    static let directoryURL = URL(string: "https://files.akhilnarang.dev/cdn/valorant/teams.json")!
    private static let refreshInterval: TimeInterval = 24 * 60 * 60
    private static let codesKey = "liveActivity.teamCodes"
    private static let refreshedAtKey = "liveActivity.teamCodesRefreshedAt"

    private struct Entry: Decodable {
        let riot_code: String?
    }

    /// Keeps codes that fit the stacked layouts: one to five letters or digits.
    static func parse(_ data: Data) throws -> [String: String] {
        try JSONDecoder().decode([String: Entry].self, from: data).reduce(into: [:]) { result, entry in
            guard let id = Int64(entry.key), id > 0,
                  let code = entry.value.riot_code?.trimmingCharacters(in: .whitespacesAndNewlines).uppercased(),
                  (1...5).contains(code.count),
                  code.allSatisfy({ $0.isLetter || $0.isNumber }) else { return }
            result[String(id)] = code
        }
    }

    static func code(for teamID: String?, in defaults: UserDefaults? = MatchActivityStylePreference.sharedDefaults) -> String? {
        guard let teamID else { return nil }
        return (defaults?.dictionary(forKey: codesKey) as? [String: String])?[teamID]
    }

    /// Returns true when the stored codes changed.
    @discardableResult
    static func prefetch(session: URLSession = .shared) async -> Bool {
        guard let defaults = MatchActivityStylePreference.sharedDefaults,
              Date().timeIntervalSince1970 - defaults.double(forKey: refreshedAtKey) > refreshInterval else { return false }
        do {
            let (data, response) = try await session.data(for: URLRequest(url: directoryURL, timeoutInterval: 10))
            guard (response as? HTTPURLResponse).map({ (200..<300).contains($0.statusCode) }) == true,
                  data.count <= 2_000_000 else { return false }
            let codes = try parse(data)
            guard !codes.isEmpty else { return false }
            let changed = (defaults.dictionary(forKey: codesKey) as? [String: String]) != codes
            defaults.set(codes, forKey: codesKey)
            defaults.set(Date().timeIntervalSince1970, forKey: refreshedAtKey)
            return changed
        } catch {
            return false
        }
    }
}

@available(iOS 16.1, *)
extension MatchActivityAttributes.ContentState.Team {
    /// Prefers the VLR tag, then the cached partner code, then the full name.
    var shortName: String {
        let trimmedTag = tag?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if !trimmedTag.isEmpty { return trimmedTag }
        return MatchActivityTeamCodes.code(for: id) ?? name
    }
}
