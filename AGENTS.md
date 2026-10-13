# Platform scope

VLR supports iOS and Android only. Keep shared code in `commonMain` and platform implementations in `iosMain` and `androidMain`.

Do not add desktop application targets, desktop source sets, desktop implementations, or desktop test runners. Use the supported mobile targets for validation. JVM tooling required by Android, Gradle, and lint remains valid.

Use `build-brief ./gradlew ...` for Gradle commands. Follow the user's current platform constraints when choosing builds and tests.

After app changes, if an iOS simulator is already running, build, install, and launch the updated app there before finishing so the user can visually review it. Verify the launched screen with a screenshot.

# Release notes

Before preparing, uploading, submitting, or publishing a release, read and follow [docs/release-checklist.md](docs/release-checklist.md). It includes pending App Store language setup and recurring Android and iOS checks.

Android and iOS ship the same version and build number from `version.xcconfig`. Xcode Cloud replaces the iOS build number with its own counter, so before an iOS release set `CURRENT_PROJECT_VERSION` to Xcode Cloud's next build number, or raise that number to match. `iosApp/ci_scripts/ci_post_clone.sh` stops the "iOS TestFlight Release" workflow when they differ. Build both platforms from the same commit.

When preparing a release, update both sets of release notes:

- In-app: edit `feature-about/src/commonMain/kotlin/dev/staticvar/vlr/featureabout/presentation/BundledRelease.kt`. Update the introduction and highlights, and assign a new unique `id` for the release. Keep that ID stable across builds and copy corrections for the same release; changing it makes the Home banner appear again. App version changes alone do not reset acknowledgement. Leave a highlight's `platform` unset for shared changes, or use `ReleasePlatform.Android` / `ReleasePlatform.Ios` for OS-specific changes.
- Play Store: update `distribution/whatsnew/whatsnew-en-US` with concise Android-facing changes for that release. Keep the entire file strictly below 500 characters, counting spaces and line breaks. Validate with `python3 -c 'from pathlib import Path; text = Path("distribution/whatsnew/whatsnew-en-US").read_text(); print(len(text)); assert len(text) < 500'`.

Keep both lists consistent with shipped changes. Verify that a new in-app release ID shows the Home banner, Dismiss/Done keeps it hidden after relaunch, and Settings → What's new? still opens the notes.

# Icon assets

Use SVG masters and lossless WebP raster icons. Use native vector XML for Android drawables and SVG layers for iOS Icon Composer. Keep another image format only when the platform requires it, and explain that exception to the user. The legacy iOS AppIcon.appiconset requires PNG exports.

Keep only shipping icon designs in art/app-icon. Store rejected concepts and mockups outside the repository. Regenerate settings previews with art/app-icon/generate-icon-previews.py.
