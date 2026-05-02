@file:Suppress("LongParameterList", "MatchingDeclarationName")

package dev.staticvar.designsystem.component.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.color.contentColorFor

/**
 * Card variants for the Prism design system.
 *
 * - **Outlined**: Card with strong flat border
 * - **Filled**: Card with filled background and border
 * - **Gradient**: Card with gradient background (requires brush parameter)
 */
public enum class PrismCardVariant {
  Outlined,
  Filled,
  Gradient,
}

/**
 * Prism design system card component.
 *
 * Provides a container with optional click handling and different visual variants.
 *
 * Example usage:
 * ```
 * PrismCard(
 *   variant = PrismCardVariant.Outlined,
 *   onClick = { }
 * ) {
 *   Text("Card content")
 * }
 *
 * // Gradient variant
 * PrismCard(
 *   variant = PrismCardVariant.Gradient,
 *   brush = Brush.linearGradient(...)
 * ) {
 *   Text("Gradient card")
 * }
 * ```
 *
 * @param modifier Modifier to apply to the card
 * @param variant Visual variant of the card
 * @param onClick Optional click handler (makes card clickable)
 * @param enabled Whether the card is enabled for interaction
 * @param shape Shape of the card
 * @param color Background color of the card (ignored if brush is provided for Gradient variant)
 * @param brush Optional gradient brush (required for Gradient variant)
 * @param contentColor Content color for children
 * @param interactionSource Interaction source for click effects
 * @param content Card content
 */
@Composable
public fun PrismCard(
  modifier: Modifier = Modifier,
  variant: PrismCardVariant = PrismCardVariant.Outlined,
  onClick: (() -> Unit)? = null,
  enabled: Boolean = true,
  shape: Shape = Prism.shapes.medium,
  color: Color = Prism.color.surface,
  brush: Brush? = null,
  contentColor: Color = contentColorFor(color),
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
  content: @Composable ColumnScope.() -> Unit,
) {
  val cardShape = shape

  val resolvedColor =
    when (variant) {
      PrismCardVariant.Filled ->
        if (color == Prism.color.surface) Prism.color.surfaceVariant else color
      else -> color
    }

  val resolvedContentColor =
    when {
      contentColor != contentColorFor(color) -> contentColor
      variant == PrismCardVariant.Filled && color == Prism.color.surface -> Prism.color.contentPrimary
      else -> contentColorFor(resolvedColor)
    }

  val borderColor = if (variant == PrismCardVariant.Outlined) Prism.color.strokeVariant else Prism.color.stroke
  val border = BorderStroke(width = Prism.dimens.strokeDefault, color = borderColor)

  val finalModifier =
    if (onClick != null) {
      modifier.clickable(
        onClick = onClick,
        enabled = enabled,
        role = Role.Button,
        interactionSource = interactionSource,
        indication = ripple(),
      )
    } else {
      modifier
    }

  PrismSurface(
    modifier = finalModifier,
    color = resolvedColor,
    brush = if (variant == PrismCardVariant.Gradient) brush else null,
    contentColor = resolvedContentColor,
    shape = cardShape,
    border = border,
  ) {
    Column(modifier = Modifier.padding(Prism.dimens.spacingM)) { content() }
  }
}
