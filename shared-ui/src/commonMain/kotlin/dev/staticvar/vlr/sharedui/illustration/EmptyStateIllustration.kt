/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.illustration

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.prism.Prism

/** Artwork only. The hosting screen supplies the message and any recovery action below it. */
public enum class EmptyStateArtwork {
  NoLiveMatches,
  NoInternet,
  UnknownError,
  NoLiveEvents,
  NoFavorites,
}

/**
 * A portrait esports illustration, drawn as a 360 × 460 vector in the current Prism palette.
 *
 * Allocate about 70% of the available content height with the caller's [modifier], for example
 * `Modifier.fillMaxWidth().fillMaxHeight(0.7f)` inside a bounded Box. The image fits that space without
 * cropping or distortion. The default minimum size is 240 × 300 dp, subject to parent constraints.
 *
 * Leave [contentDescription] null when the screen's message describes the state. Supply a localized
 * description only when the artwork conveys information that is absent from the message.
 */
@Composable
public fun EmptyStateIllustration(
  artwork: EmptyStateArtwork,
  modifier: Modifier = Modifier,
  contentDescription: String? = null,
) {
  val palette = Prism.color
  val vector = remember(artwork, palette) {
    val colors = IllustrationColors(palette)
    when (artwork) {
      EmptyStateArtwork.NoLiveMatches -> noLiveMatchesVector(colors)
      EmptyStateArtwork.NoInternet -> noInternetVector(colors)
      EmptyStateArtwork.UnknownError -> unknownErrorVector(colors)
      EmptyStateArtwork.NoLiveEvents -> noLiveEventsVector(colors)
      EmptyStateArtwork.NoFavorites -> noFavoritesVector(colors)
    }
  }
  Image(
    imageVector = vector,
    contentDescription = contentDescription,
    modifier = modifier.sizeIn(minWidth = 240.dp, minHeight = 300.dp),
    contentScale = ContentScale.Fit,
  )
}
