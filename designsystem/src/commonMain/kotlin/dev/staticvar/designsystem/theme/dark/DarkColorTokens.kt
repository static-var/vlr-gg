/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.theme.dark

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
internal data class DarkColorTokens(
  val background: Color = Color(0xFF0A0A0A),
  val surface: Color = Color(0xFF141414),
  val textPrimary: Color = Color(0xFFFFFFFF),
  val textSecondary: Color = Color(0xFFA3A3A3),
  val textTertiary: Color = Color(0xFF737373),
  val accent: Color = Color(0xFFA78BFA),
  val accentHover: Color = Color(0xFFC4B5FD),
  val accentMuted: Color = Color(0x1AA78BFA),
  val border: Color = Color(0xFF262626),
  val borderStrong: Color = Color(0xFFFFFFFF),
  val success: Color = Color(0xFF22C55E),
  val successContainer: Color = Color(0xFF052E16),
  val warning: Color = Color(0xFFF59E0B),
  val warningContainer: Color = Color(0xFF451A03),
  val danger: Color = Color(0xFFF87171),
  val dangerContainer: Color = Color(0xFF450A0A),
  val info: Color = Color(0xFFC4B5FD),
  val infoContainer: Color = Color(0xFF2E1065),
)
