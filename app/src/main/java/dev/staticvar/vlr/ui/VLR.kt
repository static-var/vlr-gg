package dev.staticvar.vlr.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Feed
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.utils.Constants
import dev.staticvar.vlr.utils.fadeOut
import dev.staticvar.vlr.utils.slideInFromBottom
import dev.staticvar.vlr.utils.slideInFromTop
import kotlinx.coroutines.delay

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

  Scaffold(
      bottomBar = {
        AnimatedVisibility(
            visible =
                navState != NavState.MATCH_DETAILS && navState != NavState.TOURNAMENT_DETAILS) {
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
        deepLinks = listOf(navDeepLink { uriPattern = "${Constants.DEEP_LINK_BASEURL}${Destination.Match.Args.ID}={${Destination.Match.Args.ID}}" })) {
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
      }
    }
  }
}

const val COLOR_ALPHA = 0.1f
const val CARD_ALPHA = 0.3f
