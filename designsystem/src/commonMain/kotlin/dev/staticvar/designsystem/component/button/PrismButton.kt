@file:Suppress("MatchingDeclarationName")

package dev.staticvar.designsystem.component.button

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

@Composable
private fun flatButtonElevation() = ButtonDefaults.buttonElevation(
  defaultElevation = Prism.dimens.elevationNone,
  pressedElevation = Prism.dimens.elevationNone,
  focusedElevation = Prism.dimens.elevationNone,
  hoveredElevation = Prism.dimens.elevationNone,
  disabledElevation = Prism.dimens.elevationNone,
)

/**
 * Prism design system button component.
 *
 * Single entry point for flat Prism actions. [style] controls the visual treatment; click,
 * enabled, and interaction parameters control behavior.
 *
 * Example usage:
 * ```
 * PrismButton(
 *   onClick = { },
 *   style = PrismButtonStyle.Primary
 * ) {
 *   Text("Click me")
 * }
 * ```
 *
 * @param onClick Called when the button is clicked
 * @param modifier Modifier to apply to the button
 * @param style Visual treatment of the button
 * @param enabled Whether the button is enabled
 * @param interactionSource Interaction source for the button
 * @param content Button content (typically Text)
 */
@Composable
public fun PrismButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  style: PrismButtonStyle = PrismButtonStyle.Primary,
  enabled: Boolean = true,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
  content: @Composable RowScope.() -> Unit,
) {
  Button(
    onClick = onClick,
    modifier = modifier.defaultMinSize(minHeight = Prism.dimens.controlHeight),
    enabled = enabled,
    shape = Prism.shapes.small,
    colors =
    ButtonDefaults.buttonColors(
      containerColor = style.containerColor(enabled = true),
      contentColor = style.contentColor(enabled = true),
      disabledContainerColor = style.containerColor(enabled = false),
      disabledContentColor = style.contentColor(enabled = false),
    ),
    elevation = flatButtonElevation(),
    border = style.border(enabled = enabled),
    contentPadding = PaddingValues(horizontal = Prism.dimens.spacingM, vertical = Prism.dimens.spacingS),
    interactionSource = interactionSource,
  ) {
    ProvideTextStyle(value = Prism.typography.button) {
      content()
    }
  }
}
