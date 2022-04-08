package dev.staticvar.vlr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.staticvar.vlr.ui.PaddingLocalCompositions
import dev.staticvar.vlr.ui.VLR
import dev.staticvar.vlr.ui.theme.VLRTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)
    WindowCompat.setDecorFitsSystemWindows(window, false)
    setContent { VLRTheme() { PaddingLocalCompositions { VLR() } } }
  }
}
