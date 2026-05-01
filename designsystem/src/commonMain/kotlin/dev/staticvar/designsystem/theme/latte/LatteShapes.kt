package dev.staticvar.designsystem.theme.latte

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape definitions for Latte theme.
 *
 * Provides consistent corner radii across all components following
 * Material3's shape system.
 */
internal object LatteShapes {
  val shapes =
    Shapes(
      extraSmall = RoundedCornerShape(4.dp),
      small = RoundedCornerShape(8.dp),
      medium = RoundedCornerShape(12.dp),
      large = RoundedCornerShape(16.dp),
      extraLarge = RoundedCornerShape(28.dp),
    )
}
