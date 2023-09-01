package dev.staticvar.vlr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import dev.staticvar.vlr.ui.PaddingLocalCompositions
import dev.staticvar.vlr.ui.VLR
import dev.staticvar.vlr.ui.theme.VLRTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    installSplashScreen()
    super.onCreate(savedInstanceState)
    setContent { VLRTheme() { PaddingLocalCompositions { VLR() } } }
  }
}
