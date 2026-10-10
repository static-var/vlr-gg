# Store screenshot renderer

This renderer turns twenty real app captures into five screenshots each for Android phones, Android tablets, iPhone, and iPad for Valorant Esports. It also renders a Google Play feature graphic, a social card, contact sheets, and a JSON validation report.

The artwork uses the rebrand palette: ivory, violet, and ink. A shared editorial layout keeps the series coherent while the court, target, ticket, bracket, and rank line motifs give each slide its own visual cue. The renderer scales captures proportionally and shows every captured pixel inside a simple frame. It does not redraw or fabricate the app interface.

## Inputs

Put the final captures at these paths:

```text
distribution/rebrand-2026/raw/android/01.png ... 05.png
distribution/rebrand-2026/raw/ios/01.png ... 05.png
```

The Android phone and iPhone inputs must be portrait PNGs. They may have different pixel dimensions. The renderer fits each image by aspect ratio and does not crop or stretch it.

Google Play tablet artwork uses genuine landscape adaptive UI captures. Put at least four, and preferably the complete five-slide sequence, at `distribution/rebrand-2026/raw/android-tablet/01.png ... 05.png`. Render the optional 1920 x 1080 set with:

```sh
python3 distribution/rebrand-2026/template/render.py --include-play-tablet
```

The renderer requires all five files when this option is selected. It does not enlarge phone captures into tablet artwork.

The app supports iPad, so App Store Connect also needs genuine tablet captures. Put those at `distribution/rebrand-2026/raw/ios-ipad/01.png ... 05.png`, then render the 2064 x 2752 set with:

```sh
python3 distribution/rebrand-2026/template/render.py --include-ipad
```

The iPad set is opt-in so missing tablet captures do not block the phone exports. The renderer has no iPhone fallback for iPad. It will stop if any requested tablet capture is absent.

Edit `manifest.json` to change headlines, kickers, colors, fonts, input paths, or output dimensions. Keep the final store copy in that file so one rerun updates both platforms.

## Final render

Run from the repository root:

```sh
python3 distribution/rebrand-2026/template/render.py
```

The command stops with a list of any missing final capture. Successful output goes to `distribution/rebrand-2026/output/final/`.

Render a completed platform without waiting for the other store. Repeat `--target` when more than one target is ready. For Android phones and the two wide assets:

```sh
python3 distribution/rebrand-2026/template/render.py \
  --target play \
  --target wide
```

Available targets are `play`, `app_store`, `play_tablet`, `app_store_ipad`, and `wide`.

## Labeled template preview

The manifest includes older development captures for layout review. Render those into a separate directory with:

```sh
python3 distribution/rebrand-2026/template/render.py \
  --preview \
  --output distribution/rebrand-2026/output/template-preview
```

Each preview portrait has a violet footer that says the capture must be replaced before publishing. The final command never falls back to these older files.

## Output inventory

```text
output/final/
  play-1080x1920/01.png ... 05.png
  app-store-iphone-1320x2868/01.png ... 05.png
  play-tablet-1920x1080/01.png ... 05.png       # with --include-play-tablet
  app-store-ipad-2064x2752/01.png ... 05.png  # with --include-ipad
  feature-graphic-1024x500.png
  social-1600x900.png
  contact-sheet-play.png
  contact-sheet-app_store.png
  contact-sheet-all.png
  validation.json
```

Before upload, inspect both platform contact sheets at full size. Check the source mapping, headline order, live data, user names, notification content, and any private details. Confirm that every record in `validation.json` has `preview_source: false` and the expected dimensions. The app is an unofficial companion. Do not add Riot, Valorant, tournament, team, or player logos to the surrounding artwork, and do not imply publisher approval.

Run the independent file check after rendering:

```sh
python3 distribution/rebrand-2026/template/validate.py \
  distribution/rebrand-2026/output/final
```

The check reads each PNG, confirms its dimensions and RGB mode, rejects preview sources, and verifies that capture scaling kept the source aspect ratio. For the labeled development set, add `--allow-preview`.

## Play Console upload copies

The September 13 draft uses the final phone PNGs and feature graphic. Both tablet sections use the equivalent quality-98 JPEG copies in `output/play-upload-tablet-jpeg/`, because the native browser file picker stalled with tablet PNGs. The source PNGs remain the lossless masters.
