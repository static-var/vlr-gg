package dev.unusedvariable.vlr.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dev.unusedvariable.vlr.data.NavState
import dev.unusedvariable.vlr.ui.events.EventDetails
import dev.unusedvariable.vlr.ui.events.EventScreen
import dev.unusedvariable.vlr.ui.match.MatchOverview
import dev.unusedvariable.vlr.ui.match.NewMatchDetails
import dev.unusedvariable.vlr.ui.news.NewsScreen
import dev.unusedvariable.vlr.ui.theme.VLRTheme
import dev.unusedvariable.vlr.utils.fadeOut
import dev.unusedvariable.vlr.utils.slideInFromBottom
import dev.unusedvariable.vlr.utils.slideInFromTop
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VLR() {
  val navController = rememberAnimatedNavController()
  val viewModel: VlrViewModel = hiltViewModel()
  val action = remember(navController) { Action(navController) }

  val systemUiController = rememberSystemUiController()
  val primaryContainer = VLRTheme.colorScheme.primaryContainer
  val background = VLRTheme.colorScheme.primaryContainer.copy(0.1f)

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
          NavigationBar() {
            NavigationBarItem(
                selected = navState == NavState.NEWS,
                icon = { Icon(imageVector = Icons.Outlined.Feed, contentDescription = "News") },
                label = { Text(text = "News") },
                onClick = action.goNews)
            NavigationBarItem(
                selected = navState == NavState.MATCH_OVERVIEW,
                icon = { Icon(imageVector = Icons.Outlined.SportsEsports, contentDescription = "Games") },
                label = { Text(text = "Matches") },
                onClick = action.matchOverview)
            NavigationBarItem(
                selected = navState == NavState.TOURNAMENT,
                icon = {
                  Icon(imageVector = Icons.Outlined.EmojiEvents, contentDescription = "Tournament")
                },
                label = { Text(text = "Events") },
                onClick = action.goEvents)
          }
        }
      },
  ) { paddingValues ->
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
            popExitTransition = { fadeOut }) {
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
