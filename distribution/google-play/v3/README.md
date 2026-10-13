# Google Play screenshots v3

This set places full-height Android captures inside a camera-free, unbranded device frame instead of cropping fragments into flat cards. Its shallow screen radius preserves the screenshots' native status-bar padding rather than clipping the upper corners. The presentation keeps the v2 editorial direction: violet-black, high-contrast serif headlines, spaced technical captions, and a restrained dark-metal device finish.

## Inputs

Put these exact PNGs in `source/raw/`:

- `01-live-matches.png`
- `02-map-stats.png`
- `03-event-results.png`
- `04-rankings.png`
- `05-widget.png`

Every input must be a 1080×2400 PNG. The renderer deliberately rejects missing, partial, differently sized, or non-PNG captures. Except for removing an 80 px stray route-title strip from the event capture, it fits the full capture into the phone screen rather than cropping app content. The oversized device continues below the 1080×1920 marketing canvas. The widget phone is positioned lower so its widget remains visible while launcher icons stay outside the artwork. This keeps unwanted lower-page UI out of the final composition without altering the source captures.

The event slide uses evergreen copy—“Every result. One circuit.”—and describes past results rather than displaying an expiring date in the marketing layer. The capture itself should likewise avoid a time-sensitive event overview.

## Render

From this directory:

```sh
./render.sh
```

The five 1080×1920 PNGs are written to `output/`, followed by a 1350×480 `review-contact-sheet.png`. The script uses only Bash and ImageMagick 7 and creates temporary intermediates outside the repository.

The default macOS fonts are Didot, Avenir Next, and Menlo, which fill the same editorial roles as Playfair Display, Geist, and Geist Mono without adding a build-time download. For exact font files:

```sh
HEADLINE_FONT=/path/to/PlayfairDisplay-Regular.ttf \
BODY_FONT=/path/to/Geist-Regular.ttf \
MONO_FONT=/path/to/GeistMono-Regular.ttf \
./render.sh
```

`SOURCE_DIR` and `OUTPUT_DIR` may also be overridden with absolute paths.

## Device frame

The active frame is drawn deterministically by ImageMagick in `render.sh`. It has no punch-hole, notch, or baked status-bar glyphs. At final scale its screen corner radius is approximately 39 px—small enough to retain the captured time and system icons while still reading as a modern phone.

The previously evaluated Telephone Pixel 9 Pro source and its MIT license remain in `assets/` for provenance, but are no longer used by the renderer because their camera and screen geometry obscured the captures.
