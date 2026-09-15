import CoreSpotlight
import SwiftUI
import shared

struct ContentView: View {
    @Environment(\.scenePhase) private var scenePhase
    @State private var pendingDeepLink: PendingDeepLink?

    var body: some View {
        ComposeView(pendingDeepLink: pendingDeepLink)
            .ignoresSafeArea()
            .onChange(of: scenePhase) { phase in
                if phase == .active { FavoriteSearchStore.shared.retryIndexing() }
            }
            .onContinueUserActivity(CSSearchableItemActionType) { activity in
                if let identifier = activity.userInfo?[CSSearchableItemActivityIdentifier] as? String,
                   let url = FavoriteSearchStore.shared.url(for: identifier) {
                    pendingDeepLink = PendingDeepLink(url: url)
                }
            }
            .onOpenURL { url in
                pendingDeepLink = PendingDeepLink(url: url)
            }
    }
}

private struct ComposeView: UIViewControllerRepresentable {
    let pendingDeepLink: PendingDeepLink?

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    func makeUIViewController(context: Context) -> UIViewController {
        context.coordinator.open(pendingDeepLink)
        return MainViewControllerKt.MainViewController(
            authToken: GeneratedBuildConfig.authToken,
            deepLinkHandler: context.coordinator.deepLinkHandler,
            onWidgetSnapshotChanged: { snapshotJSON in
                WidgetSnapshotStore.publish(snapshotJSON)
            },
            onSearchFavoritesChanged: { snapshotJSON in
                FavoriteSearchStore.shared.publish(snapshotJSON)
            }
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        context.coordinator.open(pendingDeepLink)
    }

    final class Coordinator {
        let deepLinkHandler = AppDeepLinkHandler()
        private var lastDeepLinkID: UUID?

        func open(_ deepLink: PendingDeepLink?) {
            guard let deepLink, deepLink.id != lastDeepLinkID else { return }
            lastDeepLinkID = deepLink.id
            deepLinkHandler.openUrl(url: deepLink.url.absoluteString)
        }
    }
}

private struct PendingDeepLink {
    let id = UUID()
    let url: URL
}
