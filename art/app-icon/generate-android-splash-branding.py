#!/usr/bin/env python3
"""Build the Android splash branding wordmark from the app's display font.

Requires Python 3 and fontTools. Android 12 and later draw it near the bottom of the splash screen.
Run from any directory: python3 art/app-icon/generate-android-splash-branding.py
"""

from pathlib import Path

from outlined_text import outlined_text


ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "androidApp/src/main/res/drawable/splash_branding.xml"
# The platform's branding slot is 200 by 80 dp.
WIDTH, HEIGHT = 200, 80


def main():
    wordmark = outlined_text("VAL ESPORTS", "chakra_petch_regular.ttf", 22, 5, WIDTH / 2, 44)
    OUTPUT.write_text(f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="{WIDTH}dp" android:height="{HEIGHT}dp" android:viewportWidth="{WIDTH}" android:viewportHeight="{HEIGHT}">
    <path android:fillColor="@color/splash_wordmark" android:pathData="{wordmark}" />
    <path android:fillColor="@color/splash_accent" android:pathData="M88 58h28l-4 4h-28z" />
</vector>
''')


if __name__ == "__main__":
    main()
