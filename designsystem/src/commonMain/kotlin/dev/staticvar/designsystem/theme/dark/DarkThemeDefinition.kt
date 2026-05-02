package dev.staticvar.designsystem.theme.dark

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import dev.staticvar.designsystem.prism.color.PrismColorPalette
import dev.staticvar.designsystem.prism.theme.PrismThemeDefinition
import dev.staticvar.designsystem.prism.typography.PrismFontFamilies
import dev.staticvar.designsystem.prism.typography.PrismTypography

internal object DarkThemeDefinition : PrismThemeDefinition<DarkColorTokens> {
  override fun createColorTokens(): DarkColorTokens = DarkColorTokens()

  override fun createPalette(tokens: DarkColorTokens): PrismColorPalette = DarkPalette.create(tokens)

  override fun createColorScheme(palette: PrismColorPalette): ColorScheme = DarkColorScheme.create(palette)

  override fun createTypographyTokens(
    palette: PrismColorPalette,
    fonts: PrismFontFamilies,
  ): PrismTypography = DarkTypography.createTokens(palette, fonts)

  override fun createMaterialTypography(tokens: PrismTypography): Typography =
    DarkTypography.createMaterial(tokens)

  override val shapes: Shapes
    get() = DarkShapes.shapes
}
