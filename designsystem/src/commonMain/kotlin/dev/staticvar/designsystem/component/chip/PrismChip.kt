package dev.staticvar.designsystem.component.chip

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Declarative chip model for [PrismChipGroup].
 */
@Immutable
public data class PrismChip(
  val id: String,
  val label: String,
  val icon: ImageVector? = null,
  val selectedIcon: ImageVector? = icon,
  val enabled: Boolean = true,
)
