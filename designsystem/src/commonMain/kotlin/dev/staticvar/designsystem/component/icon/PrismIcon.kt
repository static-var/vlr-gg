/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.designsystem.component.icon

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism

/**
 * App icon primitive for fixed-size logos, avatars, and small visual anchors.
 *
 * The [style] controls the container and foreground treatment. [size] controls the outer icon box
 * using the Prism preset dimensions.
 */
@Composable
public fun PrismIcon(
  imageVector: ImageVector,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  size: PrismIconSize = PrismIconSize.Medium,
  style: PrismIconStyle = PrismIconStyle.Bordered,
  tint: PrismIconTint = PrismIconTint.Primary,
  imageModifier: Modifier = Modifier,
) {
  PrismIcon(
    painter = rememberVectorPainter(image = imageVector),
    contentDescription = contentDescription,
    modifier = modifier,
    size = size,
    style = style,
    tint = tint,
    imageModifier = imageModifier,
  )
}

@Composable
public fun PrismIcon(
  painter: Painter,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  size: PrismIconSize = PrismIconSize.Medium,
  style: PrismIconStyle = PrismIconStyle.Bordered,
  contentScale: ContentScale = ContentScale.Fit,
  tint: PrismIconTint = PrismIconTint.None,
  imageModifier: Modifier = Modifier,
) {
  PrismSurface(
    modifier = modifier.size(size.containerSize),
    color = tint.containerColor(style = style),
    shape = Prism.shapes.small,
    border = style.border,
  ) {
    Box(
      modifier = Modifier
        .size(size.containerSize)
        .clip(Prism.shapes.small)
        .padding(size.contentPadding),
      contentAlignment = Alignment.Center,
    ) {
      Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = imageModifier.size(size.contentSize),
        contentScale = contentScale,
        colorFilter = tint.contentColorFilter(style = style),
      )
    }
  }
}

/**
 * Tint mode for [PrismIcon].
 *
 * Use [Primary] for the style's normal foreground color, [Alt] for Prism's accent/purple color,
 * [Inverted] to fill the container with the normal foreground and tint content with the container
 * color, and [None] for full-color artwork such as team logos.
 */
@Immutable
public enum class PrismIconTint {
  None,
  Primary,
  Alt,
  Inverted,
}

@Composable
private fun PrismIconTint.containerColor(style: PrismIconStyle): Color = when (this) {
  PrismIconTint.Inverted -> style.contentColor
  PrismIconTint.None,
  PrismIconTint.Primary,
  PrismIconTint.Alt,
  -> style.containerColor
}

@Composable
private fun PrismIconTint.contentColorFilter(style: PrismIconStyle): ColorFilter? {
  val color = when (this) {
    PrismIconTint.None -> null
    PrismIconTint.Primary -> style.contentColor
    PrismIconTint.Alt -> Prism.color.accent
    PrismIconTint.Inverted -> style.containerColor
  }

  return color?.let(ColorFilter::tint)
}

@Immutable
public enum class PrismIconSize(internal val containerSize: Dp, internal val contentPadding: Dp) {
  Size16(containerSize = 16.dp, contentPadding = 2.dp),
  Size32(containerSize = 32.dp, contentPadding = 6.dp),
  Size40(containerSize = 40.dp, contentPadding = 7.dp),
  Size48(containerSize = 48.dp, contentPadding = 8.dp),
  Size64(containerSize = 64.dp, contentPadding = 10.dp),
  Size80(containerSize = 80.dp, contentPadding = 12.dp),
  ;

  internal val contentSize: Dp = containerSize - (contentPadding * 2)

  public companion object {
    public val Small: PrismIconSize = Size16
    public val Medium: PrismIconSize = Size32
    public val Large: PrismIconSize = Size48
    public val ExtraLarge: PrismIconSize = Size64
    public val Hero: PrismIconSize = Size80
  }
}
