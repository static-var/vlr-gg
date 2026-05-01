package dev.staticvar.vlr.featureevents.presentation

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
import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.domain.model.EventMatch
import dev.staticvar.vlr.domain.model.EventMatchTeam
import dev.staticvar.vlr.domain.model.EventPrize
import dev.staticvar.vlr.domain.model.EventPrizeTeam
import dev.staticvar.vlr.domain.model.EventStanding
import dev.staticvar.vlr.domain.model.EventStatus
import dev.staticvar.vlr.domain.model.EventTeam

@PrismPreview
@Composable
internal fun eventDetailsPreview(
  @PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant,
) {
  PrismTheme(variant = variant) {
    eventDetailsScreen(
      uiState = EventDetailsUiState(event = sampleEventDetails(), isLoading = false),
      section = EventDetailSection.Matches,
      onSectionSelected = {},
      onBack = {},
      onMatchSelected = {},
      onTeamSelected = {},
      modifier = Modifier.fillMaxSize().background(Prism.color.background),
    )
  }
}

private fun sampleEventDetails(): EventDetails =
  EventDetails(
    id = "event-1",
    title = "Masters Bangkok",
    subtitle = "Playoffs",
    status = EventStatus.ONGOING,
    prize = "$" + "250k",
    dates = "Mar 8 - Mar 16",
    region = "Global",
    logoUrl = "",
    prizes =
      listOf(
        EventPrize(
          position = "1st",
          prize = "$" + "100k",
          team = EventPrizeTeam(id = "fnc", name = "FNATIC", logoUrl = "", country = "EU"),
        ),
      ),
    teams =
      listOf(
        EventTeam(id = "fnc", name = "FNATIC", logoUrl = "", seed = "#1"),
        EventTeam(id = "sen", name = "Sentinels", logoUrl = "", seed = "#2"),
      ),
    matches =
      listOf(
        EventMatch(
          matchId = "match-1",
          time = "14:00 CET",
          date = "Mar 8",
          eta = "Live now",
          status = "LIVE",
          teams =
            listOf(
              EventMatchTeam(name = "FNATIC", region = "EMEA", score = 1),
              EventMatchTeam(name = "Sentinels", region = "Americas", score = 0),
            ),
          round = "Upper Final",
          stage = "Playoffs",
        ),
      ),
    standings =
      listOf(
        EventStanding(
          teamName = "FNATIC",
          teamLogoUrl = "",
          teamCountry = "EU",
          groupName = "Playoffs",
          wins = 2,
          losses = 0,
          ties = 0,
          mapDifference = 3,
          roundDifference = 19,
          roundDelta = 19,
        ),
      ),
  )
