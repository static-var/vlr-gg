import SwiftUI
import shared

@main
struct iOSApp: App {
    init() {
        #if DEBUG
        let defaultEnvironment = "development"
        #else
        let defaultEnvironment = "production"
        #endif
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "unknown"
        let build = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "unknown"
        SentryTelemetryKt.initializeSentryTelemetry(
            dsn: GeneratedBuildConfig.sentryDsn,
            environment: GeneratedBuildConfig.sentryEnvironment.isEmpty ? defaultEnvironment : GeneratedBuildConfig.sentryEnvironment,
            release: "vlr@\(version)+\(build)",
            dist: "ios-\(build)",
            enabled: GeneratedBuildConfig.sentryEnabled
        )
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
