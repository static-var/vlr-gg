package dev.unusedvariable.vlr.ui

import androidx.navigation.NavHostController

private object Destinations {
    const val MATCH_OVERVIEW = "match_overview"
    const val MATCH = "match"
    const val EVENT = "event"
    const val NEWS = "news"
    const val EVENTS_OVERVIEW = "event_overview"
}

sealed class Destination(val route: String) {
    object MatchOverview : Destination(Destinations.MATCH_OVERVIEW)
    object News : Destination(Destinations.NEWS)
    object EventOverview : Destination(Destinations.EVENTS_OVERVIEW)
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
}

class Action(private val navController: NavHostController) {
    val pop: () -> Unit = {
        navController.popBackStack()
    }

    val matchOverview: () -> Unit = {
        navController.navigate(Destinations.MATCH_OVERVIEW)
    }

    val goNews: () -> Unit = {
        navController.navigate(Destinations.NEWS)
    }

    val goEvents: () -> Unit = {
        navController.navigate(Destinations.EVENTS_OVERVIEW)
    }

    val match: (String) -> Unit = { id ->
        navController.navigate("${Destinations.MATCH}/$id")
    }

    val event: (String) -> Unit = { id ->
        navController.navigate("${Destinations.EVENT}/$id")
    }
}