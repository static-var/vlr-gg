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

    /// Appearance and display size form part of the persistent treatment cache key.
    enum Appearance: String, CaseIterable {
        case light, dark
        var background: CGFloat { self == .dark ? 0 : 1 }
    }

    /// Three-times-resolution sizes for Dynamic Island and Lock Screen logos.
    enum LogoSize: Int, CaseIterable {
        case compact = 60, expanded = 174
    }

    private static let images: NSCache<NSString, UIImage> = {
        let cache = NSCache<NSString, UIImage>()
        cache.totalCostLimit = 4 * 1024 * 1024
        return cache
    }()

    /// Reads prepared pixels only; downloading and contrast analysis happen in app-side prefetch.
    static func image(for source: String?, appearance: Appearance, size: LogoSize) -> UIImage? {
        guard let source, let original = fileURL(for: source) else { return nil }
        return preparedImage(original: original, appearance: appearance, size: size)
            ?? originalImage(at: original)
    }

    static func preparedImage(original: URL, appearance: Appearance, size: LogoSize) -> UIImage? {
        let file = variantURL(original: original, appearance: appearance, size: size)
        let key = file.path as NSString
        if let cached = images.object(forKey: key) { return cached }
        guard let data = try? Data(contentsOf: file),
              let bitmap = try? PropertyListDecoder().decode(LogoBitmap.self, from: data),
              let cgImage = bitmap.image() else { return nil }
        let result = UIImage(cgImage: cgImage)
        images.setObject(result, forKey: key, cost: bitmap.pixels.count)
        return result
    }

    private static func originalImage(at file: URL) -> UIImage? {
        guard let data = try? Data(contentsOf: file) else { return nil }
        return image(data: data)
    }

    /// Downloads missing originals and prepares any absent theme/size variants off the main actor.
    static func prefetch(_ sources: [String]) async {
        for source in Set(sources) {
            guard !Task.isCancelled,
                  let remote = allowedRemoteURL(for: source),
                  let file = fileURL(for: source) else { continue }
            if Appearance.allCases.allSatisfy({ appearance in
                LogoSize.allCases.allSatisfy { size in
                    FileManager.default.fileExists(atPath: variantURL(original: file, appearance: appearance, size: size).path)
                }
            }) { continue }
            do {
                let image: UIImage
                if let cached = originalImage(at: file) {
                    image = cached
                } else {
                    let request = URLRequest(url: remote, timeoutInterval: 10)
                    let (data, response) = try await session.data(for: request)
                    guard !Task.isCancelled,
                          let response = response as? HTTPURLResponse,
                          (200..<300).contains(response.statusCode),
                          data.count <= 2_000_000,
                          let decoded = self.image(data: data) else { continue }
                    try FileManager.default.createDirectory(
                        at: file.deletingLastPathComponent(), withIntermediateDirectories: true
                    )
                    try data.write(to: file, options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication])
                    image = decoded
                }
                guard !Task.isCancelled, let cgImage = image.cgImage else { continue }
                _ = try prepareVariants(cgImage, original: file)
                trimCache(in: file.deletingLastPathComponent())
            } catch {
                continue
            }
        }
    }

    /// Also upgrades originals downloaded by earlier app versions without another network request.
    static func prepareVariants(_ image: CGImage, original: URL) throws -> Bool {
        let missing = Appearance.allCases.flatMap { appearance in
            LogoSize.allCases.map { (appearance, $0) }
        }.filter { appearance, size in
            !FileManager.default.fileExists(atPath: variantURL(original: original, appearance: appearance, size: size).path)
        }
        guard !missing.isEmpty else { return false }
        let analysis = MatchActivityLogoTreatment.analyze(image)
        let encoder = PropertyListEncoder()
        encoder.outputFormat = .binary
        var changed = false
        for (appearance, size) in missing {
            guard !Task.isCancelled,
                  let prepared = MatchActivityLogoTreatment.prepare(
                    image, analysis: analysis, background: appearance.background, maxPixelSize: size.rawValue
                  ), let bitmap = LogoBitmap(prepared) else { continue }
            let file = variantURL(original: original, appearance: appearance, size: size)
            try encoder.encode(bitmap).write(to: file, options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication])
            changed = true
        }
        return changed
    }

    private static func variantURL(original: URL, appearance: Appearance, size: LogoSize) -> URL {
        original.appendingPathExtension("outline-v1-\(appearance.rawValue)-\(size.rawValue).pixels")
    }

    /// Stores bounded premultiplied RGBA pixels without adding a raster image encoder dependency.
    private struct LogoBitmap: Codable {
        let width: Int
        let height: Int
        let pixels: Data

        init?(_ image: CGImage) {
            let width = image.width
            let height = image.height
            guard width > 0, height > 0, width <= 180, height <= 180 else { return nil }
            var bytes = Data(count: width * height * 4)
            let drawn = bytes.withUnsafeMutableBytes { buffer -> Bool in
                guard let context = CGContext(
                    data: buffer.baseAddress, width: width, height: height,
                    bitsPerComponent: 8, bytesPerRow: width * 4,
                    space: CGColorSpace(name: CGColorSpace.sRGB)!,
                    bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue | CGBitmapInfo.byteOrder32Big.rawValue
                ) else { return false }
                context.draw(image, in: CGRect(x: 0, y: 0, width: width, height: height))
                return true
            }
            guard drawn else { return nil }
            self.width = width
            self.height = height
            pixels = bytes
        }

        func image() -> CGImage? {
            guard width > 0, height > 0, width <= 180, height <= 180,
                  pixels.count == width * height * 4,
                  let provider = CGDataProvider(data: pixels as CFData) else { return nil }
            return CGImage(
                width: width, height: height, bitsPerComponent: 8, bitsPerPixel: 32,
                bytesPerRow: width * 4, space: CGColorSpace(name: CGColorSpace.sRGB)!,
                bitmapInfo: CGBitmapInfo(rawValue: CGImageAlphaInfo.premultipliedLast.rawValue | CGBitmapInfo.byteOrder32Big.rawValue),
                provider: provider, decode: nil, shouldInterpolate: true, intent: .defaultIntent
            )
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
