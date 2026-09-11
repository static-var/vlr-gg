/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.spoilers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.staticvar.designsystem.prism.Prism

/** A score value or a non-interactive, accessible hidden-result icon. */
@Composable
public fun SpoilerScore(
  text: String,
  modifier: Modifier = Modifier,
  color: Color = Color.Unspecified,
  style: TextStyle = LocalTextStyle.current,
  textAlign: TextAlign? = null,
  maxLines: Int = Int.MAX_VALUE,
  overflow: TextOverflow = TextOverflow.Clip,
) {
  SpoilerContent(modifier = modifier) {
    Text(
      text = text,
      color = color,
      style = style,
      textAlign = textAlign,
      maxLines = maxLines,
      overflow = overflow,
    )
  }
}

@Composable
public fun SpoilerContent(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  if (LocalSpoilerMode.current.enabled) SpoilerHiddenIcon(modifier) else Box(modifier) { content() }
}

@Composable
public fun SpoilerHiddenIcon(modifier: Modifier = Modifier) {
  val color = Prism.color.labelColor
  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    Icon(
      imageVector = Prism.icons.preview,
      contentDescription = "Result hidden",
      tint = color,
      modifier = Modifier.size(Prism.dimens.iconM).closedEyeStroke(color),
    )
  }
}

internal fun Modifier.closedEyeStroke(color: Color): Modifier = drawWithContent {
  drawContent()
  drawLine(
    color = color,
    start = Offset(size.width * 0.1f, size.height * 0.1f),
    end = Offset(size.width * 0.9f, size.height * 0.9f),
    strokeWidth = size.width / 12f,
  )
}
