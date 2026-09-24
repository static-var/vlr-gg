import CoreGraphics
import Foundation

/// Prepares cached Live Activity logos using the app's silhouette-contrast rules.
enum MatchActivityLogoTreatment {
    /// A small edge sample reusable across light and dark background variants.
    struct Analysis {
        fileprivate let edges: [EdgeColor]

        func needsOutline(background: CGFloat) -> Bool {
            guard !edges.isEmpty else { return false }
            let backgroundLuminance = linear(background)
            let lowContrast = edges.filter { edge in
                let luminance = 0.2126 * linear(edge.red + background * (1 - edge.alpha))
                    + 0.7152 * linear(edge.green + background * (1 - edge.alpha))
                    + 0.0722 * linear(edge.blue + background * (1 - edge.alpha))
                return (max(luminance, backgroundLuminance) + 0.05)
                    / (min(luminance, backgroundLuminance) + 0.05) < 2
            }.count
            return Double(lowContrast) / Double(edges.count) >= 0.35
        }
    }

    /// Premultiplied sRGB components allow compositing directly over the background.
    fileprivate struct EdgeColor {
        let red: CGFloat
        let green: CGFloat
        let blue: CGFloat
        let alpha: CGFloat
    }

    /// Samples the four-neighbor silhouette at 32 pixels; opaque rectangles are excluded.
    static func analyze(_ image: CGImage) -> Analysis {
        let size = fittedSize(image, maximum: 32)
        guard let context = context(width: size.width, height: size.height),
              let data = context.data else { return Analysis(edges: []) }
        context.draw(image, in: CGRect(x: 0, y: 0, width: size.width, height: size.height))
        let pixels = data.assumingMemoryBound(to: UInt8.self)
        func visible(_ x: Int, _ y: Int) -> Bool {
            x >= 0 && x < size.width && y >= 0 && y < size.height
                && pixels[y * context.bytesPerRow + x * 4 + 3] >= 128
        }
        var hasTransparency = false
        var edges: [EdgeColor] = []
        for y in 0..<size.height {
            for x in 0..<size.width {
                if !visible(x, y) {
                    hasTransparency = true
                } else if !visible(x - 1, y) || !visible(x + 1, y)
                    || !visible(x, y - 1) || !visible(x, y + 1) {
                    let offset = y * context.bytesPerRow + x * 4
                    edges.append(EdgeColor(
                        red: CGFloat(pixels[offset]) / 255,
                        green: CGFloat(pixels[offset + 1]) / 255,
                        blue: CGFloat(pixels[offset + 2]) / 255,
                        alpha: CGFloat(pixels[offset + 3]) / 255
                    ))
                }
            }
        }
        return Analysis(edges: hasTransparency ? edges : [])
    }

    static func prepare(
        _ image: CGImage,
        background: CGFloat,
        maxPixelSize: Int,
        radius: Int = 3
    ) -> CGImage? {
        prepare(image, analysis: analyze(image), background: background,
                maxPixelSize: maxPixelSize, radius: radius)
    }

    /// Fits the logo, adds a contrasting silhouette if needed, and keeps equal padding in all variants.
    static func prepare(
        _ image: CGImage,
        analysis: Analysis,
        background: CGFloat,
        maxPixelSize: Int,
        radius: Int = 3
    ) -> CGImage? {
        guard maxPixelSize > 0, radius >= 0,
              radius <= (Int.max - maxPixelSize) / 2 else { return nil }
        let size = fittedSize(image, maximum: maxPixelSize)
        guard let output = context(width: size.width + radius * 2, height: size.height + radius * 2)
        else { return nil }
        let rect = CGRect(x: radius, y: radius, width: size.width, height: size.height)
        if radius > 0 && analysis.needsOutline(background: background) {
            guard let tinted = context(width: size.width, height: size.height) else { return nil }
            let tintRect = CGRect(x: 0, y: 0, width: size.width, height: size.height)
            tinted.draw(image, in: tintRect)
            tinted.setBlendMode(.sourceIn)
            tinted.setFillColor(gray: background < 0.5 ? 1 : 0, alpha: 1)
            tinted.fill(tintRect)
            guard let silhouette = tinted.makeImage() else { return nil }
            for index in 0..<16 {
                let angle = CGFloat(index) * 2 * .pi / 16
                output.draw(silhouette, in: rect.offsetBy(
                    dx: cos(angle) * CGFloat(radius), dy: sin(angle) * CGFloat(radius)
                ))
            }
        }
        output.draw(image, in: rect)
        return output.makeImage()
    }

    private static func fittedSize(_ image: CGImage, maximum: Int) -> (width: Int, height: Int) {
        let scale = CGFloat(maximum) / CGFloat(max(image.width, image.height))
        return (max(1, Int((CGFloat(image.width) * scale).rounded())),
                max(1, Int((CGFloat(image.height) * scale).rounded())))
    }

    private static func context(width: Int, height: Int) -> CGContext? {
        guard let colorSpace = CGColorSpace(name: CGColorSpace.sRGB) else { return nil }
        return CGContext(
            data: nil, width: width, height: height, bitsPerComponent: 8, bytesPerRow: 0,
            space: colorSpace,
            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue | CGBitmapInfo.byteOrder32Big.rawValue
        )
    }

    private static func linear(_ component: CGFloat) -> CGFloat {
        component <= 0.04045 ? component / 12.92 : pow((component + 0.055) / 1.055, 2.4)
    }
}
