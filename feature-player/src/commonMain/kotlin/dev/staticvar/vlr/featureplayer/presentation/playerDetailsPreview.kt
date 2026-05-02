/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureplayer.presentation

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
import dev.staticvar.vlr.domain.model.PlayerAgentStat
import dev.staticvar.vlr.domain.model.PlayerInfo
import dev.staticvar.vlr.domain.model.PlayerTeam

@PrismPreview
@Composable
internal fun PlayerDetailsPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    PlayerDetailsScreen(
      uiState = PlayerDetailsUiState(player = samplePlayerInfo(), isLoading = false),
      onBack = {},
      onTeamSelected = {},
      modifier = Modifier.fillMaxSize().background(Prism.color.background),
    )
  }
}

private fun samplePlayerInfo(): PlayerInfo = PlayerInfo(
  id = "player-1",
  name = "Jake Howlett",
  alias = "Boaster",
  realName = "Jake Howlett",
  country = "UK",
  imageUrl = "",
  twitterUrl = null,
  twitchUrl = null,
  totalWinnings = 412500.0,
  currentTeam = PlayerTeam(id = "fnc", name = "FNATIC", logoUrl = "", isCurrent = true),
  pastTeams = listOf(PlayerTeam(id = "sumn", name = "SUMN FC", logoUrl = "", isCurrent = false)),
  agentStats =
  listOf(
    PlayerAgentStat(
      agentName = "Omen",
      agentImageUrl = "",
      usageCount = 12,
      usagePercent = 62.0,
      roundsPlayed = 284,
      rating = 1.12,
      acs = 221.0,
      kdRatio = 1.08,
      adr = 146.0,
      kast = 74.0,
      kpr = 0.73,
      apr = 0.39,
      fkpr = 0.11,
      fdpr = 0.08,
      kills = 207,
      deaths = 191,
      assists = 111,
      firstKills = 31,
      firstDeaths = 22,
    ),
  ),
)
