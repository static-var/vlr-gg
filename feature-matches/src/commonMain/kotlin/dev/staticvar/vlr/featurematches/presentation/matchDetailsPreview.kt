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
import dev.staticvar.vlr.domain.model.AgentInfo
import dev.staticvar.vlr.domain.model.EventInfo
import dev.staticvar.vlr.domain.model.MapData
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.MatchVideos
import dev.staticvar.vlr.domain.model.PlayerStats
import dev.staticvar.vlr.domain.model.PreviousEncounter
import dev.staticvar.vlr.domain.model.RoundInfo
import dev.staticvar.vlr.domain.model.RoundSide
import dev.staticvar.vlr.domain.model.RoundWinType
import dev.staticvar.vlr.domain.model.RoundWinner
import dev.staticvar.vlr.domain.model.TeamDetails
import dev.staticvar.vlr.domain.model.TeamPreview
import dev.staticvar.vlr.domain.model.VideoReference

@PrismPreview
@Composable
internal fun MatchDetailsPreview(
  @PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant,
) {
  PrismTheme(variant = variant) {
    MatchDetailsScreen(
      uiState = MatchDetailsUiState(match = sampleMatchDetails(), isLoading = false),
      onBack = {},
      onEventSelected = {},
      onTeamSelected = {},
      onPlayerSelected = {},
      modifier = Modifier.fillMaxSize().background(Prism.color.background),
    )
  }
}

private fun sampleMatchDetails(): MatchDetails =
  MatchDetails(
    id = "match-1",
    event =
      EventInfo(
        id = "event-1",
        name = "Masters Bangkok",
        series = "Bo3",
        stage = "Upper Final",
        img = "",
        date = "Mar 8",
        patch = "10.04",
        status = "LIVE",
      ),
    head2head =
      listOf(
        PreviousEncounter(
          id = "prev-1",
          teams =
            listOf(
              TeamPreview(id = "fnc", name = "FNATIC", region = "EMEA", img = "", score = 2, isWinner = true),
              TeamPreview(id = "sen", name = "Sentinels", region = "Americas", img = "", score = 1, isWinner = false),
            ),
        ),
      ),
    note = "Winner advances straight to the grand final.",
    score = "1 : 0",
    teams =
      listOf(
        TeamDetails(id = "fnc", name = "FNATIC", region = "EMEA", img = "", score = 1, isWinner = true),
        TeamDetails(id = "sen", name = "Sentinels", region = "Americas", img = "", score = 0, isWinner = false),
      ),
    bans = listOf("Bind", "Abyss"),
    videos =
      MatchVideos(
        streams = listOf(VideoReference(name = "Official Stream", url = "twitch.tv/valorant")),
        vods = listOf(VideoReference(name = "Map 1 VOD", url = "youtube.com/watch?v=123")),
      ),
    matchData =
      listOf(
        MapData(
          map = "Lotus",
          members =
            listOf(
              PlayerStats(
                playerId = "player-1",
                name = "Boaster",
                team = "FNATIC",
                acs = 231,
                adr = 152,
                kills = 18,
                deaths = 12,
                assists = 7,
                kast = 78,
                firstKills = 3,
                firstDeaths = 1,
                firstKillsDiff = 2,
                hsPercent = 27,
                rating = 1.18f,
                agents = listOf(AgentInfo(name = "Omen", img = "")),
              ),
            ),
          teams =
            listOf(
              TeamDetails(id = "fnc", name = "FNATIC", region = "EMEA", img = "", score = 13, isWinner = true),
              TeamDetails(id = "sen", name = "Sentinels", region = "Americas", img = "", score = 9, isWinner = false),
            ),
          rounds =
            listOf(
              RoundInfo(
                roundNo = 1,
                score = "1-0",
                winner = RoundWinner.TEAM1,
                side = RoundSide.ATTACK,
                winType = RoundWinType.ELIMINATION,
              ),
            ),
        ),
      ),
    mapCount = 3,
  )
