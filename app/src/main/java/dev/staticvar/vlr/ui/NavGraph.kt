package dev.staticvar.vlr.ui

import androidx.compose.runtime.Stable
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder

private object Destinations {
  const val MATCH_OVERVIEW = "match_overview"
  const val NEWS_OVERVIEW = "news_overview"
  const val EVENTS_OVERVIEW = "event_overview"
  const val NEWS = "news"
  const val MATCH = "match"
  const val EVENT = "event"
  const val TEAM = "team"
  const val ABOUT = "about"
  const val RANK = "rank"
  const val PLAYER = "player"
}

sealed class Destination(val route: String) {
  object MatchOverview : Destination(Destinations.MATCH_OVERVIEW)
  object NewsOverview : Destination(Destinations.NEWS_OVERVIEW)
  object EventOverview : Destination(Destinations.EVENTS_OVERVIEW)
  object About : Destination(Destinations.ABOUT)
  object Rank : Destination(Destinations.RANK)
  object Match : Destination("${Destinations.MATCH}/{${Args.ID}}") {
    object Args {
      const val ID = "id"
    }
  }

  object Event : Destination("${Destinations.EVENT}/{${Args.ID}}") {
    object Args {
      const val ID = "id"
    }
  }

  object Team : Destination("${Destinations.TEAM}/{${Args.ID}}") {
    object Args {
      const val ID = "id"
    }
  }

  object News : Destination("${Destinations.NEWS}/{${Args.ID}}") {
    object Args {
      const val ID = "id"
    }
  }

  object Player : Destination("${Destinations.PLAYER}/{${Args.ID}}") {
    object Args {
      const val ID = "id"
    }
  }
}

@Stable
class Action(private val navController: NavHostController) {
  val pop: () -> Unit = { navController.popBackStack() }

  val matchOverview: () -> Unit = {
    navController.navigate(Destinations.MATCH_OVERVIEW, builder = { navConfig(navController) })
  }

  val goNews: () -> Unit = {
    navController.navigate(Destinations.NEWS_OVERVIEW, builder = { navConfig(navController) })
  }

  val goEvents: () -> Unit = {
    navController.navigate(Destinations.EVENTS_OVERVIEW, builder = { navConfig(navController) })
  }

  val goAbout: () -> Unit = {
    navController.navigate(Destinations.ABOUT, builder = { navConfig(navController) })
  }

  val goRanks: () -> Unit = {
    navController.navigate(Destinations.RANK, builder = { navConfig(navController) })
  }

  val match: (String) -> Unit = { id -> navController.navigate("${Destinations.MATCH}/$id") }

  val event: (String) -> Unit = { id -> navController.navigate("${Destinations.EVENT}/$id") }

  val team: (String) -> Unit = { id -> navController.navigate("${Destinations.TEAM}/$id") }

  val news: (String) -> Unit = { id -> navController.navigate("${Destinations.NEWS}/$id") }

  val player: (String) -> Unit = { id -> navController.navigate("${Destinations.PLAYER}/$id") }
}

private fun NavOptionsBuilder.navConfig(navController: NavController) {
  // Pop up to the start destination of the graph to
  // avoid building up a large stack of destinations
  // on the back stack as users select items
  popUpTo(navController.graph.findStartDestination().id) { saveState = true }
  // Avoid multiple copies of the same destination when
  // re-selecting the same item
  launchSingleTop = true
  // Restore state when re-selecting a previously selected item
//  restoreState = true
}
