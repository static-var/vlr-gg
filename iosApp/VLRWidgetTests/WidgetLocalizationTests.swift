import XCTest

final class WidgetLocalizationTests: XCTestCase {
    private let languages = ["en", "pt-BR", "hi", "tr", "es", "fr", "de", "ko", "ru"]

    func testWidgetCatalogIsBundledForEverySupportedLanguage() throws {
        let keys = [
            "%lld MATCHES",
            "FAVORITES",
            "Favorite a team, player, event, or match in Val Esports.",
            "Favorite match",
            "Favorite matches",
            "LIVE",
            "Live and upcoming matches connected to your Val Esports favorites.",
            "MATCH",
            "NO MATCHES",
            "No favorites.",
            "No matches scheduled.",
            "Not started",
            "Open Val Esports once to connect your favorites.",
            "Open the app.",
            "Opens match details",
            "Score hidden",
            "Score unavailable",
            "Scores hidden",
            "TBD",
            "UPCOMING",
            "Your favorites return here when they play.",
            "You're caught up on your favorites.",
            "widget.score_format",
        ]

        for language in languages {
            let bundle = try localizedBundle(language)
            for key in keys {
                XCTAssertNotEqual(
                    bundle.localizedString(forKey: key, value: "__missing__", table: "Localizable"),
                    "__missing__",
                    "Missing \(language) widget translation for \(key)"
                )
            }
        }
    }

    func testLocalizedMatchCountUsesNativePluralRules() throws {
        let cases: [(language: String, count: Int64, expected: String)] = [
            ("en", 1, "1 MATCH"),
            ("en", 2, "2 MATCHES"),
            ("de", 2, "2 PARTIEN"),
            ("es", 2, "2 PARTIDAS"),
            ("fr", 2, "2 RENCONTRES"),
            ("hi", 2, "2 मैच"),
            ("ko", 2, "경기 2개"),
            ("pt-BR", 2, "2 PARTIDAS"),
            ("ru", 2, "2 МАТЧА"),
            ("ru", 5, "5 МАТЧЕЙ"),
            ("ru", 21, "21 МАТЧ"),
            ("tr", 2, "2 MAÇ"),
        ]

        for item in cases {
            let bundle = try localizedBundle(item.language)
            let format = bundle.localizedString(
                forKey: "%lld MATCHES",
                value: "__missing__",
                table: "Localizable"
            )
            XCTAssertNotEqual(format, "__missing__")
            XCTAssertEqual(
                String(format: format, locale: Locale(identifier: item.language), item.count),
                item.expected,
                "Wrong plural for \(item.language), count \(item.count)"
            )
        }
    }

    private func localizedBundle(_ language: String) throws -> Bundle {
        let resourceBundle = Bundle(for: Self.self)
        let url = try XCTUnwrap(
            resourceBundle.url(forResource: language, withExtension: "lproj"),
            "Missing \(language).lproj in the widget test bundle"
        )
        return try XCTUnwrap(Bundle(url: url))
    }
}
