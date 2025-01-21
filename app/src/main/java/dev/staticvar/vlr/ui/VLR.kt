package dev.staticvar.vlr.ui

import android.annotation.SuppressLint
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Feed
import androidx.compose.material.icons.automirrored.outlined.Feed
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.staticvar.vlr.R
import dev.staticvar.vlr.ui.helper.VlrNavHost
import dev.staticvar.vlr.ui.helper.plus
import dev.staticvar.vlr.ui.navabar.NavItem
import dev.staticvar.vlr.ui.navabar.TopSlot
import dev.staticvar.vlr.ui.navabar.VlrNavBar
import dev.staticvar.vlr.ui.theme.VLRTheme

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun VLR() {
  val hazeState = remember { HazeState() }
  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()

  val viewModel: VlrViewModel = hiltViewModel()
  val action = remember(navController) { Action(navController) }

  viewModel.action = action

  val resetScroll = {
    println("Resetting scroll")
    viewModel.resetScroll()
  }

  var currentNav by remember { mutableStateOf(Destination.NewsOverview.route) }
  val currentDestination = backStackEntry?.destination?.route

  val selectedMatchTypePosition by
    viewModel.selectedMatchTypePosition.collectAsStateWithLifecycle()

  val selectedEventTypePosition by
    viewModel.selectedEventTypePosition.collectAsStateWithLifecycle()

  val navItems =
    listOf<NavItem>(
      NavItem(
        title = stringResource(id = R.string.news),
        Destination.NewsOverview.route,
        Icons.AutoMirrored.Filled.Feed,
        Icons.AutoMirrored.Outlined.Feed,
        onClick = {
          if (currentNav == Destination.NewsOverview.route) resetScroll() else action.goNews()
        },
      ),
      NavItem(
        title = stringResource(id = R.string.matches),
        Destination.MatchOverview.route,
        Icons.Filled.SportsEsports,
        Icons.Outlined.SportsEsports,
        onClick = {
          if (currentNav == Destination.MatchOverview.route) resetScroll()
          else action.matchOverview().also { println("Match overview navigation") }
        },
        topSlot = TopSlot.MATCH,
      ),
      NavItem(
        title = stringResource(id = R.string.events),
        Destination.EventOverview.route,
        Icons.Filled.EmojiEvents,
        Icons.Outlined.EmojiEvents,
        onClick = {
          if (currentNav == Destination.EventOverview.route) resetScroll() else action.goEvents()
        },
        topSlot = TopSlot.EVENT,
      ),
      NavItem(
        title = stringResource(id = R.string.rank),
        Destination.Rank.route,
        Icons.Filled.Leaderboard,
        Icons.Outlined.Leaderboard,
        onClick = {
          // This causes crashes in certain scenarios.
          /*          if (currentNav == Destination.Rank.route)
            resetScroll()
          else */
          if (currentNav != Destination.Rank.route) action.goRanks()
        },
      ),
      NavItem(
        title = stringResource(id = R.string.about),
        Destination.About.route,
        Icons.Filled.Info,
        Icons.Outlined.Info,
        onClick = { if (currentNav != Destination.About.route) action.goAbout() },
      ),
    )

  var hideNav by remember { mutableStateOf(false) }

  val navSuiteType =
    NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())

  CompositionLocalProvider(LocalNavigationSuiteType provides navSuiteType) {
    when (navSuiteType) {
      NavigationSuiteType.NavigationBar,
      NavigationSuiteType.None -> {
        Scaffold(
          modifier = Modifier,
          bottomBar = {
            VlrNavBar(
              navController = navController,
              items = navItems,
              hazeState = hazeState,
              isVisible = !hideNav,
              selectedMatchType = selectedMatchTypePosition,
              selectedEventType = selectedEventTypePosition,
              changeMatchType = { viewModel.updateSelectedMatchTypePosition(it) },
              changeEventType = { viewModel.updateSelectedEventTypePosition(it) },
            )
          },
        ) { innerPadding ->
          Box(modifier = Modifier.hazeSource(hazeState).semantics { testTagsAsResourceId = true }) {
            VlrNavHost(
              navController = navController,
              innerPadding = innerPadding,
              paneState = { nav -> hideNav = nav },
            ) {
              currentNav = it
            }
          }
        }
      }
      else -> {
        NavigationSuiteScaffold(
          navigationSuiteItems = {
            navItems.forEach { navItem ->
              item(
                selected = currentDestination == navItem.route,
                icon = {
                  Crossfade(currentDestination == navItem.route, label = "NavBarItemIcon") {
                    Icon(
                      imageVector = if (it) navItem.selectedIcon else navItem.unselectedIcon,
                      contentDescription = navItem.title,
                      tint = VLRTheme.colorScheme.onPrimaryContainer,
                    )
                  }
                },
                onClick = { navItem.onClick() },
                label = { Text(text = navItem.title) },
              )
            }
          },
          layoutType =
            if (!hideNav || navSuiteType != NavigationSuiteType.NavigationBar) navSuiteType
            else NavigationSuiteType.None,
        ) {
          Box(modifier = Modifier.semantics { testTagsAsResourceId = true }) {
            VlrNavHost(
              navController = navController,
              innerPadding =
                WindowInsets.statusBars.asPaddingValues() +
                  WindowInsets.navigationBars.asPaddingValues(),
              paneState = { nav -> hideNav = nav },
            ) {
              currentNav = it
            }
          }
        }
      }
    }
  }
}
