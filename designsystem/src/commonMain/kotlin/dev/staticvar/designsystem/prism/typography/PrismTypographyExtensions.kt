/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.prism.typography

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal providing access to the current Prism typography tokens.
 *
 * Internal detail: use [MaterialTheme.prismTypography] inside composables instead
 * of referencing this local directly.
 */
internal val LocalPrismTypography =
  staticCompositionLocalOf<PrismTypography> {
    error("No PrismTypography provided. Ensure content is wrapped in a PrismTheme.")
  }

/**
 * Access the current Prism typography tokens through [MaterialTheme].
 *
 * Exposes a semantic set of [TextStyle] tokens that extend Material3 typography
 * with Prism-specific roles such as numeric emphasis, section titles, and button text.
 */
public val MaterialTheme.prismTypography: PrismTypography
  @Composable
  @ReadOnlyComposable
  get() = LocalPrismTypography.current
