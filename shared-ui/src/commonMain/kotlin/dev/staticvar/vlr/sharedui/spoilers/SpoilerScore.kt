/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.spoilers

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.unit.Dp
import dev.staticvar.designsystem.prism.Prism
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.shared_result_hidden

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
  hiddenIconSize: Dp = Prism.dimens.iconM,
) {
  SpoilerContent(modifier = modifier, hiddenIconSize = hiddenIconSize) {
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
public fun SpoilerContent(
  modifier: Modifier = Modifier,
  hiddenIconSize: Dp = Prism.dimens.iconM,
  content: @Composable () -> Unit,
) {
  val animation = Prism.anim.standard
  AnimatedContent(
    targetState = LocalSpoilerMode.current.enabled,
    modifier = modifier,
    contentAlignment = Alignment.Center,
    transitionSpec = {
      val enter = fadeIn(animation.floatSpec()) + scaleIn(animation.floatSpec(), initialScale = 0.85f)
      val exit = fadeOut(animation.floatSpec()) + scaleOut(animation.floatSpec(), targetScale = 0.85f)
      (enter togetherWith exit).using(
        SizeTransform { _, _ -> tween(durationMillis = animation.durationMillis, easing = animation.easing) },
      )
    },
    label = "spoiler_score",
  ) { hidden ->
    if (hidden) SpoilerHiddenIcon(modifier = Modifier.size(hiddenIconSize)) else content()
  }
}

@Composable
public fun SpoilerHiddenIcon(modifier: Modifier = Modifier) {
  val color = Prism.color.labelColor
  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    Icon(
      imageVector = Prism.icons.preview,
      contentDescription = stringResource(Res.string.shared_result_hidden),
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
