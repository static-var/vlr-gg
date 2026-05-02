/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.prism.dimens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal providing access to the current Prism dimension tokens.
 *
 * This is an internal implementation detail. Use [MaterialTheme.prismDimens]
 * to access dimensions in composables.
 */
internal val LocalPrismDimens =
  staticCompositionLocalOf<PrismDimens> {
    error("No PrismDimens provided. Ensure content is wrapped in a PrismTheme.")
  }

/**
 * Access the current Prism dimension tokens through MaterialTheme.
 *
 * Provides spacing, sizing, and layout dimension tokens for consistent
 * spacing hierarchy across the design system.
 *
 * Example usage:
 * ```
 * Box(
 *   modifier = Modifier.padding(MaterialTheme.prismDimens.spacingL)
 * )
 * ```
 */
public val MaterialTheme.prismDimens: PrismDimens
  @Composable
  @ReadOnlyComposable
  get() = LocalPrismDimens.current
