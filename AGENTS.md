# Platform scope

VLR supports iOS and Android only. Keep shared code in `commonMain` and platform implementations in `iosMain` and `androidMain`.

Do not add desktop application targets, desktop source sets, desktop implementations, or desktop test runners. Use the supported mobile targets for validation. JVM tooling required by Android, Gradle, and lint remains valid.

Use `build-brief ./gradlew ...` for Gradle commands. Follow the user's current platform constraints when choosing builds and tests.

After app changes, if an iOS simulator is already running, build, install, and launch the updated app there before finishing so the user can visually review it. Verify the launched screen with a screenshot.
