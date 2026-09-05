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

Both platforms show the transparent Match point illustration on Mocha `#1E1E2E`. The visible artwork is centered and measures 132 by 126 dp/pt at its nominal display size.

Android uses the native splash screen through AndroidX, with `Theme.VLR.Starting` and `ic_splash_logo`. `MainActivity` installs it before `super.onCreate()` and then switches to the normal app theme.

iOS uses `UILaunchScreen` with the `LaunchLogo` image and `LaunchBackground` color assets. The image has 1x, 2x, and 3x representations. Both generator scripts reproduce their platform's splash logo.
