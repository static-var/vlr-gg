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
                    ? "Opening \(match.team1) versus \(match.team2)."
                    : "From saved matches, opening \(match.team1) versus \(match.team2). The schedule may have changed."
            } else {
                destination = VLRWidgetContract.appURL
                response = "\(SiriMatchResponse.summary(result)) Opening Val Esports."
            }
        } catch {
            try Task.checkCancellation()
            destination = VLRWidgetContract.appURL
            response = "\(SiriMatchResponse.unavailable) Opening Val Esports."
        }
        guard await UIApplication.shared.open(destination) else {
            throw NSError(domain: "ValorantEsports.Siri", code: 1, userInfo: [NSLocalizedDescriptionKey: "I couldn't open that match. Please open Val Esports and try again."])
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
            ? "Spoiler protection is on. Match scores are hidden."
            : "Spoiler protection is off. Match scores are visible.")
    }
}

enum SiriMatchResponse {
    static let unavailable = "I couldn't check your favorite matches right now. Please try again later."

    @MainActor
    static func actions() -> SiriActions {
        SiriActions(authToken: GeneratedBuildConfig.authToken)
    }

    static func summary(_ result: SiriMatchResult) -> String {
        guard result.hasFavorites else {
            return "Add favorite teams, players, matches, or events in Val Esports first."
        }
        guard let match = result.match else {
            return result.refreshed
                ? "There are no live or upcoming matches from your favorites right now."
                : unavailable
        }
        let teams = "\(match.team1) versus \(match.team2)"
        let event = match.event.isEmpty ? "" : " in \(match.event)"
        let detail: String
        if match.status == "LIVE" {
            detail = "\(teams)\(event) is live."
        } else if let timestamp = match.startTimeEpochMillis {
            let formatter = DateFormatter()
            formatter.dateStyle = .full
            formatter.timeStyle = .short
            let date = Date(timeIntervalSince1970: timestamp.doubleValue / 1_000)
            detail = "\(teams)\(event) starts \(formatter.string(from: date))."
        } else {
            detail = "\(teams)\(event) is upcoming. The start time has not been announced."
        }
        return result.refreshed ? detail : "From saved matches: \(detail) The schedule may have changed."
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
