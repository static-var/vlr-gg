package dev.staticvar.vlr.featurerankings.presentation

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
import dev.staticvar.designsystem.component.card.PrismCardVariant
import dev.staticvar.designsystem.component.navigation.PrismTab
import dev.staticvar.designsystem.component.navigation.PrismTabs
import dev.staticvar.designsystem.prism.Prism
import org.koin.mp.KoinPlatform

@Composable
public fun RankingsRoute(
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val viewModel: RankingsViewModel = rememberKoinInstance()
  val uiState: RankingsUiState by viewModel.uiState.collectAsState()

  DisposableEffect(Unit) {
    onDispose(viewModel::clear)
  }

  RankingsScreen(
    uiState = uiState,
    onRegionSelected = viewModel::selectRegion,
    onTeamSelected = onTeamSelected,
    modifier = modifier,
  )
}

@Composable
internal fun RankingsScreen(
  uiState: RankingsUiState,
  onRegionSelected: (String) -> Unit,
  onTeamSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val selectedRegion = uiState.selectedRegion
  val selectedRanking = remember(uiState.regions, selectedRegion) {
    uiState.regions.firstOrNull { it.region == selectedRegion }
  }

  Column(
    modifier = modifier.fillMaxSize().padding(Prism.dimens.spacingM),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
  ) {
    PrismScreenTitleBar(
      title = "Ranking",
      subtitle = "Regional team standings with direct drill-down into team details.",
      preLabel = "global",
    )

    if (uiState.regions.isNotEmpty()) {
      PrismTabs(
        tabs = uiState.regions.map { PrismTab(id = it.region, label = it.region) },
        selectedTabId = selectedRegion ?: uiState.regions.first().region,
        onTabSelected = { onRegionSelected(it.id) },
      )
    }

    when {
      uiState.isLoading -> {
        StateMessage(text = "Loading rankings…")
      }

      uiState.errorMessage != null && selectedRanking == null -> {
        StateMessage(text = uiState.errorMessage ?: "Unable to load rankings.")
      }

      selectedRanking == null -> {
        StateMessage(text = "No rankings available yet.")
      }

      else -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
        ) {
          item {
            Text(
              text = "Top ${selectedRanking.teams.size} • ${selectedRanking.region}",
              style = Prism.typography.label,
              color = Prism.color.labelColor,
            )
          }
          items(selectedRanking.teams, key = { it.teamId }) { team ->
            PrismCard(
              modifier = Modifier.fillMaxWidth(),
              variant = PrismCardVariant.Outlined,
              onClick = { onTeamSelected(team.teamId) },
            ) {
              Text(
                text = "#${team.rank} ${team.teamName}",
                style = Prism.typography.cardTitle,
                color = Prism.color.titleColor,
              )
              Text(
                text = "${team.country} • ${team.points} pts",
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

@Composable
private fun StateMessage(
  text: String,
) {
  Text(
    text = text,
    modifier = Modifier.fillMaxWidth().padding(Prism.dimens.spacingM),
    style = Prism.typography.bodyLarge,
    color = Prism.color.labelColor,
  )
}

@Composable
private inline fun <reified T : Any> rememberKoinInstance(): T =
  remember {
    KoinPlatform.getKoin().get<T>()
  }
