/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.prism.typography

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle

/**
 * Standard typography token set for the Prism design system.
 *
 * Provides semantic text styles that theme variants map to, ensuring a consistent
 * typographic hierarchy while allowing each theme to supply its own sizes, weights,
 * and font families.
 *
 * Token groups:
 * - **Display/Headline**: High-impact page and hero titles
 * - **Section & Card Titles**: Mid-level hierarchy for list and card headers
 * - **Body**: Primary and secondary reading text
 * - **Label & Caption**: Supporting UI copy, metadata, helper text
 * - **Button**: Dedicated style for interactive elements
 * - **Numeric**: Emphasised and supporting numerical data
 * - **Overline**: Optional uppercase tag above other content
 */
@Immutable
public data class PrismTypography(
  val display: TextStyle,
  val headline: TextStyle,
  val sectionTitle: TextStyle,
  val cardTitle: TextStyle,
  val bodyLarge: TextStyle,
  val bodySmall: TextStyle,
  val label: TextStyle,
  val caption: TextStyle,
  val button: TextStyle,
  val numericPrimary: TextStyle,
  val numericSecondary: TextStyle,
  val overline: TextStyle,
)
