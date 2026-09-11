/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.theme.console

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.staticvar.designsystem.prism.PrismVariant
import dev.staticvar.designsystem.prism.animation.PrismAnimationPreset
import dev.staticvar.designsystem.prism.animation.PrismAnimationTokens
import dev.staticvar.designsystem.prism.color.PrismColorPalette
import dev.staticvar.designsystem.prism.icon.ConsoleIcons
import dev.staticvar.designsystem.prism.frame.PrismFrameTokens
import dev.staticvar.designsystem.prism.frame.PrismFrames
import dev.staticvar.designsystem.prism.theme.PrismThemeDefinition
import dev.staticvar.designsystem.prism.typography.PrismFontFamilies
import dev.staticvar.designsystem.prism.typography.PrismTypography
import dev.staticvar.designsystem.theme.PrismTypographyTokens
import dev.staticvar.designsystem.theme.createPrismColorScheme

internal data class ConsoleThemeDefinition(val variant: PrismVariant) : PrismThemeDefinition<PrismVariant> {
  override val icons = ConsoleIcons

  override fun createColorTokens(): PrismVariant = variant

  override fun createPalette(tokens: PrismVariant): PrismColorPalette = ConsolePalette.create(tokens)

  override fun createColorScheme(palette: PrismColorPalette): ColorScheme =
    createPrismColorScheme(palette, variant == PrismVariant.Dark)

  override fun createTypographyTokens(palette: PrismColorPalette, fonts: PrismFontFamilies): PrismTypography {
    val pixelFonts = fonts.copy(display = fonts.pixel, title = fonts.pixel, numeric = fonts.pixel, button = fonts.pixel)
    val tokens = PrismTypographyTokens.createTokens(pixelFonts)
    return tokens.copy(
      display = tokens.display.copy(fontWeight = FontWeight.Normal, fontSize = 42.sp),
      headline = tokens.headline.copy(fontWeight = FontWeight.Normal, fontSize = 32.sp),
      sectionTitle = tokens.sectionTitle.copy(fontWeight = FontWeight.Normal, fontSize = 28.sp),
      cardTitle = tokens.cardTitle.copy(fontWeight = FontWeight.Normal, fontSize = 26.sp),
      button = tokens.button.copy(fontSize = 22.sp),
      numericPrimary = tokens.numericPrimary.copy(fontSize = 34.sp),
      numericSecondary = tokens.numericSecondary.copy(fontSize = 22.sp),
    )
  }

  override fun createMaterialTypography(tokens: PrismTypography): Typography =
    PrismTypographyTokens.createMaterial(tokens)

  override val shapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
  )

  override val animations: PrismAnimationTokens = PrismAnimationTokens(
    standard = PrismAnimationPreset(120, LinearEasing),
    slowFade = PrismAnimationPreset(180, LinearEasing),
    press = PrismAnimationPreset(60, LinearEasing),
    selection = PrismAnimationPreset(90, LinearEasing),
    navigationSelection = PrismAnimationPreset(90, LinearEasing),
    sheetEnter = PrismAnimationPreset(180, FastOutSlowInEasing),
    sheetExit = PrismAnimationPreset(140, FastOutSlowInEasing),
    scrimEnter = PrismAnimationPreset(120, LinearEasing),
    scrimExit = PrismAnimationPreset(120, LinearEasing),
  )

  override fun createFrames(palette: PrismColorPalette): PrismFrames {
    val shadow = if (variant == PrismVariant.Dark) Color(0xFF454545) else Color.Black
    val border = BorderStroke(2.dp, palette.stroke)
    return PrismFrames(
      panel = PrismFrameTokens(border, shadow, DpOffset(6.dp, 6.dp)),
      control = PrismFrameTokens(border, shadow, DpOffset(4.dp, 4.dp)),
      compact = PrismFrameTokens(border, shadow, DpOffset(2.dp, 2.dp)),
    )
  }
}
