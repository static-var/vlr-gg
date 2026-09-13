/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
@file:Suppress("LongParameterList")

package dev.staticvar.designsystem.component.surface

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import dev.staticvar.designsystem.component.frame.prismFrame
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.color.contentColorFor
import dev.staticvar.designsystem.prism.frame.PrismFrameTokens

/**
 * Prism surface component that automatically manages content color.
 *
 * When you set a [color], the appropriate content color is automatically
 * provided to child composables via [LocalContentColor]. Text and Icons
 * will use this color unless explicitly overridden.
 *
 * Example:
 * ```
 * PrismSurface(color = Prism.color.accent) {
 *   Text("I'm automatically white/light colored")
 * }
 *
 * PrismSurface(color = Prism.color.surface) {
 *   Text("I'm automatically primary text color")
 * }
 *
 * PrismSurface(brush = Brush.linearGradient(...)) {
 *   Text("Gradient background")
 * }
 * ```
 *
 * @param modifier Modifier to apply to the surface
 * @param color Background color of the surface (ignored if brush is provided)
 * @param brush Optional gradient brush for the background
 * @param contentColor Content color for children (auto-calculated if not provided)
 * @param shape Shape of the surface
 * @param border Optional border
 * @param frame Optional theme frame; surfaces are flat by default.
 * @param pressProgress Foreground displacement toward the shadow, between zero and one.
 * @param content Surface content
 */
@Composable
public fun PrismSurface(
  modifier: Modifier = Modifier,
  color: Color = Prism.color.surface,
  brush: Brush? = null,
  contentColor: Color = contentColorFor(color),
  shape: Shape = Prism.shapes.small,
  border: BorderStroke? = null,
  frame: PrismFrameTokens = PrismFrameTokens(),
  pressProgress: Float = 0f,
  content: @Composable () -> Unit,
) {
  PrismSurfaceContent(
    modifier = modifier,
    color = color,
    brush = brush,
    contentColor = contentColor,
    shape = shape,
    border = border,
    frame = frame,
    pressProgress = { pressProgress },
    content = content,
  )
}

@Composable
internal fun PrismSurface(
  modifier: Modifier = Modifier,
  color: Color = Prism.color.surface,
  brush: Brush? = null,
  contentColor: Color = contentColorFor(color),
  shape: Shape = Prism.shapes.small,
  border: BorderStroke? = null,
  frame: PrismFrameTokens = PrismFrameTokens(),
  pressProgress: State<Float>?,
  content: @Composable () -> Unit,
) {
  PrismSurfaceContent(
    modifier = modifier,
    color = color,
    brush = brush,
    contentColor = contentColor,
    shape = shape,
    border = border,
    frame = frame,
    pressProgress = { pressProgress?.value ?: 0f },
    content = content,
  )
}

@Composable
private fun PrismSurfaceContent(
  modifier: Modifier,
  color: Color,
  brush: Brush?,
  contentColor: Color,
  shape: Shape,
  border: BorderStroke?,
  frame: PrismFrameTokens,
  pressProgress: () -> Float,
  content: @Composable () -> Unit,
) {
  CompositionLocalProvider(LocalContentColor provides contentColor) {
    val backgroundModifier = if (brush != null) {
      Modifier.background(brush = brush, shape = shape)
    } else {
      Modifier.background(color = color, shape = shape)
    }

    val resolvedBorder = frame.border ?: border

    Box(
      modifier =
      modifier
        .prismFrame(frame, shape, pressProgress)
        .then(if (resolvedBorder != null) Modifier.border(resolvedBorder, shape) else Modifier)
        .then(backgroundModifier)
        .clip(shape),
    ) {
      content()
    }
  }
}
