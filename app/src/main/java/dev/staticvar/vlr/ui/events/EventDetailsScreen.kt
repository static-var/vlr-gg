package dev.staticvar.vlr.ui.events

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.animation.circular.CircularRevealPlugin
import com.skydoves.landscapist.components.rememberImageComponent
import com.skydoves.landscapist.glide.GlideImage
import dev.staticvar.vlr.R
import dev.staticvar.vlr.data.api.response.TournamentDetails
import dev.staticvar.vlr.ui.Local16DPPadding
import dev.staticvar.vlr.ui.Local16DP_8DPPadding
import dev.staticvar.vlr.ui.Local4DPPadding
import dev.staticvar.vlr.ui.Local4DP_2DPPadding
import dev.staticvar.vlr.ui.Local8DPPadding
import dev.staticvar.vlr.ui.VlrViewModel
import dev.staticvar.vlr.ui.common.ErrorUi
import dev.staticvar.vlr.ui.helper.CardView
import dev.staticvar.vlr.ui.helper.VLRTabIndicator
import dev.staticvar.vlr.ui.scrim.NavigationBarSpacer
import dev.staticvar.vlr.ui.scrim.NavigationBarType
import dev.staticvar.vlr.ui.scrim.StatusBarSpacer
import dev.staticvar.vlr.ui.scrim.StatusBarType
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.Constants
import dev.staticvar.vlr.utils.StableHolder
import dev.staticvar.vlr.utils.Waiting
import dev.staticvar.vlr.utils.onFail
import dev.staticvar.vlr.utils.onPass
import dev.staticvar.vlr.utils.openAsCustomTab
import dev.staticvar.vlr.utils.patternDateTimeToReadable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun EventDetails(viewModel: VlrViewModel, id: String) {

  val modifier = Modifier

  val details by
  remember(id) { viewModel.getEventDetails(id) }.collectAsStateWithLifecycle(Waiting())

  var triggerRefresh by remember(viewModel) { mutableStateOf(true) }
  val updateState by
  remember(triggerRefresh, id) { viewModel.refreshEventDetails(id) }
    .collectAsStateWithLifecycle(initialValue = Ok(false))

  val swipeRefresh =
    rememberPullRefreshState(updateState.get() ?: false, { triggerRefresh = triggerRefresh.not() })
  val lazyListState = rememberLazyListState()

  val trackerString = id.toEventTopic()
  val isTracked by
  remember { viewModel.isTopicTracked(trackerString) }.collectAsStateWithLifecycle(null)

  val progressBarVisibility by remember(updateState.get(), swipeRefresh.progress) {
    mutableStateOf(
      updateState.get() == true || swipeRefresh.progress != 0f
    )
  }

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
            remember(tabSelection, selectedIndex, tournamentDetails) {
              tournamentDetails.matches.groupBy {
                when (selectedIndex) {
                  0 -> it.status
                  1 -> it.round
                  else -> it.stage
                }
              }
            }
          AnimatedVisibility(visible = progressBarVisibility) {
            Column {
              StatusBarSpacer(statusBarType = StatusBarType.TRANSPARENT)
              LinearProgressIndicator(
                modifier
                  .fillMaxWidth()
                  .padding(Local16DPPadding.current)
                  .animateContentSize()
                  .testTag("common:loader")
                  .align(Alignment.CenterHorizontally)
              )
            }
          }
          Box(
            modifier = Modifier
              .pullRefresh(swipeRefresh)
              .fillMaxSize(),
          ) {
            LazyColumn(
              modifier = modifier
                .fillMaxSize()
                .testTag("eventDetails:root"),
              state = lazyListState
            ) {
              item {
                StatusBarSpacer(statusBarType = StatusBarType.TRANSPARENT)
              }
              updateState.getError()?.let {
                item { ErrorUi(modifier = modifier, exceptionMessage = it.stackTraceToString()) }
              }

              item {
                TournamentDetailsHeader(
                  tournamentDetails = tournamentDetails,
                  isTracked = isTracked ?: false
                ) {
                  when (isTracked) {
                    true -> {
                      Firebase.messaging.unsubscribeFromTopic(trackerString).await()
                      viewModel.removeTopic(trackerString)
                    }

                    false -> {
                      Firebase.messaging.subscribeToTopic(trackerString).await()
                      viewModel.trackTopic(trackerString)
                    }

                    else -> {}
                  }
                }
              }
              if (tournamentDetails.participants.isNotEmpty())
                item {
                  EventDetailsTeamSlider(
                    modifier = modifier,
                    list = StableHolder(tournamentDetails.participants),
                    onClick = { viewModel.action.team(it) }
                  )
                }
              else
                item {
                  Text(
                    text = stringResource(id = R.string.no_team_info_found),
                    modifier = modifier
                      .fillMaxWidth()
                      .padding(Local16DP_8DPPadding.current),
                    textAlign = TextAlign.Center,
                    style = VLRTheme.typography.titleMedium,
                    color = VLRTheme.colorScheme.primary
                  )
                }
              if (group.isNotEmpty())
                group[group.keys.elementAt(tabSelection)]?.let { games ->
                  item {
                    EventMatchGroups(
                      modifier,
                      selectedIndex,
                      StableHolder(group),
                      tabSelection,
                      onFilterChange = { selectedIndex = it },
                      onTabChange = { tabSelection = it }
                    )
                  }
                  items(games, key = { game -> game.id }) { item ->
                    TournamentMatchOverview(
                      modifier = modifier,
                      game = item,
                      onClick = { viewModel.action.match(it) }
                    )
                  }
                }
              else
                item {
                  Text(
                    text = stringResource(id = R.string.no_match_info_found),
                    modifier = modifier
                      .fillMaxWidth()
                      .padding(Local16DP_8DPPadding.current),
                    textAlign = TextAlign.Center,
                    style = VLRTheme.typography.titleMedium,
                    color = VLRTheme.colorScheme.primary
                  )
                }
              item { NavigationBarSpacer(navigationBarType = NavigationBarType.TRANSPARENT) }
            }
          }
        }
          ?: kotlin.run {
            updateState.getError()?.let {
              ErrorUi(modifier = modifier, exceptionMessage = it.stackTraceToString())
            }
              ?: LinearProgressIndicator(modifier.animateContentSize())
          }
      }
      .onFail { Text(text = message()) }
  }
}

@Composable
fun TournamentDetailsHeader(
  modifier: Modifier = Modifier,
  tournamentDetails: TournamentDetails,
  isTracked: Boolean,
  onSubButton: suspend () -> Unit,
) {
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val notificationPermission =
    rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)

  val imageComponent = rememberImageComponent {
    add(CircularRevealPlugin())
  }

  CardView(modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
      Row(
        modifier
          .fillMaxWidth()
          .padding(Local16DPPadding.current)
      ) {
        Spacer(modifier = modifier.weight(1f))
        GlideImage(
          imageModel = { tournamentDetails.img },
          imageOptions =
          ImageOptions(
            contentScale = ContentScale.FillBounds,
            alignment = Alignment.CenterEnd,
            alpha = 0.3f,
            requestSize = IntSize(200, 200)
          ),
          component = imageComponent,
        )
      }
      Column(
        modifier
          .fillMaxWidth()
          .padding(Local8DPPadding.current)
      ) {
        Text(
          text = tournamentDetails.title,
          style = VLRTheme.typography.headlineSmall,
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
          modifier
            .fillMaxWidth()
            .padding(Local4DP_2DPPadding.current),
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
          modifier
            .fillMaxWidth()
            .padding(Local4DP_2DPPadding.current),
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
          modifier
            .fillMaxWidth()
            .padding(Local4DP_2DPPadding.current),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            Icons.Outlined.LocationOn,
            contentDescription = stringResource(R.string.location),
            tint = VLRTheme.colorScheme.primary,
          )
          Text(
            text = tournamentDetails.location.uppercase(),
            modifier = Modifier.padding(horizontal = 4.dp)
          )
        }
        Button(
          onClick = {
            (Constants.VLR_BASE + "event/" + tournamentDetails.id).openAsCustomTab(context)
          },
          modifier = modifier.fillMaxWidth(),
          shape = VLRTheme.shapes.small
        ) {
          Text(text = stringResource(id = R.string.view_at_vlr))
        }

        var processingTopicSubscription by remember { mutableStateOf(false) }

        if (
          tournamentDetails.status == TournamentDetails.Status.ONGOING ||
          tournamentDetails.status == TournamentDetails.Status.UPCOMING
        )
          Button(
            onClick = {
              if (notificationPermission.status.isGranted) {
                if (!processingTopicSubscription) {
                  processingTopicSubscription = true
                  scope.launch(Dispatchers.IO) {
                    onSubButton()
                    processingTopicSubscription = false
                  }
                }
              } else notificationPermission.launchPermissionRequest()
            },
            modifier = modifier.fillMaxWidth(),
            shape = VLRTheme.shapes.small
          ) {
            if (processingTopicSubscription) {
              LinearProgressIndicator()
            } else if (isTracked) Text(text = stringResource(R.string.unsubscribe))
            else Text(text = stringResource(R.string.get_notified))
          }
      }
    }
  }
}

@Composable
fun EventDetailsTeamSlider(
  modifier: Modifier = Modifier,
  list: StableHolder<List<TournamentDetails.Participant>>,
  onClick: (String) -> Unit,
) {
  val imageComponent = rememberImageComponent {
    add(CircularRevealPlugin())
  }
  val lazyListState = rememberLazyListState()
  Text(
    text = stringResource(R.string.teams),
    modifier = modifier
      .padding(Local16DPPadding.current)
      .testTag("eventDetails:teams"),
    style = VLRTheme.typography.titleMedium,
    color = VLRTheme.colorScheme.primary
  )
  LazyRow(
    modifier = modifier
      .fillMaxWidth()
      .testTag("eventDetails:teamList"),
    state = lazyListState
  ) {
    items(list.item, key = { list -> list.id }) {
      CardView(
        modifier
          .width(width = 150.dp)
          .aspectRatio(1f)
          .clickable { onClick(it.id) },
      ) {
        Column(
          modifier
            .fillMaxSize()
            .padding(Local8DPPadding.current),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = it.team,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = VLRTheme.typography.titleSmall,
            color = VLRTheme.colorScheme.primary,
          )
          GlideImage(
            imageModel = { it.img },
            imageOptions =
            ImageOptions(
              alignment = Alignment.Center,
            ),
            modifier = modifier
              .size(80.dp)
              .aspectRatio(1f)
              .padding(Local4DPPadding.current),
            component = imageComponent
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
  group: StableHolder<Map<String, List<TournamentDetails.Games>>>,
  tabSelection: Int,
  onFilterChange: (Int) -> Unit,
  onTabChange: (Int) -> Unit,
) {
  val filterOptions =
    listOf(
      stringResource(R.string.status),
      stringResource(R.string.rounds),
      stringResource(R.string.stage)
    )

  Column(
    modifier
      .fillMaxWidth()
      .padding(Local8DPPadding.current)
  ) {
    Text(
      text = stringResource(id = R.string.games),
      modifier = modifier.padding(Local8DPPadding.current),
      style = VLRTheme.typography.titleMedium,
      color = VLRTheme.colorScheme.primary
    )

    FilterChips(
      modifier,
      filterOptions,
      selectedIndex,
    ) {
      onFilterChange(it)
    }

    ScrollableTabRow(
      selectedTabIndex = tabSelection,
      containerColor = VLRTheme.colorScheme.primaryContainer,
      modifier =
      modifier
        .fillMaxWidth()
        .padding(Local8DPPadding.current)
        .clip(RoundedCornerShape(16.dp)),
      indicator = { indicators ->
        if (indicators.isNotEmpty()) VLRTabIndicator(indicators, tabSelection)
      }
    ) {
      group.item.keys.forEachIndexed { index, s ->
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
fun FilterChips(
  modifier: Modifier,
  filterOptions: List<String>,
  selectedIndex: Int,
  onFilterChange: (Int) -> Unit,
) {
  Row(
    modifier
      .fillMaxSize()
      .animateContentSize(),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
  ) {
    SingleChoiceSegmentedButtonRow {
      filterOptions.forEachIndexed { index, filter ->
        SegmentedButton(
          shape = SegmentedButtonDefaults.itemShape(index = index, count = filterOptions.size),
          onClick = { onFilterChange(index) },
          selected = index == selectedIndex
        ) {
          Text(filter)
        }
      }
    }
  }
}

@Composable
fun TournamentMatchOverview(
  modifier: Modifier = Modifier,
  game: TournamentDetails.Games,
  onClick: (String) -> Unit,
) {
  CardView(
    modifier = modifier.clickable { onClick(game.id) },
  ) {
    Column(modifier = modifier.padding(Local8DPPadding.current)) {
      Text(
        text = game.status.replaceFirstChar { it.uppercase() },
        modifier = modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = VLRTheme.typography.bodyMedium
      )

      Row(
        modifier = modifier.padding(Local4DPPadding.current),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = game.teams[0].name,
          style = VLRTheme.typography.titleMedium,
          modifier = modifier.weight(1f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = game.teams[0].score?.toString() ?: "-",
          style = VLRTheme.typography.titleMedium,
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
          style = VLRTheme.typography.titleMedium,
          modifier = modifier.weight(1f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = game.teams[1].score?.toString() ?: "-",
          style = VLRTheme.typography.titleMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      if (!game.time.equals("TBD", ignoreCase = true))
        Text(
          text = "${game.time} ${game.date}".patternDateTimeToReadable,
          modifier = modifier
            .fillMaxWidth()
            .padding(Local8DPPadding.current),
          textAlign = TextAlign.Center,
          style = VLRTheme.typography.labelMedium
        )
    }
  }
}

private fun String.toEventTopic() = "event-$this"
