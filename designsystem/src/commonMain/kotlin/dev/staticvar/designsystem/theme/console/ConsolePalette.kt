/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.theme.console

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import dev.staticvar.designsystem.prism.PrismVariant
import dev.staticvar.designsystem.prism.color.PrismColorPalette

internal object ConsolePalette {
  fun create(variant: PrismVariant): PrismColorPalette {
    val dark = variant == PrismVariant.Dark
    val face = if (dark) Color.Black else Color.White
    val ink = if (dark) Color.White else Color.Black
    val muted = if (dark) Color(0xFFBEBEBE) else Color(0xFF595959)
    val accent = if (dark) Color(0xFFC8AEFF) else Color(0xFF7046B8)
    val success = if (dark) Color(0xFF79E2B1) else Color(0xFF006B4F)
    val warning = if (dark) Color(0xFFFFD580) else Color(0xFF785000)
    val danger = if (dark) Color(0xFFFF9393) else Color(0xFFB42332)
    val info = if (dark) Color(0xFF9BC6FF) else Color(0xFF245DA8)
    return PrismColorPalette(
      background = face,
      backgroundElevated = face,
      surface = face,
      surfaceVariant = face,
      surfaceDim = face,
      inverseSurface = ink,
      onInverseSurface = face,
      stroke = ink,
      strokeVariant = ink,
      divider = if (dark) Color(0xFF454545) else Color(0xFFD0D0D0),
      accent = accent,
      accentVariant = accent,
      accentSubtle = lerp(face, accent, 0.06f),
      onAccent = face,
      onAccentVariant = face,
      inverseAccent = if (dark) Color(0xFF7046B8) else Color(0xFFC8AEFF),
      titleColor = ink,
      bodyColor = ink,
      labelColor = muted,
      captionColor = muted,
      contentPrimary = ink,
      contentSecondary = muted,
      contentTertiary = muted,
      success = success,
      successContainer = lerp(face, success, 0.08f),
      warning = warning,
      warningContainer = lerp(face, warning, 0.08f),
      danger = danger,
      onDanger = face,
      dangerContainer = lerp(face, danger, 0.06f),
      info = info,
      infoContainer = lerp(face, info, 0.06f),
      scrim = Color.Black,
      primaryAction = Color(0xFF79E2B1),
      onPrimaryAction = Color.Black,
    )
  }
}
