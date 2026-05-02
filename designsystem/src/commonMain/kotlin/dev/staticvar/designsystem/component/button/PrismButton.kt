@file:Suppress("MatchingDeclarationName")

package dev.staticvar.designsystem.component.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.color.contentColorFor

/**
 * Button variants for the Prism design system.
 *
 * - **Primary**: Accent-filled call-to-action
 * - **Secondary**: Flat outlined action
 * - **Tertiary**: Ghost-style flat action with border
 */
public enum class PrismButtonVariant {
  Primary,
  Secondary,
  Tertiary,
}

@Composable
private fun flatButtonElevation() =
  ButtonDefaults.buttonElevation(
    defaultElevation = Prism.dimens.elevationNone,
    pressedElevation = Prism.dimens.elevationNone,
    focusedElevation = Prism.dimens.elevationNone,
    hoveredElevation = Prism.dimens.elevationNone,
    disabledElevation = Prism.dimens.elevationNone,
  )

/**
 * Prism design system button component.
 *
 * Single entry point for flat brutalist button variants.
 *
 * Example usage:
 * ```
 * PrismButton(
 *   onClick = { },
 *   variant = PrismButtonVariant.Primary
 * ) {
 *   Text("Click me")
 * }
 * ```
 *
 * @param onClick Called when the button is clicked
 * @param modifier Modifier to apply to the button
 * @param variant Visual variant of the button
 * @param enabled Whether the button is enabled
 * @param interactionSource Interaction source for the button
 * @param content Button content (typically Text)
 */
@Composable
public fun PrismButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  variant: PrismButtonVariant = PrismButtonVariant.Primary,
  enabled: Boolean = true,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
  content: @Composable RowScope.() -> Unit,
) {
  val containerColor =
    when (variant) {
      PrismButtonVariant.Primary -> Prism.color.accent
      PrismButtonVariant.Secondary -> Prism.color.surface
      PrismButtonVariant.Tertiary -> Prism.color.background
    }
  val contentColor =
    when (variant) {
      PrismButtonVariant.Primary -> contentColorFor(containerColor)
      PrismButtonVariant.Secondary -> Prism.color.titleColor
      PrismButtonVariant.Tertiary -> Prism.color.labelColor
    }
  val border =
    when (variant) {
      PrismButtonVariant.Primary ->
        BorderStroke(
          width = Prism.dimens.strokeThick,
          color = if (enabled) Prism.color.accent else Prism.color.stroke,
        )
      PrismButtonVariant.Secondary ->
        BorderStroke(
          width = Prism.dimens.strokeDefault,
          color = if (enabled) Prism.color.stroke else Prism.color.strokeVariant,
        )
      PrismButtonVariant.Tertiary ->
        BorderStroke(
          width = Prism.dimens.strokeDefault,
          color = if (enabled) Prism.color.strokeVariant else Prism.color.stroke,
        )
    }

  Button(
    onClick = onClick,
    modifier = modifier.defaultMinSize(minHeight = Prism.dimens.controlHeight),
    enabled = enabled,
    shape = Prism.shapes.small,
    colors =
      ButtonDefaults.buttonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = Prism.color.surfaceDim,
        disabledContentColor = Prism.color.labelColor,
      ),
    elevation = flatButtonElevation(),
    border = border,
    contentPadding = PaddingValues(horizontal = Prism.dimens.spacingM, vertical = Prism.dimens.spacingS),
    interactionSource = interactionSource,
  ) {
    ProvideTextStyle(value = Prism.typography.button) {
      content()
    }
  }
}
