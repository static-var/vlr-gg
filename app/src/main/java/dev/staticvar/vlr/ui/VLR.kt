package dev.staticvar.vlr.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.staticvar.vlr.R
import dev.staticvar.vlr.ui.helper.NavItem
import dev.staticvar.vlr.ui.helper.VlrNavHost
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.ui.theme.tintedBackground
import io.sentry.compose.withSentryObservableEffect

@OptIn(
  ExperimentalMaterial3AdaptiveNavigationSuiteApi::class, ExperimentalMaterial3AdaptiveApi::class
)
@Composable
fun VLR() {
  val navController = rememberNavController().withSentryObservableEffect()
  val backStackEntry by navController.currentBackStackEntryAsState()

  val viewModel: VlrViewModel = hiltViewModel()
  val action = remember(navController) { Action(navController) }

  viewModel.action = action

  val resetScroll = { viewModel.resetScroll() }

  var currentNav by remember { mutableStateOf(Destination.NewsOverview.route) }
  val currentDestination = backStackEntry?.destination?.route

  val navItems =
    listOf<NavItem>(
      NavItem(
        title = stringResource(id = R.string.news),
        Destination.NewsOverview.route,
        Icons.AutoMirrored.Filled.Feed,
        Icons.AutoMirrored.Outlined.Feed,
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
          else action.matchOverview().also { println("Match overview navigation") }
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

  var hideNav by remember {
    mutableStateOf(false)
  }

  val navSuiteType =
    NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())

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
                tint = VLRTheme.colorScheme.onPrimaryContainer
              )
            }
          },
          onClick = {
            if (currentDestination != navItem.route)
              navItem.onClick()
//              navController.navigate(navItem.route)
          },
          label = { Text(text = navItem.title) }
        )
      }

    },
    layoutType = if (!hideNav || navSuiteType != NavigationSuiteType.NavigationBar) navSuiteType else NavigationSuiteType.None
  ) {
    Box(
      modifier =
      Modifier
        .background(VLRTheme.colorScheme.tintedBackground)
        .semantics { testTagsAsResourceId = true }
    ) {
      VlrNavHost(
        navController = navController,
        paneState = { nav ->
          hideNav = nav
        }
      ) { currentNav = it }
    }
  }
}
