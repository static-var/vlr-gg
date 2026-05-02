@file:Suppress("CyclomaticComplexMethod", "LongParameterList", "MatchingDeclarationName")

package dev.staticvar.designsystem.component.accordion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import compose.icons.LineAwesomeIcons
import compose.icons.lineawesomeicons.AngleDownSolid
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.color.contentColorFor

/**
 * Accordion variants for the Prism design system.
 *
 * - **Outlined**: Accordion with flat border
 * - **Filled**: Accordion with filled background
 * - **Minimal**: Accordion with minimal styling (no border)
 */
public enum class PrismAccordionVariant {
  Outlined,
  Filled,
  Minimal,
}

/**
 * Prism design system accordion component.
 *
 * Expandable/collapsible container with a header and content section.
 *
 * Example usage:
 * ```
 * // Outlined variant
 * PrismAccordion(
 *   variant = PrismAccordionVariant.Outlined,
 *   header = { Text("Click to expand") }
 * ) {
 *   Text("Hidden content")
 * }
 *
 * // Outlined variant
 * PrismAccordion(
 *   variant = PrismAccordionVariant.Outlined,
 *   header = { Text("Settings") }
 * ) {
 *   Text("Configuration options")
 * }
 * ```
 *
 * @param header Composable for the always-visible header
 * @param modifier Modifier to apply to the accordion
 * @param variant Visual variant of the accordion
 * @param expanded Whether the accordion is expanded (controlled)
 * @param onExpandedChange Callback when expansion state changes
 * @param enabled Whether the accordion can be toggled
 * @param shape Shape of the accordion
 * @param color Background color (automatically adjusted per variant)
 * @param contentColor Content color for children
 * @param showIndicator Whether to show expansion indicator
 * @param interactionSource Interaction source for click effects
 * @param content Expandable content
 */
@Composable
public fun PrismAccordion(
  header: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  variant: PrismAccordionVariant = PrismAccordionVariant.Outlined,
  expanded: Boolean = false,
  onExpandedChange: ((Boolean) -> Unit)? = null,
  enabled: Boolean = true,
  shape: Shape = Prism.shapes.medium,
  color: Color = Prism.color.surface,
  contentColor: Color = contentColorFor(color),
  showIndicator: Boolean = true,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
  content: @Composable ColumnScope.() -> Unit,
) {
  var internalExpanded by rememberSaveable(expanded) { mutableStateOf(expanded) }
  val isExpanded = onExpandedChange?.let { expanded } ?: internalExpanded
  val colors =
    rememberAccordionColors(
      variant = variant,
      color = color,
      contentColor = contentColor,
    )
  val border = accordionBorder(variant)

  PrismSurface(
    modifier = modifier,
    color = colors.containerColor,
    contentColor = colors.contentColor,
    shape = shape,
    border = border,
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .clickable(
              onClick = {
                onExpandedChange?.invoke(!isExpanded) ?: run { internalExpanded = !internalExpanded }
              },
              enabled = enabled,
              role = Role.Button,
              interactionSource = interactionSource,
              indication = ripple(),
            )
            .padding(horizontal = Prism.dimens.spacingM, vertical = Prism.dimens.spacingS),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        AccordionHeaderContent(
          header = header,
          isExpanded = isExpanded,
          showIndicator = showIndicator,
        )
      }

      if (isExpanded) {
        AccordionDivider()
      }

      AccordionExpandableContent(isExpanded = isExpanded, content = content)
    }
  }
}

@Composable
private fun rememberAccordionColors(
  variant: PrismAccordionVariant,
  color: Color,
  contentColor: Color,
): AccordionColors {
  val containerColor =
    when (variant) {
      PrismAccordionVariant.Filled ->
        if (color == Prism.color.surface) Prism.color.surfaceVariant else color
      PrismAccordionVariant.Minimal ->
        if (color == Prism.color.surface) Color.Transparent else color
      else -> color
    }
  val resolvedContentColor =
    when {
      contentColor != contentColorFor(color) -> contentColor
      variant == PrismAccordionVariant.Filled && color == Prism.color.surface ->
        Prism.color.contentPrimary
      variant == PrismAccordionVariant.Minimal -> Prism.color.contentPrimary
      else -> contentColorFor(containerColor)
    }

  return AccordionColors(containerColor = containerColor, contentColor = resolvedContentColor)
}

@Composable
private fun accordionBorder(variant: PrismAccordionVariant): BorderStroke? =
  when (variant) {
    PrismAccordionVariant.Outlined ->
      BorderStroke(width = Prism.dimens.strokeDefault, color = Prism.color.stroke)
    else -> null
  }

@Composable
private fun RowScope.AccordionHeaderContent(
  header: @Composable () -> Unit,
  isExpanded: Boolean,
  showIndicator: Boolean,
) {
  val rotationAngle by
    animateFloatAsState(
      targetValue = if (isExpanded) 180f else 0f,
      label = "accordion_icon_rotation",
    )

  Column(modifier = Modifier.weight(1f)) { header() }

  if (showIndicator) {
    Icon(
      imageVector = LineAwesomeIcons.AngleDownSolid,
      contentDescription = if (isExpanded) "Collapse section" else "Expand section",
      modifier = Modifier.rotate(rotationAngle).size(Prism.dimens.iconM),
      tint = Prism.color.labelColor,
    )
  }
}

@Composable
private fun AccordionDivider() {
  HorizontalDivider(
    modifier = Modifier.fillMaxWidth().padding(horizontal = Prism.dimens.spacingM),
    thickness = Prism.dimens.strokeDefault,
    color = Prism.color.stroke,
  )
}

@Composable
private fun AccordionExpandableContent(
  isExpanded: Boolean,
  content: @Composable ColumnScope.() -> Unit,
) {
  AnimatedVisibility(
    visible = isExpanded,
    enter = expandVertically(animationSpec = tween(durationMillis = 300), expandFrom = Alignment.Top),
    exit = shrinkVertically(animationSpec = tween(durationMillis = 300), shrinkTowards = Alignment.Top),
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .animateContentSize(animationSpec = tween(durationMillis = 300))
          .padding(
            horizontal = Prism.dimens.spacingM,
            vertical = Prism.dimens.spacingS,
          ),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
    ) {
      content()
    }
  }
}

private data class AccordionColors(
  val containerColor: Color,
  val contentColor: Color,
)
