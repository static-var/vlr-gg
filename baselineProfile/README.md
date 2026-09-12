# Android Baseline Profiles

This module records the Android app, including the KMP code it executes. It targets
`:androidApp`, whose application ID is `dev.staticvar.vlr`.

Connect an API 33+ Android device or start an emulator, then run from the repository root:

```sh
build-brief ./gradlew :androidApp:generateReleaseBaselineProfile
```

The startup journey records launch into Home and also produces a Startup Profile.
Four independently collected CUJs cover match filters and completed match details;
event filters and sections; rankings, team rosters, player details and favorites on
Home; and news articles, Settings and About. Lists and details are scrolled in both directions.
Existing favorites are preserved; favorites added by a completed journey are removed
by that journey. Run on a dedicated test device with the language set to English
and working backend access. Completed matches/events, a ranked team with a roster,
and a published article must be available. Missing required content fails the run
instead of silently omitting that journey.

Before each generator, `ProfileSettings.kt` uses Settings to select **Lynx · Cat**
and the highest **Surprise visits** setting, **YES**. YES is the app's current 40%
chance per eligible screen, not a forced appearance on every screen. Setup also
turns off spoiler hiding so celebrations are allowed. It restarts the app and checks
that the mascot selection and slider maximum persisted before collecting profiles.
This setup happens outside collection, including for the Startup Profile, and leaves
these preferences enabled on the test device. Production defaults are unchanged.

To validate the journeys with one iteration before full collection:

```sh
build-brief ./gradlew :baselineProfile:connectedNonMinifiedReleaseAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.dryRunMode.enable=true
```

Dry runs validate interactions but do not replace the saved production profiles.

Generated files belong in
`androidApp/src/release/generated/baselineProfiles/`. Commit both `baseline-prof.txt`
and `startup-prof.txt` after regenerating them for changes to startup or navigation.
The old profile under `app/` belongs to the retired Android application and is not
used by the KMP app.

Release builds consume the saved profiles without starting a device:

```sh
build-brief ./gradlew :androidApp:assembleRelease :androidApp:bundleRelease
```

The APK contains `assets/dexopt/baseline.prof` and `baseline.profm`. The AAB carries
the profile under `BUNDLE-METADATA/com.android.tools.build.profiles/`. ProfileInstaller
is included in the app for installation on supported Android versions. The generator
and its test dependencies are separate test APKs and are not shipped in the app.

Profile generation verifies the journeys and captures code coverage. Measure startup
and frame-time improvements separately with Macrobenchmark on a physical device.

## Validation

The profiles were regenerated on the API 36 emulator on September 12, 2026. All five
generator tests passed. The baseline contains 39,179 rules, including 8,255 app rules
and entries for CardMascot, Lynx, and Rosie rendering. `assembleRelease` and
`bundleRelease` passed, and both artifacts contained a 27,358-byte `baseline.prof`
and a 1,100-byte `baseline.profm`.
D8 reported two unmatched synthetic startup methods in `ComponentActivity` and
`DataModuleKt`; those entries were not applied. No performance delta was measured.

See Android's [Baseline Profile documentation](https://developer.android.com/topic/performance/baselineprofiles/create-baselineprofile)
and [packaging configuration](https://developer.android.com/topic/performance/baselineprofiles/configure-baselineprofiles).
