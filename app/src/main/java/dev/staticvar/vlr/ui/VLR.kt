package dev.staticvar.vlr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
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
import dev.staticvar.vlr.data.NavState
import dev.staticvar.vlr.ui.helper.AppUpdateDownloadPopup
import dev.staticvar.vlr.ui.helper.VlrBottomNavbar
import dev.staticvar.vlr.ui.helper.VlrNavHost
import dev.staticvar.vlr.ui.helper.currentAppVersion
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

  val navState: NavState by viewModel.navState.collectAsState(NavState.NEWS_OVERVIEW)

  LaunchedEffect(navState) {
    systemUiController.setNavigationBarColor(
      color =
        when (navState) {
          NavState.MATCH_DETAILS,
          NavState.TOURNAMENT_DETAILS,
          NavState.TEAM_DETAILS,
          NavState.NEWS -> transparent
          NavState.NEWS_OVERVIEW,
          NavState.TOURNAMENT,
          NavState.MATCH_OVERVIEW,
          NavState.ABOUT -> background
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
    bottomBar = { VlrBottomNavbar(navState = navState, action = action, viewModel = viewModel) }
  ) { paddingValues ->
    Box(
      modifier = Modifier.padding(paddingValues).background(VLRTheme.colorScheme.tintedBackground),
    ) {
      VlrNavHost(navController = navController)
    }
  }
}
