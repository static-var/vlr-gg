import CryptoKit
import Foundation
import ImageIO
import UIKit

/// Downloads and caches team logos for the app and Live Activity widget.
enum MatchActivityLogoCache {
    /// Accepts logo URLs only from the two image hosts used by match payloads.
    static func allowedRemoteURL(for source: String) -> URL? {
        guard let url = URL(string: source),
              let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
              url.scheme?.lowercased() == "https",
              let host = components.percentEncodedHost?.lowercased(),
              host == "owcdn.net" || host == "www.vlr.gg",
              url.user == nil, url.password == nil,
              url.port == nil || url.port == 443 else { return nil }
        return url
    }

    /// Rejects redirects that would move a logo request outside the approved hosts.
    final class RedirectGuard: NSObject, URLSessionTaskDelegate {
        func urlSession(
            _ session: URLSession,
            task: URLSessionTask,
            willPerformHTTPRedirection response: HTTPURLResponse,
            newRequest request: URLRequest,
            completionHandler: @escaping (URLRequest?) -> Void
        ) {
            completionHandler(request.url.flatMap { MatchActivityLogoCache.allowedRemoteURL(for: $0.absoluteString) } != nil
                ? request : nil)
        }
    }

    private static let session: URLSession = {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.httpShouldSetCookies = false
        configuration.httpCookieStorage = nil
        configuration.urlCredentialStorage = nil
        return URLSession(configuration: configuration, delegate: RedirectGuard(), delegateQueue: nil)
    }()

    static func image(for source: String?) -> UIImage? {
        guard let source, let file = fileURL(for: source),
              let data = try? Data(contentsOf: file) else { return nil }
        return image(data: data)
    }

    /// Downloads missing HTTPS logos and stores valid images in the shared cache.
    /// Skips failed or oversized responses and trims the cache after each write.
    static func prefetch(_ sources: [String]) async {
        for source in Set(sources) {
            guard !Task.isCancelled,
                  image(for: source) == nil,
                  let remote = allowedRemoteURL(for: source),
                  let file = fileURL(for: source) else { continue }
            do {
                let request = URLRequest(url: remote, timeoutInterval: 10)
                let (data, response) = try await session.data(for: request)
                guard !Task.isCancelled,
                      let response = response as? HTTPURLResponse,
                      (200..<300).contains(response.statusCode),
                      data.count <= 2_000_000, image(data: data) != nil else { continue }
                try FileManager.default.createDirectory(
                    at: file.deletingLastPathComponent(), withIntermediateDirectories: true
                )
                try data.write(to: file, options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication])
                trimCache(in: file.deletingLastPathComponent())
            } catch {
                continue
            }
        }
    }

    /// Removes the oldest cached files until their total size fits the cache limit.
    /// Failed deletions leave their sizes counted toward the remaining total.
    private static func trimCache(in directory: URL) {
        let keys: Set<URLResourceKey> = [.fileSizeKey, .contentModificationDateKey]
        guard let files = try? FileManager.default.contentsOfDirectory(
            at: directory, includingPropertiesForKeys: Array(keys)
        ) else { return }
        let entries = files.compactMap { file -> (url: URL, size: Int, date: Date)? in
            guard let values = try? file.resourceValues(forKeys: keys),
                  let size = values.fileSize else { return nil }
            return (file, size, values.contentModificationDate ?? .distantPast)
        }.sorted { $0.date < $1.date }
        var total = entries.reduce(0) { $0 + $1.size }
        for entry in entries {
            guard total > 32_000_000 else { break }
            do {
                try FileManager.default.removeItem(at: entry.url)
                total -= entry.size
            } catch {
                continue
            }
        }
    }

    private static func fileURL(for source: String) -> URL? {
        guard allowedRemoteURL(for: source) != nil else { return nil }
        let key = SHA256.hash(data: Data(source.utf8)).map { String(format: "%02x", $0) }.joined()
        return FileManager.default
            .containerURL(forSecurityApplicationGroupIdentifier: VLRWidgetContract.appGroupIdentifier)?
            .appendingPathComponent("Library/Caches/MatchActivityLogos", isDirectory: true)
            .appendingPathComponent(key, isDirectory: false)
    }

    /// Decodes a thumbnail sized for the Live Activity logo.
    /// Applies the source image’s orientation while limiting its pixel dimensions.
    private static func image(data: Data) -> UIImage? {
        guard let source = CGImageSourceCreateWithData(data as CFData, nil),
              let thumbnail = CGImageSourceCreateThumbnailAtIndex(source, 0, [
                kCGImageSourceCreateThumbnailFromImageAlways: true,
                kCGImageSourceCreateThumbnailWithTransform: true,
                kCGImageSourceThumbnailMaxPixelSize: 174,
              ] as CFDictionary) else { return nil }
        return UIImage(cgImage: thumbnail)
    }
}
