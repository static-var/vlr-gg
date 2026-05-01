package dev.staticvar.designsystem.theme.macchiato

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import dev.staticvar.designsystem.prism.color.PrismColorPalette
import dev.staticvar.designsystem.prism.theme.PrismThemeDefinition
import dev.staticvar.designsystem.prism.typography.PrismFontFamilies
import dev.staticvar.designsystem.prism.typography.PrismTypography

internal object MacchiatoThemeDefinition : PrismThemeDefinition<MacchiatoColorTokens> {
  override fun createColorTokens(): MacchiatoColorTokens = MacchiatoColorTokens()

  override fun createPalette(tokens: MacchiatoColorTokens): PrismColorPalette = MacchiatoPalette.create(tokens)

  override fun createColorScheme(palette: PrismColorPalette): ColorScheme = MacchiatoColorScheme.create(palette)

  override fun createTypographyTokens(
    palette: PrismColorPalette,
    fonts: PrismFontFamilies,
  ): PrismTypography = MacchiatoTypography.createTokens(palette, fonts)

  override fun createMaterialTypography(tokens: PrismTypography): Typography =
    MacchiatoTypography.createMaterial(tokens)

  override val shapes: Shapes
    get() = MacchiatoShapes.shapes
}
