/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.theme.catppuccin

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.staticvar.designsystem.prism.PrismVariant
import dev.staticvar.designsystem.prism.color.PrismColorPalette
import dev.staticvar.designsystem.prism.theme.PrismThemeDefinition
import dev.staticvar.designsystem.prism.theme.ProvidePrismTheme
import dev.staticvar.designsystem.prism.typography.PrismFontFamilies
import dev.staticvar.designsystem.prism.typography.PrismTypography
import dev.staticvar.designsystem.theme.PrismTypographyTokens
import dev.staticvar.designsystem.theme.createPrismColorScheme
import dev.staticvar.designsystem.theme.light.LightShapes

@Composable
internal fun CatppuccinTheme(variant: PrismVariant, content: @Composable () -> Unit) {
  val definition = remember(variant) { CatppuccinThemeDefinition(variant) }
  ProvidePrismTheme(definition, content)
}

internal data class CatppuccinThemeDefinition(val variant: PrismVariant) : PrismThemeDefinition<CatppuccinColorTokens> {
  private val isDark: Boolean get() = variant == PrismVariant.Dark

  override fun createColorTokens(): CatppuccinColorTokens =
    if (isDark) CatppuccinColorTokens.Frappe else CatppuccinColorTokens.Latte

  override fun createPalette(tokens: CatppuccinColorTokens): PrismColorPalette =
    CatppuccinPalette.create(tokens, isDark)

  override fun createColorScheme(palette: PrismColorPalette): ColorScheme = createPrismColorScheme(palette, isDark)

  override fun createTypographyTokens(palette: PrismColorPalette, fonts: PrismFontFamilies): PrismTypography =
    PrismTypographyTokens.createTokens(fonts)

  override fun createMaterialTypography(tokens: PrismTypography): Typography =
    PrismTypographyTokens.createMaterial(tokens)

  override val shapes: Shapes get() = LightShapes.shapes
}
