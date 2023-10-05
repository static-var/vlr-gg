package dev.staticvar.vlr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Feed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Feed
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Scaffold
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dev.staticvar.vlr.R
import dev.staticvar.vlr.ui.helper.NavItem
import dev.staticvar.vlr.ui.helper.VlrNavBar
import dev.staticvar.vlr.ui.helper.VlrNavHost
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.ui.theme.tintedBackground
import dev.staticvar.vlr.utils.FirebaseLogger

@Composable
fun VLR() {
  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()

  val viewModel: VlrViewModel = hiltViewModel()
  val action = remember(navController) { Action(navController) }

  val crashlytics by remember {
    mutableStateOf(FirebaseLogger)
  }

  val systemUiController = rememberSystemUiController()
  val background = VLRTheme.colorScheme.surfaceColorAtElevation(8.dp)
  val transparent = Color.Transparent

  viewModel.action = action

  val resetScroll = { viewModel.resetScroll() }

  var currentNav by remember { mutableStateOf(Destination.NewsOverview.route) }
  val currentDestination = backStackEntry?.destination?.route

  val navItems =
    listOf<NavItem>(
      NavItem(
        title = stringResource(id = R.string.news),
        Destination.NewsOverview.route,
        Icons.Filled.Feed,
        Icons.Outlined.Feed,
        onClick = {
          if (currentNav == Destination.NewsOverview.route) resetScroll() else action.goNews()
        }
      ),
      NavItem(
        title = stringResource(id = R.string.matches),
        Destination.MatchOverview.route,
        Icons.Filled.SportsEsports,
        Icons.Outlined.SportsEsports,
        onClick = {
          if (currentNav == Destination.MatchOverview.route) resetScroll()
          else action.matchOverview()
        }
      ),
      NavItem(
        title = stringResource(id = R.string.events),
        Destination.EventOverview.route,
        Icons.Filled.EmojiEvents,
        Icons.Outlined.EmojiEvents,
        onClick = {
          if (currentNav == Destination.EventOverview.route) resetScroll() else action.goEvents()
        }
      ),
      NavItem(
        title = stringResource(id = R.string.rank),
        Destination.Rank.route,
        Icons.Filled.Leaderboard,
        Icons.Outlined.Leaderboard,
        onClick = { if (currentNav == Destination.Rank.route) resetScroll() else action.goRanks() }
      ),
      NavItem(
        title = stringResource(id = R.string.about),
        Destination.About.route,
        Icons.Filled.Info,
        Icons.Outlined.Info,
        onClick = { if (currentNav != Destination.About.route) action.goAbout() }
      ),
    )

  LaunchedEffect(currentNav) {
    systemUiController.setNavigationBarColor(
      color =
        when (currentNav) {
          Destination.NewsOverview.route,
          Destination.MatchOverview.route,
          Destination.EventOverview.route,
          Destination.Rank.route,
          Destination.About.route -> background
          else -> transparent
        },
    )
    crashlytics.setDestinationKey(currentNav)
  }

  Scaffold(
    bottomBar = {
      VlrNavBar(
        navController = navController,
        items = navItems,
        isVisible = navItems.any { it.route == currentDestination }
      )
    },
    contentWindowInsets = WindowInsets(left = 0.dp, top = 0.dp, right = 0.dp, bottom = 0.dp)
  ) { paddingValues ->
    Box(
      modifier =
        Modifier.padding(paddingValues)
          .background(VLRTheme.colorScheme.tintedBackground)
          .semantics { testTagsAsResourceId = true },
    ) {
      VlrNavHost(navController = navController) { currentNav = it }
    }
  }
}
