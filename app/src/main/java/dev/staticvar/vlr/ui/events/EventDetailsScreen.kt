package dev.staticvar.vlr.ui.events

import androidx.compose.foundation.BorderStroke
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
import com.google.accompanist.insets.statusBarsPadding
import dev.staticvar.vlr.R
import dev.staticvar.vlr.data.api.response.TournamentDetails
import dev.staticvar.vlr.ui.CARD_ALPHA
import dev.staticvar.vlr.ui.COLOR_ALPHA
import dev.staticvar.vlr.ui.VlrViewModel
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.Waiting
import dev.staticvar.vlr.utils.onFail
import dev.staticvar.vlr.utils.onPass
import dev.staticvar.vlr.utils.onWaiting

@Composable
fun EventDetails(viewModel: VlrViewModel, id: String) {
  val details by
    remember(viewModel) { viewModel.getTournamentDetails(id) }.collectAsState(Waiting())

  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(modifier = Modifier.statusBarsPadding())

    details
      .onPass {
        data?.let { tournamentDetails ->
          var selectedIndex by remember { mutableStateOf(0) }

          var tabSelection by
            remember(selectedIndex) {
              mutableStateOf(0)
            }

          val group =
            tournamentDetails.matches.groupBy {
              when (selectedIndex) {
                0 -> it.status
                1 -> it.round
                else -> it.stage
              }
            }

          LazyColumn(modifier = Modifier.fillMaxSize()) {
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
  OutlinedCard(
    Modifier.fillMaxWidth().padding(8.dp),
    contentColor = VLRTheme.colorScheme.onPrimaryContainer,
    containerColor = VLRTheme.colorScheme.primaryContainer.copy(COLOR_ALPHA),
    border = BorderStroke(1.dp, VLRTheme.colorScheme.primaryContainer)
  ) {
    Box(modifier = Modifier.fillMaxWidth()) {
      Row(Modifier.fillMaxWidth().padding(16.dp)) {
        Spacer(modifier = Modifier.weight(1f))
        Image(
          painter =
            rememberImagePainter(data = tournamentDetails.img, builder = { crossfade(true) }),
          contentDescription = stringResource(R.string.tournament_logo_content_desciption),
          modifier = Modifier.alpha(0.3f)
        )
      }
      Column(Modifier.fillMaxWidth().padding(8.dp)) {
        Text(
          text = tournamentDetails.title,
          style = VLRTheme.typography.titleSmall,
          modifier = Modifier.padding(4.dp),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        Text(text = tournamentDetails.subtitle, modifier = Modifier.padding(4.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
          Icon(Icons.Outlined.DateRange, contentDescription = stringResource(R.string.date))
          Text(text = tournamentDetails.dates)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
          Icon(Icons.Outlined.Paid, contentDescription = stringResource(R.string.prize))
          Text(text = tournamentDetails.prize)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
          Icon(Icons.Outlined.LocationOn, contentDescription = stringResource(R.string.location))
          Text(text = tournamentDetails.location.uppercase())
        }
      }
    }
  }
}

@Composable
fun EventDetailsTeamSlider(list: List<TournamentDetails.Participant>, onClick: (String) -> Unit) {
  Text(text = "Teams", modifier = Modifier.padding(8.dp), style = VLRTheme.typography.titleSmall)
  LazyRow(modifier = Modifier.fillMaxWidth()) {
    items(list) {
      OutlinedCard(
        Modifier.padding(8.dp).width(width = 150.dp).aspectRatio(1.1f).clickable { onClick(it.id) },
        contentColor = VLRTheme.colorScheme.onPrimaryContainer,
        containerColor = VLRTheme.colorScheme.primaryContainer.copy(COLOR_ALPHA),
        border = BorderStroke(1.dp, VLRTheme.colorScheme.primaryContainer)
      ) {
        Column(
          Modifier.padding(8.dp),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = it.team,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = VLRTheme.typography.labelLarge,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
          )
          Image(
            painter = rememberImagePainter(data = it.img, builder = { crossfade(true) }),
            contentDescription = stringResource(R.string.team_logo_content_description),
            alignment = Alignment.Center,
            modifier = Modifier.size(80.dp).aspectRatio(1f)
          )
          Text(
            text = it.seed ?: "",
            modifier = Modifier.fillMaxWidth(),
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

  val filterOptions = listOf(stringResource(R.string.status), stringResource(R.string.rounds), stringResource(R.string.stage))

  Column(Modifier.fillMaxWidth().padding(8.dp)) {
    Text(text = "Games", modifier = Modifier.padding(8.dp), style = VLRTheme.typography.titleSmall)
    Row(
      modifier =
        Modifier.fillMaxWidth().padding(8.dp).clip(RoundedCornerShape(16.dp)).clickable {
          expanded = true
        },
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(text = stringResource(R.string.filter_by))
      Text(text = filterOptions[selectedIndex], Modifier.weight(1f), textAlign = TextAlign.End)
      Icon(Icons.Outlined.ArrowDropDown, contentDescription = stringResource(R.string.dropdown_content_description))
    }
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      modifier = Modifier.fillMaxWidth().padding(8.dp)
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
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).clip(RoundedCornerShape(16.dp))
    ) {
      group.keys.forEachIndexed { index, s ->
        Tab(
          selected = tabSelection == index,
          onClick = { onTabChange(index) },
          modifier = Modifier.padding(16.dp)
        ) { Text(text = s.replaceFirstChar { it.uppercase() }) }
      }
    }
  }
}

@Composable
fun TournamentMatchOverview(game: TournamentDetails.Games, onClick: (String) -> Unit) {
  Card(
    modifier =
      Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).clickable {
        onClick(game.id)
      },
    shape = RoundedCornerShape(16.dp),
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(CARD_ALPHA)
  ) {
    Column(modifier = Modifier.padding(8.dp)) {
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
          modifier = Modifier.size(24.dp).padding(2.dp)
        )
      }

      Row(modifier = Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
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
      Row(modifier = Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        textAlign = TextAlign.Center,
        style = VLRTheme.typography.labelSmall
      )
    }
  }
}
