/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.rememberAsyncImagePainter
import dev.staticvar.designsystem.component.icon.PrismIcon
import dev.staticvar.designsystem.component.icon.PrismIconSize
import dev.staticvar.designsystem.component.icon.PrismIconStyle
import dev.staticvar.designsystem.component.icon.PrismIconTint

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
  PrismIcon(
    painter = rememberAsyncImagePainter(model = imageUrl),
    contentDescription = contentDescription,
    modifier = modifier,
    size = size,
    style = style,
    contentScale = contentScale,
    tint = tint,
  )
}
