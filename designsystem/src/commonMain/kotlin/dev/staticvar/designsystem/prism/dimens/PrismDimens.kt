/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.prism.dimens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standard dimension token set for the Prism design system.
 *
 * Simplified, pragmatic spacing and sizing tokens. Use raw dp values
 * for edge cases - these tokens are for consistency, not exhaustiveness.
 *
 * Token groups:
 * - **Spacing**: 5 levels (xs → xl)
 * - **Corner**: 4 levels (s, m, l, full)
 * - **Stroke**: 2 levels (default, thick)
 * - **Icon**: 3 levels (s, m, l)
 * - **Control**: shared interactive heights and touch target minimums
 * - **Elevation**: 3 levels (none, low, high)
 * - **Divider**: thickness
 */
@Immutable
public data class PrismDimens(
  // Spacing (4dp base scale)
  val spacingXs: Dp = 4.dp,
  val spacingS: Dp = 8.dp,
  val spacingM: Dp = 16.dp,
  val spacingL: Dp = 24.dp,
  val spacingXl: Dp = 32.dp,

  // Corner Radius
  val cornerS: Dp = 8.dp,
  val cornerM: Dp = 12.dp,
  val cornerL: Dp = 16.dp,
  val cornerFull: Dp = 9999.dp,

  // Stroke Width
  val strokeDefault: Dp = 1.dp,
  val strokeThick: Dp = 2.dp,

  // Icon Sizes
  val iconS: Dp = 16.dp,
  val iconM: Dp = 24.dp,
  val iconL: Dp = 32.dp,

  // Interactive Controls
  val controlHeight: Dp = 48.dp,
  val touchTargetMin: Dp = 48.dp,

  // Elevation
  val elevationNone: Dp = 0.dp,
  val elevationLow: Dp = 2.dp,
  val elevationHigh: Dp = 8.dp,

  // Divider
  val dividerThickness: Dp = 1.dp,
)
