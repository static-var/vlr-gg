package dev.staticvar.vlr.featurematches.presentation

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
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.domain.model.TeamPreview

@PrismPreview
@Composable
internal fun matchesOverviewPreview(
  @PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant,
) {
  PrismTheme(variant = variant) {
    matchesOverviewScreen(
      uiState =
        MatchesUiState(
          matches =
            listOf(
              sampleMatchPreview(id = "match-1", status = MatchStatus.LIVE),
              sampleMatchPreview(id = "match-2", status = MatchStatus.UPCOMING),
            ),
          selectedStatus = MatchStatusFilter.Live,
          isLoading = false,
        ),
      onFilterSelected = {},
      onMatchSelected = {},
      modifier = Modifier.fillMaxSize().background(Prism.color.background),
    )
  }
}

private fun sampleMatchPreview(id: String, status: MatchStatus): MatchPreview =
  MatchPreview(
    id = id,
    event = "Masters Bangkok",
    series = "Bo3",
    status = status,
    team1 = sampleTeamPreview(name = "FNATIC", score = 1),
    team2 = sampleTeamPreview(name = "Sentinels", score = 0),
    time = "14:00 CET",
    eventId = "event-1",
  )

private fun sampleTeamPreview(name: String, score: Int): TeamPreview =
  TeamPreview(
    id = name.lowercase(),
    name = name,
    region = "EMEA",
    img = "",
    score = score,
    isWinner = score > 0,
  )
