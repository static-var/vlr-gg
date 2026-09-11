/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.theme.light

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import dev.staticvar.designsystem.prism.color.PrismColorPalette
import dev.staticvar.designsystem.prism.icon.BrutalistIcons
import dev.staticvar.designsystem.prism.theme.PrismThemeDefinition
import dev.staticvar.designsystem.prism.typography.PrismFontFamilies
import dev.staticvar.designsystem.prism.typography.PrismTypography
import dev.staticvar.designsystem.theme.PrismTypographyTokens

internal object LightThemeDefinition : PrismThemeDefinition<LightColorTokens> {
  override val icons = BrutalistIcons

  override fun createColorTokens(): LightColorTokens = LightColorTokens()

  override fun createPalette(tokens: LightColorTokens): PrismColorPalette = LightPalette.create(tokens)

  override fun createColorScheme(palette: PrismColorPalette): ColorScheme = LightColorScheme.create(palette)

  override fun createTypographyTokens(palette: PrismColorPalette, fonts: PrismFontFamilies): PrismTypography =
    PrismTypographyTokens.createTokens(fonts)

  override fun createMaterialTypography(tokens: PrismTypography): Typography = PrismTypographyTokens.createMaterial(tokens)

  override val shapes: Shapes
    get() = LightShapes.shapes
}
