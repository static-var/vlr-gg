@file:Suppress("CyclomaticComplexMethod")

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
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
  val visualState = rememberPrismTagVisualState(variant = variant, enabled = enabled)
  val resolvedText = text.trim().uppercase()

  PrismSurface(
    modifier = modifier,
    color = visualState.containerColor,
    shape = Prism.shapes.small,
    border = BorderStroke(Prism.dimens.strokeDefault, visualState.borderColor),
  ) {
    PrismTagContent(
      text = resolvedText,
      contentColor = visualState.contentColor,
      leadingContent = leadingContent,
    )
  }
}

@Composable
private fun rememberPrismTagVisualState(
  variant: PrismTagVariant,
  enabled: Boolean,
): PrismTagVisualState {
  val animation = Prism.anim.standard
  val containerColor by
    animateColorAsState(
      targetValue = tagContainerColor(variant = variant, enabled = enabled),
      animationSpec = animation.colorSpec(),
      label = "tag_container",
    )
  val contentColor by
    animateColorAsState(
      targetValue = tagContentColor(variant = variant, enabled = enabled),
      animationSpec = animation.colorSpec(),
      label = "tag_content",
    )
  val borderColor by
    animateColorAsState(
      targetValue = tagBorderColor(variant = variant, enabled = enabled),
      animationSpec = animation.colorSpec(),
      label = "tag_border",
    )

  return PrismTagVisualState(containerColor, contentColor, borderColor)
}

@Composable
private fun PrismTagContent(
  text: String,
  contentColor: Color,
  leadingContent: (@Composable () -> Unit)?,
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
      text = text,
      modifier = Modifier.align(Alignment.CenterVertically).padding(Prism.dimens.spacingXs),
      style =
        Prism.typography.caption.copy(
          fontSize = PrismTagConstants.LabelFontSize,
          lineHeight = PrismTagConstants.LabelLineHeight,
          letterSpacing = PrismTagConstants.LabelLetterSpacing,
        ),
      color = contentColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun tagContainerColor(
  variant: PrismTagVariant,
  enabled: Boolean,
): Color =
  when {
    !enabled -> Prism.color.surfaceDim
    variant == PrismTagVariant.Neutral -> Prism.color.surface
    variant == PrismTagVariant.Accent -> Prism.color.accentSubtle
    variant == PrismTagVariant.Success -> Prism.color.successContainer
    variant == PrismTagVariant.Warning -> Prism.color.warningContainer
    variant == PrismTagVariant.Danger -> Prism.color.dangerContainer
    else -> Prism.color.infoContainer
  }

@Composable
private fun tagContentColor(
  variant: PrismTagVariant,
  enabled: Boolean,
): Color =
  when {
    !enabled -> Prism.color.captionColor
    variant == PrismTagVariant.Neutral -> Prism.color.labelColor
    variant == PrismTagVariant.Accent -> Prism.color.accent
    variant == PrismTagVariant.Success -> Prism.color.success
    variant == PrismTagVariant.Warning -> Prism.color.warning
    variant == PrismTagVariant.Danger -> Prism.color.danger
    else -> Prism.color.info
  }

@Composable
private fun tagBorderColor(
  variant: PrismTagVariant,
  enabled: Boolean,
): Color =
  when {
    !enabled -> Prism.color.stroke
    variant == PrismTagVariant.Neutral -> Prism.color.stroke
    variant == PrismTagVariant.Accent -> Prism.color.accent
    variant == PrismTagVariant.Success -> Prism.color.success
    variant == PrismTagVariant.Warning -> Prism.color.warning
    variant == PrismTagVariant.Danger -> Prism.color.danger
    else -> Prism.color.info
  }

private data class PrismTagVisualState(
  val containerColor: Color,
  val contentColor: Color,
  val borderColor: Color,
)
