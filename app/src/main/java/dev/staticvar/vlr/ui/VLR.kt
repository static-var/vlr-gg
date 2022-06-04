package dev.staticvar.vlr.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Feed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Feed
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dev.staticvar.vlr.R
import dev.staticvar.vlr.data.NavState
import dev.staticvar.vlr.ui.about.AboutScreen
import dev.staticvar.vlr.ui.events.EventDetails
import dev.staticvar.vlr.ui.events.EventScreen
import dev.staticvar.vlr.ui.helper.AppUpdateDownloadPopup
import dev.staticvar.vlr.ui.helper.currentAppVersion
import dev.staticvar.vlr.ui.match.MatchOverview
import dev.staticvar.vlr.ui.match.NewMatchDetails
import dev.staticvar.vlr.ui.news.NewsDetailsScreen
import dev.staticvar.vlr.ui.news.NewsScreen
import dev.staticvar.vlr.ui.team.TeamScreen
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.ui.theme.tintedBackground
import dev.staticvar.vlr.utils.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VLR() {
  val navController = rememberAnimatedNavController()
  val viewModel: VlrViewModel = hiltViewModel()
  val action = remember(navController) { Action(navController) }

  val systemUiController = rememberSystemUiController()
  val background = VLRTheme.colorScheme.surface
  val transparent = Color.Transparent

  viewModel.action = action

  val navState: NavState by viewModel.navState.collectAsState(NavState.NEWS_OVERVIEW)

  val fontFamilyResolver = LocalFontFamilyResolver.current
  LaunchedEffect(Unit) {
    //    fontFamilyResolver.preload(appFontFamily) Crashes on 1.2.0-alpha07
  }

  LaunchedEffect(navState) {
    systemUiController.setNavigationBarColor(
      color =
        when (navState) {
          NavState.MATCH_DETAILS,
          NavState.TOURNAMENT_DETAILS,
          NavState.TEAM_DETAILS,
          NavState.NEWS -> transparent
          NavState.NEWS_OVERVIEW,
          NavState.TOURNAMENT,
          NavState.MATCH_OVERVIEW,
          NavState.ABOUT -> background
        },
    )
  }

  val context = LocalContext.current
  val currentAppVersion = context.currentAppVersion
  val remoteAppVersion by
    remember(viewModel) { viewModel.getLatestAppVersion() }.collectAsState(initial = null)

  val needAppUpdate by
    produceState<Boolean>(initialValue = false, remoteAppVersion, currentAppVersion) {
      remoteAppVersion?.let { remoteVersion ->
        value =
          if (remoteVersion.trim().equals(currentAppVersion, ignoreCase = true)) {
            i { "No new version available" }
            false
          } else {
            i { "New version | Remote Version $remoteVersion | Local Version $currentAppVersion" }
            true
          }
      }
    }

  if (needAppUpdate) {
    AppUpdateDownloadPopup(viewModel)
  }

  Scaffold(
    bottomBar = {
      AnimatedContent(
        navState,
        transitionSpec = {
          fadeIn(animationSpec = tween(200, 200)) with
            fadeOut(animationSpec = tween(200)) using
            SizeTransform { initialSize, targetSize ->
              if (navState != NavState.MATCH_DETAILS &&
                  navState != NavState.TOURNAMENT_DETAILS &&
                  navState != NavState.TEAM_DETAILS &&
                  navState != NavState.NEWS
              ) {
                keyframes {
                  // Expand horizontally first.
                  IntSize(targetSize.width, initialSize.height) at 150
                  durationMillis = 300
                }
              } else {
                keyframes {
                  // Shrink vertically first.
                  IntSize(initialSize.width, targetSize.height) at 150
                  durationMillis = 300
                }
              }
            }
        }
      ) { targetState ->
        if (targetState != NavState.MATCH_DETAILS &&
            targetState != NavState.TOURNAMENT_DETAILS &&
            targetState != NavState.TEAM_DETAILS &&
            targetState != NavState.NEWS
        ) {
          NavigationBar(
            containerColor = VLRTheme.colorScheme.surface,
            contentColor = contentColorFor(VLRTheme.colorScheme.onSurface),
            tonalElevation = 0.dp,
            modifier = Modifier.navigationBarsPadding()
          ) {
            NavigationBarItem(
              selected = navState == NavState.NEWS_OVERVIEW,
              icon = {
                Icon(
                  imageVector =
                    if (navState == NavState.NEWS_OVERVIEW) Icons.Filled.Feed
                    else Icons.Outlined.Feed,
                  contentDescription = stringResource(R.string.news),
                  tint = VLRTheme.colorScheme.onPrimaryContainer
                )
              },
              label = { Text(text = stringResource(R.string.news)) },
              onClick = if (navState == NavState.NEWS_OVERVIEW) EMPTY else action.goNews,
            )
            NavigationBarItem(
              selected = navState == NavState.MATCH_OVERVIEW,
              icon = {
                Icon(
                  imageVector =
                    if (navState == NavState.MATCH_OVERVIEW) Icons.Filled.SportsEsports
                    else Icons.Outlined.SportsEsports,
                  contentDescription = stringResource(R.string.games),
                  tint = VLRTheme.colorScheme.onPrimaryContainer
                )
              },
              label = { Text(text = stringResource(R.string.matches)) },
              onClick = if (navState == NavState.MATCH_OVERVIEW) EMPTY else action.matchOverview
            )
            NavigationBarItem(
              selected = navState == NavState.TOURNAMENT,
              icon = {
                Icon(
                  imageVector =
                    if (navState == NavState.TOURNAMENT) Icons.Filled.EmojiEvents
                    else Icons.Outlined.EmojiEvents,
                  contentDescription = stringResource(R.string.tournament),
                  tint = VLRTheme.colorScheme.onPrimaryContainer
                )
              },
              label = { Text(text = stringResource(R.string.events)) },
              onClick = if (navState == NavState.TOURNAMENT) EMPTY else action.goEvents
            )
            NavigationBarItem(
              selected = navState == NavState.ABOUT,
              icon = {
                Icon(
                  imageVector =
                    if (navState == NavState.ABOUT) Icons.Filled.Info else Icons.Outlined.Info,
                  contentDescription = stringResource(R.string.about),
                  tint = VLRTheme.colorScheme.onPrimaryContainer
                )
              },
              label = { Text(text = stringResource(R.string.about)) },
              onClick = if (navState == NavState.ABOUT) EMPTY else action.goAbout
            )
          }
        }
      }
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier.padding(paddingValues).background(VLRTheme.colorScheme.tintedBackground),
    ) {
      AnimatedNavHost(
        navController = navController,
        startDestination = Destination.NewsOverview.route
      ) {
        composable(
          Destination.NewsOverview.route,
          enterTransition = {
            if (targetState.destination.route == initialState.destination.route) null
            else slideInFromBottom
          },
          popEnterTransition = {
            if (targetState.destination.route == initialState.destination.route) null
            else slideInFromBottom
          },
          exitTransition = {
            if (targetState.destination.route == initialState.destination.route) null else fadeOut
          },
          popExitTransition = {
            if (targetState.destination.route == initialState.destination.route) null else fadeOut
          },
        ) {
          viewModel.setNavigation(NavState.NEWS_OVERVIEW)
          NewsScreen(viewModel = viewModel)
        }
        composable(
          Destination.MatchOverview.route,
          enterTransition = {
            if (targetState.destination.route == initialState.destination.route) null
            else slideInFromBottom
          },
          popEnterTransition = {
            if (targetState.destination.route == initialState.destination.route) null
            else slideInFromBottom
          },
          exitTransition = {
            if (targetState.destination.route == initialState.destination.route) null else fadeOut
          },
          popExitTransition = {
            if (targetState.destination.route == initialState.destination.route) null else fadeOut
          },
        ) {
          viewModel.setNavigation(NavState.MATCH_OVERVIEW)
          MatchOverview(viewModel = viewModel)
        }
        composable(
          Destination.EventOverview.route,
          enterTransition = {
            if (targetState.destination.route == initialState.destination.route) null
            else slideInFromBottom
          },
          popEnterTransition = {
            if (targetState.destination.route == initialState.destination.route) null
            else slideInFromBottom
          },
          exitTransition = {
            if (targetState.destination.route == initialState.destination.route) null else fadeOut
          },
          popExitTransition = {
            if (targetState.destination.route == initialState.destination.route) null else fadeOut
          },
        ) {
          viewModel.setNavigation(NavState.TOURNAMENT)
          EventScreen(viewModel = viewModel)
        }
        composable(
          Destination.About.route,
          enterTransition = {
            if (targetState.destination.route == initialState.destination.route) null
            else slideInFromBottom
          },
          popEnterTransition = {
            if (targetState.destination.route == initialState.destination.route) null
            else slideInFromBottom
          },
          exitTransition = {
            if (targetState.destination.route == initialState.destination.route) null else fadeOut
          },
          popExitTransition = {
            if (targetState.destination.route == initialState.destination.route) null else fadeOut
          },
        ) {
          viewModel.setNavigation(NavState.ABOUT)
          AboutScreen(viewModel = viewModel)
        }
        composable(
          Destination.Match.route,
          arguments = listOf(navArgument(Destination.Match.Args.ID) { type = NavType.StringType }),
          enterTransition = { fadeIn },
          popEnterTransition = { fadeIn },
          exitTransition = { fadeOut },
          popExitTransition = { fadeOut },
          deepLinks =
            listOf(
              navDeepLink {
                uriPattern =
                  "${Constants.DEEP_LINK_BASEURL}${Destination.Match.Args.ID}={${Destination.Match.Args.ID}}"
              }
            )
        ) {
          viewModel.setNavigation(NavState.MATCH_DETAILS)
          val id = it.arguments?.getString(Destination.Match.Args.ID) ?: ""
          NewMatchDetails(viewModel = viewModel, id = id)
        }
        composable(
          Destination.Event.route,
          arguments = listOf(navArgument(Destination.Event.Args.ID) { type = NavType.StringType }),
          enterTransition = { fadeIn },
          popEnterTransition = { fadeIn },
          exitTransition = { fadeOut },
          popExitTransition = { fadeOut }
        ) {
          viewModel.setNavigation(NavState.TOURNAMENT_DETAILS)
          val id = it.arguments?.getString(Destination.Event.Args.ID) ?: ""
          EventDetails(viewModel = viewModel, id = id)
        }
        composable(
          Destination.Team.route,
          arguments = listOf(navArgument(Destination.Team.Args.ID) { type = NavType.StringType }),
          enterTransition = { fadeIn },
          popEnterTransition = { fadeIn },
          exitTransition = { fadeOut },
          popExitTransition = { fadeOut }
        ) {
          viewModel.setNavigation(NavState.TEAM_DETAILS)
          val id = it.arguments?.getString(Destination.Team.Args.ID) ?: ""
          TeamScreen(viewModel = viewModel, id = id)
        }
        composable(
          Destination.News.route,
          arguments = listOf(navArgument(Destination.News.Args.ID) { type = NavType.StringType }),
          enterTransition = { fadeIn },
          popEnterTransition = { fadeIn },
          exitTransition = { fadeOut },
          popExitTransition = { fadeOut }
        ) {
          viewModel.setNavigation(NavState.NEWS)
          val id = it.arguments?.getString(Destination.News.Args.ID) ?: ""
          NewsDetailsScreen(viewModel = viewModel, id = id)
        }
      }
    }
  }
}

val EMPTY = {}
