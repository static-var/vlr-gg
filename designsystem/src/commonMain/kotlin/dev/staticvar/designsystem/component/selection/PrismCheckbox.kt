package dev.staticvar.designsystem.component.selection

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism

/**
 * Flat brutalist checkbox with a square, bordered control.
 */
@Composable
public fun PrismCheckbox(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
  val containerColor =
    when {
      !enabled -> Prism.color.surfaceDim
      checked -> Prism.color.accentSubtle
      else -> Prism.color.surface
    }
  val borderColor =
    when {
      !enabled -> Prism.color.stroke
      checked -> Prism.color.accent
      else -> Prism.color.stroke
    }
  val indicatorColor = if (enabled) Prism.color.accent else Prism.color.labelColor
  val animation = Prism.anim.standard
  val animatedContainerColor by
    animateColorAsState(
      targetValue = containerColor,
      animationSpec = animation.colorSpec(),
      label = "checkbox_container",
    )
  val animatedBorderColor by
    animateColorAsState(
      targetValue = borderColor,
      animationSpec = animation.colorSpec(),
      label = "checkbox_border",
    )
  val animatedIndicatorColor by
    animateColorAsState(
      targetValue = indicatorColor,
      animationSpec = animation.colorSpec(),
      label = "checkbox_indicator",
    )
  val animatedBorderWidth by
    animateDpAsState(
      targetValue = if (checked) Prism.dimens.strokeThick else Prism.dimens.strokeDefault,
      animationSpec = animation.dpSpec(),
      label = "checkbox_border_width",
    )
  val indicatorScale by
    animateFloatAsState(
      targetValue = if (checked) 1f else 0f,
      animationSpec = animation.floatSpec(),
      label = "checkbox_indicator_scale",
    )
  val indicatorAlpha by
    animateFloatAsState(
      targetValue = if (checked) 1f else 0f,
      animationSpec = animation.floatSpec(),
      label = "checkbox_indicator_alpha",
    )

  Box(
    modifier =
      modifier
        .sizeIn(
          minWidth = Prism.dimens.touchTargetMin,
          minHeight = Prism.dimens.touchTargetMin,
        )
        .toggleable(
          value = checked,
          onValueChange = onCheckedChange,
          enabled = enabled,
          role = Role.Checkbox,
          interactionSource = interactionSource,
          indication = ripple(),
        ),
    contentAlignment = Alignment.Center,
  ) {
    PrismSurface(
      modifier = Modifier.size(Prism.dimens.iconM),
      color = animatedContainerColor,
      shape = Prism.shapes.small,
      border =
        BorderStroke(
          width = animatedBorderWidth,
          color = animatedBorderColor,
        ),
    ) {
      Box(
        modifier =
          Modifier.fillMaxSize()
            .padding(Prism.dimens.spacingXs),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier =
            Modifier.fillMaxSize()
              .graphicsLayer {
                scaleX = indicatorScale
                scaleY = indicatorScale
                alpha = indicatorAlpha
              }
              .background(color = animatedIndicatorColor, shape = Prism.shapes.small),
        )
      }
    }
  }
}
