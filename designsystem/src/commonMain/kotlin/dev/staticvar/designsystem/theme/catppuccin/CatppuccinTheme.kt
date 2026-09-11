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
import dev.staticvar.designsystem.prism.PrismCatppuccinFlavour
import dev.staticvar.designsystem.prism.color.PrismColorPalette
import dev.staticvar.designsystem.prism.icon.CatppuccinIcons
import dev.staticvar.designsystem.prism.theme.PrismThemeDefinition
import dev.staticvar.designsystem.prism.theme.ProvidePrismTheme
import dev.staticvar.designsystem.prism.typography.PrismFontFamilies
import dev.staticvar.designsystem.prism.typography.PrismTypography
import dev.staticvar.designsystem.theme.PrismTypographyTokens
import dev.staticvar.designsystem.theme.createPrismColorScheme

@Composable
internal fun CatppuccinTheme(flavour: PrismCatppuccinFlavour, content: @Composable () -> Unit) {
  val definition = remember(flavour) { CatppuccinThemeDefinition(flavour) }
  ProvidePrismTheme(definition, content)
}

internal data class CatppuccinThemeDefinition(val flavour: PrismCatppuccinFlavour) :
  PrismThemeDefinition<CatppuccinColorTokens> {
  override val icons = CatppuccinIcons

  override fun createColorTokens(): CatppuccinColorTokens = when (flavour) {
    PrismCatppuccinFlavour.Latte -> CatppuccinColorTokens.Latte
    PrismCatppuccinFlavour.Frappe -> CatppuccinColorTokens.Frappe
    PrismCatppuccinFlavour.Macchiato -> CatppuccinColorTokens.Macchiato
    PrismCatppuccinFlavour.Mocha -> CatppuccinColorTokens.Mocha
  }

  override fun createPalette(tokens: CatppuccinColorTokens): PrismColorPalette =
    CatppuccinPalette.create(tokens, flavour.isDark)

  override fun createColorScheme(palette: PrismColorPalette): ColorScheme =
    createPrismColorScheme(palette, flavour.isDark)

  override fun createTypographyTokens(palette: PrismColorPalette, fonts: PrismFontFamilies): PrismTypography =
    PrismTypographyTokens.createTokens(fonts)

  override fun createMaterialTypography(tokens: PrismTypography): Typography =
    PrismTypographyTokens.createMaterial(tokens)

  override val shapes: Shapes get() = CatppuccinShapes.shapes
}
