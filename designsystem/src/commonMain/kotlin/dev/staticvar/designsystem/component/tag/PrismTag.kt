/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
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

/**
 * Compact, non-interactive brutalist tag for status and metadata labels.
 *
 * The visual treatment is supplied by [PrismTagStyle], which owns token-backed color and border
 * decisions while this composable keeps the tag layout and content behavior.
 */
@Composable
public fun PrismTag(
  text: String,
  modifier: Modifier = Modifier,
  style: PrismTagStyle = PrismTagStyle.Neutral,
  enabled: Boolean = true,
  leadingContent: (@Composable () -> Unit)? = null,
) {
  val visualState = rememberPrismTagVisualState(style = style, enabled = enabled)
  val resolvedText = text.trim().uppercase()

  PrismSurface(
    modifier = modifier,
    color = visualState.containerColor,
    shape = Prism.shapes.small,
    border = visualState.border,
  ) {
    PrismTagContent(
      text = resolvedText,
      contentColor = visualState.contentColor,
      leadingContent = leadingContent,
    )
  }
}

@Composable
private fun rememberPrismTagVisualState(style: PrismTagStyle, enabled: Boolean): PrismTagVisualState {
  val animation = Prism.anim.standard
  val containerColor by
    animateColorAsState(
      targetValue = if (enabled) style.containerColor else style.disabledContainerColor,
      animationSpec = animation.colorSpec(),
      label = "tag_container",
    )
  val contentColor by
    animateColorAsState(
      targetValue = if (enabled) style.contentColor else style.disabledContentColor,
      animationSpec = animation.colorSpec(),
      label = "tag_content",
    )
  val borderColor by
    animateColorAsState(
      targetValue = if (enabled) style.borderColor else style.disabledBorderColor,
      animationSpec = animation.colorSpec(),
      label = "tag_border",
    )

  return PrismTagVisualState(
    containerColor = containerColor,
    contentColor = contentColor,
    border = BorderStroke(width = Prism.dimens.strokeDefault, color = borderColor),
  )
}

@Composable
private fun PrismTagContent(text: String, contentColor: Color, leadingContent: (@Composable () -> Unit)?) {
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

private data class PrismTagVisualState(val containerColor: Color, val contentColor: Color, val border: BorderStroke)
