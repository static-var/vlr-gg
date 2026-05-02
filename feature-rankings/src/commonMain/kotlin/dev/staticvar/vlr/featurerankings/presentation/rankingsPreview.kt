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
import dev.staticvar.vlr.domain.model.RegionalRanking
import dev.staticvar.vlr.domain.model.TeamRanking

@PrismPreview
@Composable
internal fun RankingsPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    RankingsScreen(
      uiState =
      RankingsUiState(
        regions =
        listOf(
          RegionalRanking(
            region = "EMEA",
            teams =
            listOf(
              TeamRanking(
                teamId = "fnc",
                teamName = "FNATIC",
                teamLogo = "",
                country = "EU",
                rank = 1,
                points = "100",
              ),
              TeamRanking(
                teamId = "vit",
                teamName = "Vitality",
                teamLogo = "",
                country = "EU",
                rank = 2,
                points = "88",
              ),
            ),
          ),
        ),
        selectedRegion = "EMEA",
        isLoading = false,
      ),
      onRegionSelected = {},
      onTeamSelected = {},
      modifier = Modifier.fillMaxSize().background(Prism.color.background),
    )
  }
}
