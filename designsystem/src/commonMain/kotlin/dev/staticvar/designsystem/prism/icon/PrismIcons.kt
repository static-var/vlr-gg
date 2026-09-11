/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.prism.icon

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

/** Theme artwork for navigation and toolbar actions. Components supply tint and interaction state. */
@Immutable
public data class PrismIcons(
  val refresh: ImageVector,
  val share: ImageVector,
  val preview: ImageVector,
  val back: ImageVector,
  val home: PrismNavigationIcons,
  val news: PrismNavigationIcons,
  val matches: PrismNavigationIcons,
  val events: PrismNavigationIcons,
  val rankings: PrismNavigationIcons,
  val settings: PrismNavigationIcons,
)

/** Matching silhouettes for the resting and selected state of a navigation destination. */
@Immutable
public data class PrismNavigationIcons(
  val unselected: ImageVector,
  val selected: ImageVector,
)
