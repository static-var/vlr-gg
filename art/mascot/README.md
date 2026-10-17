# Mascot artwork

`lynx.svg` and `rosie.svg` are the masters for the two companions drawn in `shared-ui`'s mascot package. They use a small
SVG subset so a script can turn them into Compose `Path` code:

- `viewBox="0 0 160 160"`, elements `path` (M L H V C S Q T Z), `circle`, `ellipse`, `rect`, `line`, `polyline`, `polygon`.
- Colours are theme tokens, `fill="var(--fur)"`: fur, furLight, furDark, outline, ink, white, pink, shadow.
  `MascotArt.kt` derives them from the Prism palette.
- Layer groups, which `MascotFigure` animates: `tail` (swings around `data-pivot`), `body` (breathes), `paws` inside body
  (reused on the celebration banner), `head` (bobs), `eyes` inside head (blinks around `data-pivot`).
- The head fills the card peek window x 28..132, y 12..95; `MascotPeekWindow` crops the small card mascot to it.
- `svg2compose.py` caches every shape as a `Path` val and every distinct `stroke-width` as a `Stroke` val, so a draw
  allocates nothing while the idle animation redraws each frame.

Workflow:

```
python3 art/mascot/validate.py art/mascot/lynx.svg
python3 art/mascot/sheet.py /tmp/mascots.png lynx=art/mascot/lynx.svg rosie=art/mascot/rosie.svg
python3 art/mascot/svg2compose.py art/mascot/lynx.svg Lynx shared-ui/src/commonMain/kotlin/dev/staticvar/vlr/sharedui/mascot/LynxArt.kt
python3 art/mascot/svg2compose.py art/mascot/rosie.svg Rosie shared-ui/src/commonMain/kotlin/dev/staticvar/vlr/sharedui/mascot/RosieArt.kt
```

`sheet.py` renders light and dark previews of every placement (160 dp, 88 dp, card peek, celebration pose) through
Quick Look. It approximates the Compose palette. For the real rendering, `MascotPreview` in the same package shows both
characters at every size through the agent preview task:

```
./gradlew :shared-ui:captureComposePreviews -PagentPreview.previewNameFilter=Mascot -PagentPreview.viewportFilter=android-phone
```

The PNGs land under `shared-ui/build/agentPreviewSnapshots/`. `MascotCelebrationPreview` and
`MascotCelebrationNarrowPreview` show the speech banner at each message length, the narrow one at 320 dp with a 2x font
scale; they carry explicit sizes, so render them with `-PagentPreview.viewportFilter=preview`.
