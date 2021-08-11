package dev.unusedvariable.vlr.ui

import androidx.navigation.NavController

private object Destinations {
    const val RESULTS = "results"
    const val SCHEDULE = "schedule"
    const val MATCH = "match"
}

sealed class Destination(val route: String) {
    object Results : Destination(Destinations.RESULTS)
    object Schedule : Destination(Destinations.SCHEDULE)
    object Match : Destination("${Destinations.MATCH}/{${Args.ID}}") {
        object Args {
            const val ID = "id"
        }
    }
}

class Action(private val navController: NavController) {
    val pop: () -> Unit = {
        navController.popBackStack()
    }

    val goUpcoming: () -> Unit = {
        navController.navigate(Destinations.SCHEDULE)
    }

    val goResults: () -> Unit = {
        navController.navigate(Destinations.RESULTS)
    }

    val match: (String) -> Unit = { id ->
        navController.navigate("${Destinations.MATCH}/$id")
    }
}