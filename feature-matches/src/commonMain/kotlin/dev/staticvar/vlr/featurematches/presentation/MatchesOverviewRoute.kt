/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.appbar.PrismScreenTitleBar
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.navigation.PrismTab
import dev.staticvar.designsystem.component.navigation.PrismTabs
import dev.staticvar.designsystem.component.state.PrismStateMessage
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.component.tag.PrismTagStyle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import org.koin.mp.KoinPlatform

@Composable
public fun MatchesOverviewRoute(onMatchSelected: (String) -> Unit, modifier: Modifier = Modifier) {
  val viewModel: MatchesViewModel = rememberKoinInstance()
  val uiState: MatchesUiState by viewModel.uiState.collectAsState()

  DisposableEffect(Unit) {
    onDispose(viewModel::clear)
  }

  MatchesOverviewScreen(
    uiState = uiState,
    onFilterSelected = viewModel::selectFilter,
    onMatchSelected = onMatchSelected,
    modifier = modifier,
  )
}

@Composable
internal fun MatchesOverviewScreen(
  uiState: MatchesUiState,
  onFilterSelected: (MatchStatusFilter) -> Unit,
  onMatchSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val filteredMatches =
    remember(uiState.matches, uiState.selectedStatus) {
      uiState.matches.filter { match ->
        when (uiState.selectedStatus) {
          MatchStatusFilter.Live -> match.status == MatchStatus.LIVE
          MatchStatusFilter.Upcoming -> match.status == MatchStatus.UPCOMING
          MatchStatusFilter.Completed -> match.status == MatchStatus.COMPLETED
        }
      }
    }

  Column(
    modifier = modifier.fillMaxSize().padding(Prism.dimens.spacingM),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    PrismScreenTitleBar(
      title = "Match overview",
      subtitle = "Live, upcoming, and completed series sourced from the shared domain layer.",
      preLabel = "matches",
    )
    PrismTabs(
      tabs =
      listOf(
        PrismTab(id = MatchStatusFilter.Live.name, label = "Live"),
        PrismTab(id = MatchStatusFilter.Upcoming.name, label = "Upcoming"),
        PrismTab(id = MatchStatusFilter.Completed.name, label = "Completed"),
      ),
      selectedTabId = uiState.selectedStatus.name,
      onTabSelected = { onFilterSelected(MatchStatusFilter.valueOf(it.id)) },
    )

    when {
      uiState.isLoading -> PrismStateMessage(text = "Loading matches…")

      uiState.errorMessage != null && filteredMatches.isEmpty() ->
        PrismStateMessage(text = uiState.errorMessage ?: "Unable to load matches.")

      filteredMatches.isEmpty() -> PrismStateMessage(text = "No matches in this bucket yet.")

      else -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
        ) {
          items(filteredMatches, key = MatchPreview::id) { match ->
            PrismCard(
              modifier = Modifier.fillMaxWidth(),
              style = PrismCardStyle.Outlined,
              onClick = { onMatchSelected(match.id) },
            ) {
              PrismTag(
                text = match.status.name,
                style =
                when (match.status) {
                  MatchStatus.LIVE -> PrismTagStyle.Danger
                  MatchStatus.UPCOMING -> PrismTagStyle.Info
                  MatchStatus.COMPLETED -> PrismTagStyle.Success
                  MatchStatus.UNKNOWN -> PrismTagStyle.Neutral
                },
              )
              Text(
                text = "${match.team1.name} vs ${match.team2.name}",
                modifier = Modifier.padding(top = Prism.dimens.spacingS),
                style = Prism.typography.cardTitle,
                color = Prism.color.titleColor,
              )
              Text(
                text = "${match.event} • ${match.series}",
                modifier = Modifier.padding(top = Prism.dimens.spacingXs),
                style = Prism.typography.bodySmall,
                color = Prism.color.labelColor,
              )
              Text(
                text = buildScoreLine(match),
                modifier = Modifier.padding(top = Prism.dimens.spacingXs),
                style = Prism.typography.label,
                color = Prism.color.bodyColor,
              )
            }
          }
        }
      }
    }
  }
}

private fun buildScoreLine(match: MatchPreview): String {
  val score = "${match.team1.score ?: "-"} : ${match.team2.score ?: "-"}"
  return listOfNotNull(score, match.time).joinToString(separator = " • ")
}

@Composable
private inline fun <reified T : Any> rememberKoinInstance(): T = remember {
  KoinPlatform.getKoin().get<T>()
}
