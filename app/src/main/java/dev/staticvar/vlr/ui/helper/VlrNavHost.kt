package dev.staticvar.vlr.ui.helper

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import dev.staticvar.vlr.ui.Destination
import dev.staticvar.vlr.ui.VlrViewModel
import dev.staticvar.vlr.ui.about.AboutScreen
import dev.staticvar.vlr.ui.events.EventDetails
import dev.staticvar.vlr.ui.events.EventOverviewAdaptive
import dev.staticvar.vlr.ui.match.MatchOverviewAdaptive
import dev.staticvar.vlr.ui.match.details_ui.NewMatchDetails
import dev.staticvar.vlr.ui.news.NewsDetailsScreen
import dev.staticvar.vlr.ui.news.NewsScreenAdaptive
import dev.staticvar.vlr.ui.player.PlayerDetailsScreen
import dev.staticvar.vlr.ui.team_rank.RankScreenAdaptive
import dev.staticvar.vlr.ui.team_rank.TeamScreen
import dev.staticvar.vlr.utils.Constants
import dev.staticvar.vlr.utils.fadeIn
import dev.staticvar.vlr.utils.fadeOut
import dev.staticvar.vlr.utils.slideInFromBottom

@Composable
fun VlrNavHost(navController: NavHostController, onNavigation: (String) -> Unit) {
  val viewModel: VlrViewModel = hiltViewModel()

  NavHost(
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
      onNavigation(Destination.NewsOverview.route)
      NewsScreenAdaptive(viewModel = viewModel)
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
      onNavigation(Destination.MatchOverview.route)
      MatchOverviewAdaptive(viewModel = viewModel)
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
      onNavigation(Destination.EventOverview.route)
      EventOverviewAdaptive(viewModel = viewModel)
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
      onNavigation(Destination.About.route)
      AboutScreen()
    }
    composable(
      Destination.Rank.route,
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
      onNavigation(Destination.Rank.route)
      RankScreenAdaptive(viewModel = viewModel)
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
        },
        navDeepLink {
          uriPattern =
            "${Constants.DEEP_LINK_2_BASEURL}${Destination.Match.Args.ID}={${Destination.Match.Args.ID}}"
        }
      )
    ) {
      val id = it.arguments?.getString(Destination.Match.Args.ID) ?: ""
      onNavigation(Destination.Match.route.replace(Destination.Match.Args.ID, id))
      NewMatchDetails(viewModel = viewModel, id = id)
    }
    composable(
      Destination.Event.route,
      arguments = listOf(navArgument(Destination.Event.Args.ID) { type = NavType.StringType }),
      enterTransition = { fadeIn },
      popEnterTransition = { fadeIn },
      exitTransition = { fadeOut },
      popExitTransition = { fadeOut },
      deepLinks =
      listOf(
        navDeepLink {
          uriPattern =
            "${Constants.DEEP_LINK_BASEURL}${Destination.Event.route}={${Destination.Event.Args.ID}}"
        },
        navDeepLink {
          uriPattern =
            "${Constants.DEEP_LINK_2_BASEURL}${Destination.Event.route}={${Destination.Event.Args.ID}}"
        }
      )
    ) {
      val id = it.arguments?.getString(Destination.Event.Args.ID) ?: ""
      onNavigation(Destination.Event.route.replace(Destination.Event.Args.ID, id))
      EventDetails(viewModel = viewModel, id = id)
    }
    composable(
      Destination.Team.route,
      arguments = listOf(navArgument(Destination.Team.Args.ID) { type = NavType.StringType }),
      enterTransition = { fadeIn },
      popEnterTransition = { fadeIn },
      exitTransition = { fadeOut },
      popExitTransition = { fadeOut },
      deepLinks = listOf(
        navDeepLink {
          uriPattern =
            "${Constants.DEEP_LINK_BASEURL}${Destination.Team.route}={${Destination.Team.Args.ID}}"
        },
        navDeepLink {
          uriPattern =
            "${Constants.DEEP_LINK_2_BASEURL}${Destination.Team.route}={${Destination.Team.Args.ID}}"
        },
      )
    ) {
      val id = it.arguments?.getString(Destination.Team.Args.ID) ?: ""
      onNavigation(Destination.Event.route.replace(Destination.Team.Args.ID, id))
      TeamScreen(viewModel = viewModel, id = id)
    }
    composable(
      Destination.Player.route,
      arguments = listOf(navArgument(Destination.Player.Args.ID) { type = NavType.StringType }),
      enterTransition = { fadeIn },
      popEnterTransition = { fadeIn },
      exitTransition = { fadeOut },
      popExitTransition = { fadeOut },
      deepLinks = listOf(
        navDeepLink {
          uriPattern =
            "${Constants.DEEP_LINK_BASEURL}${Destination.Player.route}={${Destination.Player.Args.ID}}"
        },
        navDeepLink {
          uriPattern =
            "${Constants.DEEP_LINK_2_BASEURL}${Destination.Player.route}={${Destination.Player.Args.ID}}"
        },
      )
    ) {
      val id = it.arguments?.getString(Destination.Player.Args.ID) ?: ""
      onNavigation(Destination.Player.route.replace(Destination.Player.Args.ID, id))
      PlayerDetailsScreen(viewModel = viewModel, id = id)
    }
    composable(
      Destination.News.route,
      arguments = listOf(navArgument(Destination.News.Args.ID) { type = NavType.StringType }),
      enterTransition = { fadeIn },
      popEnterTransition = { fadeIn },
      exitTransition = { fadeOut },
      popExitTransition = { fadeOut },
      deepLinks =
      listOf(
        navDeepLink {
          uriPattern =
            "${Constants.DEEP_LINK_BASEURL}news={${Destination.News.Args.ID}}"
        },
        navDeepLink {
          uriPattern =
            "${Constants.DEEP_LINK_2_BASEURL}news={${Destination.News.Args.ID}}"
        }
      )
    ) {
      onNavigation(Destination.News.route)
      val id = it.arguments?.getString(Destination.News.Args.ID) ?: ""
      NewsDetailsScreen(viewModel = viewModel, id = id)
    }
  }
}
