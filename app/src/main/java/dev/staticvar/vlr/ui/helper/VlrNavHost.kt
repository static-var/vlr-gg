package dev.staticvar.vlr.ui.helper

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import dev.staticvar.vlr.data.NavState
import dev.staticvar.vlr.ui.Destination
import dev.staticvar.vlr.ui.VlrViewModel
import dev.staticvar.vlr.ui.about.AboutScreen
import dev.staticvar.vlr.ui.events.EventDetails
import dev.staticvar.vlr.ui.events.EventScreen
import dev.staticvar.vlr.ui.match.MatchOverview
import dev.staticvar.vlr.ui.match.NewMatchDetails
import dev.staticvar.vlr.ui.news.NewsDetailsScreen
import dev.staticvar.vlr.ui.news.NewsScreen
import dev.staticvar.vlr.ui.team.TeamScreen
import dev.staticvar.vlr.utils.Constants
import dev.staticvar.vlr.utils.fadeIn
import dev.staticvar.vlr.utils.fadeOut
import dev.staticvar.vlr.utils.slideInFromBottom

@Composable
fun VlrNavHost(navController: NavHostController) {
  val viewModel: VlrViewModel = hiltViewModel()
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
