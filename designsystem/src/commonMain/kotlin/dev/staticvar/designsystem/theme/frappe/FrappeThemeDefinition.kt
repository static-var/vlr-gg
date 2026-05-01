package dev.staticvar.designsystem.theme.frappe

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import dev.staticvar.designsystem.prism.color.PrismColorPalette
import dev.staticvar.designsystem.prism.theme.PrismThemeDefinition
import dev.staticvar.designsystem.prism.typography.PrismFontFamilies
import dev.staticvar.designsystem.prism.typography.PrismTypography

internal object FrappeThemeDefinition : PrismThemeDefinition<FrappeColorTokens> {
  override fun createColorTokens(): FrappeColorTokens = FrappeColorTokens()

  override fun createPalette(tokens: FrappeColorTokens): PrismColorPalette = FrappePalette.create(tokens)

  override fun createColorScheme(palette: PrismColorPalette): ColorScheme = FrappeColorScheme.create(palette)

  override fun createTypographyTokens(
    palette: PrismColorPalette,
    fonts: PrismFontFamilies,
  ): PrismTypography = FrappeTypography.createTokens(palette, fonts)

  override fun createMaterialTypography(tokens: PrismTypography): Typography =
    FrappeTypography.createMaterial(tokens)

  override val shapes: Shapes
    get() = FrappeShapes.shapes
}
