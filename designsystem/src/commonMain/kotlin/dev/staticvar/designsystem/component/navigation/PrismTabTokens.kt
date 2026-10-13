/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.navigation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/** How a theme family draws [PrismTabs] chips. Colors, frames, and motion still come from the theme. */
@Immutable
internal data class PrismTabTokens(
  /** Fully rounded ends instead of the theme's square or small corners. */
  val pill: Boolean = false,
  /** Uppercase display-font labels instead of the button style. */
  val displayLabel: Boolean = true,
  val selectedFill: PrismTabFill = PrismTabFill.Inverse,
  val count: PrismTabCount = PrismTabCount.Plain,
)

internal enum class PrismTabFill {
  /** Inverse surface, matching the section-title tab. */
  Inverse,
  Accent,
}

internal enum class PrismTabCount {
  /** Bare number beside the label. */
  Plain,

  /** Number in a rounded badge. */
  Badge,

  /** Number in a solid block with inverted colors. */
  Block,
}

internal val LocalPrismTabTokens = staticCompositionLocalOf { PrismTabTokens() }
