package dev.unusedvariable.vlr.ui

import androidx.navigation.NavHostController

private object Destinations {
    const val MATCH_OVERVIEW = "schedule"
    const val MATCH = "match"
    const val NEWS = "news"
    const val EVENTS = "events"
}

sealed class Destination(val route: String) {
    object MatchOverview : Destination(Destinations.MATCH_OVERVIEW)
    object News : Destination(Destinations.NEWS)
    object Events : Destination(Destinations.EVENTS)
    object Match : Destination("${Destinations.MATCH}/{${Args.ID}}") {
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
        navController.navigate(Destinations.EVENTS)
    }

    val match: (String) -> Unit = { id ->
        navController.navigate("${Destinations.MATCH}/$id")
    }
}