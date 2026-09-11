# iOS accessibility crash, September 11, 2026

Status: unresolved on official releases tested. The application uses official
Compose 1.12.0. The user prefers official releases, including alpha/beta releases,
over local libraries or development snapshots. The local patch is disconnected
from dependency resolution; its successful experiment remains documented below.

## Reproduction

On the iPhone 17 Pro simulator running iOS 26.5, open Home, select the ongoing
Game Changers North America event, then go back. Query the accessibility tree
immediately after each navigation and repeat. XcodeBuildMCP `snapshot_ui` and
`tap` reproduce the crash without changing saved data.

## Results

| Compose | Native GC | Result |
| --- | --- | --- |
| 1.12.0 | Default CMS | Crashed within one or two navigation transitions, reproduced twice. |
| 1.12.0 | PMCS | Crashed on the first transition. Experiment reverted. |
| 1.13.0-alpha01+dev4780 | Default CMS | Crashed after seven transitions. Experiment reverted. |
| 1.12.0 with UI/UIKit 1.12.0-vlr-a11y1 | Default CMS | Passed 30 navigation transitions with three seconds of idle time before each accessibility query, followed by carousel, team/player, and main-tab navigation. |
| Official 1.10.3, Coil 3.4.0, Lifecycle 2.10.0 | Default CMS | Crashed after five navigation transitions, before the sixth query could locate the event screen. Rollback experiment reverted. |

Stable stack: `AccessibilityElement.get-disposed → isAlive → contentOffset → bounds → accessibilityFrame`,
with `EXC_BAD_ACCESS` at address `0x30`.

Development stack: `AccessibilityElement.get-node → isAlive → accessibilityIdentifier`,
with `EXC_BAD_ACCESS` at address `0x8`.

## Framework finding

[CMP-10615](https://youtrack.jetbrains.com/issue/CMP-10615) tracks queries arriving
after an accessibility element's Kotlin peer is freed.
[Upstream PR 3385](https://github.com/JetBrains/compose-multiplatform-core/pull/3385)
moves the disposal flag into Objective-C. The numbered development build contains
that change, but it still fails the reproduction. Upstream comments independently
report the same post-fix stack, and the issue was reopened.

The additional lifecycle gap was in `scheduleAccessibilityDisablingAndCleanup`, which
calls `cleanUp()` after two seconds; `cleanUp()` clears the accessibility element
map without calling each element's `dispose()`. Full mediator disposal calls the
element disposal loop separately. The local patch moves that loop into the shared
cleanup path and guards native geometry and focus callbacks, including coordinate
conversion targets and ancestors, before they access Kotlin state. It also includes
the upstream Objective-C disposal flag. This combination passes the reproduction;
the individual additions have not been tested separately.

## Official 1.10.3 rollback experiment

Home and navigation code were unchanged. Selecting Compose 1.10.3 alone was
insufficient: Coil 3.6.2 required Foundation 1.12.0, and Lifecycle 2.11.0 required
newer Runtime modules. Restoring Coil 3.4.0 and Lifecycle 2.10.0 allowed both UI and
Runtime to resolve to official 1.10.3. No local repository or forced patched
dependency was configured. Navigation3 1.1.1 and Kotlin 2.4.20 were unchanged.

Android compilation and the iOS simulator build passed. The delayed Home/event/back
replay completed five taps, then returned the simulator home screen. Crash report
`/Users/staticvar/Library/Logs/DiagnosticReports/VLR-2026-09-11-080701.ips` matches
the tested process, PID 6303. It records `EXC_BAD_ACCESS` at `0x30` with
`AccessibilityElement.get-disposed → isAlive → isAccessibilityElement`, followed
by UIKit accessibility automation callbacks. This is the same lifetime failure
through a different accessibility callback. The rollback is not a verified mitigation.

The failed downgrade was reverted to official Compose 1.12.0, Coil 3.6.2, and
Lifecycle 2.11.0. The local patch remains disabled.

Rollback build log: `/Users/staticvar/Library/Developer/XcodeBuildMCP/workspaces/vlr-gg-057cd923038a/logs/build_run_sim_2026-09-11T02-32-46-585Z_pid210_6d4ae301.log`.

Resolved UI log: `/private/var/folders/mw/3f8bhfy50bs2mqqrt897bqc40000gn/T/build-brief/build-brief-22ca444b-1023994274.log`.

Android/runtime log: `/private/var/folders/mw/3f8bhfy50bs2mqqrt897bqc40000gn/T/build-brief/build-brief-22ca444b-1975366370.log`.

## Input and accessibility comparison

All three runs used the same installed official 1.12.0 binary on the iPhone 17 Pro
simulator, iOS 26.5. Each started from a fresh app process and used the unchanged
Home → Game Changers North America event → Back flow. No library or application
code changed between runs.

| Input and inspection | Process | Result |
| --- | --- | --- |
| Raw HID touches; framebuffer screenshots; no tool-issued accessibility queries | 10579 | 30 transitions passed, same process alive throughout. |
| Same raw HID touch coordinates; accessibility snapshot before each touch | 11264 | 30 transitions passed, including the final Home snapshot. |
| Original element-based XcodeBuildMCP `snapshot_ui` and `tap`, including tap's immediate accessibility capture | 12374 | Crashed during the third tap; its capture returned the simulator home screen. |

The first two runs tapped the event at `(200, 525)` and Back at `(40, 94)` in
portrait simulator points. They waited at least three seconds between actions.
In the control, the first destination was inspected visually. Every later frame's
header matched the corresponding Home or event reference crop exactly, and the
same process was checked before and after each action. Frames and result records
are in `/tmp/vlr-normal-run/`; the first event reference is `/tmp/vlr-normal-detail.png`.

Input implementation matters. Ordinary AXe coordinate `tap` still queries
accessibility to infer orientation. That calibration was discarded and the app
restarted before testing. The control instead sent `down`, `delay`, and `up`
primitives through AXe's low-level HID broker, which bypasses the orientation and
accessibility lookup. Screenshots used the simulator framebuffer. The second run
added `snapshot_ui` before those same raw touches. The third used the original
element-based tool and therefore also changed element resolution, touch targeting,
and query timing. This comparison does not isolate those differences individually.

The failing process matches
`/Users/staticvar/Library/Logs/DiagnosticReports/VLR-2026-09-11-083535.ips`.
The report records `EXC_BAD_ACCESS` at `0x30`, with
`AccessibilityElement.get-disposed → isAlive → isAccessibilityElement` and UIKit
accessibility attribute-query callbacks.

Ordinary touch simulation did not reproduce the failure in this bounded run, and
a single pre-touch accessibility snapshot was insufficient to trigger it. The
original, more intensive automation remained a reproducer. Query timing/frequency
and element resolution are candidates for the difference, not separately proven
causes. This does not establish that physical-device use or VoiceOver is safe, and
it does not fix the underlying lifetime defect. Accessibility remains enabled.

## Local patch experiment validation

- Built and launched on iPhone 17 Pro, iOS 26.5, using Xcode 27 beta 2.
- Completed 30 Home/event/back transitions with a three-second idle interval before
  each query, exercising the two-second cleanup path. Accessibility targets remained
  discoverable and actionable throughout; the app stayed running.
- Scrolled the event participants carousel to team eight and back to team one.
  Screenshots confirm clipping occurs at the screen edges, with content padding at
  both ends. Opened FlyQuest RED from the carousel and edith from its roster, then
  scrolled the player's agent statistics and returned to Home.
- Opened News, Matches, Events, Rankings, and Home on the patched build.
- `:shared:compileAndroidMain -PfastIos=true` passed. Dependency insight confirms
  both patched iOS modules resolve together; untouched Compose modules use 1.12.0.
- All 38 local repository files match the bundled SHA-256 manifest. Patch and
  dependency integration received a separate source review; `git diff --check` passed.

Accessibility remains enabled. No runtime interception or GC workaround remains.
The patch is local, not an official Compose release. Physical-device runtime
validation has not been performed. See [source patches and rebuild instructions](../../third-party/compose/README.md).

Build log: `/Users/staticvar/Library/Developer/XcodeBuildMCP/workspaces/vlr-gg-057cd923038a/logs/build_run_sim_2026-09-10T19-57-54-433Z_pid36053_0ff5e5a2.log`.

Android/dependency log: `/private/var/folders/mw/3f8bhfy50bs2mqqrt897bqc40000gn/T/build-brief/build-brief-22ca444b-3902356697.log`.
