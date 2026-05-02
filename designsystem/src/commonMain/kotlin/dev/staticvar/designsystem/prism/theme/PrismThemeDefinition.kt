/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.prism.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import dev.staticvar.designsystem.prism.color.PrismColorPalette
import dev.staticvar.designsystem.prism.typography.PrismFontFamilies
import dev.staticvar.designsystem.prism.typography.PrismTypography

/**
 * Theme contract describing how a concrete Prism variant supplies its styling pieces.
 */
internal interface PrismThemeDefinition<ColorTokens : Any> {
  fun createColorTokens(): ColorTokens

  fun createPalette(tokens: ColorTokens): PrismColorPalette

  fun createColorScheme(palette: PrismColorPalette): ColorScheme

  fun createTypographyTokens(palette: PrismColorPalette, fonts: PrismFontFamilies): PrismTypography

  fun createMaterialTypography(tokens: PrismTypography): Typography

  val shapes: Shapes
}
