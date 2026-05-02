/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.prism.color

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal providing access to the current Prism color palette.
 *
 * This is an internal implementation detail. Use [MaterialTheme.prismColors]
 * to access colors in composables.
 */
internal val LocalPrismColors =
  staticCompositionLocalOf<PrismColorPalette> {
    error("No PrismColorPalette provided. Ensure content is wrapped in a PrismTheme.")
  }

/**
 * Access the current Prism color palette through MaterialTheme.
 *
 * Provides extended color tokens beyond Material3's standard ColorScheme,
 * including semantic colors (titleColor, bodyColor, etc.) and design system
 * specific tokens (accent variants, semantic status colors, etc.).
 *
 * Example usage:
 * ```
 * Text(
 *   text = "Title",
 *   color = MaterialTheme.prismColors.titleColor
 * )
 * ```
 */
public val MaterialTheme.prismColors: PrismColorPalette
  @Composable
  @ReadOnlyComposable
  get() = LocalPrismColors.current
