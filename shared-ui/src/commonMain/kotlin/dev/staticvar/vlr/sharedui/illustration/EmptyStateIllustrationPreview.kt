/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.illustration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant

@PrismPreview
@Composable
internal fun NoLiveMatchesIllustrationPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  IllustrationPreview(EmptyStateArtwork.NoLiveMatches, variant)
}

@PrismPreview
@Composable
internal fun NoInternetIllustrationPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  IllustrationPreview(EmptyStateArtwork.NoInternet, variant)
}

@PrismPreview
@Composable
internal fun UnknownErrorIllustrationPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  IllustrationPreview(EmptyStateArtwork.UnknownError, variant)
}

@PrismPreview
@Composable
internal fun NoLiveEventsIllustrationPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  IllustrationPreview(EmptyStateArtwork.NoLiveEvents, variant)
}

@Composable
private fun IllustrationPreview(artwork: EmptyStateArtwork, variant: PrismVariant) {
  PrismTheme(variant = variant) {
    Box(Modifier.fillMaxSize().background(Prism.color.background), contentAlignment = Alignment.TopCenter) {
      EmptyStateIllustration(artwork, Modifier.fillMaxWidth().fillMaxHeight(0.7f))
    }
  }
}
