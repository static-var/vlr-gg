/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.tag

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism

private object PrismTagConstants {
  val ExtraHorizontalPadding = 2.dp
  val LabelFontSize = 10.sp
  val LabelLineHeight = 12.sp
  val LabelLetterSpacing = 0.4.sp
}

/**
 * Compact, non-interactive tag for status and metadata labels.
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
  val containerColor = if (enabled) style.containerColor else style.disabledContainerColor
  val contentColor = if (enabled) style.contentColor else style.disabledContentColor
  val borderColor = if (enabled) style.borderColor else style.disabledBorderColor
  val baseFrame = style.frame
  val borderWidth = style.border.width
  val frame = remember(baseFrame, borderWidth, borderColor) {
    baseFrame.copy(border = BorderStroke(width = borderWidth, color = borderColor))
  }
  val resolvedText = remember(text) { text.trim().uppercase() }

  PrismSurface(
    modifier = modifier,
    color = containerColor,
    shape = Prism.shapes.small,
    frame = frame,
  ) {
    PrismTagContent(
      text = resolvedText,
      contentColor = contentColor,
      leadingContent = leadingContent,
    )
  }
}

@Composable
private fun PrismTagContent(text: String, contentColor: Color, leadingContent: (@Composable () -> Unit)?) {
  val caption = Prism.typography.caption
  val textStyle = remember(caption) {
    caption.copy(
      fontSize = PrismTagConstants.LabelFontSize,
      lineHeight = PrismTagConstants.LabelLineHeight,
      letterSpacing = PrismTagConstants.LabelLetterSpacing,
    )
  }
  Row(
    modifier = Modifier.padding(horizontal = PrismTagConstants.ExtraHorizontalPadding),
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
      style = textStyle,
      color = contentColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}
