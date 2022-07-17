package dev.staticvar.vlr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Feed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Feed
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dev.staticvar.vlr.R
import dev.staticvar.vlr.ui.helper.*
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.ui.theme.tintedBackground
import dev.staticvar.vlr.utils.i

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VLR() {
  val navController = rememberAnimatedNavController()
  val viewModel: VlrViewModel = hiltViewModel()
  val action = remember(navController) { Action(navController) }

  val systemUiController = rememberSystemUiController()
  val background = VLRTheme.colorScheme.surfaceColorAtElevation(8.dp)
  val transparent = Color.Transparent

  viewModel.action = action

  val resetScroll = { viewModel.resetScroll() }

  var currentNav by remember { mutableStateOf(Destination.NewsOverview.route) }
  val navItems =
    listOf<NavItem>(
      NavItem(
        title = stringResource(id = R.string.news),
        Destination.NewsOverview.route,
        Icons.Filled.Feed,
        Icons.Outlined.Feed,
        onClick = { if (currentNav == Destination.News.route) resetScroll() else action.goNews() }
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
          Destination.About.route -> background
          else -> transparent
        },
    )
  }

  val context = LocalContext.current
  val currentAppVersion = context.currentAppVersion
  val remoteAppVersion by
    remember(viewModel) { viewModel.getLatestAppVersion() }.collectAsState(initial = null)

  val needAppUpdate by
    produceState<Boolean>(initialValue = false, remoteAppVersion, currentAppVersion) {
      remoteAppVersion?.let { remoteVersion ->
        value =
          if (remoteVersion.trim().equals(currentAppVersion, ignoreCase = true)) {
            i { "No new version available" }
            false
          } else {
            i { "New version | Remote Version $remoteVersion | Local Version $currentAppVersion" }
            true
          }
      }
    }

  if (needAppUpdate) {
    AppUpdateDownloadPopup(viewModel)
  }

  Scaffold(
    bottomBar = { NewNavBar(navController = navController, items = navItems, currentNav) }
  ) { paddingValues ->
    Box(
      modifier = Modifier.padding(paddingValues).background(VLRTheme.colorScheme.tintedBackground),
    ) {
      VlrNavHost(navController = navController) { currentNav = it }
    }
  }
}
