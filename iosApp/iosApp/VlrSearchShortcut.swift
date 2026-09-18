import AppIntents
import Foundation
import UIKit
import shared

struct OpenVlrIntent: AppIntent {
    static let title: LocalizedStringResource = "Open VLR"
    static let description = IntentDescription("Open Val Esports, also known as VLR.")
    static let openAppWhenRun = true

    func perform() async throws -> some IntentResult {
        .result()
    }
}

struct NextFavoriteMatchIntent: AppIntent {
    static let title: LocalizedStringResource = "Next Favorite Match"
    static let description = IntentDescription("Find your next live or upcoming favorite match without revealing scores.")
    static let openAppWhenRun = false

    @MainActor
    func perform() async throws -> some IntentResult & ProvidesDialog & ReturnsValue<String> {
        let response: String
        do {
            let result = try await SiriMatchResponse.actions().nextMatch()
            try Task.checkCancellation()
            response = SiriMatchResponse.summary(result)
        } catch {
            try Task.checkCancellation()
            response = SiriMatchResponse.unavailable
        }
        return .result(value: response, dialog: "\(response)")
    }
}

struct OpenNextFavoriteMatchIntent: AppIntent {
    static let title: LocalizedStringResource = "Open Next Favorite Match"
    static let description = IntentDescription("Open the next live or upcoming match from your favorites.")

    static let openAppWhenRun = true

    @MainActor
    func perform() async throws -> some IntentResult & ProvidesDialog & ReturnsValue<URL> {
        let destination: URL
        let response: String
        do {
            let result = try await SiriMatchResponse.actions().nextMatch()
            try Task.checkCancellation()
            if let match = result.match {
                destination = VLRWidgetContract.matchURL(id: match.id)
                response = result.refreshed
                    ? NativeLocalization.format("siri.opening_match", match.team1, match.team2)
                    : NativeLocalization.format("siri.opening_saved_match", match.team1, match.team2)
            } else {
                destination = VLRWidgetContract.appURL
                response = NativeLocalization.format(
                    "siri.summary_opening_app",
                    SiriMatchResponse.summary(result)
                )
            }
        } catch {
            try Task.checkCancellation()
            destination = VLRWidgetContract.appURL
            response = NativeLocalization.format("siri.summary_opening_app", SiriMatchResponse.unavailable)
        }
        guard await UIApplication.shared.open(destination) else {
            throw NSError(
                domain: "ValorantEsports.Siri",
                code: 1,
                userInfo: [NSLocalizedDescriptionKey: NativeLocalization.string("siri.cannot_open_match")]
            )
        }
        return .result(value: destination, dialog: "\(response)")
    }
}

struct SetSpoilerProtectionIntent: AppIntent {
    static let title: LocalizedStringResource = "Set Spoiler Protection"
    static let description = IntentDescription("Choose whether Val Esports hides match scores.")
    static let openAppWhenRun = false

    @Parameter(title: "Hide scores", default: true)
    var enabled: Bool

    static var parameterSummary: some ParameterSummary {
        Summary("Set spoiler protection to \(\.$enabled)")
    }

    @MainActor
    func perform() async throws -> some IntentResult & ProvidesDialog {
        let matchesJSON = try await SiriMatchResponse.actions().setSpoilersHidden(enabled: enabled)
        try await WidgetSnapshotStore.setSpoilersHidden(enabled, matchesJSON: matchesJSON)
        return .result(dialog: enabled
            ? "\(NativeLocalization.string("siri.spoiler_protection_on"))"
            : "\(NativeLocalization.string("siri.spoiler_protection_off"))")
    }
}

enum SiriMatchResponse {
    static var unavailable: String { NativeLocalization.string("siri.unavailable") }

    @MainActor
    static func actions() -> SiriActions {
        SiriActions(authToken: GeneratedBuildConfig.authToken)
    }

    static func summary(_ result: SiriMatchResult) -> String {
        guard result.hasFavorites else {
            return NativeLocalization.string("siri.no_favorites")
        }
        guard let match = result.match else {
            return result.refreshed
                ? NativeLocalization.string("siri.no_matches")
                : unavailable
        }
        let detail: String
        if match.status == "LIVE" {
            detail = match.event.isEmpty
                ? NativeLocalization.format("siri.match_live", match.team1, match.team2)
                : NativeLocalization.format("siri.match_live_in_event", match.team1, match.team2, match.event)
        } else if let timestamp = match.startTimeEpochMillis {
            let formatter = DateFormatter()
            formatter.dateStyle = .full
            formatter.timeStyle = .short
            let date = Date(timeIntervalSince1970: timestamp.doubleValue / 1_000)
            let formattedDate = formatter.string(from: date)
            detail = match.event.isEmpty
                ? NativeLocalization.format("siri.match_starts", match.team1, match.team2, formattedDate)
                : NativeLocalization.format(
                    "siri.match_starts_in_event",
                    match.team1,
                    match.team2,
                    match.event,
                    formattedDate
                )
        } else {
            detail = match.event.isEmpty
                ? NativeLocalization.format("siri.match_upcoming", match.team1, match.team2)
                : NativeLocalization.format("siri.match_upcoming_in_event", match.team1, match.team2, match.event)
        }
        return result.refreshed ? detail : NativeLocalization.format("siri.saved_match_detail", detail)
    }
}

struct VlrAppShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: OpenVlrIntent(),
            phrases: ["Open VLR in \(.applicationName)"],
            shortTitle: "Open VLR",
            systemImageName: "play.circle"
        )
        AppShortcut(
            intent: NextFavoriteMatchIntent(),
            phrases: [
                "What's my next favorite match in \(.applicationName)",
                "When is my next match in \(.applicationName)",
            ],
            shortTitle: "Next Favorite Match",
            systemImageName: "sportscourt"
        )
        AppShortcut(
            intent: OpenNextFavoriteMatchIntent(),
            phrases: ["Open my next match in \(.applicationName)"],
            shortTitle: "Open Next Match",
            systemImageName: "arrow.up.forward.app"
        )
        AppShortcut(
            intent: SetSpoilerProtectionIntent(),
            phrases: ["Turn on spoiler protection in \(.applicationName)"],
            shortTitle: "Hide Scores",
            systemImageName: "eye.slash"
        )
    }
}
