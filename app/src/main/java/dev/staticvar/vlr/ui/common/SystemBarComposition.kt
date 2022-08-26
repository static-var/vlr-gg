package dev.staticvar.vlr.ui.common

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dev.staticvar.vlr.ui.theme.VLRTheme

@Composable
fun SetStatusBarColor() {
  val primaryContainer = VLRTheme.colorScheme.surface.copy(0.2f)
  val systemUiController = rememberSystemUiController()
  SideEffect { systemUiController.setStatusBarColor(primaryContainer) }
}

@Composable
fun StatusBarColorForHome() {
  val primaryContainer = Color.Transparent
  val systemUiController = rememberSystemUiController()
  val isDarkMode = isSystemInDarkTheme()

  SideEffect { systemUiController.setStatusBarColor(primaryContainer, darkIcons = !isDarkMode) }
}

@Composable
fun StatusBarColorForHomeWithTabs() {
  val primaryContainer = VLRTheme.colorScheme.surface
  val systemUiController = rememberSystemUiController()
  SideEffect { systemUiController.setStatusBarColor(primaryContainer) }
}
