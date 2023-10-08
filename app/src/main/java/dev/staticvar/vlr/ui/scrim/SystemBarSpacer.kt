package dev.staticvar.vlr.ui.scrim

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.staticvar.vlr.ui.theme.VLRTheme
import dev.staticvar.vlr.ui.theme.elevatedSurfaceColor
import dev.staticvar.vlr.ui.theme.transparent

@Composable
fun StatusBarSpacer(
  modifier: Modifier = Modifier,
  statusBarType: StatusBarType
) {
  Spacer(
    modifier
      .windowInsetsTopHeight(WindowInsets.statusBars)
      .fillMaxWidth()
      .background(
        if (statusBarType == StatusBarType.TRANSPARENT) VLRTheme.colorScheme.transparent
        else VLRTheme.colorScheme.surface
      )
  )
}

@Composable
fun NavigationBarSpacer(
  modifier: Modifier = Modifier,
  navigationBarType: NavigationBarType
) {
  Spacer(
    modifier
      .windowInsetsBottomHeight(WindowInsets.navigationBars)
      .fillMaxWidth()
      .background(
        if (navigationBarType == NavigationBarType.TRANSPARENT) VLRTheme.colorScheme.transparent
        else VLRTheme.colorScheme.elevatedSurfaceColor
      )
  )
}