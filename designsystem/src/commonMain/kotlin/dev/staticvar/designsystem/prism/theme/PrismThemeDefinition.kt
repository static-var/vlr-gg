/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.prism.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import dev.staticvar.designsystem.prism.animation.DefaultPrismAnimations
import dev.staticvar.designsystem.prism.animation.PrismAnimationTokens
import dev.staticvar.designsystem.prism.color.PrismColorPalette
import dev.staticvar.designsystem.prism.frame.PrismFrames
import dev.staticvar.designsystem.prism.icon.PrismIcons
import dev.staticvar.designsystem.prism.typography.PrismFontFamilies
import dev.staticvar.designsystem.prism.typography.PrismTypography

/**
 * Theme contract describing how a concrete Prism variant supplies its styling pieces.
 */
internal interface PrismThemeDefinition<ColorTokens : Any> {
  val icons: PrismIcons

  fun createColorTokens(): ColorTokens

  fun createPalette(tokens: ColorTokens): PrismColorPalette

  fun createColorScheme(palette: PrismColorPalette): ColorScheme

  fun createTypographyTokens(palette: PrismColorPalette, fonts: PrismFontFamilies): PrismTypography

  fun createMaterialTypography(tokens: PrismTypography): Typography

  val animations: PrismAnimationTokens get() = DefaultPrismAnimations

  fun createFrames(palette: PrismColorPalette): PrismFrames = PrismFrames()

  val shapes: Shapes
}
