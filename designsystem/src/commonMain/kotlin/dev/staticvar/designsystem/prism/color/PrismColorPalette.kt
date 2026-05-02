/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.prism.color

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Standard color palette for the Prism design system.
 *
 * This interface defines semantic color tokens that all theme variants map to.
 * Each theme variant translates its unique colors into this standard palette,
 * ensuring consistency across the design system.
 *
 * Color categories:
 * - **Backgrounds**: Surface colors for different elevation levels
 * - **Borders & Dividers**: Strokes and separators
 * - **Accents**: Primary interaction colors
 * - **Content**: Text and content colors with different contrast levels
 * - **Semantic**: Status colors for success, warning, error, and info states
 */
@Immutable
public data class PrismColorPalette(
  // Backgrounds
  val background: Color,
  val backgroundElevated: Color,
  val surface: Color,
  val surfaceVariant: Color,
  val surfaceDim: Color,

  // Borders & Dividers
  val stroke: Color,
  val strokeVariant: Color,
  val divider: Color,

  // Accents & Actions
  val accent: Color,
  val accentVariant: Color,
  val accentSubtle: Color,

  // Content/Text Colors
  val titleColor: Color,
  val bodyColor: Color,
  val labelColor: Color,
  val captionColor: Color,
  val contentPrimary: Color,
  val contentSecondary: Color,
  val contentTertiary: Color,

  // Semantic Colors
  val success: Color,
  val successContainer: Color,
  val warning: Color,
  val warningContainer: Color,
  val danger: Color,
  val dangerContainer: Color,
  val info: Color,
  val infoContainer: Color,
)
