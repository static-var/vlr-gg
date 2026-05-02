/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.prism.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import dev.staticvar.designsystem.prism.color.LocalPrismColors
import dev.staticvar.designsystem.prism.dimens.LocalPrismDimens
import dev.staticvar.designsystem.prism.dimens.PrismDimens
import dev.staticvar.designsystem.prism.typography.LocalPrismTypography
import dev.staticvar.designsystem.prism.typography.rememberPrismFontFamilies

/**
 * Shared theme host that wires up a [PrismThemeDefinition].
 */
@Composable
internal fun <ColorTokens : Any> ProvidePrismTheme(
  definition: PrismThemeDefinition<ColorTokens>,
  content: @Composable () -> Unit,
) {
  val tokens = remember { definition.createColorTokens() }
  val palette = remember(tokens) { definition.createPalette(tokens) }
  val colorScheme = remember(palette) { definition.createColorScheme(palette) }

  val fonts = rememberPrismFontFamilies()
  val typographyTokens = remember(palette, fonts) { definition.createTypographyTokens(palette, fonts) }
  val materialTypography = remember(typographyTokens) { definition.createMaterialTypography(typographyTokens) }

  val dimens = remember { PrismDimens() }

  CompositionLocalProvider(
    LocalPrismColors provides palette,
    LocalPrismTypography provides typographyTokens,
    LocalPrismDimens provides dimens,
    LocalContentColor provides palette.contentPrimary,
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = materialTypography,
      shapes = definition.shapes,
      content = content,
    )
  }
}
