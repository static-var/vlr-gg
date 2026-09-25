import XCTest

final class NativeLocalizationTests: XCTestCase {
    private let languages = ["en", "pt-BR", "hi", "tr", "es", "fr", "de", "ko", "ru"]

    func testAppIntentAndSiriCatalogIsBundledForEverySupportedLanguage() throws {
        let keys = [
            "Open Val Esports",
            "Next Favorite Match",
            "Open Next Favorite Match",
            "Set Spoiler Protection",
            "Favorite",
            "LIVE",
            "Open Favorite",
            "UPCOMING",
            "favorite.kind.team",
            "favorite.kind.event",
            "favorite.kind.match",
            "favorite.kind.player",
            "spotlight.favorite.description",
            "spotlight.favorite.keyword",
            "siri.cannot_open_match",
            "siri.match_live",
            "siri.match_live_in_event",
            "siri.match_starts",
            "siri.match_starts_in_event",
            "siri.match_upcoming",
            "siri.match_upcoming_in_event",
            "siri.no_favorites",
            "siri.no_matches",
            "siri.opening_match",
            "siri.opening_saved_match",
            "siri.saved_match_detail",
            "siri.spoiler_protection_off",
            "siri.spoiler_protection_on",
            "siri.summary_opening_app",
            "siri.unavailable",
        ]

        for language in languages {
            let bundle = try localizedBundle(language, in: Bundle(for: Self.self))
            for key in keys {
                XCTAssertNotEqual(
                    bundle.localizedString(forKey: key, value: "__missing__", table: "Localizable"),
                    "__missing__",
                    "Missing \(language) translation for \(key)"
                )
            }
        }
    }

    func testAppShortcutPhrasesKeepTheApplicationNameToken() throws {
        let keys = [
            "Open ${applicationName}",
            "Open my next match in ${applicationName}",
            "Turn on spoiler protection in ${applicationName}",
            "What's my next favorite match in ${applicationName}",
            "When is my next match in ${applicationName}",
        ]

        for language in languages {
            let bundle = try localizedBundle(language, in: .main)
            for key in keys {
                let phrase = bundle.localizedString(forKey: key, value: "__missing__", table: "AppShortcuts")
                XCTAssertNotEqual(phrase, "__missing__", "Missing \(language) phrase for \(key)")
                XCTAssertEqual(
                    phrase.components(separatedBy: "${applicationName}").count - 1,
                    1,
                    "\(language) phrase must contain the applicationName token once"
                )
            }
        }
    }

    private func localizedBundle(_ language: String, in resourceBundle: Bundle) throws -> Bundle {
        let url = try XCTUnwrap(
            resourceBundle.url(forResource: language, withExtension: "lproj"),
            "Missing \(language).lproj in the app-intents test bundle"
        )
        return try XCTUnwrap(Bundle(url: url))
    }
}
