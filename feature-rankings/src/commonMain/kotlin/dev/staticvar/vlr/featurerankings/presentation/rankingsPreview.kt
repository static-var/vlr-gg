/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurerankings.presentation

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
import dev.staticvar.vlr.domain.model.RankingRegion
import dev.staticvar.vlr.domain.model.TeamRanking

@PrismPreview
@Composable
internal fun RankingsPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  RankingsPreviewContent(variant, selectedRegion = null)
}

@PrismPreview
@Composable
internal fun RegionalRankingsPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  RankingsPreviewContent(variant, selectedRegion = RankingRegion.Emea)
}

@PrismPreview
@Composable
internal fun EmptyRegionalRankingsPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  RankingsPreviewContent(variant, selectedRegion = RankingRegion.China)
}

@Composable
private fun RankingsPreviewContent(variant: PrismVariant, selectedRegion: RankingRegion?) {
  PrismTheme(variant = variant) {
    RankingsScreen(
      uiState =
      RankingsUiState(
        teams =
        listOf(
          TeamRanking(
            teamId = "1034",
            teamName = "NRG",
            teamLogo = "",
            country = "United States",
            rank = 1,
            elo = 1834.6,
            wins = 31,
            losses = 10,
            isFavorite = true,
            region = RankingRegion.Americas,
          ),
          TeamRanking(
            teamId = "2593",
            teamName = "FNATIC",
            teamLogo = "",
            country = "Europe",
            rank = 2,
            elo = 1802.4,
            wins = 27,
            losses = 11,
            region = RankingRegion.Emea,
          ),
        ).filter { selectedRegion == null || it.region == selectedRegion },
        isLoading = false,
        view = if (selectedRegion == null) RankingsView.Explore else RankingsView.Regional,
        selectedRegion = selectedRegion,
      ),
      onTeamSelected = {},
      modifier = Modifier.fillMaxSize().background(Prism.color.background),
    )
  }
}
