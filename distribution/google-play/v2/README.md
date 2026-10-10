# Google Play screenshot set v2

Historical design notes for the superseded v2 set. Its source, export, and review images were removed during cleanup. Paths below document the former layout; use `../v3/` for the current images.

## Final upload order

1. `output/01-live-matches.png` — live and scheduled matches
2. `output/02-map-stats.png` — complete KDA table for both teams
3. `output/03-event-detail.png` — completed match history and final scorelines
4. `output/04-rankings.png` — regional team rankings
5. `output/05-home-widget.png` — enlarged four-match home-screen widget

`review-contact-sheet.png` is a small side-by-side review artifact and is not intended for upload.

## Design direction

The set was built in Pen.dev/Pencil using the Monumental Editorial direction with the Violet Void palette:

- Playfair Display for large editorial headlines
- Geist Mono for labels, descriptors, and the footer
- flat violet-black `#1A1A2E` background
- white screenshot borders with no rounded marketing cards, gradients, or shadows
- concise sequence labels so the five images read as one campaign

Pencil frame IDs: `l4bnbx`, `JbetO`, `ucQ46`, `p245ix`, and `t4aMYM`.

## Crop decisions

- Map statistics uses `source/02-map-kda-complete.png`, a focused crop containing all ten player rows. The horizontally overflowing round-history strip is intentionally excluded.
- Event details uses `source/03-event-results.png`, which contains the Completed/Upcoming switch and exactly three complete result cards. The time-sensitive event overview and the next partial match card are intentionally excluded.
- The widget uses `source/05-widget-large-crop.png`, which includes the date and the complete four-match widget while excluding all launcher icons.

## Generated texture

`source/editorial-violet-texture.png` was generated as an optional quiet art-book paper texture. It was not applied to the final exports because the flat Pen.dev surface kept the small in-app text sharper and more legible.

Generation prompt: nearly black violet editorial print surface with extremely subtle tactile paper grain, monochrome `#1A1A2E`, no text, logos, shapes, gradients, light beams, objects, or watermark.
