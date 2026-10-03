#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SOURCE_DIR="$SCRIPT_DIR/source"
OUTPUT_DIR="$SCRIPT_DIR/output"
BACKGROUND="$SOURCE_DIR/abstract-esports-background-v1.png"
APP_ICON="$SCRIPT_DIR/../../app/src/main/ic_launcher-playstore.png"
FONT_REGULAR="/System/Library/Fonts/Supplemental/Verdana.ttf"
FONT_BOLD="/System/Library/Fonts/Supplemental/Verdana Bold.ttf"
WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

mkdir -p "$OUTPUT_DIR"

magick "$BACKGROUND" \
  -resize '1024x500^' -gravity center -extent 1024x500 \
  -fill 'rgba(3,4,18,0.24)' -draw 'rectangle 0,0 1024,500' \
  "$WORK_DIR/feature-base.png"

magick "$APP_ICON" -resize 220x220 \
  \( +clone -background '#050612' -shadow '38x12+0+14' \) \
  +swap -background none -layers merge "$WORK_DIR/feature-icon.png"

magick -background none -size 650x58 -gravity west \
  -font "$FONT_REGULAR" -pointsize 27 -fill '#D9DBFF' \
  caption:'VLR.gg (Unofficial)' +repage "$WORK_DIR/feature-kicker.png"

magick -background none -size 650x160 -gravity west \
  -font "$FONT_BOLD" -pointsize 52 -fill white -interline-spacing 6 \
  caption:$'VALORANT ESPORTS\nAT A GLANCE' +repage "$WORK_DIR/feature-title.png"

magick -background none -size 650x52 -gravity west \
  -font "$FONT_REGULAR" -pointsize 22 -fill '#D9DBFF' \
  caption:'Matches  •  Events  •  Rankings  •  Alerts' +repage "$WORK_DIR/feature-subtitle.png"

magick "$WORK_DIR/feature-base.png" \
  "$WORK_DIR/feature-icon.png" -geometry +58+130 -composite \
  "$WORK_DIR/feature-kicker.png" -geometry +330+82 -composite \
  "$WORK_DIR/feature-title.png" -geometry +330+142 -composite \
  "$WORK_DIR/feature-subtitle.png" -geometry +330+330 -composite \
  "$OUTPUT_DIR/feature-graphic.png"

file "$OUTPUT_DIR"/*.png
