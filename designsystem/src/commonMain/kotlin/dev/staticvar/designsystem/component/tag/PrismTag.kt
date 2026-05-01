package dev.staticvar.designsystem.component.tag

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism

private object PrismTagConstants {
  val LabelFontSize = 10.sp
  val LabelLineHeight = 12.sp
  val LabelLetterSpacing = 0.4.sp
}

public enum class PrismTagVariant {
  Neutral,
  Accent,
  Success,
  Warning,
  Danger,
  Info,
}

/**
 * Compact, non-interactive brutalist tag for status and metadata labels.
 */
@Composable
public fun PrismTag(
  text: String,
  modifier: Modifier = Modifier,
  variant: PrismTagVariant = PrismTagVariant.Neutral,
  enabled: Boolean = true,
  leadingContent: (@Composable () -> Unit)? = null,
) {
  val containerColor =
    when {
      !enabled -> Prism.color.surfaceDim
      variant == PrismTagVariant.Neutral -> Prism.color.surface
      variant == PrismTagVariant.Accent -> Prism.color.accentSubtle
      variant == PrismTagVariant.Success -> Prism.color.successContainer
      variant == PrismTagVariant.Warning -> Prism.color.warningContainer
      variant == PrismTagVariant.Danger -> Prism.color.dangerContainer
      else -> Prism.color.infoContainer
    }
  val contentColor =
    when {
      !enabled -> Prism.color.captionColor
      variant == PrismTagVariant.Neutral -> Prism.color.labelColor
      variant == PrismTagVariant.Accent -> Prism.color.accent
      variant == PrismTagVariant.Success -> Prism.color.success
      variant == PrismTagVariant.Warning -> Prism.color.warning
      variant == PrismTagVariant.Danger -> Prism.color.danger
      else -> Prism.color.info
    }
  val borderColor =
    when {
      !enabled -> Prism.color.stroke
      variant == PrismTagVariant.Neutral -> Prism.color.stroke
      variant == PrismTagVariant.Accent -> Prism.color.accent
      variant == PrismTagVariant.Success -> Prism.color.success
      variant == PrismTagVariant.Warning -> Prism.color.warning
      variant == PrismTagVariant.Danger -> Prism.color.danger
      else -> Prism.color.info
    }

  val animation = Prism.anim.standard
  val resolvedText = text.trim().uppercase()
  val animatedContainerColor by
  animateColorAsState(
    targetValue = containerColor,
    animationSpec = animation.colorSpec(),
    label = "tag_container",
  )
  val animatedContentColor by
  animateColorAsState(
    targetValue = contentColor,
    animationSpec = animation.colorSpec(),
    label = "tag_content",
  )
  val animatedBorderColor by
  animateColorAsState(
    targetValue = borderColor,
    animationSpec = animation.colorSpec(),
    label = "tag_border",
  )

  PrismSurface(
    modifier = modifier,
    color = animatedContainerColor,
    shape = Prism.shapes.small,
    border = BorderStroke(Prism.dimens.strokeDefault, animatedBorderColor),
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (leadingContent != null) {
        Box(
          modifier = Modifier.size(Prism.dimens.iconS),
          contentAlignment = Alignment.Center,
        ) {
          leadingContent()
        }
      }

      Text(
        text = resolvedText,
        modifier = Modifier.align(Alignment.CenterVertically).padding(Prism.dimens.spacingXs),
        style =
          Prism.typography.caption.copy(
            fontSize = PrismTagConstants.LabelFontSize,
            lineHeight = PrismTagConstants.LabelLineHeight,
            letterSpacing = PrismTagConstants.LabelLetterSpacing,
          ),
        color = animatedContentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
