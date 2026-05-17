/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.event.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.domain.model.EventStatus

@PrismPreview
@Composable
internal fun EventPreviewItemPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(Prism.color.background)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      EventPreviewItem(eventPreview = sampleEventPreview(status = EventStatus.ONGOING, isFavorite = true))
      EventPreviewItem(eventPreview = sampleEventPreview(status = EventStatus.UPCOMING))
      EventPreviewItem(eventPreview = sampleEventPreview(status = EventStatus.COMPLETED))
    }
  }
}

private fun sampleEventPreview(status: EventStatus, isFavorite: Boolean = false): EventPreview = EventPreview(
  id = "2283",
  title = "Valorant Champions 2025",
  status = status,
  prize = "$" + "2,250,000",
  dates = "Sep 12 - Oct 5",
  region = "Global",
  logoUrl = "",
  isFavorite = isFavorite,
)
