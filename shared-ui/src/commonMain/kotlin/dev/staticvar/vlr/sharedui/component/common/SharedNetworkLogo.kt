/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.size.Scale
import dev.staticvar.vlr.sharedui.image.softwareLogo
import kotlin.math.roundToInt

/** A borderless, freely sized logo with the same cached contrast treatment as list icons. */
@Composable
public fun SharedNetworkLogo(
  imageUrl: String,
  contentDescription: String?,
  background: Color,
  modifier: Modifier = Modifier,
  alignment: Alignment = Alignment.Center,
) {
  val context = LocalPlatformContext.current
  val decodeSize = with(LocalDensity.current) { 160.dp.roundToPx() }
  val request = remember(context, imageUrl, decodeSize) {
    ImageRequest.Builder(context).data(imageUrl).size(decodeSize).scale(Scale.FIT).softwareLogo().build()
  }
  val painter = rememberAsyncImagePainter(request)
  val state by painter.state.collectAsState()
  val image = (state as? AsyncImagePainter.State.Success)?.result?.image
  var bounds by remember { mutableStateOf(IntSize.Zero) }
  // Render only the fitted image bounds so an outline does not change its aspect ratio or alignment.
  val fittedSize = remember(image, bounds) {
    image?.takeIf { it.width > 0 && it.height > 0 }?.let {
      val scale = minOf(bounds.width.toFloat() / it.width, bounds.height.toFloat() / it.height)
      IntSize((it.width * scale).roundToInt(), (it.height * scale).roundToInt())
    } ?: IntSize.Zero
  }
  val displayPainter = rememberOutlinedLogoPainter(image, painter, fittedSize, background)
  Image(
    painter = displayPainter,
    contentDescription = contentDescription,
    modifier = modifier.onSizeChanged { bounds = it },
    alignment = alignment,
    contentScale = ContentScale.Fit,
  )
}

/** Shares asynchronous analysis, outlined bitmap caching, and original-image fallback between logo views. */
@Composable
internal fun rememberOutlinedLogoPainter(
  image: coil3.Image?,
  painter: Painter,
  pixelSize: IntSize,
  background: Color,
  enabled: Boolean = true,
): Painter {
  val source = remember(image) { image?.takeIf { it.shareable }?.let(::LogoSource) }
  val radiusPx = with(LocalDensity.current) { 1.dp.toPx() }
  val request = remember(source, pixelSize, radiusPx, background, enabled) {
    if (enabled && source != null && pixelSize.width > 0 && pixelSize.height > 0) {
      LogoTreatmentRequest(source, pixelSize, radiusPx, background)
    } else null
  }
  val treatment by rememberLogoTreatment(request, sharedLogoTreatments)
  return remember(treatment, painter, pixelSize) {
    (treatment as? LogoTreatment.Outlined)?.let { PreparedLogoPainter(it, pixelSize) } ?: painter
  }
}
