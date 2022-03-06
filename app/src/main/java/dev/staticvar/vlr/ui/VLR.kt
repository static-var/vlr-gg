package dev.staticvar.vlr.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Feed
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dev.staticvar.vlr.data.NavState
import dev.staticvar.vlr.ui.events.EventDetails
import dev.staticvar.vlr.ui.events.EventScreen
import dev.staticvar.vlr.ui.match.MatchOverview
import dev.staticvar.vlr.ui.match.NewMatchDetails
import dev.staticvar.vlr.ui.news.NewsScreen
import dev.staticvar.vlr.ui.team.TeamScreen
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VLR() {
  val navController = rememberAnimatedNavController()
  val viewModel: VlrViewModel = hiltViewModel()
  val action = remember(navController) { Action(navController) }

  val systemUiController = rememberSystemUiController()
  val primaryContainer = VLRTheme.colorScheme.primaryContainer
  val background = VLRTheme.colorScheme.primaryContainer.copy(0.2f)

  viewModel.action = action

  val navState: NavState by viewModel.navState.collectAsState()

  LaunchedEffect(key1 = navState) {
    delay(500)
    systemUiController.setStatusBarColor(
        color =
            if (navState == NavState.TOURNAMENT || navState == NavState.MATCH_OVERVIEW)
                primaryContainer
            else background,
    )
    systemUiController.setNavigationBarColor(
        color = background,
    )
  }

  val context = LocalContext.current
  val currentAppVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
  val remoteAppVersion by
      remember(viewModel) { viewModel.getLatestAppVersion() }.collectAsState(initial = null)

  val needAppUpdate by
      produceState<Boolean>(initialValue = false, remoteAppVersion, currentAppVersion) {
        remoteAppVersion?.let { remoteVersion ->
          if (remoteVersion.trim().equals(currentAppVersion, ignoreCase = true)) {
            e { "No new version available" }
            value = false
          } else {
            e { "New version available | $remoteVersion | $currentAppVersion" }
            value = true
          }
        }
      }

  if (needAppUpdate) {
    AppUpdateDownloadPopup(viewModel)
  }

  Scaffold(
      bottomBar = {
        AnimatedVisibility(
            visible =
                navState != NavState.MATCH_DETAILS &&
                    navState != NavState.TOURNAMENT_DETAILS &&
                    navState != NavState.TEAM_DETAILS) {
          NavigationBar(
              containerColor = VLRTheme.colorScheme.primaryContainer.copy(0.2f),
              contentColor = contentColorFor(VLRTheme.colorScheme.primaryContainer.copy(0.2f)),
              tonalElevation = 0.dp,
              modifier = Modifier.navigationBarsPadding()) {
            NavigationBarItem(
                selected = navState == NavState.NEWS,
                icon = {
                  Icon(
                      imageVector = Icons.Outlined.Feed,
                      contentDescription = "News",
                      tint = VLRTheme.colorScheme.onPrimaryContainer)
                },
                label = { Text(text = "News") },
                onClick = action.goNews,
            )
            NavigationBarItem(
                selected = navState == NavState.MATCH_OVERVIEW,
                icon = {
                  Icon(
                      imageVector = Icons.Outlined.SportsEsports,
                      contentDescription = "Games",
                      tint = VLRTheme.colorScheme.onPrimaryContainer)
                },
                label = { Text(text = "Matches") },
                onClick = action.matchOverview)
            NavigationBarItem(
                selected = navState == NavState.TOURNAMENT,
                icon = {
                  Icon(
                      imageVector = Icons.Outlined.EmojiEvents,
                      contentDescription = "Tournament",
                      tint = VLRTheme.colorScheme.onPrimaryContainer)
                },
                label = { Text(text = "Events") },
                onClick = action.goEvents)
          }
        }
      }) { paddingValues ->
    Box(
        modifier =
            Modifier.padding(bottom = paddingValues.calculateBottomPadding())
                .background(VLRTheme.colorScheme.primaryContainer.copy(COLOR_ALPHA)),
    ) {
      AnimatedNavHost(navController = navController, startDestination = Destination.News.route) {
        composable(
            Destination.News.route,
            enterTransition = { slideInFromBottom },
            popEnterTransition = { slideInFromBottom },
            exitTransition = { fadeOut },
            popExitTransition = { fadeOut },
        ) {
          viewModel.setNavigation(NavState.NEWS)
          NewsScreen(viewModel = viewModel)
        }
        composable(
            Destination.MatchOverview.route,
            enterTransition = { slideInFromBottom },
            popEnterTransition = { slideInFromBottom },
            exitTransition = { fadeOut },
            popExitTransition = { fadeOut },
        ) {
          viewModel.setNavigation(NavState.MATCH_OVERVIEW)
          MatchOverview(viewModel = viewModel)
        }
        composable(
            Destination.EventOverview.route,
            enterTransition = { slideInFromBottom },
            popEnterTransition = { slideInFromBottom },
            exitTransition = { fadeOut },
            popExitTransition = { fadeOut },
        ) {
          viewModel.setNavigation(NavState.TOURNAMENT)
          EventScreen(viewModel = viewModel)
        }
        composable(
            Destination.Match.route,
            arguments =
                listOf(navArgument(Destination.Match.Args.ID) { type = NavType.StringType }),
            enterTransition = { slideInFromTop },
            popEnterTransition = { slideInFromTop },
            exitTransition = { fadeOut },
            popExitTransition = { fadeOut },
            deepLinks =
                listOf(
                    navDeepLink {
                      uriPattern =
                          "${Constants.DEEP_LINK_BASEURL}${Destination.Match.Args.ID}={${Destination.Match.Args.ID}}"
                    })) {
          viewModel.setNavigation(NavState.MATCH_DETAILS)
          val id = it.arguments?.getString(Destination.Match.Args.ID) ?: ""
          NewMatchDetails(viewModel = viewModel, id = id)
        }
        composable(
            Destination.Event.route,
            arguments =
                listOf(navArgument(Destination.Event.Args.ID) { type = NavType.StringType }),
            enterTransition = { slideInFromTop },
            popEnterTransition = { slideInFromTop },
            exitTransition = { fadeOut },
            popExitTransition = { fadeOut }) {
          viewModel.setNavigation(NavState.TOURNAMENT_DETAILS)
          val id = it.arguments?.getString(Destination.Event.Args.ID) ?: ""
          EventDetails(viewModel = viewModel, id = id)
        }
        composable(
            Destination.Team.route,
            arguments = listOf(navArgument(Destination.Team.Args.ID) { type = NavType.StringType }),
            enterTransition = { slideInFromTop },
            popEnterTransition = { slideInFromTop },
            exitTransition = { fadeOut },
            popExitTransition = { fadeOut }) {
          viewModel.setNavigation(NavState.TEAM_DETAILS)
          val id = it.arguments?.getString(Destination.Team.Args.ID) ?: ""
          TeamScreen(viewModel = viewModel, id = id)
        }
      }
    }
  }
}

const val COLOR_ALPHA = 0.1f
const val CARD_ALPHA = 0.3f

@Composable
fun AppUpdateDownloadPopup(viewModel: VlrViewModel) {
  val url by remember { viewModel.getApkUrl() }.collectAsState(initial = null)
  var show by remember { mutableStateOf(true) }
  var downloadClicked by remember { mutableStateOf(false) }

  // initiate download process if dialog is still being shown and URL is not null
  val downloadProgress by
      produceState(initialValue = flowOf(Pair(0, ByteArray(0))), downloadClicked, url) {
            if (url != null && downloadClicked) {
              value = viewModel.downloadApkWithProgress(url!!)
            }
          }
          .value
          .collectAsState(initial = Pair(0, ByteArray(0)))

  // Once download is complete, initiate Installation process
  if (downloadProgress.first == 100 && downloadProgress.second.isNotEmpty()) {
    PackageHelper.convertByteToFileAndInstall(LocalContext.current, downloadProgress.second)
  }

  // Show dialog only when URL is available and make dialog dismissible only till download has not
  // begun
  if (show && url != null)
      AlertDialog(
          onDismissRequest = { if (!downloadClicked) show = false },
          title = { Text(text = "Update available") },
          text = {
            AnimatedVisibility(visible = show) {
              Column(
                  Modifier.fillMaxWidth(),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center) {
                if (downloadClicked) {
                  Text(
                      text =
                          if (downloadProgress.first == 100) "Download complete!"
                          else "Downloading... ${downloadProgress.first}%")
                  LinearProgressIndicator(
                      progress = downloadProgress.first.div(100f),
                  )
                } else {
                  Text(text = "Click download to download the update")
                }
              }
            }
          },
          confirmButton = {
            AnimatedVisibility(visible = show) {
              if (!downloadClicked)
                  Button(onClick = { downloadClicked = true }) { Text(text = "Download") }
            }
          },
          dismissButton = {
            if (!downloadClicked) Button(onClick = { show = false }) { Text(text = "Cancel") }
          })
}
