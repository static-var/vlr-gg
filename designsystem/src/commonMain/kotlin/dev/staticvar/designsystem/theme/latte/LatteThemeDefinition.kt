package dev.staticvar.designsystem.theme.latte

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import dev.staticvar.designsystem.prism.color.PrismColorPalette
import dev.staticvar.designsystem.prism.theme.PrismThemeDefinition
import dev.staticvar.designsystem.prism.typography.PrismFontFamilies
import dev.staticvar.designsystem.prism.typography.PrismTypography

internal object LatteThemeDefinition : PrismThemeDefinition<LatteColorTokens> {
  override fun createColorTokens(): LatteColorTokens = LatteColorTokens()

  override fun createPalette(tokens: LatteColorTokens): PrismColorPalette = LattePalette.create(tokens)

  override fun createColorScheme(palette: PrismColorPalette): ColorScheme = LatteColorScheme.create(palette)

  override fun createTypographyTokens(
    palette: PrismColorPalette,
    fonts: PrismFontFamilies,
  ): PrismTypography = LatteTypography.createTokens(palette, fonts)

  override fun createMaterialTypography(tokens: PrismTypography): Typography =
    LatteTypography.createMaterial(tokens)

  override val shapes: Shapes
    get() = LatteShapes.shapes
}
