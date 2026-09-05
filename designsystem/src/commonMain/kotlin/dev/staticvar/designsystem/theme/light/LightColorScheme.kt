/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.theme.light

import androidx.compose.material3.ColorScheme
import dev.staticvar.designsystem.prism.color.PrismColorPalette
import dev.staticvar.designsystem.theme.createPrismColorScheme

internal object LightColorScheme {
  fun create(palette: PrismColorPalette): ColorScheme = createPrismColorScheme(palette, isDark = false)
}
