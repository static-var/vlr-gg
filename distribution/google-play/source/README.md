# Source assets

The superseded v1 phone captures were removed. Current screenshot captures and exports are in `../v3/`. This directory retains the background used by the feature graphic.

The reusable abstract background was produced with the built-in image-generation tool using this prompt:

```text
Use case: ads-marketing
Asset type: Google Play Store screenshot backdrop and feature-graphic foundation for an unofficial competitive esports companion app
Primary request: create an original abstract premium esports broadcast background with a calm but energetic sense of live competition and real-time data
Scene/backdrop: layered angular planes, subtle arena-light beams, faint scoreboard-grid rhythm, and soft depth; no literal venue or people
Style/medium: polished raster marketing artwork, modern editorial tech aesthetic, restrained and sophisticated rather than flashy gaming art
Composition/framing: wide landscape composition with generous clean negative space across the center and upper third; edge details may be cropped for portrait derivatives
Lighting/mood: luminous, focused, trustworthy, contemporary
Color palette: deep indigo, soft lavender, muted violet, tiny restrained berry accents, near-black shadows
Text: none
Constraints: entirely original; no logos, no trademarks, no recognizable game art, no characters, no weapons, no interface mockups, no watermark; preserve ample low-detail space for later typography and real app screenshots
Avoid: neon overload, cyberpunk city, generic gamer mascots, glowing 3D text, busy particle clutter
```

Run `./render-assets.sh` from the `distribution/google-play` directory to rebuild the feature graphic. Use `v3/render.sh` for the current phone screenshots.

