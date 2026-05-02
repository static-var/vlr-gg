package dev.staticvar.vlr.featurematches.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonVariant
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardVariant
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.component.tag.PrismTagVariant
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.MapData
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.PreviousEncounter
import dev.staticvar.vlr.domain.model.TeamDetails
import org.koin.mp.KoinPlatform

@Composable
public fun MatchDetailsRoute(
  matchId: String,
  onBack: () -> Unit,
  onEventSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
  onPlayerSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val viewModel: MatchDetailsViewModel = remember(matchId) { KoinPlatform.getKoin().get<MatchDetailsViewModel>() }
  val uiState: MatchDetailsUiState by viewModel.uiState.collectAsState()

  LaunchedEffect(matchId) {
    viewModel.openMatch(matchId)
  }

  DisposableEffect(viewModel) {
    onDispose(viewModel::clear)
  }

  MatchDetailsScreen(
    uiState = uiState,
    onBack = onBack,
    onEventSelected = onEventSelected,
    onTeamSelected = onTeamSelected,
    onPlayerSelected = onPlayerSelected,
    modifier = modifier,
  )
}

@Composable
internal fun MatchDetailsScreen(
  uiState: MatchDetailsUiState,
  onBack: () -> Unit,
  onEventSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
  onPlayerSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val match = uiState.match

  Column(
    modifier = modifier.fillMaxSize().padding(Prism.dimens.spacingM),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    PrismScreenTitleBar(
      title = match?.event?.name ?: "Match details",
      subtitle = match?.event?.series ?: "Detailed match breakdown.",
      preLabel = "match",
      actions = {
        PrismButton(onClick = onBack, variant = PrismButtonVariant.Tertiary) {
          Text(text = "Back")
        }
      },
    )

    when {
      uiState.isLoading -> DetailStateMessage(text = "Loading match details…")
      uiState.errorMessage != null && match == null ->
        DetailStateMessage(text = uiState.errorMessage ?: "Unable to load match details.")
      match == null -> DetailStateMessage(text = "Match detail is unavailable.")
      else -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
        ) {
          item {
            MatchSummaryCard(
              match = match,
              onEventSelected = onEventSelected,
              onTeamSelected = onTeamSelected,
            )
          }
          if (match.matchData.isNotEmpty()) {
            item {
              PrismSectionTitle(title = "Maps", preLabel = "breakdown")
            }
            items(match.matchData, key = MapData::map) { map ->
              MapCard(
                map = map,
                onPlayerSelected = onPlayerSelected,
              )
            }
          }
          if (match.head2head.isNotEmpty()) {
            item {
              PrismSectionTitle(title = "Head to head", preLabel = "history")
            }
            items(match.head2head, key = PreviousEncounter::id) { encounter ->
              PrismCard(
                modifier = Modifier.fillMaxWidth(),
                variant = PrismCardVariant.Outlined,
                onClick = { },
              ) {
                Text(
                  text = encounter.teams.joinToString(" vs ") { it.name },
                  style = Prism.typography.cardTitle,
                  color = Prism.color.titleColor,
                )
                Text(
                  text = encounter.teams.joinToString(" • ") { team -> "${team.name} ${team.score ?: "-"}" },
                  modifier = Modifier.padding(top = Prism.dimens.spacingXs),
                  style = Prism.typography.bodySmall,
                  color = Prism.color.labelColor,
                )
              }
            }
          }
          val videos = match.videos.streams + match.videos.vods
          if (videos.isNotEmpty()) {
            item {
              PrismSectionTitle(title = "Streams & VODs", preLabel = "media")
            }
            items(videos, key = { it.name + it.url }) { video ->
              PrismCard(modifier = Modifier.fillMaxWidth(), variant = PrismCardVariant.Outlined) {
                Text(text = video.name, style = Prism.typography.cardTitle, color = Prism.color.titleColor)
                Text(
                  text = video.url,
                  modifier = Modifier.padding(top = Prism.dimens.spacingXs),
                  style = Prism.typography.bodySmall,
                  color = Prism.color.labelColor,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MatchSummaryCard(
  match: MatchDetails,
  onEventSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
) {
  PrismCard(
    modifier = Modifier.fillMaxWidth(),
    variant = PrismCardVariant.Outlined,
  ) {
    PrismTag(
      text = match.event.status ?: "Unknown",
      variant = if (match.event.status.equals("live", ignoreCase = true)) PrismTagVariant.Danger else PrismTagVariant.Info,
    )
    Text(
      text = match.score.ifBlank { "Score pending" },
      modifier = Modifier.padding(top = Prism.dimens.spacingS),
      style = Prism.typography.sectionTitle,
      color = Prism.color.titleColor,
    )
    Text(
      text = "${match.event.name} • ${match.event.series}",
      modifier = Modifier
        .padding(top = Prism.dimens.spacingXs)
        .clickable { onEventSelected(match.event.id) },
      style = Prism.typography.bodySmall,
      color = Prism.color.accent,
    )
    Row(
      modifier = Modifier.fillMaxWidth().padding(top = Prism.dimens.spacingM),
      horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
    ) {
      match.teams.forEach { team ->
        TeamSummaryCard(
          modifier = Modifier.weight(1f),
          team = team,
          onTeamSelected = onTeamSelected,
        )
      }
    }
    if (match.note.isNotBlank()) {
      Text(
        text = match.note,
        modifier = Modifier.padding(top = Prism.dimens.spacingM),
        style = Prism.typography.bodySmall,
        color = Prism.color.labelColor,
      )
    }
  }
}

@Composable
private fun TeamSummaryCard(
  team: TeamDetails,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  PrismCard(
    modifier = modifier.fillMaxWidth(),
    variant = PrismCardVariant.Filled,
    onClick = {
      val teamId: String = team.id ?: return@PrismCard
      onTeamSelected(teamId)
    },
  ) {
    Text(text = team.name, style = Prism.typography.cardTitle, color = Prism.color.titleColor)
    Text(
      text = "${team.score ?: "-"} • ${team.region}",
      modifier = Modifier.padding(top = Prism.dimens.spacingXs),
      style = Prism.typography.bodySmall,
      color = Prism.color.labelColor,
    )
  }
}

@Composable
private fun MapCard(
  map: MapData,
  onPlayerSelected: (String) -> Unit,
) {
  PrismCard(
    modifier = Modifier.fillMaxWidth(),
    variant = PrismCardVariant.Outlined,
  ) {
    Text(text = map.map, style = Prism.typography.cardTitle, color = Prism.color.titleColor)
    Text(
      text = map.teams.joinToString(" • ") { team -> "${team.name} ${team.score ?: "-"}" },
      modifier = Modifier.padding(top = Prism.dimens.spacingXs),
      style = Prism.typography.bodySmall,
      color = Prism.color.labelColor,
    )
    map.members.take(6).forEach { player ->
      Text(
        text = "${player.name} • ${player.kills}/${player.deaths}/${player.assists} • ${player.rating}",
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = Prism.dimens.spacingXs)
          .clickable { onPlayerSelected(player.playerId) },
        style = Prism.typography.bodySmall,
        color = Prism.color.bodyColor,
      )
    }
  }
}

@Composable
private fun DetailStateMessage(
  text: String,
) {
  Text(
    text = text,
    modifier = Modifier.fillMaxWidth().padding(Prism.dimens.spacingM),
    style = Prism.typography.bodyLarge,
    color = Prism.color.labelColor,
  )
}
