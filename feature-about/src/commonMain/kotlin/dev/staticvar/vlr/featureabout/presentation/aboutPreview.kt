/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureabout.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant

@PrismPreview
@Composable
internal fun AboutPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    AboutScreen(
      modifier = Modifier.fillMaxSize().background(Prism.color.background),
    )
  }
}

@PrismPreview
@Composable
internal fun AboutFeedbackPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    AboutFeedbackDialog(onDismiss = {}, onSubmit = { false })
  }
}
