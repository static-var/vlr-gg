/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.chip

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Declarative chip model for [PrismChipGroup].
 *
 * @property supportingText Optional secondary value shown after [label] with lower emphasis, such as
 * a score or count.
 */
@Immutable
public data class PrismChip(
  val id: String,
  val label: String,
  val icon: ImageVector? = null,
  val selectedIcon: ImageVector? = icon,
  val enabled: Boolean = true,
  val supportingText: String? = null,
)
