/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureabout.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant
import org.jetbrains.compose.resources.stringResource
import vlr.feature_about.generated.resources.Res
import vlr.feature_about.generated.resources.note_rate_google_play

@PrismPreview
@Composable
internal fun DeveloperNotePreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    DeveloperNoteScreen(onBack = {}, onContact = {}, onRate = {}, reviewLabel = stringResource(Res.string.note_rate_google_play))
  }
}

@PrismPreview
@Composable
internal fun DeveloperNoteContactPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    DeveloperNoteScreen(onBack = {}, onContact = {}, onRate = null, reviewLabel = "")
  }
}
