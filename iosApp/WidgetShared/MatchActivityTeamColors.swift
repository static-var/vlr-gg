import CoreGraphics
import Foundation
import UIKit

enum MatchActivityLogoColor: Codable, Equatable {
    case vivid(MatchActivityTeamColors.RGB, secondary: [MatchActivityTeamColors.RGB] = [])
    case monochrome(secondary: [MatchActivityTeamColors.RGB] = [])
    case unavailable

    var colors: [MatchActivityTeamColors.RGB] {
        switch self {
        case .vivid(let primary, let secondary): return [primary] + secondary
        case .monochrome(let secondary): return secondary
        case .unavailable: return []
        }
    }

    var secondary: [MatchActivityTeamColors.RGB] {
        switch self {
        case .vivid(_, let secondary), .monochrome(let secondary): return secondary
        case .unavailable: return []
        }
    }
}

struct MatchActivityTeamColors {
    let first: UIColor
    let second: UIColor
    let neutral: UIColor

    func color(for index: Int) -> UIColor { index == 0 ? first : second }

    struct RGB: Codable, Equatable {
        let red: Int
        let green: Int
        let blue: Int

        var color: UIColor {
            UIColor(red: CGFloat(red) / 255, green: CGFloat(green) / 255,
                    blue: CGFloat(blue) / 255, alpha: 1)
        }

        var components: [Double] { [Double(red) / 255, Double(green) / 255, Double(blue) / 255] }

        private var linearComponents: [Double] {
            components.map { $0 <= 0.04045 ? $0 / 12.92 : pow(($0 + 0.055) / 1.055, 2.4) }
        }

        var luminance: Double {
            let linear = linearComponents
            return linear[0] * 0.2126 + linear[1] * 0.7152 + linear[2] * 0.0722
        }

        func distance(from other: RGB) -> Double {
            zip(lab, other.lab).reduce(0) { $0 + pow($1.0 - $1.1, 2) }.squareRoot()
        }

        private var lab: [Double] {
            let linear = linearComponents
            let xyz = [
                (linear[0] * 0.4124 + linear[1] * 0.3576 + linear[2] * 0.1805) / 0.95047,
                linear[0] * 0.2126 + linear[1] * 0.7152 + linear[2] * 0.0722,
                (linear[0] * 0.0193 + linear[1] * 0.1192 + linear[2] * 0.9505) / 1.08883,
            ].map { $0 > 0.008856 ? pow($0, 1.0 / 3) : 7.787 * $0 + 16.0 / 116 }
            return [max(0, 116 * xyz[1] - 16), 500 * (xyz[0] - xyz[1]), 200 * (xyz[1] - xyz[2])]
        }

        func readable(on appearance: MatchActivityLogoCache.Appearance) -> RGB {
            let background = appearance == .dark ? 0.02 : 0.80
            let rgb = components
            let maximum = rgb.max()!, minimum = rgb.min()!
            let delta = maximum - minimum
            var lightness = (maximum + minimum) / 2
            let saturation = delta == 0 ? 0 : delta / (1 - abs(2 * lightness - 1))
            let hue: Double
            if delta == 0 {
                hue = 0
            } else if maximum == rgb[0] {
                hue = ((rgb[1] - rgb[2]) / delta).truncatingRemainder(dividingBy: 6)
            } else if maximum == rgb[1] {
                hue = (rgb[2] - rgb[0]) / delta + 2
            } else {
                hue = (rgb[0] - rgb[1]) / delta + 4
            }
            let normalizedHue = (hue + 6).truncatingRemainder(dividingBy: 6)
            var result = self
            while (max(result.luminance, background) + 0.05) / (min(result.luminance, background) + 0.05) < 3 {
                lightness = min(1, max(0, lightness + (appearance == .dark ? 0.025 : -0.025)))
                let chroma = (1 - abs(2 * lightness - 1)) * saturation
                let x = chroma * (1 - abs(normalizedHue.truncatingRemainder(dividingBy: 2) - 1))
                let channels: [Double]
                switch normalizedHue {
                case ..<1: channels = [chroma, x, 0]
                case ..<2: channels = [x, chroma, 0]
                case ..<3: channels = [0, chroma, x]
                case ..<4: channels = [0, x, chroma]
                case ..<5: channels = [x, 0, chroma]
                default: channels = [chroma, 0, x]
                }
                let offset = lightness - chroma / 2
                let bytes = channels.map { Int((($0 + offset) * 255).rounded()) }
                result = RGB(red: bytes[0], green: bytes[1], blue: bytes[2])
            }
            return result
        }
    }

    static func resolve(
        first: MatchActivityLogoColor?, second: MatchActivityLogoColor?,
        appearance: MatchActivityLogoCache.Appearance
    ) -> MatchActivityTeamColors {
        let component = appearance == .dark ? 255 : 0
        let monochrome = RGB(red: component, green: component, blue: component)
        let alternate = appearance == .dark
            ? RGB(red: 136, green: 136, blue: 136)
            : RGB(red: 110, green: 110, blue: 110)
        let neutral = appearance == .dark
            ? RGB(red: 158, green: 158, blue: 166)
            : RGB(red: 117, green: 117, blue: 125)
        func candidate(_ logo: MatchActivityLogoColor?) -> RGB {
            switch logo {
            case .vivid(let color, _):
                return color.readable(on: appearance)
            case .monochrome, .unavailable, nil:
                return monochrome
            }
        }
        func secondary(_ logo: MatchActivityLogoColor?, distinctFrom other: RGB) -> RGB? {
            logo?.secondary.lazy.map { $0.readable(on: appearance) }
                .first { $0.distance(from: other) >= 30 }
        }
        var firstColor = candidate(first)
        var secondColor = candidate(second)
        if firstColor.distance(from: secondColor) < 30 {
            if firstColor.distance(from: monochrome) >= 30 {
                secondColor = monochrome
            } else if let color = secondary(second, distinctFrom: firstColor) {
                secondColor = color
            } else if let color = secondary(first, distinctFrom: secondColor) {
                firstColor = color
            } else {
                firstColor = monochrome
                secondColor = alternate
            }
        }
        return MatchActivityTeamColors(first: firstColor.color, second: secondColor.color, neutral: neutral.color)
    }

    static func extract(from image: CGImage) -> MatchActivityLogoColor {
        let size = 32
        guard let context = CGContext(
            data: nil, width: size, height: size, bitsPerComponent: 8, bytesPerRow: size * 4,
            space: CGColorSpace(name: CGColorSpace.sRGB)!,
            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue | CGBitmapInfo.byteOrder32Big.rawValue
        ), let data = context.data else { return .unavailable }
        let scale = CGFloat(size) / CGFloat(max(image.width, image.height))
        let width = max(1, Int((CGFloat(image.width) * scale).rounded()))
        let height = max(1, Int((CGFloat(image.height) * scale).rounded()))
        context.interpolationQuality = .medium
        context.draw(image, in: CGRect(x: (size - width) / 2, y: (size - height) / 2, width: width, height: height))
        let pixels = data.assumingMemoryBound(to: UInt8.self)
        var visible = 0
        var white = 0
        var bins: [Int: (count: Int, red: Int, green: Int, blue: Int)] = [:]
        for offset in stride(from: 0, to: size * size * 4, by: 4) {
            let alpha = Int(pixels[offset + 3])
            guard alpha >= 128 else { continue }
            visible += 1
            let rgb = (0..<3).map { min(255, Int(pixels[offset + $0]) * 255 / alpha) }
            let maximum = rgb.max()!, minimum = rgb.min()!
            if minimum >= 192, maximum - minimum < 32 { white += 1 }
            guard Double(maximum) >= 0.15 * 255, Double(maximum - minimum) / Double(maximum) >= 0.25 else { continue }
            let key = ((rgb[0] >> 4) << 8) | ((rgb[1] >> 4) << 4) | (rgb[2] >> 4)
            let previous = bins[key] ?? (0, 0, 0, 0)
            bins[key] = (previous.count + 1, previous.red + rgb[0], previous.green + rgb[1], previous.blue + rgb[2])
        }
        guard visible > 0 else { return .unavailable }
        let ranked = bins.keys.sorted {
            let firstCount = bins[$0]!.count, secondCount = bins[$1]!.count
            return firstCount == secondCount ? $0 < $1 : firstCount > secondCount
        }
        var candidates: [RGB] = []
        for key in ranked {
            let bin = bins[key]!
            guard Double(bin.count) >= Double(visible) * 0.05 else { continue }
            let color = RGB(red: bin.red / bin.count, green: bin.green / bin.count, blue: bin.blue / bin.count)
            if candidates.allSatisfy({ $0.distance(from: color) >= 30 }) {
                candidates.append(color)
            }
            if candidates.count == 4 { break }
        }
        if white > visible / 2 || bins.isEmpty {
            return .monochrome(secondary: candidates)
        }
        guard let primary = candidates.first else { return .unavailable }
        return .vivid(primary, secondary: Array(candidates.dropFirst()))
    }
}
