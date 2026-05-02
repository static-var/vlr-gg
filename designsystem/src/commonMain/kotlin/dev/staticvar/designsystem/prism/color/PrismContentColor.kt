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
public fun contentColorFor(backgroundColor: Color): Color {
  val colors = Prism.color
  return when (backgroundColor) {
    // Accent containers
    colors.accent -> colors.surface

    colors.accentVariant -> colors.surface

    colors.accentSubtle -> colors.accent

    // Semantic containers
    colors.successContainer -> colors.success

    colors.warningContainer -> colors.warning

    colors.dangerContainer -> colors.danger

    colors.infoContainer -> colors.info

    // Surface variants
    colors.surface -> colors.contentPrimary

    colors.surfaceVariant -> colors.contentPrimary

    colors.surfaceDim -> colors.contentSecondary

    colors.backgroundElevated -> colors.contentPrimary

    colors.background -> colors.contentPrimary

    // Default fallback
    else -> colors.contentPrimary
  }
}
