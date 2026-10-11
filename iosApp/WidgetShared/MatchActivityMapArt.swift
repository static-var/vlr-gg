import Foundation
import ImageIO
import UIKit

/// Downloads map artwork for the Glance Live Activity and serves it to the widget.
/// Nothing ships in the app bundle. The widget cannot use the network, so while a Glance activity runs the app
/// stores the live map's artwork first, then the rest of the pool: the next map often starts while the app is suspended.
enum MatchActivityMapArt {
    static let manifestURL = URL(string: "https://files.akhilnarang.dev/cdn/valorant/manifest.json")!
    /// Wide enough to cover the right half of a Lock Screen Live Activity at 3x.
    static let maxPixelSize = 720
    private static let refreshInterval: TimeInterval = 24 * 60 * 60
    /// A missing live map retries sooner than the daily refresh, but not on every score update.
    private static let retryInterval: TimeInterval = 10 * 60
    private static let refreshedAtKey = "liveActivity.mapArtRefreshedAt"
    private static let attemptedAtKey = "liveActivity.mapArtAttemptedAt"

    /// Image kinds in order of preference. The loading-screen `splash` identifies a map at a glance;
    /// the in-game `banner` screenshot is the fallback until the CDN mirrors splashes.
    enum Kind: String, CaseIterable {
        case splash, banner
    }

    struct Manifest: Decodable {
        struct Entry: Decodable {
            struct Image: Decodable { let url: String }
            let name: String?
            let splash: Image?
            let banner: Image?

            func image(_ kind: Kind) -> Image? {
                switch kind {
                case .splash: return splash
                case .banner: return banner
                }
            }
        }

        let entries: [String: Entry]

        init(from decoder: Decoder) throws {
            entries = try decoder.singleValueContainer().decode([String: Entry].self)
        }

        /// The preferred artwork for each slug, limited to the CDN host.
        var artwork: [String: (kind: Kind, url: URL)] {
            entries.reduce(into: [:]) { result, entry in
                guard let slug = MatchActivityMapArt.slug(for: entry.key) else { return }
                for kind in Kind.allCases {
                    if let source = entry.value.image(kind)?.url, let url = MatchActivityMapArt.allowedRemoteURL(for: source) {
                        result[slug] = (kind, url)
                        return
                    }
                }
            }
        }
    }

    /// Lowercases a map name and keeps letters, so "Ascent" and "ascent" share one file.
    static func slug(for name: String?) -> String? {
        let slug = String((name ?? "").lowercased().unicodeScalars.filter { ("a"..."z").contains($0) })
        return slug.isEmpty || slug.count > 32 ? nil : slug
    }

    static func allowedRemoteURL(for source: String) -> URL? {
        guard let url = URL(string: source),
              url.scheme?.lowercased() == "https",
              url.host?.lowercased() == "files.akhilnarang.dev",
              url.user == nil, url.password == nil,
              url.port == nil || url.port == 443,
              url.path.hasPrefix("/cdn/valorant/") else { return nil }
        return url
    }

    private static let images: NSCache<NSString, UIImage> = {
        let cache = NSCache<NSString, UIImage>()
        cache.countLimit = 4
        return cache
    }()

    /// Reads the best stored artwork; returns nil when the map is unknown or not downloaded yet.
    static func image(forMap name: String?) -> UIImage? {
        guard let slug = slug(for: name) else { return nil }
        for kind in Kind.allCases {
            guard let file = fileURL(for: slug, kind: kind) else { return nil }
            let key = file.path as NSString
            if let cached = images.object(forKey: key) { return cached }
            if let data = try? Data(contentsOf: file), let image = downsample(data) {
                images.setObject(image, forKey: key)
                return image
            }
        }
        return nil
    }

    /// Downloads missing preferred artwork, starting with `liveMaps`.
    /// Runs daily, or sooner when a live map has no artwork yet. A stored splash replaces that map's banner.
    /// Returns true when any artwork was stored.
    @discardableResult
    static func prefetch(liveMaps: [String?], session: URLSession = .shared) async -> Bool {
        guard let defaults = MatchActivityStylePreference.sharedDefaults else { return false }
        let now = Date().timeIntervalSince1970
        var live: [String] = []
        for slug in liveMaps.compactMap(slug(for:)) where !live.contains(slug) { live.append(slug) }
        let liveMissing = live.contains { !hasArtwork(for: $0) }
        guard now - defaults.double(forKey: refreshedAtKey) > refreshInterval
                || liveMissing && now - defaults.double(forKey: attemptedAtKey) > retryInterval else { return false }
        defaults.set(now, forKey: attemptedAtKey)
        var stored = false
        do {
            let (data, response) = try await session.data(for: URLRequest(url: manifestURL, timeoutInterval: 10))
            guard (response as? HTTPURLResponse).map({ (200..<300).contains($0.statusCode) }) == true,
                  data.count <= 512_000 else { return false }
            var complete = true
            let artwork = try JSONDecoder().decode(Manifest.self, from: data).artwork
            let order = live.filter { artwork[$0] != nil } + artwork.keys.filter { !live.contains($0) }.sorted()
            for slug in order {
                guard !Task.isCancelled, let art = artwork[slug], let file = fileURL(for: slug, kind: art.kind) else { return stored }
                if FileManager.default.fileExists(atPath: file.path) { continue }
                let downloaded = await download(art.url, to: file, session: session)
                stored = stored || downloaded
                complete = complete && downloaded
                if downloaded {
                    for kind in Kind.allCases where kind != art.kind {
                        if let other = fileURL(for: slug, kind: kind) { try? FileManager.default.removeItem(at: other) }
                    }
                }
            }
            if complete { defaults.set(Date().timeIntervalSince1970, forKey: refreshedAtKey) }
        } catch {
            return stored
        }
        return stored
    }

    private static func hasArtwork(for slug: String) -> Bool {
        Kind.allCases.contains { kind in
            fileURL(for: slug, kind: kind).map { FileManager.default.fileExists(atPath: $0.path) } ?? false
        }
    }

    private static func download(_ url: URL, to file: URL, session: URLSession) async -> Bool {
        do {
            let (data, response) = try await session.data(for: URLRequest(url: url, timeoutInterval: 15))
            guard (response as? HTTPURLResponse).map({ (200..<300).contains($0.statusCode) }) == true,
                  data.count <= 8_000_000,
                  let encoded = downsample(data)?.jpegData(compressionQuality: 0.82) else { return false }
            try FileManager.default.createDirectory(at: file.deletingLastPathComponent(), withIntermediateDirectories: true)
            try encoded.write(to: file, options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication])
            return true
        } catch {
            return false
        }
    }

    private static func downsample(_ data: Data) -> UIImage? {
        guard let source = CGImageSourceCreateWithData(data as CFData, nil),
              let image = CGImageSourceCreateThumbnailAtIndex(source, 0, [
                kCGImageSourceCreateThumbnailFromImageAlways: true,
                kCGImageSourceCreateThumbnailWithTransform: true,
                kCGImageSourceThumbnailMaxPixelSize: maxPixelSize,
              ] as CFDictionary) else { return nil }
        return UIImage(cgImage: image)
    }

    private static func fileURL(for slug: String, kind: Kind) -> URL? {
        FileManager.default
            .containerURL(forSecurityApplicationGroupIdentifier: VLRWidgetContract.appGroupIdentifier)?
            .appendingPathComponent("Library/Caches/MatchActivityMaps", isDirectory: true)
            .appendingPathComponent("\(slug)-\(kind.rawValue).jpg", isDirectory: false)
    }
}
