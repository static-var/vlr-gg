/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.navigation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Declarative model for a bottom navigation destination.
 */
@Immutable
public data class PrismBottomNavItem(
  val id: String,
  val label: String,
  val icon: ImageVector,
  val selectedIcon: ImageVector = icon,
)
