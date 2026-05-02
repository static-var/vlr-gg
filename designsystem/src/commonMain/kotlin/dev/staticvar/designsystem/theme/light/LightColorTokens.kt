/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.theme.light

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
internal data class LightColorTokens(
  val background: Color = Color(0xFFFFFFFF),
  val surface: Color = Color(0xFFF5F5F5),
  val textPrimary: Color = Color(0xFF000000),
  val textSecondary: Color = Color(0xFF404040),
  val textTertiary: Color = Color(0xFF737373),
  val accent: Color = Color(0xFF7C3AED),
  val accentHover: Color = Color(0xFF6D28D9),
  val accentMuted: Color = Color(0x1A7C3AED),
  val border: Color = Color(0xFFE5E5E5),
  val borderStrong: Color = Color(0xFF000000),
  val success: Color = Color(0xFF15803D),
  val warning: Color = Color(0xFFB45309),
  val danger: Color = Color(0xFFB91C1C),
  val info: Color = Color(0xFF6D28D9),
)
