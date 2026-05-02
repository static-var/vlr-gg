package dev.staticvar.vlr.featureevents.presentation

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonVariant
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardVariant
import dev.staticvar.designsystem.component.navigation.PrismTab
import dev.staticvar.designsystem.component.navigation.PrismTabs
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.component.tag.PrismTagVariant
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.domain.model.EventMatch
import dev.staticvar.vlr.domain.model.EventStanding
import dev.staticvar.vlr.domain.model.EventStatus
import org.koin.mp.KoinPlatform

@Composable
public fun EventDetailsRoute(
  eventId: String,
  onBack: () -> Unit,
  onMatchSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val viewModel: EventDetailsViewModel = remember(eventId) { KoinPlatform.getKoin().get<EventDetailsViewModel>() }
  val uiState: EventDetailsUiState by viewModel.uiState.collectAsState()
  var section: EventDetailSection by rememberSaveable { mutableStateOf(EventDetailSection.Matches) }

  LaunchedEffect(eventId) {
    viewModel.openEvent(eventId)
  }

  DisposableEffect(viewModel) {
    onDispose(viewModel::clear)
  }

  EventDetailsScreen(
    uiState = uiState,
    section = section,
    onSectionSelected = { section = it },
    onBack = onBack,
    onMatchSelected = onMatchSelected,
    onTeamSelected = onTeamSelected,
    modifier = modifier,
  )
}

@Composable
internal fun EventDetailsScreen(
  uiState: EventDetailsUiState,
  section: EventDetailSection,
  onSectionSelected: (EventDetailSection) -> Unit,
  onBack: () -> Unit,
  onMatchSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val event = uiState.event

  Column(
    modifier = modifier.fillMaxSize().padding(Prism.dimens.spacingM),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    PrismScreenTitleBar(
      title = event?.title ?: "Tournament details",
      subtitle = event?.subtitle?.ifBlank { event.dates } ?: "Event breakdown",
      preLabel = "event",
      actions = {
        PrismButton(onClick = onBack, variant = PrismButtonVariant.Tertiary) {
          Text(text = "Back")
        }
      },
    )

    when {
      uiState.isLoading -> EventDetailStateMessage(text = "Loading tournament details…")
      uiState.errorMessage != null && event == null ->
        EventDetailStateMessage(text = uiState.errorMessage ?: "Unable to load event details.")
      event == null -> EventDetailStateMessage(text = "Tournament detail is unavailable.")
      else -> {
        EventHeaderCard(event = event, onTeamSelected = onTeamSelected)
        PrismTabs(
          tabs = EventDetailSection.entries.map { PrismTab(id = it.name, label = it.name) },
          selectedTabId = section.name,
          onTabSelected = { onSectionSelected(EventDetailSection.valueOf(it.id)) },
        )
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
        ) {
          when (section) {
            EventDetailSection.Matches -> {
              if (event.matches.isEmpty()) {
                item { EventDetailStateMessage(text = "No matches published yet.") }
              } else {
                item { PrismSectionTitle(title = "Matches", preLabel = "schedule") }
                items(event.matches, key = EventMatch::matchId) { match ->
                  PrismCard(
                    modifier = Modifier.fillMaxWidth(),
                    variant = PrismCardVariant.Outlined,
                    onClick = { onMatchSelected(match.matchId) },
                  ) {
                    Text(
                      text = match.teams.joinToString(" vs ") { it.name },
                      style = Prism.typography.cardTitle,
                      color = Prism.color.titleColor,
                    )
                    Text(
                      text = "${match.stage} • ${match.round} • ${match.status}",
                      modifier = Modifier.padding(top = Prism.dimens.spacingXs),
                      style = Prism.typography.bodySmall,
                      color = Prism.color.labelColor,
                    )
                    Text(
                      text = listOfNotNull(match.eta, "${match.date} ${match.time}".trim()).joinToString(" • "),
                      modifier = Modifier.padding(top = Prism.dimens.spacingXs),
                      style = Prism.typography.label,
                      color = Prism.color.bodyColor,
                    )
                  }
                }
              }
            }

            EventDetailSection.Standings -> {
              if (event.standings.isEmpty()) {
                item { EventDetailStateMessage(text = "No standings available yet.") }
              } else {
                item { PrismSectionTitle(title = "Standings", preLabel = "table") }
                items(event.standings, key = EventStanding::teamName) { standing ->
                  PrismCard(modifier = Modifier.fillMaxWidth(), variant = PrismCardVariant.Outlined) {
                    Text(
                      text = standing.teamName,
                      style = Prism.typography.cardTitle,
                      color = Prism.color.titleColor,
                    )
                    Text(
                      text = "${standing.teamCountry} • ${standing.groupName ?: "Open bracket"}",
                      modifier = Modifier.padding(top = Prism.dimens.spacingXs),
                      style = Prism.typography.bodySmall,
                      color = Prism.color.labelColor,
                    )
                    Text(
                      text = "${standing.wins}-${standing.losses}-${standing.ties} • RD ${standing.roundDifference}",
                      modifier = Modifier.padding(top = Prism.dimens.spacingXs),
                      style = Prism.typography.label,
                      color = Prism.color.bodyColor,
                    )
                  }
                }
              }
            }

            EventDetailSection.Prizes -> {
              if (event.prizes.isEmpty()) {
                item { EventDetailStateMessage(text = "Prize breakdown unavailable.") }
              } else {
                item { PrismSectionTitle(title = "Prizes", preLabel = "placements") }
                items(event.prizes, key = { it.position + it.prize }) { prize ->
                  val prizeTeam = prize.team
                  val prizeTeamId = prizeTeam?.id
                  PrismCard(modifier = Modifier.fillMaxWidth(), variant = PrismCardVariant.Outlined) {
                    Text(
                      text = "${prize.position} • ${prize.prize}",
                      style = Prism.typography.cardTitle,
                      color = Prism.color.titleColor,
                    )
                    Text(
                      text = prizeTeam?.name ?: "TBD",
                      modifier = Modifier
                        .padding(top = Prism.dimens.spacingXs)
                        .let { base ->
                          if (prizeTeamId != null) {
                            base.clickable { onTeamSelected(prizeTeamId) }
                          } else {
                            base
                          }
                        },
                      style = Prism.typography.bodySmall,
                      color = if (prizeTeamId != null) Prism.color.accent else Prism.color.labelColor,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun EventHeaderCard(
  event: EventDetails,
  onTeamSelected: (String) -> Unit,
) {
  PrismCard(modifier = Modifier.fillMaxWidth(), variant = PrismCardVariant.Outlined) {
    PrismTag(
      text = event.status.name,
      variant =
        when (event.status) {
          EventStatus.ONGOING -> PrismTagVariant.Danger
          EventStatus.UPCOMING -> PrismTagVariant.Info
          EventStatus.COMPLETED -> PrismTagVariant.Success
          EventStatus.UNKNOWN -> PrismTagVariant.Neutral
        },
    )
    Text(
      text = event.title,
      modifier = Modifier.padding(top = Prism.dimens.spacingS),
      style = Prism.typography.sectionTitle,
      color = Prism.color.titleColor,
    )
    Text(
      text = "${event.region} • ${event.dates} • ${event.prize}",
      modifier = Modifier.padding(top = Prism.dimens.spacingXs),
      style = Prism.typography.bodySmall,
      color = Prism.color.labelColor,
    )
    if (event.teams.isNotEmpty()) {
      PrismSectionTitle(
        title = "Participants",
        preLabel = "teams",
        modifier = Modifier.padding(top = Prism.dimens.spacingM),
      )
      Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
        event.teams.take(8).forEach { team ->
          val teamId = team.id
          Text(
            text = listOfNotNull(team.name, team.seed).joinToString(" • "),
            modifier = Modifier
              .fillMaxWidth()
              .let { base -> if (teamId != null) base.clickable { onTeamSelected(teamId) } else base },
            style = Prism.typography.bodySmall,
            color = if (teamId != null) Prism.color.accent else Prism.color.bodyColor,
          )
        }
      }
    }
  }
}

@Composable
private fun EventDetailStateMessage(
  text: String,
) {
  Text(
    text = text,
    modifier = Modifier.fillMaxWidth().padding(Prism.dimens.spacingM),
    style = Prism.typography.bodyLarge,
    color = Prism.color.labelColor,
  )
}
