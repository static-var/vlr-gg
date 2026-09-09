# VLR mobile monitoring

VLR uses Sentry Kotlin Multiplatform **0.27.0**, which pairs Android/Java **8.41.0** with Cocoa **8.58.2**. Monitoring starts from the Android and iOS entry points. Supported application targets are iOS and Android only. The old `app/` module and its historical Android Sentry project are separate from the current mobile app. See the [SDK release notes](https://github.com/getsentry/sentry-kotlin-multiplatform/blob/0.27.0/CHANGELOG.md).

## Supported features

| Feature | Android | iOS | VLR integration and limits |
| --- | --- | --- | --- |
| Unhandled errors and native crashes | Native SDK | Cocoa and Kotlin/Native integrations | Start before application work. On iOS, preserve the Sentry exception hook and disable unhandled C++ exception monitoring for Compose. |
| Handled errors | Shared API | Shared API | `AppTelemetry.captureException` reports refresh failures and explicit failures. Cancellation is not an application error. |
| Breadcrumbs and context | Shared and native | Shared and native | Stable screen names, application actions and sanitized network information help reconstruct failures. Never log authorization headers or search text. |
| Sessions and release health | Native SDK | Native SDK | Automatic session tracking provides crash-free session data. Release, distribution and environment identify the originating build. |
| Structured logs | Shared API | Shared API | The KMP SDK supports logs independently of error events. Application logs are explicit; this does not mean every Logcat or console line is uploaded. |
| Tracing and performance | Shared spans plus native integrations | Shared spans plus native integrations | Record navigation breadcrumbs and time network operations. Finish spans on success, failure and cancellation. Native instrumentation alone does not describe every Compose screen. |
| Session Replay | Native SDK | Native SDK | Compose Multiplatform support is experimental. Requested sampling is 1% of sessions and 10% of sessions with errors, with text/images masked. Cocoa 8.58.2 automatically disables Replay on the tested iOS environment because masking is unreliable; VLR preserves this SDK safeguard. |
| Profiling | Native capability | Native capability | Disabled: UI and continuous profiling require paid usage. Tracing still measures operation duration without profiling. |
| Attachments and screenshots | SDK capability | SDK capability | Consume attachment quota. Only attach intentional diagnostic data; full-screen screenshots can contain information omitted from structured events. |
| User feedback | Native `captureFeedback` API | Native `captureFeedback` API | About → Send feedback queues a standalone text report for the current User Feedback product, without identity fields or attachments. KMP 0.27.0 only exposes the older event-linked User Reports API, so shared UI calls the native adapters. The UI confirms queueing, not server delivery. |
| Application Metrics | Native Android API | No current KMP/iOS implementation | The Android native adapter emits `vlr.operation.count` and `vlr.operation.duration`. KMP 0.27.0 has no common metrics API; Cocoa 8.58.2 cannot use the current Application Metrics product. |
| Debug symbols | R8 mappings and native symbols | Archive dSYMs | Upload the exact release's artifacts explicitly. No upload authentication token belongs in an app binary. |

Sentry's [KMP documentation](https://docs.sentry.io/platforms/kotlin/guides/kotlin-multiplatform/), [Compose guidance](https://docs.sentry.io/platforms/kotlin/guides/compose-multiplatform/), [KMP logs announcement](https://sentry.io/changelog/send-logs-using-the-kotlin-multiplatform-sdk/), and [Android Application Metrics documentation](https://docs.sentry.io/platforms/android/metrics/) describe the SDK capabilities. Availability in the SDK and a validated VLR integration are separate checks.

## Free usage

As checked on 2026-09-08, the Developer plan includes monthly quotas of **5,000 errors**, **5 GB of logs**, **5 million spans**, **50 replays**, and **1 GB of attachments**. The pricing page also lists **5 GB of Application Metrics**, one uptime monitor, one cron monitor, ten custom dashboards, and email notifications. These quotas are shared across the organization, including older builds still sending events. Check the organization's current subscription for its actual allowances; historical and promotional plans can differ. [Sentry pricing](https://sentry.io/pricing/)

Profiling is excluded from this free-only integration. Sampling controls traffic but cannot guarantee a monthly quota: the number of installations and crashes varies. Keep paid overages disabled in the account and review usage before increasing trace or replay sampling. Uptime and cron monitoring operate on servers and scheduled jobs; they are not mobile SDK integrations.

## Configuration and credentials

Keep local configuration in ignored `local.properties`, `.env`, or `sentry.properties`. Use environment variables or the CI secret store for release builds. `SENTRY_DSN` identifies the ingestion project; platform-specific DSNs can keep Android and iOS separate. A DSN is shipped in the binary and must never be treated as an administrative credential.

Local runtime settings:

| Setting | Behavior |
| --- | --- |
| `SENTRY_DSN` | Shared DSN, now using the approved Nexus bridge in ignored local configuration. |
| `SENTRY_DSN_ANDROID`, `SENTRY_DSN_IOS` | Optional platform override; blank values fall back to `SENTRY_DSN`. |
| `SENTRY_ENABLED` | Defaults to `true`; `false` disables startup monitoring. An absent DSN also disables it. |
| `SENTRY_ENVIRONMENT` | Optional override; otherwise `development` for Debug and `production` for Release. |
| `SENTRY_ORG`, `SENTRY_PROJECT` | Build upload destination only. |

Runtime settings use nonblank environment values, then `local.properties`, then `.env`. Upload credentials use Sentry CLI environment variables or ignored `sentry.properties`. Runtime configuration never reads upload credentials. Store single-line property values without duplicate keys. Both platforms tag releases as `vlr@<version>+<build>` and distributions as `android-<build>` or `ios-<build>`.

Error sampling is 100%, trace sampling is 10%, and Replay requests 1% of sessions plus 10% of sessions with errors. Profiling is disabled. Screenshots are disabled; native view hierarchy attachments are enabled, although an iOS Compose canvas does not expose a complete native view tree.

Android bytecode instrumentation covers database and file I/O. Shared Ktor instrumentation provides sanitized endpoint templates without duplicate Android OkHttp instrumentation. Cocoa's native HTTP instrumentation strips sensitive standard headers but can include URL query and fragment attributes; KMP's common options cannot intercept those native transaction fields. Never put credentials in URLs. [Cocoa header sanitizer](https://github.com/getsentry/sentry-cocoa/blob/8.58.2/Sources/Swift/Core/Tools/HTTPHeaderSanitizer.swift), [HTTP span implementation](https://github.com/getsentry/sentry-cocoa/blob/8.58.2/Sources/Sentry/SentryNetworkTracker.m)

KMP 0.27.0 also cannot write sanitized exception values back to Cocoa events or expose their native request fields through common `beforeSend`. Explicit application messages, logs and breadcrumbs are sanitized, but iOS exception text can retain data supplied by the throwing library. Android additionally scrubs native exception values and request fields. Do not describe the iOS callback as comprehensive redaction; a future SDK upgrade must expose native callbacks alongside the common Kotlin/Native crash configuration before that gap can be closed.

`SENTRY_AUTH_TOKEN` is a privileged upload credential. It is used only by build tooling, never by `GeneratedBuildConfig.swift`, Android `BuildConfig`, resources or a telemetry bridge. Do not include generated configuration files in source bundles. The iOS upload script deliberately uploads debug symbols without `--include-sources`.

The [Sentry CLI configuration reference](https://docs.sentry.io/cli/configuration/) documents environment variables and `SENTRY_PROPERTIES`. An ignored properties file can contain `defaults.org`, `defaults.project`, and `auth.token`; point `defaults.project` at the current mobile project instead of the historical Android project. Environment variables can override the corresponding values.

## iOS release symbols

Use the `sentry-cli` build tool documented at [Sentry CLI installation](https://docs.sentry.io/cli/installation/), which is separate from the newer interactive `sentry` command. The helper below never runs automatically during an Xcode build.

If the executable is installed outside `PATH`, set `SENTRY_CLI` to its absolute path. It can be installed in a temporary tools directory without adding npm packages to this repository.

Archive the Release configuration with **Debug Information Format = DWARF with dSYM File**. Upload the dSYMs from the exact archive submitted to distribution. `shared` is linked statically, so its linked Kotlin/Native code belongs to the final application image and its application dSYM. A standalone intermediate framework dSYM does not replace the final application's matching symbols.

Validate the local files without contacting Sentry:

```sh
./scripts/sentry-upload-symbols.sh --check /absolute/path/VLR.xcarchive
```

After supplying `SENTRY_AUTH_TOKEN`, `SENTRY_ORG` and `SENTRY_PROJECT` through the environment, or configuring ignored `sentry.properties`, explicitly upload:

```sh
./scripts/sentry-upload-symbols.sh /absolute/path/VLR.xcarchive
```

The script also accepts an individual `.dSYM` bundle or a directory containing dSYMs. It waits for Sentry to process the files and propagates failures so a release operator can retry. Existing files are deduplicated by the CLI. Verify matching debug identifiers under the project's Debug Files settings before testing native crash symbolication. The [debug information CLI guide](https://docs.sentry.io/cli/dif/) documents validation, recursive discovery and upload processing.

## Delivery through a first-party server

A bridge on a controlled hostname can relay telemetry to Sentry when direct Sentry ingestion domains are blocked. The app must use that hostname for all enabled telemetry categories, and the bridge must support binary/compressed envelopes and forward Sentry's rate-limit responses. A reverse proxy with a fixed upstream avoids letting clients choose arbitrary destinations.

The bridge is deployed at `valorant-app.staticvar.dev` on Nexus. Its source and deployment details are in [infra/sentry-relay](../infra/sentry-relay/README.md). Use normal server egress, HTTPS, bounded request sizes and timeouts, and an allowlist of project IDs and client keys. Disable request-body and authorization logging. A bridge improves delivery only while its hostname is reachable; it cannot guarantee delivery against every DNS policy. Preserve any in-app telemetry preference regardless of transport.

## Release verification

### Integration verification — September 8, 2026

The `vlr-mobile` project received the following through the Nexus bridge from an iPhone 17 Pro simulator running iOS 26.5:

- Native and Kotlin handled errors (`VLR-MOBILE-4` and `VLR-MOBILE-3`).
- An uncaught Kotlin crash after relaunch (`VLR-MOBILE-5`), with `TelemetryVerification.kt:24` and `TelemetryVerification.swift:48` visible in the symbolicated stack.
- Structured native/KMP logs, a sampled diagnostic transaction and a real `GET /api/v1/news` span.
- Release/session health, navigation/network breadcrumbs and a view-hierarchy attachment.
- Native user feedback from About (`VLR-MOBILE-8`). Sentry classified the synthetic verification text as spam; it is visible in the feedback Spam mailbox. The app correctly reports queueing rather than guaranteed delivery or classification.

Android Debug and Release builds passed, including Release bytecode instrumentation. The final feedback adapter compiled for Android and iOS, and the final iOS simulator app built and ran. The earlier shared test run passed 60 tests, and five Swift configuration-generator tests passed. Android emulator delivery was subsequently verified as described below. Physical-device Replay masking, ANRs/app hangs and production archive symbols still require release-device verification.

Local `.env`, `local.properties`, `sentry.properties` and generated Swift configuration are ignored, untracked and owner-only. A credential-value scan found no matches in tracked or nonignored source files. Matching simulator dSYMs were uploaded without source files; production archives need their own matching upload.

### Android emulator verification — September 8, 2026

Tested the final Debug APK on `Medium_Phone_API_36.0` (Android 16). The existing installation initially crashed because its old database lacked `news.list_position`; Sentry received that real failure as `VLR-MOBILE-9`. At the owner's explicit direction, the emulator app was uninstalled and reinstalled. No database version, migration or database source code was changed.

The clean installation loaded News, Matches, match details, team details, player details, Events, Rankings, Settings and About. About → Send feedback accepted the test message and displayed **Feedback queued**. The app relaunched normally and loaded News after the deliberate crash test.

| Check | Observed result |
| --- | --- |
| Handled Kotlin/JVM errors | KMP `VLR-MOBILE-A` and native Android `VLR-MOBILE-B` received through the bridge. |
| Uncaught JVM crash | `VLR-MOBILE-C`, two test events including one after the clean install. The stack identifies `TelemetryVerificationActivity:46`. |
| Navigation and HTTP breadcrumbs | News, Matches, Events, Rankings, Settings and About; HTTP 200 results with sanitized route templates, including `/api/v1/player/{id}` and `/api/v1/team/{id}`. |
| Structured logs | Native Android and KMP verification logs plus initialization logs visible in Sentry. |
| Tracing | Forced diagnostic parent/child spans plus an actual `GET /api/v1/events` span (648.64 ms in this run). |
| Application Metrics | Both `vlr.operation.count` and `vlr.operation.duration` received with real network-operation samples. |
| Release/session health | Android release `vlr@1.0.0-debug+1`, distribution `android-1`, environment `development`; crashed sessions visible. |
| User feedback | `VLR-MOBILE-D` received. Sentry classified the synthetic diagnostic text as spam; it remains visible in the feedback Spam mailbox. |
| Attachments | View-hierarchy attachment present on the Android crash. |
| Session Replay | Two Android recordings arrived. Inspected replay `6678cb07477e4bfa9b3d37eda63dce12`; the captured feedback confirmation screen rendered with its text masked. This is a sampled screen check, not exhaustive masking validation. |

This run does not validate ANRs, NDK/C++ crashes, physical-device Replay rendering/masking, or release-only database/file spans at runtime. Replay sampling remains at its configured rates; production sampling was not increased for this test. The release build compiled the database/file instrumentation, but the debug emulator build deliberately excludes that instrumentation.

For each release, check Android and iOS separately: confirm a handled test error, a native crash after relaunch, session data, a navigation breadcrumb, a network span, and a structured log in the intended project and environment. Inspect captured event data for authorization headers, query text and generated secrets. Verify symbols against the final release artifact. Validate Compose replay rendering and masking on physical devices before enabling it. If a bridge is used, repeat delivery with direct Sentry DNS blocked and verify every enabled envelope category.

## Android release symbols

The active `androidApp` module applies the Sentry Android Gradle plugin. Uploads remain explicit:

```sh
build-brief ./gradlew :androidApp:assembleRelease -PsentryUpload=true
```

Provide the ignored Sentry upload configuration first. R8 mappings are generated/uploaded only when minification is enabled; the current app release build has minification disabled. Native symbols upload with the explicit flag. Source context uploads and dependency auto-installation are disabled, preserving the matching KMP/native SDK versions and excluding generated secrets. The legacy release workflows still target the retired `app` module; they are not a release pipeline for this KMP app.

The temporary diagnostic activities, crash triggers and synthetic event helpers used for the checks above were removed after verification. Shipped source contains no diagnostic crash entry point.
