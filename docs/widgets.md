# Favorite match widgets

The iOS WidgetKit extension and Android Glance widget show live and upcoming matches connected to direct match, team, event, and player favorites. Player favorites include their current team's schedule. Live matches sort first. Scores follow the app's spoiler setting, and each match opens `vlr://match/<id>` in the app. Small and medium iOS widgets open their single match; large widgets link each match separately.

## Shared state

`PublishUpcomingMatchesWidget` observes `FavoriteScheduleRepository`, direct favorites, and spoiler preferences. It publishes native display data, favorite IDs, the selected Prism palette, and a save timestamp. Native JSON contains no credentials. Hidden scores are removed before publication and masked again when rendered.

The favorite schedule query combines the canonical match cache, overview fallback, team schedules, and event schedules. Both overview and detail refreshes update the canonical `matches` table, so widgets read its latest status and score. Unknown and completed matches are excluded. Dates without a year remain unknown instead of guessing an instant. No schema migration is needed.

`RefreshFavoriteMatches` refreshes the overview, favorite player profiles and current teams, favorite team and event schedules, and active match details. It works without a Compose lifecycle. Network calls use the existing repositories, with at most four concurrent profile or detail requests. A failed refresh preserves the previous native snapshot for retry.

## Android refresh

`VlrApplication` initializes the dependency graph for both activity launches and background work. `FavoriteWidgetRefreshWorker` calls shared Kotlin to refresh data and publish a new snapshot. The receiver schedules unique network-constrained WorkManager jobs when a widget is enabled or updated, and cancels them when the last widget is removed. Periodic work requests a 30-minute interval. WorkManager persists jobs across ordinary process death and reboot; Android can delay work and does not run it while the app is force-stopped.

The app and worker write `upcoming_matches_widget.json` atomically. The worker replaces it only if the input snapshot has not changed during its request, so a foreground favorite, spoiler, or theme change wins over an older background response.

Android exposes two fixed widget picker entries: a centered 2×2 matchup and a 4×2 scoreboard, each with one match, match navigation, and spoiler masking. Android resize ranges cannot exclude intermediate grid sizes such as 3×2, so users choose a size in the picker instead of dragging resize handles. Grid cell dimensions remain launcher-controlled. Both providers share snapshot updates and background refresh; removing one size must not cancel refresh while the other remains installed. GlanceTheme supplies Material 3 system colors, including wallpaper-derived dynamic colors on Android 12+ and the Material baseline on older devices. Background, primary, on-surface, secondary text, and outline roles come from that theme rather than the saved Prism palette. Text uses native sans-serif because the Glance font API accepts system families rather than bundled Space Grotesk font resources.

## Android legacy widget updates

The original `app` module registered `dev.staticvar.vlr.widget.ScoreWidgetReceiver` under application ID `dev.staticvar.vlr`. The rewrite declares that exact receiver explicitly, because its new Android namespace differs. Android owns numeric app-widget IDs; retaining the package and receiver component allows existing launcher bindings to survive an in-place app update. The debug application ID remains `dev.staticvar.vlr.debug` in both versions. Release updates must use the existing signing identity and a higher version code.

This legacy widget keeps its original all-live/upcoming-matches feed and scrollable, resizable layout. It does not depend on favorites or a foreground-generated snapshot. Its own atomic snapshot comes from `MatchRepository.refreshMatches()` in a native Android worker. It uses system Material 3 colors and opens current match details. The new fixed 2×2 and 4×2 favorite widgets remain separate.

The original worker class `dev.staticvar.vlr.workers.WidgetUpdateWorker` and unique periodic work name `widget_update` are retained, with a 15-minute requested interval. WorkManager is aligned to the old catalog's 2.11.2 version to avoid downgrading its persisted database. A package-replaced receiver and app launch restore refresh scheduling for installed legacy widgets. Transient refresh failures retry without clearing the last successful data; score rendering also checks the current spoiler preference.

## iOS refresh and layouts

The WidgetKit timeline provider fetches data with native `URLSession` while the main app is closed. The extension does not link Compose or open the app's database. It uses its own generated build configuration for the same API authentication as the app.

The app writes `upcoming-matches.json` in the `group.dev.staticvar.vlr.ios` App Group. The extension writes a separate refreshed cache and accepts it only for the current app snapshot configuration. Transient HTTP or decoding failures retain the last successful data. WidgetKit receives requests for another timeline after 15 minutes for live matches or 30 minutes for upcoming matches, with an earlier request near a scheduled start. These are requested times, not a guarantee of exact refresh intervals. Scores are a snapshot of the last refresh, not a continuous live feed.

The three supported iOS families are small, medium, and large. Small shows one centered matchup and its score or time, with two lines per team name. Medium shows one scoreboard with event and match metadata. Large shows up to two scoreboards separated by a line, filling the available height. The extension bundles the app's Space Grotesk font and license. Backgrounds have a subtle tint derived from the saved Prism palette. SwiftUI adapts saved colors to its color scheme; dark rendering is verified, but switching back to light without reloading remained unresolved during simulator verification.

`iosApp/project.yml` is the XcodeGen source of truth. Signed device builds require the App Group on both app and extension provisioning profiles. Generated authentication configuration is ignored by Git.

## Verification

Run the focused shared and database tests on the supported iOS simulator target:

```sh
build-brief ./gradlew :domain:iosSimulatorArm64Test --tests '*RefreshFavoriteMatchesTest*' :data:iosSimulatorArm64Test --tests '*FavoriteScheduleRepositoryImplTest*' :shared:iosSimulatorArm64Test --tests '*UpcomingMatchesSnapshotTest*' --tests '*AppDeepLinkHandlerTest*'
```

Check native rendering and routing on a simulator or emulator. Background verification should leave the main app closed, trigger the platform's scheduled work, and verify that the native snapshot timestamp advances before opening a match from the widget.
