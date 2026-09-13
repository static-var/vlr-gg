import AppIntents

struct OpenVlrIntent: AppIntent {
    static let title: LocalizedStringResource = "Open VLR"
    static let description = IntentDescription("Open Valorant Esports, also known as VLR.")
    static let openAppWhenRun = true

    func perform() async throws -> some IntentResult {
        .result()
    }
}

struct VlrAppShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: OpenVlrIntent(),
            phrases: [
                "Open VLR in \(.applicationName)",
            ],
            shortTitle: "Open VLR",
            systemImageName: "play.circle"
        )
    }
}
