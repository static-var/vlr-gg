# Compose iOS accessibility patch

This directory preserves a diagnostic experiment: source patches and a local Maven repository for Compose UI `1.12.0-vlr-a11y1`. The application no longer selects these artifacts or registers this repository. It uses official Compose dependencies. Results and the failed official 1.10.3 rollback are tracked in [the crash investigation](../../docs/reviews/2026-09-11-ios-accessibility-crash.md).

## Provenance

- Upstream: [JetBrains/compose-multiplatform-core](https://github.com/JetBrains/compose-multiplatform-core).
- Stable base: tag `v1.12.0`, commit `f29d2f99f3beafc60992c6a73dbbaa9f841c5d4c`.
- Upstream fix: [PR #3385](https://github.com/JetBrains/compose-multiplatform-core/pull/3385), commit `38e18f4ad01821fe95c6d02571e751611cb21608`. Its stable-branch backport is `7bb8148c16345fbf40f00356ac4affc92b36f6ce`.
- Reported upstream issue: [CMP-10615](https://youtrack.jetbrains.com/issue/CMP-10615), accessibility elements receiving messages after their Kotlin peers are freed.
- License: Apache 2.0, preserved in [LICENSE.txt](LICENSE.txt) and upstream source headers.

[accessibility.patch](accessibility.patch) backports the Objective-C disposal flag, adapts the Objective-C source directory name to the stable release, and adds local lifecycle corrections. Inactivity cleanup disposes each element before releasing the tree. Native geometry and focus callbacks check disposal before accessing Kotlin state, including coordinate-conversion targets and ancestors. Accessibility remains enabled.

[build.patch](build.patch) assigns the custom version only to `ui` and `ui-uikit`. Their unchanged Compose dependencies retain `1.12.0`. It also sets the Objective-C archive minimum to iOS 15 because Xcode 27 rejects the upstream iOS 13 setting. This is below the application's iOS 16 minimum.

## Verify the bundled repository

From the project root:

```bash
(cd third-party/compose/repository && shasum -a 256 -c ../SHA256SUMS)
```

[SHA256SUMS](SHA256SUMS) covers all bundled repository files. There are six Maven modules: `ui`, `ui-iosarm64`, `ui-iossimulatorarm64`, `ui-uikit`, `ui-uikit-iosarm64`, and `ui-uikit-iossimulatorarm64`. Native binaries target iOS devices and Apple Silicon simulators.

## Rebuild

Requirements: macOS, Xcode with iOS SDKs, JDK 21, an Android SDK for upstream Gradle configuration, Git, and `build-brief` on `PATH`. Set `JAVA_HOME` to JDK 21 and `ANDROID_SDK_ROOT` to the SDK directory. The bundled build used Xcode 27 beta 2 and upstream Kotlin 2.3.20.

Run from the project root. These commands use a new temporary directory and preserve the bundled repository:

```bash
patch_dir="$PWD/third-party/compose"
work_dir="$(mktemp -d /tmp/vlr-compose-rebuild.XXXXXX)"
git clone --depth 1 --branch v1.12.0 \
  https://github.com/JetBrains/compose-multiplatform-core.git "$work_dir/source"
test "$(git -C "$work_dir/source" rev-parse HEAD)" = \
  f29d2f99f3beafc60992c6a73dbbaa9f841c5d4c
git -C "$work_dir/source" apply \
  "$patch_dir/accessibility.patch" "$patch_dir/build.patch"
bash "$patch_dir/rebuild.sh" "$work_dir/source" "$work_dir/repository"
```

[rebuild.sh](rebuild.sh) publishes only the root metadata and two iOS variants for each patched module. It disables configuration cache and configuration-on-demand because upstream cross-project task realization otherwise fails. It uses an explicit output repository and does not replace artifacts in `~/.m2`.

The bundled checksums verify the committed artifacts; a new build may differ because archive timestamps and toolchains can change. Revalidate the application and regenerate the checksum manifest when replacing the bundled repository.
