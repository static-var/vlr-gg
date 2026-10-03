# Match point app icon

The selected master is [Match point](match-point.svg). Its flat colors and stepped geometry are preserved in both platforms.

## Regenerate

Requires Python 3, Pillow, and `rsvg-convert` from librsvg. Run from the repository root:

```sh
rtk proxy python3 art/app-icon/generate-ios-icons.py
rtk proxy python3 art/app-icon/generate-android-icons.py
```

The scripts replace the launcher assets with renders of the selected master.

## iOS

`iosApp/iosApp/Assets.xcassets/AppIcon.appiconset` contains opaque RGB PNGs for iPhone, iPad, and the App Store. `Contents.json` maps each slot to its correct pixel dimensions. The source stays square; iOS applies the icon mask.

## Android

`androidApp/src/main/res` contains adaptive vector layers and legacy PNGs at all five launcher densities. The existing manifest references remain valid.

The color foreground maps the master into the central 72 dp area of a 108 dp adaptive layer. The entire illustration fits inside the 66 dp safe circle. The background is the master's mauve `#CBA6F7`.

[The monochrome master](match-point-monochrome.svg) keeps the display frame and V readable when the launcher applies wallpaper colors. Android applies the mask to adaptive icons; only the legacy PNGs have rounded or circular masks baked in.

The monochrome frame and its V are centered independently on the canvas. Its spacing does not include the color illustration's shadow.

## Splash screens

Both platforms show the transparent Match point illustration on the design system background (`#0A0A0A` dark, `#FFFFFF` light) with the outlined Val Esports wordmark.

Android uses the native splash screen through AndroidX, with `Theme.VLR.Starting` and `ic_splash_logo`. `MainActivity` installs it before `super.onCreate()` and then switches to the normal app theme. Android 12 and later also draw the `splash_branding` wordmark near the bottom edge; the platform has no slot for the iOS caption.

iOS uses `LaunchScreen.storyboard` with the `LaunchLogo` and `LaunchCaption` images and the `LaunchBackground` color. The logo stacks the illustration above the wordmark. The caption sits above the bottom safe area. Both images are SVGs with light and dark variants.

Regenerate the launch wordmarks with fontTools installed:

```sh
rtk proxy python3 art/app-icon/generate-ios-launch-screen.py
rtk proxy python3 art/app-icon/generate-android-splash-branding.py
```
