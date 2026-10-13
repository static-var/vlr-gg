# App Store assets and search review

Reviewed October 8, 2026 in Helium. Target storefront is India.

## Generated assets

Upload only these two files from `output/` as Creative Assets in Asset Library:

- `val-esports-header-3840x1646.png`
- `val-esports-search-3840x2560.png`

The SVG masters use the app's bundled Chakra Petch for headings and Space Grotesk for supporting text, with outlined text and embedded original app screenshots. Exports are RGB PNGs with no alpha channel. PNG is required for the header by Apple's specification. Review images and `validation.json` are local checks, not upload assets.

The renderer uses the shipping Match point icon and refreshed October iOS capture. It validates essential content against the Art Safe Area layers in Apple's official PSD templates. Header safe area is x1097–2743, y493–1154; search safe area is x836–3004, y765–1795. Decorative UI outside the header safe area may crop. These local crop checks do not replace App Store Connect's device preview.

Run `python3 distribution/appstore/creative-2026-10/render.py` to regenerate. Requires Pillow, fontTools and rsvg-convert, which are available on this machine.

These optional creatives are shown on iOS/iPadOS 27 and later. Existing screenshots still matter on earlier systems. After approval, select each in the live version's Header/Search Results placement and preview before publishing. A new binary is not required to replace approved creative assets on an already-live version.

Uploaded both PNGs through Helium on October 8, 2026 at 9:21 AM IST and submitted them together for standalone review. App Store Connect confirms Waiting for Review for both. Header asset ID: `47c00019-609c-8736-802f-5f1f412860bc`; search asset ID: `fa800019-609c-8736-8034-ab63ec2f2750`. Store placements have not been published; asset approval is pending.

Sources: [Apple specifications](https://developer.apple.com/help/app-store-connect/reference/app-information/creative-assets-specifications), [asset workflow](https://developer.apple.com/help/app-store-connect/manage-app-information/manage-your-app-store-assets), [best practices and templates](https://developer.apple.com/app-store/asset-best-practices/).

## Animated header

`output/val-esports-header-motion-3840x1646.mp4` is an optional video alternative to the static header. Header video spec: 21:9 at 3840×1646, 30 or 60 fps, 5–30 s, `.mov`/`.m4v`/`.mp4`. It is muted by default and loops continuously.

- **Scene:** An editorial colorblock look in four scenes: ink (scores), pink (schedules), ivory (rankings) and violet (stats). Each scene has a giant outlined backdrop word, two angled ticker tapes, halftone edges and twinkling sparkles. The icon and wordmark sit at the center, with "VALORANT" in a pill and the current word beside it. The wordmark is holographic on the ink scene. Foil-tinted match cards from the real iOS captures float on both sides. The crops are text-only, with no team or league logos.
- **Rhythm:** Every 5 s a diagonal wipe, led by two accent bands and a white hairline, brings in the next scene. The wipe changes direction each time. Type re-colors exactly where the edge passes, and the hero gets a brief chromatic split. Then the icon does a coin flip, the new word's letters spring up, and the cards flip to new matches. There are no hard cuts, and nothing flashes faster than one transition per 5 s.
- **Loop:** The 20 s loop is seamless. Every motion is periodic in the loop, and `capture.mjs` fails if the frames at 0 s and 20 s differ.
- **Safe area:** Icon, wordmark, all four tagline variants, and the progress bar sit inside the header art safe area. The cards stay outside it across their whole float path and may crop. `capture.mjs` checks both.
- **Poster frame:** Frame 0 is the settled ink "scores" scene, saved as `output/val-esports-header-motion-poster-3840x1646.png`. Select 0 s as the poster frame, because poster frames cannot be changed after approval.
- **Encoding:** H.264 High 5.1, BT.709, yuv420p, with a silent stereo AAC track. The track avoids upload checks that require audio.
- **Content:** No pricing, URLs, ©, other platforms, Apple award references, or gameplay.

To regenerate, run `node distribution/appstore/creative-2026-10/header-motion/capture.mjs`. It requires Google Chrome, ffmpeg and ffprobe; no npm packages. Add `--stills 0,2.3,9` to write review PNGs instead. Uploaded through Helium on October 13, 2026 at 12:43 AM IST. Apple processed it as a 21:9 Product Page Header and it was submitted for standalone review; App Store Connect shows Waiting for Review. Asset ID: `87c00019-609c-8736-810c-620c355870ea`. Apple approved it on October 13, 2026. At 10:48–10:52 AM IST it was published as the header on the default product page (iOS App Version 1.1.0, English (U.S.)) and on all four custom product pages: Live scores and match schedules, India match day, Brazil favorites and spoiler controls, and Team rankings and match stats. The default page's static `val-esports-header-v2-3840x1646.png` header was removed to free the single header slot; it remains approved in Asset Library. Neither the picker nor the asset page offered a poster-frame control, so App Store Connect shows its own default thumbnail.

## What the current listing already does well

Authenticated App Store Connect shows version 1.0.7/build 86, Ready for Distribution:

- Name: `Val Esports`
- Subtitle: `Live scores, matches & events`
- Keywords: `valorant,vct,schedule,results,rankings,teams,players,spoilers,widgets,competitive`
- Primary category: Sports; secondary: News.
- English (U.S.) localization; promotional text is blank.
- Approved screenshots for iPhone and iPad, plus event card/detail artwork.

The exact brand is already present in the name. Adding the same words to keywords wastes space and is unlikely to resolve the ranking issue. Apple lists text relevance and user behavior, including downloads, ratings and reviews, among search ranking factors. Exact-name matching does not guarantee first position. Apple does not publish factor weights or a way to force organic first place. [Apple search guidance](https://developer.apple.com/app-store/search/)

## Observed acquisition signal

The All sources, All territories analytics report for July 9–October 6, 2026 shows 2 first-time downloads attributed to App Store Search and 1 to App Referrer. Several other totals are dashes. They should not be interpreted as measured zero. This is not an India-specific report and does not expose downloads or ranking for the individual query `val esports`.

Low acquisition momentum is a plausible contributor to poor ranking, based on Apple's published factors. It is an inference, not a verified diagnosis of the reported 20+ position. The user's India search result was not independently reproduced.

Ratings and Reviews shows No Ratings and no reviews across all countries. The India filter also shows No Ratings and no reviews. This leaves another published ranking signal undeveloped.

## Recommended sequence

1. Publish the approved header/search creatives, then compare India search impressions and first-time downloads over consistent periods. Keep the title and subtitle initially so the creative change is easier to assess.
2. For the next version, add `tournaments,stats` to the existing keyword field. The draft has 99 characters. Keep `Val Esports` and the existing 29-character subtitle. These additions broaden relevant queries; they are not a fix for the exact-brand rank. Check third-party term eligibility during App Review as usual.
3. Fill the blank promotional text with the 161-character draft. It explains the app but does not affect search ranking. [Apple product page guidance](https://developer.apple.com/app-store/product-page/)
4. Lead screenshots with schedules/scores, followed by favorites and spoiler protection. Test a single first-screenshot or search-creative change once traffic is sufficient. With only a few acquisitions, a product page experiment may not reach a useful result. [Product page optimization analytics](https://developer.apple.com/help/app-store-connect-analytics/acquisition/product-page-optimization/)
5. Grow real usage through relevant VALORANT communities and direct App Store links. Add a native review request after users have had time to use favorites and match details, if the app does not already have one. Do not interrupt the first launch or ask only people who report being happy. [Apple review request guidance](https://developer.apple.com/documentation/storekit/requesting-app-store-reviews)
6. If immediate visibility is needed, consider an Apple Ads search campaign restricted to India with exact-match `val esports`, a user-approved spend cap and measured acquisition cost. Paid placements are separate from organic rank; eligibility and auction results still apply. No campaign or spending was started. [Apple Ads placements](https://ads.apple.com/app-store/help/ad-placements/0081-ad-placement-options)

Save a repeatable India baseline for `val esports`, `valorant scores`, `vct scores` and `valorant esports`, using the same storefront and device conditions. Record organic position separately from ads and compare weekly. Track search impressions, downloads and conversion by territory in App Store Connect. An apparent movement on one device is not enough to establish a sustained ranking improvement.

## Remaining scope

Both creatives have been uploaded and approved. The header video is published on the default and all custom product pages; check the search-results placement separately. The metadata draft has not been applied. App review-prompt implementation and paid campaigns were not performed.
