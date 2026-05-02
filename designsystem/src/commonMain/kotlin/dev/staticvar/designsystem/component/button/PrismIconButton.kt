@file:Suppress("LongParameterList", "MatchingDeclarationName")

package dev.staticvar.designsystem.component.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism

public enum class PrismIconButtonSize {
  Small,
  Medium,
  Large,
  XL,
}

/**
 * Flat brutalist icon button with a square bordered container.
 */
@Composable
public fun PrismIconButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  size: PrismIconButtonSize = PrismIconButtonSize.Medium,
  enabled: Boolean = true,
  selected: Boolean = false,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
  val (containerSize, iconSize) =
    when (size) {
      PrismIconButtonSize.Small -> Prism.dimens.iconM + Prism.dimens.spacingXs to Prism.dimens.iconS
      PrismIconButtonSize.Medium -> Prism.dimens.iconM + Prism.dimens.spacingS to Prism.dimens.iconM
      PrismIconButtonSize.Large -> Prism.dimens.iconM + Prism.dimens.spacingM to Prism.dimens.iconL
      PrismIconButtonSize.XL -> Prism.dimens.iconL + Prism.dimens.spacingL to Prism.dimens.iconL
    }

  val containerColor =
    when {
      !enabled -> Prism.color.surfaceDim
      selected -> Prism.color.accentSubtle
      else -> Prism.color.surface
    }
  val borderColor =
    when {
      !enabled -> Prism.color.stroke
      selected -> Prism.color.strokeVariant
      else -> Prism.color.stroke
    }
  val iconColor = if (enabled) Prism.color.titleColor else Prism.color.labelColor

  Box(
    modifier =
      modifier
        .sizeIn(
          minWidth = Prism.dimens.touchTargetMin,
          minHeight = Prism.dimens.touchTargetMin,
        )
        .clickable(
          onClick = onClick,
          enabled = enabled,
          role = Role.Button,
          interactionSource = interactionSource,
          indication = ripple(),
        ),
    contentAlignment = Alignment.Center,
  ) {
    PrismSurface(
      modifier = Modifier.size(containerSize),
      color = containerColor,
      shape = Prism.shapes.small,
      border = BorderStroke(Prism.dimens.strokeDefault, borderColor),
    ) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = contentDescription,
          modifier = Modifier.size(iconSize),
          tint = iconColor,
        )
      }
    }
  }
}
