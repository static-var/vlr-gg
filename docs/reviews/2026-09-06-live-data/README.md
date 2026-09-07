# Live API and mobile UI comparison

Run on September 6, 2026, approximately 23:25–23:38 IST (17:55–18:08 UTC), using the iPhone 17 Pro simulator and the current KMP app. The simulator's existing cache was preserved.

**The app does not consistently revalidate cached data while online.** This run confirmed stale news on launch, missing match history in a cached detail page, incorrect same-day news ordering, and a paused tournament labeled Upcoming.

## Method and scope

- Captured actual HTTP responses from the app's existing Darwin/Ktor client and compared them with UI accessibility snapshots and read-only SQLite exports. Requests retained the app's normal host, headers, and authentication.
- Compared 30 news entries, 86 match-list records, 75 events, and 263 ranking entries across 12 regions. Visually sampled all four main screens, event 3097, and match 734308.
- Normal navigation made the news, match-list, event-list, event-detail, and rankings requests. A separate diagnostic GET retrieved match 734308's current detail response **without persisting it**, because normal opening of that cached page did not make a request. This distinguishes server data from what the UI actually had available.
- Initial direct command-line requests returned Cloudflare HTTP 403, error 1010. They did not provide comparison payloads. The app's actual requests returned HTTP 200.
- Authentication was not written to the evidence. Temporary response diagnostics were removed, their source file restored exactly, and the normal iOS build installed and launched after the audit.
- This is a live sample of the current iOS build, not an exhaustive review of every detail page. Android shares these repositories/viewmodels but was not independently network-instrumented in this run. Reconnect and network-failure behavior were reviewed in code, not fault-injected.

## Confirmed discrepancies

### 1. Cached news is stale on launch

The cold-launch screen began with **“VARREL sent home as Global, T1 seal Champions Shanghai slots”**, dated September 5. No `/api/v1/news/` request occurred on launch.

At 17:56:13 UTC, tapping Refresh returned four September 6 stories ahead of that article, including **“Red Bull Home Ground arrives in LA for 2026 off-season”** and **“Global Esports pulls off miraculous reverse-sweep to lift first VCT title.”** These stories appeared after the manual refresh. A later relaunch again made no news request.

Cause: [NewsListViewModel.kt](/Users/staticvar/Projects/vlr-gg/feature-news/src/commonMain/kotlin/dev/staticvar/vlr/featurenews/presentation/list/NewsListViewModel.kt:44) refreshes only when the local list is empty.

Evidence: [cold-launch UI](ios-news-cold-launch.json), [cold-launch screenshot](news-cold-launch.jpg), [network responses](ios-network.json).

### 2. Same-day news order differs from the server

After refresh and scrolling fully to the top, all four new stories were present, but their order was reversed relative to the API:

| Position | API response | Mobile UI |
| --- | --- | --- |
| 1 | Red Bull Home Ground arrives in LA… (`752729`) | Global brushes aside T1… (`751992`) |
| 2 | Global Esports pulls off miraculous reverse-sweep… (`752452`) | LOUD continues miracle run… (`752130`) |
| 3 | LOUD continues miracle run… (`752130`) | Global Esports pulls off miraculous reverse-sweep… (`752452`) |
| 4 | Global brushes aside T1… (`751992`) | Red Bull Home Ground arrives in LA… (`752729`) |

All four API entries have the same timestamp, `2026-09-05T18:30:00Z`, which correctly displays as September 6 in IST. The issue is ordering, not timezone conversion.

Cause: [News.sq](/Users/staticvar/Projects/vlr-gg/local-source/src/commonMain/sqldelight/dev/staticvar/vlr/localsource/database/News.sq:50) sorts only by `date DESC`, losing the server's order for equal dates. Preserve a server-provided list position, or define a verified secondary ordering rule.

Evidence: [refreshed UI at the top](ios-news-after-refresh-top.json), [network responses](ios-network.json).

### 3. Cached live match details omit available match history

Match **734308**, **100 Thieves vs LOUD**, displayed LIVE and 0–0, matching the match-list response. Opening its detail page made **no detail request**. It showed streams but no head-to-head history.

The diagnostic detail response at 18:03:59 UTC returned **five previous encounters**, including matches `706379` (2–0) and `681333` (3–0). The app's cache contained **zero** previous encounters and six stream links.

Cause: [MatchDetailsViewModel.kt](/Users/staticvar/Projects/vlr-gg/feature-matches/src/commonMain/kotlin/dev/staticvar/vlr/featurematches/presentation/MatchDetailsViewModel.kt:117) refreshes a cached match only when map data, history, streams, and VODs are all empty. Having stream links prevents revalidation even when other details are missing or outdated. A refreshed list can update the score/status without updating these detail sections.

Evidence: [displayed match details](ios-live-match-detail-734308.json), [empty cached history](ios-cache-match_previous_encounters-734308.json), [cached streams](ios-cache-match_videos-734308.json), [network responses](ios-network.json).

### 4. A paused tournament is labeled Upcoming

Event **2634**, **Momentum Gaming: Detonation Series**, returns `status: "paused"`. The app stores it as `UPCOMING`, places it in Upcoming, and displays an UPCOMING tag.

Cause: the remote [EventStatus enum](/Users/staticvar/Projects/vlr-gg/remote-source/src/commonMain/kotlin/dev/staticvar/vlr/remotesource/common/ApiEnums.kt:42) does not represent `paused`; the [event mapper](/Users/staticvar/Projects/vlr-gg/data/src/commonMain/kotlin/dev/staticvar/vlr/data/mapper/EventTeamMappers.kt:42) converts the resulting null to Upcoming. An unsupported status should not silently become a scheduled event.

Evidence: [UI snapshot](ios-paused-event-shown-upcoming.json), [screenshot](paused-event-upcoming.jpg), [event cache](ios-cache-events.json), [network responses](ios-network.json).

## Additional freshness gaps

- **Cached rankings are not revalidated.** The first opening fetched the empty cache successfully; reopening Rankings after relaunch made no rankings request. [RankingsViewModel.kt](/Users/staticvar/Projects/vlr-gg/feature-rankings/src/commonMain/kotlin/dev/staticvar/vlr/featurerankings/presentation/RankingsViewModel.kt:45) checks only whether the cache is empty. No changed ranking values were observed during this short run.
- **Returning to the foreground did not refresh.** Backgrounding and foregrounding the same process (`34773`) on Rankings produced no new request. Source review found no shared foreground/reconnect revalidation or live-match polling. The code therefore provides no guarantee that a visible live score will update while the screen remains open. A changing live score was not observed during this run.
- **Other populated detail pages have similar cache gates.** Event, article, team, and player detail viewmodels skip refresh for already populated records. These are code-confirmed risks; a value mismatch was not established for each page in this run.
- **Most screens lack a user refresh path.** News list/article expose refresh, but navigation does not connect the refresh methods for matches, events, rankings, team, or player screens. See [AppNavigationModule.kt](/Users/staticvar/Projects/vlr-gg/shared/src/commonMain/kotlin/dev/staticvar/vlr/shared/navigation/AppNavigationModule.kt:96).
- **Cached content can conceal refresh failures.** Several list routes show the error state only when their list is empty. This was found in source, not reproduced through a forced outage.

## Presentation difference to review

Match 734308's API and cache both contain `map_count: 5`, but the hero displays `–` for Maps. [MatchDetailFormatting.kt](/Users/staticvar/Projects/vlr-gg/shared-ui/src/commonMain/kotlin/dev/staticvar/vlr/sharedui/component/match/detail/MatchDetailFormatting.kt:75) deliberately suppresses the count when map stats are absent. This is a display rule, not a stale value. If the intended field represents series length, it can show that independently of whether per-map stats have arrived.

## Checks that matched

- All **86 match-list IDs** and their teams, scores, statuses, event names/IDs, series, and timestamps matched the captured response after list refresh. The visible live match showed the correct 0–0 score.
- All **263 ranking rows** matched the response's team IDs, names, countries, logos, positions, and points. Visible Asia-Pacific examples included Global Esports at 2000 points and Paper Rex at 1972.
- All **75 event IDs** matched. Apart from the paused-status mapping, the compared fields matched or were enriched by a later detail response.
- Newly opened **R2 New Dawn 2026** (`3097`) fetched its details. The displayed 16 teams, `750 AUD~ $539` prize, participants, and sampled scheduled match agreed with that response. Its detail date/prize formatting differs from the list endpoint, but represents the same information.
- Sample upcoming match times correctly converted UTC to IST, such as `2026-09-08T21:00:00Z` displaying September 9 at 02:30.

## Recommended next work

1. Show cached content immediately, then revalidate on screen entry, foreground return, and connectivity restoration. Keep list and detail freshness separate; a list update must not imply that all detail fields were refreshed.
2. Refresh visible live matches periodically at a server-supported interval, with request deduplication and cancellation when the screen is no longer active.
3. Expose refresh/retry consistently and show when cached content remains after a failed refresh.
4. Preserve news ordering and handle paused/unknown event statuses explicitly.

No production behavior was changed as part of this audit. The evidence files contain the captured responses and snapshots used above.
