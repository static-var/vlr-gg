/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.illustration

import androidx.compose.ui.graphics.vector.ImageVector

internal fun unknownErrorVector(colors: IllustrationColors): ImageVector = illustrationVector("UnknownError") {
  backdrop(colors)
  shape(
    "M81 324 L139 271 H220 L281 325 L183 361 Z",
    fill = colors.shade,
    stroke = colors.ink,
    width = 2.0f,
  )
  shape(
    "M81 324 V337 L183 378 L281 337 V325 L183 361 Z",
    fill = colors.panel,
    stroke = colors.ink,
    width = 2.0f,
  )
  shape(
    "M122 320 L180 296 L240 321 L182 343 Z",
    fill = colors.soft,
    stroke = colors.accent,
    width = 2.0f,
  )
  shape(
    "M156 326 L180 316 L205 326 L182 336 Z",
    fill = colors.accent,
    stroke = null,
    width = 2.0f,
  )
  shape(
    "M116 343 L133 350 M239 350 L255 343",
    fill = null,
    stroke = colors.muted,
    width = 3.0f,
  )
  shape(
    "M147 319 L79 183 H277 L217 319 L181 334 Z",
    fill = colors.mist,
    stroke = null,
    width = 2.0f,
  )
  shape(
    "M65 128 L174 128 L159 168 L176 194 L158 230 L168 263 H65 Z",
    fill = colors.panel,
    stroke = colors.ink,
    width = 2.0f,
  )
  shape(
    "M195 140 H294 V275 H195 L185 245 L203 209 L187 181 Z",
    fill = colors.panel,
    stroke = colors.ink,
    width = 2.0f,
  )
  shape(
    "M78 143 H137 V159 H109 V187 H81 V229 H121 V246 H80 M281 155 H235 V184 H258 V219 H277 V257 H222 V239",
    fill = null,
    stroke = colors.faint,
    width = 3.0f,
  )
  shape(
    "M119 173 H143 V212 H130 V230 M211 161 V194 H234 V219 H212",
    fill = null,
    stroke = colors.muted,
    width = 2.0f,
  )
  shape(
    "M94 216 V202 H129 M226 202 H248 V241",
    fill = null,
    stroke = colors.accent,
    width = 3.0f,
  )
  shape(
    "M89 216 a5 5 0 1 0 10 0 a5 5 0 1 0 -10 0",
    fill = colors.accent,
    stroke = null,
    width = 2.0f,
  )
  shape(
    "M243 236 L253 246 M253 236 L243 246",
    fill = null,
    stroke = colors.accent,
    width = 2.0f,
  )
  shape(
    "M156 96 L175 92 L185 110 L163 115 Z",
    fill = colors.soft,
    stroke = colors.accent,
    width = 2.0f,
  )
  shape(
    "M295 193 L310 198 L306 214 L293 207 Z",
    fill = colors.soft,
    stroke = colors.muted,
    width = 2.0f,
  )
  shape(
    "M149 278 L166 271 L176 287 L158 293 Z",
    fill = colors.soft,
    stroke = colors.accent,
    width = 2.0f,
  )
  shape(
    "M145 204 a33 33 0 1 0 66 0 a33 33 0 1 0 -66 0",
    fill = colors.soft,
    stroke = colors.accent,
    width = 2.0f,
  )
  shape(
    "M168 194 C168 180 191 179 191 192 C191 202 178 201 178 212",
    fill = null,
    stroke = colors.ink,
    width = 4.0f,
  )
  shape(
    "M175.5 223 a2.5 2.5 0 1 0 5.0 0 a2.5 2.5 0 1 0 -5.0 0",
    fill = colors.ink,
    stroke = null,
    width = 2.0f,
  )
  shape(
    "M106 111 L100 104 M226 117 L233 108 M293 290 L301 298",
    fill = null,
    stroke = colors.muted,
    width = 2.0f,
  )
}
