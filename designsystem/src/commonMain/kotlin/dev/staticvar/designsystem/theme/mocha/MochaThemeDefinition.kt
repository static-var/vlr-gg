package dev.staticvar.designsystem.theme.mocha

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import dev.staticvar.designsystem.prism.color.PrismColorPalette
import dev.staticvar.designsystem.prism.theme.PrismThemeDefinition
import dev.staticvar.designsystem.prism.typography.PrismFontFamilies
import dev.staticvar.designsystem.prism.typography.PrismTypography

internal object MochaThemeDefinition : PrismThemeDefinition<MochaColorTokens> {
  override fun createColorTokens(): MochaColorTokens = MochaColorTokens()

  override fun createPalette(tokens: MochaColorTokens): PrismColorPalette = MochaPalette.create(tokens)

  override fun createColorScheme(palette: PrismColorPalette): ColorScheme = MochaColorScheme.create(palette)

  override fun createTypographyTokens(
    palette: PrismColorPalette,
    fonts: PrismFontFamilies,
  ): PrismTypography = MochaTypography.createTokens(palette, fonts)

  override fun createMaterialTypography(tokens: PrismTypography): Typography =
    MochaTypography.createMaterial(tokens)

  override val shapes: Shapes
    get() = MochaShapes.shapes
}
