/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match.overview

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
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.domain.model.TeamPreview

@PrismPreview
@Composable
internal fun MatchPreviewItemPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(Prism.color.background)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      MatchPreviewItem(matchPreview = sampleMatchPreview(status = MatchStatus.LIVE, isFavorite = true))
      MatchPreviewItem(matchPreview = sampleMatchPreview(status = MatchStatus.UPCOMING))
      MatchPreviewItem(matchPreview = sampleMatchPreview(status = MatchStatus.COMPLETED, isFavorite = true))
      MatchPreviewItem(matchPreview = sampleMatchPreview(status = MatchStatus.UPCOMING))
    }
  }
}

private fun sampleMatchPreview(status: MatchStatus, isFavorite: Boolean = false): MatchPreview = MatchPreview(
  id = "542270",
  event = "Valorant Champions 2025",
  series = "Playoffs-Upper Final",
  status = status,
  team1 = sampleTeamPreview(
    id = "2593",
    name = "FNATIC",
    score = if (status == MatchStatus.UPCOMING) null else 0,
    isWinner = if (status == MatchStatus.COMPLETED) false else null,
  ),
  team2 = sampleTeamPreview(
    id = "1034",
    name = "NRG",
    score = if (status == MatchStatus.UPCOMING) null else 2,
    isWinner = status == MatchStatus.COMPLETED,
  ),
  time = "2025-10-03T11:00:00Z",
  eventId = "2283",
  isFavorite = isFavorite,
)

private fun sampleTeamPreview(id: String, name: String, score: Int?, isWinner: Boolean?): TeamPreview = TeamPreview(
  id = id,
  name = name,
  region = "",
  img = "",
  score = score,
  isWinner = isWinner,
)
