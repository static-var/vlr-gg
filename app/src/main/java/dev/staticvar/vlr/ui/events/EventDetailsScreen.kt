package dev.staticvar.vlr.ui.events

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.skydoves.landscapist.CircularReveal
import com.skydoves.landscapist.glide.GlideImage
import dev.staticvar.vlr.R
import dev.staticvar.vlr.data.api.response.TournamentDetails
import dev.staticvar.vlr.ui.*
import dev.staticvar.vlr.ui.helper.CardView
import dev.staticvar.vlr.ui.helper.VLRTabIndicator
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.*

@Composable
fun EventDetails(viewModel: VlrViewModel, id: String) {
  val details by
    remember(viewModel) { viewModel.getTournamentDetails(id) }.collectAsState(Waiting())

  val primaryContainer = VLRTheme.colorScheme.surface.copy(0.2f)
  val systemUiController = rememberSystemUiController()
  SideEffect { systemUiController.setStatusBarColor(primaryContainer) }

  val modifier = Modifier
  Column(
    modifier = modifier.fillMaxSize(),
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

          LazyColumn(modifier = modifier.fillMaxSize()) {
            item { Spacer(modifier = modifier.statusBarsPadding()) }
            item { TournamentDetailsHeader(tournamentDetails = tournamentDetails) }
            item {
              EventDetailsTeamSlider(
                modifier = modifier,
                list = tournamentDetails.participants,
                onClick = { viewModel.action.team(it) }
              )
            }

            kotlin
              .runCatching {
                group[group.keys.elementAt(tabSelection)]?.let { games ->
                  item {
                    EventMatchGroups(
                      modifier,
                      selectedIndex,
                      group,
                      tabSelection,
                      onFilterChange = { selectedIndex = it },
                      onTabChange = { tabSelection = it }
                    )
                  }
                  items(games) { item ->
                    TournamentMatchOverview(
                      modifier = modifier,
                      game = item,
                      onClick = { viewModel.action.match(it) }
                    )
                  }
                }
                  ?: e { "No matches in list" }
              }
              .onFailure { it.printStackTrace() }
            item { Spacer(modifier = modifier.navigationBarsPadding()) }
          }
        }
      }
      .onWaiting { LinearProgressIndicator(modifier) }
      .onFail { Text(text = message()) }
  }
}

@Composable
fun TournamentDetailsHeader(modifier: Modifier = Modifier, tournamentDetails: TournamentDetails) {
  CardView(modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
      Row(modifier.fillMaxWidth().padding(Local16DPPadding.current)) {
        Spacer(modifier = modifier.weight(1f))
        GlideImage(
          tournamentDetails.img,
          contentDescription = stringResource(R.string.tournament_logo_content_desciption),
          modifier = modifier.alpha(0.3f),
          circularReveal = CircularReveal(1000),
          contentScale = ContentScale.Inside,
          alignment = Alignment.CenterEnd
        )
      }
      Column(modifier.fillMaxWidth().padding(Local8DPPadding.current)) {
        Text(
          text = tournamentDetails.title,
          style = VLRTheme.typography.titleSmall,
          modifier = modifier.padding(Local4DPPadding.current),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          color = VLRTheme.colorScheme.primary
        )
        if (tournamentDetails.subtitle.isNotBlank())
          Text(
            text = tournamentDetails.subtitle,
            modifier = modifier.padding(Local4DPPadding.current)
          )
        Row(
          modifier.fillMaxWidth().padding(Local4DP_2DPPadding.current),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            Icons.Outlined.DateRange,
            contentDescription = stringResource(R.string.date),
            tint = VLRTheme.colorScheme.primary,
          )
          Text(text = tournamentDetails.dates, modifier = Modifier.padding(horizontal = 4.dp))
        }
        Row(
          modifier.fillMaxWidth().padding(Local4DP_2DPPadding.current),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            Icons.Outlined.Paid,
            contentDescription = stringResource(R.string.prize),
            tint = VLRTheme.colorScheme.primary,
          )
          Text(text = tournamentDetails.prize, modifier = Modifier.padding(horizontal = 4.dp))
        }
        Row(
          modifier.fillMaxWidth().padding(Local4DP_2DPPadding.current),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            Icons.Outlined.LocationOn,
            contentDescription = stringResource(R.string.location),
            tint = VLRTheme.colorScheme.primary,
          )
          Text(text = tournamentDetails.location.uppercase(), modifier = Modifier.padding(horizontal = 4.dp))
        }
      }
    }
  }
}

@Composable
fun EventDetailsTeamSlider(
  modifier: Modifier = Modifier,
  list: List<TournamentDetails.Participant>,
  onClick: (String) -> Unit
) {
  Text(
    text = "Teams",
    modifier = modifier.padding(Local16DPPadding.current),
    style = VLRTheme.typography.titleSmall,
    color = VLRTheme.colorScheme.primary
  )
  LazyRow(modifier = modifier.fillMaxWidth()) {
    items(list) {
      CardView(
        modifier.width(width = 150.dp).aspectRatio(1f).clickable { onClick(it.id) },
      ) {
        Column(
          modifier.fillMaxSize().padding(Local8DPPadding.current),
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
          GlideImage(
            imageModel = it.img,
            contentDescription = stringResource(R.string.team_logo_content_description),
            alignment = Alignment.Center,
            modifier = modifier.size(80.dp).aspectRatio(1f),
            circularReveal = CircularReveal(1000)
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
  modifier: Modifier = Modifier,
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

  Column(modifier.fillMaxWidth().padding(Local8DPPadding.current)) {
    Text(
      text = "Games",
      modifier = modifier.padding(Local8DPPadding.current),
      style = VLRTheme.typography.titleSmall,
      color = VLRTheme.colorScheme.primary
    )
    Row(
      modifier =
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { expanded = true },
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(text = stringResource(R.string.filter_by), modifier.padding(Local8DPPadding.current))
      Text(
        text = filterOptions[selectedIndex],
        modifier.weight(1f).padding(Local8DPPadding.current),
        textAlign = TextAlign.End
      )
      Icon(
        Icons.Outlined.ArrowDropDown,
        contentDescription = stringResource(R.string.dropdown_content_description),
        modifier = modifier.padding(Local8DPPadding.current)
      )
    }
    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      modifier = modifier.fillMaxWidth().padding(Local8DPPadding.current)
    ) {
      DropdownMenuItem(
        text = {
          Text(
            text = filterOptions[0],
            color =
              if (selectedIndex == 0) VLRTheme.colorScheme.primary
              else VLRTheme.colorScheme.onBackground
          )
        },
        onClick = {
          onFilterChange(0)
          expanded = false
        }
      )
      DropdownMenuItem(
        text = {
          Text(
            text = filterOptions[1],
            color =
              if (selectedIndex == 1) VLRTheme.colorScheme.primary
              else VLRTheme.colorScheme.onBackground
          )
        },
        onClick = {
          onFilterChange(1)
          expanded = false
        }
      )
      DropdownMenuItem(
        text = {
          Text(
            text = filterOptions[2],
            color =
              if (selectedIndex == 2) VLRTheme.colorScheme.primary
              else VLRTheme.colorScheme.onBackground
          )
        },
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
        modifier.fillMaxWidth().padding(Local8DPPadding.current).clip(RoundedCornerShape(16.dp)),
      indicator = { indicators ->
        if (indicators.isNotEmpty()) VLRTabIndicator(indicators, tabSelection)
      }
    ) {
      group.keys.forEachIndexed { index, s ->
        Tab(
          selected = tabSelection == index,
          onClick = { onTabChange(index) },
        ) {
          Text(
            text = s.replaceFirstChar { it.uppercase() },
            modifier = modifier.padding(Local16DPPadding.current)
          )
        }
      }
    }
  }
}

@Composable
fun TournamentMatchOverview(
  modifier: Modifier = Modifier,
  game: TournamentDetails.Games,
  onClick: (String) -> Unit
) {
  CardView(
    modifier = modifier.clickable { onClick(game.id) },
  ) {
    Column(modifier = modifier.padding(Local8DPPadding.current)) {
      Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Text(
          text = game.status.replaceFirstChar { it.uppercase() },
          modifier = modifier.weight(1f),
          textAlign = TextAlign.Center,
          style = VLRTheme.typography.displaySmall
        )
        Icon(
          Icons.Outlined.OpenInNew,
          contentDescription = stringResource(R.string.open_match_content_description),
          modifier = modifier.size(24.dp).padding(Local2DPPadding.current)
        )
      }

      Row(
        modifier = modifier.padding(Local4DPPadding.current),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = game.teams[0].name,
          style = VLRTheme.typography.titleSmall,
          modifier = modifier.weight(1f),
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
        modifier = modifier.padding(Local4DPPadding.current),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = game.teams[1].name,
          style = VLRTheme.typography.titleSmall,
          modifier = modifier.weight(1f),
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
        modifier = modifier.fillMaxWidth().padding(Local8DPPadding.current),
        textAlign = TextAlign.Center,
        style = VLRTheme.typography.labelSmall
      )
    }
  }
}
