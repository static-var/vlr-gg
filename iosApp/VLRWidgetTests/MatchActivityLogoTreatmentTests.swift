import CoreGraphics
import Foundation
import UIKit
import XCTest

/// Checks silhouette contrast decisions, rendered pixels, and persistent logo variants.
final class MatchActivityLogoTreatmentTests: XCTestCase {
    func testTransparentLogosReceiveOutlineOnlyOnLowContrastBackground() throws {
        let dark = try image(gray: 0)
        let light = try image(gray: 1)
        let darkAnalysis = MatchActivityLogoTreatment.analyze(dark)
        let lightAnalysis = MatchActivityLogoTreatment.analyze(light)
        XCTAssertTrue(darkAnalysis.needsOutline(background: 0))
        XCTAssertFalse(darkAnalysis.needsOutline(background: 1))
        XCTAssertFalse(lightAnalysis.needsOutline(background: 0))
        XCTAssertTrue(lightAnalysis.needsOutline(background: 1))
    }

    func testOpaqueRectanglesAndEmptyImagesDoNotReceiveOutline() throws {
        let opaque = try image(gray: 0, inset: 0)
        let empty = try image(gray: 0, alpha: 0)
        for source in [opaque, empty] {
            let analysis = MatchActivityLogoTreatment.analyze(source)
            XCTAssertFalse(analysis.needsOutline(background: 0))
            XCTAssertFalse(analysis.needsOutline(background: 1))
        }
    }

    func testOutlineAddsContrastingPixelsWithoutReplacingLogoOrFillingBackground() throws {
        let source = try image(gray: 0)
        let outlined = try XCTUnwrap(MatchActivityLogoTreatment.prepare(source, background: 0, maxPixelSize: 32))
        let original = try XCTUnwrap(MatchActivityLogoTreatment.prepare(source, background: 1, maxPixelSize: 32))
        let outlinedPixels = try pixels(outlined)
        let originalPixels = try pixels(original)
        let center = (19 * outlined.width + 19) * 4
        XCTAssertEqual(Array(outlinedPixels[center..<center + 4]), [0, 0, 0, 255])
        XCTAssertEqual(Array(outlinedPixels[0..<4]), [0, 0, 0, 0])
        let addedPixels = stride(from: 0, to: outlinedPixels.count, by: 4).filter {
            originalPixels[$0 + 3] == 0 && outlinedPixels[$0 + 3] > 0
        }
        XCTAssertFalse(addedPixels.isEmpty)
        for offset in addedPixels {
            XCTAssertEqual(outlinedPixels[offset], outlinedPixels[offset + 3])
            XCTAssertEqual(outlinedPixels[offset + 1], outlinedPixels[offset + 3])
            XCTAssertEqual(outlinedPixels[offset + 2], outlinedPixels[offset + 3])
        }
    }

    func testNonSquareVariantsPreserveFittedDimensionsAndEqualPadding() throws {
        let source = try image(width: 40, height: 20, gray: 0, inset: 4)
        for background in [CGFloat(0), CGFloat(1)] {
            let rendered = try XCTUnwrap(MatchActivityLogoTreatment.prepare(
                source, background: background, maxPixelSize: 60
            ))
            XCTAssertEqual(rendered.width, 66)
            XCTAssertEqual(rendered.height, 36)
        }
    }

    func testCachedVariantsRoundTripPixelsForEveryAppearanceAndSize() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let original = directory.appendingPathComponent("synthetic-logo")
        let source = try image(width: 1024, height: 512, gray: 0, inset: 64)
        XCTAssertTrue(try MatchActivityLogoCache.prepareVariants(source, original: original))
        XCTAssertFalse(try MatchActivityLogoCache.prepareVariants(source, original: original))
        XCTAssertEqual(try FileManager.default.contentsOfDirectory(atPath: directory.path).count, 5)
        XCTAssertEqual(MatchActivityLogoCache.logoColor(original: original), .monochrome())
        for appearance in MatchActivityLogoCache.Appearance.allCases {
            for size in MatchActivityLogoCache.LogoSize.allCases {
                let loaded = try XCTUnwrap(MatchActivityLogoCache.image(
                    original: original, appearance: appearance, size: size
                ))
                let cached = try XCTUnwrap(loaded.cgImage)
                let expectedPoints: CGFloat = size == .compact ? 20 : 58
                XCTAssertEqual(max(loaded.size.width, loaded.size.height), expectedPoints, accuracy: 0.001)
                XCTAssertEqual(loaded.size.width / loaded.size.height,
                               CGFloat(cached.width) / CGFloat(cached.height), accuracy: 0.001)
                let expected = try XCTUnwrap(MatchActivityLogoTreatment.prepare(
                    source, background: appearance.background, maxPixelSize: size.rawValue
                ))
                XCTAssertEqual(cached.width, expected.width)
                XCTAssertEqual(cached.height, expected.height)
                XCTAssertLessThanOrEqual(cached.width, 180)
                XCTAssertLessThanOrEqual(cached.height, 180)
                XCTAssertEqual(try pixels(cached), try pixels(expected))
            }
        }
    }

    func testMissingPreparedVariantsLoadOriginalWithinRequestedPointAndPixelBounds() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let original = directory.appendingPathComponent("synthetic-logo.png")
        let source = try image(width: 1024, height: 512, gray: 0, inset: 64)
        try XCTUnwrap(UIImage(cgImage: source).pngData()).write(to: original)

        for size in MatchActivityLogoCache.LogoSize.allCases {
            XCTAssertNil(MatchActivityLogoCache.preparedImage(original: original, appearance: .dark, size: size))
            let loaded = try XCTUnwrap(MatchActivityLogoCache.image(
                original: original, appearance: .dark, size: size
            ))
            let bitmap = try XCTUnwrap(loaded.cgImage)
            let expectedPoints: CGFloat = size == .compact ? 20 : 58
            XCTAssertEqual(loaded.size.width, expectedPoints, accuracy: 0.001)
            XCTAssertEqual(loaded.size.height, expectedPoints / 2, accuracy: 0.001)
            XCTAssertLessThanOrEqual(bitmap.width, size == .compact ? 60 : 174)
            XCTAssertEqual(bitmap.width, bitmap.height * 2)
        }
    }

    func testLogoColorsKeepWhitePrimaryAndExtractSecondaryColors() throws {
        let context = try makeContext(width: 32, height: 32)
        context.setFillColor(gray: 1, alpha: 1)
        context.fill(CGRect(x: 0, y: 0, width: 32, height: 32))
        context.setFillColor(red: 1, green: 0, blue: 0, alpha: 1)
        context.fill(CGRect(x: 8, y: 8, width: 16, height: 16))
        let logo = try XCTUnwrap(context.makeImage())
        XCTAssertEqual(MatchActivityTeamColors.extract(from: logo), .monochrome(secondary: [.init(red: 255, green: 0, blue: 0)]))
        XCTAssertEqual(MatchActivityTeamColors.extract(from: try image(gray: 0)), .monochrome())
        XCTAssertEqual(MatchActivityTeamColors.extract(from: try image(gray: 1)), .monochrome())
        XCTAssertEqual(MatchActivityTeamColors.extract(from: try image(gray: 0, alpha: 0)), .unavailable)
    }

    func testColoredPrimaryIsPreservedWhenWhiteIsSecondary() throws {
        let context = try makeContext(width: 32, height: 32)
        context.setFillColor(red: 1, green: 0, blue: 0, alpha: 1)
        context.fill(CGRect(x: 0, y: 0, width: 32, height: 32))
        context.setFillColor(gray: 1, alpha: 1)
        context.fill(CGRect(x: 8, y: 8, width: 16, height: 16))
        XCTAssertEqual(
            MatchActivityTeamColors.extract(from: try XCTUnwrap(context.makeImage())),
            .vivid(.init(red: 255, green: 0, blue: 0))
        )
    }

    func testSmallAccentDoesNotDictateTeamColor() throws {
        let context = try makeContext(width: 32, height: 32)
        context.setFillColor(gray: 1, alpha: 1)
        context.fill(CGRect(x: 0, y: 0, width: 32, height: 32))
        context.setFillColor(red: 1, green: 0, blue: 0, alpha: 1)
        context.fill(CGRect(x: 0, y: 0, width: 2, height: 2))
        XCTAssertEqual(MatchActivityTeamColors.extract(from: try XCTUnwrap(context.makeImage())), .monochrome())
    }

    func testTeamColorsPreserveVividLogoAndUseWhiteForDarkMonochrome() throws {
        let red = MatchActivityTeamColors.RGB(red: 255, green: 0, blue: 0)
        let dark = MatchActivityTeamColors.resolve(first: .vivid(red), second: .monochrome(), appearance: .dark)
        XCTAssertEqual(dark.first, red.color)
        XCTAssertEqual(dark.second, MatchActivityTeamColors.RGB(red: 255, green: 255, blue: 255).color)
        let light = MatchActivityTeamColors.resolve(first: .vivid(red), second: .monochrome(), appearance: .light)
        XCTAssertEqual(light.second, MatchActivityTeamColors.RGB(red: 0, green: 0, blue: 0).color)
    }

    func testSimilarTeamColorsUseWhiteFallbackWithReadableLightEquivalent() throws {
        let red = MatchActivityTeamColors.RGB(red: 255, green: 0, blue: 0)
        for appearance in MatchActivityLogoCache.Appearance.allCases {
            let colors = MatchActivityTeamColors.resolve(first: .vivid(red), second: .vivid(red), appearance: appearance)
            XCTAssertEqual(colors.first, red.readable(on: appearance).color)
            let component = appearance == .dark ? 255 : 0
            XCTAssertEqual(colors.second, MatchActivityTeamColors.RGB(red: component, green: component, blue: component).color)
            assertReadableAndDistinct(colors, appearance: appearance)
        }
    }

    func testWhitePrimariesUseSecondaryColorFromEitherLogo() throws {
        let context = try makeContext(width: 32, height: 32)
        context.setFillColor(gray: 1, alpha: 1)
        context.fill(CGRect(x: 0, y: 0, width: 32, height: 32))
        context.setFillColor(red: 1, green: 0, blue: 0, alpha: 1)
        context.fill(CGRect(x: 8, y: 8, width: 16, height: 8))
        let extracted = MatchActivityTeamColors.extract(from: try XCTUnwrap(context.makeImage()))
        let red = MatchActivityTeamColors.RGB(red: 255, green: 0, blue: 0)
        XCTAssertEqual(extracted, .monochrome(secondary: [red]))
        for appearance in MatchActivityLogoCache.Appearance.allCases {
            for firstHasSecondary in [false, true] {
                let colors = MatchActivityTeamColors.resolve(
                    first: firstHasSecondary ? extracted : .monochrome(),
                    second: firstHasSecondary ? .monochrome() : extracted, appearance: appearance
                )
                let component = appearance == .dark ? 255 : 0
                let primary = MatchActivityTeamColors.RGB(red: component, green: component, blue: component).color
                XCTAssertEqual(firstHasSecondary ? colors.first : colors.second, red.readable(on: appearance).color)
                XCTAssertEqual(firstHasSecondary ? colors.second : colors.first, primary)
                assertReadableAndDistinct(colors, appearance: appearance)
            }
        }
    }

    func testWhitePrimariesWithoutUsableSecondaryUseReadableGray() throws {
        for appearance in MatchActivityLogoCache.Appearance.allCases {
            let colors = MatchActivityTeamColors.resolve(
                first: .monochrome(), second: .monochrome(), appearance: appearance
            )
            let gray = appearance == .dark ? 136 : 110
            XCTAssertEqual(colors.second, MatchActivityTeamColors.RGB(red: gray, green: gray, blue: gray).color)
            assertReadableAndDistinct(colors, appearance: appearance)
        }
    }

    func testExtractedColorsReachGraphicalContrastInBothAppearances() throws {
        let difficult = [MatchActivityTeamColors.RGB(red: 15, green: 40, blue: 90),
                         MatchActivityTeamColors.RGB(red: 250, green: 220, blue: 30)]
        for appearance in MatchActivityLogoCache.Appearance.allCases {
            let background = appearance == .dark ? 0.02 : 0.80
            for color in difficult {
                let readable = color.readable(on: appearance)
                let ratio = (max(readable.luminance, background) + 0.05) / (min(readable.luminance, background) + 0.05)
                XCTAssertGreaterThanOrEqual(ratio, 3)
            }
        }
    }

    func testOldColorCacheRegeneratesWithoutRebuildingRasterVariants() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let original = directory.appendingPathComponent("synthetic-logo")
        let source = try image(gray: 0)
        XCTAssertTrue(try MatchActivityLogoCache.prepareVariants(source, original: original))
        let colorFile = original.appendingPathExtension("team-color-v2.plist")
        let oldColorFile = original.appendingPathExtension("team-color-v1.plist")
        try FileManager.default.moveItem(at: colorFile, to: oldColorFile)
        let variants = try FileManager.default.contentsOfDirectory(at: directory, includingPropertiesForKeys: nil)
            .filter { $0.pathExtension == "pixels" }
        XCTAssertEqual(variants.count, 4)
        let before = try variants.map { try Data(contentsOf: $0) }
        XCTAssertTrue(try MatchActivityLogoCache.prepareVariants(source, original: original))
        XCTAssertEqual(try variants.map { try Data(contentsOf: $0) }, before)
        XCTAssertEqual(try PropertyListDecoder().decode(MatchActivityLogoColor.self, from: Data(contentsOf: colorFile)), .monochrome())
        XCTAssertEqual(MatchActivityLogoCache.logoColor(original: original), .monochrome())
        XCTAssertFalse(try MatchActivityLogoCache.prepareVariants(source, original: original))
    }

    private func assertReadableAndDistinct(
        _ colors: MatchActivityTeamColors, appearance: MatchActivityLogoCache.Appearance
    ) {
        let background = appearance == .dark ? 0.02 : 0.80
        let components = [colors.first, colors.second].map { color -> MatchActivityTeamColors.RGB in
            var red: CGFloat = 0, green: CGFloat = 0, blue: CGFloat = 0, alpha: CGFloat = 0
            color.getRed(&red, green: &green, blue: &blue, alpha: &alpha)
            return .init(red: Int((red * 255).rounded()), green: Int((green * 255).rounded()), blue: Int((blue * 255).rounded()))
        }
        for color in components {
            let ratio = (max(color.luminance, background) + 0.05) / (min(color.luminance, background) + 0.05)
            XCTAssertGreaterThanOrEqual(ratio, 3)
        }
        XCTAssertGreaterThanOrEqual(components[0].distance(from: components[1]), 30)
    }

    private func image(
        width: Int = 32, height: Int = 32, gray: CGFloat, alpha: CGFloat = 1, inset: Int = 8
    ) throws -> CGImage {
        let context = try makeContext(width: width, height: height)
        context.setFillColor(gray: gray, alpha: alpha)
        context.fill(CGRect(x: inset, y: inset, width: width - inset * 2, height: height - inset * 2))
        return try XCTUnwrap(context.makeImage())
    }

    private func pixels(_ image: CGImage) throws -> [UInt8] {
        let context = try makeContext(width: image.width, height: image.height)
        context.draw(image, in: CGRect(x: 0, y: 0, width: image.width, height: image.height))
        let data = try XCTUnwrap(context.data).assumingMemoryBound(to: UInt8.self)
        return Array(UnsafeBufferPointer(start: data, count: image.width * image.height * 4))
    }

    private func makeContext(width: Int, height: Int) throws -> CGContext {
        try XCTUnwrap(CGContext(
            data: nil, width: width, height: height, bitsPerComponent: 8, bytesPerRow: width * 4,
            space: CGColorSpace(name: CGColorSpace.sRGB)!,
            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue | CGBitmapInfo.byteOrder32Big.rawValue
        ))
    }
}
