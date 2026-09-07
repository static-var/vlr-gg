/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.prism.color

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import dev.staticvar.designsystem.prism.Prism

/**
 * Returns the appropriate content color for a given container color.
 *
 * This function defines semantic color pairs for the Prism design system,
 * ensuring proper contrast and readability.
 *
 * Usage:
 * ```
 * Surface(
 *   color = Prism.color.accent,
 *   contentColor = contentColorFor(Prism.color.accent)
 * ) {
 *   Text("Auto-colored text") // Uses contentColor automatically
 * }
 * ```
 *
 * @param backgroundColor The container/background color
 * @return The appropriate content color for the given background
 */
@Composable
@ReadOnlyComposable
public fun contentColorFor(backgroundColor: Color): Color = Prism.color.contentColorFor(backgroundColor)

/** Resolves semantic container/content pairs without requiring a composition. */
public fun PrismColorPalette.contentColorFor(backgroundColor: Color): Color = when (backgroundColor) {
  primaryAction -> onPrimaryAction
  accent -> onAccent
  accentVariant -> onAccentVariant
  accentSubtle -> accent
  danger -> onDanger
  successContainer -> success
  warningContainer -> warning
  dangerContainer -> danger
  infoContainer -> info
  surfaceDim -> contentSecondary
  else -> contentPrimary
}
