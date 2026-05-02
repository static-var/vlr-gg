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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.color.contentColorFor

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
 * @param content Surface content
 */
@Composable
public fun PrismSurface(
  modifier: Modifier = Modifier,
  color: Color = Prism.color.surface,
  brush: Brush? = null,
  contentColor: Color = contentColorFor(color),
  shape: Shape = RectangleShape,
  border: BorderStroke? = null,
  content: @Composable () -> Unit,
) {
  CompositionLocalProvider(LocalContentColor provides contentColor) {
    val backgroundModifier = if (brush != null) {
      Modifier.background(brush = brush, shape = shape)
    } else {
      Modifier.background(color = color, shape = shape)
    }

    Box(
      modifier =
      modifier
        .then(if (border != null) Modifier.border(border, shape) else Modifier)
        .then(backgroundModifier)
        .clip(shape),
    ) {
      content()
    }
  }
}
