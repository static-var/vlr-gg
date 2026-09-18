/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import dev.staticvar.designsystem.component.icon.PrismIcon
import dev.staticvar.designsystem.component.icon.PrismIconSize
import dev.staticvar.designsystem.component.icon.PrismIconStyle
import dev.staticvar.designsystem.component.icon.PrismIconTint
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.sharedui.image.softwareLogo

/**
 * Shared app icon that downloads and displays image content from a URL.
 *
 * Network loading and cache behavior come from the app-wide Coil image loader installed by
 * `ProvideSharedImageLoader`. The Prism layer still owns the container, size, and tint treatment.
 * [conditionalOutline] outlines low-contrast logo edges; disable for portraits.
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
  imageModifier: Modifier = Modifier,
  conditionalOutline: Boolean = true,
  parentBackground: Color = Prism.color.background,
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

  val useConditionalOutline = conditionalOutline && size != PrismIconSize.Small && tint == PrismIconTint.None && contentScale == ContentScale.Fit
  val context = LocalPlatformContext.current
  val request = remember(context, imageUrl, useConditionalOutline) {
    ImageRequest.Builder(context).data(imageUrl).apply {
      if (useConditionalOutline) softwareLogo()
    }.build()
  }
  val painter = rememberAsyncImagePainter(model = request)
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

  val image = (painterState as? AsyncImagePainter.State.Success)?.result?.image
  val source = remember(image) { image?.takeIf { it.shareable }?.let(::LogoSource) }
  var pixelSize by remember { mutableStateOf(IntSize.Zero) }
  val radiusPx = with(LocalDensity.current) { 1.dp.toPx() }
  val background = style.containerColor.compositeOver(parentBackground)
  val treatmentRequest = remember(source, pixelSize, radiusPx, background, useConditionalOutline) {
    if (useConditionalOutline && source != null && pixelSize.width > 0 && pixelSize.height > 0) {
      LogoTreatmentRequest(source, pixelSize, radiusPx, background)
    } else null
  }
  val treatment by rememberLogoTreatment(treatmentRequest, sharedLogoTreatments)
  val displayPainter = remember(treatment, painter, pixelSize) {
    val outlined = treatment as? LogoTreatment.Outlined
    if (outlined != null) PreparedLogoPainter(outlined, pixelSize) else painter
  }

  PrismIcon(
    painter = displayPainter,
    contentDescription = contentDescription,
    modifier = modifier,
    size = size,
    style = style,
    contentScale = contentScale,
    tint = tint,
    imageModifier = if (useConditionalOutline) {
      imageModifier.onSizeChanged { pixelSize = it }
    } else imageModifier,
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
    color = when (tint) {
      PrismIconTint.Inverted -> style.contentColor
      PrismIconTint.None,
      PrismIconTint.Primary,
      PrismIconTint.Alt,
      -> style.containerColor
    },
    shape = Prism.shapes.small,
    border = style.border,
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

          PrismIconTint.Inverted -> style.containerColor
        },
      )
    }
  }
}

private val PrismIconSize.fallbackContainerSize: Dp
  get() = when (this) {
    PrismIconSize.Size16 -> 16.dp
    PrismIconSize.Size32 -> 32.dp
    PrismIconSize.Size40 -> 40.dp
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
