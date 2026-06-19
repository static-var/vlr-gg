/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import dev.staticvar.designsystem.component.icon.PrismIcon
import dev.staticvar.designsystem.component.icon.PrismIconSize
import dev.staticvar.designsystem.component.icon.PrismIconStyle
import dev.staticvar.designsystem.component.icon.PrismIconTint
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism

/**
 * Shared app icon that downloads and displays image content from a URL.
 *
 * Network loading and cache behavior come from the app-wide Coil image loader installed by
 * `ProvideSharedImageLoader`. The Prism layer still owns the container, size, and tint treatment.
 */
@Composable
public fun SharedNetworkIcon(
  imageUrl: String?,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  size: PrismIconSize = PrismIconSize.Medium,
  style: PrismIconStyle = PrismIconStyle.Bordered,
  contentScale: ContentScale = ContentScale.Fit,
  tint: PrismIconTint = PrismIconTint.None,
) {
  if (imageUrl.isNullOrBlank()) {
    SharedNetworkIconFallback(
      text = sharedNetworkIconFallbackText(contentDescription),
      modifier = modifier,
      size = size,
      style = style,
      tint = tint,
    )
    return
  }

  val painter = rememberAsyncImagePainter(model = imageUrl)
  val painterState: AsyncImagePainter.State by painter.state.collectAsState()
  if (painterState is AsyncImagePainter.State.Error) {
    SharedNetworkIconFallback(
      text = sharedNetworkIconFallbackText(contentDescription),
      modifier = modifier,
      size = size,
      style = style,
      tint = tint,
    )
    return
  }

  PrismIcon(
    painter = painter,
    contentDescription = contentDescription,
    modifier = modifier,
    size = size,
    style = style,
    contentScale = contentScale,
    tint = tint,
  )
}

@Composable
private fun SharedNetworkIconFallback(
  text: String,
  modifier: Modifier,
  size: PrismIconSize,
  style: PrismIconStyle,
  tint: PrismIconTint,
) {
  val containerSize = size.fallbackContainerSize
  PrismSurface(
    modifier = modifier.size(containerSize),
    color = style.containerColor,
    shape = Prism.shapes.small,
    border = style.border ?: BorderStroke(width = Prism.dimens.strokeDefault, color = Prism.color.stroke),
  ) {
    Box(modifier = Modifier.size(containerSize), contentAlignment = Alignment.Center) {
      Text(
        text = text,
        style = Prism.typography.label,
        color = when (tint) {
          PrismIconTint.Alt -> Prism.color.accent
          PrismIconTint.None,
          PrismIconTint.Primary,
          -> style.contentColor
        },
      )
    }
  }
}

private val PrismIconSize.fallbackContainerSize: Dp
  get() = when (this) {
    PrismIconSize.Size16 -> 16.dp
    PrismIconSize.Size32 -> 32.dp
    PrismIconSize.Size48 -> 48.dp
    PrismIconSize.Size64 -> 64.dp
    PrismIconSize.Size80 -> 80.dp
  }

internal fun sharedNetworkIconFallbackText(contentDescription: String?): String {
  val words = contentDescription
    ?.trim()
    ?.split(Regex("\\s+"))
    ?.filter(String::isNotBlank)
    .orEmpty()

  return words
    .take(2)
    .mapNotNull { word -> word.firstOrNull(Char::isLetterOrDigit)?.uppercaseChar() }
    .joinToString(separator = "")
    .ifBlank { "?" }
}
