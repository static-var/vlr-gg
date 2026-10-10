# Val Esports App Store screenshots at 9:41

## Apple device frames

The latest set uses official silver iPhone 18 Pro and 13-inch iPad Pro M5 bezels. See `frames/README.md` for sources and display geometry. Original native captures remain unchanged. Display masks follow Apple's bezel transparency, including rounded corners and the camera cutout.

```sh
rtk proxy python3 distribution/rebrand-2026/template/render.py \
  --manifest distribution/app-store-941/manifest-device-frames.json \
  --output distribution/app-store-941/output-device-frames \
  --target app_store --target app_store_ipad
rtk proxy python3 distribution/rebrand-2026/template/validate.py \
  distribution/app-store-941/output-device-frames
```

All ten framed exports and both contact sheets are in `output-device-frames/`.

This separate set reuses the existing ivory, violet, and ink layout from `distribution/rebrand-2026/template/render.py`. The original captures and artwork remain in `distribution/rebrand-2026/`.

## Capture requirements

Capture the running iOS app after setting the simulator status bar to 9:41. Save the original simulator PNGs directly to these paths. The time must be present in the captured status bar. Do not paint, replace, or overlay the clock afterward.

| File | Screen |
| --- | --- |
| `01.png` | Matches |
| `02.png` | Match details |
| `03.png` | Home with favorites |
| `04.png` | Event details |
| `05.png` | Rankings |

The iPhone originals belong in `raw/ios/`. The iPad originals belong in `raw/ios-ipad/`. Each device must supply all five genuine captures. The manifest has no older capture fallback.

## Render

Run from the repository root after all ten captures are ready:

```sh
rtk proxy python3 distribution/rebrand-2026/template/render.py \
  --manifest distribution/app-store-941/manifest.json \
  --output distribution/app-store-941/output \
  --target app_store \
  --target app_store_ipad

rtk proxy python3 distribution/rebrand-2026/template/validate.py \
  distribution/app-store-941/output
```

The renderer scales the full capture proportionally inside its frame. It does not redraw the app or its status bar.

## Output

- `output/app-store-iphone-1320x2868/01.png` through `05.png`
- `output/app-store-ipad-2064x2752/01.png` through `05.png`
- `output/contact-sheet-app_store.png`
- `output/contact-sheet-app_store_ipad.png`
- `output/contact-sheet-all.png`
- `output/validation.json`

Validation checks PNG format, RGB mode, recorded dimensions, absence of preview captures, and proportional scaling. Review the original captures and the rendered images visually to confirm 9:41 and the intended screen in every image. File validation does not inspect the clock text or establish capture provenance.
