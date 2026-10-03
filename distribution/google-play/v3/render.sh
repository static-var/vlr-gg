#!/usr/bin/env bash
set -euo pipefail

# Render the VLR.GG Google Play screenshot set with a camera-free device frame.
#
# Requirements: bash and ImageMagick 7 (`magick` and `identify`).
# The inputs are intentionally strict so a partial or accidentally downscaled
# Android capture cannot silently become a release asset.

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source_dir="${SOURCE_DIR:-$script_dir/source/raw}"
output_dir="${OUTPUT_DIR:-$script_dir/output}"

canvas_width=1080
canvas_height=1920
phone_width=650
phone_height=1372

# A shallow 9:20 inset keeps the screenshot's native status-bar padding intact.
# The reduced radius is deliberately much smaller than the former Pixel frame,
# and the shell has no punch-hole or baked status-bar artwork.
screen_left=24
screen_top=17
screen_width=602
screen_height=1338
screen_radius=28
shell_radius=52

background='#1A1A2E'
ink='#FBF9FF'
muted='#B9B2C7'
accent='#A88AF4'

# The renderer defaults to macOS system fonts with the same editorial roles as
# Playfair Display and Geist. Exact font files can be supplied without editing
# this script, e.g. HEADLINE_FONT=/path/PlayfairDisplay-Regular.ttf.
headline_font="${HEADLINE_FONT:-/System/Library/Fonts/Supplemental/Didot.ttc}"
body_font="${BODY_FONT:-/System/Library/Fonts/Avenir Next.ttc}"
mono_font="${MONO_FONT:-/System/Library/Fonts/Menlo.ttc}"

inputs=(
  '01-live-matches.png'
  '02-map-stats.png'
  '03-event-results.png'
  '04-rankings.png'
  '05-widget.png'
)

# One large, consistent device scale makes the app UI legible and mirrors the
# existing listing's phone-cutout composition. Every phone continues below the
# canvas. The widget starts lower so its full widget ends at the canvas edge
# while the launcher icons remain outside the artwork.
phone_widths=(900 900 900 900 900)
phone_heights=(1900 1900 1900 1900 1900)
phone_xs=(90 90 90 90 90)
phone_ys=(430 430 430 430 790)

labels=(
  'MATCHES / 01'
  'MATCH CENTER / 02'
  'EVENTS / 03'
  'RANKINGS / 04'
  'HOME SCREEN / 05'
)

headlines=(
  $'LIVE.\nRIGHT NOW.'
  $'MAP BY\nMAP.'
  $'EVERY RESULT.\nONE CIRCUIT.'
  $'THE WORLD,\nRANKED.'
  $'SCORES,\nAT HOME.'
)

descriptors=(
  'SCORES / SCHEDULES / MATCH ALERTS'
  'ROUND HISTORY / KDA / PLAYER STATS'
  'PAST MATCHES / SERIES SCORES / EVENT CONTEXT'
  'REGIONAL FORM / TEAM RATINGS'
  '4 MATCHES / ONE GLANCE / HOME WIDGET'
)

die() {
  printf 'render: %s\n' "$*" >&2
  exit 1
}

command -v magick >/dev/null 2>&1 || die 'ImageMagick 7 (`magick`) is required'
command -v identify >/dev/null 2>&1 || die 'ImageMagick `identify` is required'
[[ -f "$headline_font" ]] || die "missing headline font: $headline_font"
[[ -f "$body_font" ]] || die "missing body font: $body_font"
[[ -f "$mono_font" ]] || die "missing mono font: $mono_font"

for input_name in "${inputs[@]}"; do
  input_path="$source_dir/$input_name"
  [[ -f "$input_path" ]] || die "missing source screenshot: $input_path"

  dimensions="$(identify -quiet -format '%wx%h' "$input_path")" || \
    die "cannot inspect source screenshot: $input_path"
  [[ "$dimensions" == '1080x2400' ]] || \
    die "$input_path must be 1080x2400, got $dimensions"

  format="$(identify -quiet -format '%m' "$input_path")" || \
    die "cannot inspect source format: $input_path"
  [[ "$format" == 'PNG' ]] || die "$input_path must be a PNG, got $format"
done

mkdir -p "$output_dir"
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/vlr-play-v3.XXXXXX")"
trap 'rm -rf -- "$work_dir"' EXIT

# Draw an unbranded, camera-free shell locally. The narrow bezel, subtle metal
# highlights, and restrained corner radius preserve the phone-mockup treatment
# without covering screenshot content.
magick \
  -size "${phone_width}x${phone_height}" xc:none \
  -fill '#08080D' -stroke '#82788E' -strokewidth 3 \
  -draw "roundrectangle 2,2 $((phone_width - 3)),$((phone_height - 3)) ${shell_radius},${shell_radius}" \
  -fill none -stroke '#C7BDCF55' -strokewidth 1 \
  -draw "roundrectangle 6,6 $((phone_width - 7)),$((phone_height - 7)) $((shell_radius - 4)),$((shell_radius - 4))" \
  "$work_dir/device-body.png"

magick \
  -size "${phone_width}x${phone_height}" xc:none \
  -fill '#605869' -stroke '#C7BDCF66' -strokewidth 1 \
  -draw "roundrectangle $((phone_width - 2)),240 $((phone_width - 1)),340 1,1" \
  -draw "roundrectangle 0,180 1,238 1,1" \
  -draw "roundrectangle 0,255 1,345 1,1" \
  "$work_dir/device-details.png"

magick \
  -size "${screen_width}x${screen_height}" xc:none \
  -fill white \
  -draw "roundrectangle 0,0 $((screen_width - 1)),$((screen_height - 1)) ${screen_radius},${screen_radius}" \
  "$work_dir/screen-mask.png"

magick \
  -size "${phone_width}x${phone_height}" xc:none \
  -fill black \
  -draw "roundrectangle 5,5 $((phone_width - 6)),$((phone_height - 6)) ${shell_radius},${shell_radius}" \
  -channel A -blur '0x31' -evaluate multiply 0.68 +channel \
  "$work_dir/device-shadow.png"

render_slide() {
  local index="$1"
  local input_path="$source_dir/${inputs[$index]}"
  local output_path="$output_dir/${inputs[$index]}"
  local screen_path="$work_dir/screen-$index.png"
  local phone_path="$work_dir/phone-$index.png"
  local label_text="${labels[$index]}"
  local headline_text="${headlines[$index]}"
  local descriptor_text="${descriptors[$index]}"
  local slide_phone_width="${phone_widths[$index]}"
  local slide_phone_height="${phone_heights[$index]}"
  local slide_phone_x="${phone_xs[$index]}"
  local slide_phone_y="${phone_ys[$index]}"
  local number
  number="$(printf '%02d' "$((index + 1))")"

  # Preserve the full 9:20 capture and apply only the shallow generic-screen
  # radius. Slide 03 removes its known stale 80 px route-title artifact first.
  if [[ "$index" == '2' ]]; then
    # The event capture contains a one-line stale route title above its actual
    # Status/Rounds/Stage content. Remove only that 80 px capture artifact.
    magick "$input_path" \
      -crop '1080x2320+0+80' +repage \
      -filter Lanczos \
      -resize "${screen_width}x${screen_height}!" \
      "$work_dir/screen-scaled-$index.png"
  else
    magick "$input_path" \
      -filter Lanczos \
      -resize "${screen_width}x${screen_height}!" \
      "$work_dir/screen-scaled-$index.png"
  fi

  magick "$work_dir/screen-scaled-$index.png" \
    "$work_dir/screen-mask.png" \
    -alpha off -compose CopyOpacity -composite \
    "$screen_path"

  magick \
    -size "${phone_width}x${phone_height}" xc:none \
    "$work_dir/device-body.png" -geometry +0+0 -composite \
    "$screen_path" -geometry "+${screen_left}+${screen_top}" -composite \
    "$work_dir/device-details.png" -geometry +0+0 -composite \
    "$phone_path"

  magick "$phone_path" \
    -resize "${slide_phone_width}x${slide_phone_height}!" \
    "$work_dir/phone-final-$index.png"

  magick "$work_dir/device-shadow.png" \
    -resize "${slide_phone_width}x${slide_phone_height}!" \
    "$work_dir/shadow-final-$index.png"

  magick \
    -size "${canvas_width}x${canvas_height}" "xc:$background" \
    -fill '#FFFFFF0B' -stroke none \
    -draw 'circle 910,320 1190,320' \
    -fill none -stroke '#FFFFFF12' -strokewidth 2 \
    -draw 'line 80,405 1000,405' \
    -fill "$accent" -stroke none \
    -draw 'rectangle 80,72 132,78' \
    -font "$mono_font" -pointsize 25 -kerning 7 -fill "$accent" \
    -annotate +80+119 "$label_text" \
    -font "$headline_font" -pointsize 100 -kerning -1 -interline-spacing -16 -fill "$ink" \
    -annotate +76+217 "$headline_text" \
    -font "$mono_font" -pointsize 21 -kerning 3 -fill "$muted" \
    -annotate +80+383 "$descriptor_text" \
    -font "$headline_font" -pointsize 250 -fill '#FFFFFF0D' \
    -gravity NorthEast -annotate +54+44 "$number" -gravity NorthWest \
    "$work_dir/shadow-final-$index.png" -geometry "+${slide_phone_x}+$((slide_phone_y + 24))" -composite \
    "$work_dir/phone-final-$index.png" -geometry "+${slide_phone_x}+${slide_phone_y}" -composite \
    -strip -define png:color-type=6 \
    "$output_path"

  dimensions="$(identify -quiet -format '%wx%h' "$output_path")"
  [[ "$dimensions" == '1080x1920' ]] || die "bad output dimensions for $output_path: $dimensions"
  printf 'rendered %s\n' "$output_path"
}

for index in 0 1 2 3 4; do
  render_slide "$index"
done

magick \
  "$output_dir/${inputs[0]}" \
  "$output_dir/${inputs[1]}" \
  "$output_dir/${inputs[2]}" \
  "$output_dir/${inputs[3]}" \
  "$output_dir/${inputs[4]}" \
  -resize '270x480!' +append \
  "$script_dir/review-contact-sheet.png"

printf 'rendered 5 screenshots in %s\n' "$output_dir"
printf 'rendered review contact sheet in %s\n' "$script_dir/review-contact-sheet.png"
