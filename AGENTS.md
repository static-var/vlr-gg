# Platform scope

VLR supports iOS and Android only. Keep shared code in `commonMain` and platform implementations in `iosMain` and `androidMain`.

Do not add desktop application targets, desktop source sets, desktop implementations, or desktop test runners. Use the supported mobile targets for validation. JVM tooling required by Android, Gradle, and lint remains valid.

Use `build-brief ./gradlew ...` for Gradle commands. Follow the user's current platform constraints when choosing builds and tests.

After app changes, if an iOS simulator is already running, build, install, and launch the updated app there before finishing so the user can visually review it. Verify the launched screen with a screenshot.

# Release notes

When preparing a release, update both sets of release notes:

- In-app: edit `feature-about/src/commonMain/kotlin/dev/staticvar/vlr/featureabout/presentation/BundledRelease.kt`. Update the introduction and highlights, and assign a new unique `id` for the release. Keep that ID stable across builds and copy corrections for the same release; changing it makes the Home banner appear again. App version changes alone do not reset acknowledgement. Leave a highlight's `platform` unset for shared changes, or use `ReleasePlatform.Android` / `ReleasePlatform.Ios` for OS-specific changes.
- Play Store: update `distribution/whatsnew/whatsnew-en-US` with concise Android-facing changes for that release. Keep the entire file strictly below 500 characters, counting spaces and line breaks. Validate with `python3 -c 'from pathlib import Path; text = Path("distribution/whatsnew/whatsnew-en-US").read_text(); print(len(text)); assert len(text) < 500'`.

Keep both lists consistent with shipped changes. Verify that a new in-app release ID shows the Home banner, Dismiss/Done keeps it hidden after relaunch, and Settings → What's new? still opens the notes.
