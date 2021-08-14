package dev.unusedvariable.vlr.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.navArgument
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dev.unusedvariable.vlr.data.NavState
import dev.unusedvariable.vlr.ui.match.MatchDetails
import dev.unusedvariable.vlr.ui.results.ResultsScreen
import dev.unusedvariable.vlr.ui.schedule.SchedulePage
import dev.unusedvariable.vlr.ui.theme.VLRTheme
import dev.unusedvariable.vlr.utils.*

@Composable
fun VLR() {
    val navController = rememberAnimatedNavController()
    val viewModel: VlrViewModel = hiltViewModel()
    val action = remember(navController) {
        Action(navController)
    }

    val systemUiController = rememberSystemUiController()
    systemUiController.setStatusBarColor(
        color = Color.Transparent,
        darkIcons = !isSystemInDarkTheme()
    )
    systemUiController.setNavigationBarColor(
        color = if (isSystemInDarkTheme()) VLRTheme.colors.background else VLRTheme.colors.primary,
        darkIcons = false
    )

    viewModel.action = action

    val navState: NavState by viewModel.navState.collectAsState()

    Scaffold(
        bottomBar = {
            AnimatedVisibility(visible = navState != NavState.MATCH_DETAILS) {
                BottomNavigation(
                    modifier = Modifier
                        .navigationBarsPadding(true)
                        .animateContentSize()
                ) {
                    BottomNavigationItem(
                        selected = navState == NavState.UPCOMING,
                        icon = {},
                        label = { Text(text = "Schedule", style = VLRTheme.typography.body1) },
                        onClick = action.goUpcoming
                    )
                    BottomNavigationItem(
                        selected = navState == NavState.RESULTS,
                        icon = {},
                        label = { Text(text = "Results", style = VLRTheme.typography.body1) },
                        onClick = action.goResults
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())) {
            AnimatedNavHost(
                navController = navController,
                startDestination = Destination.Schedule.route
            ) {
                composable(
                    Destination.Schedule.route,
                    enterTransition = { _, _ ->
                        slideInFromLeft
                    },
                    popEnterTransition = { _, _ ->
                        slideInFromBottom
                    },
                    exitTransition = { _, _ -> fadeOut },
                    popExitTransition = { _, _ -> fadeOut },
                ) {
                    viewModel.setNavigation(NavState.UPCOMING)
                    SchedulePage(
                        viewModel = viewModel
                    )
                }
                composable(
                    Destination.Results.route,
                    enterTransition = { _, _ ->
                        slideInFromRight
                    },
                    popEnterTransition = { _, _ ->
                        slideInFromBottom
                    },
                    exitTransition = { _, _ -> fadeOut },
                    popExitTransition = { _, _ -> fadeOut },
                ) {
                    viewModel.setNavigation(NavState.RESULTS)
                    ResultsScreen(viewModel = viewModel)
                }
                composable(
                    Destination.Match.route,
                    arguments = listOf(navArgument(Destination.Match.Args.ID) {
                        type = NavType.StringType
                    }),
                    enterTransition = { _, _ ->
                        slideInFromTop
                    },
                    popEnterTransition = { _, _ ->
                        slideInFromTop
                    },
                    exitTransition = { _, _ -> fadeOut },
                    popExitTransition = { _, _ -> fadeOut }
                ) {
                    viewModel.setNavigation(NavState.MATCH_DETAILS)
                    val id = it.arguments?.getString(Destination.Match.Args.ID) ?: ""
                    MatchDetails(viewModel = viewModel, matchUrl = id)
                }
            }
        }
    }
}