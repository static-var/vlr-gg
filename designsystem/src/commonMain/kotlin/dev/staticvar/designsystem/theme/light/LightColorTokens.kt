/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
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
  val successContainer: Color = Color(0xFFDCFCE7),
  val warning: Color = Color(0xFFB45309),
  val warningContainer: Color = Color(0xFFFEF3C7),
  val danger: Color = Color(0xFFB91C1C),
  val dangerContainer: Color = Color(0xFFFEE2E2),
  val info: Color = Color(0xFF6D28D9),
  val infoContainer: Color = Color(0xFFEDE9FE),
)
