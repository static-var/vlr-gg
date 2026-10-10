# App Store 1.0.2 privacy and age declaration draft

Reviewed 2026-09-17. Source: `/private/tmp/vlr-dependency-updates`. This is a draft for review, not a published declaration. No App Store forms or server configuration were changed by this audit.

## Privacy answers supported by the app source

**Data collected: Yes.** Sentry is enabled. Recommended conservative declarations for the app's own monitoring:

| Data type | Purpose | Linked to user | Tracking |
| --- | --- | --- | --- |
| Crash Data | App Functionality | Yes, conservative | No |
| Performance Data | App Functionality | Yes, conservative | No |
| Other Diagnostic Data | App Functionality | Yes, conservative | No |
| Product Interaction | App Functionality | Yes, conservative | No |
| Device ID | App Functionality | Yes | No |
| Customer Support | App Functionality | Yes, conservative | No |

These tracking answers cover the monitoring implementation only. Embedded provider collection remains unresolved below. Add Analytics only if the developer also uses these records to evaluate user behavior, audience, or features. Apple includes crash reduction, performance improvements, and customer support in App Functionality.

### Evidence

- `shared/src/commonMain/kotlin/dev/staticvar/vlr/shared/telemetry/SentryTelemetry.kt:50`: PII disabled; crash sampling 100%; tracing 10%; automatic sessions, hangs and watchdog tracking; view hierarchy attached; 100 breadcrumbs; replay sampled at 1% of sessions and 10% on errors; replay text/images masked; logs enabled. Sanitization removes credentials and URL queries, but does not remove `event.user`.
- `shared/src/iosMain/kotlin/dev/staticvar/vlr/shared/telemetry/SentryTelemetry.ios.kt`: native initialization and feedback submission. Feedback contains free text with no supplied name, email or attachments.
- `feature-about/src/commonMain/kotlin/dev/staticvar/vlr/featureabout/presentation/AboutFeedbackDialog.kt`: optional 2,000-character feedback form. Customer Support describes this data; the app does not ask for specific contact details.
- `iosApp/iosApp/GeneratedBuildConfig.swift`: monitoring enabled.
- `iosApp/iosApp.xcodeproj/project.xcworkspace/xcshareddata/swiftpm/Package.resolved`: Sentry Cocoa 8.58.4, revision `f196cbb98e0388381d57908f2f5a9bdc385cde68`.
- Matching SDK checkout: `/Users/staticvar/Library/Developer/Xcode/DerivedData/iosApp-egwxytfvzhlblvawwzyxflnwwjap/SourcePackages/checkouts/sentry-cocoa/Sources/Sentry/SentryClient.m:826` and `:979`. It assigns a persistent installation ID to `event.user.userId` when no user is supplied, independently of `sendDefaultPii=false`. Device ID is the conservative classification for this installation identifier; no account identifier is supplied by the app.
- The SDK's default privacy manifest labels diagnostics unlinked. Actual persistent identifier inclusion does not establish anonymization; this draft conservatively marks linked data rather than relying on that generic manifest.

Paths above are relative to the reviewed source unless absolute. Collection inferred from enabled configuration was not verified by capturing an actual production envelope.

## Calendar access

Added 2026-10-03 from the working tree at `/Users/staticvar/Projects/vlr-gg`, after the audit above. It ships with the event calendar button, so it applies to the first release that includes that button.

**No new data type to declare.** The app asks for add-only calendar access and writes match entries to the user's calendar. It cannot read the calendar, and nothing about the calendar is sent to the developer, Sentry or the API. Apple's privacy answers cover data that leaves the device, so the table above is unchanged.

| Item | Answer |
| --- | --- |
| Permission | `NSCalendarsWriteOnlyAccessUsageDescription`, add-only, iOS 17 and later |
| When asked | Only when the user taps the calendar button on an event |
| What is written | Match title, event name, stage and round, start and end time, match link |
| Read access | None. `NSCalendarsFullAccessUsageDescription` is not declared |
| Stored on device | IDs and start times of entries already added, in `UserDefaults`, to avoid duplicates |
| iOS 16 | No permission. The app previews an `.ics` file that the user can share |

App Review asks for a purpose string that explains the use. The string is "Val Esports adds the upcoming matches you choose to your calendar." and is translated for all nine app languages.

### Evidence

- `shared-ui/src/iosMain/kotlin/dev/staticvar/vlr/sharedui/calendar/CalendarExporter.ios.kt`: `requestWriteOnlyAccessToEvents`, then saves entries to the default calendar. No network call.
- `iosApp/iosApp/Info.plist` and `iosApp/iosApp/InfoPlist.xcstrings`: purpose string and translations.
- `feature-matches/src/iosMain/kotlin/dev/staticvar/vlr/featurematches/calendar/MatchCalendarExporter.ios.kt`: the single-match button uses the system event editor, which needs no permission on iOS 17 and later.

Verified on the iOS 27 simulator: the system prompt shows the purpose string and 16 entries were saved. Not verified on a device or on iOS 16.

### To confirm before publishing

- Sentry breadcrumbs and session replay are enabled. Replay masks text and images, and the calendar code logs nothing, but no production envelope was captured to confirm that match titles written to the calendar never appear in telemetry.
- If the app's privacy policy lists permissions, add calendar (add-only).

## Questions to resolve before publishing privacy answers

1. Inspect the deployed `/media/youtube/<id>` and `/media/twitch/<id>` pages and provider behavior. The app loads these inside a restricted WKWebView. Apple requires disclosure of collection via such web traffic. Cookie/identifier collection, advertising, retention and downstream tracking cannot be established from mobile code alone.
2. Confirm server access-log and Sentry ingest retention and whether IP addresses are stored or used to derive location. Do not infer Coarse Location solely from having network requests.
3. Confirm how monitoring data is used operationally, and whether collected records are anonymized before reaching a server. This determines whether Analytics or different linkage answers are warranted.

Local server investigation: `/Users/staticvar/Projects/vlrgg-scraper` is at `72c2bfd` on `staticvar/fix-event-prize-parser`, with no other listed worktree. Its `app/services/news.py:238` extracts iframe/video URLs but the checkout contains no `/media/` player routes, `youtube-nocookie` configuration, or matching provider embed implementation. A targeted search of local project and temporary Python/HTML files did not locate the deployed player code. No server changes were made. This checkout cannot resolve the provider questions.

## Age questionnaire draft

| Question | Recommendation | Basis or remaining uncertainty |
| --- | --- | --- |
| Unrestricted web access | No | Native news; restricted player main-frame navigation; clicked links open externally. |
| Messaging and chat | No | Feedback is sent to developer, not between users. |
| Parental controls | No | No implementation found. |
| Age assurance | No | No implementation found. |
| Gambling / simulated gambling / loot boxes | No | No such app features found. |
| Medical or treatment / wellness features | No | No such app features found. |
| Advertising | Unresolved | No native ad SDK found; embedded player advertising needs verification. |
| User-generated content | Review embedded content | No app-user posting or comment feed; creator clips may need classification depending on intended experience. |
| Weapons and violence | Review actual news/video content | Valorant gameplay can depict guns and fantasy violence. Do not mark all None just because the app is a reader; frequency cannot be established from source alone. |
| Contests | No participation feature found | Reporting tournament results alone does not establish an app contest mechanic. |

Do not select other content frequencies without reviewing representative served articles and clips. Do not target an age number by altering answers; use the rating generated from accurate descriptors.

Navigation evidence: `shared-ui/src/iosMain/kotlin/dev/staticvar/vlr/sharedui/component/news/detail/ArticleVideoWebView.ios.kt:108` restricts main-frame navigation to the original player URL and opens activated links externally. `remote-source/src/commonMain/kotlin/dev/staticvar/vlr/remotesource/news/NewsDataSourceImpl.kt:44` validates YouTube/Twitch IDs and same-API-host player paths.

## Official reference

- [Apple App Privacy Details](https://developer.apple.com/app-store/app-privacy-details/): collection, identifier/linkage, purposes, feedback and webview definitions.
- [Apple: Accessing the event store](https://developer.apple.com/documentation/eventkit/accessing-the-event-store): add-only and full calendar access levels.
- [Apple age rating values and definitions](https://developer.apple.com/help/app-store-connect/reference/app-information/age-ratings-values-and-definitions): unrestricted browsing, UGC and content descriptors.
