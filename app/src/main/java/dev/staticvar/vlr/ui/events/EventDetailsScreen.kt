package dev.staticvar.vlr.ui.events

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dev.staticvar.vlr.R
import dev.staticvar.vlr.data.api.response.TournamentDetails
import dev.staticvar.vlr.ui.*
import dev.staticvar.vlr.ui.helper.CardView
import dev.staticvar.vlr.ui.helper.VLRTabIndicator
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.ui.theme.tintedBackground
import dev.staticvar.vlr.utils.Waiting
import dev.staticvar.vlr.utils.onFail
import dev.staticvar.vlr.utils.onPass
import dev.staticvar.vlr.utils.onWaiting

@Composable
fun EventDetails(viewModel: VlrViewModel, id: String) {
  val details by
    remember(viewModel) { viewModel.getTournamentDetails(id) }.collectAsState(Waiting())

  val primaryContainer = VLRTheme.colorScheme.tintedBackground
  val systemUiController = rememberSystemUiController()
  SideEffect { systemUiController.setStatusBarColor(primaryContainer) }

  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    details
      .onPass {
        data?.let { tournamentDetails ->
          var selectedIndex by remember { mutableStateOf(0) }

          var tabSelection by remember(selectedIndex) { mutableStateOf(0) }

          val group =
            tournamentDetails.matches.groupBy {
              when (selectedIndex) {
                0 -> it.status
                1 -> it.round
                else -> it.stage
              }
            }

          LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { Spacer(modifier = Modifier.statusBarsPadding()) }
            item { TournamentDetailsHeader(tournamentDetails = tournamentDetails) }
            item {
              EventDetailsTeamSlider(
                list = tournamentDetails.participants,
                onClick = { viewModel.action.team(it) }
              )
            }
            item {
              EventMatchGroups(
                selectedIndex,
                group,
                tabSelection,
                onFilterChange = { selectedIndex = it },
                onTabChange = { tabSelection = it }
              )
            }
            items(
              group[group.keys.toList()[tabSelection]] ?: group[group.keys.toList()[0]] ?: listOf()
            ) { TournamentMatchOverview(game = it, onClick = { viewModel.action.match(it) }) }
            item { Spacer(modifier = Modifier.navigationBarsPadding()) }
          }
        }
      }
      .onWaiting { LinearProgressIndicator() }
      .onFail { Text(text = message()) }
  }
}

@Composable
fun TournamentDetailsHeader(tournamentDetails: TournamentDetails) {
  CardView() {
    Box(modifier = Modifier.fillMaxWidth()) {
      Row(Modifier.fillMaxWidth().padding(Local16DPPadding.current)) {
        Spacer(modifier = Modifier.weight(1f))
        Image(
          painter =
            rememberImagePainter(data = tournamentDetails.img, builder = { crossfade(true) }),
          contentDescription = stringResource(R.string.tournament_logo_content_desciption),
          modifier = Modifier.alpha(0.3f),
        )
      }
      Column(Modifier.fillMaxWidth().padding(Local8DPPadding.current)) {
        Text(
          text = tournamentDetails.title,
          style = VLRTheme.typography.titleSmall,
          modifier = Modifier.padding(Local4DPPadding.current),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          color = VLRTheme.colorScheme.primary
        )
        if (tournamentDetails.subtitle.isNotBlank())
          Text(
            text = tournamentDetails.subtitle,
            modifier = Modifier.padding(Local4DPPadding.current)
          )
        Row(
          Modifier.fillMaxWidth().padding(Local4DP_2DPPadding.current),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            Icons.Outlined.DateRange,
            contentDescription = stringResource(R.string.date),
            tint = VLRTheme.colorScheme.primary,
          )
          Text(text = tournamentDetails.dates)
        }
        Row(
          Modifier.fillMaxWidth().padding(Local4DP_2DPPadding.current),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            Icons.Outlined.Paid,
            contentDescription = stringResource(R.string.prize),
            tint = VLRTheme.colorScheme.primary,
          )
          Text(text = tournamentDetails.prize)
        }
        Row(
          Modifier.fillMaxWidth().padding(Local4DP_2DPPadding.current),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            Icons.Outlined.LocationOn,
            contentDescription = stringResource(R.string.location),
            tint = VLRTheme.colorScheme.primary,
          )
          Text(text = tournamentDetails.location.uppercase())
        }
      }
    }
  }
}

@Composable
fun EventDetailsTeamSlider(list: List<TournamentDetails.Participant>, onClick: (String) -> Unit) {
  Text(
    text = "Teams",
    modifier = Modifier.padding(Local16DPPadding.current),
    style = VLRTheme.typography.titleSmall,
    color = VLRTheme.colorScheme.primary
  )
  LazyRow(modifier = Modifier.fillMaxWidth()) {
    items(list) {
      CardView(
        Modifier.width(width = 150.dp).aspectRatio(1f).clickable { onClick(it.id) },
      ) {
        Column(
          Modifier.fillMaxSize().padding(Local8DPPadding.current),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = it.team,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = VLRTheme.typography.labelLarge,
            color = VLRTheme.colorScheme.primary,
          )
          Image(
            painter = rememberImagePainter(data = it.img, builder = { crossfade(true) }),
            contentDescription = stringResource(R.string.team_logo_content_description),
            alignment = Alignment.Center,
            modifier = Modifier.size(80.dp).aspectRatio(1f)
          )
          Text(
            text = it.seed ?: "",
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = VLRTheme.typography.labelMedium
          )
        }
      }
    }
  }
}

@Composable
fun EventMatchGroups(
  selectedIndex: Int,
  group: Map<String, List<TournamentDetails.Games>>,
  tabSelection: Int,
  onFilterChange: (Int) -> Unit,
  onTabChange: (Int) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }

  val filterOptions =
    listOf(
      stringResource(R.string.status),
      stringResource(R.string.rounds),
      stringResource(R.string.stage)
    )

  Column(Modifier.fillMaxWidth().padding(Local8DPPadding.current)) {
    Text(
      text = "Games",
      modifier = Modifier.padding(Local8DPPadding.current),
      style = VLRTheme.typography.titleSmall,
      color = VLRTheme.colorScheme.primary
    )
    Row(
      modifier =
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { expanded = true },
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(text = stringResource(R.string.filter_by), Modifier.padding(Local8DPPadding.current))
      Text(
        text = filterOptions[selectedIndex],
        Modifier.weight(1f).padding(Local8DPPadding.current),
        textAlign = TextAlign.End
      )
      Icon(
        Icons.Outlined.ArrowDropDown,
        contentDescription = stringResource(R.string.dropdown_content_description),
        modifier = Modifier.padding(Local8DPPadding.current)
      )
    }
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      modifier = Modifier.fillMaxWidth().padding(Local8DPPadding.current)
    ) {
      DropdownMenuItem(
        text = { Text(text = filterOptions[0]) },
        onClick = {
          onFilterChange(0)
          expanded = false
        }
      )
      DropdownMenuItem(
        text = { Text(text = filterOptions[1]) },
        onClick = {
          onFilterChange(1)
          expanded = false
        }
      )
      DropdownMenuItem(
        text = { Text(text = filterOptions[2]) },
        onClick = {
          onFilterChange(2)
          expanded = false
        }
      )
    }

    ScrollableTabRow(
      selectedTabIndex = tabSelection,
      containerColor = VLRTheme.colorScheme.primaryContainer,
      modifier =
        Modifier.fillMaxWidth().padding(Local8DPPadding.current).clip(RoundedCornerShape(16.dp)),
      indicator = { indicators -> VLRTabIndicator(indicators, tabSelection) }
    ) {
      group.keys.forEachIndexed { index, s ->
        Tab(
          selected = tabSelection == index,
          onClick = { onTabChange(index) },
        ) {
          Text(
            text = s.replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(Local16DPPadding.current)
          )
        }
      }
    }
  }
}

@Composable
fun TournamentMatchOverview(game: TournamentDetails.Games, onClick: (String) -> Unit) {
  CardView(
    modifier = Modifier.clickable { onClick(game.id) },
  ) {
    Column(modifier = Modifier.padding(Local8DPPadding.current)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Text(
          text = game.status.replaceFirstChar { it.uppercase() },
          modifier = Modifier.weight(1f),
          textAlign = TextAlign.Center,
          style = VLRTheme.typography.displaySmall
        )
        Icon(
          Icons.Outlined.OpenInNew,
          contentDescription = stringResource(R.string.open_match_content_description),
          modifier = Modifier.size(24.dp).padding(Local2DPPadding.current)
        )
      }

      Row(
        modifier = Modifier.padding(Local4DPPadding.current),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = game.teams[0].name,
          style = VLRTheme.typography.titleSmall,
          modifier = Modifier.weight(1f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = game.teams[0].score?.toString() ?: "-",
          style = VLRTheme.typography.titleSmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Row(
        modifier = Modifier.padding(Local4DPPadding.current),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = game.teams[1].name,
          style = VLRTheme.typography.titleSmall,
          modifier = Modifier.weight(1f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = game.teams[1].score?.toString() ?: "-",
          style = VLRTheme.typography.titleSmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Text(
        text = "${game.time} - ${game.date}",
        modifier = Modifier.fillMaxWidth().padding(Local8DPPadding.current),
        textAlign = TextAlign.Center,
        style = VLRTheme.typography.labelSmall
      )
    }
  }
}
