package dev.unusedvariable.vlr.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
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
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VLR() {
    val navController = rememberAnimatedNavController()
    val viewModel: VlrViewModel = hiltViewModel()
    val action = remember(navController) {
        Action(navController)
    }

    val systemUiController = rememberSystemUiController()
    val primaryContainer = VLRTheme.colorScheme.primaryContainer
    val background = VLRTheme.colorScheme.primaryContainer.copy(0.1f)
    val isDark = isSystemInDarkTheme()


    viewModel.action = action

    val navState: NavState by viewModel.navState.collectAsState()

    LaunchedEffect(key1 = navState) {
        delay(500)
        systemUiController.setStatusBarColor(
            color = background,
        )
        systemUiController.setNavigationBarColor(
            color = if (navState == NavState.MATCH_DETAILS) background else primaryContainer,
        )
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(visible = navState != NavState.MATCH_DETAILS) {
                BottomNavigation(
                    modifier = Modifier
                        .navigationBarsPadding(true)
                        .animateContentSize(),
                    backgroundColor = VLRTheme.colorScheme.primaryContainer,
                    contentColor = VLRTheme.colorScheme.onPrimaryContainer
                ) {
                    BottomNavigationItem(
                        selected = navState == NavState.UPCOMING,
                        icon = { Text(text = "Schedule", style = VLRTheme.typography.bodyMedium) },
                        onClick = action.goUpcoming
                    )
                    BottomNavigationItem(
                        selected = navState == NavState.RESULTS,
                        icon = { Text(text = "Results", style = VLRTheme.typography.bodyMedium) },
                        onClick = action.goResults
                    )
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(bottom = paddingValues.calculateBottomPadding())
                .background(VLRTheme.colorScheme.primaryContainer.copy(COLOR_ALPHA)),
        ) {
            AnimatedNavHost(
                navController = navController,
                startDestination = Destination.Schedule.route
            ) {
                composable(
                    Destination.Schedule.route,
                    enterTransition = { slideInFromLeft },
                    popEnterTransition = { slideInFromBottom },
                    exitTransition = { fadeOut },
                    popExitTransition = { fadeOut },
                ) {
                    viewModel.setNavigation(NavState.UPCOMING)
                    SchedulePage(
                        viewModel = viewModel
                    )
                }
                composable(
                    Destination.Results.route,
                    enterTransition = { slideInFromRight },
                    popEnterTransition = { slideInFromBottom },
                    exitTransition = { fadeOut },
                    popExitTransition = { fadeOut },
                ) {
                    viewModel.setNavigation(NavState.RESULTS)
                    ResultsScreen(viewModel = viewModel)
                }
                composable(
                    Destination.Match.route,
                    arguments = listOf(navArgument(Destination.Match.Args.ID) {
                        type = NavType.StringType
                    }),
                    enterTransition = { slideInFromTop },
                    popEnterTransition = { slideInFromTop },
                    exitTransition = { fadeOut },
                    popExitTransition = { fadeOut }
                ) {
                    viewModel.setNavigation(NavState.MATCH_DETAILS)
                    val id = it.arguments?.getString(Destination.Match.Args.ID) ?: ""
                    MatchDetails(viewModel = viewModel, matchUrl = id)
                }
            }
        }
    }
}

const val COLOR_ALPHA = 0.1f
const val CARD_ALPHA = 0.3f