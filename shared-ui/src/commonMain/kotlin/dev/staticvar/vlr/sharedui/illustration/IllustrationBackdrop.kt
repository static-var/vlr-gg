/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.illustration

import androidx.compose.ui.graphics.vector.ImageVector

internal fun ImageVector.Builder.backdrop(colors: IllustrationColors) {
  shape(
    "M38 202 C22 124 85 65 164 76 C236 34 319 104 316 182 C347 253 304 339 223 346 C147 389 51 334 44 276 Z",
    fill = colors.mist,
    stroke = null,
    width = 2.0f,
  )
  shape(
    "M40 349 L177 291 L323 351 L181 412 Z",
    fill = colors.mist,
    stroke = null,
    width = 2.0f,
  )
  shape(
    "M61 349 L181 397 L300 350",
    fill = null,
    stroke = colors.faint,
    width = 1.0f,
  )
  shape(
    "M95 335 L217 384",
    fill = null,
    stroke = colors.faint,
    width = 1.0f,
  )
  shape(
    "M133 320 L255 368",
    fill = null,
    stroke = colors.faint,
    width = 1.0f,
  )
  shape(
    "M100 368 L220 319",
    fill = null,
    stroke = colors.faint,
    width = 1.0f,
  )
  shape(
    "M140 385 L263 337",
    fill = null,
    stroke = colors.faint,
    width = 1.0f,
  )
  shape(
    "M40 151 V129 H62 M298 281 H320 V259 M285 96 H302 V113",
    fill = null,
    stroke = colors.faint,
    width = 2.0f,
  )
  shape(
    "M57 99 H65 M61 95 V103",
    fill = null,
    stroke = colors.muted,
    width = 1.5f,
  )
  shape(
    "M303 215 H311 M307 211 V219",
    fill = null,
    stroke = colors.muted,
    width = 1.5f,
  )
  shape(
    "M85 286 H93 M89 282 V290",
    fill = null,
    stroke = colors.muted,
    width = 1.5f,
  )
  shape(
    "M268 66 a3 3 0 1 0 6 0 a3 3 0 1 0 -6 0",
    fill = colors.muted,
    stroke = null,
    width = 2.0f,
  )
  shape(
    "M31 271 a2 2 0 1 0 4 0 a2 2 0 1 0 -4 0",
    fill = colors.accent,
    stroke = null,
    width = 2.0f,
  )
}
