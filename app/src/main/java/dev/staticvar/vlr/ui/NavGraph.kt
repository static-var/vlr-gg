package dev.staticvar.vlr.ui

import androidx.compose.runtime.Stable
import androidx.navigation.NavHostController

private object Destinations {
  const val MATCH_OVERVIEW = "match_overview"
  const val NEWS_OVERVIEW = "news_overview"
  const val EVENTS_OVERVIEW = "event_overview"
  const val NEWS = "news"
  const val MATCH = "match"
  const val EVENT = "event"
  const val TEAM = "team"
  const val ABOUT = "about"
}

sealed class Destination(val route: String) {
  object MatchOverview : Destination(Destinations.MATCH_OVERVIEW)
  object NewsOverview : Destination(Destinations.NEWS_OVERVIEW)
  object EventOverview : Destination(Destinations.EVENTS_OVERVIEW)
  object About : Destination(Destinations.ABOUT)
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
}

@Stable
class Action(private val navController: NavHostController) {
  val pop: () -> Unit = { navController.popBackStack() }

  val matchOverview: () -> Unit = { navController.navigate(Destinations.MATCH_OVERVIEW) }

  val goNews: () -> Unit = { navController.navigate(Destinations.NEWS_OVERVIEW) }

  val goEvents: () -> Unit = { navController.navigate(Destinations.EVENTS_OVERVIEW) }

  val goAbout: () -> Unit = { navController.navigate(Destinations.ABOUT) }

  val match: (String) -> Unit = { id -> navController.navigate("${Destinations.MATCH}/$id") }

  val event: (String) -> Unit = { id -> navController.navigate("${Destinations.EVENT}/$id") }

  val team: (String) -> Unit = { id -> navController.navigate("${Destinations.TEAM}/$id") }

  val news: (String) -> Unit = { id -> navController.navigate("${Destinations.NEWS}/$id") }
}
